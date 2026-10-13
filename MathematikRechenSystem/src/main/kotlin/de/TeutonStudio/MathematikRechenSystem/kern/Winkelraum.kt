package de.TeutonStudio.MathematikRechenSystem.kern

/** Gemeinsame Grenze für Richtungsvariablen, konkrete Richtungen und Polarumrechnung. */
const val MAXIMALE_RICHTUNGS_WINKEL = 64

/** W_n enthält genau ein Vorzeichen und n reelle Halbkreiswinkel in Grad. */
fun winkelraum(n: Int): Tupelraum {
    require(n in 0..MAXIMALE_RICHTUNGS_WINKEL) { "Die Winkelanzahl muss zwischen 0 und $MAXIMALE_RICHTUNGS_WINKEL liegen." }
    return Tupelraum(listOf(EndlicheMenge(setOf(RationaleZahl.von(-1),RationaleZahl.Eins)))+
        List(n) { ReellesIntervall(RationaleZahl.Null,false,RationaleZahl.von(180),true) })
}

/** Ein kanonischer Wn-Domainnachweis, unabhängig von LaTeX und Variablennamen. */
fun istWinkelraum(menge:MengenAusdruck):Boolean = menge is Tupelraum &&
    menge.komponenten.size in 1..MAXIMALE_RICHTUNGS_WINKEL+1 && menge==winkelraum(menge.komponenten.size-1)

/** Explizite Wn-Argumentnormalisierung; beliebige andere Tupeldomänen bleiben unberührt. */
fun normalisiereWinkelraumArgument(argument:MathematischesObjekt,domain:MengenAusdruck):MathematischesObjekt {
    require(istWinkelraum(domain)) { "Die Argumentnormalisierung benötigt einen kanonischen Winkelraum." }
    if(argument is TypisiertesElement) return argument // symbolische Bindung bleibt bis zur konkreten Anwendung bestehen
    val tupel=argument as? Tupel ?: error("Ein Winkelraumargument muss ein Richtungstupel sein.")
    val komponenten=tupel.elemente.mapIndexed { i,wert ->
        val zahl=when {
            i>0 && wert is GradWinkel -> wert.grad
            i>0 && wert is WinkelAusdruck -> vereinfache(Division(multiplikation(winkelBogenmass(wert),RationaleZahl.von(180)),Pi))
            wert is ZahlAusdruck -> wert
            else -> error("Die Richtung benötigt ein Zahlvorzeichen und Halbkreiswinkel.")
        }
        when(DomaenenAuswerter.wert(zahl)) { is DomaenenErgebnis.NichtDefiniert,is DomaenenErgebnis.NichtEndlich -> error("Richtungswerte müssen endlich und definiert sein."); else -> Unit }
        zahl
    }
    val normal=Tupel(komponenten)
    require(ElementBeziehung(normal,domain).entscheide().wahrheitswert!=Wahrheitswert.Lüge) { "Das Richtungstupel liegt außerhalb des Winkelraums (Vorzeichen ±1, Halbkreiswinkel in [0°,180°))." }
    return normal
}
