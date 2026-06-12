import os
import shutil

src_root = "app/src/main/kotlin/com/nyora"
android_dir = os.path.join(src_root, "android")
hasan_dir = os.path.join(src_root, "hasan72341")

for root, dirs, files in os.walk(android_dir):
    rel_path = os.path.relpath(root, android_dir)
    target_dir = os.path.join(hasan_dir, rel_path)
    
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    
    for file in files:
        if file.endswith(".kt"):
            src_file = os.path.join(root, file)
            target_file = os.path.join(target_dir, file)
            
            if not os.path.exists(target_file):
                print(f"Moving {src_file} -> {target_file}")
                # We need to change the package declaration in the file
                with open(src_file, 'r') as f:
                    content = f.read()
                content = content.replace("package com.nyora.android", "package com.nyora.hasan72341")
                content = content.replace("import com.nyora.android", "import com.nyora.hasan72341")
                with open(target_file, 'w') as f:
                    f.write(content)
