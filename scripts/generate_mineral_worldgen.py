import json
import os

BASE_DIR = "/mnt/dados/dev/Quantum-Industries"
DATA_DIR = os.path.join(BASE_DIR, "src/main/resources/data")

CONFIGURED_DIR = os.path.join(DATA_DIR, "quantumindustries/worldgen/configured_feature")
PLACED_DIR = os.path.join(DATA_DIR, "quantumindustries/worldgen/placed_feature")
BIOME_MOD_DIR = os.path.join(DATA_DIR, "quantumindustries/neoforge/biome_modifier")

C_TAGS_DIR = os.path.join(DATA_DIR, "c/tags")

os.makedirs(CONFIGURED_DIR, exist_ok=True)
os.makedirs(PLACED_DIR, exist_ok=True)
os.makedirs(BIOME_MOD_DIR, exist_ok=True)

# Definições geológicas dos 18 minerais: (id, product, is_metal, vein_size, count_per_chunk, min_y, max_y)
GEOLOGY = [
    # Camada Rasa / Média
    ("cassiterite", "tin", True, 9, 12, 16, 96),
    ("bauxite", "aluminum", True, 12, 8, 32, 128),
    ("halite", "salt", False, 14, 6, 40, 90),

    # Camada Intermediária
    ("sphalerite", "zinc", True, 9, 10, -16, 64),
    ("galena", "lead", True, 8, 8, -32, 48),
    ("pentlandite", "nickel", True, 8, 7, -24, 56),
    ("sulfur", "sulfur", False, 8, 8, -32, 32),
    ("apatite", "apatite", False, 8, 6, -10, 60),
    ("graphite", "graphite", False, 7, 5, -20, 40),

    # Camada Profunda (Deepslate)
    ("silver", "silver", True, 7, 6, -60, 16),
    ("fluorite", "fluorite", False, 8, 6, -48, 16),
    ("cinnabar", "cinnabar", False, 6, 5, -50, 10),
    ("cobaltite", "cobalt", True, 6, 4, -56, -16),
    ("rutile", "titanium", True, 6, 4, -60, -10),
    ("wolframite", "tungsten", True, 6, 4, -60, -16),
    ("uraninite", "uranium", True, 6, 4, -60, -10),
    ("osmiridium", "osmium", True, 6, 4, -64, -20),
    ("sperrylite", "platinum", True, 5, 3, -64, -30)
]

def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")

all_placed_features = []

for m_id, prod, is_metal, vein_size, count_per_chunk, min_y, max_y in GEOLOGY:
    stone_ore = f"quantumindustries:{m_id}_ore"
    deepslate_ore = f"quantumindustries:deepslate_{m_id}_ore"

    # 1. Configured Feature
    configured_data = {
        "type": "minecraft:ore",
        "config": {
            "discard_chance_on_air_exposure": 0.0,
            "size": vein_size,
            "targets": [
                {
                    "state": {
                        "Name": stone_ore
                    },
                    "target": {
                        "predicate_type": "minecraft:tag_match",
                        "tag": "minecraft:stone_ore_replaceables"
                    }
                },
                {
                    "state": {
                        "Name": deepslate_ore
                    },
                    "target": {
                        "predicate_type": "minecraft:tag_match",
                        "tag": "minecraft:deepslate_ore_replaceables"
                    }
                }
            ]
        }
    }
    write_json(os.path.join(CONFIGURED_DIR, f"{m_id}_ore.json"), configured_data)

    # 2. Placed Feature
    placed_name = f"{m_id}_ore"
    placed_data = {
        "feature": f"quantumindustries:{m_id}_ore",
        "placement": [
            {
                "type": "minecraft:count",
                "count": count_per_chunk
            },
            {
                "type": "minecraft:in_square"
            },
            {
                "type": "minecraft:height_range",
                "height": {
                    "type": "minecraft:uniform",
                    "min_inclusive": {
                        "absolute": min_y
                    },
                    "max_inclusive": {
                        "absolute": max_y
                    }
                }
            },
            {
                "type": "minecraft:biome"
            }
        ]
    }
    write_json(os.path.join(PLACED_DIR, f"{placed_name}.json"), placed_data)
    all_placed_features.append(f"quantumindustries:{placed_name}")

    # 3. Tags Comunitárias (c:...)
    # Block Tag: c:ores/<prod> e c:ores
    block_ores_dir = os.path.join(C_TAGS_DIR, f"block/ores/{prod}")
    os.makedirs(block_ores_dir, exist_ok=True)
    write_json(os.path.join(C_TAGS_DIR, f"block/ores/{prod}.json"), {
        "replace": False,
        "values": [stone_ore, deepslate_ore]
    })

    # Item Tag: c:ores/<prod>
    item_ores_dir = os.path.join(C_TAGS_DIR, f"item/ores")
    os.makedirs(item_ores_dir, exist_ok=True)
    write_json(os.path.join(C_TAGS_DIR, f"item/ores/{prod}.json"), {
        "replace": False,
        "values": [stone_ore, deepslate_ore]
    })

    if is_metal:
        # c:raw_materials/<prod>
        raw_dir = os.path.join(C_TAGS_DIR, f"item/raw_materials")
        os.makedirs(raw_dir, exist_ok=True)
        write_json(os.path.join(C_TAGS_DIR, f"item/raw_materials/{prod}.json"), {
            "replace": False,
            "values": [f"quantumindustries:raw_{prod}"]
        })

        # c:ingots/<prod>
        ingot_dir = os.path.join(C_TAGS_DIR, f"item/ingots")
        os.makedirs(ingot_dir, exist_ok=True)
        write_json(os.path.join(C_TAGS_DIR, f"item/ingots/{prod}.json"), {
            "replace": False,
            "values": [f"quantumindustries:{prod}_ingot"]
        })

        # c:nuggets/<prod>
        nugget_dir = os.path.join(C_TAGS_DIR, f"item/nuggets")
        os.makedirs(nugget_dir, exist_ok=True)
        write_json(os.path.join(C_TAGS_DIR, f"item/nuggets/{prod}.json"), {
            "replace": False,
            "values": [f"quantumindustries:{prod}_nugget"]
        })
    else:
        # c:dusts/<prod> ou c:gems/<prod>
        tag_sub = "gems" if m_id in ["fluorite", "apatite"] else "dusts"
        chem_dir = os.path.join(C_TAGS_DIR, f"item/{tag_sub}")
        os.makedirs(chem_dir, exist_ok=True)
        prod_item = {
            "sulfur": "quantumindustries:sulfur_dust",
            "fluorite": "quantumindustries:fluorite_gem",
            "cinnabar": "quantumindustries:cinnabar_dust",
            "apatite": "quantumindustries:apatite_gem",
            "halite": "quantumindustries:salt_rock",
            "graphite": "quantumindustries:graphite_dust"
        }.get(m_id, f"quantumindustries:{prod}_product")
        write_json(os.path.join(C_TAGS_DIR, f"item/{tag_sub}/{prod}.json"), {
            "replace": False,
            "values": [prod_item]
        })

# 4. NeoForge Biome Modifier (Gera em todos os biomas do Overworld)
biome_modifier_data = {
    "type": "neoforge:add_features",
    "biomes": "#minecraft:is_overworld",
    "features": all_placed_features,
    "step": "underground_ores"
}
write_json(os.path.join(BIOME_MOD_DIR, "add_quantum_ores.json"), biome_modifier_data)

print(f"Worldgen concluído! {len(all_placed_features)} minérios configurados para geração no Overworld com tags c:* e biome modifier.")
