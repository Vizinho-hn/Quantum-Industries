#!/usr/bin/env python3
import colorsys
import json
import os
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DATA = ROOT / "src/main/resources/data/quantumindustries"
COMMON_TAGS = ROOT / "src/main/resources/data/c/tags/item"

ITEM_TEX_DIR = ASSETS / "textures/item"
BLOCK_TEX_DIR = ASSETS / "textures/block"
BLOCKSTATES_DIR = ASSETS / "blockstates"
BLOCK_MODELS_DIR = ASSETS / "models/block"
ITEM_MODELS_DIR = ASSETS / "models/item"
ITEMS_DEF_DIR = ASSETS / "items"
LOOT_DIR = DATA / "loot_table/blocks"
RECIPE_DIR = DATA / "recipe"

for d in (ITEM_TEX_DIR, BLOCK_TEX_DIR, BLOCKSTATES_DIR, BLOCK_MODELS_DIR, ITEM_MODELS_DIR, ITEMS_DEF_DIR, LOOT_DIR, RECIPE_DIR):
    d.mkdir(parents=True, exist_ok=True)

ALLOYS = [
    ("bronze", "Bronze", "Bronze", "CD7F32"),
    ("brass", "Latão", "Brass", "D4AF37"),
    ("invar", "Invar", "Invar", "9FA8A3"),
    ("electrum", "Electrum", "Electrum", "E6C65B"),
    ("constantan", "Constantan", "Constantan", "C87D65"),
    ("steel", "Aço", "Steel", "626870"),
]

EXTRA_SHEETS = [
    ("lead", "Chumbo", "Lead", "4A4E69"),
    ("tin", "Estanho", "Tin", "D0DEE4"),
    ("zinc", "Zinco", "Zinc", "8DA2B0"),
]


def write_json(path: Path, data: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def read_rgba_png(path: Path):
    raw = path.read_bytes()
    pos = 8
    width = height = bit_depth = color_type = 0
    idat = bytearray()
    palette = []
    trns = b""
    while pos < len(raw):
        length = struct.unpack(">I", raw[pos:pos+4])[0]
        ctype = raw[pos+4:pos+8]
        chunk = raw[pos+8:pos+8+length]
        pos += 12 + length
        if ctype == b"IHDR":
            width, height, bit_depth, color_type, _, _, _ = struct.unpack(">IIBBBBB", chunk)
        elif ctype == b"PLTE":
            palette = [(chunk[i], chunk[i+1], chunk[i+2]) for i in range(0, len(chunk), 3)]
        elif ctype == b"tRNS":
            trns = chunk
        elif ctype == b"IDAT":
            idat.extend(chunk)
        elif ctype == b"IEND":
            break
    decompressed = zlib.decompress(bytes(idat))
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    stride = width * bpp
    pixels = []
    prev_row = bytearray(stride)
    idx = 0
    for y in range(height):
        ftype = decompressed[idx]
        idx += 1
        row = bytearray(decompressed[idx:idx+stride])
        idx += stride
        if ftype == 1:
            for i in range(bpp, stride):
                row[i] = (row[i] + row[i - bpp]) & 0xFF
        elif ftype == 2:
            for i in range(stride):
                row[i] = (row[i] + prev_row[i]) & 0xFF
        elif ftype == 3:
            for i in range(stride):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + ((left + prev_row[i]) >> 1)) & 0xFF
        elif ftype == 4:
            for i in range(stride):
                a = row[i - bpp] if i >= bpp else 0
                b = prev_row[i]
                c = prev_row[i - bpp] if i >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                row[i] = (row[i] + pr) & 0xFF
        prev_row = row
        row_px = []
        for x in range(width):
            if color_type == 6:
                r, g, b, a = row[x*4:x*4+4]
            elif color_type == 2:
                r, g, b = row[x*3:x*3+3]
                a = 255
            elif color_type == 3:
                pi = row[x]
                r, g, b = palette[pi]
                a = trns[pi] if pi < len(trns) else 255
            elif color_type == 4:
                r = g = b = row[x*2]
                a = row[x*2+1]
            else:
                r = g = b = row[x]
                a = 255
            row_px.append((r, g, b, a))
        pixels.append(row_px)
    return width, height, pixels


def write_rgba_png(path: Path, width: int, height: int, pixels):
    raw_rows = bytearray()
    for row in pixels:
        raw_rows.append(0)
        for r, g, b, a in row:
            raw_rows.extend((r, g, b, a))

    def make_chunk(ctype: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(ctype + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + ctype + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_rows), 9)
    png = b"\x89PNG\r\n\x1a\n" + make_chunk(b"IHDR", ihdr) + make_chunk(b"IDAT", idat) + make_chunk(b"IEND", b"")
    path.write_bytes(png)


def save_tinted(template_path: Path, hex_color: str, out_path: Path):
    width, height, pixels = read_rgba_png(template_path)
    r_t = int(hex_color[0:2], 16) / 255.0
    g_t = int(hex_color[2:4], 16) / 255.0
    b_t = int(hex_color[4:6], 16) / 255.0
    h_t, s_t, v_t = colorsys.rgb_to_hsv(r_t, g_t, b_t)

    out_pixels = []
    for y in range(height):
        out_row = []
        for x in range(width):
            r, g, b, a = pixels[y][x]
            if a == 0:
                out_row.append((0, 0, 0, 0))
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            v = max(0.0, min(1.0, pow(lum, 0.9) * (0.55 + 0.65 * v_t)))
            s = max(0.0, min(1.0, s_t * (1.15 - 0.45 * lum)))
            nr, ng, nb = colorsys.hsv_to_rgb(h_t, s, v)
            out_row.append((int(round(nr * 255)), int(round(ng * 255)), int(round(nb * 255)), a))
        out_pixels.append(out_row)
    write_rgba_png(out_path, width, height, out_pixels)


# 1. Generate textures for alloy ingots, nuggets, sheets, extra sheets, and bronze_gear
ingot_template = ITEM_TEX_DIR / "tin_ingot.png"
nugget_template = ITEM_TEX_DIR / "tin_nugget.png"
sheet_template = ITEM_TEX_DIR / "iron_sheet.png"
gear_template = ITEM_TEX_DIR / "iron_gear.png"

for alloy_id, _, _, hex_col in ALLOYS:
    save_tinted(ingot_template, hex_col, ITEM_TEX_DIR / f"{alloy_id}_ingot.png")
    save_tinted(nugget_template, hex_col, ITEM_TEX_DIR / f"{alloy_id}_nugget.png")
    save_tinted(sheet_template, hex_col, ITEM_TEX_DIR / f"{alloy_id}_sheet.png")

for metal_id, _, _, hex_col in EXTRA_SHEETS:
    save_tinted(sheet_template, hex_col, ITEM_TEX_DIR / f"{metal_id}_sheet.png")

save_tinted(gear_template, "CD7F32", ITEM_TEX_DIR / "bronze_gear.png")

# 2. Generate Item Models & Item Definitions
def register_flat_item(item_id: str):
    write_json(ITEM_MODELS_DIR / f"{item_id}.json", {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"quantumindustries:item/{item_id}"
        }
    })
    write_json(ITEMS_DEF_DIR / f"{item_id}.json", {
        "model": {
            "type": "minecraft:model",
            "model": f"quantumindustries:item/{item_id}"
        }
    })


for alloy_id, _, _, _ in ALLOYS:
    register_flat_item(f"{alloy_id}_ingot")
    register_flat_item(f"{alloy_id}_nugget")
    register_flat_item(f"{alloy_id}_sheet")

    # Ingot <-> Nugget recipes
    write_json(RECIPE_DIR / f"{alloy_id}_ingot_from_nuggets.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [f"quantumindustries:{alloy_id}_nugget"] * 9,
        "result": {
            "id": f"quantumindustries:{alloy_id}_ingot",
            "count": 1
        }
    })
    write_json(RECIPE_DIR / f"{alloy_id}_nugget_from_ingot.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [f"quantumindustries:{alloy_id}_ingot"],
        "result": {
            "id": f"quantumindustries:{alloy_id}_nugget",
            "count": 9
        }
    })

    # Common tags
    write_json(COMMON_TAGS / f"ingots/{alloy_id}.json", {
        "values": [f"quantumindustries:{alloy_id}_ingot"]
    })
    write_json(COMMON_TAGS / f"nuggets/{alloy_id}.json", {
        "values": [f"quantumindustries:{alloy_id}_nugget"]
    })
    write_json(COMMON_TAGS / f"plates/{alloy_id}.json", {
        "values": [f"quantumindustries:{alloy_id}_sheet"]
    })

for metal_id, _, _, _ in EXTRA_SHEETS:
    register_flat_item(f"{metal_id}_sheet")
    write_json(COMMON_TAGS / f"plates/{metal_id}.json", {
        "values": [f"quantumindustries:{metal_id}_sheet"]
    })

register_flat_item("bronze_gear")
write_json(RECIPE_DIR / "bronze_gear.json", {
    "type": "quantumindustries:foundations",
    "craft": "bronze_gear"
})

# 2B. Generate Metal Dusts, Heated Alloy Ingots, Alloy Wires, Coils, and Chemical Reagents
METAL_DUSTS = [
    ("copper", "Cobre", "Copper", "D77A42", "minecraft:copper_ingot"),
    ("iron", "Ferro", "Iron", "B8BFC0", "minecraft:iron_ingot"),
    ("gold", "Ouro", "Gold", "ECC34C", "minecraft:gold_ingot"),
    ("tin", "Estanho", "Tin", "D0DCE5", "quantumindustries:tin_ingot"),
    ("zinc", "Zinco", "Zinc", "A8B8B0", "quantumindustries:zinc_ingot"),
    ("lead", "Chumbo", "Lead", "5C6274", "quantumindustries:lead_ingot"),
    ("nickel", "Níquel", "Nickel", "C2BA9B", "quantumindustries:nickel_ingot"),
    ("silver", "Prata", "Silver", "DCE6EE", "quantumindustries:silver_ingot"),
]

dust_template = ITEM_TEX_DIR / "sulfur_dust.png"
heated_template = ITEM_TEX_DIR / "heated_copper_ingot.png"
wire_template = ITEM_TEX_DIR / "gold_wire.png"
coil_template = ITEM_TEX_DIR / "copper_coil.png"
bottle_template = ITEM_TEX_DIR / "galvanic_electrolyte.png"

for dust_id, _, _, hex_col, smelted_result in METAL_DUSTS:
    item_name = f"{dust_id}_dust"
    save_tinted(dust_template, hex_col, ITEM_TEX_DIR / f"{item_name}.png")
    register_flat_item(item_name)
    write_json(COMMON_TAGS / f"dusts/{dust_id}.json", {
        "values": [f"quantumindustries:{item_name}"]
    })
    write_json(RECIPE_DIR / f"{dust_id}_ingot_from_smelting_{item_name}.json", {
        "type": "minecraft:smelting",
        "category": "misc",
        "cookingtime": 200,
        "experience": 0.7,
        "ingredient": f"quantumindustries:{item_name}",
        "result": {"id": smelted_result, "count": 1}
    })
    write_json(RECIPE_DIR / f"{dust_id}_ingot_from_blasting_{item_name}.json", {
        "type": "minecraft:blasting",
        "category": "misc",
        "cookingtime": 100,
        "experience": 0.7,
        "ingredient": f"quantumindustries:{item_name}",
        "result": {"id": smelted_result, "count": 1}
    })

# Heated alloy ingots
for heated_id, hex_col in [
    ("heated_bronze_ingot", "FF9238"),
    ("heated_brass_ingot", "FFB845"),
    ("heated_steel_ingot", "FF6A3D"),
]:
    save_tinted(heated_template, hex_col, ITEM_TEX_DIR / f"{heated_id}.png")
    register_flat_item(heated_id)

# Alloy wires, coil, reagents
save_tinted(wire_template, "F5D76E", ITEM_TEX_DIR / "electrum_wire.png")
register_flat_item("electrum_wire")

save_tinted(wire_template, "D28268", ITEM_TEX_DIR / "constantan_wire.png")
register_flat_item("constantan_wire")

save_tinted(coil_template, "D28268", ITEM_TEX_DIR / "constantan_coil.png")
register_flat_item("constantan_coil")

save_tinted(bottle_template, "7CE8C8", ITEM_TEX_DIR / "hydrochloric_acid.png")
register_flat_item("hydrochloric_acid")

save_tinted(ingot_template, "3E424B", ITEM_TEX_DIR / "refractory_brick.png")
register_flat_item("refractory_brick")

save_tinted(dust_template, "9BE564", ITEM_TEX_DIR / "phosphate_fertilizer.png")
register_flat_item("phosphate_fertilizer")

for foundation_craft in ("constantan_coil", "refractory_brick", "phosphate_fertilizer"):
    write_json(RECIPE_DIR / f"{foundation_craft}.json", {
        "type": "quantumindustries:foundations",
        "craft": foundation_craft
    })

# 3. Blocks: bellows_alloy_kiln, electric_alloy_smelter & jaw_crusher
bellows_kiln_model = {
    "textures": {
        "bricks": "minecraft:block/bricks",
        "stone": "minecraft:block/stone_bricks",
        "copper": "minecraft:block/copper_block",
        "fire": "minecraft:block/magma",
        "iron": "minecraft:block/iron_block",
        "particle": "minecraft:block/bricks"
    },
    "elements": [
        {
            "from": [1, 0, 1],
            "to": [15, 6, 15],
            "faces": {
                "north": {"texture": "#stone"},
                "south": {"texture": "#stone"},
                "east": {"texture": "#stone"},
                "west": {"texture": "#stone"},
                "up": {"texture": "#fire"},
                "down": {"texture": "#stone"}
            }
        },
        {
            "from": [2, 6, 2],
            "to": [14, 14, 14],
            "faces": {
                "north": {"texture": "#bricks"},
                "south": {"texture": "#bricks"},
                "east": {"texture": "#bricks"},
                "west": {"texture": "#bricks"},
                "up": {"texture": "#copper"},
                "down": {"texture": "#bricks"}
            }
        },
        {
            "from": [4, 14, 4],
            "to": [12, 16, 12],
            "faces": {
                "north": {"texture": "#iron"},
                "south": {"texture": "#iron"},
                "east": {"texture": "#iron"},
                "west": {"texture": "#iron"},
                "up": {"texture": "#fire"},
                "down": {"texture": "#iron"}
            }
        }
    ]
}

electric_smelter_model = {
    "textures": {
        "frame": "minecraft:block/iron_block",
        "coil": "minecraft:block/copper_block",
        "core": "minecraft:block/blast_furnace_front_on",
        "side": "minecraft:block/blast_furnace_side",
        "top": "minecraft:block/blast_furnace_top",
        "particle": "minecraft:block/blast_furnace_side"
    },
    "elements": [
        {
            "from": [0, 0, 0],
            "to": [16, 4, 16],
            "faces": {
                "north": {"texture": "#frame"},
                "south": {"texture": "#frame"},
                "east": {"texture": "#frame"},
                "west": {"texture": "#frame"},
                "up": {"texture": "#top"},
                "down": {"texture": "#frame"}
            }
        },
        {
            "from": [1, 4, 1],
            "to": [15, 14, 15],
            "faces": {
                "north": {"texture": "#core"},
                "south": {"texture": "#side"},
                "east": {"texture": "#coil"},
                "west": {"texture": "#coil"},
                "up": {"texture": "#top"},
                "down": {"texture": "#side"}
            }
        },
        {
            "from": [2, 14, 2],
            "to": [14, 16, 14],
            "faces": {
                "north": {"texture": "#coil"},
                "south": {"texture": "#coil"},
                "east": {"texture": "#coil"},
                "west": {"texture": "#coil"},
                "up": {"texture": "#top"},
                "down": {"texture": "#top"}
            }
        }
    ]
}

jaw_crusher_model = {
    "textures": {
        "base": "minecraft:block/Smooth_stone",
        "stone": "minecraft:block/cobblestone",
        "iron": "minecraft:block/iron_block",
        "hopper": "minecraft:block/cauldron_inner",
        "particle": "minecraft:block/iron_block"
    },
    "elements": [
        {
            "from": [0, 0, 0],
            "to": [16, 5, 16],
            "faces": {
                "north": {"texture": "#stone"},
                "south": {"texture": "#stone"},
                "east": {"texture": "#stone"},
                "west": {"texture": "#stone"},
                "up": {"texture": "#iron"},
                "down": {"texture": "#stone"}
            }
        },
        {
            "from": [1, 5, 1],
            "to": [15, 13, 15],
            "faces": {
                "north": {"texture": "#iron"},
                "south": {"texture": "#iron"},
                "east": {"texture": "#stone"},
                "west": {"texture": "#stone"},
                "up": {"texture": "#hopper"},
                "down": {"texture": "#iron"}
            }
        },
        {
            "from": [2, 13, 2],
            "to": [6, 16, 14],
            "faces": {
                "north": {"texture": "#iron"},
                "south": {"texture": "#iron"},
                "east": {"texture": "#hopper"},
                "west": {"texture": "#iron"},
                "up": {"texture": "#iron"},
                "down": {"texture": "#iron"}
            }
        },
        {
            "from": [10, 13, 2],
            "to": [14, 16, 14],
            "faces": {
                "north": {"texture": "#iron"},
                "south": {"texture": "#iron"},
                "east": {"texture": "#iron"},
                "west": {"texture": "#hopper"},
                "up": {"texture": "#iron"},
                "down": {"texture": "#iron"}
            }
        }
    ]
}

write_json(BLOCK_MODELS_DIR / "bellows_alloy_kiln.json", bellows_kiln_model)
write_json(BLOCK_MODELS_DIR / "electric_alloy_smelter.json", electric_smelter_model)
write_json(BLOCK_MODELS_DIR / "jaw_crusher.json", jaw_crusher_model)

for block_name in ("bellows_alloy_kiln", "electric_alloy_smelter", "jaw_crusher"):
    write_json(BLOCKSTATES_DIR / f"{block_name}.json", {
        "variants": {
            "facing=north": {"model": f"quantumindustries:block/{block_name}"},
            "facing=east": {"model": f"quantumindustries:block/{block_name}", "y": 90},
            "facing=south": {"model": f"quantumindustries:block/{block_name}", "y": 180},
            "facing=west": {"model": f"quantumindustries:block/{block_name}", "y": 270}
        }
    })
    write_json(ITEM_MODELS_DIR / f"{block_name}.json", {
        "parent": f"quantumindustries:block/{block_name}"
    })
    write_json(ITEMS_DEF_DIR / f"{block_name}.json", {
        "model": {
            "type": "minecraft:model",
            "model": f"quantumindustries:block/{block_name}"
        }
    })
    write_json(RECIPE_DIR / f"{block_name}.json", {
        "type": "quantumindustries:foundations",
        "craft": block_name
    })
    write_json(LOOT_DIR / f"{block_name}.json", {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"quantumindustries:{block_name}"
                    }
                ],
                "conditions": [
                    {
                        "condition": "minecraft:survives_explosion"
                    }
                ]
            }
        ]
    })

# 4. Update pt_br.json and en_us.json
pt_path = ASSETS / "lang/pt_br.json"
en_path = ASSETS / "lang/en_us.json"
pt_data = json.loads(pt_path.read_text(encoding="utf-8"))
en_data = json.loads(en_path.read_text(encoding="utf-8"))

# Assay materials & observations for tin, zinc, lead
pt_assay = {
    "gui.quantumindustries.assay.material.tin": "Estanho",
    "gui.quantumindustries.assay.observation.tin.inspection": "Metal prateado claro com reflexo suave e cristalino.",
    "gui.quantumindustries.assay.observation.tin.heating": "Funde em baixa temperatura formando gotas prateadas.",
    "gui.quantumindustries.assay.observation.tin.reaction": "Libera finas bolhas lentas em solução ácida morna.",
    "gui.quantumindustries.assay.observation.tin.mechanical": "Dobra-se com estalo característico sem se partir.",
    "gui.quantumindustries.assay.observation.tin.galvanic": "Ponteiro responde com deflexão moderada",
    "gui.quantumindustries.assay.material.zinc": "Zinco",
    "gui.quantumindustries.assay.observation.zinc.inspection": "Metal cinza-azulado com facetas lamelares brilhantes.",
    "gui.quantumindustries.assay.observation.zinc.heating": "Queima com chama branco-azulada e fumaça densa de óxido.",
    "gui.quantumindustries.assay.observation.zinc.reaction": "Efervesce vigorosamente liberando gás no frasco.",
    "gui.quantumindustries.assay.observation.zinc.mechanical": "Apresenta resistência quebradiça a frio, cedendo sob carga.",
    "gui.quantumindustries.assay.observation.zinc.galvanic": "Forte potencial anódico no galvanômetro",
    "gui.quantumindustries.assay.material.lead": "Chumbo",
    "gui.quantumindustries.assay.observation.lead.inspection": "Metal cinza-chumbo muito denso e de brilho fosco.",
    "gui.quantumindustries.assay.observation.lead.heating": "Liquefaz rapidamente sob o fole formando película cinzenta.",
    "gui.quantumindustries.assay.observation.lead.reaction": "Forma película eletroquímica estável em meio ácido.",
    "gui.quantumindustries.assay.observation.lead.mechanical": "Deforma-se de modo extremamente macio e silencioso.",
    "gui.quantumindustries.assay.observation.lead.galvanic": "Excelente acumulação reversível em par galvânico",
    "gui.quantumindustries.assay.sample": "Amostras: cobre, ferro, ouro, estanho, zinco ou chumbo (bruto, pó ou lingote); carvão, diamante, quartzo, redstone, lápis-lazúli, esmeralda ou ametista.",
}

en_assay = {
    "gui.quantumindustries.assay.material.tin": "Tin",
    "gui.quantumindustries.assay.observation.tin.inspection": "Pale silvery metal with a soft crystalline sheen.",
    "gui.quantumindustries.assay.observation.tin.heating": "Melts at low heat into bright silvery droplets.",
    "gui.quantumindustries.assay.observation.tin.reaction": "Releases slow, fine bubbles in warm acid solution.",
    "gui.quantumindustries.assay.observation.tin.mechanical": "Bends with a distinct crystalline creak without snapping.",
    "gui.quantumindustries.assay.observation.tin.galvanic": "Needle responds with moderate deflection",
    "gui.quantumindustries.assay.material.zinc": "Zinc",
    "gui.quantumindustries.assay.observation.zinc.inspection": "Bluish-grey metal with bright lamellar facets.",
    "gui.quantumindustries.assay.observation.zinc.heating": "Burns with a bluish-white flare and dense oxide fumes.",
    "gui.quantumindustries.assay.observation.zinc.reaction": "Effervesces vigorously, releasing gas in the flask.",
    "gui.quantumindustries.assay.observation.zinc.mechanical": "Shows brittle resistance when cold, yielding under load.",
    "gui.quantumindustries.assay.observation.zinc.galvanic": "Strong anodic potential on the galvanometer",
    "gui.quantumindustries.assay.material.lead": "Lead",
    "gui.quantumindustries.assay.observation.lead.inspection": "Very dense, dull-grey metal with a muted sheen.",
    "gui.quantumindustries.assay.observation.lead.heating": "Liquefies quickly under the bellows with a grey film.",
    "gui.quantumindustries.assay.observation.lead.reaction": "Forms a stable electrochemical film in acidic medium.",
    "gui.quantumindustries.assay.observation.lead.mechanical": "Deforms with extreme softness and no audible ring.",
    "gui.quantumindustries.assay.observation.lead.galvanic": "Excellent reversible accumulation in a galvanic pair",
    "gui.quantumindustries.assay.sample": "Samples: copper, iron, gold, tin, zinc or lead (raw, dust or ingot); coal, diamond, quartz, redstone, lapis lazuli, emerald or amethyst shard.",
}

pt_data.update(pt_assay)
en_data.update(en_assay)

# Blocks and machine messages
pt_data.update({
    "block.quantumindustries.bellows_alloy_kiln": "Forno de Ligas a Fole",
    "item.quantumindustries.bellows_alloy_kiln": "Forno de Ligas a Fole",
    "block.quantumindustries.bellows_alloy_kiln.research": "Pré-requisito: Metalurgia de Ligas (Era I).",
    "block.quantumindustries.bellows_alloy_kiln.busy": "Forno ocupado ou combinação de metais incompatível.",
    "gui.quantumindustries.kiln.output_ready": "Pronto: %s× %s",
    "message.quantumindustries.kiln.status": "Carvão: %s ticks | Calor: %s | Fusão: %s%%",
    "message.quantumindustries.alloy_kiln.research": "Pesquise Metalurgia de Ligas primeiro.",
    "message.quantumindustries.alloy_kiln.busy": "Forno ocupado ou combinação de metais incompatível.",
    "message.quantumindustries.alloy_kiln.status": "Entrada: %s + %s | Saída: %s | Carvão: %s | Calor: %s | Fusão: %s",
    "block.quantumindustries.electric_alloy_smelter": "Fundidora de Ligas Elétrica",
    "item.quantumindustries.electric_alloy_smelter": "Fundidora de Ligas Elétrica",
    "block.quantumindustries.electric_alloy_smelter.research": "Pré-requisito: Fundição Eletrotérmica de Ligas (Era II).",
    "block.quantumindustries.electric_alloy_smelter.busy": "Fundidora ocupada ou combinação de metais incompatível.",
    "gui.quantumindustries.smelter.progress": "Fundindo liga: %s%%",
    "gui.quantumindustries.smelter.output_ready": "Pronto: %s× %s",
    "gui.quantumindustries.smelter.waiting_energy": "Aguardando energia (4 FE/t)",
    "gui.quantumindustries.smelter.idle": "Insira par de metais para liga",
    "message.quantumindustries.alloy_smelter.research": "Pesquise Fundição Eletrotérmica de Ligas primeiro.",
    "message.quantumindustries.alloy_smelter.busy": "Fundidora ocupada ou combinação de metais incompatível.",
    "message.quantumindustries.alloy_smelter.status": "Entrada: %s + %s | Saída: %s | Energia: %s FE | Fusão: %s",
    "block.quantumindustries.jaw_crusher": "Britador de Mandíbulas",
    "item.quantumindustries.jaw_crusher": "Britador de Mandíbulas",
    "message.quantumindustries.jaw_crusher.research": "Pré-requisito: Beneficiamento e Cominuição Mineral (Era I).",
    "message.quantumindustries.jaw_crusher.busy": "Britador ocupado ou material incompatível.",
    "message.quantumindustries.jaw_crusher.status": "Britagem: %s/%s | Energia: %s FE | Entrada: %s | Saída: %s",
    "item.quantumindustries.lead_sheet": "Chapa de Chumbo",
    "item.quantumindustries.tin_sheet": "Chapa de Estanho",
    "item.quantumindustries.zinc_sheet": "Chapa de Zinco",
    "item.quantumindustries.bronze_gear": "Engrenagem de Bronze",
    "item.quantumindustries.heated_bronze_ingot": "Lingote de Bronze Aquecido",
    "item.quantumindustries.heated_brass_ingot": "Lingote de Latão Aquecido",
    "item.quantumindustries.heated_steel_ingot": "Lingote de Aço Aquecido",
    "item.quantumindustries.electrum_wire": "Fio de Electrum",
    "item.quantumindustries.constantan_wire": "Fio de Constantan",
    "item.quantumindustries.constantan_coil": "Bobina Térmica de Constantan",
    "item.quantumindustries.hydrochloric_acid": "Ácido Clorídrico Diluído",
    "item.quantumindustries.refractory_brick": "Tijolo Refratário de Grafite",
    "item.quantumindustries.phosphate_fertilizer": "Fertilizante Fosfatado",
})

en_data.update({
    "block.quantumindustries.bellows_alloy_kiln": "Bellows Alloy Kiln",
    "item.quantumindustries.bellows_alloy_kiln": "Bellows Alloy Kiln",
    "block.quantumindustries.bellows_alloy_kiln.research": "Requires Alloy Metallurgy (Era I).",
    "block.quantumindustries.bellows_alloy_kiln.busy": "Kiln is busy or metal combination is incompatible.",
    "gui.quantumindustries.kiln.output_ready": "Ready: %s× %s",
    "message.quantumindustries.kiln.status": "Fuel: %s ticks | Heat: %s | Smelting: %s%%",
    "message.quantumindustries.alloy_kiln.research": "Research Alloy Metallurgy first.",
    "message.quantumindustries.alloy_kiln.busy": "Kiln is busy or metal combination is incompatible.",
    "message.quantumindustries.alloy_kiln.status": "Input: %s + %s | Output: %s | Fuel: %s | Heat: %s | Smelting: %s",
    "block.quantumindustries.electric_alloy_smelter": "Electric Alloy Smelter",
    "item.quantumindustries.electric_alloy_smelter": "Electric Alloy Smelter",
    "block.quantumindustries.electric_alloy_smelter.research": "Requires Electrothermal Alloy Smelting (Era II).",
    "block.quantumindustries.electric_alloy_smelter.busy": "Smelter is busy or metal combination is incompatible.",
    "gui.quantumindustries.smelter.progress": "Smelting alloy: %s%%",
    "gui.quantumindustries.smelter.output_ready": "Ready: %s× %s",
    "gui.quantumindustries.smelter.waiting_energy": "Waiting for energy (4 FE/t)",
    "gui.quantumindustries.smelter.idle": "Insert pair of alloying metals",
    "message.quantumindustries.alloy_smelter.research": "Research Electrothermal Alloy Smelting first.",
    "message.quantumindustries.alloy_smelter.busy": "Smelter is busy or metal combination is incompatible.",
    "message.quantumindustries.alloy_smelter.status": "Input: %s + %s | Output: %s | Energy: %s FE | Smelting: %s",
    "block.quantumindustries.jaw_crusher": "Jaw Crusher",
    "item.quantumindustries.jaw_crusher": "Jaw Crusher",
    "message.quantumindustries.jaw_crusher.research": "Requires Mineral Beneficiation & Comminution (Era I).",
    "message.quantumindustries.jaw_crusher.busy": "Crusher is busy or material is incompatible.",
    "message.quantumindustries.jaw_crusher.status": "Crushing: %s/%s | Energy: %s FE | Input: %s | Output: %s",
    "item.quantumindustries.lead_sheet": "Lead Sheet",
    "item.quantumindustries.tin_sheet": "Tin Sheet",
    "item.quantumindustries.zinc_sheet": "Zinc Sheet",
    "item.quantumindustries.bronze_gear": "Bronze Gear",
    "item.quantumindustries.heated_bronze_ingot": "Heated Bronze Ingot",
    "item.quantumindustries.heated_brass_ingot": "Heated Brass Ingot",
    "item.quantumindustries.heated_steel_ingot": "Heated Steel Ingot",
    "item.quantumindustries.electrum_wire": "Electrum Wire",
    "item.quantumindustries.constantan_wire": "Constantan Wire",
    "item.quantumindustries.constantan_coil": "Constantan Heating Coil",
    "item.quantumindustries.hydrochloric_acid": "Diluted Hydrochloric Acid",
    "item.quantumindustries.refractory_brick": "Graphite Refractory Brick",
    "item.quantumindustries.phosphate_fertilizer": "Phosphate Fertilizer",
})

for alloy_id, pt_name, en_name, _ in ALLOYS:
    pt_data[f"item.quantumindustries.{alloy_id}_ingot"] = f"Lingote de {pt_name}"
    pt_data[f"item.quantumindustries.{alloy_id}_nugget"] = f"Pepita de {pt_name}"
    pt_data[f"item.quantumindustries.{alloy_id}_sheet"] = f"Chapa de {pt_name}"
    en_data[f"item.quantumindustries.{alloy_id}_ingot"] = f"{en_name} Ingot"
    en_data[f"item.quantumindustries.{alloy_id}_nugget"] = f"{en_name} Nugget"
    en_data[f"item.quantumindustries.{alloy_id}_sheet"] = f"{en_name} Sheet"

for dust_id, pt_name, en_name, _, _ in METAL_DUSTS:
    pt_data[f"item.quantumindustries.{dust_id}_dust"] = f"Pó de {pt_name} Beneficiado"
    en_data[f"item.quantumindustries.{dust_id}_dust"] = f"Crushed {en_name} Dust"

write_json(pt_path, pt_data)
write_json(en_path, en_data)
print("Successfully generated alloy, crusher, dust, wire, and chemical textures, models, recipes, tags, and translations.")
