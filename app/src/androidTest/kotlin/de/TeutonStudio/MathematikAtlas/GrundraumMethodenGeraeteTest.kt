package de.TeutonStudio.MathematikAtlas

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AnschlussSymbol
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class GrundraumMethodenGeraeteTest {
    @get:Rule val compose = createComposeRule()
    private val keineAktionen = object : KnotenInspektorAktionen {
        override fun knoten(knoten: KnotenDaten) = Unit
        override fun parameter(schlüssel: String, wert: String) = Unit
        override fun name(wert: String) = Unit
        override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
        override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = Unit
    }

    @Test fun variableZeigtNurGrundraumUndNameAuchBeiGrosserSchrift() {
        val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        var ergebnis by mutableStateOf<KnotenAuswertungsErgebnis?>(null)
        compose.setContent {
            MaterialTheme {
                val dichte = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(dichte, 1.6f)) {
                    Column(Modifier.width(240.dp).verticalScroll(rememberScrollState())) {
                        KnotenInspektorRegister.finde(k.art)!!.Inhalt(k, ergebnis, keineAktionen)
                    }
                }
            }
        }
        compose.onNodeWithText("Grundraum fehlt. Verbinde eine Menge mit dem Grundraumeingang.").assertIsDisplayed()
        compose.onNodeWithText("Struktur", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Komponenten", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Variablenbereich").assertDoesNotExist()
        compose.runOnIdle {
            ergebnis = KnotenAuswertungsErgebnis(emptyMap(), eingänge = mapOf("grundraum" to BedingterWert(
                KartesischerPotenzraum(ReelleZahlen, RationaleZahl.von(2)))))
        }
        compose.onNodeWithText("Struktur und Dimension werden vom Grundraum übernommen.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Grundraum fehlt. Verbinde eine Menge mit dem Grundraumeingang.").assertDoesNotExist()
    }

    @Test fun methodenHalbkreiseZeigenLogischeTypenUndSchraegeFarbteilungen() {
        fun atom(id: String) = TypAusdruck.Atom(TypId(id))
        fun tupel(vararg t: TypAusdruck) = TypAusdruck.Parameterisiert(TypId("typ.tupel"), t.toList())
        val zahl = atom("mathematik.zahl")
        val menge = atom("mathematik.menge")
        val bool = atom("mathematik.aussage")
        fun port(a: TypAusdruck, b: TypAusdruck) = AnschlussDaten(name = "methode", kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.methode"), vertrag = AnschlussVertrag(TypAusdruck.Parameterisiert(TypId("mathematik.methode"), listOf(a, b))))
        val einfach = port(tupel(tupel(zahl, zahl)), tupel(bool))
        val geteilt = port(tupel(TypAusdruck.Vereinigung(listOf(zahl, menge))), tupel(TypAusdruck.Vereinigung(listOf(bool, menge))))
        compose.setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.padding(24.dp).testTag("bild")) {
                        Text("Tupel → Aussage")
                        Box(Modifier.testTag("einfach")) { AnschlussSymbol(einfach, listOf(Color.Gray), 96f, 1f, true) {
                            when (it.art.wert) { "mathematik.tupel" -> Color.Yellow; "mathematik.aussage" -> Color.Green; else -> Color.Gray }
                        } }
                        Text("Zahl/Menge → Aussage/Menge")
                        Box(Modifier.testTag("geteilt")) { AnschlussSymbol(geteilt, listOf(Color.Gray), 96f, 1f, true) {
                            when (it.art.wert) { "mathematik.zahl" -> Color.Red; "mathematik.menge" -> Color.Blue; "mathematik.aussage" -> Color.Green; else -> Color.Gray }
                        } }
                    }
                }
            }
        }
        val p = compose.onNodeWithTag("einfach").captureToImage().toPixelMap()
        assertEquals(Color.Yellow.toArgb(), p[(p.width * .25f).toInt(), (p.height * .25f).toInt()].toArgb())
        assertEquals(Color.Green.toArgb(), p[(p.width * .75f).toInt(), (p.height * .25f).toInt()].toArgb())
        val q = compose.onNodeWithTag("geteilt").captureToImage().toPixelMap()
        fun farben(links: Boolean): Set<Int> = buildSet {
            val von = if (links) .03f else .54f
            val bis = if (links) .46f else .97f
            for (x in (q.width * von).toInt()..(q.width * bis).toInt())
                for (y in (q.height * .03f).toInt()..(q.height * .97f).toInt()) add(q[x,y].toArgb())
        }
        assertTrue(farben(true).containsAll(listOf(Color.Red.toArgb(), Color.Blue.toArgb())))
        assertTrue(farben(false).containsAll(listOf(Color.Green.toArgb(), Color.Blue.toArgb())))
        val datei = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "grundraum-methoden-farben.png")
        datei.outputStream().use { compose.onNodeWithTag("bild").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val stücke = android.util.Base64.encodeToString(datei.readBytes(), android.util.Base64.NO_WRAP).chunked(2800)
        stücke.forEachIndexed { index, stück -> android.util.Log.i("AtlasPruefungBild", "$index/${stücke.size}:$stück") }
    }
}
