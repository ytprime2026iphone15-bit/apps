# Release a new store version: bump versionCode, build with the real catalog URL, publish to the repo dir.
# usage: CATALOG_URL=... BASE_URL=... bash release_store.sh "what's new"
set -e; cd /tmp/st
VC=$(( $(cat vc.txt 2>/dev/null || echo 1) + 1 )); echo $VC > vc.txt
VC=$VC VN=1.$((VC-1)) bash build.sh
python3 publish.py /downloads/store-v$VC.apk --name "متجر التطبيقات" --desc "المتجر نفسه، بيتحدث من هنا" --notes "${1:-تحسينات}"
