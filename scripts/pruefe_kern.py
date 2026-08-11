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

# Diese Prüfung ist absichtlich ein schneller, Android-unabhängiger Smoke-Test
# des MathematikRechenSystems und seines neutralen G0.2-Typfundaments.
# Modulübergreifende Graph-/JSON-Abhängigkeiten, Kartenadapter und die App werden
# anschließend über Gradle getestet und gebaut.
quellen = sorted((wurzel / "TypSystem/src/main/kotlin").rglob("*.kt"))
quellen += sorted((wurzel / "MathematikRechenSystem/src/main/kotlin").rglob("*.kt"))
quellen.append(wurzel / "werkzeuge/Prüfung.kt")

with tempfile.TemporaryDirectory(prefix="mathematik-atlas-") as tmp:
    jar = Path(tmp) / "pruefung.jar"
    subprocess.run(
        [kotlinc, *(str(q) for q in quellen), "-include-runtime", "-d", str(jar)],
        check=True,
    )
    subprocess.run([java, "-jar", str(jar)], check=True)
