package de.TeutonStudio.MathematikAtlas

import android.graphics.Bitmap
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
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
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Opt-in-Messung; keine zeitabhängige Unit-Testassertion und kein behauptetes Hardwarelimit. */
class VisualisierungsLeistungsTest {
    private val compose=createAndroidComposeRule<ComponentActivity>()
    // Die Annahme muss außerhalb der Compose-Coroutine geprüft werden, damit
    // AndroidJUnitRunner die Opt-in-Messung als übersprungen klassifiziert.
    @get:Rule val regeln: TestRule = RuleChain.outerRule(TestRule { basis, _ ->
        object : Statement() {
            override fun evaluate() {
                assumeTrue(InstrumentationRegistry.getArguments().getString("atlas.measureVisualization") == "true")
                basis.evaluate()
            }
        }
    }).around(compose)
    @Test fun vergleicheRendererAufDerselbenHardware() {
        val frames=Collections.synchronizedList(mutableListOf<Long>())
        val drawFrames=Collections.synchronizedList(mutableListOf<Long>())
        val layoutFrames=Collections.synchronizedList(mutableListOf<Long>())
        val erfassen=AtomicBoolean(false)
        val thread=HandlerThread("AtlasFrameMessung").also{it.start()}
        val listener=android.view.Window.OnFrameMetricsAvailableListener { _, metrics, _ -> if(erfassen.get()) { frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION)); drawFrames.add(metrics.getMetric(FrameMetrics.DRAW_DURATION));layoutFrames.add(metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)) } }
        compose.runOnUiThread {compose.activity.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
        var renderer by mutableStateOf<KnotenRenderer?>(null)
        var knoten by mutableStateOf(MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero))
        var generation by mutableStateOf(0)
        val aktionen=object:KnotenRendererAktionen {override fun eigenschaftenErsetzen(eigenschaften:Map<String,KnotenEigenschaft>){knoten=knoten.copy(eigenschaften=eigenschaften)}}
        compose.setContent {MaterialTheme {Box(Modifier.size(900.dp,650.dp).testTag("perf-surface")){key(generation){renderer?.Inhalt(knoten,false,aktionen)}}}}
        val resultate=JSONArray()
        fun bytes()=Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull()?:0L
        try {
            for(baseline in if(InstrumentationRegistry.getArguments().getString("atlas.measureBaseline")=="false")listOf(false)else listOf(true,false)) for(detail in listOf(false,true)) for(dimension in listOf(RaumDimension.R2,RaumDimension.R3)) {
                val zeiten=mutableListOf<Double>();var allokationen=0L
                repeat(5){index ->
                    val x=Variable("x");val y=Variable("y")
                    val parameter=if(dimension==RaumDimension.R3)listOf(x,y)else listOf(x)
                    val vorschrift=if(dimension==RaumDimension.R3)addition(multiplikation(x,x),multiplikation(y,y),RationaleZahl.von((index+1).toLong()))else addition(x,RationaleZahl.von((index+1).toLong()))
                    val menge=Abbild(if(parameter.size==2)KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))else ReelleZahlen,
                        Methode(name="f",parameter=parameter,vorschrift=vorschrift,zielMenge=ReelleZahlen,werteVorräte=parameter.associate{it.name to ReelleZahlen}))
                    val ergebnis=KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("menge" to BedingterWert(menge)))
                    val callback:(KnotenDaten)->KnotenAuswertungsErgebnis?={ergebnis}
                    val r=if(baseline) {
                        val name="de.TeutonStudio.MathematikKnoten.visualisierung.baseline.ui."+if(detail)"BaselineDetailRenderer"else"VisualisierungsKnotenRenderer"
                        val typ=runCatching{Class.forName(name)}.getOrNull()?:return@repeat
                        typ.constructors.single().newInstance(callback) as KnotenRenderer
                    }else if(!detail) VisualisierungsKnotenRenderer(callback) else object:KnotenRenderer {
                        @Composable override fun Inhalt(k:KnotenDaten,ausgewählt:Boolean,a:KnotenRendererAktionen){VisualisierungsDetailansicht(k,ergebnis,{a.eigenschaftenErsetzen(it)},Modifier.fillMaxSize())}
                    }
                    val c=VisualisierungsKonfiguration(dimension=dimension,dimensionsModus=DimensionsModus.Manuell,methodenModus=MethodenDarstellungsModus.Funktionsgraph)
                    val vorher=bytes();val start=System.nanoTime()
                    compose.runOnIdle {knoten=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften=c.zuEigenschaften());renderer=r;generation++}
                    compose.waitUntil(30_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()}
                    compose.waitForIdle()
                    zeiten+=(System.nanoTime()-start)/1e6;allokationen+=(bytes()-vorher)
                }
                if(zeiten.isEmpty())continue
                frames.clear();drawFrames.clear();layoutFrames.clear()
                if(detail) {
                    val node=compose.onNodeWithTag("perf-surface")
                    val center=node.fetchSemanticsNode().size.let{Offset(it.width*.3f,it.height*.5f)}
                    node.performTouchInput {down(center)}
                    repeat(50){index ->if(index==5)erfassen.set(true);node.performTouchInput {advanceEventTime(16);moveTo(center+Offset(index*3f,index*1.1f))};Thread.sleep(16)}
                    erfassen.set(false);node.performTouchInput {up()};compose.waitForIdle()
                }
                erfassen.set(false)
                val sortiert=synchronized(frames){frames.toList().sorted()}
                fun percentil(werte:MutableList<Long>):Any {val w=synchronized(werte){werte.toList().sorted()};return if(w.isEmpty())JSONObject.NULL else w[((w.size-1)*.95).toInt()]/1e6}
                val p95=if(sortiert.isEmpty())JSONObject.NULL else sortiert[((sortiert.size-1)*.95).toInt()]/1e6
                val objekt=JSONObject().put("baseline",baseline).put("detail",detail).put("dimension",dimension.name)
                    .put("zeitenMs",JSONArray(zeiten)).put("allokationenBytes",allokationen).put("frames",sortiert.size).put("frameP95Ms",p95).put("drawP95Ms",percentil(drawFrames)).put("layoutP95Ms",percentil(layoutFrames)).put("gestenMessung","kontinuierliche Bewegung nach fünf Aufwärmschritten, ohne Anheben")
                resultate.put(objekt);Log.i("AtlasVisualisierungMessung",objekt.toString())
                if(!baseline && detail && dimension==RaumDimension.R2) {
                    val f=File(compose.activity.cacheDir,"visualisierung-neu.png")
                    f.outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
                    val chunks=android.util.Base64.encodeToString(f.readBytes(),android.util.Base64.NO_WRAP).chunked(2800)
                    chunks.forEachIndexed { i, text -> Log.i("AtlasVisualisierungBild", "$i/${chunks.size}:$text") }
                }
            }
            val bericht=JSONObject().put("geraet",android.os.Build.MODEL).put("android",android.os.Build.VERSION.RELEASE).put("resultate",resultate)
            File(compose.activity.cacheDir,"visualisierung-messung.json").writeText(bericht.toString(2))
            Log.i("AtlasVisualisierungMessung",bericht.toString())
        }finally {compose.runOnUiThread{compose.activity.window.removeOnFrameMetricsAvailableListener(listener)};thread.quitSafely()}
    }

    @Test fun messeTupelfilterUndUeberlagerteGebiete() {
        val frames=Collections.synchronizedList(mutableListOf<Long>())
        val erfassen=AtomicBoolean(false)
        val thread=HandlerThread("AtlasFilterMessung").also{it.start()}
        val listener=android.view.Window.OnFrameMetricsAvailableListener { _, m, _ ->
            if(erfassen.get()) frames.add(m.getMetric(FrameMetrics.TOTAL_DURATION))
        }
        compose.runOnUiThread {compose.activity.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
        var eingänge by mutableStateOf<KnotenAuswertungsErgebnis?>(null)
        var konfiguration by mutableStateOf(VisualisierungsKonfiguration())
        var generation by mutableStateOf(0)
        val knoten=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        compose.setContent {MaterialTheme {Box(Modifier.size(900.dp,650.dp).testTag("filter-perf")) {
            key(generation) { eingänge?.let { VisualisierungsDetailansicht(knoten.copy(eigenschaften=konfiguration.zuEigenschaften()),it,{},Modifier.fillMaxSize()) } }
        }}}
        val resultate=JSONArray()
        fun bytes()=Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull()?:0L
        try {
            for(anzahl in listOf(1,2)) for(q in listOf(DarstellungsQualität.Standard,DarstellungsQualität.Hoch)) {
                val zeiten=mutableListOf<Double>();var allokationen=0L
                repeat(5) { index ->
                    val p=TypisiertesElement("p","tupel")
                    val a=strukturKomponente(p,RationaleZahl.Eins,true) as ZahlAusdruck
                    val b=strukturKomponente(p,RationaleZahl.von(2),true) as ZahlAusdruck
                    val basis=KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))
                    fun filter(umgekehrt:Boolean)=GefilterteMenge(basis,MathematischeMethode("P",listOf(p),
                        Vergleich(a,if(umgekehrt)VergleichsArt.Größer else VergleichsArt.Kleiner,addition(multiplikation(b,b),RationaleZahl.von((index+1).toLong(),100))),WahrheitsMenge,werteVorräte=mapOf("p" to basis)))
                    val e=KnotenAuswertungsErgebnis(emptyMap(),eingänge=buildMap {
                        put("menge",BedingterWert(filter(false)))
                        if(anzahl==2) put("menge2",BedingterWert(filter(true)))
                    })
                    val vorher=bytes();val start=System.nanoTime()
                    compose.runOnIdle {
                        val c=VisualisierungsKonfiguration(qualität=q,dimensionsModus=DimensionsModus.Manuell,dimension=RaumDimension.R2)
                        konfiguration=c.copy(sampling=c.sampling.copy(maximalesRasterBudget=c.detailBudget,
                            auflösung1D=if(q==DarstellungsQualität.Hoch)2000 else 160,
                            auflösung2D=if(q==DarstellungsQualität.Hoch)240 else 72,
                            auflösung3D=if(q==DarstellungsQualität.Hoch)40 else 22))
                        eingänge=e;generation++
                    }
                    compose.waitUntil(30_000) {compose.onAllNodesWithText("Berechne",substring=true).fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("Verfeinere",substring=true).fetchSemanticsNodes().isEmpty()}
                    compose.waitForIdle();zeiten+=(System.nanoTime()-start)/1e6;allokationen+=bytes()-vorher
                }
                frames.clear()
                val node=compose.onNodeWithTag("filter-perf")
                val mitte=node.fetchSemanticsNode().size.let{Offset(it.width*.3f,it.height*.5f)}
                node.performTouchInput {down(mitte)}
                repeat(50) { index ->
                    if(index==5) erfassen.set(true)
                    node.performTouchInput {advanceEventTime(16);moveTo(mitte+Offset(index*3f,index*1.1f))}
                    Thread.sleep(16)
                }
                erfassen.set(false);node.performTouchInput {up()};compose.waitForIdle()
                val zeitenFrame=synchronized(frames){frames.toList().sorted()}
                val messung=JSONObject().put("ebenen",anzahl).put("qualität",q.name)
                    .put("detailSichtbarMs",JSONArray(zeiten)).put("allokationenBytesFuenfAnfragen",allokationen)
                    .put("frames",zeitenFrame.size).put("frameP95Ms",if(zeitenFrame.isEmpty())JSONObject.NULL else zeitenFrame[((zeitenFrame.size-1)*.95).toInt()]/1e6)
                resultate.put(messung)
            }
            val bericht=JSONObject().put("geraet",android.os.Build.MODEL).put("android",android.os.Build.VERSION.RELEASE)
                .put("hinweis","Opt-in Instrumentierung, fünf kalte Geometrievarianten; Detailzeit enthält Compose-Test-Synchronisierung. Frame-Messung während kontinuierlichem Pan nach fünf Aufwärmschritten. Keine Zeitassertionen.")
                .put("resultate",resultate)
            File(compose.activity.cacheDir,"filter-messung.json").writeText(bericht.toString(2))
            Log.i("AtlasFilterMessung",bericht.toString())
        } finally {compose.runOnUiThread{compose.activity.window.removeOnFrameMetricsAvailableListener(listener)};thread.quitSafely()}
    }
}
