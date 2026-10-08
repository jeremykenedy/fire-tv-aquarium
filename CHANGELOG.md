# Changelog

## 1.3.1 - 2026-10-08

- Add explicit cleartext blocking and system-only certificate trust to the APK.
- Harden installer device validation, remote-shell quoting, and APK verification.
- Preserve rendering behavior while simplifying environment drawing and safely
  publishing settings snapshots to the renderers.
- Enforce 100% Java and Python line and branch coverage across application source.
- Complete applicable quality checks, documentation, and release badges.
- Omit paid-plan features and skip Scrutinizer while repository import is unavailable.
- No breaking changes, major feature changes, or new commands or flags.
- Upgrade in place from earlier releases with the same package and signing key;
  saved preferences and screensaver backups are preserved.

## 1.3.0 - 2026-10-08

- Support Fire TV, Android TV, and Google TV with one offline APK.
- Target Android SDK 36 while retaining Android API 23 compatibility.
- Use physical display resolution and a local 1080p video fallback where needed.
- Add Random to every aquarium setting, resolved once per showing.
- Add Random version with switches to include or exclude each look and footage.
- Keep fixed choices, saved Random preferences, and existing settings during upgrades.
- Add --state-file for independent screensaver restore backups on multiple TVs.
- Preserve the application package and signing key; no breaking setting changes.
- Upgrade by installing the signed APK with adb install -r or install.py.

## 1.2.0

- Preserve the first installed aquarium video as Original 4K footage.
- Add saved Day / Night brightness for every animated look and original footage.
- Add Follow and Star badges and explicit tracking-free wording to both banners.

- Add six selectable aquarium appearances with Realistic as the default.
- Add realistic, cinematic, and classic drawn fish, sea life, and five matching
  underwater backgrounds for each artwork family.
- Retain the original Cartoon appearance and all independent aquarium controls.
- Animate transparent fish artwork with tail, fin, and tentacle movement.
- Save appearance preferences and retain older settings during upgrades.
- Add appearance screenshots and native Fire TV verification.

## 1.1.0 - 2026-10-08

- Add the original native 4K animated aquarium and remote-operated settings.
- Add five environments, population presets, exact count, six fish species,
  coordinated schools, seven optional sea-life groups, sunlight shimmer,
  bubbles, lighting, size, speed, and clock options.
- Retain the bundled real 4K aquarium footage mode.
- Save preferences across app restarts and upgrades.
- Move the app into its own repository with installation and restore tools,
  documentation, light/dark banners, automated checks, and MIT software license.

## 1.0.0 - 2026-10-08

- Create and device-test the offline 4K video screensaver prototype.
- Add signed builds, checksum verification, and reversible screensaver selection.
