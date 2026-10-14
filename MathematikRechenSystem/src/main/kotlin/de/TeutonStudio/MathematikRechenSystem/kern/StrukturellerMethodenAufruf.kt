package de.TeutonStudio.MathematikRechenSystem.kern

/** Ein Methodenargument ist ein typisierter Methodenwert, kein Zahlenname. */
data class MethodenVariable(
    override val name: String,
    override val bindungsId: String,
    override val signatur: MethodenSignatur,
) : MethodenParameter, SignaturtragendeMethode {
    init { require(name.isNotBlank() && bindungsId.isNotBlank()) }
    override fun zuLatex(): String = name
}

/** Symbolischer Aufruf mit erhaltener Argumentstruktur. */
data class StrukturellerMethodenAufruf(
    val methode: MathematischesObjekt,
    val argumente: Tupel,
) : MathematischesObjekt {
    override fun zuLatex(): String = methode.zuLatex() + argumente.zuLatex()
}

/** Explizite numerische Projektion eines Methoden-Ergebnistupels, mathematisch 1-basiert. */
data class MethodenZahlKomponente(
    val aufruf: StrukturellerMethodenAufruf,
    val index: Int,
) : ZahlAusdruck {
    init { require(index > 0) }
    override fun zuLatex(): String = "\\operatorname{proj}_{$index}\\left(${aufruf.zuLatex()}\\right)"
}

/** Methodenwertige Projektion mit Signatur für weitere symbolische Anwendungen. */
data class MethodenWertKomponente(
    val aufruf: StrukturellerMethodenAufruf,
    val index: Int,
    override val signatur: MethodenSignatur,
) : MathematischesObjekt, SignaturtragendeMethode {
    init { require(index > 0) }
    override val name: String get() = "proj_$index"
    override fun zuLatex(): String = "\\operatorname{proj}_{$index}\\left(${aufruf.zuLatex()}\\right)"
}

internal fun StrukturellerMethodenAufruf.ersetzeAufruf(bindungen: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val ziel = ersetze(methode, bindungen)
    val argumente = ersetze(argumente, bindungen) as Tupel
    return if (ziel is MathematischeMethode) {
        if (ziel.prüfeDefinitionsBedingungen(argumente).any { it.entscheide().wahrheitswert == Wahrheitswert.Lüge })
            copy(methode = ziel, argumente = argumente)
        else ziel.wendeMathematischAlsTupelAn(argumente)
    } else copy(methode = ziel, argumente = argumente)
}

/** Erfasst Aufrufbedingungen vor der Substitution, auch wenn das Ergebnis den Aufruf vereinfacht. */
internal fun MathematischesObjekt.aufgerufeneBedingungen(
    bindungen: Map<String, MathematischesObjekt>,
    kontext: RechenKontext,
): Set<Aussage> {
    val bedingungen = linkedSetOf<Aussage>()
    fun besuche(wert: MathematischesObjekt, tiefe: Int) {
        require(tiefe <= 128) { "Methodenaufrufe sind zu tief verschachtelt." }
        val kinder: List<MathematischesObjekt> = when (wert) {
            is FallAusdruck -> {
                besuche(wert.aussage, tiefe + 1)
                val aussage = ersetze(wert.aussage, bindungen)
                when (aussage.entscheide(kontext).wahrheitswert) {
                    Wahrheitswert.Wahr -> besuche(wert.wahr, tiefe + 1)
                    Wahrheitswert.Lüge -> besuche(wert.lüge, tiefe + 1)
                    null -> {
                        bedingungen += wert.wahr.aufgerufeneBedingungen(bindungen, kontext).map { Implikation(aussage, it) }
                        bedingungen += wert.lüge.aufgerufeneBedingungen(bindungen, kontext).map { Implikation(Negation(aussage), it) }
                    }
                }
                emptyList()
            }
            is StrukturellerMethodenAufruf -> {
                besuche(wert.methode, tiefe + 1)
                besuche(wert.argumente, tiefe + 1)
                val ziel = ersetze(wert.methode, bindungen)
                if (ziel is MathematischeMethode) {
                    val argumente = ersetze(wert.argumente, bindungen) as Tupel
                    bedingungen += ziel.prüfeDefinitionsBedingungen(argumente)
                    val innereBindungen = ziel.parameter.mapIndexed { i, p -> (p.bindungsId ?: p.name) to argumente.elemente[i] }.toMap()
                    bedingungen += ziel.aufgerufeneVoraussetzungen(innereBindungen, kontext)
                }
                emptyList()
            }
            is MethodenZahlKomponente -> listOf(wert.aufruf)
            is MethodenWertKomponente -> listOf(wert.aufruf)
            is IndexKomponente -> listOf(wert.quelle) + wert.indizes
            is Tupel -> wert.elemente
            is Matrix -> wert.zeilen.flatten()
            is OrientierterVektor -> wert.werte
            is Tensor -> wert.werte
            is Addition -> wert.summanden
            is Multiplikation -> wert.faktoren
            is Division -> listOf(wert.dividend, wert.divisor)
            is Potenz -> listOf(wert.basis, wert.exponent)
            is Sinus -> listOf(wert.argument)
            is Cosinus -> listOf(wert.argument)
            is ArcSinus -> listOf(wert.argument)
            is ArcCosinus -> listOf(wert.argument)
            is Wurzel -> listOf(wert.argument)
            is Betrag -> listOf(wert.argument)
            is Konjugierte -> listOf(wert.argument)
            is Maximum -> wert.operanden
            is Minimum -> wert.operanden
            is InversesElement -> listOf(wert.argument)
            is StrukturierteDivision -> listOf(wert.dividend, wert.divisor)
            is Argument -> listOf(wert.zahl)
            is GradWinkel -> listOf(wert.grad)
            is WinkelAusBogenmass -> listOf(wert.argument)
            is WinkelBogenmass -> listOf(wert.winkel)
            is SinusGrad -> listOf(wert.winkel)
            is Negation -> listOf(wert.aussage)
            is Implikation -> listOf(wert.voraussetzung, wert.folgerung)
            is Äquivalenz -> listOf(wert.links, wert.rechts)
            is Adjunktion -> listOf(wert.links, wert.rechts)
            is Selbigkeit -> listOf(wert.links, wert.rechts)
            is TeilmengenBeziehung -> listOf(wert.links, wert.rechts)
            is EchteTeilmengeBeziehung -> listOf(wert.links, wert.rechts)
            is ObermengenBeziehung -> listOf(wert.links, wert.rechts)
            is Disjunktheit -> listOf(wert.links, wert.rechts)
            is OffeneDefinitionsBedingung -> listOf(wert.objekt)
            is Vereinigung -> wert.mengen
            is Schnitt -> wert.mengen + listOfNotNull(wert.grundMenge)
            is KartesischesProdukt -> wert.mengen
            is MengenDifferenz -> listOf(wert.links, wert.rechts)
            is Folgenraum -> listOf(wert.elementMenge)
            is Vektorraum -> listOf(wert.skalarMenge)
            is Matrizenraum -> listOf(wert.skalarMenge)
            is Tensorraum -> listOf(wert.elementMenge) + wert.dimensionen
            is BeschraenkteZahlmenge -> listOfNotNull(wert.untereGrenze, wert.obereGrenze)
            is SymbolischeZahlKomponente -> listOf(wert.quelle, wert.index) + wert.weitereIndizes
            is StrukturAchsenLaenge -> listOf(wert.quelle)
            is EndlicheIndexMenge -> listOf(wert.obergrenze)
            is Exponentialfunktion -> listOf(wert.argument)
            is NatürlicherLogarithmus -> listOf(wert.argument)
            is Logarithmus -> listOf(wert.basis, wert.argument)
            is KomplexeZahl -> listOf(wert.realteil, wert.imaginärteil)
            is Konjunktion -> wert.aussagen
            is Disjunktion -> wert.aussagen
            is Gleichheit -> listOf(wert.links, wert.rechts)
            is Ungleichheit -> listOf(wert.links, wert.rechts)
            is Vergleich -> listOf(wert.links, wert.rechts)
            is ElementBeziehung -> listOf(wert.element, wert.menge)
            is EndlicheMenge -> wert.elemente.toList()
            is Tupelraum -> wert.komponenten
            is ReellesIntervall -> listOf(wert.links, wert.rechts)
            else -> emptyList()
        }
        kinder.forEach { besuche(it, tiefe + 1) }
    }
    besuche(this, 0)
    return bedingungen
}

/** Bedingungen dürfen beim Ersetzen weder aus der Vorschrift noch aus Metadaten verschwinden. */
internal fun MathematischeMethode.aufgerufeneVoraussetzungen(
    bindungen: Map<String, MathematischesObjekt>,
    kontext: RechenKontext,
): Set<Aussage> = (listOf(vorschrift, zielMenge) + voraussetzungen + werteVorräte.values + listOfNotNull(effektiverWerteVorrat))
    .flatMap { it.aufgerufeneBedingungen(bindungen, kontext) }.toSet()
