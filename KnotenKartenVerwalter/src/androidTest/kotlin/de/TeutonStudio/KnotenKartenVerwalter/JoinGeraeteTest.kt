package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class JoinGeraeteTest {
    @get:Rule val compose = createComposeRule()
    @Test fun touchErgaenztJoinUndLoestNurDieGegriffeneLeitung() {
        val art = AnschlussArtId("test")
        val q = KnotenDaten(art = "test", name = "Quelle", position = GraphPunkt(25f, 90f), größe = GraphGröße(100f, 100f),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val join = KnotenDaten(art = "test.join", name = "Join", position = GraphPunkt(230f, 90f), größe = GraphGröße(120f, 140f),
            anschlüsse = listOf(AnschlussDaten(name = "operanden", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, belegung = AnschlussBelegung.Mehrfach)))
        val editor = KartenEditorZustand(KartenDaten(name = "Join", knoten = listOf(q, join)), GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))))
        compose.setContent { MaterialTheme { KnotenKartenEditor(editor, Modifier.fillMaxSize(), zeigeKnotenInspektor = false) } }
        repeat(2) {
            val quelle = compose.onNode(hasContentDescription("Quelle, Anschluss wert,", substring = true))
            val delta = compose.onNode(hasContentDescription("Join, Anschluss operanden,", substring = true))
                .fetchSemanticsNode().boundsInRoot.center - quelle.fetchSemanticsNode().boundsInRoot.center
            quelle.performTouchInput { swipe(center, center + delta, durationMillis = 300) }
        }
        compose.runOnIdle { assertEquals(2, editor.karte.verbindungen.size) }
        compose.onNode(hasContentDescription("Join, Anschluss operanden · 1,", substring = true)).performTouchInput {
            down(center); moveBy(Offset(-70f, -90f), delayMillis = 300); up()
        }
        compose.runOnIdle {
            assertEquals(1, editor.karte.verbindungen.size)
            editor.rückgängig()
            assertEquals(2, editor.karte.verbindungen.size)
        }
    }
}
