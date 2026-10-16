package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.VerbindungsEinfügeAuswahlDialog
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class VerbindungsEinfügeDialogDesktopTest {
    @Test fun auswahlBestaetigtErstExplizitUndAbbruchIstGetrennt() = runDesktopComposeUiTest(width = 600, height = 380) {
        var wert: Int? = null
        var abbrüche = 0
        setContent { MaterialTheme {
            VerbindungsEinfügeAuswahlDialog("Anschluss auswählen", listOf("Argument links" to "Zahl", "Argument rechts" to "Winkel · Mit Übersetzer"),
                { abbrüche++ }, { wert = it })
        } }
        screenshot(onAllNodes(isRoot()).onLast(), "/tmp/atlas-anschluss-dialog.png")
        onNodeWithText("Verbinden").assertIsNotEnabled()
        onNodeWithText("Argument rechts").performClick()
        runOnIdle { assertNull(wert) }
        onNodeWithText("Verbinden").performClick()
        runOnIdle { assertEquals(1, wert); assertEquals(0, abbrüche) }
        onNodeWithText("Abbrechen").performClick()
        runOnIdle { assertEquals(1, abbrüche) }
    }

    @Test fun vieleLangeEintraegeBleibenMitGrosserSchriftUndNiedrigerHoeheErreichbar() = runDesktopComposeUiTest(width = 480, height = 360) {
        var dunkel by mutableStateOf(false)
        var gewählt: Int? = null
        val optionen = (0..39).map { "Anschluss $it mit einem langen verständlichen Rollennamen" to "Strukturierter Wert mit Übersetzer und zusätzlicher Anschlussbeschreibung" }
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
            MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                VerbindungsEinfügeAuswahlDialog("Verbindungsweg auswählen", optionen, {}, { gewählt = it })
            }
        } }
        for (dark in listOf(false, true)) {
            runOnIdle { dunkel = dark; gewählt = null }
            val titel = onNodeWithText("Verbindungsweg auswählen")
            val layouts = mutableListOf<TextLayoutResult>()
            titel.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            assertTrue(titel.fetchSemanticsNode().boundsInRoot.height + 1f >= layouts.single().size.height,
                "Der Aufgabentitel muss bei großer Schrift vollständig sichtbar bleiben")
            onNodeWithText(optionen.last().first).performScrollTo().performClick()
            onNodeWithText("Verbinden").assertIsDisplayed().performClick()
            onNodeWithText("Abbrechen").assertIsDisplayed()
            runOnIdle { assertEquals(39, gewählt) }
            if (dark) screenshot(onAllNodes(isRoot()).onLast(), "/tmp/atlas-anschluss-dialog-gross.png")
        }
    }
}

private fun screenshot(node: SemanticsNodeInteraction, pfad: String) {
    val pixel = node.captureToImage().toPixelMap()
    val bild = BufferedImage(pixel.width, pixel.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until pixel.height) for (x in 0 until pixel.width) bild.setRGB(x, y, pixel[x, y].toArgb())
    ImageIO.write(bild, "png", File(pfad))
}
