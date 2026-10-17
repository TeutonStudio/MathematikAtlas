package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AnschlussSymbol
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class MethodenFarbenDesktopTest {
    @Test fun halbkreisfarbenBleibenBeiInkompatibilitaetUndThemewechselErhalten() = runDesktopComposeUiTest(width = 390, height = 350) {
        var kompatibel by mutableStateOf(true)
        var dunkel by mutableStateOf(false)
        var schrift by mutableFloatStateOf(1f)
        val anschluss = AnschlussDaten(name = "f", art = AnschlussArtId("mathematik.methode"), kante = AnschlussKante.Rechts,
            vertrag = AnschlussVertrag(TypAusdruck.Parameterisiert(TypId("mathematik.methode"), listOf(
                TypAusdruck.Atom(TypId("zahl")), TypAusdruck.Atom(TypId("menge"))))))
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, schrift)) {
                MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(32.dp).size(100.dp).testTag("symbol")) {
                            AnschlussSymbol(anschluss, listOf(Color.Red), 100f, 1f, kompatibel,
                                { if (it.art.wert == "zahl") Color.Red else Color.Blue })
                        }
                    }
                }
            }
        }
        for (theme in listOf(false, true)) for (skala in listOf(1f, 2f)) for (aktiv in listOf(true, false, true)) {
            runOnIdle { dunkel = theme; schrift = skala; kompatibel = aktiv }
            waitForIdle()
            val pixel = onNodeWithTag("symbol").captureToImage().toPixelMap()
            // Oberhalb des Pfeils, sicher innerhalb der beiden Halbkreise.
            val links = pixel[25,25]
            val rechts = pixel[75,25]
            assertEquals(1f, links.red, .02f)
            assertEquals(0f, links.green, .02f)
            assertEquals(0f, links.blue, .02f)
            assertEquals(1f, rechts.blue, .02f)
            assertEquals(0f, rechts.red, .02f)
            assertEquals(0f, rechts.green, .02f)
        }
    }
}
