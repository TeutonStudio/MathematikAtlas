package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Native Skia-CPU-Zeichenzeit und Test-Eingabe bis Bild; keine Display-/Present-Framezeit. */
class VisualisierungsLeistungsDesktopTest {
    @get:Rule val compose=createComposeRule()
    @Test fun messeNativeDarstellung() {
        assumeTrue(System.getProperty("atlas.measureVisualizationDesktop")=="true")
        val x=Variable("x");val y=Variable("y")
        val graph=Methode(name="f",parameter=listOf(x),vorschrift=Sinus(multiplikation(RationaleZahl.von(9),x)),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)).graphMenge()
        val region=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),Vergleich(y,VergleichsArt.Kleiner,multiplikation(x,x)))
        val fläche=Methode(name="f",parameter=listOf(x,y),vorschrift=addition(multiplikation(x,x),multiplikation(y,y)),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)).graphMenge()
        var knoten by mutableStateOf(MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero))
        var eingang by mutableStateOf<KnotenAuswertungsErgebnis?>(null)
        val zeichnen=Collections.synchronizedList(mutableListOf<Double>())
        val erfassen=AtomicBoolean(false)
        val dreiecke=Collections.synchronizedList(mutableListOf<Int>())
        compose.setContent { MaterialTheme {
            Box(Modifier.size(600.dp,400.dp)) {
                VisualisierungsDetailansicht(knoten,eingang,{ knoten=knoten.copy(eigenschaften=it) },Modifier.fillMaxSize(),
                    beiZeichenDauer={ zeit,anzahl -> if(erfassen.get()) {zeichnen.add(zeit/1e6);dreiecke.add(anzahl)} })
            }
        } }
        fun warte() { compose.waitUntil(30_000) {
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Darstellung fertig")).fetchSemanticsNodes().isNotEmpty()
        } }
        fun p95(l:List<Double>)=l.sorted().let { if(it.isEmpty())JSONObject.NULL else it[((it.size-1)*.95).toInt()] }
        val messungen=JSONArray()
        val laufzahl=System.getProperty("atlas.desktopMeasureRepeats","3").toInt().coerceAtLeast(1)
        val anfragen=System.getProperty("atlas.desktopMeasureRequests","20").toInt().coerceAtLeast(1)
        val gesten=System.getProperty("atlas.desktopMeasureInputs","300").toInt().coerceAtLeast(1)
        for((name,mengen) in listOf("Kurve" to listOf(graph),"KurveUndGebiet" to listOf(graph,region),"Flaeche3D" to listOf(fläche)).filter { System.getProperty("atlas.desktopMeasureCases")?.split(",")?.contains(it.first)!=false }) {
            val c=VisualisierungsKonfiguration(dimension=if(name=="Flaeche3D")RaumDimension.R3 else RaumDimension.R2,dimensionsModus=DimensionsModus.Manuell)
            repeat(laufzahl) { lauf ->
                val kalt=mutableListOf<Double>()
                repeat(anfragen) { index ->
                    val start=System.nanoTime()
                    compose.runOnIdle {
                        val angepasst=c.copy(bereiche=c.bereiche.copy(x=ZahlenBereich(-10.0-(index+lauf*20)*.01,10.0+(index+lauf*20)*.01)))
                        knoten=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=angepasst.zuEigenschaften())
                        eingang=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mengen.mapIndexed { i,m -> (if(i==0) "menge" else "menge${i+1}") to BedingterWert(m) }.toMap())
                    }
                    warte();compose.onNodeWithTag("visualisierung-plot").captureToImage()
                    kalt+=(System.nanoTime()-start)/1e6
                }
                zeichnen.clear();dreiecke.clear()
                val bisBild=mutableListOf<Double>()
                val node=compose.onNodeWithTag("visualisierung-plot")
                node.performTouchInput {down(center)}
                repeat(gesten+5) { index ->
                    erfassen.set(index>=5)
                    val start=System.nanoTime()
                    node.performTouchInput {advanceEventTime(16);moveTo(center+Offset((index%100)*.3f,(index%80)*.1f))}
                    node.captureToImage()
                    if(index>=5) bisBild+=(System.nanoTime()-start)/1e6
                }
                erfassen.set(false);node.performTouchInput {up()};warte()
                messungen.put(JSONObject().put("fall",name).put("lauf",lauf+1).put("kalteAnfragen",kalt.size)
                    .put("detailBisBildP95Ms",p95(kalt)).put("eingaben",bisBild.size).put("eingabeBisBildP95Ms",p95(bisBild))
                    .put("maximaleInteraktionsDreiecke",dreiecke.maxOrNull() ?: 0)
                    .put("zeichenAufrufe",zeichnen.size).put("zeichenCpuP95Ms",p95(zeichnen.toList())))
            }
        }
        java.io.File(System.getProperty("atlas.desktopMeasureOutput","/tmp/atlas-visual-optimierung/desktop-messung.json")).writeText(
            JSONObject().put("plattform",System.getProperty("os.name")+" "+System.getProperty("os.arch"))
                .put("hinweis","Native Compose/Skia-Tests; CPU-Zeichnung und Test-Eingabe bis Capture enthalten Testaufwand, keine Display-Present-Framezeit. Detailmessung kann Cachetreffer enthalten.")
                .put("messungen",messungen).toString(2))
    }
}
