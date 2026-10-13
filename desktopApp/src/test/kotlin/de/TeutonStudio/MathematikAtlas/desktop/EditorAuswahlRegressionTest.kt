package de.TeutonStudio.MathematikAtlas.desktop
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import de.TeutonStudio.MathematikKnoten.MathematikKnotenRenderer
import de.TeutonStudio.MathematikKnoten.GeometrieVisualisierungsKnotenRenderer
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.*
class EditorAuswahlRegressionTest {
 private fun assertKeinTextUeberlauf(node: SemanticsNodeInteraction) {
  val result = mutableListOf<TextLayoutResult>()
  node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
  assertTrue(result.isNotEmpty())
  assertTrue(result.none { it.didOverflowHeight || (0 until it.lineCount).any { zeile -> it.getLineRight(zeile) > it.size.width + 1f } }, result.joinToString { "${it.layoutInput.text.text}: size=${it.size}, constraints=${it.layoutInput.constraints}, overflowH=${it.didOverflowHeight}, overflowW=${it.didOverflowWidth}, paragraph=${it.multiParagraph.width}, right=${it.getLineRight(it.lineCount-1)}, last=${it.getLineBottom(it.lineCount-1)}" })
 }
 @Test fun gruppenkopfSkaliertAlsGanzesUndLangeFusszeilePasstInKapsel() {
  val titel = "Eine Gruppe mit einem sehr langen vollständig lesbaren Titel"
  val k = KnotenDaten(art="test", name="K", position=GraphPunkt(100f,180f),
   eigenschaften=mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")))
  val g = VisuelleKnotenGruppeDaten(titel=titel,knotenIds=setOf(k.id),position=GraphPunkt(50f,80f),größe=GraphGröße(180f,110f))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k),visuelleGruppen=listOf(g),ansicht=AnsichtsFenster(zoom=0.5f)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  val renderer = MathematikKnotenRenderer(ergebnisFür={KnotenAuswertungsErgebnis(emptyMap(),auswertungsDauerNanos=Long.MAX_VALUE)})
  compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f,2f)) {
   KnotenKartenEditor(z,Modifier.size(1200.dp,900.dp),rendererFür={renderer},zeigeKnotenInspektor=false)
  } } }
  compose.waitForIdle()
  assertKeinTextUeberlauf(compose.onNodeWithText(titel))
  assertKeinTextUeberlauf(compose.onNode(hasContentDescription("Letzte Auswertungsdauer",substring=true)))
  compose.runOnIdle {
   val node = z.darstellungsKarte.knoten.single()
   val gruppe = z.darstellungsKarte.visuelleGruppen.single()
   assertTrue(node.position.y + node.größe.höhe <= gruppe.position.y + gruppe.größe.höhe - 24f)
  }
 }
 @Test fun produktiverViewportMisstBedienungUndDiagnoseBeiGrosserSchrift() {
  val k = KnotenDaten(art="mathematik.visualisierung",name="Visualisierung",größe=GraphGröße(100f,80f))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  val renderer = GeometrieVisualisierungsKnotenRenderer { null }
  compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f,2f)) {
   KnotenKartenEditor(z,Modifier.size(1200.dp,900.dp),rendererFür={renderer},zeigeKnotenInspektor=false)
  } } }
  compose.waitForIdle()
  assertKeinTextUeberlauf(compose.onNodeWithText("Geometrievisualisierung"))
  assertKeinTextUeberlauf(compose.onNodeWithText("Verbinde ein geometrisches Objekt."))
  compose.runOnIdle { assertTrue(z.darstellungsKarte.knoten.single().größe.höhe > 300f) }
 }

 @Test fun kapselEckenGehoerenNichtZurAuswahlflaeche() {
  val k = KnotenDaten(art="test", name="Kapsel", position=GraphPunkt(60f,100f),
   eigenschaften=mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  compose.setContent { MaterialTheme { KnotenKartenEditor(z,Modifier.size(800.dp,600.dp),zeigeKnotenInspektor=false) } }
  compose.onNodeWithContentDescription("Knoten Kapsel, 0 Anschlüsse").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f,1f)) }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertNull(z.ausgewählterKnoten) }
  compose.onNodeWithContentDescription("Knoten Kapsel, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten) }
 }
 @Test fun inhaltUndGrosseSchriftBestimmenGeometrieUnabhaengigVomZoom() {
  val k = KnotenDaten(art="test", name="Lang", position=GraphPunkt(60f,100f), größe=GraphGröße(30f,20f))
  val z = KartenEditorZustand(KartenDaten(name="Test", knoten=listOf(k)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  val renderer = object : KnotenRenderer {
   @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
    Column { Text("Ein langer Titel muss vollständig dargestellt werden"); Text("Zeile zwei"); Text("Zeile drei") }
   }
  }
  compose.setContent { MaterialTheme {
   CompositionLocalProvider(LocalDensity provides Density(1f,2f)) {
    KnotenKartenEditor(z,Modifier.size(800.dp,600.dp),rendererFür={renderer},zeigeKnotenInspektor=false)
   }
  } }
  compose.waitForIdle()
  lateinit var vorher: GraphGröße
  compose.runOnIdle {
   vorher = z.darstellungsKarte.knoten.single().größe
   assertTrue(vorher.breite >= 360f); assertTrue(vorher.höhe >= 192f)
   assertEquals(GraphGröße(30f,20f),z.karte.knoten.single().größe)
   z.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(zoom=0.5f)),false)
  }
  compose.waitForIdle()
  compose.runOnIdle { assertEquals(vorher,z.darstellungsKarte.knoten.single().größe) }
 }

 @Test fun gruppenDragVerwendetLokaleKoordinatenBeiZoom() {
  val g = VisuelleKnotenGruppeDaten(titel="G",position=GraphPunkt(100f,100f),größe=GraphGröße(400f,300f))
  val z = KartenEditorZustand(KartenDaten(name="Test",visuelleGruppen=listOf(g)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDensity provides Density(1f,1f)) {
   KnotenKartenEditor(z,Modifier.size(1200.dp,900.dp),zeigeKnotenInspektor=false)
  } } }
  for (zoom in listOf(0.5f,2f)) {
   compose.runOnIdle { z.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(zoom=zoom)),false) }
   compose.waitForIdle()
   val vorher = z.karte.visuelleGruppen.single().position
   compose.onNodeWithContentDescription("Gruppe G auswählen").performTouchInput {
    down(center); moveBy(androidx.compose.ui.geometry.Offset(60f,0f),delayMillis=100); up()
   }
   compose.waitForIdle()
   compose.runOnIdle { assertEquals(60f/zoom,z.karte.visuelleGruppen.single().position.x-vorher.x,1f) }
  }
 }

 @get:Rule val compose = createComposeRule()
 @Test fun klickWaehltKnotenAuchNachFaltung() {
  val k = KnotenDaten(id=KnotenId("a"), art="test",name="A", position=GraphPunkt(160f,160f), größe=GraphGröße(180f,100f))
  val g = VisuelleKnotenGruppeDaten(id=VisuelleGruppenId("g"),titel="G",knotenIds=setOf(k.id),position=GraphPunkt(100f,80f),größe=GraphGröße(400f,300f))
  val z = KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(k),visuelleGruppen=listOf(g)),GraphPrüfung(AnschlussArtRegister(emptyList())))
  compose.setContent { MaterialTheme { KnotenKartenEditor(z,Modifier.size(800.dp,600.dp),zeigeKnotenInspektor=false) } }
  compose.onNodeWithText("G").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(g.id,z.ausgewählteGruppe) }
  compose.onNodeWithContentDescription("Knoten A, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten); z.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(g.id,true)) }
  compose.waitForIdle()
  compose.onNodeWithContentDescription("Knoten G, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(g.id,z.ausgewählteGruppe); z.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(g.id,false)) }
  compose.waitForIdle()
  compose.onNodeWithContentDescription("Knoten A, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten) }
  compose.onNodeWithContentDescription("Gruppe G auswählen").performTouchInput { doubleClick() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(g.id,z.geöffneteGruppe) }
  compose.onNodeWithContentDescription("Knoten A, 0 Anschlüsse").performTouchInput { click() }
  compose.mainClock.advanceTimeBy(600)
  compose.runOnIdle { assertEquals(k.id,z.ausgewählterKnoten) }
 }
}
