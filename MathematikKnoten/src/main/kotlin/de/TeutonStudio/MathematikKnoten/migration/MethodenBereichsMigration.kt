package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

/**
 * Trennt historische gemischte `mathematik.restriktion`-Knoten verlustfrei in die
 * beiden heute expliziten semantischen Modi.
 *
 * Ein historischer Knoten mit mindestens einer verbundenen `ergänzung.*` wird zur
 * Bereichsanpassung. Ohne verbundene Ergänzung bleibt er eine reine Restriktion;
 * rein abgeleitete freie Ergänzungshandles werden dann entfernt. Knoten-, Anschluss-
 * und Edge-IDs verbundener Daten bleiben unverändert. Die Migration ist idempotent.
 */
internal fun KartenDaten.migriereMethodenBereichsOperatoren(): KartenDaten {
    var verändert = false
    val neueKnoten = knoten.map { knoten ->
        if (knoten.art != RESTRIKTIONS_KNOTEN_ART) return@map knoten

        val vorhandenerModus = knoten.parameter[METHODEN_BEREICHS_OPERATOR_PARAMETER]
        val ergänzungen = knoten.anschlüsse.filter { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
        val verbundeneErgänzungen = ergänzungen.filter { anschluss ->
            val ref = AnschlussVerweis(knoten.id, anschluss.id)
            verbindungen.any { it.von == ref || it.zu == ref }
        }
        val abgeleiteterModus = if (verbundeneErgänzungen.isNotEmpty()) {
            METHODEN_BEREICHS_OPERATOR_ANPASSUNG
        } else {
            METHODEN_BEREICHS_OPERATOR_RESTRIKTION
        }
        val modus = vorhandenerModus ?: abgeleiteterModus

        val neueAnschlüsse = if (
            modus == METHODEN_BEREICHS_OPERATOR_RESTRIKTION && verbundeneErgänzungen.isEmpty()
        ) {
            knoten.anschlüsse.filterNot { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
        } else {
            knoten.anschlüsse
        }
        val neueParameter = if (vorhandenerModus == modus) knoten.parameter
        else knoten.parameter + (METHODEN_BEREICHS_OPERATOR_PARAMETER to modus)

        if (neueAnschlüsse == knoten.anschlüsse && neueParameter == knoten.parameter) knoten
        else {
            verändert = true
            knoten.copy(anschlüsse = neueAnschlüsse, parameter = neueParameter)
        }
    }
    return if (verändert) copy(knoten = neueKnoten) else this
}
