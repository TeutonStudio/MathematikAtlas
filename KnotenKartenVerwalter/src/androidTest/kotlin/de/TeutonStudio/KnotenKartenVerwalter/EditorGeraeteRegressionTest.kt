package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditorGeraeteRegressionTest {
 @get:Rule val compose = createComposeRule()
 @Test fun gruppeUndEnthaltenerKnotenBleibenDirektAuswaehlbar() {
  val k = KnotenDaten(art="test",name="A",position=GraphPunkt(160f,160f))
  val g = VisuelleKnotenGruppeDaten(titel="G",knotenIds=setOf(k.id),position=GraphPunkt(100f,80f),größe=GraphGröße(400f,300f))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k),visuelleGruppen=listOf(g)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  compose.setContent { MaterialTheme { KnotenKartenEditor(z,Modifier.size(800.dp,600.dp),zeigeKnotenInspektor=false) } }
  compose.onNodeWithContentDescription("Gruppe G auswählen").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(g.id,z.ausgewählteGruppe) }
  compose.onNodeWithContentDescription("Knoten A, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten); z.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(g.id,true)) }
  compose.onNodeWithContentDescription("Knoten G, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(g.id,z.ausgewählteGruppe) }
 }
 @Test fun kapselEckeWirdNichtAlsKnotenTrefferBehandelt() {
  val k = KnotenDaten(art="test",name="Kapsel",position=GraphPunkt(60f,100f),eigenschaften=mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  compose.setContent { MaterialTheme { KnotenKartenEditor(z,Modifier.size(800.dp,600.dp),zeigeKnotenInspektor=false) } }
  compose.onNodeWithContentDescription("Knoten Kapsel, 0 Anschlüsse").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f,1f)) }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertNull(z.ausgewählterKnoten) }
  compose.onNodeWithContentDescription("Knoten Kapsel, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten) }
 }
}
