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


# Der sichtbare mathematische Erstellen-Katalog ist plattformneutral.
app_root = wurzel / "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas"
alter_versionskatalog = app_root / "MathematikKnotenVorlagenV2300.kt"
if alter_versionskatalog.exists():
    fehler.append("Der app-seitige Versionskatalog MathematikKnotenVorlagenV2300.kt darf nicht zurückkehren")

kanonischer_katalog = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/katalog/KanonischerMathematikKnotenKatalog.kt"
if not kanonischer_katalog.exists():
    fehler.append("KanonischerMathematikKnotenKatalog.kt fehlt in der Mathematikschicht")

app_katalog = app_root / "MathematikKnotenKatalog.kt"
if not app_katalog.exists():
    fehler.append("Die dünne App-Fassade MathematikKnotenKatalog.kt fehlt")
else:
    text = app_katalog.read_text(encoding="utf-8")
    if "KanonischerMathematikKnotenKatalog" not in text:
        fehler.append("Die App verwendet nicht den kanonischen Mathematikknoten-Katalog")
    if "V2300" in text:
        fehler.append("Die App-Fassade darf keine versionsspezifische Knotenkataloglogik enthalten")

desktop_zustand = wurzel / "desktopApp/src/main/kotlin/de/TeutonStudio/MathematikAtlas/desktop/DesktopAtlasZustand.kt"
if desktop_zustand.exists() and "KanonischerMathematikKnotenKatalog" not in desktop_zustand.read_text(encoding="utf-8"):
    fehler.append("DesktopAtlasZustand verwendet nicht den kanonischen Mathematikknoten-Katalog")

alter_gesamtpfad = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/geometrie/GesamterMathematikAuswerter.kt"
neuer_gesamtpfad = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/katalog/GesamterMathematikAuswerter.kt"
if alter_gesamtpfad.exists():
    fehler.append("GesamterMathematikAuswerter gehört nicht in den Geometrieordner")
if not neuer_gesamtpfad.exists():
    fehler.append("GesamterMathematikAuswerter fehlt im Katalogordner")


# Mathematische Kartenmigrationen sind plattformneutral und besitzen genau eine Pipeline.
alte_app_migrationen = (
    app_root / "TranspositionsMigration.kt",
    app_root / "speicher/MethodenAnschlussMigration.kt",
)
for datei in alte_app_migrationen:
    if datei.exists():
        fehler.append(f"App-lokale mathematische Migration darf nicht zurückkehren: {datei.relative_to(wurzel)}")

karten_codec = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/migration/MathematikKartenCodec.kt"
if not karten_codec.exists():
    fehler.append("MathematikKartenCodec.kt fehlt als gemeinsame Migrationspipeline")

app_json = app_root / "speicher/KartenJson.kt"
if not app_json.exists():
    fehler.append("KartenJson-App-Fassade fehlt")
else:
    text = app_json.read_text(encoding="utf-8")
    if "MathematikKartenCodec" not in text:
        fehler.append("KartenJson delegiert nicht an MathematikKartenCodec")
    if ".migriere" in text or "migriereTranspositionsKnoten" in text:
        fehler.append("KartenJson darf keine eigene mathematische Migrationskette pflegen")

desktop_speicher = wurzel / "desktopApp/src/main/kotlin/de/TeutonStudio/MathematikAtlas/desktop/DesktopKartenSpeicher.kt"
if not desktop_speicher.exists():
    fehler.append("DesktopKartenSpeicher fehlt")
elif "MathematikKartenCodec" not in desktop_speicher.read_text(encoding="utf-8"):
    fehler.append("DesktopKartenSpeicher verwendet nicht den gemeinsamen MathematikKartenCodec")

if fehler:
    print("Architekturprüfung fehlgeschlagen:")
    print("\n".join(f"- {f}" for f in fehler))
    sys.exit(1)
print("Architekturprüfung erfolgreich.")
