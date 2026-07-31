package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EntscheidungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Ungleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.Wahrheitswert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class UngleichheitKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Vorlage entspricht dem Gleichheitsknoten mit eigenem stabilen Typ`() {
        val knoten = MathematikKnotenVorlagen.Ungleichheit.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("mathematik.ungleichheit", knoten.art)
        assertEquals("Ungleichheit", knoten.name)
        assertEquals(listOf("links", "rechts"), eingänge.map { it.name })
        assertEquals(listOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Objekt.id), eingänge.map { it.art })
        assertEquals("aussage", ausgang.name)
        assertEquals(MathematikAnschlussArten.Aussage.id, ausgang.art)
        assertNotNull(register.finde(knoten.art))
        assertEquals(1, MathematikKnotenVorlagen.alle.count { it.art == knoten.art })
    }

    @Test
    fun `verschiedene rationale Zahlen ergeben eine wahre Ungleichheit`() {
        val aussage = werteAus("1", "2")
        val entscheidung = aussage.entscheide()

        assertEquals(Wahrheitswert.Wahr, entscheidung.wahrheitswert)
        assertEquals(EntscheidungsStatus.Bewiesen, entscheidung.status)
        assertEquals("1 \\neq 2", aussage.zuLatex())
    }

    @Test
    fun `gleiche rationale Zahlen widerlegen die Ungleichheit`() {
        val entscheidung = werteAus("2/4", "1/2").entscheide()

        assertEquals(Wahrheitswert.Lüge, entscheidung.wahrheitswert)
        assertEquals(EntscheidungsStatus.Widerlegt, entscheidung.status)
    }

    @Test
    fun `fehlende rechte Seite bleibt ein Knotenfehler`() {
        val knoten = MathematikKnotenVorlagen.Ungleichheit.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))

        val fehler = assertFailsWith<IllegalStateException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf("links" to BedingterWert(RationaleZahl.parse("1"))),
                    rechenKontext = RechenKontext(),
                ),
            )
        }

        assertEquals("Rechte Seite fehlt.", fehler.message)
    }

    private fun werteAus(links: String, rechts: String): Ungleichheit {
        val knoten = MathematikKnotenVorlagen.Ungleichheit.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "links" to BedingterWert(RationaleZahl.parse(links)),
                    "rechts" to BedingterWert(RationaleZahl.parse(rechts)),
                ),
                rechenKontext = RechenKontext(),
            ),
        )
        return assertIs(ergebnis.ausgaben.getValue("aussage").objekt)
    }
}
