package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class KnotenAnhaengeDesktopTest {
    private val fehler = "Dieser längere Fehler beschreibt vollständig, weshalb die aktuelle Eingabe für diesen Knoten nicht ausgewertet werden kann."
    private fun editor(knoten: KnotenDaten, zoom: Float = 1f) = KartenEditorZustand(
        KartenDaten(name = "Anhänge", knoten = listOf(knoten), ansicht = AnsichtsFenster(zoom = zoom)),
        GraphPrüfung(AnschlussArtRegister(emptyList())),
    )

    private fun assertVollstaendig(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertTrue(layouts.none { it.didOverflowHeight || it.didOverflowWidth }, layouts.toString())
    }

    @Test fun anhaengeWachsenAusserhalbUndAendernWederKoerperNochGriff() = runDesktopComposeUiTest(width = 1500, height = 1400) {
        val compose = this
        val basis = KnotenDaten(art = "test", name = "Körper", position = GraphPunkt(80f, 350f), größe = GraphGröße(300f, 130f))
        val zustand = mutableStateOf(editor(basis))
        val schrift = mutableStateOf(1f)
        val dunkel = mutableStateOf(false)
        val ergebnis = mutableStateOf(KnotenAuswertungsErgebnis(emptyMap()))
        compose.setContent {
            MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalDensity provides Density(1f, schrift.value)) {
                    KnotenKartenEditor(zustand.value, Modifier.size(1500.dp, 1400.dp),
                        rendererFür = { MathematikKnotenRenderer(ergebnisFür = { ergebnis.value }).mitAuswertungszeit { ergebnis.value } },
                        zeigeKnotenInspektor = false)
                }
            }
        }
        for (adapter in listOf(false, true)) for (font in listOf(1f, 2f)) for (zoom in listOf(.5f, 2f)) for (dark in listOf(false, true)) {
            compose.runOnIdle {
                schrift.value = font
                dunkel.value = dark
                val knoten = basis.copy(eigenschaften = if (adapter) mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")) else emptyMap())
                zustand.value = editor(knoten, zoom).also { it.wähleKnoten(knoten.id) }
                ergebnis.value = KnotenAuswertungsErgebnis(emptyMap())
            }
            compose.waitForIdle()
            val ohne = compose.onNodeWithContentDescription("Knoten Körper, 0 Anschlüsse").fetchSemanticsNode().boundsInRoot
            val griffOhne = compose.onNodeWithContentDescription("Knoten Körper vergrößern").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                ergebnis.value = KnotenAuswertungsErgebnis(emptyMap(), fehler = fehler,
                    warnungen = listOf("Warnung eins", "Warnung zwei", fehler), elementFehler = mapOf("element" to "Elementfehler"),
                    auswertungsDauerNanos = 1_234_000L)
            }
            compose.waitForIdle()
            val körper = compose.onNodeWithContentDescription("Knoten Körper, 0 Anschlüsse").fetchSemanticsNode().boundsInRoot
            val griff = compose.onNodeWithContentDescription("Knoten Körper vergrößern").fetchSemanticsNode().boundsInRoot
            assertEquals(ohne, körper, "Anhänge dürfen die Körpergeometrie nicht verändern.")
            assertEquals(griffOhne, griff)
            assertTrue(kotlin.math.abs(griff.center.y - körper.bottom) < 15f * zoom, "adapter=$adapter font=$font zoom=$zoom dark=$dark Körper=$körper Griff=$griff Root=${compose.onAllNodes(isRoot()).onLast().fetchSemanticsNode().boundsInRoot}")
            val fehlerNode = compose.onNodeWithText(fehler)
            compose.onAllNodesWithText(fehler).assertCountEquals(1)
            assertVollstaendig(fehlerNode)
            assertTrue(fehlerNode.fetchSemanticsNode().boundsInRoot.bottom <= körper.top + 1f)
            for (text in listOf("Elementfehler", "Warnung eins", "Warnung zwei")) {
                val node = compose.onNodeWithText(text)
                node.assertIsDisplayed()
                assertTrue(node.fetchSemanticsNode().boundsInRoot.bottom <= körper.top + 1f)
            }
            val zeit = compose.onNode(hasContentDescription("Letzte Auswertungsdauer", substring = true))
            zeit.assertIsDisplayed()
            assertTrue(zeit.fetchSemanticsNode().boundsInRoot.top >= körper.bottom - 1f)
            compose.runOnIdle { assertEquals(basis.größe, zustand.value.karte.knoten.single().größe) }
            if (!adapter && font == 1f && zoom == 2f && !dark) screenshot("/tmp/knoten-anhaenge-rechteck.png", compose)
            if (adapter && font == 1f && zoom == 2f && dark) screenshot("/tmp/knoten-anhaenge-adapter.png", compose)
            compose.runOnIdle { ergebnis.value = KnotenAuswertungsErgebnis(emptyMap()) }
            compose.onAllNodesWithText(fehler).assertCountEquals(0)
            compose.onAllNodes(hasContentDescription("Letzte Auswertungsdauer", substring = true)).assertCountEquals(0)
            compose.runOnIdle { assertEquals(KnotenAnhangMaße(), zustand.value.knotenAnhänge[basis.id] ?: KnotenAnhangMaße()) }
        }
    }

    @Test fun zeitfeldWaehltNichtUndGroessenaenderungBleibtUndoFaehig() = runDesktopComposeUiTest {
        val compose = this
        val knoten = KnotenDaten(art = "test", name = "K", position = GraphPunkt(100f, 150f), größe = GraphGröße(300f, 140f))
        val z = editor(knoten)
        val renderer = MathematikKnotenRenderer(ergebnisFür = { KnotenAuswertungsErgebnis(emptyMap(), auswertungsDauerNanos = 1000L) })
        compose.setContent { MaterialTheme {
            KnotenKartenEditor(z, Modifier.size(1000.dp, 800.dp), rendererFür = { renderer }, zeigeKnotenInspektor = false)
        } }
        compose.runOnIdle { z.wähleKnoten(knoten.id) }
        compose.onNodeWithText("Δzeit", substring = true).performTouchInput { click() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertNull(z.ausgewählterKnoten); z.wähleKnoten(knoten.id) }
        compose.onNodeWithContentDescription("Knoten K vergrößern").performTouchInput {
            down(center); moveBy(Offset(80f, 60f), delayMillis = 150); up()
        }
        compose.runOnIdle {
            assertTrue(z.karte.knoten.single().größe.breite > knoten.größe.breite)
            assertTrue(z.karte.knoten.single().größe.höhe > knoten.größe.höhe)
            z.rückgängig()
            assertEquals(knoten.größe, z.karte.knoten.single().größe)
        }
    }

    @Test fun lokaleViewportHinweiseWerdenObenAngezeigtUndGemeinsamMitFehlernEntfernt() = runDesktopComposeUiTest {
        val compose = this
        val knoten = KnotenDaten(art = "mathematik.visualisierung", name = "Geometrie", position = GraphPunkt(60f, 200f))
        val z = editor(knoten)
        val ergebnis = mutableStateOf<KnotenAuswertungsErgebnis?>(null)
        compose.setContent { MaterialTheme {
            KnotenKartenEditor(z, Modifier.size(1100.dp, 900.dp), rendererFür = {
                GeometrieVisualisierungsKnotenRenderer { ergebnis.value }.mitAuswertungszeit { ergebnis.value }
            }, zeigeKnotenInspektor = false)
        } }
        val hinweis = compose.onNodeWithText("Verbinde ein geometrisches Objekt.")
        hinweis.assertIsDisplayed()
        val körper = compose.onNodeWithContentDescription("Knoten Geometrie, 0 Anschlüsse").fetchSemanticsNode().boundsInRoot
        assertTrue(hinweis.fetchSemanticsNode().boundsInRoot.bottom <= körper.top)
        compose.runOnIdle { ergebnis.value = KnotenAuswertungsErgebnis(emptyMap(), fehler = "Ungültige Geometrie") }
        compose.onNodeWithText("Ungültige Geometrie").assertIsDisplayed()
        compose.onAllNodesWithText("Verbinde ein geometrisches Objekt.").assertCountEquals(0)
    }

    @Test fun mausAufAeussererGriffhaelfteBleibtBeimKnotenUndAnschluss() = runDesktopComposeUiTest {
        val compose = this
        val art = AnschlussArtId("test")
        val anschluss = AnschlussDaten(name = "aus", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)
        val knoten = KnotenDaten(art = "test", name = "K", position = GraphPunkt(100f, 100f), größe = GraphGröße(240f, 140f), anschlüsse = listOf(anschluss))
        val z = KartenEditorZustand(KartenDaten(name = "Griffe", knoten = listOf(knoten)), GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))))
        var hintergrundMenüs = 0
        var verbindungenZumHintergrund = 0
        compose.setContent { MaterialTheme {
            KnotenKartenEditor(z, Modifier.size(1024.dp, 768.dp), zeigeKnotenInspektor = false,
                beiHintergrundKontext = { hintergrundMenüs++ },
                beiVerbindungAufHintergrund = { _, _ -> verbindungenZumHintergrund++ })
        } }
        for (zoom in listOf(.5f, 2f)) {
            compose.runOnIdle { z.ersetzeKarte(KartenDaten(name = "Griffe", knoten = listOf(knoten), ansicht = AnsichtsFenster(zoom = zoom))) }
            val port = compose.onNode(hasContentDescription("K, Anschluss aus", substring = true))
            val vorherigeVerbindungen = verbindungenZumHintergrund
            port.performMouseInput { moveTo(Offset(width - 3f, center.y)); press(); moveBy(Offset(100f, 30f), delayMillis = 150); release() }
            compose.runOnIdle {
                assertEquals(vorherigeVerbindungen + 1, verbindungenZumHintergrund)
                assertEquals(0, hintergrundMenüs)
                z.wähleKnoten(knoten.id)
            }
            compose.onNodeWithContentDescription("Knoten K vergrößern").performMouseInput {
                moveTo(Offset(width - 1f, height - 1f)); press(); moveBy(Offset(70f, 60f), delayMillis = 150); release()
            }
            compose.runOnIdle {
                assertTrue(z.karte.knoten.single().größe.breite > knoten.größe.breite)
                assertEquals(knoten.id, z.ausgewählterKnoten)
                z.rückgängig()
                assertEquals(knoten.größe, z.karte.knoten.single().größe)
            }
        }
    }

    @Test fun langeKopfzeileBleibtBeimPanSichtbarWennDerKoerperAusserhalbDesRenderfenstersLiegt() = runDesktopComposeUiTest {
        val compose = this
        val knoten = KnotenDaten(art = "test", name = "K", position = GraphPunkt(80f, 100f), größe = GraphGröße(300f, 120f))
        val z = editor(knoten)
        val text = (1..90).joinToString("\n") { "Meldung $it" }
        val renderer = MathematikKnotenRenderer(ergebnisFür = { KnotenAuswertungsErgebnis(emptyMap(), fehler = text) })
        compose.setContent { MaterialTheme {
            KnotenKartenEditor(z, Modifier.size(1024.dp, 768.dp), rendererFür = { renderer }, zeigeKnotenInspektor = false)
        } }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(z.knotenAnhänge.getValue(knoten.id).oben > 1200f)
            z.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(verschiebung = GraphPunkt(0f, 1700f))), mitHistorie = false)
        }
        compose.onNodeWithText(text).assertIsDisplayed()
        compose.onNodeWithContentDescription("Knoten K, 0 Anschlüsse").assertExists().assertIsNotDisplayed()
        compose.runOnIdle { assertEquals(knoten.position, z.karte.knoten.single().position) }
    }

    private fun screenshot(pfad: String, compose: ComposeUiTest) {
        val pixel = compose.onAllNodes(isRoot()).onLast().captureToImage().toPixelMap()
        val bild = BufferedImage(pixel.width, pixel.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixel.height) for (x in 0 until pixel.width) {
            val c = pixel[x, y]
            bild.setRGB(x, y, ((c.alpha * 255).toInt() shl 24) or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt())
        }
        ImageIO.write(bild, "png", File(pfad))
    }
}
