package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

class VerbindungsAuswahlDesktopTest {
    @get:Rule val compose = createComposeRule()
    private val laufzeit = MathematikKartenLaufzeit()
    private val quelle = KnotenDaten(art="test",name="Quelle",position=GraphPunkt(80f,150f),anschlüsse=listOf(
        AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Matrix.id)))
    private val ziel = KnotenDaten(art="test",name="Ziel",position=GraphPunkt(700f,150f),anschlüsse=listOf(
        AnschlussDaten(name="wert",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Methode.id)))
    private val basis = KartenDaten(name="Dialog",knoten=listOf(quelle,ziel))
    private fun zeige(): KartenEditorZustand {
        val editor=KartenEditorZustand(basis,laufzeit.graphPrüfung)
        editor.verbindungsPlaner={ k,v,z -> planeÜbersetzerVerbindungen(k,v,z,laufzeit.graphPrüfung) }
        compose.setContent { MaterialTheme { KnotenKartenEditor(editor,Modifier.size(1200.dp,750.dp),zeigeKnotenInspektor=false) } }
        compose.onNode(hasContentDescription("Quelle, Anschluss wert",substring=true)).performClick()
        compose.onNode(hasContentDescription("Ziel, Anschluss wert",substring=true)).performClick()
        compose.onNodeWithText("Verbindungsweg auswählen").assertIsDisplayed()
        return editor
    }
    @Test fun klickAufPortsOeffnetAuswahlUndUebernimmtDenGewaehltenUebersetzer() {
        val editor=zeige()
        compose.runOnIdle { assertEquals(basis,editor.karte) }
        compose.onNodeWithText("Matrix zerlegen").performScrollTo().performClick()
        compose.runOnIdle {
            val neu=editor.karte.knoten.single { it.art==ÜBERSETZER_ART }
            assertEquals("matrixzerlegen",übersetzerZiel(neu));assertEquals(2,editor.karte.verbindungen.size)
            editor.rückgängig();assertEquals(basis,editor.karte)
        }
    }
    @Test fun abbrechenLaesstDieKarteUnveraendert() {
        val editor=zeige()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.runOnIdle { assertEquals(basis,editor.karte);assertFalse(editor.kannRückgängig()) }
    }
}
