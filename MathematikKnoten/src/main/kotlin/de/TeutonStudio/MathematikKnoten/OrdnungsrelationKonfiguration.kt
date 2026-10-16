package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

/** Relation und ein noch unveränderter Vorlagenname bilden einen Konfigurationswechsel. */
fun konfiguriereOrdnungsrelation(knoten: KnotenDaten, relation: String): KnotenDaten {
    require(knoten.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART)
    val namen = mapOf("kleiner" to "Kleiner", "kleinerGleich" to "Kleiner oder gleich",
        "größer" to "Größer", "größerGleich" to "Größer oder gleich")
    val name = requireNotNull(namen[relation]) { "Unbekannte Ordnungsrelation '$relation'." }
    return knoten.copy(parameter = knoten.parameter + ("relation" to relation),
        name = if (knoten.name in namen.values) name else knoten.name)
}
