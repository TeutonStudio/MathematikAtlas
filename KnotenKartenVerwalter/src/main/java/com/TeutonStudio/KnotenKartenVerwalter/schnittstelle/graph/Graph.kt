package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.abstandZuBezier
import com.TeutonStudio.KnotenKartenVerwalter.abstandZuVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.aufKnoten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.erhalteZoomfaktor
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeKarte
import com.TeutonStudio.KnotenKartenVerwalter.tangente
import com.TeutonStudio.KnotenKartenVerwalter.zuKarte
import kotlin.math.hypot
import kotlin.math.max

private const val ANSCHLUSS_TREFFER_RADIUS = 14f
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
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik
    val karte = kartenFabrik.erzeugeKarte(this,daten,zustand,aktualisierung,onVerbindungErstellen,onKontextAktion,onAuswahlÄndern)

    public val selektiert
        get() = karte.zustand.auswahl.value
    public val selektiertFarbe = Color(0xFF2563EB)
    public var ctx by mutableStateOf<Pair<String, IntOffset>>("" to IntOffset.Zero)

    public fun keinKontext() { ctx = "" to IntOffset.Zero }

    public fun erhalteVerbindungNachKlick(pos: Offset): Pair<Verbindung,Float>? {
        val liste = inhalt.filterIsInstance<Verbindung>().map {
            val bezier = listOf(it.start.value,it.c1(),it.c2(),it.ende.value)
            it to pos.abstandZuBezier(bezier)
        }; if (liste.isEmpty()) return null
        return liste.minBy { it.second }
    }

    public fun wähle(wahl: AuswahlDaten) {
        karte.zustand.auswahl.value = wahl
        karte.onAuswahlÄndern(wahl)
    }

    public fun verschiebeKnoten(id: String, um: Offset) {
        val k = karte.knoten.filter { it.daten.id == id }
        if (k.isEmpty()) return
        k[0].daten.position += um / karte.zustand.zoom
//        karte.aktualisierung(id,nach)
    }

    public fun erhaltePseudoAnschlussZiel(): Pair<Anschluss,Float> {
        val p = karte.pseudoVerbindung.value?.ende?.value ?: KartenPosition.Zero
        val nA = erhalteAnschlussNachKartePos(p)
        return nA to (p-nA.erhaltePosition()).getDistanceSquared()
    }

    public fun erhalteAnschlussNachKartePos(pos: BildschirmPosition): Anschluss = erhalteAnschlussNachKartePos(pos.zuKarte(karte.zustand))
    public fun erhalteAnschlussNachKartePos(pos: KartenPosition): Anschluss = inhalt.filterIsInstance<Anschluss>().filter { it.daten.id != "pseudo" }.minBy { (it.erhaltePosition() - pos).getDistanceSquared() }

    @Composable
    public fun zuComposable(modifier: Modifier) = karte.zuComposable(modifier)
}
