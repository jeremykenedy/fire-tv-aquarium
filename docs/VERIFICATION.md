# Device verification

Aquarium 4K 1.1.0 verification, October 8, 2026

## Local checks

- Six installer tests passed, including checksum rejection, shell quoting,
  preservation of the original settings, wrong-device rejection, and rollback.
- 491,025 Java assertions passed for corrupt settings, population bounds,
  species distribution, school direction and spacing, and day-long swim paths.
- Android SDK build succeeded; APK signature verification succeeded.
- Built APK requests zero permissions and stores the video uncompressed.
- Java/Python formatting, ShellCheck, documentation links, Bandit, and Gitleaks
  checks passed for the documentation and CI setup.

## Fire TV checks

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

Screenshots are device captures at the Fire TV UI capture resolution of
1920x1080. SurfaceFlinger verified the actual aquarium surface at native 4K.
The animated aquarium is stylized 3D; frame rate varies by device and settings.

GitHub Actions builds the APK and runs the installer, swimming-path, and
permission checks. CI generates its own temporary signing key for verification.
The downloadable release APK is signed with the stable local application key.
