"""Checks for installation integrity, setting rollback, and shell quoting."""

import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "aquarium_install", Path(__file__).with_name("install.py")
)
installer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(installer)


class InstallTests(unittest.TestCase):
    def test_checksum_mismatch_does_not_contact_device(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "build").mkdir()
            (root / "build/aquarium-4k.apk").write_bytes(b"changed APK bytes")
            (root / "build/aquarium-4k.apk.sha256").write_text("0" * 64 + "  aquarium-4k.apk\n")
            with patch.object(installer, "HERE", root), patch.object(installer, "adb") as adb:
                with self.assertRaisesRegex(SystemExit, "checksum mismatch"):
                    installer.install("tv", root / "state.json")
                adb.assert_not_called()
            self.assertFalse((root / "state.json").exists())

    def test_shell_values_are_quoted(self):
        with patch.object(subprocess, "check_output", return_value="1\n") as run:
            installer.adb("tv", "shell", "settings", "put", "secure", "key", "x; echo injected")
            self.assertEqual(
                run.call_args.args[0],
                ["adb", "-s", "tv", "shell", "settings put secure key 'x; echo injected'"],
            )

    def test_backup_preserves_first_observed_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "state.json"
            original = ("previous/.Dream", "0", "null")
            with patch.object(installer, "adb", side_effect=original):
                installer.save_original("tv", path)
            saved_bytes = path.read_bytes()
            with patch.object(installer, "adb") as adb:
                installer.save_original("tv", path)
                adb.assert_not_called()
            self.assertEqual(path.read_bytes(), saved_bytes)
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_restore_deletes_originally_unset_setting(self):
        with patch.object(installer, "adb", side_effect=["", "null"]) as adb:
            installer.set_setting("tv", "screensaver_components", "null")
            self.assertEqual(
                adb.call_args_list[0].args,
                ("tv", "shell", "settings", "delete", "secure", "screensaver_components"),
            )

    def test_wrong_device_restore_does_not_change_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "state.json"
            path.write_text(json.dumps({"device": "other-tv", "settings": {}}))
            with patch.object(installer, "adb") as adb:
                with self.assertRaisesRegex(SystemExit, "different device"):
                    installer.restore("tv", path)
                adb.assert_not_called()

    def test_failed_activation_restores_original_settings(self):
        import hashlib

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "build").mkdir()
            data = b"local build"
            (root / "build/aquarium-4k.apk").write_bytes(data)
            (root / "build/aquarium-4k.apk.sha256").write_text(hashlib.sha256(data).hexdigest())
            failure = RuntimeError("device disconnected")
            with (
                patch.object(installer, "HERE", root),
                patch.object(installer, "save_original"),
                patch.object(installer, "adb", return_value="Success"),
                patch.object(installer, "set_setting", side_effect=[None, failure]),
                patch.object(installer, "restore") as restore,
            ):
                with self.assertRaisesRegex(RuntimeError, "disconnected"):
                    installer.install("tv", root / "state.json")
                restore.assert_called_once_with("tv", root / "state.json")


if __name__ == "__main__":
    unittest.main()
