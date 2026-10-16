package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MathematikKnotenRenderer
import de.TeutonStudio.MathematikKnoten.mitAuswertungszeit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Prüft den gemeinsamen Editor auch unter Android mit echter Touch-Eingabe. */
class KnotenAnhaengeGeraeteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun meldungenUndZeitHaengenAussenUndEntfernenSichOhneGroessenaenderung() {
        val knoten = KnotenDaten(art = "test", name = "Körper", position = GraphPunkt(60f, 280f), größe = GraphGröße(280f, 120f))
        val z = KartenEditorZustand(KartenDaten(name = "Anhänge", knoten = listOf(knoten)), GraphPrüfung(AnschlussArtRegister(emptyList())))
        val ergebnis = mutableStateOf(KnotenAuswertungsErgebnis(emptyMap()))
        compose.setContent {
            MaterialTheme {
                val dichte = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(dichte, 1.5f)) {
                    KnotenKartenEditor(z, Modifier.fillMaxSize(), rendererFür = {
                        MathematikKnotenRenderer(ergebnisFür = { ergebnis.value }).mitAuswertungszeit { ergebnis.value }
                    }, zeigeKnotenInspektor = false)
                }
            }
        }
        val ohne = compose.onNodeWithContentDescription("Knoten Körper, 0 Anschlüsse").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            ergebnis.value = KnotenAuswertungsErgebnis(emptyMap(), fehler = "Die Eingabe ist ungültig und muss vor der Auswertung korrigiert werden.",
                warnungen = listOf("Erster Hinweis", "Zweiter Hinweis"), auswertungsDauerNanos = 1_200_000L)
        }
        compose.onNodeWithText("Erster Hinweis").assertIsDisplayed()
        compose.onNodeWithText("Zweiter Hinweis").assertIsDisplayed()
        val körper = compose.onNodeWithContentDescription("Knoten Körper, 0 Anschlüsse").fetchSemanticsNode().boundsInRoot
        assertEquals(ohne, körper)
        val fehler = compose.onNodeWithText("Die Eingabe ist ungültig und muss vor der Auswertung korrigiert werden.").fetchSemanticsNode().boundsInRoot
        assertTrue(fehler.bottom <= körper.top)
        val zeit = compose.onNode(hasContentDescription("Letzte Auswertungsdauer", substring = true))
        assertTrue(zeit.fetchSemanticsNode().boundsInRoot.top >= körper.bottom)
        compose.onNodeWithContentDescription("Knoten Körper, 0 Anschlüsse").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(knoten.id, z.ausgewählterKnoten) }
        zeit.performTouchInput { click() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertNull(z.ausgewählterKnoten); ergebnis.value = KnotenAuswertungsErgebnis(emptyMap()) }
        compose.onAllNodesWithText("Erster Hinweis").assertCountEquals(0)
        compose.onAllNodes(hasContentDescription("Letzte Auswertungsdauer", substring = true)).assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(knoten.größe, z.karte.knoten.single().größe)
            assertTrue(z.meldungenFür(knoten.id).isEmpty())
        }
    }
}
