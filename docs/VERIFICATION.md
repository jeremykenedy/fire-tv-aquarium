# Device verification

Aquarium 4K 1.2.0 verification, October 8, 2026

## Local checks

- Six installer tests passed, including checksum rejection, shell quoting,
  preservation of the original settings, wrong-device rejection, and rollback.
- 491,068 Java assertions passed for corrupt settings, population and appearance
  bounds, day/night brightness, species distribution, school direction and
  spacing, and day-long swim paths.
- Android SDK build succeeded; APK signature verification succeeded.
- Built APK requests zero permissions and stores the video uncompressed.
- Java/Python formatting, ShellCheck, documentation links, Bandit, and Gitleaks
  checks passed for the documentation and CI setup.

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
