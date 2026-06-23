package de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektInspektor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.karten.MatheKarte
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageWert
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.latexInfo

typealias ZahlenRelationDaten = relation.ZahlenRelationDaten

class relation(
    override val graph: Graph,
    override val daten: ZahlenRelationDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<ZahlenRelationDaten>, GraphDatenObjektInspektor<ZahlenRelationDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite = 220f
    override val minimaleTiefe = 90f

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier.matchParentSize()) {
            Column {
                Text(daten.name)
                LaTeXFormelText(daten.formel(), besitzer.daten)
            }
        }
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }

    @Composable
    override fun BoxScope.Inspektor() {
        Composable()
    }

    @Composable
    override fun Inhalt() {
        BasisObjektKontext(
            latexInfo("Formel", besitzer.daten, daten.formel()),
            auswahl("Relation", daten.relation.anzeige, ZahlenRelation.entries.map { it.anzeige }) {
                daten.setzeRelation(ZahlenRelation.entries.first { relation -> relation.anzeige == it })
                (besitzer.daten as? MatheKarte.MatheKarteDaten)?.aktualisierePullCaches()
            },
        ).Inhalt()
    }

    class ZahlenRelationDaten(
        override val id: GraphDatenId,
        override val name: String = "Zahlenrelation",
    ) : GraphDatenKnoten, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableMapOf<String, Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)

        val relation: ZahlenRelation
            get() = data[RELATION_SCHLUESSEL]?.toString()?.let { key ->
                ZahlenRelation.entries.firstOrNull { it.name == key }
            } ?: ZahlenRelation.GLEICH

        init {
            data[RELATION_SCHLUESSEL] = ZahlenRelation.GLEICH.name
            listOf(eingang(0), eingang(1), ausgang()).forEachIndexed { index, anschluss ->
                anschlüsse.add(anschluss)
                anschlussIdx[anschluss.id] = if (anschluss.richtung == Richtung.Ausgang) 0 else index
            }
            aktualisiereCache()
        }

        fun setzeRelation(neueRelation: ZahlenRelation) {
            data[RELATION_SCHLUESSEL] = neueRelation.name
            aktualisiereCache()
        }

        fun aktualisiereCache() {
            val wert = werte().mapNotNull { it.toDoubleOrNull() }
            val aussage = if (wert.size == 2) {
                if (relation.pruefe(wert[0], wert[1])) AussageWert.WAHR else AussageWert.LUEGE
            } else {
                AussageWert.UNBEKANNT
            }
            ausgangCache()?.cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(aussage)
        }

        fun formel(): String {
            val argumente = werte().mapIndexed { index, wert -> wert.takeIf { it != "?" } ?: "x_{${index + 1}}" }
            return "${argumente.getOrElse(0) { "x_1" }} ${relation.symbol} ${argumente.getOrElse(1) { "x_2" }}"
        }

        private fun werte(): List<String> =
            anschlüsse
                .filterIsInstance<ZahlenObjektAnschluss.ZahlenAnschlussDaten>()
                .filter { it.istEingang }
                .sortedBy { anschlussIdx[it.id] ?: Int.MAX_VALUE }
                .map { (it.cache as? ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten)?.latex ?: "?" }

        private fun eingang(index: Int) =
            ZahlenObjektAnschluss.ZahlenAnschlussDaten("$id-eingang-$index", Kante.Links, Richtung.Eingang).apply {
                label = "Zahl ${index + 1}"
                klasse = ZahlenObjektAnschluss.EINGANG_ART
            }

        private fun ausgang() =
            AussageObjektAnschluss.AussageAnschlussDaten("$id-ausgang-0", Kante.Rechts, Richtung.Ausgang).apply {
                label = "Aussage"
                klasse = AussageObjektAusgang.ANSCHLUSS_ART
            }

        private fun ausgangCache(): GraphDatenAnschluss.auswertbarerGDA? =
            anschlüsse.filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>().firstOrNull { it.istAusgang }
    }

    enum class ZahlenRelation(val anzeige: String, val symbol: String) {
        GLEICH("=", "="),
        UNGLEICH("!=", "\\ne"),
        KLEINER("<", "<"),
        KLEINER_GLEICH("<=", "\\le"),
        GROESSER(">", ">"),
        GROESSER_GLEICH(">=", "\\ge");

        fun pruefe(links: Double, rechts: Double): Boolean = when (this) {
            GLEICH -> links == rechts
            UNGLEICH -> links != rechts
            KLEINER -> links < rechts
            KLEINER_GLEICH -> links <= rechts
            GROESSER -> links > rechts
            GROESSER_GLEICH -> links >= rechts
        }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "relationZahl"
        const val RELATION_SCHLUESSEL = "zahlen-relation"
    }
}
