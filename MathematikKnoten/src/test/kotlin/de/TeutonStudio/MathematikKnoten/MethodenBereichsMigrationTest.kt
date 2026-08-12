package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MethodenBereichsMigrationTest {
    @Test
    fun `historische reine Restriktion bleibt Restriktion und verliert nur freien Ergaenzungshandle`() {
        val grund = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val frei = ergänzungsAnschluss("ergänzung.0", 2)
        val historisch = grund.copy(
            parameter = grund.parameter - METHODEN_BEREICHS_OPERATOR_PARAMETER,
            anschlüsse = grund.anschlüsse + frei,
        )
        val karte = KartenDaten(name = "Alt", knoten = listOf(historisch))

        val migriert = karte.migriereMethodenBereichsOperatoren()
        val knoten = migriert.knoten.single()

        assertEquals(METHODEN_BEREICHS_OPERATOR_RESTRIKTION, knoten.methodenBereichsOperator())
        assertTrue(knoten.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
        assertEquals(migriert, migriert.migriereMethodenBereichsOperatoren())
    }

    @Test
    fun `historische Restriktion mit verbundener Ergaenzung wird verlustfrei Bereichsanpassung`() {
        val grund = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val ergänzung = ergänzungsAnschluss("ergänzung.7", 2)
        val historisch = grund.copy(
            parameter = grund.parameter - METHODEN_BEREICHS_OPERATOR_PARAMETER,
            anschlüsse = grund.anschlüsse + ergänzung,
        )
        val quelle = KnotenDaten(
            art = "test.methode",
            name = "Quelle",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Methode.id,
                ),
            ),
        )
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(historisch.id, ergänzung.id),
        )
        val karte = KartenDaten(
            name = "Alt",
            knoten = listOf(quelle, historisch),
            verbindungen = listOf(verbindung),
        )

        val migriert = karte.migriereMethodenBereichsOperatoren()
        val knoten = migriert.knoten.first { it.id == historisch.id }
        val migrierteErgänzung = knoten.anschlüsse.single { it.id == ergänzung.id }

        assertEquals(METHODEN_BEREICHS_OPERATOR_ANPASSUNG, knoten.methodenBereichsOperator())
        assertEquals(ergänzung.id, migrierteErgänzung.id)
        assertEquals(ergänzung.reihenfolge, migrierteErgänzung.reihenfolge)
        assertEquals(listOf(verbindung), migriert.verbindungen)
        assertEquals(migriert, migriert.migriereMethodenBereichsOperatoren())
    }

    private fun ergänzungsAnschluss(name: String, reihenfolge: Int) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = reihenfolge,
        dynamischErzeugt = true,
    )
}
