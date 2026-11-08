"""下载并校验开发依赖，不将它们嵌入模组。 / Fetch verified external build inputs."""
import hashlib
import json
from pathlib import Path
import urllib.request
import argparse
import io
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description='Fetch pinned build inputs for one loader.')
parser.add_argument('--platform', choices=['forge', 'fabric', 'neoforge'], default='forge')
platform = parser.parse_args().platform
folder = {'forge': '', 'fabric': 'fabric-1.20.1', 'neoforge': 'neoforge-1.21.1'}[platform]
manifest = 'dependencies.json' if platform == 'forge' else f'dependencies-{platform}.json'
target = root / folder / 'libs'
target.mkdir(parents=True, exist_ok=True)
def extract_nested(data):
    """Loom 开发运行需显式加载本体内嵌依赖。 / Expose bundled mods to Loom dev runs."""
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if 'fabric.mod.json' not in archive.namelist():
            return
        metadata = json.loads(archive.read('fabric.mod.json'))
        for entry in metadata.get('jars', []):
            content = archive.read(entry['file'])
            nested = target / 'nested' / Path(entry['file']).name
            nested.parent.mkdir(exist_ok=True)
            nested.write_bytes(content)
            extract_nested(content)

for item in json.loads((root / 'tools' / manifest).read_text(encoding='utf-8')):
    path = target / item['filename']
    if path.is_file():
        with path.open('rb') as existing:
            matched = hashlib.file_digest(existing, 'sha512').hexdigest() == item['sha512']
        if matched:
            if item.get('expose_nested_mods'):
                extract_nested(path.read_bytes())
            print('Verified:', path.name)
            continue
    request = urllib.request.Request(item['url'], headers={'User-Agent': 'GoetyManualRiding/1.0 dependency setup'})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != item['sha512']:
        raise ValueError('Dependency hash mismatch: ' + path.name)
    path.write_bytes(data)
    if item.get('expose_nested_mods'):
        extract_nested(data)
    print('Downloaded and verified:', path.name)
