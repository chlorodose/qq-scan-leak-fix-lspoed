#!/usr/bin/env python3
"""Download pinned build dependencies into toolchain/ without system installation."""
import hashlib
import json
import os
from pathlib import Path
import tarfile
import urllib.request
import zipfile

os.chdir(Path(__file__).resolve().parent)
Path('toolchain').mkdir(exist_ok=True)
for name, spec in json.loads(Path('dependencies.json').read_text()).items():
    target = Path('toolchain') / name
    if not target.exists():
        print('Downloading', name, flush=True)
        urllib.request.urlretrieve(spec['url'], target)
    if hashlib.sha256(target.read_bytes()).hexdigest() != spec['sha256']:
        raise RuntimeError('SHA256 mismatch: ' + name)
with zipfile.ZipFile('toolchain/aapt2.jar') as archive:
    archive.extract('aapt2', 'toolchain')
Path('toolchain/aapt2').chmod(0o755)
# Avoid re-extracting over the JDK's read-only license files on repeated setup.
if not Path('toolchain/jdk-17.0.21+9/bin/java').is_file():
    with tarfile.open('toolchain/jdk.tar.gz') as archive:
        archive.extractall('toolchain', filter='data')
print('Ready: bash build.sh')
