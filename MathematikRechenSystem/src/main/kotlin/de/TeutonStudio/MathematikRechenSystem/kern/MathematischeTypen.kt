package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.*

/** Kanonische IDs und Konstruktoren des gemeinsamen neutralen Typkerns. */
object MathematischeTypen {
    /** Domänenneutraler Obertyp sämtlicher durch den Atlas transportierbarer Werte. */
    val AtlasWert = TypId("atlas.wert")

    val Objekt = TypId("mathematik.objekt")
    val Winkel = TypId("mathematik.winkel")
    val Zahl = TypId("mathematik.zahl")
    val Aussage = TypId("mathematik.aussage")
    val Menge = TypId("mathematik.menge")
    val Topologie = TypId("mathematik.topologie")
    val TopologischerRaum = TypId("mathematik.raum.topologisch")
    val MetrischerRaum = TypId("mathematik.raum.metrisch")
    val Mass = TypId("mathematik.mass")
    val Vektor = TypId("mathematik.vektor")
    val SpaltenVektor = TypId("mathematik.vektor.spalte")
    val ZeilenVektor = TypId("mathematik.vektor.zeile")
    val Matrix = TypId("mathematik.matrix")
    val Tensor = TypId("mathematik.tensor")
    val Tupel = TypId("typ.tupel")
    val UnendlichesTupel = TypId("typ.tupel.unendlich")
    val Methode = TypId("mathematik.methode")

    /** Nichtmathematische Darstellungswerte benutzen denselben neutralen Typkern. */
    val Grafik = TypId("grafik")
    val SvgGrafik = TypId("grafik.svg")
    val SvgStil = TypId("grafik.svg.stil")

    val Natuerlich = TypId("mathematik.zahl.natuerlich")
    val NatuerlichMitNull = TypId("mathematik.zahl.natuerlich-mit-null")
    val Ganz = TypId("mathematik.zahl.ganz")
    val Rational = TypId("mathematik.zahl.rational")
    val Reell = TypId("mathematik.zahl.reell")
    val Komplex = TypId("mathematik.zahl.komplex")
    val Quaternion = TypId("mathematik.zahl.quaternion")

    val konstruktoren: List<TypKonstruktorDefinition> = listOf(
        // Tupel sind variadisch, einschließlich Tupel<>, und komponentenweise kovariant.
        TypKonstruktorDefinition(Menge, listOf(TypVarianz.Kovariant)),
        TypKonstruktorDefinition(Tupel, standardVarianz = TypVarianz.Kovariant),
        TypKonstruktorDefinition(UnendlichesTupel, listOf(TypVarianz.Kovariant)),
        TypKonstruktorDefinition(SpaltenVektor, listOf(TypVarianz.Kovariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(ZeilenVektor, listOf(TypVarianz.Kovariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(Matrix, listOf(TypVarianz.Kovariant, TypVarianz.Invariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(Tensor),
        TypKonstruktorDefinition(Methode, listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant)),
    )

    fun istAtomUntertyp(von: TypId, erwartet: TypId): Boolean {
        if (von == erwartet) return true
        val eltern = mapOf(
            Natuerlich to NatuerlichMitNull,
            NatuerlichMitNull to Ganz,
            Ganz to Rational,
            Rational to Reell,
            Reell to Komplex,
            Komplex to Quaternion,
            Quaternion to Zahl,
            Zahl to Objekt,
            Winkel to Objekt,
            Aussage to Objekt,
            Menge to Objekt,
            Topologie to Objekt,
            TopologischerRaum to Objekt,
            MetrischerRaum to TopologischerRaum,
            Mass to Objekt,
            Vektor to Objekt,
            SpaltenVektor to Vektor,
            ZeilenVektor to Vektor,
            Matrix to Objekt,
            Tensor to Objekt,
            Tupel to Objekt,
            UnendlichesTupel to Objekt,
            Objekt to AtlasWert,
            Methode to AtlasWert,
            Grafik to AtlasWert,
            SvgGrafik to Grafik,
            SvgStil to AtlasWert,
        )
        var aktuell: TypId? = von
        val besucht = mutableSetOf<TypId>()
        while (aktuell != null && besucht.add(aktuell)) {
            if (aktuell == erwartet) return true
            aktuell = eltern[aktuell]
        }
        return false
    }

    val typSystem: TypSystem = StandardTypSystem(
        istAtomUntertyp = ::istAtomUntertyp,
        konstruktoren = konstruktoren,
    )
}

/** Typ eines Elements, das aus dieser mathematischen Menge gewählt wird. */
fun MengenAusdruck.elementTypAusdruck(): TypAusdruck = when (this) {
    is EndlicheMenge -> {
        val typen = elemente.map(::endlicherElementTyp).distinct()
        when (typen.size) {
            0 -> TypAusdruck.Unbekannt
            1 -> typen.single()
            else -> TypAusdruck.Vereinigung(typen)
        }
    }
    Gradmenge -> TypAusdruck.Atom(MathematischeTypen.Winkel)
    is Potenzmenge -> TypAusdruck.Parameterisiert(MathematischeTypen.Menge, listOf(grundMenge.elementTypAusdruck()))
    is EndlicheIndexMenge -> TypAusdruck.Atom(MathematischeTypen.Natuerlich)
    is StrukturErgebnisMenge -> strukturErgebnisTyp()
    NatürlicheZahlen -> TypAusdruck.Atom(MathematischeTypen.Natuerlich)
    GanzeZahlen -> TypAusdruck.Atom(MathematischeTypen.Ganz)
    RationaleZahlen -> TypAusdruck.Atom(MathematischeTypen.Rational)
    ReelleZahlen, is ReellesIntervall -> TypAusdruck.Atom(MathematischeTypen.Reell)
    KomplexeZahlen -> TypAusdruck.Atom(MathematischeTypen.Komplex)
    is KartesischerPotenzraum -> kartesischeFaktorenOderNull()?.let {
        TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, it.map(MengenAusdruck::elementTypAusdruck))
    } ?: TypAusdruck.Atom(MathematischeTypen.Tupel)
    is KartesischesProdukt -> TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, mengen.map(MengenAusdruck::elementTypAusdruck))
    is Tupelraum -> TypAusdruck.Parameterisiert(
        MathematischeTypen.Tupel,
        komponenten.map(MengenAusdruck::elementTypAusdruck),
    )
    is Vektorraum -> TypAusdruck.Parameterisiert(
        when (orientierung) {
            VektorOrientierung.Spalte -> MathematischeTypen.SpaltenVektor
            VektorOrientierung.Zeile -> MathematischeTypen.ZeilenVektor
        },
        listOf(
            skalarMenge.elementTypAusdruck(),
            TypAusdruck.Literal(dimension.toString()),
        ),
    )
    is Matrizenraum -> TypAusdruck.Parameterisiert(
        MathematischeTypen.Matrix,
        listOf(
            skalarMenge.elementTypAusdruck(),
            TypAusdruck.Literal(zeilen.toString()),
            TypAusdruck.Literal(spalten.toString()),
        ),
    )
    is Tensorraum -> TypAusdruck.Parameterisiert(
        if (matrixForm) MathematischeTypen.Matrix else MathematischeTypen.Tensor,
        listOf(elementMenge.elementTypAusdruck()) + dimensionen.map { TypAusdruck.Literal(it.zuLatex()) },
    )
    is BeschraenkteZahlmenge -> when (traeger) {
        FundamentalerZahlbereich.NATUERLICH_POSITIV -> TypAusdruck.Atom(MathematischeTypen.Natuerlich)
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> TypAusdruck.Atom(MathematischeTypen.NatuerlichMitNull)
        FundamentalerZahlbereich.GANZ -> TypAusdruck.Atom(MathematischeTypen.Ganz)
        FundamentalerZahlbereich.RATIONAL -> TypAusdruck.Atom(MathematischeTypen.Rational)
        FundamentalerZahlbereich.REELL -> TypAusdruck.Atom(MathematischeTypen.Reell)
        FundamentalerZahlbereich.KOMPLEX -> TypAusdruck.Atom(MathematischeTypen.Komplex)
        FundamentalerZahlbereich.QUATERNION -> TypAusdruck.Atom(MathematischeTypen.Quaternion)
    }
    else -> TypAusdruck.Unbekannt
}

private fun endlicherElementTyp(wert: MathematischesObjekt): TypAusdruck = when (wert) {
    is ZahlAusdruck -> TypAusdruck.Atom(MathematischeTypen.Zahl)
    is WinkelAusdruck -> TypAusdruck.Atom(MathematischeTypen.Winkel)
    is Aussage -> TypAusdruck.Atom(MathematischeTypen.Aussage)
    is Tupel -> TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, wert.elemente.map(::endlicherElementTyp))
    is MengenAusdruck -> TypAusdruck.Parameterisiert(MathematischeTypen.Menge, listOf(wert.elementTypAusdruck()))
    is Methode -> wert.methodenTypAusdruck()
    else -> TypAusdruck.Unbekannt
}

private fun StrukturErgebnisMenge.strukturErgebnisTyp(): TypAusdruck = when (anschlussArt) {
    "mathematik.zahl" -> TypAusdruck.Atom(MathematischeTypen.Zahl)
    "mathematik.tupel" -> TypAusdruck.Atom(MathematischeTypen.Tupel)
    "mathematik.vektor.spalte" -> TypAusdruck.Parameterisiert(
        MathematischeTypen.SpaltenVektor,
        listOf(TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal(form?.singleOrNull()?.zuLatex() ?: "?")),
    )
    "mathematik.vektor.zeile" -> TypAusdruck.Parameterisiert(
        MathematischeTypen.ZeilenVektor,
        listOf(TypAusdruck.Atom(MathematischeTypen.Zahl), TypAusdruck.Literal(form?.singleOrNull()?.zuLatex() ?: "?")),
    )
    "mathematik.matrix" -> TypAusdruck.Parameterisiert(
        MathematischeTypen.Matrix,
        listOf(TypAusdruck.Atom(MathematischeTypen.Zahl)) +
            List(2) { index -> TypAusdruck.Literal(form?.getOrNull(index)?.zuLatex() ?: "?") },
    )
    "mathematik.tensor" -> TypAusdruck.Parameterisiert(
        MathematischeTypen.Tensor,
        listOf(TypAusdruck.Atom(MathematischeTypen.Zahl)) + form.orEmpty().map { TypAusdruck.Literal(it.zuLatex()) },
    )
    else -> TypAusdruck.Atom(MathematischeTypen.Objekt)
}

/** Neutrale MethodenSignatur ist immer Methode<Tupel<...>, Tupel<...>>. */
fun MethodenSignatur.typAusdruck(): TypAusdruck = typAusdruck

/** Semantischer Typ einer Methode, unabhängig von ihrer Ausführungs- oder Mathematik-Capability. */
fun Methode.methodenTypAusdruck(): TypAusdruck =
    (this as? SignaturtragendeMethode)?.signatur?.typAusdruck
        ?: TypAusdruck.Atom(MathematischeTypen.Methode)
