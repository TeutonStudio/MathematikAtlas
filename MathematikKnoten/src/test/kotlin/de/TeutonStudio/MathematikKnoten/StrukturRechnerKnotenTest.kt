package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturRechnerKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Neue Strukturknoten sind im Katalog und Auswerter registriert`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()
        val erwartet = setOf(
            SKALARPRODUKT_ART,
            TENSORPRODUKT_ART,
            DIMENSIONEN_ART,
            TensorRechner.KNOTEN_ART,
            AussagenSatzRechner.KNOTEN_ART,
            CAUCHY_ART,
        )

        assertTrue(erwartet.all(arten::contains))
        erwartet.forEach { assertNotNull(register.finde(it), "Auswerter für $it fehlt") }
    }

    @Test
    fun `Skalarproduktknoten akzeptiert Zeile und Spalte gemeinsam`() {
        val knoten = StrukturRechnerKnotenVorlagen.Skalarprodukt.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(SKALARPRODUKT_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "links" to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2)))),
                    "rechts" to BedingterWert(SpaltenVektor(listOf(zahl(3), zahl(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(zahl(11), ergebnis.ausgaben.getValue("wert").objekt)
        assertNull(ergebnis.fehler)
    }

    @Test
    fun `Skalarprodukt meldet ungleiche Tupellaengen strukturiert`() {
        val knoten = StrukturRechnerKnotenVorlagen.Skalarprodukt.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(SKALARPRODUKT_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "links" to BedingterWert(Tupel(listOf(zahl(1), zahl(2)))),
                    "rechts" to BedingterWert(Tupel(listOf(zahl(3)))),
                ),
                RechenKontext(),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertContains(ergebnis.fehler.orEmpty(), "Länge")
    }

    @Test
    fun `Dimensionenknoten gibt Form und Stufe getrennt aus`() {
        val knoten = StrukturRechnerKnotenVorlagen.Dimensionen.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
            ),
        )
        val ergebnis = register.finde(DIMENSIONEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("objekt" to BedingterWert(matrix)),
                RechenKontext(),
            ),
        )

        assertEquals(Tupel(listOf(zahl(2), zahl(3))), ergebnis.ausgaben.getValue("dimensionen").objekt)
        assertEquals(zahl(2), ergebnis.ausgaben.getValue("stufe").objekt)
    }

    @Test
    fun `Tensorrechner passt Anschluesse atomar an den Operator an`() {
        val basis = StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero)
        val basisAusgang = basis.anschlüsse.first { it.name == "wert" }

        val skalar = konfiguriereTensorRechner(basis, TensorRechnerOperator.SKALARMULTIPLIKATION)
        assertEquals(listOf("skalar", "tensor", "wert"), skalar.anschlüsse.map { it.name })
        assertEquals(MathematikAnschlussArten.Zahl.id, skalar.anschlüsse.first { it.name == "skalar" }.art)
        assertEquals(basisAusgang.id, skalar.anschlüsse.first { it.name == "wert" }.id)

        val norm = konfiguriereTensorRechner(skalar, TensorRechnerOperator.NORM)
        val normAusgang = norm.anschlüsse.first { it.name == "wert" }
        assertEquals(listOf("tensor", "wert"), norm.anschlüsse.map { it.name })
        assertEquals(MathematikAnschlussArten.Zahl.id, normAusgang.art)
        assertNotEquals(basisAusgang.id, normAusgang.id)
        assertEquals(TensorRechnerOperator.NORM.stabileId, norm.parameter[RECHNER_OPERATOR_PARAMETER])

        val tensorprodukt = konfiguriereTensorRechner(norm, TensorRechnerOperator.TENSORPRODUKT)
        assertNotEquals(normAusgang.id, tensorprodukt.anschlüsse.first { it.name == "wert" }.id)
    }

    @Test
    fun `Tensorrechner gibt unvollstaendige Konfiguration als Knotenfehler aus`() {
        val knoten = konfiguriereTensorRechner(
            StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),
            TensorRechnerOperator.KONTRAKTION,
        )
        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertNotNull(ergebnis.fehler)
    }

    @Test
    fun `Matrixzeilen akzeptieren kartesische Tupel ueber gemeinsame Komponentenansicht`() {
        val matrix = konfiguriereMatrix(
            MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            MATRIX_ZEILEN,
            höhe = 2,
            breite = 2,
        )
        val ergebnis = register.finde("mathematik.matrix")!!.auswerten(
            KnotenAuswertungsKontext(
                matrix,
                mapOf(
                    matrixZeileName(0) to BedingterWert(Tupel(listOf(zahl(1), zahl(2)))),
                    matrixZeileName(1) to BedingterWert(Tupel(listOf(zahl(3), zahl(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            listOf(listOf(zahl(1), zahl(2)), listOf(zahl(3), zahl(4))),
            assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen,
        )
    }

    @Test
    fun `Matrixzeile weist Spaltenvektor mit konkretem Fehler zurueck`() {
        val matrix = konfiguriereMatrix(
            MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            MATRIX_ZEILEN,
            höhe = 1,
            breite = 2,
        )
        val ergebnis = register.finde("mathematik.matrix")!!.auswerten(
            KnotenAuswertungsKontext(
                matrix,
                mapOf(matrixZeileName(0) to BedingterWert(SpaltenVektor(listOf(zahl(1), zahl(2))))),
                RechenKontext(),
            ),
        )

        assertContains(ergebnis.fehler.orEmpty(), "kein Spaltenvektor")
    }

    @Test
    fun `Tensorrechner Norm verwendet dynamischen Tensoreingang`() {
        val knoten = konfiguriereTensorRechner(
            StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),
            TensorRechnerOperator.NORM,
        )
        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("tensor" to BedingterWert(SpaltenVektor(listOf(zahl(3), zahl(4))))),
                RechenKontext(),
            ),
        )

        assertNull(ergebnis.fehler)
        assertFalse(ergebnis.ausgaben.isEmpty())
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
