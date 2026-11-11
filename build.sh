#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$PWD/toolchain/jdk-17.0.21+9}"
export PATH="$JAVA_HOME/bin:$PATH"
rm -rf build/classes build/test build/dex
mkdir -p build/classes build/test build/dex
javac --release 8 -cp toolchain/android.jar:toolchain/xposed-api.jar -d build/classes src/local/lilith/qqscanfix/*.java
javac --release 8 -cp build/classes -d build/test FilterTest.java
java -cp build/classes:build/test local.lilith.qqscanfix.FilterTest
jar cf build/code.jar -C build/classes .
java -cp toolchain/r8.jar com.android.tools.r8.D8 --min-api 27 --lib toolchain/android.jar --classpath toolchain/xposed-api.jar --output build/dex build/code.jar
toolchain/aapt2 compile --dir res -o build/resources.zip
toolchain/aapt2 link -I toolchain/android.jar --manifest AndroidManifest.xml -A assets -o build/unsigned.apk build/resources.zip
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:
    z.write('build/dex/classes.dex','classes.dex',compress_type=zipfile.ZIP_DEFLATED)
PY
if [[ ! -f signing.p12 ]]; then
    (umask 077; keytool -genkeypair -keystore signing.p12 -storetype PKCS12 -alias qqscanfix -storepass local-build -keypass local-build -keyalg RSA -keysize 3072 -validity 3650 -dname 'CN=Local QQ Scan Fix')
fi
javac -cp toolchain/apksig.jar -d build Sign.java
java -cp toolchain/apksig.jar:build Sign signing.p12 build/unsigned.apk build/qq-scan-fix.apk
toolchain/aapt2 dump badging build/qq-scan-fix.apk
