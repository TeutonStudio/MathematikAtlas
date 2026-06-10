package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
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
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten.Companion.zuAuswahl
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
import com.TeutonStudio.KnotenKartenVerwalter.printLogCat
import com.TeutonStudio.KnotenKartenVerwalter.verschiebe
import com.TeutonStudio.KnotenKartenVerwalter.wechsle
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import com.TeutonStudio.KnotenKartenVerwalter.zuComposable
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.joinToString

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
abstract class Karte(
    _graph: Graph
): GraphObjekt(_graph) {
    abstract override val daten: KarteDaten
    abstract val zustand: KarteZustand
    abstract val knotenFabrik: KnotenFabrik
    abstract val verbindungFabrik: VerbindungFabrik
    abstract val pseudoVerbindung: MutableState<Verbindung?>
    abstract val aktualisierung: KartenAktualisierung
    abstract val onVerbindungErstellen: VerbindungErstellen
    abstract val onKontextAktion: KontextAktionAusführen
    abstract val onAuswahlÄndern: AuswahlÄndern

    val knoten by lazy { daten.knoten.mapNotNull {
        knotenFabrik.erzeugeKnoten(graph, it, this)
    } }
    val refListe by lazy { knoten.flatMap { k -> k.daten.anschlüsse.entries
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })
        .map { (anschluss, idx) -> Pair(anschluss,k.daten) } }.toMap()
        .map { (it.value.id to it.key.id) to it }.toMap() }
    val verbindungen by lazy { daten.verbindungen.mapNotNull {
        val startEntry = refListe[it.ids.erhalteErtes()]
        val endeEntry = refListe[it.ids.erhalteZweites()]

        if (startEntry == null || endeEntry == null) return@mapNotNull null
        val start = derivedStateOf { startEntry.toPair().wechsle().pos().zuBild(zustand.ansicht).toOffset() }
        val ende = derivedStateOf { endeEntry.toPair().wechsle().pos().zuBild(zustand.ansicht).toOffset() }
        verbindungFabrik.erzeugeVerbindung(graph,it,start,ende)
    } }

    @Composable
    override fun zuComposable(modifier: Modifier) {
        Box(
            modifier = modifier
                .draggable2D(
                    state = rememberDraggable2DState {
                        zustand.verschiebe(it)
                        onAuswahlÄndern(AuswahlDaten.LEER)
                        graph.keinKontext()
                    }
                )
                .pointerInput(daten.id) {
                    detectTapGestures(
                        onTap = {
                            // TODO herausfinden, wie ich it. tranformieren muss
                            val v = graph.erhalteVerbindungNachKlick(it)
                            if (v != null && v.second < 10f) {
                                printLogCat(v.first, v.second)
//                                v.first.daten.ausgewaehlt = true
                                onAuswahlÄndern(v.first.daten.zuAuswahl())
                            }
                            onAuswahlÄndern(AuswahlDaten.LEER)
                            graph.keinKontext()
                        },
                        onLongPress = { graph.ctx = daten.id to it.round() },
                    )
                }
        ) {
            verbindungen.zuComposable(Modifier.matchParentSize()/*.pointerInput(verbindungen.joinToString { it.daten.id }) {
                detectTapGestures( // TODO Verbindung nach klickpunkt ermitteln
                    onTap = {},
                    onLongPress = {}
                )
            }*/)
            pseudoVerbindung.value?.zuComposable()
            knoten.zuComposable({ d -> Modifier},{d -> { a,idx -> Modifier }})
            if (öffneKontext.value) erhalteKontextFenster(graph.ctx.second)
        }
    }

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
): Karte(_graph) {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val pseudoVerbindung = mutableStateOf<Verbindung?>(null)

/*    @Composable
    override fun zuComposable(modifier: Modifier) {
        var ctx by remember { mutableStateOf(false) }
        var ctxPos by remember { mutableStateOf(IntOffset.Zero) }
        Box(
            modifier = modifier.draggable2D(
                state = rememberDraggable2DState {
                    zustand.verschiebe(it)
                    onAuswahlÄndern(AuswahlDaten.LEER)
                    ctx = false
                }
            ).pointerInput(daten.id) {
                detectTapGestures(
                    onTap = {
                        onAuswahlÄndern(AuswahlDaten.LEER)
                        ctx = false
                    },
                    onLongPress = { ctx = true; ctxPos = it.round() },
                )
            }
        ) {
            verbindungen.zuComposable({ d -> Modifier.fillMaxSize() })
            knoten.zuComposable({ d -> Modifier},{d -> { a,idx -> Modifier }})
            if (ctx) öffneKontext(ctxPos)
        }
    }*/

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
        Box(
            modifier = Modifier.offset { pos }
        ) {
            Text("Kontextfenster der Karte")
        }
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
