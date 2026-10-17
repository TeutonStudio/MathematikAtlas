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

/** Vollständige Hexringe mit 6k Plätzen einschließlich der Kantenzwischenplätze. */
internal fun planePieFächer(einträge: List<PieEintrag>, größen: Map<String, GraphGröße>, zoom: Float,
    richtung: Float, abstand: Float = 24f): List<PiePlatz> {
    if (einträge.isEmpty()) return emptyList()
    val gruppen = einträge.groupBy { it.gruppe }
    val maße = einträge.associate { e -> e.id to (größen[e.id] ?: e.vorlage.standardGröße) }
    val schritt = hypot(maße.values.maxOf { it.breite * zoom } + abstand * 2f,
        maße.values.maxOf { it.höhe * zoom } + abstand * 2f)
    val sektor = 360f / gruppen.size
    val platziert = mutableListOf<PiePlatz>()
    gruppen.entries.forEachIndexed { index, (_, inhalt) ->
        var nächster = 0
        var ring = 1
        val mitteWinkel = (richtung + (index + .5f) * sektor) * PI.toFloat() / 180f
        val normal = Offset(cos(mitteWinkel), sin(mitteWinkel))
        while (nächster < inhalt.size) {
            for (punkt in pieHexRing(ring, schritt)) {
                val grad = ((atan2(punkt.y, punkt.x) * 180f / PI.toFloat() - richtung) % 360f + 360f) % 360f
                if (gruppen.size > 1 && floor(grad / sektor).toInt().coerceAtMost(gruppen.size - 1) != index) continue
                val e = inhalt[nächster]
                val g = maße.getValue(e.id)
                val b = g.breite * zoom
                val h = g.höhe * zoom
                val rahmen = Rect(punkt.x - b / 2f, punkt.y - h / 2f, punkt.x + b / 2f, punkt.y + h / 2f)
                if (rahmen.inflate(abstand).contains(Offset.Zero)) continue
                if (gruppen.size == 2 && punkt.x * normal.x + punkt.y * normal.y -
                    abs(normal.x) * b / 2f - abs(normal.y) * h / 2f < abstand / 2f) continue
                platziert += PiePlatz(e, rahmen)
                nächster++
                if (nächster == inhalt.size) break
            }
            ring++
        }
    }
    return platziert
}

internal fun pieHexRing(ring: Int, schritt: Float): List<Offset> {
    require(ring > 0 && schritt > 0f)
    val ecken = List(6) { i ->
        val winkel = -PI.toFloat() / 2f + i * PI.toFloat() / 3f
        Offset(cos(winkel), sin(winkel)) * (ring * schritt)
    }
    return ecken.indices.flatMap { i ->
        List(ring) { j -> ecken[i] + (ecken[(i + 1) % 6] - ecken[i]) * (j.toFloat() / ring) }
    }
}

/** Anschlussbezogene Auswahl verwendet dieselbe Geometrie wie die normale Auswahl. */
internal fun planePieKreise(einträge: List<PieEintrag>, größen: Map<String, GraphGröße>, zoom: Float,
    abstand: Float = 56f): List<PiePlatz> = planePieFächer(einträge, größen, zoom, -90f, abstand)

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
