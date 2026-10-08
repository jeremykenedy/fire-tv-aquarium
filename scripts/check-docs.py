#!/usr/bin/env python3
"""Check local documentation links, headings, banner XML, and action pins."""

import re
from pathlib import Path

from defusedxml import ElementTree

ROOT = Path(__file__).resolve().parent.parent
README = "README.md"


def anchors(path):
    text = re.sub(r"```.*?```", "", path.read_text(), flags=re.S)
    result = set()
    for heading in re.findall(r"^#{1,6} (.+)$", text, flags=re.M):
        slug = re.sub(r"[^\w\s-]", "", heading.lower()).replace(" ", "-")
        result.add(slug)
    return result


def check_links(path):
    text = path.read_text()
    if any(char in text for char in ("\u2014", "\u201c", "\u201d")):
        raise SystemExit(f"Unsupported punctuation in {path}")
    targets = re.findall(r"\]\(([^\s)]+)\)", text)
    targets += re.findall(r'(?:src|href|srcset)="([^"]+)"', text)
    checked = 0
    for target in targets:
        if target.startswith(("https:", "http:", "mailto:")):
            continue
        name, _, fragment = target.partition("#")
        linked = (path.parent / name).resolve() if name else path
        if not linked.is_relative_to(ROOT) or not linked.is_file():
            raise SystemExit(f"{path.name}: missing {target}")
        if fragment and fragment not in anchors(linked):
            raise SystemExit(f"{path.name}: missing anchor {target}")
        checked += 1
    return checked


def check_readme():
    readme = (ROOT / README).read_text()
    headings = anchors(ROOT / README) - {"table-of-contents"}
    for heading in headings:
        if f"](#{heading})" not in readme:
            raise SystemExit(f"README contents missing {heading}")
    if not readme.rstrip().endswith(
        "This package is open-sourced software licensed under the [MIT license](LICENSE)."
    ):
        raise SystemExit("README must end with the MIT license statement")


def check_banners():
    for mode in ("light", "dark"):
        banner = ElementTree.parse(ROOT / f"art/banner-{mode}.svg", forbid_dtd=True).getroot()
        if banner.get("viewBox") != "0 0 800 200":
            raise SystemExit(f"Unexpected {mode} banner dimensions")


def check_actions():
    for workflow in (ROOT / ".github/workflows").glob("*.yml"):
        for reference in re.findall(r"uses:\s*([^\s]+)", workflow.read_text()):
            if not re.fullmatch(r"[\w./-]+@[0-9a-f]{40}", reference):
                raise SystemExit(f"Unpinned action in {workflow}: {reference}")


def main():
    documents = [
        ROOT / name for name in (README, "INSTALLATION.md", "CONTRIBUTING.md", "CHANGELOG.md")
    ]
    documents.extend((ROOT / "docs").glob("*.md"))
    checked = sum(check_links(path) for path in documents)
    check_readme()
    check_banners()
    check_actions()
    print(f"Documentation verified: {checked} local links, contents, banners, license, action pins")


if __name__ == "__main__":
    main()
