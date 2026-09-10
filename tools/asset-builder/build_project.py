"""One project -> isolated source snapshot, native exports, validation and browser review.
Consumes artist-editable OBJ files; never regenerates them with the procedural sculpt scripts.
"""
import argparse,hashlib,json,math,re,shutil,subprocess,sys,time,uuid
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
HELPERS=REPO/'client/starfall-haven/models/v1'
sys.path.insert(0,str(HELPERS))
from meshlib import Mesh,PALETTE,export,render
from package_assets import glb,segment_hits
import numpy as np

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def local_path(base,value):
    p=(base/value).resolve()
    if not p.is_relative_to(base.resolve()):raise ValueError('Source/material path must stay inside project: '+value)
    if not p.is_file():raise FileNotFoundError(p)
    return p

def read_obj(path,name,project_root):
    m=Mesh(name);materials={};material=None;inputs=[path]
    lines=path.read_text(encoding='utf-8-sig').splitlines()
    for line in lines:
        parts=line.split()
        if not parts or parts[0].startswith('#'):continue
        if parts[0]=='mtllib':
            for library in parts[1:]:
                p=local_path(project_root,str((path.parent/library).relative_to(project_root)));inputs.append(p);current=None
                for ml in p.read_text(encoding='utf-8-sig').splitlines():
                    words=ml.split()
                    if not words:continue
                    if words[0]=='newmtl':current=' '.join(words[1:])
                    elif words[0]=='Kd':
                        values=[float(x) for x in words[1:]]
                        if current is None or len(values)!=3 or not all(math.isfinite(x) and 0<=x<=1 for x in values):raise ValueError('Invalid material colour: '+str(p))
                        materials[current]=[round(x*255) for x in values]
                    elif words[0].lower().startswith('map_'):raise ValueError('Texture maps need an explicit native conversion; use solid material colours for this prototype: '+str(p))
        elif parts[0]=='v':
            p=[float(x) for x in parts[1:4]]
            if len(p)!=3 or not all(math.isfinite(x) for x in p):raise ValueError('Invalid OBJ vertex')
            m.vert(p)
        elif parts[0]=='usemtl':material=' '.join(parts[1:])
        elif parts[0]=='f':
            if len(parts)!=4:raise ValueError('Triangulate the mesh before OBJ export: '+str(path))
            face=[]
            for token in parts[1:]:
                index=int(token.split('/')[0]);index=index-1 if index>0 else len(m.v)+index
                if index<0 or index>=len(m.v):raise ValueError('OBJ face index out of range')
                face.append(index)
            if material is not None and material not in materials:raise ValueError('Missing material colour: '+material)
            m.f.append(face);m.c.append(materials.get(material,PALETTE['stone']))
    if not m.v or not m.f:raise ValueError('OBJ contains no triangle mesh: '+str(path))
    m.clean()
    if not m.f:raise ValueError('No faces survive native coordinate quantization')
    return m,inputs

def load_project(path):
    data=json.loads(path.read_text(encoding='utf-8'));base=path.parent;meshes={};inputs=[path]
    for item in data['assets']:
        name=item['name']
        if not re.fullmatch('[a-z][a-z0-9_]*',name) or name in meshes:raise ValueError('Invalid/duplicate asset name: '+name)
        m,paths=read_obj(local_path(base,item['source']),name,base);meshes[name]=m;inputs+=paths
    for item in data.get('assemblies',[]):
        name=item['name']
        if not re.fullmatch('[a-z][a-z0-9_]*',name) or name in meshes:raise ValueError('Invalid/duplicate assembly')
        m=Mesh(name)
        for part in item['parts']:
            scale=part.get('scale',[1,1,1]);offset=part.get('offset',[0,0,0])
            if len(scale)!=3 or len(offset)!=3 or not all(math.isfinite(x) for x in scale+offset) or min(scale)<=0:raise ValueError('Invalid assembly transform')
            m.merge(meshes[part['asset']],offset,scale)
        meshes[name]=m.clean()
    for view in data['views']:
        if view not in meshes:raise ValueError('Unknown review view: '+view)
    for value in data.get('artwork',[])+[data['reference']]:
        p=(base/value).resolve()
        if not p.is_relative_to(REPO) or not p.is_file():raise ValueError('Missing/outside-workspace artwork or reference: '+value)
        inputs.append(p)
    return data,meshes,list(dict.fromkeys(inputs))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--action',choices=['Build','Inspect'],default='Build');parser.add_argument('--project',type=Path,default=REPO/'client/starfall-haven/project/project.json');parser.add_argument('--java',default='C:/Program Files/Java/jre1.8.0_503/bin/java.exe');parser.add_argument('--baseline',type=Path,default=Path("C:/Users/Tcarn/Desktop/DyNamic's 639/DyNamic-local.jar"));args=parser.parse_args()
    start=time.perf_counter();data,meshes,inputs=load_project(args.project.resolve())
    if args.action=='Inspect':
        print(json.dumps({'project':data['name'],'scope':data['scope'],'assets':data['assets'],'meshes':{n:{'vertices':len(m.v),'triangles':len(m.f)} for n,m in meshes.items()}},indent=2));return
    probe=REPO/'build/starfall-models-v1/classes/NativeAssetProbe.class'
    for p in [args.baseline,probe,Path(args.java)]:
        if not p.is_file():raise FileNotFoundError('Missing existing decoder prerequisite: '+str(p))
    tools=[Path(__file__),HELPERS/'meshlib.py',HELPERS/'package_assets.py',HELPERS/'viewer-template.html',probe,args.baseline]
    guarded={str(p):digest(p) for p in inputs+tools}
    output=REPO/'build/starfall-content-project'/('candidate-'+uuid.uuid4().hex)
    output.mkdir(parents=True);print('Building '+str(output),flush=True)
    for p in inputs:
        target=output/'source-snapshot'/p.relative_to(REPO);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
    exports=output/'exports';exports.mkdir();previews=output/'previews';previews.mkdir();records=[]
    for m in meshes.values():
        record=export(m,exports);record['glbBytes']=glb(m,exports);records.append(record)
    cp=';'.join([str(probe.parent),str(REPO/'bin'),str(REPO/'lib/netty.jar'),str(args.baseline)])
    with (output/'native-decoder.log').open('w') as log:
        subprocess.run([args.java,'-cp',cp,'NativeAssetProbe','decode',str(output/'decoded'),str(exports)],cwd=REPO,stdout=log,stderr=subprocess.STDOUT,check=True)
    for m in meshes.values():
        expected=json.loads((exports/(m.name+'.expected.json')).read_text());actual=json.loads((output/'decoded'/(m.name+'.dat.json')).read_text())
        if expected!=actual:raise AssertionError('Actual client decoding mismatch: '+m.name)
    passage=None
    if 'arch_assembled' in meshes:
        for x in [-2.5,-1.25,0,1.25,2.5]:
            for z in [.1,1,2,2.75]:
                if segment_hits(meshes['arch_assembled'],(x,-4,z),(x,4,z)):raise AssertionError('Geometry crosses the required arch passage')
        passage='20 sampled rays clear; server clipping not registered'
    views={n:{'vertices':meshes[n].v,'faces':meshes[n].f,'colors':meshes[n].c} for n in data['views']}
    html=(HELPERS/'viewer-template.html').read_text(encoding='utf-8').replace('__MESH_DATA__',json.dumps(views,separators=(',',':')))
    artwork=output/'artwork';artwork.mkdir()
    for value in data.get('artwork',[]):
        p=(args.project.resolve().parent/value).resolve();shutil.copy2(p,artwork/p.name)
    reference=(args.project.resolve().parent/data['reference']).resolve();shutil.copy2(reference,output/'design-reference.png')
    html=html.replace('<p id="status">','<p class="note"><a href="design-reference.png" target="_blank">Approved design reference</a><br><a href="artwork/arch-banner.svg" target="_blank">Banner artwork master</a></p><p id="status">')
    (output/'review.html').write_text(html,encoding='utf-8')
    for name in data['views']:render(meshes[name],(1000,1000),title=data['name']+' / '+name.replace('_',' ')).save(previews/(name+'.png'))
    for p,sha in guarded.items():
        if digest(Path(p))!=sha:raise RuntimeError('An input changed during build: '+p)
    report={'status':'PASS','scope':data['scope'],'seconds':time.perf_counter()-start,'sourceHashes':guarded,'meshes':records,'nativeDecoder':'Exact vertices/faces/HSL match','archPassage':passage,'artisticState':{a['name']:a['review'] for a in data['assets']},'pending':(['Reference-matching replacement statue'] if 'statue' in meshes else [])+['Artist-authored arch/eagle enhancements','Native rendering/culling/performance','Cache/map packing and registered server clipping','Live NPC/service integration'],'browserReview':str(output/'review.html')}
    (output/'receipt.json').write_text(json.dumps(report,indent=2))
    # Latest changes only after complete successful export/decoder checks. Older candidates are retained.
    (output.parent/'latest.json').write_text(json.dumps({'candidate':str(output),'review':str(output/'review.html'),'receipt':str(output/'receipt.json')},indent=2))
    print(json.dumps({'status':'PASS','seconds':report['seconds'],'models':len(records),'review':report['browserReview']}))

if __name__=='__main__':main()
