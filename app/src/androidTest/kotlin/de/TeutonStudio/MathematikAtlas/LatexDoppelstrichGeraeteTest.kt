package de.TeutonStudio.MathematikAtlas

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LatexDoppelstrichGeraeteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun doppelstrichUnterscheidetSichVonAufrechterSchriftImHellenTheme() = prüfeGlyphen(false)
    @Test fun doppelstrichUnterscheidetSichVonAufrechterSchriftImDunklenTheme() = prüfeGlyphen(true)

    private fun prüfeGlyphen(dunkel: Boolean) {
        val hintergrund = if (dunkel) darkColorScheme().surface else lightColorScheme().surface
        compose.setContent {
            MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                Surface {
                    Column(Modifier.padding(16.dp).testTag("glyphen")) {
                        Text("Aufrecht / Doppelstrich / ohne Argumentklammern")
                        for (buchstabe in "NZQRCH") {
                            Row {
                                listOf("\\mathrm{$buchstabe}", "\\mathbb{$buchstabe}", "\\mathbb $buchstabe")
                                    .forEachIndexed { index, latex ->
                                        Box(Modifier.size(72.dp, 48.dp).testTag("$buchstabe-$index"), Alignment.Center) {
                                            LatexText(latex, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 30.sp))
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
        for (buchstabe in "NZQRCH") {
            val bilder = (0..2).map { index ->
                val tag = "$buchstabe-$index"
                warteAufFormel(tag, hintergrund.toArgb())
                compose.onNodeWithTag(tag).captureToImage().pixel()
            }
            // Auch ein aufrechtes R war im fehlerhaften OTF-Pfad identisch zu mathbb{R}.
            assertFalse("$buchstabe muss Doppelstrichglyphen besitzen", bilder[0].contentEquals(bilder[1]))
            assertArrayEquals("Beide mathbb-Schreibweisen müssen gleich aussehen", bilder[1], bilder[2])
        }
        speichereBild("glyphen", "mathbb-glyphen-${if (dunkel) "dunkel" else "hell"}")
    }

    @Test fun mengenKnotenUndGemeinsameFormellayoutsInBeidenThemes() {
        val knoten = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val ergebnis = GesamterMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()),
        )
        assertNull(ergebnis.fehler)
        val renderer = MathematikKnotenRenderer(ergebnisFür = { ergebnis })
        val aktionen = object : KnotenRendererAktionen {
            override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
        }
        val formeln = listOf(
            "\\mathbb{R}^2\\quad\\mathbb N_0\\quad x\\in\\mathbb{R}",
            "\\frac{1}{2}+\\frac{x+1}{x-1}",
            "\\left(\\begin{pmatrix}1&2\\\\3&4\\end{pmatrix}\\right)",
            "f:\\begin{cases}\\mathbb R\\longrightarrow\\mathbb C\\\\x\\mapsto\\frac{x}{2}\\end{cases}",
            "\\int_{x\\in\\mathbb R}^{b}f(x)",
        )
        var dunkel by mutableStateOf(false)
        compose.setContent {
            MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                Surface {
                    Column(Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()).padding(16.dp).testTag("galerie"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(260.dp, 110.dp).testTag("menge")) {
                            renderer.Inhalt(knoten, false, aktionen)
                        }
                        formeln.forEachIndexed { index, latex ->
                            Box(Modifier.testTag("formel-$index")) { LatexFormel(latex) }
                        }
                    }
                }
            }
        }
        for (theme in listOf(false, true)) {
            compose.runOnIdle { dunkel = theme }
            val hintergrund = (if (theme) darkColorScheme() else lightColorScheme()).surface.toArgb()
            compose.onNodeWithText("Menge").assertIsDisplayed()
            for (index in formeln.indices) {
                val tag = "formel-$index"
                compose.onNodeWithTag(tag).performScrollTo()
                warteAufFormel(tag, hintergrund)
            }
            compose.onNodeWithTag("menge").performScrollTo()
            speichereBild("galerie", "mathbb-formeln-${if (theme) "dunkel" else "hell"}")
        }
    }

    private fun warteAufFormel(tag: String, hintergrund: Int) {
        compose.waitUntil(timeoutMillis = 10_000) {
            val bild = compose.onNodeWithTag(tag).captureToImage()
            bild.width > 0 && bild.height > 0 && bild.pixel().any { it != hintergrund }
        }
    }

    private fun ImageBitmap.pixel(): IntArray {
        val bild = toPixelMap()
        return IntArray(width * height) { bild[it % width, it / width].toArgb() }
    }

    private fun speichereBild(tag: String, name: String) {
        val datei = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "$name.png")
        datei.outputStream().use {
            compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        // Connected-Tests deinstallieren die isolierte App; Bilder deshalb auch im Testlog erhalten.
        val stücke = android.util.Base64.encodeToString(datei.readBytes(), android.util.Base64.NO_WRAP).chunked(2800)
        stücke.forEachIndexed { index, stück -> android.util.Log.i("AtlasLatexBild", "$name:$index/${stücke.size}:$stück") }
    }
}
