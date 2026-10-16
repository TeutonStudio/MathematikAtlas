package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/** Opt-in-Messung ohne Zeitassertionen. Ersetzt keine Frame- oder Referenzgerätemessung. */
class VisualisierungsSamplingMessungTest {
    @Test fun messeSamplingUndCache() = runBlocking<Unit> {
        assumeTrue(System.getProperty("atlas.measureSampling") == "true")
        val x=Variable("x");val y=Variable("y")
        val region=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)), Vergleich(y,VergleichsArt.Kleiner,multiplikation(x,x)))
        val graph=Methode(name="f",parameter=listOf(x),vorschrift=Sinus(x),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)).graphMenge()
        val karte=MathematikKartenCodec.lade(javaClass.getResource("/regression/tupel-filter-visualisierung-v3.json")!!.readText())
        val laufzeit=MathematikKartenLaufzeit().auswerten(karte)
        val filter=laufzeit.knoten.getValue(karte.knoten.single {it.art=="mathematik.visualisierung"}.id).eingänge.getValue("menge").objekt as MengenAusdruck
        val zeilen=mutableListOf<String>()
        for((name,mengen) in listOf("TupelfilterNutzerkarte" to listOf(filter),"TupelfilterUndParabelgebiet" to listOf(filter,region),"Parabelgebiet" to listOf(region),"Sinusgraph" to listOf(graph),"ZweiGebiete" to listOf(region,DefinierteMenge(region.variablen,Vergleich(y,VergleichsArt.Größer,multiplikation(x,x)))))) {
            for(q in listOf(DarstellungsQualität.Standard,DarstellungsQualität.Hoch)) {
                val b=VisualisierungsBerechnung()
                val c0=VisualisierungsKonfiguration(qualität=q)
                val c=c0.copy(sampling=c0.sampling.copy(maximalesRasterBudget=c0.detailBudget/mengen.size,auflösung2D=if(q==DarstellungsQualität.Hoch)240 else 72,bildBreite=800.0,bildHöhe=600.0))
                suspend fun messe(vorschau:Boolean):Double {
                    val start=System.nanoTime()
                    mengen.map { async { b.berechne(it,if(vorschau)c.alsVorschau()else c,true) } }.awaitAll()
                    return (System.nanoTime()-start)/1e6
                }
                try {
                    val grob=messe(true);val detail=messe(false);val cache=messe(false)
                    zeilen += "{\"fall\":\"$name\",\"qualität\":\"$q\",\"vorschauMs\":$grob,\"detailMs\":$detail,\"cacheMs\":$cache,\"cacheBytes\":${b.speicherBytes}}"
                } finally { b.schließen() }
            }
        }
        java.io.File("/tmp/filter-sampling-benchmark.json").writeText("{\"plattform\":\"${System.getProperty("os.name")} ${System.getProperty("os.arch")}\",\"java\":\"${System.getProperty("java.version")}\",\"messungen\":[${zeilen.joinToString(",")}]}")
    }
}
