package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.VektorStrukturAusgabe

/**
 * Überführt die früher separat erzeugbaren Tupel-Auflöse-/Ergänzknoten in die
 * entsprechenden Modi des kanonischen Vektorrechners. Anschluss-IDs bleiben
 * erhalten, damit bestehende Edges nicht neu verdrahtet werden müssen.
 */
fun KartenDaten.migriereLegacyVektorStrukturKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        when (knoten.art) {
            TUPEL_AUFLÖSEN_ART -> migriereAufloeser(knoten)
            TUPEL_ERGÄNZEN_ART -> migriereZusammenfuehrer(knoten)
            else -> knoten
        }
    },
)

private fun migriereAufloeser(knoten: KnotenDaten): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
    val ausgaenge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
        .mapIndexed { index, anschluss ->
            anschluss.copy(
                name = "element${index + 1}",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                reihenfolge = index,
                kannSichErweitern = false,
                dynamischErzeugt = true,
            )
        }
    return knoten.copy(
        art = VektorRechner.KNOTEN_ART,
        name = "Vektorrechner",
        anschlüsse = listOfNotNull(
            eingang?.copy(
                name = "struktur",
                art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = setOf(
                    MathematikAnschlussArten.Tupel.id,
                    MathematikAnschlussArten.SpaltenVektor.id,
                    MathematikAnschlussArten.ZeilenVektor.id,
                ),
                reihenfolge = 0,
            ),
        ) + ausgaenge,
        parameter = knoten.parameter + mapOf(
            VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.ZERLEGEN.stabileId,
        ),
    )
}

private fun migriereZusammenfuehrer(knoten: KnotenDaten): KnotenDaten {
    val eingange = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
        .mapIndexed { index, anschluss ->
            anschluss.copy(
                name = "element.${index + 1}",
                art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = setOf(
                    MathematikAnschlussArten.Objekt.id,
                    MathematikAnschlussArten.Zahl.id,
                    MathematikAnschlussArten.Tupel.id,
                    MathematikAnschlussArten.SpaltenVektor.id,
                    MathematikAnschlussArten.ZeilenVektor.id,
                ),
                reihenfolge = index,
                kannSichErweitern = index == knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang } - 1,
            )
        }
    val ausgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?.copy(
            name = "struktur",
            art = MathematikAnschlussArten.Objekt.id,
            zulässigeArten = setOf(
                MathematikAnschlussArten.Tupel.id,
                MathematikAnschlussArten.SpaltenVektor.id,
                MathematikAnschlussArten.ZeilenVektor.id,
            ),
            reihenfolge = 0,
        )
    return knoten.copy(
        art = VektorRechner.KNOTEN_ART,
        name = "Vektorrechner",
        anschlüsse = eingange + listOfNotNull(ausgang),
        parameter = knoten.parameter + mapOf(
            VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.ZUSAMMENFUEHREN.stabileId,
            VEKTOR_RECHNER_STRUKTUR_AUSGABE to VektorStrukturAusgabe.TUPEL.stabileId,
        ),
    )
}
