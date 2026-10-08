# Architecture

The app uses Android platform APIs with no external runtime libraries or
services. Its package is `com.jeremykenedy.firetv.aquarium`.

| Component | Responsibility |
|---|---|
| `AquariumActivity` | Remote-operated settings and full-screen preview |
| `AquariumOptions` | Immutable, validated configuration and population presets |
| `AquariumPreferences` | Private on-device settings storage |
| `AquariumDisplay` | Switch between animated and video content; display clock |
| `AquariumSceneView` | Native 4K OpenGL surface and frame requests |
| `AquariumAppearanceRenderer` | Appearance selection, artwork textures, depth layers, fish and sea-life placement |
| `AquariumTextureShader` | Transparent surfaces, tail and arm movement, lighting, sunlight, bubbles |
| `AquariumRenderer` | Original Cartoon camera, environment, fish, bubbles, lighting |
| `AquariumMesh` | Original fish, plant, coral, rock, and tentacle geometry |
| `AquariumShader` | Fish patterns, shading, movement, fog, sunlight highlights |
| `MarineLife` | Optional larger animals and jellyfish |
| `FishMotion` | Independent swimming and coordinated school paths |
| `AquariumPlayback` | Muted, looping platform MediaPlayer video playback |
| `AquariumDreamService` | System-managed idle screensaver lifecycle |
| `install.py` | Checksum verification, installation, activation, restore |

## Lifecycle

The activity starts its display on resume and stops it on pause. The DreamService
starts content when dreaming begins and stops on dreaming end or detachment.
Switching modes releases the old content before constructing its replacement.
The idle dream is non-interactive, allowing the system to exit on a remote key.

Preferences are immutable snapshots passed to the renderer through a volatile
reference. UI changes do not mutate structures while the GL thread reads them.
Meshes and shaders are rebuilt when a GL context is created. Artwork textures
load lazily on the GL thread and are released when the selected family or
background changes. See [Artwork](ARTWORK.md).

## Resolution

Both animated and video surfaces request a fixed 3840x2160 buffer. The window
prefers a supported 4K display mode. Fire TV can render its UI at 1080p while
compositing that separate aquarium surface at 4K. Device verification checks
actual SurfaceFlinger buffer, crop, display frame, and hardware composition;
a manifest declaration alone does not establish native 4K output.

## Offline operation

The manifest declares zero requested permissions. Content is bundled or rendered
locally, preferences stay in private storage, and time comes from the device.
There are no network clients, ad SDKs, analytics SDKs, or remote asset loaders.

## Installer state

The installer records the first observed screensaver settings with mode 0600.
It validates the device identity, preserves the initial backup across upgrades,
quotes shell arguments, verifies changes by reading them back, and attempts
rollback if selecting the new screensaver fails. It does not modify timeouts.
