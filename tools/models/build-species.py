#!/usr/bin/env python3
"""Author closed fish bodies and curved fins using the original atlas as skin."""

import argparse
import hashlib
import json
import math
import struct
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "assets/models"


def build_skins(species):
    cell = species["cell"]
    atlas_path = ROOT / "res/drawable-nodpi" / species.get("atlas", "realistic_fish.png")
    with Image.open(atlas_path) as atlas:
        cell_width = atlas.width / 2
        cell_height = atlas.height / species.get("atlas_rows", 3)
        texture = atlas.crop(
            (
                cell[0] * cell_width,
                cell[1] * cell_height,
                (cell[0] + 1) * cell_width,
                (cell[1] + 1) * cell_height,
            )
        ).resize((512, 512), Image.Resampling.LANCZOS)
    MODELS.mkdir(parents=True, exist_ok=True)
    texture_path = MODELS / (species["name"] + ".png")
    front_skin = species.get("front_skin")

    def save_skin(image, destination, front_name):
        if front_skin:
            with Image.open(ROOT / "assets/skin-sources" / front_name) as front:
                packed = Image.new("RGBA", (1024, 512))
                packed.paste(image, (0, 0))
                packed.paste(front.resize((512, 512), Image.Resampling.LANCZOS), (512, 0))
                image = packed
        image.save(destination, optimize=True)

    save_skin(texture, texture_path, species["name"] + ".png" if not front_skin else front_skin)
    painted_name = "illustrated-marine.png" if "atlas" in species else "illustrated-fish.png"
    painted_path = ROOT / "assets/skin-sources" / painted_name
    if painted_path.exists():
        with Image.open(painted_path) as painted:
            painted_width = painted.width / 2
            painted_height = painted.height / species.get("atlas_rows", 3)
            painted_skin = painted.crop(
                (
                    cell[0] * painted_width,
                    cell[1] * painted_height,
                    (cell[0] + 1) * painted_width,
                    (cell[1] + 1) * painted_height,
                )
            )
            save_skin(
                painted_skin.resize((512, 512), Image.Resampling.LANCZOS),
                MODELS / ("painted-" + species["name"] + ".png"),
                "painted-" + front_skin if front_skin else "",
            )
    return cell, atlas_path, front_skin


def build_body(profile, section, vertex, triangles):
    ring_positions = sorted(
        set(
            [profile[0][0] + (profile[-1][0] - profile[0][0]) * i / 41 for i in range(42)]
            + [point[0] for point in profile]
        )
    )
    rings, sides = len(ring_positions), 28
    for ring in range(rings):
        x = ring_positions[ring]
        top, bottom, radius = section(x)
        for side in range(sides):
            theta = 2 * math.pi * side / sides
            vertex(
                x,
                (top + bottom) * 0.5 - (bottom - top) * 0.5 * math.cos(theta),
                radius * math.sin(theta),
            )
    for ring in range(rings - 1):
        for side in range(sides):
            a = ring * sides + side
            an = ring * sides + (side + 1) % sides
            b = a + sides
            bn = an + sides
            triangles.extend([(a, an, bn), (a, bn, b)])
    for ring, reverse in [(0, True), (rings - 1, False)]:
        x = profile[0 if reverse else -1][0]
        top, bottom, _ = section(x)
        cap = vertex(x, (top + bottom) * 0.5, 0)
        for side in range(sides):
            a = ring * sides + side
            b = ring * sides + (side + 1) % sides
            triangles.append((cap, b, a) if reverse else (cap, a, b))


def fin_root_shifts(fin, outline, section, profile):
    root_shifts = []
    for i, (x, y) in enumerate(outline):
        shift = 0
        if i in fin.get("roots", []):
            top, bottom, _ = section(max(profile[0][0], min(profile[-1][0], x)))
            shift = (top + 10 if fin["kind"] == "dorsal" else bottom - 10) - y
        root_shifts.append(shift)
    center_shift = sum(root_shifts) / len(root_shifts)

    return root_shifts, center_shift


def fin_distance(x, y, fin, section, profile):
    top, bottom, _ = section(max(profile[0][0], min(profile[-1][0], x)))
    if fin["kind"] == "tail":
        return max(0, profile[0][0] - x)
    if fin["kind"] == "dorsal":
        return max(0, top - y)
    if fin["kind"] == "anal":
        return max(0, y - bottom)
    ax, ay = fin["attachment"]
    return math.hypot(x - ax, y - ay)


def append_fin_vertex(fin, vertex, scale, x, y, z, flex_value, skin_y):
    angle = math.radians(fin.get("rotate_x", 0))
    pivot = fin.get("pivot_y", 0)
    sign = -1 if flex_value < 0 or fin["depth"] < 0 else 1
    membrane_flex = sign * (0.0001 + 0.9999 * abs(flex_value))
    return vertex(
        fin.get("pivot_x", 0) + (x - fin.get("pivot_x", 0)) * fin.get("mirror_x", 1),
        pivot + (y - pivot) * math.cos(angle),
        z - (y - pivot) * scale * math.sin(angle) * fin.get("span", 1),
        membrane_flex,
        y if skin_y is None else skin_y,
        x,
    )


def append_membrane_indices(offset, size, steps, triangles, back):
    for edge in range(size):
        a = offset + 1 + edge
        b = offset + 1 + (edge + 1) % size
        triangles.append((offset, b, a) if back else (offset, a, b))
    for radial in range(steps - 1):
        for edge in range(size):
            a = offset + 1 + radial * size + edge
            an = offset + 1 + radial * size + (edge + 1) % size
            b = a + size
            bn = an + size
            triangles.extend([(a, an, b), (an, bn, b)] if back else [(a, b, an), (an, b, bn)])


def build_fin(fin, vertex, vertices, triangles, section, profile, scale):

    def fin_vertex(x, y, z, flex_value=0, skin_y=None):
        return append_fin_vertex(fin, vertex, scale, x, y, z, flex_value, skin_y)

    if "ribbon" in fin:
        build_ribbon(fin, fin_vertex, vertices, triangles)
        return
    outline = fin["outline"]
    cx = sum(p[0] for p in outline) / len(outline)
    cy = sum(p[1] for p in outline) / len(outline)
    root_shifts, center_shift = fin_root_shifts(fin, outline, section, profile)

    def raw_flex(x, y):
        return fin_distance(x, y, fin, section, profile)

    reach = max(1, max(raw_flex(x, y) for x, y in outline))
    sign = -1 if fin["depth"] < 0 else 1

    def flex(x, y, sign=sign, reach=reach, raw=raw_flex):
        return sign * min(1, raw(x, y) / reach)

    for back in (False, True):
        offset = len(vertices)
        thickness = -0.0015 if back else 0.0015
        fin_vertex(
            cx,
            cy + center_shift,
            fin["depth"] + fin["curve"] + thickness,
            flex(cx, cy + center_shift),
            skin_y=cy,
        )
        steps = 4
        for radial in range(1, steps + 1):
            t = radial / steps
            for edge, (x, y) in enumerate(outline):
                skin_y = cy + (y - cy) * t
                attached_y = skin_y + center_shift * (1 - t) + root_shifts[edge] * t
                fin_vertex(
                    cx + (x - cx) * t,
                    attached_y,
                    fin["depth"] + math.cos(t * math.pi * 0.5) * fin["curve"] + thickness,
                    flex(cx + (x - cx) * t, attached_y),
                    skin_y=skin_y,
                )
        append_membrane_indices(offset, len(outline), steps, triangles, back)


def build_ribbon(fin, fin_vertex, vertices, triangles):
    points = fin["ribbon"]
    sign = -1 if fin["depth"] < 0 else 1
    for back in (False, True):
        first = len(vertices)
        for i, (x, y, width) in enumerate(points):
            before = points[max(0, i - 1)]
            after = points[min(len(points) - 1, i + 1)]
            dx, dy = after[0] - before[0], after[1] - before[1]
            length = max(0.001, math.hypot(dx, dy))
            nx, ny = -dy / length, dx / length
            t = i / (len(points) - 1)
            z = fin["depth"] + math.sin(t * math.pi) * fin["curve"]
            z += -0.0015 if back else 0.0015
            for side in (-1, 1):
                fin_vertex(x + side * nx * width, y + side * ny * width, z, sign * t)
        for i in range(len(points) - 1):
            a = first + i * 2
            for triangle in [(a, a + 1, a + 3), (a, a + 3, a + 2)]:
                av, bv, cv = [vertices[j] for j in triangle]
                winding = (bv[0] - av[0]) * (cv[1] - av[1]) - (bv[1] - av[1]) * (cv[0] - av[0])
                if (winding < 0) != back:
                    triangle = (triangle[0], triangle[2], triangle[1])
                triangles.append(triangle)


def encode_mesh(vertices, triangles, front_skin):
    low = [min(v[axis] for v in vertices) for axis in range(3)]
    high = [max(v[axis] for v in vertices) for axis in range(3)]
    factor = 2 / (high[0] - low[0])
    for v in vertices:
        for axis in range(3):
            v[axis] = (v[axis] - (low[axis] + high[axis]) * 0.5) * factor
    normals = [[0.0, 0.0, 0.0] for _ in vertices]
    for indices in triangles:
        a, b, c = [vertices[i] for i in indices]
        u = [b[i] - a[i] for i in range(3)]
        v = [c[i] - a[i] for i in range(3)]
        n = [
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0],
        ]
        for index in indices:
            for axis in range(3):
                normals[index][axis] += n[axis]
    if front_skin:
        for v in vertices:
            v[3] *= 0.5
    output = bytearray(struct.pack("<4sIII", b"FISH", 2, len(vertices), len(triangles) * 3))
    for v, n in zip(vertices, normals, strict=True):
        length = math.sqrt(sum(x * x for x in n))
        n = [x / length for x in n] if length > 1e-10 else [0, 0, 1]
        output.extend(struct.pack("<9f", *v[:3], *n, *v[3:]))
    for triangle in triangles:
        output.extend(struct.pack("<3H", *triangle))
    return low, high, factor, output


def build(species):
    cell, atlas_path, front_skin = build_skins(species)
    x0, y0, x1, y1 = species["bounds"]
    scale = 2 / (x1 - x0)
    center = ((x0 + x1) / 2, (y0 + y1) / 2)
    vertices = []
    triangles = []

    def vertex(x, y, z, flex=0, skin_y=None, skin_x=None):
        index = len(vertices)
        vertices.append(
            [
                (x - center[0]) * scale,
                (center[1] - y) * scale,
                z,
                (x if skin_x is None else skin_x) / 512,
                (y if skin_y is None else skin_y) / 512,
                flex,
            ]
        )
        return index

    profile = species["body"]

    def section(x):
        for a, b in zip(profile[:-1], profile[1:], strict=True):
            if a[0] <= x <= b[0]:
                t = (x - a[0]) / (b[0] - a[0])
                return [a[i] + (b[i] - a[i]) * t for i in range(1, 4)]
        return list(profile[-1][1:])

    build_body(profile, section, vertex, triangles)

    for fin in species["fins"]:
        build_fin(fin, vertex, vertices, triangles, section, profile, scale)
    low, high, factor, output = encode_mesh(vertices, triangles, front_skin)
    (MODELS / (species["name"] + ".bin")).write_bytes(output)
    metadata = {
        "species": species["name"],
        "format": ("FISH v2, stride 36, body finFlex 0; signed membrane finFlex 0.0001 to 1"),
        "geometry": "Original distinct anatomical volume and curved fin surfaces",
        "skin_source": "../../res/drawable-nodpi/" + atlas_path.name,
        "license": "MIT",
        "skin_origin": "Original project atlas",
        "skin_source_commit": "96179f7163dd4dc44478f2ef9d94d679f1e1734c",
        "skin_source_sha256": hashlib.sha256(atlas_path.read_bytes()).hexdigest(),
        "atlas_cell": list(cell),
        "vertices": len(vertices),
        "triangles": len(triangles),
        "mesh_sha256": hashlib.sha256(output).hexdigest(),
        "status": "Native visual prototype pending review",
    }
    if front_skin:
        metadata["frontal_skin"] = "../skin-sources/" + front_skin
        metadata["skin_layout"] = "1024x512: original side skin left, frontal head skin right"
        metadata["frontal_projection"] = {
            "uv": [
                0.5 / (factor * 0.66),
                0.75 + (low[2] + high[2]) * 0.25 / 0.66,
                -1 / (factor * scale * 140),
                (center[1] - (low[1] + high[1]) * 0.5 / scale - 190) / 140,
            ],
            "blend_start": ((240 - center[0]) * scale - (low[0] + high[0]) * 0.5) * factor,
            "blend_end": ((300 - center[0]) * scale - (low[0] + high[0]) * 0.5) * factor,
        }
    if "mouth_uv" in species:
        metadata["mouth_uv"] = species["mouth_uv"]
        nose = profile[-1]
        mouth = [
            (nose[0] - center[0]) * scale,
            (center[1] - (nose[1] + nose[2]) * 0.5) * scale,
            0,
        ]
        metadata["mouth_local"] = [
            (mouth[axis] - (low[axis] + high[axis]) * 0.5) * factor for axis in range(3)
        ]
        metadata["tail_motion"] = (
            "lateral" if species["name"] == "great-white-shark" else "vertical"
        )
    (MODELS / (species["name"] + "-source.json")).write_text(json.dumps(metadata, indent=2) + "\n")
    print(species["name"], len(vertices), "vertices", len(triangles), "triangles")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "profiles", nargs="?", type=Path, default=ROOT / "tools/models/species-profiles.json"
    )
    profiles = json.loads(parser.parse_args().profiles.read_text())
    for species in profiles:
        build(species)
