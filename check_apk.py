#!/usr/bin/env python3
"""Verify the built app requests no permissions and bundles native UHD footage."""

import os
import re
import subprocess
import zipfile
from pathlib import Path

here = Path(__file__).resolve().parent
sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))
aapt = sdk / "build-tools/36.0.0/aapt"
apk = here / "build/aquarium-4k.apk"
permissions = subprocess.check_output([str(aapt), "dump", "permissions", str(apk)], text=True)
if "uses-permission:" in permissions:
    raise SystemExit(f"Unexpected APK permissions: {permissions}")
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
if "package: name='com.jeremykenedy.firetv.aquarium'" not in badging:
    raise SystemExit("Unexpected APK package identity")
if "versionCode='11' versionName='1.4.0'" not in badging:
    raise SystemExit("APK release identity must be 1.4.0 with version code 11")
policy = subprocess.check_output(
    [str(aapt), "dump", "xmltree", str(apk), "res/xml/network_security_config.xml"], text=True
)
if re.findall(r"cleartextTrafficPermitted=\(type 0x12\)(0x[0-9a-f]+)", policy) != ["0x0"]:
    raise SystemExit("APK must forbid cleartext traffic")
if re.findall(r'\bsrc="([^"]+)"', policy) != ["system"]:
    raise SystemExit("APK must use system certificate authorities")
with zipfile.ZipFile(apk) as archive:
    for clip in ("aquarium.mp4", "aquarium_hd.mp4"):
        if archive.getinfo("res/raw/" + clip).compress_type != zipfile.ZIP_STORED:
            raise SystemExit(f"Video must be stored without ZIP compression: {clip}")
    if "classes.dex" not in archive.namelist():
        raise SystemExit("APK is missing executable classes")
    packaged = {Path(name).name for name in archive.namelist()}
    for family in ("realistic", "cinematic", "drawn"):
        for asset in ("reef", "tank", "ocean", "kelp", "deep", "fish", "marine"):
            if f"{family}_{asset}.png" not in packaged:
                raise SystemExit(f"Missing artwork: {family}_{asset}")
    for species in (
        "clownfish",
        "yellow-tang",
        "blue-tang",
        "angelfish",
        "neon-tetra",
        "betta",
        "great-white-shark",
        "humpback-whale",
        "bottlenose-dolphin",
    ):
        for suffix in (".bin", ".png", "-source.json"):
            name = "assets/models/" + species + suffix
            if name not in archive.namelist():
                raise SystemExit(f"Missing marine model asset: {name}")
    for species in (
        "clownfish",
        "yellow-tang",
        "blue-tang",
        "angelfish",
        "neon-tetra",
        "betta",
        "great-white-shark",
        "humpback-whale",
        "bottlenose-dolphin",
    ):
        if "assets/models/painted-" + species + ".png" not in archive.namelist():
            raise SystemExit(f"Missing illustrated marine skin: {species}")
    if "illustrated_marine.png" not in packaged:
        raise SystemExit("Illustrated marine artwork is missing")
    if "assets/licenses/MIT.txt" not in archive.namelist():
        raise SystemExit("Marine artwork license is missing")
    if any(name.startswith("assets/source-models/") for name in archive.namelist()):
        raise SystemExit("Authoring source models must not be bundled")
print("APK verified: offline permissions, UHD footage, artwork, marine models, expected package")
