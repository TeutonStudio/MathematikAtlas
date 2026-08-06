package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface PotenzStrukturAufloesung {
    data class Gefunden(
        val struktur: PotenzStruktur,
        val multiplikation: StrukturMultiplikation,
    ) : PotenzStrukturAufloesung

    data class NichtEindeutig(
        val grund: String,
        val vorgeschlageneOperatorIds: List<String>,
    ) : PotenzStrukturAufloesung

    data class NichtVorhanden(val grund: String) : PotenzStrukturAufloesung
}

object StandardPotenzStrukturen {
    fun zahlbereich(bereich: ZahlbereichsId): PotenzStruktur {
        val fundamental = FundamentalerZahlbereich.entries.singleOrNull { it.id == bereich.wert }
            ?: error("Für den Zahlbereich ${bereich.wert} ist keine fundamentale Potenzstruktur registriert.")
        return zahlbereich(fundamental)
    }

    fun zahlbereich(bereich: FundamentalerZahlbereich): PotenzStruktur = PotenzStruktur(
        id = "potenz.zahlbereich.${bereich.id}",
        traeger = PotenzTraeger.Zahlbereich(bereich),
        multiplikationsOperatorId = "arithmetik.multiplikation",
        abgeschlossenheit = NachweisStatus.Nachgewiesen,
        assoziativitaet = NachweisStatus.Nachgewiesen,
        neutralesElement = RationaleZahl.Eins,
        neutralitaet = NachweisStatus.Nachgewiesen,
    )

    fun matrix(matrix: Matrix): PotenzStruktur {
        val quadratisch = matrix.zeilenAnzahl == matrix.spaltenAnzahl
        val skalarMenge = matrix.tensorZahlBereich
        return PotenzStruktur(
            id = "potenz.matrix.${matrix.zeilenAnzahl}x${matrix.spaltenAnzahl}.${skalarMenge.zuLatex()}",
            traeger = PotenzTraeger.Matrixraum(
                matrix.zeilenAnzahl,
                matrix.spaltenAnzahl,
                skalarMenge,
            ),
            multiplikationsOperatorId = MatrixRechnerOperator.MATRIXPRODUKT.stabileId,
            abgeschlossenheit = if (quadratisch) {
                NachweisStatus.Nachgewiesen
            } else {
                NachweisStatus.Widerlegt
            },
            assoziativitaet = if (quadratisch) {
                NachweisStatus.Nachgewiesen
            } else {
                NachweisStatus.Widerlegt
            },
            neutralesElement = if (quadratisch) {
                einheitsMatrixFuerPotenz(matrix.zeilenAnzahl)
            } else {
                null
            },
            neutralitaet = if (quadratisch) {
                NachweisStatus.Nachgewiesen
            } else {
                NachweisStatus.Widerlegt
            },
        )
    }

    fun aufloesen(
        basis: MathematischesObjekt,
        werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
        annahmen: Set<Aussage> = emptySet(),
    ): PotenzStrukturAufloesung = when (basis) {
        is ZahlAusdruck -> {
            val menge = runCatching {
                inferiereZahlenWertevorrat(basis, werteVorräte, annahmen)
            }.getOrNull()
            val bereich = menge?.fundamentalerZahlbereichOderNull()
            if (bereich == null) {
                PotenzStrukturAufloesung.NichtVorhanden(
                    "Der Zahlbereich von ${basis.zuLatex()} ist nicht eindeutig bestimmbar.",
                )
            } else {
                PotenzStrukturAufloesung.Gefunden(
                    zahlbereich(bereich),
                    standardZahlMultiplikation,
                )
            }
        }
        is Matrix -> PotenzStrukturAufloesung.Gefunden(
            matrix(basis),
            standardMatrixMultiplikation,
        )
        is Tupel -> PotenzStrukturAufloesung.NichtEindeutig(
            "Ein Tupel besitzt keine kanonische innere Multiplikation.",
            listOf("produkt.hadamard", "produkt.komponentenweise", "produkt.explizit"),
        )
        is Tensorartig -> PotenzStrukturAufloesung.NichtEindeutig(
            "Vektoren und Tensoren benötigen einen ausdrücklich gewählten inneren Produktvertrag.",
            listOf("produkt.hadamard", "produkt.kontrahiert", "produkt.explizit"),
        )
        is Methode -> PotenzStrukturAufloesung.NichtVorhanden(
            "Methodenpotenzen werden punktweise über die Struktur ihrer Zielwerte aufgelöst.",
        )
        else -> PotenzStrukturAufloesung.NichtVorhanden(
            "Für ${basis::class.simpleName} ist keine Standard-Potenzstruktur registriert.",
        )
    }
}

val standardZahlMultiplikation: StrukturMultiplikation = { links, rechts ->
    require(links is ZahlAusdruck && rechts is ZahlAusdruck) {
        "Die Standard-Zahlmultiplikation akzeptiert nur Zahlenausdrücke."
    }
    multiplikation(links, rechts)
}

val standardMatrixMultiplikation: StrukturMultiplikation = { links, rechts ->
    require(links is Matrix && rechts is Matrix) {
        "Die Standard-Matrixmultiplikation akzeptiert nur Matrizen."
    }
    links * rechts
}

fun einheitsMatrixFuerPotenz(dimension: Int): Matrix {
    require(dimension > 0)
    return Matrix(
        List(dimension) { zeile ->
            List(dimension) { spalte ->
                if (zeile == spalte) RationaleZahl.Eins else RationaleZahl.Null
            }
        },
    )
}

internal fun pruefePotenzMengenEinbettung(
    teil: MengenAusdruck,
    ober: MengenAusdruck,
    kontext: RechenKontext,
): PotenzBasisPruefung {
    if (teil == ober) return PotenzBasisPruefung.Gueltig
    val teilBereich = teil.fundamentalerZahlbereichOderNull()
    val oberBereich = ober.fundamentalerZahlbereichOderNull()
    if (teilBereich != null && oberBereich != null) {
        return if (FundamentaleZahlbereiche.istTeilbereich(teilBereich, oberBereich)) {
            PotenzBasisPruefung.Gueltig
        } else {
            PotenzBasisPruefung.Ungueltig(
                "${teil.zuLatex()} ist kein Teilbereich von ${ober.zuLatex()}.",
            )
        }
    }
    val aussage = TeilmengenBeziehung(teil, ober)
    return when (aussage.entscheide(kontext).wahrheitswert) {
        Wahrheitswert.Wahr -> PotenzBasisPruefung.Gueltig
        Wahrheitswert.Lüge -> PotenzBasisPruefung.Ungueltig(
            "${teil.zuLatex()} ist nachweislich keine Teilmenge von ${ober.zuLatex()}.",
        )
        null -> PotenzBasisPruefung.Bedingt(setOf(aussage))
    }
}
