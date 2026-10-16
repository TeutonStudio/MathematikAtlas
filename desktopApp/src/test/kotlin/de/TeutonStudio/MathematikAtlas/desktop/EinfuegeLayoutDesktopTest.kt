package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class EinfuegeLayoutDesktopTest {
    private data class Szene(val editor: KartenEditorZustand, val a: KnotenId, val b: KnotenId, val c: KnotenId, val d: KnotenId)
    private fun szene(zoom: Float, font: Float = 1f): Szene {
        val art = AnschlussArtId("test")
        fun knoten(name: String, x: Float, y: Float) = KnotenDaten(art = "test", name = name,
            position = GraphPunkt(x * font, y * font), größe = GraphGröße(220f * font, 120f * font),
            anschlüsse = listOf(
                AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
                AnschlussDaten(name = "out", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val a = knoten("A", 60f, 150f)
        val b = knoten("B", 500f, 150f)
        val c = knoten("C", 280f, 340f)
        val d = knoten("D", 730f, 150f)
        val kante = VerbindungDaten(von = AnschlussVerweis(a.id, a.anschlüsse[1].id), zu = AnschlussVerweis(b.id, b.anschlüsse[0].id))
        return Szene(KartenEditorZustand(KartenDaten(name = "Drag", knoten = listOf(a, b, c, d), verbindungen = listOf(kante),
            ansicht = AnsichtsFenster(zoom = zoom)), GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test"))))), a.id, b.id, c.id, d.id)
    }
    private fun KartenDaten.knoten(id: KnotenId) = knoten.single { it.id == id }

    @Test fun touchSchafftBeiZoomSchriftUndThemePlatz() = prüfeLayout(maus = false)
    @Test fun mausSchafftBeiZoomSchriftUndThemePlatz() = prüfeLayout(maus = true)

    private fun prüfeLayout(maus: Boolean) = runDesktopComposeUiTest(width = 2600, height = 1300) {
        val aktuell = mutableStateOf(szene(1f))
        val font = mutableStateOf(1f)
        val dunkel = mutableStateOf(false)
        setContent { MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(1f, font.value)) {
                KnotenKartenEditor(aktuell.value.editor, Modifier.size(2600.dp, 1300.dp), zeigeKnotenInspektor = false)
            }
        } }
        for (schrift in listOf(1f, 2f)) for (zoom in listOf(.5f, 1.25f)) for (dark in listOf(false, true)) {
            runOnIdle { font.value = schrift; dunkel.value = dark; aktuell.value = szene(zoom, schrift) }
            waitForIdle()
            val s = aktuell.value
            val vorher = s.editor.karte
            val dargestellt = s.editor.darstellungsKarte
            val a = dargestellt.knoten(s.a); val b = dargestellt.knoten(s.b); val c = dargestellt.knoten(s.c)
            val von = a.position + a.anschlussPositionRelativ(a.anschlüsse[1])
            val zu = b.position + b.anschlussPositionRelativ(b.anschlüsse[0])
            val ziel = GraphPunkt((von.x + zu.x - c.größe.breite) / 2f, (von.y + zu.y - c.größe.höhe) / 2f)
            val delta = ziel - c.position
            val node = onNodeWithContentDescription("Knoten C, 2 Anschlüsse")
            if (maus) node.performMouseInput {
                moveTo(Offset(30f, 15f)); press(); moveBy(Offset(delta.x * zoom, delta.y * zoom), delayMillis = 300)
            } else node.performTouchInput {
                down(Offset(30f, 15f)); moveBy(Offset(delta.x * zoom, delta.y * zoom), delayMillis = 300)
            }
            waitForIdle()
            // Touch-Slop gehört zur bestehenden Drag-Geste. Das neue Layout muss
            // die tatsächlich erreichte Ablageposition unverändert erhalten.
            val abgelegt = s.editor.karte.knoten(s.c).position
            if (maus) node.performMouseInput { release() } else node.performTouchInput { up() }
            waitForIdle()
            runOnIdle {
                val karte = s.editor.darstellungsKarte
                val links = karte.knoten(s.a); val mitte = karte.knoten(s.c); val rechts = karte.knoten(s.b); val hindernis = karte.knoten(s.d)
                assertEquals(2, s.editor.karte.verbindungen.size, "font=$schrift zoom=$zoom dark=$dark")
                assertTrue(links.position.x + links.größe.breite + 31.9f <= mitte.position.x)
                assertTrue(mitte.position.x + mitte.größe.breite + 31.9f <= rechts.position.x)
                assertTrue(rechts.position.x + rechts.größe.breite + 31.9f <= hindernis.position.x)
                assertEquals(abgelegt, mitte.position)
                assertEquals(vorher.knoten.map { it.position.y }.filterIndexed { i, _ -> vorher.knoten[i].id != s.c },
                    s.editor.karte.knoten.filter { it.id != s.c }.map { it.position.y })
            }
            val physisch = onNodeWithContentDescription("Knoten B, 2 Anschlüsse").fetchSemanticsNode().boundsInRoot
            assertEquals(s.editor.darstellungsKarte.knoten(s.b).position.x * zoom, physisch.left, 1.1f)
            if (schrift == 1f && zoom == 1.25f) {
                val pixels = onRoot().captureToImage().toPixelMap()
                val bild = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    val p = pixels[x, y]
                    bild.setRGB(x, y, ((p.alpha * 255).toInt() shl 24) or ((p.red * 255).toInt() shl 16) or
                        ((p.green * 255).toInt() shl 8) or (p.blue * 255).toInt())
                }
                ImageIO.write(bild, "png", File("/tmp/atlas-einfuege-${if (dark) "dunkel" else "hell"}.png"))
            }
            runOnIdle {
                val nachher = s.editor.karte
                s.editor.rückgängig(); assertEquals(vorher, s.editor.karte); assertFalse(s.editor.kannRückgängig())
                s.editor.wiederholen(); assertEquals(nachher, s.editor.karte)
            }
        }
    }

    @Test fun animationFolgtGespeichertemEndstandUndUndoUnterbrichtSie() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val s = szene(1f)
        setContent { MaterialTheme { KnotenKartenEditor(s.editor, Modifier.size(1200.dp, 800.dp), zeigeKnotenInspektor = false) } }
        waitForIdle()
        val vorher = s.editor.karte
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performMouseInput {
            moveTo(Offset(30f, 15f)); press(); moveBy(Offset(0f, -190f), delayMillis = 300); release()
        }
        runOnIdle {
            assertEquals(2, s.editor.karte.verbindungen.size)
            assertEquals(28f, s.editor.karte.knoten(s.a).position.x)
            assertEquals(60f, s.editor.darstellungsKarte.knoten(s.a).position.x)
        }
        mainClock.advanceTimeBy(96)
        runOnIdle {
            val x = s.editor.darstellungsKarte.knoten(s.a).position.x
            assertTrue(x > 28f && x < 60f, "Zwischenstand $x")
            val projection = s.editor.projektion
            val k = projection.karte.knoten(s.a)
            assertEquals(k.position + k.anschlussPositionRelativ(k.anschlüsse[1]), projection.anschlussPositionen[AnschlussVerweis(k.id, k.anschlüsse[1].id)])
            s.editor.rückgängig(); assertEquals(vorher, s.editor.karte)
        }
        mainClock.advanceTimeBy(400)
        runOnIdle { assertEquals(vorher.knoten.map { it.position }, s.editor.darstellungsKarte.knoten.map { it.position }) }
        mainClock.autoAdvance = true
    }
}
