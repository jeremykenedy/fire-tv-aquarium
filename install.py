#!/usr/bin/env python3
"""Install Aquarium 4K or restore the previous screensaver without losing settings."""

import argparse
import hashlib
import json
import os
import re
import shlex
import shutil
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
COMPONENT = "com.jeremykenedy.firetv.aquarium/.AquariumDreamService"
KEYS = ("screensaver_components", "screensaver_enabled", "screensaver_activate_on_sleep")


def device_serial(value):
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._:-]*", value):
        raise argparse.ArgumentTypeError("Invalid device serial")
    return value


def adb(device, *args):
    device = device_serial(device)
    executable = shutil.which("adb")
    if executable is None:
        raise SystemExit("Install Android platform-tools and add adb to PATH")
    if args and args[0] == "shell":
        args = ("shell", shlex.join(args[1:]))
    return subprocess.check_output([executable, "-s", device, *args], text=True).strip()


def save_original(device, path):
    if path.exists():
        saved = json.loads(path.read_text())
        if saved["device"] != device:
            raise SystemExit("The saved settings belong to a different device.")
        return
    saved = {
        "device": device,
        "settings": {key: adb(device, "shell", "settings", "get", "secure", key) for key in KEYS},
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as file:
        json.dump(saved, file, indent=2)
        file.write("\n")


def set_setting(device, key, value):
    if value == "null":
        adb(device, "shell", "settings", "delete", "secure", key)
    else:
        adb(device, "shell", "settings", "put", "secure", key, value)
    actual = adb(device, "shell", "settings", "get", "secure", key)
    if actual != value:
        raise SystemExit(f"Device did not save {key}: expected {value!r}, received {actual!r}")


def restore(device, path):
    if not path.is_file():
        raise SystemExit("No saved screensaver settings exist for this app.")
    saved = json.loads(path.read_text())
    if saved["device"] != device:
        raise SystemExit("The saved settings belong to a different device.")
    for key in KEYS:
        set_setting(device, key, saved["settings"][key])
    print("Restored the previous screensaver settings. Aquarium remains installed.")


def install(device, path):
    apk = HERE / "build" / "aquarium-4k.apk"
    checksum = HERE / "build" / "aquarium-4k.apk.sha256"
    if not apk.is_file() or not checksum.is_file():
        raise SystemExit("Build first: bash build.sh")
    expected = checksum.read_text().split()[0]
    digest = hashlib.sha256()
    with apk.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != expected:
        raise SystemExit("APK checksum mismatch. Rebuild before installing.")
    save_original(device, path)
    print(adb(device, "install", "--no-incremental", "-r", str(apk)))
    try:
        set_setting(device, "screensaver_components", COMPONENT)
        set_setting(device, "screensaver_enabled", "1")
        set_setting(device, "screensaver_activate_on_sleep", "1")
    except BaseException:
        restore(device, path)
        raise
    print("Aquarium 4K selected. Existing screensaver and sleep timeouts are preserved.")
    print(
        f"Restore: python3 install.py --device {device} "
        f"--state-file {shlex.quote(str(path))} --restore"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--device", required=True, type=device_serial, help="ADB device serial or IP:port"
    )
    parser.add_argument("--restore", action="store_true", help="Restore the original screensaver")
    parser.add_argument(
        "--state-file", type=Path, help="Separate original-settings backup for this TV"
    )
    args = parser.parse_args()
    adb(args.device, "get-state")
    state = args.state_file or HERE / "device-state.json"
    if args.restore:
        restore(args.device, state)
    else:
        install(args.device, state)


if __name__ == "__main__":
    main()
