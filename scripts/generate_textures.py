#!/usr/bin/env python3
"""
Generates authentic 16x16 pixel-art textures for missing Quantum Industries items,
matching the color harmonies and outline geometry of existing era1 items.
"""

from pathlib import Path
from PIL import Image

TEXTURE_DIR = Path("/mnt/dados/dev/Quantum-Industries/src/main/resources/assets/quantumindustries/textures/item")

def generate_gold_sheet():
    ref = Image.open(TEXTURE_DIR / "copper_sheet.png").convert("RGBA")
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            r, g, b, a = ref.getpixel((x, y))
            if a == 0:
                continue
            brightness = (r + g + b) / 3.0
            if brightness < 80:  # Outline
                out.putpixel((x, y), (85, 60, 15, 255))
            elif brightness < 150:  # Dark gold
                out.putpixel((x, y), (210, 155, 30, 255))
            elif brightness < 200:  # Base gold
                out.putpixel((x, y), (245, 195, 45, 255))
            else:  # Highlight
                out.putpixel((x, y), (255, 238, 115, 255))
    out.save(TEXTURE_DIR / "gold_sheet.png")
    print("Created gold_sheet.png")

def generate_gold_wire():
    ref = Image.open(TEXTURE_DIR / "copper_wire.png").convert("RGBA")
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            r, g, b, a = ref.getpixel((x, y))
            if a == 0:
                continue
            brightness = (r + g + b) / 3.0
            if brightness < 100:
                out.putpixel((x, y), (190, 135, 25, 255))
            elif brightness < 150:
                out.putpixel((x, y), (235, 180, 40, 255))
            else:
                out.putpixel((x, y), (255, 230, 105, 255))
    out.save(TEXTURE_DIR / "gold_wire.png")
    print("Created gold_wire.png")

def generate_heated_ingot(output_name, metal_hue="iron"):
    # Standard 16x16 isometric Minecraft ingot geometry
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Outer dark crust
    crust = (110, 25, 10, 255) if metal_hue == "iron" else (100, 30, 15, 255)
    hot_red = (215, 55, 15, 255) if metal_hue == "iron" else (205, 70, 20, 255)
    bright_orange = (255, 135, 25, 255) if metal_hue == "iron" else (245, 145, 30, 255)
    core_glow = (255, 225, 95, 255)

    ingot_pixels = [
        # (x, y, color)
        # Top bevel
        (5, 4, crust), (6, 4, crust), (7, 4, crust), (8, 4, crust), (9, 4, crust), (10, 4, crust),
        (4, 5, crust), (5, 5, hot_red), (6, 5, hot_red), (7, 5, bright_orange), (8, 5, bright_orange), (9, 5, hot_red), (10, 5, crust),
        (3, 6, crust), (4, 6, hot_red), (5, 6, bright_orange), (6, 6, core_glow), (7, 6, core_glow), (8, 6, bright_orange), (9, 6, hot_red), (10, 6, crust),
        (3, 7, crust), (4, 7, hot_red), (5, 7, bright_orange), (6, 7, core_glow), (7, 7, core_glow), (8, 7, bright_orange), (9, 7, hot_red), (10, 7, crust),
        (3, 8, crust), (4, 8, hot_red), (5, 8, bright_orange), (6, 8, core_glow), (7, 8, core_glow), (8, 8, bright_orange), (9, 8, hot_red), (10, 8, crust),
        (4, 9, crust), (5, 9, hot_red), (6, 9, bright_orange), (7, 9, bright_orange), (8, 9, hot_red), (9, 9, crust),
        # Lower body
        (4, 10, crust), (5, 10, hot_red), (6, 10, hot_red), (7, 10, hot_red), (8, 10, hot_red), (9, 10, crust),
        (5, 11, crust), (6, 11, crust), (7, 11, crust), (8, 11, crust),
    ]
    for x, y, col in ingot_pixels:
        out.putpixel((x, y), col)
    out.save(TEXTURE_DIR / output_name)
    print(f"Created {output_name}")

def generate_galvanic_electrolyte():
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glass_edge = (45, 60, 65, 255)
    liquid_dark = (15, 130, 145, 230)
    liquid_bright = (25, 195, 210, 245)
    surface_glow = (75, 240, 255, 255)
    cork = (145, 105, 55, 255)

    # Flask geometry
    # Cork top
    out.putpixel((7, 2), cork)
    out.putpixel((8, 2), cork)
    out.putpixel((7, 3), cork)
    out.putpixel((8, 3), cork)
    # Neck
    out.putpixel((6, 4), glass_edge); out.putpixel((9, 4), glass_edge)
    out.putpixel((6, 5), glass_edge); out.putpixel((9, 5), glass_edge)
    # Body
    for y in range(6, 13):
        out.putpixel((4, y), glass_edge)
        out.putpixel((11, y), glass_edge)
        for x in range(5, 11):
            if y == 6:
                out.putpixel((x, y), surface_glow)
            elif y in (7, 8):
                out.putpixel((x, y), liquid_bright if (x + y) % 2 == 0 else liquid_dark)
            else:
                out.putpixel((x, y), liquid_dark)
    # Bottom
    for x in range(5, 11):
        out.putpixel((x, 13), glass_edge)
    out.save(TEXTURE_DIR / "galvanic_electrolyte.png")
    print("Created galvanic_electrolyte.png")

def generate_pigment_sediment():
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    outline = (40, 30, 50, 255)
    sed_dark = (75, 55, 95, 255)
    sed_mid = (115, 85, 145, 255)
    sed_bright = (155, 120, 190, 255)

    pixels = [
        (6, 6, outline), (7, 6, outline), (8, 6, outline), (9, 6, outline),
        (5, 7, outline), (6, 7, sed_bright), (7, 7, sed_bright), (8, 7, sed_mid), (9, 7, outline),
        (4, 8, outline), (5, 8, sed_bright), (6, 8, sed_mid), (7, 8, sed_mid), (8, 8, sed_dark), (9, 8, outline),
        (4, 9, outline), (5, 9, sed_mid), (6, 9, sed_dark), (7, 9, sed_dark), (8, 9, sed_dark), (9, 9, outline),
        (5, 10, outline), (6, 10, sed_dark), (7, 10, sed_dark), (8, 10, outline),
        (6, 11, outline), (7, 11, outline),
    ]
    for x, y, col in pixels:
        out.putpixel((x, y), col)
    out.save(TEXTURE_DIR / "pigment_sediment.png")
    print("Created pigment_sediment.png")

if __name__ == "__main__":
    generate_gold_sheet()
    generate_gold_wire()
    generate_heated_ingot("heated_iron_ingot.png", "iron")
    generate_heated_ingot("heated_copper_ingot.png", "copper")
    generate_galvanic_electrolyte()
    generate_pigment_sediment()
    print("All textures generated successfully!")
