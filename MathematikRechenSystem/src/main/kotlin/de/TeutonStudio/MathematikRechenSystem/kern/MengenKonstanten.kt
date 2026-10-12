package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/** Nullstellige Mengenoperatoren. IDs bleiben mit historischen Mengenknoten kompatibel. */
enum class MengenKonstante(
    val stabileId: String,
    val titel: String,
    val definitionLatex: String,
    val menge: MengenAusdruck,
) {
    LEER("leer", "Leere Menge", "\\varnothing:=\\{\\}", LeereMenge),
    N("N", "Natürliche Zahlen", "\\mathbb N:=\\{1,2,3,\\ldots\\}", NatürlicheZahlen),
    N0("N0", "Natürliche Zahlen mit Null", NichtnegativeGanzeZahlenSemantik.filterLatex, NichtnegativeGanzeZahlenSemantik.menge),
    Z("Z", "Ganze Zahlen", "\\mathbb Z:=\\{\\ldots,-2,-1,0,1,2,\\ldots\\}", GanzeZahlen),
    Q("Q", "Rationale Zahlen", "\\mathbb Q:=\\{a/b\\mid a\\in\\mathbb Z, b\\in\\mathbb N\\}", RationaleZahlen),
    R("R", "Reelle Zahlen", "\\mathbb R:=\\text{vollständiger geordneter Körper}", ReelleZahlen),
    C("C", "Komplexe Zahlen", "\\mathbb C:=\\{a+bi\\mid a,b\\in\\mathbb R, i^2=-1\\}", KomplexeZahlen),
    P("P", "Primzahlen", "\\mathbb P:=\\{p\\in\\mathbb N\\mid p>1,\\ p\\text{ hat genau zwei positive Teiler}\\}", Primzahlen),
    ZI("Zi", "Gaußsche ganze Zahlen", "\\mathbb Z[i]:=\\{a+bi\\mid a,b\\in\\mathbb Z\\}", GaußscheGanzeZahlen),
    PZI("PZi", "Gaußsche Primzahlen", "\\mathbb P_{\\mathbb Z[i]}:=\\{z\\in\\mathbb Z[i]\\mid z\\text{ ist ein Primelement}\\}", GaußschePrimzahlen);

    val signatur: OperatorSignatur get() = OperatorSignatur(
        "konstante", emptyList(), listOf(OperatorRolle("menge", FormelTyp.MENGE)),
    )
    val symbolLatex: String get() = menge.zuLatex()
    fun auswerten(): MengenAusdruck = menge

    companion object {
        fun vonId(id: String?): MengenKonstante? = entries.firstOrNull { it.stabileId == id }
        fun vonMenge(menge: MengenAusdruck): MengenKonstante? = entries.firstOrNull { it.menge == menge }
    }
}

internal fun mengenWahrheit(wahr: Boolean) = AussageErgebnis(
    if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
    if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
)

/** Vollständige Inklusionstabelle ausschließlich für die zehn bekannten Konstanten.
 * Negative Einträge sind durch Gegenbeispiele belegt, nicht aus fehlenden Kanten abgeleitet.
 */
internal fun konstanteTeilmenge(teil: MengenAusdruck, grund: MengenAusdruck): Boolean? {
    val a = MengenKonstante.vonMenge(teil) ?: return null
    val b = MengenKonstante.vonMenge(grund) ?: return null
    if (a == b || a == MengenKonstante.LEER) return true
    val obermengen = when (a) {
        MengenKonstante.LEER -> MengenKonstante.entries.toSet()
        MengenKonstante.P -> setOf(MengenKonstante.N, MengenKonstante.N0, MengenKonstante.Z, MengenKonstante.Q, MengenKonstante.R, MengenKonstante.C, MengenKonstante.ZI)
        MengenKonstante.N -> setOf(MengenKonstante.N0, MengenKonstante.Z, MengenKonstante.Q, MengenKonstante.R, MengenKonstante.C, MengenKonstante.ZI)
        MengenKonstante.N0 -> setOf(MengenKonstante.Z, MengenKonstante.Q, MengenKonstante.R, MengenKonstante.C, MengenKonstante.ZI)
        MengenKonstante.Z -> setOf(MengenKonstante.Q, MengenKonstante.R, MengenKonstante.C, MengenKonstante.ZI)
        MengenKonstante.Q -> setOf(MengenKonstante.R, MengenKonstante.C)
        MengenKonstante.R, MengenKonstante.ZI -> setOf(MengenKonstante.C)
        MengenKonstante.PZI -> setOf(MengenKonstante.ZI, MengenKonstante.C)
        MengenKonstante.C -> emptySet()
    }
    return b in obermengen
}

/** Exakte Probedivision; nach 10000 Kandidaten bleibt die Aussage offen. */
private fun istPrim(n: BigInteger): Boolean? {
    if (n < BigInteger.TWO) return false
    if (n == BigInteger.TWO) return true
    if (n.mod(BigInteger.TWO) == BigInteger.ZERO) return false
    var teiler = BigInteger.valueOf(3)
    repeat(10000) {
        if (teiler * teiler > n) return true
        if (n.mod(teiler) == BigInteger.ZERO) return false
        teiler += BigInteger.TWO
    }
    return null
}

/** Gemeinsame Mitgliedschaft konkreter rationaler und komplex-rationaler Zahlen. */
internal fun konstanteMitgliedschaft(element: MathematischesObjekt, menge: MengenAusdruck, kontext: RechenKontext): AussageErgebnis? {
    val konstante = MengenKonstante.vonMenge(menge) ?: return null
    if (konstante == MengenKonstante.LEER) return mengenWahrheit(false)
    val wert = (element as? ZahlAusdruck)?.let { vereinfache(it, kontext) } ?: element
    val real: RationaleZahl
    val imag: RationaleZahl
    when (wert) {
        is RationaleZahl -> { real = wert; imag = RationaleZahl.Null }
        is KomplexeZahl -> {
            real = vereinfache(wert.realteil, kontext) as? RationaleZahl ?: return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
            imag = vereinfache(wert.imaginärteil, kontext) as? RationaleZahl ?: return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
        else -> return null
    }
    val reell = imag.zähler.signum() == 0
    val ganz = real.nenner == BigInteger.ONE
    val gaussGanz = ganz && imag.nenner == BigInteger.ONE
    val wahr = when (konstante) {
        MengenKonstante.LEER -> false
        MengenKonstante.C -> true
        MengenKonstante.Q, MengenKonstante.R -> reell
        MengenKonstante.Z -> reell && ganz
        MengenKonstante.N -> reell && ganz && real.zähler.signum() > 0
        MengenKonstante.N0 -> reell && ganz && real.zähler.signum() >= 0
        MengenKonstante.ZI -> gaussGanz
        MengenKonstante.P -> if (!reell || !ganz) false else istPrim(real.zähler)
        MengenKonstante.PZI -> if (!gaussGanz) false else {
            val a = real.zähler.abs()
            val b = imag.zähler.abs()
            if (a.signum() != 0 && b.signum() != 0) istPrim(a * a + b * b)
            else {
                val achse = if (a.signum() == 0) b else a
                if (achse.mod(BigInteger.valueOf(4)) != BigInteger.valueOf(3)) false else istPrim(achse)
            }
        }
    }
    return wahr?.let(::mengenWahrheit) ?: AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Die exakte Primzahlprüfung überschreitet ihr Arbeitsbudget.")
}

/** Eigenschaften relativ zum reellen affinen Raum beziehungsweise seiner Standardtopologie. */
fun konstanteReelleMengeneigenschaft(menge: MengenAusdruck, eigenschaft: String): AussageStatus? {
    val quelle = MengenKonstante.vonMenge(menge) ?: return null
    if (konstanteTeilmenge(menge, ReelleZahlen) != true) return AussageStatus.UNENTSCHEIDBAR
    val wahr = when (eigenschaft) {
        "offen" -> quelle == MengenKonstante.LEER || quelle == MengenKonstante.R
        "abgeschlossen" -> quelle != MengenKonstante.Q
        "konvex" -> quelle == MengenKonstante.LEER || quelle == MengenKonstante.R
        else -> return null
    }
    return if (wahr) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
}
