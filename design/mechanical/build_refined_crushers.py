"""Generate the two editable Blockbench rigs and the matching QKS mesh assets."""
import base64
import json
import random
import math
import uuid
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "design/mechanical"
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
ATLAS = {"wood": (3, 3, 35, 35), "dark": (43, 3, 73, 34),
         "steel": (3, 43, 35, 69), "iron": (75, 43, 105, 67),
         "gold": (4, 77, 42, 97), "stone": (97, 105, 121, 125)}
QUADS = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4),
         (3, 7, 6, 2), (0, 4, 7, 3), (1, 2, 6, 5)]


class Crusher:
    def __init__(self, name, wide):
        self.name = name
        self.wide = wide
        self.parts = defaultdict(list)

    def mesh(self, name, vertices, faces, material, bone="frame"):
        self.parts[bone].append((name, vertices, faces, ATLAS[material]))

    def box(self, name, low, high, material, bone="frame"):
        x, y, z = low
        X, Y, Z = high
        self.mesh(name, [(x,y,z),(X,y,z),(X,Y,z),(x,Y,z),
                         (x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)], QUADS, material, bone)

    def bar(self, name, x0, x1, start, end, width, material, bone="frame"):
        y,z = start
        Y,Z = end
        length = math.hypot(Y-y,Z-z)
        a,b = -(Z-z)*width/(2*length), (Y-y)*width/(2*length)
        yz = [(y-a,z-b),(Y-a,Z-b),(Y+a,Z+b),(y+a,z+b)]
        self.mesh(name, [(x,*p) for x in (x0,x1) for p in yz], QUADS, material, bone)

    def ring(self, name, x0, x1, radius, inner, material, bone):
        count = 16
        vertices = [(x,8+r*math.cos(i*math.tau/count),8+r*math.sin(i*math.tau/count))
                    for x,r in [(x0,radius),(x1,radius),(x0,inner),(x1,inner)] for i in range(count)]
        faces = []
        for i in range(count):
            j = (i+1)%count
            faces.extend([(j,i,i+count,j+count), (i,j,j+2*count,i+2*count),
                          (j+count,i+count,i+3*count,j+3*count),
                          (i+2*count,j+2*count,j+3*count,i+3*count)])
        self.mesh(name, vertices, [f[::-1] for f in faces], material, bone)

    def inlet(self, center):
        rings = [(16,5),(16,3.8),(12.4,2.15),(12.4,2.9)]
        vertices = [(center+x,y,8+z) for y,r in rings for x,z in [(-r,-r),(r,-r),(r,r),(-r,r)]]
        faces = [(a+i,b+i,b+(i+1)%4,a+(i+1)%4)
                 for a,b in [(0,4),(4,8),(8,12),(12,0)] for i in range(4)]
        self.mesh("Cuba integrada",vertices,faces,"steel")

    def chute(self, center, front, rear):
        self.bar("Fundo da calha",center-4.5,center+4.5,(3,front),(8,rear),.8,"steel")
        for x in (center-4.5,center+3.6):
            self.bar("Guia da calha",x,x+.9,(4.1,front),(9.1,rear),1.6,"iron")

    def bay(self, x, z, top, label, solid_base=True):
        if solid_base:
            self.box(label+" • base",(x+.5,0,z+.5),(x+15.5,2,z+15.5),"dark")
        for a in (x+.6,x+13.5):
            for b in (z+.6,z+13.5):
                self.box("Montante",(a,1.5,b),(a+1.9,top,b+1.9),"wood")
                self.box("Sapata",(a-.25,0,b-.25),(a+2.15,2.35,b+2.15),"iron" if solid_base else "stone")
                self.box("Rebite",(a+.65,top-.12,b+.65),(a+1.25,top+.02,b+1.25),"gold")
        rail_start, rail_end = (.6, 15.4) if solid_base else (2.5, 13.5)
        for b in (z+.6,z+13.5):
            self.box("Travessa",(x+rail_start,top-1.6,b),(x+rail_end,top,b+1.9),"wood")
        for a in (x+.6,x+13.5):
            self.box("Longarina",(a,top-1.6,z+2.5),(a+1.9,top,z+13.5),"dark" if solid_base else "wood")

    def compact_service_faces(self):
        for start, end in [
                ((.55,0,2.75),(1.75,2,13.25)), ((14.25,0,2.75),(15.45,2,13.25)),
                ((2.75,0,.55),(13.25,2,1.75)), ((2.75,0,14.25),(13.25,2,15.45))]:
            self.box("Viga de madeira da base", start, end, "wood")
        for start, end in [
                ((1.75,0,1.75),(5.3,2,14.25)), ((10.7,0,1.75),(14.25,2,14.25)),
                ((5.3,0,1.75),(10.7,2,5.3)), ((5.3,0,10.7),(10.7,2,14.25))]:
            self.box("Prancha da base", start, end, "wood")

        # Bottom-facing chute: outer lip narrows to a dark throat inside the block.
        rings = [(0,2.7), (0,1.95), (1.7,1.1), (1.7,2.7)]
        vertices = [(8+x,y,8+z) for y,r in rings for x,z in [(-r,-r),(r,-r),(r,r),(-r,r)]]
        faces = [(a+(i+1)%4,b+(i+1)%4,b+i,a+i)
                 for a,b in [(0,4),(4,8),(8,12),(12,0)] for i in range(4)]
        self.mesh("Moldura inferior de pedra lisa", vertices, faces[:4], "stone")
        self.mesh("Interior do bocal de extração", vertices, faces[4:], "iron")

        for bottom, top in ((2.35,6.15), (6.3,10.2), (10.35,14.2)):
            self.box("Prancha lateral", (13.65,bottom,2.45), (14.85,top,13.55), "wood")
            self.box("Prancha traseira", (2.45,bottom,13.65), (13.55,top,14.85), "wood")
        for bottom, top in ((5.7,6.55), (14.05,15.35)):
            self.box("Faixa traseira de pedra lisa", (2.45,bottom,14.83), (13.6,top,15.62), "stone")
            self.box("Faixa lateral de pedra lisa", (14.83,bottom,2.45), (15.62,top,13.6), "stone")
        for z in (1.05,13.6):
            self.box("Aplique de pedra lisa do lado QKS", (15.39,2.35,z), (15.66,15.35,z+1.3), "stone")
        for x in (1.05,13.6):
            self.box("Aplique de pedra lisa traseiro", (x,2.35,15.39), (x+1.3,15.35,15.66), "stone")
            for y in (3,6,13.3):
                self.box("Rebite traseiro", (x+.43,y,15.66), (x+.82,y+.39,15.75), "gold")
        for z in (1.7,14.25):
            for y in (3,6,13.3):
                self.box("Rebite lateral", (15.66,y,z-.2), (15.75,y+.39,z+.2), "gold")

        self.box("Placa de pedra lisa do mancal QKS", (14.84,4.8,4.8), (15.24,11.2,11.2), "stone")
        self.box("Assento quadrado da entrada", (15.22,5.1,5.1), (15.52,10.9,10.9), "stone")

    def build(self):
        center = 24 if self.wide else 8
        if self.wide:
            self.bay(0,0,14.8,"Transmissão")
            self.bay(0,-16,12,"Apoio da biela")
            self.bay(16,0,15.65,"Câmara")
            self.bay(16,-16,12,"Descarga")
            self.bar("Travamento diagonal",1.2,2.6,(3,-14.2),(11,-1.8),1.6,"wood")
            self.bar("Contraventamento",1.2,2.6,(11,-14.2),(3,-1.8),1.6,"dark")
            self.box("Eixo transversal",(3.9,7,7),(21,9,9),"iron","rotor")
            self.chute(center,-15.1,-6)
            jaw_z = -5.5
        else:
            self.bay(0,0,15.65,"Chassi",solid_base=False)
            self.compact_service_faces()
            self.box("Viga frontal inferior", (2.5,1.6,.85), (13.5,3.15,2.3), "wood")
            self.box("Painel atrás do volante", (4.2,2.2,2.5), (5,14.35,13.5), "dark")
            self.box("Travessa sob a cuba", (2.5,12.1,2.45), (13.5,14.45,3.6), "wood")
            for x in (2.5,12):
                self.box("Ombreira da descarga", (x,3,2.5), (x+1.5,12.2,4.1), "dark")
            self.chute(center,.85,5)
            jaw_z = 5.5
        self.inlet(center)
        self.box("Fundo do corpo",(center-5,2.2,7),(center+5,9,14),"dark")
        for x in (center-5,center+4):
            self.box("Placa lateral",(x,3,jaw_z),(x+1,12,13),"iron")
        self.box("Mandíbula fixa",(center-3.6,9.6,jaw_z),(center+3.6,11.5,jaw_z+1.6),"steel")
        self.box("Mandíbula móvel",(center-3.6,6.6,jaw_z+1),(center+3.6,8.5,jaw_z+2.6),"steel","moving_jaw")
        for i in range(4):
            x = center-3.35+i*1.75
            self.box("Dente fixo",(x,8.85,jaw_z),(x+.9,9.65,jaw_z+1),"iron")
            self.box("Dente móvel",(x,8.5,jaw_z+1),(x+.9,9.2,jaw_z+2),"iron","moving_jaw")
        self.ring("Aro do volante",.8,2.55,5.55,4.55 if self.wide else 3.7,"wood","rotor")
        if not self.wide:
            self.ring("Disco de madeira",1.1,2.15,3.72,1.35,"wood","rotor")
            self.ring("Filete do aro",.65,.84,5.43,5.12,"dark","rotor")
        for dy,dz in ((4.65,0),(0,4.65),(3.28,3.28),(3.28,-3.28)):
            self.bar("Raio do volante",1,2.3,(8-dy,8-dz),(8+dy,8+dz),.72,"dark","rotor")
        self.box("Cubo metálico",(.1,6,6),(2.5,10,10),"iron","rotor")
        self.box("Pino excêntrico",(2.56,8.5,7.35),(4.1,9.8,8.65),"gold","crank_pin")
        self.bar("Biela",2.65,3.8,(9.15,8),(5,-8 if self.wide else 2.4),1.1,"steel","connecting_rod")
        self.box("Mancal da biela",(2.5,4.3,-8.8 if self.wide else 1.55),
                 (4,5.7,-7.2 if self.wide else 3.25),"iron")

    def export(self, texture):
        norm = 32 if self.wide else 16
        offset = 16 if self.wide else 0
        bones, groups, outline, elements = [], [], [], []
        pivots = {"rotor":(0,8,8),"crank_pin":(0,8,8),"connecting_rod":(0,9.15,8),
                  "moving_jaw":(24 if self.wide else 8,8,-4 if self.wide else 7)}
        def uid(label):
            return str(uuid.uuid5(uuid.NAMESPACE_URL,f"quantumindustries/{self.name}/{label}"))
        for bone, parts in self.parts.items():
            pivot = pivots.get(bone,(0,0,0))
            bone_id = uid(bone)
            groups.append(dict(name=bone,uuid=bone_id,origin=list(pivot),rotation=[0,0,0],export=True))
            children, mesh_faces = [], []
            for name, verts, faces, (u,v,U,V) in parts:
                ident = uid(str(len(elements)))
                children.append(ident)
                uv = [(u,v),(U,v),(U,V),(u,V)]
                bbfaces = {}
                for i, face in enumerate(faces):
                    keys = [f"v{j:03d}" for j in face]
                    bbfaces[f"f{i:03d}"] = dict(vertices=keys,texture=0,uv=dict(zip(keys,uv)))
                    mesh_faces.append([[round(x/norm,7),round(y/norm,7),round((z+offset)/norm,7),a/128,b/128]
                                       for (x,y,z),(a,b) in zip((verts[j] for j in face),uv)])
                elements.append(dict(name=name,uuid=ident,type="mesh",origin=[0,0,0],rotation=[0,0,0],
                                     vertices={f"v{i:03d}":list(p) for i,p in enumerate(verts)},faces=bbfaces,
                                     export=True,visibility=True,shading=True))
            outline.append(dict(uuid=bone_id,children=children,isOpen=True))
            bones.append(dict(name=bone,pivot=[0,0,0],mesh_faces=mesh_faces))
        geo = {"format_version":"1.12.0","minecraft:geometry":[{"description":{
            "identifier":f"geometry.{self.name}","texture_width":128,"texture_height":128},"bones":bones}]}
        (ASSETS/f"geckolib/models/block/{self.name}.geo.json").write_text(json.dumps(geo,separators=(",",":"))+"\n")
        bb = dict(meta=dict(format_version="5.0",model_format="free",box_uv=False),name=self.name,
                  resolution=dict(width=128,height=128),elements=elements,groups=groups,outliner=outline,
                  textures=[texture],animations=[])
        (DESIGN/f"{self.name}.bbmodel").write_text(json.dumps(bb,ensure_ascii=False,separators=(",",":"))+"\n")


def write_stone_swatch(path):
    """Reserve an unused patch of the atlas for matte, smooth stone."""
    from PIL import Image

    with Image.open(path) as image:
        atlas = image.convert("RGBA")
    rng = random.Random(47)
    shades = [[rng.choice((-4, -3, -1, 0, 1, 3, 4)) for _ in range(13)] for _ in range(11)]
    for y in range(104, 126):
        for x in range(96, 122):
            local_x = x - 96
            local_y = y - 104
            coarse = shades[local_y // 2][(local_x + local_y // 2) // 2 % 13]
            value = 143 + coarse + rng.randint(-2, 2)
            atlas.putpixel((x, y), (value, value, value - 2, 255))
    atlas.save(path)


if __name__ == "__main__":
    atlas = ASSETS / "textures/block/jaw_crusher_a.png"
    write_stone_swatch(atlas)
    texture = json.loads((DESIGN/"jaw_crusher.bbmodel").read_text())["textures"][0]
    texture["source"] = "data:image/png;base64," + base64.b64encode(atlas.read_bytes()).decode()
    for suffix, wide in (("compact",False),("multiblock",True)):
        machine = Crusher(f"jaw_crusher_{suffix}",wide)
        machine.build()
        machine.export(texture)
