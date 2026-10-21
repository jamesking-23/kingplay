#!/bin/bash
# Installs one release over an older one, the way Android does for an in-app update.
set -x
base=https://github.com/jamesking-23/kingplay/releases/download
curl -sSL -o old.apk "$base/$OLD_TAG/KingPlay-debug.apk"
curl -sSL -o new.apk "$base/$NEW_TAG/KingPlay-debug.apk"
pkg=com.kingplay.app.debug
adb install old.apk
echo "::notice::old installed: $(adb shell dumpsys package $pkg | grep -m1 versionCode)"
out=$(adb install -r new.apk 2>&1)
echo "::notice::update result: $out"
echo "::notice::after update: $(adb shell dumpsys package $pkg | grep -m1 versionCode)"
adb shell am start -n $pkg/com.kingplay.app.MainActivity
sleep 8
echo "::notice::running: $(adb shell pidof $pkg)"
