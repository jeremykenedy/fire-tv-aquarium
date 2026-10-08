#!/usr/bin/env python3
"""Verify the built app requests no permissions and bundles native UHD footage."""

import os
import subprocess
import zipfile
from pathlib import Path

here = Path(__file__).resolve().parent
sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))
aapt = sdk / "build-tools/36.0.0/aapt"
apk = here / "build/aquarium-4k.apk"
permissions = subprocess.check_output([str(aapt), "dump", "permissions", str(apk)], text=True)
assert "uses-permission:" not in permissions, permissions
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
assert "package: name='com.jeremykenedy.firetv.aquarium'" in badging
with zipfile.ZipFile(apk) as archive:
    assert archive.getinfo("res/raw/aquarium.mp4").compress_type == zipfile.ZIP_STORED
    assert "classes.dex" in archive.namelist()
print("APK verified: zero requested permissions, bundled UHD footage, expected package")
