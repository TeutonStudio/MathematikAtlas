package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Ersetzt die Tensorrechner-Schnittstelle atomar passend zum Operator.
 * Anschlüsse gleicher Rolle behalten ihre IDs; unpassende Verbindungen werden
 * durch KnotenKonfigurationErsetzen in derselben Undo-Aktion entfernt.
 */
fun konfiguriereTensorRechner(
    knoten: KnotenDaten,
    operator: TensorRechnerOperator,
): KnotenDaten {
    require(knoten.art == TensorRechner.KNOTEN_ART) { "Nur Tensorrechner können so konfiguriert werden." }
    val vorhandene = knoten.anschlüsse.associateBy { it.name }

    fun eingang(name: String, art: AnschlussArtId, reihenfolge: Int): AnschlussDaten =
        vorhandene[name]?.copy(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art,
            reihenfolge = reihenfolge,
            kannSichErweitern = false,
            dynamischErzeugt = false,
        ) ?: AnschlussDaten(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art,
            reihenfolge = reihenfolge,
        )

    val eingänge = when (operator) {
        TensorRechnerOperator.SKALARMULTIPLIKATION -> listOf(
            eingang("skalar", MathematikAnschlussArten.Zahl.id, 0),
            eingang("tensor", MathematikAnschlussArten.Objekt.id, 1),
        )
        TensorRechnerOperator.ACHSENPERMUTATION,
        TensorRechnerOperator.TRANSPONIEREN,
        TensorRechnerOperator.ACHSENSCHNITT,
        TensorRechnerOperator.INDEXAUSWERTUNG,
        TensorRechnerOperator.KONTRAKTION,
        TensorRechnerOperator.NORM,
        -> listOf(eingang("tensor", MathematikAnschlussArten.Objekt.id, 0))
        else -> listOf(
            eingang("links", MathematikAnschlussArten.Objekt.id, 0),
            eingang("rechts", MathematikAnschlussArten.Objekt.id, 1),
        )
    }
    val ausgangsArt = when (operator) {
        TensorRechnerOperator.INDEXAUSWERTUNG,
        TensorRechnerOperator.NORM,
        -> MathematikAnschlussArten.Zahl.id
        else -> MathematikAnschlussArten.Objekt.id
    }
    val ausgang = vorhandene["wert"]?.copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangsArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangsArt,
    )
    return knoten.copy(
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter + (RECHNER_OPERATOR_PARAMETER to operator.stabileId),
    )
}

fun KartenEditorZustand.setzeTensorRechnerOperator(knotenId: KnotenId, operator: TensorRechnerOperator) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
    val konfiguriert = konfiguriereTensorRechner(knoten, operator)
    führeAus(KartenAktion.KnotenKonfigurationErsetzen(knotenId, konfiguriert.parameter, konfiguriert.anschlüsse))
}

/**
 * Konsolidiert die in v2.20.0 eingeführten Strukturknoten auf gemeinsame
 * Komponentenverträge und auf auswertbare Fehlerzustände.
 */
internal fun MathematikAuswerterRegister.registriereLineareStrukturErweiterungen() {
    registriere(SKALARPRODUKT_ART) { kontext ->
        val links = kontext.eingänge["links"]?.objekt
            ?: return@registriere kontext.fehler("Linker Eingang fehlt.")
        val rechts = kontext.eingänge["rechts"]?.objekt
            ?: return@registriere kontext.fehler("Rechter Eingang fehlt.")
        val linearitaet = runCatching {
            SkalarproduktLinearitaet.valueOf(
                kontext.knoten.parameter["linearitaet"] ?: SkalarproduktLinearitaet.RECHTSLINEAR.name,
            )
        }.getOrDefault(SkalarproduktLinearitaet.RECHTSLINEAR)

        when (
            val ergebnis = standardSkalarprodukt(
                links,
                rechts,
                SkalarproduktSpezifikation(linearitaet = linearitaet, konjugiert = true),
            )
        ) {
            is StrukturPruefung.Gueltig -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf("wert" to BedingterWert(ergebnis.wert, kontext.annahmen())),
                eingänge = kontext.eingänge,
                warnungen = if (kontext.knoten.parameter["konjugiert"] == "false") {
                    listOf("Die veraltete Option 'konjugiert=false' wird beim Standardskalarprodukt ignoriert.")
                } else emptyList(),
            )
            is StrukturPruefung.Bedingt -> ergebnis.wert?.let { wert ->
                KnotenAuswertungsErgebnis(
                    ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
                    eingänge = kontext.eingänge,
                    warnungen = ergebnis.bedingungen,
                )
            } ?: kontext.fehler(ergebnis.bedingungen.joinToString())
            is StrukturPruefung.Ungueltig -> kontext.fehler(ergebnis.grund)
            is StrukturPruefung.Unentscheidbar -> kontext.fehler(ergebnis.grund)
        }
    }

    registriere(TensorRechner.KNOTEN_ART) { kontext ->
        val operator = tensorOperator(kontext.knoten.parameter[RECHNER_OPERATOR_PARAMETER])
        val ergebnis = TensorRechner.erzeuge(
            operator,
            tensorEingaben(kontext, operator),
            TensorRechnerKonfiguration(
                achsen = parseIntListe(kontext.knoten.parameter["achsen"]),
                indizes = parseIntListe(kontext.knoten.parameter["indizes"]),
                permutation = parseIntListe(kontext.knoten.parameter["permutation"]),
            ),
        )
        when (ergebnis) {
            is TensorRechnerErgebnis.Wert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf("wert" to BedingterWert(ergebnis.objekt, kontext.annahmen())),
                eingänge = kontext.eingänge,
            )
            is TensorRechnerErgebnis.Bedingt -> kontext.fehler(
                ergebnis.bedingungen.joinToString().ifBlank { "Das Tensorergebnis ist nur bedingt bestimmt." },
            )
            is TensorRechnerErgebnis.Ungueltig -> kontext.fehler(ergebnis.nachricht)
        }
    }

    // Der historische Matrixauswerter verlangte konkrete Vektorklassen.
    // Die gemeinsame Ansicht erlaubt zusätzlich homogene kartesische Tupel.
    registriere("mathematik.matrix") { kontext ->
        runCatching { kontext.matrixAusKomponentenfolgen() }.fold(
            onSuccess = { matrix -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf("matrix" to BedingterWert(matrix, kontext.annahmen())),
                eingänge = kontext.eingänge,
            ) },
            onFailure = { ursache -> kontext.fehler(ursache.message ?: "Die Matrix konnte nicht erzeugt werden.") },
        )
    }
}

private fun tensorEingaben(
    kontext: KnotenAuswertungsKontext,
    operator: TensorRechnerOperator,
): List<TensorRechnerEingabe> = when (operator) {
    TensorRechnerOperator.SKALARMULTIPLIKATION -> listOfNotNull(
        (kontext.eingänge["skalar"] ?: kontext.eingänge["links"])
            ?.objekt?.let { TensorRechnerEingabe("skalar", it) },
        (kontext.eingänge["tensor"] ?: kontext.eingänge["rechts"])
            ?.objekt?.let { TensorRechnerEingabe("tensor", it) },
    )
    TensorRechnerOperator.TENSORPRODUKT -> listOfNotNull(
        kontext.eingänge["links"]?.objekt?.let { TensorRechnerEingabe("links", it) },
        kontext.eingänge["rechts"]?.objekt?.let { TensorRechnerEingabe("rechts", it) },
    )
    TensorRechnerOperator.ACHSENPERMUTATION,
    TensorRechnerOperator.TRANSPONIEREN,
    TensorRechnerOperator.ACHSENSCHNITT,
    TensorRechnerOperator.INDEXAUSWERTUNG,
    TensorRechnerOperator.KONTRAKTION,
    TensorRechnerOperator.NORM,
    -> listOfNotNull(
        (kontext.eingänge["tensor"] ?: kontext.eingänge["links"])
            ?.objekt?.let { TensorRechnerEingabe("tensor", it) },
    )
    else -> listOfNotNull(
        kontext.eingänge["links"]?.objekt?.let { TensorRechnerEingabe("links", it) },
        kontext.eingänge["rechts"]?.objekt?.let { TensorRechnerEingabe("rechts", it) },
    )
}

private fun KnotenAuswertungsKontext.matrixAusKomponentenfolgen(): Matrix {
    val höhe = parameterInt("höhe")
    val breite = parameterInt("breite")
    return when (knoten.parameter["erzeugungsArt"] ?: MATRIX_EINZEL_EINGABEN) {
        MATRIX_METHODE -> {
            val methode = eingänge["methode"]?.objekt as? Methode ?: error("Matrixmethode fehlt.")
            matrixAusMethode(methode, höhe, breite)
        }
        MATRIX_ZEILEN -> Matrix(List(höhe) { zeile ->
            val ansicht = komponentenAnsicht(matrixZeileName(zeile), "Zeile ${zeile + 1}")
            require(ansicht.orientierung != VektorOrientierung.Spalte) {
                "Zeile ${zeile + 1} darf kein Spaltenvektor sein."
            }
            require(ansicht.laenge == breite) {
                "Zeile ${zeile + 1} muss $breite Elemente besitzen, hat aber ${ansicht.laenge}."
            }
            ansicht.komponenten
        })
        MATRIX_SPALTEN -> {
            val spalten = List(breite) { spalte ->
                val ansicht = komponentenAnsicht(matrixSpalteName(spalte), "Spalte ${spalte + 1}")
                require(ansicht.orientierung != VektorOrientierung.Zeile) {
                    "Spalte ${spalte + 1} darf kein Zeilenvektor sein."
                }
                require(ansicht.laenge == höhe) {
                    "Spalte ${spalte + 1} muss $höhe Elemente besitzen, hat aber ${ansicht.laenge}."
                }
                ansicht.komponenten
            }
            Matrix(List(höhe) { zeile -> List(breite) { spalte -> spalten[spalte][zeile] } })
        }
        else -> Matrix(List(höhe) { zeile -> List(breite) { spalte ->
            eingänge[matrixEintragName(zeile, spalte)]?.objekt as? ZahlAusdruck
                ?: error("Matrixeintrag (${zeile + 1},${spalte + 1}) fehlt oder ist keine Zahl.")
        } })
    }
}

private fun KnotenAuswertungsKontext.komponentenAnsicht(
    anschlussName: String,
    bezeichnung: String,
): NumerischeKomponentenAnsicht {
    val objekt = eingänge[anschlussName]?.objekt ?: error("$bezeichnung fehlt.")
    return when (val ansicht = objekt.numerischeKomponentenAnsicht()) {
        is StrukturPruefung.Gueltig -> ansicht.wert
        is StrukturPruefung.Bedingt -> error(ansicht.bedingungen.joinToString())
        is StrukturPruefung.Ungueltig -> error("$bezeichnung ist ungültig: ${ansicht.grund}")
        is StrukturPruefung.Unentscheidbar -> error("$bezeichnung ist nicht entscheidbar: ${ansicht.grund}")
    }
}

private fun tensorOperator(wert: String?): TensorRechnerOperator =
    TensorRechnerOperator.entries.firstOrNull { it.stabileId == wert || it.name == wert }
        ?: TensorRechnerOperator.TENSORPRODUKT

private fun parseIntListe(wert: String?): List<Int> =
    wert.orEmpty().split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }

private fun KnotenAuswertungsKontext.parameterInt(name: String): Int =
    knoten.parameter[name]?.toIntOrNull()?.takeIf { it > 0 }
        ?: error("Parameter $name muss eine positive ganze Zahl sein.")

private fun KnotenAuswertungsKontext.annahmen() = eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehler(nachricht: String) = KnotenAuswertungsErgebnis(
    ausgaben = emptyMap(),
    eingänge = eingänge,
    fehler = nachricht,
)
