package de.TeutonStudio.MathematikRechenSystem.kern

enum class VektorRechnerOperator(val stabileId: String) {
    ADDITION("vektor.addition"),
    SUBTRAKTION("vektor.subtraktion"),
    SKALARMULTIPLIKATION("vektor.skalarmultiplikation"),
    NEGATION("vektor.negation"),
    SKALARPRODUKT("vektor.skalarprodukt"),
    KREUZPRODUKT("vektor.kreuzprodukt"),
    NORM("vektor.norm"),
    NORMALISIERUNG("vektor.normalisierung"),
    HADAMARD_PRODUKT("vektor.hadamard"),
    PROJEKTION("vektor.projektion"),
    WINKEL("vektor.winkel"),
}

data class KartesischerKoordinatenVertrag(
    val dimension: Int,
    val zahlbereich: FundamentalerZahlbereich,
    val basisId: String,
    val koordinatensystemId: String,
    val standardBasis: Boolean,
) {
    init {
        require(dimension > 0)
        require(basisId.isNotBlank())
        require(koordinatensystemId.isNotBlank())
    }
}

sealed interface VektorQuelle {
    val vertrag: KartesischerKoordinatenVertrag

    data class Vektor(
        val wert: OrientierterVektor,
        override val vertrag: KartesischerKoordinatenVertrag,
    ) : VektorQuelle {
        init { require(wert.werte.size == vertrag.dimension) }
    }

    data class Koordinaten(
        val tupel: Tupel,
        override val vertrag: KartesischerKoordinatenVertrag,
    ) : VektorQuelle {
        init { require(tupel.elemente.size == vertrag.dimension) }
    }
}

data class VektorRechnerAnfrage(
    val operator: VektorRechnerOperator,
    val vektoren: List<VektorQuelle>,
    val skalare: List<ZahlAusdruck> = emptyList(),
)

sealed interface VektorRechnerErgebnis {
    data class VektorWert(
        val wert: OrientierterVektor,
        val vertrag: KartesischerKoordinatenVertrag,
        val bedingungen: List<Aussage> = emptyList(),
    ) : VektorRechnerErgebnis

    data class ZahlWert(
        val wert: ZahlAusdruck,
        val bereich: FundamentalerZahlbereich,
        val bedingungen: List<Aussage> = emptyList(),
    ) : VektorRechnerErgebnis

    data class Ungueltig(
        val code: String,
        val nachricht: String,
        val betroffeneArgumente: List<Int> = emptyList(),
    ) : VektorRechnerErgebnis
}

object VektorRechner {
    const val KNOTEN_ART = "mathematik.vektorrechner"

    fun erzeuge(anfrage: VektorRechnerAnfrage): VektorRechnerErgebnis {
        val vektoren = anfrage.vektoren.mapIndexed { index, quelle ->
            when (val ergebnis = materialisiere(quelle)) {
                is Materialisierung.Erfolg -> ergebnis.vektor
                is Materialisierung.Fehler -> return VektorRechnerErgebnis.Ungueltig(
                    ergebnis.code,
                    ergebnis.nachricht,
                    listOf(index),
                )
            }
        }
        pruefeVertraege(anfrage.vektoren)?.let { return it }

        return when (anfrage.operator) {
            VektorRechnerOperator.ADDITION -> {
                if (vektoren.size < 2) return anzahlFehler("Addition", "mindestens zwei Vektoren")
                vektorErgebnis(
                    komponentenweise(vektoren) { werte -> addition(werte) },
                    anfrage.vektoren.first().vertrag,
                )
            }
            VektorRechnerOperator.SUBTRAKTION -> {
                if (vektoren.size != 2) return anzahlFehler("Subtraktion", "genau zwei Vektoren")
                vektorErgebnis(
                    orientiereWie(vektoren.first(), vektoren[0].werte.zip(vektoren[1].werte, ::subtraktion)),
                    anfrage.vektoren.first().vertrag,
                )
            }
            VektorRechnerOperator.SKALARMULTIPLIKATION -> {
                if (vektoren.size != 1 || anfrage.skalare.size != 1) {
                    return anzahlFehler("Skalarmultiplikation", "einen Vektor und einen Skalar")
                }
                vektorErgebnis(
                    orientiereWie(
                        vektoren.single(),
                        vektoren.single().werte.map { multiplikation(anfrage.skalare.single(), it) },
                    ),
                    anfrage.vektoren.single().vertrag,
                )
            }
            VektorRechnerOperator.NEGATION -> {
                if (vektoren.size != 1) return anzahlFehler("Negation", "genau einen Vektor")
                vektorErgebnis(
                    orientiereWie(vektoren.single(), vektoren.single().werte.map(::negation)),
                    anfrage.vektoren.single().vertrag,
                )
            }
            VektorRechnerOperator.SKALARPRODUKT -> {
                if (vektoren.size != 2) return anzahlFehler("Skalarprodukt", "genau zwei Vektoren")
                VektorRechnerErgebnis.ZahlWert(
                    skalarproduktOhneOrientierungsZwang(vektoren[0], vektoren[1]),
                    gemeinsamerBereich(anfrage.vektoren),
                )
            }
            VektorRechnerOperator.KREUZPRODUKT -> {
                if (vektoren.size != 2 || vektoren.any { it.werte.size != 3 }) {
                    return VektorRechnerErgebnis.Ungueltig(
                        "kreuzprodukt_dimension",
                        "Das klassische Kreuzprodukt benötigt genau zwei dreidimensionale Vektoren.",
                    )
                }
                val a = vektoren[0].werte
                val b = vektoren[1].werte
                val komponenten = listOf(
                    subtraktion(multiplikation(a[1], b[2]), multiplikation(a[2], b[1])),
                    subtraktion(multiplikation(a[2], b[0]), multiplikation(a[0], b[2])),
                    subtraktion(multiplikation(a[0], b[1]), multiplikation(a[1], b[0])),
                )
                vektorErgebnis(orientiereWie(vektoren.first(), komponenten), anfrage.vektoren.first().vertrag)
            }
            VektorRechnerOperator.NORM -> {
                if (vektoren.size != 1) return anzahlFehler("Norm", "genau einen Vektor")
                VektorRechnerErgebnis.ZahlWert(
                    norm(vektoren.single()),
                    FundamentalerZahlbereich.REELL,
                )
            }
            VektorRechnerOperator.NORMALISIERUNG -> {
                if (vektoren.size != 1) return anzahlFehler("Normalisierung", "genau einen Vektor")
                val vektor = vektoren.single()
                if (istExakterNullvektor(vektor)) {
                    return VektorRechnerErgebnis.Ungueltig(
                        "nullvektor_normalisierung",
                        "Der Nullvektor kann nicht normalisiert werden.",
                    )
                }
                val laenge = norm(vektor)
                vektorErgebnis(
                    orientiereWie(vektor, vektor.werte.map { Division(it, laenge) }),
                    anfrage.vektoren.single().vertrag,
                    listOf(Ungleichheit(laenge, RationaleZahl.Null)),
                )
            }
            VektorRechnerOperator.HADAMARD_PRODUKT -> {
                if (vektoren.size != 2) return anzahlFehler("Hadamard-Produkt", "genau zwei Vektoren")
                vektorErgebnis(
                    orientiereWie(
                        vektoren.first(),
                        vektoren[0].werte.zip(vektoren[1].werte) { links, rechts ->
                            multiplikation(links, rechts)
                        },
                    ),
                    anfrage.vektoren.first().vertrag,
                )
            }
            VektorRechnerOperator.PROJEKTION -> {
                if (vektoren.size != 2) return anzahlFehler("Projektion", "Vektor und Zielvektor")
                val ziel = vektoren[1]
                val nenner = skalarproduktOhneOrientierungsZwang(ziel, ziel)
                if (istExakterNullvektor(ziel)) {
                    return VektorRechnerErgebnis.Ungueltig(
                        "projektion_auf_nullvektor",
                        "Eine Projektion auf den Nullvektor ist nicht definiert.",
                    )
                }
                val faktor = Division(skalarproduktOhneOrientierungsZwang(vektoren[0], ziel), nenner)
                vektorErgebnis(
                    orientiereWie(ziel, ziel.werte.map { multiplikation(faktor, it) }),
                    anfrage.vektoren[1].vertrag,
                    listOf(Ungleichheit(nenner, RationaleZahl.Null)),
                )
            }
            VektorRechnerOperator.WINKEL -> {
                if (vektoren.size != 2) return anzahlFehler("Winkel", "genau zwei Vektoren")
                val normA = norm(vektoren[0])
                val normB = norm(vektoren[1])
                if (vektoren.any(::istExakterNullvektor)) {
                    return VektorRechnerErgebnis.Ungueltig(
                        "winkel_nullvektor",
                        "Der Winkel zu einem Nullvektor ist nicht definiert.",
                    )
                }
                VektorRechnerErgebnis.ZahlWert(
                    ArcCosinus(
                        Division(
                            skalarproduktOhneOrientierungsZwang(vektoren[0], vektoren[1]),
                            multiplikation(normA, normB),
                        ),
                    ),
                    FundamentalerZahlbereich.REELL,
                    listOf(
                        Ungleichheit(normA, RationaleZahl.Null),
                        Ungleichheit(normB, RationaleZahl.Null),
                    ),
                )
            }
        }
    }

    fun alsFormelAusdruck(
        id: String,
        operator: VektorRechnerOperator,
        argumente: List<Pair<String, FormelAusdruck>>,
        ergebnisTyp: FormelTyp,
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id,
        operator.stabileId,
        argumente.mapIndexed { index, (rolle, ausdruck) -> FormelArgument(rolle, index, ausdruck) },
        ergebnisTyp,
    )

    private fun materialisiere(quelle: VektorQuelle): Materialisierung = when (quelle) {
        is VektorQuelle.Vektor -> Materialisierung.Erfolg(quelle.wert)
        is VektorQuelle.Koordinaten -> {
            if (!quelle.vertrag.standardBasis) {
                Materialisierung.Fehler(
                    "basiswechsel_erforderlich",
                    "Koordinaten in einer Nichtstandardbasis benötigen einen sichtbaren Basiswechsel.",
                )
            } else {
                val werte = quelle.tupel.elemente.mapIndexed { index, element ->
                    element as? ZahlAusdruck ?: return Materialisierung.Fehler(
                        "koordinate_keine_zahl",
                        "Koordinate ${index + 1} ist kein Zahlterm.",
                    )
                }
                Materialisierung.Erfolg(SpaltenVektor(werte))
            }
        }
    }

    private fun pruefeVertraege(quellen: List<VektorQuelle>): VektorRechnerErgebnis.Ungueltig? {
        if (quellen.isEmpty()) return null
        val dimensionen = quellen.map { it.vertrag.dimension }.distinct()
        if (dimensionen.size > 1) return VektorRechnerErgebnis.Ungueltig(
            "dimensionen_inkompatibel",
            "Vektoroperation benötigt gleiche Dimensionen; erhalten: ${dimensionen.joinToString()}.",
        )
        val systeme = quellen.map { it.vertrag.koordinatensystemId }.distinct()
        val basen = quellen.map { it.vertrag.basisId }.distinct()
        if (systeme.size > 1 || basen.size > 1) return VektorRechnerErgebnis.Ungueltig(
            "koordinatenvertraege_inkompatibel",
            "Koordinatensysteme und Basen müssen übereinstimmen oder explizit umgerechnet werden.",
        )
        return null
    }

    private fun komponentenweise(
        vektoren: List<OrientierterVektor>,
        operation: (List<ZahlAusdruck>) -> ZahlAusdruck,
    ): OrientierterVektor = orientiereWie(
        vektoren.first(),
        List(vektoren.first().werte.size) { index -> operation(vektoren.map { it.werte[index] }) },
    )

    private fun skalarproduktOhneOrientierungsZwang(
        links: OrientierterVektor,
        rechts: OrientierterVektor,
    ): ZahlAusdruck = addition(links.werte.zip(rechts.werte) { a, b -> multiplikation(a, b) })

    private fun norm(vektor: OrientierterVektor): ZahlAusdruck =
        Wurzel(addition(vektor.werte.map { Potenz(it, RationaleZahl.von(2)) }))

    private fun orientiereWie(vorbild: OrientierterVektor, werte: List<ZahlAusdruck>): OrientierterVektor =
        if (vorbild is ZeilenVektor) ZeilenVektor(werte) else SpaltenVektor(werte)

    private fun vektorErgebnis(
        wert: OrientierterVektor,
        vertrag: KartesischerKoordinatenVertrag,
        bedingungen: List<Aussage> = emptyList(),
    ) = VektorRechnerErgebnis.VektorWert(wert, vertrag, bedingungen)

    private fun gemeinsamerBereich(quellen: List<VektorQuelle>): FundamentalerZahlbereich =
        FundamentaleZahlbereiche.kleinsterGemeinsamerBereich(quellen.map { it.vertrag.zahlbereich })

    private fun istExakterNullvektor(vektor: OrientierterVektor): Boolean =
        vektor.werte.all { it == RationaleZahl.Null }

    private fun anzahlFehler(operator: String, erwartet: String) = VektorRechnerErgebnis.Ungueltig(
        "argumentanzahl",
        "$operator benötigt $erwartet.",
    )

    private sealed interface Materialisierung {
        data class Erfolg(val vektor: OrientierterVektor) : Materialisierung
        data class Fehler(val code: String, val nachricht: String) : Materialisierung
    }
}

object VektorRechnerMigration {
    val alteKnotenArten: Map<String, VektorRechnerOperator> = mapOf(
        "mathematik.vektoraddition" to VektorRechnerOperator.ADDITION,
        "mathematik.skalarprodukt" to VektorRechnerOperator.SKALARPRODUKT,
        "mathematik.kreuzprodukt" to VektorRechnerOperator.KREUZPRODUKT,
        "mathematik.vektornorm" to VektorRechnerOperator.NORM,
    )
}
