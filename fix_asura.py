import os
import re

file_path = "app/src/main/kotlin/com/nyora/hasan72341/js/NyoraJsOtaUpdater.kt"
with open(file_path, "r") as f:
    content = f.read()

# Update BUNDLED_VERSION
content = re.sub(r'private val BUNDLED_VERSION = \d+', 'private val BUNDLED_VERSION = 2', content)

with open(file_path, "w") as f:
    f.write(content)
