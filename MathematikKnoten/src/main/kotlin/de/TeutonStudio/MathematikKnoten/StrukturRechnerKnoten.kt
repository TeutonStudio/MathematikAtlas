package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val SKALARPRODUKT_ART = "mathematik.skalarprodukt"
const val TENSORPRODUKT_ART = "mathematik.tensorprodukt"
const val DIMENSIONEN_ART = "mathematik.dimensionen"
const val CAUCHY_ART = "mathematik.cauchy"
const val RECHNER_OPERATOR_PARAMETER = "operator"

object StrukturRechnerKnotenVorlagen {
    private fun eingang(
        name: String,
        art: AnschlussArtId = MathematikAnschlussArten.Objekt.id,
        reihenfolge: Int = 0,
        erweiterbar: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihenfolge,
        kannSichErweitern = erweiterbar,
    )

    private fun ausgang(name: String, art: AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val Skalarprodukt = KnotenVorlage(
        art = SKALARPRODUKT_ART,
        name = "Skalarprodukt",
        kategorie = "Lineare Algebra",
        beschreibung = "Bildet ein Skalarprodukt orientierungsunabhängig aus Vektoren oder kartesischen Tupeln.",
        standardGröße = GraphGröße(260f, 120f),
        anschlüsse = listOf(
            eingang("links", reihenfolge = 0),
            eingang("rechts", reihenfolge = 1),
            ausgang("wert", MathematikAnschlussArten.Zahl.id),
        ),
        standardParameter = mapOf(
            "linearitaet" to SkalarproduktLinearitaet.RECHTSLINEAR.name,
            "konjugiert" to "true",
        ),
    )

    val Tensorprodukt = KnotenVorlage(
        art = TENSORPRODUKT_ART,
        name = "Tensorprodukt",
        kategorie = "Tensoren",
        beschreibung = "Verkettet die Achsen zweier tensorartig betrachteter Objekte in fester Links-rechts-Reihenfolge.",
        standardGröße = GraphGröße(260f, 120f),
        anschlüsse = listOf(
            eingang("links", reihenfolge = 0),
            eingang("rechts", reihenfolge = 1),
            ausgang("wert", MathematikAnschlussArten.Objekt.id),
        ),
    )

    val Dimensionen = KnotenVorlage(
        art = DIMENSIONEN_ART,
        name = "Dimensionen",
        kategorie = "Tensoren",
        beschreibung = "Gibt die geordnete Tensorform und die Stufe eines Zahl-, Tupel-, Vektor-, Matrix- oder Tensorobjekts aus.",
        standardGröße = GraphGröße(280f, 125f),
        anschlüsse = listOf(
            eingang("objekt"),
            ausgang("dimensionen", MathematikAnschlussArten.Tupel.id),
            ausgang("stufe", MathematikAnschlussArten.Zahl.id),
        ),
    )

    val Tensorrechner = KnotenVorlage(
        art = TensorRechner.KNOTEN_ART,
        name = "Tensorrechner",
        kategorie = "Tensoren",
        beschreibung = "Einheitlicher Rechner für komponentenweise Operationen, Tensorprodukt, Achsenoperationen und Kontraktion.",
        standardGröße = GraphGröße(290f, 135f),
        anschlüsse = listOf(
            eingang("links", reihenfolge = 0),
            eingang("rechts", reihenfolge = 1),
            ausgang("wert", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = mapOf(
            RECHNER_OPERATOR_PARAMETER to TensorRechnerOperator.TENSORPRODUKT.stabileId,
            "achsen" to "",
            "indizes" to "",
            "permutation" to "",
        ),
    )

    val Aussagensatz = KnotenVorlage(
        art = AussagenSatzRechner.KNOTEN_ART,
        name = "Aussagensatz",
        kategorie = "Aussagen: Aussagenlogik",
        beschreibung = "Einheitlicher Aussagenrechner für Verknüpfungen, Prädikate und explizit gebundene Quantoren.",
        standardGröße = GraphGröße(300f, 145f),
        anschlüsse = listOf(
            eingang("a", MathematikAnschlussArten.Aussage.id, 0, true),
            eingang("b", MathematikAnschlussArten.Aussage.id, 1, true),
            eingang("bereich", MathematikAnschlussArten.Menge.id, 2),
            ausgang("wert", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = mapOf(
            "festeEingänge" to "2",
            RECHNER_OPERATOR_PARAMETER to AussagenSatzOperator.KONJUNKTION.stabileId,
            "variablenId" to "x",
            "variablenName" to "x",
            "praedikatName" to "P",
        ),
    )

    val Cauchy = KnotenVorlage(
        art = CAUCHY_ART,
        name = "Cauchy",
        kategorie = "Analysis: Nichtstandardanalysis",
        beschreibung = "Prüft ein unnatürliches kartesisches Tupel primär über unendliche Hyperindizes.",
        standardGröße = GraphGröße(270f, 115f),
        anschlüsse = listOf(
            eingang("tupel"),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
    )

    val alle = listOf(Skalarprodukt, Tensorprodukt, Dimensionen, Tensorrechner, Aussagensatz, Cauchy)
}

internal fun MathematikAuswerterRegister.registriereStrukturRechnerKnoten() {
    registriere(SKALARPRODUKT_ART) { kontext ->
        val links = kontext.mathematischerEingang("links", "Linker Skalarproduktoperand")
        val rechts = kontext.mathematischerEingang("rechts", "Rechter Skalarproduktoperand")
        val linearitaet = runCatching {
            SkalarproduktLinearitaet.valueOf(
                kontext.knoten.parameter["linearitaet"] ?: SkalarproduktLinearitaet.RECHTSLINEAR.name,
            )
        }.getOrDefault(SkalarproduktLinearitaet.RECHTSLINEAR)
        val wert = when (
            val ergebnis = standardSkalarprodukt(
                links,
                rechts,
                SkalarproduktSpezifikation(
                    linearitaet = linearitaet,
                    konjugiert = kontext.knoten.parameter["konjugiert"]?.toBooleanStrictOrNull() ?: true,
                ),
            )
        ) {
            is StrukturPruefung.Gueltig -> ergebnis.wert
            is StrukturPruefung.Bedingt -> ergebnis.wert ?: error(ergebnis.bedingungen.joinToString())
            is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
            is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
            eingänge = kontext.eingänge,
        )
    }

    registriere(TENSORPRODUKT_ART) { kontext ->
        val links = kontext.mathematischerEingang("links", "Linker Tensorproduktoperand")
        val rechts = kontext.mathematischerEingang("rechts", "Rechter Tensorproduktoperand")
        val wert = when (val ergebnis = tensorprodukt(links, rechts)) {
            is StrukturPruefung.Gueltig -> ergebnis.wert
            is StrukturPruefung.Bedingt -> ergebnis.wert ?: error(ergebnis.bedingungen.joinToString())
            is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
            is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
            eingänge = kontext.eingänge,
        )
    }

    registriere(DIMENSIONEN_ART) { kontext ->
        val objekt = kontext.mathematischerEingang("objekt", "Dimensionsobjekt")
        val wert = when (val ergebnis = tensorDimensionen(objekt)) {
            is StrukturPruefung.Gueltig -> ergebnis.wert
            is StrukturPruefung.Bedingt -> ergebnis.wert ?: error(ergebnis.bedingungen.joinToString())
            is StrukturPruefung.Ungueltig -> error(ergebnis.grund)
            is StrukturPruefung.Unentscheidbar -> error(ergebnis.grund)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "dimensionen" to BedingterWert(wert.dimensionen, kontext.annahmen()),
                "stufe" to BedingterWert(wert.stufe, kontext.annahmen()),
            ),
            eingänge = kontext.eingänge,
        )
    }

    registriere(TensorRechner.KNOTEN_ART) { kontext ->
        val operator = tensorOperator(kontext.knoten.parameter[RECHNER_OPERATOR_PARAMETER])
        val links = kontext.eingänge["links"]?.mathematischesObjekt("Linker Tensorrechner-Eingang")
        val rechts = kontext.eingänge["rechts"]?.mathematischesObjekt("Rechter Tensorrechner-Eingang")
        val eingaben = when (operator) {
            TensorRechnerOperator.SKALARMULTIPLIKATION -> listOfNotNull(
                links?.let { TensorRechnerEingabe("skalar", it) },
                rechts?.let { TensorRechnerEingabe("tensor", it) },
            )
            TensorRechnerOperator.TENSORPRODUKT -> listOfNotNull(
                links?.let { TensorRechnerEingabe("links", it) },
                rechts?.let { TensorRechnerEingabe("rechts", it) },
            )
            TensorRechnerOperator.ACHSENPERMUTATION,
            TensorRechnerOperator.TRANSPONIEREN,
            TensorRechnerOperator.ACHSENSCHNITT,
            TensorRechnerOperator.INDEXAUSWERTUNG,
            TensorRechnerOperator.KONTRAKTION,
            TensorRechnerOperator.NORM,
            -> listOfNotNull(links?.let { TensorRechnerEingabe("tensor", it) })
            else -> listOfNotNull(
                links?.let { TensorRechnerEingabe("links", it) },
                rechts?.let { TensorRechnerEingabe("rechts", it) },
            )
        }
        val ergebnis = TensorRechner.erzeuge(
            operator,
            eingaben,
            TensorRechnerKonfiguration(
                achsen = parseIntListe(kontext.knoten.parameter["achsen"]),
                indizes = parseIntListe(kontext.knoten.parameter["indizes"]),
                permutation = parseIntListe(kontext.knoten.parameter["permutation"]),
            ),
        )
        val wert = when (ergebnis) {
            is TensorRechnerErgebnis.Wert -> ergebnis.objekt
            is TensorRechnerErgebnis.Bedingt -> error(ergebnis.bedingungen.joinToString())
            is TensorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
            eingänge = kontext.eingänge,
        )
    }

    registriere(AussagenSatzRechner.KNOTEN_ART) { kontext ->
        val operator = aussagenOperator(kontext.knoten.parameter[RECHNER_OPERATOR_PARAMETER])
        val aussagen = kontext.operatorEingänge { _, index ->
            UnentscheidbareAussage("A_$index", "unverbunden")
        }.map { eingang ->
            LogischesAtom(eingang.objekt as? Aussage ?: error("Aussageneingang ist ungültig."))
        }
        val bereich = kontext.eingänge["bereich"]?.objekt as? MengenAusdruck ?: NatürlicheZahlen
        val variable = LogischeVariable(
            kontext.knoten.parameter["variablenId"].orEmpty().ifBlank { "x" },
            kontext.knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
            bereich,
        )
        val ergebnis = AussagenSatzRechner.erzeuge(
            operator,
            if (operator in quantorOperatoren) aussagen.take(1) else aussagen,
            variable,
            kontext.knoten.parameter["praedikatName"],
        )
        val wert: MathematischesObjekt = when (ergebnis) {
            is AussagenSatzErgebnis.AussageWert -> ergebnis.aussage
            is AussagenSatzErgebnis.PraedikatWert -> ergebnis.praedikat
            is AussagenSatzErgebnis.Ungueltig -> error(ergebnis.nachricht)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
            eingänge = kontext.eingänge,
        )
    }

    registriere(CAUCHY_ART) { kontext ->
        val tupel = kontext.eingänge["tupel"]?.objekt as? UnnatuerlichesKartesischesTupel
            ?: error("Cauchy benötigt ein unnatürliches kartesisches Tupel.")
        val aussage = when (val ergebnis = pruefeCauchy(tupel)) {
            is CauchyErgebnis.AussageWert -> ergebnis.aussage
            is CauchyErgebnis.NichtAnwendbar -> error(ergebnis.grund)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("aussage" to BedingterWert(aussage, kontext.annahmen())),
            eingänge = kontext.eingänge,
        )
    }
}

private val quantorOperatoren = setOf(
    AussagenSatzOperator.ALLQUANTOR,
    AussagenSatzOperator.EXISTENZQUANTOR,
    AussagenSatzOperator.EINDEUTIGER_EXISTENZQUANTOR,
)

private fun tensorOperator(wert: String?): TensorRechnerOperator =
    TensorRechnerOperator.entries.firstOrNull { it.stabileId == wert || it.name == wert }
        ?: TensorRechnerOperator.TENSORPRODUKT

private fun aussagenOperator(wert: String?): AussagenSatzOperator =
    AussagenSatzOperator.entries.firstOrNull { it.stabileId == wert || it.name == wert }
        ?: AussagenSatzOperator.KONJUNKTION

private fun parseIntListe(wert: String?): List<Int> =
    wert.orEmpty().split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }

private fun KnotenAuswertungsKontext.annahmen() = eingänge.values.flatMap { it.annahmen }.toSet()
