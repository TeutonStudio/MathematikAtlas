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
    wurzel / "desktopApp/src/main",
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
    if not quellwurzel.exists():
        continue
    for datei in quellwurzel.rglob("*.kt"):
        text = datei.read_text(encoding="utf-8")
        for muster, beschreibung in verbotene_muster.items():
            for treffer in re.finditer(muster, text):
                zeile = text.count("\n", 0, treffer.start()) + 1
                fehler.append(f"{datei.relative_to(wurzel)}:{zeile}: {beschreibung}")

kern = wurzel / "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern"
if (kern / "Funktionen.kt").exists():
    fehler.append("MathematikRechenSystem/.../kern/Funktionen.kt: historische Kerndatei existiert noch")

atlaswert_datei = kern / "AtlasWert.kt"
if not atlaswert_datei.exists():
    fehler.append("MathematikRechenSystem/.../kern/AtlasWert.kt: domänenneutraler AtlasWert-Vertrag fehlt")
else:
    atlaswert_text = atlaswert_datei.read_text(encoding="utf-8")
    if not re.search(r"\binterface\s+AtlasWert\b", atlaswert_text):
        fehler.append(f"{atlaswert_datei.relative_to(wurzel)}: AtlasWert ist kein Interface")
    atlaswert_block = atlaswert_text.split("interface AtlasWert", 1)[1].split("}", 1)[0]
    for verboten in ("zuLatex", "MengenAusdruck", "MathematischesObjekt"):
        if verboten in atlaswert_block:
            fehler.append(f"{atlaswert_datei.relative_to(wurzel)}: AtlasWert erzwingt verbotene Mathematiksemantik '{verboten}'")

objekt_datei = kern / "MathematischesObjekt.kt"
objekt_text = objekt_datei.read_text(encoding="utf-8")
if not re.search(r"sealed\s+interface\s+MathematischesObjekt\s*:\s*AtlasWert", objekt_text):
    fehler.append(f"{objekt_datei.relative_to(wurzel)}: MathematischesObjekt muss AtlasWert implementieren")
if re.search(r"interface\s+DarstellungsWert\s*:\s*[^\n{]*MathematischesObjekt", objekt_text):
    fehler.append(f"{objekt_datei.relative_to(wurzel)}: DarstellungsWert darf kein MathematischesObjekt sein")
if not re.search(r"interface\s+DarstellungsWert\s*:\s*AtlasWert", objekt_text):
    fehler.append(f"{objekt_datei.relative_to(wurzel)}: DarstellungsWert muss direkt am neutralen AtlasWert-Kanal hängen")

vertrag_datei = kern / "MethodenVertrag.kt"
if not vertrag_datei.exists():
    fehler.append("MathematikRechenSystem/.../kern/MethodenVertrag.kt: offener Methodenvertrag fehlt")
else:
    vertrag_text = vertrag_datei.read_text(encoding="utf-8")
    erforderliche_vertraege = (
        "interface Methode",
        "interface SignaturtragendeMethode",
        "interface MathematischeSignaturtragendeMethode",
        "interface MathematischAuswertbareMethode",
        "interface BereichsanpassungsTragendeMethode",
    )
    for vertrag in erforderliche_vertraege:
        if vertrag not in vertrag_text:
            fehler.append(f"{vertrag_datei.relative_to(wurzel)}: erforderlicher Vertrag '{vertrag}' fehlt")

    methode_obervertrag = vertrag_text.split("interface Methode", 1)[1].split("interface SignaturtragendeMethode", 1)[0]
    if "MathematischesObjekt" in methode_obervertrag:
        fehler.append(f"{vertrag_datei.relative_to(wurzel)}: Methode darf kein MathematischesObjekt voraussetzen")
    if "MengenAusdruck" in methode_obervertrag:
        fehler.append(f"{vertrag_datei.relative_to(wurzel)}: allgemeiner Methode-Vertrag darf keine MengenAusdruck-Semantik kennen")
    if "bereichsanpassung" in methode_obervertrag:
        fehler.append(f"{vertrag_datei.relative_to(wurzel)}: Bereichsanpassung darf kein Sonderfall im allgemeinen Methode-Obervertrag sein")

    signatur_obervertrag = vertrag_text.split("interface SignaturtragendeMethode", 1)[1].split("interface MathematischeSignaturtragendeMethode", 1)[0]
    if "MengenAusdruck" in signatur_obervertrag:
        fehler.append(f"{vertrag_datei.relative_to(wurzel)}: SignaturtragendeMethode erzwingt MengenAusdruck")

fundament_datei = kern / "MethodenFundament.kt"
fundament_text = fundament_datei.read_text(encoding="utf-8")
if "data class MethodenKomponente(" not in fundament_text or "val typ: TypAusdruck" not in fundament_text:
    fehler.append(f"{fundament_datei.relative_to(wurzel)}: neutrale MethodenKomponente mit TypAusdruck fehlt")
if "data class MethodenSignatur(" not in fundament_text:
    fehler.append(f"{fundament_datei.relative_to(wurzel)}: neutrale MethodenSignatur fehlt")
else:
    neutraler_block = fundament_text.split("data class MethodenSignatur(", 1)[1].split("data class MathematischeArgumentKomponente", 1)[0]
    if "MengenAusdruck" in neutraler_block:
        fehler.append(f"{fundament_datei.relative_to(wurzel)}: neutrale MethodenSignatur enthält mathematische Mengen")
    for bestandteil in ("argumentTupelTyp", "ergebnisTupelTyp"):
        if bestandteil not in neutraler_block:
            fehler.append(f"{fundament_datei.relative_to(wurzel)}: neutrale Signatur kanonisiert '{bestandteil}' nicht")
if "data class MathematischeMethodenSignatur(" not in fundament_text:
    fehler.append(f"{fundament_datei.relative_to(wurzel)}: zusätzliche mathematische Raum-/Mengensignatur fehlt")
for bestandteil in ("kanonischerArgumentRaum", "definitionsRaum", "zielRaum"):
    if bestandteil not in fundament_text:
        fehler.append(f"{fundament_datei.relative_to(wurzel)}: mathematische Signatur benötigt '{bestandteil}'")

methoden_datei = kern / "Methoden.kt"
methoden_text = methoden_datei.read_text(encoding="utf-8")
if "data class MathematischeMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: konkrete MathematischeMethode fehlt")
if ") : MathematischAuswertbareMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: MathematischeMethode trägt die Auswertungs-Capability nicht")
for eigenschaft in ("val ausgaben:", "val zielMengen:"):
    if eigenschaft in methoden_text:
        fehler.append(f"{methoden_datei.relative_to(wurzel)}: persistente Mehrfachausgabe-Eigenschaft {eigenschaft}")
for bestandteil in ("kanonischeVorschrift", "wendeKanonischMathematischAn", "mathematischeSignatur", "override val signatur"):
    if bestandteil not in methoden_text:
        fehler.append(f"{methoden_datei.relative_to(wurzel)}: kanonische mathematische Methodenstruktur '{bestandteil}' fehlt")

mengen_datei = kern / "Mengen.kt"
mengen_text = mengen_datei.read_text(encoding="utf-8")
if "data class Tupelraum(val komponenten: List<MengenAusdruck>)" not in mengen_text:
    fehler.append(f"{mengen_datei.relative_to(wurzel)}: Tupelraum darf den leeren Komponentenfall nicht verbieten")
if '0 -> "\\\\{()\\\\}"' not in mengen_text:
    fehler.append(f"{mengen_datei.relative_to(wurzel)}: leerer Tupelraum muss als {{()}} kanonisiert werden")
if "if (faktoren.isEmpty()) return Tupelraum(emptyList())" not in mengen_text:
    fehler.append(f"{mengen_datei.relative_to(wurzel)}: leeres kartesisches Produkt darf nicht zu LeereMenge kollabieren")

aufruf_datei = wurzel / "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/MethodenAufrufAuswerter.kt"
if aufruf_datei.exists():
    aufruf_text = aufruf_datei.read_text(encoding="utf-8")
    if not any(muster in aufruf_text for muster in (
        "methode is MathematischAuswertbareMethode",
        "methode as? MathematischAuswertbareMethode",
    )):
        fehler.append(f"{aufruf_datei.relative_to(wurzel)}: mathematischer Methodenaufruf ist nicht an die Auswertungs-Capability gebunden")

graph_datei = kern / "MethodenGraph.kt"
if graph_datei.exists():
    graph_text = graph_datei.read_text(encoding="utf-8")
    if "val methode: MathematischeMethode" not in graph_text:
        fehler.append(f"{graph_datei.relative_to(wurzel)}: Graphmenge speichert keine explizite MathematischeMethode")
    if 'alsMathematischeMethode("einen mathematischen Funktionsgraphen")' not in graph_text:
        fehler.append(f"{graph_datei.relative_to(wurzel)}: Graphkonstruktion prüft die mathematische Capability nicht")

kompositions_datei = kern / "Komposition.kt"
if kompositions_datei.exists():
    kompositions_text = kompositions_datei.read_text(encoding="utf-8")
    if "innenSignatur.ergebnisTupelTyp" not in kompositions_text or "außenSignatur.argumentTupelTyp" not in kompositions_text:
        fehler.append(f"{kompositions_datei.relative_to(wurzel)}: generische Komposition prüft nicht die neutralen Tupeltypen")
    if "prüfeTeilmenge" not in kompositions_text:
        fehler.append(f"{kompositions_datei.relative_to(wurzel)}: mathematische Komposition prüft keine Teilmengenkompatibilität")
    if re.search(r"zielMenge\s*==\s*.*werteVorrat|werteVorrat\s*==\s*.*zielMenge", kompositions_text):
        fehler.append(f"{kompositions_datei.relative_to(wurzel)}: mathematische Komposition verlangt wieder Mengengleichheit")

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
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: reine Restriktion darf keine Ergänzungsmethoden akzeptieren")
    if "override val signatur" not in restriktions_text or "get() = basis.signatur" not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Restriktion darf den neutralen Methodenvertrag nicht verändern")
    if "copy(effektiverDefinitionsRaum = werteVorrat)" not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Restriktion muss ausschließlich den mathematischen Definitionsraum ändern")
    if "\\\\operatorname{Bereichsanpassung}" not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Bereichsanpassung benötigt eine von f|_M getrennte Darstellung")

laufzeit_datei = wurzel / "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/Auswertung.kt"
if laufzeit_datei.exists():
    laufzeit_text = laufzeit_datei.read_text(encoding="utf-8")
    bedingter_block = laufzeit_text.split("data class BedingterWert(", 1)[1].split(")\n\n", 1)[0]
    if "val objekt: AtlasWert" not in bedingter_block:
        fehler.append(f"{laufzeit_datei.relative_to(wurzel)}: generischer Kartenwertkanal transportiert nicht AtlasWert")

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
        fehler.append(f"{codec_datei.relative_to(wurzel)}: Methodenbereich-Migration muss vor Speichern und nach Dekodierung laufen")

if fehler:
    print("Das Methodenmodell verletzt den G0.1-G0.5-Vertrag:")
    print("\n".join(f"- {eintrag}" for eintrag in fehler))
    sys.exit(1)

print("G0.1-G0.5-Methodenmodell, AtlasWert- und Methodenbereich-Vertrag erfolgreich geprüft.")
