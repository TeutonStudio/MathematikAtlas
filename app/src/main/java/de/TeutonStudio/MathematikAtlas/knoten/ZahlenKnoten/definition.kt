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
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.definition as definitionsZeile
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.latexInfo
import de.TeutonStudio.MathematikAtlas.karten.MatheKarte

typealias ZahlenDefinitionDaten = definition.ZahlenDefinitionDaten

class definition(
    override val graph: Graph,
    override val daten: ZahlenDefinitionDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<ZahlenDefinitionDaten>, GraphDatenObjektInspektor<ZahlenDefinitionDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite = 200f
    override val minimaleTiefe = 80f

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
            definitionsZeile("Wert", daten.wert) {
                daten.setzeWert(it)
                (besitzer.daten as? MatheKarte.MatheKarteDaten)?.aktualisierePullCaches()
            },
        ).Inhalt()
    }

    class ZahlenDefinitionDaten(
        initialWert: String = "0",
        override val id: GraphDatenId,
        override val name: String = "Zahl",
    ) : GraphDatenKnoten, GraphDatenKnoten.gerichteteGDK<GraphDatenAnschluss>, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableMapOf<String, Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)
        override val richtung = Richtung.Ausgang
        override val anschlussLabel = mutableStateMapOf<Kante, Map<Int, String>>()

        var wert: String
            get() = data[WERT_SCHLUESSEL]?.toString() ?: "0"
            set(value) {
                data[WERT_SCHLUESSEL] = value
                ausgang()?.cache = ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten(formel())
            }

        init {
            data[WERT_SCHLUESSEL] = initialWert
            val ausgang = erhalteAnschluss(0, Kante.Rechts, "Zahl")
            anschlüsse.add(ausgang)
            anschlussIdx[ausgang.id] = 0
            setzeWert(initialWert)
        }

        override fun erhalteAnschluss(idx: Int, kante: Kante, label: String): GraphDatenAnschluss =
            ZahlenObjektAnschluss.ZahlenAnschlussDaten("$id-ausgang-$idx", kante, Richtung.Ausgang).apply {
                this.label = label
                klasse = ZahlenObjektAnschluss.AUSGANG_ART
            }

        fun formel(): String = wert.ifBlank { "0" }

        fun setzeWert(neuerWert: String) {
            wert = neuerWert.ifBlank { "0" }
        }

        private fun ausgang(): GraphDatenAnschluss.auswertbarerGDA? =
            anschlüsse.filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>().firstOrNull { it.istAusgang }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "definitionZahl"
        const val WERT_SCHLUESSEL = "zahl-wert"
    }
}
