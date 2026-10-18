#!/bin/bash
exec </dev/null
adb logcat -c
timeout 420 gradle :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee build.log
rc=${PIPESTATUS[0]}
adb logcat -d -v threadtime > logcat.txt
PIDS=$(grep -o -E 'Start proc [0-9]+:com\.kingplay\.app\.debug' logcat.txt | grep -o -E '[0-9]+' | sort -u | paste -sd'|')
{
  echo "pids: $PIDS rc=$rc"
  grep -h -B2 -A14 -E '<failure|<error' $(find app/build/outputs -name '*.xml' 2>/dev/null) /dev/null | head -30
  echo "---- FATAL"
  grep -A40 "FATAL EXCEPTION" logcat.txt | cut -c34-230 | head -60
  echo "---- app errors"
  if [ -n "$PIDS" ]; then grep -E "^[0-9-]+ [0-9:.]+ +($PIDS) +[0-9]+ [EWF] " logcat.txt | grep -v -E 'ThreadPoolExecutor|Choreographer|ClassLoaderContext|ziparchive|CPU variant|Known variants|different ABI' | cut -c1-200 | head -8; fi
  echo "---- kingplay tags"
  grep -i -E 'kingplay|ScanWorker|Hilt' logcat.txt | grep -E ' [EWF] ' | head -15
  echo "---- last app lines"
  if [ -n "$PIDS" ]; then grep -E "^[0-9-]+ [0-9:.]+ +($PIDS) +[0-9]+ [IDEWF] " logcat.txt | grep -v -E 'ClassLoaderContext|ziparchive|nativeloader|CPU variant' | cut -c1-260 | tail -40; fi
  echo "---- ANR"
  grep -i -E 'ANR in|am_anr|Input dispatching timed out' logcat.txt | head -5
} > summary.txt 2>&1
echo "::error title=Gradle output::$(head -c 9500 summary.txt | sed ':a;N;$!ba;s/%/%25/g;s/\r//g;s/\n/%0A/g')"
exit $rc
