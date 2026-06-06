package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KoordinatenUmrechnung
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

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
        val richtung: AnschlussRichtung?,
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
    val position: Offset,
    val zahlenTyp: ZahlenTyp? = null,
)

/**
 * Interner, aufgeloester Graph einer Karte.
 *
 * Diese Klasse sammelt die Logik, die vorher in Karte.kt verteilt war:
 * - Knoten nach ID
 * - Anschlussreferenzen
 * - Verbindungspunkte
 * - Hit-Testing
 */
internal class KartenGraph(
    val knoten: List<Knoten>,
    val verbindungen: List<VerbindungDaten>,
    val zustand: KarteZustand,
) {
    val knotenNachId: Map<String, Knoten> = knoten.associateBy { it.daten.id }

    val anschlussReferenzen: List<AnschlussReferenz> =
        knoten.flatMap { it.anschlussReferenzen(zustand) }

    fun anschlussReferenz(knotenId: String, anschlussId: String): AnschlussReferenz? =
        anschlussReferenzen.firstOrNull {
            it.knotenId == knotenId && it.anschlussId == anschlussId
        }

    fun startOffset(verbindung: VerbindungDaten): Offset? =
        anschlussReferenzen.firstOrNull {
            it.knotenId == verbindung.quellKnotenId &&
                    it.anschlussId == verbindung.quellAnschlussId &&
                    it.richtung == AnschlussRichtung.Ausgang
        }?.position

    fun endeOffset(verbindung: VerbindungDaten): Offset? =
        anschlussReferenzen.firstOrNull {
            it.knotenId == verbindung.zielKnotenId &&
                    it.anschlussId == verbindung.zielAnschlussId &&
                    it.richtung == AnschlussRichtung.Eingang
        }?.position

    fun naechsterAnschluss(
        position: Offset,
        maxAbstand: Float,
        ausgenommen: AnschlussReferenz? = null,
    ): AnschlussReferenz? =
        position.naechsterAnschluss(
            anschluesse = anschlussReferenzen.filterNot {
                ausgenommen != null &&
                        it.knotenId == ausgenommen.knotenId &&
                        it.anschlussId == ausgenommen.anschlussId
            },
            maxAbstand = maxAbstand,
        )

    fun treffer(position: Offset): KartenTreffer {
        naechsterAnschluss(position, maxAbstand = 16f)?.let {
            return KartenTreffer.Anschluss(
                knotenId = it.knotenId,
                anschlussId = it.anschlussId,
                richtung = it.richtung,
            )
        }

        knoten.firstOrNull { position.enthaeltBildschirmPunkt(it.daten, zustand) }?.let {
            return KartenTreffer.Knoten(it.daten.id)
        }

        verbindungen.firstOrNull { verbindung ->
            val start = startOffset(verbindung)
            val ende = endeOffset(verbindung)
            start != null && ende != null && position.abstandZuBezier(start, ende) <= 8f
        }?.let {
            return KartenTreffer.Verbindung(it.id)
        }

        return KartenTreffer.Hintergrund
    }
}

/**
 * Richtung aus AnschlussDaten bestimmen.
 *
 * Falls ein spaeterer Anschluss keine Richtung besitzt, wird er fuer
 * Verbindungen ignoriert. Ein Anschluss ohne Richtung ist visuell moeglich,
 * aber nicht verbindbar. Ja, sogar Anschluesse brauchen Papierkram.
 */
/*internal val AnschlussDaten.richtungOderNull: AnschlussRichtung?
    get() = (this as? RichtungsAnschlussDaten)?.richtung

internal fun KnotenAnschlüsse.filterRichtung(richtung: AnschlussRichtung): KnotenAschlüsse =
    filter { (anschluss, _) -> anschluss.richtungOderNull == richtung }*/

/**
 * Loest alle gerichteten Anschluesse eines Knotens in Bildschirmpositionen auf.
 */
internal fun Knoten.anschlussReferenzen(zustand: KarteZustand): List<AnschlussReferenz> =
    erhalteAnschlüsse()
        .entries
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })
        .mapNotNull { (anschluss, _) -> anschlussReferenz(anschluss, zustand) }

/**
 * Berechnet die Bildschirmposition eines einzelnen Anschlusses.
 */
internal fun Knoten.anschlussReferenz(
    anschluss: AnschlussDaten,
    zustand: KarteZustand,
): AnschlussReferenz {
    val richtung = if (anschluss is RichtungsAnschlussDaten) anschluss.richtung else null
    val anschluesseAnKante = erhalteAnschlüsse()
        .entries
        .filter { (daten, _) -> daten.kante == anschluss.kante }
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })

    val indexAnKante = anschluesseAnKante
        .indexOfFirst { (daten, _) -> daten.id == anschluss.id }
        .coerceAtLeast(0)

    val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
    val anteil = (indexAnKante + 1f) / (anzahlAnKante + 1f)

    val weltPosition = Offset(
        x = when (anschluss.kante) {
            AnschlussKante.Links -> daten.position.x
            AnschlussKante.Rechts -> daten.position.x + daten.fläche.x
            AnschlussKante.Oben,
            AnschlussKante.Unten -> daten.position.x + daten.fläche.x * anteil
        },
        y = when (anschluss.kante) {
            AnschlussKante.Links,
            AnschlussKante.Rechts -> daten.position.y + daten.fläche.y * anteil
            AnschlussKante.Oben -> daten.position.y
            AnschlussKante.Unten -> daten.position.y + daten.fläche.y
        },
    )

    return AnschlussReferenz(
        knotenId = daten.id,
        anschlussId = anschluss.id,
        richtung = richtung,
        kante = anschluss.kante,
        position = weltPosition.zuBildschirmOffset(zustand),
        zahlenTyp = null,
    )
}

/**
 * Sucht den naechsten Anschluss zu einer Bildschirmposition.
 */
internal fun Offset.naechsterAnschluss(
    anschluesse: List<AnschlussReferenz>,
    maxAbstand: Float,
): AnschlussReferenz? =
    anschluesse
        .map { it to hypot(x - it.position.x, y - it.position.y) }
        .filter { it.second <= maxAbstand }
        .minByOrNull { it.second }
        ?.first

internal fun KartenTreffer.zuAuswahl(): AuswahlDaten = when (this) {
    KartenTreffer.Hintergrund -> AuswahlDaten()
    is KartenTreffer.Knoten -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Anschluss -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Verbindung -> AuswahlDaten(verbindungIds = setOf(verbindungId))
}

/**
 * Zoomt um die Mitte des sichtbaren Kartencontainers.
 */
internal fun KarteZustand.zoomUm(faktor: Float, fläche: IntSize): KarteZustand {
    val mittelpunkt = Offset(fläche.width / 2f, fläche.height / 2f)
    return transformiereUm(mittelpunkt, Offset.Zero, faktor)
}

/**
 * Transformiert den Viewport um einen Bildschirm-Mittelpunkt.
 */
internal fun KarteZustand.transformiereUm(
    zentrum: Offset,
    pan: Offset,
    zoomÄnderung: Float,
): KarteZustand {
    val alterZoom = zoomSicher()
    val neuerZoom = (alterZoom * zoomÄnderung).coerceIn(0.25f, 3f)
    val weltZentrum = (zentrum - verschiebung) / alterZoom
    return KarteZustand(
        this,
        zoom = neuerZoom,
        verschiebung = zentrum - weltZentrum * neuerZoom + pan,
    )
}

/**
 * Berechnet einen Viewport, der alle Knoten sichtbar in den Container einpasst.
 */
internal fun KarteDaten.zoomAufInhalt(
    fläche: IntSize,
    aktuellerZustand: KarteZustand,
): KarteZustand {
    val grenzen = knoten.grenzen() ?: return aktuellerZustand
    if (fläche.width <= 0 || fläche.height <= 0) return aktuellerZustand

    val padding = 48f
    val breite = (grenzen.rechts - grenzen.links).coerceAtLeast(1f)
    val hoehe = (grenzen.unten - grenzen.oben).coerceAtLeast(1f)
    val neuerZoom = minOf(
        (fläche.width - padding * 2f) / breite,
        (fläche.height - padding * 2f) / hoehe,
    ).coerceIn(0.25f, 3f)

    val verschiebung = Offset(
        x = (fläche.width - breite * neuerZoom) / 2f - grenzen.links * neuerZoom,
        y = (fläche.height - hoehe * neuerZoom) / 2f - grenzen.oben * neuerZoom,
    )

    return KarteZustand(aktuellerZustand, zoom = neuerZoom, verschiebung = verschiebung)
}

/**
 * Rechteckige Begrenzung in Weltkoordinaten.
 */
internal data class KartenGrenzen(
    val links: Float,
    val oben: Float,
    val rechts: Float,
    val unten: Float,
)

/**
 * Berechnet die Gesamtgrenzen einer Knotenliste inklusive optionalem Padding.
 */
internal fun List<KnotenDaten>.grenzen(padding: Float = 0f): KartenGrenzen? {
    if (isEmpty()) return null

    val grenzen = fold<KnotenDaten, KartenGrenzen?>(null) { aktuelleGrenzen, knoten ->
        val links = knoten.position.x
        val oben = knoten.position.y
        val rechts = links + knoten.fläche.x
        val unten = oben + knoten.fläche.y

        if (aktuelleGrenzen == null) {
            KartenGrenzen(links, oben, rechts, unten)
        } else {
            KartenGrenzen(
                links = minOf(aktuelleGrenzen.links, links),
                oben = minOf(aktuelleGrenzen.oben, oben),
                rechts = maxOf(aktuelleGrenzen.rechts, rechts),
                unten = maxOf(aktuelleGrenzen.unten, unten),
            )
        }
    } ?: return null

    return KartenGrenzen(
        links = grenzen.links - padding,
        oben = grenzen.oben - padding,
        rechts = grenzen.rechts + padding,
        unten = grenzen.unten + padding,
    )
}

/**
 * Rechnet eine Bildschirmposition in Weltkoordinaten um.
 */
internal fun Offset.zuWeltPosition(zustand: KarteZustand): Offset =
    KoordinatenUmrechnung.bildschirmZuWelt(this, zustand)

/**
 * Rechnet eine Weltposition in Bildschirmkoordinaten um.
 */
internal fun Offset.zuBildschirmOffset(zustand: KarteZustand): Offset =
    KoordinatenUmrechnung.weltZuBildschirm(this, zustand)

/**
 * Rechnet eine Weltposition in eine ganzzahlige Bildschirmposition fuer Modifier.offset um.
 */
internal fun Offset.zuBildschirmIntOffset(zustand: KarteZustand): IntOffset {
    val offset = zuBildschirmOffset(zustand)
    return IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
}

/**
 * Liefert einen robusten Zoomfaktor.
 */
internal fun KarteZustand.zoomSicher(): Float =
    zoom.takeIf { it > 0f } ?: 1f

internal operator fun Offset.div(wert: Float): Offset =
    Offset(x / wert, y / wert)

internal operator fun Offset.times(wert: Float): Offset =
    Offset(x * wert, y * wert)

/**
 * Bildschirmpunkt innerhalb des sichtbaren Knotenrechtecks?
 */
private fun Offset.enthaeltBildschirmPunkt(
    knoten: KnotenDaten,
    zustand: KarteZustand,
): Boolean {
    val linksOben = knoten.position.zuBildschirmOffset(zustand)
    val zoom = zustand.zoomSicher()
    return x in linksOben.x..(linksOben.x + knoten.fläche.x * zoom) &&
            y in linksOben.y..(linksOben.y + knoten.fläche.y * zoom)
}

/**
 * Approximiert den Abstand zu einer Bezier-Verbindung durch kurze Liniensegmente.
 */
private fun Offset.abstandZuBezier(start: Offset, ende: Offset): Float {
    val kontrollAbstand = maxOf(48f, abs(ende.x - start.x) / 2f)
    val p1 = Offset(start.x + kontrollAbstand, start.y)
    val p2 = Offset(ende.x - kontrollAbstand, ende.y)

    var besterAbstand = Float.MAX_VALUE
    var vorher = start

    for (schritt in 1..24) {
        val t = schritt / 24f
        val punkt = kubisch(start, p1, p2, ende, t)
        besterAbstand = minOf(besterAbstand, abstandZuSegment(vorher, punkt))
        vorher = punkt
    }

    return besterAbstand
}

private fun kubisch(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float,
): Offset {
    val u = 1f - t
    return p0 * u.pow(3) +
            p1 * (3f * u.pow(2) * t) +
            p2 * (3f * u * t.pow(2)) +
            p3 * t.pow(3)
}

private fun Offset.abstandZuSegment(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    if (dx == 0f && dy == 0f) return hypot(x - a.x, y - a.y)

    val t = (((x - a.x) * dx + (y - a.y) * dy) / (dx * dx + dy * dy))
        .coerceIn(0f, 1f)
    val projektion = Offset(a.x + t * dx, a.y + t * dy)

    return hypot(x - projektion.x, y - projektion.y)
}