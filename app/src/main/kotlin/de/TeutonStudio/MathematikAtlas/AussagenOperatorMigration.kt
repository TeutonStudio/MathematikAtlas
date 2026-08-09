package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_SEMANTIK
import de.TeutonStudio.MathematikKnoten.AUSSAGEN_LOGIK_XOR
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/**
 * Bewahrt historische Aussagenverträge und führt rein benennende Migrationen
 * idempotent aus. Anschluss-IDs, Knoten-IDs und Verbindungen werden dabei nicht
 * verändert.
 */
internal fun migriereAussagenOperatoren(karte: KartenDaten): KartenDaten = karte.copy(
    knoten = karte.knoten.map(::migriereAussagenKnoten),
)

private fun migriereAussagenKnoten(knoten: KnotenDaten): KnotenDaten = when {
    istHistorischeAussageZuMethodeVariante(knoten) ->
        knoten.copy(name = "Aussage zu Prädikat")

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

/**
 * Nur der historische Standardname der echten Prädikatsvariante wird ersetzt.
 * Ein allgemeiner Term-zu-Methode-Knoten und benutzerdefinierte Namen bleiben
 * unverändert, selbst wenn sie dieselbe persistierte Knotenart teilen.
 */
private fun istHistorischeAussageZuMethodeVariante(knoten: KnotenDaten): Boolean {
    if (knoten.art != "mathematik.termZuMethode" || knoten.name != "Aussage zu Methode") return false
    val eingang = knoten.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "term"
    } ?: return false
    val ausgang = knoten.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "methode"
    } ?: return false
    return eingang.art == MathematikAnschlussArten.Aussage.id &&
        ausgang.art == MathematikAnschlussArten.AussageMethode.id
}
