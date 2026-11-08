"""下载并校验开发依赖，不将它们嵌入模组。 / Fetch verified external build inputs."""
import hashlib
import json
from pathlib import Path
import urllib.request

root = Path(__file__).resolve().parents[1]
target = root / 'libs'
target.mkdir(exist_ok=True)
for item in json.loads((root / 'tools/dependencies.json').read_text(encoding='utf-8')):
    path = target / item['filename']
    if path.is_file():
        with path.open('rb') as existing:
            matched = hashlib.file_digest(existing, 'sha512').hexdigest() == item['sha512']
        if matched:
            print('Verified:', path.name)
            continue
    request = urllib.request.Request(item['url'], headers={'User-Agent': 'GoetyManualRiding/1.0 dependency setup'})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != item['sha512']:
        raise ValueError('Dependency hash mismatch: ' + path.name)
    path.write_bytes(data)
    print('Downloaded and verified:', path.name)
