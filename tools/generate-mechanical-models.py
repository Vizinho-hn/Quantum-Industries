"""Development-only exporter. Pillow is needed only when regenerating the original pixel atlas."""
import base64
import json
import uuid
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/quantumindustries'
DATA = ROOT / 'src/main/resources/data/quantumindustries'
DESIGN = ROOT / 'design/mechanical'
def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')

palette = ['#684832','#846347','#555753','#999C98','#A76B45','#3C3934','#B9AE91','#D2C9B2']
texture = Image.new('RGB',(64,32))
for i,color in enumerate(palette):
    tile=Image.new('RGB',(16,16),color);d=ImageDraw.Draw(tile)
    rgb=tile.getpixel((0,0))
    dark=tuple(max(0,v-14) for v in rgb);light=tuple(min(255,v+12) for v in rgb)
    for y in (3,7,11,15):d.line((0,y,15,y),fill=dark)
    if i in (0,1):
        for x,y in [(2,1),(9,5),(4,10)]:d.line((x,y,x+3,y),fill=light)
    else:
        d.line((0,0,15,0),fill=light);d.line((0,0,0,15),fill=light)
    texture.paste(tile,((i%4)*16,(i//4)*16))
texpath=ASSETS/'textures/block/mechanical_workshop.png';texpath.parent.mkdir(parents=True,exist_ok=True);texture.save(texpath)
parts={}
def box(boxes,name,f,t,material=0,group='base'):
    boxes.append({'name':name,'from':f,'to':t,'material':material,'group':group})

saw=[]
for x in (1,12):
    for z in (1,12):box(saw,'Perna',[x,0,z],[x+3,6,z+3],0)
box(saw,'Prateleira',[1,1,1],[15,2,15],0)
for x in (0,9):box(saw,'Tampo dividido',[x,6,0],[x+7,7.5,16],1)
for x in (1,13):box(saw,'Mancal',[x,7.5,6],[x+2,10.5,10],2)
box(saw,'Eixo principal',[0,7.5,7.5],[16,8.5,8.5],3,'rotor')
box(saw,'Disco central',[7.6,3,3],[8.4,13,13],3,'rotor')
box(saw,'Disco vertical',[7.6,1.5,5],[8.4,14.5,11],3,'rotor')
box(saw,'Disco horizontal',[7.6,5,1.5],[8.4,11,14.5],3,'rotor')
for y,z in [(1,7),(2,4),(4,2),(7,1),(11,2),(13,4),(14,7),(13,11),(11,13),(7,14),(4,13),(2,11)]:
    box(saw,'Dente de serra',[7.5,y,z],[8.5,y+1,z+1],2,'rotor')
box(saw,'Cubo da lamina',[7.3,6.5,6.5],[8.7,9.5,9.5],2,'rotor')
box(saw,'Braco da manivela',[14.8,7.5,7.5],[15.4,8.5,12],4,'crank')
box(saw,'Punho da manivela',[14.8,7.2,11.3],[16,8.8,12.5],0,'crank')
parts['mechanical_sawmill']=saw

shaft=[]
for z in (1,12):
    box(shaft,'Mancal inferior',[4,4,z],[12,6,z+3],2)
    box(shaft,'Mancal superior',[4,10,z],[12,12,z+3],2)
    for x in (4,10):box(shaft,'Mancal lateral',[x,6,z],[x+2,10,z+3],2)
box(shaft,'Eixo de transmissao',[7,7,0],[9,9,16],3,'rotor')
box(shaft,'Chaveta',[8.7,7.8,0],[9.4,8.4,16],2,'rotor')
box(shaft,'Braco da manivela',[7.5,7.5,14.8],[8.5,12,15.4],4,'crank')
box(shaft,'Punho da manivela',[7.2,11.2,14.7],[8.8,12.5,16],0,'crank')
parts['mechanical_shaft_block']=shaft

wheel=[]
for z in (1,12):box(wheel,'Suporte',[6,0,z],[10,10,z+3],0)
box(wheel,'Eixo hidraulico',[7,7,0],[9,9,16],3,'rotor')
for f,t in [([4,14,6],[12,16,10]),([4,0,6],[12,2,10]),([0,4,6],[2,12,10]),([14,4,6],[16,12,10]),
            ([2,2,6],[4,4,10]),([12,2,6],[14,4,10]),([2,12,6],[4,14,10]),([12,12,6],[14,14,10])]:
    box(wheel,'Aro de madeira',f,t,1,'rotor')
box(wheel,'Raio horizontal',[1,7,7],[15,9,9],0,'rotor')
box(wheel,'Raio vertical',[7,1,7],[9,15,9],0,'rotor')
box(wheel,'Cubo de ferro',[6,6,5.5],[10,10,10.5],2,'rotor')
parts['primitive_water_wheel']=wheel

def element(b, bb=False):
    i=b['material']; u=(i%4)*4;v=(i//4)*8
    return {'name':b['name'],'from':b['from'],'to':b['to'],'faces':{
        f:{'uv':[u,v,u+4,v+8],'texture':'#workshop'} for f in ('north','south','east','west','up','down')}}

display={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.625,.625,.625]},
         'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},
         'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.5,.5,.5]}}
for name,boxes in parts.items():
    common={'parent':'minecraft:block/block','textures':{'workshop':'quantumindustries:block/mechanical_workshop','particle':'quantumindustries:block/mechanical_workshop'},'display':display}
    save(ASSETS/f'models/block/{name}.json',{**common,'elements':[element(b) for b in boxes if b['group']=='base']})
    save(ASSETS/f'models/item/{name}.json',{**common,'elements':[element(b) for b in boxes if b['group']!='crank']})
    save(ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'quantumindustries:item/{name}'}})
    if name=='mechanical_shaft_block':
        variants={'axis=z':{'model':f'quantumindustries:block/{name}'},'axis=x':{'model':f'quantumindustries:block/{name}','y':90},'axis=y':{'model':f'quantumindustries:block/{name}','x':90}}
    else:
        variants={f'facing={d}':{'model':f'quantumindustries:block/{name}',**({'y':a} if a else {})} for d,a in [('north',0),('east',90),('south',180),('west',270)]}
    save(ASSETS/f'blockstates/{name}.json',{'variants':variants})
    save(DATA/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'quantumindustries:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    cubes=[]
    for idx,b in enumerate(boxes):
        e=element(b);e['faces']={face:{'uv':[value*(4 if j%2==0 else 2) for j,value in enumerate(data['uv'])],'texture':0} for face,data in e['faces'].items()}
        cubes.append({**e,'type':'cube','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+str(idx))),'box_uv':False,'autouv':0,'origin':[8,8,8]})
    save(DESIGN/f'{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':name,'resolution':{'width':64,'height':32},'elements':cubes,'outliner':[
        {'name':g,'origin':[8,8,8],'children':[e['uuid'] for e,b in zip(cubes,boxes) if b['group']==g]} for g in ('base','rotor','crank')],
        'textures':[{'name':'mechanical_workshop.png','id':'0','width':64,'height':32,'uv_width':64,'uv_height':32,'source':'data:image/png;base64,'+base64.b64encode(texpath.read_bytes()).decode()}],'display':display})
save(ASSETS/'mechanical/parts.json',parts)
save(DATA/'recipe/mechanical_sawmill.json',{'type':'quantumindustries:foundations','craft':'mechanical_sawmill'})
for wood in ('oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','pale_oak','crimson','warped'):
    log,woodblock=('stem','hyphae') if wood in ('crimson','warped') else ('log','wood')
    inputs=[f'minecraft:{wood}_{log}',f'minecraft:stripped_{wood}_{log}',f'minecraft:{wood}_{woodblock}',f'minecraft:stripped_{wood}_{woodblock}']
    save(DATA/f'recipe/sawing/{wood}.json',{'type':'quantumindustries:mechanical_processing','inputs':inputs,'result':{'id':f'minecraft:{wood}_planks','count':6},'ticks':100,'mechanical_load':6,'research':'mechanical_transmission'})
print('Exported three voxel models, Blockbench projects, original pixel atlas and eleven sawing processes.')
