package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

fun werteNatuerlichePotenzAus(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
    kontext: RechenKontext = RechenKontext(),
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
): PotenzAuswertung {
    val basisVoraussetzungen = when (
        val pruefung = struktur.traeger.pruefe(basis, kontext, werteVorräte)
    ) {
        PotenzBasisPruefung.Gueltig -> emptySet()
        is PotenzBasisPruefung.Bedingt -> pruefung.voraussetzungen
        is PotenzBasisPruefung.Ungueltig -> return PotenzAuswertung.Ungueltig(
            code = "basis_nicht_im_traeger",
            grund = pruefung.grund,
        )
    }
    return werteNatuerlichePotenzNachBasisPruefung(
        basis,
        ordnung,
        struktur,
        multiplikation,
        basisVoraussetzungen,
    )
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

    /*
     * Die Methodensignatur garantiert bereits, dass jeder Vorschriftswert im
     * deklarierten Ziel liegt. Der nackte Term darf deshalb nicht erneut ohne
     * die Variablenbereiche der Methode inferiert werden.
     */
    val punktweise = werteNatuerlichePotenzNachBasisPruefung(
        basis = methode.vorschrift,
        ordnung = ordnung,
        struktur = struktur,
        multiplikation = multiplikation,
        basisVoraussetzungen = zielVoraussetzungen,
    )
    val name = PunktweiseMethodenPotenz(methode, ordnung, struktur).zuLatex()
    return when (punktweise) {
        is PotenzAuswertung.Wert -> MethodenPotenzAuswertung.Wert(
            methode = methode.copy(
                name = name,
                vorschrift = punktweise.wert,
            ),
            voraussetzungen = punktweise.voraussetzungen,
        )
        is PotenzAuswertung.Symbolisch -> {
            val ausdruck = PunktweiseMethodenPotenz(
                methode,
                ordnung,
                struktur,
                punktweise.potenz.voraussetzungen,
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
            val ausdruck = PunktweiseMethodenPotenz(
                methode,
                ordnung,
                struktur,
                punktweise.voraussetzungen,
            )
            MethodenPotenzAuswertung.Bedingt(
                ausdruck = ausdruck,
                methode = methode.copy(
                    name = name,
                    vorschrift = punktweise.potenz.copy(
                        voraussetzungen = punktweise.voraussetzungen,
                    ),
                ),
                voraussetzungen = punktweise.voraussetzungen,
            )
        }
        is PotenzAuswertung.Ungueltig -> MethodenPotenzAuswertung.Ungueltig(
            punktweise.code,
            punktweise.grund,
        )
    }
}

private fun werteNatuerlichePotenzNachBasisPruefung(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
    basisVoraussetzungen: Set<Aussage>,
): PotenzAuswertung = when (ordnung) {
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
        val voraussetzungen = basisVoraussetzungen + struktur.neutralitaet.offenePotenzVoraussetzungen(
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
        struktur.abgeschlossenheit.offenePotenzVoraussetzungen(
            "Die Multiplikation ist auf ${struktur.traegerMenge.zuLatex()} abgeschlossen.",
            "Potenzstruktur ${struktur.id}",
        ) +
        struktur.assoziativitaet.offenePotenzVoraussetzungen(
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
    val neutralZulaessig = struktur.neutralesElement != null &&
        struktur.neutralitaet != NachweisStatus.Widerlegt
    val positiveIterationZulaessig = struktur.abgeschlossenheit != NachweisStatus.Widerlegt &&
        struktur.assoziativitaet != NachweisStatus.Widerlegt
    val ordnungsEinschraenkung: Set<Aussage> = when {
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
                struktur.abgeschlossenheit.offenePotenzVoraussetzungen(
                    "Die Multiplikation ist auf ${struktur.traegerMenge.zuLatex()} abgeschlossen.",
                    "Potenzstruktur ${struktur.id}",
                ),
            )
            addAll(
                struktur.assoziativitaet.offenePotenzVoraussetzungen(
                    "Die Multiplikation auf ${struktur.traegerMenge.zuLatex()} ist assoziativ.",
                    "Potenzstruktur ${struktur.id}",
                ),
            )
        }
        if (neutralZulaessig) {
            val neutral = requireNotNull(struktur.neutralesElement)
            addAll(
                struktur.neutralitaet.offenePotenzVoraussetzungen(
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
        if (exponent > BigInteger.ZERO) {
            faktor = multiplikation(faktor, faktor)
        }
    }
    return requireNotNull(ergebnis)
}

private fun NachweisStatus.offenePotenzVoraussetzungen(
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
