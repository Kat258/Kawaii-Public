import zipfile
import json
import os
import re

def get_obfuscated_name(changelog_path, old_class):
    """
    从 ChangeLog.txt 中解析旧类名对应的混淆后新类名
    """
    if not os.path.exists(changelog_path):
        print(f"Error: {changelog_path} not found!")
        return None
    
    # 匹配格式: Class: ... old.class.Name => new.class.Name
    # 注意：ZKM 的 ChangeLog 中类名之前可能有 public final 等修饰符
    pattern = re.compile(rf"Class:.*?\b{re.escape(old_class)}\b\s*=>\s*([^\s]+)")
    
    try:
        with open(changelog_path, 'r', encoding='utf-8') as f:
            content = f.read()
            match = pattern.search(content)
            if match:
                return match.group(1).strip()
    except Exception as e:
        print(f"Error reading ChangeLog: {e}")
    
    return None

def fix_jar():
    base_dir = r"obf-workspace\ZKM-21.0.0-Cracked\ZKM 21.0.0"
    jar_path = os.path.join(base_dir, "Kawaii-1S.jar")
    changelog_path = os.path.join(base_dir, "ChangeLog.txt")
    temp_jar_path = jar_path + ".tmp"
    
    old_class = "dev.kizuna.Kawaii"
    new_class = get_obfuscated_name(changelog_path, old_class)

    if not new_class:
        print(f"Could not find obfuscated name for {old_class} in ChangeLog.txt")
        return

    print(f"Fixing {jar_path}...")
    print(f"Auto-detected mapping: {old_class} -> {new_class}")

    with zipfile.ZipFile(jar_path, 'r') as zin:
        with zipfile.ZipFile(temp_jar_path, 'w') as zout:
            for item in zin.infolist():
                buffer = zin.read(item.filename)
                if item.filename == "fabric.mod.json":
                    print("Found fabric.mod.json, updating entrypoints...")
                    try:
                        data = json.loads(buffer.decode('utf-8'))
                        
                        # 递归替换 entrypoints 中的类名
                        def replace_entrypoint(obj):
                            if isinstance(obj, dict):
                                return {k: replace_entrypoint(v) for k, v in obj.items()}
                            elif isinstance(obj, list):
                                return [replace_entrypoint(i) for i in obj]
                            elif isinstance(obj, str):
                                if obj == old_class:
                                    return new_class
                            return obj

                        if "entrypoints" in data:
                            data["entrypoints"] = replace_entrypoint(data["entrypoints"])
                        
                        buffer = json.dumps(data, indent=2).encode('utf-8')
                    except Exception as e:
                        print(f"Error processing fabric.mod.json: {e}")
                
                zout.writestr(item, buffer)

    # 替换原 JAR 包
    if os.path.exists(jar_path):
        os.remove(jar_path)
    os.rename(temp_jar_path, jar_path)
    print("Successfully updated fabric.mod.json with the latest obfuscated name.")

if __name__ == "__main__":
    fix_jar()
