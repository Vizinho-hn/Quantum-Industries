import json
import os

BASE_DIR = "/mnt/dados/dev/Quantum-Industries"
ITEMS_DIR = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries/items")
os.makedirs(ITEMS_DIR, exist_ok=True)

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

def write_item_def(item_id, model_target):
    path = os.path.join(ITEMS_DIR, f"{item_id}.json")
    data = {
        "model": {
            "type": "minecraft:model",
            "model": model_target
        }
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

count = 0

for m_id, prod, is_metal in MINERALS:
    # 1. Definições de Itens para os Blocos de Minério (Stone e Deepslate)
    write_item_def(f"{m_id}_ore", f"quantumindustries:block/{m_id}_ore")
    count += 1
    write_item_def(f"deepslate_{m_id}_ore", f"quantumindustries:block/deepslate_{m_id}_ore")
    count += 1

    # 2. Itens derivados
    if is_metal:
        write_item_def(f"raw_{prod}", f"quantumindustries:item/raw_{prod}")
        count += 1
        write_item_def(f"{prod}_ingot", f"quantumindustries:item/{prod}_ingot")
        count += 1
        write_item_def(f"{prod}_nugget", f"quantumindustries:item/{prod}_nugget")
        count += 1
    else:
        chem_item = {
            "sulfur": "sulfur_dust",
            "fluorite": "fluorite_gem",
            "cinnabar": "cinnabar_dust",
            "apatite": "apatite_gem",
            "halite": "salt_rock",
            "graphite": "graphite_dust"
        }.get(m_id, f"{prod}_product")
        write_item_def(chem_item, f"quantumindustries:item/{chem_item}")
        count += 1

print(f"Sucesso! {count} definições de modelos de itens (Item Model Definitions) geradas em {ITEMS_DIR}!")
