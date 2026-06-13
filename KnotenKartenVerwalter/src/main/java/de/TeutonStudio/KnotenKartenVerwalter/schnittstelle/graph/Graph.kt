package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.erzeugeKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung

private const val VERBINDUNG_TREFFER_RADIUS = 10f

/**
 * Graph ist die dünne Render-Brücke zwischen fachlichen Kartendaten und
 * der interaktiven Kartenoberfläche.
 *
 * Die Karte selbst bleibt damit ein konkretes GraphObjekt, aber der aufrufende
 * Code muss nicht mehr direkt BasisKarte kennen. Navigation/Testapps erzeugen
 * nur noch einen Graph aus KarteDaten und Callbacks.
 */
class Graph(
    private val daten: KarteDaten,
    private val zustand: KarteZustand,
    private val aktualisierung: KartenAktualisierung = { kId,pos -> },
    private val onVerbindungErstellen: VerbindungErstellen = {},
//    private val onKontextAktion: KontextAktionAusführen = {},
    private val onAuswahlÄndern: AuswahlÄndern = { a -> },
): GraphHintergrund, GraphKarte, GraphSteuerung {
    override var aktuell: Int = 0
    override val verlauf = mutableStateMapOf<Int,Any>()
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()
    val karte = kartenFabrik.erzeugeKarte(this,daten,zustand,aktualisierung,onVerbindungErstellen,/*onKontextAktion,*/onAuswahlÄndern).apply { registriere() }
    val knoten get() = karte.knoten
    val anschlüsse get() = karte.anschlüsse
    val verbindung get() = karte.verbindungen

    public val selektiertFarbe = Color(0xFF2563EB)

    @Composable public fun zuComposable(modifier: Modifier) = Hintergrund(karte.zustand,75f,Modifier) {
        karte.zuComposable(/*modifier.matchParentSize()*/)
        Row(Modifier.padding(16.dp).zIndex(1f).align(Alignment.BottomEnd), Arrangement.spacedBy(8.dp),Alignment.Bottom) {
            karte.zuSteuerung()
            karte.zuÜbersicht()
        }
    }

    public fun MutableState<AuswahlDaten>.erhalteInspektorObjekt(): GraphObjekt? = when {
        value is EinzelAuswahl -> inhalt.find { it.daten.id == (value as EinzelAuswahl).auswahlId }
        else -> null
    }
}
