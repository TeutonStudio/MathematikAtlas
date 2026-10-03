package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.ZahlenBereich
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrbitKnotenInspektorTest {
    @Test
    fun `Bereichseingabe verwirft einen endlichen Differenzueberlauf`() {
        assertEquals(null, parseZahlenBereich("-${Double.MAX_VALUE}, ${Double.MAX_VALUE}"))
        assertEquals(null, parseZahlenBereich("0, Infinity"))
        assertEquals(null, parseZahlenBereich("2, 1"))
        assertEquals(ZahlenBereich(-2.0, 3.0), parseZahlenBereich("-2, 3"))
    }

    @Test
    fun `veraltete Zustandsauswahl bleibt über verbundene Methode reparierbar`() {
        val z = Variable("z")
        val c = Variable("c")
        val methode = Methode(
            name = "f",
            parameter = listOf(z, c),
            vorschrift = addition(z, c),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to ReelleZahlen),
        )
        val knoten = MathematikKnotenVorlagen.Orbit.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("zustandsArgument" to "alt"),
        )
        val fehlgeschlagen = KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            fehler = "Das gewählte Zustandsargument gehört nicht mehr zur Methode.",
            eingänge = mapOf(
                "schritt" to BedingterWert(methode),
                "start" to BedingterWert(RationaleZahl.Null),
            ),
        )

        val auswahl = orbitArgumentAuswahl(knoten, fehlgeschlagen)

        assertEquals(listOf("z", "c"), auswahl.parameter.map { it.name })
        assertEquals("alt", auswahl.gewählt)
        assertTrue(auswahl.veraltet)
    }
}
