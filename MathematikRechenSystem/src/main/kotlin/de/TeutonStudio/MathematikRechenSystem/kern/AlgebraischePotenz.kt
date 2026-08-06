package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface PotenzTraeger {
    val menge: MengenAusdruck

    fun pruefe(
        basis: MathematischesObjekt,
        kontext: RechenKontext = RechenKontext(),
        werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    ): PotenzBasisPruefung

    data class Zahlbereich(
        val bereich: FundamentalerZahlbereich,
    ) : PotenzTraeger {
        override val menge: MengenAusdruck = bereich.alsMenge()

        override fun pruefe(
            basis: MathematischesObjekt,
            kontext: RechenKontext,
            werteVorräte: Map<String, MengenAusdruck>,
        ): PotenzBasisPruefung {
            if (basis !is ZahlAusdruck) {
                return PotenzBasisPruefung.Ungueltig(
                    "Die Struktur ${bereich.latex} akzeptiert nur Zahlenausdrücke.",
                )
            }
            val unbekannteVariablen = basis.enthalteneVariablen()
                .filterNot { it.name in werteVorräte }
            if (unbekannteVariablen.isNotEmpty()) {
                return PotenzBasisPruefung.Bedingt(
                    setOf(ElementBeziehung(basis, menge)),
                )
            }
            val basisMenge = runCatching {
                inferiereZahlenWertevorrat(basis, werteVorräte, kontext.annahmen)
            }.getOrNull() ?: return PotenzBasisPruefung.Bedingt(
                setOf(ElementBeziehung(basis, menge)),
            )
            val basisBereich = basisMenge.fundamentalerZahlbereichOderNull()
            return when {
                basisBereich == null -> PotenzBasisPruefung.Bedingt(setOf(ElementBeziehung(basis, menge)))
                FundamentaleZahlbereiche.istTeilbereich(basisBereich, bereich) -> PotenzBasisPruefung.Gueltig
                else -> PotenzBasisPruefung.Ungueltig(
                    "${basis.zuLatex()} liegt in ${basisBereich.latex}, nicht im Potenzträger ${bereich.latex}.",
                )
            }
        }
    }

    data class Matrixraum(
        val zeilen: Int,
        val spalten: Int,
        val skalarMenge: MengenAusdruck,
    ) : PotenzTraeger {
        init { require(zeilen > 0 && spalten > 0) }
        override val menge: MengenAusdruck = Matrizenraum(zeilen, spalten, skalarMenge)

        override fun pruefe(
            basis: MathematischesObjekt,
            kontext: RechenKontext,
            werteVorräte: Map<String, MengenAusdruck>,
        ): PotenzBasisPruefung {
            val matrix = basis as? Matrix ?: return PotenzBasisPruefung.Ungueltig(
                "Die Struktur ${menge.zuLatex()} akzeptiert nur Matrizen.",
            )
            if (matrix.zeilenAnzahl != zeilen || matrix.spaltenAnzahl != spalten) {
                return PotenzBasisPruefung.Ungueltig(
                    "Die Matrix besitzt Form ${matrix.zeilenAnzahl}×${matrix.spaltenAnzahl}, erwartet wird $zeilen×$spalten.",
                )
            }
            return pruefePotenzMengenEinbettung(matrix.tensorZahlBereich, skalarMenge, kontext)
        }
    }

    data class Explizit(
        override val menge: MengenAusdruck,
    ) : PotenzTraeger {
        override fun pruefe(
            basis: MathematischesObjekt,
            kontext: RechenKontext,
            werteVorräte: Map<String, MengenAusdruck>,
        ): PotenzBasisPruefung {
            val aussage = ElementBeziehung(basis, menge)
            return when (aussage.entscheide(kontext).wahrheitswert) {
                Wahrheitswert.Wahr -> PotenzBasisPruefung.Gueltig
                Wahrheitswert.Lüge -> PotenzBasisPruefung.Ungueltig(
                    "${basis.zuLatex()} liegt nachweislich nicht in ${menge.zuLatex()}.",
                )
                null -> PotenzBasisPruefung.Bedingt(setOf(aussage))
            }
        }
    }
}

sealed interface PotenzBasisPruefung {
    data object Gueltig : PotenzBasisPruefung
    data class Bedingt(val voraussetzungen: Set<Aussage>) : PotenzBasisPruefung
    data class Ungueltig(val grund: String) : PotenzBasisPruefung
}

data class PotenzStruktur(
    val id: String,
    val traeger: PotenzTraeger,
    val multiplikationsOperatorId: String,
    val abgeschlossenheit: NachweisStatus,
    val assoziativitaet: NachweisStatus,
    val neutralesElement: MathematischesObjekt?,
    val neutralitaet: NachweisStatus,
) : MathematischesObjekt {
    init {
        require(id.isNotBlank())
        require(multiplikationsOperatorId.isNotBlank())
    }

    val traegerMenge: MengenAusdruck get() = traeger.menge

    override fun zuLatex(): String =
        "\\left(${traegerMenge.zuLatex()},\\operatorname{${multiplikationsOperatorId.potenzLatexText()}}\\right)"
}

data class AlgebraischePotenz(
    val basis: MathematischesObjekt,
    val ordnung: IterationsOrdnung,
    val struktur: PotenzStruktur,
    val voraussetzungen: Set<Aussage> = emptySet(),
) : MathematischesObjekt {
    val operatorId: String = IterationsArt.MULTIPLIKATION.operatorId

    override fun zuLatex(): String = IterierterAusdruck(
        basis = basis,
        art = IterationsArt.MULTIPLIKATION,
        ordnung = ordnung,
    ).zuLatex()
}

data class PunktweiseMethodenPotenz(
    val methode: Methode,
    val ordnung: IterationsOrdnung,
    val struktur: PotenzStruktur,
    val voraussetzungen: Set<Aussage> = emptySet(),
) : MathematischesObjekt {
    val operatorId: String = "iteration.multiplikation.punktweise"
    override fun zuLatex(): String = "{${methode.name}}^{${ordnung.zuLatex()}}"
}

sealed interface PotenzAuswertung {
    data class Wert(
        val wert: MathematischesObjekt,
        val traeger: MengenAusdruck,
        val voraussetzungen: Set<Aussage> = emptySet(),
    ) : PotenzAuswertung

    data class Symbolisch(val potenz: AlgebraischePotenz) : PotenzAuswertung

    data class Bedingt(
        val potenz: AlgebraischePotenz,
        val voraussetzungen: Set<Aussage>,
    ) : PotenzAuswertung

    data class Ungueltig(
        val code: String,
        val grund: String,
    ) : PotenzAuswertung
}

sealed interface MethodenPotenzAuswertung {
    data class Wert(
        val methode: Methode,
        val voraussetzungen: Set<Aussage> = emptySet(),
    ) : MethodenPotenzAuswertung

    data class Symbolisch(
        val ausdruck: PunktweiseMethodenPotenz,
        val methode: Methode,
    ) : MethodenPotenzAuswertung

    data class Bedingt(
        val ausdruck: PunktweiseMethodenPotenz,
        val methode: Methode,
        val voraussetzungen: Set<Aussage>,
    ) : MethodenPotenzAuswertung

    data class Ungueltig(
        val code: String,
        val grund: String,
    ) : MethodenPotenzAuswertung
}

typealias StrukturMultiplikation = (MathematischesObjekt, MathematischesObjekt) -> MathematischesObjekt

private fun String.potenzLatexText(): String =
    replace("\\", "").replace("_", "\\_").replace(".", "\\mathord{.}")
