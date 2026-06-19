package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

// PullSystem für Daten um transformation von KarteDaten auszuwerten ohne ihren Graph zu erzeugen.
/*
interface PullSystem<A: PullAnschluss>: KnotenGraphDaten, AnschlüsseDaten<A> {
    abstract class PullDaten<T: Any>() {
        private lateinit var wert: T
        constructor(speicher: String): this().apply {
            wert = ausSpeicher(speicher)
        }
        public abstract fun ausSpeicher(wert: String): T
        public abstract fun zuSpeicher(wert: T): String
    }

    public val anschlussCache: SnapshotStateMap<String, PullDaten<*>>

    public fun erhalteCache() = derivedStateOf {
        val listEingang = anschlüsse.filterIsInstance<EingangDaten>()
        val listAusgang = anschlüsse.filterIsInstance<AusgangDaten>()
        val cache = mutableStateMapOf<String, PullDaten<*>>()
        cache.putAll(listAusgang.associate { it.id to baueCache(it as A,listEingang as List<A>) })
        return@derivedStateOf cache
    }

    public fun baueCache(ausgang: A, eingänge: List<A>): PullDaten<*>
}*/
