#!/bin/bash
adb logcat -c
gradle :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee build.log
rc=${PIPESTATUS[0]}
adb logcat -d -v threadtime > logcat.txt
{ grep -n -A25 "FATAL EXCEPTION" logcat.txt | head -80; echo ----; grep -n -E "KingPlay|PlaybackService|ExoPlayer|AndroidRuntime|Hilt|Dagger" logcat.txt | grep -E " E | W " | head -40; echo ----; grep -E "FAILED|player error|never started|Test.*failed|Exception" build.log | head -20; } > summary.txt
echo "::error title=Gradle output::$(head -c 9000 summary.txt | sed ':a;N;$!ba;s/%/%25/g;s/\r//g;s/\n/%0A/g')"
exit $rc
