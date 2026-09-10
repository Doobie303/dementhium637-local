"""Focused OBJ importer guard checks and source-to-candidate integration checks."""
import json,tempfile,time
from pathlib import Path
import build_project as b
start=time.perf_counter();checks=[]
with tempfile.TemporaryDirectory(prefix='asset-import-',dir=b.REPO/'build/starfall-content-project') as temp:
    root=Path(temp);obj=root/'fixture.obj';mtl=root/'fixture.mtl'
    mtl.write_text('newmtl stone\nKd 0.5 0.6 0.7\n')
    prefix='mtllib fixture.mtl\nv 0 0 0\nv 1 0 0\nv 0 0 1\nusemtl stone\n'
    obj.write_text(prefix+'f -3 -2 -1\n');m,_=b.read_obj(obj,'fixture',root)
    assert m.f==[[0,1,2]] and m.c==[[128,153,178]];checks.append('OBJ negative indices and material colour')
    for label,content in [('index range',prefix+'f 1 2 9\n'),('non-triangle',prefix+'f 1 2 3 1\n'),('non-finite vertex','v nan 0 0\nf 1 1 1\n'),('material traversal','mtllib ../outside.mtl\n')]:
        obj.write_text(content)
        try:b.read_obj(obj,'fixture',root)
        except (ValueError,FileNotFoundError):checks.append('Reject '+label)
        else:raise AssertionError('Invalid input accepted: '+label)
    obj.write_text(prefix+'f 1 2 3\n');mtl.write_text('newmtl stone\nKd 0.5 0.6 0.7\nmap_Kd texture.png\n')
    try:b.read_obj(obj,'fixture',root)
    except ValueError:checks.append('Reject unconverted texture maps')
    else:raise AssertionError('Unsupported texture silently discarded')
latest=json.loads((b.REPO/'build/starfall-content-project/latest.json').read_text());candidate=Path(latest['candidate']);receipt=json.loads((candidate/'receipt.json').read_text())
assert receipt['status']=='PASS' and receipt['nativeDecoder'].startswith('Exact')
for name,sha in receipt['sourceHashes'].items():assert b.digest(Path(name))==sha,'Input drift: '+name
checks.append('Exact built input identity preserved')
project=json.loads((b.REPO/'client/starfall-haven/project/project.json').read_text());base=b.REPO/'client/starfall-haven/project'
assert (candidate/'design-reference.png').read_bytes()==(base/project['reference']).read_bytes()
for value in project['artwork']:assert (candidate/'artwork'/Path(value).name).read_bytes()==(base/value).read_bytes()
checks.append('Reference and banner artwork bundled unchanged')
for m in receipt['meshes']:
    expected=json.loads((candidate/'exports'/(m['name']+'.expected.json')).read_text());actual=json.loads((candidate/'decoded'/(m['name']+'.dat.json')).read_text());assert expected==actual
    # Initial imported authoring baseline is identical to the separately validated prior exports.
    assert (candidate/'exports'/(m['name']+'.dat')).read_bytes()==(b.HELPERS/'exports'/(m['name']+'.dat')).read_bytes()
checks.append(f"All {len(receipt['meshes'])} imported meshes/assemblies preserve prior native exports and actual decoder identity")
report={'status':'PASS','seconds':time.perf_counter()-start,'coverage':checks,'candidate':str(candidate)}
(candidate/'tooling-verification.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))
