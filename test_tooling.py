"""Exercise APK packaging and documentation checks using local fixtures."""

import contextlib
import importlib.util
import io
import runpy
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from defusedxml.common import DTDForbidden

ROOT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location(
    "documentation_checks", ROOT / "scripts/check-docs.py"
)
docs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(docs)


class PackagingTests(unittest.TestCase):
    def check(
        self,
        permission="",
        badging="package: name='com.jeremykenedy.firetv.aquarium'",
        missing=None,
        compressed=False,
        missing_dex=False,
        policy='cleartextTrafficPermitted=(type 0x12)0x0 src="system"',
    ):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "fixture.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                if not missing_dex:
                    archive.writestr("classes.dex", b"dex")
                for clip in ("aquarium.mp4", "aquarium_hd.mp4"):
                    archive.writestr(
                        "res/raw/" + clip,
                        b"local clip",
                        compress_type=zipfile.ZIP_DEFLATED if compressed else zipfile.ZIP_STORED,
                    )
                for family in ("realistic", "cinematic", "drawn"):
                    for asset in ("reef", "tank", "ocean", "kelp", "deep", "fish", "marine"):
                        name = f"{family}_{asset}.png"
                        if name != missing:
                            archive.writestr("res/drawable/" + name, b"artwork")
            with (
                zipfile.ZipFile(apk) as archive,
                patch.object(zipfile, "ZipFile", return_value=archive),
                patch("subprocess.check_output", side_effect=[permission, badging, policy]),
                contextlib.redirect_stdout(io.StringIO()),
            ):
                runpy.run_path(str(ROOT / "check_apk.py"))

    def test_valid_offline_package(self):
        self.check()

    def test_network_permission_wrong_identity_or_missing_artwork_fails(self):
        for arguments in (
            {"permission": "uses-permission: android.permission.INTERNET"},
            {"badging": "another.package"},
            {"compressed": True},
            {"missing_dex": True},
            {"policy": 'cleartextTrafficPermitted=(type 0x12)0xffffffff src="system"'},
            {"policy": 'cleartextTrafficPermitted=(type 0x12)0x0 src="user"'},
            {"missing": "drawn_marine.png"},
        ):
            with self.subTest(arguments=arguments), self.assertRaises(SystemExit):
                self.check(**arguments)


class DocumentationTests(unittest.TestCase):
    def test_live_repository_and_script_entry_point(self):
        docs.main()
        runpy.run_path(str(ROOT / "scripts/check-docs.py"), run_name="__main__")

    def fixture(self, root):
        (root / "docs").mkdir()
        (root / "art").mkdir()
        (root / ".github/workflows").mkdir(parents=True)
        (root / "LICENSE").write_text("MIT")
        for name in ("INSTALLATION.md", "CONTRIBUTING.md", "CHANGELOG.md"):
            (root / name).write_text("")
        (root / "README.md").write_text(
            "# Table of Contents\n\n[Section](#section)\n# Section\n"
            "[Guide](docs/guide.md#guide)\n[Web](https://example.com)\n"
            "This package is open-sourced software licensed under the [MIT license](LICENSE).\n"
        )
        (root / "docs/guide.md").write_text("# Guide\n```\n# Ignore fenced heading\n```\n")
        for mode in ("light", "dark"):
            (root / f"art/banner-{mode}.svg").write_text('<svg viewBox="0 0 800 200"/>')
        (root / ".github/workflows/test.yml").write_text("uses: actions/checkout@" + "a" * 40)

    def test_banner_dtd_is_rejected_without_entity_expansion(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            self.fixture(root)
            (root / "art/banner-light.svg").write_text(
                '<!DOCTYPE svg [<!ENTITY payload "expanded">]>'
                '<svg viewBox="0 0 800 200">&payload;</svg>'
            )
            with patch.object(docs, "ROOT", root), self.assertRaises(DTDForbidden):
                docs.check_banners()

    def test_local_links_anchors_license_banners_and_action_pins(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            self.fixture(root)
            with patch.object(docs, "ROOT", root):
                docs.main()
                self.assertEqual(docs.anchors(root / "docs/guide.md"), {"guide"})
                readme = root / "README.md"
                original = readme.read_text()
                for broken in (
                    "[Missing](missing.md)\n",
                    "[Anchor](docs/guide.md#missing)\n",
                    "[Outside](../outside.md)\n",
                    "# No Contents Entry\n",
                    "bad\u2014punctuation\n",
                ):
                    readme.write_text(broken + original)
                    with self.assertRaises(SystemExit):
                        docs.main()
                readme.write_text(original.replace("MIT license", "Different license"))
                with self.assertRaises(SystemExit):
                    docs.main()
                readme.write_text(original)
                (root / "art/banner-dark.svg").write_text('<svg viewBox="0 0 1 1"/>')
                with self.assertRaises(SystemExit):
                    docs.main()
                (root / "art/banner-dark.svg").write_text('<svg viewBox="0 0 800 200"/>')
                (root / ".github/workflows/test.yml").write_text("uses: actions/checkout@main")
                with self.assertRaises(SystemExit):
                    docs.main()
