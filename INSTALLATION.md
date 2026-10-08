# Installation

## Contents

- [Prepare the TV](#prepare-the-tv)
- [Android TV and Google TV](#android-tv-and-google-tv)
- [Multiple TVs](#multiple-tvs)
- [Download and install](#download-and-install)
- [Upgrade](#upgrade)
- [Restore the previous screensaver](#restore-the-previous-screensaver)
- [Uninstall](#uninstall)

## Prepare the TV

Enable ADB debugging in Fire TV Developer options, find the TV's network address,
and connect from a computer with Android Platform Tools installed. Developer
options and their menu location vary by Fire TV model. On models with hidden
developer options, repeatedly select the device name under About to reveal them.

```bash
adb connect DEVICE_IP:5555
adb devices
```

Accept the computer's debugging authorization on the TV. For a USB-connected
device, use its serial in place of `DEVICE_IP:5555` in the commands below.

## Android TV and Google TV

Enable Developer options by selecting the Android TV OS build under About
repeatedly, then enable USB debugging or Wireless debugging. Menu names vary by
vendor. Authorize the computer on the TV. The same APK and installer are used on
all three platforms. No Google account is required by Aquarium.

For Wireless debugging with pairing, use the pairing and connection addresses
shown on the TV; their ports can differ:

```bash
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUG_PORT
python3 install.py --device TV_IP:DEBUG_PORT --state-file device-states/living-room.json
```

For USB, substitute the serial shown by `adb devices`. A TV exposing ordinary
network ADB may use port 5555 instead. Android TV often provides a Screen saver
menu under Device Preferences. Some Google TV versions show only Ambient Mode;
the installer selects Aquarium using ADB and reads back its screensaver settings.
Check actual idle activation on your model, as a vendor can restrict or replace
third-party dreams. Full-screen Show aquarium remains available from the app.

The app uses native 4K surfaces on a supported 4K display. On other displays it
uses the physical display size. Original footage uses a bundled 1080p fallback
when the device lacks a compatible hardware 4K decoder or 4K playback fails.

## Multiple TVs

Use a separate private backup for each TV and keep the same path when upgrading,
restoring, or uninstalling that TV:

```bash
python3 install.py --device FIRST_TV --state-file device-states/first-tv.json
python3 install.py --device SECOND_TV --state-file device-states/second-tv.json
python3 install.py --device SECOND_TV --state-file device-states/second-tv.json --restore
```

The directory is ignored by Git, and backups have mode 0600. Without --state-file,
the original device-state.json is used for compatibility. A backup belonging to
a different device is rejected before installation or restoration.

## Download and install

The repository and releases are private, so authenticate the GitHub CLI with an
account that can access them. The installer uses Python 3.10 or newer.

```bash
git clone git@github.com:jeremykenedy/fire-tv-aquarium.git
cd fire-tv-aquarium
mkdir -p build
gh release download --repo jeremykenedy/fire-tv-aquarium \
  --pattern aquarium-4k.apk --pattern aquarium-4k.apk.sha256 --dir build
python3 install.py --device DEVICE_IP:5555
```

You can also download both files from the latest release page and place them
in `build/`. The installer validates the APK checksum before installation.

It records the first observed values of `screensaver_components`,
`screensaver_enabled`, and `screensaver_activate_on_sleep` in `device-state.json`,
then installs the APK and selects `AquariumDreamService`. If activation fails,
it attempts to restore those original settings. Existing idle and sleep
timeouts are preserved. Keep the backup private and retain it for restoration.

Already-installed copies are upgraded with `adb install -r`. App preferences
are retained when the package and signing key match. A successful install opens
no settings automatically; launch Aquarium 4K from the TV's app list.

## Upgrade

Download the newest APK and checksum into the same clone:

```bash
gh release download --repo jeremykenedy/fire-tv-aquarium \
  --pattern aquarium-4k.apk --pattern aquarium-4k.apk.sha256 --dir build --clobber
python3 install.py --device DEVICE_IP:5555
```

The existing backup remains the restore baseline. Do not replace the signing key
when building your own upgrades. See [Building](docs/BUILDING.md).

## Restore the previous screensaver

```bash
python3 install.py --device DEVICE_IP:5555 --restore
```

Restoration uses the original recorded settings, including deleting values that
were originally unset. It leaves Aquarium installed and preserves preferences.
The saved device identity must match the device supplied to the installer.

## Uninstall

Restore first, then remove the app:

```bash
python3 install.py --device DEVICE_IP:5555 --restore
adb -s DEVICE_IP:5555 uninstall com.jeremykenedy.firetv.aquarium
```

Uninstalling removes the app's saved aquarium preferences. Avoid removing the
currently selected screensaver before restoring its replacement.
