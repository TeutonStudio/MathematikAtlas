package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RandTopologischerRaumTest {
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde(RAND_KNOTEN_ART)!!

    @Test
    fun `neue Randvorlage verlangt einen topologischen Raum`() {
        val knoten = RandKnotenVorlagen.Rand.erzeuge(GraphPunkt.Zero)

        assertEquals(listOf("menge", "raum", "rand"), knoten.anschlüsse.map { it.name })
        assertTrue(knoten.parameter.isEmpty())

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("menge" to BedingterWert(EndlicheMenge(setOf(RationaleZahl.Eins)))),
                RechenKontext(),
            ),
        )
        assertNotNull(ergebnis.fehler)
        assertTrue(ergebnis.fehler!!.contains("topologische Raum"))
    }

    @Test
    fun `Rand verwendet die verbundene Topologie`() {
        val knoten = RandKnotenVorlagen.Rand.erzeuge(GraphPunkt.Zero)
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins))
        val raum = TopologischerRaum(ReelleZahlen, DiskreteTopologie(ReelleZahlen))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(menge),
                    "raum" to BedingterWert(raum),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(LeereMenge, ergebnis.ausgaben.getValue("rand").objekt)
    }

    @Test
    fun `alte explizite Randparameter bleiben migrationsfaehig`() {
        val knoten = RandKnotenVorlagen.Rand.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("topologie" to "kanonisch", "umgebungsraum" to "R", "relativ" to "false"),
        )
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("menge" to BedingterWert(menge)),
                RechenKontext(),
            ),
        )

        assertEquals(menge, ergebnis.ausgaben.getValue("rand").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("Historische Randparameter") })
    }
}
