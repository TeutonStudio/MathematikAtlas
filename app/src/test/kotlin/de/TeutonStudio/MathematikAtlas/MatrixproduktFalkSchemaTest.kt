package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MatrixproduktFalkSchemaTest {
    private fun z(wert: Long) = RationaleZahl.von(wert)

    @Test
    fun `fehlende Auswertung verwendet gekennzeichnetes Standardbeispiel`() {
        val daten = matrixproduktFalkDaten(null)

        assertEquals(FalkDatenQuelle.Standardbeispiel, daten.quelle)
        assertEquals(2, daten.links.zeilenAnzahl)
        assertEquals(3, daten.links.spaltenAnzahl)
        assertEquals(3, daten.rechts.zeilenAnzahl)
        assertEquals(2, daten.rechts.spaltenAnzahl)
    }

    @Test
    fun `ausgewertete Matrizen werden unverändert übernommen`() {
        val links = Matrix(listOf(listOf(z(2), z(3))))
        val rechts = Matrix(listOf(listOf(z(5)), listOf(z(7))))

        val daten = matrixproduktFalkDaten(auswertung(links, rechts))
        val schema = assertIs<DetailliertesFalkSchemaErgebnis.Gültig>(
            detailliertesFalkSchema(daten.links, daten.rechts),
        ).modell

        assertEquals(FalkDatenQuelle.Knotendaten, daten.quelle)
        assertEquals("2\\cdot 5 + 3\\cdot 7", schema.summenLatex())
        assertEquals("31", schema.ergebnisEintrag.zuLatex())
    }

    @Test
    fun `orientierte Vektoren verwenden die kanonische Matrixansicht`() {
        val links = ZeilenVektor(listOf(z(1), z(2), z(3)))
        val rechts = SpaltenVektor(listOf(z(4), z(5), z(6)))

        val daten = matrixproduktFalkDaten(auswertung(links, rechts))

        assertEquals(FalkDatenQuelle.Knotendaten, daten.quelle)
        assertEquals(1, daten.links.zeilenAnzahl)
        assertEquals(3, daten.links.spaltenAnzahl)
        assertEquals(3, daten.rechts.zeilenAnzahl)
        assertEquals(1, daten.rechts.spaltenAnzahl)
    }

    @Test
    fun `inkompatible echte Eingaben werden nicht durch das Beispiel ersetzt`() {
        val links = Matrix(listOf(listOf(z(1), z(2))))
        val rechts = Matrix(listOf(listOf(z(3), z(4))))

        val daten = matrixproduktFalkDaten(auswertung(links, rechts))
        val schema = detailliertesFalkSchema(daten.links, daten.rechts)

        assertEquals(FalkDatenQuelle.Knotendaten, daten.quelle)
        assertIs<DetailliertesFalkSchemaErgebnis.Inkompatibel>(schema)
    }

    @Test
    fun `große Achse begrenzt Einträge und hält die Auswahl sichtbar`() {
        val projektion = projiziereFalkAchse(30, ausgewählt = 17)
        val indices = projektion.filterIsInstance<FalkAchsenElement.Index>().map { it.wert }

        assertTrue(indices.size <= 8)
        assertTrue(17 in indices)
        assertTrue(0 in indices)
        assertTrue(29 in indices)
        assertTrue(projektion.any { it is FalkAchsenElement.Auslassung })
    }

    @Test
    fun `lange Produktsumme bleibt geordnet und wird visuell begrenzt`() {
        val links = Matrix(listOf(List(12) { z((it + 1).toLong()) }))
        val rechts = Matrix(List(12) { listOf(z((it + 21).toLong())) })
        val modell = assertIs<DetailliertesFalkSchemaErgebnis.Gültig>(
            detailliertesFalkSchema(links, rechts),
        ).modell

        val latex = gekürzteFalkSummeLatex(modell)

        assertTrue(latex.startsWith("1\\cdot 21 + 2\\cdot 22"))
        assertTrue("\\cdots" in latex)
        assertTrue(latex.endsWith("11\\cdot 31 + 12\\cdot 32"))
    }

    private fun auswertung(
        links: MathematischesObjekt,
        rechts: MathematischesObjekt,
    ) = KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        eingänge = mapOf("a" to BedingterWert(links), "b" to BedingterWert(rechts)),
    )
}
