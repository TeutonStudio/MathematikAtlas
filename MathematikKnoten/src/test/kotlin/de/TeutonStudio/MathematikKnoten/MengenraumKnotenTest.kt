package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Abbildungsmenge
import de.TeutonStudio.MathematikRechenSystem.kern.Matrizenraum
import de.TeutonStudio.MathematikRechenSystem.kern.ModuloZahlenraum
import de.TeutonStudio.MathematikRechenSystem.kern.Primzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorraum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MengenraumKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `alle Mengenraumknoten besitzen Menge Ausgänge und registrierte Auswerter`() {
        MengenraumKnotenVorlagen.alle.forEach { vorlage ->
            val knoten = vorlage.erzeuge(GraphPunkt.Zero)
            assertTrue(knoten.anschlüsse.any { it.name == "menge" && it.richtung == AnschlussRichtung.Ausgang })
            assertNotNull(register.finde(knoten.art), "Auswerter für ${knoten.art} fehlt.")
        }
    }

    @Test
    fun `Abbildungsmenge folgt der Konvention A hoch B gleich Abbildungen B nach A`() {
        val knoten = MengenraumKnotenVorlagen.Abbildungsmenge.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "zielmenge" to BedingterWert(Primzahlen),
                    "definitionsmenge" to BedingterWert(ReelleZahlen),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            Abbildungsmenge(zielMenge = Primzahlen, definitionsMenge = ReelleZahlen),
            ergebnis.ausgaben.getValue("menge").objekt,
        )
    }

    @Test
    fun `Vektor Matrix und Tensorraum übernehmen ihre Dimensionen`() {
        val grundmenge = mapOf("grundmenge" to BedingterWert(ReelleZahlen))

        val vektorKnoten = MengenraumKnotenVorlagen.Vektorraum.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("dimension" to "4"),
        )
        val vektor = register.finde(vektorKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(vektorKnoten, grundmenge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        assertEquals(Tensorraum(ReelleZahlen, listOf(4)), vektor)

        val matrixKnoten = MengenraumKnotenVorlagen.Matrizenraum.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("zeilen" to "2", "spalten" to "5"),
        )
        val matrix = register.finde(matrixKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(matrixKnoten, grundmenge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        assertEquals(Matrizenraum(2, 5, ReelleZahlen), matrix)

        val tensorKnoten = MengenraumKnotenVorlagen.Tensorraum.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("dimensionen" to "2,3,4"),
        )
        val tensor = register.finde(tensorKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(tensorKnoten, grundmenge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        assertEquals(Tensorraum(ReelleZahlen, listOf(2, 3, 4)), tensor)
    }

    @Test
    fun `Modulo Zahlenraum validiert den Modul`() {
        val gültig = MengenraumKnotenVorlagen.ModuloZahlenraum.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("modul" to "7"),
        )
        val menge = register.finde(gültig.art)!!.auswerten(
            KnotenAuswertungsKontext(gültig, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        assertEquals(ModuloZahlenraum(7), menge)

        val ungültig = gültig.copy(parameter = mapOf("modul" to "1"))
        assertFailsWith<IllegalArgumentException> {
            register.finde(ungültig.art)!!.auswerten(
                KnotenAuswertungsKontext(ungültig, emptyMap(), RechenKontext()),
            )
        }
    }
}
