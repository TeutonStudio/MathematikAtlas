package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.anschlussArtFürMathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal const val METHODEN_AUFRUF_STELLIGKEIT = "methodenAufruf.stelligkeit"
internal const val METHODEN_AUFRUF_ZIELMENGE = "methodenAufruf.zielMenge"
internal const val METHODEN_AUFRUF_VERTRAGSFEHLER = "methodenAufruf.vertragsfehler"
internal const val METHODEN_AUFRUF_PARAMETER_PREFIX = "methodenAufruf.parameter."
internal const val METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX = "methodenAusgang."

internal fun methodenAusgangArgumentprojektionSchlüssel(anschlussName: String): String =
    "$METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX$anschlussName.argumentprojektion"

internal fun KnotenDaten.methodenAusgangArgumentprojektion(anschlussName: String): String =
    parameter[methodenAusgangArgumentprojektionSchlüssel(anschlussName)]
        ?.takeIf { it == METHODEN_ARGUMENTPROJEKTION_TUPEL }
        ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT

/**
 * Synchronisiert alle UI-Projektionen, die von einer konkreten Methodensignatur
 * abhängen. Eine unbekannte Methode erzeugt dabei niemals eine geratene Stelligkeit.
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
        when (knoten.art) {
            METHODEN_AUFRUF_ART -> {
                val methode = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("methode")
                    ?.objekt as? Methode
                val projektion = methodenArgumentprojektionFürAufruf(karte, knoten)
                synchronisiereMethodenAufruf(knoten, methode, projektion)
            }
            METHODEN_ARGUMENTE_ART -> {
                val methode = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("methode")
                    ?.objekt as? Methode
                synchronisiereMethodenArgumente(knoten, methode)
            }
            else -> knoten
        }
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

    val gültigeVerbindungen = ergebnis.verbindungen.filter { verbindung ->
        val ohneAktuelle = ergebnis.copy(
            verbindungen = ergebnis.verbindungen.filterNot { it.id == verbindung.id },
        )
        prüfung.prüfe(ohneAktuelle, verbindung.von, verbindung.zu) is VerbindungsPrüfung.Erlaubt
    }
    return synchronisiereTupelAuflöser(
        ergebnis.copy(verbindungen = gültigeVerbindungen),
        auswertung,
        prüfung,
    )
}

private fun methodenArgumentprojektionFürAufruf(karte: KartenDaten, aufruf: KnotenDaten): String {
    val methodenEingang = aufruf.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    val eingangsVerweis = AnschlussVerweis(aufruf.id, methodenEingang.id)
    val verbindung = karte.verbindungen.singleOrNull { it.zu == eingangsVerweis }
        ?: return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    val quelle = karte.knoten.firstOrNull { it.id == verbindung.von.knotenId }
        ?: return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    val ausgang = quelle.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId }
        ?: return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    if (ausgang.richtung != AnschlussRichtung.Ausgang || ausgang.art != MathematikAnschlussArten.Methode.id) {
        return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    }
    return quelle.methodenAusgangArgumentprojektion(ausgang.name)
}

private fun synchronisiereMethodenAufruf(
    knoten: KnotenDaten,
    methode: Methode?,
    projektion: String,
): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val ausgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    } ?: return knoten
    val bisherigeArgumente = knoten.argumentAnschlüsse()

    if (methode == null) {
        val argumente = listOf(tupelAnschluss(knoten.id, bisherigeArgumente.firstOrNull()))
        return knoten.copy(
            anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + argumente +
                ausgang.copy(art = MathematikAnschlussArten.Objekt.id),
            parameter = knoten.parameter
                .filterKeys { !it.startsWith(METHODEN_AUFRUF_PARAMETER_PREFIX) }
                .minus(METHODEN_AUFRUF_STELLIGKEIT)
                .minus(METHODEN_AUFRUF_ZIELMENGE)
                .minus(METHODEN_AUFRUF_VERTRAGSFEHLER)
                .plus(METHODEN_ANWENDUNG_ERGEBNIS_ART to MathematikAnschlussArten.Objekt.id.wert)
                .plus(METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL)
                .plus("festeEingänge" to "1"),
        )
    }

    val ausgabe = runCatching { methode.einzigeAusgabe().second }.getOrElse { fehler ->
        return knoten.copy(parameter = knoten.parameter + (
            METHODEN_AUFRUF_VERTRAGSFEHLER to (fehler.message ?: "Die Methode benötigt genau eine Ausgabe.")
        ))
    }
    val zielMenge = runCatching { methode.methodenSignatur().zielMenge }.getOrElse { fehler ->
        return knoten.copy(parameter = knoten.parameter + (
            METHODEN_AUFRUF_VERTRAGSFEHLER to (fehler.message ?: "Die Zielmenge der Methode fehlt.")
        ))
    }
    val ergebnisArt = anschlussArtFürMathematischesObjekt(ausgabe)
    val argumente = if (projektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
        if (methode.parameter.isEmpty()) emptyList()
        else listOf(tupelAnschluss(knoten.id, bisherigeArgumente.firstOrNull()))
    } else {
        methode.parameter.mapIndexed { index, parameter ->
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
    }
    val vertragsParameter = buildMap {
        put(METHODEN_AUFRUF_STELLIGKEIT, methode.parameter.size.toString())
        put(METHODEN_AUFRUF_ZIELMENGE, zielMenge.zuLatex())
        put(METHODEN_ANWENDUNG_ERGEBNIS_ART, ergebnisArt.wert)
        put(METHODEN_AUFRUF_ARGUMENTPROJEKTION, projektion)
        put("festeEingänge", argumente.size.toString())
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

private fun synchronisiereMethodenArgumente(knoten: KnotenDaten, methode: Methode?): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val projektion = knoten.parameter[METHODEN_ARGUMENTE_PROJEKTION]
        ?: METHODEN_ARGUMENTPROJEKTION_TUPEL
    val bisherigeAusgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

    val ausgänge = if (methode == null || projektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
        val bisher = bisherigeAusgänge.firstOrNull { it.name == "argumente" }
        listOf(
            (bisher ?: AnschlussDaten(
                id = methodenArgumentTupelId(knoten.id),
                name = "argumente",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            )).copy(
                id = methodenArgumentTupelId(knoten.id),
                name = "argumente",
                art = MathematikAnschlussArten.Tupel.id,
                reihenfolge = 0,
                kannSichErweitern = false,
                dynamischErzeugt = false,
            ),
        )
    } else {
        val signatur = runCatching { methode.methodenSignatur() }.getOrNull() ?: return knoten
        buildList {
            signatur.argumente.forEachIndexed { index, argument ->
                val name = methodenArgumentAusgangName(argument, index)
                val bisher = bisherigeAusgänge.firstOrNull { it.id == methodenArgumentId(knoten.id, index) }
                    ?: bisherigeAusgänge.firstOrNull { it.name == name }
                add(
                    (bisher ?: AnschlussDaten(
                        id = methodenArgumentId(knoten.id, index),
                        name = name,
                        richtung = AnschlussRichtung.Ausgang,
                        kante = AnschlussKante.Rechts,
                        art = MathematikAnschlussArten.Objekt.id,
                    )).copy(
                        id = methodenArgumentId(knoten.id, index),
                        name = name,
                        art = MathematikAnschlussArten.Objekt.id,
                        reihenfolge = index,
                        kannSichErweitern = false,
                        dynamischErzeugt = false,
                    ),
                )
            }
            val bisherDimension = bisherigeAusgänge.firstOrNull { it.name == "dimension" }
            add(
                (bisherDimension ?: AnschlussDaten(
                    id = methodenArgumentDimensionId(knoten.id),
                    name = "dimension",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                )).copy(
                    id = methodenArgumentDimensionId(knoten.id),
                    name = "dimension",
                    art = MathematikAnschlussArten.Zahl.id,
                    reihenfolge = signatur.argumente.size,
                    kannSichErweitern = false,
                    dynamischErzeugt = false,
                ),
            )
        }
    }

    return knoten.copy(
        anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + ausgänge,
    )
}

private fun tupelAnschluss(knotenId: KnotenId, bisher: AnschlussDaten?): AnschlussDaten =
    (bisher ?: AnschlussDaten(
        id = argumentId(knotenId, 0),
        name = "argument-0",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Tupel.id,
    )).copy(
        id = argumentId(knotenId, 0),
        name = "argument-0",
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = 1,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )

private fun KnotenDaten.argumentAnschlüsse() = anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
    .sortedBy { it.reihenfolge }

private fun argumentId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:methodenAufruf:argument:$index")

private fun methodenArgumentTupelId(knotenId: KnotenId) =
    AnschlussId("${knotenId.wert}:methodenArgumente:tupel")

private fun methodenArgumentId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:methodenArgumente:argument:$index")

private fun methodenArgumentDimensionId(knotenId: KnotenId) =
    AnschlussId("${knotenId.wert}:methodenArgumente:dimension")

private fun anschlussArtFürParameter(parameter: MethodenParameter): AnschlussArtId = when (parameter) {
    is Variable -> MathematikAnschlussArten.Zahl.id
    is AussagenParameter -> MathematikAnschlussArten.Aussage.id
    is MengenParameter -> MathematikAnschlussArten.Menge.id
    is TypisiertesElement -> AnschlussArtId(parameter.anschlussArt)
    is AllgemeinerParameter -> MathematikAnschlussArten.Objekt.id
}
