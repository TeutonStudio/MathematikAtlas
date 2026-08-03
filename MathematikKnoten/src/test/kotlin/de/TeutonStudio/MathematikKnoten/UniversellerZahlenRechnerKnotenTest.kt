package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class UniversellerZahlenRechnerKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun kontext(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingänge,
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `Katalog zeigt einen Zahlenrechner und interner Definitionskatalog alle Operatoren`() {
        val sichtbar = alleMathematikKnotenVorlagen()
        assertEquals(1, sichtbar.count { it.art == ZAHLENRECHNER_ART })
        assertTrue(UniversellerZahlenOperator.entries.all { operator ->
            ZahlenRechnerKnotenVorlagen.alle.any {
                it.standardParameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId
            }
        })
        assertTrue(sichtbar.none { it.art in historischeZahlenRechnerArten })
        assertNotNull(register.finde(ZAHLENRECHNER_ART))
    }

    @Test
    fun `Migration erhaelt Knoten Anschluss und Edge IDs`() {
        val alt = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val a = alt.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = alt.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }
        val quelle = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val quellAusgang = quelle.anschlüsse.single()
        val edge = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quellAusgang.id),
            zu = AnschlussVerweis(alt.id, a.id),
        )
        val karte = KartenDaten(
            name = "Migration",
            knoten = listOf(quelle, alt),
            verbindungen = listOf(edge),
        )

        val migriert = migriereHistorischeZahlenRechnerKnoten(karte)
        val neu = migriert.knoten.single { it.id == alt.id }

        assertEquals(ZAHLENRECHNER_ART, neu.art)
        assertEquals(UniversellerZahlenOperator.ADDITION.stabileId, neu.parameter[ZAHLENRECHNER_OPERATOR])
        assertTrue(neu.anschlüsse.any { it.id == a.id })
        assertTrue(neu.anschlüsse.any { it.id == ausgang.id })
        assertEquals(edge.id, migriert.verbindungen.single().id)
        assertEquals(edge.zu, migriert.verbindungen.single().zu)
    }

    @Test
    fun `Addition vereinigt kompatible Zahlbereiche`() {
        val knoten = ZahlenRechnerKnotenVorlagen.alle.first {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.ADDITION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "wert1" to BedingterWert(RationaleZahl.von(2)),
                    "wert2" to BedingterWert(RationaleZahl.von(3)),
                ),
            ),
        )
        assertEquals(RationaleZahl.von(5), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Subtraktion erweitert Ergebnisbereich wenn natuerliche Zahlen nicht abgeschlossen sind`() {
        val knoten = ZahlenRechnerKnotenVorlagen.alle.first {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.SUBTRAKTION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "wert1" to BedingterWert(RationaleZahl.von(2)),
                    "wert2" to BedingterWert(RationaleZahl.von(3)),
                ),
            ),
        )
        assertEquals(RationaleZahl.von(-1), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Division durch null liefert Definitionsluecke`() {
        val knoten = ZahlenRechnerKnotenVorlagen.alle.first {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.DIVISION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "zaehler" to BedingterWert(RationaleZahl.Eins),
                    "nenner" to BedingterWert(RationaleZahl.Null),
                ),
            ),
        )
        assertNotNull(ergebnis.fehler)
        assertTrue(ergebnis.ausgaben.isEmpty())
    }

    @Test
    fun `Operatorwechsel rekonstruiert Anschluesse deterministisch`() {
        val start = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val addition = konfiguriereZahlenRechnerKnoten(start, UniversellerZahlenOperator.ADDITION)
        val division = konfiguriereZahlenRechnerKnoten(addition, UniversellerZahlenOperator.DIVISION)
        val erneut = konfiguriereZahlenRechnerKnoten(division, UniversellerZahlenOperator.ADDITION)

        assertEquals(addition.anschlüsse.map { it.name }, erneut.anschlüsse.map { it.name })
        assertEquals(addition.anschlüsse.map { it.id }, erneut.anschlüsse.map { it.id })
    }
}