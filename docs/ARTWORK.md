# Appearance artwork

Aquarium 4K includes three matching artwork families: realistic, cinematic,
and classic drawn. Each family contains a fish atlas, a sea-life atlas, and
Reef, Fish tank, Ocean, Kelp forest, and Deep sea backgrounds.

Artwork is bundled under `res/drawable-nodpi/`. It uses the project's MIT license.
Recorded video remains separately licensed in [MEDIA_LICENSE.txt](../MEDIA_LICENSE.txt).

## Rendering

Six fish species use distinct closed three-dimensional meshes with normals,
texture coordinates, rounded body sections and separate curved fins. They turn
through depth using their actual swimming heading and pitch. A traveling spinal
wave moves the tail, while membrane weights add independent fin flutter. Body
surfaces remain closed around dark mouth artwork; fin membranes retain alpha.
Sharks, humpbacks and dolphins use the same mesh pipeline, with lateral shark
motion and vertical cetacean tail beats. Their measured mouth metadata drives
feeding contact in the same coordinate system as the rendered model.

Octopuses, turtles, rays and jellyfish retain their subdivided animated artwork
surfaces. They are not represented as articulated volume models. All sea-life
switches remain available in every look.

Realistic and Classic Windows aquarium use natural skins and scenery. Animated
3D uses natural skins with cinematic scenery. Cartoon, Finding Nemo inspired,
and Little Mermaid inspired use original painted skins rather than color filters
on the natural textures. Cartoon combines cel shading with drawn scenery;
Finding Nemo inspired uses a warm cinematic environment; Little Mermaid inspired
uses drawn scenery and a softer material palette. These names describe the
visual direction of the project's animals and environments.

Swimming state persists independently for each fish. Paths include gradual turns,
pauses, changes of depth and separation. Loose schools retain room to wander.
Predators alternate cruising and feeding, prey evade nearby hunters, and consumed
fish return gradually from outside the visible area.

Sunlight beams and caustic shimmer animate separately and can be disabled.
Lighting, bubbles, fish counts, species and sea-life switches are independent of
appearance. Only the active background and marine atlas are held as textures.
Geometry buffers remain uploaded; switching between natural and painted styles
replaces the skin textures without rebuilding the meshes.

## Resolution

The animated surface follows its actual app window. The tested Fire TV uses
1920x1080 buffers with identity display composition; native 3840x2160 output
is not established by an advertised panel mode or a larger intermediate buffer.
See [Verification](VERIFICATION.md) for the measured output and pacing.

Background textures are approximately 1672x941, original fish atlases are
1024x1536, and sea-life atlases are 887x1774. Volume models use individual natural
and painted skins. Artwork dimensions differ from the render viewport. The
recorded source video is 3840x2160, with a local 1920x1080 derivative selected for
HD output or decoder fallback. Current documentation captures are 1920x1080.

Textures load without Android density scaling, preserve alpha, and use linear
filtering. Images are center-cropped to fill the display without stretching.
If a device's maximum texture size is smaller than an atlas, decoding samples
it down before upload. The app performs no downloads or remote image requests.

See [Configuration](CONFIGURATION.md) for all choices and
[Verification](VERIFICATION.md) for device results.
