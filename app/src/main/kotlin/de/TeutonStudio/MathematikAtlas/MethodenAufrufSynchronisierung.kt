package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_ANWENDUNG_ERGEBNIS_ART
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_AUFRUF_ART
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal const val METHODEN_AUFRUF_STELLIGKEIT = "methodenAufruf.stelligkeit"
internal const val METHODEN_AUFRUF_ZIELMENGE = "methodenAufruf.zielMenge"
internal const val METHODEN_AUFRUF_VERTRAGSFEHLER = "methodenAufruf.vertragsfehler"
internal const val METHODEN_AUFRUF_PARAMETER_PREFIX = "methodenAufruf.parameter."

/**
 * Gleicht die persistierte Anschlussstruktur eines allgemeinen Methodenaufrufs mit dem
 * ausgewerteten Methodenvertrag ab. Argument-IDs werden deterministisch aus Knoten-ID
 * und Index gebildet; bestehende Kanten werden beim ersten Abgleich entsprechend migriert.
 */
internal fun synchronisiereMethodenAufrufe(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten {
    val argumentIdErsetzungen = buildMap {
        karte.knoten.filter { it.art == METHODEN_AUFRUF_ART }.forEach { knoten ->
            knoten.argumentAnschlüsse().forEachIndexed { index, anschluss ->
                val alt = AnschlussVerweis(knoten.id, anschluss.id)
                val neu = AnschlussVerweis(knoten.id, argumentId(knoten.id, index))
                if (alt != neu) put(alt, neu)
            }
        }
    }
    val synchronisierteKnoten = karte.knoten.map { knoten ->
        if (knoten.art != METHODEN_AUFRUF_ART) return@map knoten
        val methode = auswertung.knoten[knoten.id]
            ?.eingänge
            ?.get("methode")
            ?.objekt as? Funktion
        synchronisiereMethodenAufruf(knoten, methode)
    }
    var ergebnis = karte.copy(
        knoten = synchronisierteKnoten,
        verbindungen = karte.verbindungen.map { verbindung ->
            verbindung.copy(
                von = argumentIdErsetzungen[verbindung.von] ?: verbindung.von,
                zu = argumentIdErsetzungen[verbindung.zu] ?: verbindung.zu,
            )
        },
    )
    val vorhandeneAnschlüsse = ergebnis.knoten.flatMap { knoten ->
        knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) }
    }.toSet()
    ergebnis = ergebnis.copy(verbindungen = ergebnis.verbindungen.filter {
        it.von in vorhandeneAnschlüsse && it.zu in vorhandeneAnschlüsse
    })

    // Eine Vertragsänderung kann bestehende Argument- oder Ausgangskanten typwidrig machen.
    // Solche Kanten werden kontrolliert entfernt, statt den gesamten Graphen inkonsistent zu lassen.
    val gültigeVerbindungen = ergebnis.verbindungen.filter { verbindung ->
        val ohneAktuelle = ergebnis.copy(
            verbindungen = ergebnis.verbindungen.filterNot { it.id == verbindung.id },
        )
        prüfung.prüfe(ohneAktuelle, verbindung.von, verbindung.zu) is VerbindungsPrüfung.Erlaubt
    }
    return ergebnis.copy(verbindungen = gültigeVerbindungen)
}

private fun synchronisiereMethodenAufruf(knoten: KnotenDaten, methode: Funktion?): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val ausgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    } ?: return knoten
    val bisherigeArgumente = knoten.argumentAnschlüsse()

    if (methode == null) {
        val anzahl = maxOf(2, bisherigeArgumente.size)
        val argumente = List(anzahl) { index ->
            val bisher = bisherigeArgumente.getOrNull(index)
            (bisher ?: AnschlussDaten(
                id = argumentId(knoten.id, index),
                name = "argument-$index",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Objekt.id,
            )).copy(
                id = argumentId(knoten.id, index),
                name = "argument-$index",
                art = MathematikAnschlussArten.Objekt.id,
                reihenfolge = index + 1,
                kannSichErweitern = true,
                dynamischErzeugt = index >= 2,
            )
        }
        return knoten.copy(
            anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + argumente +
                ausgang.copy(art = MathematikAnschlussArten.Objekt.id),
            parameter = knoten.parameter
                .filterKeys { !it.startsWith(METHODEN_AUFRUF_PARAMETER_PREFIX) }
                .minus(METHODEN_AUFRUF_STELLIGKEIT)
                .minus(METHODEN_AUFRUF_ZIELMENGE)
                .minus(METHODEN_AUFRUF_VERTRAGSFEHLER)
                .plus(METHODEN_ANWENDUNG_ERGEBNIS_ART to MathematikAnschlussArten.Objekt.id.wert)
                .plus("festeEingänge" to "2"),
        )
    }

    val ausgabe = runCatching { methode.einzigeAusgabe().second }.getOrElse { fehler ->
        return knoten.copy(parameter = knoten.parameter + (
            METHODEN_AUFRUF_VERTRAGSFEHLER to (fehler.message ?: "Die Methode benötigt genau eine Ausgabe.")
        ))
    }
    val zielMenge = runCatching { methode.einzigeZielMenge }.getOrElse { fehler ->
        return knoten.copy(parameter = knoten.parameter + (
            METHODEN_AUFRUF_VERTRAGSFEHLER to (fehler.message ?: "Die Zielmenge der Methode fehlt.")
        ))
    }
    val ergebnisArt = anschlussArtFürObjekt(ausgabe)
    val argumente = methode.parameter.mapIndexed { index, parameter ->
        val bisher = bisherigeArgumente.getOrNull(index)
        (bisher ?: AnschlussDaten(
            id = argumentId(knoten.id, index),
            name = "argument-$index",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = anschlussArtFürParameter(parameter),
        )).copy(
            id = argumentId(knoten.id, index),
            name = "argument-$index",
            art = anschlussArtFürParameter(parameter),
            reihenfolge = index + 1,
            kannSichErweitern = false,
            dynamischErzeugt = false,
        )
    }
    val vertragsParameter = buildMap {
        put(METHODEN_AUFRUF_STELLIGKEIT, methode.parameter.size.toString())
        put(METHODEN_AUFRUF_ZIELMENGE, zielMenge.zuLatex())
        put(METHODEN_ANWENDUNG_ERGEBNIS_ART, ergebnisArt.wert)
        put("festeEingänge", methode.parameter.size.toString())
        methode.parameter.forEachIndexed { index, parameter ->
            val präfix = "$METHODEN_AUFRUF_PARAMETER_PREFIX$index."
            put("${präfix}name", parameter.name)
            put("${präfix}art", anschlussArtFürParameter(parameter).wert)
            methode.werteVorräte[parameter.name]?.let { put("${präfix}werteVorrat", it.zuLatex()) }
        }
    }
    return knoten.copy(
        anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + argumente +
            ausgang.copy(art = ergebnisArt),
        parameter = knoten.parameter
            .filterKeys { !it.startsWith(METHODEN_AUFRUF_PARAMETER_PREFIX) }
            .minus(METHODEN_AUFRUF_STELLIGKEIT)
            .minus(METHODEN_AUFRUF_ZIELMENGE)
            .minus(METHODEN_AUFRUF_VERTRAGSFEHLER) + vertragsParameter,
    )
}

private fun KnotenDaten.argumentAnschlüsse() = anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
    .sortedBy { it.reihenfolge }

private fun argumentId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:methodenAufruf:argument:$index")

private fun anschlussArtFürParameter(parameter: FunktionsParameter): AnschlussArtId = when (parameter) {
    is Variable -> MathematikAnschlussArten.Zahl.id
    is AussagenParameter -> MathematikAnschlussArten.Aussage.id
    is MengenParameter -> MathematikAnschlussArten.Menge.id
    is TypisiertesElement -> AnschlussArtId(parameter.anschlussArt)
    is AllgemeinerParameter -> MathematikAnschlussArten.Objekt.id
}

private fun anschlussArtFürObjekt(objekt: MathematischesObjekt): AnschlussArtId = when (objekt) {
    is ZahlAusdruck -> MathematikAnschlussArten.Zahl.id
    is Aussage -> MathematikAnschlussArten.Aussage.id
    is MengenAusdruck -> MathematikAnschlussArten.Menge.id
    is SpaltenVektor -> MathematikAnschlussArten.SpaltenVektor.id
    is ZeilenVektor -> MathematikAnschlussArten.ZeilenVektor.id
    is Matrix -> MathematikAnschlussArten.Matrix.id
    is Tupel -> MathematikAnschlussArten.Tupel.id
    is Funktion -> MathematikAnschlussArten.Funktion.id
    is TypisiertesElement -> AnschlussArtId(objekt.anschlussArt)
    else -> MathematikAnschlussArten.Objekt.id
}
