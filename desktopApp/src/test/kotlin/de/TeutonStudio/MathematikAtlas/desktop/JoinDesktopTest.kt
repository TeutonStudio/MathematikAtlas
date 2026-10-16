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
class JoinDesktopTest {
    private fun szene(zoom: Float): KartenEditorZustand {
        val art = AnschlussArtId("test")
        val join = KnotenDaten(art = "test.join", name = "Join", position = GraphPunkt(550f, 140f), größe = GraphGröße(240f, 130f),
            anschlüsse = listOf(AnschlussDaten(name = "operanden", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, belegung = AnschlussBelegung.Mehrfach)))
        val q = KnotenDaten(art = "test.quelle", name = "Quelle", position = GraphPunkt(100f, 140f),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val v = VerbindungDaten(von = AnschlussVerweis(q.id, q.anschlüsse.single().id), zu = AnschlussVerweis(join.id, join.anschlüsse.single().id))
        return KartenEditorZustand(KartenDaten(name = "Join", knoten = listOf(q, join), verbindungen = listOf(v), ansicht = AnsichtsFenster(zoom = zoom)), GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))))
    }
    @Test fun joinLaesstSichMitTouchUndMausErweiternUndEinzelnLoesen() = runDesktopComposeUiTest(width = 1400, height = 1000) {
        val editor = mutableStateOf(szene(1f))
        val dunkel = mutableStateOf(false)
        val schrift = mutableStateOf(1f)
        setContent { MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(1f, schrift.value)) {
                KnotenKartenEditor(editor.value, Modifier.size(1400.dp, 1000.dp), zeigeKnotenInspektor = false)
            }
        } }
        for (maus in listOf(false, true)) for (zoom in listOf(.5f, 1.25f)) for (dark in listOf(false, true)) {
            runOnIdle { editor.value = szene(zoom); dunkel.value = dark; schrift.value = if (dark) 2f else 1f }
            waitForIdle()
            fun port(name: String) = onNode(hasContentDescription(name, substring = true))
            val quelle = port("Quelle, Anschluss wert,")
            val delta = port("Join, Anschluss operanden,").fetchSemanticsNode().boundsInRoot.center -
                quelle.fetchSemanticsNode().boundsInRoot.center
            if (maus) quelle.performMouseInput { moveTo(center); press(); moveTo(center + delta, delayMillis = 300); release() }
            else quelle.performTouchInput { swipe(center, center + delta, durationMillis = 300) }
            waitForIdle()
            runOnIdle { assertEquals(2, editor.value.karte.verbindungen.size) }
            val griff = port("Join, Anschluss operanden · 1,")
            if (maus) griff.performMouseInput { moveTo(center); press(); moveBy(Offset(-90f, -110f), delayMillis = 300); release() }
            else griff.performTouchInput { down(center); moveBy(Offset(-90f, -110f), delayMillis = 300); up() }
            waitForIdle()
            runOnIdle {
                assertEquals(1, editor.value.karte.verbindungen.size)
                editor.value.rückgängig()
                assertEquals(2, editor.value.karte.verbindungen.size)
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val bild = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            bild.setRGB(x, y, ((c.alpha * 255).toInt() shl 24) or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt())
        }
        ImageIO.write(bild, "png", File("/tmp/atlas-join-desktop.png"))
    }
}
