package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*

const val POLYNOM_STELLEN = "polynom.stellen"
const val POLYNOM_SUCHRADIUS = "polynom.suchradius"
const val MATRIX_VARIABLE_DIMENSION = "variable.matrixDimension"

fun polynomNumerikKonfiguration(parameter: Map<String,String>): PolynomNumerikKonfiguration {
    val stellen = parameter[POLYNOM_STELLEN]?.toIntOrNull() ?: 6
    val radius = parameter[POLYNOM_SUCHRADIUS]?.toIntOrNull() ?: 10
    return PolynomNumerikKonfiguration(stellen,radius)
}
fun konfigurierePolynomNumerik(knoten:KnotenDaten,konfiguration:PolynomNumerikKonfiguration):KnotenDaten =
    knoten.copy(parameter=knoten.parameter+mapOf(POLYNOM_STELLEN to konfiguration.stellen.toString(),POLYNOM_SUCHRADIUS to konfiguration.suchradius.toString()))

/** 0: historische skalare Variable, 1..8: quadratische Matrix mit geordneten Einträgen. */
fun konfiguriereMatrixVariable(knoten:KnotenDaten,dimension:Int):KnotenDaten {
    require(knoten.art=="mathematik.variable" && dimension in 0..8)
    return knoten.copy(parameter=knoten.parameter+(MATRIX_VARIABLE_DIMENSION to dimension.toString()),
        anschlüsse=knoten.anschlüsse.map { if(it.richtung==AnschlussRichtung.Ausgang)
            it.copy(art=if(dimension==0) MathematikAnschlussArten.Zahl.id else MathematikAnschlussArten.Matrix.id) else it })
}

/** Gemeinsamer Dienst für beide Oberflächen; abgebrochene Aufträge gelangen nie in den Cache. */
object PolynomNumerikDienst {
    private data class Schluessel(val problem:PolynomGleichungsProblem,val konfiguration:PolynomNumerikKonfiguration)
    private val cache=object:LinkedHashMap<Schluessel,PolynomLoesungsErgebnis>(16,0.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Schluessel,PolynomLoesungsErgebnis>?)=size>16
    }
    suspend fun berechne(problem:PolynomGleichungsProblem,konfiguration:PolynomNumerikKonfiguration):PolynomLoesungsErgebnis = withContext(Dispatchers.Default) {
        val key=Schluessel(problem,konfiguration)
        synchronized(cache) { cache[key] }?.let { return@withContext it }
        val auftrag=currentCoroutineContext()
        val budget=PolynomRechenBudget { auftrag.ensureActive() }
        val vorbereitet=problem.eigenwertMatrix?.let { eigenwertPolynomProblem(it,budget) }?:problem
        val r=loesePolynomGleichungsSystem(vorbereitet,konfiguration,budget).copy(
            analytischeFormeln=analytischePolynomFormeln(vorbereitet))
        auftrag.ensureActive()
        synchronized(cache) { cache[key]=r }; r
    }
    suspend fun familienMitglied(problem:PolynomGleichungsProblem,familie:PolynomLoesungsErgebnis,
        belegung:Map<String,RationaleZahl>,stellen:Int):ZertifiziertePolynomLoesung=withContext(Dispatchers.Default) {
        ensureActive(); wertePolynomFamilieAus(problem,familie,belegung,stellen).also { ensureActive() }
    }

}
