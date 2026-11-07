import json
import os

BASE_DIR = "/mnt/dados/dev/Quantum-Industries"
DATA_DIR = os.path.join(BASE_DIR, "src/main/resources/data/quantumindustries")
ASSETS_DIR = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries")

LOOT_DIR = os.path.join(DATA_DIR, "loot_table/blocks")
RECIPE_DIR = os.path.join(DATA_DIR, "recipe")
BLOCKSTATES_DIR = os.path.join(ASSETS_DIR, "blockstates")
BLOCK_MODELS_DIR = os.path.join(ASSETS_DIR, "models/block")
ITEM_MODELS_DIR = os.path.join(ASSETS_DIR, "models/item")

os.makedirs(LOOT_DIR, exist_ok=True)
os.makedirs(RECIPE_DIR, exist_ok=True)
os.makedirs(BLOCKSTATES_DIR, exist_ok=True)
os.makedirs(BLOCK_MODELS_DIR, exist_ok=True)
os.makedirs(ITEM_MODELS_DIR, exist_ok=True)

MINERALS = [
    # (id, product, is_metal)
    ("cassiterite", "tin", True),
    ("sphalerite", "zinc", True),
    ("galena", "lead", True),
    ("pentlandite", "nickel", True),
    ("silver", "silver", True),
    ("bauxite", "aluminum", True),
    ("uraninite", "uranium", True),
    ("osmiridium", "osmium", True),
    ("sperrylite", "platinum", True),
    ("wolframite", "tungsten", True),
    ("rutile", "titanium", True),
    ("cobaltite", "cobalt", True),
    ("sulfur", "sulfur", False),
    ("fluorite", "fluorite", False),
    ("cinnabar", "cinnabar", False),
    ("apatite", "apatite", False),
    ("halite", "salt", False),
    ("graphite", "graphite", False)
]

def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")

for m_id, prod, is_metal in MINERALS:
    dropped_item = f"quantumindustries:raw_{prod}" if is_metal else {
        "sulfur": "quantumindustries:sulfur_dust",
        "fluorite": "quantumindustries:fluorite_gem",
        "cinnabar": "quantumindustries:cinnabar_dust",
        "apatite": "quantumindustries:apatite_gem",
        "halite": "quantumindustries:salt_rock",
        "graphite": "quantumindustries:graphite_dust"
    }.get(m_id, f"quantumindustries:{prod}_product")

    # 1. Loot Tables e Modelos para os Blocos (Stone e Deepslate)
    for prefix in ["", "deepslate_"]:
        block_id = f"{prefix}{m_id}_ore"

        # Loot table (dropa o item bruto/químico)
        loot_table = {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [
                        {
                            "type": "minecraft:item",
                            "name": dropped_item
                        }
                    ],
                    "conditions": [
                        {
                            "condition": "minecraft:survives_explosion"
                        }
                    ]
                }
            ]
        }
        write_json(os.path.join(LOOT_DIR, f"{block_id}.json"), loot_table)

        # Blockstate
        blockstate = {
            "variants": {
                "": {
                    "model": f"quantumindustries:block/{block_id}"
                }
            }
        }
        write_json(os.path.join(BLOCKSTATES_DIR, f"{block_id}.json"), blockstate)

        # Block model
        block_model = {
            "parent": "minecraft:block/cube_all",
            "textures": {
                "all": f"quantumindustries:block/{block_id}"
            }
        }
        write_json(os.path.join(BLOCK_MODELS_DIR, f"{block_id}.json"), block_model)

        # Item model do bloco
        block_item_model = {
            "parent": f"quantumindustries:block/{block_id}"
        }
        write_json(os.path.join(ITEM_MODELS_DIR, f"{block_id}.json"), block_item_model)

    # 2. Itens e Receitas
    if is_metal:
        # Raw Item Model
        write_json(os.path.join(ITEM_MODELS_DIR, f"raw_{prod}.json"), {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"quantumindustries:item/raw_{prod}"
            }
        })

        # Ingot Model
        write_json(os.path.join(ITEM_MODELS_DIR, f"{prod}_ingot.json"), {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"quantumindustries:item/{prod}_ingot"
            }
        })

        # Nugget Model
        write_json(os.path.join(ITEM_MODELS_DIR, f"{prod}_nugget.json"), {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"quantumindustries:item/{prod}_nugget"
            }
        })

        # Smelting Recipe (Raw -> Ingot)
        write_json(os.path.join(RECIPE_DIR, f"{prod}_ingot_from_smelting_raw_{prod}.json"), {
            "type": "minecraft:smelting",
            "category": "misc",
            "ingredient": f"quantumindustries:raw_{prod}",
            "result": {
                "id": f"quantumindustries:{prod}_ingot",
                "count": 1
            },
            "experience": 0.7,
            "cookingtime": 200
        })

        # Blasting Recipe (Raw -> Ingot)
        write_json(os.path.join(RECIPE_DIR, f"{prod}_ingot_from_blasting_raw_{prod}.json"), {
            "type": "minecraft:blasting",
            "category": "misc",
            "ingredient": f"quantumindustries:raw_{prod}",
            "result": {
                "id": f"quantumindustries:{prod}_ingot",
                "count": 1
            },
            "experience": 0.7,
            "cookingtime": 100
        })

        # Compactação 9 Nuggets -> 1 Ingot
        write_json(os.path.join(RECIPE_DIR, f"{prod}_ingot_from_nuggets.json"), {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [f"quantumindustries:{prod}_nugget"] * 9,
            "result": {
                "id": f"quantumindustries:{prod}_ingot",
                "count": 1
            }
        })

        # Descompactação 1 Ingot -> 9 Nuggets
        write_json(os.path.join(RECIPE_DIR, f"{prod}_nugget_from_ingot.json"), {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [f"quantumindustries:{prod}_ingot"],
            "result": {
                "id": f"quantumindustries:{prod}_nugget",
                "count": 9
            }
        })
    else:
        # Chemical Product Model
        prod_item = dropped_item.split(":")[1]
        write_json(os.path.join(ITEM_MODELS_DIR, f"{prod_item}.json"), {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"quantumindustries:item/{prod_item}"
            }
        })

print("Geração de JSONs de minerais concluída com sucesso!")
