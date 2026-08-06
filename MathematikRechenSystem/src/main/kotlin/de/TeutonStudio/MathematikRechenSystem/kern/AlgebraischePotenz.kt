package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

data class PotenzStruktur(
    val id: String,
    val traeger: ZahlbereichsId?,
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
}

data class AlgebraischePotenz(
    val basis: MathematischesObjekt,
    val ordnung: IterationsOrdnung,
    val struktur: PotenzStruktur,
    val voraussetzungen: Set<String> = emptySet(),
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
) : MathematischesObjekt {
    val operatorId: String = "iteration.multiplikation.punktweise"
    override fun zuLatex(): String = "{${methode.name}}^{${ordnung.zuLatex()}}"
}

sealed interface PotenzAuswertung {
    data class Wert(val wert: MathematischesObjekt) : PotenzAuswertung
    data class Symbolisch(val potenz: AlgebraischePotenz) : PotenzAuswertung
    data class Bedingt(
        val potenz: AlgebraischePotenz,
        val voraussetzungen: Set<String>,
    ) : PotenzAuswertung
    data class Ungueltig(val grund: String) : PotenzAuswertung
}

typealias StrukturMultiplikation = (MathematischesObjekt, MathematischesObjekt) -> MathematischesObjekt

fun werteNatuerlichePotenzAus(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
): PotenzAuswertung {
    if (struktur.abgeschlossenheit == NachweisStatus.Widerlegt) {
        return PotenzAuswertung.Ungueltig("Die Multiplikation ist auf der gewählten Trägermenge nicht abgeschlossen.")
    }

    return when (ordnung) {
        is IterationsOrdnung.Symbolisch -> symbolischeOderBedingtePotenz(basis, ordnung, struktur)
        is IterationsOrdnung.Konkret -> werteKonkretePotenzAus(basis, ordnung.wert, struktur, multiplikation)
    }
}

private fun werteKonkretePotenzAus(
    basis: MathematischesObjekt,
    ordnung: BigInteger,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
): PotenzAuswertung {
    if (ordnung == BigInteger.ZERO) {
        val neutral = struktur.neutralesElement
            ?: return PotenzAuswertung.Ungueltig("Die nullte Potenz benötigt ein neutrales Element.")
        return when (struktur.neutralitaet) {
            NachweisStatus.Nachgewiesen -> PotenzAuswertung.Wert(neutral)
            NachweisStatus.Widerlegt -> PotenzAuswertung.Ungueltig("Das angegebene Element ist nicht neutral.")
            else -> PotenzAuswertung.Bedingt(
                AlgebraischePotenz(basis, IterationsOrdnung.Konkret(ordnung), struktur),
                setOf("Die Neutralität von ${neutral.zuLatex()} muss nachgewiesen werden."),
            )
        }
    }

    if (ordnung == BigInteger.ONE) return PotenzAuswertung.Wert(basis)

    when (struktur.assoziativitaet) {
        NachweisStatus.Widerlegt -> return PotenzAuswertung.Ungueltig(
            "Klammerungsfreie natürliche Potenzen benötigen eine assoziative Multiplikation.",
        )
        NachweisStatus.Nachgewiesen -> Unit
        else -> return PotenzAuswertung.Bedingt(
            AlgebraischePotenz(basis, IterationsOrdnung.Konkret(ordnung), struktur),
            setOf("Die Assoziativität der Multiplikation muss nachgewiesen werden."),
        )
    }

    if (struktur.abgeschlossenheit != NachweisStatus.Nachgewiesen) {
        return PotenzAuswertung.Bedingt(
            AlgebraischePotenz(basis, IterationsOrdnung.Konkret(ordnung), struktur),
            setOf("Die Abgeschlossenheit der Multiplikation muss nachgewiesen werden."),
        )
    }

    return PotenzAuswertung.Wert(
        potenziereDurchQuadrieren(basis, ordnung, struktur, multiplikation),
    )
}

private fun symbolischeOderBedingtePotenz(
    basis: MathematischesObjekt,
    ordnung: IterationsOrdnung.Symbolisch,
    struktur: PotenzStruktur,
): PotenzAuswertung {
    val potenz = AlgebraischePotenz(basis, ordnung, struktur)
    val voraussetzungen = linkedSetOf<String>()
    if (struktur.abgeschlossenheit != NachweisStatus.Nachgewiesen) {
        voraussetzungen += "Die Multiplikation muss auf der Trägermenge abgeschlossen sein."
    }
    if (struktur.assoziativitaet != NachweisStatus.Nachgewiesen) {
        voraussetzungen += "Die Multiplikation muss assoziativ sein."
    }
    if (struktur.neutralesElement == null || struktur.neutralitaet != NachweisStatus.Nachgewiesen) {
        voraussetzungen += "Für den möglichen Nullfall muss ein neutrales Element nachgewiesen sein."
    }
    return if (voraussetzungen.isEmpty()) {
        PotenzAuswertung.Symbolisch(potenz)
    } else {
        PotenzAuswertung.Bedingt(potenz, voraussetzungen)
    }
}

private fun potenziereDurchQuadrieren(
    basis: MathematischesObjekt,
    ordnung: BigInteger,
    struktur: PotenzStruktur,
    multiplikation: StrukturMultiplikation,
): MathematischesObjekt {
    require(ordnung > BigInteger.ZERO)
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

object StandardPotenzStrukturen {
    fun zahlbereich(bereich: ZahlbereichsId): PotenzStruktur = PotenzStruktur(
        id = "potenz.zahlbereich.${bereich.wert}",
        traeger = bereich,
        multiplikationsOperatorId = "arithmetik.multiplikation",
        abgeschlossenheit = NachweisStatus.Nachgewiesen,
        assoziativitaet = NachweisStatus.Nachgewiesen,
        neutralesElement = RationaleZahl.Eins,
        neutralitaet = NachweisStatus.Nachgewiesen,
    )
}

val standardZahlMultiplikation: StrukturMultiplikation = { links, rechts ->
    require(links is ZahlAusdruck && rechts is ZahlAusdruck) {
        "Die Standard-Zahlmultiplikation akzeptiert nur Zahlenausdrücke."
    }
    multiplikation(links, rechts)
}
