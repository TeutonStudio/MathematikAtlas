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


# Konzept- und Preview-Verträge der kanonischen Mathematikschicht.
konzept_ordner = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/konzeptknoten"
if (konzept_ordner / "GenerischeKonzeptKnoten.kt").exists():
    fehler.append("GenerischeKonzeptKnoten.kt darf nach der expliziten Migration nicht mehr existieren")
for datei in konzept_ordner.glob("*.kt"):
    if "GenerischeKonzeptKnoten" in datei.read_text(encoding="utf-8"):
        fehler.append(f"{datei.relative_to(wurzel)} verwendet den verbotenen Catch-all GenerischeKonzeptKnoten")

preview_ordner = wurzel / "MathematikKnoten/src/debug/kotlin/de/TeutonStudio/MathematikKnoten/previews"
preview_index = wurzel / "MathematikKnoten/src/debug/resources/de/TeutonStudio/MathematikKnoten/previews/index.json"
if not preview_index.exists():
    fehler.append("Preview-Index fehlt")
else:
    import json
    daten = json.loads(preview_index.read_text(encoding="utf-8"))
    erwartete = set(daten["previewDateien"])
    vorhandene = {p.name for p in preview_ordner.glob("*Preview.kt") if p.name != "KnotenPreviewRahmen.kt"}
    if vorhandene != erwartete:
        fehler.append(f"Previewdateien stimmen nicht mit Index überein: erwartet {len(erwartete)}, vorhanden {len(vorhandene)}")
    for name in erwartete:
        text = (preview_ordner / name).read_text(encoding="utf-8")
        if "@Preview" not in text or "KnotenVariantenPreview" not in text:
            fehler.append(f"{name} verwendet nicht den echten Previewrahmen")

app_konzepte = wurzel / "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte"
verbotene_generatoren = {
    "DivisionDefinitionsKarten.kt", "MengenoperatorDefinitionsKarten.kt",
    "OrdnungsrelationDefinitionsKarte.kt", "ReellesIntervallDefinitionsKarte.kt",
    "ZahlenRechnerDefinitionsKarten.kt", "MatrixProduktKonzept.kt",
}
for name in verbotene_generatoren:
    if (app_konzepte / name).exists():
        fehler.append(f"App-lokaler statischer Kartengenerator {name} wurde nicht entfernt")

if fehler:
    print("Architekturprüfung fehlgeschlagen:")
    print("\n".join(f"- {f}" for f in fehler))
    sys.exit(1)
print("Architekturprüfung erfolgreich.")
