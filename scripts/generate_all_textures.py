#!/usr/bin/env python3
"""
Generates complete authentic 16x16 pixel-art textures for all items in Quantum Industries,
and exports high-resolution crisp showcase renders (128x128 nearest-neighbor)
for the GitHub documentation catalog.
"""

from pathlib import Path
from PIL import Image

ROOT = Path("/mnt/dados/dev/Quantum-Industries")
ITEM_TEX_DIR = ROOT / "src/main/resources/assets/quantumindustries/textures/item"
SHOWCASE_DIR = ROOT / "docs/images/items"
SHOWCASE_DIR.mkdir(parents=True, exist_ok=True)

def generate_research_data():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Blueprint dossier / research paper
    # Palette
    border = (15, 23, 42, 255)       # Slate 900
    bp_dark = (30, 58, 138, 255)     # Blue 900
    bp_mid = (37, 99, 235, 255)      # Blue 600
    bp_light = (96, 165, 250, 255)   # Blue 400
    cyan_line = (56, 189, 248, 255)  # Sky 400
    core_glow = (224, 242, 254, 255) # Lightest sky
    brass_clip = (217, 119, 6, 255)  # Amber 600
    brass_hi = (251, 191, 36, 255)   # Amber 400

    # Page body (2 to 13 horizontal, 2 to 14 vertical)
    for y in range(2, 14):
        for x in range(3, 13):
            img.putpixel((x, y), bp_mid)

    # Borders
    for x in range(3, 13):
        img.putpixel((x, 2), border)
        img.putpixel((x, 13), border)
    for y in range(2, 14):
        img.putpixel((3, y), border)
        img.putpixel((12, y), border)

    # Shading
    for y in range(3, 13):
        img.putpixel((11, y), bp_dark)
    for x in range(4, 12):
        img.putpixel((x, 12), bp_dark)

    # Brass clip on top-left
    img.putpixel((4, 1), brass_clip)
    img.putpixel((5, 1), brass_hi)
    img.putpixel((4, 2), brass_hi)
    img.putpixel((5, 2), brass_clip)
    img.putpixel((4, 3), brass_clip)

    # Technical schematic drawings on blueprint (circuit / atom lines)
    # Horizontal grid lines
    img.putpixel((5, 5), cyan_line)
    img.putpixel((6, 5), cyan_line)
    img.putpixel((7, 5), cyan_line)
    img.putpixel((8, 5), bp_light)
    img.putpixel((9, 5), cyan_line)
    img.putpixel((10, 5), cyan_line)

    # Central atomic / quantum node
    img.putpixel((7, 7), cyan_line)
    img.putpixel((8, 7), cyan_line)
    img.putpixel((6, 8), cyan_line)
    img.putpixel((7, 8), core_glow)
    img.putpixel((8, 8), core_glow)
    img.putpixel((9, 8), cyan_line)
    img.putpixel((7, 9), cyan_line)
    img.putpixel((8, 9), cyan_line)

    # Lower waveform / formula lines
    img.putpixel((5, 10), cyan_line)
    img.putpixel((6, 11), cyan_line)
    img.putpixel((7, 10), cyan_line)
    img.putpixel((8, 11), cyan_line)
    img.putpixel((9, 10), cyan_line)
    img.putpixel((10, 11), cyan_line)

    img.save(ITEM_TEX_DIR / "research_data.png")
    print("Created research_data.png")

def generate_empirical_field_notes():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Leatherbound laboratory observation notebook
    edge = (35, 20, 10, 255)
    leather_dark = (85, 45, 20, 255)
    leather_mid = (130, 70, 30, 255)
    leather_light = (165, 95, 45, 255)
    parchment = (245, 235, 195, 255)
    ribbon = (195, 35, 35, 255)
    ribbon_shadow = (130, 20, 20, 255)
    gold = (235, 180, 45, 255)

    # Notebook cover (3 to 13 x, 2 to 14 y)
    for y in range(2, 14):
        for x in range(3, 13):
            img.putpixel((x, y), leather_mid)

    # Outlines
    for x in range(3, 13):
        img.putpixel((x, 2), edge)
        img.putpixel((x, 13), edge)
    for y in range(2, 14):
        img.putpixel((3, y), edge)
        img.putpixel((12, y), edge)

    # Spine on left
    for y in range(3, 13):
        img.putpixel((4, y), leather_dark)
        img.putpixel((5, y), leather_light)

    # Parchment page side (right edge peeking)
    for y in range(4, 12):
        img.putpixel((11, y), parchment)

    # Brass corner reinforcements
    img.putpixel((3, 2), gold)
    img.putpixel((4, 2), gold)
    img.putpixel((3, 3), gold)
    img.putpixel((11, 2), gold)
    img.putpixel((12, 2), gold)
    img.putpixel((12, 3), gold)
    img.putpixel((3, 13), gold)
    img.putpixel((4, 13), gold)
    img.putpixel((3, 12), gold)
    img.putpixel((11, 13), gold)
    img.putpixel((12, 13), gold)
    img.putpixel((12, 12), gold)

    # Red ribbon bookmark hanging out
    img.putpixel((8, 2), ribbon_shadow)
    for y in range(3, 14):
        img.putpixel((8, y), ribbon)
    img.putpixel((8, 14), ribbon)
    img.putpixel((9, 14), ribbon)
    img.putpixel((8, 15), ribbon)

    # Scientific embossed symbol on cover (brass flask/atom badge)
    img.putpixel((7, 7), gold)
    img.putpixel((9, 7), gold)
    img.putpixel((7, 8), gold)
    img.putpixel((9, 8), gold)

    img.save(ITEM_TEX_DIR / "empirical_field_notes.png")
    print("Created empirical_field_notes.png")

def generate_primitive_mechanical_core():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Dark iron casing with brass gear & copper bearings
    iron_dark = (35, 40, 45, 255)
    iron_mid = (75, 85, 95, 255)
    iron_light = (130, 145, 160, 255)
    brass_dark = (160, 110, 25, 255)
    brass_mid = (220, 165, 35, 255)
    brass_light = (255, 215, 85, 255)
    copper_core = (205, 95, 35, 255)
    copper_hi = (245, 145, 75, 255)

    # Outer casing frame
    for y in range(2, 14):
        for x in range(2, 14):
            if (x in (2, 13) and y in (2, 13)):
                continue
            img.putpixel((x, y), iron_mid)

    # Outer border
    for x in range(3, 13):
        img.putpixel((x, 2), iron_dark)
        img.putpixel((x, 13), iron_dark)
    for y in range(3, 13):
        img.putpixel((2, y), iron_dark)
        img.putpixel((13, y), iron_dark)

    # Inner bevel
    for x in range(3, 13):
        img.putpixel((x, 3), iron_light)
    for y in range(3, 13):
        img.putpixel((3, y), iron_light)

    # Gear cavity (dark recess)
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), iron_dark)

    # Brass gear teeth (cross and diagonals)
    # Gear teeth at edges
    img.putpixel((7, 4), brass_light); img.putpixel((8, 4), brass_light)
    img.putpixel((7, 11), brass_dark); img.putpixel((8, 11), brass_dark)
    img.putpixel((4, 7), brass_light); img.putpixel((4, 8), brass_mid)
    img.putpixel((11, 7), brass_mid); img.putpixel((11, 8), brass_dark)

    # Diagonal teeth
    img.putpixel((5, 5), brass_light)
    img.putpixel((10, 5), brass_mid)
    img.putpixel((5, 10), brass_mid)
    img.putpixel((10, 10), brass_dark)

    # Gear ring
    for y in range(5, 11):
        for x in range(5, 11):
            if x in (5, 10) or y in (5, 10):
                img.putpixel((x, y), brass_mid)

    # Copper central shaft socket
    img.putpixel((7, 7), copper_hi)
    img.putpixel((8, 7), copper_hi)
    img.putpixel((7, 8), copper_core)
    img.putpixel((8, 8), copper_core)
    # Axle hole
    img.putpixel((7, 8), (20, 20, 25, 255))

    img.save(ITEM_TEX_DIR / "primitive_mechanical_core.png")
    print("Created primitive_mechanical_core.png")

def generate_energy_inspection_goggles():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Steampunk / Industrial inspection goggles
    strap_dark = (50, 25, 15, 255)
    strap_mid = (100, 55, 30, 255)
    brass_dark = (150, 100, 20, 255)
    brass_mid = (215, 160, 35, 255)
    brass_light = (255, 220, 90, 255)
    cyan_dark = (15, 115, 135, 255)
    cyan_bright = (35, 215, 240, 255)
    cyan_glow = (180, 245, 255, 255)

    # Leather strap across forehead
    for x in range(0, 16):
        img.putpixel((x, 6), strap_dark)
        img.putpixel((x, 7), strap_mid)
        img.putpixel((x, 8), strap_dark)

    # Left eye rim (brass circle x: 2..7, y: 5..11)
    # Right eye rim (brass circle x: 8..13, y: 5..11)
    for ox in (2, 8):
        # Outer rim
        for x in range(ox, ox + 6):
            img.putpixel((x, 5), brass_dark)
            img.putpixel((x, 11), brass_dark)
        for y in range(5, 12):
            img.putpixel((ox, y), brass_dark)
            img.putpixel((ox + 5, y), brass_dark)

        # Highlight top rim
        img.putpixel((ox + 1, 5), brass_light)
        img.putpixel((ox + 2, 5), brass_light)
        img.putpixel((ox + 3, 5), brass_light)
        img.putpixel((ox + 4, 5), brass_mid)
        img.putpixel((ox, 6), brass_light)

        # Lens interior (ox+1..ox+4, 6..10)
        for y in range(6, 11):
            for x in range(ox + 1, ox + 5):
                img.putpixel((x, y), cyan_dark)

        # Lens energy glow & glass gleam
        img.putpixel((ox + 1, 6), cyan_glow)
        img.putpixel((ox + 2, 6), cyan_bright)
        img.putpixel((ox + 1, 7), cyan_bright)
        img.putpixel((ox + 2, 8), cyan_bright)
        img.putpixel((ox + 3, 8), cyan_glow)
        img.putpixel((ox + 3, 9), cyan_bright)

    # Bridge between goggles (brass clip at x: 7, 8)
    img.putpixel((7, 7), brass_light)
    img.putpixel((8, 7), brass_mid)
    img.putpixel((7, 8), brass_dark)
    img.putpixel((8, 8), brass_dark)

    img.save(ITEM_TEX_DIR / "energy_inspection_goggles.png")
    print("Created energy_inspection_goggles.png")

if __name__ == "__main__":
    generate_research_data()
    generate_empirical_field_notes()
    generate_primitive_mechanical_core()
    generate_energy_inspection_goggles()
    print("Generated all missing base textures!")
