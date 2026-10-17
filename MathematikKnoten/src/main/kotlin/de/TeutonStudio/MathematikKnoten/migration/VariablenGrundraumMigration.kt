package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val VARIABLE_GRUNDRAUM_VERTRAG = "variable.grundraumVertrag"

/** Alte implizite Bereiche werden einmalig zu expliziten, editierbaren Quellen. */
fun KartenDaten.migriereHistorischeVariablenGrundräume(): KartenDaten {
    val karte = diagnostiziereQuellenBereiche().migriereVariablenGrundraum()
    val neu = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()
    val variablen = karte.knoten.map { k ->
        if (RECHNER_MIGRATION_DIAGNOSE in k.parameter || k.art != "mathematik.variable" || VARIABLE_GRUNDRAUM_VERTRAG in k.eigenschaften) return@map k
        val eingang = k.anschlüsse.single { it.name == "grundraum" && it.richtung == AnschlussRichtung.Eingang }
        if (karte.verbindungen.none { it.zu == AnschlussVerweis(k.id, eingang.id) }) {
            val basis = historischeVariablenBasis(k)
            val raum = historischerVariablenGrundraum(k, basis)
            val id = "${k.id.wert}:migration-grundraum"
            require(karte.knoten.none { it.id.wert == id }) { "Die ID der historischen Grundraumquelle ist bereits belegt: $id" }
            val position = GraphPunkt(k.position.x - 340f, k.position.y)
            val auswahl = MengenKnotenAuswahl.entries.firstOrNull { it.menge() == raum }
            val quelle = if (auswahl != null) MengenKnotenVorlagen.vorlage(auswahl).erzeuge(position) else {
                val formel = GebundeneFormel(FormelAusdruck.Literal("$id:literal", raum, FormelTyp.MENGE), emptyList(), emptyMap())
                konfiguriereGebundeneKnotenFormel(ZahlenRechnerKnotenVorlagen.standard.erzeuge(position), formel)
            }
            val stabil = quelle.copy(id = KnotenId(id), name = "Grundraum · ${k.parameter["name"] ?: "x"}",
                anschlüsse = quelle.anschlüsse.map { it.copy(id = AnschlussId("$id:${it.richtung}:${it.name}")) })
            neu += stabil
            val ausgang = stabil.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            verbindungen += VerbindungDaten(id = VerbindungsId("$id:verbindung"),
                von = AnschlussVerweis(stabil.id, ausgang.id), zu = AnschlussVerweis(k.id, eingang.id))
        }
        val erzeugteQuelle = neu.lastOrNull()?.takeIf { it.id.wert == "${k.id.wert}:migration-grundraum" }
        val raum = if (erzeugteQuelle != null) historischerVariablenGrundraum(k, historischeVariablenBasis(k)) else null
        k.copy(eigenschaften = k.eigenschaften + (VARIABLE_GRUNDRAUM_VERTRAG to KnotenEigenschaft.Text("1")),
            anschlüsse = k.anschlüsse.map { p ->
                if (p.richtung == AnschlussRichtung.Ausgang && raum != null) p.copy(art = variablenAnschlussArt(raum.elementTypAusdruck()),
                    vertrag = p.vertrag.copy(typ = raum.elementTypAusdruck())) else p
            })
    }
    return karte.copy(knoten = variablen + neu, verbindungen = karte.verbindungen + verbindungen)
}

internal fun historischeVariablenBasis(k: KnotenDaten): MengenAusdruck {
    val roh = k.parameter["werteVorrat"]?.trim()?.uppercase().orEmpty().ifBlank { "R" }
    val id = mapOf("ℕ" to "N", "ℤ" to "Z", "ℚ" to "Q", "ℝ" to "R", "ℂ" to "C")[roh] ?: roh
    return requireNotNull(MengenKnotenAuswahl.vonIdOderNull(id)) { "Unbekannter historischer Variablenbereich: $roh" }.menge()
}
