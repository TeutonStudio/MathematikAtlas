package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.*

/** Kanonische IDs und Konstruktoren des gemeinsamen neutralen Typkerns. */
object MathematischeTypen {
    val Objekt = TypId("mathematik.objekt")
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
            Methode to Objekt,
            Grafik to Objekt,
            SvgGrafik to Grafik,
            SvgStil to Objekt,
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
    NatürlicheZahlen -> TypAusdruck.Atom(MathematischeTypen.Natuerlich)
    GanzeZahlen -> TypAusdruck.Atom(MathematischeTypen.Ganz)
    RationaleZahlen -> TypAusdruck.Atom(MathematischeTypen.Rational)
    ReelleZahlen, is ReellesIntervall -> TypAusdruck.Atom(MathematischeTypen.Reell)
    KomplexeZahlen -> TypAusdruck.Atom(MathematischeTypen.Komplex)
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
        MathematischeTypen.Tensor,
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

/** Neutrale MethodenSignatur ist immer Methode<Tupel<...>, Tupel<...>>. */
fun MethodenSignatur.typAusdruck(): TypAusdruck = typAusdruck

/** Semantischer Typ einer Methode, unabhängig von ihrer Ausführungs- oder Mathematik-Capability. */
fun Methode.methodenTypAusdruck(): TypAusdruck =
    (this as? SignaturtragendeMethode)?.signatur?.typAusdruck
        ?: TypAusdruck.Atom(MathematischeTypen.Methode)
