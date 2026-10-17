package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

const val TENSORRAUM_LEGACY_DIMENSIONEN_ART = "mathematik.tensorraumDimensionenLegacy"

/**
 * Materialisiert den historischen Inspector-String eines Tensorraums einmalig als sichtbare Tupelquelle.
 * Bereits migrierte Knoten werden unverändert gelassen; die IDs sind deterministisch aus der Tensorraum-ID abgeleitet.
 */
fun KartenDaten.migriereTensorraumDimensionen(): KartenDaten {
    val neueKnoten = knoten.toMutableList()
    val neueVerbindungen = verbindungen.toMutableList()
    var geändert = false

    knoten.filter { it.art == "mathematik.tensorraum" }.forEach { tensorraum ->
        if (tensorraum.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang && it.name == "dimensionen" }) return@forEach
        val roh = tensorraum.parameter["dimensionen"] ?: return@forEach
        val dimensionen = roh.split(',').map(String::trim).filter(String::isNotBlank)
        require(dimensionen.isNotEmpty()) { "Historischer Tensorraum ${tensorraum.id.wert} besitzt keine Dimensionen." }
        require(dimensionen.all { it.toIntOrNull()?.let { wert -> wert > 0 } == true }) {
            "Historische Tensorraumdimensionen '$roh' sind nicht vollständig positive ganze Zahlen."
        }

        val eingangId = AnschlussId("${tensorraum.id.wert}-dimensionen")
        val eingang = AnschlussDaten(
            id = eingangId,
            name = "dimensionen",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Tupel.id,
            reihenfolge = tensorraum.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang },
        )
        val migriert = tensorraum.copy(
            anschlüsse = tensorraum.anschlüsse + eingang,
            parameter = tensorraum.parameter - "dimensionen",
        )
        neueKnoten[neueKnoten.indexOfFirst { it.id == tensorraum.id }] = migriert

        val quelleId = KnotenId("${tensorraum.id.wert}-dimensionen-legacy")
        val quelleAusgangId = AnschlussId("${tensorraum.id.wert}-dimensionen-legacy-tupel")
        if (neueKnoten.none { it.id == quelleId }) {
            neueKnoten += KnotenDaten(
                id = quelleId,
                art = TENSORRAUM_LEGACY_DIMENSIONEN_ART,
                name = "Dimensionen",
                position = GraphPunkt(tensorraum.position.x - 300f, tensorraum.position.y + 80f),
                größe = GraphGröße(220f, 96f),
                anschlüsse = listOf(
                    AnschlussDaten(
                        id = quelleAusgangId,
                        name = "tupel",
                        richtung = AnschlussRichtung.Ausgang,
                        kante = AnschlussKante.Rechts,
                        art = MathematikAnschlussArten.Tupel.id,
                    ),
                ),
                parameter = mapOf("werte" to dimensionen.joinToString(",")),
            )
        }
        val verbindungsId = VerbindungsId("${tensorraum.id.wert}-dimensionen-legacy-verbindung")
        if (neueVerbindungen.none { it.id == verbindungsId }) {
            neueVerbindungen += VerbindungDaten(
                id = verbindungsId,
                von = AnschlussVerweis(quelleId, quelleAusgangId),
                zu = AnschlussVerweis(tensorraum.id, eingangId),
            )
        }
        geändert = true
    }

    return if (geändert) copy(knoten = neueKnoten, verbindungen = neueVerbindungen) else this
}

/** Ersetzt historische n/m-Rollen durch eine sichtbare, verlustfreie Tupelzusammenstellung. */
fun KartenDaten.migriereMatrizenDimensionstupel(): KartenDaten {
    val neueKnoten = knoten.toMutableList()
    val neueVerbindungen = verbindungen.toMutableList()
    knoten.forEach { alt ->
        if (RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@forEach
        val auswahl = MengenKnotenAuswahl.vonIdOderNull(alt.parameter[MENGEN_KNOTEN_AUSWAHL])
        val quelle = alt.art == MENGEN_KNOTEN_ART && auswahl?.istMatrizenmenge == true && alt.kartenVerweis == null
        val rechner = alt.art == de.TeutonStudio.MathematikRechenSystem.kern.MengenRechner.KNOTEN_ART &&
            de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerOperator.vonIdOderNull(alt.parameter[MENGENRECHNER_OPERATOR_PARAMETER]) ==
                de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerOperator.MATRIZENMENGE
        if (!quelle && !rechner) return@forEach
        val versuch = runCatching {
            val ports = listOf("n", "m").map { name -> alt.anschlüsse.single { it.name == name && it.richtung == AnschlussRichtung.Eingang } }
            require(ports.all { (it.art == MathematikAnschlussArten.Zahl.id && it.zulässigeArten.isEmpty() || it.art == MathematikAnschlussArten.AtlasWert.id && it.zulässigeArten == setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id)) }) { "Die historischen Dimensionstypen sind mehrdeutig." }
            fun fallbackFür(k: KnotenDaten, rollen: Set<String>) = k.eingangsKartenVerweise.keys.any { it.substringBefore("@karte:") in rollen }
            fun methodenQuelle(v: VerbindungDaten, besucht: Set<KnotenId>): Boolean {
                val k = knoten.singleOrNull { it.id == v.von.knotenId } ?: return true
                if (k.id in besucht) return true
                val port = k.anschlüsse.singleOrNull { it.id == v.von.anschlussId } ?: return true
                if (port.art == MathematikAnschlussArten.Methode.id || MathematikAnschlussArten.Methode.id in port.zulässigeArten) return true
                val priorisierung = port.artPriorisiertEingänge ?: return false
                if (MathematikAnschlussArten.Methode.id !in priorisierung.prioritäten) return false
                val rollen = priorisierung.eingänge.toSet()
                if (fallbackFür(k, rollen)) return true
                val ids = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name in rollen }.map { it.id }.toSet()
                return verbindungen.filter { it.zu.knotenId == k.id && it.zu.anschlussId in ids }.any { methodenQuelle(it, besucht + k.id) }
            }
            require(!fallbackFür(alt, setOf("n", "m")) && verbindungen
                .filter { it.zu.knotenId == alt.id && ports.any { p -> p.id == it.zu.anschlussId } }
                .none { methodenQuelle(it, setOf(alt.id)) }) {
                "Methodenwertige oder nicht eindeutig auflösbare Dimensionen benötigen eine explizite gemeinsame Dimensionsmethode; das Original bleibt erhalten."
            }
            val hilfsId = KnotenId("${alt.id.wert}:dimensionstupel")
            val dimensionId = AnschlussId("${alt.id.wert}:raum:dimension")
            val ausgangId = AnschlussId("${alt.id.wert}:dimensionstupel:ausgang")
            val kantenId = VerbindungsId("${alt.id.wert}:dimensionstupel:verbindung")
            require(neueKnoten.none { it.id == hilfsId } && neueVerbindungen.none { it.id == kantenId } &&
                neueKnoten.flatMap { it.anschlüsse }.none { it.id == dimensionId || it.id == ausgangId }) { "Eine Migrationsidentität ist bereits belegt." }
            fun dimensionsRolle(name: String) = name in setOf("n", "m") || name.startsWith("n@") || name.startsWith("m@")
            fun dimensionsParameter(name: String) = name.startsWith("standardwert.n") || name.startsWith("standardwert.m")
            val hilfe = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt(alt.position.x - 280f, alt.position.y + 80f)).copy(
                id = hilfsId, name = "Dimensionen",
                anschlüsse = ports.mapIndexed { i, p -> p.copy(reihenfolge = i, kannSichErweitern = true) } +
                    AnschlussDaten(id = ausgangId, name = "tupel", richtung = AnschlussRichtung.Ausgang,
                        kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id),
                parameter = MathematikKnotenVorlagen.Tupel.standardParameter + alt.parameter.filterKeys(::dimensionsParameter),
                eingangsKartenVerweise = alt.eingangsKartenVerweise.filterKeys(::dimensionsRolle),
            )
            val parameter = alt.parameter.filterKeys { !dimensionsParameter(it) } + if (quelle) mapOf(
                MENGEN_KNOTEN_AUSWAHL to (if (auswahl == MengenKnotenAuswahl.REELLE_MATRIZEN) MengenKnotenAuswahl.REELLE_ZAHLEN else MengenKnotenAuswahl.KOMPLEXE_ZAHLEN).stabileId,
                MENGEN_DIMENSIONSART_PARAMETER to MENGEN_DIMENSION_TENSOR,
            ) else mapOf(MENGENRECHNER_OPERATOR_PARAMETER to de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerOperator.TENSORRAUM.stabileId)
            // Historische Räume bleiben matrixgebunden; neue Tensorräume erlauben beide Wertstrukturen.
            val neu = alt.copy(
                parameter = parameter + ("raum.matrixHistorisch" to "true"),
                anschlüsse = alt.anschlüsse.filterNot { it in ports } + AnschlussDaten(id = dimensionId, name = "dimension",
                    richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id, reihenfolge = if (quelle) 0 else 1),
                eingangsKartenVerweise = alt.eingangsKartenVerweise.filterKeys { !dimensionsRolle(it) },
            )
            Triple(neu, hilfe, ports)
        }
        versuch.fold(onSuccess = { (neu, hilfe, ports) ->
            neueKnoten[neueKnoten.indexOfFirst { it.id == alt.id }] = neu
            neueKnoten += hilfe
            neueVerbindungen.indices.forEach { index ->
                val v = neueVerbindungen[index]
                if (v.zu.knotenId == alt.id && ports.any { it.id == v.zu.anschlussId })
                    neueVerbindungen[index] = v.copy(zu = v.zu.copy(knotenId = hilfe.id))
            }
            neueVerbindungen += VerbindungDaten(id = VerbindungsId("${alt.id.wert}:dimensionstupel:verbindung"),
                von = AnschlussVerweis(hilfe.id, hilfe.anschlüsse.last().id),
                zu = AnschlussVerweis(neu.id, neu.anschlüsse.single { it.name == "dimension" && it.richtung == AnschlussRichtung.Eingang }.id))
        }, onFailure = { fehler ->
            neueKnoten[neueKnoten.indexOfFirst { it.id == alt.id }] = alt.copy(parameter = alt.parameter +
                (RECHNER_MIGRATION_DIAGNOSE to "Dimensionstupel: ${fehler.message}"))
        })
    }
    return if (neueKnoten == knoten && neueVerbindungen == verbindungen) this else copy(knoten = neueKnoten, verbindungen = neueVerbindungen)
}
