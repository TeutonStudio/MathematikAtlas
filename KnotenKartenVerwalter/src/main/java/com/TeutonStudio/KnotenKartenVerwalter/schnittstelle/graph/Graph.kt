package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand

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
    private val knotenKlassen: KnotenFabrik = BasisKnotenFabrik,
    private val verbindungArten: VerbindungArten = VerbindungArten.Standard,
    private val aktualisierung: KartenAktualisierung = { _, _ -> },
    private val onVerbindungErstellen: VerbindungErstellen = {},
    private val onKontextAktion: KontextAktionAusführen = {},
    private val onAuswahlÄndern: AuswahlÄndern = {},
) : GraphObjekt {

    private val karte: Karte
        get() = BasisKarte(
            daten = daten,
            zustand = zustand,
            knotenKlassen = knotenKlassen,
            verbindungArten = verbindungArten,
            aktualisierung = aktualisierung,
            onVerbindungErstellen = onVerbindungErstellen,
            onKontextAktion = onKontextAktion,
            onAuswahlÄndern = onAuswahlÄndern,
        )

    @Composable
    override fun zuComposable(modifier: Modifier) {
        karte.zuComposable(modifier)
    }

    override fun planeVerbindung(a: Anschluss) {
        karte.planeVerbindung(a)
    }

    override fun erstelleVerbindung(von: Anschluss, zu: Anschluss) {
        karte.erstelleVerbindung(von, zu)
    }
}

/**
 * Kompakter Einstieg für alte Aufrufstellen:
 *
 * karte.zuGraphComposable(...)
 */
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
}