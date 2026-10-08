# Frequently asked questions

## Where do I download the app?

Download `aquarium-4k.apk` and `aquarium-4k.apk.sha256` from the
[latest GitHub release](https://github.com/jeremykenedy/fire-tv-aquarium/releases/latest).
The repository is private, so your GitHub account needs access. This project
currently distributes a sideloaded APK. See [Installation](../INSTALLATION.md).

## Does one APK work on all three TV platforms?

Yes. Fire TV, Android TV, and Google TV use the same Java-only APK, Android
platform APIs, D-pad controls, and DreamService. Android API 23 or newer and
OpenGL ES 2.0 are required. Native 4K also requires a suitable physical display.
Vendor restrictions can affect automatic screensaver activation. Physical Fire
TV and emulator Android TV / Google TV results are distinguished in
[Verification](VERIFICATION.md).

## Is the aquarium actually 4K?

On a compatible 4K TV the app requests a native 3840x2160 surface. This has been
verified through the actual buffer and hardware display composition on Fire TV,
even though that TV's UI and screenshots are 1080p. Animation texture sources have
their own resolution; see [Artwork](ARTWORK.md). On a lower-resolution display,
the surface uses the supported physical resolution, capped at 4K.

The original video is 3840x2160. A bundled 1920x1080 copy is used when the display
or hardware decoder does not support UHD, or after a UHD playback failure. Both
copies are local, muted, and looping. The software does not promise a particular
frame rate on every TV.

## Is there any tracking, analytics, or advertising?

No. The APK requests zero permissions and includes no ads, trackers, analytics
libraries, network clients, accounts, or runtime downloads. Preferences and
diagnostic logs stay on the TV through the app. GitHub and development scanners
are repository services and are not included in the installed application.
ADB is used by your computer to install and diagnose the app; the aquarium
does not contact that computer or an external service during playback.

## Can I use it without internet?

Yes. Once installed, animation, footage, settings, and the clock work offline.
Internet is needed on your computer to download a release or build tools. ADB
installation can use USB or a local network connection supported by your TV.

## Can I keep the first real aquarium video?

Choose Mode: Original 4K footage. It preserves the exact original loop used by
the first installed prototype, with a local HD derivative for compatible
fallback. Day / Night and Clock also work in footage mode. Fish count, species,
backgrounds, and sea-life switches customize animation and cannot alter recorded
video.

## Are the cinematic and drawn looks the films themselves?

No. The appearance names describe visual directions. The app uses original
fish and scenery rather than film characters or extracted movie footage. Each
animated look supports all five backgrounds and the same aquarium settings.
See [Configuration](CONFIGURATION.md) and [media licensing](../MEDIA_LICENSE.txt).

## Can every choice be Random?

Every aquarium control offers Random. Mode: Random version chooses among seven
eligible versions: six animated looks and original footage. Its Shuffle switches
control eligibility and cannot all be disabled. Look: Random chooses among all
six animated looks while Mode stays fixed. The Shuffle switches themselves stay
On or Off. Random results are chosen once per showing and can repeat by chance.

Population and exact Fish count are coupled. Random Population uses the five
presets; Random Fish chooses 0-60 and uses Custom. Choosing one clears the other's
Random choice. Sea life is additional to the ordinary fish count.

## Can I always use Night while everything else changes?

Yes. Keep Day / Night on Night and choose Random only for other settings.
Night dims the aquarium and clock to 40% while leaving settings readable. It
does not follow a schedule or change the TV's brightness settings.

## Does upgrading keep my settings?

Yes, when the Android package and signing key match. Official releases retain
the same identity, and the installer upgrades in place. Version 1.3.0 leaves
existing options fixed until you choose Random. Uninstalling or clearing app
data removes aquarium preferences. Keep the separate computer-side screensaver
backup so you can restore the TV's original selection.

## What if my TV hides screensaver selection?

The installer selects the DreamService over ADB and reads its saved values
back. Actual idle startup must still be checked on the TV because vendor
Ambient Mode and power policies differ. Show aquarium provides a full-screen
preview even if the vendor prevents automatic idle startup. See
[Troubleshooting](TROUBLESHOOTING.md#the-screensaver-was-selected-but-does-not-start).

## Can the Star badge automatically star the repository?

The badge opens this repository, where you can click GitHub's Star button while
signed in. A README link cannot perform an authenticated star action on its own.
The Follow badge opens the GitHub profile for the same reason. No access token
is embedded in a badge or the app.

## Is everything MIT licensed?

The software and original animated assets use MIT. The original video and its
HD derivative use the separate Pixabay Content License. Their source and terms
are recorded in [MEDIA_LICENSE.txt](../MEDIA_LICENSE.txt). MIT does not relicense
the footage. No code or assets from the supplied wallpaper APKs are included.
