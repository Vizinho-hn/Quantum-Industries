"""Reproduce PR 2 resources only; does not rewrite the legacy workshop atlas.

Geometry uses Bedrock per-face UVs for QksGeoModelLoader. Pillow is a
regeneration-only dependency. Run from any directory; no Gradle required.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/quantumindustries'
DATA = ROOT / 'src/main/resources/data/quantumindustries'
PIVOT = [0, 8, 0]
ATLAS_SIZE = 128
TILE = 32
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
AXIS = ('small_cogwheel', 'large_cogwheel', 'iron_flywheel', 'mechanical_pulley')
WHEELS = ('medium_water_wheel', 'large_water_wheel')
BLOCKS = AXIS + WHEELS
# Deep, shadow, mid, light, highlight. Requested Era I materials only.
PALETTES = {
    'oak': ('#2B1A0F', '#4B3322', '#6D4C34', '#845735', '#9C6A43'),
    'iron': ('#111214', '#2B2E33', '#3B3E44', '#565A62', '#747981'),
    'bronze': ('#2E2212', '#6E4F24', '#9C7438', '#C5994E', '#D1AE6D'),
    'leather': ('#2E190E', '#5B361D', '#7D4E2D', '#9E6841', '#B98559'),
}
MATERIALS = tuple(PALETTES)


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf8')


def atlas():
    image = Image.new('RGBA', (ATLAS_SIZE, ATLAS_SIZE))
    draw = ImageDraw.Draw(image)
    for column, material in enumerate(MATERIALS):
        p = PALETTES[material]
        for row in range(4):
            x, y = column*TILE, row*TILE
            draw.rectangle((x,y,x+31,y+31), fill=p[2])
            draw.line((x,y,x+31,y), fill=p[4])
            draw.line((x,y+1,x,y+30), fill=p[3])
            draw.line((x+31,y+1,x+31,y+31), fill=p[0])
            draw.line((x+1,y+31,x+30,y+31), fill=p[1])
            if material in ('oak','leather'):
                for v in (7,15,23):
                    draw.line((x+1,y+v,x+30,y+v), fill=p[1])
                    draw.line((x+2,y+v-1,x+29,y+v-1), fill=p[3])
                if material == 'oak':
                    for u,v in ((4,3),(17,10),(8,19),(22,26)):
                        draw.line((x+u,y+v,x+u+6,y+v), fill=p[3])
                        draw.line((x+u+2,y+v+1,x+u+7,y+v+1), fill=p[1])
                    if row == 1:
                        for inset in (6,11):
                            draw.rectangle((x+inset,y+inset,x+31-inset,y+31-inset), outline=p[1])
                else:
                    for u in range(3,30,4):
                        draw.point((x+u,y+3), fill=p[4])
                        draw.point((x+u,y+28), fill=p[0])
            else:
                for v in (9,21):
                    draw.line((x+4,y+v,x+15,y+v), fill=p[3])
                    draw.line((x+17,y+v+3,x+25,y+v+3), fill=p[1])
                for u,v in ((4,4),(25,25)):
                    draw.rectangle((x+u,y+v,x+u+3,y+v+3), fill=p[0])
                    draw.rectangle((x+u,y+v,x+u+2,y+v+2), fill=p[3])
                    draw.point((x+u,y+v), fill=p[4])
    return image


def cube(name, origin, size, material='oak', angle=0):
    column = MATERIALS.index(material)
    spans = ((size[0],size[1]),)*2 + ((size[2],size[1]),)*2 + ((size[0],size[2]),)*2
    uv = {}
    for index, (face, (w,h)) in enumerate(zip(FACES, spans)):
        # A few faces share the same grain; no oversized box UV unwrapping.
        uv[face] = {'uv': [column*TILE+1, (index % 4)*TILE+1],
                    'uv_size': [min(30,max(8,math.ceil(w)*2)), min(30,max(8,math.ceil(h)*2))]}
    result = {'name': name, 'origin': origin, 'size': size, 'uv': uv}
    if angle:
        result.update(pivot=PIVOT, rotation=[0,0,angle])
    return result


def radial(name, radius, width, thickness, depth, material, angle, z=0):
    return cube(name, [-width/2,8+radius-thickness/2,z-depth/2],
                [width,thickness,depth], material, angle)


def ring(radius, thickness, depth, count, material, z=0, label='Rim'):
    # Segments meet at the inner polygon; discrete mitres remain visible.
    width = 2*(radius-thickness/2)*math.tan(math.pi/count)
    return [radial(f'{label} {i+1:02}', radius,width,thickness,depth,material,i*360/count,z)
            for i in range(count)]


def axle():
    return [cube('Coupling tenon', [-2,6,-8], [4,4,16]),
            cube('Bronze hub',[-3,5,-2.75],[6,6,5.5],'bronze')]


def spokes(count, reach, width=1.5, depth=2, material='oak'):
    return [cube(f'Spoke {i+1:02}',[-width/2,8,-depth/2],[width,reach,depth],material,i*360/count)
            for i in range(count)]


def mounts(reinforced=False):
    parts = []
    for end,z in (('North',-7),('South',5.5)):
        parts.append(cube(end+' foot',[-6,0,z],[12,1.25,1.5],'iron'))
        for side,x in (('L',-5),('R',3)):
            leg = cube(end+side+' inclined timber', [x,1.25,z+.25],[2,4.75,1])
            leg.update(pivot=[x+1,1.25,z+.75], rotation=[0,0,-18 if x < 0 else 18])
            parts.append(leg)
            parts.append(cube(end+side+' iron strap',[x-.25,2,z],[2.5,1,1.5],'iron'))
        parts.append(cube(end+' bearing saddle',[-4,5.5,z],[8,.75,1.5],'iron'))
        # Four bars frame an open square so the rotating tenon stays visible.
        parts.extend([cube(end+' bearing lower',[-3,5,z],[6,1,1.5],'bronze'),
                      cube(end+' bearing upper',[-3,10,z],[6,1,1.5],'bronze'),
                      cube(end+' bearing left',[-3,6,z],[1,4,1.5],'bronze'),
                      cube(end+' bearing right',[2,6,z],[1,4,1.5],'bronze')])
        if reinforced:
            parts.append(cube(end+' tie beam',[-5,3,z],[10,1,1.5],'iron'))
    return parts


def gears(large=False):
    count, tip, rim_radius = (16,12,9.1) if large else (8,8.5,5.2)
    result = axle()+spokes(8 if large else 4,rim_radius)
    result += ring(rim_radius,1.5,3,count,'iron',label='Bolted ring')
    # Teeth bridge the ring to the full tip radius; staggered sides avoid a solid disc.
    for i in range(count):
        result.append(cube(f'Tooth {i+1:02}',[-1.1,8+rim_radius,-1.25],
                           [2.2,tip-rim_radius,2.5],'oak',i*360/count))
        result.append(radial(f'Ring bolt {i+1:02}',rim_radius,.75,.75,.75,'bronze',i*360/count,-1.75))
    return result


def flywheel():
    result = axle()+spokes(6,5.5,1.5,2,'iron')+ring(6.25,1.5,4,12,'iron')
    for i in range(2):
        result.append(radial(f'Counterweight {i}',5.3,3,2,4.5,'bronze',i*180))
    return result


def pulley():
    # Two flanges leave the entire central six-pixel belt track recessed.
    result = axle()+spokes(8,3,2,6)
    result += ring(3.7,2,6,12,'oak',label='Recessed belt drum')
    for z in (-3.75,3.75):
        result += ring(4.65,1.5,1.5,12,'bronze',z,label=f'Flange {z}')
    return result


def water_wheel(large=False):
    radius, count, spoke_count = (40,16,16) if large else (24,12,8)
    result = axle()+spokes(spoke_count,radius-4,2 if large else 1.5,2)
    for z in (-3.25,3.25):
        result += ring(radius-4,2.5,1.5,count,'oak',z,label=f'Timber rim {z}')
        result += ring(radius-4,1,1,count,'iron',z+(.9 if z>0 else -.9),label=f'Iron band {z}')
    # Tangential floors have exact +/- radius cardinal extent. Upright lips
    # and side cheeks form open buckets rather than solid paddle boxes.
    width = (radius-5)*2*math.tan(math.pi/count)*.8
    for i in range(count):
        angle = i*360/count
        result.append(cube(f'Bucket floor {i+1:02}',[-width/2,8+radius-3,-4.5],
                           [width,1.5,9],'oak',angle))
        result.append(cube(f'Bucket lip {i+1:02}',[width/2-1,8+radius-3,-4.5],
                           [1,3,9],'oak',angle))
        for z in (-4.5,3.5):
            result.append(cube(f'Bucket cheek {i+1:02} {z}',[-width/2,8+radius-3,z],
                               [width,3,1],'iron',angle))
        result.append(radial(f'Hub strap {i+1:02}',4,1,3,1,'bronze',angle,-3.25))
    return result


def icon(name):
    """32-pixel inventory silhouettes with discrete material ramps and cutouts."""
    im = Image.new('RGBA',(32,32))
    d = ImageDraw.Draw(im)
    oak, iron, bronze, leather = (PALETTES[m] for m in MATERIALS)
    if name == 'drive_belt':
        # Rolled leather with hollow center, stitched edges and brass splice.
        for bounds,color in (((3,5,28,27),leather[0]),((4,5,27,25),leather[1]),
                             ((5,5,26,23),leather[2]),((6,6,25,21),leather[3])):
            d.ellipse(bounds,fill=color)
        d.ellipse((9,9,22,18),fill=leather[0])
        d.ellipse((10,10,21,17),fill=(0,0,0,0))
        d.arc((6,6,25,22),190,300,fill=leather[4],width=1)
        for x,y in ((7,10),(6,14),(8,19),(12,22),(17,23),(22,21)):
            d.point((x,y),fill=leather[4])
        d.rectangle((21,17,26,21),fill=bronze[0])
        d.rectangle((22,17,25,20),fill=bronze[2])
        d.line((22,17,25,17),fill=bronze[4])
        return im
    water = name in WHEELS
    gear = 'cogwheel' in name
    outer = 12 if water or name=='large_cogwheel' else 10
    if gear:
        count = 16 if name=='large_cogwheel' else 8
        for i in range(count):
            a = i*2*math.pi/count
            x,y = round(16+outer*math.sin(a)),round(15-outer*math.cos(a))
            d.rectangle((x-2,y-2,x+2,y+2),fill=oak[0])
            d.rectangle((x-1,y-2,x+1,y+1),fill=oak[3])
    if water or name in ('iron_flywheel','mechanical_pulley'):
        d.rectangle((8,24,11,28),fill=iron[1]);d.rectangle((21,24,24,28),fill=iron[1])
        d.rectangle((6,28,26,29),fill=iron[0]);d.line((7,28,25,28),fill=iron[3])
    for radius,color in ((outer,iron[0]),(outer-1,oak[3] if water else iron[3]),
                         (outer-2,oak[2] if water else iron[2]),(outer-3,iron[1])):
        d.ellipse((16-radius,15-radius,16+radius,15+radius),fill=color)
    hole=outer-4
    d.ellipse((16-hole,15-hole,16+hole,15+hole),fill=(0,0,0,0))
    for i in range(8 if water else 4):
        a=i*math.pi/(4 if water else 2)
        end=(round(16+(outer-2)*math.sin(a)),round(15-(outer-2)*math.cos(a)))
        d.line((16,15,*end),fill=oak[0] if water or gear else iron[0],width=3)
        d.line((16,14,end[0],end[1]-1),fill=oak[3] if water or gear else iron[3],width=1)
    if water:
        for i in range(12 if name==WHEELS[0] else 16):
            a=i*math.pi*2/(12 if name==WHEELS[0] else 16)
            x,y=round(16+outer*math.sin(a)),round(15-outer*math.cos(a))
            d.rectangle((x-1,y-1,x+1,y+1),fill=oak[1])
            d.point((x-1,y-1),fill=oak[4])
    if name=='mechanical_pulley':
        d.ellipse((8,7,24,23),fill=bronze[0]);d.ellipse((9,7,23,21),fill=bronze[2])
        d.ellipse((11,9,21,20),fill=oak[0]);d.ellipse((12,9,20,18),fill=oak[2])
    d.rectangle((12,11,20,19),fill=bronze[0]);d.rectangle((13,11,19,17),fill=bronze[2])
    d.line((13,11,19,11),fill=bronze[4]);d.rectangle((15,13,17,16),fill=iron[0])
    return im


RECIPES = {
    'small_cogwheel': ([' P ','ISI',' P '], {'P':'#minecraft:planks','I':'minecraft:iron_ingot','S':'quantumindustries:mechanical_shaft_block'}),
    'large_cogwheel': (['PIP','ISI','PIP'], {'P':'#minecraft:planks','I':'minecraft:iron_ingot','S':'quantumindustries:small_cogwheel'}),
    'iron_flywheel': (['IBI','GSG','IBI'], {'I':'minecraft:iron_ingot','B':'quantumindustries:bronze_ingot','G':'quantumindustries:iron_gear','S':'quantumindustries:mechanical_shaft_block'}),
    'mechanical_pulley': (['BIB','PSP','BIB'], {'B':'quantumindustries:bronze_ingot','I':'minecraft:iron_ingot','P':'#minecraft:planks','S':'quantumindustries:mechanical_shaft_block'}),
    'drive_belt': (['LLL','B B','LLL'], {'L':'minecraft:leather','B':'quantumindustries:bronze_ingot'}),
    'medium_water_wheel': (['PIP','PWP','PIP'], {'P':'#minecraft:planks','I':'minecraft:iron_ingot','W':'quantumindustries:primitive_water_wheel'}),
    'large_water_wheel': (['PIP','BWB','PIP'], {'P':'#minecraft:planks','I':'minecraft:iron_ingot','B':'quantumindustries:bronze_ingot','W':'quantumindustries:medium_water_wheel'}),
}
NAMES = {
    'en_us': ('Small Cogwheel','Large Cogwheel','Iron Flywheel','Mechanical Pulley','Medium Water Wheel','Large Water Wheel','Drive Belt'),
    'pt_br': ('Engrenagem Pequena','Engrenagem Grande','Volante de Ferro','Polia Mecânica',"Roda-d’Água Média","Roda-d’Água Grande",'Correia de Transmissão'),
}
TOOLTIPS = {
    'en_us': ('Eight wooden teeth on an iron ring. Couples to a mechanical shaft.',
              'Sixteen wooden teeth with reinforced spokes and an iron ring.',
              'A heavy cast-iron rotor stores energy supplied by the shaft.',
              'A grooved wooden drum with bronze flanges for drive belts.',
              'A 3×3 oak paddle wheel. Leave room for the rim to turn.',
              'A 5×5 twin-ring oak water wheel with deep buckets.',
              'Use on two pulleys to link them; sneak for a crossed belt. Use a wrench to unlink.'),
    'pt_br': ('Oito dentes de madeira em aro de ferro. Acopla a um eixo mecânico.',
              'Dezesseis dentes de madeira, raios reforçados e aro de ferro.',
              'Um rotor pesado de ferro fundido armazena energia fornecida pelo eixo.',
              'Tambor de madeira com canal e flanges de bronze para correias.',
              'Roda de pás de carvalho 3×3. Reserve espaço para o aro girar.',
              'Roda-d’água de carvalho 5×5 com aro duplo e caçambas profundas.',
              'Use em duas polias para conectá-las; agache para cruzar a correia. Use uma chave para desconectar.'),
}
MESSAGES = {
    'select_first': ('First pulley selected. Use the belt on a second pulley.', 'Primeira polia selecionada. Use a correia em uma segunda polia.'),
    'linked_open': ('Pulleys connected with an open belt.', 'Polias conectadas com correia aberta.'),
    'linked_crossed': ('Pulleys connected with a crossed belt.', 'Polias conectadas com correia cruzada.'),
    'unlinked': ('Drive belt disconnected.', 'Correia de transmissão desconectada.'),
    'invalid_target': ('Select two compatible pulleys with parallel shafts.', 'Selecione duas polias compatíveis com eixos paralelos.'),
    'too_far': ('The second pulley is too far away.', 'A segunda polia está longe demais.'),
}


def main():
    texture = atlas()
    rotors = (gears(),gears(True),flywheel(),pulley(),water_wheel(),water_wheel(True))
    for name,rotor in zip(BLOCKS,rotors):
        bones = [{'name':'rotor','pivot':PIVOT,'cubes':rotor}]
        if name not in AXIS[:2]:
            bones.insert(0,{'name':'base','pivot':PIVOT,'cubes':mounts(name==WHEELS[1])})
        size = 6 if name==WHEELS[1] else 4 if name==WHEELS[0] else 2
        geo = {'format_version':'1.12.0','minecraft:geometry':[{
            'description':{'identifier':'geometry.'+name,'texture_width':ATLAS_SIZE,'texture_height':ATLAS_SIZE,
                           'visible_bounds_width':size,'visible_bounds_height':size,'visible_bounds_offset':[0,.5,0]},
            'bones':bones}]}
        save(ASSETS/f'geckolib/models/block/{name}.geo.json',geo)
        texture.save(ASSETS/f'textures/block/{name}.png')
        save(ASSETS/f'models/block/{name}.json',{'parent':'minecraft:block/block',
             'textures':{'particle':f'quantumindustries:block/{name}'},'elements':[]})
        variants = {f'axis={a}':{'model':f'quantumindustries:block/{name}',**r}
                    for a,r in (('z',{}),('x',{'y':90}),('y',{'x':90}))} if name in AXIS else {
                    f'facing={a}':{'model':f'quantumindustries:block/{name}',**({'y':r} if r else {})}
                    for a,r in (('north',0),('east',90),('south',180),('west',270))}
        save(ASSETS/f'blockstates/{name}.json',{'variants':variants})
        save(DATA/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,
             'entries':[{'type':'minecraft:item','name':'quantumindustries:'+name}],
             'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for name in BLOCKS+('drive_belt',):
        icon(name).save(ASSETS/f'textures/item/{name}.png')
        save(ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':'quantumindustries:item/'+name}})
        save(ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'quantumindustries:item/'+name}})
        pattern,key=RECIPES[name]
        save(DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':key,
                                      'result':{'id':'quantumindustries:'+name,'count':1}})
    for locale_index,locale in enumerate(('en_us','pt_br')):
        path=ASSETS/f'lang/{locale}.json'
        lang=json.loads(path.read_text(encoding='utf8'))
        for name,title,tooltip in zip(BLOCKS+('drive_belt',),NAMES[locale],TOOLTIPS[locale]):
            kind='item' if name=='drive_belt' else 'block'
            lang[f'{kind}.quantumindustries.{name}']=title
            lang[f'tooltip.quantumindustries.{name}']=tooltip
        for suffix,translations in MESSAGES.items():
            lang['message.quantumindustries.belt.'+suffix]=translations[locale_index]
        save(path,lang)
    files = []
    for name in BLOCKS:
        files.extend([ASSETS/f'geckolib/models/block/{name}.geo.json',
                      ASSETS/f'textures/block/{name}.png', ASSETS/f'blockstates/{name}.json',
                      ASSETS/f'models/block/{name}.json', DATA/f'loot_table/blocks/{name}.json'])
    for name in BLOCKS+('drive_belt',):
        files.extend([ASSETS/f'items/{name}.json', ASSETS/f'models/item/{name}.json',
                      ASSETS/f'textures/item/{name}.png', DATA/f'recipe/{name}.json'])
    files.extend(ASSETS/f'lang/{locale}.json' for locale in NAMES)
    support = ('tools/generate-qks-pr2-resources.py','tools/preview-qks-pr2-resources.py',
               'tools/qks-pr2-review.png','tools/qks-pr2-resource-manifest.json',
               'tests/test_qks_pr2_resources.py')
    save(ROOT/'tools/qks-pr2-resource-manifest.json', {
        'resources': sorted(str(p.relative_to(ROOT)) for p in files), 'support': list(support)})
    print('Generated 6 centered QKS geometries, 13 PNGs, 6 blockstates/particle models/loot tables, 7 item chains/recipes and 2 locales.')


if __name__ == '__main__':
    main()
