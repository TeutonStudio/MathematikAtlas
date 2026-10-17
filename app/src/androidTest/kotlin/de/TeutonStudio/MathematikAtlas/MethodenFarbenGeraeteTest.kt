package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
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
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import androidx.compose.ui.test.junit4.createComposeRule

class MethodenFarbenGeraeteTest {
    @get:Rule val compose = createComposeRule()
    @Test fun halbkreisfarbenBleibenBeiInkompatibilitaetUndThemewechselErhalten() {
        var kompatibel by mutableStateOf(true)
        var dunkel by mutableStateOf(false)
        var schrift by mutableFloatStateOf(1f)
        val anschluss = AnschlussDaten(name = "f", art = AnschlussArtId("mathematik.methode"), kante = AnschlussKante.Rechts,
            vertrag = AnschlussVertrag(TypAusdruck.Parameterisiert(TypId("mathematik.methode"), listOf(
                TypAusdruck.Atom(TypId("zahl")), TypAusdruck.Atom(TypId("menge"))))))
        compose.setContent {
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
            compose.runOnIdle { dunkel = theme; schrift = skala; kompatibel = aktiv }
            compose.waitForIdle()
            val pixel = compose.onNodeWithTag("symbol").captureToImage().toPixelMap()
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
    @Test fun legendeZeigtBeideSignaturenUndSchliesstOhneKartenaenderung() {
        lateinit var atlas: AtlasZustand
        var offen by mutableStateOf(true)
        var dunkel by mutableStateOf(false)
        var schrift by mutableFloatStateOf(1f)
        fun port(argument: String, ausgabe: String) = AnschlussDaten(name = "f", kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.methode"), vertrag = AnschlussVertrag(TypAusdruck.Parameterisiert(
                TypId("mathematik.methode"), listOf(TypAusdruck.Atom(TypId(argument)), TypAusdruck.Atom(TypId(ausgabe))))))
        val karte = KartenDaten(name = "Legendentest", knoten = listOf(KnotenDaten(art = "test", name = "Methoden",
            anschlüsse = listOf(port("mathematik.zahl", "mathematik.menge"), port("mathematik.menge", "mathematik.zahl")))))
        compose.setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            atlas = remember { AtlasZustand(object : android.content.ContextWrapper(context) {
                private val basis = java.io.File(context.cacheDir, "pmm-legende-${java.util.UUID.randomUUID()}")
                override fun getFilesDir() = java.io.File(basis, "files").apply { mkdirs() }
                override fun getNoBackupFilesDir() = java.io.File(basis, "no-backup").apply { mkdirs() }
            }).also { it.editor.ersetzeKarte(karte) } }
            val dichte = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(dichte.density, schrift)) {
                MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                    if (offen) AnschlussLegendenDialog(atlas) { offen = false }
                }
            }
        }
        for (theme in listOf(false, true)) for (skala in listOf(1f, 2f)) {
            compose.runOnIdle { offen = true; dunkel = theme; schrift = skala }
            compose.onNodeWithText("Methode: Zahl → Menge").assertExists()
            compose.onNodeWithText("Methode: Menge → Zahl").assertExists()
            compose.onNodeWithText("Schließen").assertIsDisplayed()
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.cacheDir, "pmm-legende-$theme-$skala.png").outputStream().use { out ->
                compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            compose.onNodeWithText("Schließen").performClick()
            compose.onNodeWithText("Legende").assertDoesNotExist()
            compose.runOnIdle { assertEquals(karte, atlas.editor.karte) }
        }
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

}
