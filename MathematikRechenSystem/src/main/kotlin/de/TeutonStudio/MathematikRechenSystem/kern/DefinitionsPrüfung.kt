package de.TeutonStudio.MathematikRechenSystem.kern

/** Conservative remainder for a domain that the symbolic kernel cannot certify yet. */
data class OffeneDefinitionsBedingung(val objekt: MathematischesObjekt) : Aussage {
    override fun entscheide(kontext: RechenKontext) = AussageErgebnis(null, EntscheidungsStatus.Unbekannt,
        "Die Definitionslage dieses Ausdrucks ist nicht nachgewiesen.")
    override fun zuLatex() = "\\operatorname{definiert}\\left(${objekt.zuLatex()}\\right)"
}

/** Conditions are collected BEFORE simplification can cancel a denominator or a function. */
fun definitionsBedingungen(objekt: MathematischesObjekt): List<Aussage> {
    fun kinder(vararg teile: MathematischesObjekt) = teile.flatMap(::definitionsBedingungen)
    fun nz(x: ZahlAusdruck) = Ungleichheit(x, RationaleZahl.Null)
    return when (objekt) {
        is RationaleZahl, is Variable, is AllgemeinerParameter, is MathematischeKonstante,
        is WahrheitsKonstante, is AussagenParameter, is TypisiertesElement, is MengenParameter -> emptyList()
        is Addition -> objekt.summanden.flatMap(::definitionsBedingungen)
        is Multiplikation -> objekt.faktoren.flatMap(::definitionsBedingungen)
        is Division -> kinder(objekt.dividend, objekt.divisor) + nz(objekt.divisor)
        is StrukturierteDivision -> kinder(objekt.dividend, objekt.divisor) + nz(objekt.divisor)
        is Potenz -> kinder(objekt.basis, objekt.exponent) + when (val n = objekt.exponent) {
            is RationaleZahl -> if (n.nenner == java.math.BigInteger.ONE) {
                if (n.zähler.signum() <= 0) listOf(nz(objekt.basis)) else emptyList()
            } else listOf(OffeneDefinitionsBedingung(objekt))
            else -> listOf(OffeneDefinitionsBedingung(objekt))
        }
        is Konjugierte -> kinder(objekt.argument)
        is Betrag -> kinder(objekt.argument)
        is GradWinkel -> kinder(objekt.grad)
        is WinkelAusBogenmass -> kinder(objekt.argument)
        is WinkelBogenmass -> kinder(objekt.winkel)
        is SinusGrad -> kinder(objekt.winkel)
        is Sinus -> kinder(objekt.argument)
        is Cosinus -> kinder(objekt.argument)
        is Exponentialfunktion -> kinder(objekt.argument)
        is Wurzel -> kinder(objekt.argument) // principal complex square root exists for every finite number
        is KomplexeZahl -> kinder(objekt.realteil, objekt.imaginärteil)
        is ArcSinus -> kinder(objekt.argument) + objekt.reelleDefinitionsBedingung()
        is ArcCosinus -> kinder(objekt.argument) + objekt.reelleDefinitionsBedingung()
        is NatürlicherLogarithmus -> kinder(objekt.argument) + nz(objekt.argument)
        is Logarithmus -> kinder(objekt.basis, objekt.argument) + listOf(nz(objekt.basis),
            Ungleichheit(objekt.basis, RationaleZahl.Eins), nz(objekt.argument))
        is Gleichheit -> kinder(objekt.links, objekt.rechts)
        is Selbigkeit -> kinder(objekt.links, objekt.rechts)
        is Ungleichheit -> kinder(objekt.links, objekt.rechts)
        is Vergleich -> kinder(objekt.links, objekt.rechts)
        is Negation -> kinder(objekt.aussage)
        is Konjunktion -> objekt.aussagen.flatMap(::definitionsBedingungen)
        is Disjunktion -> objekt.aussagen.flatMap(::definitionsBedingungen)
        is Implikation -> kinder(objekt.voraussetzung, objekt.folgerung)
        is Äquivalenz -> kinder(objekt.links, objekt.rechts)
        is Adjunktion -> kinder(objekt.links, objekt.rechts)
        is ElementBeziehung -> kinder(objekt.element, objekt.menge)
        is Matrix -> objekt.zeilen.flatten().flatMap(::definitionsBedingungen)
        is SpaltenVektor -> objekt.werte.flatMap(::definitionsBedingungen)
        is ZeilenVektor -> objekt.werte.flatMap(::definitionsBedingungen)
        is UnterraumMenge -> (objekt.richtungen.flatten() + objekt.stuetzpunkt.orEmpty()).flatMap(::definitionsBedingungen) + (objekt.richtungen.flatten() + objekt.stuetzpunkt.orEmpty()).map { ElementBeziehung(it, ReelleZahlen) }
        is VerschobeneKoordinatenMenge -> definitionsBedingungen(objekt.original) + objekt.verschiebung.flatMap(::definitionsBedingungen)
        is Tupel -> objekt.elemente.flatMap(::definitionsBedingungen)
        is EndlicheMenge -> objekt.elemente.flatMap(::definitionsBedingungen)
        is OffeneDefinitionsBedingung -> emptyList()
        // Sets themselves are definitions; their bound predicates are checked at membership time.
        is MengenAusdruck -> emptyList()
        else -> listOf(OffeneDefinitionsBedingung(objekt))
    }.distinct()
}

fun prüfeDefinitionslage(objekt: MathematischesObjekt, kontext: RechenKontext): AussageErgebnis {
    val bedingungen = definitionsBedingungen(objekt).filterNot { it in kontext.annahmen }
    val e = Konjunktion(bedingungen).entscheide(kontext)
    return if (e.wahrheitswert == Wahrheitswert.Lüge) e.copy(status = EntscheidungsStatus.NichtAuswertbar,
        begründung = "Der Ausdruck verletzt eine Definitionsbedingung.") else e
}
