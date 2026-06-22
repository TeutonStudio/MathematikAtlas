package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt

typealias fremderAnschluss = Pair<GraphDatenKnoten, String>
typealias paarAnschluss = Pair<GraphDatenAnschluss,GraphDatenAnschluss>

interface GraphDatenKnoten: GraphDaten, GraphDaten.benanntesGD, GraphDaten.bewegbareGD, GraphDaten.orthogoneGD {
    public override var klasse: KnotenArt?
    public var beweglich: Boolean // TODELETE
    public val anschlüsse: SnapshotStateList<GraphDatenAnschluss>
    public val anschlussIdx: SnapshotStateMap<String, Int>
    public val data: MutableMap<String, Any>

    interface gerichteteGDK<A: GraphDatenAnschluss>: GraphDatenKnoten {
        /** Richtung der Anschlusse im Verbindungsfluss. */
        val richtung: GraphDatenAnschluss.gerichteteGDA.AnschlussRichtung
        public val anschlussLabel: SnapshotStateMap<Kante,Map<Int,String>>

        public fun erhalteAnschluss(idx:Int, kante: Kante, label:String): A

        override val anschlüsse: SnapshotStateList<GraphDatenAnschluss>
            get() = mutableStateListOf<GraphDatenAnschluss>().apply {
                Kante.entries.forEach { k: Kante -> anschlussLabel[k]?.entries?.forEach {
                    add(erhalteAnschluss(it.key,k,it.value))
                } }
            }
        // .entries.map { erhateAnschluss() } }
        // anschlussLabel.values.mapIndexed(::erhateAnschluss)

    }
    interface auswertbarerGDK: GraphDatenKnoten {
        override fun wurdeVerbunden(von: String, mit: fremderAnschluss) {
            super.wurdeVerbunden(von, mit)

            val eigenerAnschluss = (this to von).zuAnschluss()
            val eigenerAusgang = eigenerAnschluss as? GraphDatenAnschluss.auswertbarerGDA
            if (eigenerAusgang?.istAusgang == true) {
                val eingangCache = anschlüsse
                    .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                    .filter { it.istEingang }
                    .map { it.cache }

                if (eingangCache.isNotEmpty()) {
                    eigenerAusgang.cache = eigenerAusgang.baueCache(eingangCache)
                }
            }
        }
    }

    public fun erlaubeVerbindung(von:String,mit: fremderAnschluss) = ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1,a2) ->
        a1.erlaubeVerbindung(a2) && a2.erlaubeVerbindung(a1)
    } ?: false
    public fun wurdeVerbunden(von:String,mit: fremderAnschluss) = ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1,a2) ->
        a1.wurdeVerbunden(a2); a2.wurdeVerbunden(a1)
    }

    public fun fremderAnschluss.zuAnschluss() = first.anschlüsse.find { it.id == second }
    private fun <R> Pair<GraphDatenAnschluss?,GraphDatenAnschluss?>.let(block: (paarAnschluss) -> R?) = first?.let { a1 -> second?.let { a2 -> block(a1 to a2) } }

}
