import argparse, hashlib, json, shutil, struct
from pathlib import Path
p=argparse.ArgumentParser(description='Import a paired Fast Ride input atlas into this mod.')
p.add_argument('source', type=Path, help='Directory containing poses.png and layout.json')
a=p.parse_args()
root=Path(__file__).resolve().parents[1]
data=json.loads((a.source/'layout.json').read_text(encoding='utf-8-sig'))
raw=(a.source/'poses.png').read_bytes()
assert raw[:8]==b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>II',raw[16:24])==(data['width'],data['height'])
assert data['width']==2048 and data['height']%48==0
for e in data['entries']:
    assert 0<e['width']*2<=128
    assert e['index']>=0 and (e['index']+17)//16*48+48<=data['height']
target=root/'src/main/resources/assets/manualriding/textures/gui/input'
target.mkdir(parents=True,exist_ok=True)
for name in ('poses.png','layout.json'):
    shutil.copyfile(a.source/name,target/name)
    print(name,hashlib.sha256((target/name).read_bytes()).hexdigest())
