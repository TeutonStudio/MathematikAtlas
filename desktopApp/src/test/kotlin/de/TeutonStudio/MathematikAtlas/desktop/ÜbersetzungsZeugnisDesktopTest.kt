package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Rule
import org.junit.Test

class ÜbersetzungsZeugnisDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test fun zeugnisDetailsBleibenBeiSchmalemFensterUndGrosserSchriftErreichbar() {
        val quelle=Tupel(listOf(RationaleZahl.Null,RationaleZahl.Null))
        val r=ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(quelle,ÜbersetzungsZiel.POLAR)) as ÜbersetzungsErgebnis.Erfolg
        val wert=BedingterWert(r.wert,übersetzungen=ÜbersetzungsMetadaten(r.zeugnis,listOf(r.zeugnis),r.kontext))
        val k=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val schrift=mutableStateOf(1f);val dunkel=mutableStateOf(false);val breit=mutableStateOf(false)
        compose.setContent {
            val d=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density,schrift.value)) {
                MaterialTheme(colorScheme=if(dunkel.value) darkColorScheme() else lightColorScheme()) {
                    RechnerAuswertungsschemaInhalt(k,KnotenAuswertungsErgebnis(mapOf("ergebnis" to wert)),Modifier.width(if(breit.value) 800.dp else 390.dp).height(if(breit.value) 390.dp else 600.dp))
                }
            }
        }
        for(s in listOf(1f,2f)) for(d in listOf(false,true)) for(b in listOf(false,true)) {
            compose.runOnIdle { schrift.value=s;dunkel.value=d;breit.value=b }
            compose.onNodeWithText("Übersetzungszeugnis").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Numerische Näherung; keine zertifizierte Fehlergrenze").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Herkunft:",substring=true).performScrollTo().assertIsDisplayed()
        }
    }
}
