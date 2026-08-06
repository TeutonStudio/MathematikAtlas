package de.TeutonStudio.MathematikRechenSystem.kern

private const val ITERATIONS_BASIS_ROLLE = "basis"
private const val ITERATIONS_ORDNUNG_ROLLE = "ordnung"

object IterationsFormelCodec {
    fun zuFormel(ausdruck: IterierterAusdruck): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id = "iteration-${ausdruck.art.name.lowercase()}",
        operatorId = ausdruck.operatorId,
        argumente = listOf(
            FormelArgument(
                rollenId = ITERATIONS_BASIS_ROLLE,
                position = 0,
                ausdruck = ausdruck.basis.alsIterationsLiteral("basis"),
            ),
            FormelArgument(
                rollenId = ITERATIONS_ORDNUNG_ROLLE,
                position = 1,
                ausdruck = ausdruck.ordnung.alsFormelAusdruck(),
            ),
        ),
        typ = when (ausdruck.art) {
            IterationsArt.MULTIPLIKATION -> ausdruck.basis.formelTyp()
            IterationsArt.DIFFERENTIATION,
            IterationsArt.SELBSTKOMPOSITION,
            -> FormelTyp.METHODE
        },
        bedingungen = (ausdruck.ordnung as? IterationsOrdnung.Symbolisch)?.annahmen?.toList().orEmpty(),
    )

    fun ausFormel(formel: FormelAusdruck.Operation): IterierterAusdruck {
        val art = IterationsArt.entries.singleOrNull { it.operatorId == formel.operatorId }
            ?: error("Die Formeloperation '${formel.operatorId}' ist keine registrierte Iteration.")
        val argumente = formel.argumente.associateBy(FormelArgument::rollenId)
        val basis = requireNotNull(argumente[ITERATIONS_BASIS_ROLLE]) {
            "Der Iterationsformel fehlt die Basis."
        }.ausdruck.alsIterationsBasis(art)
        val ordnung = requireNotNull(argumente[ITERATIONS_ORDNUNG_ROLLE]) {
            "Der Iterationsformel fehlt die Ordnung."
        }.ausdruck.alsIterationsOrdnung(formel.bedingungen.toSet())
        return IterierterAusdruck(basis, art, ordnung)
    }
}

private fun IterationsOrdnung.alsFormelAusdruck(): FormelAusdruck = when (this) {
    is IterationsOrdnung.Konkret -> RationaleZahl.von(wert).alsIterationsLiteral("ordnung")
    is IterationsOrdnung.Symbolisch -> ausdruck.alsIterationsLiteral("ordnung-symbolisch")
}

private fun MathematischesObjekt.alsIterationsLiteral(suffix: String): FormelAusdruck.Literal =
    FormelAusdruck.Literal(
        id = "iteration-$suffix",
        wert = this,
        typ = formelTyp(),
    )

private fun MathematischesObjekt.formelTyp(): FormelTyp = when (this) {
    is ZahlAusdruck -> FormelTyp.ZAHL
    is Methode -> FormelTyp.METHODE
    is MengenAusdruck -> FormelTyp.MENGE
    is Aussage -> FormelTyp.AUSSAGE
    is Tupel -> FormelTyp.TUPEL
    is ZeilenVektor, is SpaltenVektor -> FormelTyp.VEKTOR
    is Matrix -> FormelTyp.MATRIX
    is Tensor -> FormelTyp.TENSOR
    else -> FormelTyp.OBJEKT
}

private fun FormelAusdruck.alsIterationsBasis(art: IterationsArt): MathematischesObjekt = when (this) {
    is FormelAusdruck.Literal -> {
        if (art != IterationsArt.MULTIPLIKATION) {
            require(wert is Methode) {
                "Differentiation und Selbstkomposition benötigen im Formel-DAG eine Methodenbasis."
            }
        }
        wert
    }
    is FormelAusdruck.Variable -> {
        require(art == IterationsArt.MULTIPLIKATION) {
            "Eine freie Formelvariable ist ohne Methodenvertrag keine gültige Methodeniteration."
        }
        Variable(name, latex)
    }
    is FormelAusdruck.Platzhalter -> {
        require(art == IterationsArt.MULTIPLIKATION) {
            "Ein Platzhalter ist ohne Methodenvertrag keine gültige Methodeniteration."
        }
        AllgemeinerParameter(id, "\\square")
    }
    is FormelAusdruck.Operation -> error(
        "Eine verschachtelte Iterationsbasis muss vor dem Rückweg als typisiertes Literal ausgewertet werden.",
    )
}

private fun FormelAusdruck.alsIterationsOrdnung(
    bedingungen: Set<Aussage>,
): IterationsOrdnung {
    val objekt = when (this) {
        is FormelAusdruck.Literal -> wert
        is FormelAusdruck.Variable -> Variable(name, latex)
        is FormelAusdruck.Platzhalter -> return IterationsOrdnung.Symbolisch(
            ausdruck = Variable(id, "\\square"),
            annahmen = bedingungen.ifEmpty {
                setOf(UnentscheidbareAussage("\\square\\in\\mathbb N_0", "Iterationsordnung"))
            },
        )
        is FormelAusdruck.Operation -> return IterationsOrdnung.Symbolisch(
            ausdruck = Variable(id, FormelRenderer.render(this).latex),
            annahmen = bedingungen.ifEmpty {
                setOf(UnentscheidbareAussage("${FormelRenderer.render(this).latex}\\in\\mathbb N_0", "Iterationsordnung"))
            },
        )
    }
    return when (val pruefung = pruefeIterationsOrdnung(objekt, bedingungen)) {
        is IterationsOrdnungsPruefung.Gueltig -> pruefung.ordnung
        is IterationsOrdnungsPruefung.Ungueltig -> error(pruefung.nachricht)
    }
}
