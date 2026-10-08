# Changelog

## 1.1.3 — TeleVip's own icon in Settings

- **TeleVip's entry in Settings shows TeleVip's app icon** — the paper plane on its yellow tile —
  instead of the ghost.
- Also from 1.1.2: Cherrygram 11.3.0, which is built on Telegram 12.10.6, had the same problem as
  Nekogram 12.10.6 and works again too.

## 1.1.2 — Nekogram 12.10.6 works again

- **Fixed: on Nekogram 12.10.6 TeleVip did almost nothing** — no TeleVip entry in Settings, ads
  still shown, and most other features off. Since 1.0.8, TeleVip had mistaken that build for one
  covered by an old name table made from Nekogram 12.8.1, so it looked for classes that do not
  exist in 12.10.6. TeleVip now always reads the names from the installed app, on every client
  and build, which is what the weekly client checks test. Cherrygram's table was mislabelled the
  same way.
- **TeleVip's entry is back at the top of Settings, with its ghost icon.** On builds that rename
  the row's icon view beyond recognition, TeleVip now finds the icon by itself.

## 1.1.1 — ads blocked at every step

- **Block Ads now has three barriers instead of one**, so a channel's sponsored post cannot get
  through if one of them misses:
  1. the request that fetches ads is dropped before it is sent, as before;
  2. if an ad request is sent anyway, the server's answer is read as "no ads";
  3. a chat that asks for its sponsored posts is told there are none, whatever was fetched or
     cached.

  This covers sponsored posts in channels and bot chats, the video player's ads and the "Ad"
  results in chat search. The pinned proxy sponsor channel stays removed as before.
- **Block Ads is on by default.** If you never touched the switch, it is now on; if you turned
  it off, it stays off.

## 1.1.0 — new package name, for the Xposed Modules Repo

- **TeleVip's package name is now `io.github.re_televip.televip`** (it was `com.my.televip`).
  The Xposed Modules Repo (modules.lsposed.org) only lists modules whose package name their
  author owns, and this one belongs to the Re-TeleVIP organisation. Android treats it as a new
  app, so once:
  1. Install this version, then uninstall the old *TeleVip* (`com.my.televip`).
  2. In LSPosed / Vector, enable TeleVip again and tick your Telegram clients.
  3. Force stop the clients and reopen them.

  Your TeleVip settings are kept: they are stored inside each Telegram client, not in TeleVip.
- Releases are also published to TeleVip's page in the Xposed Modules Repo, so LSPosed's module
  repository can offer updates.

## 1.0.8 — Show deleted messages on Nekogram 12.10.5+, Android 17 SDK

- **Show deleted messages works on Nekogram 12.10.5 and 12.10.6.** 1.0.7 picked the wrong event
  there: Nekogram numbers its events one higher than Telegram does. It also intercepted a
  profile-screen event instead. The event is now read from the code that posts it, never
  assumed. That is correct on all 37 builds checked, including Telegram FOSS 10.14.3, whose
  events are numbered differently again.
- Built against the Android 17 SDK (API 37). The weekly toolchain update now keeps the SDK
  current too, alongside AGP, Gradle and the libxposed API, and pushes only if the build and
  tests pass.
- Client watch: the F-Droid listing no longer fails on clients with a long history (Forkgram).
- Clean-up: unused resolver code removed, compiler warnings fixed, and deprecated Android calls
  replaced (the client's version code; the audio player's stream type).
- The builds TeleVip was last verified against are updated: Telegram Web 12.10.6, Nekogram
  12.10.6, Cherrygram 11.3.0, Nagram 1248, NagramX 1258, Forkgram 12.10.4 and Forkgram Classic
  12.10.8.

## 1.0.7 — Nekogram 12.10.5+ and the last five releases of every client

- **Nekogram 12.10.5 and 12.10.6 are supported.** Their build renames, reorders, merges and
  inlines much more than before, which broke the settings entry, Ghost Mode, Block Ads,
  saving edits, the photo viewer and more. Every feature now finds its code there again.
- **Mercurygram** is supported.
- **Every feature was checked on the last five releases of each client** that can be
  downloaded, 36 builds in all:
  - Nekogram 12.9.2–12.10.6, Cherrygram 10.9–11.3, Nagram and NagramX;
  - Forkgram 12.9.5–12.10.4, Forkgram Classic 12.10.3–12.10.8 and Mercurygram 12.9.0–12.10.5;
  - Telegram 12.10.5 and 12.10.6.
- **The weekly Client watch now checks the last five releases of each of these clients**, not
  just the newest one. Forkgram, Forkgram Classic and Mercurygram are fetched from F-Droid.
- *Ghost Mode*: reading a channel without being seen marked it read with the wrong id on Nagram.
  The account's own channel lookup is used now.
- *Fix TL error*: the parse error is recognised by its message, where the build has no name
  for it.
- *Disable number rounding*, *Save edits history* and *Hide app updates* keep working where the
  build reorders those methods' parameters or drops their return value.
- TeleVip's dialogs use the system dialog everywhere. The Telegram-styled one never actually
  loaded, because of a misspelt class name.

## 1.0.6 — ad blocking that works

- **Hide Proxy Sponsor is now Block Ads**, and your earlier setting carries over. It blocks the
  requests that fetch ads before they are sent, so nothing is downloaded and nothing is shown:
  - sponsored posts in channels and bot chats, and the ads in the video player;
  - the "Ad" results in chat search;
  - the proxy sponsor / announcement channel pinned to the top of the chat list.
- **The sponsor channel no longer comes back.** It used to be removed only after the client had
  already asked the server for it, so the reply put it straight back. The check is now skipped,
  and a channel saved from before is removed along with the settings that restored it on start.
- The ad requests are found by their protocol ids on renamed builds. Every Block Ads hook point
  resolves on Telegram 12.10.5, Nekogram 12.10.3 and Cherrygram 12.10.1, and the weekly Client
  watch checks them.

## 1.0.5 — every feature reaches its code

Every feature's lookups were replayed against Telegram 12.10.5, Nekogram 12.10.3 and
Cherrygram 12.10.1, and the ones that pointed at nothing were fixed:

- **TeleVip's entry in Settings is back on official Telegram**, which made the methods it hooks
  static and dropped their unused parameters.
- **TeleVip's settings page no longer fails on Telegram and Cherrygram**: the switch rows it is
  built from are found there again.
- **Nicegram, Nagram, Turrit, Momogram and NekoX**: the parameter lists these forks changed are
  honoured again (1.0.4 ignored them, which silently disabled some hooks there).
- *Jump to message* works where Telegram inlined the overload it called; *Hide seen* finds
  the channel lookup on renamed builds.
- Found on more builds: the photo viewer, a viewer's image, the local file path of media and the
  settings adapter.
- Fingerprints hold on Nagram and NagramX too, which add their own menu items and look-alike cells.
- Weekly automation: Dependabot updates are merged once they build, and the Monday Client watch
  releases whatever changed if every client still checks out.
- Removed the fourteen empty per-client name tables.

## 1.0.4 — official Telegram and Cherrygram

- **Official Telegram, Forkgram and the other unobfuscated forks now get the same runtime
  resolution as Nekogram.** Telegram renames its UI classes (ChatActivity, SettingsActivity,
  PhotoViewer's menus, ...) in every release, and TeleVip looked them up by their source names
  there, so the features that hook them stayed off. Every client is now resolved from its own
  APK, keeping a hand-made name table only on the exact build it was made for. Every feature hook
  point resolves on Telegram 12.10.5.
- **Cherrygram 12.10.1 is supported again.** It renames even the core `org.telegram.messenger`
  and TL classes and nearly all of their methods. These are now found by strings only they load,
  TL constructor ids, and the names that native code forces every build to keep. Every feature
  hook point resolves on it.
- Fingerprints follow more of R8's rewrites: methods turned static, narrowed field and return
  types, and fields that are no longer narrowed.
- The weekly Client watch also checks that no fingerprint ever picks a different symbol than the
  real name, wherever a build keeps it.

## 1.0.3 — release safeguard

- A release is never published without the signed APK: if the signing secrets are missing or
  stop working, the release fails instead of shipping only the debug build.
- Installs over 1.0.2 directly (same signing key).

## 1.0.2 — signed release APK

- **The release APK is now signed**, so it installs directly. Builds read the key from the
  `KEYSTORE_*` repository secrets (or a local `keystore.properties`); nothing secret is kept in
  the repository. Because it is signed with a different key than the debug APK, uninstall a debug
  build once before installing the release one.
- Hooking several methods at once no longer skips the rest when one of them is missing.
- Mapping caches of earlier client builds are deleted when a new one is written.
- Removed unused code, resources and template files; Android Gradle Plugin 9.4.1.

## 1.0.1 — works on renamed builds, Ghost Mode dialog fixed

Version numbering restarts at 1.0.1 (versionCode 341, so it still installs over 3.7.0).

### Fixed

- **Tapping *Join* in the Ghost Mode dialog crashed Nekogram, and *Dismiss* left the Ghost Mode
  button dead.** On Nekogram 12.10.3 `LaunchActivity.frameLayout` resolved to the tablet-only
  `shadowTablet`, which is null on phones: showing the page failed and hiding it threw from
  inside the dialog's click handler. The field is now found as the view `onCreate` hands to
  `setContentView`, the wrapper checks it really is the window's content view (and falls back to
  that view when it is not), `show`/`hide` can no longer throw, and dialog button actions run
  inside a guard so a failure is logged instead of taking the client down.
- The dialog button listener no longer depends on the listener method's name, and answers
  `equals`/`hashCode` properly instead of returning null.

### Restored on obfuscated (R8-renamed) builds

Every feature below was inactive on Nekogram 12.10.3 because its hook point was renamed,
inlined, reordered or narrowed. Each is now found by what the code does, checked against the
12.10.3 bytecode in `NekogramApkTest`:

- **Hide pinned messages** — `updatePinnedMessageView` (R8 swapped its parameters to
  `(int, boolean)`; the real order is now published to the hook), `createPinnedMessageView` and
  the `pinnedMessageView` field.
- **Remove content-saving restrictions** — `hasSelectedNoforwardsMessage`.
- **Save edits history** — `fillMessageMenu`, `processSelectedOption`, `selectedObject`.
- **Show deleted messages** — `measureTime` and the time-label fields it writes
  (`currentTimeString`, `timeWidth`, `timeTextWidth`, `Theme.chat_timePaint`).
- **Secret media save** — the chat's `didPressImage` (R8 dropped its unused flag).
- **Save protected stories** — `StoryItemHolder.allowScreenshots`.
- **Disable stories** — `hasStories(long)` is hooked on every client, since `hasStories()` is
  inlined away in some builds.
- **Always save media** — `setIsAboutToSwitchToIndex` and `galleryButton`; `openPhoto` and
  `setParentActivity` fall back to the one overload R8 keeps.
- **Chat and profile menu entries** (*To the beginning*, *To the message*, *Approximate creation
  date*) — `ActionBarMenuItem` and its `addSubItem`/`lazilyAddSubItem`, `headerItem`,
  `otherItem`, `createActionBarMenu`. Clicks are now routed from each screen's live menu listener
  instead of by class name, which renamed builds do not keep.
- **Show user ID / hide online status on profiles** — `updateProfileData`, `userId`, `chatId`,
  `nameTextView`, `onlineTextView`, and `SimpleTextView`'s text methods (found by shape when
  renamed).
- Menu and settings icons fall back to the resource table when `R$drawable` has been stripped.

The resolver gained the matching tools: parameter-order tolerance, "called by" and
"stores a new anonymous subclass" facts, subtype-narrowed fields, and class lookup by one
distinctive method. The fingerprint version is bumped, so a cached mapping from the previous
module version is discarded and rebuilt on first start.

### Hook resilience

- **Signature drift no longer kills a hook.** When the exact signature is gone the method is
  re-matched by name, but only when the match is unambiguous (same arity when parameter types are
  given); anything else is left unhooked rather than attached to the wrong overload.
- **A parameter type that no longer resolves is a wildcard** instead of failing the whole hook.
- **`HookHealth` logs one line at startup**: how many hooks resolved, drifted or are missing.

## 3.7.0 — Xposed API 102, Vector 2.2, Zygisk Next, Nekogram X

### Modern Xposed API (libxposed 102)

TeleVip is now a **libxposed API 102 module**, loaded through a single modern entry point:

| Entry | Descriptor | Class |
|---|---|---|
| Modern (API 102) | `META-INF/xposed/java_init.list` + `module.prop` | `com.my.televip.xposed.TeleVipModule` |

It funnels into `com.my.televip.xposed.ModuleEntry#attach`. The legacy API 93 entry
(`assets/xposed_init` → `MainHook`) was dropped before release: leaving it in place would make
LSPosed 1.9.x and EdXposed advertise the module and then fail at load time, because there is no
longer a `de.robv` entry class for them to find.

New compatibility layer under `com.my.televip.xposed`:

- **`XBridge`** — the only seam to the framework: hooking, logging, module APK path, deoptimize,
  framework name/version. Everything else in the module goes through it.
- **`ModernBackend`** — adapts to `XposedInterface.hook(Executable).intercept(Hooker)`. API 102
  replaced the before/after pair with an OkHttp-style interceptor chain, so the adapter rebuilds
  classic semantics on top of `Chain`: a result set in `beforeMethod` short-circuits the chain
  (original never runs), otherwise `chain.proceed(args)` runs with the mutated argument array and
  its outcome is handed to `afterMethod`, which may still override it.

`AbstractMethodHook` no longer extends `XC_MethodHook`; it is a plain class with a nested
`MethodHookParam` that keeps the same surface (`args`, `thisObject`, `getResult`, `setResult`,
`setThrowable`), so all existing feature code compiles unchanged. `XC_MethodReplacement` is
replaced by `com.my.televip.base.MethodReplacement`.

### No runtime dependency on `de.robv.*` for reflection

A module loaded through the modern API gets **no legacy Xposed classes at all**, so every
`XposedHelpers` call would have thrown `NoClassDefFoundError`. All 58 call sites now use
`com.my.televip.reflect.XReflect`, a self-contained reimplementation with the same signatures,
the same best-match argument resolution (exact match → boxing → primitive widening →
`null`-compatible, most specific wins) and the same unchecked `NoSuchMethodError` /
`NoSuchFieldError` behaviour. Field and class lookups are cached.

### Vector 2.2 / Zygisk Next hardening

- The module APK path no longer depends on `initZygote()`, which the modern API does not have and
  which is unreliable for app-scoped modules under Zygisk Next. `XBridge#modulePath` takes it from
  the backend (`getModuleApplicationInfo().sourceDir` on modern, `StartupParam` on legacy) and
  falls back to deriving it from our own class loader.
- Language packs moved from `assets/lang/` to `src/main/resources/lang/` and are read straight off
  the module class loader — no APK path needed at all. The old ZIP scan is kept as a fallback and
  still auto-discovers packs the index does not list.
- `Logger` routes through `XBridge` and degrades to `android.util.Log` if no backend is
  installed yet.
- Startup now logs the active backend, framework name and version.

### Nekogram

- **Nekogram X added** (`nekox.messenger`): new `Clients/NekogramX.java` resolver, `ClientType`
  entry and `META-INF/xposed/scope.list` item. Like its Momogram fork, NekoX ships unobfuscated
  Telegram symbols, so the mapping tables are empty and names resolve to upstream Telegram names.
- **Version drift is now visible.** `ClientChecker#checkClientVersion` records the build each
  resolver table was generated against and logs one clear warning at startup when the installed
  client differs — loud for the obfuscated clients (Nekogram, Cherrygram), where a mismatch means
  essentially every hook silently fails.
- **Official Telegram re-verified against 12.10.1** (build 70382). The symbols the module resolves
  by name — `TLRPC.Message` fields, `TL_messages_readHistory` / `TL_channels_readHistory`,
  `TL_updateDeleteMessages`, `MessagesController` / `ConnectionsManager` / `UserConfig` accessors
  and `PeerStoriesView$StoryItemHolder#allowScreenshots` — are unchanged from the previously
  verified build, so no hook needed updating.

> The Nekogram/Cherrygram R8 mapping tables themselves still have to be regenerated from the target
> APK whenever those clients update; that cannot be done from source.

### Build / repo fixes

- `settings.gradle` included `':TeleVip'`, a module that does not exist — removed.
- `.gitignore` had an unresolved merge conflict (`<<<<<<< HEAD` … `>>>>>>>`) committed to it —
  resolved, plus the generated `DexHolder.java` is now ignored.
- Added `io.github.libxposed:api:102.0.0` as a `compileOnly` dependency in the version catalog.
- ProGuard rules for both entry points (`-adaptresourcefilecontents META-INF/xposed/java_init.list`
  and the `XposedModule` keep rule).
- `xposeddescription` moved to a string resource, with `android:description` added for the modern
  API (which reads the module description from there); Arabic kept in `values-ar`.
- Version bumped to 3.7.0 (340).
