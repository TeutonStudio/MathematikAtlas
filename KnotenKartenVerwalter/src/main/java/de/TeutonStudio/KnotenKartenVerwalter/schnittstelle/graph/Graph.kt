package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKarte
//import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.erzeugeKarte

private const val VERBINDUNG_TREFFER_RADIUS = 10f

/**
 * Render-Brücke zwischen fachlichen [KarteDaten] und der interaktiven Kartenoberfläche.
 *
 * Der Graph erzeugt die konkrete Karte über [kartenFabrik] und stellt Hintergrund,
 * Übersicht und Steuerung als gemeinsame Compose-Ebene bereit.
 */
class Graph(
    private val daten: GraphDatenKarte,
    private val zustand: Zustand,
    private val aktualisierung: KartenAktualisierung = { kId,pos -> },
    private val onVerbindungErstellen: VerbindungErstellen = {},
    private val onAuswahlÄndern: AuswahlÄndern = { a -> },
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik,
): GraphHintergrund, GraphKarte, GraphSteuerung {
    override var aktuell: Int = 0
    override val verlauf = mutableStateMapOf<Int,Any>()
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()
    val karte = kartenFabrik.erzeugeKarte(this,daten,zustand,aktualisierung,onVerbindungErstellen,onAuswahlÄndern).apply { registriere() }
    val knoten get() = karte.knoten
    val anschlüsse get() = karte.anschlüsse
    val verbindung get() = karte.verbindungen

    public val selektiertFarbe = Color(0xFF2563EB)

    /**
     * Erstellt die vollständige Compose-Darstellung des Graphen.
     * Sie bindet Karte, Hintergrund, Steuerung und Übersicht in einer Oberfläche zusammen.
     *
     * @param modifier äußerer Modifier der Graphdarstellung
     */
    @Composable public fun Composable(modifier: Modifier) = /*Hintergrund(karte.zustand,75f,modifier)*/ Box {
        karte.ComposableStandard()
        Row(Modifier.padding(16.dp).zIndex(1f).align(Alignment.BottomEnd), Arrangement.spacedBy(8.dp),Alignment.Bottom) {
//            karte.zuSteuerung()
//            karte.zuÜbersicht()
        }
    }

/*    public fun MutableState<AuswahlDaten>.erhalteInspektorObjekt(): GraphObjekt? = when {
        value is EinzelAuswahl -> inhalt.find { it.daten.id == (value as EinzelAuswahl).auswahlId }
        else -> null
    }*/
}
