package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlüsseDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import kotlin.collections.emptyMap

typealias PullObjekt = PullSystem<out AnschlüsseDaten<out RichtungsAnschlussDaten>>
typealias Cache = Map<String,Any>
interface PullSystem<K: KnotenAnschlussDaten<out RichtungsAnschlussDaten>>: GraphKnotenObjekt<K> {
    val cacheAnschlüsse: SnapshotStateMap<AnschlussDaten, Cache>

    public fun aktualisiereCache() {
        val verbindungen = graph.karte.verbindungen.filter {
            it.daten.ids.knotenIdMann == daten.id || it.daten.ids.knotenIdWeib == daten.id
        }
        val reinDaten = daten.anschlüsse.filterIsInstance<EingangDaten>()
        val rausDaten = daten.anschlüsse.filter { it !in reinDaten }
        val cacheAnschluss = verbindungen.filter { v ->
            reinDaten.map { it.id }.enthältAnschlussId(v.daten.ids)
        }.verbundeneAnschlüsse(reinDaten).filter { it.besitzer is PullObjekt }.mapNotNull { (it.besitzer as PullObjekt).cacheAnschlüsse[it.daten] }
        cacheAnschlüsse.putAll(rausDaten.associateWith { baueCache(it,cacheAnschluss) })
    }

    open fun baueCache(raus: AnschlussDaten, reinCache: Iterable<Cache>): Map<String,Any> {
        return emptyMap()
    }

    private fun Iterable<String>.enthältAnschlussId(ids: IDEhe) = contains(ids.anschlussIdMann) || contains(ids.anschlussIdWeib)

    private fun Iterable<Verbindung>.verbundeneAnschlüsse(anschlüsse: Iterable<AnschlussDaten>) = mapNotNull {
        graph.karte.erhalteAnschlussMann(it.daten.ids)?.let { a ->
            if (a.daten in anschlüsse) null else a
        } ?: graph.karte.erhalteAnschlussWeib(it.daten.ids)?.let { a ->
            if (a.daten in anschlüsse) null else a
        }
    }
}