#!/usr/bin/env bash
# Smoke test on a running emulator: install the APK, open it, and check it is still alive
# a while later (spec 006, T006-18). It exists because 0.1.0 was published without anybody
# having run it, and it crashed on launch (missing INTERNET permission).
#
#   scripts/smoke-test.sh path/to/app.apk
set -euo pipefail

apk="${1:?usage: smoke-test.sh <apk>}"
pkg="io.github.abeleiras.aura"
wait_seconds="${SMOKE_WAIT_SECONDS:-20}"

fail() {
  echo "::error::SMOKE TEST FAILED: $1"
  echo "---- fatal exceptions ----"
  adb logcat -d -b crash,main 2>/dev/null | grep -A30 "FATAL EXCEPTION" | head -80 || true
  echo "---- other relevant logcat (emulator noise filtered out) ----"
  adb logcat -d -b main,system,crash 2>/dev/null | grep -iE "abeleiras|AndroidRuntime|SecurityException" \
    | grep -vE "ApkAssets|AppHibernationService|ShortcutService|AiAi|ImeTracker" | tail -60 || true
  exit 1
}

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done

adb install -r "$apk" || fail "the APK doesn't install"
adb shell pm grant "$pkg" android.permission.RECORD_AUDIO || true
adb shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS || true

adb logcat -c
adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null || fail "the launcher activity can't be started"

# Long enough for the first frames and for the startup update check, which makes a network request.
sleep "$wait_seconds"

pid="$(adb shell pidof "$pkg" | tr -d '\r' || true)"
crash_pid="$(adb shell pidof "$pkg:crash" | tr -d '\r' || true)"
echo "main process: ${pid:-<none>} | error screen process: ${crash_pid:-<none>}"

[ -n "$pid" ] || fail "the app process is not running $wait_seconds s after launch"
[ -z "$crash_pid" ] || fail "the app's error screen is showing (a crash was caught)"
if adb logcat -d -b crash | grep -q "FATAL EXCEPTION"; then fail "a fatal exception was logged"; fi

echo "Smoke test passed: Aura is still running $wait_seconds s after launch."
