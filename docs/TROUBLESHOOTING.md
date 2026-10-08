# Troubleshooting

## ADB shows unauthorized or offline

Check the TV's debugging authorization prompt and accept the expected computer.
Confirm both devices are reachable on the same network. Reconnect with
`adb connect DEVICE_IP:5555` and check `adb devices`. Menu names and connection
support depend on the Fire TV model and Fire OS version.

## Checksum mismatch

Download the APK and checksum from the same release, replacing both files in
`build/`, or rebuild locally. The installer stops before contacting the device
for installation when the checksum does not match.

## Signature mismatch during upgrade

Android will reject an upgrade signed with a different key. Use the signed
release APK or restore the original local key and password. Uninstalling the
existing app removes its saved aquarium settings; do this only if you intend
to replace the signing identity.

## Screensaver does not appear in Settings

Some Fire OS versions do not list third-party screensavers. Use the supplied
installer to select AquariumDreamService through ADB. Confirm the saved value:

```bash
adb -s DEVICE_IP:5555 shell settings get secure screensaver_components
```

It should name `com.jeremykenedy.firetv.aquarium/.AquariumDreamService`.
The installer preserves existing idle and sleep timers. If the TV sleeps before
its screensaver interval, inspect its existing settings rather than assuming
this app changed them.

## Controls appear disabled

Fish and environment controls apply to Animated aquarium mode. Switch Mode back
to Animated aquarium. Clock remains available in Real 4K footage mode.

## Animation slows down

Reduce the population or use a smaller preset, then disable optional sea life or
bubbles. Native 4K rendering costs more than a 1080p surface. Frame rate depends
on the Fire TV's GPU, the number of animals, and the selected effects.

## Playback or graphics failure

Read the local app logs:

```bash
adb -s DEVICE_IP:5555 logcat -s Aquarium4K AndroidRuntime
```

For a build, verify the bundled footage is H.264 at 3840x2160, the SDK packages
match the documented versions, and the Java tools are on PATH. For graphics,
verify the device supports OpenGL ES 2.0. Capture the failing options and device
model when reporting a reproducible issue.

## Restore backup belongs to another device

Use the same device identity recorded by this clone's `device-state.json`.
The installer rejects a mismatched serial rather than applying another device's
settings. Maintain separate clones or backups for separate TVs.
