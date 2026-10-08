#!/usr/bin/env python3
"""Check local documentation links, headings, banner XML, and action pins."""

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def anchors(path):
    text = re.sub(r"```.*?```", "", path.read_text(), flags=re.S)
    result = set()
    for heading in re.findall(r"^#{1,6} (.+)$", text, flags=re.M):
        slug = re.sub(r"[^\w\s-]", "", heading.lower()).replace(" ", "-")
        result.add(slug)
    return result


def main():
    checked = 0
    documents = [
        ROOT / name for name in ("README.md", "INSTALLATION.md", "CONTRIBUTING.md", "CHANGELOG.md")
    ]
    documents.extend((ROOT / "docs").glob("*.md"))
    for path in documents:
        text = path.read_text()
        assert "\u2014" not in text and "\u201c" not in text and "\u201d" not in text, path
        targets = re.findall(r"\]\(([^\s)]+)\)", text)
        targets += re.findall(r'(?:src|href|srcset)="([^"]+)"', text)
        for target in targets:
            if target.startswith(("https:", "http:", "mailto:")):
                continue
            name, _, fragment = target.partition("#")
            linked = (path.parent / name).resolve() if name else path
            assert linked.is_relative_to(ROOT) and linked.is_file(), (
                f"{path.name}: missing {target}"
            )
            if fragment:
                assert fragment in anchors(linked), f"{path.name}: missing anchor {target}"
            checked += 1
    readme = (ROOT / "README.md").read_text()
    headings = anchors(ROOT / "README.md") - {"table-of-contents"}
    for heading in headings:
        assert f"](#{heading})" in readme, f"README contents missing {heading}"
    assert readme.rstrip().endswith(
        "This package is open-sourced software licensed under the [MIT license](LICENSE)."
    )
    for mode in ("light", "dark"):
        banner = ET.parse(ROOT / f"art/banner-{mode}.svg").getroot()
        assert banner.get("viewBox") == "0 0 800 200"
    for workflow in (ROOT / ".github/workflows").glob("*.yml"):
        for reference in re.findall(r"uses:\s*([^\s]+)", workflow.read_text()):
            assert re.fullmatch(r"[\w./-]+@[0-9a-f]{40}", reference), (
                f"Unpinned action in {workflow}: {reference}"
            )
    print(f"Documentation verified: {checked} local links, contents, banners, license, action pins")


if __name__ == "__main__":
    main()
