package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.*

/** Kanonische IDs und Konstruktoren des gemeinsamen neutralen Typkerns. */
object MathematischeTypen {
    /** Domänenneutraler Obertyp für alle durch den Atlas transportierbaren Werte. */
    val AtlasWert = TypId("atlas.wert")

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

    /**
     * Historisch stabile Typ-ID des Methodenanschlusses. Der String bleibt aus
     * Persistenzkompatibilität bestehen; seine Semantik ist seit G0.5 domänenneutral.
     */
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
        // Tupel sind variadisch und komponentenweise kovariant, einschließlich Tupel<>.
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

/** Typ eines Elements, das aus dieser Menge gewählt wird. Die Inferenz bleibt konservativ. */
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

/**
 * Jede Methode ist typseitig kanonisch `Tupel<...> -> Tupel<...>`.
 * Einertupel und leere Tupel kollabieren nicht.
 */
fun MethodenSignatur.typAusdruck(): TypAusdruck = TypAusdruck.Parameterisiert(
    MathematischeTypen.Methode,
    listOf(argumentTupelTyp, ergebnisTupelTyp),
)

/** Semantischer Typ einer Methode, unabhängig von ihrer Ausführungs-Capability. */
fun Methode.methodenTypAusdruck(): TypAusdruck =
    (this as? SignaturtragendeMethode)?.signatur?.typAusdruck()
        ?: TypAusdruck.Atom(MathematischeTypen.Methode)
