#!/usr/bin/env python3
"""Rebuild the desktop and mobile aquarium banners and their cache-safe copies."""

import hashlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art"
FONT = "system-ui, -apple-system, 'Segoe UI', sans-serif"
CSS = """<style>
@keyframes swim{0%,100%{transform:translateX(-12px) scaleX(1)}
35%{transform:translateX(16px) scaleX(1)}
45%{transform:translateX(16px) scaleX(-1)}
80%{transform:translateX(-12px) scaleX(-1)}
90%{transform:translateX(-12px) scaleX(1)}}
@keyframes rise{0%{transform:translateY(0);opacity:0}
20%{opacity:.8}
80%{opacity:.6}
100%{transform:translateY(-70px);opacity:0}}
.fish,.bubble{transform-box:fill-box;transform-origin:center}
.fish{animation:swim 6.4s ease-in-out infinite}
.second{animation-duration:8.2s;animation-delay:-3.1s}
.bubble{animation:rise 3.6s linear infinite}
.later{animation-duration:4.4s;animation-delay:-2.2s}
@media(prefers-reduced-motion:reduce){.fish,.bubble{animation:none}}
</style>"""
ILLUSTRATION = """<rect width="174" height="116" rx="14"
 fill="url(#water)"/>
<path d="M35 0 L62 116 M98 0 L123 116 M133 0 L159 116"
 stroke="#60a5fa" stroke-width="12" opacity=".15"/>
<path d="M12 110 Q18 74 9 66 M22 111 Q30 65 23 49 M149 111 Q154 71 166 59"
 fill="none"
 stroke="#4ade80" stroke-width="4"/>
<g transform="translate(65 38)">
<g class="fish">
<path d="M-19 0 L-32 -13 L-32 13 Z"
 fill="#fbbf24"/>
<ellipse rx="23" ry="13"
 fill="#f59e0b"/>
<path d="M-6 -11 L-6 11 M8 -11 L8 11"
 stroke="#fff" stroke-width="5"/>
<circle cx="14" cy="-2" r="2.5"
 fill="#0f172a"/>
</g>
</g>
<g transform="translate(119 78)">
<g class="fish second">
<path d="M-19 0 L-32 -13 L-32 13 Z"
 fill="#a78bfa"/>
<ellipse rx="23" ry="13"
 fill="#60a5fa"/>
<path d="M-8 -11 Q1 -19 10 -10"
 fill="#3b82f6"/>
<circle cx="14" cy="-2" r="2.5"
 fill="#0f172a"/>
</g>
</g>
<circle class="bubble" cx="146" cy="94" r="3"
 fill="none"
 stroke="#c4b5fd" opacity=".7"/>
<circle class="bubble later" cx="137" cy="86" r="4"
 fill="none"
 stroke="#c4b5fd" opacity=".7"/>"""


def banner(dark, mobile, still):
    width, height = (392, 408) if mobile else (800, 200)
    title, subtitle = ("#fff", "#a8a29e") if dark else ("#0f172a", "#64748b")
    background = "url(#background)" if dark else "#fff"
    text_x, title_y, tagline_y = (20, 76, 105) if mobile else (40, 80, 112)
    pills = [
        ("3D fish", 64, "#eff6ff", "#60a5fa", "#1e40af"),
        ("Six looks", 70, "#f5f3ff", "#a78bfa", "#6d28d9"),
        ("Offline", 62, "#f0fdf4", "#4ade80", "#166534"),
        ("No ads", 62, "#fffbeb", "#fbbf24", "#b45309"),
    ]
    x = text_x
    tags = []
    for text, pill_width, light_fill, border, light_text in pills:
        tags.append(
            f'<g transform="translate({x} {128 if mobile else 132})">'
            f'<rect width="{pill_width}" height="22" rx="11" '
            f'fill="{"#1a1a1a" if dark else light_fill}" '
            f'stroke="{border}" stroke-width=".8"/>'
            f'<text x="{pill_width / 2}" y="15" font-size="{9 if mobile else 10}" '
            f'font-weight="500" text-anchor="middle" '
            f'fill="{border if dark else light_text}">{text}</text></g>'
        )
        x += pill_width + 8
    illustration = ILLUSTRATION
    if still:
        illustration = re.sub(r' class="[^"]+"', "", illustration)
    transform = "translate(54 194) scale(1.6)" if mobile else "translate(578 42)"
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}"
 viewBox="0 0 {width} {height}"
 role="img" aria-labelledby="title description">
<title id="title">Aquarium 4K</title>
<desc id="description">
Three-dimensional fish, six appearances, and an offline aquarium for your TV.
</desc>
{"" if still else CSS}
<defs>
<linearGradient id="background" x2="1" y2="1">
<stop stop-color="#0c0a09"/>
<stop offset="1" stop-color="#1c1917"/>
</linearGradient>
<linearGradient id="water" x2="0" y2="1">
<stop stop-color="#3b82f6"/>
<stop offset="1" stop-color="#1e40af"/>
</linearGradient>
</defs>
<rect width="{width}" height="{height}" rx="16" fill="{background}"
 stroke="{"#292524" if dark else "#e5e7eb"}"/>
<g font-family="{FONT}">
<text x="{text_x}" y="{title_y}"
 font-size="{36 if mobile else 40}" font-weight="700"
 letter-spacing="-.5" fill="{title}">Aquarium 4K</text>
<text x="{text_x}" y="{tagline_y}"
 font-size="{13 if mobile else 14}" fill="{subtitle}">A living underwater scene for your TV.</text>
{"".join(tags)}</g>
<g transform="{transform}">{illustration}</g>
</svg>
'''


def main():
    for path in ART.glob("banner-*.*.svg"):
        path.unlink()
    names = {}
    for mobile in (False, True):
        for still in (False, True):
            for dark in (False, True):
                name = (
                    "banner-"
                    + ("mobile-" if mobile else "")
                    + ("still-" if still else "")
                    + ("dark" if dark else "light")
                )
                source = banner(dark, mobile, still)
                (ART / f"{name}.svg").write_text(source)
                hashed = name + "." + hashlib.sha256(source.encode()).hexdigest()[:12] + ".svg"
                (ART / hashed).write_text(source)
                names[name] = hashed
    links = []
    for theme in ("light", "dark"):
        sources = "".join(
            f'<source media="{media}" srcset="art/{names[key]}">'
            for media, key in [
                (
                    "(max-width: 600px) and (prefers-reduced-motion: reduce)",
                    f"banner-mobile-still-{theme}",
                ),
                ("(prefers-reduced-motion: reduce)", f"banner-still-{theme}"),
                ("(max-width: 600px)", f"banner-mobile-{theme}"),
            ]
        )
        links.append(
            f'  <a href="https://github.com/jeremykenedy/fire-tv-aquarium#gh-{theme}-mode-only">'
            f'<picture>{sources}<img src="art/{names[f"banner-{theme}"]}#gh-{theme}-mode-only" '
            'alt="Aquarium 4K" width="800"></picture></a>'
        )
    readme = ROOT / "README.md"
    text = readme.read_text()
    end = text.index("</p>") + len("</p>")
    readme.write_text('<p align="center">\n' + "\n".join(links) + "\n</p>" + text[end:])


if __name__ == "__main__":
    main()
