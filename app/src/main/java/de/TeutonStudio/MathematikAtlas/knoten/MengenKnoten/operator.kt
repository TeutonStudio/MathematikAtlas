package de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten

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
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.MengenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.karten.MatheKarte
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.latexInfo

typealias MengenOperatorDaten = operator.MengenOperatorDaten

class operator(
    override val graph: Graph,
    override val daten: MengenOperatorDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<MengenOperatorDaten>, GraphDatenObjektInspektor<MengenOperatorDaten> {
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

    @Composable override fun BoxScope.KontextFenster(pos: IntOffset) = StandardKontextFenster(pos)
    @Composable override fun BoxScope.Inspektor() = Composable()

    @Composable
    override fun Inhalt() {
        BasisObjektKontext(
            latexInfo("Formel", besitzer.daten, daten.formel()),
            auswahl("Operator", daten.operator.anzeige, MengenVerknuepfung.entries.map { it.anzeige }) {
                daten.setzeOperator(MengenVerknuepfung.entries.first { op -> op.anzeige == it })
                (besitzer.daten as? MatheKarte.MatheKarteDaten)?.aktualisierePullCaches()
            },
        ).Inhalt()
    }

    class MengenOperatorDaten(
        override val id: GraphDatenId,
        override val name: String = "Mengenoperator",
    ) : GraphDatenKnoten, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableMapOf<String, Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)

        val operator: MengenVerknuepfung
            get() = data[OPERATOR_SCHLUESSEL]?.toString()?.let { key ->
                MengenVerknuepfung.entries.firstOrNull { it.name == key }
            } ?: MengenVerknuepfung.VEREINIGUNG

        init {
            data[OPERATOR_SCHLUESSEL] = MengenVerknuepfung.VEREINIGUNG.name
            listOf(eingang(0), eingang(1), ausgang()).forEachIndexed { index, anschluss ->
                anschlüsse.add(anschluss)
                anschlussIdx[anschluss.id] = if (anschluss.richtung == Richtung.Ausgang) 0 else index
            }
            aktualisiereCache()
        }

        fun setzeOperator(neuerOperator: MengenVerknuepfung) {
            data[OPERATOR_SCHLUESSEL] = neuerOperator.name
            aktualisiereCache()
        }

        fun aktualisiereCache() {
            ausgangCache()?.cache = MengenObjektAnschluss.MengenAnschlussDaten.CacheDaten(formel())
        }

        fun formel(): String = operator.latex(eingangsWerte())

        private fun eingangsWerte(): List<String> =
            anschlüsse
                .filterIsInstance<MengenObjektAnschluss.MengenAnschlussDaten>()
                .filter { it.istEingang }
                .sortedBy { anschlussIdx[it.id] ?: Int.MAX_VALUE }
                .mapIndexed { index, anschluss ->
                    (anschluss.cache as? MengenObjektAnschluss.MengenAnschlussDaten.CacheDaten)?.latex
                        ?.takeIf { it != MengenObjektAnschluss.UNBEKANNT }
                        ?: "M_{${index + 1}}"
                }

        private fun eingang(index: Int) =
            MengenObjektAnschluss.MengenAnschlussDaten("$id-eingang-$index", Kante.Links, Richtung.Eingang).apply {
                label = "Menge ${index + 1}"
                klasse = MengenObjektAnschluss.EINGANG_ART
            }

        private fun ausgang() =
            MengenObjektAnschluss.MengenAnschlussDaten("$id-ausgang-0", Kante.Rechts, Richtung.Ausgang).apply {
                label = "Ergebnis"
                klasse = MengenObjektAnschluss.AUSGANG_ART
            }

        private fun ausgangCache(): GraphDatenAnschluss.auswertbarerGDA? =
            anschlüsse.filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>().firstOrNull { it.istAusgang }
    }

    enum class MengenVerknuepfung(val anzeige: String, val symbol: String) {
        VEREINIGUNG("Vereinigung", "\\cup"),
        SCHNITT("Schnitt", "\\cap"),
        DIFFERENZ("Differenz", "\\setminus"),
        KARTESISCH("Kartesisches Produkt", "\\times");

        fun latex(argumente: List<String>): String =
            argumente.joinToString(" $symbol ").ifBlank { "\\emptyset" }.eingeklammert()
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "operatorMenge"
        const val OPERATOR_SCHLUESSEL = "mengen-operator"
    }
}

private fun String.eingeklammert(): String = "\\left($this\\right)"
