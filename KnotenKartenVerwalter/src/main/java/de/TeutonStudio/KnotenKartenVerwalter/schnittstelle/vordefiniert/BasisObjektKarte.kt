package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Kontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindete
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.veränderung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.wählte

class BasisObjektKarte(
    override val graph: Graph,
    override val daten: GraphDatenKarte,
    veränderung: veränderung,
    verbindete: verbindete,
    wählte: wählte,
) : GraphDatenObjektKarte<GraphDatenKarte> {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    override val kontextData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.name,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Duplizieren",Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Vernichten",Modifier.clickable { TODO("Kein vernichten implementiert") }) },
    )

    override fun beiKlick(klickPos: Offset) { super.beiKlick(klickPos) }
    override fun beiHalten(klickPos: Offset) { super.beiHalten(klickPos) }
    override fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float) { super.beiTransform(centroid, zoomDelta, panDelta, rotationChange) }

    override fun definiereVerbindung(
        mann: GraphDatenObjektAnschluss<*>,
        weib: GraphDatenObjektAnschluss<*>
    ) {
        super.definiereVerbindung(mann, weib)
        daten.verbindungen.plus(
            BasisObjektVerbindung(graph, BasisDatenVerbindung(
                mann.daten.id+"-"+weib.daten.id,
                GraphDatenVerbindung.IDEhe(mann,weib)
            ),
            derivedStateOf { mann.pos },
            derivedStateOf { weib.pos },
            )
        )
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        Card(Modifier.offset { pos }.padding(4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(daten.name)
                Text("Knoten: ${daten.knoten.size}")
                Text("Verbindungen: ${daten.verbindungen.size}")
            }
        }
    }

/*    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }*/

    override val pseudoVerbindung: MutableState<GraphDatenObjektVerbindung<*>?> = mutableStateOf(null)
    override val zustand: Zustand = Zustand()
    override val auswahl: Auswahl = Auswahl()
    override val ctx: Kontext = Kontext()

    public companion object {
        public const val KARTEN_ART = "default"
    }
}
