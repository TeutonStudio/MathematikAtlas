package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
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
    private val kartenKlassen: KartenFabrik = BasisKartenFabrik,
    private val aktualisierung: KartenAktualisierung = { _, _ -> },
    private val onVerbindungErstellen: VerbindungErstellen = {},
    private val onKontextAktion: KontextAktionAusführen = {},
    private val onAuswahlÄndern: AuswahlÄndern = {},
) : GraphObjekt {

    private val karte: Karte
        get() = kartenKlassen.erzeugeKarte(daten) ?: TODO("Fehlerhafte Daten Klassen zuordnung")

    @Composable
    override fun zuComposable(modifier: Modifier) = karte.zuComposable(modifier)

    override fun planeVerbindung(a: Anschluss) = karte.planeVerbindung(a)

    override fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = karte.erstelleVerbindung(von, zu)
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
