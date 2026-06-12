package com.TeutonStudio.KnotenKartenVerwalter

import android.graphics.RectF
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.istVertikal
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import kotlin.math.abs
import kotlin.math.max

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap() = this.toMap().toMutableMap()
public fun <T> Pair<T,T>.enthält(value: T): Boolean = first == value || second == value
public fun <K,V> Pair<K,V>.wechsle(): Pair<V,K> = Pair(second,first)
public fun <T> Pair<T,T>.toSet(): Set<T> = setOf(first,second)

operator fun Offset.times(other: Offset): Float = x * other.x + y * other.y

public fun printLogCat(vararg arg: Any?) = arg.forEach { println(it) }


// Geometrie

/**
 * Position in der Karte
 */
typealias KartenPosition = Offset
/**
 * Position im Knoten
 */
typealias KnotenPosition = Offset
/**
 * Position auf dem Bildschirm
 */
typealias BildschirmPosition = IntOffset
/**
 * Dimensionen eines Rechteck
 */
typealias Rechteck = Rect

operator fun Rechteck.times(other: Float): Offset = diagonale() * other
public fun Rechteck.diagonale(): Offset = Offset(width,height)

public fun Rechteck(breite: Float, tiefe: Float, position: KartenPosition): Rechteck= Rect(position,position+ Offset(breite,tiefe))


public fun BildschirmPosition.abstandZuVerbindung(verbindung: Verbindung): Float {
    val p = Offset(x.toFloat(), y.toFloat())

    // Achtung: start/ende sind bei dir semantisch Bildschirmpositionen,
    // obwohl der Typ State<KartenPosition> heißt.
    val start = verbindung.start.value
    val ende = verbindung.ende.value

    val kontrollAbstand = max(48f, abs(ende.x - start.x) / 2f)
    val c1 = Offset(start.x + kontrollAbstand, start.y)
    val c2 = Offset(ende.x - kontrollAbstand, ende.y)

    var vorher = start
    var kleinsterAbstand = Float.POSITIVE_INFINITY

    for (i in 1..32) {
        val t = i / 32f
        val aktuell = kubisch(start, c1, c2, ende, t)

        kleinsterAbstand = minOf(
            kleinsterAbstand,
            p.abstandZuSegment(vorher, aktuell),
        )

        vorher = aktuell
    }

    return kleinsterAbstand
}

private fun kubisch(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float, ): Offset {
    val u = 1f - t

    return Offset(
        x = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x,
        y = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y,
    )
}

private fun Offset.abstandZuSegment(a: Offset, b: Offset): Float {
    val ab = b - a; val ap = this - a; val abQuadrat = ab.x * ab.x + ab.y * ab.y

    if (abQuadrat <= 0.0001f) { return (this - a).getDistanceSquared() }

    val t = ((ap.x * ab.x + ap.y * ab.y) / abQuadrat).coerceIn(0f, 1f)

    val naechsterPunkt = Offset(x = a.x + ab.x * t, y = a.y + ab.y * t)

    return (this - naechsterPunkt).getDistanceSquared()
}

fun Offset.abstandZuBezier(bezier: List<Offset>, schritte: Int = 32): Float {
    var vorher = bezier[0]
    var kleinsterAbstand = Float.POSITIVE_INFINITY

    for (i in 1..schritte) {
        val t = i / schritte.toFloat()
        val aktuell = kubisch(bezier[0],bezier[1],bezier[2],bezier[3],t)

        kleinsterAbstand = minOf(kleinsterAbstand,abstandZuSegment(vorher, aktuell))
        vorher = aktuell
    }

    return kleinsterAbstand
}

/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun BildschirmPosition.zuIntOffset(zustand: KarteZustand): IntOffset = this + (zustand.pos * zustand.zoom).round()

/**
 * Kurzform für eine reine Verschiebung ohne expliziten Zoom.
 */
/*public fun BildschirmPosition.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
)*/

// Anschluss

/**
 * Seite eines Anschlusses am Knoten.
 *
 * Eingänge liegen links am Knotenrahmen, Ausgänge rechts. Diese Richtung wird
 * sowohl für das Rendering als auch für die Validierung neuer Verbindungen
 * verwendet.
 */


/**
 * Anschlüsse eines Knotens und ihre Sortierung
 */
typealias KnotenAnschlüsse = MutableMap<out AnschlussDaten,Int>
/**
 * Der Modifier für einen Anschluss, abhängig von Anschluss und index
 */
typealias AnschlussModifier = (AnschlussDaten, Int) -> Modifier

// public fun KnotenAnschlüsse.filterKante(kante: AnschlussKante): KnotenAnschlüsse = this.filter { (daten,idx) -> daten.kante == kante }.toMutableMap()
public fun Map<Anschluss<out AnschlussDaten>,Int>.filterKante(kante: AnschlussKante): Map<Anschluss<out AnschlussDaten>,Int> = this.filter { (a,idx) -> a.daten.kante == kante }.toMutableMap()
public fun KnotenAnschlüsse.filterRichtung(richtung: AnschlussRichtung): KnotenAnschlüsse = this.filter { (daten,idx) -> when (richtung) {
    AnschlussRichtung.Eingang -> daten is EingangDaten
    AnschlussRichtung.Ausgang -> daten is AusgangDaten
} }.toMutableMap()


public fun RectF.overlaps(other: RectF) = left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top


/*public fun Pair<KnotenDaten,AnschlussDaten>.pos(): KartenPosition {
    val anteil = relAnteilKante(first.anschlüsse,second.id,second.kante)
    return Offset(
        x = second.kante.wertFür(
            first.position.x,
            first.position.x + first.dimension.width,
            0f,
            first.position.x + first.dimension.width * anteil
        ),
        y = second.kante.wertFür(
            0f,
            first.position.y + first.dimension.height * anteil,
            first.position.y,
            first.position.y + first.dimension.height
        ),
    )
}*/

/*private fun relAnteilKante(anschlüsse: KnotenAnschlüsse, aId: String, kante: AnschlussKante): Float {
    val sorter = compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id }
    val anschluesseAnKante = anschlüsse.entries.filter { (daten, _) -> daten.kante == kante }.sortedWith(sorter)
    val indexAnKante = anschluesseAnKante.indexOfFirst { (daten, _) -> daten.id == aId }.coerceAtLeast(0)
    val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
    return (indexAnKante + 1f) / (anzahlAnKante + 1f)
}*/

// Verbindung

public fun Iterable<Verbindung>.plusVlt(arg: Verbindung?): Iterable<Verbindung> = if (arg != null) plus(arg) else this
//public fun Iterable<Verbindung>.abwählen() = forEach { it.daten.ausgewaehlt = false }

typealias idReferenz = Pair<Pair<String,String>,Pair<String,String>>

public fun idReferenz(von: Pair<KnotenDaten, AnschlussDaten>, zu: Pair<KnotenDaten, AnschlussDaten>): idReferenz = (von.first.id to zu.first.id) to (von.second.id to zu.second.id)

public fun idReferenz.erhalteKnotenIds(): Pair<String,String> = this.first
public fun idReferenz.erhalteAnschlussIds(): Pair<String,String> = this.second

public fun idReferenz.erhalteErtes(): Pair<String,String> = this.first.first to this.second.first
public fun idReferenz.erhalteZweites(): Pair<String,String> = this.first.second to this.second.second

//public fun idReferenz.istVerbunden(ref: AnschlussReferenz): Boolean = this.hatKnotenId(ref.knotenId) && this.hatAnschlussId(ref.anschlussId)

public fun idReferenz.hatKnotenId(id: String): Boolean = this.erhalteKnotenIds().enthält(id)
public fun idReferenz.istVerbunden(daten: KnotenDaten): Boolean = this.hatKnotenId(daten.id)

public fun idReferenz.hatAnschlussId(id: String): Boolean = this.erhalteAnschlussIds().enthält(id)
public fun idReferenz.istVerbunden(daten: AnschlussDaten): Boolean = this.hatAnschlussId(daten.id)

public fun idReferenz.hatGleichenKnoten(other: idReferenz): Boolean {
    val knotenIds = other.erhalteKnotenIds()
    return this.hatKnotenId(knotenIds.first) || this.hatKnotenId(knotenIds.second)
}

public fun idReferenz.hatGleichenAnschluss(other: idReferenz): Boolean {
    val anschlussIds1 = this.erhalteAnschlussIds()
    val anschlussIds2 = other.erhalteAnschlussIds()
    val ersterAnschluss = anschlussIds1.first == anschlussIds2.first || anschlussIds1.first == anschlussIds2.second
    val zweiterAnschluss = anschlussIds1.second == anschlussIds2.first || anschlussIds1.second == anschlussIds2.second
    return  (ersterAnschluss || zweiterAnschluss) && hatGleichenKnoten(other)
}

public fun AnschlussKante.tangente(): Offset = when (this) {
    AnschlussKante.Links -> Offset(-1f, 0f)
    AnschlussKante.Rechts -> Offset(1f, 0f)
    AnschlussKante.Oben -> Offset(0f, -1f)
    AnschlussKante.Unten -> Offset(0f, 1f)
}

/*@Composable
public fun Iterable<Verbindung>.zuComposable(modifier: Modifier = Modifier) {
    if (this.count() == 0) return
    Canvas(modifier = modifier) { forEach { verbindung -> verbindung.zeichnung(this) } }
}*/

// Knoten

public fun KnotenDaten.erhalteSize(): Size = Size(breite,tiefe)

/**
 * Callback fuer eine geaenderte Knotenposition in Weltkoordinaten.
 */
typealias KartenAktualisierung = (knotenId: String, position: KartenPosition) -> Unit

/**
 * Callback, wenn durch Anschluss-Drag eine neue Verbindung entstanden ist.
 */
typealias VerbindungErstellen = (verbindung: VerbindungDaten) -> Unit

/**
 * Callback fuer Aktionen aus dem Kontextmenue der Karte.
 */
typealias KontextAktionAusführen = (aktion: KartenKontextAktion) -> Unit

/**
 * Callback fuer kontrollierte Auswahl von Knoten und Verbindungen.
 */
typealias AuswahlÄndern = (auswahl: AuswahlDaten) -> Unit

// Modifier

public fun radius(kante: AnschlussKante, radius: Dp = (2.5f).dp): Dp = if (kante == AnschlussKante.Rechts || kante == AnschlussKante.Unten) radius else -radius

public fun Modifier.fillMaxKante(kante: AnschlussKante,@FloatRange fraction: Float = 1f): Modifier = if (kante.istVertikal()) fillMaxHeight(fraction) else fillMaxWidth(fraction)

public fun Modifier.offsetKante(kante: AnschlussKante, offset: Dp = 0.dp) = if(kante.istVertikal()) offset(x=offset) else offset(y=offset)
