package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeKarte

private const val VERBINDUNG_TREFFER_RADIUS = 10f

typealias veränderung = (GraphDatenId, GraphPosition) -> Unit
typealias verbindete = (GraphDatenObjektAnschluss<*>,GraphDatenObjektAnschluss<*>) -> Unit
typealias wählte = (Auswahl) -> Unit

class Graph(
    private val daten: GraphDatenKarte,
    private val zustand: Zustand,
    private val veränderung: veränderung = { kId, pos -> },
    private val verbindete: verbindete = { a1, a2 -> },
    private val wählte: wählte = { a -> },
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik,
): GraphHintergrund, GraphKarte, GraphSteuerung {
    override var aktuell: Int = 0
    override val verlauf = mutableStateMapOf<Int,Any>()
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()
    val karte = kartenFabrik.erzeugeKarte(this,daten,zustand,veränderung,verbindete,wählte).apply { registriere() }
    val knoten get() = karte.knoten
    val anschlüsse get() = karte.anschlüsse
    val verbindung get() = karte.verbindungen

    public val selektiertFarbe = Color(0xFF2563EB)

    @Composable public fun Composable(modifier: Modifier) = /*Hintergrund(karte.zustand,75f,modifier)*/ Box {
        karte.ComposableStandard()
        Row(Modifier.padding(16.dp).zIndex(1f).align(Alignment.BottomEnd), Arrangement.spacedBy(8.dp),Alignment.Bottom) {
//            karte.zuSteuerung()
//            karte.zuÜbersicht()
        }
    }

}
