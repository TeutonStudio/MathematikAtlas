package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KartenKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.erhalteErtes
import com.TeutonStudio.KnotenKartenVerwalter.erhalteNachBildPos
import com.TeutonStudio.KnotenKartenVerwalter.erhalteZweites
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.plusVlt
import com.TeutonStudio.KnotenKartenVerwalter.pos
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KartenOberfläche
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import kotlin.collections.component1
import kotlin.collections.component2

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
    val pseudoVerbindung: MutableState<Verbindung?>
    val aktualisierung: KartenAktualisierung
    val onVerbindungErstellen: VerbindungErstellen
    val onKontextAktion: KontextAktionAusführen
    val onAuswahlÄndern: AuswahlÄndern

/*    override fun definiereGraph(g: Graph) {
        super.definiereGraph(g)
        graph = g
    }*/
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
    _graph: Graph,
    override val daten: KarteDaten,
    override val zustand: KarteZustand = KarteZustand(),
    override val aktualisierung: KartenAktualisierung,
    override val onVerbindungErstellen: VerbindungErstellen,
    override val onKontextAktion: KontextAktionAusführen,
    override val onAuswahlÄndern: AuswahlÄndern,
) : Karte {
    override lateinit var graph: Graph
    init { definiereGraph(_graph) }
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val pseudoVerbindung = mutableStateOf<Verbindung?>(null)

    @Composable
    override fun zuComposable(modifier: Modifier) {
        val knotenListe = remember(daten.knoten) {
            graph.inhalt.filterIsInstance<Knoten>().forEach {
                graph.inhalt.remove(it)
            } // TODO ist korrekte entfernung alter GraphObjekte
            daten.knoten.mapNotNull { knotenFabrik.erzeugeKnoten(graph, it,this) }
        }
        val referenz = remember(knotenListe) { knotenListe.flatMap { it.anschlussReferenzen(zustand) } }.toMap()
        val verbindungListe = remember(daten.verbindungen,knotenListe) { // referenz nicht als Key, da alle key von referenz auch hier key sind
            val refIdx = referenz.map { (it.value.id to it.key.id) to it }.toMap()
            graph.inhalt.filterIsInstance<Verbindung>().forEach {
                graph.inhalt.remove(it)
            } // TODO ist korrekte entfernung alter GraphObjekte
            daten.verbindungen.mapNotNull { verbindung ->
                val startEntry = refIdx[verbindung.ids.erhalteErtes()]
                val endeEntry = refIdx[verbindung.ids.erhalteZweites()]

                if (startEntry == null || endeEntry == null) return@mapNotNull null
                val start = derivedStateOf { pos(startEntry).zuBild(zustand.ansicht).toOffset() }
                val ende = derivedStateOf { pos(endeEntry).zuBild(zustand.ansicht).toOffset() }
                verbindungFabrik.erzeugeVerbindung(graph,verbindung,start,ende)
            }
        } // TODO Herausfinden, wie GraphObjekte einmalig erzeugt und dann verändert, statt neu erzeugt werden können

        KartenOberfläche(
            karte = this,
            zustand = zustand,
            erhalteNachBildPos = graph::erhalteNachBildPos,
            knoten = knotenListe,
            verbindungen = verbindungListe.plusVlt(pseudoVerbindung.value),
            modifier = modifier.fillMaxSize().clipToBounds().background(Color(0xFFF8FAFC)),
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

    private fun pos(arg: Map.Entry<AnschlussDaten, KnotenDaten>): KartenPosition = (arg.value to arg.key).pos()

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
