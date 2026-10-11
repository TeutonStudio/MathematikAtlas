package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

/** Fachliche Freigabegrenze, unabhängig vom Android-Teilen-Adapter. */
internal class KartenVeröffentlichungsDienst(private val speicher: KartenSpeicher) {
    fun karte(karte: KartenDaten, ordnung: KartenOrdnung): String =
        erstelle(karte.name, FreigabeArt.Karte, listOf(karte), ordnung)

    fun sammlung(pfad: List<String>, karten: List<KartenDaten>, ordnung: KartenOrdnung): String =
        erstelle(pfad.last(), FreigabeArt.Sammlung, karten, ordnung, pfad)

    private fun erstelle(name: String, art: FreigabeArt, karten: List<KartenDaten>, ordnung: KartenOrdnung, pfad: List<String>? = null): String {
        require(karten.isNotEmpty()) { "Die Veröffentlichung enthält keine eigenen Karten." }
        require(karten.all { speicher.herkunftFür(it.id) == KartenHerkunft.EIGEN }) {
            "Nur eigene Karten dürfen veröffentlicht werden."
        }
        return speicher.erstelleFreigabePaket(name, art, karten, ordnung, sammlungsPfad = pfad)
    }
}
