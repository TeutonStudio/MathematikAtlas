package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss.Companion.findMann
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss.Companion.findWeib
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeVerbindung

private const val VERBINDUNG_TREFFER_RADIUS = 10f

typealias veränderung = (GraphDatenId, GraphPosition) -> Unit
typealias verbindete = (GraphDatenObjektAnschluss<*>,GraphDatenObjektAnschluss<*>) -> Unit
typealias wählte = (Auswahl) -> Unit

class Graph(
    private val daten: GraphDatenKarte,
//    private val zustand: Zustand,
    private val veränderung: veränderung = { kId, pos -> },
    private val verbindete: verbindete = { a1, a2 -> },
    private val wählte: wählte = { a -> },
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik,
): GraphHintergrund, GraphKarte, GraphSteuerung {
    override var aktuell: Int = 0
    override val verlauf = mutableStateMapOf<Int,Any>()
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()
    val karte = kartenFabrik.erzeugeKarte(this,daten,veränderung,verbindete,wählte).apply { registriere() }
    val knoten by GraphCache({ daten.knoten }) { k: GraphDatenKnoten ->
        karte.knotenFabrik.erzeugeKnoten(this,k,karte)
    }
    val anschlüsse by GraphCache({ karte.daten.knoten.flatMap { it.anschlüsse } }) { d: GraphDatenAnschluss ->
        knoten.find { it.daten.anschlüsse.contains(d) }?.let { it.anschlussFabrik.erzeugeAnschluss(this,d,it)?.apply { registriere() } }
    }
    private fun verbindungsDatenMitPositioniertenEndpunkten() =
        karte.daten.verbindungen.filter { verbindung ->
            val mann = anschlüsse.findMann(verbindung.ids)
            val weib = anschlüsse.findWeib(verbindung.ids)

            mann?.layoutCoordinates?.value != null &&
                    weib?.layoutCoordinates?.value != null
        }

    val verbindungen by GraphCache({ verbindungsDatenMitPositioniertenEndpunkten() }) { v: GraphDatenVerbindung ->
        anschlüsse.findMann(v.ids)?.let { aM -> anschlüsse.findWeib(v.ids)?.let { aW ->
            karte.verbindungFabrik.erzeugeVerbindung(this,v, derivedStateOf { aM.pos }, derivedStateOf { aW.pos })
        } }
    }

    public val selektiertFarbe = Color(0xFF2563EB)

    @Composable public fun Composable(modifier: Modifier) = /*Hintergrund(karte.zustand,75f,modifier)*/ Box(modifier.wrapContentSize()) {
        karte.ComposableStandard()
        Row(Modifier
            .padding(16.dp)
            .zIndex(1f)
            .align(Alignment.BottomEnd), Arrangement.spacedBy(8.dp),Alignment.Bottom) {
//            karte.zuSteuerung()
//            karte.zuÜbersicht()
        }
    }

}
