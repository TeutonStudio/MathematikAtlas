package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import kotlin.test.*

class SubtraktionKnotenTest {
    private val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())

    @Test fun `Subtraktion berechnet die Differenz und erhält ihre Darstellung`() {
        val sieben = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("sieben"),
            parameter = mapOf("wert" to "7"),
        )
        val drei = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(0f, 150f)).copy(
            id = KnotenId("drei"),
            parameter = mapOf("wert" to "3"),
        )
        val minus = ErweiterteMathematikKnotenVorlagen.Subtraktion.erzeuge(GraphPunkt(320f, 70f)).copy(id = KnotenId("minus"))
        fun ref(knoten: KnotenDaten, name: String, richtung: AnschlussRichtung) = AnschlussVerweis(
            knoten.id,
            knoten.anschlüsse.single { it.name == name && it.richtung == richtung }.id,
        )
        val karte = KartenDaten(
            name = "Subtraktion",
            knoten = listOf(sieben, drei, minus),
            verbindungen = listOf(
                VerbindungDaten(von = ref(sieben, "wert", AnschlussRichtung.Ausgang), zu = ref(minus, "minuend", AnschlussRichtung.Eingang)),
                VerbindungDaten(von = ref(drei, "wert", AnschlussRichtung.Ausgang), zu = ref(minus, "subtrahend", AnschlussRichtung.Eingang)),
            ),
        )

        val ergebnis = auswerter.auswerten(karte)
        val wert = ergebnis.knoten.getValue(minus.id).ausgaben.getValue("wert")

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(RationaleZahl.von(4), wert.objekt)
        assertEquals("7 - 3", wert.latexDarstellung)
    }
}
