# Changelog

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
