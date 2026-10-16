#!/usr/bin/env python3
"""Convert the pinned CC0 barramundi asset into the runtime's small mesh format."""

import hashlib
import io
import json
import struct
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "assets/source-models/BarramundiFish.glb"
EXPECTED = "ecc3bafb6b00f2c8b810863c388e3768a7b7ea0d0335e8cb8c574c266e571f4a"
COMMIT = "edc7c9e67c639d230715049ee31f9a96a6babbbe"


def read_scene():
    source = SOURCE.read_bytes()
    if hashlib.sha256(source).hexdigest() != EXPECTED:
        raise ValueError("The source model differs from the pinned upstream asset")
    magic, version, length = struct.unpack_from("<III", source)
    if magic != 0x46546C67 or version != 2 or length != len(source):
        raise ValueError("Invalid glTF binary header")
    json_length, json_type = struct.unpack_from("<II", source, 12)
    if json_type != 0x4E4F534A:
        raise ValueError("Expected the glTF JSON chunk")
    scene = json.loads(source[20 : 20 + json_length])
    binary_offset = 28 + json_length

    def view(index):
        record = scene["bufferViews"][index]
        start = binary_offset + record.get("byteOffset", 0)
        return source[start : start + record["byteLength"]]

    def accessor(index, components, code):
        record = scene["accessors"][index]
        data = view(record["bufferView"])[record.get("byteOffset", 0) :]
        stride = struct.calcsize("<" + code * components)
        return list(struct.iter_unpack("<" + code * components, data[: record["count"] * stride]))

    return scene, view, accessor


def write_mesh(positions, normals, uvs, indices):
    low = [min(point[axis] for point in positions) for axis in range(3)]
    high = [max(point[axis] for point in positions) for axis in range(3)]
    center = [(a + b) * 0.5 for a, b in zip(low, high, strict=True)]
    scale = 2 / (high[2] - low[2])
    output = bytearray(struct.pack("<4sIII", b"FISH", 1, len(positions), len(indices)))
    # The authored fish faces -Z. This proper rotation puts the nose along +X.
    for position, normal, uv in zip(positions, normals, uvs, strict=True):
        x, y, z = [(value - origin) * scale for value, origin in zip(position, center, strict=True)]
        nx, ny, nz = normal
        output.extend(struct.pack("<8f", -z, y, x, -nz, ny, nx, *uv))
    output.extend(struct.pack("<" + "H" * len(indices), *indices))
    directory = ROOT / "assets/models"
    directory.mkdir(parents=True, exist_ok=True)
    mesh_path = directory / "barramundi.bin"
    mesh_path.write_bytes(output)
    return directory, output


def write_texture(scene, view, directory):
    texture_index = scene["materials"][0]["pbrMetallicRoughness"]["baseColorTexture"]["index"]
    image_index = scene["textures"][texture_index]["source"]
    image = Image.open(io.BytesIO(view(scene["images"][image_index]["bufferView"])))
    original_size = image.size
    image.thumbnail((2048, 2048), Image.Resampling.LANCZOS)
    texture_path = directory / "barramundi.png"
    image.save(texture_path, optimize=True)
    return original_size, image.size, texture_path


def export():
    scene, view, accessor = read_scene()
    primitive = scene["meshes"][0]["primitives"][0]
    attrs = primitive["attributes"]
    positions = accessor(attrs["POSITION"], 3, "f")
    normals = accessor(attrs["NORMAL"], 3, "f")
    uvs = accessor(attrs["TEXCOORD_0"], 2, "f")
    indices = [item[0] for item in accessor(primitive["indices"], 1, "H")]
    if len(positions) > 65535 or max(indices) >= len(positions):
        raise ValueError("Mesh exceeds the unsigned-short index contract")
    directory, output = write_mesh(positions, normals, uvs, indices)
    original_size, texture_size, texture_path = write_texture(scene, view, directory)
    metadata = {
        "name": "Barramundi Fish",
        "author": "Microsoft",
        "license": "CC0-1.0",
        "upstream_commit": COMMIT,
        "source": f"https://github.com/KhronosGroup/glTF-Sample-Assets/tree/{COMMIT}/Models/BarramundiFish",
        "source_sha256": EXPECTED,
        "license_file": "../source-models/BarramundiFish-LICENSE.md",
        "transform": (
            "Proper rotation from source -Z nose to +X nose, bbox centered, "
            "total length 2 units; normals and UV preserved"
        ),
        "vertex_count": len(positions),
        "triangle_count": len(indices) // 3,
        "texture_source_dimensions": list(original_size),
        "texture_dimensions": list(texture_size),
        "mesh_sha256": hashlib.sha256(output).hexdigest(),
        "texture_sha256": hashlib.sha256(texture_path.read_bytes()).hexdigest(),
    }
    (directory / "barramundi-source.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    export()
