#!/usr/bin/env python3
"""
Generates authentic 16x16 pixel-art textures for flour, bread_dough, rustic_bread,
and renders an isometric preview of manual_grain_mill.
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance
import numpy as np

ROOT = Path("/mnt/dados/dev/Quantum-Industries")
ITEM_TEX_DIR = ROOT / "src/main/resources/assets/quantumindustries/textures/item"
DOCS_ITEMS = ROOT / "docs/images/items"
DOCS_BLOCKS = ROOT / "docs/images/blocks"

def generate_flour():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Sack of milled grain / flour with white/cream flour dusting
    sack_edge = (60, 45, 30, 255)
    sack_shadow = (115, 85, 50, 255)
    sack_mid = (165, 130, 85, 255)
    sack_hi = (205, 175, 125, 255)
    flour_dark = (215, 210, 195, 255)
    flour_mid = (245, 242, 232, 255)
    flour_hi = (255, 255, 250, 255)
    rope = (85, 60, 35, 255)

    # Sack body (3..12 x, 6..14 y)
    for y in range(7, 14):
        for x in range(4, 13):
            img.putpixel((x, y), sack_mid)

    # Outline
    for x in range(5, 12):
        img.putpixel((x, 14), sack_edge)
    for y in range(8, 14):
        img.putpixel((3, y), sack_edge)
        img.putpixel((13, y), sack_edge)

    # Top tied neck
    img.putpixel((6, 5), rope)
    img.putpixel((7, 5), rope)
    img.putpixel((8, 5), rope)
    img.putpixel((9, 5), rope)

    # Top ruffled lip
    img.putpixel((5, 4), sack_shadow)
    img.putpixel((6, 4), sack_mid)
    img.putpixel((7, 4), sack_hi)
    img.putpixel((8, 4), sack_mid)
    img.putpixel((9, 4), sack_hi)
    img.putpixel((10, 4), sack_shadow)

    # Shading and highlights on sack
    for y in range(8, 13):
        img.putpixel((4, y), sack_hi)
        img.putpixel((12, y), sack_shadow)

    # Flour spilling out from top opening
    img.putpixel((7, 3), flour_hi)
    img.putpixel((8, 3), flour_mid)
    img.putpixel((7, 2), flour_hi)

    # Flour dust markings on front of sack
    img.putpixel((6, 8), flour_mid)
    img.putpixel((7, 8), flour_hi)
    img.putpixel((8, 8), flour_mid)
    img.putpixel((7, 9), flour_mid)
    img.putpixel((8, 9), flour_dark)
    img.putpixel((6, 10), flour_dark)

    img.save(ITEM_TEX_DIR / "flour.png")
    # Export 128x128 showcase
    showcase = img.resize((128, 128), Image.NEAREST)
    showcase.save(DOCS_ITEMS / "flour.png")
    print("Created flour.png")

def generate_bread_dough():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Soft, kneaded ball of rustic bread dough
    edge = (100, 80, 50, 255)
    shadow = (150, 125, 80, 255)
    mid = (205, 180, 130, 255)
    light = (235, 215, 165, 255)
    hi = (250, 240, 205, 255)
    flour_dust = (255, 255, 245, 255)

    # Dough ball shape (3..12 x, 5..13 y)
    for y in range(6, 13):
        for x in range(4, 13):
            img.putpixel((x, y), mid)

    # Corners / contour
    contour = [
        (6, 4), (7, 4), (8, 4), (9, 4),
        (5, 5), (10, 5), (11, 5),
        (4, 6), (12, 6),
        (3, 7), (13, 7),
        (3, 8), (13, 8),
        (3, 9), (13, 9),
        (3, 10), (13, 10),
        (4, 11), (12, 11),
        (4, 12), (12, 12),
        (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13), (11, 13)
    ]
    for x, y in contour:
        img.putpixel((x, y), edge)

    # Inner filling
    for y in range(5, 13):
        for x in range(4, 13):
            if (x, y) not in contour:
                img.putpixel((x, y), mid)

    # Top highlight
    for x in range(6, 10):
        img.putpixel((x, 5), light)
    for x in range(5, 11):
        img.putpixel((x, 6), light)
    img.putpixel((7, 6), hi)
    img.putpixel((8, 6), hi)

    # Dough kneading creases / folds
    img.putpixel((6, 8), shadow)
    img.putpixel((7, 8), shadow)
    img.putpixel((8, 9), shadow)
    img.putpixel((9, 9), shadow)
    img.putpixel((10, 8), shadow)

    # Bottom shadow
    for x in range(5, 12):
        img.putpixel((x, 12), shadow)

    # Dusted flour specks
    img.putpixel((6, 7), flour_dust)
    img.putpixel((9, 7), flour_dust)
    img.putpixel((8, 10), flour_dust)

    img.save(ITEM_TEX_DIR / "bread_dough.png")
    showcase = img.resize((128, 128), Image.NEAREST)
    showcase.save(DOCS_ITEMS / "bread_dough.png")
    print("Created bread_dough.png")

def generate_rustic_bread():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Crusty artisanal rustic loaf with diagonal bakery slash cuts
    crust_dark = (75, 40, 15, 255)
    crust_mid = (145, 80, 25, 255)
    crust_baked = (195, 115, 35, 255)
    crust_light = (225, 150, 60, 255)
    slash_crumb = (250, 225, 175, 255)
    flour_dust = (245, 235, 215, 255)

    # Loaf shape (elliptical, 2..13 x, 5..12 y)
    for y in range(6, 12):
        for x in range(3, 14):
            img.putpixel((x, y), crust_baked)

    # Outline
    for x in range(5, 12):
        img.putpixel((x, 4), crust_dark)
        img.putpixel((x, 12), crust_dark)
    for y in range(6, 11):
        img.putpixel((2, y), crust_dark)
        img.putpixel((14, y), crust_dark)
    img.putpixel((3, 5), crust_dark); img.putpixel((4, 5), crust_dark)
    img.putpixel((12, 5), crust_dark); img.putpixel((13, 5), crust_dark)
    img.putpixel((3, 11), crust_dark); img.putpixel((4, 11), crust_dark)
    img.putpixel((12, 11), crust_dark); img.putpixel((13, 11), crust_dark)

    # Top golden crust
    for x in range(5, 12):
        img.putpixel((x, 5), crust_light)

    # Bottom baked shadow
    for x in range(4, 13):
        img.putpixel((x, 11), crust_mid)

    # Artisan baker slash cuts revealing golden-white crumb
    # Slash 1: (5,6) -> (6,8)
    img.putpixel((5, 6), slash_crumb)
    img.putpixel((5, 7), slash_crumb)
    img.putpixel((6, 8), slash_crumb)
    img.putpixel((4, 6), crust_dark)
    img.putpixel((6, 7), crust_dark)

    # Slash 2: (8,6) -> (9,8)
    img.putpixel((8, 6), slash_crumb)
    img.putpixel((8, 7), slash_crumb)
    img.putpixel((9, 8), slash_crumb)
    img.putpixel((7, 6), crust_dark)
    img.putpixel((9, 7), crust_dark)

    # Slash 3: (11,6) -> (12,8)
    img.putpixel((11, 6), slash_crumb)
    img.putpixel((11, 7), slash_crumb)
    img.putpixel((12, 8), slash_crumb)
    img.putpixel((10, 6), crust_dark)

    # Flour dust highlights on upper crust
    img.putpixel((6, 5), flour_dust)
    img.putpixel((9, 5), flour_dust)

    img.save(ITEM_TEX_DIR / "rustic_bread.png")
    showcase = img.resize((128, 128), Image.NEAREST)
    showcase.save(DOCS_ITEMS / "rustic_bread.png")
    print("Created rustic_bread.png")

def render_grain_mill_preview():
    # Render manual_grain_mill isometric block preview
    from scripts.render_all_blocks import render_isometric_block, make_planks_texture, make_stone_brick_texture, make_iron_block_texture
    
    # Dark oak base
    dark_oak = Image.new("RGBA", (16, 16), (55, 38, 22, 255))
    draw = ImageDraw.Draw(dark_oak)
    for y in (3, 7, 11, 15):
        draw.line([(0, y), (15, y)], fill=(35, 24, 14, 255))

    # Millstone top (round stone + iron hopper center)
    mill_stone = Image.new("RGBA", (16, 16), (125, 125, 125, 255))
    draw = ImageDraw.Draw(mill_stone)
    draw.rectangle([0, 0, 15, 15], outline=(90, 90, 90, 255))
    draw.ellipse([3, 3, 12, 12], fill=(160, 160, 160, 255), outline=(75, 75, 75, 255))
    draw.rectangle([6, 6, 9, 9], fill=(45, 45, 50, 255)) # Iron central chute
    # Crank arm handle
    draw.line([(8, 8), (14, 8)], fill=(190, 195, 200, 255))
    draw.point((14, 7), fill=(145, 105, 55, 255)) # Wood handle knob

    iso = render_isometric_block(mill_stone, dark_oak, dark_oak)
    iso.save(DOCS_BLOCKS / "manual_grain_mill.png")
    print("Rendered manual_grain_mill.png")

if __name__ == "__main__":
    generate_flour()
    generate_bread_dough()
    generate_rustic_bread()
    render_grain_mill_preview()
    print("All grain mill assets successfully generated!")
