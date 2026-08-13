package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KartenMethodenTupelTest {
    @Test
    fun `leere Karte wird als nullstellige nullwertige Tupelmethode ausgewertet`() {
        val intern = KartenDaten(name = "Leer")
        val verweis = KartenVerweis(intern.id, intern.version)
        val gruppenKnoten = KnotenDaten(
            art = "methode.${intern.id.wert}",
            name = "Leer",
            kartenVerweis = verweis,
        )
        val außen = KartenDaten(name = "Außen", knoten = listOf(gruppenKnoten))
        val auswerter = KartenAuswerter(
            register = MathematikAuswerterRegister(),
            kartenQuelle = KartenQuelle { angefragt -> if (angefragt == verweis) intern else null },
        )

        val ergebnis = auswerter.auswerten(außen)

        assertTrue(ergebnis.fehler.isEmpty())
        val methode = assertIs<MathematischeMethode>(
            ergebnis.knoten.getValue(gruppenKnoten.id).ausgaben.getValue("methode").objekt,
        )
        assertEquals(0, methode.parameter.size)
        assertEquals(0, methode.ausgabeNamen.size)
        assertEquals(0, methode.ergebnisTupel.anzahl)
        assertEquals(Tupelraum(emptyList()), methode.mathematischeSignatur.definitionsRaum)
        assertEquals(Tupelraum(emptyList()), methode.mathematischeSignatur.zielRaum)
    }
}
