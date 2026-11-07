#!/usr/bin/env python3
"""
Renders pixel-perfect 3D isometric block previews for all Quantum Industries blocks
and machines using perspective transformation and face lighting.
"""

from pathlib import Path
from PIL import Image, ImageEnhance, ImageDraw
import numpy as np

ROOT = Path("/mnt/dados/dev/Quantum-Industries")
BLOCKS_OUT = ROOT / "docs/images/blocks"
BLOCKS_OUT.mkdir(parents=True, exist_ok=True)

def find_coeffs(target_quad, source_rect):
    """
    target_quad: [(x0,y0), (x1,y1), (x2,y2), (x3,y3)]
    source_rect: [(u0,v0), (u1,v1), (u2,v2), (u3,v3)]
    """
    matrix = []
    for p_tgt, p_src in zip(target_quad, source_rect):
        matrix.append([p_tgt[0], p_tgt[1], 1, 0, 0, 0, -p_src[0]*p_tgt[0], -p_src[0]*p_tgt[1]])
        matrix.append([0, 0, 0, p_tgt[0], p_tgt[1], 1, -p_src[1]*p_tgt[0], -p_src[1]*p_tgt[1]])
    A = np.matrix(matrix, dtype=float)
    B = np.array(source_rect).reshape(8)
    res = np.dot(np.linalg.inv(A.T * A) * A.T, B)
    return np.array(res).reshape(8)

def render_isometric_block(top_img, left_img, right_img, canvas_size=(160, 160)):
    cw, ch = canvas_size
    canvas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))

    # Cube geometry in canvas coordinates
    # Top face corners:
    p_top_north = (80, 16)
    p_top_east = (140, 50)
    p_top_south = (80, 84)
    p_top_west = (20, 50)

    # Bottom corners:
    p_bot_west = (20, 114)
    p_bot_south = (80, 148)
    p_bot_east = (140, 114)

    src_corners = [(0, 0), (64, 0), (64, 64), (0, 64)]

    # 1. Top Face
    top_resized = top_img.resize((64, 64), Image.NEAREST)
    coeffs_top = find_coeffs([p_top_north, p_top_east, p_top_south, p_top_west], src_corners)
    top_warped = top_resized.transform((cw, ch), Image.PERSPECTIVE, coeffs_top, Image.NEAREST)

    # 2. Left Face (South-West) - shaded 80%
    left_resized = left_img.resize((64, 64), Image.NEAREST)
    left_shaded = ImageEnhance.Brightness(left_resized).enhance(0.80)
    coeffs_left = find_coeffs([p_top_west, p_top_south, p_bot_south, p_bot_west], src_corners)
    left_warped = left_shaded.transform((cw, ch), Image.PERSPECTIVE, coeffs_left, Image.NEAREST)

    # 3. Right Face (South-East) - shaded 60%
    right_resized = right_img.resize((64, 64), Image.NEAREST)
    right_shaded = ImageEnhance.Brightness(right_resized).enhance(0.60)
    coeffs_right = find_coeffs([p_top_south, p_top_east, p_bot_east, p_bot_south], src_corners)
    right_warped = right_shaded.transform((cw, ch), Image.PERSPECTIVE, coeffs_right, Image.NEAREST)

    # Composite:
    canvas = Image.alpha_composite(canvas, left_warped)
    canvas = Image.alpha_composite(canvas, right_warped)
    canvas = Image.alpha_composite(canvas, top_warped)
    return canvas

# Standard material texture generators (16x16)
def make_planks_texture():
    im = Image.new("RGBA", (16, 16), (145, 105, 55, 255))
    draw = ImageDraw.Draw(im)
    for y in (3, 7, 11, 15):
        draw.line([(0, y), (15, y)], fill=(95, 65, 30, 255))
    return im

def make_iron_block_texture():
    im = Image.new("RGBA", (16, 16), (185, 195, 205, 255))
    draw = ImageDraw.Draw(im)
    draw.rectangle([0, 0, 15, 15], outline=(140, 150, 160, 255))
    draw.point((1, 1), fill=(225, 235, 245, 255))
    return im

def make_copper_block_texture():
    im = Image.new("RGBA", (16, 16), (195, 105, 65, 255))
    draw = ImageDraw.Draw(im)
    draw.rectangle([0, 0, 15, 15], outline=(145, 75, 45, 255))
    draw.point((1, 1), fill=(235, 145, 95, 255))
    return im

def make_gold_block_texture():
    im = Image.new("RGBA", (16, 16), (245, 195, 45, 255))
    draw = ImageDraw.Draw(im)
    draw.rectangle([0, 0, 15, 15], outline=(190, 145, 25, 255))
    return im

def make_stone_brick_texture():
    im = Image.new("RGBA", (16, 16), (120, 120, 120, 255))
    draw = ImageDraw.Draw(im)
    draw.line([(0, 7), (15, 7)], fill=(65, 65, 65, 255))
    draw.line([(0, 15), (15, 15)], fill=(65, 65, 65, 255))
    draw.line([(7, 0), (7, 7)], fill=(65, 65, 65, 255))
    draw.line([(11, 8), (11, 15)], fill=(65, 65, 65, 255))
    return im

def make_glass_texture():
    im = Image.new("RGBA", (16, 16), (180, 220, 240, 80))
    draw = ImageDraw.Draw(im)
    draw.rectangle([0, 0, 15, 15], outline=(220, 240, 255, 200))
    draw.line([(3, 3), (7, 7)], fill=(255, 255, 255, 220))
    return im

# Render block library
wood = make_planks_texture()
iron = make_iron_block_texture()
copper = make_copper_block_texture()
gold = make_gold_block_texture()
stone = make_stone_brick_texture()
glass = make_glass_texture()

# Custom machine blocks:
# 1. Forming Bench (Wood base + iron anvil top)
forming_top = iron.copy()
draw = ImageDraw.Draw(forming_top)
draw.rectangle([4, 4, 11, 11], fill=(100, 110, 120, 255))
forming_iso = render_isometric_block(forming_top, wood, wood)
forming_iso.save(BLOCKS_OUT / "forming_bench.png")
print("Rendered forming_bench.png")

# 2. Galvanic Accumulator (Wood casing + copper/gold terminals + lapis cells)
acc_top = wood.copy()
draw = ImageDraw.Draw(acc_top)
draw.rectangle([3, 3, 6, 6], fill=(210, 110, 40, 255)) # Copper terminal
draw.rectangle([9, 3, 12, 6], fill=(245, 200, 45, 255)) # Gold terminal
draw.rectangle([4, 9, 11, 13], fill=(30, 80, 180, 255)) # Lapis cell
acc_iso = render_isometric_block(acc_top, wood, iron)
acc_iso.save(BLOCKS_OUT / "galvanic_accumulator.png")
print("Rendered galvanic_accumulator.png")

# 3. Electromechanical Press (Heavy steel frame + anvil base + piston head)
press_top = iron.copy()
draw = ImageDraw.Draw(press_top)
draw.rectangle([3, 3, 12, 12], fill=(60, 65, 75, 255))
draw.rectangle([5, 5, 10, 10], fill=(200, 120, 40, 255))
press_iso = render_isometric_block(press_top, iron, iron)
press_iso.save(BLOCKS_OUT / "electromechanical_press.png")
print("Rendered electromechanical_press.png")

# 4. Manual Dynamo (Copper coils + iron housing + shaft pivot)
dynamo_tex_path = ROOT / "src/main/resources/assets/quantumindustries/textures/block/dynamo_materials.png"
if dynamo_tex_path.exists():
    d_tex = Image.open(dynamo_tex_path).convert("RGBA")
    dyn_iso = render_isometric_block(copper, d_tex, iron)
else:
    dyn_iso = render_isometric_block(copper, iron, iron)
dyn_iso.save(BLOCKS_OUT / "manual_dynamo.png")
print("Rendered manual_dynamo.png")

# 5. Wire Drawing Bench
wire_iso = render_isometric_block(iron, wood, copper)
wire_iso.save(BLOCKS_OUT / "wire_drawing_bench.png")
print("Rendered wire_drawing_bench.png")

# 6. Bellows Brazier (Stone brick forge + glowing coal fire)
brazier_top = stone.copy()
draw = ImageDraw.Draw(brazier_top)
draw.rectangle([3, 3, 12, 12], fill=(220, 60, 20, 255))
draw.rectangle([5, 5, 10, 10], fill=(255, 180, 40, 255))
brazier_iso = render_isometric_block(brazier_top, stone, iron)
brazier_iso.save(BLOCKS_OUT / "bellows_brazier.png")
print("Rendered bellows_brazier.png")

# 7. Copper Busbar (Heavy copper bar)
busbar_iso = render_isometric_block(copper, copper, copper)
busbar_iso.save(BLOCKS_OUT / "copper_busbar.png")
print("Rendered copper_busbar.png")

# 8. Insulated Conduit (Wool dielectric insulation with copper core)
wool_tex = Image.new("RGBA", (16, 16), (70, 75, 80, 255))
draw = ImageDraw.Draw(wool_tex)
draw.rectangle([5, 5, 10, 10], fill=(210, 120, 45, 255))
conduit_iso = render_isometric_block(wool_tex, wool_tex, wool_tex)
conduit_iso.save(BLOCKS_OUT / "insulated_conduit.png")
print("Rendered insulated_conduit.png")

# 9. Piezo Switch (Quartz crystal sensor with copper terminals)
quartz_tex = Image.new("RGBA", (16, 16), (235, 230, 225, 255))
draw = ImageDraw.Draw(quartz_tex)
draw.rectangle([4, 4, 11, 11], fill=(210, 120, 45, 255))
piezo_iso = render_isometric_block(quartz_tex, iron, copper)
piezo_iso.save(BLOCKS_OUT / "piezo_switch.png")
print("Rendered piezo_switch.png")

# 10. Load Relay
relay_iso = render_isometric_block(iron, copper, iron)
relay_iso.save(BLOCKS_OUT / "load_relay.png")
print("Rendered load_relay.png")

# 11. Mechanical Shaft Block
mech_workshop_tex = ROOT / "src/main/resources/assets/quantumindustries/textures/block/mechanical_workshop.png"
if mech_workshop_tex.exists():
    mw_tex = Image.open(mech_workshop_tex).convert("RGBA")
    shaft_iso = render_isometric_block(iron, mw_tex, wood)
else:
    shaft_iso = render_isometric_block(iron, iron, wood)
shaft_iso.save(BLOCKS_OUT / "mechanical_shaft_block.png")
print("Rendered mechanical_shaft_block.png")

# 12. Fluid Pipe
pipe_top = copper.copy()
draw = ImageDraw.Draw(pipe_top)
draw.rectangle([5, 5, 10, 10], fill=(40, 140, 200, 255)) # Liquid core
pipe_iso = render_isometric_block(pipe_top, copper, copper)
pipe_iso.save(BLOCKS_OUT / "fluid_pipe.png")
print("Rendered fluid_pipe.png")

# 13. Glass Pipe
g_top = glass.copy()
draw = ImageDraw.Draw(g_top)
draw.rectangle([5, 5, 10, 10], fill=(30, 160, 220, 220))
gpipe_iso = render_isometric_block(g_top, glass, glass)
gpipe_iso.save(BLOCKS_OUT / "glass_pipe.png")
print("Rendered glass_pipe.png")

# 14. Pneumatic Tube
pneu_top = glass.copy()
draw = ImageDraw.Draw(pneu_top)
draw.rectangle([0, 0, 15, 15], outline=(195, 105, 65, 255))
pneu_iso = render_isometric_block(pneu_top, glass, copper)
pneu_iso.save(BLOCKS_OUT / "pneumatic_tube.png")
print("Rendered pneumatic_tube.png")

# 15. Bellows Ejector
ej_top = iron.copy()
draw = ImageDraw.Draw(ej_top)
draw.rectangle([4, 4, 11, 11], fill=(195, 105, 65, 255))
ejector_iso = render_isometric_block(ej_top, iron, wood)
ejector_iso.save(BLOCKS_OUT / "bellows_ejector.png")
print("Rendered bellows_ejector.png")

# 16. Gravitational Sorter
sorter_iso = render_isometric_block(wood, iron, wood)
sorter_iso.save(BLOCKS_OUT / "gravitational_sorter.png")
print("Rendered gravitational_sorter.png")

# 17. Peristaltic Pump
pump_top = iron.copy()
draw = ImageDraw.Draw(pump_top)
draw.ellipse([4, 4, 11, 11], fill=(195, 105, 65, 255))
pump_iso = render_isometric_block(pump_top, iron, copper)
pump_iso.save(BLOCKS_OUT / "peristaltic_pump.png")
print("Rendered peristaltic_pump.png")

# 18. Settling Vat (Decantation vat with liquid level)
vat_top = iron.copy()
draw = ImageDraw.Draw(vat_top)
draw.rectangle([2, 2, 13, 13], fill=(35, 60, 130, 255))
vat_iso = render_isometric_block(vat_top, glass, iron)
vat_iso.save(BLOCKS_OUT / "settling_vat.png")
print("Rendered settling_vat.png")

# 19. Scientific Analyzer Tier 2
table_tex_path = ROOT / "src/main/resources/assets/quantumindustries/textures/block/research_table.png"
if table_tex_path.exists():
    rt_tex = Image.open(table_tex_path).convert("RGBA")
    an_iso = render_isometric_block(rt_tex, iron, iron)
else:
    an_iso = render_isometric_block(iron, iron, iron)
an_iso.save(BLOCKS_OUT / "scientific_analyzer.png")
print("Rendered scientific_analyzer.png")

print("All block renders complete!")
