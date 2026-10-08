# Releasing

The README's live badges show the latest stable GitHub release and total release
asset downloads across all releases. Download counts include the APK and checksum
assets; they are not installation or active-user counts. Check that both badges
resolve after publishing. Never put access tokens in badge URLs.

Release APKs use the stable local signing key. CI builds are validation builds
and use temporary keys.

1. Update the version name and integer version code in `build.sh`.
2. Update [CHANGELOG.md](../CHANGELOG.md).
3. Run tests, style checks, documentation checks, and the Android build.
4. Verify APK permissions and signature, and test the signed build on the relevant TV platforms.
5. Commit the source and wait for the CI checks for that commit to pass.
6. Create a release at the full source commit SHA and upload both output files.

```bash
bash test.sh
bash scripts/check-style.sh
python3 scripts/check-docs.py
bash build.sh
python3 check_apk.py

gh release create v1.3.1 \
  build/aquarium-4k.apk build/aquarium-4k.apk.sha256 \
  --repo jeremykenedy/fire-tv-aquarium \
  --target "$(git rev-parse HEAD)" --title 'Aquarium 4K 1.3.1' \
  --notes-file build/release-notes.txt
```

Change the tag and title for subsequent releases. GitHub requires a branch name
or full commit SHA as `--target`; a short SHA may be rejected. Check that the
uploaded APK and checksum match the locally tested bytes before announcing it.
Do not upload the keystore, password, device backup, raw source footage, or build
logs containing device addresses. Preserve the separate media license notice.

## Release verification

Keep the package `com.jeremykenedy.firetv.aquarium`, increment the version code,
and verify that the signing certificate matches the previous official release.
Test an in-place upgrade so preferences survive. Record physical hardware and
emulator results separately in [Verification](VERIFICATION.md), including any
fallback path that was implemented but not induced in testing.

Release notes must describe breaking changes, major changes, minor changes,
added commands or flags, the upgrade path, and validation. State explicitly when
there are no breaking changes or new commands. Use the stable signing key for the
tested release APK, and never publish CI's temporary-key APK as an upgrade.

After publishing, download the two uploaded assets into a separate directory:

```bash
gh release download v1.3.1 --repo jeremykenedy/fire-tv-aquarium \
  --pattern aquarium-4k.apk --pattern aquarium-4k.apk.sha256 \
  --dir build/release-1.3.1
```

Compare the downloaded APK's SHA-256 with the downloaded checksum and locally
tested APK, and verify its signing certificate. Check the release target commit,
asset names, and sizes. Preserve the locally tested APK if rebuilding solely for
documentation or CI changes; APK packaging and signing can change byte hashes
even when application sources are identical.
