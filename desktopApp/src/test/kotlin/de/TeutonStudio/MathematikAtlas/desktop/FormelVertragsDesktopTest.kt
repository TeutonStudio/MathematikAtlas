package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

class FormelVertragsDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test fun ungueltigerImportErhaeltEntwurfUndAbbrechenAendertNichts() {
        val start=FormelLatexCodec.importiereGebunden("x+1",listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)))
        var übernommen:GebundeneFormel?=null;var geschlossen=false
        compose.setContent {MaterialTheme {GebundenerFormelBauerDialog(start,null,{geschlossen=true}) {f,_->übernommen=f}}}
        compose.onNodeWithText("Formel übernehmen").assertIsEnabled()
        compose.onNodeWithText("LaTeX mit deklarierten Symbolen").performTextReplacement("\\frac{")
        compose.onNodeWithText("Formel übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.runOnIdle {assertTrue(geschlossen);assertNull(übernommen);assertEquals("qx",start.quellen.single().id)}
    }
    @Test fun nichtUebernommenerSymbolnameSperrtGlobaleUebernahme() {
        val start=FormelLatexCodec.importiereGebunden("x",listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)))
        compose.setContent {MaterialTheme {GebundenerFormelBauerDialog(start,null,{}) {_,_->}}}
        compose.onNodeWithText("Symbolname").performScrollTo().performTextReplacement("z")
        compose.onNodeWithText("Formel übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Namen übernehmen").performScrollTo().performClick()
        compose.onNodeWithText("Formel übernehmen").assertIsEnabled()
    }
}
