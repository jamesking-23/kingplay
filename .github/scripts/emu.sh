#!/bin/bash
exec </dev/null
adb logcat -c
timeout 420 gradle :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee build.log
rc=${PIPESTATUS[0]}
adb logcat -d -v threadtime > logcat.txt
rm -rf shots; mkdir -p shots; adb pull /sdcard/Android/data/com.kingplay.app.debug/files/shots/. shots/ >/dev/null 2>&1 || true
{
  echo "rc=$rc"
  echo "---- test runner"
  grep -E "TestRunner|INSTRUMENTATION_|Instrumentation run failed|Process crashed" logcat.txt build.log | cut -c1-230 | head -30
  echo "---- FATAL"
  grep -A24 "FATAL EXCEPTION" logcat.txt | cut -c34-220 | head -40
  echo "---- native"
  grep -E "Fatal signal|SIGSEGV|SIGABRT|backtrace:|Cmdline: com.kingplay" logcat.txt | cut -c1-200 | head -12
  echo "---- died"
  grep -E "Process com.kingplay.* has died|am_crash|am_proc_died.*kingplay|Force finishing|ANR in" logcat.txt | cut -c1-200 | head -8
} > summary.txt 2>&1
echo "::error title=Gradle output::$(head -c 9500 summary.txt | sed ':a;N;$!ba;s/%/%25/g;s/\r//g;s/\n/%0A/g')"
exit $rc
