# Device verification

Aquarium 4K 1.3.4 verification, October 9, 2026

## Version 1.3.4 release checks

The signed APK uses version name `1.3.4`, version code `8`, and the existing
release signing certificate. Full-screen preview sets `FLAG_KEEP_SCREEN_ON`;
returning to settings or leaving the activity clears it. This keeps the display
awake only while the user is watching the aquarium preview.

| Platform | Environment | Signed release checks |
|---|---|---|
| Fire TV | Physical API 30 TV with 4K panel | In-place signed upgrade, settings launch, full-screen preview, `KEEP_SCREEN_ON` window flag, display remained Awake/ON for 20 seconds, renderer active at 12 FPS |
| Android TV | Official API 31 ARM64 emulator, 1920x1080 | Lifecycle regression test verifies preview sets the keep-screen-on flag and Back clears it |
| Google TV | Official API 34 ARM64 emulator, 1920x1080 | Not retested for this preview lifecycle change |

ADB frame capture confirmed the animated surface was producing frames. The
physical panel could not be independently observed by this capture; the user
reported the TV remained black before this fix. No permission, preference
schema, minimum Android version, or network-access changes.

## Version 1.3.3 release checks

## Version 1.3.3 release checks

The signed APK uses version name `1.3.3`, version code `7`, and the existing
release signing certificate. The settings Activity no longer requests the
screensaver's immersive 4K window mode or runs the animated surface underneath
its controls. Its initial window uses the same dark background as the settings
panel. Preview starts the animation; Menu or Back returns to settings.

| Platform | Environment | Signed release checks |
|---|---|---|
| Android TV | Official API 31 ARM64 emulator, 1920x1080 | Settings opened directly and through the Fire TV UI picker, stable panel, Preview animation, Menu return, no crash |
| Fire TV | Physical API 30 TV with 4K panel | Installed and tested after release. Settings panel and preview launch were checked; the later user report identified the preview display turning black. |
| Google TV | Official API 34 ARM64 emulator, 1920x1080 | Not retested for this settings-only change |

Emulator screenshots confirmed the dark startup window, controls visible before
Preview, the aquarium rendering after Preview, and Menu restoring the settings
panel. The 1.3.3 release has no permission, preference schema, minimum Android
version, or network-access changes.

## Version 1.3.2 release checks

The signed APK uses version name `1.3.2`, version code `6`, and the existing
release signing certificate. The DreamService keeps the Random options
resolved when its display is attached instead of resolving and configuring a
second time when the dream starts.

| Platform | Environment | Signed release checks |
|---|---|---|
| Fire TV | Physical API 30 TV with 4K panel | In-place upgrade, settings controls, randomized 15-second idle startup, active AquariumDreamService, rendered aquarium capture, remote navigation and preview return |
| Android TV | Official API 31 ARM64 emulator, 1920x1080 | Signed APK installation, settings and preview startup, remote navigation, saved options after reopening, preview return |
| Google TV | Official API 34 ARM64 emulator, 1920x1080 | In-place upgrade, settings and preview startup, remote navigation, saved options after reopening, preview return |

Only Fire TV is physical hardware. Android TV and Google TV results are from
emulators; physical models still need hardware testing. No preference schema,
minimum Android version, requested permissions, or network access changed.

## Version 1.3.1 release checks

The signed APK has version name `1.3.1` and version code `5`. Its signing
certificate matches the previous official 1.3.0 release. APK verification
confirmed zero requested permissions, blocked cleartext traffic, system-only
certificate trust, bundled footage, artwork, and executable classes.

| Platform | Environment | Signed release checks |
|---|---|---|
| Fire TV | Physical API 30 TV with 4K panel | In-place upgrade, saved settings, remote navigation, random controls, day/night, reopening, UHD playback, all native appearance checks, native 3840x2160 dream surface, automatic idle activation and Back exit |
| Android TV | Official API 31 ARM64 emulator, 1920x1080 | In-place installation, cold-start saved settings comparison, native remote controls, random controls, day/night, reopening, local HD playback, automatic idle activation and Back exit |
| Google TV | Official API 34 ARM64 emulator, 1920x1080 | In-place upgrade, saved settings, native remote controls, random controls, day/night, reopening, local HD playback, automatic idle activation and Back exit |

All native display check runners passed. The Fire TV appearance runner passed
all looks, backgrounds, species, and sea-life checks. Test runners restored
preferences, screensaver selections, timeout settings, and prior sleep states.
The Android TV comparison was repeated with fresh activity starts so retained
view state did not affect the settings comparison.

Only Fire TV hardware was connected. Physical Android TV and Google TV
verification remains unavailable and is not claimed by these emulator results.
Source CI enforces 100% Java and Python line and branch coverage. The tested
release APK is kept separately from CI's temporary-key validation APKs.

## Local checks

- Twenty Python tooling tests passed, including checksum rejection, shell quoting,
  preservation of original settings, invalid-device rejection, rollback, compiled
  certificate policy checks, and unsafe documentation XML rejection.
- 575,155 Java assertions passed for corrupt settings, population and appearance
  bounds, random choices, shuffle exclusions, fixed-option preservation, day/night brightness, species distribution, school direction and
  spacing, and day-long swim paths.
- Android SDK build succeeded; APK signature verification succeeded.
- Built APK requests zero permissions and stores the video uncompressed.
- Java/Python formatting, ShellCheck, documentation links, Bandit, and Gitleaks
  checks passed for the documentation and CI setup.

## Version 1.3.0 platform checks

| Platform | Environment | Result |
|---|---|---|
| Fire TV | Physical API 30 TV with 4K panel | Remote controls, Random settings and shuffle exclusions, preferences, UHD video playback, native 4K display composition, automatic idle activation and Back exit |
| Android TV | Official API 31 ARM64 emulator, 1920x1080 | Native settings and preview, day/night, random controls and persistence, 1080p rendering and local video playback, automatic idle activation and Back exit |
| Google TV | Official API 34 ARM64 emulator, 1920x1080 | Native settings and preview, day/night, random controls and persistence, 1080p rendering and local video playback, automatic idle activation and Back exit |

Android TV and Google TV are verified on emulators; additional physical TV models
still need hardware testing. Both emulators reported and played muted, looping
1920x1080 footage. Physical Fire TV detection selected UHD. The APK targets SDK
36, installs on both emulators without the older-target Play Protect warning,
and has the same signing certificate as version 1.2.0.

Idle checks temporarily used a 15-second timeout, confirmed the aquarium's active
DreamService and Dreaming power state, and checked the return to Awake after Back.
Screensaver selections, idle timeouts, and stay-awake settings were restored.
Screenshots confirmed rendered aquarium content in both emulator dreams.

The physical Fire TV was temporarily awakened for final screen captures. Its
animated dream used a 3840x2160 SurfaceView buffer, full 3840x2160 source and
physical display frame, and hardware composition. The player reported muted,
looping 3840x2160 footage. Native controls, random preferences, shuffle exclusions,
and reopening passed while the screen was awake. The TV's original asleep state
was restored afterward. The fallback on an actual UHD decoder error is implemented
but was not induced during these checks.

## Fire TV checks

Version 1.2.0, on the connected Android SDK 30 Fire TV:

- All 30 combinations of six looks and five backgrounds rendered with no GL
  errors on a native 3840x2160 offscreen GPU surface.
- Each of six fish species and seven optional sea-life types changed the
  rendered pixels in all three artwork families: 18 species and 21 sea-life
  checks. Captures were visually checked for transparent edges and backgrounds.
- Appearance, background, population, species, and sea-life preferences saved
  and read back correctly, with prior settings restored after the check.
- Day / Night was reached through the activity's remote key dispatch and Right
  selected Night. All six look changes retained the Night selection.
- The brightness control remained enabled for Original 4K footage while
  animated-only controls were disabled. Night and footage selections survived
  closing and reopening the settings activity.
- Actual screen captures showed animated Night brightness at 0.400 of Day in
  a clear-water region. Original footage measured 0.413 across moving frames.
- SHA-256 of the bundled video matches the first installed prototype's video
  exactly: `9e28c6c8b84443a24f28c0e40bd8f50700629d9723d5c105868f82f948f1fccb`.

The separate native checks are documented in [Building](BUILDING.md). They are
compiled in CI; GPU execution and display captures require the physical TV.

The following lifecycle, remote-option, and idle-activation checks were completed
for version 1.1.0 before the appearance and day/night additions:

Fire OS / Android SDK 30, connected 4K panel:

- Every population preset, exact count adjustment, fish species, background,
  speed, size, lighting, bubbles, sunlight shimmer, clock, and sea-life switch
  operated through remote key events and reflected in the settings screen.
- All seven optional sea-life types rendered in the Ocean preview.
- Preferences survived force-stopping and reopening the app.
- Full-screen preview, Back/Menu navigation, reset, and switching between
  animated and real-footage modes worked. Footage-only mode disabled the
  controls for animated fish and scenery, while retaining their saved values.
- Native SurfaceView buffers were 3840x2160, with hardware display composition
  using full 3840x2160 source crop and display frame, despite the 1080p UI.
- DreamService started automatically after a temporary 15-second idle timeout.
  A remote key exited the dream. The prior idle and sleep timeouts were restored.
- Native 3840x2160 hardware-decoded footage played muted and looping.

Settings and footage screenshots are device captures at 1920x1080. Appearance
images are captured from the TV's offscreen 3840x2160 GPU surface and reduced to
1920x1080. SurfaceFlinger verified full-screen aquarium surfaces at native 4K.
Artwork resolution is documented in [Artwork](ARTWORK.md). Frame rate varies by
device and settings; the realistic preview sampled about 16 fps on this TV.

GitHub Actions builds the APK and runs the installer, swimming-path, and
permission checks. CI generates its own temporary signing key for verification.
The downloadable release APK is signed with the stable local application key.
