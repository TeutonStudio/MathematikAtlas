package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardwerteUndKonjugationTest {
    private val auswerter = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister())

    @Test
    fun `Multiplikation startet mit zwei erweiterbaren Eingängen`() {
        val knoten = MathematikKnotenVorlagen.Multiplikation.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(listOf("a", "b"), eingänge.sortedBy { it.reihenfolge }.map { it.name })
        assertTrue(eingänge.all { it.kannSichErweitern })
        assertEquals("2", knoten.parameter["festeEingänge"])
    }

    @Test
    fun `Potenz verwendet einen unverbundenen Standardexponenten`() {
        val basis = zahl("basis", "2")
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("potenz"),
            parameter = mapOf("standardwert.exponent" to "-1"),
        )
        val karte = KartenDaten(
            name = "Kehrwert",
            knoten = listOf(basis, potenz),
            verbindungen = listOf(verbinde(basis, "wert", potenz, "basis")),
        )

        val ergebnis = auswerter.auswerten(karte)
        val wert = ergebnis.knoten.getValue(potenz.id).ausgaben.getValue("wert").objekt

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(Potenz(RationaleZahl.von(2), RationaleZahl.von(-1)), wert)
    }

    @Test
    fun `Verbindung überschreibt den Standardwert`() {
        val basis = zahl("basis", "2")
        val exponent = zahl("exponent", "3")
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("potenz"),
            parameter = mapOf("standardwert.exponent" to "-1"),
        )
        val karte = KartenDaten(
            name = "Potenz",
            knoten = listOf(basis, exponent, potenz),
            verbindungen = listOf(
                verbinde(basis, "wert", potenz, "basis"),
                verbinde(exponent, "wert", potenz, "exponent"),
            ),
        )

        val ergebnis = auswerter.auswerten(karte)
        val wert = ergebnis.knoten.getValue(potenz.id).ausgaben.getValue("wert").objekt

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(Potenz(RationaleZahl.von(2), RationaleZahl.von(3)), wert)
    }

    @Test
    fun `Ungültiger Standardwert wird als Knotenfehler gemeldet`() {
        val basis = zahl("basis", "2")
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("potenz"),
            parameter = mapOf("standardwert.exponent" to "keine Zahl"),
        )
        val karte = KartenDaten(
            name = "Fehler",
            knoten = listOf(basis, potenz),
            verbindungen = listOf(verbinde(basis, "wert", potenz, "basis")),
        )

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.single().contains("Standardwert für 'exponent'"))
    }

    @Test
    fun `Reelle Zahl wird beim Konjugieren auf sich selbst abgebildet`() {
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("x"),
            parameter = mapOf("name" to "x", "werteVorrat" to "R"),
        )
        val konjugierte = MathematikKnotenVorlagen.Konjugierte.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("konjugierte"))
        val karte = KartenDaten(
            name = "Reelle Konjugation",
            knoten = listOf(variable, konjugierte),
            verbindungen = listOf(verbinde(variable, "wert", konjugierte, "zahl")),
        )

        val ergebnis = auswerter.auswerten(karte)
        val wert = ergebnis.knoten.getValue(konjugierte.id).ausgaben.getValue("wert")

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(Variable("x"), wert.objekt)
        assertEquals(ReelleZahlen, wert.werteVorrat)
    }

    private fun zahl(id: String, wert: String) = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(
        id = KnotenId(id),
        parameter = mapOf("wert" to wert),
    )

    private fun verbinde(von: KnotenDaten, ausgang: String, zu: KnotenDaten, eingang: String) = VerbindungDaten(
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == ausgang }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == eingang }.id),
    )
}
