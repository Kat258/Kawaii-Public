import random
import string
import os

def generate_names():
    prefix = "mSS0"
    chars = string.ascii_letters + string.digits
    names = []
    for _ in range(1000):
        suffix = ''.join(random.choice(chars) for _ in range(74))
        names.append(prefix + suffix)
    
    file_path = r"obf-workspace\ZKM-21.0.0-Cracked\ZKM 21.0.0\names.txt"
    with open(file_path, "w", encoding="utf-8") as f:
        f.write("\n".join(names))
    print(f"Successfully generated 1000 names to {file_path}")

if __name__ == "__main__":
    generate_names()
