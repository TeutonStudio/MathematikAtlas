package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface PotenzDienstErgebnis {
    val voraussetzungen: Set<Aussage>

    data class ObjektWert(
        val wert: MathematischesObjekt,
        val traeger: MengenAusdruck,
        override val voraussetzungen: Set<Aussage> = emptySet(),
        val strukturId: String,
    ) : PotenzDienstErgebnis

    data class MethodenWert(
        val methode: Methode,
        override val voraussetzungen: Set<Aussage> = emptySet(),
        val strukturId: String,
    ) : PotenzDienstErgebnis

    data class Symbolisch(
        val wert: MathematischesObjekt,
        override val voraussetzungen: Set<Aussage>,
        val strukturId: String,
    ) : PotenzDienstErgebnis

    data class Ungueltig(
        val code: String,
        val grund: String,
    ) : PotenzDienstErgebnis {
        override val voraussetzungen: Set<Aussage> = emptySet()
    }
}

object PotenzDienst {
    fun werteAus(
        basis: MathematischesObjekt,
        ordnung: IterationsOrdnung,
        expliziteStruktur: PotenzStruktur? = null,
        kontext: RechenKontext = RechenKontext(),
        werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    ): PotenzDienstErgebnis {
        val aufloesung = expliziteStruktur?.let(::loeseExpliziteStrukturAuf)
            ?: loeseAutomatischAuf(basis, werteVorräte, kontext.annahmen)
        if (aufloesung !is PotenzStrukturAufloesung.Gefunden) {
            return aufloesung.alsDienstFehler()
        }
        return if (basis is Methode) {
            werteMethodenPotenzAus(basis, ordnung, aufloesung, kontext)
        } else {
            werteObjektPotenzAus(
                basis,
                ordnung,
                aufloesung,
                kontext,
                werteVorräte,
            )
        }
    }

    private fun loeseAutomatischAuf(
        basis: MathematischesObjekt,
        werteVorräte: Map<String, MengenAusdruck>,
        annahmen: Set<Aussage>,
    ): PotenzStrukturAufloesung {
        if (basis !is Methode) {
            return StandardPotenzStrukturen.aufloesen(basis, werteVorräte, annahmen)
        }
        val zielBereich = basis.zielMenge.fundamentalerZahlbereichOderNull()
        if (zielBereich != null) {
            return PotenzStrukturAufloesung.Gefunden(
                StandardPotenzStrukturen.zahlbereich(zielBereich),
                standardZahlMultiplikation,
            )
        }
        val matrix = basis.vorschrift as? Matrix
        if (matrix != null) {
            return PotenzStrukturAufloesung.Gefunden(
                StandardPotenzStrukturen.matrix(matrix),
                standardMatrixMultiplikation,
            )
        }
        return PotenzStrukturAufloesung.NichtVorhanden(
            "Für die Zielmenge ${basis.zielMenge.zuLatex()} ist keine eindeutige punktweise Potenzstruktur registriert.",
        )
    }

    private fun loeseExpliziteStrukturAuf(
        struktur: PotenzStruktur,
    ): PotenzStrukturAufloesung {
        struktur.multiplikationsMethode?.let { methode ->
            return PotenzStrukturAufloesung.Gefunden(
                struktur = struktur,
                multiplikation = { links, rechts -> methode.wendeAn(listOf(links, rechts)) },
            )
        }
        return when (struktur.multiplikationsOperatorId) {
            "arithmetik.multiplikation" -> PotenzStrukturAufloesung.Gefunden(
                struktur,
                standardZahlMultiplikation,
            )
            MatrixRechnerOperator.MATRIXPRODUKT.stabileId -> PotenzStrukturAufloesung.Gefunden(
                struktur,
                standardMatrixMultiplikation,
            )
            else -> PotenzStrukturAufloesung.NichtVorhanden(
                "Für den Multiplikationsoperator '${struktur.multiplikationsOperatorId}' ist noch keine Laufzeitimplementierung registriert.",
            )
        }
    }

    private fun werteObjektPotenzAus(
        basis: MathematischesObjekt,
        ordnung: IterationsOrdnung,
        aufloesung: PotenzStrukturAufloesung.Gefunden,
        kontext: RechenKontext,
        werteVorräte: Map<String, MengenAusdruck>,
    ): PotenzDienstErgebnis = when (
        val ergebnis = werteNatuerlichePotenzAus(
            basis,
            ordnung,
            aufloesung.struktur,
            aufloesung.multiplikation,
            kontext,
            werteVorräte,
        )
    ) {
        is PotenzAuswertung.Wert -> PotenzDienstErgebnis.ObjektWert(
            wert = ergebnis.wert,
            traeger = ergebnis.traeger,
            voraussetzungen = ergebnis.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is PotenzAuswertung.Symbolisch -> PotenzDienstErgebnis.Symbolisch(
            wert = ergebnis.potenz,
            voraussetzungen = ergebnis.potenz.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is PotenzAuswertung.Bedingt -> PotenzDienstErgebnis.Symbolisch(
            wert = ergebnis.potenz,
            voraussetzungen = ergebnis.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is PotenzAuswertung.Ungueltig -> PotenzDienstErgebnis.Ungueltig(
            ergebnis.code,
            ergebnis.grund,
        )
    }

    private fun werteMethodenPotenzAus(
        methode: Methode,
        ordnung: IterationsOrdnung,
        aufloesung: PotenzStrukturAufloesung.Gefunden,
        kontext: RechenKontext,
    ): PotenzDienstErgebnis = when (
        val ergebnis = wertePunktweiseMethodenPotenzAus(
            methode,
            ordnung,
            aufloesung.struktur,
            aufloesung.multiplikation,
            kontext,
        )
    ) {
        is MethodenPotenzAuswertung.Wert -> PotenzDienstErgebnis.MethodenWert(
            methode = ergebnis.methode,
            voraussetzungen = ergebnis.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is MethodenPotenzAuswertung.Symbolisch -> PotenzDienstErgebnis.Symbolisch(
            wert = ergebnis.methode,
            voraussetzungen = ergebnis.ausdruck.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is MethodenPotenzAuswertung.Bedingt -> PotenzDienstErgebnis.Symbolisch(
            wert = ergebnis.methode,
            voraussetzungen = ergebnis.voraussetzungen,
            strukturId = aufloesung.struktur.id,
        )
        is MethodenPotenzAuswertung.Ungueltig -> PotenzDienstErgebnis.Ungueltig(
            ergebnis.code,
            ergebnis.grund,
        )
    }
}

private fun PotenzStrukturAufloesung.alsDienstFehler(): PotenzDienstErgebnis.Ungueltig = when (this) {
    is PotenzStrukturAufloesung.Gefunden -> error("Eine gefundene Struktur ist kein Fehler.")
    is PotenzStrukturAufloesung.NichtEindeutig -> PotenzDienstErgebnis.Ungueltig(
        code = "potenzstruktur_nicht_eindeutig",
        grund = "$grund Verfügbare explizite Produktarten: ${vorgeschlageneOperatorIds.joinToString()}.",
    )
    is PotenzStrukturAufloesung.NichtVorhanden -> PotenzDienstErgebnis.Ungueltig(
        code = "potenzstruktur_fehlt",
        grund = grund,
    )
}
