#!/bin/bash
exec </dev/null
adb logcat -G 16M
adb logcat -c
timeout 420 gradle :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee build.log
rc=${PIPESTATUS[0]}
adb logcat -d -v threadtime > logcat.txt
rm -rf shots; mkdir -p shots
python3 - <<'PY'
import re,base64,collections
parts=collections.defaultdict(dict)
for line in open('logcat.txt',errors='ignore'):
    m=re.search(r'KPSHOT *: (\S+) (\d+) (\d+) (\S+)',line)
    if m: parts[m.group(1)][int(m.group(2))]=(int(m.group(3)),m.group(4))
for name,d in parts.items():
    n=next(iter(d.values()))[0]
    if len(d)==n: open('shots/%s.jpg'%name,'wb').write(base64.b64decode(''.join(d[i][1] for i in range(1,n+1))))
notes=[l for l in open('logcat.txt',errors='ignore') if 'KPNOTE' in l]
open('shots/notes.txt','w').write(''.join(notes))
PY
{
  echo "rc=$rc"
  echo "---- test runner"
  grep -E "TestRunner|INSTRUMENTATION_|Instrumentation run failed|Process crashed" logcat.txt build.log | cut -c1-230 | head -30
  echo "---- tour notes"; cut -c34-1500 shots/notes.txt 2>/dev/null | tr '|' '\n' | head -30; ls shots 2>/dev/null | head -20
  echo "---- FATAL"
  grep -A24 "FATAL EXCEPTION" logcat.txt | cut -c34-220 | head -40
  echo "---- native"
  grep -E "Fatal signal|SIGSEGV|SIGABRT|backtrace:|Cmdline: com.kingplay" logcat.txt | cut -c1-200 | head -12
  echo "---- died"
  grep -E "Process com.kingplay.* has died|am_crash|am_proc_died.*kingplay|Force finishing|ANR in" logcat.txt | cut -c1-200 | head -8
} > summary.txt 2>&1
echo "::error title=Gradle output::$(head -c 9500 summary.txt | sed ':a;N;$!ba;s/%/%25/g;s/\r//g;s/\n/%0A/g')"
exit $rc
