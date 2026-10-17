package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Überführt historische Testaufbauten in Graphen mit expliziten Quellen. */
internal fun KartenDaten.mitTestGrundräumen(): KartenDaten = copy(knoten = knoten.map {
    if (it.art == "mathematik.variable") it.copy(eigenschaften = it.eigenschaften - VARIABLE_GRUNDRAUM_VERTRAG) else it
}).migriereHistorischeVariablenGrundräume().let { neu ->
    val ids = knoten.map { it.id }.toSet()
    MathematikKartenCodec.lade(KartenDatenJson.schreibe(neu.copy(knoten = neu.knoten.filter { it.id !in ids } + neu.knoten.filter { it.id in ids })))
}

internal fun testGrundraum(k: KnotenDaten): Map<String, BedingterWert> = mapOf("grundraum" to BedingterWert(
    historischerVariablenGrundraum(k, MengenKnotenAuswahl.vonId(k.parameter["werteVorrat"]).menge())))
