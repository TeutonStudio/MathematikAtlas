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

internal fun methodenAusgangErgebnisprojektionSchlüssel(anschlussName: String): String =
    "$METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX$anschlussName.ergebnisprojektion"

internal fun KnotenDaten.methodenAusgangArgumentprojektion(anschlussName: String): String =
    parameter[methodenAusgangArgumentprojektionSchlüssel(anschlussName)]
        ?.takeIf { it == METHODEN_ARGUMENTPROJEKTION_TUPEL }
        ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT

internal fun KnotenDaten.methodenAusgangErgebnisprojektion(anschlussName: String): String =
    parameter[methodenAusgangErgebnisprojektionSchlüssel(anschlussName)]
        ?.takeIf { it == METHODEN_ERGEBNISPROJEKTION_TUPEL }
        ?: METHODEN_ERGEBNISPROJEKTION_DIREKT

/**
 * Synchronisiert alle UI-Projektionen, die von einer konkreten Methodensignatur
 * abhängen. Eine unbekannte Methode erzeugt dabei niemals eine geratene Stelligkeit.
 * Ebenso bleibt ihre Ausgabe über den universellen Tupelvertrag verdrahtbar.
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
                val argumentProjektion = methodenArgumentprojektionFürVerbraucher(karte, knoten)
                val ergebnisProjektion = methodenErgebnisprojektionFürVerbraucher(karte, knoten)
                synchronisiereMethodenAufruf(knoten, methode, argumentProjektion, ergebnisProjektion)
            }
            METHODEN_ARGUMENTE_ART -> {
                val methode = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("methode")
                    ?.objekt as? Methode
                val projektion = methodenArgumentprojektionFürVerbraucher(karte, knoten)
                synchronisiereMethodenArgumente(knoten, methode, projektion)
            }
            METHODEN_ZIELMENGE_ART -> {
                val projektion = methodenErgebnisprojektionFürVerbraucher(karte, knoten)
                knoten.copy(
                    parameter = knoten.parameter +
                        (METHODEN_ZIELMENGE_ERGEBNISPROJEKTION to projektion),
                )
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

private data class MethodenQuellAusgang(
    val knoten: KnotenDaten,
    val anschluss: AnschlussDaten,
)

private fun methodenQuellAusgang(karte: KartenDaten, verbraucher: KnotenDaten): MethodenQuellAusgang? {
    val methodenEingang = verbraucher.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return null
    val eingangsVerweis = AnschlussVerweis(verbraucher.id, methodenEingang.id)
    val verbindung = karte.verbindungen.singleOrNull { it.zu == eingangsVerweis } ?: return null
    val quelle = karte.knoten.firstOrNull { it.id == verbindung.von.knotenId } ?: return null
    val ausgang = quelle.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId } ?: return null
    if (ausgang.richtung != AnschlussRichtung.Ausgang || ausgang.art != MathematikAnschlussArten.Methode.id) {
        return null
    }
    return MethodenQuellAusgang(quelle, ausgang)
}

/**
 * Die Argumentprojektion gehört dem Verbraucher. Der Methodenausgang wird nur noch
 * als Migrationsfallback gelesen, damit bestehende Karten ihr bisheriges Layout behalten.
 */
private fun methodenArgumentprojektionFürVerbraucher(karte: KartenDaten, verbraucher: KnotenDaten): String {
    val lokal = when (verbraucher.art) {
        METHODEN_AUFRUF_ART -> verbraucher.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
        METHODEN_ARGUMENTE_ART -> verbraucher.parameter[METHODEN_ARGUMENTE_PROJEKTION]
        else -> null
    }
    lokal?.takeIf { it == METHODEN_ARGUMENTPROJEKTION_TUPEL || it == METHODEN_ARGUMENTPROJEKTION_SEPARIERT }
        ?.let { return it }

    val quelle = methodenQuellAusgang(karte, verbraucher) ?: return METHODEN_ARGUMENTPROJEKTION_TUPEL
    return quelle.knoten.methodenAusgangArgumentprojektion(quelle.anschluss.name)
}

private fun methodenErgebnisprojektionFürVerbraucher(karte: KartenDaten, verbraucher: KnotenDaten): String {
    val quelle = methodenQuellAusgang(karte, verbraucher) ?: return METHODEN_ERGEBNISPROJEKTION_DIREKT
    return quelle.knoten.methodenAusgangErgebnisprojektion(quelle.anschluss.name)
}

private fun synchronisiereMethodenAufruf(
    knoten: KnotenDaten,
    methode: Methode?,
    argumentProjektion: String,
    ergebnisProjektion: String,
): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val ausgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    } ?: return knoten
    val bisherigeArgumente = knoten.argumentAnschlüsse()
    val signatur = (methode as? SignaturtragendeMethode)?.signatur

    if (methode == null || signatur == null) {
        val argumente = listOf(tupelAnschluss(knoten.id, bisherigeArgumente.firstOrNull()))
        return knoten.copy(
            anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + argumente +
                ausgang.copy(art = MathematikAnschlussArten.Tupel.id),
            parameter = knoten.parameter
                .filterKeys { !it.startsWith(METHODEN_AUFRUF_PARAMETER_PREFIX) }
                .minus(METHODEN_AUFRUF_STELLIGKEIT)
                .minus(METHODEN_AUFRUF_ZIELMENGE)
                .minus(METHODEN_AUFRUF_VERTRAGSFEHLER)
                .plus(METHODEN_ANWENDUNG_ERGEBNIS_ART to MathematikAnschlussArten.Tupel.id.wert)
                .plus(METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL)
                .plus(METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_TUPEL)
                .plus("festeEingänge" to "1"),
        )
    }

    val mathematischeAusgabe = runCatching { methode.einzigeAusgabe().second }.getOrNull()
    val zielMenge = signatur.zielMenge
    val effektiveErgebnisProjektion = if (
        zielMenge is Tupelraum || mathematischeAusgabe is Tupel || ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL
    ) {
        METHODEN_ERGEBNISPROJEKTION_TUPEL
    } else {
        METHODEN_ERGEBNISPROJEKTION_DIREKT
    }
    val roheErgebnisArt = mathematischeAusgabe
        ?.let(::anschlussArtFürMathematischesObjekt)
        ?: anschlussArtFürZielMenge(zielMenge)
    val ergebnisArt = if (effektiveErgebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL) {
        MathematikAnschlussArten.Tupel.id
    } else {
        roheErgebnisArt
    }
    val projizierteZielMenge = if (
        effektiveErgebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL && zielMenge !is Tupelraum
    ) {
        Tupelraum(listOf(zielMenge))
    } else {
        zielMenge
    }
    val argumente = if (argumentProjektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
        if (signatur.argumente.isEmpty()) emptyList()
        else listOf(tupelAnschluss(knoten.id, bisherigeArgumente.firstOrNull()))
    } else {
        signatur.argumente.mapIndexed { index, argument ->
            val parameter = argument.parameter
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
        put(METHODEN_AUFRUF_STELLIGKEIT, signatur.argumente.size.toString())
        put(METHODEN_AUFRUF_ZIELMENGE, projizierteZielMenge.zuLatex())
        put(METHODEN_ANWENDUNG_ERGEBNIS_ART, ergebnisArt.wert)
        put(METHODEN_AUFRUF_ARGUMENTPROJEKTION, argumentProjektion)
        put(METHODEN_AUFRUF_ERGEBNISPROJEKTION, effektiveErgebnisProjektion)
        put("festeEingänge", argumente.size.toString())
        signatur.argumente.forEachIndexed { index, argument ->
            val präfix = "$METHODEN_AUFRUF_PARAMETER_PREFIX$index."
            put("${präfix}name", argument.parameter.name)
            put("${präfix}art", anschlussArtFürParameter(argument.parameter).wert)
            put("${präfix}werteVorrat", argument.werteVorrat.zuLatex())
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

private fun anschlussArtFürZielMenge(zielMenge: MengenAusdruck): AnschlussArtId = when {
    zielMenge == WahrheitsMenge -> MathematikAnschlussArten.Aussage.id
    zielMenge is Tupelraum -> MathematikAnschlussArten.Tupel.id
    zielMenge is Vektorraum && zielMenge.orientierung == VektorOrientierung.Spalte -> MathematikAnschlussArten.SpaltenVektor.id
    zielMenge is Vektorraum && zielMenge.orientierung == VektorOrientierung.Zeile -> MathematikAnschlussArten.ZeilenVektor.id
    zielMenge is Matrizenraum -> MathematikAnschlussArten.Matrix.id
    zielMenge is Tensorraum -> MathematikAnschlussArten.Tensor.id
    zielMenge.istZahlenmenge() -> MathematikAnschlussArten.Zahl.id
    else -> MathematikAnschlussArten.Objekt.id
}

private fun synchronisiereMethodenArgumente(
    knoten: KnotenDaten,
    methode: Methode?,
    konfigurierteProjektion: String,
): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val projektion = if (methode !is SignaturtragendeMethode) METHODEN_ARGUMENTPROJEKTION_TUPEL else konfigurierteProjektion
    val bisherigeAusgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

    val ausgänge = if (projektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
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
        val signatur = (methode as SignaturtragendeMethode).signatur
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
        parameter = knoten.parameter + (METHODEN_ARGUMENTE_PROJEKTION to projektion),
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
