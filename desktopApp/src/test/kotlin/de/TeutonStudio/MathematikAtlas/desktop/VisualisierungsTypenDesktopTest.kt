package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class VisualisierungsTypenDesktopTest {
    @Test fun aequivalenteRaeumeZeigenFlaecheInBeidenThemesBeiSchriftfaktorZwei() = runDesktopComposeUiTest(width=400,height=320) {
        val r=ReelleZahlen
        val zwei=RationaleZahl.von(2)
        val räume=listOf<MengenAusdruck>(KartesischesProdukt(listOf(r,r)),kartesischePotenz(r,zwei),Tupelraum(listOf(r,r)),
            Vektorraum(VektorOrientierung.Spalte,2,r),Matrizenraum(1,2,r),Tensorraum(r,listOf(zwei)))
        var index by mutableIntStateOf(0)
        var dunkel by mutableStateOf(false)
        val config=VisualisierungsKonfiguration(achsenSichtbar=false,rasterSichtbar=false,
            ebenen=mapOf("menge" to EbenenStil(name="Gebiet",farbe=0xFF2563EB,deckkraft=1.0)))
        val knoten=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=config.zuEigenschaften())
        setContent {
            MaterialTheme(colorScheme=if(dunkel) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                    val raum=räume[index]
                    val filter=GefilterteMenge(raum,MathematischeMethode("P",listOf(AllgemeinerParameter("p")),
                        WahrheitsKonstante(true),WahrheitsMenge,werteVorräte=mapOf("p" to raum)))
                    val ergebnis=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(filter)))
                    Surface { VisualisierungsDetailansicht(knoten,ergebnis,{},Modifier.size(360.dp,280.dp)) }
                }
            }
        }
        for(theme in listOf(false,true)) for(i in räume.indices) {
            runOnIdle { dunkel=theme;index=i }
            waitUntil(timeoutMillis=15_000) {
                onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() &&
                    onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()
            }
            waitForIdle()
            val bild=onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
            var blau=0
            for(y in 0 until bild.height step 3) for(x in 0 until bild.width step 3) {
                val p=bild[x,y]
                if(p.blue > p.red+.15f && p.blue > p.green+.1f) blau++
            }
            assertTrue(blau>50,"Keine sichtbare Fläche für ${räume[i]} (dunkel=$theme): $blau")
        }
    }

    @Test fun einzelProjektionUndRaumwechselBleibenAuchInDerOberflaecheGueltig() = runDesktopComposeUiTest(width=400,height=450) {
        var config by mutableStateOf(VisualisierungsKonfiguration().copy(bereiche=VisualisierungsKonfiguration().bereiche.copy(z=null)))
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                    Column(Modifier.size(360.dp,400.dp).verticalScroll(rememberScrollState())) {
                        VisualisierungsEinstellungen(config,config.dimension,{config=it})
                    }
                }
            }
        }
        onNodeWithText("Ausgabeprojektion").performScrollTo().performTextReplacement("4")
        runOnIdle {
            assertEquals(RaumDimension.R1,config.dimension)
            assertEquals(listOf(4),config.ausgabeKoordinaten)
            assertEquals(config.ausgabeKoordinaten,VisualisierungsKonfiguration.aus(config.zuEigenschaften()).ausgabeKoordinaten)
        }
        onNodeWithText("R3").performScrollTo().performClick()
        runOnIdle {
            assertEquals(RaumDimension.R3,config.dimension)
            assertEquals(listOf("x","y","z"),listOfNotNull(config.achsen.x,config.achsen.y,config.achsen.z))
            assertNotNull(config.bereiche.z)
            assertTrue(config.ausgabeKoordinaten.isEmpty())
        }
    }
}
