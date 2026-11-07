#!/usr/bin/env python3
"""
Exports clean, high-resolution showcase graphics for all items and blocks
in Quantum Industries for use in GitHub Markdown documentation.
"""

from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

ROOT = Path("/mnt/dados/dev/Quantum-Industries")
ITEM_SRC = ROOT / "src/main/resources/assets/quantumindustries/textures/item"
DOCS_IMAGES = ROOT / "docs/images"
ITEMS_OUT = DOCS_IMAGES / "items"
BLOCKS_OUT = DOCS_IMAGES / "blocks"

ITEMS_OUT.mkdir(parents=True, exist_ok=True)
BLOCKS_OUT.mkdir(parents=True, exist_ok=True)

# 1. Export crisp 128x128 nearest-neighbor renders for every item
SCALE = 8 # 16x16 -> 128x128

for tex_path in sorted(ITEM_SRC.glob("*.png")):
    if tex_path.name == "era1_materials.png":
        continue
    img = Image.open(tex_path).convert("RGBA")
    # Upscale using nearest neighbor for razor-sharp pixel art
    upscaled = img.resize((img.width * SCALE, img.height * SCALE), Image.NEAREST)
    out_path = ITEMS_OUT / tex_path.name
    upscaled.save(out_path)
    print(f"Exported item: {out_path.name} ({upscaled.size})")

# 2. Block Renders
# Use existing high-quality renders where available
existing_renders = {
    "research_table_tier_1": ROOT / "design/research_table_tier_1-isometrica.png",
    "empirical_assay_bench": ROOT / "design/empirical_assay_bench-preview.png",
}

for name, src in existing_renders.items():
    if src.exists():
        im = Image.open(src).convert("RGBA")
        im.thumbnail((320, 320), Image.LANCZOS)
        out = BLOCKS_OUT / f"{name}.png"
        im.save(out)
        print(f"Exported block (from render): {out.name}")

# Crop sawmill and water wheel from workshop / crafts images if available
workshop_img = ROOT / "docs/design/mechanical/images/workshop.png"
if workshop_img.exists():
    w_im = Image.open(workshop_img).convert("RGBA")
    # Mechanical sawmill is in center-left
    # Water wheel is in background/left
    # Let's save a clean crop or snapshot
    sawmill_crop = w_im.crop((350, 200, 750, 600))
    sawmill_crop.thumbnail((320, 320), Image.LANCZOS)
    sawmill_crop.save(BLOCKS_OUT / "mechanical_sawmill.png")
    print("Exported block: mechanical_sawmill.png")

    wheel_crop = w_im.crop((50, 150, 450, 650))
    wheel_crop.thumbnail((320, 320), Image.LANCZOS)
    wheel_crop.save(BLOCKS_OUT / "primitive_water_wheel.png")
    print("Exported block: primitive_water_wheel.png")

print("Showcase asset generation complete!")
