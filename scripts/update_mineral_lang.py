import json
import os

BASE_DIR = "/mnt/dados/dev/Quantum-Industries"
PT_PATH = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries/lang/pt_br.json")
EN_PATH = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries/lang/en_us.json")

MINERALS = [
    # (id, product, name_pt, prod_pt, name_en, prod_en, is_metal, formula)
    ("cassiterite", "tin", "Cassiterita", "Estanho", "Cassiterite", "Tin", True, "SnO₂"),
    ("sphalerite", "zinc", "Esfalerita", "Zinco", "Sphalerite", "Zinc", True, "(Zn,Fe)S"),
    ("galena", "lead", "Galena", "Chumbo", "Galena", "Lead", True, "PbS"),
    ("pentlandite", "nickel", "Pentlandita", "Níquel", "Pentlandite", "Nickel", True, "(Fe,Ni)₉S₈"),
    ("silver", "silver", "Acantita", "Prata", "Silver", "Silver", True, "Ag₂S"),
    ("bauxite", "aluminum", "Bauxita", "Alumínio", "Bauxite", "Aluminum", True, "AlO(OH)"),
    ("uraninite", "uranium", "Uraninita", "Urânio", "Uraninite", "Uranium", True, "UO₂"),
    ("osmiridium", "osmium", "Osmirídio", "Ósmio", "Osmiridium", "Osmium", True, "(Os,Ir)"),
    ("sperrylite", "platinum", "Esperrilita", "Platina", "Sperrylite", "Platinum", True, "PtAs₂"),
    ("wolframite", "tungsten", "Wolframita", "Tungstênio", "Wolframite", "Tungsten", True, "(Fe,Mn)WO₄"),
    ("rutile", "titanium", "Rutilo", "Titânio", "Rutile", "Titanium", True, "TiO₂"),
    ("cobaltite", "cobalt", "Cobaltita", "Cobalto", "Cobaltite", "Cobalt", True, "CoAsS"),
    ("sulfur", "sulfur", "Enxofre Nativo", "Pó de Enxofre", "Native Sulfur", "Sulfur Dust", False, "S₈"),
    ("fluorite", "fluorite", "Fluorita", "Cristal de Fluorita", "Fluorite", "Fluorite Crystal", False, "CaF₂"),
    ("cinnabar", "cinnabar", "Cinábrio", "Pó de Cinábrio", "Cinnabar", "Cinnabar Dust", False, "HgS"),
    ("apatite", "apatite", "Apatita", "Apatita", "Apatite", "Apatite Mineral", False, "Ca₅(PO₄)₃(F,Cl,OH)"),
    ("halite", "salt", "Halita", "Sal Gema", "Halite", "Rock Salt", False, "NaCl"),
    ("graphite", "graphite", "Grafite", "Pó de Grafite", "Graphite", "Graphite Dust", False, "C")
]

with open(PT_PATH, "r", encoding="utf-8") as f:
    pt_data = json.load(f)

with open(EN_PATH, "r", encoding="utf-8") as f:
    en_data = json.load(f)

for m_id, prod, name_pt, prod_pt, name_en, prod_en, is_metal, formula in MINERALS:
    # Stone ore
    pt_data[f"block.quantumindustries.{m_id}_ore"] = f"Minério de {name_pt}"
    pt_data[f"item.quantumindustries.{m_id}_ore"] = f"Minério de {name_pt}"
    en_data[f"block.quantumindustries.{m_id}_ore"] = f"{name_en} Ore"
    en_data[f"item.quantumindustries.{m_id}_ore"] = f"{name_en} Ore"

    # Deepslate ore
    pt_data[f"block.quantumindustries.deepslate_{m_id}_ore"] = f"Minério de {name_pt} em Ardósia Profunda"
    pt_data[f"item.quantumindustries.deepslate_{m_id}_ore"] = f"Minério de {name_pt} em Ardósia Profunda"
    en_data[f"block.quantumindustries.deepslate_{m_id}_ore"] = f"Deepslate {name_en} Ore"
    en_data[f"item.quantumindustries.deepslate_{m_id}_ore"] = f"Deepslate {name_en} Ore"

    if is_metal:
        # Raw
        pt_data[f"item.quantumindustries.raw_{prod}"] = f"{prod_pt} Bruto"
        en_data[f"item.quantumindustries.raw_{prod}"] = f"Raw {prod_en}"
        # Ingot
        pt_data[f"item.quantumindustries.{prod}_ingot"] = f"Lingote de {prod_pt}"
        en_data[f"item.quantumindustries.{prod}_ingot"] = f"{prod_en} Ingot"
        # Nugget
        pt_data[f"item.quantumindustries.{prod}_nugget"] = f"Pepita de {prod_pt}"
        en_data[f"item.quantumindustries.{prod}_nugget"] = f"{prod_en} Nugget"
    else:
        prod_item_id = {
            "sulfur": "sulfur_dust",
            "fluorite": "fluorite_gem",
            "cinnabar": "cinnabar_dust",
            "apatite": "apatite_gem",
            "halite": "salt_rock",
            "graphite": "graphite_dust"
        }.get(m_id, f"{prod}_product")
        pt_data[f"item.quantumindustries.{prod_item_id}"] = prod_pt
        en_data[f"item.quantumindustries.{prod_item_id}"] = prod_en

with open(PT_PATH, "w", encoding="utf-8") as f:
    json.dump(pt_data, f, indent=2, ensure_ascii=False)
    f.write("\n")

with open(EN_PATH, "w", encoding="utf-8") as f:
    json.dump(en_data, f, indent=2, ensure_ascii=False)
    f.write("\n")

print("Locales atualizados com sucesso!")
