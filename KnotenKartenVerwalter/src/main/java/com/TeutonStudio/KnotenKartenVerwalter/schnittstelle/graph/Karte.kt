package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KartenKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.daten.aktiv.LiveKarte
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KartenOberfläche

@Suppress("UNCHECKED_CAST")
val BasisKartenFabrik: KartenFabrik = mapOf(
    BasisKarte.KARTEN_ART to ::BasisKarte as KartenKonstruktor,
)

/**
 * Trefferziel auf der Karte.
 *
 * Diese Struktur gehoert zur Graph-Logik, nicht zur Compose-Karte:
 * Hit-Testing fragt den Graphen, was unter einer Bildschirmposition liegt.
 */
sealed class KartenTreffer {
    data object Hintergrund : KartenTreffer()
    data class Knoten(val knotenId: String) : KartenTreffer()
    data class Anschluss(
        val knotenId: String,
        val anschlussId: String,
        val richtung: AnschlussRichtung,
    ) : KartenTreffer()

    data class Verbindung(val verbindungId: String) : KartenTreffer()
}

/**
 * Beschreibt eine Aktion aus einem Kontextmenue.
 *
 * Die Karte rendert nur das Menue. Der Graph liefert das Ziel.
 */
data class KartenKontextAktion(
    val ziel: KartenTreffer,
    val weltPosition: Offset,
    val aktion: String,
)

/**
 * Aufgeloeste Anschlussposition eines Knotens.
 *
 * Position liegt in Bildschirmkoordinaten, weil sie fuer Rendering,
 * Hit-Testing und Verbindungsdrag verwendet wird.
 */
internal data class AnschlussReferenz(
    val knotenId: String,
    val anschlussId: String,
    val richtung: AnschlussRichtung?,
    val kante: AnschlussKante,
    val position: BildschirmPosition,
)

/**
 * Karte als GraphObjekt.
 *
 * Diese Datei haelt nur die Objekt-/Klassenebene:
 * - Karte
 * - BasisKarte
 * - minimale zuComposable-Bruecke
 *
 * Die eigentliche Compose-Oberflaeche liegt in KarteComposable.kt.
 */
sealed interface Karte: GraphObjekt {
    val daten: KarteDaten
    val zustand: KarteZustand
    val knotenFabrik: KnotenFabrik
    val verbindungFabrik: VerbindungFabrik
    val aktualisierung: KartenAktualisierung
    val onVerbindungErstellen: VerbindungErstellen
    val onKontextAktion: KontextAktionAusführen
    val onAuswahlÄndern: AuswahlÄndern
}

/**
 * Standardkarte.
 *
 * Analog zum Knoten:
 * - Daten werden gehalten.
 * - Fabriken und Renderarten werden gehalten.
 * - zuComposable delegiert an die Oberflaechen-Datei.
 */
open class BasisKarte(
    override val daten: KarteDaten,
    override val zustand: KarteZustand = KarteZustand(),
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik,
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik,
    override val aktualisierung: KartenAktualisierung = { knotenId,position ->  },
    override val onVerbindungErstellen: VerbindungErstellen = {},
    override val onKontextAktion: KontextAktionAusführen = {},
    override val onAuswahlÄndern: AuswahlÄndern = {},
) : Karte {

    @Composable
    override fun zuComposable(modifier: Modifier) {
        KartenOberfläche(
            daten = daten,
            zustand = zustand,
            knotenFabrik = knotenFabrik,
            verbindungFabrik = verbindungFabrik,
            modifier = modifier,
            aktualisierung = aktualisierung,
            onVerbindungErstellen = onVerbindungErstellen,
            onKontextAktion = onKontextAktion,
            onAuswahlÄndern = onAuswahlÄndern,
        )
    }

    public companion object {
        public const val KARTEN_ART: KnotenArt = "default"
    }
}

/**
 * Kompatibilitaets-Bruecke fuer bisherigen Aufruf:
 *
 *     daten.zuComposable(...)
 */
/*
@Composable
fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    knotenKlassen: KnotenFabrik = BasisKnotenFabrik,
    verbindungArten: VerbindungArten = VerbindungArten.Standard,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
    onAuswahlÄndern: AuswahlÄndern = {},
) {
    BasisKarte(
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
