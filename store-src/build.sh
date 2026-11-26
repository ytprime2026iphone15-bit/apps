set -e
cd /tmp/st; export JAVA_HOME=/tmp/b/jdk-17.0.21.1+2 PATH=/tmp/b/jdk-17.0.21.1+2/bin:$PATH
BT=/tmp/b/sdk/build-tools/34.0.0; AJ=/tmp/b/sdk/platforms/android-34/android.jar
CATALOG_URL=${CATALOG_URL:-https://example.invalid/catalog.json}; VC=${VC:-1}; VN=${VN:-1.0}
rm -rf out cls gen; mkdir -p out cls gen/com/mohamed/store
sed "s#@@CATALOG_URL@@#$CATALOG_URL#;s#@@STORE_CERT@@#$(cat cert.txt)#" src/com/mohamed/store/Core.java > gen/com/mohamed/store/Core.java
for f in Main InstallReceiver BootReceiver UpdateJob; do cp src/com/mohamed/store/$f.java gen/com/mohamed/store/; done
sed -i "s/android:versionCode=\"[0-9]*\" android:versionName=\"[^\"]*\"/android:versionCode=\"$VC\" android:versionName=\"$VN\"/" AndroidManifest.xml
$BT/aapt2 compile --dir res -o out/res.zip
$BT/aapt2 link -I $AJ --manifest AndroidManifest.xml -o out/base.apk out/res.zip
javac -encoding UTF-8 -source 8 -target 8 -cp $AJ -d cls gen/com/mohamed/store/*.java 2>&1 | grep -v -i "warning\|^Note" || true
$BT/d8 --lib $AJ --min-api 29 --output out $(find cls -name '*.class')
(cd out && zip -q base.apk classes.dex)
$BT/zipalign -f 4 out/base.apk out/al.apk
OUT=/downloads/store-v$VC.apk
$BT/apksigner sign --ks ks.jks --ks-pass env:KS_PASS --key-pass env:KS_PASS --ks-key-alias k --out $OUT out/al.apk
$BT/apksigner verify --print-certs $OUT | grep SHA-256
$BT/aapt2 dump badging $OUT | head -2
ls -la $OUT
