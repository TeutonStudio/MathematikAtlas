package com.TeutonStudio.KnotenKartenVerwalter

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.AnschlussSpalte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.AnschlussZeile
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.VerbindungUmgebung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AnschlussModifierStandard
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AnschlussReferenz
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisAusgang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Karte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap() = this.toMap().toMutableMap()
public fun <T> Pair<T,T>.enthält(value: T): Boolean = first == value || second == value
public fun <T> Pair<T,T>.toSet(): Set<T> = setOf(first,second)

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

public fun Rechteck(breite: Float, tiefe: Float, position: KartenPosition): Rechteck= Rect(position,position+ Offset(breite,tiefe))

/**
 *
 */
public fun KartenPosition.zuBild(ansicht: AnsichtsfensterDaten): BildschirmPosition {
    val zoom = ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)
    val verschiebung = ansicht.erhalteVerschiebung()

    return IntOffset(
        x = (x * zoom + verschiebung.x).roundToInt(),
        y = (y * zoom + verschiebung.y).roundToInt(),
    )
}


/**
 *
 */
public fun BildschirmPosition.zuKarte(ansicht: AnsichtsfensterDaten): KartenPosition {
    val zoom = ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)
    val verschiebung = ansicht.erhalteVerschiebung()

    return Offset(
        x = (x - verschiebung.x) / zoom,
        y = (y - verschiebung.y) / zoom,
    )
}


/**
 *
 */
public fun KartenPosition.aufKnoten(daten: KnotenDaten): Boolean = daten.dimension.contains(this)

/**
 *
 */
public fun KartenPosition.aufVerbindung(daten: VerbindungDaten,knoten: Pair<Knoten,Knoten>): Boolean {
    return TODO("formel zu ermittelung der entfernung zur Verbindung zur pos definieren")
}

private fun KnotenPosition.zuKarte(daten: KnotenDaten): KartenPosition {
    return this.zuKarte(daten.position)
}

private fun KnotenPosition.zuKarte(zentrum: KartenPosition): KartenPosition {
    return TODO("definieren, ob der lokale Knotenraum andere skalierung hat als der der Karte.")
}


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

    if (abQuadrat <= 0.0001f) {
        return hypot(
            (x - a.x).toDouble(),
            (y - a.y).toDouble(),
        ).toFloat()
    }

    val t = ((ap.x * ab.x + ap.y * ab.y) / abQuadrat).coerceIn(0f, 1f)

    val naechsterPunkt = Offset(x = a.x + ab.x * t, y = a.y + ab.y * t)

    return hypot(
        (x - naechsterPunkt.x).toDouble(),
        (y - naechsterPunkt.y).toDouble(),
    ).toFloat()
}

/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun BildschirmPosition.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.x * zustand.ansicht.erhalteZoomfaktor() + zustand.ansicht.erhalteVerschiebung().x).roundToInt(),
    y = (this.y * zustand.ansicht.erhalteZoomfaktor() + zustand.ansicht.erhalteVerschiebung().y).roundToInt(),
)

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
enum class AnschlussRichtung {
    Eingang,
    Ausgang,
}

public fun AnschlussRichtung?.istEingang(): Boolean = this == AnschlussRichtung.Eingang
public fun AnschlussRichtung?.istAusgang(): Boolean = this == AnschlussRichtung.Ausgang

public fun Anschluss.istEingang(): Boolean = this is BasisEingang
public fun Anschluss.istAusgang(): Boolean = this is BasisAusgang

/**
 * Kante eines Knotens, an der ein Anschluss liegt.
 */
enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten,
}

public fun AnschlussKante.istVertikal(): Boolean = this == AnschlussKante.Links || this == AnschlussKante.Rechts
public fun AnschlussKante.istHorizontal(): Boolean = this == AnschlussKante.Oben || this == AnschlussKante.Unten


/**
 * Positioniert Anschlüsse gleichmäßig an einer Knotenkante.
 */
@Composable
public fun Map<Anschluss,Int>.zuLeiste(kante: AnschlussKante, leisteModifier: Modifier = Modifier, modifier: AnschlussModifier = { daten,idx -> AnschlussModifierStandard }) {
    val listeComposable = this.filterKante(kante).map { (anschluss,idx) -> @Composable { anschluss.zuComposable(/*modifier(anschluss.daten,idx)*/) } }
    if (kante.istVertikal()) AnschlussSpalte(leisteModifier,listeComposable)
    else if (kante.istHorizontal()) AnschlussZeile(leisteModifier,listeComposable)
    else TODO()
}


/**
 * Anschlüsse eines Knotens und ihre Sortierung
 */
typealias KnotenAnschlüsse = MutableMap<AnschlussDaten,Int>
/**
 * Der Modifier für einen Anschluss, abhängig von Anschluss und index
 */
typealias AnschlussModifier = (AnschlussDaten, Int) -> Modifier

// public fun KnotenAnschlüsse.filterKante(kante: AnschlussKante): KnotenAnschlüsse = this.filter { (daten,idx) -> daten.kante == kante }.toMutableMap()
public fun Map<Anschluss,Int>.filterKante(kante: AnschlussKante): Map<Anschluss,Int> = this.filter { (a,idx) -> a.daten.kante == kante }.toMutableMap()
public fun KnotenAnschlüsse.filterRichtung(richtung: AnschlussRichtung): KnotenAnschlüsse = this.filter { (daten,idx) -> when (richtung) {
    AnschlussRichtung.Eingang -> daten is EingangDaten
    AnschlussRichtung.Ausgang -> daten is AusgangDaten
} }.toMutableMap()



public fun Pair<KnotenDaten,AnschlussDaten>.pos(): KartenPosition {
    val anteil = relAnteilKante(first.anschlüsse,second.id,second.kante)
    return Offset(
        x = when (second.kante) {
            AnschlussKante.Links -> first.position.x
            AnschlussKante.Rechts -> first.position.x + first.dimension.width
            AnschlussKante.Oben,
            AnschlussKante.Unten -> first.position.x + first.dimension.width * anteil
        },
        y = when (second.kante) {
            AnschlussKante.Links,
            AnschlussKante.Rechts -> first.position.y + first.dimension.height * anteil
            AnschlussKante.Oben -> first.position.y
            AnschlussKante.Unten -> first.position.y + first.dimension.height
        },
    )
}

private fun relAnteilKante(anschlüsse: KnotenAnschlüsse, aId: String, kante: AnschlussKante): Float {
    val sorter = compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id }
    val anschluesseAnKante = anschlüsse.entries.filter { (daten, _) -> daten.kante == kante }.sortedWith(sorter)
    val indexAnKante = anschluesseAnKante.indexOfFirst { (daten, _) -> daten.id == aId }.coerceAtLeast(0)
    val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
    return (indexAnKante + 1f) / (anzahlAnKante + 1f)
}

// Verbindung

public fun Iterable<Verbindung>.plusVlt(arg: Verbindung?): Iterable<Verbindung> = if (arg != null) plus(arg) else this

typealias idReferenz = Pair<Pair<String,String>,Pair<String,String>>

public fun idReferenz(von: Pair<KnotenDaten, AnschlussDaten>,zu: Pair<KnotenDaten, AnschlussDaten>): idReferenz = (von.first.id to zu.first.id) to (von.second.id to zu.second.id)

public fun idReferenz.erhalteKnotenIds(): Pair<String,String> = this.first
public fun idReferenz.erhalteAnschlussIds(): Pair<String,String> = this.second

public fun idReferenz.erhalteErtes(): Pair<String,String> = this.first.first to this.second.first
public fun idReferenz.erhalteZweites(): Pair<String,String> = this.first.second to this.second.second

public fun idReferenz.istVerbunden(ref: AnschlussReferenz): Boolean = this.hatKnotenId(ref.knotenId) && this.hatAnschlussId(ref.anschlussId)

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
    AnschlussKante.Links -> Offset(1f, 0f)
    AnschlussKante.Rechts -> Offset(-1f, 0f)
    AnschlussKante.Oben -> Offset(0f, -1f)
    AnschlussKante.Unten -> Offset(0f, 1f)
}

@Composable
public fun Iterable<Verbindung>.zuComposable(
    modifier: Modifier = Modifier,
) {
    if (this.count() == 0) return
    Canvas(modifier = modifier) { forEach { verbindung -> verbindung.zeichnung() } }
}

// Knoten

public fun KnotenDaten.zuAuswahl(): AuswahlDaten = AuswahlDaten(setOf(id))
public fun KnotenDaten.erhalteSize(): Size = Size(breite,tiefe)


@Composable @JvmName("IterKnoten2Composable")
public fun Iterable<Knoten>.zuComposable(
    modifierKnoten: (KnotenDaten) -> Modifier,
    modifierAnschluss: (KnotenDaten) -> AnschlussModifier,
) = this.forEach { it.zuComposable(modifierKnoten(it.daten),modifierAnschluss(it.daten),1f) }

// Karten

/**
 * Persistierter Viewport einer Karte.
 *
 * `x` und `y` beschreiben die Verschiebung des Weltkoordinatensystems in
 * Bildschirmkoordinaten. `zoom` beschreibt den Skalierungsfaktor zwischen Welt-
 * und Bildschirmkoordinaten.
 */
typealias AnsichtsfensterDaten = Triple<Float,Float,Float>

public fun AnsichtsfensterDaten(zoom: Float, verschiebung: Offset): AnsichtsfensterDaten = Triple(zoom,verschiebung.x,verschiebung.y)
public fun StandardAnsicht(): AnsichtsfensterDaten = Triple(1f,0f,0f)

public fun KarteZustand.verschiebe(delta: Offset) { ansicht = Triple(ansicht.first,ansicht.second + delta.x, ansicht.third + delta.y) }
public fun KarteZustand.zoome(delta: Float) { ansicht = Triple(ansicht.first + delta,ansicht.second,ansicht.third) }
public fun KarteZustand.transformiere(verschiebung: Offset,zoom: Float) { ansicht = Triple(ansicht.first + zoom,ansicht.second + verschiebung.x, ansicht.third + verschiebung.y) }

public fun AnsichtsfensterDaten.erhalteVerschoben(von: BildschirmPosition): BildschirmPosition = TODO()
public fun AnsichtsfensterDaten.erhalteVerschiebung(): Offset = Offset(this.second,this.third)
public fun AnsichtsfensterDaten.erhalteZoomfaktor(): Float = first

/**
 *
 */
public fun KarteZustand.erhalteNachBildPos(
    pos: BildschirmPosition,
    knoten: Iterable<Knoten>,
    verbindung: Iterable<Verbindung>,
): GraphObjekt? {
    val kartePos = pos.zuKarte(this.ansicht) // TODO umrechnung der Bildschirm position [pos] abhängig von ansicht zoom und ansicht verschiebung.
    var auswahl: GraphObjekt? = null
    knoten.forEach {
        if (kartePos.aufKnoten(it.daten)) auswahl = it
    }
    verbindung.forEach {
        val k = knoten.filter { k -> it.daten.ids.hatKnotenId(k.daten.id) }
        if (kartePos.aufVerbindung(it.daten,k[0] to k[1])) auswahl = it
    }

    return auswahl
}

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

public fun alignment(kante: AnschlussKante): Alignment = when(kante) {
    AnschlussKante.Links -> Alignment.CenterStart
    AnschlussKante.Rechts -> Alignment.CenterEnd
    AnschlussKante.Oben -> Alignment.TopCenter
    AnschlussKante.Unten -> Alignment.BottomCenter
}

public fun Modifier.fillMaxKante(kante: AnschlussKante,@FloatRange fraction: Float = 1f): Modifier = if (kante.istVertikal()) fillMaxHeight(fraction) else fillMaxWidth(fraction)

public fun Modifier.offsetKante(kante: AnschlussKante, offset: Dp = 0.dp) = if(kante.istVertikal()) offset(x=offset) else offset(y=offset)
