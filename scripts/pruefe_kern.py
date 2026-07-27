#!/usr/bin/env python3
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

wurzel = Path(__file__).resolve().parents[1]
kotlinc = shutil.which("kotlinc")
java = shutil.which("java")
if not kotlinc or not java:
    print("kotlinc und java werden für diese zusätzliche Kernprüfung benötigt.")
    sys.exit(2)

quellen: list[Path] = []
for verzeichnis in (
    wurzel / "KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/daten",
    wurzel / "KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/logik",
    wurzel / "MathematikRechenSystem/src/main/kotlin",
    wurzel / "MathematikKartenAdapter/src/main/kotlin",
):
    quellen.extend(sorted(verzeichnis.rglob("*.kt")))
quellen.extend([
    wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAnschlussArten.kt",
    wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt",
    wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt",
    wurzel / "werkzeuge/Prüfung.kt",
])
with tempfile.TemporaryDirectory(prefix="mathematik-atlas-") as tmp:
    jar = Path(tmp) / "pruefung.jar"
    subprocess.run([kotlinc, *(str(q) for q in quellen), "-include-runtime", "-d", str(jar)], check=True)
    subprocess.run([java, "-jar", str(jar)], check=True)
