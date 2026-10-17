package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.abflachenEineEbene

internal val übersetzungsKonsolidierungAltarten = setOf("mathematik.tupelZuSpalte", "mathematik.tupelZuZeile",
    "mathematik.spalteZuTupel", "mathematik.zeileZuTupel", TUPEL_ABFLACHEN_ART)

/** Die gespeicherte Richtung entscheidet auch bei gleichnamigen Ein-/Ausgängen. */
fun KartenDaten.migriereÜbersetzungsKonsolidierung(): KartenDaten {
    val rollen = mutableMapOf<KnotenId, Map<String,String>>()
    val neu = knoten.map { alt ->
        if (alt.art !in übersetzungsKonsolidierungAltarten || RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@map alt
        runCatching {
            val abflachen = alt.art == TUPEL_ABFLACHEN_ART
            val zuVektor = alt.art in setOf("mathematik.tupelZuSpalte", "mathematik.tupelZuZeile")
            val typOrientierung = if (alt.art.endsWith("Zeile") || alt.art == "mathematik.zeileZuTupel") VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
            val orientierung = if (zuVektor) alt.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: typOrientierung else typOrientierung
            if (zuVektor) require(orientierung in setOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE)) { "Unbekannte Vektororientierung '$orientierung'." }
            if (abflachen) abflachenEineEbene(alt.parameter)
            val ziel = if (abflachen) "abflachen" else if (zuVektor) orientierung else "tupel"
            val eingangsRollen = mapOf((if (abflachen || zuVektor) "tupel" else "vektor") to "wert")
            val ausgangsRollen = mapOf((if (zuVektor) "vektor" else "tupel") to "ergebnis")
            val eingang = alt.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Eingang }
                ?: error("Es muss genau einen Eingang geben.")
            val ausgang = alt.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Ausgang }
                ?: error("Es muss genau einen Ausgang geben.")
            require(alt.anschlüsse.size == 2 && eingang.name in eingangsRollen && ausgang.name in ausgangsRollen &&
                alt.anschlüsse.map { it.id }.distinct().size == 2 && knoten.count { it.id == alt.id } == 1 &&
                alt.anschlüsse.none { it.belegung != AnschlussBelegung.Einzeln || it.kannSichErweitern || it.sammelAnschluss != null } &&
                verbindungen.count { it.zu == AnschlussVerweis(alt.id,eingang.id) } <= 1) {
                "Die Anschlussrollen, Identitäten oder Einzelbelegungen sind mehrdeutig."
            }
            val register = AnschlussArtRegister(MathematikAnschlussArten.alle + GeometrieAnschlussArten.alle)
            fun kompatibel(p:AnschlussDaten, art:AnschlussArtId) =
                (setOf(p.art) + p.zulässigeArten).all { register.istUnterart(it,art) }
            val vektorArt = if (orientierung == VEKTOR_ORIENTIERUNG_ZEILE) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id
            require(kompatibel(eingang, if (abflachen || zuVektor) MathematikAnschlussArten.Tupel.id else vektorArt) &&
                kompatibel(ausgang, if (zuVektor) vektorArt else MathematikAnschlussArten.Tupel.id)) {
                "Die gespeicherten Anschlussarten widersprechen dem Übersetzungsziel."
            }
            val umbenannt = alt.copy(art = ÜBERSETZER_ART,
                eigenschaften = alt.eigenschaften + (ÜBERSETZER_ZIEL to KnotenEigenschaft.Text(ziel)),
                anschlüsse = alt.anschlüsse.map { p -> p.remappeStrukturRollen(eingangsRollen).copy(
                    name = (if (p.richtung == AnschlussRichtung.Eingang) eingangsRollen else ausgangsRollen).getValue(p.name)) },
                parameter = remappeStrukturParameter(alt.parameter,eingangsRollen,ausgangsRollen),
                eingangsKartenVerweise = remappeEindeutig(alt.eingangsKartenVerweise) { remappeFallbackRolle(it,eingangsRollen) })
            val konfiguriert = konfiguriereÜbersetzer(umbenannt,ziel)
            require(konfiguriert.anschlüsse.map { it.id }.toSet() == alt.anschlüsse.map { it.id }.toSet()) { "Die Anschlussidentitäten sind nicht vollständig erhaltbar." }
            rollen[alt.id] = eingangsRollen
            konfiguriert.copy(name = alt.name)
        }.getOrElse { fehler -> alt.copy(parameter = alt.parameter +
            (RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: ${fehler.message}")) }
    }
    return copy(knoten = neu, verbindungen = verbindungen.map { v ->
        val map = rollen[v.zu.knotenId]
        if (map == null || v.eingangsFallback == null) v else v.copy(eingangsFallback = remappeFallbackRolle(requireNotNull(v.eingangsFallback),map))
    })
}
