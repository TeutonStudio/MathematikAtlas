package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.math.*

internal data class PieEintrag(val id: String, val gruppe: String, val vorlage: KnotenVorlage, val mitÜbersetzer: Boolean = false)
internal data class PiePlatz(val eintrag: PieEintrag, val rahmen: Rect)

/** Bestehende Konzeptzuordnung, ohne geplante und historische Einträge. */
internal fun pieKonzeptEinträge(einträge: List<KonzeptBibliothekEintrag>, kategorie: String): List<PieEintrag> =
    einträge.filter { it.istEinfügbar && it.kategoriePfade.any { p -> p.firstOrNull() == kategorie } }
        .distinctBy { it.id }.map { e ->
            val pfad = e.kategoriePfade.filter { it.firstOrNull() == kategorie }.minBy { it.joinToString("/") }
            PieEintrag(e.id, if (pfad.size > 1) KonzeptBibliothekRegister.bezeichnungFür(pfad.drop(0)) else "Weitere", e.vorlage!!)
        }.sortedWith(compareBy({ it.gruppe }, { it.vorlage.name }, { it.id }))

/** Ein fester Versionsverweis in beiden Hälften; keine Filterung nach vorhandener Richtung. */
internal fun pieKartenEinträge(karten: List<KartenDaten>, vorlage: (KartenDaten, KartenKnotenZustand) -> KnotenVorlage): List<PieEintrag> =
    KartenKnotenZustand.entries.flatMap { modus ->
        karten.distinctBy { it.id }.sortedWith(compareBy({ it.name.lowercase() }, { it.id.wert })).map {
            PieEintrag("karte/${it.id.wert}/${it.version}/${modus.name}",
                if (modus == KartenKnotenZustand.Schnittstelle) "Ein-/Ausgänge" else "Methodenausgang", vorlage(it, modus))
        }
    }

/** Gruppen belegen Kreissektoren; ihre Einträge verteilen sich auf radiale Reihen.
 * Die beiden Kartendarstellungen belegen dadurch jeweils eine volle Kreishälfte.
 * Große Knoten rücken nach außen, ohne ihre Zoomgröße oder den Ursprung zu ändern.
 */
internal fun planePieFächer(einträge: List<PieEintrag>, größen: Map<String, GraphGröße>, zoom: Float,
    richtung: Float, abstand: Float = 24f): List<PiePlatz> {
    val gruppen = einträge.groupBy { it.gruppe }
    val platziert = mutableListOf<PiePlatz>()
    val sektor = 360f / gruppen.size.coerceAtLeast(1)
    gruppen.entries.forEachIndexed { index, (_, inhalt) ->
        val spalten = minOf(inhalt.size, maxOf(1, ceil(8f / gruppen.size).toInt()))
        inhalt.forEachIndexed { nummer, e ->
            val winkel = richtung + index * sektor + (nummer % spalten + .5f) * sektor / spalten
            val rad = winkel * PI.toFloat() / 180f
            val strahl = Offset(cos(rad), sin(rad))
            val größe = größen[e.id] ?: e.vorlage.standardGröße
            val b = größe.breite * zoom
            val h = größe.höhe * zoom
            val sektorMitte = (richtung + (index + .5f) * sektor) * PI.toFloat() / 180f
            val halbkreisNormal = Offset(cos(sektorMitte), sin(sektorMitte))
            var radius = abstand * 4f
            var rahmen: Rect
            do {
                val mitte = strahl * radius
                rahmen = Rect(mitte.x - b / 2f, mitte.y - h / 2f, mitte.x + b / 2f, mitte.y + h / 2f)
                radius += maxOf(abstand, 16f)
            } while (platziert.any { it.rahmen.inflate(abstand).overlaps(rahmen) } || rahmen.inflate(abstand).contains(Offset.Zero) ||
                (gruppen.size == 2 && rahmen.center.x * halbkreisNormal.x + rahmen.center.y * halbkreisNormal.y -
                    abs(halbkreisNormal.x) * b / 2f - abs(halbkreisNormal.y) * h / 2f < abstand / 2f))
            platziert += PiePlatz(e, rahmen)
        }
    }
    return platziert
}

/** Echte konzentrische Kreise; Beschriftungen sind in den Hüllkreisen enthalten. */
internal fun planePieKreise(einträge: List<PieEintrag>, größen: Map<String, GraphGröße>, zoom: Float,
    abstand: Float = 56f): List<PiePlatz> {
    var außen = abstand * 2f
    return einträge.chunked(8).flatMap { ring ->
        val radien = ring.map { e ->
            val g = größen[e.id] ?: e.vorlage.standardGröße
            hypot(g.breite * zoom, g.höhe * zoom + abstand) / 2f
        }
        val hülle = radien.max()
        val radius = maxOf(außen + abstand + hülle,
            if (ring.size > 1) (hülle + abstand / 2f) / sin(PI.toFloat() / ring.size) else 0f)
        außen = radius + hülle
        ring.mapIndexed { i, e ->
            val g = größen[e.id] ?: e.vorlage.standardGröße
            val winkel = -PI.toFloat() / 2f + i * 2f * PI.toFloat() / ring.size
            val mitte = Offset(cos(winkel), sin(winkel)) * radius
            PiePlatz(e, Rect(mitte.x - g.breite * zoom / 2f, mitte.y - g.höhe * zoom / 2f,
                mitte.x + g.breite * zoom / 2f, mitte.y + g.höhe * zoom / 2f))
        }
    }
}

/** Flüchtiger Menüzoom; der Punkt unter dem Fingermittelpunkt bleibt dort liegen. */
internal data class PieTransformation(val position: Offset, val zoom: Float)
internal fun transformierePie(position: Offset, zoom: Float, mittelpunkt: Offset, pan: Offset, faktor: Float): PieTransformation {
    val neu = (zoom * faktor).coerceIn(.2f, 4f)
    return PieTransformation(mittelpunkt + pan - (mittelpunkt - position) * (neu / zoom), neu)
}
internal fun skalierePiePlätze(plätze: List<PiePlatz>, faktor: Float): List<PiePlatz> = plätze.map {
    it.copy(rahmen = Rect(it.rahmen.topLeft * faktor, it.rahmen.bottomRight * faktor))
}

internal fun pieWeltPosition(zeiger: Offset, griff: GraphPunkt, ansicht: AnsichtsFenster, dichte: Float): GraphPunkt =
    GraphPunkt((zeiger.x - ansicht.verschiebung.x) / (ansicht.zoom * dichte),
        (zeiger.y - ansicht.verschiebung.y) / (ansicht.zoom * dichte)) - griff

internal fun pieKategorieAmPunkt(punkt: Offset, mitte: Offset, anzahl: Int, innen: Float, außen: Float): Int? {
    val delta = punkt - mitte
    if (delta.getDistance() !in innen..außen || anzahl == 0) return null
    val grad = (atan2(delta.y, delta.x) * 180f / PI.toFloat() + 90f + 360f) % 360f
    return floor(grad / (360f / anzahl)).toInt().coerceIn(0, anzahl - 1)
}
