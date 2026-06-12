package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.erzeugeKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung

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
    private val onKontextAktion: KontextAktionAusführen = {},
    private val onAuswahlÄndern: AuswahlÄndern = { a -> },
) {
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik
    public val inhalt: MutableList<GraphObjekt<out GraphDaten>> = mutableListOf()
    val karte = kartenFabrik.erzeugeKarte(this,daten,zustand,aktualisierung,onVerbindungErstellen,onKontextAktion,onAuswahlÄndern).apply { registriere() }
    val knoten get() = karte.knoten
    val anschlüsse get() = karte.anschlüsse
    val verbindung get() = karte.verbindungen

    public val selektiert get() = karte.zustand.auswahl.value
    public val selektiertFarbe = Color(0xFF2563EB)
    public var ctx by mutableStateOf<Pair<String, IntOffset>>("" to IntOffset.Zero)

    public fun keinKontext() { ctx = "" to IntOffset.Zero }

    public fun erhalteVerbindungNachPos(pos: KartenPosition): Pair<Verbindung,Offset>? = verbindung.map { it to it.abstand(pos) }.minByOrNull { it.second.getDistanceSquared() }
    public fun erhalteAnschlussNachPos(pos: KartenPosition): Pair<Anschluss<out AnschlussDaten>,Offset>? = anschlüsse.map { it to it.pos - pos  }.minByOrNull { it.second.getDistanceSquared() }

    public fun wähle(wahl: AuswahlDaten = AuswahlDaten.LEER) {
        karte.zustand.auswahl.value = wahl
        karte.onAuswahlÄndern(wahl)
    }

    public fun verschiebeKnoten(id: String, um: Offset) {
        val k = karte.knoten.filter { it.daten.id == id }
        if (k.isEmpty()) return
        k[0].daten.position += um / karte.zustand.zoom
//        karte.aktualisierung(id,nach)
    }

    public fun definiereVerbindung(mann: Anschluss<out AnschlussDaten>, weib: Anschluss<out AnschlussDaten>) {
        karte.daten.verbindungen.add(VerbindungDaten(
            setOf(mann.daten.id,weib.daten.id).joinToString("-"),
            mann.besitzer.daten.id to weib.besitzer.daten.id,
            mann.daten.id to weib.daten.id,
            "",null
        ))
        // TODO karte neu erzeugen
    }

    public fun erhaltePseudoAnschlussZiel(): Pair<Anschluss<out AnschlussDaten>,Float> {
        val p = karte.pseudoVerbindung.value?.ende?.value ?: KartenPosition.Zero
        val nA = erhalteAnschlussNachKartePos(p)
        return nA to (p-nA.pos).getDistanceSquared()
    }

//    public fun erhalteAnschlussNachKartePos(pos: BildschirmPosition): Anschluss = erhalteAnschlussNachKartePos(pos.zuKarte(karte.zustand))
    public fun erhalteAnschlussNachKartePos(pos: KartenPosition): Anschluss<out AnschlussDaten> = anschlüsse.filter { it.daten.id != "pseudo" }.minBy { (it.pos - pos).getDistanceSquared() }

    @Composable
    public fun zuComposable(modifier: Modifier) = karte.zuComposable(modifier)
}
