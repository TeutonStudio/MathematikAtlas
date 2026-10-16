package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import kotlinx.coroutines.*
import java.util.LinkedHashMap

/** Begrenzte, geteilte Arbeit. Der letzte abgemeldete Verbraucher bricht auch die Berechnung ab. */
internal class VisualisierungsBerechnung(private val speicherLimit: Long = 32L * 1024 * 1024) {
    private data class Schlüssel(val menge: MengenAusdruck, val signatur: List<Any?>)
    private data class Eintrag(val ergebnis: VisualisierungsErgebnis, val bytes: Long)
    private class Auftrag(val tor: CompletableDeferred<Unit>, var priorität: Int) {
        lateinit var arbeit: Deferred<VisualisierungsErgebnis>
        var verbraucher = 0
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = LinkedHashMap<Schlüssel, Eintrag>(16, .75f, true)
    private val laufend = mutableMapOf<Schlüssel, Auftrag>()
    private val wartend = mutableListOf<Auftrag>()
    private var aktiv = 0
    internal var speicherBytes: Long = 0; private set
    private fun verteile() {
        while (aktiv < 2 && wartend.isNotEmpty()) {
            val nächster = wartend.maxBy { it.priorität }
            wartend.remove(nächster)
            aktiv++
            nächster.tor.complete(Unit)
        }
    }
    @Synchronized fun gecacht(menge: MengenAusdruck, c: VisualisierungsKonfiguration): VisualisierungsErgebnis? =
        cache[Schlüssel(menge, c.samplingSignatur())]?.ergebnis

    suspend fun berechne(menge: MengenAusdruck, c: VisualisierungsKonfiguration, detail: Boolean,
        sampler: (suspend () -> VisualisierungsErgebnis)? = null,
    ): VisualisierungsErgebnis {
        val schlüssel = Schlüssel(menge, c.samplingSignatur())
        val auftrag: Auftrag
        synchronized(this) {
            cache[schlüssel]?.let { return it.ergebnis }
            auftrag = laufend[schlüssel]?.takeUnless { it.arbeit.isCancelled } ?: Auftrag(CompletableDeferred(), if (detail) 1 else 0).also { neu ->
                laufend[schlüssel] = neu
                wartend += neu
                neu.arbeit = scope.async(start = CoroutineStart.LAZY) {
                        neu.tor.await()
                        val context = currentCoroutineContext()
                        // Abbruchprüfung ist auch in den synchronen fachlichen Schleifen verfügbar.
                        val ergebnis = sampler?.invoke() ?: VisualisierungsSampler.sample(menge, c) { context.ensureActive() }
                        context.ensureActive()
                        synchronized(this@VisualisierungsBerechnung) {
                            val bytes = ergebnis.geschätzteBytes()
                            if (bytes <= speicherLimit) {
                                cache.put(schlüssel, Eintrag(ergebnis, bytes))?.let { speicherBytes -= it.bytes }
                                speicherBytes += bytes
                                while (speicherBytes > speicherLimit) {
                                    val erster = cache.entries.iterator(); val e = erster.next(); speicherBytes -= e.value.bytes; erster.remove()
                                }
                            }
                        }
                        ergebnis
                }
                neu.arbeit.invokeOnCompletion {
                        synchronized(this@VisualisierungsBerechnung) {
                            if (neu in wartend) wartend.remove(neu) else if (neu.tor.isCompleted) aktiv--
                            laufend.remove(schlüssel, neu)
                            verteile()
                        }
                }
            }
            auftrag.verbraucher++
            if (detail) auftrag.priorität = 1
            auftrag.arbeit.start()
            verteile()
        }
        return try { auftrag.arbeit.await() } finally {
            synchronized(this) { if (--auftrag.verbraucher == 0 && !auftrag.arbeit.isCompleted) auftrag.arbeit.cancel() }
        }
    }
    fun schließen() { scope.cancel() }
}

internal fun VisualisierungsErgebnis.geschätzteBytes(): Long {
    val e = this as? VisualisierungsErgebnis.Erfolgreich
    val t = this as? VisualisierungsErgebnis.Teilweise
    val punkte = e?.punkte ?: t?.punkte.orEmpty()
    val linien = e?.linien ?: t?.linien.orEmpty()
    val dreiecke = e?.dreiecke ?: t?.dreiecke.orEmpty()
    val zellen = e?.zellen ?: t?.zellen.orEmpty()
    return 512L + punkte.sumOf { 112L + it.weitereFarbwerte.size * 32L } + linien.sumOf { 64L + it.punkte.size * 128L } +
        dreiecke.size * 448L + zellen.sumOf { 192L + (it.grund?.length ?: 0) * 2L } + (e?.intervalle ?: t?.intervalle.orEmpty()).size * 96L +
        (e?.hinweise ?: t?.hinweise.orEmpty()).sumOf { 48L + it.length * 2L }
}

internal val gemeinsameVisualisierungsBerechnung = VisualisierungsBerechnung()
