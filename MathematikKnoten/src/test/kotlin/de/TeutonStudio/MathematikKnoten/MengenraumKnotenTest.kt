package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MengenraumKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun dimensionen(vararg werte: Long) = werte.map(RationaleZahl::von)

    @Test
    fun `alle Mengenraumknoten besitzen Menge Ausgänge und registrierte Auswerter`() {
        MengenraumKnotenVorlagen.alle.forEach { vorlage ->
            val knoten = vorlage.erzeuge(GraphPunkt.Zero)
            assertTrue(knoten.anschlüsse.any { it.name == "menge" && it.richtung == AnschlussRichtung.Ausgang })
            assertNotNull(register.finde(knoten.art), "Auswerter für ${knoten.art} fehlt.")
        }
    }

    @Test
    fun `Leere Menge Knoten gibt die kanonische leere Menge aus`() {
        val knoten = MengenraumKnotenVorlagen.LeereMenge.erzeuge(GraphPunkt.Zero)
        val menge = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("menge").objekt

        assertEquals(LeereMenge, menge)
        assertEquals("\\varnothing", menge.zuLatex())
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
        assertEquals(Tensorraum(ReelleZahlen, dimensionen(4)), vektor)

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
        assertEquals(Tensorraum(ReelleZahlen, dimensionen(2, 3, 4)), tensor)
    }

    @Test
    fun `Tensorraum erhält symbolische natürliche Dimensionen und macht Positivität sichtbar`() {
        val n = Variable("n")
        val m = Variable("m")
        val dimensionen = BedingterWert(
            objekt = Tupel(listOf(n, RationaleZahl.von(3), m)),
            variablenQuellen = listOf(
                VariablenQuelle(KnotenId("n-quelle"), "n", NatürlicheZahlen),
                VariablenQuelle(KnotenId("m-quelle"), "m", NatürlicheZahlen),
            ),
        )
        val knoten = MengenraumKnotenVorlagen.Tensorraum.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "grundmenge" to BedingterWert(ReelleZahlen),
                    "dimensionen" to dimensionen,
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("menge")

        assertEquals(
            Tensorraum(ReelleZahlen, listOf(n, RationaleZahl.von(3), m)),
            ergebnis.objekt,
        )
        assertTrue(Vergleich(n, VergleichsArt.Größer, RationaleZahl.Null) in ergebnis.annahmen)
        assertTrue(Vergleich(m, VergleichsArt.Größer, RationaleZahl.Null) in ergebnis.annahmen)
        assertEquals("\\mathbb{R}^{n\\times3\\timesm}", ergebnis.objekt.zuLatex())
    }

    @Test
    fun `Tensorraum lehnt symbolische Dimension ohne natürlichen Vertrag ab`() {
        val n = Variable("n")
        val knoten = MengenraumKnotenVorlagen.Tensorraum.erzeuge(GraphPunkt.Zero)
        assertFailsWith<IllegalArgumentException> {
            register.finde(knoten.art)!!.auswerten(
                KnotenAuswertungsKontext(
                    knoten,
                    mapOf(
                        "grundmenge" to BedingterWert(ReelleZahlen),
                        "dimensionen" to BedingterWert(
                            objekt = Tupel(listOf(n)),
                            variablenQuellen = listOf(
                                VariablenQuelle(KnotenId("n-quelle"), "n", ReelleZahlen),
                            ),
                        ),
                    ),
                    RechenKontext(),
                ),
            )
        }
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
