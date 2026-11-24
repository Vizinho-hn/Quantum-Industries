"""Offline textured orthographic geometry review; not an in-game renderer.

Writes tools/qks-pr2-review.png. Needs Pillow and NumPy, only for review.
"""
import importlib.util
import itertools
import math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('resources', ROOT/'tools/generate-qks-pr2-resources.py')
resources = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resources)
# Cube face corners in Bedrock coordinates; depth is resolved per pixel.
CORNERS = list(itertools.product((0,1), repeat=3))
FACE_INDICES = {
    'north':(0,2,6,4), 'south':(5,7,3,1),
    'west':(1,3,2,0), 'east':(4,6,7,5),
    'up':(2,3,7,6), 'down':(1,0,4,5),
}


def render(name, side=260):
    geo = __import__('json').loads((resources.ASSETS/f'geckolib/models/block/{name}.geo.json').read_text())['minecraft:geometry'][0]
    atlas = np.asarray(Image.open(resources.ASSETS/f'textures/block/{name}.png').convert('RGB'))
    yaw,pitch=math.radians(23),math.radians(12)
    view=np.array([[math.cos(yaw),0,math.sin(yaw)],
                   [math.sin(yaw)*math.sin(pitch),math.cos(pitch),-math.cos(yaw)*math.sin(pitch)],
                   [-math.sin(yaw)*math.cos(pitch),math.sin(pitch),math.cos(yaw)*math.cos(pitch)]])
    cubes=[]
    for bone in geo['bones']:
        for c in bone['cubes']:
            v=np.array(c['origin'])+np.array(CORNERS)*np.array(c['size'])
            a=math.radians(c.get('rotation',[0,0,0])[2])
            rot=np.array([[math.cos(a),-math.sin(a),0],[math.sin(a),math.cos(a),0],[0,0,1]])
            pivot=np.array(c.get('pivot',resources.PIVOT))
            v=((v-pivot)@rot.T+pivot-np.array(resources.PIVOT))@view.T
            cubes.append((c,v))
    points=np.concatenate([v for _,v in cubes])
    extent=np.ptp(points[:,:2],axis=0)
    scale=(side-26)/max(extent)
    center=(points[:,:2].max(axis=0)+points[:,:2].min(axis=0))/2
    canvas=np.full((side,side,3),(32,29,26),dtype=np.uint8)
    zbuffer=np.full((side,side),-np.inf)
    light={'north':.85,'south':.95,'west':.75,'east':.85,'up':1.10,'down':.65}
    for c,v in cubes:
        xy=(v[:,:2]-center)*scale
        xy[:,0]+=side/2
        xy[:,1]=side/2-xy[:,1]
        for face,indices in FACE_INDICES.items():
            f=c['uv'][face]
            u,w=f['uv'][0],f['uv_size'][0]
            t,h=f['uv'][1],f['uv_size'][1]
            tex=np.array([[u,t],[u,t+h],[u+w,t+h],[u+w,t]])
            ids=list(indices)
            for tri in ((0,1,2),(0,2,3)):
                idx=[ids[i] for i in tri]
                p=xy[idx]
                low=np.maximum(np.floor(p.min(axis=0)).astype(int),0)
                high=np.minimum(np.ceil(p.max(axis=0)).astype(int),side-1)
                if (high<low).any():
                    continue
                x,y=np.meshgrid(np.arange(low[0],high[0]+1),np.arange(low[1],high[1]+1))
                a,b,d=p
                den=(b[1]-d[1])*(a[0]-d[0])+(d[0]-b[0])*(a[1]-d[1])
                if abs(den)<1e-7:
                    continue
                wa=((b[1]-d[1])*(x-d[0])+(d[0]-b[0])*(y-d[1]))/den
                wb=((d[1]-a[1])*(x-d[0])+(a[0]-d[0])*(y-d[1]))/den
                wc=1-wa-wb
                weights=np.stack((wa,wb,wc),axis=-1)
                depth=weights@v[idx,2]
                mask=(weights.min(axis=-1)>=-1e-6)&(depth>zbuffer[y,x])
                uv=weights@tex[list(tri)]
                uv=np.clip(uv.astype(int),0,127)
                colors=np.clip(atlas[uv[:,:,1],uv[:,:,0]]*light[face],0,255).astype(np.uint8)
                canvas[y[mask],x[mask]]=colors[mask]
                zbuffer[y[mask],x[mask]]=depth[mask]
    return Image.fromarray(canvas)


def main():
    sheet=Image.new('RGB',(840,740),(24,22,20))
    draw=ImageDraw.Draw(sheet)
    draw.text((12,8),'QKS PR 2 | OFFLINE GEOMETRY + TEXTURE REVIEW (not Minecraft)',fill='#d5c5a5')
    for i,name in enumerate(resources.BLOCKS):
        x,y=(i%3)*280,35+(i//3)*315
        sheet.paste(render(name),(x+10,y))
        draw.text((x+12,y+265),name,fill='#d5c5a5')
        icon=Image.open(resources.ASSETS/f'textures/item/{name}.png')
        sheet.paste(icon.resize((48,48),Image.Resampling.NEAREST),(x+220,y+257),icon.resize((48,48),Image.Resampling.NEAREST))
    belt=Image.open(resources.ASSETS/'textures/item/drive_belt.png').resize((64,64),Image.Resampling.NEAREST)
    sheet.paste(belt,(20,670),belt)
    draw.text((90,690),'drive_belt | 32px sprites, binary alpha | 128px material atlases',fill='#d5c5a5')
    output=ROOT/'tools/qks-pr2-review.png'
    sheet.save(output)
    print(output)


if __name__ == '__main__':
    main()
