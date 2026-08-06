package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

sealed interface PotenzTraeger {
    val menge: MengenAusdruck
    fun pruefe(basis: MathematischesObjekt, kontext: RechenKontext = RechenKontext()): PotenzBasisPruefung

    data class Zahlbereich(
        val bereich: FundamentalerZahlbereich,
    ) : PotenzTraeger {
        override val menge: MengenAusdruck = bereich.alsMenge()

        override fun pruefe(
            basis: MathematischesObjekt,
            kontext: RechenKontext,
        ): PotenzBasisPruefung {
            if (basis !is ZahlAusdruck) {
                return PotenzBasisPruefung.Ungueltig(
                    "Die Struktur ${bereich.latex} akzeptiert nur Zahlenausdrücke.",
                )
            }
            val basisMenge = runCatching { inferiereZahlenWertevorrat(basis) }.getOrNull()
                ?: return PotenzBasisPruefung.Bedingt(
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
        ): PotenzBasisPruefung {
            val matrix = basis as? Matrix ?: return PotenzBasisPruefung.Ungueltig(
                "Die Struktur ${menge.zuLatex()} akzeptiert nur Matrizen.",
            )
            if (matrix.zeilenAnzahl != zeilen || matrix.spaltenAnzahl != spalten) {
                return PotenzBasisPruefung.Ungueltig(
                    "Die Matrix besitzt Form ${matrix.zeilenAnzahl}×${matrix.spaltenAnzahl}, erwartet wird $zeilen×$spalten.",
                )
            }
            return pruefeMengenEinbettung(matrix.tensorZahlBereich, skalarMenge, kontext)
        }
    }

    data class Explizit(
        override val menge: MengenAusdruck,
    ) : PotenzTraeger {
        override fun pruefe(
            basis: MathematischesObjekt,
            kontext: RechenKontext,
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
) {
    init {
        require(id.isNotBlank())
        require(multiplikationsOperatorId.isNotBlank())
    }

    val traegerMenge: MengenAusdruck get() = traeger.menge
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

fun werteNatuerlichePotenzAus(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
    kontext: RechenKontext = RechenKontext(),
): PotenzAuswertung {
    val basisVoraussetzungen = when (val pruefung = struktur.traeger.pruefe(basis, kontext)) {
        PotenzBasisPruefung.Gueltig -> emptySet()
        is PotenzBasisPruefung.Bedingt -> pruefung.voraussetzungen
        is PotenzBasisPruefung.Ungueltig -> return PotenzAuswertung.Ungueltig(
            code = "basis_nicht_im_traeger",
            grund = pruefung.grund,
        )
    }

    return when (ordnung) {
        is IterationsOrdnung.Symbolisch -> werteSymbolischePotenzAus(
            basis,
            ordnung,
            struktur,
            basisVoraussetzungen,
        )
        is IterationsOrdnung.Konkret -> werteKonkretePotenzAus(
            basis,
            ordnung.wert,
            struktur,
            multiplikation,
            basisVoraussetzungen,
        )
    }
}

fun wertePunktweiseMethodenPotenzAus(
    methode: Methode,
    ordnung: IterationsOrdnung,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
    kontext: RechenKontext = RechenKontext(),
): MethodenPotenzAuswertung {
    val zielBeziehung = TeilmengenBeziehung(methode.zielMenge, struktur.traegerMenge)
    val zielVoraussetzungen = when (zielBeziehung.entscheide(kontext).wahrheitswert) {
        Wahrheitswert.Wahr -> emptySet()
        Wahrheitswert.Lüge -> return MethodenPotenzAuswertung.Ungueltig(
            code = "zielmenge_nicht_im_traeger",
            grund = "Die Zielmenge ${methode.zielMenge.zuLatex()} liegt nicht im Potenzträger ${struktur.traegerMenge.zuLatex()}.",
        )
        null -> setOf(zielBeziehung)
    }

    val punktweise = werteNatuerlichePotenzAus(
        basis = methode.vorschrift,
        ordnung = ordnung,
        struktur = struktur,
        multiplikation = multiplikation,
        kontext = kontext,
    )
    val name = PunktweiseMethodenPotenz(methode, ordnung, struktur).zuLatex()
    return when (punktweise) {
        is PotenzAuswertung.Wert -> MethodenPotenzAuswertung.Wert(
            methode = methode.copy(
                name = name,
                vorschrift = punktweise.wert,
            ),
            voraussetzungen = zielVoraussetzungen + punktweise.voraussetzungen,
        )
        is PotenzAuswertung.Symbolisch -> {
            val ausdruck = PunktweiseMethodenPotenz(
                methode,
                ordnung,
                struktur,
                zielVoraussetzungen + punktweise.potenz.voraussetzungen,
            )
            MethodenPotenzAuswertung.Symbolisch(
                ausdruck = ausdruck,
                methode = methode.copy(
                    name = name,
                    vorschrift = punktweise.potenz,
                ),
            )
        }
        is PotenzAuswertung.Bedingt -> {
            val voraussetzungen = zielVoraussetzungen + punktweise.voraussetzungen
            val ausdruck = PunktweiseMethodenPotenz(methode, ordnung, struktur, voraussetzungen)
            MethodenPotenzAuswertung.Bedingt(
                ausdruck = ausdruck,
                methode = methode.copy(
                    name = name,
                    vorschrift = punktweise.potenz.copy(voraussetzungen = voraussetzungen),
                ),
                voraussetzungen = voraussetzungen,
            )
        }
        is PotenzAuswertung.Ungueltig -> MethodenPotenzAuswertung.Ungueltig(
            punktweise.code,
            punktweise.grund,
        )
    }
}

private fun werteKonkretePotenzAus(
    basis: MathematischesObjekt,
    ordnung: BigInteger,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
    basisVoraussetzungen: Set<Aussage>,
): PotenzAuswertung {
    if (ordnung == BigInteger.ONE) {
        return PotenzAuswertung.Wert(
            wert = basis,
            traeger = struktur.traegerMenge,
            voraussetzungen = basisVoraussetzungen,
        )
    }

    if (ordnung == BigInteger.ZERO) {
        val neutral = struktur.neutralesElement
            ?: return PotenzAuswertung.Ungueltig(
                "neutrales_element_fehlt",
                "Die nullte Potenz benötigt ein neutrales Element der konkreten Struktur.",
            )
        if (struktur.neutralitaet == NachweisStatus.Widerlegt) {
            return PotenzAuswertung.Ungueltig(
                "neutralitaet_widerlegt",
                "Das angegebene Element ${neutral.zuLatex()} ist nachweislich nicht neutral.",
            )
        }
        val voraussetzungen = basisVoraussetzungen + struktur.neutralitaet.offeneVoraussetzungen(
            "${neutral.zuLatex()} ist das beidseitig neutrale Element von ${struktur.traegerMenge.zuLatex()}.",
            "Potenzstruktur ${struktur.id}",
        )
        return if (voraussetzungen.isEmpty()) {
            PotenzAuswertung.Wert(neutral, struktur.traegerMenge)
        } else {
            PotenzAuswertung.Bedingt(
                AlgebraischePotenz(
                    basis,
                    IterationsOrdnung.Konkret(ordnung),
                    struktur,
                    voraussetzungen,
                ),
                voraussetzungen,
            )
        }
    }

    if (struktur.abgeschlossenheit == NachweisStatus.Widerlegt) {
        return PotenzAuswertung.Ungueltig(
            "abgeschlossenheit_widerlegt",
            "Die Multiplikation ist auf ${struktur.traegerMenge.zuLatex()} nicht abgeschlossen.",
        )
    }
    if (struktur.assoziativitaet == NachweisStatus.Widerlegt) {
        return PotenzAuswertung.Ungueltig(
            "assoziativitaet_widerlegt",
            "Klammerungsfreie natürliche Potenzen benötigen eine assoziative Multiplikation.",
        )
    }

    val voraussetzungen = basisVoraussetzungen +
        struktur.abgeschlossenheit.offeneVoraussetzungen(
            "Die Multiplikation ist auf ${struktur.traegerMenge.zuLatex()} abgeschlossen.",
            "Potenzstruktur ${struktur.id}",
        ) +
        struktur.assoziativitaet.offeneVoraussetzungen(
            "Die Multiplikation auf ${struktur.traegerMenge.zuLatex()} ist assoziativ.",
            "Potenzstruktur ${struktur.id}",
        )

    if (voraussetzungen.isNotEmpty()) {
        return PotenzAuswertung.Bedingt(
            AlgebraischePotenz(
                basis,
                IterationsOrdnung.Konkret(ordnung),
                struktur,
                voraussetzungen,
            ),
            voraussetzungen,
        )
    }

    return runCatching {
        potenziereDurchQuadrieren(basis, ordnung, multiplikation)
    }.fold(
        onSuccess = { PotenzAuswertung.Wert(it, struktur.traegerMenge) },
        onFailure = { fehler ->
            PotenzAuswertung.Ungueltig(
                "multiplikation_nicht_auswertbar",
                fehler.message ?: "Die registrierte Multiplikation konnte nicht ausgewertet werden.",
            )
        },
    )
}

private fun werteSymbolischePotenzAus(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung.Symbolisch,
    struktur: PotenzStruktur,
    basisVoraussetzungen: Set<Aussage>,
): PotenzAuswertung {
    val neutralZulaessig = struktur.neutralesElement != null && struktur.neutralitaet != NachweisStatus.Widerlegt
    val positiveIterationZulaessig = struktur.abgeschlossenheit != NachweisStatus.Widerlegt &&
        struktur.assoziativitaet != NachweisStatus.Widerlegt
    val ordnungsEinschraenkung = when {
        neutralZulaessig && positiveIterationZulaessig -> emptySet()
        neutralZulaessig -> setOf(
            UnentscheidbareAussage(
                "${ordnung.zuLatex()}\\in\\{0,1\\}",
                "Potenzstruktur ${struktur.id}",
            ),
        )
        positiveIterationZulaessig -> setOf(
            Vergleich(ordnung.ausdruck, VergleichsArt.GrößerGleich, RationaleZahl.Eins),
        )
        else -> setOf(Gleichheit(ordnung.ausdruck, RationaleZahl.Eins))
    }

    val voraussetzungen = linkedSetOf<Aussage>().apply {
        addAll(basisVoraussetzungen)
        addAll(ordnung.annahmen)
        addAll(ordnungsEinschraenkung)
        if (positiveIterationZulaessig) {
            addAll(
                struktur.abgeschlossenheit.offeneVoraussetzungen(
                    "Die Multiplikation ist auf ${struktur.traegerMenge.zuLatex()} abgeschlossen.",
                    "Potenzstruktur ${struktur.id}",
                ),
            )
            addAll(
                struktur.assoziativitaet.offeneVoraussetzungen(
                    "Die Multiplikation auf ${struktur.traegerMenge.zuLatex()} ist assoziativ.",
                    "Potenzstruktur ${struktur.id}",
                ),
            )
        }
        if (neutralZulaessig) {
            val neutral = requireNotNull(struktur.neutralesElement)
            addAll(
                struktur.neutralitaet.offeneVoraussetzungen(
                    "${neutral.zuLatex()} ist das neutrale Element.",
                    "Potenzstruktur ${struktur.id}",
                ),
            )
        }
    }
    val potenz = AlgebraischePotenz(basis, ordnung, struktur, voraussetzungen)
    return if (voraussetzungen.isEmpty()) {
        PotenzAuswertung.Symbolisch(potenz)
    } else {
        PotenzAuswertung.Bedingt(potenz, voraussetzungen)
    }
}

private fun potenziereDurchQuadrieren(
    basis: MathematischesObjekt,
    ordnung: BigInteger,
    multiplikation: StrukturMultiplikation,
): MathematischesObjekt {
    require(ordnung > BigInteger.ONE)
    var exponent = ordnung
    var faktor = basis
    var ergebnis: MathematischesObjekt? = null

    while (exponent > BigInteger.ZERO) {
        if (exponent.testBit(0)) {
            ergebnis = if (ergebnis == null) faktor else multiplikation(ergebnis, faktor)
        }
        exponent = exponent.shiftRight(1)
        if (exponent > BigInteger.ZERO) faktor = multiplikation(faktor, faktor)
    }
    return requireNotNull(ergebnis)
}

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
        val neutral = if (quadratisch) einheitsMatrixFuerPotenz(matrix.zeilenAnzahl) else null
        return PotenzStruktur(
            id = "potenz.matrix.${matrix.zeilenAnzahl}x${matrix.spaltenAnzahl}.${skalarMenge.zuLatex()}",
            traeger = PotenzTraeger.Matrixraum(
                matrix.zeilenAnzahl,
                matrix.spaltenAnzahl,
                skalarMenge,
            ),
            multiplikationsOperatorId = MatrixRechnerOperator.MATRIXPRODUKT.stabileId,
            abgeschlossenheit = if (quadratisch) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
            assoziativitaet = if (quadratisch) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
            neutralesElement = neutral,
            neutralitaet = if (quadratisch) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
        )
    }

    fun aufloesen(basis: MathematischesObjekt): PotenzStrukturAufloesung {
        return when (basis) {
            is ZahlAusdruck -> {
                val menge = runCatching { inferiereZahlenWertevorrat(basis) }.getOrNull()
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

private fun NachweisStatus.offeneVoraussetzungen(
    latex: String,
    system: String,
): Set<Aussage> = when (this) {
    NachweisStatus.Nachgewiesen -> emptySet()
    NachweisStatus.Widerlegt -> emptySet()
    is NachweisStatus.Bedingt -> bedingungen.toSet()
    NachweisStatus.Unvollstaendig,
    NachweisStatus.Unentscheidbar,
    -> setOf(UnentscheidbareAussage(latex, system))
}

private fun pruefeMengenEinbettung(
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
