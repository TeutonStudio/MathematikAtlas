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

    fun migriere(alt: KnotenDaten): KnotenDaten = KartenDaten(name="Migration",knoten=listOf(alt))
        .migriereHistorischeKnotenRollen(::migriereGeprüft).knoten.single()

    internal fun migriereGeprüft(alt: KnotenDaten): KnotenDaten {
        if (RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return alt
        val ziel = zuordnungen[alt.art] ?: return alt
        prüfeHistorischeAnschlussForm(alt)
        require(alt.art!="mathematik.adjunktion" || alt.parameter[AUSSAGEN_LOGIK_SEMANTIK] in setOf(null,AUSSAGEN_LOGIK_XOR)) {"Unbekannte historische Adjunktionssemantik."}
        val id = if (alt.art == "mathematik.adjunktion" && alt.parameter[AUSSAGEN_LOGIK_SEMANTIK] != AUSSAGEN_LOGIK_XOR)
            "aussage.konjunktion" else ziel.operatorId
        val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(ziel.knotenArt) ?: return alt
        val definition = StrukturRechnerOperatoren.finde(familie, id)
        val eingänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val ausgänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
        val variadisch = istAssoziativerRechnerOperator(id)
        val bekannteNamen=when(alt.art) {
            "mathematik.matrixProdukt" -> setOf("a","b")
            "mathematik.matrixInvertieren","mathematik.transponiereMatrix" -> setOf("matrix")
            "mathematik.negation" -> setOf("aussage")
            "mathematik.vektornorm" -> setOf("vektor")
            else -> if(eingänge.map {it.name}.toSet()==setOf("links","rechts")) setOf("links","rechts") else setOf("a","b")
        }
        require(eingänge.all {it.name in bekannteNamen || variadisch && it.dynamischErzeugt && it.name.isNotBlank()}) {"Unbekannte historische Rechnerrolle."}
        require(eingänge.map {it.reihenfolge}.distinct().size==eingänge.size) {"Mehrdeutige historische Rechnerreihenfolge."}
        if(!variadisch || alt.art=="mathematik.matrixProdukt") require(eingänge.map {it.name}.toSet()==bekannteNamen) {"Historische Pflichtrollen fehlen oder sind überzählig."}
        if(alt.art=="mathematik.matrixProdukt" || alt.art=="mathematik.kreuzprodukt" || alt.art=="mathematik.implikation") {
            val erwartet=if("links" in bekannteNamen) listOf("links","rechts") else listOf("a","b")
            require(eingänge.map {it.name}==erwartet) {"Die Reihenfolge widerspricht den historischen festen Operandenrollen."}
        }
        val eingangsArten=when(familie) {
            StrukturRechnerKnotenFamilie.MATRIX -> setOf(MathematikAnschlussArten.Matrix.id,MathematikAnschlussArten.Vektor.id,MathematikAnschlussArten.Methode.id)
            StrukturRechnerKnotenFamilie.VEKTOR -> setOf(MathematikAnschlussArten.Vektor.id,MathematikAnschlussArten.Methode.id)
            else -> setOf(MathematikAnschlussArten.Aussage.id)
        }
        eingänge.forEach {prüfeHistorischenPortTyp(it,eingangsArten,variadisch)}
        val ausgangsArt=when(familie) {
            StrukturRechnerKnotenFamilie.MATRIX -> MathematikAnschlussArten.Matrix.id
            StrukturRechnerKnotenFamilie.VEKTOR -> if(id in setOf("vektor.norm","vektor.skalarprodukt")) MathematikAnschlussArten.Zahl.id else MathematikAnschlussArten.Vektor.id
            else -> MathematikAnschlussArten.Aussage.id
        }
        prüfeHistorischenPortTyp(ausgänge.single(),setOf(ausgangsArt))
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
        val neueEingänge = eingänge.mapIndexed { index, port -> port.copy(name = namen[index], kannSichErweitern = variadisch) }
        val basis = alt.copy(art = ziel.knotenArt,
            anschlüsse = neueEingänge + ausgänge.single().copy(name = vectorPorts.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }?.name ?: "wert"),
            parameter = alt.parameter + (RECHNER_OPERATOR_PARAMETER to id))
        val neu = if (familie == StrukturRechnerKnotenFamilie.VEKTOR)
            konfiguriereVektorRechner(basis, requireNotNull(VektorRechnerOperator.vonIdOderNull(id)))
        else konfiguriereStrukturRechner(basis, familie, id)
        if (neu.anschlüsse.map { it.id }.toSet() != alt.anschlüsse.map { it.id }.toSet()) return alt.copy(
            parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to
                "${alt.art} bleibt erhalten: Anschlussidentitäten konnten nicht vollständig erhalten werden."))
        // Preserve custom title, style, original ordering and stable references after role normalization.
        val altePorts = basis.anschlüsse.associateBy { it.id }
        return normalisiereRechnerMethodenAnschluesse(neu.copy(name = alt.name, anschlüsse = neu.anschlüsse.map { port ->
            altePorts[port.id]?.let { original ->
                val bekannteMatrixErweiterung = familie == StrukturRechnerKnotenFamilie.MATRIX && original.richtung == AnschlussRichtung.Eingang && original.zulässigeArten.isEmpty()
                original.copy(name=port.name,kannSichErweitern=port.kannSichErweitern,
                    art=if(bekannteMatrixErweiterung) port.art else original.art,
                    zulässigeArten=if(bekannteMatrixErweiterung) port.zulässigeArten else original.zulässigeArten)
            } ?: port
        }))
    }
}

fun KartenDaten.migriereRechnerEinzeloperationen(): KartenDaten = migriereHistorischeKnotenRollen(RechnerEinzeloperationMigration::migriereGeprüft)
