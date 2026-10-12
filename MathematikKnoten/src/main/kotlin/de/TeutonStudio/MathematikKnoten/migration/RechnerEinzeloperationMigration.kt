package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val RECHNER_MIGRATION_DIAGNOSE = "rechnerMigrationDiagnose"

data class HistorischerRechnerOperator(val knotenArt: String, val operatorId: String)

/** Ergänzt die bereits vorhandenen Zahlen-, Mengen-, Vektor- und Tensormigrationen. */
object RechnerEinzeloperationMigration {
    val zuordnungen: Map<String, HistorischerRechnerOperator> = buildMap {
        fun matrix(art: String, id: String) { put(art, HistorischerRechnerOperator(MatrixRechner.KNOTEN_ART, id)) }
        matrix("mathematik.matrixProdukt", "matrix.produkt")
        matrix("mathematik.matrixInvertieren", "matrix.inverse")
        matrix("mathematik.transponiereMatrix", "matrix.transponieren")
        VektorRechnerMigration.alteKnotenArten.forEach { (art, op) -> put(art, HistorischerRechnerOperator(VektorRechner.KNOTEN_ART, op.stabileId)) }
        fun aussage(art: String, id: String) { put(art, HistorischerRechnerOperator(AussagenSatzRechner.KNOTEN_ART, id)) }
        aussage("mathematik.negation", "aussage.negation")
        aussage("mathematik.konjunktion", "aussage.konjunktion")
        aussage("mathematik.disjunktion", "aussage.disjunktion")
        aussage("mathematik.adjunktion", "aussage.adjunktion")
        aussage("mathematik.implikation", "aussage.implikation")
        aussage("mathematik.äquivalenz", "aussage.aequivalenz")
    }

    fun migriere(alt: KnotenDaten): KnotenDaten {
        val ziel = zuordnungen[alt.art] ?: return alt
        val id = if (alt.art == "mathematik.adjunktion" && alt.parameter[AUSSAGEN_LOGIK_SEMANTIK] != AUSSAGEN_LOGIK_XOR)
            "aussage.konjunktion" else ziel.operatorId
        val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(ziel.knotenArt) ?: return alt
        val definition = StrukturRechnerOperatoren.finde(familie, id)
        val eingänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val ausgänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
        val variadisch = istAssoziativerRechnerOperator(id)
        if (ausgänge.size != 1 || eingänge.size < definition.eingänge.size || !variadisch && eingänge.size != definition.eingänge.size) {
            return alt.copy(parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to
                "${alt.art} bleibt erhalten: Die gespeicherten Anschlussrollen sind nicht eindeutig auf $id abbildbar."))
        }
        val vectorPorts = if (familie == StrukturRechnerKnotenFamilie.VEKTOR)
            vektorRechnerAnschluesse(requireNotNull(VektorRechnerOperator.vonIdOderNull(id))) else emptyList()
        val rollen = if (vectorPorts.isNotEmpty()) vectorPorts.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name }
            else definition.eingänge.map { it.name }
        val namen = eingänge.mapIndexed { index, port -> rollen.getOrNull(index) ?: port.name }
        if (namen.distinct().size != namen.size) return alt.copy(parameter = alt.parameter +
            (RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: Mehrdeutige Operandenrollen."))
        val umbenennung = eingänge.mapIndexed { index, port -> port.name to namen[index] }.toMap()
        val standardwerte = alt.parameter.filterKeys { it.startsWith("standardwert.") }.map { (key, value) ->
            "standardwert.${umbenennung[key.removePrefix("standardwert.")] ?: key.removePrefix("standardwert.")}" to value
        }
        val kartenQuellen = alt.eingangsKartenVerweise.map { (name, verweis) -> (umbenennung[name] ?: name) to verweis }
        if (standardwerte.map { it.first }.distinct().size != standardwerte.size ||
            kartenQuellen.map { it.first }.distinct().size != kartenQuellen.size) return alt.copy(parameter = alt.parameter +
                (RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: Mehrdeutige gespeicherte Eingangsquellen."))
        val neueEingänge = eingänge.mapIndexed { index, port -> port.copy(name = namen[index], kannSichErweitern = variadisch) }
        val basis = alt.copy(art = ziel.knotenArt,
            anschlüsse = neueEingänge + ausgänge.single().copy(name = vectorPorts.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }?.name ?: "wert"),
            eingangsKartenVerweise = kartenQuellen.toMap(),
            parameter = alt.parameter.filterKeys { it != RECHNER_MIGRATION_DIAGNOSE && !it.startsWith("standardwert.") } +
                standardwerte.toMap() + (RECHNER_OPERATOR_PARAMETER to id))
        val neu = if (familie == StrukturRechnerKnotenFamilie.VEKTOR)
            konfiguriereVektorRechner(basis, requireNotNull(VektorRechnerOperator.vonIdOderNull(id)))
        else konfiguriereStrukturRechner(basis, familie, id)
        if (neu.anschlüsse.map { it.id }.toSet() != alt.anschlüsse.map { it.id }.toSet()) return alt.copy(
            parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to
                "${alt.art} bleibt erhalten: Anschlussidentitäten konnten nicht vollständig erhalten werden."))
        // Preserve custom title, style, original ordering and stable references after role normalization.
        val altePorts = basis.anschlüsse.associateBy { it.id }
        return neu.copy(name = alt.name, anschlüsse = neu.anschlüsse.map { port ->
            altePorts[port.id]?.let { port.copy(reihenfolge = it.reihenfolge, dynamischErzeugt = it.dynamischErzeugt) } ?: port
        })
    }
}

fun KartenDaten.migriereRechnerEinzeloperationen(): KartenDaten = copy(knoten = knoten.map(RechnerEinzeloperationMigration::migriere))
