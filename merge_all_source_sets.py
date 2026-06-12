import os
import shutil

src_root = "app/src"
source_sets = ["main", "debug", "release", "nightly", "test", "androidTest"]

for ss in source_sets:
    base_path = os.path.join(src_root, ss, "kotlin", "com", "nyora")
    android_dir = os.path.join(base_path, "android")
    hasan_dir = os.path.join(base_path, "hasan72341")
    
    if not os.path.exists(android_dir):
        continue
        
    print(f"Processing {android_dir}...")
    
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
                    with open(src_file, 'r') as f:
                        content = f.read()
                    content = content.replace("package com.nyora.android", "package com.nyora.hasan72341")
                    content = content.replace("import com.nyora.android", "import com.nyora.hasan72341")
                    with open(target_file, 'w') as f:
                        f.write(content)
                else:
                    print(f"Skipping duplicate: {file}")
    
    # Rename the old dir
    legacy_dir = android_dir + "_legacy"
    if os.path.exists(legacy_dir):
        shutil.rmtree(legacy_dir)
    os.rename(android_dir, legacy_dir)
