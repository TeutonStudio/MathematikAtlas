package de.TeutonStudio.MathematikRechenSystem.kern

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Deterministischer, versionsbehafteter JSON-Codec für den Zahlbereichsgraphen.
 *
 * Das Kernmodul bleibt ohne zusätzliche Serialisierungsabhängigkeit. Freie Texte
 * werden deshalb URL-sicher als Base64-Felder in stabilen Datensätzen gespeichert.
 * Die JSON-Struktur selbst bleibt lesbar, diffbar und unabhängig von Kotlin-Typnamen.
 */
object ZahlbereichsGraphJsonCodec {
    const val SCHEMA_VERSION: Int = 1

    fun exportiere(graph: ZahlbereichsGraph): String {
        val knoten = graph.knoten()
            .sortedBy { it.id.wert }
            .map(::knotenDatensatz)
        val relationen = graph.relationen()
            .sortedWith(
                compareBy<BereichsRelation>(
                    { it.quelle.wert },
                    { it.ziel.wert },
                    { it.art.name },
                    { it.adapterId.orEmpty() },
                ),
            )
            .map(::relationsDatensatz)

        return buildString {
            append('{')
            append("\"schemaVersion\":")
            append(SCHEMA_VERSION)
            append(",\"nodes\":")
            appendJsonStringArray(knoten)
            append(",\"relations\":")
            appendJsonStringArray(relationen)
            append('}')
        }
    }

    fun importiere(json: String): ZahlbereichsGraph {
        val version = Regex("\\\"schemaVersion\\\"\\s*:\\s*(\\d+)")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: throw IllegalArgumentException("Dem Zahlbereichsgraphen fehlt eine gültige schemaVersion.")
        require(version == SCHEMA_VERSION) {
            "Nicht unterstützte Zahlbereichsgraph-Version $version; erwartet wird $SCHEMA_VERSION."
        }

        val knoten = liesStringArray(json, "nodes").map(::parseKnotenDatensatz)
        val relationen = liesStringArray(json, "relations").map(::parseRelationsDatensatz)
        return ZahlbereichsGraph(knoten, relationen)
    }

    private fun knotenDatensatz(knoten: ZahlbereichsKnoten): String = listOf(
        kodiere(knoten.id.wert),
        kodiere(knoten.latex),
        kodiere(knoten.name),
    ).joinToString("|")

    private fun relationsDatensatz(relation: BereichsRelation): String = buildList {
        add(kodiere(relation.quelle.wert))
        add(kodiere(relation.ziel.wert))
        add(relation.art.name)
        add(if (relation.verlustfrei) "1" else "0")
        add(if (relation.kanonisch) "1" else "0")
        add(relation.adapterId?.let(::kodiere).orEmpty())
        add(relation.voraussetzungen.size.toString())
        addAll(relation.voraussetzungen.sorted().map(::kodiere))
    }.joinToString("|")

    private fun parseKnotenDatensatz(datensatz: String): ZahlbereichsKnoten {
        val felder = datensatz.split('|')
        require(felder.size == 3) { "Ungültiger Zahlbereichsknoten-Datensatz." }
        return ZahlbereichsKnoten(
            id = ZahlbereichsId(dekodiere(felder[0])),
            latex = dekodiere(felder[1]),
            name = dekodiere(felder[2]),
        )
    }

    private fun parseRelationsDatensatz(datensatz: String): BereichsRelation {
        val felder = datensatz.split('|')
        require(felder.size >= 7) { "Ungültiger Zahlbereichsrelations-Datensatz." }
        val anzahlVoraussetzungen = felder[6].toIntOrNull()
            ?: throw IllegalArgumentException("Ungültige Anzahl von Voraussetzungen.")
        require(anzahlVoraussetzungen >= 0 && felder.size == 7 + anzahlVoraussetzungen) {
            "Die Zahl der gespeicherten Voraussetzungen ist inkonsistent."
        }
        return BereichsRelation(
            quelle = ZahlbereichsId(dekodiere(felder[0])),
            ziel = ZahlbereichsId(dekodiere(felder[1])),
            art = runCatching { BereichsRelationArt.valueOf(felder[2]) }
                .getOrElse { throw IllegalArgumentException("Unbekannte Bereichsrelation '${felder[2]}'.", it) },
            verlustfrei = parseBooleanFeld(felder[3], "verlustfrei"),
            kanonisch = parseBooleanFeld(felder[4], "kanonisch"),
            adapterId = felder[5].takeIf(String::isNotEmpty)?.let(::dekodiere),
            voraussetzungen = felder.drop(7).mapTo(linkedSetOf(), ::dekodiere),
        )
    }

    private fun parseBooleanFeld(feld: String, name: String): Boolean = when (feld) {
        "1" -> true
        "0" -> false
        else -> throw IllegalArgumentException("Ungültiges Boolesches Feld '$name'.")
    }

    private fun StringBuilder.appendJsonStringArray(werte: List<String>) {
        append('[')
        werte.forEachIndexed { index, wert ->
            if (index > 0) append(',')
            append('"')
            append(wert)
            append('"')
        }
        append(']')
    }

    private fun liesStringArray(json: String, schluessel: String): List<String> {
        val marker = "\"$schluessel\""
        val markerPosition = json.indexOf(marker)
        require(markerPosition >= 0) { "JSON-Feld '$schluessel' fehlt." }
        val start = json.indexOf('[', markerPosition + marker.length)
        require(start >= 0) { "JSON-Feld '$schluessel' besitzt kein Array." }
        val ende = json.indexOf(']', start + 1)
        require(ende >= 0) { "JSON-Array '$schluessel' ist nicht geschlossen." }
        val inhalt = json.substring(start + 1, ende).trim()
        if (inhalt.isEmpty()) return emptyList()

        return inhalt.split(',').map { eintrag ->
            val bereinigt = eintrag.trim()
            require(bereinigt.length >= 2 && bereinigt.first() == '"' && bereinigt.last() == '"') {
                "JSON-Array '$schluessel' enthält einen ungültigen Eintrag."
            }
            bereinigt.substring(1, bereinigt.lastIndex)
        }
    }

    private fun kodiere(wert: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(wert.toByteArray(StandardCharsets.UTF_8))

    private fun dekodiere(wert: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(wert), StandardCharsets.UTF_8)
    }.getOrElse { throw IllegalArgumentException("Ungültiges Base64-Feld im Zahlbereichsgraphen.", it) }
}
