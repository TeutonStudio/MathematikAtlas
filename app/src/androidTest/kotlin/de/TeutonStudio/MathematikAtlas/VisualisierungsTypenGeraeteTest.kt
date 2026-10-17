package de.TeutonStudio.MathematikAtlas

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
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VisualisierungsTypenGeraeteTest {
    @get:Rule val compose=createComposeRule()
    @Test fun aequivalenteRaeumeZeigenFlaecheInBeidenThemesBeiSchriftfaktorZwei() {
        val r=ReelleZahlen
        val zwei=RationaleZahl.von(2)
        val räume=listOf<MengenAusdruck>(KartesischesProdukt(listOf(r,r)),kartesischePotenz(r,zwei),Tupelraum(listOf(r,r)),
            Vektorraum(VektorOrientierung.Spalte,2,r),Matrizenraum(1,2,r),Tensorraum(r,listOf(zwei)))
        var index by mutableIntStateOf(0)
        var dunkel by mutableStateOf(false)
        val config=VisualisierungsKonfiguration(achsenSichtbar=false,rasterSichtbar=false,
            ebenen=mapOf("menge" to EbenenStil(name="Gebiet",farbe=0xFF2563EB,deckkraft=1.0)))
        val knoten=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=config.zuEigenschaften())
        compose.setContent {
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
            compose.runOnIdle { dunkel=theme;index=i }
            compose.waitUntil(timeoutMillis=15_000) {
                compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() &&
                    compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()
            }
            compose.waitForIdle()
            val bild=compose.onNodeWithTag("visualisierung-plot").captureToImage().toPixelMap()
            var blau=0
            for(y in 0 until bild.height step 3) for(x in 0 until bild.width step 3) {
                val p=bild[x,y]
                if(p.blue > p.red+.15f && p.blue > p.green+.1f) blau++
            }
            assertTrue("Keine sichtbare Fläche für ${räume[i]} (dunkel=$theme): $blau",blau>50)
        }
    }

    @Test fun einzelProjektionUndRaumwechselBleibenAuchInDerOberflaecheGueltig()  {
        var config by mutableStateOf(VisualisierungsKonfiguration().copy(bereiche=VisualisierungsKonfiguration().bereiche.copy(z=null)))
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                    Column(Modifier.size(360.dp,400.dp).verticalScroll(rememberScrollState())) {
                        VisualisierungsEinstellungen(config,config.dimension,{config=it})
                    }
                }
            }
        }
        compose.onNodeWithText("Ausgabeprojektion").performScrollTo().performTextReplacement("4")
        compose.runOnIdle {
            assertEquals(RaumDimension.R1,config.dimension)
            assertEquals(listOf(4),config.ausgabeKoordinaten)
            assertEquals(config.ausgabeKoordinaten,VisualisierungsKonfiguration.aus(config.zuEigenschaften()).ausgabeKoordinaten)
        }
        compose.onNodeWithText("R3").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(RaumDimension.R3,config.dimension)
            assertEquals(listOf("x","y","z"),listOfNotNull(config.achsen.x,config.achsen.y,config.achsen.z))
            assertNotNull(config.bereiche.z)
            assertTrue(config.ausgabeKoordinaten.isEmpty())
        }
    }
}
