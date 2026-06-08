package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeKarte

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
    private val zustand: KarteZustand = KarteZustand(),
    private val aktualisierung: KartenAktualisierung = { kId,pos -> },
    private val onVerbindungErstellen: VerbindungErstellen = {},
    private val onKontextAktion: KontextAktionAusführen = {},
    private val onAuswahlÄndern: AuswahlÄndern = { a -> },
) {
    private val kartenFabrik: KartenFabrik = BasisKartenFabrik
    public val inhalt: MutableList<GraphObjekt> = mutableListOf()

    public fun erhalteNachBildPos(pos: BildschirmPosition, zustand: KarteZustand): GraphObjekt {
        val karte = inhalt.filterIsInstance<Karte>().first()
        val knoten = inhalt.filterIsInstance<Knoten>()
        val verbindung = inhalt.filterIsInstance<Verbindung>()
        // TODO
        return karte
    }

    @Composable
    public fun zuComposable(modifier: Modifier) {
        val karte = remember(daten) {
            kartenFabrik.erzeugeKarte(
                graph = this,
                daten = daten,
                zustand = zustand,
                aktualisierung = { kId,pos ->
                    val knoten = daten.knoten.filter { it.id == kId }
                    if (knoten.size != 1) TODO("Knoten ID Fehler")
                    knoten[0].position = pos
                    aktualisierung(kId,pos)
//        scope.invalid() // TODO wie??
                },
                onVerbindungErstellen = onVerbindungErstellen,
                onKontextAktion = onKontextAktion,
                onAuswahlÄndern = { a ->
                    daten.knoten.forEach {
                        it.ausgewaehlt = it.id in a.knotenIds
                    }
                    daten.verbindungen.forEach {
                        it.ausgewaehlt = it.id in a.verbindungIds
                    }
                    onAuswahlÄndern(a)
                },
            )
        }
        karte.zuComposable(modifier)
    }
}

/**
 * Kompakter Einstieg für alte Aufrufstellen:
 *
 * karte.zuGraphComposable(...)
 */
/*
@Composable
fun KarteDaten.zuGraphComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    knotenKlassen: KnotenFabrik = BasisKnotenFabrik,
    verbindungArten: VerbindungArten = VerbindungArten.Standard,
    aktualisierung: KartenAktualisierung = { _, _ -> },
    onVerbindungErstellen: (VerbindungDaten) -> Unit = {},
    onKontextAktion: (KartenKontextAktion) -> Unit = {},
    onAuswahlÄndern: (AuswahlDaten) -> Unit = {},
) {
    Graph(
        daten = this,
        zustand = zustand,
        knotenKlassen = knotenKlassen,
        verbindungArten = verbindungArten,
        aktualisierung = aktualisierung,
        onVerbindungErstellen = onVerbindungErstellen,
        onKontextAktion = onKontextAktion,
        onAuswahlÄndern = onAuswahlÄndern,
    ).zuComposable(modifier)
}*/
