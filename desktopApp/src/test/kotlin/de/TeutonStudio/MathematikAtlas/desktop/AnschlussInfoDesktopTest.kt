package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class AnschlussInfoDesktopTest {
    private val art = AnschlussArtId("test.info")
    private val register = AnschlussArtRegister(listOf(AnschlussArt(art, "Testtyp", beschreibung = "Beschreibung des Anschlusses.")))
    private fun szene(zoom: Float = 1f): KartenEditorZustand {
        fun port(ein: Boolean) = AnschlussDaten(name = if (ein) "Eingabe" else "Wert",
            richtung = if (ein) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
            kante = if (ein) AnschlussKante.Links else AnschlussKante.Rechts, art = art)
        val q = KnotenDaten(art = "test", name = "Quelle", position = GraphPunkt(70f, 90f), größe = GraphGröße(160f, 120f), anschlüsse = listOf(port(false)))
        val z = KnotenDaten(art = "test", name = "Ziel", position = GraphPunkt(470f, 90f), größe = GraphGröße(160f, 120f), anschlüsse = listOf(port(true)))
        return KartenEditorZustand(KartenDaten(name = "Info", knoten = listOf(q, z), ansicht = AnsichtsFenster(zoom = zoom)), GraphPrüfung(register))
    }
    private fun SemanticsNodeInteractionsProvider.port(name: String) = onNode(hasContentDescription(name, substring = true))

    @Test fun hoverHatVerzoegerungUndVerschwindetBeiExitPressScrollUndSperre() = runDesktopComposeUiTest(width = 1000, height = 700) {
        val editor = szene()
        val sperre = mutableStateOf(false)
        setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
            KnotenKartenEditor(editor, Modifier.size(1000.dp, 700.dp).testTag("editor"), zeigeKnotenInspektor = false,
                interaktionGesperrt = sperre.value, anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register) })
        } } }
        mainClock.autoAdvance = false
        val port = port("Quelle, Anschluss Wert,")
        fun hover() { port.performMouseInput { moveTo(center) }; mainClock.advanceTimeBy(550); waitForIdle() }
        port.performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(400); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        mainClock.advanceTimeBy(150); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertIsDisplayed()
        port.performMouseInput { exit() }; mainClock.advanceTimeByFrame(); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        hover(); port.performMouseInput { press() }; mainClock.advanceTimeByFrame(); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        port.performMouseInput { release() }; mainClock.advanceTimeBy(50); waitForIdle()
        onNodeWithText("Schließen").performClick(); mainClock.advanceTimeBy(50)
        port.performMouseInput { exit() }; hover()
        port.performMouseInput { scroll(-1f) }; mainClock.advanceTimeByFrame(); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        // Re-query the moved anchor after zoom.
        port.performMouseInput { exit(); moveTo(center) }; mainClock.advanceTimeBy(550); waitForIdle()
        runOnIdle { sperre.value = true }; mainClock.advanceTimeBy(50); waitForIdle()
        onNodeWithText("Ausgang · Testtyp").assertDoesNotExist()
        runOnIdle { assertTrue(editor.karte.verbindungen.isEmpty()); assertFalse(editor.kannRückgängig()) }
    }

    @Test fun kurzinfoBleibtBeiZoomUndGrosserSchriftAufFensterbreiteBegrenzt() {
        for (zoom in listOf(.5f, 1.25f)) for (dark in listOf(false, true)) {
            runDesktopComposeUiTest(width = 1000, height = 700) {
                val editor = szene(zoom)
                val langerName = "Ein langer Anschlussname ".repeat(8).trim()
                setContent { MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    CompositionLocalProvider(LocalDensity provides Density(1f, if (dark) 2f else 1f)) {
                        KnotenKartenEditor(editor, Modifier.size(1000.dp, 700.dp), zeigeKnotenInspektor = false,
                            anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register)?.copy(name = langerName) })
                    }
                } }
                mainClock.autoAdvance = false
                port("Quelle, Anschluss Wert,").performMouseInput { moveTo(center) }
                mainClock.advanceTimeBy(550); waitForIdle()
                val bounds = onNodeWithText(langerName).fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.width <= 320f, "Kurzinfo darf bei Zoom $zoom nicht mitskalieren: $bounds")
                onNodeWithText("Ausgang · Testtyp").assertIsDisplayed()
                port("Quelle, Anschluss Wert,").performMouseInput { exit() }
                mainClock.advanceTimeByFrame(); waitForIdle()
            }
        }
    }

    @Test fun tippenMausklickKontextUndDragBleibenGetrennt() = runDesktopComposeUiTest(width = 1000, height = 700) {
        val editor = szene()
        var kontext = 0
        setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
            KnotenKartenEditor(editor, Modifier.size(1000.dp, 700.dp), zeigeKnotenInspektor = false,
                beiAnschlussKontext = { kontext++ }, anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register) })
        } } }
        val quelle = port("Quelle, Anschluss Wert,")
        quelle.performTouchInput { click() }
        onNodeWithText("Anschluss: Wert").assertIsDisplayed()
        onNodeWithText("Beschreibung des Anschlusses.").assertIsDisplayed()
        runOnIdle { assertNull(editor.verbindungsStart); assertFalse(editor.kannRückgängig()) }
        onNodeWithText("Schließen").performClick()
        quelle.performMouseInput { click() }
        onNodeWithText("Anschluss: Wert").assertIsDisplayed()
        onNodeWithText("Schließen").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Schließen").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        quelle.performTouchInput { longClick() }
        quelle.performMouseInput { click(button = MouseButton.Secondary) }
        runOnIdle { assertEquals(2, kontext) }
        onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        val delta = port("Ziel, Anschluss Eingabe,").fetchSemanticsNode().boundsInRoot.center - quelle.fetchSemanticsNode().boundsInRoot.center
        val vorher = editor.karte.ansicht
        quelle.performTouchInput { swipe(center, center + delta, durationMillis = 300) }
        onNodeWithText("Anschluss: Wert").assertDoesNotExist()
        runOnIdle {
            assertEquals(1, editor.karte.verbindungen.size); assertEquals(vorher, editor.karte.ansicht)
            editor.rückgängig(); assertEquals(0, editor.karte.verbindungen.size)
            editor.wiederholen(); assertEquals(1, editor.karte.verbindungen.size)
        }
        // Abziehen einer vorhandenen Verbindung bleibt ein Undo-Schritt.
        port("Ziel, Anschluss Eingabe,").performMouseInput { moveTo(center); press(); moveBy(Offset(100f, 170f), delayMillis = 300); release() }
        runOnIdle { assertTrue(editor.karte.verbindungen.isEmpty()); editor.rückgängig(); assertEquals(1, editor.karte.verbindungen.size) }
    }

    @Test fun joinBeitragGruppenanschlussEntfernenUndKartenwechsel() = runDesktopComposeUiTest(width = 1000, height = 700) {
        val editor = szene()
        val basis = editor.karte
        val quelle = basis.knoten.first()
        val ziel = basis.knoten.last().let { k -> k.copy(anschlüsse = k.anschlüsse.map { it.copy(belegung = AnschlussBelegung.Mehrfach) }) }
        val verbindung = VerbindungDaten(von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id))
        val joinKarte = basis.copy(knoten = listOf(quelle, ziel), verbindungen = listOf(verbindung))
        editor.ersetzeKarte(joinKarte)
        setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
            KnotenKartenEditor(editor, Modifier.size(1000.dp, 700.dp), zeigeKnotenInspektor = false,
                anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register) })
        } } }
        port("Ziel, Anschluss Eingabe · 1,").performClick()
        onNodeWithText("Getroffener Beitrag").assertIsDisplayed()
        onNodeWithText("Quelle: Quelle · Wert").assertIsDisplayed()
        runOnIdle { assertEquals(joinKarte, editor.karte); assertFalse(editor.kannRückgängig()) }
        // Entfernen nur der Leitung schließt die Ansicht ihres nicht mehr existierenden Griffs.
        runOnIdle { editor.führeAus(KartenAktion.VerbindungLöschen(verbindung.id)) }
        onNodeWithText("Schließen").assertDoesNotExist()
        runOnIdle { editor.ersetzeKarte(basis.copy(visuelleGruppen = listOf(
            VisuelleKnotenGruppeDaten(titel = "Gruppe", knotenIds = setOf(ziel.id), eingeklappt = true,
                position = GraphPunkt(430f, 70f), größe = GraphGröße(230f, 200f))))) }
        waitForIdle()
        // Am gefalteten Gruppenanschluss wird die Originalreferenz zum Provider gereicht.
        port("Eingabe").performClick()
        onNodeWithText("Anschluss: Eingabe").assertIsDisplayed()
        onNodeWithText("Ziel").assertIsDisplayed()
        runOnIdle { editor.führeAus(KartenAktion.KnotenLöschen(ziel.id)) }
        onNodeWithText("Schließen").assertDoesNotExist()
        runOnIdle { editor.ersetzeKarte(basis) }
        port("Quelle, Anschluss Wert,").performClick()
        runOnIdle { editor.ersetzeKarte(basis.copy(id = neueKartenId())) }
        onNodeWithText("Schließen").assertDoesNotExist()
        // Außenklick und erneutes Öffnen verwenden denselben Abschluss.
        port("Quelle, Anschluss Wert,").performClick()
        onAllNodes(isRoot()).onLast().performTouchInput { click(Offset(4f, 4f)) }
        onNodeWithText("Schließen").assertDoesNotExist()
    }

    @Test fun dialogIstBeiGrosserSchriftUndKleinenFensternBedienbar() {
        for ((breite, höhe) in listOf(390 to 600, 800 to 350)) for (dunkel in listOf(false, true)) {
            runDesktopComposeUiTest(width = breite, height = höhe) {
                val editor = szene(.5f)
                setContent { MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                        KnotenKartenEditor(editor, Modifier.size(breite.dp, höhe.dp), zeigeKnotenInspektor = false,
                            anschlussInfoFür = { k, r -> anschlussBasisInfo(k, r, register)?.copy(
                                typ = "Langer strukturierter Typvertrag ".repeat(15),
                                verbindungen = List(25) { AnschlussInfoVerbindung(VerbindungsId("v$it"), "Quelle $it · langer Anschlussname") }) })
                    }
                } }
                port("Quelle, Anschluss Wert,").performClick()
                onNodeWithText("Schließen").assertIsDisplayed()
                val pixels = onAllNodes(isRoot()).onLast().captureToImage().toPixelMap()
                val bild = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    val c = pixels[x, y]
                    bild.setRGB(x, y, ((c.alpha * 255).toInt() shl 24) or ((c.red * 255).toInt() shl 16) or
                        ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt())
                }
                ImageIO.write(bild, "png", File("/tmp/atlas-anschluss-info-$breite-$höhe-$dunkel.png"))
                onNodeWithText("Quelle 24 · langer Anschlussname").performScrollTo().assertIsDisplayed()
                onNodeWithText("Schließen").performClick()
                onNodeWithText("Anschluss: Wert").assertDoesNotExist()
            }
        }
    }
}
