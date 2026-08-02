package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

private val alteTranspositionsArten = setOf(
    "mathematik.transponiereSpalte",
    "mathematik.transponiereZeile",
    "mathematik.transponiereMatrix",
)

/** Vereinigt historische Transpositionsknoten ohne Änderung ihrer Anschluss-IDs oder Edges. */
internal fun migriereTranspositionsKnoten(karte: KartenDaten): KartenDaten {
    if (karte.knoten.none { it.art in alteTranspositionsArten }) return karte
    val standard = MathematikKnotenVorlagen.Transponieren.erzeuge(GraphPunkt.Zero)
    val standardEingang = standard.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
    val standardAusgang = standard.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

    return karte.copy(knoten = karte.knoten.map { alt ->
        if (alt.art !in alteTranspositionsArten) return@map alt
        val alterEingang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
        val alterAusgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        alt.copy(
            art = "mathematik.transponieren",
            name = "Transponieren",
            anschlüsse = listOf(
                (alterEingang ?: standardEingang).copy(
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = standardEingang.art,
                    reihenfolge = 0,
                    kannSichErweitern = false,
                    dynamischErzeugt = false,
                    artFolgtEingang = null,
                    artVereinigtEingänge = emptyList(),
                    zulässigeArten = standardEingang.zulässigeArten,
                    artAbbildungVonEingang = null,
                ),
                (alterAusgang ?: standardAusgang).copy(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = standardAusgang.art,
                    reihenfolge = 0,
                    kannSichErweitern = false,
                    dynamischErzeugt = false,
                    artFolgtEingang = null,
                    artVereinigtEingänge = emptyList(),
                    zulässigeArten = emptySet(),
                    artAbbildungVonEingang = standardAusgang.artAbbildungVonEingang,
                ),
            ),
            parameter = alt.parameter + ("achsenPermutation" to (alt.parameter["achsenPermutation"] ?: "1,0")),
        )
    })
}
