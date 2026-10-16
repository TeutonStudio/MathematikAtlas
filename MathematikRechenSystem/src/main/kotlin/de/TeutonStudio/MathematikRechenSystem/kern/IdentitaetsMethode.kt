package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypVariablenId

/** Neutrale Aufruf-Capability mit semantischen Komponenten-IDs als Argument- und Ergebnisschlüsseln. */
interface AllgemeinAuswertbareMethode : SignaturtragendeMethode {
    fun rufeAuf(argumente: Map<String, AtlasWert>): Map<String, MethodenAufrufErgebnis>
}

/** Ein Ergebnis darf vollständige Metadaten des angegebenen Arguments durchreichen. */
data class MethodenAufrufErgebnis(val wert: AtlasWert, val durchgereichtesArgument: String? = null)

/** Polymorphe Identität mit gemeinsamer Typvariable und ohne erfundene Universalmenge. */
data object IdentitaetsMethode : AllgemeinAuswertbareMethode, LatexDarstellbar {
    const val ARGUMENT = "identitaet:argument"
    const val ERGEBNIS = "identitaet:ergebnis"
    override val name = "Identität"
    override val signatur = MethodenSignatur(
        listOf(MethodenKomponente(ARGUMENT, "x", 0, TypAusdruck.Variable(TypVariablenId("identitaet:T")))),
        listOf(MethodenKomponente(ERGEBNIS, "x", 0, TypAusdruck.Variable(TypVariablenId("identitaet:T")))),
    )
    override fun rufeAuf(argumente: Map<String, AtlasWert>): Map<String, MethodenAufrufErgebnis> {
        require(argumente.keys == setOf(ARGUMENT)) { "Identität benötigt genau ein Argument." }
        return mapOf(ERGEBNIS to MethodenAufrufErgebnis(argumente.getValue(ARGUMENT), ARGUMENT))
    }
    override fun zuLatex() = "x\\mapsto x"
}

/** Identität auf deklarierter Menge mit den vorhandenen mathematischen Aufrufprüfungen. */
fun identitaetAuf(menge: MengenAusdruck): MathematischeMethode {
    val id = IdentitaetsMethode.ARGUMENT
    val parameter: MethodenParameter = when (val typ = menge.elementTypAusdruck()) {
        is TypAusdruck.Atom -> when (typ.id) {
            MathematischeTypen.Zahl, MathematischeTypen.Reell, MathematischeTypen.Komplex,
            MathematischeTypen.Rational, MathematischeTypen.Ganz, MathematischeTypen.Natuerlich,
            MathematischeTypen.NatuerlichMitNull -> Variable("x", bindungsId = id)
            MathematischeTypen.Aussage -> AussagenParameter("x", bindungsId = id)
            MathematischeTypen.Menge -> MengenParameter("x", bindungsId = id)
            else -> AllgemeinerParameter("x", bindungsId = id)
        }
        else -> AllgemeinerParameter("x", bindungsId = id)
    }
    return MathematischeMethode("Identität", listOf(parameter), Tupel(listOf(parameter)), Tupelraum(listOf(menge)),
        mapOf("x" to menge), argumentIdentitäten = mapOf("x" to id),
        ergebnisIdentitäten = mapOf("wert" to IdentitaetsMethode.ERGEBNIS))
}

/** Eine typisierte Identität benötigt keine erfundene mathematische Universalmenge. */
data class TypisierteIdentitaetsMethode(val elementTyp: TypAusdruck) : AllgemeinAuswertbareMethode, LatexDarstellbar {
    override val name = IdentitaetsMethode.name
    override val signatur = MethodenSignatur(
        listOf(MethodenKomponente(IdentitaetsMethode.ARGUMENT, "x", 0, elementTyp)),
        listOf(MethodenKomponente(IdentitaetsMethode.ERGEBNIS, "x", 0, elementTyp)))
    override fun rufeAuf(argumente: Map<String, AtlasWert>): Map<String, MethodenAufrufErgebnis> {
        val ergebnis = IdentitaetsMethode.rufeAuf(argumente)
        val typ = konkreterIdentitaetsTyp(argumente.getValue(IdentitaetsMethode.ARGUMENT))
        require(MathematischeTypen.typSystem.prüfe(typ, elementTyp) !is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel) { "Das Argument passt nicht zum Typ der Identität." }
        return ergebnis
    }
    override fun zuLatex() = IdentitaetsMethode.zuLatex()
}

private fun konkreterIdentitaetsTyp(wert: AtlasWert): TypAusdruck = when (wert) {
    is Methode -> wert.methodenTypAusdruck()
    is de.TeutonStudio.TypSystem.TypTragend -> wert.typAusdruck
    is Aussage -> TypAusdruck.Atom(MathematischeTypen.Aussage)
    is MengenAusdruck -> TypAusdruck.Parameterisiert(MathematischeTypen.Menge, listOf(wert.elementTypAusdruck()))
    is Tupel -> TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, wert.elemente.map(::konkreterIdentitaetsTyp))
    is MathematischesObjekt -> if (wert.freieMethodenParameter().isNotEmpty()) TypAusdruck.Unbekannt else runCatching { inferiereZielmenge(wert).elementTypAusdruck() }.getOrDefault(TypAusdruck.Unbekannt)
    else -> TypAusdruck.Unbekannt
}
