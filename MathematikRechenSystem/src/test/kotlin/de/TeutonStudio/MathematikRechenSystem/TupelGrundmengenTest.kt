package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class TupelGrundmengenTest {
    @Test fun `gemeinsame Grundmenge ist Vereinigung der Komponenten`() {
        val raum = Tupelraum(listOf(ReelleZahlen, RationaleZahlen))
        assertEquals(vereinige(raum.komponenten), raum.gemeinsameGrundmenge())
        assertTrue(raum.potenzLatex().endsWith("^{2}"))
    }

    @Test fun `strukturierte Bereiche behalten Tupelform und Orientierung`() {
        val raum = Tupelraum(listOf(ReelleZahlen, ReelleZahlen))
        val a = RationaleZahl.Eins
        val b = RationaleZahl.von(2)
        assertEquals(raum, strukturRaumÜbersetzung(raum, Tupel(listOf(a,b))))
        val zeile = strukturRaumÜbersetzung(raum, ZeilenVektor(listOf(a,b))) as Vektorraum
        assertEquals(VektorOrientierung.Zeile, zeile.orientierung)
        assertEquals(2, zeile.dimension)
        assertEquals(ReelleZahlen, zeile.skalarMenge)
    }

    @Test fun `Abflachung bewahrt verschachtelte Komponentenbereiche`() {
        val raum = Tupelraum(listOf(ReelleZahlen, Tupelraum(listOf(RationaleZahlen, ReelleZahlen))))
        val tupel = Tupel(listOf(RationaleZahl.Eins, Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))))
        assertEquals(Tupelraum(listOf(ReelleZahlen, RationaleZahlen, ReelleZahlen)), tupelAbflachenRaum(tupel, raum))
    }
}
