package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TUPEL_AUFLÖSEN_ART
import de.TeutonStudio.MathematikKnoten.anschlussArtFürMathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel

internal const val TUPEL_AUFLÖSEN_ANZAHL = "tupelAuflösen.anzahl"

/**
 * Synchronisiert den letzten erfolgreich bekannten Tupelvertrag mit den
 * dynamischen Ausgängen. Bei einer fehlgeschlagenen/fehlenden Tupelauswertung
 * bleibt der bestehende Vertrag unverändert.
 */
internal fun synchronisiereTupelAuflöser(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten {
    val idErsetzungen = buildMap {
        karte.knoten.filter { it.art == TUPEL_AUFLÖSEN_ART }.forEach { knoten ->
            knoten.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Ausgang }
                .sortedBy { it.reihenfolge }
                .forEachIndexed { index, anschluss ->
                    val alt = AnschlussVerweis(knoten.id, anschluss.id)
                    val neu = AnschlussVerweis(knoten.id, elementId(knoten.id, index))
                    if (alt != neu) put(alt, neu)
                }
        }
    }

    val synchronisierteKnoten = karte.knoten.map { knoten ->
        if (knoten.art != TUPEL_AUFLÖSEN_ART) return@map knoten
        val tupel = auswertung.knoten[knoten.id]
            ?.eingänge
            ?.get("tupel")
            ?.objekt as? Tupel
            ?: return@map knoten
        synchronisiereTupelAuflöserKnoten(knoten, tupel)
    }

    var ergebnis = karte.copy(
        knoten = synchronisierteKnoten,
        verbindungen = karte.verbindungen.map { verbindung ->
            verbindung.copy(
                von = idErsetzungen[verbindung.von] ?: verbindung.von,
                zu = idErsetzungen[verbindung.zu] ?: verbindung.zu,
            )
        },
    )

    val vorhandeneAnschlüsse = ergebnis.knoten.flatMap { knoten ->
        knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) }
    }.toSet()
    ergebnis = ergebnis.copy(verbindungen = ergebnis.verbindungen.filter {
        it.von in vorhandeneAnschlüsse && it.zu in vorhandeneAnschlüsse
    })

    val gültigeVerbindungen = ergebnis.verbindungen.filter { verbindung ->
        val ohneAktuelle = ergebnis.copy(
            verbindungen = ergebnis.verbindungen.filterNot { it.id == verbindung.id },
        )
        prüfung.prüfe(ohneAktuelle, verbindung.von, verbindung.zu) is VerbindungsPrüfung.Erlaubt
    }
    return ergebnis.copy(verbindungen = gültigeVerbindungen)
}

private fun synchronisiereTupelAuflöserKnoten(knoten: KnotenDaten, tupel: Tupel): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "tupel"
    } ?: return knoten
    val bisherigeAusgänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }

    val ausgänge = tupel.elemente.mapIndexed { index, element ->
        (bisherigeAusgänge.getOrNull(index) ?: AnschlussDaten(
            id = elementId(knoten.id, index),
            name = "element-${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
        )).copy(
            id = elementId(knoten.id, index),
            name = "element-${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
            reihenfolge = index,
            kannSichErweitern = false,
            dynamischErzeugt = true,
        )
    }

    val mindestHöhe = maxOf(115f, 78f + 28f * ausgänge.size)
    return knoten.copy(
        anschlüsse = listOf(eingang.copy(reihenfolge = 0)) + ausgänge,
        größe = knoten.größe.copy(höhe = maxOf(knoten.größe.höhe, mindestHöhe)),
        parameter = knoten.parameter + (TUPEL_AUFLÖSEN_ANZAHL to ausgänge.size.toString()),
    )
}

private fun elementId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:tupelAuflösen:element:${index + 1}")
