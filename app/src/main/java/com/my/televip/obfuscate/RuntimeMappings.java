package com.my.televip.obfuscate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.my.televip.ClientChecker;
import com.my.televip.logging.Logger;
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
 * Keeps an obfuscated client working across its own updates.
 *
 * <p>The static per-client tables name what one specific build called each class. Run against any
 * other build of that client, they are not merely out of date - most of those names still exist,
 * attached to different classes, so hooks land on the wrong code. So when the running build is not
 * the one the table was made from, the table is not used at all. Instead every symbol is resolved
 * against the running APK itself: by its real name where the build kept it, by fingerprint where it
 * was renamed. What cannot be pinned down unambiguously is left unresolved and shows up as missing
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

    /** The mapping in force, or null when the static table (or no table) is being used. */
    public static Mapping active() {
        return active;
    }

    public static String summary() {
        return summary;
    }

    // ------------------------------------------------------------ prefetch

    /**
     * Starts resolving in the background for an obfuscated client. Called when the client's class
     * loader is ready, well before any activity; costs nothing for other clients.
     */
    public static void prefetch(String packageName, ClassLoader classLoader) {
        try {
            ClientChecker.ClientType client = ClientChecker.ClientType.fromPackage(packageName);
            if (client == null || !client.isTgnetObfuscated()) return;
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
     * Decides, once the first activity exists, whether the static table can be trusted for this
     * build. Returns normally in every case; failures leave the client on name lookups only.
     */
    public static void activate(Context context, String packageName) {
        try {
            ClientChecker.ClientType client = ClientChecker.ClientType.fromPackage(packageName);
            if (client == null || !client.isTgnetObfuscated()) return;

            long running = versionCode(context);
            Long verified = ClientChecker.verifiedVersionCode(client);
            if (verified != null && verified == running) {
                summary = "static table matches this build (" + running + ")";
                Logger.l("obfuscation: " + summary);
                return;
            }

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
            summary = "running build " + running + " is not the table's (" + verified + "), resolved "
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
                if (dir != null && (dir.isDirectory() || dir.mkdirs())) writeText(cache, mapping.serialize());
            } catch (Throwable ignored) {
                // Not being able to cache only costs the next launch a rescan.
            }
        }
        return mapping;
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
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
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
