package de.TeutonStudio.KnotenKartenVerwalter.daten

import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

const val KNOTEN_FORM_EIGENSCHAFT = "atlas.darstellungsform"
enum class KnotenDarstellungsForm(val schlüssel: String, val titel: String) {
    Rechteck("rechteck", "Rechteck"), Adapter("adapter", "Kapsel"), Ellipse("ellipse", "Ellipse"),
    Dreieck("dreieck", "Dreieck"), Raute("raute", "Raute"),
}
val KnotenDaten.darstellungsForm: KnotenDarstellungsForm
    get() = KnotenDarstellungsForm.entries.firstOrNull { it.schlüssel == eigenschaften.text(KNOTEN_FORM_EIGENSCHAFT, "") }
        ?: KnotenDarstellungsForm.Rechteck

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
    val h = größe.höhe
    if (darstellungsForm !in setOf(KnotenDarstellungsForm.Rechteck, KnotenDarstellungsForm.Adapter)) {
        val punkt = when (anschluss.kante) {
            AnschlussKante.Links -> GraphPunkt(0f, h * anteil)
            AnschlussKante.Rechts -> GraphPunkt(w, h * anteil)
            AnschlussKante.Oben -> GraphPunkt(w * anteil, 0f)
            AnschlussKante.Unten -> GraphPunkt(w * anteil, h)
        }
        // Schnitt eines Strahls vom Körperzentrum zur zugeordneten Rechteckkante.
        // Darstellung und magnetische Ziele verwenden denselben Konturschnitt.
        val dx = punkt.x / w.coerceAtLeast(1f) - .5f
        val dy = punkt.y / h.coerceAtLeast(1f) - .5f
        val faktor = when (darstellungsForm) {
            KnotenDarstellungsForm.Ellipse -> .5f / sqrt(dx * dx + dy * dy)
            KnotenDarstellungsForm.Raute -> .5f / (kotlin.math.abs(dx) + kotlin.math.abs(dy))
            KnotenDarstellungsForm.Dreieck -> minOf(
                if (dy > 0f) .5f / dy else Float.POSITIVE_INFINITY,
                if (2f * dx - dy > 0f) .5f / (2f * dx - dy) else Float.POSITIVE_INFINITY,
                if (-2f * dx - dy > 0f) .5f / (-2f * dx - dy) else Float.POSITIVE_INFINITY,
            )
            else -> 1f
        }
        return GraphPunkt((.5f + dx * faktor) * w, (.5f + dy * faktor) * h)
    }
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
    if (darstellungsForm !in setOf(KnotenDarstellungsForm.Rechteck, KnotenDarstellungsForm.Adapter)) {
        val x = (punkt.x + puffer) / (w + 2f * puffer).coerceAtLeast(1f)
        val y = (punkt.y + puffer) / (h + 2f * puffer).coerceAtLeast(1f)
        return when (darstellungsForm) {
            KnotenDarstellungsForm.Ellipse -> (x - .5f) * (x - .5f) + (y - .5f) * (y - .5f) <= .25001f
            KnotenDarstellungsForm.Raute -> kotlin.math.abs(x - .5f) + kotlin.math.abs(y - .5f) <= .50001f
            KnotenDarstellungsForm.Dreieck -> kotlin.math.abs(x - .5f) <= y / 2f + .00001f
            else -> true
        }
    }
    if (darstellungsForm != KnotenDarstellungsForm.Adapter) return true
    val schnitt = kapselEinzug(punkt.y.coerceIn(0f, h), h, adapterEckenRadius(w, h))
    return punkt.x >= schnitt - puffer && punkt.x <= w - schnitt + puffer
}
