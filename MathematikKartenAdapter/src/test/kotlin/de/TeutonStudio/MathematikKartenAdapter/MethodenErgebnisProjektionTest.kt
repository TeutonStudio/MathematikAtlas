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

    @Test
    fun `kanonisches tupelwertiges Einzelergebnis behält die äussere Komponente`() {
        val inneresTupel = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val ergebnis = Tupel(listOf(inneresTupel))
        val innererRaum = Tupelraum(listOf(ReelleZahlen, ReelleZahlen))
        val ziel = Tupelraum(listOf(innererRaum))

        assertEquals(ergebnis to ziel, projiziereMethodenErgebnis(
            ergebnis, ziel, METHODEN_ERGEBNISPROJEKTION_TUPEL, kanonisch = true,
        ))
        assertEquals(inneresTupel to innererRaum, projiziereMethodenErgebnis(
            ergebnis, ziel, METHODEN_ERGEBNISPROJEKTION_DIREKT, kanonisch = true,
        ))
    }

    @Test
    fun `nullwertiges kanonisches Ergebnis bleibt auch direkt ein leeres Tupel`() {
        val ergebnis = Tupel(emptyList())
        val ziel = Tupelraum(emptyList())
        assertEquals(ergebnis to ziel, projiziereMethodenErgebnis(
            ergebnis, ziel, METHODEN_ERGEBNISPROJEKTION_DIREKT, kanonisch = true,
        ))
    }
}
