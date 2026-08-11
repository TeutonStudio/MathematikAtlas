package de.TeutonStudio.MathematikRechenSystem.kern

enum class VektorRechnerOperator(val stabileId: String, val titel: String) {
    ADDITION("vektor.addition", "Addition"),
    SUBTRAKTION("vektor.subtraktion", "Subtraktion"),
    SKALARMULTIPLIKATION("vektor.skalarmultiplikation", "Skalarmultiplikation"),
    NEGATION("vektor.negation", "Negation"),
    SKALARPRODUKT("vektor.skalarprodukt", "Skalarprodukt"),
    KREUZPRODUKT("vektor.kreuzprodukt", "Kreuzprodukt"),
    NORM("vektor.norm", "Norm"),
    NORMALISIERUNG("vektor.normalisierung", "Normalisierung"),
    HADAMARD_PRODUKT("vektor.hadamard", "Hadamard-Produkt"),
    PROJEKTION("vektor.projektion", "Projektion"),
    WINKEL("vektor.winkel", "Winkel"),
    DISTANZ("vektor.distanz", "Distanz"),
    WINKEL_ZU_ACHSE("vektor.winkelZuAchse", "Winkel zu Achse"),
    VEKTORFELD_INTEGRIEREN("vektor.vektorfeldIntegral", "Vektorfeld integrieren"),
    ZERLEGEN("vektor.zerlegen", "Zerlegen"),
    ZUSAMMENFUEHREN("vektor.zusammenfuehren", "Zusammenführen"),
    ;

    companion object {
        fun vonIdOderNull(id: String?): VektorRechnerOperator? = entries.firstOrNull {
            it.stabileId == id || it.name.equals(id, ignoreCase = true)
        }
    }
}

enum class VektorMetrik(val stabileId: String, val titel: String) {
    EUKLIDISCH("metrik.l2", "Euklidisch (L²)"),
    L1("metrik.l1", "Manhattan (L¹)"),
    LINF("metrik.linf", "Maximum (L∞)"),
    ;

    companion object {
        fun vonIdOderStandard(id: String?): VektorMetrik = entries.firstOrNull {
            it.stabileId == id || it.name.equals(id, ignoreCase = true)
        } ?: EUKLIDISCH
    }
}

enum class VektorStrukturAusgabe(val stabileId: String, val titel: String) {
    TUPEL("tupel", "Tupel"),
    SPALTE("spalte", "Spaltenvektor"),
    ZEILE("zeile", "Zeilenvektor"),
    ;

    companion object {
        fun vonIdOderStandard(id: String?): VektorStrukturAusgabe = entries.firstOrNull {
            it.stabileId == id || it.name.equals(id, ignoreCase = true)
        } ?: TUPEL
    }
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
    val vektoren: List<VektorQuelle> = emptyList(),
    val skalare: List<ZahlAusdruck> = emptyList(),
    val objekte: List<MathematischesObjekt> = emptyList(),
    val methode: Methode? = null,
    val menge: MengenAusdruck? = null,
    val mass: IntegralMass? = null,
    val metrik: VektorMetrik = VektorMetrik.EUKLIDISCH,
    val achse: Int = 1,
    val strukturAusgabe: VektorStrukturAusgabe = VektorStrukturAusgabe.TUPEL,
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

    data class ObjektWert(
        val wert: MathematischesObjekt,
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
        if (anfrage.operator == VektorRechnerOperator.ZERLEGEN) return zerlege(anfrage)
        if (anfrage.operator == VektorRechnerOperator.ZUSAMMENFUEHREN) return fuehreZusammen(anfrage)
        if (anfrage.operator == VektorRechnerOperator.VEKTORFELD_INTEGRIEREN) return integriereVektorfeld(anfrage)

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
                vektorErgebnis(komponentenweise(vektoren) { werte -> addition(werte) }, anfrage.vektoren.first().vertrag)
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
                    orientiereWie(vektoren.single(), vektoren.single().werte.map { multiplikation(anfrage.skalare.single(), it) }),
                    anfrage.vektoren.single().vertrag,
                )
            }
            VektorRechnerOperator.NEGATION -> {
                if (vektoren.size != 1) return anzahlFehler("Negation", "genau einen Vektor")
                vektorErgebnis(orientiereWie(vektoren.single(), vektoren.single().werte.map(::negation)), anfrage.vektoren.single().vertrag)
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
                VektorRechnerErgebnis.ZahlWert(norm(vektoren.single()), FundamentalerZahlbereich.REELL)
            }
            VektorRechnerOperator.NORMALISIERUNG -> {
                if (vektoren.size != 1) return anzahlFehler("Normalisierung", "genau einen Vektor")
                val vektor = vektoren.single()
                if (istExakterNullvektor(vektor)) {
                    return VektorRechnerErgebnis.Ungueltig("nullvektor_normalisierung", "Der Nullvektor kann nicht normalisiert werden.")
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
                        vektoren[0].werte.zip(vektoren[1].werte) { links, rechts -> multiplikation(links, rechts) },
                    ),
                    anfrage.vektoren.first().vertrag,
                )
            }
            VektorRechnerOperator.PROJEKTION -> {
                if (vektoren.size != 2) return anzahlFehler("Projektion", "Vektor und Zielvektor")
                val ziel = vektoren[1]
                val nenner = skalarproduktOhneOrientierungsZwang(ziel, ziel)
                if (istExakterNullvektor(ziel)) {
                    return VektorRechnerErgebnis.Ungueltig("projektion_auf_nullvektor", "Eine Projektion auf den Nullvektor ist nicht definiert.")
                }
                val faktor = Division(skalarproduktOhneOrientierungsZwang(vektoren[0], ziel), nenner)
                vektorErgebnis(
                    orientiereWie(ziel, ziel.werte.map { multiplikation(faktor, it) }),
                    anfrage.vektoren[1].vertrag,
                    listOf(Ungleichheit(nenner, RationaleZahl.Null)),
                )
            }
            VektorRechnerOperator.WINKEL -> winkel(vektoren, anfrage.vektoren)
            VektorRechnerOperator.DISTANZ -> distanz(vektoren, anfrage.vektoren, anfrage.metrik)
            VektorRechnerOperator.WINKEL_ZU_ACHSE -> winkelZuAchse(vektoren, anfrage.achse)
            VektorRechnerOperator.VEKTORFELD_INTEGRIEREN,
            VektorRechnerOperator.ZERLEGEN,
            VektorRechnerOperator.ZUSAMMENFUEHREN,
            -> error("Spezialoperator wurde vor der Vektormaterialisierung abgefangen.")
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

    private fun distanz(
        vektoren: List<OrientierterVektor>,
        quellen: List<VektorQuelle>,
        metrik: VektorMetrik,
    ): VektorRechnerErgebnis {
        if (vektoren.size != 2) return anzahlFehler("Distanz", "genau zwei Vektoren")
        val differenzen = vektoren[0].werte.zip(vektoren[1].werte, ::subtraktion)
        val wert = when (metrik) {
            VektorMetrik.EUKLIDISCH -> Wurzel(addition(differenzen.map { Potenz(it, RationaleZahl.von(2)) }))
            VektorMetrik.L1 -> addition(differenzen.map(::Betrag))
            VektorMetrik.LINF -> differenzen.map(::Betrag).let { betraege ->
                if (betraege.size == 1) betraege.single() else maximum(betraege)
            }
        }
        return VektorRechnerErgebnis.ZahlWert(wert, gemeinsamerBereich(quellen))
    }

    private fun winkel(
        vektoren: List<OrientierterVektor>,
        quellen: List<VektorQuelle>,
    ): VektorRechnerErgebnis {
        if (vektoren.size != 2) return anzahlFehler("Winkel", "genau zwei Vektoren")
        val normA = norm(vektoren[0])
        val normB = norm(vektoren[1])
        if (vektoren.any(::istExakterNullvektor)) {
            return VektorRechnerErgebnis.Ungueltig("winkel_nullvektor", "Der Winkel zu einem Nullvektor ist nicht definiert.")
        }
        return VektorRechnerErgebnis.ZahlWert(
            ArcCosinus(Division(skalarproduktOhneOrientierungsZwang(vektoren[0], vektoren[1]), multiplikation(normA, normB))),
            FundamentalerZahlbereich.REELL,
            listOf(Ungleichheit(normA, RationaleZahl.Null), Ungleichheit(normB, RationaleZahl.Null)),
        )
    }

    private fun winkelZuAchse(vektoren: List<OrientierterVektor>, achse: Int): VektorRechnerErgebnis {
        if (vektoren.size != 1) return anzahlFehler("Winkel zu Achse", "genau einen Vektor")
        val vektor = vektoren.single()
        if (achse !in 1..vektor.werte.size) {
            return VektorRechnerErgebnis.Ungueltig(
                "achse_ausserhalb_dimension",
                "Achse $achse liegt außerhalb der Dimension ${vektor.werte.size}; Achsen werden 1-basiert gezählt.",
            )
        }
        if (istExakterNullvektor(vektor)) {
            return VektorRechnerErgebnis.Ungueltig("winkel_nullvektor", "Der Winkel des Nullvektors zu einer Achse ist nicht definiert.")
        }
        val laenge = norm(vektor)
        return VektorRechnerErgebnis.ZahlWert(
            ArcCosinus(Division(vektor.werte[achse - 1], laenge)),
            FundamentalerZahlbereich.REELL,
            listOf(Ungleichheit(laenge, RationaleZahl.Null)),
        )
    }

    private fun zerlege(anfrage: VektorRechnerAnfrage): VektorRechnerErgebnis {
        val objekt = anfrage.objekte.singleOrNull() ?: anfrage.vektoren.singleOrNull()?.let { quelle ->
            when (val materialisiert = materialisiere(quelle)) {
                is Materialisierung.Erfolg -> materialisiert.vektor
                is Materialisierung.Fehler -> return VektorRechnerErgebnis.Ungueltig(materialisiert.code, materialisiert.nachricht)
            }
        } ?: return anzahlFehler("Zerlegen", "genau ein Tupel oder einen Vektor")
        val elemente = when (objekt) {
            is Tupel -> objekt.elemente
            is OrientierterVektor -> objekt.werte
            else -> return VektorRechnerErgebnis.Ungueltig("struktur_nicht_zerlegbar", "Zerlegen akzeptiert Tupel und Vektoren.")
        }
        return VektorRechnerErgebnis.ObjektWert(Tupel(elemente))
    }

    private fun fuehreZusammen(anfrage: VektorRechnerAnfrage): VektorRechnerErgebnis {
        if (anfrage.objekte.isEmpty()) return anzahlFehler("Zusammenführen", "mindestens ein Element oder Tupel")
        val elemente = anfrage.objekte.flatMap { objekt ->
            when (objekt) {
                is Tupel -> objekt.elemente
                is OrientierterVektor -> objekt.werte
                else -> listOf(objekt)
            }
        }
        if (elemente.isEmpty()) return VektorRechnerErgebnis.Ungueltig("struktur_leer", "Eine leere Struktur kann nicht zusammengeführt werden.")
        return when (anfrage.strukturAusgabe) {
            VektorStrukturAusgabe.TUPEL -> VektorRechnerErgebnis.ObjektWert(Tupel(elemente))
            VektorStrukturAusgabe.SPALTE,
            VektorStrukturAusgabe.ZEILE,
            -> {
                val zahlen = elemente.mapIndexed { index, objekt ->
                    objekt as? ZahlAusdruck ?: return VektorRechnerErgebnis.Ungueltig(
                        "vektor_komponente_keine_zahl",
                        "Komponente ${index + 1} ist keine Zahl und kann deshalb nicht in einen Vektor übernommen werden.",
                    )
                }
                VektorRechnerErgebnis.ObjektWert(
                    if (anfrage.strukturAusgabe == VektorStrukturAusgabe.ZEILE) ZeilenVektor(zahlen) else SpaltenVektor(zahlen),
                )
            }
        }
    }

    private fun integriereVektorfeld(anfrage: VektorRechnerAnfrage): VektorRechnerErgebnis {
        val methode = anfrage.methode ?: return VektorRechnerErgebnis.Ungueltig(
            "vektorfeld_fehlt",
            "Vektorfeld integrieren benötigt eine Methode.",
        )
        val menge = anfrage.menge ?: return VektorRechnerErgebnis.Ungueltig(
            "integrationsmenge_fehlt",
            "Vektorfeld integrieren benötigt eine Integrationsmenge.",
        )
        val mass = anfrage.mass ?: return VektorRechnerErgebnis.Ungueltig(
            "integrationsmass_fehlt",
            "Für das Vektorfeldintegral muss das Integrationsmaß explizit verbunden werden.",
        )
        val komponenten = when (val vorschrift = methode.vorschrift) {
            is OrientierterVektor -> vorschrift.werte
            is Tupel -> vorschrift.elemente.mapIndexed { index, objekt ->
                objekt as? ZahlAusdruck ?: return VektorRechnerErgebnis.Ungueltig(
                    "vektorfeld_komponente_keine_zahl",
                    "Komponente ${index + 1} des Vektorfelds ist keine Zahl.",
                )
            }
            else -> return VektorRechnerErgebnis.Ungueltig(
                "methode_nicht_vektorwertig",
                "Die Methode muss einen Vektor oder ein Zahlentupel ausgeben.",
            )
        }
        val bereich = IntegralBereich(listOf(menge))
        if (methode.parameter.size != bereich.dimension) {
            return VektorRechnerErgebnis.Ungueltig(
                "integral_dimension",
                "Methodenstelligkeit und Dimension des Integrationsbereichs stimmen nicht überein.",
            )
        }
        val integrierteKomponenten = komponenten.mapIndexed { index, term ->
            val komponentenMethode = Methode(
                name = "${methode.name}_{${index + 1}}",
                parameter = methode.parameter,
                vorschrift = term,
                zielMenge = ReelleZahlen,
                werteVorräte = methode.werteVorräte,
                effektiverWerteVorrat = methode.effektiverWerteVorrat,
                bereichsanpassung = methode.bereichsanpassung,
            )
            val integral = runCatching {
                methodenIntegral(komponentenMethode, bereich, kurz = false, mass = mass)
            }.getOrElse { fehler ->
                return VektorRechnerErgebnis.Ungueltig("vektorfeld_integral", fehler.message ?: "Vektorfeldintegral nicht auswertbar.")
            }
            werteIntegralAus(integral).wert
        }
        val voraussetzungen = integrierteKomponenten.filterIsInstance<StrukturiertesIntegral>()
            .flatMap { it.voraussetzungen }
            .distinct()
        return VektorRechnerErgebnis.ObjektWert(Tupel(integrierteKomponenten), voraussetzungen)
    }

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
