# Device verification

Aquarium 4K 1.4.0, verified October 22, 2026.

## Release identity

The tested signed APK uses version code 11 and package
`com.jeremykenedy.firetv.aquarium`.

- APK SHA-256: `5e9b482e5238e183f8d8d07eeecd0b13b7cd411948937c39a0dfbc0806ba8eba`.
- Signing certificate SHA-256: `719244d625fa38f30d48e1a14aedfe18ce2d97183743046cceeb2ee04555859a`.
- Stable package, certificate and all 16 stored preferences survive in-place upgrades.
- No requested Android permissions, network access or third-party runtime libraries.

## Physical Fire TV

A physical Fire TV running API 30 rendered all six looks with 60 Large fish,
five marine visitors, Lively speed, bubbles and sunlight enabled. SurfaceFlinger
presentation timestamps measured the real app surface; screen-recording frame
counts and renderer logs were not substituted for presented-frame pacing.

| Appearance | Presented FPS | Median interval, ms |
|---|---|---|
| Realistic | 30.121 | 33.334 |
| Classic Windows aquarium | 30.121 | 33.328 |
| Animated 3D | 30.120 | 33.337 |
| Cartoon | 30.121 | 33.327 |
| Finding Nemo inspired | 30.121 | 33.335 |
| Little Mermaid inspired | 30.240 | 33.335 |

Every sample used an actual 1920x1080 buffer, crop and layer bounds, with an
identity 1x1 transform and DEVICE composition. These results establish native
1080p composition, not native 4K output. The same scene with an unnecessary
3840x2160 buffer had previously failed the minimum 24 FPS acceptance gate.
The release follows its real app window instead of selecting an unused supported
4K panel mode. No model, geometry, texture or motion reduction was needed.

The first normal DreamService session ran input-free for at least 45 seconds.
The system Somnambulator started the selected non-test DreamService manually;
this is not proof of automatic inactivity activation. No idle or sleep timer
was changed. Vendor-specific automatic activation remains a separate device
integration check.

Preview, Menu return, Back return and Home exit passed. Before and after the
checks, the 16 raw aquarium fields, eight relevant system fields, Fire TV UI
protection state and its saved backup matched exactly. The original selected
normal aquarium Dream and Dreaming power state were restored.

The ordinary saved Realistic profile, with 16 fish and no large visitors, was
captured in an 8.057-second recording and two stills eight seconds apart. Stored
Random size and lighting choices remained Random. Review showed independent
spacing, headings and turns, dimensional fish, and no incidental text, watermark
or obvious motion stall. The recording was captured at 1280x720; that encoder
resolution does not replace the surface or pacing measurements above.

Physical logs recorded shark and whale captures. Controlled native GPU tests
also draw the packaged predator and prey meshes at measured mouth contact:
visible prey pixels disappear after capture. Those controlled tests establish
the contact path, not every possible natural encounter. Independent wandering,
pauses, depth, gradual turns, escape and population replenishment have focused
long-running host regressions.

## Native gallery and settings

The same signed APK was upgraded in place on the Google TV API 34 emulator.
The [gallery](GALLERY.md) contains all six ordinary 16-fish looks and the original
video in Day and Night modes, captured at 1920x1080. The current settings panel
capture comes from the physical Fire TV. The Night video capture averages about
40 percent of Day brightness across moving frames. These are actual native
captures, not mockups or maximum-load marketing images.

The Google TV capture run restored all 16 raw Aquarium fields, all 18 Aquarium
Live fields and eight system values exactly, and returned Home. The settings
interface preserves integer and boolean types, latent Random values and shuffle
masks. Invalid or mixed invalid batches are rejected atomically. Physical
cross-TV settings transfer is a separate deployment check.

## Source checks

- Host swimming/settings suite: 1,524,930 checks passed.
- Python installer and tooling tests passed.
- Java coverage: 1,434 lines and 750 branches, zero missed.
- Python coverage: 211 statements and 100 branches, zero missed.
- Style, documentation, Android build, APK policy and signing checks passed.
- Native sunlight comparison covers multiple timestamps, lighting and ray settings.
- The specialized opaque background matched the original GPU pixels in 108 cases.
- Native body and fin tests distinguish opaque body surfaces from transparent fin membranes.

Coverage does not establish physical visual quality or automatic idle behavior.
CI builds use temporary signing keys; the public upgrade uses the tested stable
certificate above.

## Platform limits

Android API 23 and OpenGL ES 2.0 remain the minimum. Android TV API 31 emulator
checks from earlier releases are historical; the final 1.4.0 APK was not retested
on that platform. Physical Android TV and Google TV models still need their own
validation. Native 3840x2160 window and display composition remain unverified.
A panel mode or oversized render buffer alone does not prove that capability.

Previous release results remain in their tagged source history. In particular,
[1.3.6 verification](https://github.com/jeremykenedy/fire-tv-aquarium/blob/v1.3.6/docs/VERIFICATION.md)
also recorded a physical 1920x1080 surface. Earlier claims based on a 3840x2160
buffer should not be read as fresh confirmation of end-to-end native 4K output.
