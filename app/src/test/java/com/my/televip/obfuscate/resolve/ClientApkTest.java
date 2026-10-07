package com.my.televip.obfuscate.resolve;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.my.televip.obfuscate.dex.DexClass;
import com.my.televip.obfuscate.dex.DexIndex;
import com.my.televip.obfuscate.dex.DexNames;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks that every feature's hook points resolve in a client APK - any Telegram client, any build.
 * Run by the weekly Client watch workflow against each client's latest release:
 *
 * <pre>TELEVIP_CLIENT_APK=client.apk [TELEVIP_CLIENT_REPORT=report.md] gradlew :app:testDebugUnitTest --tests '*ClientApkTest'</pre>
 */
public class ClientApkTest {

    /**
     * Every symbol the module's code asks for (read from its sources, see {@link CallSites})
     * must land on something in this build, unless the module has a working fallback for it or
     * another route to the same feature resolves.
     */
    @Test
    public void everyFeatureHookPointResolves() throws Exception {
        String path = System.getenv("TELEVIP_CLIENT_APK");
        assumeTrue("set TELEVIP_CLIENT_APK to run", path != null && new File(path).isFile());

        DexIndex index = DexIndex.fromApk(new File(path));
        Resolver.Report report = new Resolver.Report();
        Mapping mapping = new Resolver(index, TelegramFingerprints.owners()).resolve(TelegramFingerprints.all(), report);
        List<CallSites.Site> sites = CallSites.read();
        List<CallSites.Site> missing = CallSites.missing(index, mapping, sites);
        String pkg = ApkPackage.of(new File(path));
        CallSites.Verdict verdict = CallSites.verdict(sites, missing, pkg);

        StringBuilder md = new StringBuilder();
        md.append("Package ").append(pkg).append(". Symbols: ").append(report.count(Resolver.Outcome.KEPT)).append(" by real name, ")
                .append(report.count(Resolver.Outcome.FINGERPRINTED)).append(" fingerprinted, ")
                .append(report.count(Resolver.Outcome.AMBIGUOUS)).append(" ambiguous, ")
                .append(report.count(Resolver.Outcome.UNRESOLVED)).append(" unresolved. Call sites: ")
                .append(sites.size() - missing.size()).append(" of ").append(sites.size()).append(" resolve.\n\n");
        if (verdict.broken.isEmpty()) {
            md.append("Every feature reaches its code.\n");
        } else {
            md.append("| Feature | Unresolved |\n|---|---|\n");
            for (Map.Entry<String, List<String>> e : verdict.broken.entrySet()) {
                md.append("| ").append(e.getKey()).append(" | `").append(String.join("`, `", e.getValue())).append("` |\n");
            }
        }
        if (!verdict.degraded.isEmpty()) {
            md.append("\nUsing a fallback: ").append(String.join("; ", verdict.degraded)).append(".\n");
        }
        writeReport(md.toString());

        if (!verdict.broken.isEmpty()) fail("Features broken on this build:\n" + md);
    }

    /**
     * Wherever this build keeps a real name, the fingerprint alone must find the same symbol - or
     * nothing. A fingerprint that picks another symbol here would hook the wrong code on a fork
     * that renames it.
     */
    @Test
    public void fingerprintsNeverContradictRealNames() throws Exception {
        String path = System.getenv("TELEVIP_CLIENT_APK");
        assumeTrue("set TELEVIP_CLIENT_APK to run", path != null && new File(path).isFile());

        DexIndex index = DexIndex.fromApk(new File(path));
        Mapping named = new Resolver(index, TelegramFingerprints.owners())
                .resolve(TelegramFingerprints.all(), new Resolver.Report());
        Mapping fingerprinted = new Resolver(index, TelegramFingerprints.owners()).fingerprintsOnly()
                .resolve(TelegramFingerprints.all(), new Resolver.Report());

        List<String> wrong = new ArrayList<>();
        compare(named.classes(), fingerprinted.classes(), wrong);
        compare(named.fields(), fingerprinted.fields(), wrong);
        compare(named.methods(), fingerprinted.methods(), wrong);
        if (wrong.isEmpty()) return;
        String md = "\n### Fingerprints that find the wrong symbol\n\n| Symbol | Found | Real |\n|---|---|---|\n";
        for (String w : wrong) md += "| " + w + " |\n";
        writeReport(md);
        fail(md);
    }

    /**
     * AdBlock finds each ad request's deserializeResponse by its shape, not its name (see
     * AdBlock#answerReader): exactly one instance method taking (stream, int, boolean) and
     * returning something. Requests a build does not have are the call-site check's business.
     */
    @Test
    public void adAnswersCanBeReplaced() throws Exception {
        String path = System.getenv("TELEVIP_CLIENT_APK");
        assumeTrue("set TELEVIP_CLIENT_APK to run", path != null && new File(path).isFile());

        DexIndex index = DexIndex.fromApk(new File(path));
        Mapping mapping = new Resolver(index, TelegramFingerprints.owners())
                .resolve(TelegramFingerprints.all(), new Resolver.Report());
        List<String> wrong = new ArrayList<>();
        for (String request : new String[]{"org.telegram.tgnet.TLRPC$TL_messages_getSponsoredMessages",
                "org.telegram.tgnet.TLRPC$TL_contacts_getSponsoredPeers"}) {
            String name = mapping.classes().containsKey(request) ? mapping.classes().get(request) : request;
            DexClass cls = index.byDescriptor(DexNames.toDescriptor(name));
            if (cls == null) continue;
            int readers = 0;
            for (DexClass.Method m : cls.methods) {
                String[] p = m.parameterTypes();
                if (!m.isStatic() && !m.isConstructor() && p.length == 3 && p[1].equals("I") && p[2].equals("Z")
                        && !m.returnType().equals("V")) readers++;
            }
            if (readers != 1) wrong.add(request + " (" + name + ") has " + readers);
        }
        if (wrong.isEmpty()) return;
        String md = "\n### Ad answers Block Ads cannot replace\n\n" + String.join("\n", wrong) + "\n";
        writeReport(md);
        fail(md);
    }

    private static void compare(Map<String, String> truth, Map<String, String> found, List<String> wrong) {
        for (Map.Entry<String, String> e : truth.entrySet()) {
            String got = found.get(e.getKey());
            if (got != null && !got.equals(e.getValue())) {
                wrong.add("`" + e.getKey() + "` | `" + got + "` | `" + e.getValue() + "`");
            }
        }
    }

    /** Appends, so both tests' findings end up in the one report. */
    private static void writeReport(String text) throws Exception {
        String out = System.getenv("TELEVIP_CLIENT_REPORT");
        if (out == null) return;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out, true), StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }
}
