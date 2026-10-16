package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PieVorschauDesktopTest {
    @Test fun passiverKnotenHatKartenzoomUndGreiftNichtInDieKarteEin() = runDesktopComposeUiTest(width = 1100, height = 900) {
        val k = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        var zoom by mutableFloatStateOf(1f)
        var schrift by mutableFloatStateOf(1f)
        var dunkel by mutableStateOf(false)
        var größe = k.größe
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, schrift)) {
                MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                    Box(Modifier.fillMaxSize()) {
                        KnotenVorschau(k, MathematikKnotenRenderer(), zoom, { MaterialTheme.colorScheme.primary },
                            Modifier.padding(24.dp).testTag("passiv"), beiGröße = { größe = it })
                    }
                }
            }
        }
        for (z in listOf(.25f, 1f, 3.5f)) {
            runOnIdle { zoom = z }
            waitForIdle()
            val bounds = onNodeWithTag("passiv").fetchSemanticsNode().boundsInRoot
            assertEquals(größe.breite * z, bounds.width, 2f)
        }
        runOnIdle { zoom = 1f; schrift = 2f; dunkel = true }
        waitForIdle()
        val pixel = onRoot().captureToImage().toPixelMap()
        val bild = BufferedImage(pixel.width, pixel.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixel.height) for (x in 0 until pixel.width) {
            val p = pixel[x, y]
            bild.setRGB(x, y, ((p.alpha * 255).toInt() shl 24) or ((p.red * 255).toInt() shl 16) or
                ((p.green * 255).toInt() shl 8) or (p.blue * 255).toInt())
        }
        ImageIO.write(bild, "png", File("/tmp/atlas-pie-vorschau.png"))
    }

    @Test fun gesperrterEditorIgnoriertScrollUndHintergrundDrag() = runDesktopComposeUiTest(width = 700, height = 500) {
        val editor = KartenEditorZustand(KartenDaten(name = "gesperrt"), GraphPrüfung(AnschlussArtRegister()))
        setContent { MaterialTheme { KnotenKartenEditor(editor, interaktionGesperrt = true, modifier = Modifier.testTag("editor")) } }
        val vorher = editor.karte
        onNodeWithTag("editor").performMouseInput { moveTo(center); scroll(-2f) }
        onNodeWithTag("editor").performTouchInput { swipe(center, center + Offset(100f, 70f)) }
        runOnIdle { assertEquals(vorher, editor.karte) }
    }
}
