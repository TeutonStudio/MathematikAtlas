package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TUPEL_AUFLÖSEN_ART
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.MATRIX_ZERLEGEN_RICHTUNG
import de.TeutonStudio.MathematikKnoten.TENSOR_OPERATION_ID
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_SPEZIFIKATION
import de.TeutonStudio.MathematikKnoten.anschlussArtFürMathematischesObjekt
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.OrientierterVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixZerlegeRichtung
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorartig
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.TypisiertesElement
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import java.math.BigInteger

internal const val TUPEL_AUFLÖSEN_ANZAHL = "tupelAuflösen.anzahl"
internal const val VEKTOR_ZERLEGEN_ANZAHL = "vektorZerlegen.anzahl"

/**
 * Synchronisiert den letzten erfolgreich bekannten Strukturvertrag mit den
 * dynamischen Ausgängen aller Auflöser und Zerleger. Bei fehlgeschlagener Auswertung bleibt der bestehende
 * Vertrag unverändert; dadurch flackert die Graphstruktur nicht zwischen zwei
 * Auswertungszuständen.
 */
internal fun synchronisiereStrukturZerleger(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten {
    fun istDynamischerAuflöser(knoten: KnotenDaten): Boolean =
        knoten.art == TUPEL_AUFLÖSEN_ART ||
            (
                knoten.art == VektorRechner.KNOTEN_ART &&
                    VektorRechnerOperator.vonIdOderNull(knoten.parameter[VEKTOR_RECHNER_OPERATOR]) ==
                    VektorRechnerOperator.ZERLEGEN
                ) || knoten.art == MatrixRechner.KNOTEN_ART &&
            knoten.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.ZERLEGEN.stabileId ||
            knoten.art == TensorRechner.KNOTEN_ART &&
            (knoten.parameter[TENSOR_OPERATION_ID] ?: knoten.parameter[RECHNER_OPERATOR_PARAMETER]) ==
            TensorRechnerOperator.ZERLEGEN.stabileId

    val idErsetzungen = buildMap {
        karte.knoten.filter(::istDynamischerAuflöser).forEach { knoten ->
            knoten.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Ausgang }
                .sortedBy { it.reihenfolge }
                .forEachIndexed { index, anschluss ->
                    val alt = AnschlussVerweis(knoten.id, anschluss.id)
                    val neu = AnschlussVerweis(knoten.id, dynamischerAusgangId(knoten, index, anschluss.name))
                    if (alt != neu) put(alt, neu)
                }
        }
    }

    val synchronisierteKnoten = karte.knoten.map { knoten ->
        when {
            knoten.art == TUPEL_AUFLÖSEN_ART -> {
                val tupel = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("tupel")
                    ?.objekt as? Tupel
                    ?: return@map knoten
                synchronisiereAuflöserKnoten(knoten, tupel.elemente, "tupel", TUPEL_AUFLÖSEN_ANZAHL)
            }
            knoten.art == VektorRechner.KNOTEN_ART &&
                VektorRechnerOperator.vonIdOderNull(knoten.parameter[VEKTOR_RECHNER_OPERATOR]) ==
                VektorRechnerOperator.ZERLEGEN -> {
                val struktur = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("struktur")
                    ?.objekt
                    ?: return@map knoten
                val elemente = when (struktur) {
                    is Tupel -> struktur.elemente
                    is OrientierterVektor -> struktur.werte
                    is TypisiertesElement -> {
                        val anzahl = struktur.strukturForm?.firstOrNull()?.konkreteDimensionOderNull()
                            ?: return@map synchronisiereMethodenAusgang(knoten, "struktur")
                        return@map synchronisiereStrukturAusgaenge(
                            knoten,
                            "struktur",
                            anzahl,
                            "element",
                            MathematikAnschlussArten.Zahl.id,
                            VEKTOR_ZERLEGEN_ANZAHL,
                        )
                    }
                    else -> return@map knoten
                }
                synchronisiereAuflöserKnoten(knoten, elemente, "struktur", VEKTOR_ZERLEGEN_ANZAHL)
            }
            knoten.art == MatrixRechner.KNOTEN_ART &&
                knoten.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.ZERLEGEN.stabileId -> {
                val matrix = auswertung.knoten[knoten.id]?.eingänge?.get("matrix")?.objekt ?: return@map knoten
                val form = when (matrix) {
                    is Matrix -> listOf(matrix.zeilenAnzahl, matrix.spaltenAnzahl)
                    is TypisiertesElement -> matrix.konkreteFormOderNull()
                    else -> return@map knoten
                } ?: return@map synchronisiereMethodenAusgang(knoten, "matrix")
                val richtung = runCatching {
                    MatrixZerlegeRichtung.valueOf(knoten.parameter[MATRIX_ZERLEGEN_RICHTUNG] ?: MatrixZerlegeRichtung.ZEILEN.name)
                }.getOrDefault(MatrixZerlegeRichtung.ZEILEN)
                val anzahl = form[if (richtung == MatrixZerlegeRichtung.ZEILEN) 0 else 1]
                val prefix = if (richtung == MatrixZerlegeRichtung.ZEILEN) "zeile" else "spalte"
                val art = if (richtung == MatrixZerlegeRichtung.ZEILEN) {
                    MathematikAnschlussArten.ZeilenVektor.id
                } else MathematikAnschlussArten.SpaltenVektor.id
                synchronisiereStrukturAusgaenge(knoten, "matrix", anzahl, prefix, art, "matrixZerlegen.anzahl")
            }
            knoten.art == TensorRechner.KNOTEN_ART &&
                (knoten.parameter[TENSOR_OPERATION_ID] ?: knoten.parameter[RECHNER_OPERATOR_PARAMETER]) ==
                TensorRechnerOperator.ZERLEGEN.stabileId -> {
                val tensor = auswertung.knoten[knoten.id]?.eingänge?.get("tensor")?.objekt ?: return@map knoten
                val form = when (tensor) {
                    is Tensorartig -> tensor.tensorForm
                    is TypisiertesElement -> tensor.konkreteFormOderNull()
                    else -> return@map knoten
                } ?: return@map synchronisiereMethodenAusgang(knoten, "tensor")
                val achse = knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION]
                    ?.split(',')?.firstOrNull()?.trim()?.toIntOrNull()?.minus(1) ?: return@map knoten
                if (achse !in form.indices) return@map knoten
                val restRang = form.size - 1
                val art = when (restRang) {
                    0 -> MathematikAnschlussArten.Zahl.id
                    2 -> MathematikAnschlussArten.Matrix.id
                    else -> MathematikAnschlussArten.Tensor.id
                }
                synchronisiereStrukturAusgaenge(
                    knoten, "tensor", form[achse], "schnitt", art, "tensorZerlegen.anzahl",
                )
            }
            else -> knoten
        }
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

/** Quellkompatibler Name für bestehende Aufrufer und historische Tests. */
internal fun synchronisiereTupelAuflöser(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten = synchronisiereStrukturZerleger(karte, auswertung, prüfung)

private fun synchronisiereMethodenAusgang(knoten: KnotenDaten, eingangsName: String): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == eingangsName
    } ?: return knoten
    val vorhanden = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "methode"
    }
    val methode = (vorhanden ?: AnschlussDaten(
        id = AnschlussId("${knoten.id.wert}:strukturZerlegen:methode"),
        name = "methode",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten.Methode.id,
    )).copy(reihenfolge = 0, dynamischErzeugt = true)
    return knoten.copy(anschlüsse = listOf(eingang.copy(reihenfolge = 0), methode))
}

private fun synchronisiereStrukturAusgaenge(
    knoten: KnotenDaten,
    eingangsName: String,
    anzahl: Int,
    prefix: String,
    art: AnschlussArtId,
    anzahlParameter: String,
): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == eingangsName
    } ?: return knoten
    val ausgänge = List(anzahl) { index ->
        AnschlussDaten(
            id = dynamischerAusgangId(knoten, index),
            name = "$prefix${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = art,
            reihenfolge = index,
            dynamischErzeugt = true,
        )
    }
    return knoten.copy(
        anschlüsse = listOf(eingang.copy(reihenfolge = 0)) + ausgänge,
        größe = knoten.größe.copy(höhe = maxOf(knoten.größe.höhe, 78f + 28f * anzahl)),
        parameter = knoten.parameter + (anzahlParameter to anzahl.toString()),
    )
}

private fun de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck.konkreteDimensionOderNull(): Int? =
    (this as? RationaleZahl)?.takeIf {
        it.nenner == BigInteger.ONE && it.zähler.signum() > 0 && it.zähler.bitLength() < 31
    }?.zähler?.toInt()

private fun TypisiertesElement.konkreteFormOderNull(): List<Int>? =
    strukturForm?.map { it.konkreteDimensionOderNull() ?: return null }

private fun synchronisiereAuflöserKnoten(
    knoten: KnotenDaten,
    elemente: List<de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt>,
    eingangsName: String,
    anzahlParameter: String,
): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == eingangsName
    } ?: return knoten
    val bisherigeAusgänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }

    val ausgänge = elemente.mapIndexed { index, element ->
        (bisherigeAusgänge.getOrNull(index) ?: AnschlussDaten(
            id = elementId(knoten.id, index, knoten.art),
            name = "element${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
        )).copy(
            id = elementId(knoten.id, index, knoten.art),
            name = "element${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
            zulässigeArten = emptySet(),
            reihenfolge = index,
            kannSichErweitern = false,
            dynamischErzeugt = true,
        )
    }

    val mindestHöhe = maxOf(115f, 78f + 28f * ausgänge.size)
    return knoten.copy(
        anschlüsse = listOf(eingang.copy(reihenfolge = 0)) + ausgänge,
        größe = knoten.größe.copy(höhe = maxOf(knoten.größe.höhe, mindestHöhe)),
        parameter = knoten.parameter + (anzahlParameter to ausgänge.size.toString()),
    )
}

private fun elementId(knotenId: KnotenId, index: Int, knotenArt: String) =
    AnschlussId(
        if (knotenArt == TUPEL_AUFLÖSEN_ART) {
            "${knotenId.wert}:tupelAuflösen:element:${index + 1}"
        } else {
            "${knotenId.wert}:vektorZerlegen:element:${index + 1}"
        },
    )

private fun dynamischerAusgangId(knoten: KnotenDaten, index: Int, name: String? = null): AnschlussId = when {
    name == "methode" -> AnschlussId("${knoten.id.wert}:strukturZerlegen:methode")
    knoten.art == MatrixRechner.KNOTEN_ART -> AnschlussId("${knoten.id.wert}:matrixZerlegen:${index + 1}")
    knoten.art == TensorRechner.KNOTEN_ART -> AnschlussId("${knoten.id.wert}:tensorZerlegen:${index + 1}")
    else -> elementId(knoten.id, index, knoten.art)
}
