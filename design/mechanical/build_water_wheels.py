"""Rebuild the three QKS rotors and their editable Blockbench projects."""
import base64
import json
import math
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DESIGN = ROOT / "design/mechanical"
FACES = ("north", "south", "east", "west", "up", "down")


def save(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def reference_rotor(radius, count, width, socket, palette):
    """Style D: a plated polygon around a continuous row of recessed wooden buckets."""
    cubes = []

    def add(name, x, y, z, w, h, depth, material, angle=0):
        u, v, uw, vh = palette[material]
        cube = dict(name=name, origin=[x, y, z], size=[w, h, depth],
                    uv={face:dict(uv=[u,v],uv_size=[uw,vh]) for face in FACES})
        if angle:
            cube.update(pivot=[0,8,0], rotation=[0,0,angle])
        cubes.append(cube)

    half = width / 2
    step = 360 / count
    angle_half = math.pi / count
    rim = radius * math.cos(angle_half)
    band = radius * .18
    rim_depth = min(1.1, width * .17)
    tangent = math.tan(angle_half)
    chord = 2 * rim * tangent
    joint = min(1.6, radius * .125)
    cap = min(4.9, radius * .36)

    for i in range(8):
        spoke = min(radius * .23, 5)
        add("Raio de carvalho", -spoke/2, 8, -half+rim_depth*.55,
            spoke, rim-band*.48, width-rim_depth*1.10, "wood", i*45)

    for i in range(count):
        angle = i * step
        deck = rim - band*.85
        floor_chord = 2 * deck * tangent
        floor_depth = width - 2*rim_depth
        # Wide wood floor and a riser at each joint create the reference's stepped buckets.
        add("Fundo da caçamba", -floor_chord/2, 8+deck, -floor_depth/2,
            floor_chord, band*.21, floor_depth, "dark", angle)
        riser = min(.56, band*.34)
        add("Degrau de madeira", -riser/2, 8+deck, -floor_depth/2,
            riser, rim-deck-.12, floor_depth, "wood", angle+step/2)
        for side in (-1, 1):
            wood_z = -half if side < 0 else half-rim_depth
            face_z = -half-.24 if side < 0 else half
            add("Aro de madeira", -chord/2, 8+rim-band, wood_z,
                chord, band, rim_depth, "wood", angle)
            add("Faixa prateada contínua", -chord/2, 8+rim-band*.64, face_z,
                chord, band*.64, .24, "iron", angle)
            plate_z = face_z-.16 if side < 0 else face_z+.23
            joint_y = 8+radius-band*.70
            add("Chapa quadrada da junção", -joint/2, joint_y, plate_z,
                joint, band*.63, .2, "iron", angle+step/2)
            rivet = min(.43, joint*.37)
            add("Pino da junção", -rivet/2, joint_y+band*.28,
                plate_z-.11 if side < 0 else plate_z+.2,
                rivet, rivet, .11, "rivet", angle+step/2)

    for side in (-1, 1):
        plate_z = -half-.39 if side < 0 else half
        bolt_z = plate_z-.12 if side < 0 else plate_z+.39
        collar_z = plate_z-.38 if side < 0 else plate_z+.39
        add("Placa quadrada do cubo", -cap, 8-cap, plate_z,
            2*cap, 2*cap, .39, "iron")
        collar = socket + .68
        add("Colar de bronze", -collar/2, 8-collar/2, collar_z, collar, collar, .38, "brass")
        socket_z = -half-.81 if side < 0 else half+.77
        add("Encaixe rente ao cubo", -socket/2, 8-socket/2, socket_z, socket, socket, .04, "socket")
        bolt = min(.55, cap*.18)
        for x in (-cap*.79, cap*.79):
            for y in (-cap*.79, cap*.79):
                add("Rebite do cubo", x-bolt/2, 8+y-bolt/2, bolt_z, bolt, bolt, .12, "rivet")
    return cubes


def plate_atlas(source_path, target_path):
    """Paint new iron swatches in the large atlas's unused right-hand column."""
    from PIL import Image
    with Image.open(source_path) as source:
        atlas = source.convert("RGBA")
    for y in range(16, 48):
        for x in range(98, 126):
            value = 126 + ((x//3*31 + y//2*17) % 30) - (13 if y % 8 == 0 else 0)
            if x in (98, 124):
                value += 14
            atlas.putpixel((x,y), (value+3,value+1,value,255))
    for y in range(52,58):
        for x in range(98,104):
            value = 207 if x < 101 and y < 55 else 172
            atlas.putpixel((x,y), (value+3,value+1,value,255))
    atlas.save(target_path)


def element(cube, name, element_id, vanilla=False):
    x,y,z=cube["origin"]
    w,h,d=cube["size"]
    start, end = [8-x-w,y,8+z],[8-x,y+h,8+z+d]
    angle = -cube.get("rotation",[0,0,0])[2]
    if vanilla and angle:
        reduced = (angle+45) % 90 - 45
        turn = round((angle-reduced)/90) % 4
        for _ in range(turn):
            start,end = [16-end[1],start[0],start[2]],[16-start[1],end[0],end[2]]
        angle = reduced
    faces = {}
    for key, values in cube["uv"].items():
        u,v=values["uv"]
        W,H=values["uv_size"]
        unit = 8 if vanilla else 1
        faces[key]=dict(uv=[u/unit,v/unit,(u+W)/unit,(v+H)/unit],texture="#wheel" if vanilla else 0)
    result=dict(name=name,faces=faces,**{"from":start,"to":end})
    if vanilla:
        if angle:
            result["rotation"]=dict(origin=[8,8,8],axis="z",angle=angle)
    else:
        px, py, pz = cube.get("pivot", [0,8,0])
        result.update(type="cube",box_uv=False,origin=[8-px,py,8+pz],rotation=[0,0,angle],uuid=element_id)
    return result


def build():
    texture_template = json.loads((DESIGN/"primitive_water_wheel.bbmodel").read_text())["textures"][0]
    atlas_dir = ASSETS / "textures/block"
    atlas = atlas_dir / "water_wheel_d.png"
    plate_atlas(atlas_dir / "large_water_wheel.png", atlas)
    embedded_atlas = "data:image/png;base64," + base64.b64encode(atlas.read_bytes()).decode()
    palette = {"wood":(1,1,30,29), "dark":(1,33,30,27),
               "iron":(98,16,28,32), "brass":(65,1,17,17), "rivet":(98,52,6,6), "socket":(34,2,15,13)}
    for size, radius, segments, width, socket in (
            ("primitive",7.7,8,6.1,3), ("medium",21.5,12,9.5,4), ("large",37.5,12,13.2,6)):
        name = f"{size}_water_wheel"
        path = ASSETS / f"geckolib/models/block/{name}.geo.json"
        geo = json.loads(path.read_text())
        # The wheel is supported at its axle. The legacy stand obscured the hub.
        geo["minecraft:geometry"][0]["bones"] = [dict(
            name="rotor", pivot=[0,8,0], cubes=reference_rotor(radius,segments,width,socket,palette))]
        spin = geo["minecraft:geometry"][0]["bones"][0]
        save(path,geo)
        def uid(key): return str(uuid.uuid5(uuid.NAMESPACE_URL,f"quantumindustries:{name}:{key}"))
        groups, elements=[],[]
        for bone in (spin,):
            group=bone["name"]
            children=[]
            for i,c in enumerate(bone["cubes"]):
                e=element(c,c.get("name",group.title()),uid(f"{group}/{i}"))
                children.append(e["uuid"])
                elements.append(e)
            groups.append(dict(name=group,uuid=uid(group),origin=[8,8,8],children=children,
                               export=True,isOpen=True))
        texture = dict(texture_template, name=atlas.name, uuid=uid("texture"), source=embedded_atlas)
        block_path = ASSETS / f"models/block/{name}.json"
        block = json.loads(block_path.read_text())
        block["textures"] = {"particle":"quantumindustries:block/water_wheel_d"}
        if size == "primitive":
            block["elements"] = []
        save(block_path, block)
        save(DESIGN/f"{name}.bbmodel",dict(
            meta=dict(format_version="4.10",model_format="free",box_uv=False),name=name,
            resolution=dict(width=128,height=128),elements=elements,outliner=groups,textures=[texture],animations=[]))

        if size=="primitive":
            item_path=ASSETS/f"models/item/{name}.json"
            item=json.loads(item_path.read_text())
            mesh=[element(c,c["name"],None,vanilla=True) for c in spin["cubes"]]
            item["elements"] = mesh
            item["textures"] = {"wheel":"quantumindustries:block/water_wheel_d",
                                "particle":"quantumindustries:block/water_wheel_d"}
            save(item_path,item)
            parts_path=ASSETS/"mechanical/parts.json"
            parts=json.loads(parts_path.read_text())
            parts[name]=[
                dict(name=e["name"],group="rotor",**{"from":e["from"],"to":e["to"]},
                     **({"rotation":e["rotation"]} if "rotation" in e else {})) for e in mesh]
            # Patch only this entry, preserving formatting and edits in other parts.
            raw=parts_path.read_text()
            start=raw.index(f'"{name}"')
            begin=raw.index("[",start)
            _, end=json.JSONDecoder().raw_decode(raw[begin:])
            value=json.dumps(parts[name],ensure_ascii=False,indent=2).replace("\n","\n  ")
            parts_path.write_text(raw[:begin]+value+raw[begin+end:])


if __name__=="__main__":
    build()
