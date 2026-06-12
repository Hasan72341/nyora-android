import re
file_path = "app/src/main/kotlin/com/nyora/hasan72341/js/NyoraJsOtaUpdater.kt"
with open(file_path, "r") as f:
    content = f.read()
content = re.sub(r'private val BUNDLED_VERSION = \d+', 'private val BUNDLED_VERSION = 36', content)
with open(file_path, "w") as f:
    f.write(content)
