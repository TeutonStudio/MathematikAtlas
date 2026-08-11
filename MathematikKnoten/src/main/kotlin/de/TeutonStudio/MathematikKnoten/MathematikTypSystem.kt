package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

/** Kanonische semantische Typ-IDs der Mathematikdomäne. */
object MathematikTypen {
    val Objekt = TypId("mathematik.objekt")
    val Zahl = TypId("mathematik.zahl")
    val Aussage = TypId("mathematik.aussage")
    val Menge = TypId("mathematik.menge")
    val Mass = TypId("mathematik.mass")
    val Vektor = TypId("mathematik.vektor")
    val SpaltenVektor = TypId("mathematik.vektor.spalte")
    val ZeilenVektor = TypId("mathematik.vektor.zeile")
    val Matrix = TypId("mathematik.matrix")
    val Tensor = TypId("mathematik.tensor")
    val Tupel = TypId("mathematik.tupel")
    val LeeresTupel = TypId("mathematik.tupel.leer")
    val Methode = TypId("mathematik.methode")
    val Folge = TypId("mathematik.folge")

    val N = TypId("mathematik.zahl.N")
    val N0 = TypId("mathematik.zahl.N0")
    val Z = TypId("mathematik.zahl.Z")
    val Q = TypId("mathematik.zahl.Q")
    val R = TypId("mathematik.zahl.R")
    val C = TypId("mathematik.zahl.C")
    val H = TypId("mathematik.zahl.H")

    fun dimension(wert: Int): TypId {
        require(wert > 0) { "Eine Dimension muss positiv sein." }
        return TypId("mathematik.dimension.$wert")
    }

    val SymbolischeDimension = TypId("mathematik.dimension.symbolisch")
}

/**
 * Erweitert die groben Anschlusskategorien um echte mathematische Typbeziehungen
 * und parametrisierte Konstruktoren. Die Anschlussarten bleiben dabei erhalten.
 */
fun mathematikTypRegister(arten: AnschlussArtRegister): TypRegister =
    typRegisterAusAnschlussArten(arten).apply {
        registriereAtom(MathematikTypen.N, listOf(MathematikTypen.Z, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.N0, listOf(MathematikTypen.Z, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.Z, listOf(MathematikTypen.Q, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.Q, listOf(MathematikTypen.R, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.R, listOf(MathematikTypen.C, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.C, listOf(MathematikTypen.H, MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.H, listOf(MathematikTypen.Zahl))
        registriereAtom(MathematikTypen.LeeresTupel, listOf(MathematikTypen.Tupel))
        registriereAtom(MathematikTypen.SymbolischeDimension)

        registriereKonstruktor(
            TypKonstruktorBeschreibung(MathematikTypen.Menge, standardVarianz = TypVarianz.Kovariant),
            listOf(MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(MathematikTypen.Tupel, standardVarianz = TypVarianz.Kovariant),
            listOf(MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(
                MathematikTypen.SpaltenVektor,
                argumentVarianzen = listOf(TypVarianz.Kovariant, TypVarianz.Invariant),
            ),
            listOf(MathematikTypen.Vektor, MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(
                MathematikTypen.ZeilenVektor,
                argumentVarianzen = listOf(TypVarianz.Kovariant, TypVarianz.Invariant),
            ),
            listOf(MathematikTypen.Vektor, MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(
                MathematikTypen.Matrix,
                argumentVarianzen = listOf(TypVarianz.Kovariant, TypVarianz.Invariant, TypVarianz.Invariant),
            ),
            listOf(MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(MathematikTypen.Tensor, standardVarianz = TypVarianz.Kovariant),
            listOf(MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(MathematikTypen.Folge, standardVarianz = TypVarianz.Kovariant),
            listOf(MathematikTypen.Objekt),
        )
        registriereKonstruktor(
            TypKonstruktorBeschreibung(
                MathematikTypen.Methode,
                argumentVarianzen = listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant),
            ),
            listOf(MathematikTypen.Objekt),
        )
    }

fun mathematikTypSystem(arten: AnschlussArtRegister): TypSystem =
    StandardTypSystem(mathematikTypRegister(arten))

/**
 * Übersetzt bereits vorhandene Mengen- und Methodenmodelle in den neutralen
 * G0.2-Typkern. Die mathematische Menge bleibt die Quelle der Wahrheit.
 */
class MathematikTypResolver(
    private val typSystem: TypSystem,
) {
    fun elementTyp(menge: MengenAusdruck): TypAusdruck = typSystem.normalisiere(when (menge) {
        NatürlicheZahlen -> atom(MathematikTypen.N)
        GanzeZahlen -> atom(MathematikTypen.Z)
        RationaleZahlen -> atom(MathematikTypen.Q)
        ReelleZahlen -> atom(MathematikTypen.R)
        KomplexeZahlen -> atom(MathematikTypen.C)
        NichtnegativeGanzeZahlenSemantik.menge -> atom(MathematikTypen.N0)
        is BeschraenkteZahlmenge -> atom(typFürFundamentalenZahlbereich(menge.traeger))
        is ReellesIntervall -> atom(MathematikTypen.R)
        is ModuloZahlenraum -> atom(MathematikTypen.Z)
        Primzahlen -> atom(MathematikTypen.N)
        GaußscheGanzeZahlen, GaußschePrimzahlen -> atom(MathematikTypen.C)

        is Vektorraum -> TypAusdruck.Parameterisiert(
            if (menge.orientierung == VektorOrientierung.Spalte) MathematikTypen.SpaltenVektor else MathematikTypen.ZeilenVektor,
            listOf(elementTyp(menge.skalarMenge), atom(MathematikTypen.dimension(menge.dimension))),
        )
        is Matrizenraum -> TypAusdruck.Parameterisiert(
            MathematikTypen.Matrix,
            listOf(
                elementTyp(menge.skalarMenge),
                atom(MathematikTypen.dimension(menge.zeilen)),
                atom(MathematikTypen.dimension(menge.spalten)),
            ),
        )
        is Tensorraum -> TypAusdruck.Parameterisiert(
            MathematikTypen.Tensor,
            listOf(elementTyp(menge.elementMenge)) + menge.dimensionen.map(::dimensionsTyp),
        )
        is Tupelraum -> tupelTyp(menge.komponenten.map(::elementTyp))
        is KartesischesProdukt -> tupelTyp(menge.mengen.map(::elementTyp))
        is Folgenraum -> TypAusdruck.Parameterisiert(MathematikTypen.Folge, listOf(elementTyp(menge.elementMenge)))
        is Abbildungsmenge -> methodenTyp(
            tupelTyp(listOf(elementTyp(menge.definitionsMenge))),
            elementTyp(menge.zielMenge),
        )
        is Potenzmenge -> TypAusdruck.Parameterisiert(MathematikTypen.Menge, listOf(elementTyp(menge.grundMenge)))

        is GefilterteMenge -> elementTyp(menge.menge)
        is MengenDifferenz -> elementTyp(menge.links)
        is DefinierteMenge -> {
            val typen = menge.variablen.map { elementTyp(it.grundMenge) }
            if (typen.size == 1) typen.single() else tupelTyp(typen)
        }
        is Vereinigung -> gemeinsameOberartOderVereinigung(menge.mengen.map(::elementTyp))
        is Schnitt -> {
            val typen = menge.mengen.map(::elementTyp).distinct()
            if (typen.size == 1) typen.single() else TypAusdruck.Unbekannt
        }
        is EndlicheMenge -> {
            if (menge.elemente.isEmpty()) TypAusdruck.Unbekannt
            else gemeinsameOberartOderVereinigung(menge.elemente.map(::objektTyp))
        }
        LeereMenge -> TypAusdruck.Unbekannt
        is BenannteMenge -> menge.fundamentalerZahlbereichOderNull()
            ?.let { atom(typFürFundamentalenZahlbereich(it)) }
            ?: TypAusdruck.Unbekannt
        else -> menge.fundamentalerZahlbereichOderNull()
            ?.let { atom(typFürFundamentalenZahlbereich(it)) }
            ?: TypAusdruck.Unbekannt
    })

    fun mengenTyp(menge: MengenAusdruck): TypAusdruck =
        TypAusdruck.Parameterisiert(MathematikTypen.Menge, listOf(elementTyp(menge)))

    fun methodenTyp(methode: Methode): TypAusdruck {
        val signatur = runCatching { methode.methodenSignatur() }.getOrNull()
            ?: return atom(MathematikTypen.Methode)
        val definitionsTyp = if (signatur.argumente.isEmpty()) {
            atom(MathematikTypen.LeeresTupel)
        } else {
            tupelTyp(signatur.argumente.map { elementTyp(it.werteVorrat) })
        }
        return methodenTyp(definitionsTyp, elementTyp(signatur.zielMenge))
    }

    fun objektTyp(objekt: MathematischesObjekt): TypAusdruck = typSystem.normalisiere(when (objekt) {
        is RationaleZahl -> atom(MathematikTypen.Q)
        is ZahlAusdruck -> atom(MathematikTypen.Zahl)
        is Aussage -> atom(MathematikTypen.Aussage)
        is MengenAusdruck -> mengenTyp(objekt)
        is Tupel -> tupelTyp(objekt.elemente.map(::objektTyp))
        is SpaltenVektor -> atom(MathematikTypen.SpaltenVektor)
        is ZeilenVektor -> atom(MathematikTypen.ZeilenVektor)
        is Matrix -> atom(MathematikTypen.Matrix)
        is Tensor -> atom(MathematikTypen.Tensor)
        is Methode -> methodenTyp(objekt)
        is TypisiertesElement -> atom(TypId(objekt.anschlussArt))
        else -> atom(MathematikTypen.Objekt)
    })

    private fun methodenTyp(definitionsTyp: TypAusdruck, zielTyp: TypAusdruck): TypAusdruck =
        TypAusdruck.Parameterisiert(MathematikTypen.Methode, listOf(definitionsTyp, zielTyp))

    private fun tupelTyp(komponenten: List<TypAusdruck>): TypAusdruck = when (komponenten.size) {
        0 -> atom(MathematikTypen.LeeresTupel)
        else -> TypAusdruck.Parameterisiert(MathematikTypen.Tupel, komponenten)
    }

    private fun gemeinsameOberartOderVereinigung(typen: List<TypAusdruck>): TypAusdruck =
        typSystem.gemeinsameOberart(typen) ?: TypAusdruck.Unbekannt

    private fun dimensionsTyp(dimension: ZahlAusdruck): TypAusdruck {
        val rational = dimension as? RationaleZahl
        if (rational != null && rational.nenner == BigInteger.ONE && rational.zähler.signum() > 0) {
            val wert = runCatching { rational.zähler.intValueExact() }.getOrNull()
            if (wert != null) return atom(MathematikTypen.dimension(wert))
        }
        return atom(MathematikTypen.SymbolischeDimension)
    }

    private fun typFürFundamentalenZahlbereich(bereich: FundamentalerZahlbereich): TypId = when (bereich) {
        FundamentalerZahlbereich.NATUERLICH_POSITIV -> MathematikTypen.N
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> MathematikTypen.N0
        FundamentalerZahlbereich.GANZ -> MathematikTypen.Z
        FundamentalerZahlbereich.RATIONAL -> MathematikTypen.Q
        FundamentalerZahlbereich.REELL -> MathematikTypen.R
        FundamentalerZahlbereich.KOMPLEX -> MathematikTypen.C
        FundamentalerZahlbereich.QUATERNION -> MathematikTypen.H
    }

    private fun atom(id: TypId): TypAusdruck.Atom = TypAusdruck.Atom(id)
}

/** Baut einen semantischen Portvertrag direkt aus einer vorhandenen Methode. */
fun methodenAnschlussVertrag(
    methode: Methode,
    arten: AnschlussArtRegister,
): AnschlussVertrag {
    val system = mathematikTypSystem(arten)
    return AnschlussVertrag(typ = MathematikTypResolver(system).methodenTyp(methode))
}
