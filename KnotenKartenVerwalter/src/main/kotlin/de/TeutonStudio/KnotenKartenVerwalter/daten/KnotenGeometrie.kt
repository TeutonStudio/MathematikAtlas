package de.TeutonStudio.KnotenKartenVerwalter.daten

import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

const val KNOTEN_FORM_EIGENSCHAFT = "atlas.darstellungsform"
enum class KnotenDarstellungsForm { Rechteck, Adapter }
val KnotenDaten.darstellungsForm: KnotenDarstellungsForm
    get() = if (eigenschaften.text(KNOTEN_FORM_EIGENSCHAFT, "") == "adapter") KnotenDarstellungsForm.Adapter else KnotenDarstellungsForm.Rechteck

fun adapterEckenRadius(breite: Float, höhe: Float): Float = min(breite, höhe).coerceAtLeast(0f) / 2f

private fun kapselEinzug(y: Float, höhe: Float, radius: Float): Float {
    val abstand = when {
        y < radius -> radius - y
        y > höhe - radius -> y - (höhe - radius)
        else -> 0f
    }
    return radius - sqrt((radius * radius - abstand * abstand).coerceAtLeast(0f))
}

/** Gemeinsame Geometrie für sichtbare Griffe, Verbindungen und magnetische Treffer. */
fun KnotenDaten.anschlussPositionRelativ(anschluss: AnschlussDaten): GraphPunkt {
    val aufKante = anschlüsse.filter { it.kante == anschluss.kante }.sortedBy { it.reihenfolge }
    val anteil = (aufKante.indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0) + 1f) / (aufKante.size + 1f)
    val w = größe.breite
    val h = if (darstellungsForm == KnotenDarstellungsForm.Adapter) (größe.höhe - darstellungsFußzeilenHöhe).coerceAtLeast(0f) else größe.höhe
    val radius = if (darstellungsForm == KnotenDarstellungsForm.Adapter) adapterEckenRadius(w, h) else 0f
    // Beide Viertelkreise und gegebenenfalls die gerade Seitenstrecke teilen dieselbe Bogenlänge.
    val bogen = PI.toFloat() * radius / 2f
    val gerade = (h - 2 * radius).coerceAtLeast(0f)
    val strecke = (2 * bogen + gerade) * anteil
    val y = when {
        anteil == .5f -> h / 2f
        radius == 0f -> h * anteil
        strecke < bogen -> radius - radius * cos(strecke / radius)
        strecke <= bogen + gerade -> radius + strecke - bogen
        else -> h - radius + radius * sin((strecke - bogen - gerade) / radius)
    }
    val einzug = kapselEinzug(y, h, radius)
    return when (anschluss.kante) {
        AnschlussKante.Links -> GraphPunkt(einzug, y)
        AnschlussKante.Rechts -> GraphPunkt(w - einzug, y)
        AnschlussKante.Oben -> GraphPunkt(radius + (w - 2 * radius) * anteil, 0f)
        AnschlussKante.Unten -> GraphPunkt(radius + (w - 2 * radius) * anteil, h)
    }
}

fun KnotenDaten.enthältRelativenPunkt(punkt: GraphPunkt, puffer: Float = 0f): Boolean {
    val w = größe.breite
    val h = größe.höhe
    if (punkt.y !in -puffer..(h + puffer) || punkt.x !in -puffer..(w + puffer)) return false
    if (darstellungsForm != KnotenDarstellungsForm.Adapter) return true
    val körper = (h - darstellungsFußzeilenHöhe).coerceAtLeast(0f)
    if (punkt.y > körper) return true
    val schnitt = kapselEinzug(punkt.y.coerceIn(0f, körper), körper, adapterEckenRadius(w, körper))
    return punkt.x >= schnitt - puffer && punkt.x <= w - schnitt + puffer
}
