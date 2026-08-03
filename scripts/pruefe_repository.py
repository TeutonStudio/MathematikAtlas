#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

wurzel = Path(__file__).resolve().parents[1]
verpflichtend = [
    "settings.gradle.kts", "build.gradle.kts", "gradlew", "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar", "app/src/main/AndroidManifest.xml",
    "KnotenKartenVerwalter/build.gradle.kts", "MathematikRechenSystem/build.gradle.kts",
    "MathematikKartenAdapter/build.gradle.kts", "MathematikKnoten/build.gradle.kts",
    "AGENTS.md", "docs/codex/GIT_IDENTITY.md", "scripts/samai-git.sh",
]
fehlt = [p for p in verpflichtend if not (wurzel / p).is_file()]
if fehlt:
    print("Fehlende Dateien:", ", ".join(fehlt)); sys.exit(1)

for xml in wurzel.rglob("*.xml"):
    ET.parse(xml)

with zipfile.ZipFile(wurzel / "gradle/wrapper/gradle-wrapper.jar") as jar:
    if "org/gradle/wrapper/GradleWrapperMain.class" not in jar.namelist():
        raise SystemExit("Wrapper-Hauptklasse fehlt")

subprocess.run(["bash", "-n", str(wurzel / "scripts/samai-git.sh")], check=True)
subprocess.run([sys.executable, str(wurzel / "scripts/pruefe_architektur.py")], check=True)
subprocess.run([sys.executable, str(wurzel / "scripts/pruefe_methodenmodell.py")], check=True)
print("Repository-Struktur, XML, Wrapper und SamAI-Gitwerkzeug erfolgreich geprüft.")
