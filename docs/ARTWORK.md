# Appearance artwork

Aquarium 4K includes three matching artwork families: realistic, cinematic,
and classic drawn. Each family contains a fish atlas, a sea-life atlas, and
Reef, Fish tank, Ocean, Kelp forest, and Deep sea backgrounds.

Artwork is bundled under `res/drawable-nodpi/`. It uses the project's MIT license.
The original Cartoon appearance uses the existing geometry renderer. Recorded
video remains separately licensed in [MEDIA_LICENSE.txt](../MEDIA_LICENSE.txt).

## Rendering

Detailed fish and sea-life images have transparent backgrounds. OpenGL places
them on subdivided swimming surfaces and deforms tails, fins, and flowing arms.
Depth affects size and underwater haze. Animals can swim in either direction;
schools retain their shared formation. This is layered artwork animated by the
app rather than an articulated 3D model for every creature.

Realistic and Classic Windows aquarium use natural artwork. Animated 3D and
Finding Nemo inspired use cinematic 3D artwork; the latter adds a warmer, more
vivid environment palette. Little Mermaid inspired uses drawn animals and
matching painted scenery. Those style names describe the visual direction of
original animals and environments.

Sunlight beams and caustic shimmer are animated separately and can be disabled.
Lighting, bubbles, fish counts, species, and sea-life switches remain independent
of appearance. Only the active background and two active atlases are held as
textures. Switching artwork families releases the previous textures.

## Resolution

The aquarium surface renders at 3840x2160. Background textures are approximately
1672x941, fish atlases are 1024x1536, and sea-life atlases are 887x1774. These
are source artwork dimensions, distinct from the 4K rendering surface. The
bundled recorded video is native 3840x2160. Device screenshots are 1920x1080
because that is the Fire TV's capture resolution.

Textures load without Android density scaling, preserve alpha, and use linear
filtering. Images are center-cropped to fill the display without stretching.
If a device's maximum texture size is smaller than an atlas, decoding samples
it down before upload. The app performs no downloads or remote image requests.

See [Configuration](CONFIGURATION.md) for all choices and
[Verification](VERIFICATION.md) for device results.
