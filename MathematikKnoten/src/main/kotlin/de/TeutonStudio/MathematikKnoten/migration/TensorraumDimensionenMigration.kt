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
