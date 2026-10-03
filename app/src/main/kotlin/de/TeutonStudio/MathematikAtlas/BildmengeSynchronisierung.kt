package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.Methode

/**
 * Synchronisiert die Eingangsprojektion des Bildmengen-Knotens mit der verbundenen
 * Methodensignatur. Der Produktmodus besitzt genau einen Mengeneingang, der
 * Einzelmengenmodus genau einen Mengeneingang je Methodenargument.
 */
internal fun synchronisiereBildmengenAnschlüsse(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
): KartenDaten {
    val synchronisierteKnoten = karte.knoten.map { knoten ->
        if (knoten.art != "mathematik.abbild") return@map knoten

        val methode = auswertung.knoten[knoten.id]
            ?.eingänge
            ?.get("methode")
            ?.objekt as? Methode

        synchronisiereBildmengenKnoten(knoten, methode)
    }

    val vorhandeneAnschlüsse = synchronisierteKnoten.flatMap { knoten ->
        knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) }
    }.toSet()

    return karte.copy(
        knoten = synchronisierteKnoten,
        verbindungen = karte.verbindungen.filter { verbindung ->
            verbindung.von in vorhandeneAnschlüsse && verbindung.zu in vorhandeneAnschlüsse
        },
    )
}

private fun synchronisiereBildmengenKnoten(
    knoten: KnotenDaten,
    methode: Methode?,
): KnotenDaten {
    val bisher = knoten.anschlüsse.associateBy(AnschlussDaten::name)
    val methodenEingang = (bisher["methode"] ?: AnschlussDaten(
        name = "methode",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
    )).copy(
        name = "methode",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )
    val ausgang = (bisher["menge"]?.takeIf { it.richtung == AnschlussRichtung.Ausgang }
        ?: knoten.anschlüsse.firstOrNull {
            it.richtung == AnschlussRichtung.Ausgang && it.art == MathematikAnschlussArten.Menge.id
        }
        ?: AnschlussDaten(
            name = "menge",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Menge.id,
        )).copy(
        name = "menge",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Menge.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )

    val modus = knoten.parameter[BILDMENGE_ARGUMENT_MODUS] ?: BILDMENGE_MODUS_PRODUKT
    if (modus != BILDMENGE_MODUS_EINZELMENGEN || methode == null) {
        val mengenEingang = (bisher["menge"]?.takeIf { it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(
                name = "menge",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
            )).copy(
            name = "menge",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Menge.id,
            reihenfolge = 0,
            kannSichErweitern = false,
            dynamischErzeugt = false,
        )
        return knoten.copy(
            anschlüsse = listOf(
                mengenEingang,
                methodenEingang.copy(reihenfolge = 1),
                ausgang,
            ),
            parameter = if (BILDMENGE_ARGUMENT_MODUS in knoten.parameter) {
                knoten.parameter
            } else {
                knoten.parameter + (BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_PRODUKT)
            },
        )
    }

    val mengenEingänge = methode.parameter.mapIndexed { index, _ ->
        val name = bildmengeArgumentName(index)
        (bisher[name] ?: AnschlussDaten(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Menge.id,
        )).copy(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Menge.id,
            reihenfolge = index + 1,
            kannSichErweitern = false,
            dynamischErzeugt = true,
        )
    }

    return knoten.copy(
        anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + mengenEingänge + ausgang,
        parameter = knoten.parameter + (BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_EINZELMENGEN),
    )
}
