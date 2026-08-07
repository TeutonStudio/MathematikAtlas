package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.IntegralAusgabeform
import de.TeutonStudio.MathematikRechenSystem.kern.IntegralMethodenDarstellung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegralKartenJsonTest {
    @Test
    fun `Methodenmodus roundtrippt Darstellung Mass und Handles`() {
        val knoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero).copy(
            parameter = IntegralKnotenVorlagen.Integral.standardParameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.METHODE.name,
                INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER to IntegralMethodenDarstellung.VOLLSTAENDIG.name,
                INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.ALLGEMEIN.name,
                INTEGRAL_MASS_SYMBOL_PARAMETER to "\\nu",
            ),
        )
        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Integral", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(IntegralAusgabeform.METHODE.name, gelesen.parameter[INTEGRAL_AUSGABEFORM_PARAMETER])
        assertEquals(IntegralMethodenDarstellung.VOLLSTAENDIG.name, gelesen.parameter[INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER])
        assertEquals(IntegralMassModus.ALLGEMEIN.name, gelesen.parameter[INTEGRAL_MASS_MODUS_PARAMETER])
        assertEquals("\\nu", gelesen.parameter[INTEGRAL_MASS_SYMBOL_PARAMETER])
        assertEquals(knoten.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `Termmodus roundtrippt Quellen IDs und termbezogene Handles`() {
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_QUELLEN_IDS_PARAMETER to "quelle.x,quelle.y",
            ),
        )
        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Term", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(IntegralAusgabeform.TERM.name, gelesen.parameter[INTEGRAL_AUSGABEFORM_PARAMETER])
        assertEquals("quelle.x,quelle.y", gelesen.parameter[INTEGRAL_QUELLEN_IDS_PARAMETER])
        assertEquals(setOf("variable", "menge", "term", "mass", "wert"), gelesen.anschlüsse.map { it.name }.toSet())
        assertTrue(gelesen.anschlüsse.none { it.name == "methode" })
    }

    @Test
    fun `historische Methodenvariante wird beim Lesen migriert`() {
        val alt = KnotenDaten(
            art = "mathematik.integralMethode",
            name = "Alt",
            position = GraphPunkt.Zero,
            parameter = mapOf("kurz" to "true"),
        )
        val gelesen = KartenJson.lese(
            KartenDatenJson.schreibe(KartenDaten(name = "Alt", knoten = listOf(alt))),
        ).knoten.single()

        assertEquals(INTEGRAL_KNOTEN_ART, gelesen.art)
        assertEquals(IntegralAusgabeform.METHODE.name, gelesen.parameter[INTEGRAL_AUSGABEFORM_PARAMETER])
        assertEquals(IntegralMethodenDarstellung.KURZ.name, gelesen.parameter[INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER])
        assertTrue(gelesen.anschlüsse.any { it.name == "methode" })
    }
}
