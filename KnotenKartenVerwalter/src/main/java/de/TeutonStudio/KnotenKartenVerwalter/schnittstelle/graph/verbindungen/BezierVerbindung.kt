package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.times
import kotlin.math.hypot
import kotlin.math.max

//typealias Kante = AnschlussGraphDaten.AnschlussKante

open class BezierVerbindung(
    graph: Graph,
    daten: VerbindungDaten,
    override val start: State<KartenPosition>,
    override val ende: State<KartenPosition>,
): Verbindung(graph,daten) {
    override var startKante = Kante.Rechts
    override var endeKante = Kante.Links


    public override fun erhaltePfad(): Path = Path().apply {
        val cubic = { o1: Offset, o2: Offset, o3:  Offset -> cubicTo(o1.x,o1.y,o2.x,o2.y,o3.x,o3.y) }
        val move = { p: Offset -> moveTo(p.x,p.y) }
        move(start.value); cubic(c1(),c2(),ende.value)
    }

    public override fun abstand(pos: KartenPosition): Offset {
        val p0 = start.value
        val p3 = ende.value

        val p1 = c1(p0, p3)
        val p2 = c2(p0, p3)

        var besterT = 0f
        var besterPunkt = p0
        var kleinsterAbstandSq = Float.POSITIVE_INFINITY

        val schritte = 48
        var vorherT = 0f
        var vorherPunkt = cubic(p0, p1, p2, p3, 0f)

        for (i in 1..schritte) {
            val t = i / schritte.toFloat()
            val punkt = cubic(p0, p1, p2, p3, t)

            val kandidat = nächsterPunktAufSegment(pos, vorherPunkt, punkt)
            val abstandSq = (pos - kandidat).getDistanceSquared()

            if (abstandSq < kleinsterAbstandSq) {
                kleinsterAbstandSq = abstandSq
                besterPunkt = kandidat
                besterT = vorherT + (t - vorherT) * segmentAnteil(vorherPunkt, punkt, kandidat)
            }

            vorherT = t
            vorherPunkt = punkt
        }

        /*
         * Lokale Verfeinerung um den besten gefundenen Bereich.
         */
        val radius = 1f / schritte
        var links = (besterT - radius).coerceIn(0f, 1f)
        var rechts = (besterT + radius).coerceIn(0f, 1f)

        repeat(16) {
            val t1 = links + (rechts - links) / 3f
            val t2 = rechts - (rechts - links) / 3f

            val d1 = (pos - cubic(p0, p1, p2, p3, t1)).getDistanceSquared()
            val d2 = (pos - cubic(p0, p1, p2, p3, t2)).getDistanceSquared()

            if (d1 < d2) {
                rechts = t2
            } else {
                links = t1
            }
        }

        val feinerT = (links + rechts) / 2f
        besterPunkt = cubic(p0, p1, p2, p3, feinerT)

        return pos - besterPunkt
    }

    public fun c1(
        _start: KartenPosition = start.value,
        _ende: KartenPosition = ende.value,
    ): Offset {
        val startRichtung = startKante.tangente()
        val dx = _ende.x - _start.x
        val dy = _ende.y - _start.y
        val distanz = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val kontrollAbstand = max(48f, distanz * 0.35f).coerceAtMost(240f)
        return _start + startRichtung * kontrollAbstand
    }

    public fun c2(
        _start: KartenPosition = start.value,
        _ende: KartenPosition = ende.value,
    ): Offset {
        val endeRichtung = endeKante.tangente()
        val dx = _ende.x - _start.x
        val dy = _ende.y - _start.y
        val distanz = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val kontrollAbstand = max(48f, distanz * 0.35f).coerceAtMost(240f)
        return _ende + endeRichtung * kontrollAbstand
    }

    private fun cubic(
        p0: Offset,
        p1: Offset,
        p2: Offset,
        p3: Offset,
        t: Float,
    ): Offset {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        return p0 * uuu +
                p1 * (3f * uu * t) +
                p2 * (3f * u * tt) +
                p3 * ttt
    }

    private fun nächsterPunktAufSegment(
        pos: Offset,
        a: Offset,
        b: Offset,
    ): Offset {
        val ab = b - a
        val abSq = ab.getDistanceSquared()

        if (abSq == 0f) return a

        val t = (((pos - a) * ab) / abSq).coerceIn(0f, 1f)
        return a + ab * t
    }

    private fun segmentAnteil(
        a: Offset,
        b: Offset,
        punkt: Offset,
    ): Float {
        val ab = b - a
        val abSq = ab.getDistanceSquared()

        if (abSq == 0f) return 0f

        return (((punkt - a) * ab) / abSq).coerceIn(0f, 1f)
    }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "bezier"
    }
}
