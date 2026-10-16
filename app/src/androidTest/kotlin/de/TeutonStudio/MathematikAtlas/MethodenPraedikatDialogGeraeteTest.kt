package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import org.junit.Rule
import org.junit.Test

class MethodenPraedikatDialogGeraeteTest {
    @get:Rule val compose=createComposeRule()
    @Test fun allgemeinesPraedikatZeigtDiagnoseBeiSchmalerAnsichtUndGrosserSchrift() {
        val m=object : SignaturtragendeMethode {
            override val name="Script"
            override val signatur=MethodenSignatur(emptyList(),listOf(MethodenKomponente("r","r",0,TypAusdruck.Atom(MathematischeTypen.Aussage))))
        }
        val p=AnschlussDaten(name="p",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Methode.id)
        val feld=KartenTabellenAnschluss("p",p.art,p,KnotenDaten(art="test",name="Test",anschlüsse=listOf(p)))
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                    Column(Modifier.width(240.dp).height(260.dp).verticalScroll(rememberScrollState())) {
                        VerbundenerKartenTabellenEingang(feld,BedingterWert(m),{_,standard -> standard},{_,_ ->})
                    }
                }
            }
        }
        compose.onNodeWithText("Für diese Methode fehlen eine mathematische Vorschrift und Definitionsmengen für die Wahrheitstabelle.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Argument 1",substring=true).assertDoesNotExist()
    }
}
