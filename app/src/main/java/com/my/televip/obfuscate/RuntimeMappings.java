package com.my.televip.obfuscate;

import android.content.Context;

import com.my.televip.ClientChecker;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.obfuscate.dex.DexIndex;
import com.my.televip.obfuscate.resolve.Mapping;
import com.my.televip.obfuscate.resolve.Resolver;
import com.my.televip.obfuscate.resolve.TelegramFingerprints;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Keeps every client working across its own updates, whatever R8 renamed in it.
 *
 * <p>Even official Telegram renames its UI classes (ChatActivity, SettingsActivity, ...) in each
 * release, and obfuscated forks rename nearly everything. The static per-client tables name what
 * one specific build called each class. Run against any other build of that client, they are not
 * merely out of date - most of those names still exist, attached to different classes, so hooks
 * land on the wrong code - and even on their own build they lack every symbol added since. So
 * every symbol is resolved against the running APK itself, on every client and build: by its real
 * name where the build kept it, by fingerprint where it was renamed. What cannot be pinned down unambiguously is left unresolved and shows up as missing
 * in the hook health report, rather than being guessed.</p>
 *
 * <p>Resolution starts as soon as the client's class loader exists, off the main thread, so it is
 * normally finished before the first activity needs it. The result is cached per build, so after
 * the first launch following an update it is a file read.</p>
 */
public final class RuntimeMappings {

    /** Bump when the cache file format or the resolver's behaviour changes. */
    private static final int CACHE_FORMAT = 1;

    private static volatile Mapping active;
    private static volatile FutureTask<Mapping> prefetch;
    private static volatile String summary;

    private RuntimeMappings() {
    }

    /** The mapping in force, or null when the static table is being used. */
    public static Mapping active() {
        return active;
    }

    // ------------------------------------------------------------ prefetch

    /**
     * Starts resolving in the background. Called when the client's class loader is ready, well
     * before any activity.
     */
    public static void prefetch(String packageName, ClassLoader classLoader) {
        try {
            ClientChecker.ClientType client = ClientChecker.ClientType.fromPackage(packageName);
            if (client == null) return;
            final String apk = apkPathOf(classLoader);
            if (apk == null) return;
            final File cache = cacheFile(defaultCacheDir(packageName), packageName, new File(apk));
            FutureTask<Mapping> task = new FutureTask<>(new Callable<Mapping>() {
                @Override
                public Mapping call() throws Exception {
                    return loadOrResolve(new File(apk), cache);
                }
            });
            prefetch = task;
            Thread thread = new Thread(task, "TeleVip-resolve");
            thread.setDaemon(true);
            thread.start();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    // ------------------------------------------------------------ activation

    /**
     * Puts the APK's mapping in force once the first activity exists. Returns normally in every
     * case; failures leave the client on name lookups only.
     */
    public static void activate(Context context, String packageName) {
        try {
            ClientChecker.ClientType client = ClientChecker.ClientType.fromPackage(packageName);
            if (client == null) return;

            // Always from the APK, even on the build a static table was made from: the tables lack
            // every symbol added since (Block Ads' among them), and ClientChecker's verified build -
            // which once picked the table - moves on whenever a release is checked against a newer
            // build. Resolving from the APK is also what the client checks test, on every build.
            long running = versionCode(context);

            long start = System.nanoTime();
            Mapping mapping = null;
            FutureTask<Mapping> task = prefetch;
            if (task != null) {
                try {
                    mapping = task.get(20, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    Logger.w("obfuscation: background resolution failed, retrying in place: " + t);
                }
            }
            if (mapping == null) {
                File apk = new File(context.getApplicationInfo().sourceDir);
                mapping = loadOrResolve(apk, cacheFile(context.getCacheDir(), packageName, apk));
            }
            active = mapping;
            long ms = (System.nanoTime() - start) / 1_000_000;
            summary = "build " + running + ", resolved "
                    + mapping.size() + " symbols from the APK in " + ms + " ms"
                    + (lastReport == null ? " (cached)" : " - " + describe(lastReport));
            Logger.l("obfuscation: " + summary);
        } catch (Throwable t) {
            // Never fall back to the stale table. With no mapping, names resolve to themselves:
            // kept names work, renamed ones are reported missing.
            active = new Mapping();
            summary = "resolution failed, using real names only: " + t;
            Logger.e(t);
        }
    }

    // ------------------------------------------------------------ resolution

    private static volatile Resolver.Report lastReport;

    static Mapping loadOrResolve(File apk, File cache) throws Exception {
        if (cache != null && cache.isFile()) {
            try {
                return Mapping.deserialize(readText(cache));
            } catch (Throwable ignored) {
                // A corrupt cache is just a cache miss.
            }
        }
        DexIndex index = DexIndex.fromApk(apk);
        Resolver resolver = new Resolver(index, TelegramFingerprints.owners());
        Resolver.Report report = new Resolver.Report();
        Mapping mapping = resolver.resolve(TelegramFingerprints.all(), report);
        lastReport = report;
        if (cache != null) {
            try {
                File dir = cache.getParentFile();
                if (dir != null && (dir.isDirectory() || dir.mkdirs())) {
                    writeText(cache, mapping.serialize());
                    deleteStale(dir, cache);
                }
            } catch (Throwable ignored) {
                // Not being able to cache only costs the next launch a rescan.
            }
        }
        return mapping;
    }

    /** Mappings of earlier client builds (or fingerprint versions) are never read again. */
    private static void deleteStale(File dir, File current) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (!f.equals(current) && name.startsWith("mapping-") && name.endsWith(".txt")) f.delete();
        }
    }

    private static String describe(Resolver.Report report) {
        return report.count(Resolver.Outcome.KEPT) + " by real name, "
                + report.count(Resolver.Outcome.FINGERPRINTED) + " by fingerprint, "
                + report.count(Resolver.Outcome.AMBIGUOUS) + " refused as ambiguous, "
                + report.count(Resolver.Outcome.UNRESOLVED) + " not found";
    }

    // --------------------------------------------------------------- helpers

    /**
     * The client's own APK, from its class loader's description - public, stable behaviour of
     * BaseDexClassLoader - so it is available before any Context exists.
     */
    static String apkPathOf(ClassLoader classLoader) {
        String description = String.valueOf(classLoader);
        int start = description.indexOf("/data/app/");
        if (start < 0) return null;
        int end = description.indexOf(".apk", start);
        return end > start ? description.substring(start, end + 4) : null;
    }

    /** The client's cache directory, computed from the process uid (100000 uids per user). */
    private static File defaultCacheDir(String packageName) {
        int userId = android.os.Process.myUid() / 100000;
        return new File("/data/user/" + userId + "/" + packageName + "/cache");
    }

    private static File cacheFile(File dir, String packageName, File apk) {
        if (dir == null) return null;
        String key = packageName + "-" + apk.length() + "-" + apk.lastModified()
                + "-f" + TelegramFingerprints.VERSION + "-c" + CACHE_FORMAT;
        return new File(new File(dir, "televip"), "mapping-" + key + ".txt");
    }

    private static long versionCode(Context context) throws Exception {
        return Utils.versionCode(context.getPackageManager().getPackageInfo(context.getPackageName(), 0));
    }

    private static String readText(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void writeText(File file, String text) throws Exception {
        File tmp = new File(file.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(file)) tmp.delete();
    }
}
