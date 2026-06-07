package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition

//
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.VerbindungUmgebung
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungArt
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungKonstruktor

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.istEingang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.VerbindungPfad
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import kotlin.invoke

// Kotlin
import kotlin.math.hypot

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisVerbindung.VERBINDUNG_ART to ::BasisVerbindung as VerbindungKonstruktor,
)

/**
 * Erstellt eine Zeichenfunktion für eine einzelne Verbindung.
 *
 * Die eigentliche Geometrie wird erst im Canvas-DrawScope gezeichnet, damit alle
 * Verbindungen gemeinsam auf einer Canvas-Ebene hinter den Knoten liegen können.
 */
// private fun VerbindungDaten.zuPfad(start: Offset, ende: Offset): DrawScope.() -> Unit = { VerbindungPfad(this@zuPfad, start, ende) }

/**
 * Rendert eine einzelne Verbindung zwischen zwei Bildschirmpositionen.
 */
// @Composable
// public fun VerbindungDaten.zuComposable(start: Offset, ende: Offset, modifier: Modifier = Modifier) = BasisVerbindung(this, start = start, ende = ende).zuComposable(modifier)

/**
 * Rendert eine Liste fachlicher Verbindungen.
 *
 * Die übergebenen Funktionen lösen die referenzierten Anschlusspositionen auf.
 * Verbindungen mit fehlenden Endpunkten werden übersprungen.
 */
@Composable
public fun List<Verbindung>.zuComposable(modifier: Modifier = Modifier) = VerbindungUmgebung(modifier,this.map { it.zeichnung() })

/**
 * Rendert eine Liste fachlicher Verbindungen.
 *
 * Die übergebenen Funktionen lösen die referenzierten Anschlusspositionen auf.
 * Verbindungen mit fehlenden Endpunkten werden übersprungen.
 */
@Composable
public fun List<VerbindungDaten>.zuComposable(
    start: (VerbindungDaten) -> KartenPosition,
    ende: (VerbindungDaten) -> KartenPosition,
    modifier: Modifier = Modifier,
    fabrik: VerbindungFabrik = BasisVerbindungFabrik,
) = this.mapNotNull { fabrik.erzeugeVerbindung(it,null,start.invoke(it) to ende.invoke(it)) }.zuComposable(modifier)

/**
 * Rendert bereits aufgelöste Verbindungen.
 *
 * Diese Variante wird unter anderem für die temporäre Verbindung beim Ziehen
 * eines Anschlusses verwendet.
 */
/*@Composable
public fun List<Triple<VerbindungDaten, Offset, Offset>>.zuComposable(modifier: Modifier = Modifier) = VerbindungUmgebung(
    modifier,
    this.map { BasisVerbindung(it.first, start = it.second, ende = it.third).zeichnung() },
)*/

// TODO KartenPosition nicht eher was für Daten ??
sealed interface Verbindung: GraphObjekt {
    public val daten: VerbindungDaten
    public val von: Anschluss?
    public val zu: Anschluss?
    public val start: KartenPosition
    public val ende: KartenPosition

    public fun zeichnung(): DrawScope.() -> Unit
}

open class BasisVerbindung(
    override val daten: VerbindungDaten,
    override val von: Anschluss? = null,
    override val zu: Anschluss? = null,
    override val start: KartenPosition = Offset.Zero,
    override val ende: KartenPosition = Offset.Zero,
): Verbindung {
    @Composable
    override fun zuComposable(modifier: Modifier) = VerbindungUmgebung(modifier, zeichnung())

    override fun zeichnung(): DrawScope.() -> Unit = { VerbindungPfad(daten,start,ende) }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "default"
    }
}

/**
 * Temporärer Zustand waehrend eine neue Verbindung gezogen wird.
 *
 * Diese Datei kennt keine Karte und keine Compose-Oberflaeche.
 * Sie weiss nur: Startanschluss, aktuelle Pointerposition, Zielsuche,
 * Kompatibilitaet, Verbindung erzeugen.
 */
internal data class VerbindungsDrag(
    val start: AnschlussReferenz,
    val aktuellePosition: BildschirmPosition,
)

/**
 * Baut die Vorschau-Verbindung fuer den Canvas-Layer.
 *
 * Wenn der Drag an einem Eingang startet, wird die Vorschau visuell gedreht,
 * damit die Bezier-Tangenten weiterhin passend aussehen.
 */
internal fun VerbindungsDrag.zuVorschau(): Triple<VerbindungDaten, BildschirmPosition, BildschirmPosition> {
    val startPosition = if (start.richtung.istEingang()) { aktuellePosition } else { start.position }
    val endePosition = if (start.richtung == AnschlussRichtung.Eingang) { start.position } else { aktuellePosition }

    return Triple(
        VerbindungDaten(
            id = "temporaer",
            ids = ("" to "") to (start.anschlussId to start.anschlussId),
            klasse = BasisVerbindung.VERBINDUNG_ART,
            ausgewaehlt = true,
        ),
        startPosition,
        endePosition,
    )
}

/**
 * Pointer-Interaktion fuer das Ziehen einer Verbindung ab einem Anschluss.
 *
 * Der Graph wird als Lambda uebergeben, damit der Pointer-Handler auch nach
 * Recompositionen aktuelle Knoten, Anschluesse und Verbindungen sieht.
 */
/*internal fun Modifier.verbindungsZiehen(
    start: AnschlussReferenz,
    graph: () -> Karte,
    onDragAendern: (VerbindungsDrag?) -> Unit,
    onZiehtAnschlussAendern: (Boolean) -> Unit,
    onBlockiereHintergrundGestenAendern: (Boolean) -> Unit,
    onVerbindungErstellen: (VerbindungDaten) -> Unit,
//    regeln: VerbindungsRegeln = VerbindungsRegeln(),
    maxZielAbstand: Float = 28f,
): Modifier =
    pointerInput(start.knotenId, start.anschlussId, start.position) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var lokalerDrag = VerbindungsDrag(
                start = start,
                aktuellePosition = start.position,
            )

            try {
                onZiehtAnschlussAendern(true)
                onBlockiereHintergrundGestenAendern(true)
                onDragAendern(lokalerDrag)

                down.consume()

                drag(down.id) { change ->
                    change.consume()
                    change.scaleFactor // TODO für zoom verwenden
                    change.panOffset // TODO vlt besser als selbst auszurechnen
                    val rel = change.position - change.previousPosition // TODO vlt. panOffset
                    lokalerDrag = lokalerDrag.copy(
                        aktuellePosition = lokalerDrag.aktuellePosition + IntOffset(rel.x.toInt(),rel.y.toInt())
                    )

                    onDragAendern(lokalerDrag)
                }

                val aktuellerGraph = graph()
                val ziel = lokalerDrag.aktuellePosition.naechsterAnschluss(
                    anschluesse = aktuellerGraph.daten, //.erhalteAnschlüsse(referenz),
                    maxAbstand = maxZielAbstand,
                )

                if (ziel != null) {
                    val verbindung = lokalerDrag.start.zuVerbindungOderNull(
                        ziel = ziel,
                        vorhandeneVerbindungen = aktuellerGraph.daten.verbindungen,
//                        regeln = regeln,
                    )

                    if (verbindung != null) {
                        onVerbindungErstellen(verbindung)
                    }
                }
            } finally {
                onDragAendern(null)
                onZiehtAnschlussAendern(false)
                onBlockiereHintergrundGestenAendern(false)
            }
        }
    }*/

/**
 * Erstellt eine Verbindung, falls Start und Ziel fachlich kompatibel sind.
 */
internal fun AnschlussReferenz.zuVerbindungOderNull(
    ziel: AnschlussReferenz,
    vorhandeneVerbindungen: List<VerbindungDaten>,
//    regeln: VerbindungsRegeln = VerbindungsRegeln(),
): VerbindungDaten? {
    if (knotenId == ziel.knotenId) return null
    if (richtung == ziel.richtung) return null

    val quelle = if (richtung == AnschlussRichtung.Ausgang) this else ziel
    val ende = if (richtung == AnschlussRichtung.Eingang) this else ziel

    val verbindung = VerbindungDaten(
        id = "verbindung-${quelle.knotenId}-${quelle.anschlussId}-${ende.knotenId}-${ende.anschlussId}",
        ids = (quelle.knotenId to ende.knotenId) to (quelle.anschlussId to ende.anschlussId),
        klasse = BasisVerbindung.VERBINDUNG_ART
    ) /*.mitTypPruefung(
        quellTyp = quelle.zahlenTyp,
        zielTyp = ende.zahlenTyp,
    )*/

/*    val erlaubt = regeln.darfErstellen( TODO
        vorhandeneVerbindungen = vorhandeneVerbindungen,
        neueVerbindung = verbindung,
        quellRichtung = quelle.richtung,
        zielRichtung = ende.richtung,
    )*/

    return if (true) verbindung else null
}

/**
 * Loest alle gerichteten Anschluesse eines Knotens in Bildschirmpositionen auf.
 */
internal fun Knoten.anschlussReferenzen(zustand: KarteZustand): List<AnschlussReferenz> =
    daten.anschlüsse.entries
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })
        .mapNotNull { (anschluss, _) -> anschlussReferenz(anschluss, zustand) }

/**
 * Berechnet die Bildschirmposition eines einzelnen Anschlusses.
 */
internal fun Knoten.anschlussReferenz(
    anschluss: AnschlussDaten,
    zustand: KarteZustand,
): AnschlussReferenz? {
    val richtung = if (anschluss is RichtungsAnschlussDaten) anschluss.richtung else null
//    val richtung = anschluss.richtungOderNull ?: return null
    val anschluesseAnKante = daten.anschlüsse.entries
        .filter { (daten, _) -> daten.kante == anschluss.kante }
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })

    val indexAnKante = anschluesseAnKante
        .indexOfFirst { (daten, _) -> daten.id == anschluss.id }
        .coerceAtLeast(0)

    val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
    val anteil = (indexAnKante + 1f) / (anzahlAnKante + 1f)

    val kartePos = Offset(
        x = when (anschluss.kante) {
            AnschlussKante.Links -> daten.position.x
            AnschlussKante.Rechts -> daten.position.x + daten.dimension.width
            AnschlussKante.Oben,
            AnschlussKante.Unten -> daten.position.x + daten.dimension.width * anteil
        },
        y = when (anschluss.kante) {
            AnschlussKante.Links,
            AnschlussKante.Rechts -> daten.position.y + daten.dimension.height * anteil
            AnschlussKante.Oben -> daten.position.y
            AnschlussKante.Unten -> daten.position.y + daten.dimension.height
        },
    ) as KartenPosition

    return AnschlussReferenz(
        knotenId = daten.id,
        anschlussId = anschluss.id,
        richtung = richtung,
        kante = anschluss.kante,
        position = kartePos.zuBild(zustand.ansicht),
    )
}

/**
 * Sucht den naechsten Anschluss zu einer Bildschirmposition.
 */
internal fun BildschirmPosition.naechsterAnschluss(
    anschluesse: List<AnschlussReferenz>,
    maxAbstand: Float,
): AnschlussReferenz? =
    anschluesse
        .map { it to hypot((x - it.position.x).toDouble(), (y - it.position.y).toDouble()) }
        .filter { it.second <= maxAbstand }
        .minByOrNull { it.second }
        ?.first

internal fun KartenTreffer.zuAuswahl(): AuswahlDaten = when (this) {
    KartenTreffer.Hintergrund -> AuswahlDaten()
    is KartenTreffer.Knoten -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Anschluss -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Verbindung -> AuswahlDaten(verbindungIds = setOf(verbindungId))
}
