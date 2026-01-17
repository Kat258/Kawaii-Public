#!/usr/bin/env python3
"""
downgrade_jar_major.py

把 jar 中的 class 文件的 major version 从指定值替换为另一个值（默认 21 -> 17）。
会为每个修改过的 jar 生成原文件的备份： <name>.jar.bak

用法:
    python downgrade_jar_major.py                 # 扫描当前目录的所有 .jar 并替换 21->17
    python downgrade_jar_major.py path/to/a.jar   # 只处理指定的 jar
    python downgrade_jar_major.py -r               # 递归扫描子目录
    python downgrade_jar_major.py --from 52 --to 51
"""

import argparse
import struct
import sys
import os
import tempfile
import shutil
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

MAGIC = b'\xCA\xFE\xBA\xBE'

def process_jar(path, from_major=65, to_major=61, make_backup=True):
    changed_classes = 0
    total_classes = 0

    with ZipFile(path, 'r') as zin:
        # create temp file for new jar
        tmp_fd, tmp_path = tempfile.mkstemp(suffix='.jar')
        os.close(tmp_fd)
        try:
            with ZipFile(tmp_path, 'w') as zout:
                for info in zin.infolist():
                    data = zin.read(info.filename)
                    write_data = data

                    if info.filename.endswith('.class') and len(data) >= 8 and data[0:4] == MAGIC:
                        total_classes += 1
                        # major is bytes 6..7 (big-endian)
                        major = struct.unpack('>H', data[6:8])[0]
                        if major == from_major:
                            # build new data
                            new_major_bytes = struct.pack('>H', to_major)
                            write_data = data[:6] + new_major_bytes + data[8:]
                            changed_classes += 1

                    # preserve compress_type and external_attr (permissions) if possible
                    new_info = ZipInfo(info.filename)
                    new_info.date_time = info.date_time
                    new_info.compress_type = info.compress_type
                    new_info.external_attr = info.external_attr
                    new_info.comment = info.comment
                    new_info.create_system = info.create_system
                    # write bytes
                    zout.writestr(new_info, write_data)

            if changed_classes > 0:
                # make backup
                if make_backup:
                    bak_path = path + '.bak'
                    if os.path.exists(bak_path):
                        # don't overwrite existing .bak; create numbered bak
                        i = 1
                        while os.path.exists(f"{bak_path}.{i}"):
                            i += 1
                        bak_path = f"{bak_path}.{i}"
                    shutil.move(path, bak_path)
                    shutil.move(tmp_path, path)
                    print(f"[+] Modified {path}: total .class={total_classes}, changed={changed_classes}. Backup at {bak_path}")
                else:
                    # overwrite original (no backup)
                    os.remove(path)
                    shutil.move(tmp_path, path)
                    print(f"[+] Modified {path}: total .class={total_classes}, changed={changed_classes}. (no backup)")
            else:
                os.remove(tmp_path)
                print(f"[-] No matching class major versions found in {path} (checked {total_classes} classes).")

        except Exception:
            # cleanup temp file on error
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
            raise

def find_jars(root='.', recursive=False):
    if recursive:
        for dirpath, dirnames, filenames in os.walk(root):
            for fn in filenames:
                if fn.lower().endswith('.jar'):
                    yield os.path.join(dirpath, fn)
    else:
        for fn in os.listdir(root):
            if fn.lower().endswith('.jar') and os.path.isfile(fn):
                yield os.path.join(root, fn)

def main():
    parser = argparse.ArgumentParser(description="Downgrade jar .class major version (e.g. 21->17).")
    parser.add_argument('paths', nargs='*', help='jar file(s) to process. If omitted, scan current directory.')
    parser.add_argument('--from', dest='from_major', type=int, default=21, help='original major version to replace (default 21)')
    parser.add_argument('--to', dest='to_major', type=int, default=17, help='new major version (default 17)')
    parser.add_argument('-r', '--recursive', action='store_true', help='recursively find jars in subdirectories when no explicit files provided')
    parser.add_argument('--no-backup', action='store_true', help="don't keep a .bak backup (default: create backup)")
    args = parser.parse_args()

    targets = []
    if args.paths:
        for p in args.paths:
            if os.path.isdir(p):
                # if a directory path given, scan it
                targets.extend(list(find_jars(p, recursive=args.recursive)))
            else:
                targets.append(p)
    else:
        targets = list(find_jars('.', recursive=args.recursive))

    if not targets:
        print("No .jar files found to process.")
        sys.exit(0)

    for jar in targets:
        if not os.path.isfile(jar):
            print(f"Skipping (not a file): {jar}")
            continue
        try:
            process_jar(jar, from_major=args.from_major, to_major=args.to_major, make_backup=not args.no_backup)
        except Exception as e:
            print(f"[!] Error processing {jar}: {e}")

if __name__ == '__main__':
    main()
