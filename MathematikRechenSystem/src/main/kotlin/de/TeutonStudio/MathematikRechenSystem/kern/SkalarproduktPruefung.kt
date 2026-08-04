package de.TeutonStudio.MathematikRechenSystem.kern

const val SKALARPRODUKT_BEGRIFF_ID = "lina.skalarprodukt"
const val SKALARPRODUKT_ZERTIFIKAT_VERSION = 1

data class SkalarproduktNachweisReferenzen(
    val linearitaet: String = "",
    val konjugierteSymmetrie: String = "",
    val positivDefinit: String = "",
) {
    val alle: Set<String>
        get() = setOf(linearitaet, konjugierteSymmetrie, positivDefinit)
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
}

data class SkalarproduktZeugnis(
    val methode: Methode,
    val vektorRaum: MengenAusdruck,
    val zahlbereich: FundamentalerZahlbereich,
    val linearitaet: SkalarproduktLinearitaet,
    val zertifikatVersion: Int,
    val referenzen: Set<String>,
) : BegriffsZeugnis {
    override val begriffsId: String = SKALARPRODUKT_BEGRIFF_ID
    override val skalarKoerper: MengenAusdruck = zahlbereich.alsMenge()
}

val SKALARPRODUKT_SPEZIFIKATION = BegriffsSpezifikation(
    id = SKALARPRODUKT_BEGRIFF_ID,
    name = "Skalarprodukt",
    rollen = listOf(
        BegriffsRolle("methode", "Methode", BegriffsRollenArt.METHODE),
    ),
    axiome = listOf(
        BegriffsAxiom("signatur", "Signatur", "\\langle\\cdot,\\cdot\\rangle:V\\times V\\to K"),
        BegriffsAxiom("linearitaet", "Linearität", "\\langle u,av+bw\\rangle=a\\langle u,v\\rangle+b\\langle u,w\\rangle"),
        BegriffsAxiom("symmetrie", "Konjugierte Symmetrie", "\\langle u,v\\rangle=\\overline{\\langle v,u\\rangle}"),
        BegriffsAxiom("positiv", "Positive Definitheit", "\\langle v,v\\rangle\\geq0\\land(\\langle v,v\\rangle=0\\Leftrightarrow v=0)"),
    ),
)

fun pruefeSkalarprodukt(
    methode: Methode,
    zahlbereich: FundamentalerZahlbereich,
    linearitaet: SkalarproduktLinearitaet,
    referenzen: SkalarproduktNachweisReferenzen,
    zertifikatVersion: Int = SKALARPRODUKT_ZERTIFIKAT_VERSION,
): BegriffsAussage {
    val diagnosen = mutableListOf<String>()
    val vertrag = methode.vertragOderNull()
    val parameterArten = methode.parameter.map { parameter ->
        (parameter as? TypisiertesElement)?.anschlussArt
    }
    val vektorArten = setOf(
        "mathematik.objekt",
        "mathematik.tupel",
        "mathematik.vektor.spalte",
        "mathematik.vektor.zeile",
    )
    val parameterPassen = methode.parameter.size == 2 && (
        parameterArten.all { it != null && it in vektorArten } ||
            methode.parameter.all { it is Variable }
        )
    val ausgabePasst = methode.ausgabeNamen.size == 1 &&
        methode.vorschrift is ZahlAusdruck &&
        vertrag?.zielMenge?.skalarproduktZahlbereichOderNull() == zahlbereich
    val signaturPasst = vertrag != null &&
        vertrag.argumentMengen.size == 2 &&
        parameterPassen &&
        ausgabePasst

    val pruefungen = mutableListOf<BegriffsAxiomPruefung>()
    pruefungen += BegriffsAxiomPruefung(
        id = "signatur",
        name = "Signatur",
        status = if (signaturPasst) NachweisStatus.Nachgewiesen else NachweisStatus.Widerlegt,
        begruendung = if (signaturPasst) {
            "Die Methode besitzt zwei Vektorargumente und genau einen skalaren Ausgang in ${zahlbereich.latex}."
        } else {
            "Erwartet wird eine zweistellige Methode V × V → ${zahlbereich.latex} mit genau einem Zahlenausgang."
        },
    )

    fun nachweis(
        id: String,
        name: String,
        referenz: String,
        beschreibung: String,
    ): BegriffsAxiomPruefung {
        val bereinigt = referenz.trim()
        return BegriffsAxiomPruefung(
            id = id,
            name = name,
            status = if (bereinigt.isBlank()) NachweisStatus.Unvollstaendig else NachweisStatus.Nachgewiesen,
            begruendung = if (bereinigt.isBlank()) {
                "Für $beschreibung fehlt eine explizite Nachweisreferenz."
            } else {
                "$beschreibung wird durch '$bereinigt' belegt."
            },
        )
    }

    pruefungen += nachweis(
        "linearitaet",
        "Linearität",
        referenzen.linearitaet,
        when (linearitaet) {
            SkalarproduktLinearitaet.RECHTSLINEAR -> "Rechtslinearität"
            SkalarproduktLinearitaet.LINKSLINEAR -> "Linkslinearität"
        },
    )
    pruefungen += nachweis(
        "symmetrie",
        "Konjugierte Symmetrie",
        referenzen.konjugierteSymmetrie,
        "konjugierte Symmetrie",
    )
    pruefungen += nachweis(
        "positiv",
        "Positive Definitheit",
        referenzen.positivDefinit,
        "positive Definitheit",
    )

    if (zertifikatVersion != SKALARPRODUKT_ZERTIFIKAT_VERSION) {
        pruefungen += BegriffsAxiomPruefung(
            id = "zertifikatVersion",
            name = "Zertifikatversion",
            status = NachweisStatus.Unvollstaendig,
            begruendung = "Zertifikatversion $zertifikatVersion wird nicht unterstützt; erwartet wird Version $SKALARPRODUKT_ZERTIFIKAT_VERSION.",
        )
    }

    val status = statusAus(pruefungen)
    when (status) {
        NachweisStatus.Nachgewiesen -> diagnosen += "Die Skalarproduktdefinition ist vollständig und versionsfest zertifiziert."
        NachweisStatus.Widerlegt -> diagnosen += "Die Methodensignatur ist nicht mit einem Skalarprodukt kompatibel."
        else -> diagnosen += "Das Zertifikat ist unvollständig; die Definition darf noch nicht im Vektorrechner ausgeführt werden."
    }

    val vektorRaum = vertrag?.argumentMengen?.firstOrNull()
        ?: BenannteMenge("V", "V")
    val zeugnis = if (status == NachweisStatus.Nachgewiesen) {
        SkalarproduktZeugnis(
            methode = methode,
            vektorRaum = vektorRaum,
            zahlbereich = zahlbereich,
            linearitaet = linearitaet,
            zertifikatVersion = zertifikatVersion,
            referenzen = referenzen.alle,
        )
    } else {
        null
    }

    return BegriffsAussage(
        pruefung = BegriffsPruefung(
            begriffsId = SKALARPRODUKT_BEGRIFF_ID,
            begriffsName = "Skalarprodukt",
            axiomPruefungen = pruefungen,
            status = status,
            diagnosen = diagnosen,
            zeugnis = zeugnis,
            spezifikation = SKALARPRODUKT_SPEZIFIKATION,
            kandidat = BegriffsKandidat(
                spezifikationId = SKALARPRODUKT_BEGRIFF_ID,
                belegung = mapOf("methode" to methode),
                zertifikatReferenzen = referenzen.alle,
            ),
        ),
        formelLatex = "\\operatorname{Skalarprodukt}\\left(${methode.name}\\right)",
    )
}

fun skalarproduktZertifikatFehler(
    aussage: BegriffsAussage,
    erwarteterZahlbereich: FundamentalerZahlbereich,
    erwarteteLinearitaet: SkalarproduktLinearitaet,
): String? {
    if (aussage.pruefung.begriffsId != SKALARPRODUKT_BEGRIFF_ID) {
        return "Die Aussage zertifiziert keinen Skalarproduktbegriff."
    }
    if (aussage.pruefung.status != NachweisStatus.Nachgewiesen) {
        return "Das Skalarproduktzertifikat ist nicht vollständig nachgewiesen."
    }
    val zeugnis = aussage.pruefung.zeugnis as? SkalarproduktZeugnis
        ?: return "Das Skalarproduktzertifikat enthält kein ausführbares Zeugnis."
    if (zeugnis.zertifikatVersion != SKALARPRODUKT_ZERTIFIKAT_VERSION) {
        return "Die Zertifikatversion ${zeugnis.zertifikatVersion} wird nicht unterstützt."
    }
    if (zeugnis.zahlbereich != erwarteterZahlbereich) {
        return "Das Zertifikat gilt für ${zeugnis.zahlbereich.latex}, benötigt wird ${erwarteterZahlbereich.latex}."
    }
    if (
        erwarteterZahlbereich == FundamentalerZahlbereich.QUATERNION &&
        zeugnis.linearitaet != erwarteteLinearitaet
    ) {
        return "Die quaternionische Linearitätsseite des Zertifikats ist nicht kompatibel."
    }
    return null
}

fun MengenAusdruck.skalarproduktZahlbereichOderNull(): FundamentalerZahlbereich? =
    fundamentalerZahlbereichOderNull()
        ?: takeIf { it.zuLatex() == FundamentalerZahlbereich.QUATERNION.latex }
            ?.let { FundamentalerZahlbereich.QUATERNION }

data class SkalarproduktFalkSchema(
    val dimension: Int,
    val linearitaet: SkalarproduktLinearitaet,
    val konjugiert: Boolean,
    val linksName: String = "u",
    val rechtsName: String = "v",
) {
    init { require(dimension > 0) }

    fun summandLatex(index: String = "i"): String = when (linearitaet) {
        SkalarproduktLinearitaet.RECHTSLINEAR -> {
            val links = if (konjugiert) "\\overline{${linksName}_{$index}}" else "${linksName}_{$index}"
            "$links\\,${rechtsName}_{$index}"
        }
        SkalarproduktLinearitaet.LINKSLINEAR -> {
            val rechts = if (konjugiert) "\\overline{${rechtsName}_{$index}}" else "${rechtsName}_{$index}"
            "${linksName}_{$index}\\,$rechts"
        }
    }

    fun zuLatex(): String =
        "\\left\\langle $linksName,$rechtsName\\right\\rangle=" +
            "\\sum_{i=0}^{${dimension - 1}}${summandLatex()}"
}
