package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MethodenAnforderungTest {
    @Test
    fun `ergebnisanforderung unterscheidet konkrete methoden semantisch`() {
        val x = Variable("x")
        val zahlMethode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val mengenMethode = Methode(
            name = "A",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to MengenParameter("A_x")),
            zielMengen = mapOf("wert" to BenannteMenge("M", "\\mathfrak{M}")),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(zahlMethode))
        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.menge").prüfe(mengenMethode))
        assertNotNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(mengenMethode))
    }
}
