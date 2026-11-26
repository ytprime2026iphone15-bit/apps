# Store handover (متجر التطبيقات)

Public repo. No secrets here. Store app: package com.mohamed.store. Catalog: catalog.json. APKs: apps/.
Catalog URL (baked into the store): https://raw.githubusercontent.com/ytprime2026iphone15-bit/apps/main/catalog.json

## Where the secrets are (Instinct vault, never in chat or this repo)
- "GitHub apps token": login entry, password = fine-grained token, repo apps only, Contents read/write, no expiry. Account ytprime2026iphone15-bit (Google sign-in with the YouTube Gmail; sign-in needs a tap on his phone).
- "Android signing key: salah | tracker | ricoh | store": login entries, username says the alias (k), password field = `<keystore password>|<base64 of the .jks>`. Key password equals the keystore password. Restore: `tools vault write-file --key password --name "<entry>" --return-channel whatsapp` (the user must approve sandbox use once per entry via the approval link), then split at "|", base64-decode the second part into a .jks.
- Music keystore: kept by the music agent (package com.garrebha.music). Cert pinned in the store: 730490a6...11f6.
- Signing cert SHA-256 (must match catalog `cert`): salah fa809742b02315076baa4f87d53e6eb3dfa1dbc57f7c3062e70830292a509e33, tracker 30535dfca4244f9602c03ffa3744c615bf740c58141f98007eeb3abdd0a6c1eb, ricoh c3726f9d28fa53289e97c772ae444f9493d711c9f64d872810c960dc1a11ca8f, store 3d677795bc1726a367b37b8473df5ba1da2ec0394d5c9d0f6ecc41c17d8aabb1.

## Rules that keep updates working
1. Same package name and the same signing key forever. A new key means users must uninstall first.
2. versionCode must always go up.
3. The store pins certs for known packages (PINNED list in Core.java). A new app with a new key works with the cert in catalog.json; add it to PINNED in the next store release.

## Publish an app update
1. Have the signed APK. Run `python3 publish.py APK --name "..." --desc "..." --notes "..." --repo <clone of this repo> --base-url https://raw.githubusercontent.com/ytprime2026iphone15-bit/apps/main` (uses aapt2/apksigner from Android build-tools to read package, version and cert). It copies the APK to apps/ (old file removed) and updates catalog.json with sha256/size/cert.
2. Get the token to the sandbox: `tools vault write-file --key password --name "GitHub apps token" --return-channel whatsapp`, then `bash push.sh "message"` (commit, pull --rebase, push with the token, deletes the token file).
3. Wait about 5 minutes for GitHub's raw cache, then check the catalog URL and that each APK's sha256 matches.

## Release a new store version
`CATALOG_URL=<catalog url> BASE_URL=<raw base> KS_PASS=<store keystore password> bash release_store.sh "what's new"` (bumps versionCode in vc.txt, builds, signs with the store key, publishes), then push as above. Sources are in store-src/. Needs a JDK 17, Android build-tools 34 and android-34 platform jar (paths are at the top of build.sh).

## How the store behaves
Android 10+. First use: user allows "install unknown apps" for the store. Silent updates only on Android 12+ for apps the store itself installed; otherwise a notification and a confirm tap. Background check every ~6 h; also on opening. Verifies sha256, package, versionCode and signer before installing.
