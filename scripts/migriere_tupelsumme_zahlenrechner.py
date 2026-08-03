#!/usr/bin/env python3
from pathlib import Path


def ersetze(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    treffer = text.count(alt)
    if treffer != anzahl:
        raise SystemExit(f"{pfad}: erwartet {anzahl} Treffer, gefunden {treffer}: {alt!r}")
    datei.write_text(text.replace(alt, neu, anzahl), encoding="utf-8")


spur = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/SpurKnoten.kt"
ersetze(
    spur,
    "import de.TeutonStudio.MathematikRechenSystem.kern.Tupel\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.Tupel\nimport de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator\n",
)
ersetze(
    spur,
    "        art = MathematikKnotenVorlagen.IterierteSumme.art,",
    "        art = ZAHLENRECHNER_ART,",
)
ersetze(
    spur,
    '        standardParameter = mapOf("eingabeModus" to ITERIERTE_SUMME_TUPEL_MODUS),',
    '''        standardParameter = mapOf(
            ZAHLENRECHNER_OPERATOR to UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId,
            "eingabeModus" to ITERIERTE_SUMME_TUPEL_MODUS,
        ),''',
)
ersetze(
    spur,
    '''

    registriere(MathematikKnotenVorlagen.IterierteSumme.art) { kontext ->
        val tupelWert = kontext.eingänge["tupel"]
        val ergebnis = if (tupelWert != null) {
            val tupel = tupelWert.objekt as? Tupel ?: error("Der Tupel-Eingang enthält kein Tupel.")
            addition(tupel.elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck
                    ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
            })
        } else {
            val methode = kontext.eingänge["methode"]?.objekt as? Methode ?: error("Zahlfunktion fehlt.")
            val indexMenge = kontext.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            iterierteSumme(methode, indexMenge)
        }
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(ergebnis, annahmen)),
            eingänge = kontext.eingänge,
        )
    }
''',
    "\n",
)

# Nicht mehr benötigte Imports aus dem alten Parallelauswerter entfernen.
for zeile in (
    "import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.Methode\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.Tupel\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.addition\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.iterierteSumme\n",
):
    ersetze(spur, zeile, "")

universal = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/UniversellerZahlenRechnerKnoten.kt"
ersetze(
    universal,
    "            val standard = standardAnschluesse(operator)",
    '''            val standard = if (
                operator == UniversellerZahlenOperator.ITERIERTE_SUMME &&
                alt.parameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS
            ) {
                listOf(
                    spezialEingang("tupel", MathematikAnschlussArten.Tupel.id, 0),
                    zahlenAusgang(),
                )
            } else {
                standardAnschluesse(operator)
            }''',
)
ersetze(
    universal,
    '''        UniversellerZahlenOperator.ITERIERTE_SUMME,
        UniversellerZahlenOperator.ITERIERTES_PRODUKT,
        -> {
            val methode = k.eingänge["methode"]?.objekt as? Methode
                ?: error("Iterationsmethode fehlt.")
            val indexmenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck
                ?: error("Indexmenge fehlt.")
            val objekt = if (operator == UniversellerZahlenOperator.ITERIERTE_SUMME) {
                iterierteSumme(methode, indexmenge)
            } else {
                iteriertesProdukt(methode, indexmenge)
            }
            UniverselleZahlenAusgabe(objekt, gemeinsam)
        }''',
    '''        UniversellerZahlenOperator.ITERIERTE_SUMME,
        UniversellerZahlenOperator.ITERIERTES_PRODUKT,
        -> {
            val tupel = k.eingänge["tupel"]?.objekt as? Tupel
            val objekt = if (tupel != null) {
                val komponenten = tupel.elemente.mapIndexed { index, element ->
                    element as? ZahlAusdruck
                        ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
                }
                if (operator == UniversellerZahlenOperator.ITERIERTE_SUMME) {
                    addition(komponenten)
                } else {
                    multiplikation(komponenten)
                }
            } else {
                val methode = k.eingänge["methode"]?.objekt as? Methode
                    ?: error("Iterationsmethode fehlt.")
                val indexmenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck
                    ?: error("Indexmenge fehlt.")
                if (operator == UniversellerZahlenOperator.ITERIERTE_SUMME) {
                    iterierteSumme(methode, indexmenge)
                } else {
                    iteriertesProdukt(methode, indexmenge)
                }
            }
            UniverselleZahlenAusgabe(objekt, gemeinsam)
        }''',
)

katalog = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/AussagenLogikKnoten.kt"
ersetze(
    katalog,
    '''private fun vorlagenSchlüssel(vorlage: KnotenVorlage): Pair<String, String> =
    vorlage.art to (vorlage.standardParameter["operator"] ?: vorlage.name)''',
    '''private fun vorlagenSchlüssel(vorlage: KnotenVorlage): Pair<String, String> =
    vorlage.art to listOf(
        vorlage.standardParameter["operator"].orEmpty(),
        vorlage.standardParameter["eingabeModus"].orEmpty(),
        vorlage.name,
    ).joinToString("|")''',
)

test = "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/SpurKnotenTest.kt"
ersetze(
    test,
    "        val ergebnis = register.finde(MathematikKnotenVorlagen.IterierteSumme.art)!!.auswerten(",
    "        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(",
)
ersetze(
    test,
    '''        val vorlagen = alleMathematikKnotenVorlagen()
            .filter { it.art == MathematikKnotenVorlagen.IterierteSumme.art }

        assertEquals(2, vorlagen.size)''',
    '''        val vorlagen = alleMathematikKnotenVorlagen().filter {
            it.art == ZAHLENRECHNER_ART &&
                it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId
        }

        assertEquals(2, vorlagen.size)''',
)
ersetze(
    test,
    "import de.TeutonStudio.MathematikRechenSystem.kern.Tupel\n",
    "import de.TeutonStudio.MathematikRechenSystem.kern.Tupel\nimport de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator\n",
)
