#!/usr/bin/env python3
from pathlib import Path
import sys

wurzel = Path(__file__).resolve().parents[1]
fehler: list[str] = []

def pruefe(modul: str, verbotene: tuple[str, ...]) -> None:
    for datei in (wurzel / modul / "src").rglob("*.kt"):
        text = datei.read_text(encoding="utf-8")
        for verboten in verbotene:
            if verboten in text:
                fehler.append(f"{datei.relative_to(wurzel)} enthält verbotene Abhängigkeit {verboten}")

pruefe("MathematikRechenSystem", ("import android.", "import androidx.", "import de.TeutonStudio.KnotenKartenVerwalter", "import de.TeutonStudio.MathematikKartenAdapter", "import de.TeutonStudio.MathematikKnoten"))
pruefe("KnotenKartenVerwalter", ("import de.TeutonStudio.MathematikRechenSystem", "import de.TeutonStudio.MathematikKartenAdapter", "import de.TeutonStudio.MathematikKnoten", "import de.TeutonStudio.MathematikAtlas"))
pruefe("MathematikKartenAdapter", ("import de.TeutonStudio.MathematikKnoten", "import de.TeutonStudio.MathematikAtlas"))

if fehler:
    print("Architekturprüfung fehlgeschlagen:")
    print("\n".join(f"- {f}" for f in fehler))
    sys.exit(1)
print("Architekturprüfung erfolgreich.")
