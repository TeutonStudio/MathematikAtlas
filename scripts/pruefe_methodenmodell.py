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
    r"\b(?:data\s+class|class)\s+Methode\b": "Methode darf kein geschlossener Klassen-Laufzeittyp mehr sein",
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

vertrag_datei = kern / "MethodenVertrag.kt"
if not vertrag_datei.exists():
    fehler.append("MathematikRechenSystem/.../kern/MethodenVertrag.kt: offener Methodenvertrag fehlt")
else:
    vertrag_text = vertrag_datei.read_text(encoding="utf-8")
    erforderliche_vertraege = (
        "interface Methode",
        "interface SignaturtragendeMethode",
        "interface MathematischAuswertbareMethode",
        "interface BereichsanpassungsTragendeMethode",
    )
    for vertrag in erforderliche_vertraege:
        if vertrag not in vertrag_text:
            fehler.append(f"{vertrag_datei.relative_to(wurzel)}: erforderlicher Vertrag '{vertrag}' fehlt")

    if "interface Methode" in vertrag_text and "interface SignaturtragendeMethode" in vertrag_text:
        methode_obervertrag = vertrag_text.split("interface Methode", 1)[1].split("interface SignaturtragendeMethode", 1)[0]
        if "bereichsanpassung" in methode_obervertrag:
            fehler.append(
                f"{vertrag_datei.relative_to(wurzel)}: Bereichsanpassung darf kein Sonderfall im allgemeinen Methode-Obervertrag sein"
            )

methoden_datei = kern / "Methoden.kt"
methoden_text = methoden_datei.read_text(encoding="utf-8")
if "data class MathematischeMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: konkrete MathematischeMethode fehlt")
if ") : MathematischAuswertbareMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: MathematischeMethode trägt die Auswertungs-Capability nicht")
for eigenschaft in ("val ausgaben:", "val zielMengen:"):
    if eigenschaft in methoden_text:
        fehler.append(f"{methoden_datei.relative_to(wurzel)}: persistente Mehrfachausgabe-Eigenschaft {eigenschaft}")

aufruf_datei = wurzel / "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/MethodenAufrufAuswerter.kt"
if aufruf_datei.exists():
    aufruf_text = aufruf_datei.read_text(encoding="utf-8")
    if "methode is MathematischAuswertbareMethode" not in aufruf_text:
        fehler.append(
            f"{aufruf_datei.relative_to(wurzel)}: Methodenaufruf ist nicht an die mathematische Auswertungs-Capability gebunden"
        )

# Mathematische Konstruktionen müssen die offene Methode-Grenze explizit verengen.
graph_datei = kern / "MethodenGraph.kt"
if graph_datei.exists():
    graph_text = graph_datei.read_text(encoding="utf-8")
    if "val methode: MathematischeMethode" not in graph_text:
        fehler.append(f"{graph_datei.relative_to(wurzel)}: Graphmenge speichert keine explizite MathematischeMethode")
    if 'alsMathematischeMethode("einen mathematischen Funktionsgraphen")' not in graph_text:
        fehler.append(f"{graph_datei.relative_to(wurzel)}: Graphkonstruktion prüft die mathematische Capability nicht")

restriktions_datei = kern / "MethodenRestriktion.kt"
if restriktions_datei.exists():
    restriktions_text = restriktions_datei.read_text(encoding="utf-8")
    if 'basis.alsMathematischeMethode("mathematische Restriktion")' not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Restriktion verengt die Basismethode nicht")
    if "val basis: MathematischeMethode" not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Restriktionsherkunft speichert keine mathematische Basis")
    for bestandteil in (
        "data class MethodenRestriktion",
        "data class MethodenBereichsanpassung",
        "fun passeMethodenBereichAn(",
    ):
        if bestandteil not in restriktions_text:
            fehler.append(f"{restriktions_datei.relative_to(wurzel)}: getrennte Methodenbereich-Semantik '{bestandteil}' fehlt")
    restriktions_signatur = re.search(
        r"fun\s+restriktiereMethode\s*\((.*?)\)\s*:\s*MethodenRestriktionsErgebnis",
        restriktions_text,
        flags=re.DOTALL,
    )
    if restriktions_signatur is None:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: kanonische Restriktionsfunktion fehlt")
    elif "ergänz" in restriktions_signatur.group(1).lower():
        fehler.append(
            f"{restriktions_datei.relative_to(wurzel)}: reine Restriktion darf keine Ergänzungsmethoden akzeptieren"
        )
    if "\\\\operatorname{Bereichsanpassung}" not in restriktions_text:
        fehler.append(
            f"{restriktions_datei.relative_to(wurzel)}: Bereichsanpassung benötigt eine von f|_M getrennte Darstellung"
        )

knoten_datei = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/RestriktionsKnoten.kt"
if knoten_datei.exists():
    knoten_text = knoten_datei.read_text(encoding="utf-8")
    for bestandteil in (
        "METHODEN_BEREICHS_OPERATOR_RESTRIKTION",
        "METHODEN_BEREICHS_OPERATOR_ANPASSUNG",
        "val Restriktion = KnotenVorlage(",
        "val Bereichsanpassung = KnotenVorlage(",
    ):
        if bestandteil not in knoten_text:
            fehler.append(f"{knoten_datei.relative_to(wurzel)}: getrennte Knotenvariante '{bestandteil}' fehlt")

codec_datei = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/migration/MathematikKartenCodec.kt"
if codec_datei.exists():
    codec_text = codec_datei.read_text(encoding="utf-8")
    if codec_text.count(".migriereMethodenBereichsOperatoren()") < 2:
        fehler.append(
            f"{codec_datei.relative_to(wurzel)}: Methodenbereich-Migration muss vor Speichern und nach Dekodierung laufen"
        )

if fehler:
    print("Das Methodenmodell verletzt den G0.1/G0.2-Vertrag:")
    print("\n".join(f"- {eintrag}" for eintrag in fehler))
    sys.exit(1)

print("G0.1/G0.2-Methodenmodell und Methodenbereich-Vertrag erfolgreich geprüft.")
