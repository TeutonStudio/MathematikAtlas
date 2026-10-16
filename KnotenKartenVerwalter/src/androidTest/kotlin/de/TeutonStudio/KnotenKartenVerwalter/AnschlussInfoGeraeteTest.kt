package de.TeutonStudio.KnotenKartenVerwalter

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Isolierter Editor ohne Dateien oder produktiven App-Zustand. */
class AnschlussInfoGeraeteTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View
    private lateinit var editor: KartenEditorZustand
    private var kontext = 0
    private var downZeit = 0L
    private fun szene() {
        val art = AnschlussArtId("test")
        val q = KnotenDaten(art = "test", name = "Quelle", position = GraphPunkt(30f, 90f), größe = GraphGröße(140f, 100f),
            anschlüsse = listOf(AnschlussDaten(name = "Wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val z = KnotenDaten(art = "test", name = "Ziel", position = GraphPunkt(290f, 90f), größe = GraphGröße(140f, 100f),
            anschlüsse = listOf(AnschlussDaten(name = "Eingabe", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art)))
        val register = AnschlussArtRegister(listOf(AnschlussArt(art, "Testtyp", beschreibung = "Beschreibung des Anschlusses.")))
        editor = KartenEditorZustand(KartenDaten(name = "Info", knoten = listOf(q, z)), GraphPrüfung(register))
        compose.setContent { view = LocalView.current; MaterialTheme {
            KnotenKartenEditor(editor, Modifier.fillMaxSize(), zeigeKnotenInspektor = false,
                beiAnschlussKontext = { kontext++ }, anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register) })
        } }
    }
    private fun port(name: String) = compose.onNode(hasContentDescription(name, substring = true))
    private fun stift(action: Int, position: Offset) {
        compose.runOnIdle {
            val zeit = android.os.SystemClock.uptimeMillis()
            if (action == MotionEvent.ACTION_DOWN) downZeit = zeit
            val props = MotionEvent.PointerProperties().apply { id = 17; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val coords = MotionEvent.PointerCoords().apply {
                x = position.x; y = position.y
                pressure = if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) 1f else 0f
            }
            val e = MotionEvent.obtain(downZeit, zeit, action, 1, arrayOf(props), arrayOf(coords), 0, 0,
                1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_HOVER_EXIT)
                view.dispatchGenericMotionEvent(e) else view.dispatchTouchEvent(e)
            e.recycle()
        }
    }
    @android.annotation.TargetApi(29)
    @Test fun stiftHoverVerzoegerungExitUndTippenMitZurueck() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 29)
        szene()
        compose.mainClock.autoAdvance = false
        val punkt = port("Quelle, Anschluss Wert,").fetchSemanticsNode().boundsInRoot.center
        stift(MotionEvent.ACTION_HOVER_ENTER, punkt)
        stift(MotionEvent.ACTION_HOVER_MOVE, punkt)
        compose.mainClock.advanceTimeBy(400); compose.waitForIdle()
        compose.onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(150); compose.waitForIdle()
        compose.onNodeWithText("Ausgang · Testtyp").assertIsDisplayed()
        stift(MotionEvent.ACTION_HOVER_EXIT, punkt)
        // AndroidComposeView verzögert HoverExit um eine reale Handler-Framezeit.
        compose.waitUntil(1_000) {
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodesWithText("Ausgang · Testtyp").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        stift(MotionEvent.ACTION_DOWN, punkt); stift(MotionEvent.ACTION_UP, punkt)
        compose.mainClock.advanceTimeBy(100); compose.waitForIdle()
        compose.onNodeWithText("Anschluss: Wert").assertIsDisplayed()
        compose.onNodeWithText("Beschreibung des Anschlusses.").assertIsDisplayed()
        compose.runOnIdle { assertNull(editor.verbindungsStart); assertFalse(editor.kannRückgängig()) }
        compose.mainClock.autoAdvance = true
        // Direkt an das native Dialogfenster senden: unabhängig vom Systemfokus
        // (das Tablet kann die Test-Activity während Instrumentierung überlagern).
        compose.runOnIdle {
            val roots = android.view.inspector.WindowInspector.getGlobalWindowViews()
            android.util.Log.i("AnschlussInfoTest", roots.joinToString { "${it.javaClass.simpleName}:focus=${it.hasWindowFocus()}:${(it.layoutParams as? android.view.WindowManager.LayoutParams)?.title}" })
            val dialogRoot = roots.last()
            val zeit = android.os.SystemClock.uptimeMillis()
            dialogRoot.dispatchKeyEvent(android.view.KeyEvent(zeit, zeit, android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK, 0))
            dialogRoot.dispatchKeyEvent(android.view.KeyEvent(zeit, zeit + 10, android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK, 0))
        }
        compose.waitUntil(2_000) {
            compose.onAllNodesWithText("Anschluss: Wert").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Anschluss: Wert").assertDoesNotExist()
    }
    @Test fun stiftDragErstelltVerbindungOhneDialogOderKamerabewegung() {
        szene()
        val start = port("Quelle, Anschluss Wert,").fetchSemanticsNode().boundsInRoot.center
        val ziel = port("Ziel, Anschluss Eingabe,").fetchSemanticsNode().boundsInRoot.center
        val ansicht = editor.karte.ansicht
        stift(MotionEvent.ACTION_DOWN, start)
        stift(MotionEvent.ACTION_MOVE, start + Offset(30f, 0f))
        stift(MotionEvent.ACTION_MOVE, ziel)
        stift(MotionEvent.ACTION_UP, ziel)
        compose.waitForIdle()
        compose.onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, editor.karte.verbindungen.size); assertEquals(ansicht, editor.karte.ansicht) }
    }

    @Test fun touchKontextDragCancelUndUndoRedo() {
        szene()
        val q = port("Quelle, Anschluss Wert,")
        q.performTouchInput { click() }
        compose.onNodeWithText("Anschluss: Wert").assertIsDisplayed()
        compose.onNodeWithText("Schließen").performClick()
        q.performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1, kontext) }
        compose.onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        val delta = port("Ziel, Anschluss Eingabe,").fetchSemanticsNode().boundsInRoot.center - q.fetchSemanticsNode().boundsInRoot.center
        val ansicht = editor.karte.ansicht
        q.performTouchInput { swipe(center, center + delta, durationMillis = 300) }
        compose.onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, editor.karte.verbindungen.size); assertEquals(ansicht, editor.karte.ansicht)
            editor.rückgängig(); assertTrue(editor.karte.verbindungen.isEmpty())
            editor.wiederholen(); assertEquals(1, editor.karte.verbindungen.size)
        }
        port("Ziel, Anschluss Eingabe,").performTouchInput { down(center); moveBy(Offset(100f, 150f), delayMillis = 300); cancel() }
        compose.runOnIdle { assertEquals(1, editor.karte.verbindungen.size); assertNull(editor.verbindungsStart) }
    }
}
