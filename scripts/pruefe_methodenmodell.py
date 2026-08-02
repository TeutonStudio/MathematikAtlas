#!/usr/bin/env python3
from pathlib import Path
import re
import sys

wurzel = Path(__file__).resolve().parents[1]
quellwurzeln = [
    wurzel / "MathematikRechenSystem/src/main",
    wurzel / "MathematikKartenAdapter/src/main",
    wurzel / "MathematikKnoten/src/main",
    wurzel / "app/src/main",
]

verbotene_muster = {
    r"\b(?:data\s+class|class|interface|typealias)\s+Funktion\b": "physischer Typ Funktion",
    r"\bFunktionsParameter\b": "historischer Parametertyp FunktionsParameter",
    r"\bGebundeneFunktion\b": "historischer Bindungstyp GebundeneFunktion",
    r"\btypealias\s+Methode\b": "Übergangs-Typealias für Methode",
    r"\benthalteneFunktionsParameter\b": "historische Parameteranalyse",
    r"\bfreieFunktionsParameter\b": "historische freie Parameteranalyse",
}

fehler: list[str] = []
for quellwurzel in quellwurzeln:
    for datei in quellwurzel.rglob("*.kt"):
        text = datei.read_text(encoding="utf-8")
        for muster, beschreibung in verbotene_muster.items():
            for treffer in re.finditer(muster, text):
                zeile = text.count("\n", 0, treffer.start()) + 1
                fehler.append(f"{datei.relative_to(wurzel)}:{zeile}: {beschreibung}")

kern = wurzel / "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern"
if (kern / "Funktionen.kt").exists():
    fehler.append("MathematikRechenSystem/.../kern/Funktionen.kt: historische Kerndatei existiert noch")

methoden_datei = kern / "Methoden.kt"
methoden_text = methoden_datei.read_text(encoding="utf-8")
for eigenschaft in ("val ausgaben:", "val zielMengen:"):
    if eigenschaft in methoden_text:
        fehler.append(f"{methoden_datei.relative_to(wurzel)}: persistente Mehrfachausgabe-Eigenschaft {eigenschaft}")

if fehler:
    print("Das physische Methodenmodell enthält historische Produktivstrukturen:")
    print("\n".join(f"- {eintrag}" for eintrag in fehler))
    sys.exit(1)

print("Physisches Methodenmodell erfolgreich geprüft.")
