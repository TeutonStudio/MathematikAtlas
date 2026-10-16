package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

/** Positive Gegenproben zur Pie-Sperre: der normale Tap-Handler bleibt mitkomponiert. */
@OptIn(ExperimentalTestApi::class)
class KartennavigationDesktopTest {
    private fun editor(knoten: List<KnotenDaten> = emptyList()) =
        KartenEditorZustand(KartenDaten(name = "Navigation", knoten = knoten), GraphPrüfung(AnschlussArtRegister()))

    @Test fun einFingerVerschiebtBeiGeschlossenemMenue() = runDesktopComposeUiTest(width = 900, height = 700) {
        val editor = editor()
        setContent { MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) } }
        onNodeWithTag("karte").performTouchInput { swipe(center, center + Offset(140f, 80f)) }
        runOnIdle {
            assertTrue(editor.karte.ansicht.verschiebung.x > 80f)
            assertTrue(editor.karte.ansicht.verschiebung.y > 40f)
            assertEquals(1f, editor.karte.ansicht.zoom)
            assertFalse(editor.kannRückgängig())
        }
    }

    @Test fun zweiFingerZoomenBeiGeschlossenemMenue() = runDesktopComposeUiTest(width = 900, height = 700) {
        val editor = editor()
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) }
        } }
        onNodeWithTag("karte").performTouchInput { navigationPinch(Offset.Zero) }
        runOnIdle { assertTrue(editor.karte.ansicht.zoom > 1.3f); assertFalse(editor.kannRückgängig()) }
    }

    @Test fun zweiFingerVerschiebenUndZoomenGemeinsam() = runDesktopComposeUiTest(width = 900, height = 700) {
        val editor = editor()
        setContent { MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) } }
        onNodeWithTag("karte").performTouchInput { navigationPinch(Offset(70f, 40f)) }
        runOnIdle {
            assertTrue(editor.karte.ansicht.zoom > 1.3f)
            assertNotEquals(GraphPunkt.Zero, editor.karte.ansicht.verschiebung)
        }
    }

    @Test fun mausradZoomUndMittlereTasteVerschieben() = runDesktopComposeUiTest(width = 900, height = 700) {
        val editor = editor()
        setContent { MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) } }
        onNodeWithTag("karte").performMouseInput { moveTo(center); scroll(-2f) }
        val nachZoom = runOnIdle { editor.karte.ansicht.also { assertTrue(it.zoom > 1f) } }
        onNodeWithTag("karte").performMouseInput {
            moveTo(center); press(MouseButton.Tertiary); moveBy(Offset(130f, 70f), delayMillis = 200); release(MouseButton.Tertiary); exit()
        }
        runOnIdle {
            assertNotEquals(nachZoom.verschiebung, editor.karte.ansicht.verschiebung)
            assertEquals(nachZoom.zoom, editor.karte.ansicht.zoom)
        }
    }

    @Test fun leertasteMitPrimaererMaustasteVerschiebtStattAuszuwaehlen() = runDesktopComposeUiTest(width = 900, height = 700) {
        val k = KnotenDaten(art = "test", name = "Fokus", position = GraphPunkt(60f, 60f))
        val editor = editor(listOf(k))
        setContent { MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) } }
        val fokus = onNodeWithContentDescription("Knoten Fokus, 0 Anschlüsse")
        fokus.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        fokus.performKeyInput { keyDown(Key.Spacebar) }
        onNodeWithTag("karte").performMouseInput {
            moveTo(center); press(); moveBy(Offset(130f, 70f), delayMillis = 200); release(); exit()
        }
        fokus.performKeyInput { keyUp(Key.Spacebar) }
        runOnIdle {
            assertNotEquals(GraphPunkt.Zero, editor.karte.ansicht.verschiebung)
            assertEquals(k.position, editor.karte.knoten.single().position)
            assertFalse(editor.kannRückgängig())
        }
    }

    @Test fun sperreBlockiertGestenUndGibtNavigationNachJedemAufhebenWiederFrei() = runDesktopComposeUiTest(width = 900, height = 700) {
        val editor = editor()
        var gesperrt by mutableStateOf(true)
        setContent { MaterialTheme { KnotenKartenEditor(editor, interaktionGesperrt = gesperrt, modifier = Modifier.testTag("karte")) } }
        repeat(3) {
            runOnIdle { gesperrt = true }
            val vorher = runOnIdle { editor.karte }
            onNodeWithTag("karte").performTouchInput { swipe(center, center + Offset(140f, 80f)) }
            onNodeWithTag("karte").performTouchInput { navigationPinch(Offset(30f, 20f)) }
            onNodeWithTag("karte").performMouseInput {
                moveTo(center); scroll(-2f); press(MouseButton.Tertiary); moveBy(Offset(90f, 50f), delayMillis = 200); release(MouseButton.Tertiary); exit()
            }
            runOnIdle { assertEquals(vorher, editor.karte); gesperrt = false }
            onNodeWithTag("karte").performTouchInput { swipe(center, center + Offset(140f, 80f)) }
            runOnIdle { assertNotEquals(vorher.ansicht.verschiebung, editor.karte.ansicht.verschiebung) }
        }
    }

    @Test fun anschlussZiehenAusserhalbDesKoerpersBewegtDieKameraNicht() = runDesktopComposeUiTest(width = 900, height = 700) {
        val art = AnschlussArtId("test.navigation")
        val aus = AnschlussDaten(name = "aus", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)
        val ein = AnschlussDaten(name = "ein", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art)
        val quelle = KnotenDaten(art = "test", name = "Quelle", position = GraphPunkt(100f, 180f), anschlüsse = listOf(aus))
        val ziel = KnotenDaten(art = "test", name = "Ziel", position = GraphPunkt(520f, 180f), anschlüsse = listOf(ein))
        val editor = KartenEditorZustand(KartenDaten(name = "Anschlüsse", knoten = listOf(quelle, ziel)),
            GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))))
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) }
        } }
        val port = onNode(hasContentDescription("Quelle, Anschluss aus", substring = true))
        val zielPort = onNode(hasContentDescription("Ziel, Anschluss ein", substring = true))
        val delta = zielPort.fetchSemanticsNode().boundsInRoot.center - port.fetchSemanticsNode().boundsInRoot.center
        val ansicht = runOnIdle { editor.karte.ansicht }
        port.performTouchInput {
            down(Offset(width - 3f, center.y)); moveBy(delta, delayMillis = 200); up()
        }
        runOnIdle {
            assertEquals(ansicht, editor.karte.ansicht)
            assertEquals(1, editor.karte.verbindungen.size)
            assertEquals(quelle.position, editor.karte.knoten.first { it.id == quelle.id }.position)
            editor.rückgängig(); assertTrue(editor.karte.verbindungen.isEmpty())
            editor.wiederholen(); assertEquals(1, editor.karte.verbindungen.size)
        }
    }

    @Test fun knotenziehenBewegtDieKameraNichtUndBleibtUndoFaehig() = runDesktopComposeUiTest(width = 900, height = 700) {
        val k = KnotenDaten(art = "test", name = "Ziehbar", position = GraphPunkt(90f, 90f))
        val editor = editor(listOf(k))
        setContent { MaterialTheme { KnotenKartenEditor(editor, modifier = Modifier.testTag("karte")) } }
        onNodeWithContentDescription("Knoten Ziehbar, 0 Anschlüsse").performTouchInput {
            down(Offset(30f, 20f)); moveBy(Offset(100f, 70f), delayMillis = 200); up()
        }
        runOnIdle {
            assertEquals(AnsichtsFenster.Standard, editor.karte.ansicht)
            val verschoben = editor.karte.knoten.single().position
            assertNotEquals(k.position, verschoben)
            editor.rückgängig(); assertEquals(k.position, editor.karte.knoten.single().position)
            editor.wiederholen(); assertEquals(verschoben, editor.karte.knoten.single().position)
        }
    }
}

private fun TouchInjectionScope.navigationPinch(versatz: Offset) {
    val mitte = center
    down(0, mitte - Offset(70f, 0f)); down(1, mitte + Offset(70f, 0f))
    moveTo(0, mitte + versatz / 2f - Offset(105f, 0f)); moveTo(1, mitte + versatz / 2f + Offset(105f, 0f))
    moveTo(0, mitte + versatz - Offset(140f, 0f)); moveTo(1, mitte + versatz + Offset(140f, 0f))
    up(0); up(1)
}
