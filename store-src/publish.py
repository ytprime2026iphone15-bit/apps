#!/usr/bin/env python3
"""Add or update one app in the store catalog.
usage: publish.py APK --name NAME [--desc TEXT] [--notes TEXT] [--repo DIR] [--base-url URL]
Copies the APK into DIR/apps/<package>-v<versionCode>.apk (removes older files of that app),
and writes the entry (versionCode, versionName, sha256, size, signer cert) into DIR/catalog.json.
Pushing DIR to the hosting repo is a separate step (git add -A; commit; push)."""
import argparse, hashlib, json, os, re, shutil, subprocess, sys
BT = "/tmp/b/sdk/build-tools/34.0.0"
ap = argparse.ArgumentParser(); ap.add_argument("apk"); ap.add_argument("--name", required=True)
ap.add_argument("--desc", default=""); ap.add_argument("--notes", default="")
ap.add_argument("--repo", default="/tmp/st/repo"); ap.add_argument("--base-url", default=os.environ.get("BASE_URL", "https://raw.githubusercontent.com/OWNER/apps/main"))
a = ap.parse_args()
b = subprocess.check_output([BT + "/aapt2", "dump", "badging", a.apk], text=True)
m = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", b)
pkg, vc, vn = m.group(1), int(m.group(2)), m.group(3)
c = subprocess.check_output([BT + "/apksigner", "verify", "--print-certs", a.apk], text=True)
cert = re.search(r"SHA-256 digest: ([0-9a-f]+)", c).group(1)
os.makedirs(a.repo + "/apps", exist_ok=True)
for f in os.listdir(a.repo + "/apps"):
    if f.startswith(pkg + "-v"): os.remove(a.repo + "/apps/" + f)
fn = f"{pkg}-v{vc}.apk"; shutil.copy(a.apk, a.repo + "/apps/" + fn)
data = open(a.apk, "rb").read()
cat_path = a.repo + "/catalog.json"
cat = json.load(open(cat_path)) if os.path.exists(cat_path) else {"apps": []}
e = next((x for x in cat["apps"] if x["id"] == pkg), None)
if e is None: e = {"id": pkg}; cat["apps"].append(e)
if e.get("versionCode", 0) > vc: sys.exit(f"refusing: catalog has newer versionCode {e['versionCode']}")
e.update({"name": a.name or e.get("name"), "desc": a.desc or e.get("desc", ""), "notes": a.notes, "versionCode": vc, "versionName": vn,
          "url": f"{a.base_url}/apps/{fn}", "sha256": hashlib.sha256(data).hexdigest(), "size": len(data), "cert": cert})
json.dump(cat, open(cat_path, "w"), ensure_ascii=False, indent=1)
print("published", pkg, vn, vc, "sha256", e["sha256"][:12], "cert", cert[:12])
