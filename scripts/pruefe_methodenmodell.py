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

wert_datei = kern / "MathematischesObjekt.kt"
wert_text = wert_datei.read_text(encoding="utf-8")
for bestandteil in (
    "interface AtlasWert",
    "interface MathematischesObjekt : AtlasWert",
    "interface DarstellungsWert : AtlasWert",
):
    if bestandteil not in wert_text:
        fehler.append(f"{wert_datei.relative_to(wurzel)}: Wertvertrag '{bestandteil}' fehlt")
if "interface DarstellungsWert : MathematischesObjekt" in wert_text:
    fehler.append(f"{wert_datei.relative_to(wurzel)}: DarstellungsWert darf kein MathematischesObjekt sein")

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
        "interface SymbolischMathematischeMethode",
        "interface BereichsanpassungsTragendeMethode",
    )
    for vertrag in erforderliche_vertraege:
        if vertrag not in vertrag_text:
            fehler.append(f"{vertrag_datei.relative_to(wurzel)}: erforderlicher Vertrag '{vertrag}' fehlt")

    methode_obervertrag = vertrag_text.split("interface Methode", 1)[1].split("interface SignaturtragendeMethode", 1)[0]
    for verboten in ("MathematischesObjekt", "MengenAusdruck", "zielMenge", "werteVorr", "vorschrift", "parameter"):
        if verboten in methode_obervertrag:
            fehler.append(
                f"{vertrag_datei.relative_to(wurzel)}: allgemeiner Methode-Vertrag enthält mathematische Semantik '{verboten}'"
            )

    signatur_vertrag = vertrag_text.split("interface SignaturtragendeMethode", 1)[1].split(
        "interface MathematischeSignaturtragendeMethode", 1
    )[0]
    if "MengenAusdruck" in signatur_vertrag:
        fehler.append(
            f"{vertrag_datei.relative_to(wurzel)}: SignaturtragendeMethode darf keinen MengenAusdruck voraussetzen"
        )

fundament_datei = kern / "MethodenFundament.kt"
fundament_text = fundament_datei.read_text(encoding="utf-8")
for bestandteil in (
    "data class MethodenKomponente(",
    "val id: String",
    "val typ: TypAusdruck",
    "data class MethodenSignatur(",
    "val ergebnisse: List<MethodenKomponente>",
    "data class MathematischeMethodenSignatur(",
    "val effektiverDefinitionsRaum: MengenAusdruck?",
    "val kanonischerArgumentRaum: Tupelraum",
    "val zielRaum: Tupelraum",
):
    if bestandteil not in fundament_text:
        fehler.append(f"{fundament_datei.relative_to(wurzel)}: Signaturbestandteil '{bestandteil}' fehlt")
neutraler_signaturblock = fundament_text.split("data class MethodenSignatur(", 1)[1].split(
    "private fun tupelTyp", 1
)[0]
if "MengenAusdruck" in neutraler_signaturblock:
    fehler.append(
        f"{fundament_datei.relative_to(wurzel)}: neutrale MethodenSignatur enthält mathematische Mengen"
    )

methoden_datei = kern / "Methoden.kt"
methoden_text = methoden_datei.read_text(encoding="utf-8")
if "data class MathematischeMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: konkrete MathematischeMethode fehlt")
if ") : SymbolischMathematischeMethode" not in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: MathematischeMethode trägt die mathematische Capability nicht")
for bestandteil in (
    "override val mathematischeSignatur",
    "val ergebnisTupel: Tupel",
    "val zielRaum: Tupelraum",
    "fun wendeMathematischAlsTupelAn(argumente: Tupel): Tupel",
    "fun prüfeMethodenTypKomposition(",
    "fun prüfeMathematischeKomposition(",
):
    if bestandteil not in methoden_text:
        fehler.append(f"{methoden_datei.relative_to(wurzel)}: kanonischer Methodenbestandteil '{bestandteil}' fehlt")
if "zielInnen == wertevorratAußen" in methoden_text:
    fehler.append(f"{methoden_datei.relative_to(wurzel)}: Komposition verlangt weiterhin unnötige Mengengleichheit")

typ_datei = kern / "MathematischeTypen.kt"
typ_text = typ_datei.read_text(encoding="utf-8")
if 'val AtlasWert = TypId("atlas.wert")' not in typ_text:
    fehler.append(f"{typ_datei.relative_to(wurzel)}: neutraler Typobervertrag atlas.wert fehlt")
for verboten in ("Methode to Objekt", "Grafik to Objekt", "SvgStil to Objekt"):
    if verboten in typ_text:
        fehler.append(f"{typ_datei.relative_to(wurzel)}: neutrale Typdomäne ist wieder mathematisch gekoppelt: '{verboten}'")
for erforderlich in ("Objekt to AtlasWert", "Methode to AtlasWert", "Grafik to AtlasWert"):
    if erforderlich not in typ_text:
        fehler.append(f"{typ_datei.relative_to(wurzel)}: Typbeziehung '{erforderlich}' fehlt")

mengen_datei = kern / "Mengen.kt"
mengen_text = mengen_datei.read_text(encoding="utf-8")
tupelraum_block = mengen_text.split("data class Tupelraum", 1)[1].split("data class Folgenraum", 1)[0]
if "require(komponenten.isNotEmpty())" in tupelraum_block:
    fehler.append(f"{mengen_datei.relative_to(wurzel)}: Tupelraum verbietet weiterhin den leeren Tupelraum")
if '0 -> "\\\\{()\\\\}"' not in tupelraum_block:
    fehler.append(f"{mengen_datei.relative_to(wurzel)}: leerer Tupelraum wird nicht als {{()}} dargestellt")

aufruf_datei = wurzel / "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/MethodenAufrufAuswerter.kt"
if aufruf_datei.exists():
    aufruf_text = aufruf_datei.read_text(encoding="utf-8")
    if not any(
        muster in aufruf_text
        for muster in (
            "methode is MathematischAuswertbareMethode",
            "methode as? MathematischAuswertbareMethode",
            "val auswertbareMethode = methode as? MathematischAuswertbareMethode",
        )
    ):
        fehler.append(
            f"{aufruf_datei.relative_to(wurzel)}: mathematischer Methodenaufruf ist nicht an seine Capability gebunden"
        )

laufzeit_datei = wurzel / "MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/Auswertung.kt"
laufzeit_text = laufzeit_datei.read_text(encoding="utf-8")
if "val objekt: AtlasWert" not in laufzeit_text:
    fehler.append(f"{laufzeit_datei.relative_to(wurzel)}: allgemeiner Laufzeitkanal transportiert nicht AtlasWert")

# Die generische UI-Synchronisierung darf ihre Handles nicht mehr aus Mathematikfeldern ableiten.
sync_datei = wurzel / "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/MethodenAufrufSynchronisierung.kt"
if sync_datei.exists():
    sync_text = sync_datei.read_text(encoding="utf-8")
    for verboten in (
        "methode.parameter",
        "methode.vorschrift",
        "methode.zielMenge",
        "methode.werteVorräte",
        "methode.effektiverWerteVorrat",
    ):
        if verboten in sync_text:
            fehler.append(
                f"{sync_datei.relative_to(wurzel)}: generische Handle-Synchronisierung liest mathematisches Feld '{verboten}'"
            )
    for erforderlich in (
        "SignaturtragendeMethode",
        "AnschlussVertrag(komponent.typ)",
        "signatur.ergebnisTyp",
        "mathematischeSignatur",
    ):
        if erforderlich not in sync_text:
            fehler.append(f"{sync_datei.relative_to(wurzel)}: neutrale Handle-Ableitung '{erforderlich}' fehlt")

anschluss_datei = wurzel / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAnschlussArten.kt"
if anschluss_datei.exists():
    anschluss_text = anschluss_datei.read_text(encoding="utf-8")
    if 'id = AnschlussArtId("atlas.wert")' not in anschluss_text:
        fehler.append(f"{anschluss_datei.relative_to(wurzel)}: AtlasWert-Anschlussart fehlt")
    if "val Methode = AnschlussArt(" not in anschluss_text or "elternArt = AtlasWert.id" not in anschluss_text:
        fehler.append(f"{anschluss_datei.relative_to(wurzel)}: Methodenanschluss ist nicht unter AtlasWert eingeordnet")

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
    for bestandteil in (
        "data class MethodenRestriktion",
        "data class MethodenBereichsanpassung",
        "fun passeMethodenBereichAn(",
        "override val mathematischeSignatur",
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
    if "effektiverDefinitionsRaum = kanonisiereBereichsRaum" not in restriktions_text:
        fehler.append(f"{restriktions_datei.relative_to(wurzel)}: Restriktion ändert nicht explizit nur den Definitionsraum")

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
    print("Das Methodenmodell verletzt den domänenneutralen G0/#431-Vertrag:")
    print("\n".join(f"- {eintrag}" for eintrag in fehler))
    sys.exit(1)

print("Domänenneutraler AtlasWert-/Methodenvertrag und mathematische Bereichssemantik erfolgreich geprüft.")
