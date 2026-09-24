# TeleVip LSPosed

<p>
  <img src="https://img.shields.io/badge/Platform-Android-green">
  <img src="https://img.shields.io/badge/Framework-LSPosed%20%7C%20Vector-blue">
  <img src="https://img.shields.io/badge/Xposed%20API-93%20%7C%20102-blueviolet">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange">
</p>

A powerful Xposed module that adds advanced customization features to Telegram clients.


## ✨ Features

### Privacy
- Hide "Seen" status in:
    - Private chats
    - Channels and Groups
- Hide "Typing..." indicator
- Hide online status
- Hide phone number
- Hide story view status
- Show deleted messages
- Prevent deletion of secret media

### Media & Stories
- Save protected stories to gallery
- Save voice messages
- Enable secret media
- Save message edit history

### Telegram Modifications
- Remove content saving restrictions
- Disable stories
- Hide pinned messages
- Disable channel swipe
- Disable profile swipe
- Disable update notifications
- Disable number rounding

### Performance
- Boost Telegram download speed

### Premium
- Enable Local Premium


> More features are available but not listed here.


# 📱 Supported Clients

| Client | Version |
|---|---|
| Telegram | 12.8.3 (69222) |
| Telegram Beta | 12.9.0 (69579) |
| Telegram Web | 12.8.3 (69229) |
| TG Connect | 11.13.1 (11130109) |
| Plus Messenger | 12.8.1.0 (22350) |
| Nagram | 12.8.1 (1239) |
| NagramX | 12.8.1-2bcd1bd (1253) |
| Nagram XF | 12.7.3 (1245) |
| Nekogram | 12.8.1 (69160) |
| Nekogram X | any (`nekox.messenger`, unobfuscated) |
| Cherrygram | 12.8.1 (69160) |
| Nicegram | 1.55.0 (2139) |
| iMe | 12.8.1 (12080102) |
| iMe Direct | 12.8.1 (12080109) |
| X Plus | 12.0.1 (61669) |
| ForkClient | 12.8.4.0 (691908) |
| ForkClient Beta | 12.8.4.0 (691909) |
| Skygram | 10.20.6 (40639) |
| Teegra | 10.3.2 (41469) |
| Telegraph | 12.8.1.1 (69172) |
| Telega | 2.4.3 (107) |
| Momogram | 12.6.4 |
| Forkgram Classic | 12.8.10.0 |
| Turrit | 1.8.9.9.5 |


# 🧩 Supported frameworks

TeleVip is a **libxposed API 102 module only**. It declares `minApiVersion=102`, so frameworks that
implement the modern contract load it and older ones do not advertise it at all.

| Framework | Module API | Entry point |
|---|---|---|
| Vector 2.2+ (JingMatrix) | 102 (modern libxposed) | `META-INF/xposed/java_init.list` → `com.my.televip.xposed.TeleVipModule` |
| LSPosed 1.10+ | 102 (modern libxposed) | same as above |
| LSPosed 1.9.x, EdXposed, LSPatch | 93 (legacy) | **not supported** — the legacy `assets/xposed_init` entry was removed |

Zygisk providers: **Zygisk Next / NeoZygisk**, Magisk built-in Zygisk and KernelSU are all supported —
the module talks to the Xposed framework only through `com.my.televip.xposed.XBridge` and never
assumes a particular loader. In particular it no longer depends on `initZygote()` for its own APK
path, which is what used to break language loading on app-scoped modules under Zygisk Next.



# 🔄 Surviving Telegram updates

Official Telegram is not obfuscated, so TeleVip hooks it by plain name — 74 classes and 172
distinct method names. Every one of those is something a client release can move, and when one
moves the matching feature simply stops working.

What the module does about it:

- **Signature drift is tolerated.** A method that keeps its name but gains a parameter, or has a
  parameter type renamed, used to take its hook down. It is now re-matched by name, and the hook
  is attached anyway. This is only attempted once the exact lookup has already failed, so a hook
  that still resolves normally behaves exactly as before.
- **It refuses to guess.** A drifted candidate is accepted only when it is the single possibility.
  Where the call site passes parameter types the arity must still match, so the argument positions
  the callback reads stay aligned; where it passes none, the callback was written against a
  no-argument method and cannot be reading arguments at all. Anything ambiguous is left alone.
- **A class that no longer resolves becomes a wildcard** rather than failing the whole hook, so a
  renamed inner class no longer takes down a hook whose method is still there.
- **Breakage is visible.** One line at startup reports how the hooks landed:

  ```
  hook health: 68 resolved, 2 drifted, 1 missing
    drifted (signature changed, hooked anyway): SharedConfig#setNewAppVersionAvailable
    missing methods (feature inactive): ChatActivity#processSentMessage
  ```

  A bug report can then name the symbol that moved instead of "stories stopped working".

What it still cannot do, and no amount of matching will:

- **A renamed method or class cannot be found.** If Telegram renames `allowScreenshots`, nothing
  identifies the replacement; that feature is inactive until the name is updated here.
- **A reordered or ambiguous signature is left alone** on purpose — attaching to the wrong overload
  in a privacy feature is worse than that feature being off, because it would look like it works.
- **The obfuscated forks** (Nekogram, Cherrygram) still need their R8 mapping tables regenerated
  from the target APK on every client release. None of the above helps there.



# 📥 Download

GitHub builds the APK for you, so you do not need an Android SDK to get one.

1. Open the [**Actions** tab](../../actions/workflows/build-apk.yml).
2. Click the newest **Build APK** run — or press **Run workflow** to build the current code now.
3. Scroll to **Artifacts**, download the `TeleVip-…` file, and unzip it.
4. Copy `TeleVip-…-debug.apk` to your phone and open it to install.
5. In **LSPosed** or **Vector**: **Modules** → enable **TeleVip** → tick your Telegram clients →
   **force stop** Telegram and reopen it.

Each run repeats these steps on its own summary page, with the exact file names for that build.

> Install the **debug** APK. The release APK is only signed when the repository has signing
> secrets set (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) — Android
> refuses to install an unsigned APK, which is what "App not installed" means.



# 🛠️ Building

```bash
./gradlew :app:assembleRelease
```

Requirements: JDK 17, Android SDK 36. The libxposed API (`io.github.libxposed:api:102.0.0`) is a
`compileOnly` dependency, so it is not packaged — the framework provides its own implementation at
runtime.

To get an installable APK locally, either build the debug variant (`./gradlew :app:assembleDebug`,
signed with the debug key) or create a `keystore.properties` in the repository root so the release
variant is signed.

The `Nekogram` and `Cherrygram` resolvers hold R8 name mappings that are **specific to one client
build**. When a mismatch is detected TeleVip now logs a single explicit warning at startup instead
of failing silently; regenerate those tables when either client updates.



# 📢 Updates

All TeleVip updates are published on Telegram:

➡️ https://t.me/t_l0_e


# ⚠️ Warning
> This module is intended for educational purposes only. Its use may result in issues with your Telegram account, including the risk of banning or suspension. Use it at your own risk.


# 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

See the [LICENSE](./LICENSE) file for more information.


# Credits

Partially based on:

- [Re-Telegram](https://github.com/Sakion-Team/Re-Telegram).


Developed by **@mustafa1dev**