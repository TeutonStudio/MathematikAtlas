#!/usr/bin/env python3
from pathlib import Path


def ersetze(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    treffer = text.count(alt)
    if treffer != anzahl:
        raise SystemExit(f"{pfad}: erwartet {anzahl} Treffer, gefunden {treffer}: {alt!r}")
    datei.write_text(text.replace(alt, neu, anzahl), encoding="utf-8")


modell = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/KonzeptModell.kt"
ersetze(
    modell,
    "import de.TeutonStudio.MathematikKnoten.SPUR_ART\n",
    """import de.TeutonStudio.MathematikKnoten.SPUR_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
""",
)
ersetze(
    modell,
    '''    val knotenArten: Set<KnotenArtId>,
    val reiter: List<KonzeptReiter>,''',
    '''    val knotenArten: Set<KnotenArtId>,
    val knotenParameter: Map<String, String> = emptyMap(),
    val reiter: List<KonzeptReiter>,''',
)
ersetze(
    modell,
    '''    init {
        require(reiter.count { it.rolle == KonzeptReiterRolle.Definition } == 1) {
            "$name benötigt genau einen Definitionsreiter."
        }
    }
}''',
    '''    init {
        require(reiter.count { it.rolle == KonzeptReiterRolle.Definition } == 1) {
            "$name benötigt genau einen Definitionsreiter."
        }
    }

    fun erklärt(knoten: KnotenDaten): Boolean =
        knoten.art in knotenArten && knotenParameter.all { (schlüssel, wert) ->
            knoten.parameter[schlüssel] == wert
        }
}''',
)
ersetze(
    modell,
    '''        festeVorlagen
            .groupBy(KnotenVorlage::art)
            .values
            .map { varianten ->
                when (varianten.first().art) {
                    MathematikKnotenVorlagen.Division.art -> DivisionDefinitionsKarten.konzept
                    MathematikKnotenVorlagen.ORDNUNGSRELATION_ART ->
                        OrdnungsrelationDefinitionsKarten.katalogKonzept(varianten)
                    else -> konzeptFür(varianten)
                }
            }''',
    '''        festeVorlagen
            .groupBy { vorlage ->
                if (vorlage.art == ZAHLENRECHNER_ART) {
                    "${vorlage.art}:${vorlage.standardParameter[ZAHLENRECHNER_OPERATOR]}"
                } else {
                    vorlage.art.toString()
                }
            }
            .values
            .map { varianten ->
                when (varianten.first().art) {
                    ZAHLENRECHNER_ART -> ZahlenRechnerDefinitionsKarten.konzept(varianten)
                    MathematikKnotenVorlagen.Division.art -> DivisionDefinitionsKarten.konzept
                    MathematikKnotenVorlagen.ORDNUNGSRELATION_ART ->
                        OrdnungsrelationDefinitionsKarten.katalogKonzept(varianten)
                    else -> konzeptFür(varianten)
                }
            }''',
)
ersetze(
    modell,
    '''    private val nachArt: Map<KnotenArtId, KonzeptDefinition> by lazy {
        alle.flatMap { konzept -> konzept.knotenArten.map { art -> art to konzept } }.toMap()
    }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? =
        if (knoten.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART) {
            OrdnungsrelationDefinitionsKarten.fürKnoten(knoten)
        } else {
            nachArt[knoten.art]
        }''',
    '''    private val nachArt: Map<KnotenArtId, List<KonzeptDefinition>> by lazy {
        alle.flatMap { konzept -> konzept.knotenArten.map { art -> art to konzept } }
            .groupBy({ it.first }, { it.second })
    }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? =
        if (knoten.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART) {
            OrdnungsrelationDefinitionsKarten.fürKnoten(knoten)
        } else {
            nachArt[knoten.art]?.firstOrNull { it.erklärt(knoten) }
        }''',
)
ersetze(
    modell,
    '''                    karte.knoten.filter { it.art in konzept.knotenArten }.forEach { knoten ->
                        add("Selbstbezug in ${konzept.id}/${reiter.id}/${karte.id}: ${knoten.id}")
                    }''',
    '''                    karte.knoten.filter(konzept::erklärt).forEach { knoten ->
                        add("Selbstbezug in ${konzept.id}/${reiter.id}/${karte.id}: ${knoten.id}")
                    }''',
)

iter_konzept = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/IterierteOperatorKonzept.kt"
ersetze(
    iter_konzept,
    '''    val konzeptVarianten = varianten.filterNot { vorlage ->
        vorlage.art == MathematikKnotenVorlagen.IterierteSumme.art &&
            vorlage.standardParameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS
    }''',
    '''    val konzeptVarianten = varianten.filterNot { vorlage ->
        vorlage.standardParameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS
    }''',
)

iter_def = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/IterierteOperatorDefinitionsKarten.kt"
ersetze(
    iter_def,
    "import de.TeutonStudio.MathematikKnoten.*\n",
    """import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
""",
)
ersetze(
    iter_def,
    '''private fun iterierteKonfiguration(vorlage: KnotenVorlage): IterierteDefinitionsKonfiguration = when (vorlage.art) {
    "mathematik.iterierteSumme" -> IterierteDefinitionsKonfiguration(''',
    '''private fun iterierteKonfiguration(vorlage: KnotenVorlage): IterierteDefinitionsKonfiguration {
    val kennung: Any? = if (vorlage.art == ZAHLENRECHNER_ART) {
        vorlage.standardParameter[ZAHLENRECHNER_OPERATOR]
    } else {
        vorlage.art
    }
    return when (kennung) {
    "mathematik.iterierteSumme", UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId -> IterierteDefinitionsKonfiguration(''',
)
ersetze(
    iter_def,
    '''    "mathematik.iteriertesProdukt" -> IterierteDefinitionsKonfiguration(''',
    '''    "mathematik.iteriertesProdukt", UniversellerZahlenOperator.ITERIERTES_PRODUKT.stabileId -> IterierteDefinitionsKonfiguration(''',
)
ersetze(
    iter_def,
    '''    else -> error("Für ${vorlage.art} existiert keine iterierte Definitionskonfiguration.")
}

private fun iteriertesKartesischesProduktDefinitionsKarte(''',
    '''    else -> error("Für ${vorlage.art} existiert keine iterierte Definitionskonfiguration.")
    }
}

private fun iteriertesKartesischesProduktDefinitionsKarte(''',
)

iter_test = "app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/IterierteOperatorKonzeptReiterTest.kt"
ersetze(
    iter_test,
    "import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen\n",
    """import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
""",
)
ersetze(
    iter_test,
    '''    private val variantenAnzahl = mapOf(
        MathematikKnotenVorlagen.IterierteSumme.art to 1,
        MathematikKnotenVorlagen.IteriertesProdukt.art to 1,
        MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART to 3,
        MathematikKnotenVorlagen.IterierteVereinigung.art to 1,
        MathematikKnotenVorlagen.IterierterSchnitt.art to 1,
        MathematikKnotenVorlagen.IteriertesKartesischesProdukt.art to 1,
    )''',
    '''    private val variantenAnzahl = mapOf(
        (ZAHLENRECHNER_ART to UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId) to 1,
        (ZAHLENRECHNER_ART to UniversellerZahlenOperator.ITERIERTES_PRODUKT.stabileId) to 1,
        (MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART to null) to 3,
        (MathematikKnotenVorlagen.IterierteVereinigung.art to null) to 1,
        (MathematikKnotenVorlagen.IterierterSchnitt.art to null) to 1,
        (MathematikKnotenVorlagen.IteriertesKartesischesProdukt.art to null) to 1,
    )

    private fun schlüssel(konzept: KonzeptDefinition) =
        konzept.knotenArten.single() to konzept.knotenParameter[ZAHLENRECHNER_OPERATOR]''',
)
ersetze(
    iter_test,
    '''        val konzepte = TestDefinitionsKarten.alle.filter { konzept ->
            konzept.knotenArten.singleOrNull() in variantenAnzahl.keys
        }''',
    '''        val konzepte = TestDefinitionsKarten.alle.filter { konzept ->
            konzept.knotenArten.size == 1 && schlüssel(konzept) in variantenAnzahl.keys
        }''',
    anzahl=2,
)
ersetze(
    iter_test,
    '''            val art = konzept.knotenArten.single()
            val erwarteteVarianten = variantenAnzahl.getValue(art)''',
    '''            val erwarteteVarianten = variantenAnzahl.getValue(schlüssel(konzept))''',
)
ersetze(
    iter_test,
    '''            val erklärteArt = konzept.knotenArten.single()
            konzept.reiter.drop(1).filter { reiter ->''',
    '''            konzept.reiter.drop(1).filter { reiter ->''',
)
ersetze(
    iter_test,
    '''                assertFalse(karte.knoten.any { it.art == erklärteArt }, reiter.titel)''',
    '''                assertFalse(karte.knoten.any(konzept::erklärt), reiter.titel)''',
)

katalog_test = "app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/KonzeptKatalogV2311Test.kt"
ersetze(
    katalog_test,
    "import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen\n",
    """import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_KOMPLEX_TUPEL
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_KOMPLEX_EINGABE
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
""",
)
ersetze(
    katalog_test,
    '''        val vorlagen = MathematikKnotenVorlagen.alle + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle''',
    '''        val vorlagen = (alleMathematikKnotenVorlagen() + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle)
            .distinctBy { it.art to it.name }''',
)
ersetze(
    katalog_test,
    '''                    assertFalse(karte.knoten.any { it.art in konzept.knotenArten }, "${konzept.id}/${reiter.id}/${karte.id}")''',
    '''                    assertFalse(karte.knoten.any(konzept::erklärt), "${konzept.id}/${reiter.id}/${karte.id}")''',
)
ersetze(
    katalog_test,
    '''        val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt.Zero)))''',
    '''        val division = alleMathematikKnotenVorlagen().single {
            it.art == ZAHLENRECHNER_ART &&
                it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.DIVISION.stabileId
        }
        val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(division.erzeuge(GraphPunkt.Zero)))''',
)
ersetze(
    katalog_test,
    '''        assertTrue(definition.knoten.any { it.art == MathematikKnotenVorlagen.Kehrwert.art })
        assertFalse(definition.knoten.any { it.art == MathematikKnotenVorlagen.Division.art })''',
    '''        assertTrue(definition.knoten.any {
            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.KEHRWERT.stabileId
        })
        assertFalse(definition.knoten.any {
            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.DIVISION.stabileId
        })''',
)
ersetze(
    katalog_test,
    '''            assertTrue(reiter.karteFür(KomplexDarstellung.Polar).knoten.any { it.art == MathematikKnotenVorlagen.KomplexAusTupel.art && it.parameter["modus"] == "polar" })''',
    '''            assertTrue(reiter.karteFür(KomplexDarstellung.Polar).knoten.any {
                it.art == ZAHLENRECHNER_ART &&
                    it.parameter[ZAHLENRECHNER_OPERATOR] ==
                    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR.stabileId &&
                    it.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE] == ZAHLENRECHNER_KOMPLEX_TUPEL
            })''',
)

spur_test = "app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/SpurDefinitionsKarteTest.kt"
ersetze(
    spur_test,
    "import de.TeutonStudio.MathematikKnoten.SPUR_ART\n",
    """import de.TeutonStudio.MathematikKnoten.SPUR_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
""",
)
ersetze(
    spur_test,
    '''            it.art == MathematikKnotenVorlagen.IterierteSumme.art &&
                it.parameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS''',
    '''            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId &&
                it.parameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS''',
)
