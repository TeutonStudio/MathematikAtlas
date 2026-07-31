package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_SEMANTIK
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_XOR
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/**
 * Bewahrt die bis v2.9.1 als UND ausgewertete Adjunktion, indem alte Instanzen
 * explizit zu Konjunktionen werden. Nur neu markierte Knoten erhalten XOR-Semantik.
 */
internal fun migriereAussagenOperatoren(karte: KartenDaten): KartenDaten = karte.copy(
    knoten = karte.knoten.map(::migriereAussagenKnoten),
)

private fun migriereAussagenKnoten(knoten: KnotenDaten): KnotenDaten = when {
    knoten.art == "mathematik.adjunktion" && knoten.parameter[AUSSAGEN_LOGIK_SEMANTIK] != AUSSAGEN_LOGIK_XOR ->
        knoten.copy(
            art = "mathematik.konjunktion",
            name = if (knoten.name == "Adjunktion") "Konjunktion" else knoten.name,
            anschlüsse = knoten.anschlüsse.map { anschluss ->
                if (anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(kannSichErweitern = true) else anschluss
            },
            parameter = (knoten.parameter - AUSSAGEN_LOGIK_SEMANTIK) + mapOf(
                "festeEingänge" to "2",
                "operatorAnzeige" to (knoten.parameter["operatorAnzeige"] ?: "wert"),
            ),
        )

    knoten.art == MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART &&
        knoten.parameter["operator"] == "adjunktion" &&
        knoten.parameter[AUSSAGEN_LOGIK_SEMANTIK] != AUSSAGEN_LOGIK_XOR ->
        knoten.copy(
            name = if (knoten.name == "Iterierte Adjunktion") "Iterierte Konjunktion" else knoten.name,
            parameter = (knoten.parameter - AUSSAGEN_LOGIK_SEMANTIK) + ("operator" to "konjunktion"),
        )

    knoten.art in setOf("mathematik.konjunktion", "mathematik.disjunktion", "mathematik.adjunktion") ->
        knoten.copy(
            anschlüsse = knoten.anschlüsse.map { anschluss ->
                if (anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(kannSichErweitern = true) else anschluss
            },
            parameter = knoten.parameter + mapOf(
                "festeEingänge" to "2",
                "operatorAnzeige" to (knoten.parameter["operatorAnzeige"] ?: "wert"),
            ),
        )

    else -> knoten
}
