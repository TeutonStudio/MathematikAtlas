package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.text.style.TextAlign
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotizKnotenTest {
    @Test fun `Vorlage ist anschlusslos und besitzt stabile Standardwerte`() {
        val vorlage = KartenWerkzeugVorlagen.Notiz
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)

        assertEquals(NOTIZ_KNOTEN_ART, vorlage.art)
        assertEquals("Darstellung", vorlage.kategorie)
        assertEquals(GraphGröße(280f, 160f), vorlage.standardGröße)
        assertTrue(knoten.anschlüsse.isEmpty())
        assertEquals("", knoten.parameter[NOTIZ_TEXT_PARAMETER])
        assertEquals("links", knoten.parameter[NOTIZ_AUSRICHTUNG_PARAMETER])
        assertEquals("16", knoten.parameter[NOTIZ_SCHRIFTGROESSE_PARAMETER])
    }

    @Test fun `Textausrichtung bildet alle vier Werte und Fallback ab`() {
        assertEquals(TextAlign.Start, notizTextAusrichtung("links"))
        assertEquals(TextAlign.End, notizTextAusrichtung("rechts"))
        assertEquals(TextAlign.Center, notizTextAusrichtung("zentriert"))
        assertEquals(TextAlign.Justify, notizTextAusrichtung("blocksatz"))
        assertEquals(TextAlign.Start, notizTextAusrichtung("unbekannt"))
        assertEquals(TextAlign.Start, notizTextAusrichtung(null))
    }

    @Test fun `Schriftgroesse akzeptiert nur den vereinbarten Bereich`() {
        assertEquals(8, notizSchriftgrößeSp("8"))
        assertEquals(42, notizSchriftgrößeSp("42"))
        assertEquals(96, notizSchriftgrößeSp("96"))
        assertEquals(16, notizSchriftgrößeSp("7"))
        assertEquals(16, notizSchriftgrößeSp("97"))
        assertEquals(16, notizSchriftgrößeSp("Unsinn"))
        assertEquals(16, notizSchriftgrößeSp(null))
    }

    @Test fun `Notiz roundtript Text Formatierung und gezogene Groesse`() {
        val text = "Erwartetes Ergebnis:\n∀ x ∈ ℝ: f′(x) = 3x² − 2"
        val notiz = KartenWerkzeugVorlagen.Notiz.erzeuge(GraphPunkt(40f, 80f)).copy(
            größe = GraphGröße(420f, 210f),
            parameter = KartenWerkzeugVorlagen.Notiz.standardParameter + mapOf(
                NOTIZ_TEXT_PARAMETER to text,
                NOTIZ_AUSRICHTUNG_PARAMETER to "blocksatz",
                NOTIZ_SCHRIFTGROESSE_PARAMETER to "24",
            ),
        )

        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Notiztest", knoten = listOf(notiz))),
        ).knoten.single()

        assertEquals(NOTIZ_KNOTEN_ART, gelesen.art)
        assertTrue(gelesen.anschlüsse.isEmpty())
        assertEquals(GraphGröße(420f, 210f), gelesen.größe)
        assertEquals(text, gelesen.parameter[NOTIZ_TEXT_PARAMETER])
        assertEquals("blocksatz", gelesen.parameter[NOTIZ_AUSRICHTUNG_PARAMETER])
        assertEquals("24", gelesen.parameter[NOTIZ_SCHRIFTGROESSE_PARAMETER])
    }
}
