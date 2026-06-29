package de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten

import androidx.compose.foundation.clickable
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
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.karten.MatheKarte
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.latexInfo

typealias ZahlenOperatorDaten = operator.ZahlenOperatorDaten

class operator(
    override val graph: Graph,
    override val daten: ZahlenOperatorDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<ZahlenOperatorDaten>, GraphDatenObjektInspektor<ZahlenOperatorDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite = 220f
    override val minimaleTiefe = 90f

    override val kontextData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.name,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Duplizieren",Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Vernichten",Modifier.clickable { TODO("Kein vernichten implementiert") }) },
    )


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
            auswahl(
                "Operator",
                daten.operator.anzeige,
                ZahlenVerknuepfung.entries.map { it.anzeige },
            ) { wert ->
                daten.setzeOperator(ZahlenVerknuepfung.entries.first { it.anzeige == wert })
                (besitzer.daten as? MatheKarte.MatheKarteDaten)?.aktualisierePullCaches()
            },
        ).Inhalt()
    }

    class ZahlenOperatorDaten(
        override val id: GraphDatenId,
        override val name: String = "Zahlenoperator",
    ) : GraphDatenKnoten, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableMapOf<String, Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)

        val operator: ZahlenVerknuepfung
            get() = data[OPERATOR_SCHLUESSEL]?.toString()?.let { key ->
                ZahlenVerknuepfung.entries.firstOrNull { it.name == key }
            } ?: ZahlenVerknuepfung.ADDITION

        init {
            data[OPERATOR_SCHLUESSEL] = ZahlenVerknuepfung.ADDITION.name
            listOf(
                eingang(0),
                eingang(1),
                erzeugeAusgang(),
            ).forEachIndexed { index, anschluss ->
                anschlüsse.add(anschluss)
                anschlussIdx[anschluss.id] = if (anschluss.richtung == Richtung.Ausgang) 0 else index
            }
            aktualisiereCache()
        }

        fun setzeOperator(neuerOperator: ZahlenVerknuepfung) {
            data[OPERATOR_SCHLUESSEL] = neuerOperator.name
            aktualisiereCache()
        }

        fun aktualisiereCache() {
            ausgang()?.cache = ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten(formel())
        }

        fun formel(): String {
            val argumente = eingangsWerte()
            return operator.latex(argumente)
        }

        private fun eingangsWerte(): List<String> =
            anschlüsse
                .filterIsInstance<ZahlenObjektAnschluss.ZahlenAnschlussDaten>()
                .filter { it.istEingang }
                .sortedBy { anschlussIdx[it.id] ?: Int.MAX_VALUE }
                .mapIndexed { index, anschluss ->
                    (anschluss.cache as? ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten)?.latex
                        ?.takeIf { it != ZahlenObjektAnschluss.UNBEKANNT }
                        ?: "x_{${index + 1}}"
                }

        private fun eingang(index: Int) =
            ZahlenObjektAnschluss.ZahlenAnschlussDaten("$id-eingang-$index", Kante.Links, Richtung.Eingang).apply {
                label = "Zahl ${index + 1}"
                klasse = ZahlenObjektAnschluss.EINGANG_ART
            }

        private fun erzeugeAusgang() =
            ZahlenObjektAnschluss.ZahlenAnschlussDaten("$id-ausgang-0", Kante.Rechts, Richtung.Ausgang).apply {
                label = "Ergebnis"
                klasse = ZahlenObjektAnschluss.AUSGANG_ART
            }

        private fun ausgang(): GraphDatenAnschluss.auswertbarerGDA? =
            anschlüsse.filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>().firstOrNull { it.istAusgang }
    }

    enum class ZahlenVerknuepfung(val anzeige: String, val symbol: String) {
        ADDITION("Addition", "+"),
        SUBTRAKTION("Subtraktion", "-"),
        MULTIPLIKATION("Multiplikation", "\\cdot"),
        DIVISION("Division", "\\frac");

        fun latex(argumente: List<String>): String = when (this) {
            ADDITION -> argumente.joinToString(" + ").eingeklammert()
            SUBTRAKTION -> "${argumente.getOrElse(0) { "x_1" }} - ${argumente.getOrElse(1) { "x_2" }}".eingeklammert()
            MULTIPLIKATION -> argumente.joinToString(" \\cdot ").eingeklammert()
            DIVISION -> "\\frac{${argumente.getOrElse(0) { "x_1" }}}{${argumente.getOrElse(1) { "x_2" }}}"
        }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "operatorZahl"
        const val OPERATOR_SCHLUESSEL = "zahlen-operator"
    }
}

private fun String.eingeklammert(): String = "\\left($this\\right)"
