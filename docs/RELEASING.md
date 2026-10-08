# Releasing

Keep the release version badge in `README.md` in sync with the released version.
The downloads badge identifies this as a private repository because public badge
services cannot read its download counts. Never put access tokens in badge URLs.

Release APKs use the stable local signing key. CI builds are validation builds
and use temporary keys.

1. Update the version name and integer version code in `build.sh`.
2. Update [CHANGELOG.md](../CHANGELOG.md).
3. Run tests, style checks, documentation checks, and the Android build.
4. Verify APK permissions and signature, and test the signed build on Fire TV.
5. Commit the source and wait for the CI checks for that commit to pass.
6. Create a release at the full source commit SHA and upload both output files.

```bash
bash test.sh
bash scripts/check-style.sh
python3 scripts/check-docs.py
bash build.sh
python3 check_apk.py

gh release create v1.1.0 \
  build/aquarium-4k.apk build/aquarium-4k.apk.sha256 \
  --repo jeremykenedy/fire-tv-aquarium \
  --target "$(git rev-parse HEAD)" --title 'Aquarium 4K 1.1.0' \
  --notes-file build/release-notes.txt
```

Change the tag and title for subsequent releases. GitHub requires a branch name
or full commit SHA as `--target`; a short SHA may be rejected. Check that the
uploaded APK and checksum match the locally tested bytes before announcing it.
Do not upload the keystore, password, device backup, raw source footage, or build
logs containing device addresses. Preserve the separate media license notice.
