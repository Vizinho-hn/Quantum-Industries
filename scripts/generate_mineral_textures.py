import os
import shutil
import colorsys
import zipfile
from PIL import Image

BASE_DIR = "/mnt/dados/dev/Quantum-Industries"
EMENDATUS_DIR = os.path.join(BASE_DIR, "design/emendatus_assets/src/main/resources/assets/emendatusenigmatica/textures")
JAR_PATH = os.path.join(BASE_DIR, "build/moddev/artifacts/minecraft-patched-26.1.2.125.jar")
VANILLA_CACHE_DIR = os.path.join(BASE_DIR, "design/vanilla_textures")

ITEM_TEMPLATES = os.path.join(EMENDATUS_DIR, "items/templates")
BLOCK_TEMPLATES = os.path.join(EMENDATUS_DIR, "blocks/templates/ore")

OUT_ITEM_DIR = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries/textures/item")
OUT_BLOCK_DIR = os.path.join(BASE_DIR, "src/main/resources/assets/quantumindustries/textures/block")

BUILD_ITEM_DIR = os.path.join(BASE_DIR, "build/resources/main/assets/quantumindustries/textures/item")
BUILD_BLOCK_DIR = os.path.join(BASE_DIR, "build/resources/main/assets/quantumindustries/textures/block")

os.makedirs(OUT_ITEM_DIR, exist_ok=True)
os.makedirs(OUT_BLOCK_DIR, exist_ok=True)
os.makedirs(BUILD_ITEM_DIR, exist_ok=True)
os.makedirs(BUILD_BLOCK_DIR, exist_ok=True)
os.makedirs(VANILLA_CACHE_DIR, exist_ok=True)

# 1. Carregar texturas oficiais Vanilla
stone_path = os.path.join(VANILLA_CACHE_DIR, "stone.png")
deepslate_path = os.path.join(VANILLA_CACHE_DIR, "deepslate.png")

if not os.path.exists(stone_path) or not os.path.exists(deepslate_path):
    with zipfile.ZipFile(JAR_PATH, "r") as z:
        with open(stone_path, "wb") as f:
            f.write(z.read("assets/minecraft/textures/block/stone.png"))
        with open(deepslate_path, "wb") as f:
            f.write(z.read("assets/minecraft/textures/block/deepslate.png"))

vanilla_stone = Image.open(stone_path).convert("RGBA")
vanilla_deepslate = Image.open(deepslate_path).convert("RGBA")

# 2. Paletas de cores hexadecimais balanceadas e de alto contraste
MINERAL_COLORS = {
    # Metais
    "tin": "D0DEE4",        # Prateado suave
    "zinc": "8DA2B0",       # Cinza ardósia azulado
    "lead": "4A4E69",       # Chumbo escuro azulado
    "nickel": "CBD0A3",     # Níquel amarelado metálico
    "silver": "E5F2F7",     # Prata brilhante
    "aluminum": "C5D1D9",   # Alumínio fosco industrial
    "uranium": "5BC332",    # Verde fluorescente
    "osmium": "5296BD",     # Azul ciano metálico
    "platinum": "9FD2E8",   # Platina de alto brilho
    "tungsten": "4F5661",   # Cinza escuro refratário
    "titanium": "CAD0D8",   # Titânio metálico aeroespacial
    "cobalt": "2658CE",     # Azul cobalto elétrico profundo
    
    # Químicos e Minerais não-metálicos (Cores vívidas de alto contraste)
    "sulfur": "F5DE18",     # Amarelo enxofre solar vibrante
    "fluorite": "2BD9C8",    # Verde-água / turquesa fluorescente
    "cinnabar": "E6242B",    # Vermelho cinábrio rubi intenso
    "apatite": "1BA7E6",     # Azul cerúleo brilhante
    "salt": "FCE4DC",       # Rosa halita translúcido / sal-gema cristalino
    "graphite": "2B2D33"    # Carbono escuro
}

# (id_bloco, id_produto, is_metal, template_tipo, item_sub_ids)
MINERAL_MAP = [
    ("cassiterite", "tin", True, "metal", ["raw_tin", "tin_ingot", "tin_nugget"]),
    ("sphalerite", "zinc", True, "metal", ["raw_zinc", "zinc_ingot", "zinc_nugget"]),
    ("galena", "lead", True, "metal", ["raw_lead", "lead_ingot", "lead_nugget"]),
    ("pentlandite", "nickel", True, "metal", ["raw_nickel", "nickel_ingot", "nickel_nugget"]),
    ("silver", "silver", True, "metal", ["raw_silver", "silver_ingot", "silver_nugget"]),
    ("bauxite", "aluminum", True, "metal", ["raw_aluminum", "aluminum_ingot", "aluminum_nugget"]),
    ("uraninite", "uranium", True, "metal", ["raw_uranium", "uranium_ingot", "uranium_nugget"]),
    ("osmiridium", "osmium", True, "metal", ["raw_osmium", "osmium_ingot", "osmium_nugget"]),
    ("sperrylite", "platinum", True, "metal", ["raw_platinum", "platinum_ingot", "platinum_nugget"]),
    ("wolframite", "tungsten", True, "metal", ["raw_tungsten", "tungsten_ingot", "tungsten_nugget"]),
    ("rutile", "titanium", True, "metal", ["raw_titanium", "titanium_ingot", "titanium_nugget"]),
    ("cobaltite", "cobalt", True, "metal", ["raw_cobalt", "cobalt_ingot", "cobalt_nugget"]),
    
    # Químicos: Mapeamos tanto o nome simples quanto o nome exato registrado no ModItems
    ("sulfur", "sulfur", False, "dust", ["sulfur", "sulfur_dust"]),
    ("fluorite", "fluorite", False, "gem_1", ["fluorite", "fluorite_gem"]),
    ("cinnabar", "cinnabar", False, "gem_2", ["cinnabar", "cinnabar_dust"]),
    ("apatite", "apatite", False, "gem_3", ["apatite", "apatite_gem"]),
    ("halite", "salt", False, "gem_4", ["salt", "salt_rock"]),
    ("graphite", "graphite", False, "dust", ["graphite", "graphite_dust"])
]

# 3. Matemática oficial de Hue-Shift do Emendatus Enigmatica
def hex_to_hsv(hex_str):
    hex_str = hex_str.lstrip('#')
    r, g, b = [int(hex_str[i:i+2], 16) / 255.0 for i in (0, 2, 4)]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    return h * 360.0, s * 100.0, v * 100.0

def hsv_to_rgb(h, s, v):
    r, g, b = colorsys.hsv_to_rgb(h / 360.0, max(0.0, min(1.0, s / 100.0)), max(0.0, min(1.0, v / 100.0)))
    return int(round(r * 255)), int(round(g * 255)), int(round(b * 255))

def hue_shift(hex_str, factor, is_highlight):
    h, s, v = hex_to_hsv(hex_str)
    if is_highlight:
        if 0 <= h < 60:
            h2 = h + (4 * factor); s2 = s - (5 * factor); v2 = v + (10 * factor)
        elif 60 <= h < 120:
            h2 = h - (4 * factor); s2 = s - (5 * factor); v2 = v + (10 * factor)
        else:
            h2 = h + (8 * factor) if (h < 180 or h >= 240) else h - (8 * factor)
            s2 = s - (2 * factor); v2 = v + (20 * factor)
    else:
        if 0 <= h < 60:
            h2 = h - (4 * factor); s2 = s + (5 * factor); v2 = v - (15 * factor)
        elif 60 <= h < 120:
            h2 = h + (4 * factor); s2 = s + (5 * factor); v2 = v - (15 * factor)
        else:
            h2 = h - (8 * factor) if (h < 180 or h >= 240) else h + (8 * factor)
            s2 = s + (2 * factor); v2 = v - (10 * factor)
    h2 = h2 % 360.0
    s2 = max(0.0, min(100.0, s2))
    v2 = max(0.0, min(100.0, v2))
    return hsv_to_rgb(h2, s2, v2)

def get_color_tiers(hex_str):
    hex_clean = hex_str.lstrip('#')
    base_rgb = tuple(int(hex_clean[i:i+2], 16) for i in (0, 2, 4))
    h2 = hue_shift(hex_clean, 3, True)
    h1 = hue_shift(hex_clean, 1, True)
    s1 = hue_shift(hex_clean, 1, False)
    s2 = hue_shift(hex_clean, 2, False)
    return [h2, h1, base_rgb, s1, s2]

# 4. Composição com as 5 camadas volumétricas
def composite_layers(template_dir, color_tiers):
    result = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in [4, 3, 2, 1, 0]:
        layer_path = os.path.join(template_dir, f"0{i}.png")
        if not os.path.exists(layer_path):
            continue
        layer_img = Image.open(layer_path).convert("RGBA")
        color = color_tiers[i]
        pixels = layer_img.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = pixels[x, y]
                if a > 0:
                    tint_r = int(round((r / 255.0) * color[0]))
                    tint_g = int(round((g / 255.0) * color[1]))
                    tint_b = int(round((b / 255.0) * color[2]))
                    pixels[x, y] = (tint_r, tint_g, tint_b, a)
        result.alpha_composite(layer_img)
    return result

def save_image(img, path, build_path):
    img.save(path)
    img.save(build_path)

count = 0

for m_id, prod, is_metal, kind, item_targets in MINERAL_MAP:
    hex_color = MINERAL_COLORS[prod]
    tiers = get_color_tiers(hex_color)

    # 1. Blocos de Minério (Stone e Deepslate)
    # Todos os minérios usam os veios volumétricos 'metal' ou 'gem' com sombra projetada
    ore_template_dir = os.path.join(BLOCK_TEMPLATES, "metal" if is_metal else "gem")
    shadow_drop_path = os.path.join(ore_template_dir, "shadow_drop.png")
    shadow_drop = Image.open(shadow_drop_path).convert("RGBA")

    vein = composite_layers(ore_template_dir, tiers)

    # Stone Ore
    stone_ore = vanilla_stone.copy()
    stone_ore.alpha_composite(shadow_drop)
    stone_ore.alpha_composite(vein)
    save_image(stone_ore, os.path.join(OUT_BLOCK_DIR, f"{m_id}_ore.png"), os.path.join(BUILD_BLOCK_DIR, f"{m_id}_ore.png"))
    count += 1

    # Deepslate Ore
    deepslate_ore = vanilla_deepslate.copy()
    deepslate_ore.alpha_composite(shadow_drop)
    deepslate_ore.alpha_composite(vein)
    save_image(deepslate_ore, os.path.join(OUT_BLOCK_DIR, f"deepslate_{m_id}_ore.png"), os.path.join(BUILD_BLOCK_DIR, f"deepslate_{m_id}_ore.png"))
    count += 1

    # Se for halita, também salvar réplica com nome salt_ore para compatibilidade
    if m_id == "halite":
        save_image(stone_ore, os.path.join(OUT_BLOCK_DIR, "salt_ore.png"), os.path.join(BUILD_BLOCK_DIR, "salt_ore.png"))
        save_image(deepslate_ore, os.path.join(OUT_BLOCK_DIR, "deepslate_salt_ore.png"), os.path.join(BUILD_BLOCK_DIR, "deepslate_salt_ore.png"))

    # 2. Itens derivados
    if is_metal:
        # Raw Material
        raw_img = composite_layers(os.path.join(ITEM_TEMPLATES, "raw"), tiers)
        save_image(raw_img, os.path.join(OUT_ITEM_DIR, f"raw_{prod}.png"), os.path.join(BUILD_ITEM_DIR, f"raw_{prod}.png"))
        count += 1

        # Ingot
        ingot_img = composite_layers(os.path.join(ITEM_TEMPLATES, "ingot"), tiers)
        save_image(ingot_img, os.path.join(OUT_ITEM_DIR, f"{prod}_ingot.png"), os.path.join(BUILD_ITEM_DIR, f"{prod}_ingot.png"))
        count += 1

        # Nugget
        nugget_img = composite_layers(os.path.join(ITEM_TEMPLATES, "nugget"), tiers)
        save_image(nugget_img, os.path.join(OUT_ITEM_DIR, f"{prod}_nugget.png"), os.path.join(BUILD_ITEM_DIR, f"{prod}_nugget.png"))
        count += 1
    else:
        # Químicos / Cristais / Pós
        if kind == "dust":
            item_template_dir = os.path.join(ITEM_TEMPLATES, "dust")
        elif kind == "gem_1":
            item_template_dir = os.path.join(ITEM_TEMPLATES, "gem/template_1")
        elif kind == "gem_2":
            item_template_dir = os.path.join(ITEM_TEMPLATES, "gem/template_2")
        elif kind == "gem_3":
            item_template_dir = os.path.join(ITEM_TEMPLATES, "gem/template_3")
        elif kind == "gem_4":
            item_template_dir = os.path.join(ITEM_TEMPLATES, "gem/template_4")
        else:
            item_template_dir = os.path.join(ITEM_TEMPLATES, "dust")

        item_img = composite_layers(item_template_dir, tiers)
        for target_name in item_targets:
            save_image(item_img, os.path.join(OUT_ITEM_DIR, f"{target_name}.png"), os.path.join(BUILD_ITEM_DIR, f"{target_name}.png"))
            count += 1

print(f"Generated and synced {count} authentic textures successfully.")
