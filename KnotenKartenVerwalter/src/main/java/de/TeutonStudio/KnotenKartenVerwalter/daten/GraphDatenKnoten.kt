package de.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss.gerichteteGDA.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt

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
        val richtung: AnschlussRichtung
        public val anschlussLabel: SnapshotStateMap<Kante,Map<Int,String>>

        public fun erhateAnschluss(idx:Int, kante: Kante, label:String): A

        override val anschlüsse: SnapshotStateList<GraphDatenAnschluss>
            get() = mutableStateListOf<GraphDatenAnschluss>().apply {
                Kante.entries.forEach { k: Kante -> anschlussLabel[k]?.entries?.forEach {
                    add(erhateAnschluss(it.key,k,it.value))
                } }
            }
        // .entries.map { erhateAnschluss() } }
        // anschlussLabel.values.mapIndexed(::erhateAnschluss)

    }
    interface auswertbarerGDK: GraphDatenKnoten {
        override fun wurdeVerbunden(von: String, mit: Pair<GraphDatenKnoten, String>) {
            val eingangCache = ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1, a2) ->
                listOf(
                    if (a1 is GraphDatenAnschluss.auswertbarerGDA && a1.istEingang) a1 else null,
                    if (a2 is GraphDatenAnschluss.auswertbarerGDA && a2.istEingang) a2 else null,
                )
            }.map { it?.cache }
            ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1, a2) ->
                listOf(
                    if (a1 is GraphDatenAnschluss.auswertbarerGDA && a1.istAusgang) a1 else null,
                    if (a2 is GraphDatenAnschluss.auswertbarerGDA && a2.istAusgang) a2 else null,
                )
            }.filterNotNull().forEach { it.cache = it.baueCache(eingangCache) }
            super.wurdeVerbunden(von, mit)
        }
    }

    public fun erlaubeVerbindung(von:String,mit: Pair<GraphDatenKnoten,String>) = ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1,a2) ->
        a1.erlaubeVerbindung(a2) && a2.erlaubeVerbindung(a1)
    } ?: false
    public fun wurdeVerbunden(von:String,mit: Pair<GraphDatenKnoten,String>) = ((this to von).zuAnschluss() to mit.zuAnschluss()).let { (a1,a2) ->
        a1.wurdeVerbunden(a2); a2.wurdeVerbunden(a1)
    }

    public fun Pair<GraphDatenKnoten,String>.zuAnschluss() = first.anschlüsse.find { it.id == second }
    private fun <R> Pair<GraphDatenAnschluss?,GraphDatenAnschluss?>.let(block: (paarAnschluss) -> R?) = first?.let { a1 -> second?.let { a2 -> block(a1 to a2) } }

}