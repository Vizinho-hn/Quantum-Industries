#!/usr/bin/env python3
"""Export the edited Blockbench project to the shared runtime mesh and static block halves."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
a=ROOT/'src/main/resources/assets/quantumindustries';d=ROOT/'design/assay-v2'
p=json.loads((d/'empirical_workshop.bbmodel').read_text())
groups={g['uuid']:g['name'] for g in p['groups']};parents={}
for node in p['outliner']:
 for child in node.get('children',[]):
  if isinstance(child,str):parents[child]=groups[node['uuid']]
boxes=[]
for e in p['elements']:
 uv=e['faces']['north']['uv'];m=round(uv[0]/16)+8*round(uv[1]/16)
 boxes.append({'name':e['name'],'from':e['from'],'to':e['to'],'group':parents[e['uuid']],'material':m})
(d/'geometry.json').write_text(json.dumps({'boxes':boxes},indent=2)+'\n')
(a/'assay/workshop.json').write_text(json.dumps({'boxes':boxes},separators=(',',':'))+'\n')
def element(b,shift=0):
 m=b['material'];u=(m%8)*2;v=(m//8)*4
 return {'name':b['name'],'from':[b['from'][0]-shift,*b['from'][1:]],'to':[b['to'][0]-shift,*b['to'][1:]],'faces':{f:{'uv':[u,v,u+2,v+4],'texture':'#all'} for f in ['north','south','east','west','up','down']}}
textures={'all':'quantumindustries:block/empirical_workshop','particle':'quantumindustries:block/empirical_workshop'}
for part,lo,hi in [('foot',0,16),('head',16,32)]:
 els=[]
 for b in boxes:
  if b['group']!='base':continue
  f=b['from'].copy();t=b['to'].copy();f[0]=max(f[0],lo);t[0]=min(t[0],hi)
  if f[0]<t[0]:els.append(element(dict(b,**{'from':f,'to':t}),lo))
 (a/f'models/block/empirical_assay_bench_{part}.json').write_text(json.dumps({'textures':textures,'elements':els},indent=2)+'\n')
(a/'models/block/empirical_assay_bench.json').write_text(json.dumps({'textures':textures,'elements':[element(b) for b in boxes if b['group'] in ('base','screw','head','tray')],'display':{'gui':{'rotation':[25,225,0],'translation':[-4,-2,0],'scale':[.42,.42,.42]},'ground':{'scale':[.25,.25,.25]},'fixed':{'scale':[.5,.5,.5]}}},indent=2)+'\n')
print(f'Exported {len(boxes)} cuboids from Blockbench')
