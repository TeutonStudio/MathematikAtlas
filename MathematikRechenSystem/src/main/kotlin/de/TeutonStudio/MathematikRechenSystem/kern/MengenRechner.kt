package de.TeutonStudio.MathematikRechenSystem.kern

enum class MengenRechnerOperator(val stabileId: String) {
    VEREINIGUNG("menge.vereinigung"),
    SCHNITT("menge.schnitt"),
    DIFFERENZ("menge.differenz"),
    SYMMETRISCHE_DIFFERENZ("menge.symmetrischeDifferenz"),
    KOMPLEMENT("menge.komplement"),
    KARTESISCHES_PRODUKT("menge.kartesischesProdukt"),
    ITERIERTES_KARTESISCHES_PRODUKT("menge.iteriertesKartesischesProdukt"),
    ITERIERTE_VEREINIGUNG("menge.iterierteVereinigung"),
    ITERIERTER_SCHNITT("menge.iterierterSchnitt"),
    POTENZMENGE("menge.potenzmenge"),
    BILD("menge.bild"),
    URBILD("menge.urbild"),
    ;

    companion object {
        fun vonIdOderNull(id: String?): MengenRechnerOperator? = entries.firstOrNull { operator ->
            id == operator.stabileId || id.equals(operator.name, ignoreCase = true)
        }
    }
}

data class MengenRechnerEingabe(
    val rollenId: String,
    val menge: MengenAusdruck,
    val elementUniversum: MengenAusdruck? = null,
)

sealed interface MengenRechnerErgebnis {
    data class Wert(
        val menge: MengenAusdruck,
        val universum: MengenAusdruck?,
        val operator: MengenRechnerOperator,
        val bedingungen: List<String> = emptyList(),
    ) : MengenRechnerErgebnis {
        val latex: String get() = menge.zuLatex()
    }

    data class Bedingt(
        val latex: String,
        val moeglicheUniversen: Set<MengenAusdruck>,
        val bedingungen: List<String>,
    ) : MengenRechnerErgebnis

    data class Ungueltig(val code: String, val nachricht: String) : MengenRechnerErgebnis
}

object MengenRechner {
    const val KNOTEN_ART = "mathematik.mengenrechner"

    fun erzeuge(
        operator: MengenRechnerOperator,
        eingaben: List<MengenRechnerEingabe>,
    ): MengenRechnerErgebnis {
        if (eingaben.isEmpty()) return MengenRechnerErgebnis.Ungueltig(
            "eingabe_fehlt",
            "Der Mengenoperator ${operator.name} benötigt mindestens eine Eingabe.",
        )
        val universen = eingaben.mapNotNull { it.elementUniversum }.distinct()
        val gemeinsamesUniversum = universen.singleOrNull()
        val inkompatibel = universen.size > 1

        return when (operator) {
            MengenRechnerOperator.VEREINIGUNG -> {
                if (eingaben.size < 2) ungueltigeAnzahl(operator, 2)
                else if (inkompatibel) bedingt(operator, eingaben, universen)
                else MengenRechnerErgebnis.Wert(
                    vereinige(eingaben.map { it.menge }),
                    gemeinsamesUniversum,
                    operator,
                )
            }
            MengenRechnerOperator.SCHNITT -> {
                if (eingaben.size < 2) ungueltigeAnzahl(operator, 2)
                else if (inkompatibel) bedingt(operator, eingaben, universen)
                else MengenRechnerErgebnis.Wert(
                    normalisiereZahlmengenSchnitt(eingaben.map { it.menge }, gemeinsamesUniversum),
                    gemeinsamesUniversum,
                    operator,
                )
            }
            MengenRechnerOperator.DIFFERENZ -> binaer(eingaben, "grundmenge", "abzug") { links, rechts ->
                MengenRechnerErgebnis.Wert(
                    mengenDifferenz(links.menge, rechts.menge),
                    links.elementUniversum,
                    operator,
                    universumsBedingungen(links, rechts),
                )
            }
            MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ -> binaer(eingaben, "links", "rechts") { links, rechts ->
                val wert = vereinige(
                    listOf(
                        mengenDifferenz(links.menge, rechts.menge),
                        mengenDifferenz(rechts.menge, links.menge),
                    ),
                )
                MengenRechnerErgebnis.Wert(
                    wert,
                    links.elementUniversum ?: rechts.elementUniversum,
                    operator,
                    universumsBedingungen(links, rechts),
                )
            }
            MengenRechnerOperator.KOMPLEMENT -> binaer(eingaben, "menge", "universum") { menge, universum ->
                MengenRechnerErgebnis.Wert(
                    mengenDifferenz(universum.menge, menge.menge),
                    universum.elementUniversum ?: universum.menge,
                    operator,
                )
            }
            MengenRechnerOperator.KARTESISCHES_PRODUKT -> {
                if (eingaben.size < 2) ungueltigeAnzahl(operator, 2)
                else MengenRechnerErgebnis.Wert(
                    kartesischesProdukt(eingaben.map { it.menge }),
                    Tupelraum(eingaben.map { it.elementUniversum ?: it.menge }),
                    operator,
                )
            }
            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
            MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
            MengenRechnerOperator.ITERIERTER_SCHNITT,
            -> MengenRechnerErgebnis.Ungueltig(
                "iterationsvertrag",
                "${operator.name} benötigt eine Mengenmethode und eine Indexmenge und wird deshalb über den Knotenauswerter ausgeführt.",
            )
            MengenRechnerOperator.POTENZMENGE -> {
                if (eingaben.size != 1) ungueltigeAnzahl(operator, 1)
                else MengenRechnerErgebnis.Wert(
                    Potenzmenge(eingaben.single().menge),
                    Potenzmenge(eingaben.single().elementUniversum ?: eingaben.single().menge),
                    operator,
                )
            }
            MengenRechnerOperator.BILD,
            MengenRechnerOperator.URBILD,
            -> MengenRechnerErgebnis.Bedingt(
                latex = "\\operatorname{${if (operator == MengenRechnerOperator.BILD) "Bild" else "Urbild"}}" +
                    "\\left(${eingaben.joinToString(",") { it.menge.zuLatex() }}\\right)",
                moeglicheUniversen = universen.toSet(),
                bedingungen = listOf("Eine kompatible Methode mit Definitions- und Zielmengenvertrag muss verbunden sein."),
            )
        }
    }

    fun alsFormelAusdruck(
        id: String,
        operator: MengenRechnerOperator,
        argumente: List<Pair<String, FormelAusdruck>>,
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id = id,
        operatorId = operator.stabileId,
        argumente = argumente.mapIndexed { index, (rolle, ausdruck) ->
            FormelArgument(rolle, index, ausdruck)
        },
        typ = FormelTyp.MENGE,
    )

    private fun binaer(
        eingaben: List<MengenRechnerEingabe>,
        linkeRolle: String,
        rechteRolle: String,
        operation: (MengenRechnerEingabe, MengenRechnerEingabe) -> MengenRechnerErgebnis,
    ): MengenRechnerErgebnis {
        val links = eingaben.singleOrNull { it.rollenId == linkeRolle }
        val rechts = eingaben.singleOrNull { it.rollenId == rechteRolle }
        return if (links == null || rechts == null || eingaben.size != 2) {
            MengenRechnerErgebnis.Ungueltig(
                "argumentrollen",
                "Erwartet werden genau die Rollen '$linkeRolle' und '$rechteRolle'.",
            )
        } else operation(links, rechts)
    }

    private fun bedingt(
        operator: MengenRechnerOperator,
        eingaben: List<MengenRechnerEingabe>,
        universen: List<MengenAusdruck>,
    ) = MengenRechnerErgebnis.Bedingt(
        latex = eingaben.joinToString(
            separator = if (operator == MengenRechnerOperator.SCHNITT) " \\cap " else " \\cup ",
        ) { it.menge.zuLatex() },
        moeglicheUniversen = universen.toSet(),
        bedingungen = listOf("Für die beteiligten Elementuniversen fehlt eine gemeinsame Einbettung."),
    )

    private fun universumsBedingungen(
        links: MengenRechnerEingabe,
        rechts: MengenRechnerEingabe,
    ): List<String> = if (
        links.elementUniversum == null || rechts.elementUniversum == null ||
        links.elementUniversum == rechts.elementUniversum
    ) emptyList() else listOf("Die Elementuniversen müssen kompatibel eingebettet sein.")

    private fun ungueltigeAnzahl(operator: MengenRechnerOperator, erwartet: Int) =
        MengenRechnerErgebnis.Ungueltig(
            "argumentanzahl",
            "${operator.name} benötigt ${if (erwartet == 1) "genau eine" else "mindestens $erwartet"} Menge(n).",
        )
}

object MengenRechnerMigration {
    val alteKnotenArten: Map<String, MengenRechnerOperator> = mapOf(
        "mathematik.vereinigung" to MengenRechnerOperator.VEREINIGUNG,
        "mathematik.schnitt" to MengenRechnerOperator.SCHNITT,
        "mathematik.differenz" to MengenRechnerOperator.DIFFERENZ,
        "mathematik.mengendifferenz" to MengenRechnerOperator.DIFFERENZ,
        "mathematik.symmetrischeDifferenz" to MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ,
        "mathematik.kartesischesProdukt" to MengenRechnerOperator.KARTESISCHES_PRODUKT,
        "mathematik.iteriertesKartesischesProdukt" to MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
        "mathematik.iterierteVereinigung" to MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
        "mathematik.iterierterSchnitt" to MengenRechnerOperator.ITERIERTER_SCHNITT,
        "mathematik.potenzmenge" to MengenRechnerOperator.POTENZMENGE,
    )
}
