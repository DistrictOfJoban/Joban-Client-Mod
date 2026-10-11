"""Audit documented members against compiled Java and MTR classes, without Minecraft startup."""
import argparse
import json
import struct
import zipfile
from pathlib import Path


def class_info(data):
    pos = 0
    def number(fmt):
        nonlocal pos
        value = struct.unpack_from('>' + fmt, data, pos)[0]
        pos += struct.calcsize(fmt)
        return value
    def skip(size):
        nonlocal pos
        pos += size
    assert number('I') == 0xCAFEBABE
    skip(4)
    pool = [None] * number('H')
    index = 1
    while index < len(pool):
        tag = number('B')
        if tag == 1:
            size = number('H'); pool[index] = data[pos:pos + size].decode('utf-8', errors='replace'); skip(size)
        elif tag in (7, 8, 16, 19, 20): pool[index] = number('H')
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): skip(4)
        elif tag in (5, 6): skip(8); index += 1
        elif tag == 15: skip(3)
        else: raise ValueError('Unknown constant pool tag ' + str(tag))
        index += 1
    skip(4)
    parent = number('H')
    parents = [pool[pool[parent]]] if parent else []
    parents += [pool[pool[number('H')]] for _ in range(number('H'))]
    def members():
        result = []
        for _ in range(number('H')):
            flags, name, descriptor = number('H'), pool[number('H')], pool[number('H')]
            for _ in range(number('H')): skip(2); skip(number('I'))
            if flags & 1: result.append((name, descriptor, flags))
        return result
    return parents, members(), members()


def parameter_count(descriptor):
    count, pos = 0, 1
    while descriptor[pos] != ')':
        while descriptor[pos] == '[': pos += 1
        if descriptor[pos] == 'L': pos = descriptor.index(';', pos)
        count += 1; pos += 1
    return count


def audit(classes, jars, manifest):
    archives = [zipfile.ZipFile(jar) for jar in jars]
    cache = {}
    def info(name):
        if name in cache: return cache[name]
        entry = name.replace('.', '/') + '.class'
        path = classes / entry
        data = path.read_bytes() if path.exists() else None
        if data is None:
            for archive in archives:
                try: data = archive.read(entry); break
                except KeyError: pass
        result = class_info(data) if data else ([], [], [])
        cache[name] = result
        return result
    def public_members(name, field=False, inherited=True, visited=None):
        visited = set() if visited is None else visited
        if name in visited: return []
        visited.add(name)
        parents, fields, methods = info(name)
        result = list(fields if field else methods)
        if inherited:
            for parent in parents: result.extend(public_members(parent.replace('/', '.'), field, True, visited))
        return result
    failures = []
    groups = {}
    unimplemented = []
    for entry in manifest['members']:
        if entry.get('status') == 'upstream_unimplemented':
            unimplemented.append(entry['signature']); continue
        kind, member, owner = entry['kind'], entry['member'], manifest['types'][entry['type']]
        available = public_members(owner, kind == 'field', kind != 'constructor')
        matches = [item for item in available if item[0] == member]
        if entry.get('required_descriptors') and not all(any(item[1] == descriptor for item in matches) for descriptor in entry['required_descriptors']): matches = []
        if kind != 'field':
            if not all(any(parameter_count(item[1]) == count or
                       (item[2] & 0x80 and count >= parameter_count(item[1]) - 1) for item in matches) for count in entry['arities']): matches = []
        if not matches: failures.append(entry['signature'] + ' -> ' + owner)
        groups[entry['page']] = groups.get(entry['page'], 0) + 1
    for archive in archives: archive.close()
    print(json.dumps({'documented_members': len(manifest['members']), 'pages': groups, 'upstream_unimplemented': unimplemented, 'missing': failures}, indent=2))
    if failures: raise SystemExit('API coverage audit failed')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--jar', action='append', type=Path, default=[])
    parser.add_argument('--manifest', type=Path, default=Path(__file__).with_name('api-coverage.json'))
    args = parser.parse_args()
    audit(args.classes, args.jar, json.loads(args.manifest.read_text(encoding='utf-8')))


if __name__ == '__main__': main()
