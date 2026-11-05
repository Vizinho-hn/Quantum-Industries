// Run with the path to a PixelFX checkout; no Node dependencies or JS shipped to Minecraft.
import {pathToFileURL} from 'node:url';
import {writeFileSync,mkdirSync} from 'node:fs';
import {resolve} from 'node:path';
const root=resolve(process.argv[2]??'/mnt/dados/dev/quantum-pixelfx');
const {renderFrames}=await import(pathToFileURL(root+'/tools/lib/fxlib.mjs'));
const {encodePNG}=await import(pathToFileURL(root+'/tools/lib/png.mjs'));
const {burn,noiseFor,hotFirst}=await import(pathToFileURL(root+'/src/fx/fire/_lib.js'));
const {fit}=await import(pathToFileURL(root+'/src/core/index.js'));
const meta={size:[32,48],origin:[16,42],duration:2,loop:true,fps:10,options:{palette:['#401b18','#83301e','#ba451a','#ee6f21','#ff9e36','#ffd45b','#ffe586','#fff2bc','#ffffff']}};
const mod={meta,draw(p,t,o){burn(p,t,[[-2,0,.7,1.5,1],[2,0,.65,1.5,1],[0,0,1,2.2,0]],{H:hotFirst(fit(o.palette,9)),D:2,NL:noiseFor(2),S:23,height:17,spread:3,puffs:2,licks:2,smoke:0,embers:2,glow:.25,glowR:12});}};
const r=renderFrames({meta,mod},{fps:10,actor:false,bg:'none'});
const out='src/main/resources/assets/quantumindustries/textures/assay';mkdirSync(out,{recursive:true});
const strip=new Uint8Array(32*48*r.n*4);
for(let i=0;i<r.n;i++)strip.set(r.frames[i].clear,i*32*48*4);
writeFileSync(out+'/burner.png',encodePNG(32,48*r.n,strip));
writeFileSync('design/assay-v2/vfx-export.json',JSON.stringify({generator:'PixelFX burn',frames:r.n,width:32,height:48,fps:10,seed:23},null,2));
console.log(r.n+' deterministic flame frames');
