package com.my.televip.obfuscate.resolve;

import static com.my.televip.obfuscate.resolve.Body.*;
import static com.my.televip.obfuscate.resolve.Classes.*;
import static com.my.televip.obfuscate.resolve.Symbol.cls;
import static com.my.televip.obfuscate.resolve.Symbol.field;
import static com.my.televip.obfuscate.resolve.Symbol.method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything TeleVip needs from a Telegram-based client, described so it can be found in a build
 * that renamed it.
 *
 * <p>The vocabulary - which classes, which member keys - is exactly the one the static per-client
 * tables use, so the result slots into {@code ResolverRegistry} in their place. Each symbol is
 * looked up by its real name first; the fingerprint is only consulted when the build renamed it.
 * The fingerprints describe Telegram's code, not one fork's, so they apply to any client built on
 * it; a fork only needs its own table where it has diverged.</p>
 *
 * <p>Change {@link #VERSION} whenever a fingerprint changes, so mappings cached on devices from an
 * older description are thrown away rather than trusted.</p>
 */
public final class TelegramFingerprints {

    public static final int VERSION = 2;

    private TelegramFingerprints() {
    }

    /** Owner simple names as the call sites write them, to full original names. */
    public static Map<String, String> owners() {
        Map<String, String> o = new HashMap<>();
        o.put("ActionBar", "org.telegram.ui.ActionBar.ActionBar");
        o.put("ActionBar$ActionBarMenuOnItemClick", "org.telegram.ui.ActionBar.ActionBar$ActionBarMenuOnItemClick");
        o.put("ActionBarMenuItem", "org.telegram.ui.ActionBar.ActionBarMenuItem");
        o.put("AlertDialog", "org.telegram.ui.ActionBar.AlertDialog");
        o.put("AlertDialog$Builder", "org.telegram.ui.ActionBar.AlertDialog$Builder");
        o.put("AlertDialog$OnButtonClickListener", "org.telegram.ui.ActionBar.AlertDialog$OnButtonClickListener");
        o.put("AndroidUtilities", "org.telegram.messenger.AndroidUtilities");
        o.put("ApplicationLoader", "org.telegram.messenger.ApplicationLoader");
        o.put("BaseFragment", "org.telegram.ui.ActionBar.BaseFragment");
        o.put("Browser", "org.telegram.messenger.browser.Browser");
        o.put("ChatActivity", "org.telegram.ui.ChatActivity");
        o.put("ChatActivity$ChatMessageCellDelegate", "org.telegram.ui.ChatActivity$ChatMessageCellDelegate");
        o.put("ChatMessageCell", "org.telegram.ui.Cells.ChatMessageCell");
        o.put("ChatMessageCell$MessageAccessibilityNodeProvider", "org.telegram.ui.Cells.ChatMessageCell$MessageAccessibilityNodeProvider");
        o.put("DispatchQueue", "org.telegram.messenger.DispatchQueue");
        o.put("FastDateFormat", "org.telegram.messenger.time.FastDateFormat");
        o.put("FileLoadOperation", "org.telegram.messenger.FileLoadOperation");
        o.put("FileLoader", "org.telegram.messenger.FileLoader");
        o.put("HeaderCell", "org.telegram.ui.Cells.HeaderCell");
        o.put("ImageReceiver", "org.telegram.messenger.ImageReceiver");
        o.put("LocaleController", "org.telegram.messenger.LocaleController");
        o.put("LongSparseArray", "androidx.collection.LongSparseArray");
        o.put("MessageObject", "org.telegram.messenger.MessageObject");
        o.put("MessagesController", "org.telegram.messenger.MessagesController");
        o.put("MessagesStorage", "org.telegram.messenger.MessagesStorage");
        o.put("NotificationCenter", "org.telegram.messenger.NotificationCenter");
        o.put("NotificationsController", "org.telegram.messenger.NotificationsController");
        o.put("PeerStoriesView$StoryItemHolder", "org.telegram.ui.Stories.PeerStoriesView$StoryItemHolder");
        o.put("PhotoViewer", "org.telegram.ui.PhotoViewer");
        o.put("PhotoViewer$PhotoViewerProvider", "org.telegram.ui.PhotoViewer$PhotoViewerProvider");
        o.put("PhotoViewer$PlaceProviderObject", "org.telegram.ui.PhotoViewer$PlaceProviderObject");
        o.put("ProfileActivity", "org.telegram.ui.ProfileActivity");
        o.put("QuickAckDelegate", "org.telegram.tgnet.QuickAckDelegate");
        o.put("RequestDelegateTimestamp", "org.telegram.tgnet.RequestDelegateTimestamp");
        o.put("SQLiteCursor", "org.telegram.SQLite.SQLiteCursor");
        o.put("SQLiteDatabase", "org.telegram.SQLite.SQLiteDatabase");
        o.put("SQLitePreparedStatement", "org.telegram.SQLite.SQLitePreparedStatement");
        o.put("SecretMediaViewer", "org.telegram.ui.SecretMediaViewer");
        o.put("SettingsActivity", "org.telegram.ui.SettingsActivity");
        o.put("SettingsActivity$SettingCell", "org.telegram.ui.SettingsActivity$SettingCell");
        o.put("SettingsActivity$SettingCell$Factory", "org.telegram.ui.SettingsActivity$SettingCell$Factory");
        o.put("ShadowSectionCell", "org.telegram.ui.Cells.ShadowSectionCell");
        o.put("SharedConfig", "org.telegram.messenger.SharedConfig");
        o.put("StoriesController", "org.telegram.ui.Stories.StoriesController");
        o.put("TLObject", "org.telegram.tgnet.TLObject");
        o.put("TLRPC$Chat", "org.telegram.tgnet.TLRPC$Chat");
        o.put("TLRPC$EncryptedChat", "org.telegram.tgnet.TLRPC$EncryptedChat");
        o.put("TLRPC$InputPeer", "org.telegram.tgnet.TLRPC$InputPeer");
        o.put("TLRPC$Message", "org.telegram.tgnet.TLRPC$Message");
        o.put("TLRPC$Peer", "org.telegram.tgnet.TLRPC$Peer");
        o.put("TLRPC$TL_channels_readHistory", "org.telegram.tgnet.TLRPC$TL_channels_readHistory");
        o.put("TLRPC$TL_channels_readMessageContents", "org.telegram.tgnet.TLRPC$TL_channels_readMessageContents");
        o.put("TLRPC$TL_inputPeerChannel", "org.telegram.tgnet.TLRPC$TL_inputPeerChannel");
        o.put("TLRPC$TL_messages_affectedMessages", "org.telegram.tgnet.TLRPC$TL_messages_affectedMessages");
        o.put("TLRPC$TL_messages_readDiscussion", "org.telegram.tgnet.TLRPC$TL_messages_readDiscussion");
        o.put("TLRPC$TL_messages_readEncryptedHistory", "org.telegram.tgnet.TLRPC$TL_messages_readEncryptedHistory");
        o.put("TLRPC$TL_messages_readHistory", "org.telegram.tgnet.TLRPC$TL_messages_readHistory");
        o.put("TLRPC$TL_messages_readMessageContents", "org.telegram.tgnet.TLRPC$TL_messages_readMessageContents");
        o.put("TLRPC$TL_messages_sendMedia", "org.telegram.tgnet.TLRPC$TL_messages_sendMedia");
        o.put("TLRPC$TL_messages_sendMessage", "org.telegram.tgnet.TLRPC$TL_messages_sendMessage");
        o.put("TLRPC$TL_messages_sendMultiMedia", "org.telegram.tgnet.TLRPC$TL_messages_sendMultiMedia");
        o.put("TLRPC$TL_messages_sendPaidReaction", "org.telegram.tgnet.TLRPC$TL_messages_sendPaidReaction");
        o.put("TLRPC$TL_messages_sendReaction", "org.telegram.tgnet.TLRPC$TL_messages_sendReaction");
        o.put("TLRPC$TL_messages_setEncryptedTyping", "org.telegram.tgnet.TLRPC$TL_messages_setEncryptedTyping");
        o.put("TLRPC$TL_messages_setTyping", "org.telegram.tgnet.TLRPC$TL_messages_setTyping");
        o.put("TLRPC$User", "org.telegram.tgnet.TLRPC$User");
        o.put("TLRPC$messages_Messages", "org.telegram.tgnet.TLRPC$messages_Messages");
        o.put("TL_account$updateStatus", "org.telegram.tgnet.tl.TL_account$updateStatus");
        o.put("TL_stories$TL_stories_incrementStoryViews", "org.telegram.tgnet.tl.TL_stories$TL_stories_incrementStoryViews");
        o.put("TL_stories$TL_stories_readStories", "org.telegram.tgnet.tl.TL_stories$TL_stories_readStories");
        o.put("TL_update$TL_updateDeleteChannelMessages", "org.telegram.tgnet.tl.TL_update$TL_updateDeleteChannelMessages");
        o.put("TL_update$TL_updateDeleteMessages", "org.telegram.tgnet.tl.TL_update$TL_updateDeleteMessages");
        o.put("TextCheckCell", "org.telegram.ui.Cells.TextCheckCell");
        o.put("TextSettingsCell", "org.telegram.ui.Cells.TextSettingsCell");
        o.put("Theme", "org.telegram.ui.ActionBar.Theme");
        o.put("Theme$ResourcesProvider", "org.telegram.ui.ActionBar.Theme$ResourcesProvider");
        o.put("UItem", "org.telegram.ui.Components.UItem");
        o.put("UItem$UItemFactory", "org.telegram.ui.Components.UItem$UItemFactory");
        o.put("UniversalAdapter", "org.telegram.ui.Components.UniversalAdapter");
        o.put("UserConfig", "org.telegram.messenger.UserConfig");
        o.put("Utilities", "org.telegram.messenger.Utilities");
        o.put("WriteToSocketDelegate", "org.telegram.tgnet.WriteToSocketDelegate");
        o.put("LaunchActivity", "org.telegram.ui.LaunchActivity");
        return Collections.unmodifiableMap(o);
    }

    public static List<Symbol> all() {
        List<Symbol> s = new ArrayList<>();
        keptInRecentBuilds(s);
        classes(s);
        settings(s);
        actionBar(s);
        alertDialog(s);
        cells(s);
        chat(s);
        photoViewer(s);
        stories(s);
        misc(s);
        return s;
    }

    /**
     * Symbols whose real names current builds keep. Listed without a fingerprint: if a future
     * build renames one, it shows up as missing in the hook health report rather than guessed at.
     */
    private static void keptInRecentBuilds(List<Symbol> s) {
        s.add(cls("org.telegram.messenger.AndroidUtilities"));
        s.add(cls("org.telegram.messenger.ApplicationLoader"));
        s.add(cls("org.telegram.messenger.DispatchQueue"));
        s.add(cls("org.telegram.messenger.FileLoadOperation"));
        s.add(cls("org.telegram.messenger.FileLoader"));
        s.add(cls("org.telegram.messenger.LocaleController"));
        s.add(cls("org.telegram.messenger.MessageObject"));
        s.add(cls("org.telegram.messenger.MessagesController"));
        s.add(cls("org.telegram.messenger.MessagesStorage"));
        s.add(cls("org.telegram.messenger.NotificationCenter"));
        s.add(cls("org.telegram.messenger.NotificationsController"));
        s.add(cls("org.telegram.messenger.SharedConfig"));
        s.add(cls("org.telegram.messenger.UserConfig"));
        s.add(cls("org.telegram.messenger.time.FastDateFormat"));
        s.add(cls("org.telegram.tgnet.QuickAckDelegate"));
        s.add(cls("org.telegram.tgnet.RequestDelegateTimestamp"));
        s.add(cls("org.telegram.tgnet.TLObject"));
        s.add(cls("org.telegram.tgnet.TLRPC$Chat"));
        s.add(cls("org.telegram.tgnet.TLRPC$EncryptedChat"));
        s.add(cls("org.telegram.tgnet.TLRPC$InputPeer"));
        s.add(cls("org.telegram.tgnet.TLRPC$Message"));
        s.add(cls("org.telegram.tgnet.TLRPC$Peer"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_channels_readHistory"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_channels_readMessageContents"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_inputPeerChannel"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_affectedMessages"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_readDiscussion"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_readEncryptedHistory"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_readHistory"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_readMessageContents"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_sendMedia"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_sendMessage"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_sendMultiMedia"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_sendPaidReaction"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_sendReaction"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_setEncryptedTyping"));
        s.add(cls("org.telegram.tgnet.TLRPC$TL_messages_setTyping"));
        s.add(cls("org.telegram.tgnet.TLRPC$User"));
        s.add(cls("org.telegram.tgnet.TLRPC$messages_Messages"));
        s.add(cls("org.telegram.tgnet.WriteToSocketDelegate"));
        s.add(cls("org.telegram.tgnet.tl.TL_account$updateStatus"));
        s.add(cls("org.telegram.tgnet.tl.TL_stories$TL_stories_incrementStoryViews"));
        s.add(cls("org.telegram.tgnet.tl.TL_stories$TL_stories_readStories"));
        s.add(cls("org.telegram.tgnet.tl.TL_update$TL_updateDeleteChannelMessages"));
        s.add(cls("org.telegram.tgnet.tl.TL_update$TL_updateDeleteMessages"));
        s.add(field("ApplicationLoader", "applicationContext"));
        s.add(field("FileLoadOperation", "downloadChunkSizeBig"));
        s.add(field("FileLoadOperation", "maxCdnParts"));
        s.add(field("FileLoadOperation", "maxDownloadRequests"));
        s.add(field("FileLoadOperation", "maxDownloadRequestsBig"));
        s.add(field("LocaleController", "currentLocale"));
        s.add(field("LocaleController", "isRTL"));
        s.add(field("MessagesController", "dialogMessagesByIds"));
        s.add(field("NotificationCenter", "messagesDeleted"));
        s.add(field("NotificationCenter", "tlSchemeParseException"));
        s.add(field("UserConfig", "clientUserId"));
        s.add(field("UserConfig", "selectedAccount"));
        s.add(field("Utilities", "stageQueue"));
        s.add(field("TLRPC$InputPeer", "channel_id"));
        s.add(field("TLRPC$InputPeer", "chat_id"));
        s.add(field("TLRPC$InputPeer", "user_id"));
        s.add(field("TLRPC$Message", "flags"));
        s.add(field("TLRPC$Message", "from_id"));
        s.add(field("TLRPC$Message", "id"));
        s.add(field("TLRPC$Message", "message"));
        s.add(field("TLRPC$Message", "ttl"));
        s.add(field("TLRPC$Peer", "channel_id"));
        s.add(field("TLRPC$Peer", "chat_id"));
        s.add(field("TLRPC$Peer", "user_id"));
        s.add(field("TLRPC$TL_channels_readHistory", "channel"));
        s.add(field("TLRPC$TL_channels_readHistory", "max_id"));
        s.add(field("TLRPC$TL_messages_affectedMessages", "pts"));
        s.add(field("TLRPC$TL_messages_affectedMessages", "pts_count"));
        s.add(field("TLRPC$TL_messages_readHistory", "max_id"));
        s.add(field("TLRPC$TL_messages_readHistory", "peer"));
        s.add(field("TLRPC$TL_messages_readDiscussion", "peer"));
        s.add(field("TLRPC$TL_messages_sendMedia", "peer"));
        s.add(field("TLRPC$TL_messages_sendMessage", "peer"));
        s.add(field("TLRPC$TL_messages_sendMultiMedia", "peer"));
        s.add(field("TLRPC$TL_messages_sendPaidReaction", "peer"));
        s.add(field("TLRPC$TL_messages_sendReaction", "peer"));
        s.add(field("TLRPC$User", "phone"));
        s.add(field("TLRPC$messages_Messages", "messages"));
        s.add(field("TL_account$updateStatus", "offline"));
        s.add(field("TL_update$TL_updateDeleteChannelMessages", "channel_id"));
        s.add(field("TL_update$TL_updateDeleteChannelMessages", "messages"));
        s.add(field("TL_update$TL_updateDeleteMessages", "messages"));
        s.add(field("MessagesController", "dialogMessage"));
        s.add(method("SQLiteCursor", "byteBufferValue"));
        s.add(method("SQLiteCursor", "dispose"));
        s.add(method("SQLiteCursor", "intValue"));
        s.add(method("SQLiteCursor", "longValue"));
        s.add(method("SQLiteCursor", "next"));
        s.add(method("SQLiteDatabase", "executeFast"));
        s.add(method("SQLiteDatabase", "queryFinalized"));
        s.add(method("SQLitePreparedStatement", "bindByteBufferIB").named("bindByteBuffer"));
        s.add(method("SQLitePreparedStatement", "bindByteBufferIO").named("bindByteBuffer"));
        s.add(method("SQLitePreparedStatement", "bindByteBufferJIBI").named("bindByteBuffer"));
        s.add(method("SQLitePreparedStatement", "bindInteger"));
        s.add(method("SQLitePreparedStatement", "bindLongIJ").named("bindLong"));
        s.add(method("SQLitePreparedStatement", "bindLongJIJ").named("bindLong"));
        s.add(method("SQLitePreparedStatement", "dispose"));
        s.add(method("SQLitePreparedStatement", "requery"));
        s.add(method("SQLitePreparedStatement", "step"));
        s.add(method("SQLitePreparedStatement", "stepJ").named("step"));
        s.add(method("AndroidUtilities", "isTabletInternal"));
        s.add(method("DispatchQueue", "postRunnableR").named("postRunnable"));
        s.add(method("DispatchQueue", "postRunnableRJ").named("postRunnable"));
        s.add(method("FileLoadOperation", "updateParams"));
        s.add(method("FileLoader", "getInstance"));
        s.add(method("FileLoader", "getPathToMessageO").named("getPathToMessage"));
        s.add(method("FileLoader", "getPathToMessageOZ").named("getPathToMessage"));
        s.add(method("FileLoader", "getPathToMessageOZZ").named("getPathToMessage"));
        s.add(method("ImageReceiver", "getImageLocation"));
        s.add(method("LocaleController", "formatShortNumber"));
        s.add(method("LocaleController", "formatYearMont"));
        s.add(method("LocaleController", "getInstance"));
        s.add(method("MessageObject", "canForwardMessage"));
        s.add(method("MessageObject", "getDialogId"));
        s.add(method("MessageObject", "getDialogIdO").named("getDialogId"));
        s.add(method("MessageObject", "isMusic"));
        s.add(method("MessageObject", "isSecret"));
        s.add(method("MessageObject", "isVoice"));
        s.add(method("MessagesController", "checkPromoInfoInternal"));
        s.add(method("MessagesController", "deleteMessagesAAOJIZI").named("deleteMessages"));
        s.add(method("MessagesController", "deleteMessagesAAOJIZIZ").named("deleteMessages"));
        s.add(method("MessagesController", "deleteMessagesAAOJZIZJOI").named("deleteMessages"));
        s.add(method("MessagesController", "deleteMessagesAAOJZIZJOIZI").named("deleteMessages"));
        s.add(method("MessagesController", "getGlobalMainSettings"));
        s.add(method("MessagesController", "getInputChannelJ").named("getInputChannel"));
        s.add(method("MessagesController", "getInputChannelO").named("getInputChannel"));
        s.add(method("MessagesController", "getInputChannelO2").named("getInputChannel"));
        s.add(method("MessagesController", "getInstance"));
        s.add(method("MessagesController", "isChatNoForwardsJ").named("isChatNoForwards"));
        s.add(method("MessagesController", "isChatNoForwardsO").named("isChatNoForwards"));
        s.add(method("MessagesController", "processNewDifferenceParams"));
        s.add(method("MessagesController", "removePromoDialog"));
        s.add(method("MessagesController", "storiesEnabled"));
        s.add(method("MessagesController", "storyEntitiesAllowed"));
        s.add(method("MessagesController", "storyEntitiesAllowedO").named("storyEntitiesAllowed"));
        s.add(method("MessagesStorage", "getDatabase"));
        s.add(method("MessagesStorage", "getInstance"));
        s.add(method("MessagesStorage", "getStorageQueue"));
        s.add(method("MessagesStorage", "markMessagesAsDeletedJAZZII").named("markMessagesAsDeleted"));
        s.add(method("MessagesStorage", "markMessagesAsDeletedJIZZ").named("markMessagesAsDeleted"));
        s.add(method("MessagesStorage", "putMessagesAZZZIIJ").named("putMessages"));
        s.add(method("MessagesStorage", "putMessagesAZZZIZIJ").named("putMessages"));
        s.add(method("MessagesStorage", "putMessagesOJIIZIJ").named("putMessages"));
        s.add(method("NotificationCenter", "postNotificationName"));
        s.add(method("NotificationsController", "removeDeletedMessagesFromNotifications"));
        s.add(method("SharedConfig", "isAppUpdateAvailable"));
        s.add(method("SharedConfig", "setNewAppVersionAvailable"));
        s.add(method("UserConfig", "getClientUserId"));
        s.add(method("UserConfig", "getCurrentUser"));
        s.add(method("UserConfig", "isPremium"));
        s.add(method("FastDateFormat", "formatD").named("format"));
        s.add(method("FastDateFormat", "formatJ").named("format"));
        s.add(method("FastDateFormat", "formatOSF").named("format"));
        s.add(method("TLRPC$Message", "TLdeserialize"));
        s.add(method("TLRPC$Message", "readAttachPath"));
        s.add(method("ChatActivity", "createView"));
        s.add(method("ChatActivity", "isSwipeBackEnabled"));
        s.add(method("ProfileActivity", "isSwipeBackEnabled"));
    }

    // ------------------------------------------------------------------ classes

    private static void classes(List<Symbol> s) {
        // Helpers: not used by call sites directly, but other fingerprints anchor on them.
        s.add(cls("org.telegram.ui.ActionBar.BaseFragment")
                .from(superclassOf("org.telegram.ui.ChatActivity")));
        s.add(cls("org.telegram.ui.ActionBar.Theme$ResourcesProvider")
                .from(fieldTypeOf("org.telegram.ui.ActionBar.BaseFragment", "resourceProvider"))
                .where(isInterface()));
        s.add(cls("org.telegram.ui.ActionBar.AlertDialog")
                .from(fieldTypesOf("org.telegram.ui.ActionBar.AlertDialog$Builder"))
                .where(extendsType("android.app.Dialog")));

        s.add(cls("androidx.collection.LongSparseArray")
                .from(fieldTypeOf("org.telegram.messenger.MessagesController", "dialogMessage"))
                .where(implementsType("java.lang.Cloneable"), hasField(false, "long[]")));
        s.add(cls("org.telegram.messenger.browser.Browser")
                .from(declaringStrings("com.duckduckgo.mobile.android", "vivaldi-browser")));
        s.add(cls("org.telegram.ui.ActionBar.Theme")
                .from(declaringStrings("autoNightScheduleByLocation", "autoNightLastSunCheckDay")));
        s.add(cls("org.telegram.ui.Stories.StoriesController")
                .from(returnTypeOf("org.telegram.messenger.MessagesController", "getStoriesController")));
    }

    // ----------------------------------------------------------------- settings

    private static void settings(List<Symbol> s) {
        String uitem = "org.telegram.ui.Components.UItem";
        // UItem's API is a large set of static factories (asHeader, asCheck, asButton, ...);
        // nothing else a settings screen touches looks like that.
        s.add(cls(uitem)
                .from(paramTypesOf("org.telegram.ui.SettingsActivity"))
                .where(staticFactories(15)));
        s.add(cls("org.telegram.ui.SettingsActivity$SettingCell$Factory")
                .from(declaringMethod(uitem, "int", "int", "int", "int", "java.lang.CharSequence", "java.lang.CharSequence", "java.lang.CharSequence")));
        s.add(cls("org.telegram.ui.Components.UItem$UItemFactory")
                .from(superclassOf("org.telegram.ui.SettingsActivity$SettingCell$Factory")));
        s.add(method("SettingsActivity$SettingCell$Factory", "createView"));
        s.add(method("SettingsActivity$SettingCell$Factory", "bindView"));
        s.add(cls("org.telegram.ui.SettingsActivity$SettingCell")
                .from(instantiatedBySymbol("SettingsActivity$SettingCell$Factory#createView"))
                .where(extendsType("android.widget.LinearLayout"), hasField(false, "android.widget.ImageView")));
        s.add(cls("org.telegram.ui.Components.UniversalAdapter")
                .from(paramTypesOf("org.telegram.ui.SettingsActivity$SettingCell$Factory"))
                .where(hasMethod(false, uitem, "int"),
                        isNot(inherits("androidx.recyclerview.widget.RecyclerView"))));

        s.add(method("SettingsActivity", "fillItems")
                .sig("void", "java.util.ArrayList", "org.telegram.ui.Components.UniversalAdapter").reads(0)
                .where(calls(uitem, uitem, "int")));
        s.add(method("SettingsActivity", "onClick")
                .sig("void", uitem, "android.view.View", "int", "float", "float").reads(0));
        s.add(method("SettingsActivity$SettingCell", "set")
                .sig("void", "int", "int", "int", "java.lang.CharSequence", "java.lang.CharSequence", "java.lang.CharSequence"));
        // Factory.of assigns item.id, iconResId, text, subtext, textValue in that order.
        String of7 = "SettingsActivity$SettingCell$Factory#ofIIIICCC";
        s.add(field("UItem", "id").type("int").writtenBy(of7, 0));
        s.add(field("UItem", "text").type("java.lang.CharSequence").writtenBy(of7, 0));
        s.add(field("UItem", "subtext").type("java.lang.CharSequence").writtenBy(of7, 1));
        s.add(field("SettingsActivity$SettingCell", "iconView")
                .type("android.widget.ImageView").onlyOneOfType());
        s.add(method("SettingsActivity$SettingCell$Factory", "ofIIIIC").named("of").isStatic(true)
                .sig(uitem, "int", "int", "int", "int", "java.lang.CharSequence"));
        s.add(method("SettingsActivity$SettingCell$Factory", "ofIIIICC").named("of").isStatic(true)
                .sig(uitem, "int", "int", "int", "int", "java.lang.CharSequence", "java.lang.CharSequence"));
        s.add(method("SettingsActivity$SettingCell$Factory", "ofIIIICCC").named("of").isStatic(true)
                .sig(uitem, "int", "int", "int", "int", "java.lang.CharSequence", "java.lang.CharSequence", "java.lang.CharSequence"));
    }

    // ---------------------------------------------------------------- action bar

    private static void actionBar(List<Symbol> s) {
        String ami = "org.telegram.ui.ActionBar.ActionBarMenuItem";
        String sub = "org.telegram.ui.ActionBar.ActionBarMenuSubItem";
        String item = "org.telegram.ui.ActionBar.ActionBarMenuItem$Item";
        String rp = "org.telegram.ui.ActionBar.Theme$ResourcesProvider";
        String drawable = "android.graphics.drawable.Drawable";

        s.add(cls("org.telegram.ui.ActionBar.ActionBar")
                .from(fieldTypeOf("org.telegram.ui.ActionBar.BaseFragment", "actionBar"))
                .where(extendsType("android.widget.FrameLayout")));
        // A tiny listener class every fragment subclasses: onItemClick(int) and canOpenMenu().
        s.add(cls("org.telegram.ui.ActionBar.ActionBar$ActionBarMenuOnItemClick")
                .from(fieldTypesOf("org.telegram.ui.ActionBar.ActionBar"))
                .where(extendsType("java.lang.Object"), hasMethod(false, "void", "int"),
                        hasMethod(false, "boolean"), methodCountAtMost(4)));
        s.add(method("ActionBar", "setActionBarMenuOnItemClick")
                .sig("void", "org.telegram.ui.ActionBar.ActionBar$ActionBarMenuOnItemClick"));

        s.add(cls(ami)
                .from(subclassesOf("android.widget.FrameLayout"))
                .where(hasMethod(false, "android.widget.TextView", "int", "java.lang.CharSequence"),
                        hasMethod(false, "void", "android.view.View", "int", "int")));
        s.add(cls(sub)
                .from(returnTypeWhere(ami, "int", "int", "java.lang.CharSequence"))
                .where(extendsType("android.widget.FrameLayout")));
        s.add(cls(item)
                .from(returnTypeWhere(ami, "int", "int", "java.lang.CharSequence"))
                .where(extendsType("java.lang.Object")));

        s.add(method("ActionBarMenuItem", "addSubItemIC").named("addSubItem").sig("android.widget.TextView", "int", "java.lang.CharSequence"));
        s.add(method("ActionBarMenuItem", "addSubItemIIC").named("addSubItem").sig(sub, "int", "int", "java.lang.CharSequence"));
        s.add(method("ActionBarMenuItem", "addSubItemIICO").named("addSubItem").sig(sub, "int", "int", "java.lang.CharSequence", rp));
        s.add(method("ActionBarMenuItem", "addSubItemIICZ").named("addSubItem").sig(sub, "int", "int", "java.lang.CharSequence", "boolean"));
        s.add(method("ActionBarMenuItem", "addSubItemIIDCZZ").named("addSubItem")
                .sig(sub, "int", "int", drawable, "java.lang.CharSequence", "boolean", "boolean"));
        s.add(method("ActionBarMenuItem", "addSubItemIIDCZZO").named("addSubItem")
                .sig(sub, "int", "int", drawable, "java.lang.CharSequence", "boolean", "boolean", rp));
        s.add(method("ActionBarMenuItem", "addSubItemIV").named("addSubItem").sig("android.view.View", "int", "android.view.View"));
        s.add(method("ActionBarMenuItem", "addSubItemIVII").named("addSubItem").sig("void", "int", "android.view.View", "int", "int"));
        s.add(method("ActionBarMenuItem", "addSubItemVII").named("addSubItem").sig("void", "android.view.View", "int", "int"));
        s.add(method("ActionBarMenuItem", "lazilyAddSubItemIDC").named("lazilyAddSubItem").sig(item, "int", drawable, "java.lang.CharSequence"));
        s.add(method("ActionBarMenuItem", "lazilyAddSubItemIIC").named("lazilyAddSubItem").sig(item, "int", "int", "java.lang.CharSequence"));
        s.add(method("ActionBarMenuItem", "lazilyAddSubItemIIDCZZ").named("lazilyAddSubItem")
                .sig(item, "int", "int", drawable, "java.lang.CharSequence", "boolean", "boolean"));
    }

    // -------------------------------------------------------------- alert dialog

    private static void alertDialog(List<Symbol> s) {
        String dialog = "org.telegram.ui.ActionBar.AlertDialog";
        String builder = "org.telegram.ui.ActionBar.AlertDialog$Builder";
        String listener = "org.telegram.ui.ActionBar.AlertDialog$OnButtonClickListener";

        // The dialog's own setTitle overrides android.app.Dialog's, so it keeps its name - and the
        // field it writes tells the builder's title setter apart from its message setter.
        s.add(method("AlertDialog", "setTitle").sig("void", "java.lang.CharSequence"));
        s.add(cls(listener)
                .from(paramTypeWhere(builder, "void", "java.lang.CharSequence", null),
                        paramTypeWhere(builder, builder, "java.lang.CharSequence", null))
                .where(isInterface(), hasMethod(false, "void", dialog, "int")));
        s.add(method("AlertDialog$OnButtonClickListener", "onClick").sig("void", dialog, "int"));
        s.add(method("AlertDialog", "setButton").sig("void", "int", "java.lang.CharSequence", listener)
                .where(switchKey(-3), switchKey(-2), switchKey(-1)));

        s.add(method("AlertDialog$Builder", "setTitle").sig(builder, "java.lang.CharSequence").voidable()
                .where(writesFieldWrittenBy("AlertDialog#setTitle", "java.lang.CharSequence")));
        s.add(method("AlertDialog$Builder", "setMessage").sig(builder, "java.lang.CharSequence").voidable()
                .where(writesFieldOfType(dialog, "java.lang.CharSequence"),
                        not(writesFieldWrittenBy("AlertDialog#setTitle", "java.lang.CharSequence"))));
        s.add(method("AlertDialog$Builder", "setViewV").named("setView").sig(builder, "android.view.View").voidable());
        s.add(method("AlertDialog$Builder", "setViewVI").named("setView").sig(builder, "android.view.View", "int").voidable());
        s.add(method("AlertDialog$Builder", "create").sig(dialog)
                .where(not(callsAnyNamed("show"))));
        s.add(method("AlertDialog$Builder", "show").sig(dialog).voidable()
                .where(callsAnyNamed("show")));
        s.add(method("AlertDialog$Builder", "getDismissRunnable").sig("java.lang.Runnable"));
        // Positive is pinned by meaning: its listener is the one the dialog invokes with
        // BUTTON_POSITIVE (-1). Negative and neutral cannot be told apart that way - Telegram's
        // neutral handler also passes BUTTON_NEGATIVE - so they are resolved only where a build
        // leaves exactly one candidate, and otherwise refused rather than risk a swapped action.
        s.add(method("AlertDialog$Builder", "setPositiveButton").sig(builder, "java.lang.CharSequence", listener)
                .voidable().narrowedStrings()
                .where(writesListenerInvokedWith(dialog, listener, -1)));
        s.add(method("AlertDialog$Builder", "setNegativeButton").sig(builder, "java.lang.CharSequence", listener)
                .voidable().narrowedStrings()
                .where(not(writesListenerInvokedWith(dialog, listener, -1))));
        s.add(method("AlertDialog$Builder", "setNeutralButton").sig(builder, "java.lang.CharSequence", listener)
                .voidable().narrowedStrings()
                .where(not(writesListenerInvokedWith(dialog, listener, -1))));
    }

    // -------------------------------------------------------------------- cells

    private static void cells(List<Symbol> s) {
        String rp = "org.telegram.ui.ActionBar.Theme$ResourcesProvider";
        s.add(cls("org.telegram.ui.Cells.HeaderCell")
                .from(subclassesOf("android.widget.FrameLayout"))
                .where(hasMethod(false, "void", "java.lang.CharSequence"), hasMethod(false, "void", "java.lang.CharSequence", "boolean"),
                        hasConstructor("android.content.Context", "int", "int", "int", "int", "boolean", "boolean", rp)));
        s.add(method("HeaderCell", "setTextC").named("setText").sig("void", "java.lang.CharSequence"));
        s.add(method("HeaderCell", "setTextCZ").named("setText").sig("void", "java.lang.CharSequence", "boolean"));

        s.add(cls("org.telegram.ui.Cells.TextCheckCell")
                .from(subclassesOf("android.widget.FrameLayout"))
                .where(hasConstructor("android.content.Context"),
                        hasMethod(false, "void", "java.lang.CharSequence", "java.lang.CharSequence", "boolean", "boolean", "boolean")));
        s.add(method("TextCheckCell", "isChecked").sig("boolean"));
        s.add(method("TextCheckCell", "setChecked").sig("void", "boolean")
                .where(calls("org.telegram.ui.Components.Switch", "void", "boolean", "boolean")));
        s.add(method("TextCheckCell", "setTextAndCheck").sig("void", "java.lang.CharSequence", "boolean", "boolean"));
        // The setters start with textView.setText(...), so the first TextView they read is it.
        s.add(field("TextCheckCell", "textView").type("android.widget.TextView").readBy("TextCheckCell#setTextAndCheck", 0));
        s.add(method("TextCheckCell", "setTextAndValueAndCheck").sig("void", "java.lang.CharSequence", "java.lang.CharSequence", "boolean", "boolean", "boolean"));

        s.add(cls("org.telegram.ui.Cells.TextSettingsCell")
                .from(subclassesOf("android.widget.FrameLayout"))
                .where(hasConstructor("android.content.Context"), hasConstructor("android.content.Context", "int", rp),
                        hasMethod(false, "void", "java.lang.CharSequence", "boolean"),
                        hasMethod(false, "void", "java.lang.CharSequence", "java.lang.CharSequence", "boolean", "boolean")));
        s.add(method("TextSettingsCell", "setText").sig("void", "java.lang.CharSequence", "boolean"));
        s.add(field("TextSettingsCell", "textView").type("android.widget.TextView").readBy("TextSettingsCell#setText", 0));
        s.add(method("TextSettingsCell", "setTextAndValueCCZ").named("setTextAndValue").sig("void", "java.lang.CharSequence", "java.lang.CharSequence", "boolean"));
        s.add(method("TextSettingsCell", "setTextAndValueCCZZ").named("setTextAndValue").sig("void", "java.lang.CharSequence", "java.lang.CharSequence", "boolean", "boolean"));

        s.add(cls("org.telegram.ui.Cells.ShadowSectionCell")
                .from(subclassesOf("android.view.View"))
                .where(hasConstructor("android.content.Context", "int", "int", rp)));
    }

    // --------------------------------------------------------------------- chat

    private static void chat(List<Symbol> s) {
        String cell = "org.telegram.ui.Cells.ChatMessageCell";
        String mo = "org.telegram.messenger.MessageObject";
        s.add(cls("org.telegram.ui.Cells.ChatMessageCell$MessageAccessibilityNodeProvider")
                .from(declaringStrings("AccActionEnterSelectionMode", "AccDescrMsgNotPlayed"))
                .where(extendsType("android.view.accessibility.AccessibilityNodeProvider")));
        s.add(cls(cell)
                .from(fieldTypesOf("org.telegram.ui.Cells.ChatMessageCell$MessageAccessibilityNodeProvider"))
                .where(implementsType("org.telegram.messenger.ImageReceiver$ImageReceiverDelegate")));
        s.add(method("ChatMessageCell", "getMessageObject").sig(mo).where(callsNothing()));
        s.add(method("ChatMessageCell", "measureTime").sig("void", mo));

        s.add(method("ChatActivity", "createPinnedMessageView").sig("void"));
        s.add(method("ChatActivity", "hasSelectedNoforwardsMessage").sig("boolean"));
        s.add(method("ChatActivity", "processSelectedOption").sig("void", "int"));
        s.add(method("ChatActivity", "scrollToMessageIdIIZIZI").named("scrollToMessageId")
                .sig("void", "int", "int", "boolean", "int", "boolean", "int"));
        s.add(method("ChatActivity", "scrollToMessageIdIIZIZIIR").named("scrollToMessageId")
                .sig("void", "int", "int", "boolean", "int", "boolean", "int", "java.lang.Integer", "java.lang.Runnable"));
        s.add(method("ChatActivity", "scrollToMessageIdIIZIZIIABR").named("scrollToMessageId")
                .sig("void", "int", "int", "boolean", "int", "boolean", "int", "java.lang.Integer", "byte[]", "java.lang.Runnable"));
        s.add(method("ChatActivity", "sendSecretMediaDelete").sig("java.lang.Runnable", mo));
        s.add(method("ChatActivity", "sendSecretMessageRead").sig("java.lang.Runnable", mo, "boolean"));
        s.add(method("ChatActivity", "updatePinnedMessageViewZ").named("updatePinnedMessageView").sig("void", "boolean"));
        s.add(method("ChatActivity", "updatePinnedMessageViewZI").named("updatePinnedMessageView").sig("void", "boolean", "int"));

        s.add(cls("org.telegram.ui.ChatActivity$ChatMessageCellDelegate")
                .from(declaringMethod("void", cell, "float", "float", "boolean"))
                .where(hasField(false, "org.telegram.ui.ChatActivity")));
        s.add(method("ChatActivity$ChatMessageCellDelegate", "didPressImage").sig("void", cell, "float", "float", "boolean"));
    }

    // ------------------------------------------------------------- photo viewer

    private static void photoViewer(List<Symbol> s) {
        String pv = "org.telegram.ui.PhotoViewer";
        String provider = "org.telegram.ui.PhotoViewer$PhotoViewerProvider";
        String place = "org.telegram.ui.PhotoViewer$PlaceProviderObject";
        String mo = "org.telegram.messenger.MessageObject";
        String loc = "org.telegram.tgnet.TLRPC$FileLocation";
        String imgLoc = "org.telegram.messenger.ImageLocation";
        String chat = "org.telegram.ui.ChatActivity";
        String blocks = "org.telegram.ui.PhotoViewer$PageBlocksAdapter";
        String fragment = "org.telegram.ui.ActionBar.BaseFragment";
        String rp = "org.telegram.ui.ActionBar.Theme$ResourcesProvider";

        s.add(cls(provider)
                .from(declaringMethod(null, mo, loc, "int", "boolean", "boolean"))
                .where(isInterface()));
        s.add(cls(place).from(returnTypeWhere(provider, mo, loc, "int", "boolean", "boolean")));
        s.add(method("PhotoViewer$PhotoViewerProvider", "getPlaceForPhoto").sig(place, mo, loc, "int", "boolean", "boolean"));
        s.add(cls(blocks)
                .from(paramTypeWhere(pv, "boolean", "int", null, provider))
                .where(isInterface()));

        s.add(method("PhotoViewer", "getInstance").isStatic(true).sig(pv));
        s.add(method("PhotoViewer", "openPhotoAIJJJO").named("openPhoto")
                .sig("boolean", "java.util.ArrayList", "int", "long", "long", "long", provider));
        s.add(method("PhotoViewer", "openPhotoAIO").named("openPhoto").sig("boolean", "java.util.ArrayList", "int", provider));
        s.add(method("PhotoViewer", "openPhotoIOO").named("openPhoto").sig("boolean", "int", blocks, provider));
        s.add(method("PhotoViewer", "openPhotoOIOJJJO").named("openPhoto")
                .sig("boolean", mo, "int", chat, "long", "long", "long", provider));
        s.add(method("PhotoViewer", "openPhotoOJJJOZ").named("openPhoto")
                .sig("boolean", mo, "long", "long", "long", provider, "boolean"));
        s.add(method("PhotoViewer", "openPhotoOO").named("openPhoto").sig("boolean", loc, provider));
        s.add(method("PhotoViewer", "openPhotoOOJJJO").named("openPhoto")
                .sig("boolean", mo, chat, "long", "long", "long", provider));
        s.add(method("PhotoViewer", "openPhotoOOO").named("openPhoto").sig("boolean", loc, imgLoc, provider));
        s.add(method("PhotoViewer", "openPhotoOOOOAAAIOOJJJZOI").named("openPhoto")
                .sig("boolean", mo, loc, imgLoc, imgLoc, "java.util.ArrayList", "java.util.ArrayList", "java.util.ArrayList",
                        "int", provider, chat, "long", "long", "long", "boolean", blocks, "java.lang.Integer"));
        s.add(method("PhotoViewer", "setIsAboutToSwitchToIndexIZZ").named("setIsAboutToSwitchToIndex")
                .sig("void", "int", "boolean", "boolean"));
        s.add(method("PhotoViewer", "setIsAboutToSwitchToIndexIZZZ").named("setIsAboutToSwitchToIndex")
                .sig("void", "int", "boolean", "boolean", "boolean"));
        s.add(method("PhotoViewer", "setParentActivityA").named("setParentActivity").sig("void", "android.app.Activity"));
        s.add(method("PhotoViewer", "setParentActivityAO").named("setParentActivity").sig("void", "android.app.Activity", rp));
        s.add(method("PhotoViewer", "setParentActivityAOO").named("setParentActivity")
                .sig("void", "android.app.Activity", fragment, rp));
        s.add(method("PhotoViewer", "setParentActivityO").named("setParentActivity").sig("void", fragment));
        s.add(method("PhotoViewer", "setParentActivityOO").named("setParentActivity").sig("void", fragment, rp));

        s.add(field("LaunchActivity", "frameLayout").type("android.widget.FrameLayout").onlyOneOfType());

        s.add(method("SecretMediaViewer", "closePhoto").sig("boolean", "boolean", "boolean"));
        s.add(method("SecretMediaViewer", "openMedia")
                .sig("void", mo, provider, "java.lang.Runnable", "java.lang.Runnable"));
    }

    // ------------------------------------------------------------------ stories

    private static void stories(List<Symbol> s) {
        String peerStories = "org.telegram.tgnet.tl.TL_stories$PeerStories";
        s.add(method("StoriesController", "hasStoriesJ").named("hasStories").sig("boolean", "long")
                .where(callsSibling(peerStories, "long"), callsSibling("boolean", "long"),
                        callsNamed("java.util.ArrayList", "isEmpty")));
        // hasStories(): "stories in the dialog list, or the user's own". R8 may inline it away
        // entirely, and a lookalike (hasOnlySelfStories) also checks the list, so the fingerprint
        // excludes anything that inspects individual peers.
        s.add(method("StoriesController", "hasStories").sig("boolean")
                .where(callsSibling("boolean"), callsNamed("java.util.ArrayList", "size"),
                        not(touchesField(peerStories, "peer"))));
    }

    // --------------------------------------------------------------------- misc

    private static void misc(List<Symbol> s) {
        String browser = "org.telegram.messenger.browser.Browser";
        String progress = "org.telegram.messenger.browser.Browser$Progress";
        String ctx = "android.content.Context";
        String uri = "android.net.Uri";

        s.add(method("LongSparseArray", "getJ").named("get").sig("java.lang.Object", "long"));
        s.add(method("LongSparseArray", "getJO").named("get").sig("java.lang.Object", "long", "java.lang.Object"));

        s.add(cls(progress).from(paramTypeWhere(browser, "void", ctx, uri, "boolean", "boolean", "boolean",
                null, "java.lang.String", "boolean", "boolean", "boolean")));
        // openUrlInSystemBrowser has the same signature, but it goes straight to the ten-parameter
        // overload; openUrl(Context, String) never does, whether or not R8 inlines the step between.
        s.add(method("Browser", "openUrlCS").named("openUrl").isStatic(true).sig("void", ctx, "java.lang.String")
                .where(not(callsSibling("void", ctx, uri, "boolean", "boolean", "boolean", progress,
                        "java.lang.String", "boolean", "boolean", "boolean"))));
        s.add(method("Browser", "openUrlCSZ").named("openUrl").isStatic(true).sig("void", ctx, "java.lang.String", "boolean"));
        s.add(method("Browser", "openUrlCSZZ").named("openUrl").isStatic(true)
                .sig("void", ctx, "java.lang.String", "boolean", "boolean"));
        s.add(method("Browser", "openUrlCU").named("openUrl").isStatic(true).sig("void", ctx, uri));
        s.add(method("Browser", "openUrlCUZ").named("openUrl").isStatic(true).sig("void", ctx, uri, "boolean"));
        s.add(method("Browser", "openUrlCUZZ").named("openUrl").isStatic(true).sig("void", ctx, uri, "boolean", "boolean"));
        s.add(method("Browser", "openUrlCUZZO").named("openUrl").isStatic(true)
                .sig("void", ctx, uri, "boolean", "boolean", progress));
        s.add(method("Browser", "openUrlCUZZZOSZZZ").named("openUrl").isStatic(true)
                .sig("void", ctx, uri, "boolean", "boolean", "boolean", progress, "java.lang.String", "boolean", "boolean", "boolean"));

        // "return currentTheme.isDark()": one call, returned as is. isCurrentThemeDay is the same
        // call negated (xor-int/lit8 ..., 1), or two calls where getActiveTheme() is not inlined.
        s.add(method("Theme", "isCurrentThemeDark").isStatic(true).sig("boolean")
                .where(callCount(1), not(usesOpcode(0xdf))));
        s.add(field("Theme", "chat_timePaint").isStatic(true).type("android.text.TextPaint")
                .accessedBy("ChatMessageCell#measureTime"));
    }
}
