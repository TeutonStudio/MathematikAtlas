package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.LinkedHashMap
import kotlinx.coroutines.selects.select


internal enum class VerfeinerungsPhase { Vorschau, Verfeinerung, Fertig, Begrenzt }
internal fun phaseFür(e:VisualisierungsErgebnis) = when(e) {
    is VisualisierungsErgebnis.Teilweise -> if(e.statistik.budgetErschöpft || e.hinweise.any { "Speichergrenze" in it || "Materialisierungsgrenze" in it || "Verfeinerungsgrenze" in it }) VerfeinerungsPhase.Begrenzt else VerfeinerungsPhase.Fertig
    is VisualisierungsErgebnis.Erfolgreich -> if(e.statistik.budgetErschöpft) VerfeinerungsPhase.Begrenzt else VerfeinerungsPhase.Fertig
    is VisualisierungsErgebnis.NichtDarstellbar -> if(e.grund.contains("Speichergrenze")) VerfeinerungsPhase.Begrenzt else VerfeinerungsPhase.Fertig
    else -> VerfeinerungsPhase.Fertig
}
internal data class SamplingStand(val ergebnis: VisualisierungsErgebnis, val phase: VerfeinerungsPhase) {
    val fertig get() = phase == VerfeinerungsPhase.Fertig || phase == VerfeinerungsPhase.Begrenzt
}

/** Gemeinsame Reservierungen für lebende Sampler und veröffentlichte Zeichenpakete. */
internal class VisualisierungsSpeicher(private val maximum: Long = 64L * 1024 * 1024) {
    var bytes: Long = 0; private set
    inner class Reservierung : AutoCloseable {
        private var größe=0L
        private var geschlossen=false
        @Synchronized fun setze(neu:Long) {
            require(neu>=0)
            if(geschlossen) {
                if(neu>0) throw CancellationException("Zeichenpaket wurde bereits freigegeben.")
                return
            }
            synchronized(this@VisualisierungsSpeicher) {
                if(neu-größe>maximum-bytes) throw VisualisierungsSpeicherGrenze()
                bytes+=neu-größe;größe=neu
            }
        }
        @Synchronized override fun close() { if(!geschlossen) { setze(0);geschlossen=true } }
    }
    fun reserviere() = Reservierung()
}
internal class VisualisierungsSpeicherGrenze : RuntimeException("Darstellung wegen Speichergrenze unvollständig.")
internal val visualisierungsArbeitsSpeicher = VisualisierungsSpeicher()

/** Faire Quanten auch für synchrone Kerncallbacks: der Kotlin-Aufrufstack bleibt erhalten.
 * Wartende IO-Worker schlafen am Monitor und verbrauchen keine CPU. Maximal zwei rechnen.
 */
internal class VisualisierungsRechenZeit(private val parallelität: Int = 2) {
    private val monitor = Object()
    internal class Eintritt(val priorität: () -> Int)
    private val wartend = mutableListOf<Eintritt>()
    private var aktiv = 0
    fun vormerken(priorität:()->Int):Eintritt = synchronized(monitor) { Eintritt(priorität).also { wartend+=it } }
    fun abmelden(eintritt:Eintritt) { synchronized(monitor) { wartend.remove(eintritt);monitor.notifyAll() } }
    fun betrete(priorität: () -> Int, abbruch: () -> Unit, vorgemerkt:Eintritt?=null): AutoCloseable {
        val eintritt = vorgemerkt ?: vormerken(priorität)
        synchronized(monitor) {
            try {
                while (true) {
                    abbruch()
                    if (aktiv < parallelität && wartend.maxByOrNull { it.priorität() } === eintritt) {
                        wartend.remove(eintritt); aktiv++; break
                    }
                    monitor.wait(10)
                }
            } catch (e: Throwable) { wartend.remove(eintritt); monitor.notifyAll(); throw e }
        }
        return AutoCloseable { synchronized(monitor) { aktiv--; monitor.notifyAll() } }
    }
}

/** Threadgebundene Brücke; keine Coroutine suspendiert innerhalb eines synchronen Samplers. */
internal class SamplingSitzung(
    private val zeit: VisualisierungsRechenZeit,
    private val priorität: () -> Int,
    private val abbruch: () -> Unit,
    private val publiziere: (VisualisierungsErgebnis) -> Unit,
    private val uhr: () -> Long = System::nanoTime,
    private val probeCache: (Any,List<Double>,()->AdaptivePunktAuswertung)->AdaptivePunktAuswertung = { _,_,f -> f() },
    private var vorgemerkt:VisualisierungsRechenZeit.Eintritt?=null,
    speicherDienst:VisualisierungsSpeicher=visualisierungsArbeitsSpeicher,
) : AutoCloseable {
    private var eintritt: AutoCloseable? = null
    private var quantumStart = 0L
    private var veröffentlichung = 0L
    private val speicher = speicherDienst.reserviere()
    private var reservierteBytes = 0L
    private var zwischenBytes=0L
    private var definitionsVersion=0L
    private var veröffentlichteDefinitionsVersion=0L
    val hatNeueDefinitionsLücken get()=definitionsVersion!=veröffentlichteDefinitionsVersion
    fun definitionsLücke() { definitionsVersion++ }
    private var abbildung:(VisualisierungsErgebnis)->VisualisierungsErgebnis={it}
    fun <T> mitAbbildung(neu:(VisualisierungsErgebnis)->VisualisierungsErgebnis,block:()->T):T {
        val alt=abbildung;abbildung={alt(neu(it))}
        return try {block()} finally {abbildung=alt}
    }
    fun prüfe() {
        abbruch()
        val jetzt = uhr()
        if (eintritt == null || jetzt - quantumStart >= 8_000_000L) {
            eintritt?.close(); eintritt = null
            eintritt = zeit.betrete(priorität, abbruch,vorgemerkt);vorgemerkt=null
            quantumStart = uhr()
        }
    }
    fun probe(herkunft:Any,argumente:List<Double>,auswerten:()->AdaptivePunktAuswertung)=probeCache(herkunft,argumente,auswerten)
    fun reserviere(bytes: Long) {
        val neu = reservierteBytes + bytes
        speicher.setze(neu+zwischenBytes); reservierteBytes = neu
    }
    fun zwischenstand(sofort: Boolean = false, erzeuge: () -> VisualisierungsErgebnis) {
        prüfe()
        val jetzt = uhr()
        if (sofort || veröffentlichung == 0L || jetzt - veröffentlichung >= 50_000_000L) {
            // Vor dem Kopieren Platz für einen neuen Snapshot reservieren.
            speicher.setze(reservierteBytes+maxOf(zwischenBytes,reservierteBytes))
            val e = abbildung(erzeuge())
            // Kopierte Zwischenstände bleiben neben dem Arbeitsmodell bis zum nächsten Stand lebend.
            zwischenBytes=e.geschätzteBytes()
            speicher.setze(reservierteBytes + zwischenBytes)
            publiziere(e); veröffentlichung = jetzt
            veröffentlichteDefinitionsVersion=definitionsVersion
        }
    }
    override fun close() { eintritt?.close(); eintritt = null; speicher.close() }
    companion object {
        private val aktuell = ThreadLocal<SamplingSitzung>()
        fun aktuelle() = aktuell.get()
        fun <T> mit(sitzung: SamplingSitzung, block: () -> T): T {
            check(aktuell.get() == null)
            aktuell.set(sitzung)
            return try { sitzung.prüfe(); block() } finally { aktuell.remove() }
        }
    }
}
internal fun samplingSpeicher(bytes: Long) { SamplingSitzung.aktuelle()?.reserviere(bytes) }
internal fun samplingZwischenstand(sofort: Boolean = false, erzeuge: () -> VisualisierungsErgebnis) {
    SamplingSitzung.aktuelle()?.zwischenstand(sofort, erzeuge)
}

/** Geteilte Sitzungen; der letzte abgemeldete Verbraucher beendet auch die Berechnung. */
internal class VisualisierungsBerechnung(private val speicherLimit: Long = 32L * 1024 * 1024, private val arbeitsSpeicher:VisualisierungsSpeicher=visualisierungsArbeitsSpeicher) {
    private data class Schlüssel(val menge: MengenAusdruck, val signatur: List<Any?>)
    private data class Eintrag(val ergebnis: VisualisierungsErgebnis, val bytes: Long)
    private class Auftrag(@Volatile var priorität: Int) {
        val stand = MutableStateFlow<SamplingStand?>(null)
        lateinit var arbeit: Deferred<VisualisierungsErgebnis>
        var vormerkung:VisualisierungsRechenZeit.Eintritt?=null
        val tor=CompletableDeferred<Unit>()
        var verbraucher = 0
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(10))
    private val zeit = VisualisierungsRechenZeit()
    private val wartend=mutableListOf<Auftrag>()
    private var zugelassen=0
    private fun verteile() {
        while(wartend.isNotEmpty()) {
            val a=wartend.maxBy { it.priorität }
            // Acht Hintergrundstacks, zwei Plätze ausschließlich als Dialogreserve.
            if(zugelassen >= if(a.priorität>0) 10 else 8) break
            wartend.remove(a);zugelassen++
            a.vormerkung=zeit.vormerken { a.priorität };a.tor.complete(Unit)
        }
    }
    private val cache = LinkedHashMap<Schlüssel, Eintrag>(16, .75f, true)
    private val laufend = mutableMapOf<Schlüssel, Auftrag>()
    private data class ProbeSchlüssel(val herkunft:Any,val argumente:List<Double>)
    private val proben=LinkedHashMap<ProbeSchlüssel,AdaptivePunktAuswertung>(16,.75f,true)
    private var probenBytes=0L
    private var treffer=0L
    internal val probenTreffer:Long @Synchronized get()=treffer
    private fun probe(herkunft:Any,argumente:List<Double>,auswerten:()->AdaptivePunktAuswertung):AdaptivePunktAuswertung {
        val key=ProbeSchlüssel(herkunft,argumente.toList())
        synchronized(this) { proben[key]?.let { treffer++;return it } }
        val wert=auswerten()
        synchronized(this) {
            if(proben.put(key,wert)==null) { probenBytes+=384;speicherBytes+=384 }
            beschränkeCache()
        }
        return wert
    }
    private fun beschränkeCache() {
        while(speicherBytes>speicherLimit) {
            if(proben.isNotEmpty() && (probenBytes>speicherLimit/2 || cache.isEmpty())) {
                val i=proben.entries.iterator();i.next();i.remove();probenBytes-=384;speicherBytes-=384
            } else {
                val i=cache.entries.iterator();val e=i.next();speicherBytes-=e.value.bytes;i.remove()
            }
        }
    }
    internal var speicherBytes: Long = 0; private set
    @Synchronized fun gecacht(menge: MengenAusdruck, c: VisualisierungsKonfiguration): VisualisierungsErgebnis? =
        cache[Schlüssel(menge, c.samplingSignatur())]?.ergebnis

    fun beobachte(menge: MengenAusdruck, c: VisualisierungsKonfiguration, detail: Boolean,
        sampler: (suspend () -> VisualisierungsErgebnis)? = null,
    ): Flow<SamplingStand> = flow {

        val schlüssel = Schlüssel(menge, c.samplingSignatur())
        val auftrag = synchronized(this@VisualisierungsBerechnung) {
            cache[schlüssel]?.let { return@synchronized it }
            val a = laufend[schlüssel]?.takeUnless { it.arbeit.isCancelled } ?: Auftrag(if(detail) 1 else 0).also { neu ->
                laufend[schlüssel] = neu
                wartend+=neu
                neu.arbeit = scope.async(start = CoroutineStart.LAZY) {
                    neu.tor.await()
                    val context = currentCoroutineContext()
                    var letzter: VisualisierungsErgebnis? = null
                    val sitzung = SamplingSitzung(zeit, { neu.priorität }, { context.ensureActive() }, { e ->
                        letzter = e
                        neu.stand.value = SamplingStand(e, if(neu.stand.value == null) VerfeinerungsPhase.Vorschau else VerfeinerungsPhase.Verfeinerung)
                    }, vorgemerkt=neu.vormerkung,probeCache=::probe,speicherDienst=arbeitsSpeicher)
                    val roh = try {
                        if(sampler != null) {
                            // Test-/Kompatibilitätscallback darf suspendieren; keine ThreadLocal-Brücke.
                            val eintritt = zeit.betrete({ neu.priorität },{ context.ensureActive() },neu.vormerkung)
                            try { sampler() } finally { eintritt.close() }
                        } else SamplingSitzung.mit(sitzung) {
                            VisualisierungsSampler.sample(menge, c, sitzung::prüfe)
                        }
                    } catch (e: VisualisierungsSpeicherGrenze) {
                        sitzung.begrenzterStand(letzter,e)
                    } finally { sitzung.close() }
                    context.ensureActive()
                    val ergebnis=if(c.sampling.automatisch) roh.automatischeHinweise() else roh
                    synchronized(this@VisualisierungsBerechnung) {
                        val bytes = ergebnis.geschätzteBytes()
                        if(bytes <= speicherLimit) {
                            cache.put(schlüssel, Eintrag(ergebnis,bytes))?.let { speicherBytes -= it.bytes }
                            speicherBytes += bytes
                            beschränkeCache()
                        }
                    }
                    neu.stand.value=SamplingStand(ergebnis,phaseFür(ergebnis))
                    ergebnis
                }
                neu.arbeit.invokeOnCompletion {
                    neu.vormerkung?.let(zeit::abmelden)
                    synchronized(this@VisualisierungsBerechnung) {
                        if(!wartend.remove(neu) && neu.tor.isCompleted) zugelassen--
                        laufend.remove(schlüssel,neu);verteile()
                    }
                }
            }
            a.verbraucher++; if(detail) a.priorität=1
            a.arbeit.start();verteile(); a
        }
        if(auftrag is Eintrag) { emit(SamplingStand(auftrag.ergebnis,phaseFür(auftrag.ergebnis))); return@flow }
        auftrag as Auftrag
        try {
            coroutineScope {
                val updates=auftrag.stand.filterNotNull().conflate().produceIn(this)
                try {
                    while(true) {
                        val stand=select<SamplingStand> {
                            updates.onReceive { it }
                            auftrag.arbeit.onAwait { auftrag.stand.value!! }
                        }
                        emit(stand)
                        if(stand.fertig) break
                    }
                } finally { updates.cancel() }
            }
        } finally {
            synchronized(this@VisualisierungsBerechnung) { if(--auftrag.verbraucher == 0 && !auftrag.arbeit.isCompleted) auftrag.arbeit.cancel() }
        }
    }
    suspend fun berechne(menge: MengenAusdruck,c: VisualisierungsKonfiguration,detail:Boolean,
        sampler:(suspend () -> VisualisierungsErgebnis)?=null): VisualisierungsErgebnis = beobachte(menge,c,detail,sampler).last().ergebnis
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
        (dreiecke.size + (e?.interaktionsDreiecke ?: t?.interaktionsDreiecke.orEmpty()).size) * 448L + zellen.sumOf { 192L + (it.grund?.length ?: 0) * 2L } + (e?.intervalle ?: t?.intervalle.orEmpty()).size * 96L +
        (e?.hinweise ?: t?.hinweise.orEmpty()).sumOf { 48L + it.length * 2L }
}

internal val gemeinsameVisualisierungsBerechnung = VisualisierungsBerechnung()

/** Kamera und Detailanforderung dürfen bestehende, fachlich passende Geometrie weiterverwenden. */
internal fun VisualisierungsKonfiguration.inhaltsSignatur(menge: MengenAusdruck?): List<Any?> = listOf(
    menge, dimension, achsen, bereiche, methodenModus, festeSchnitte, ausgabeKoordinaten,
    if (farbe.modus == FarbModus.Spektrum) listOf(farbe.variable, farbe.bereich) else null,
    sampling.toleranz, sampling.fensterBegrenztePrädikatsMengen, sampling.maximaleOrbitSchritte,
)

/** Nur ein Erstaufbau benötigt eine Vorschau. Navigation wartet und fordert direkt Detail an. */
internal suspend fun berechneVisualisierungsFolge(
    c: VisualisierungsKonfiguration,
    hatGeometrie: Boolean,
    berechne: suspend (VisualisierungsKonfiguration) -> VisualisierungsErgebnis,
    veröffentliche: (VisualisierungsErgebnis, Boolean) -> Unit,
    warteAufRuhe: suspend () -> Unit = { delay(120) },
) {
    if (!hatGeometrie) {
        val vorschau = c.alsVorschau().let { it.copy(sampling = it.sampling.copy(
            zielFehlerPixel = 3.0 / if(c.dimension == RaumDimension.R3) c.kamera.zoom else 1.0)) }
        val ergebnis = berechne(vorschau)
        currentCoroutineContext().ensureActive()
        veröffentliche(ergebnis, false)
    }
    warteAufRuhe()
    val ergebnis = berechne(c)
    currentCoroutineContext().ensureActive()
    veröffentliche(ergebnis, true)
}

/** Bereits ausreichend genaue Geometrie innerhalb der Reserve wird nur transformiert. */
internal fun VisualisierungsKonfiguration.decktAb(neu:VisualisierungsKonfiguration):Boolean {
    val alt=sampling.sichtBereiche ?: return false
    if(neu.sampling.bildBreite<=0 || neu.sampling.bildHöhe<=0) return false
    val sichtbar=VisualisierungsTransformation(neu,neu.sampling.bildBreite,neu.sampling.bildHöhe).sichtBereiche(0.0)
    fun enthält(a:ZahlenBereich,b:ZahlenBereich)=a.minimum<=b.minimum && a.maximum>=b.maximum
    if(!enthält(alt.x,sichtbar.x) || !enthält(alt.y,sichtbar.y)) return false
    val zoomFaktor=neu.kamera.zoom/kamera.zoom
    return sampling.zielFehlerPixel*zoomFaktor<=neu.sampling.zielFehlerPixel*(if(neu.dimension==RaumDimension.R3) zoomFaktor else 1.0)
}

/** Gemischtradix-Iterator; keine Zwischenliste sämtlicher Kombinationen. */
internal fun kartesischeStützwerte(domänen:List<NumerischeDomäne>):Sequence<List<Double>> = sequence {
    if(domänen.isEmpty() || domänen.any { it.werte.isEmpty() }) return@sequence
    val indices=IntArray(domänen.size)
    while(true) {
        yield(domänen.indices.map { domänen[it].werte[indices[it]] })
        var achse=indices.lastIndex
        while(achse>=0) {
            indices[achse]++
            if(indices[achse]<domänen[achse].werte.size) break
            indices[achse]=0;achse--
        }
        if(achse<0) break
    }
}

private fun VisualisierungsErgebnis.automatischeHinweise():VisualisierungsErgebnis {
    fun text(s:String)=s.replace("Zellbudget","Nachweisgrenze").replace("Arbeitsbudget","technische Arbeitsgrenze")
        .replace("Auswertungsbudget","technische Arbeitsgrenze").replace("Rasterbudget","Materialisierungsgrenze")
        .replace("Samplingbudget","Materialisierungsgrenze").replace("Gesamtbudget","Materialisierungsgrenze")
    fun zellen(l:List<VisualisierungsZelle>)=l.map { it.copy(grund=it.grund?.let(::text)) }
    return when(this) {
        is VisualisierungsErgebnis.Erfolgreich -> copy(hinweise=hinweise.map(::text),zellen=zellen(zellen))
        is VisualisierungsErgebnis.Teilweise -> copy(hinweise=hinweise.map(::text),zellen=zellen(zellen))
        is VisualisierungsErgebnis.NichtDarstellbar -> copy(grund=text(grund))
        is VisualisierungsErgebnis.BedingtDarstellbar -> copy(grund=text(grund),bedingungen=bedingungen.map(::text))
        is VisualisierungsErgebnis.ProjektionErforderlich -> this
    }
}

private fun VisualisierungsErgebnis.ohneVeralteteFlächen():VisualisierungsErgebnis {
    val hinweis="Die ältere Flächenvorschau wurde wegen neu erkannter Definitionslücken verworfen."
    return when(this) {
        is VisualisierungsErgebnis.Erfolgreich -> copy(dreiecke=emptyList(),interaktionsDreiecke=emptyList(),hinweise=hinweise+hinweis)
        is VisualisierungsErgebnis.Teilweise -> copy(dreiecke=emptyList(),interaktionsDreiecke=emptyList(),hinweise=hinweise+hinweis)
        else -> this
    }
}

internal fun SamplingSitzung.begrenzterStand(letzter:VisualisierungsErgebnis?,fehler:VisualisierungsSpeicherGrenze):VisualisierungsErgebnis {
    val v=if(hatNeueDefinitionsLücken) letzter?.ohneVeralteteFlächen() else letzter
    return when(v) {
        is VisualisierungsErgebnis.Erfolgreich -> VisualisierungsErgebnis.Teilweise(v.punkte,v.hinweise+fehler.message.orEmpty(),
            intervalle=v.intervalle,zellen=v.zellen,linien=v.linien,dreiecke=v.dreiecke,interaktionsDreiecke=v.interaktionsDreiecke)
        is VisualisierungsErgebnis.Teilweise -> v.copy(hinweise=v.hinweise+fehler.message.orEmpty())
        else -> VisualisierungsErgebnis.NichtDarstellbar(fehler.message.orEmpty())
    }
}
