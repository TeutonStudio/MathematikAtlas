package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*

/** Kanonische semantische Typ-IDs des Mathematik Atlas. */
object MathematikTypen {
    val Objekt = TypId("math.objekt")
    val Zahl = TypId("math.zahl")
    val QuaternionZahl = TypId("math.zahl.quaternion")
    val KomplexeZahl = TypId("math.zahl.komplex")
    val ReelleZahl = TypId("math.zahl.reell")
    val RationaleZahl = TypId("math.zahl.rational")
    val GanzeZahl = TypId("math.zahl.ganz")
    val NichtnegativeGanzeZahl = TypId("math.zahl.natuerlich.mit_null")
    val NatürlicheZahl = TypId("math.zahl.natuerlich.positiv")
    val Aussage = TypId("math.aussage")
    val Menge = TypId("math.menge")
    val Mass = TypId("math.mass")
    val Vektor = TypId("math.vektor")
    val SpaltenVektor = TypId("math.vektor.spalte")
    val ZeilenVektor = TypId("math.vektor.zeile")
    val Matrix = TypId("math.matrix")
    val Tensor = TypId("math.tensor")
    val Methode = TypId("math.methode")

    val objekt = TypAusdruck.Atom(Objekt)
    val zahl = TypAusdruck.Atom(Zahl)
    val quaternionZahl = TypAusdruck.Atom(QuaternionZahl)
    val komplexeZahl = TypAusdruck.Atom(KomplexeZahl)
    val reelleZahl = TypAusdruck.Atom(ReelleZahl)
    val rationaleZahl = TypAusdruck.Atom(RationaleZahl)
    val ganzeZahl = TypAusdruck.Atom(GanzeZahl)
    val nichtnegativeGanzeZahl = TypAusdruck.Atom(NichtnegativeGanzeZahl)
    val natürlicheZahl = TypAusdruck.Atom(NatürlicheZahl)
    val aussage = TypAusdruck.Atom(Aussage)
    val menge = TypAusdruck.Atom(Menge)
    val mass = TypAusdruck.Atom(Mass)
    val vektor = TypAusdruck.Atom(Vektor)
    val spaltenVektor = TypAusdruck.Atom(SpaltenVektor)
    val zeilenVektor = TypAusdruck.Atom(ZeilenVektor)
    val matrix = TypAusdruck.Atom(Matrix)
    val tensor = TypAusdruck.Atom(Tensor)

    fun tupel(vararg komponenten: TypAusdruck): TypAusdruck =
        TypAusdruck.Parameterisiert(TypKernIds.Tupel, komponenten.toList())

    fun mengeVon(elementTyp: TypAusdruck): TypAusdruck = TypAusdruck.Parameterisiert(Menge, listOf(elementTyp))

    fun spaltenVektor(elementTyp: TypAusdruck, dimension: Int): TypAusdruck = TypAusdruck.Parameterisiert(
        SpaltenVektor,
        listOf(elementTyp, TypAusdruck.Literal(dimension.toString())),
    )

    fun zeilenVektor(elementTyp: TypAusdruck, dimension: Int): TypAusdruck = TypAusdruck.Parameterisiert(
        ZeilenVektor,
        listOf(elementTyp, TypAusdruck.Literal(dimension.toString())),
    )

    fun matrix(elementTyp: TypAusdruck, zeilen: Int, spalten: Int): TypAusdruck = TypAusdruck.Parameterisiert(
        Matrix,
        listOf(elementTyp, TypAusdruck.Literal(zeilen.toString()), TypAusdruck.Literal(spalten.toString())),
    )

    fun tensor(elementTyp: TypAusdruck, form: List<String>): TypAusdruck = TypAusdruck.Parameterisiert(
        Tensor,
        listOf(elementTyp, TypAusdruck.Literal(form.joinToString("×"))),
    )

    fun methode(argumente: List<TypAusdruck>, ziel: TypAusdruck): TypAusdruck = TypAusdruck.Parameterisiert(
        Methode,
        listOf(TypAusdruck.Parameterisiert(TypKernIds.Tupel, argumente), ziel),
    )

    fun vereinigung(vararg alternativen: TypAusdruck): TypAusdruck = TypAusdruck.Vereinigung(alternativen.toList())
}

class MathematikTypSystem : StandardTypSystem(
    atomTypen = listOf(
        AtomTypDefinition(MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Zahl, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.QuaternionZahl, MathematikTypen.Zahl),
        AtomTypDefinition(MathematikTypen.KomplexeZahl, MathematikTypen.QuaternionZahl),
        AtomTypDefinition(MathematikTypen.ReelleZahl, MathematikTypen.KomplexeZahl),
        AtomTypDefinition(MathematikTypen.RationaleZahl, MathematikTypen.ReelleZahl),
        AtomTypDefinition(MathematikTypen.GanzeZahl, MathematikTypen.RationaleZahl),
        AtomTypDefinition(MathematikTypen.NichtnegativeGanzeZahl, MathematikTypen.GanzeZahl),
        AtomTypDefinition(MathematikTypen.NatürlicheZahl, MathematikTypen.NichtnegativeGanzeZahl),
        AtomTypDefinition(MathematikTypen.Aussage, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Menge, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Mass, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Vektor, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.SpaltenVektor, MathematikTypen.Vektor),
        AtomTypDefinition(MathematikTypen.ZeilenVektor, MathematikTypen.Vektor),
        AtomTypDefinition(MathematikTypen.Matrix, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Tensor, MathematikTypen.Objekt),
        AtomTypDefinition(MathematikTypen.Methode, MathematikTypen.Objekt),
    ),
    konstruktoren = listOf(
        TypKonstruktorDefinition(TypKernIds.Tupel, standardVarianz = TypVarianz.Kovariant),
        TypKonstruktorDefinition(MathematikTypen.Menge, listOf(TypVarianz.Kovariant)),
        TypKonstruktorDefinition(MathematikTypen.SpaltenVektor, listOf(TypVarianz.Kovariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(MathematikTypen.ZeilenVektor, listOf(TypVarianz.Kovariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(MathematikTypen.Matrix, listOf(TypVarianz.Kovariant, TypVarianz.Invariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(MathematikTypen.Tensor, listOf(TypVarianz.Kovariant, TypVarianz.Invariant)),
        TypKonstruktorDefinition(
            MathematikTypen.Methode,
            varianzen = listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant),
        ),
    ),
    anschlussArtTypen = mapOf(
        MathematikAnschlussArten.Objekt.id to MathematikTypen.objekt,
        MathematikAnschlussArten.Zahl.id to MathematikTypen.zahl,
        MathematikAnschlussArten.Aussage.id to MathematikTypen.aussage,
        MathematikAnschlussArten.Menge.id to MathematikTypen.menge,
        MathematikAnschlussArten.Mass.id to MathematikTypen.mass,
        MathematikAnschlussArten.Vektor.id to MathematikTypen.vektor,
        MathematikAnschlussArten.SpaltenVektor.id to MathematikTypen.spaltenVektor,
        MathematikAnschlussArten.ZeilenVektor.id to MathematikTypen.zeilenVektor,
        MathematikAnschlussArten.Matrix.id to MathematikTypen.matrix,
        MathematikAnschlussArten.Tensor.id to MathematikTypen.tensor,
        MathematikAnschlussArten.Tupel.id to TypAusdruck.Unbekannt,
        MathematikAnschlussArten.Methode.id to TypAusdruck.Atom(MathematikTypen.Methode),
    ) + MathematikAnschlussArten.historischeMethodenIds.associateWith {
        TypAusdruck.Atom(MathematikTypen.Methode)
    },
)

fun AnschlussDaten.mitMathematikTyp(typ: TypAusdruck): AnschlussDaten =
    copy(vertrag = vertrag.copy(typ = typ))

fun AnschlussDaten.mitStrukturAnforderung(id: String): AnschlussDaten =
    copy(vertrag = vertrag.copy(anforderungen = vertrag.anforderungen + TypAnforderung.Struktur(id)))
