"""Stage signed APKs and a short, versioned Release body. No publishing or signing."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import unquote

from verify_release_notes import check, FEATURE_HEADINGS, FIX_HEADINGS, DEFAULT_FORBIDDEN_HEADINGS, IDENTITY_HEADINGS, DEFAULT_LINK_PATTERN


def output(*args):
    return subprocess.check_output(args, text=True, encoding='utf-8', errors='replace').strip()


def apk_tool(tools, name):
    suffix = ('.bat' if name == 'apksigner' else '.exe') if os.name == 'nt' else ''
    return str(tools / (name + suffix))


def prepare(args):
    if not re.fullmatch(r'v[0-9][A-Za-z0-9.-]*', args.tag):
        raise ValueError('Expected a v-prefixed version tag')
    if output('git', 'rev-parse', args.tag + '^{commit}') != output('git', 'rev-parse', 'HEAD'):
        raise ValueError('Build checkout must match the release tag')
    notes = Path('docs/release-notes') / (args.tag + '.md')
    errors = check(notes, FEATURE_HEADINGS, FIX_HEADINGS, DEFAULT_FORBIDDEN_HEADINGS,
                   IDENTITY_HEADINGS, re.compile(DEFAULT_LINK_PATTERN), 40)
    if errors:
        raise ValueError('\n'.join(errors))
    text = notes.read_text(encoding='utf-8')
    if any(heading in text.splitlines() for heading in IDENTITY_HEADINGS):
        raise ValueError('Build information is appended from the actual APKs; omit it in source notes')
    links = re.findall(r'https://github\.com/' + re.escape(args.repository) +
                       r'/blob/' + re.escape(args.tag) + r'/([^\s)]+)', text)
    if not links:
        raise ValueError('Notes must link to a document in this repository at the release tag')
    for link in links:
        path = unquote(link.split('#', 1)[0])
        output('git', 'cat-file', '-e', args.tag + ':' + path)
    dist = Path(args.dist)
    if dist.exists() and any(dist.iterdir()):
        raise ValueError('Release staging directory must be empty')
    dist.mkdir(parents=True, exist_ok=True)
    rows = ['| 项目 | 值 |', '| --- | --- |', f'| 版本 | `{args.tag}` |',
            f'| 源码提交 | `{output("git", "rev-parse", "HEAD")}` |']
    metadata = []
    names = set()
    for item in args.apk:
        variant, path = item.split('=', 1)
        apk = Path(path)
        if not re.fullmatch(r'[A-Za-z0-9-]+', variant):
            raise ValueError('Variant must use ASCII letters, digits and hyphens')
        badging = output(apk_tool(args.tools, 'aapt'), 'dump', 'badging', str(apk))
        package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
        if not package or package.group(3) != args.tag[1:]:
            raise ValueError('APK versionName must match the tag without v')
        if 'application-debuggable' in badging:
            raise ValueError('Debuggable APK cannot be published')
        native = re.search(r"native-code:\s*(.*)", badging)
        abi = 'universal' if native is None else '-'.join(re.findall(r"'([^']+)'", native.group(1)))
        # Both projects currently have no native libraries: universal is accurate.
        if abi != 'universal':
            raise ValueError('Native ABI packaging changed; choose an explicit asset naming policy')
        signer = output(apk_tool(args.tools, 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk))
        cert = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)', signer)
        if not cert or cert.group(1).lower() != args.certificate.lower():
            raise ValueError('Unexpected signing certificate')
        if 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signer:
            raise ValueError('APK requires a verified v2 signature')
        name = f'{args.project}-{variant}-{abi}-{args.tag[1:]}.apk' if variant != 'default' else f'{args.project}-{abi}-{args.tag[1:]}.apk'
        if name in names:
            raise ValueError('Duplicate release asset name')
        names.add(name)
        shutil.copyfile(apk, dist / name)
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        (dist / (name + '.sha256')).write_bytes(f'{digest}  {name}\n'.encode('ascii'))
        minimum = re.search(r"(?:minSdkVersion|sdkVersion):'([^']+)'", badging)
        target = re.search(r"targetSdkVersion:'([^']+)'", badging)
        if not minimum or not target:
            raise ValueError('Cannot read min / target SDK from APK')
        rows.extend([f'| {variant} 包名 / 版本号 | `{package.group(1)} / {package.group(2)}` |',
                     f'| {variant} 架构 / min / target SDK | `{abi} / {minimum.group(1)} / {target.group(1)}` |',
                     f'| {variant} 签名证书 SHA-256 | `{cert.group(1).lower()}` |',
                     f'| {name} SHA-256 | `{digest}` |'])
        metadata.append({'asset': name, 'package': package.group(1), 'version_code': int(package.group(2)),
                         'sha256': digest, 'certificate_sha256': cert.group(1).lower()})
    body = text.rstrip() + '\n\n## 构建信息\n\n' + '\n'.join(rows) + '\n'
    if len(body.splitlines()) > 40:
        raise ValueError('Final Release body exceeds 40 lines')
    Path(args.notes_out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.notes_out).write_text(body, encoding='utf-8', newline='\n')
    print(json.dumps(metadata, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True)
    parser.add_argument('--project', required=True)
    parser.add_argument('--repository', required=True)
    parser.add_argument('--certificate', required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--apk', action='append', required=True, help='variant=APK path')
    parser.add_argument('--dist', default='dist')
    parser.add_argument('--notes-out', default='build/release/release-notes.md')
    prepare(parser.parse_args())
