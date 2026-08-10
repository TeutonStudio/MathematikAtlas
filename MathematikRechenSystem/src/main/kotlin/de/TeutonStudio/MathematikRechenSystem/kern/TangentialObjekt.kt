package de.TeutonStudio.MathematikRechenSystem.kern

enum class TangentialAusgabeForm {
    METHODE,
    MENGE,
}

/**
 * Strukturierter geometrischer Tangentialausdruck für Fälle, die nicht als
 * eindeutiger Funktionsgraph materialisiert werden können oder sollen.
 */
data class TangentialMenge(
    val methode: Methode,
    val argument: MathematischesObjekt,
    val graphRaum: MengenAusdruck = methode.graphRaum(),
    val begriff: DifferentialBegriff = DifferentialBegriff.REELL_FRECHET,
) : MengenAusdruck {
    override fun zuLatex(): String =
        "T_{\\left(${argument.zuLatex()},${methode.name}(${argument.zuLatex()})\\right)}\\Gamma_{${methode.name}}"
}

sealed interface TangentialErgebnis {
    data class MethodeWert(
        val methode: Methode,
        val verwendeteRegel: String,
    ) : TangentialErgebnis

    data class MengeWert(
        val menge: MengenAusdruck,
        val verwendeteRegel: String,
    ) : TangentialErgebnis

    data class NichtDarstellbarAlsMethode(
        val menge: TangentialMenge,
        val grund: String,
    ) : TangentialErgebnis
}

/**
 * Erzeugt das gemeinsame Tangentialobjekt. Der exakt implementierte Kernfall ist
 * eine einstellige zahlwertige Methode. Höherdimensionale Fälle bleiben als
 * strukturierte Tangentialmenge erhalten und können von späteren Koordinaten-
 * beziehungsweise Differentialadaptern materialisiert werden.
 */
fun tangentialObjekt(
    methode: Methode,
    argument: MathematischesObjekt,
    ausgabeForm: TangentialAusgabeForm,
    begriff: DifferentialBegriff = DifferentialBegriff.REELL_FRECHET,
): TangentialErgebnis {
    val symbolischeMenge = TangentialMenge(methode, argument, methode.graphRaum(), begriff)
    val skalarMethode = erzeugeSkalareTangentialMethodeOderNull(methode, argument, begriff)

    return when (ausgabeForm) {
        TangentialAusgabeForm.METHODE -> if (skalarMethode != null) {
            TangentialErgebnis.MethodeWert(
                skalarMethode,
                "Skalare affine Linearisierung f(a)+f'(a)(x-a).",
            )
        } else {
            TangentialErgebnis.NichtDarstellbarAlsMethode(
                symbolischeMenge,
                "Die Tangentialmenge ist für diese Signatur nicht als eindeutige Methode über den vorhandenen Argumentkoordinaten materialisierbar. Verwende die Mengenausgabe.",
            )
        }

        TangentialAusgabeForm.MENGE -> if (skalarMethode != null) {
            TangentialErgebnis.MengeWert(
                skalarMethode.graphMenge(),
                "Graph der skalaren affinen Tangentialmethode.",
            )
        } else {
            TangentialErgebnis.MengeWert(
                symbolischeMenge,
                "Strukturierte Tangentialmenge im Graphraum; Materialisierung benötigt den passenden Koordinaten-/Differentialadapter.",
            )
        }
    }
}

private fun erzeugeSkalareTangentialMethodeOderNull(
    methode: Methode,
    argument: MathematischesObjekt,
    begriff: DifferentialBegriff,
): Methode? {
    val variable = methode.parameter.singleOrNull() as? Variable ?: return null
    val argumentZahl = argument as? ZahlAusdruck ?: return null
    val vorschrift = methode.vorschrift as? ZahlAusdruck ?: return null
    val funktionsWert = runCatching { methode.wendeAn(listOf(argumentZahl)) as? ZahlAusdruck }.getOrNull() ?: return null
    val ableitung = runCatching {
        differenziereMethodeStrukturiert(
            methode = methode,
            ordnung = DifferentialOrdnung.Konkret(1),
            operator = DifferentialOperator.Total,
            begriff = begriff,
        )
    }.getOrNull() ?: return null
    if (ableitung.status !in setOf(
            DifferentialUnterstuetzungsStatus.BERECHNET,
            DifferentialUnterstuetzungsStatus.BEDINGT,
        )
    ) return null
    val ableitungsWert = runCatching {
        ableitung.methode.wendeAn(listOf(argumentZahl)) as? ZahlAusdruck
    }.getOrNull() ?: return null

    val term = vereinfache(
        addition(
            funktionsWert,
            multiplikation(
                ableitungsWert,
                subtraktion(variable, argumentZahl),
            ),
        ),
    )
    return Methode(
        name = "t_{${argumentZahl.zuLatex()}}",
        parameter = listOf(variable),
        vorschrift = term,
        zielMenge = methode.zielMenge,
        werteVorräte = methode.werteVorräte,
        ausgabeNamen = methode.ausgabeNamen,
        effektiverWerteVorrat = methode.effektiverWerteVorrat,
    )
}
