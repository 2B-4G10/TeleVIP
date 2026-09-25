package com.my.televip.obfuscate.resolve;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import com.my.televip.obfuscate.dex.DexIndex;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * Resolves everything against a real Nekogram 12.10.3 APK (versionCode 70890) and pins the result.
 *
 * <p>Each expected name was checked by hand against the bytecode and the matching Nekogram source
 * tag, so a failure here means a fingerprint or the dex reader changed behaviour. Point
 * {@code TELEVIP_NEKOGRAM_12103_APK} at the APK to run it; without it the test is skipped, which
 * is the case on CI.</p>
 */
public class NekogramApkTest {

    private static Mapping mapping;
    private static Resolver.Report report;

    @BeforeClass
    public static void resolve() throws Exception {
        String path = System.getenv("TELEVIP_NEKOGRAM_12103_APK");
        if (path == null || !new File(path).isFile()) return;
        Resolver resolver = new Resolver(DexIndex.fromApk(new File(path)), TelegramFingerprints.owners());
        report = new Resolver.Report();
        mapping = resolver.resolve(TelegramFingerprints.all(), report);
    }

    private static void requireApk() {
        assumeTrue("set TELEVIP_NEKOGRAM_12103_APK to run", mapping != null);
    }

    @Test
    public void keptNamesResolveToThemselves() {
        requireApk();
        assertEquals("org.telegram.messenger.MessagesController",
                mapping.resolveClass("org.telegram.messenger.MessagesController"));
        assertEquals("deleteMessages", mapping.resolveMethod("MessagesController", "deleteMessagesAAOJZIZJOIZI"));
        assertEquals("ttl", mapping.resolveField("TLRPC$Message", "ttl"));
    }

    @Test
    public void theSettingsEntryResolvesEndToEnd() {
        requireApk();
        assertEquals("n8b", mapping.resolveClass("org.telegram.ui.Components.UItem"));
        assertEquals("y9b", mapping.resolveClass("org.telegram.ui.Components.UniversalAdapter"));
        assertEquals("ee9", mapping.resolveClass("org.telegram.ui.SettingsActivity$SettingCell$Factory"));
        assertEquals("fe9", mapping.resolveClass("org.telegram.ui.SettingsActivity$SettingCell"));
        assertEquals("a", mapping.resolveMethod("SettingsActivity$SettingCell$Factory", "ofIIIICCC"));
        assertEquals("b0", mapping.resolveMethod("SettingsActivity", "fillItems"));
        assertEquals("m0", mapping.resolveMethod("SettingsActivity", "onClick"));
        // R8 deleted the unused parameters; the call sites must be told the real lists.
        assertArrayEquals(new String[]{"java.util.ArrayList"}, mapping.resolveParameters("fillItems"));
        assertArrayEquals(new String[]{"n8b"}, mapping.resolveParameters("onClick"));
        assertEquals("d", mapping.resolveField("UItem", "id"));
        assertEquals("m", mapping.resolveField("UItem", "text"));
        assertEquals("n", mapping.resolveField("UItem", "subtext"));
        assertEquals("n", mapping.resolveField("SettingsActivity$SettingCell", "iconView"));
    }

    @Test
    public void theSettingsPageResolves() {
        requireApk();
        assertEquals("I0", mapping.resolveField("LaunchActivity", "frameLayout"));
        assertEquals("wka", mapping.resolveClass("org.telegram.ui.Cells.TextCheckCell"));
        assertEquals("h", mapping.resolveMethod("TextCheckCell", "setTextAndCheck"));
        assertEquals("d", mapping.resolveMethod("TextCheckCell", "setChecked"));
        assertEquals("rma", mapping.resolveClass("org.telegram.ui.Cells.TextSettingsCell"));
        assertEquals("qs4", mapping.resolveClass("org.telegram.ui.Cells.HeaderCell"));
    }

    @Test
    public void anchorsFollowRealCode() {
        requireApk();
        assertEquals("mw5", mapping.resolveClass("androidx.collection.LongSparseArray"));
        assertEquals("q6a", mapping.resolveClass("org.telegram.ui.Stories.StoriesController"));
        assertEquals("H", mapping.resolveMethod("StoriesController", "hasStoriesJ"));
        assertEquals("coa", mapping.resolveClass("org.telegram.ui.ActionBar.Theme"));
        assertEquals("ut0", mapping.resolveClass("org.telegram.messenger.browser.Browser"));
        assertEquals("w32", mapping.resolveClass("org.telegram.ui.Cells.ChatMessageCell"));
        assertEquals("l", mapping.resolveMethod("ChatMessageCell", "getMessageObject"));
        assertEquals("k", mapping.resolveMethod("AlertDialog$Builder", "setTitle"));
        assertEquals("f", mapping.resolveMethod("AlertDialog$Builder", "setMessage"));
        // openUrlInSystemBrowser shares openUrl(Context, String)'s signature; only openUrl skips
        // straight past the ten-parameter overload.
        assertEquals("r", mapping.resolveMethod("Browser", "openUrlCS"));
        // isCurrentThemeDay is the same call negated; only the un-negated one is isCurrentThemeDark.
        assertEquals("a1", mapping.resolveMethod("Theme", "isCurrentThemeDark"));
        // Prevent Media: found by the kept MessagesController calls they make, although R8
        // narrowed their Runnable returns to the lambda classes.
        assertEquals("Ua", mapping.resolveMethod("ChatActivity", "sendSecretMediaDelete"));
        assertEquals("Va", mapping.resolveMethod("ChatActivity", "sendSecretMessageRead"));
    }

    /** What cannot be pinned down must stay unresolved - these are the ones that would be guesses. */
    @Test
    public void lookalikesAreRefusedNotGuessed() {
        requireApk();
        // All three button setters live behind one merged click handler; nothing separates them.
        assertNull(mapping.resolveMethod("AlertDialog$Builder", "setPositiveButton"));
        assertNull(mapping.resolveMethod("AlertDialog$Builder", "setNegativeButton"));
        assertNull(mapping.resolveMethod("AlertDialog$Builder", "setNeutralButton"));
        // Inlined away in this build; the only no-arg lookalike inspects peers and must not match.
        assertNull(mapping.resolveMethod("StoriesController", "hasStories"));
        // Only the seven-argument overload survived R8.
        assertNull(mapping.resolveMethod("SettingsActivity$SettingCell$Factory", "ofIIIICC"));
    }

    @Test
    public void nothingResolvesToAStaleOldName() {
        requireApk();
        // 12.8.1 called MessagesController "org.telegram.messenger.n0"; that class still exists in
        // 12.10.3 as something else, which is exactly why an old table must never be applied.
        assertEquals("org.telegram.messenger.MessagesController",
                mapping.resolveClass("org.telegram.messenger.MessagesController"));
    }
}
