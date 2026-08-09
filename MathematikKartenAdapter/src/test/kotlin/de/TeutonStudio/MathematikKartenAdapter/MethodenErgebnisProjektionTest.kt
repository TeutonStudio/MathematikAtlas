package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MethodenErgebnisProjektionTest {
    @Test
    fun `skalare Ausgabe wird genau einmal zum Einertupel`() {
        val wert = RationaleZahl.von(3)

        val (projiziert, ziel) = projiziereMethodenErgebnis(
            wert = wert,
            zielMenge = ReelleZahlen,
            projektion = METHODEN_ERGEBNISPROJEKTION_TUPEL,
        )

        assertEquals(listOf(wert), assertIs<Tupel>(projiziert).elemente)
        assertEquals(listOf(ReelleZahlen), assertIs<Tupelraum>(ziel).komponenten)
    }

    @Test
    fun `bereits tupelige Ausgabe und Zielmenge bleiben flach`() {
        val wert = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val ziel = Tupelraum(listOf(ReelleZahlen, ReelleZahlen))

        val (projiziert, projiziertesZiel) = projiziereMethodenErgebnis(
            wert = wert,
            zielMenge = ziel,
            projektion = METHODEN_ERGEBNISPROJEKTION_TUPEL,
        )

        assertEquals(wert, projiziert)
        assertEquals(ziel, projiziertesZiel)
    }

    @Test
    fun `direkte Projektion verändert weder Wert noch Zielmenge`() {
        val wert = RationaleZahl.von(5)

        val (projiziert, ziel) = projiziereMethodenErgebnis(
            wert = wert,
            zielMenge = GanzeZahlen,
            projektion = METHODEN_ERGEBNISPROJEKTION_DIREKT,
        )

        assertEquals(wert, projiziert)
        assertEquals(GanzeZahlen, ziel)
    }
}
