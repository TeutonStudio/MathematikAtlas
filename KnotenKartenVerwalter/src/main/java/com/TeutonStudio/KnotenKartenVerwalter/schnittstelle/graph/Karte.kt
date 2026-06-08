package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
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
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.erhalteAnschlussIds
import com.TeutonStudio.KnotenKartenVerwalter.erhalteKnotenIds
import com.TeutonStudio.KnotenKartenVerwalter.erhalteNachBildPos
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KartenOberfläche

@Suppress("UNCHECKED_CAST")
val BasisKartenFabrik: KartenFabrik = mapOf(
    BasisKarte.KARTEN_ART to ::BasisKarte as KartenKonstruktor,
)

/*private fun basisKarteKonstruktor(
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
): Karte = BasisKarte(
    daten = daten,
    zustand = zustand,
    knotenFabrik = BasisKnotenFabrik,
    verbindungFabrik = BasisVerbindungFabrik,
    aktualisierung = aktualisierung,
    onVerbindungErstellen = onVerbindungErstellen,
    onKontextAktion = onKontextAktion,
    onAuswahlÄndern = onAuswahlÄndern,
)*/


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
data class AnschlussReferenz(
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
    override val aktualisierung: KartenAktualisierung = { knotenId,position ->  },
    override val onVerbindungErstellen: VerbindungErstellen = {},
    override val onKontextAktion: KontextAktionAusführen = {},
    override val onAuswahlÄndern: AuswahlÄndern = {},
) : Karte {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    val knoten
        get() = daten.knoten.mapNotNull { knotenFabrik.erzeugeKnoten(it,this) }
    val anschlussReferenzen
        get() = knoten.flatMap { it.anschlussReferenzen(zustand) }

    val verbindungen
        get(): List<Verbindung> {
            return daten.verbindungen.mapNotNull { verbindung ->
                val knotenIds = verbindung.ids.erhalteKnotenIds()
                val anschlussIds = verbindung.ids.erhalteAnschlussIds()

                val start = anschlussReferenzen.firstOrNull {
                    it.knotenId == knotenIds.first && it.anschlussId == anschlussIds.first
                }

                val ende = anschlussReferenzen.firstOrNull {
                    it.knotenId == knotenIds.second && it.anschlussId == anschlussIds.second
                }

                if (start == null || ende == null) return@mapNotNull null

                verbindungFabrik.erzeugeVerbindung(
                    daten = verbindung,
                    positionen = start.position.toOffset() to ende.position.toOffset(),
                )
            }
        }

    @Composable
    override fun zuComposable(modifier: Modifier) {
        KartenOberfläche(
            karte = this,
            zustand = zustand,
            knoten = knoten,
            verbindungen = verbindungen,
            modifier = modifier.fillMaxSize().clipToBounds().background(Color(0xFFF8FAFC))
//            .onSizeChanged { fläche = it } TODO
                    ,
            aktualisierung = aktualisierung,
            onVerbindungErstellen = onVerbindungErstellen,
            onKontextAktion = onKontextAktion,
            onAuswahlÄndern = onAuswahlÄndern,
        )
    }

    @Composable
    override fun öffneKontext(pos: BildschirmPosition) {
        TODO("Not yet implemented")
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
