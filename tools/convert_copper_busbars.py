#!/usr/bin/env python3
"""Convert Blockbench .bbmodel files for copper busbar to Minecraft block JSON models."""
import json
from pathlib import Path

ROOT = Path("/mnt/dados/dev/Quantum-Industries")
MODELS_DIR = ROOT / "design/models"
OUT_DIR = ROOT / "src/main/resources/assets/quantumindustries/models/block"

DISPLAY_TRANSFORMS = {
    "gui": {
        "rotation": [30, 225, 0],
        "translation": [0, 0, 0],
        "scale": [0.625, 0.625, 0.625]
    },
    "ground": {
        "rotation": [0, 0, 0],
        "translation": [0, 3, 0],
        "scale": [0.25, 0.25, 0.25]
    },
    "fixed": {
        "rotation": [0, 0, 0],
        "translation": [0, 0, 0],
        "scale": [0.5, 0.5, 0.5]
    },
    "thirdperson_righthand": {
        "rotation": [75, 45, 0],
        "translation": [0, 2.5, 0],
        "scale": [0.375, 0.375, 0.375]
    },
    "thirdperson_lefthand": {
        "rotation": [75, 45, 0],
        "translation": [0, 2.5, 0],
        "scale": [0.375, 0.375, 0.375]
    },
    "firstperson_righthand": {
        "rotation": [0, 45, 0],
        "translation": [0, 0, 0],
        "scale": [0.4, 0.4, 0.4]
    },
    "firstperson_lefthand": {
        "rotation": [0, 225, 0],
        "translation": [0, 0, 0],
        "scale": [0.4, 0.4, 0.4]
    }
}

CONVERSIONS = [
    ("copper_cable_conduit_v2.bbmodel", "copper_busbar.json", True),
    ("copper_cable_elbow_v2.bbmodel", "copper_busbar_elbow.json", False),
    ("copper_cable_tee_v2.bbmodel", "copper_busbar_tee.json", False),
    ("copper_cable_cross_v2.bbmodel", "copper_busbar_cross.json", False),
    ("copper_cable_machine_v2.bbmodel", "copper_busbar_machine.json", False),
]

def clean_num(n):
    f = float(n)
    return int(f) if f.is_integer() else round(f, 4)

def convert_bbmodel(bb_path: Path, out_path: Path, include_display: bool):
    with open(bb_path, "r", encoding="utf-8") as f:
        bb_data = json.load(f)

    elements = []
    for el in bb_data.get("elements", []):
        if el.get("export") is False:
            continue
        from_coord = [clean_num(x) for x in el["from"]]
        to_coord = [clean_num(x) for x in el["to"]]
        faces = {}
        for side, face_data in el.get("faces", {}).items():
            raw_uv = face_data.get("uv", [0, 0, 16, 16])
            # Divide UV by 4.0 because bbmodel resolution is 64x64 while Minecraft uses 0..16
            scaled_uv = [clean_num(u / 4.0) for u in raw_uv]
            face_dict = {
                "uv": scaled_uv,
                "texture": "#0"
            }
            if "rotation" in face_data and face_data["rotation"]:
                face_dict["rotation"] = face_data["rotation"]
            faces[side] = face_dict

        elem_dict = {
            "name": el.get("name", "cube"),
            "from": from_coord,
            "to": to_coord,
            "faces": faces
        }
        elements.append(elem_dict)

    out_data = {
        "textures": {
            "0": "quantumindustries:block/copper_busbar",
            "particle": "quantumindustries:block/copper_busbar"
        },
        "elements": elements
    }
    if include_display:
        out_data["display"] = DISPLAY_TRANSFORMS

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out_data, f, indent=2)
    print(f"Exported {bb_path.name} -> {out_path.name} ({len(elements)} elements)")

def main():
    for bb_file, out_file, inc_disp in CONVERSIONS:
        bb_path = MODELS_DIR / bb_file
        out_path = OUT_DIR / out_file
        convert_bbmodel(bb_path, out_path, inc_disp)

if __name__ == "__main__":
    main()
