#!/bin/bash
# A rooted Android emulator on an Apple-silicon Mac for trying Re: TeleVIP on real clients:
# Android 14 (arm64), Magisk with Zygisk (rooted with rootAVD), Vector (LSPosed), Re: TeleVIP
# and the Telegram clients.
#
#   tools/android-emulator.sh setup         one-time setup, about 30 minutes; asks you to tap a
#                                           few things on the emulator along the way
#   tools/android-emulator.sh start         start the emulator again later
#   tools/android-emulator.sh install       install or update Re: TeleVIP and the clients
#   tools/android-emulator.sh check <pkg>   open a client, then save a screenshot and the log
#   tools/android-emulator.sh capture       save a screenshot and the log of what is on screen
#
# Everything goes in ~/Library/Android/sdk (the Android SDK) and ~/televip-emulator (the rest).
# Needs macOS on an Apple chip, an internet connection and about 15 GB of disk.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
API=34
IMAGE="system-images;android-$API;google_apis;arm64-v8a"
RAMDISK="system-images/android-$API/google_apis/arm64-v8a/ramdisk.img"
AVD=televip
WORK="$HOME/televip-emulator"
CMDLINE_TOOLS="https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip"
VECTOR_ZIP="https://github.com/JingMatrix/Vector/releases/download/v2.2/Vector-v2.2-3080-Release.zip"

# Client name | GitHub repo (or a direct URL) | regex picking its arm64 APK (matched against
# the download URL) from the latest release
CLIENTS="Telegram|https://telegram.org/dl/android/apk|
Nekogram|Nekogram/Nekogram|arm64-v8a
Cherrygram|arsLan4k1390/Cherrygram|arm64
Nagram|NextAlone/Nagram|arm64
NagramX|risin42/NagramX|/NagramX-[^/]*arm64
Momogram|im030/Momogram|arm64-v8a"

export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
ADB="$SDK/platform-tools/adb"
mkdir -p "$WORK"

# Runs a command as root on the emulator, passed to su as one string.
as_root() { "$ADB" shell "su -c '$1'"; }

bold() { printf '\n\033[1m%s\033[0m\n' "$*"; }
step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
wait_for_you() { printf '\n\033[1;33m%s\033[0m\n' "$*"; read -r -p "Press Enter when done... " _; }
die() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

check_mac() {
  [ "$(uname -s)" = Darwin ] || die "This script is for macOS."
  [ "$(uname -m)" = arm64 ] || die "This script is for Macs with an Apple chip (M1 or later)."
}

java_ready() {
  if java -version >/dev/null 2>&1; then return; fi
  if [ -x /opt/homebrew/opt/openjdk@17/bin/java ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
    export PATH="$JAVA_HOME/bin:$PATH"
    return
  fi
  command -v brew >/dev/null 2>&1 || die "Java is needed for the Android SDK tools. Install Homebrew from https://brew.sh (one command), then run this again."
  step "Installing Java 17 (Homebrew)"
  brew install openjdk@17
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
}

sdk_ready() {
  local sdkmanager="$SDK/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$sdkmanager" ]; then
    step "Downloading the Android SDK command-line tools"
    mkdir -p "$SDK/cmdline-tools"
    curl -fL --retry 3 -o "$WORK/cmdline-tools.zip" "$CMDLINE_TOOLS"
    rm -rf "$SDK/cmdline-tools/latest" "$SDK/cmdline-tools/cmdline-tools"
    unzip -q "$WORK/cmdline-tools.zip" -d "$SDK/cmdline-tools"
    mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  fi
  step "Installing the emulator and the Android $API image (a few GB)"
  yes | "$sdkmanager" --licenses >/dev/null || true
  "$sdkmanager" "platform-tools" "emulator" "$IMAGE"
}

avd_ready() {
  if "$SDK/cmdline-tools/latest/bin/avdmanager" list avd 2>/dev/null | grep -q "Name: $AVD\$"; then return; fi
  step "Creating the emulator ($AVD)"
  echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD" -k "$IMAGE" -d pixel_7
  local config="$HOME/.android/avd/$AVD.avd/config.ini"
  { echo "hw.keyboard=yes"; echo "disk.dataPartition.size=8G"; } >> "$config"
}

running() { "$ADB" devices 2>/dev/null | grep -q '^emulator-'; }

boot() {
  if running; then return; fi
  step "Starting the emulator (a window opens; the first boot takes a few minutes)"
  # Always a cold boot: a saved snapshot would skip the patched ramdisk.
  nohup "$SDK/emulator/emulator" -avd "$AVD" -no-snapshot -no-boot-anim >"$WORK/emulator.log" 2>&1 &
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
  sleep 5
}

wait_until_off() {
  for _ in $(seq 1 90); do running || return 0; sleep 2; done
  "$ADB" emu kill >/dev/null 2>&1 || true
  sleep 5
}

reboot_device() {
  step "Restarting the emulator"
  "$ADB" reboot
  sleep 10
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
  sleep 5
}

root_device() {
  if "$ADB" shell 'command -v magisk' >/dev/null 2>&1; then echo "Magisk is already installed."; return; fi
  step "Rooting the emulator with Magisk (rootAVD)"
  if [ ! -d "$WORK/rootAVD" ]; then
    git clone --depth 1 https://gitlab.com/newbit/rootAVD.git "$WORK/rootAVD"
  fi
  bold "In a moment rootAVD lists Magisk versions: leave it, it picks the stable one after 10 seconds."
  bold "Then the Magisk app opens on the emulator. Within 60 seconds:
  1. tap Install (on the Magisk card),
  2. tap Select and Patch a File,
  3. pick fakeboot.img (in Downloads),
  4. tap LET'S GO, and when it says All done, come back here and press Enter."
  read -r -p "Press Enter to start... " _
  (cd "$WORK/rootAVD" && PATH="$SDK/platform-tools:$PATH" ./rootAVD.sh "$RAMDISK" FAKEBOOTIMG)
  bold "rootAVD shuts the emulator down; starting it again with Magisk in place."
  wait_until_off
  boot
}

grant_root_and_zygisk() {
  bold "The Magisk app opens on the emulator:
  - if it says 'Requires additional setup', tap OK; the emulator restarts and this script waits;
  - when it asks whether to give Shell superuser access, tap GRANT."
  "$ADB" shell monkey -p com.topjohnwu.magisk -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  until as_root id 2>/dev/null | grep -q 'uid=0'; do sleep 3; "$ADB" wait-for-device; done
  echo "Root works."
  step "Turning on Zygisk"
  # Through a file: the SQL's own quotes would not survive adb shell and su -c.
  printf '%s\n' "magisk --sqlite \"REPLACE INTO settings (key,value) VALUES('zygisk',1)\"" > "$WORK/zygisk.sh"
  "$ADB" push "$WORK/zygisk.sh" /data/local/tmp/zygisk.sh >/dev/null
  as_root "sh /data/local/tmp/zygisk.sh"
}

install_vector() {
  if as_root "ls /data/adb/modules" 2>/dev/null | grep -qi 'zygisk_lsposed\|zygisk_vector'; then
    echo "Vector is already installed."
    return
  fi
  step "Installing Vector (LSPosed)"
  curl -fL --retry 3 -o "$WORK/vector.zip" "$VECTOR_ZIP"
  "$ADB" push "$WORK/vector.zip" /data/local/tmp/vector.zip >/dev/null
  as_root "magisk --install-module /data/local/tmp/vector.zip"
}

download_apk() {   # name, repo-or-url, regex -> path
  local name=$1 from=$2 pattern=$3 file="$WORK/apks/$1.apk" url
  mkdir -p "$WORK/apks"
  case "$from" in
    https://*) url=$from ;;
    *)
      url=$(curl -fsSL "https://api.github.com/repos/$from/releases/latest" \
        | grep -o '"browser_download_url": *"[^"]*\.apk"' | sed 's/.*"\(https[^"]*\)"/\1/' \
        | grep -E -e "$pattern" | head -n 1) || true
      ;;
  esac
  [ -n "$url" ] || { echo "No APK found for $name, skipped." >&2; return 1; }
  curl -fL --retry 3 -C - -o "$file" "$url" >&2
  echo "$file"
}

install_apps() {
  step "Installing Re: TeleVIP"
  local televip
  televip=$(download_apk ReTeleVIP 2B-4G10/TeleVIP '-release\.apk$') && "$ADB" install -r "$televip"
  echo "$CLIENTS" | while IFS='|' read -r name from pattern; do
    step "Installing $name"
    local apk
    apk=$(download_apk "$name" "$from" "$pattern") && "$ADB" install -r "$apk" || echo "$name was not installed."
  done
}

enable_module() {
  bold "Last step, on the emulator:
  1. Pull down the notification shade and tap the Vector notification
     (or open the Phone app and dial *#*#5776733#*#*).
  2. Modules -> Re: TeleVIP -> turn it on, then tick the Telegram clients.
  3. Force stop the clients (or run: tools/android-emulator.sh check <package>)."
  wait_for_you "Enable Re: TeleVIP in Vector as above."
}

capture() {   # [name]
  local name=${1:-screen} out
  out="$WORK/checks/$name-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$WORK/checks"
  "$ADB" exec-out screencap -p > "$out.png"
  "$ADB" logcat -d > "$out-logcat.txt"
  grep -iE 'TeleVip|LSPosed|Vector|lspd|AndroidRuntime|FATAL' "$out-logcat.txt" > "$out-televip.txt" || true
  bold "Saved:"
  ls -1 "$out"*
}

check() {   # package
  local pkg=${1:?usage: check <package name, e.g. nu.gpu.nagram>}
  "$ADB" logcat -c
  "$ADB" shell am force-stop "$pkg"
  "$ADB" shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null
  echo "Waiting 20 seconds for $pkg to start and Re: TeleVIP to hook it..."
  sleep 20
  capture "$pkg"
  bold "Open Re: TeleVIP Settings in the client, then run: tools/android-emulator.sh capture $pkg-settings"
}

case "${1:-}" in
  setup)
    check_mac; java_ready; sdk_ready; avd_ready; boot; root_device; grant_root_and_zygisk
    install_vector; reboot_device; install_apps; enable_module
    bold "Done. Try it: tools/android-emulator.sh check nu.gpu.nagram"
    ;;
  start) check_mac; boot ;;
  install) check_mac; boot; install_apps ;;
  check) shift; check "$@" ;;
  capture) shift; capture "$@" ;;
  *) sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
