#!/usr/bin/env python3
from pathlib import Path


def ersetze(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    gefunden = text.count(alt)
    if gefunden < anzahl:
        raise SystemExit(f"{pfad}: erwartete Ersetzung fehlt: {alt[:100]!r}; gefunden={gefunden}")
    text = text.replace(alt, neu, anzahl)
    datei.write_text(text, encoding="utf-8")


# Zahlbereiche anhand der stabilen mathematischen Darstellung statt unzulässiger Objektgleichheit erkennen.
ersetze(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/UniversellerZahlenRechner.kt",
    '''        werteVorrat == KomplexeZahlen || ausdruck is KomplexeZahl -> ZahlenRechnerBereich.KOMPLEX
        werteVorrat == ReelleZahlen -> ZahlenRechnerBereich.REELL
        werteVorrat == RationaleZahlen -> ZahlenRechnerBereich.RATIONAL
        werteVorrat == GanzeZahlen -> ZahlenRechnerBereich.GANZ
        werteVorrat == NatürlicheZahlen -> if (ausdruck == RationaleZahl.Null) {
            ZahlenRechnerBereich.NATUERLICH_MIT_NULL
        } else {
            ZahlenRechnerBereich.NATUERLICH
        }
''',
    '''        "\\\\mathbb C" in vorratLatex || ausdruck is KomplexeZahl -> ZahlenRechnerBereich.KOMPLEX
        "\\\\mathbb R" in vorratLatex -> ZahlenRechnerBereich.REELL
        "\\\\mathbb Q" in vorratLatex -> ZahlenRechnerBereich.RATIONAL
        "\\\\mathbb Z" in vorratLatex -> ZahlenRechnerBereich.GANZ
        "\\\\mathbb N_0" in vorratLatex -> ZahlenRechnerBereich.NATUERLICH_MIT_NULL
        "\\\\mathbb N" in vorratLatex -> if (ausdruck == RationaleZahl.Null) {
            ZahlenRechnerBereich.NATUERLICH_MIT_NULL
        } else {
            ZahlenRechnerBereich.NATUERLICH
        }
''',
)

# Bestehende Ausdruckskonstruktoren korrekt verwenden.
ersetze(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/UniversellerZahlenRechnerKnoten.kt",
    '(argument as? KomplexeZahl)?.let(::Argument)',
    '(argument as? KomplexeZahl)?.let { Argument(it.imaginärteil, it.realteil) }',
)
for alt in (
    'BenannteMenge("hyperreell", latex)',
    'BenannteMenge("quaternion", latex)',
    'BenannteMenge("modulo", latex)',
    'BenannteMenge("zahlbereich-unbekannt", latex)',
):
    ersetze(
        "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/UniversellerZahlenRechnerKnoten.kt",
        alt,
        'BenannteMenge(latex)',
    )

# Die Auswertungskontexte benötigen ausdrücklich eine Karte und benannte Argumente.
test_pfad = "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/UniversellerZahlenRechnerKnotenTest.kt"
for name in ("knoten", "getrennt", "tupel"):
    datei = Path(test_pfad)
    text = datei.read_text(encoding="utf-8")
    alt = f"KnotenAuswertungsKontext(\n                {name},\n"
    neu = f"KnotenAuswertungsKontext(\n                knoten = {name},\n                eingänge = "
    treffer = text.count(alt)
    if treffer:
        text = text.replace(alt, neu)
        datei.write_text(text, encoding="utf-8")

# Nach dem obigen Präfix folgt bereits mapOf(...); nur der bisher dritte Positionsparameter wird ersetzt.
datei = Path(test_pfad)
text = datei.read_text(encoding="utf-8")
alt = "                RechenKontext(),\n            ),"
treffer = text.count(alt)
if treffer != 4:
    raise SystemExit(f"{test_pfad}: vier RechenKontext-Enden erwartet, gefunden={treffer}")
text = text.replace(
    alt,
    '                karte = KartenDaten(name = "Test"),\n                rechenKontext = RechenKontext(),\n            ),',
)
datei.write_text(text, encoding="utf-8")

# Paketimport muss die bereits migrierten Knoten remappen, nicht die ursprünglichen Knoten wieder einsetzen.
ersetze(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/speicher/KartenSpeicher.kt",
    '''        val remappteKarten = paket.karten.map { karte ->
            karte.migriereMethodenAnschlüsse()
                .migriereUniversellenZahlenRechner()
                .copy(
''',
    '''        val remappteKarten = paket.karten.map { karte ->
            val migriert = karte.migriereMethodenAnschlüsse()
                .migriereUniversellenZahlenRechner()
            migriert.copy(
''',
)
ersetze(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/speicher/KartenSpeicher.kt",
    "                            knoten = karte.knoten.map { knoten ->",
    "                            knoten = migriert.knoten.map { knoten ->",
)
