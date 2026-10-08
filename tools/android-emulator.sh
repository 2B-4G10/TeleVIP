#!/bin/bash
# A rooted Android emulator on an Apple-silicon Mac for trying Re: TeleVIP on real clients:
# Android 14 (arm64) with Magisk and Zygisk, Vector (LSPosed), Re: TeleVIP and the Telegram
# clients, with Re: TeleVIP already turned on for them. Nothing to tap on the emulator.
#
#   tools/android-emulator.sh setup            one-time setup, about 30 minutes
#   tools/android-emulator.sh start            start the emulator again later (and root it)
#   tools/android-emulator.sh stop             shut the emulator down
#   tools/android-emulator.sh status           what is running: root, Zygisk, Vector, the module
#   tools/android-emulator.sh install          install or update Re: TeleVIP and the clients
#   tools/android-emulator.sh install-apk F    install a local Re: TeleVIP build (an .apk file)
#   tools/android-emulator.sh check <pkg>      open a client, then save a screenshot and the logs
#   tools/android-emulator.sh capture [name]   save a screenshot and the logs of what is on screen
#   tools/android-emulator.sh cleanup-old      delete what the first version of this script left
#
# Everything goes in ~/Documents/televip-emulator (or $TELEVIP_EMULATOR_HOME). The big folders
# end in .nosync so iCloud does not upload them. Needs macOS on an Apple chip, an internet
# connection and about 15 GB of disk.
#
# Root comes from Magisk's own emulator script (live_setup.sh). It starts Magisk without
# patching the system image, so `start` runs it again after every boot.
set -euo pipefail

ROOT="${TELEVIP_EMULATOR_HOME:-$HOME/Documents/televip-emulator}"
SDK="$ROOT/android-sdk.nosync"
DL="$ROOT/downloads.nosync"
CHECKS="$ROOT/checks"
LOGS="$ROOT/logs"
TMP="$ROOT/tmp.nosync"

API=34
IMAGE="system-images;android-$API;google_apis;arm64-v8a"
AVD=televip
PORT=5580                       # a fixed port, so a phone or another emulator never gets in the way
SERIAL="emulator-$PORT"
MODULE=io.github.re_televip.televip

CMDLINE_TOOLS="https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip"
MAGISK_APK="https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk"
MAGISK_APK_SHA256=e0d32d2123532860f97123d927b1bb86c4e08e6fd8a48bfc6b5bee0afae9ebd5
LIVE_SETUP="https://raw.githubusercontent.com/topjohnwu/Magisk/v30.7/scripts/live_setup.sh"
LIVE_SETUP_SHA256=b3dc430c96718cd4a08e30282c6c57f3da026bc4e75de09e0a426eaed64a1e49
VECTOR_ZIP="https://github.com/JingMatrix/Vector/releases/download/v2.2/Vector-v2.2-3080-Release.zip"
VECTOR_ZIP_SHA256=9ee8323575d615f7b3f1076ff60b2a63a49390ef11881b52632311a37f6f79cc

# Client name | GitHub repo (or a direct URL) | regex picking its arm64 APK (matched against
# the download URL) from the latest release
CLIENTS="Telegram|https://telegram.org/dl/android/apk|
Nekogram|Nekogram/Nekogram|arm64-v8a
Cherrygram|arsLan4k1390/Cherrygram|arm64
Nagram|NextAlone/Nagram|arm64
NagramX|risin42/NagramX|/NagramX-[^/]*arm64
Momogram|im030/Momogram|arm64-v8a"

# Only the AVD moves to Documents: adb keys and emulator settings stay in ~/.android, where
# both adb and the emulator look for them.
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK" ANDROID_AVD_HOME="$ROOT/avd.nosync"
ADB_BIN="$SDK/platform-tools/adb"
mkdir -p "$ROOT" "$DL" "$CHECKS" "$LOGS" "$TMP" "$ANDROID_AVD_HOME"

bold() { printf '\n\033[1m%s\033[0m\n' "$*"; }
step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

# Asks a yes/no question. TELEVIP_YES=1 answers yes; without a terminal it takes the default.
ask() {   # question, default (y or n)
  local reply
  if [ "${TELEVIP_YES:-}" = 1 ]; then reply=y; elif [ ! -t 0 ]; then reply=$2; else
    read -r -p "$1 [$( [ "$2" = y ] && echo Y/n || echo y/N )] " reply || reply=$2
    reply=${reply:-$2}
  fi
  case "$reply" in [Yy]*) return 0 ;; *) return 1 ;; esac
}

adb_dev() { "$ADB_BIN" -s "$SERIAL" "$@"; }
running() { "$ADB_BIN" devices 2>/dev/null | grep -q "^${SERIAL}[[:space:]]*device"; }
prop() { adb_dev shell getprop "$1" 2>/dev/null | tr -d '\r'; }

# Runs a shell script as root on the emulator. Going through a file keeps quotes intact, and
# /system/xbin/su is the emulator's own su (the Google APIs images let adb's shell use it).
root_sh() {
  # shellcheck disable=SC2016  # $PATH expands on the emulator
  printf '%s\n' 'export PATH=/debug_ramdisk:/sbin:/data/adb/magisk:$PATH' "$1" > "$TMP/root.sh"
  adb_dev push "$TMP/root.sh" /data/local/tmp/televip-root.sh >/dev/null
  adb_dev shell /system/xbin/su 0 sh /data/local/tmp/televip-root.sh
}

# Downloads a file once, checking its SHA-256 when one is given.
fetch() {   # url, file, [sha256]
  if [ ! -s "$2" ]; then
    curl -fL --retry 3 -o "$2.part" "$1" || die "Could not download $1. Check the internet connection and run the command again."
    mv "$2.part" "$2"
  fi
  if [ -n "${3:-}" ] && [ "$(shasum -a 256 "$2" | cut -d' ' -f1)" != "$3" ]; then
    rm -f "$2"
    die "The download of $1 is not the expected file (checksum mismatch). Run the command again."
  fi
}

check_mac() {
  [ "$(uname -s)" = Darwin ] || die "This script is for macOS."
  [ "$(uname -m)" = arm64 ] || die "This script is for Macs with an Apple chip (M1 or later)."
}

java_ready() {
  if java -version >/dev/null 2>&1; then return; fi
  local brew_jdk=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  if [ ! -x "$brew_jdk/bin/java" ]; then
    command -v brew >/dev/null 2>&1 || die "Java is needed for the Android SDK tools. Install Homebrew from https://brew.sh (one command), then run this again."
    step "Installing Java 17 (Homebrew)"
    brew install openjdk@17
  fi
  export JAVA_HOME="$brew_jdk" PATH="$brew_jdk/bin:$PATH"
}

sdk_ready() {
  local sdkmanager="$SDK/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$sdkmanager" ]; then
    step "Downloading the Android SDK command-line tools"
    fetch "$CMDLINE_TOOLS" "$DL/cmdline-tools.zip"
    mkdir -p "$SDK/cmdline-tools"
    rm -rf "$SDK/cmdline-tools/latest" "$SDK/cmdline-tools/cmdline-tools"
    unzip -q "$DL/cmdline-tools.zip" -d "$SDK/cmdline-tools"
    mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  fi
  if [ -x "$ADB_BIN" ] && [ -x "$SDK/emulator/emulator" ] && [ -d "$SDK/system-images/android-$API/google_apis/arm64-v8a" ]; then
    return
  fi
  step "Installing the emulator and the Android $API image (a few GB)"
  yes | "$sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
  "$sdkmanager" --sdk_root="$SDK" "platform-tools" "emulator" "$IMAGE"
}

set_config() {   # file, key, value
  { grep -v "^$2=" "$1" || true; echo "$2=$3"; } > "$1.new" && mv "$1.new" "$1"
}

avd_ready() {
  local config="$ANDROID_AVD_HOME/$AVD.avd/config.ini"
  if [ ! -f "$config" ]; then
    step "Creating the emulator ($AVD)"
    echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD" -k "$IMAGE" -d pixel_7 -p "$ANDROID_AVD_HOME/$AVD.avd"
  fi
  set_config "$config" hw.keyboard yes
  set_config "$config" hw.ramSize 4096
  set_config "$config" disk.dataPartition.size 8G
}

wait_booted() {   # seconds
  local i=0
  until [ "$(prop sys.boot_completed)" = 1 ]; do
    i=$((i + 3)); [ "$i" -lt "$1" ] || return 1
    if [ -n "${EMU_PID:-}" ] && ! kill -0 "$EMU_PID" 2>/dev/null; then
      tail -n 30 "$LOGS/emulator.log" >&2 || true
      die "The emulator closed while starting (log above, full log: $LOGS/emulator.log)."
    fi
    sleep 3
  done
  sleep 5
}

boot() {
  if running; then return; fi
  stop_first_attempt
  step "Starting the emulator (a window opens; the first start takes a few minutes)"
  nohup "$SDK/emulator/emulator" -avd "$AVD" -port "$PORT" -no-snapshot -no-boot-anim \
    >"$LOGS/emulator.log" 2>&1 &
  EMU_PID=$!
  local i=0
  until running; do
    i=$((i + 3))
    if [ "$i" -ge 300 ] || ! kill -0 "$EMU_PID" 2>/dev/null; then
      tail -n 30 "$LOGS/emulator.log" >&2 || true
      die "The emulator did not come up (log above, full log: $LOGS/emulator.log)."
    fi
    sleep 3
  done
  wait_booted 900 || die "Android did not finish starting in 15 minutes (log: $LOGS/emulator.log)."
}

magisk_live() {
  # shellcheck disable=SC2016  # $d expands on the emulator
  root_sh 'for d in /debug_ramdisk /sbin; do [ -x $d/magisk ] && $d/magisk --path >/dev/null 2>&1 && exit 0; done; exit 1' >/dev/null 2>&1
}

root_device() {
  adb_dev shell 'test -x /system/xbin/su' || die "This emulator image has no /system/xbin/su; it must be a Google APIs image (not Google Play)."
  step "Starting Magisk (root) on the emulator"
  local abi
  abi=$(prop ro.product.cpu.abi)
  fetch "$MAGISK_APK" "$DL/magisk.apk" "$MAGISK_APK_SHA256"
  fetch "$LIVE_SETUP" "$DL/live_setup.sh" "$LIVE_SETUP_SHA256"
  unzip -p "$DL/magisk.apk" "lib/$abi/libbusybox.so" > "$TMP/busybox" || die "Magisk has no busybox for this emulator's ABI ($abi)."
  adb_dev push "$TMP/busybox" "$DL/live_setup.sh" /data/local/tmp/ >/dev/null
  adb_dev push "$DL/magisk.apk" /data/local/tmp/magisk.apk >/dev/null
  # Restarts Android's framework with Magisk in place: the screen goes black for a moment.
  adb_dev shell sh /data/local/tmp/live_setup.sh > "$LOGS/magisk.log" 2>&1 || true
  wait_booted 600 || die "Android did not come back after starting Magisk (log: $LOGS/magisk.log)."
  magisk_live || { tail -n 30 "$LOGS/magisk.log" >&2; die "Magisk did not start (log above: $LOGS/magisk.log)."; }
  echo "Magisk is running."
}

zygisk_ready() {
  root_sh "magisk --sqlite \"SELECT value FROM settings WHERE key='zygisk'\"" 2>/dev/null | grep -q 'value=1'
}

enable_zygisk() {
  if zygisk_ready; then return 1; fi
  step "Turning on Zygisk"
  root_sh "magisk --sqlite \"REPLACE INTO settings (key,value) VALUES('zygisk',1)\""
  zygisk_ready || die "Zygisk could not be turned on."
}

vector_installed() { root_sh 'test -d /data/adb/modules/zygisk_vector -o -d /data/adb/modules_update/zygisk_vector' 2>/dev/null; }

install_vector() {
  if vector_installed; then return 1; fi
  step "Installing Vector (LSPosed)"
  fetch "$VECTOR_ZIP" "$DL/vector.zip" "$VECTOR_ZIP_SHA256"
  unzip -p "$DL/vector.zip" sepolicy.rule > "$TMP/vector-sepolicy.rule"
  adb_dev push "$DL/vector.zip" /data/local/tmp/vector.zip >/dev/null
  adb_dev push "$TMP/vector-sepolicy.rule" /data/local/tmp/vector-sepolicy.rule >/dev/null
  # Vector's SELinux types first, so the installer can label its files with them.
  root_sh 'magiskpolicy --live --apply /data/local/tmp/vector-sepolicy.rule; magisk --install-module /data/local/tmp/vector.zip' \
    > "$LOGS/vector-install.log" 2>&1 || { cat "$LOGS/vector-install.log" >&2; die "Vector did not install (log above)."; }
  vector_installed || die "Vector did not install (log: $LOGS/vector-install.log)."
}

CLI=/data/adb/modules/zygisk_vector/cli
vector_ready() { root_sh "$CLI status" >/dev/null 2>&1; }

wait_vector() {
  local i=0
  until vector_ready; do
    i=$((i + 3))
    [ "$i" -lt 120 ] || die "Vector did not start. Run: $0 capture vector, and send the files in $CHECKS."
    sleep 3
  done
}

download_apk() {   # name, repo-or-url, regex -> path
  local name=$1 from=$2 pattern=$3 file="$DL/apks/$1.apk" url
  mkdir -p "$DL/apks"
  case "$from" in
    https://*) url=$from ;;
    *)
      url=$(curl -fsSL "https://api.github.com/repos/$from/releases/latest" \
        | grep -o '"browser_download_url": *"[^"]*\.apk"' | sed 's/.*"\(https[^"]*\)"/\1/' \
        | grep -E -e "$pattern" | head -n 1) || true
      ;;
  esac
  [ -n "$url" ] || { echo "No APK found for $name, skipped." >&2; return 1; }
  rm -f "$file"
  curl -fL --retry 3 -o "$file" "$url" >&2 || { echo "Could not download $name." >&2; return 1; }
  echo "$file"
}

install_televip() {   # apk
  step "Installing Re: TeleVIP"
  adb_dev install -r "$1"
  mkdir -p "$DL/apks"
  [ "$1" -ef "$DL/apks/ReTeleVIP.apk" ] || cp "$1" "$DL/apks/ReTeleVIP.apk"
}

install_apps() {
  local apk name from pattern
  apk=$(download_apk ReTeleVIP 2B-4G10/TeleVIP '-release\.apk$') || die "Could not download Re: TeleVIP."
  install_televip "$apk"
  while IFS='|' read -r name from pattern; do
    step "Installing $name"
    if apk=$(download_apk "$name" "$from" "$pattern" </dev/null); then
      adb_dev install -r -g "$apk" </dev/null || echo "$name was not installed."
    fi
  done <<EOF
$CLIENTS
EOF
}

# The clients on the emulator that Re: TeleVIP supports (its META-INF/xposed/scope.list).
installed_clients() {
  local packages
  [ -f "$DL/apks/ReTeleVIP.apk" ] || die "Re: TeleVIP's APK is missing from $DL/apks. Run: $0 install"
  packages=$(adb_dev shell pm list packages 2>/dev/null | tr -d '\r' | sed 's/^package://')
  unzip -p "$DL/apks/ReTeleVIP.apk" META-INF/xposed/scope.list 2>/dev/null | tr -d '\r' \
    | while read -r pkg; do
        if [ -n "$pkg" ] && printf '%s\n' "$packages" | grep -Fqx "$pkg"; then echo "$pkg"; fi
      done
}

enable_module() {
  step "Turning on Re: TeleVIP in Vector"
  local clients scope="" pkg i=0
  clients=$(installed_clients)
  [ -n "$clients" ] || die "No supported Telegram client is installed. Run: $0 install"
  for pkg in $clients; do scope="$scope $pkg/0"; done
  # Vector registers a module a moment after its APK is installed.
  until root_sh "$CLI modules enable $MODULE" >/dev/null 2>&1; do
    i=$((i + 3))
    [ "$i" -lt 60 ] || die "Vector does not know Re: TeleVIP yet. Run: $0 status"
    sleep 3
  done
  root_sh "$CLI scope set $MODULE$scope"
  for pkg in $clients; do adb_dev shell am force-stop "$pkg"; done
}

status() {
  if ! running; then echo "The emulator is not running. Start it with: $0 start"; return; fi
  echo "Emulator: running ($SERIAL)"
  if magisk_live; then echo "Magisk: running"; else echo "Magisk: not running (run: $0 start)"; return; fi
  if zygisk_ready; then echo "Zygisk: on"; else echo "Zygisk: off"; fi
  if vector_ready; then
    root_sh "$CLI status" || true
    echo; root_sh "$CLI modules ls" || true
    echo; root_sh "$CLI scope ls $MODULE" || true
  else
    echo "Vector: not running"
  fi
}

capture() {   # [name]
  running || die "The emulator is not running. Start it with: $0 start"
  local name=${1:-screen} out
  out="$CHECKS/$name-$(date +%Y%m%d-%H%M%S)"
  adb_dev exec-out screencap -p > "$out.png"
  adb_dev logcat -d > "$out-logcat.txt"
  grep -iE 'TeleVip|LSPosed|Vector|lspd|Magisk|zygisk|AndroidRuntime|FATAL' "$out-logcat.txt" > "$out-televip.txt" || true
  # Vector's own logs (module and framework), readable only as root.
  if root_sh 'rm -rf /data/local/tmp/vector-log; cp -r /data/adb/lspd/log /data/local/tmp/vector-log && chmod -R a+rX /data/local/tmp/vector-log' >/dev/null 2>&1; then
    adb_dev pull /data/local/tmp/vector-log "$out-vector-log" >/dev/null 2>&1 || true
  fi
  LAST_CAPTURE=$out
  bold "Saved:"
  ls -1d "$out"*
}

check() {   # package
  local pkg=${1:?usage: check <package name, e.g. nu.gpu.nagram>}
  running || die "The emulator is not running. Start it with: $0 start"
  adb_dev logcat -c
  adb_dev shell am force-stop "$pkg"
  adb_dev shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null
  echo "Waiting 20 seconds for $pkg to start and Re: TeleVIP to hook it..."
  sleep 20
  capture "$pkg"
  if grep -q '\[TeleVip\]' "$LAST_CAPTURE-televip.txt" || grep -rqs '\[TeleVip\]' "$LAST_CAPTURE-vector-log"; then
    bold "Re: TeleVIP is loaded in $pkg."
  else
    bold "No Re: TeleVIP lines in $pkg's log yet: it may not be loaded. Run: $0 status"
  fi
  bold "Open Re: TeleVIP Settings in the client, then run: $0 capture $pkg-settings"
}

# The first version of this script used ~/Library/Android/sdk, ~/.android/avd and
# ~/televip-emulator, and rooted with rootAVD. Its emulator must not run beside this one.
OLD_SDK="$HOME/Library/Android/sdk"
stop_first_attempt() {
  local adb serial
  for adb in "$ADB_BIN" "$OLD_SDK/platform-tools/adb"; do
    [ -x "$adb" ] || continue
    for serial in $("$adb" devices 2>/dev/null | awk '/^emulator-/ {print $1}'); do
      [ "$serial" = "$SERIAL" ] && continue
      if "$adb" -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1 | grep -qx "$AVD"; then
        echo "Closing the emulator from the first attempt ($serial)."
        "$adb" -s "$serial" emu kill >/dev/null 2>&1 || true
        sleep 5
      fi
    done
  done
}

cleanup_old() {   # default answer (y or n)
  local paths=() p keep_sdk=""
  if [ -e "$HOME/.android/avd/$AVD.avd" ]; then
    paths+=("$HOME/.android/avd/$AVD.avd" "$HOME/.android/avd/$AVD.ini")
  fi
  [ -e "$HOME/televip-emulator" ] && paths+=("$HOME/televip-emulator")
  if [ -d "$OLD_SDK" ]; then
    # Only if it holds nothing but what the first script installed.
    keep_sdk=$(find "$OLD_SDK" -mindepth 1 -maxdepth 1 ! -name '.*' ! -name cmdline-tools \
      ! -name platform-tools ! -name emulator ! -name system-images ! -name licenses -print -quit)
    [ -z "$keep_sdk" ] && paths+=("$OLD_SDK")
  fi
  if [ "${#paths[@]}" -eq 0 ]; then
    [ "$1" = y ] || echo "Nothing left from the first attempt."
    return 0
  fi
  bold "The first attempt left these (the emulator now lives in $ROOT):"
  du -sh "${paths[@]}" 2>/dev/null || true
  [ -n "$keep_sdk" ] && echo "($OLD_SDK stays: it has other things in it, like $keep_sdk)"
  ask "Delete them?" "$1" || return 0
  stop_first_attempt
  if [ -x "$OLD_SDK/platform-tools/adb" ]; then "$OLD_SDK/platform-tools/adb" kill-server >/dev/null 2>&1 || true; fi
  for p in "${paths[@]}"; do rm -rf "$p"; done
  echo "Deleted."
}

write_readme() {
  cp "$0" "$ROOT/android-emulator.sh" 2>/dev/null || true
  chmod +x "$ROOT/android-emulator.sh"
  cat > "$ROOT/README.txt" <<EOF
Re: TeleVIP test emulator (Android $API, Magisk + Zygisk, Vector, Re: TeleVIP, Telegram clients)

  ./android-emulator.sh start            start the emulator and root it (needed after every start)
  ./android-emulator.sh status           what is running
  ./android-emulator.sh check <package>  open a client, save a screenshot and logs in checks/
  ./android-emulator.sh capture [name]   save a screenshot and logs of the screen in checks/
  ./android-emulator.sh install-apk F    install a local Re: TeleVIP build
  ./android-emulator.sh stop             shut it down

Packages: Telegram org.telegram.messenger.web, Nekogram tw.nekomimi.nekogram,
Cherrygram uz.unnarsx.cherrygram, Nagram xyz.nextalone.nagram, Nagram X nu.gpu.nagram,
Momogram momo.gram

Folders: checks/ (screenshots and logs), logs/ (setup logs). The .nosync folders hold the
Android SDK, the emulator and downloads; iCloud leaves them alone.
EOF
}

root_and_vector() {
  root_device
  local again=""
  enable_zygisk && again=1
  install_vector && again=1
  # Zygisk and Vector load when Magisk starts, so start it once more.
  if [ -n "$again" ]; then root_device; fi
  wait_vector
  echo "Vector is running."
}

# The emulator running, with Magisk and Vector (Magisk has to be started again after each boot).
ready() {
  check_mac; boot
  if magisk_live; then echo "Magisk is already running."; else root_and_vector; fi
}

case "${1:-}" in
  setup)
    check_mac
    cleanup_old y
    java_ready; sdk_ready; avd_ready; write_readme; boot
    root_and_vector
    install_apps; enable_module
    status
    bold "Done. The emulator and its files are in $ROOT."
    bold "Try it: $0 check nu.gpu.nagram"
    ;;
  start) ready ;;
  stop) if running; then adb_dev emu kill >/dev/null; echo "Stopped."; else echo "The emulator is not running."; fi ;;
  status) status ;;
  install) ready; install_apps; enable_module ;;
  install-apk)
    [ -f "${2:-}" ] || die "usage: $0 install-apk path/to/app.apk"
    ready; install_televip "$2"; enable_module
    ;;
  check) shift; check "$@" ;;
  capture) shift; capture "$@" ;;
  cleanup-old) cleanup_old n ;;
  *) sed -n '2,23p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
