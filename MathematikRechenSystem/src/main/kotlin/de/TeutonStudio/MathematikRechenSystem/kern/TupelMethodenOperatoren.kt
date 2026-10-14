package de.TeutonStudio.MathematikRechenSystem.kern

/** Exakte Inversion rational-affiner R^n-Abbildungen; keine aus Stichproben vermutete Linearität. */
fun MathematischeMethode.invertiereRationalAffin(): MathematischeMethode {
    val variablen = parameter.map { it as? Variable ?: error("Affine Inversion benötigt skalare Zahlenparameter.") }
    val n = variablen.size
    require(ergebnisTupel.anzahl == n) { "Affine Inversion benötigt gleich viele Ein- und Ausgabekomponenten." }
    require(werteVorräte.values.all { prüfeTeilmenge(it, ReelleZahlen).wahrheitswert == Wahrheitswert.Wahr }) { "Affine Inversion benötigt reelle Argumenträume." }
    if (n == 0) return copy(name = "$name^{-1}")
    val zeilen = mutableListOf<List<RationaleZahl>>()
    val verschiebung = mutableListOf<RationaleZahl>()
    ergebnisTupel.elemente.forEach { ausgabe ->
        var rest = ausgabe as? ZahlAusdruck ?: error("Affine Inversion benötigt Zahlenergebnisse.")
        val koeffizienten = variablen.map { v ->
            val polynom = zerlegeAlsPolynom(rest, v, maximalerGrad = 1) ?: error("Die Vorschrift ist nicht rational-affin.")
            val faktor = vereinfache(polynom.koeffizienten.getOrElse(1) { RationaleZahl.Null }) as? RationaleZahl
                ?: error("Ein affiner Koeffizient hängt von weiteren Parametern ab.")
            rest = polynom.koeffizienten[0]
            faktor
        }
        zeilen += koeffizienten
        verschiebung += vereinfache(rest) as? RationaleZahl ?: error("Die affine Verschiebung ist nicht rational.")
    }
    val invers = Matrix(zeilen).inverseRational()
    val neueParameter = ausgabeNamen.map { ausgabe -> Variable(ausgabe, bindungsId = ergebnisIdentitäten.getValue(ausgabe)) }
    val differenz = neueParameter.mapIndexed { i, p -> subtraktion(p, verschiebung[i]) }
    val rückwerte = invers.zeilen.map { zeile -> vereinfache(addition(zeile.mapIndexed { i, a -> multiplikation(a, differenz[i]) })) }
    val rückArgumente = Tupel(rückwerte)
    return MathematischeMethode(
        name = "$name^{-1}", parameter = neueParameter, vorschrift = rückArgumente,
        zielMenge = mathematischeSignatur.kanonischerArgumentRaum,
        werteVorräte = neueParameter.mapIndexed { i, p -> p.name to mathematischeSignatur.ergebnisse[i].zielMenge }.toMap(),
        ausgabeNamen = parameter.map { it.name },
        argumentIdentitäten = ergebnisIdentitäten,
        ergebnisIdentitäten = argumentIdentitäten,
        voraussetzungen = prüfeDefinitionsBedingungen(rückArgumente),
    )
}

/** Partielle Ableitung aller Ergebniskomponenten; die Argumentidentitäten bleiben unverändert. */
fun MathematischeMethode.leiteNachArgumentAb(argumentId: String): MathematischeMethode {
    val variable = parameter.singleOrNull { argumentIdentitäten.getValue(it.name) == argumentId } as? Variable
        ?: error("Die Ableitungsidentität bezeichnet keinen skalaren Zahlenparameter.")
    fun ableitung(wert: MathematischesObjekt): MathematischesObjekt = when (wert) {
        is ZahlAusdruck -> ableiten(wert, variable).ergebnis
        is Tupel -> Tupel(wert.elemente.map(::ableitung))
        is OrientierterVektor -> when (wert) {
            is SpaltenVektor -> SpaltenVektor(wert.werte.map { ableiten(it, variable).ergebnis })
            is ZeilenVektor -> ZeilenVektor(wert.werte.map { ableiten(it, variable).ergebnis })
        }
        is Matrix -> Matrix(wert.zeilen.map { zeile -> zeile.map { ableiten(it, variable).ergebnis } })
        is Tensor -> wert.copy(werte = wert.werte.map { ableiten(it, variable).ergebnis })
        else -> error("Für diese Ergebniskomponente ist keine Ableitung definiert.")
    }
    val ergebnis = Tupel(ergebnisTupel.elemente.map(::ableitung))
    return copy(name = "\\partial_{$argumentId}$name", vorschrift = ergebnis, übersetzungen = übersetzungen.alsHerkunft(),
        zielMenge = Tupelraum(ergebnis.elemente.map(::analytischerErgebnisRaum)),
        voraussetzungen = voraussetzungen + definitionsBedingungen(ergebnis))
}

/** Polynom-Stammfunktion nach einem Argument; Integrationskonstanten bleiben explizite Außenquellen. */
fun MathematischeMethode.integrierePolynomiell(
    argumentId: String,
    konstanten: List<Variable> = ausgabeNamen.map { Variable("C_$it", bindungsId = java.util.UUID.randomUUID().toString()) },
): MathematischeMethode {
    val variable = parameter.singleOrNull { argumentIdentitäten.getValue(it.name) == argumentId } as? Variable
        ?: error("Die Integrationsidentität bezeichnet keinen Zahlenparameter.")
    require(konstanten.size == ausgabeNamen.size && konstanten.map { it.bindungsId ?: it.name }.distinct().size == konstanten.size)
    require(konstanten.none { k -> parameter.any { (it.bindungsId ?: it.name) == (k.bindungsId ?: k.name) } })
    val ergebnis = ergebnisTupel.elemente.mapIndexed { i, wert ->
        val polynom = zerlegeAlsPolynom(wert as? ZahlAusdruck ?: error("Polynomintegration benötigt Zahlenergebnisse."), variable)
            ?: error("Die Ergebniskomponente ist kein unterstütztes Polynom.")
        addition(polynom.koeffizienten.mapIndexed { grad, c ->
            multiplikation(Division(c, RationaleZahl.von((grad + 1).toLong())), Potenz(variable, RationaleZahl.von((grad + 1).toLong())))
        } + konstanten[i])
    }
    return copy(name = "\\int $name", vorschrift = Tupel(ergebnis), übersetzungen = übersetzungen.alsHerkunft(), zielMenge = Tupelraum(ergebnis.map(::analytischerErgebnisRaum)))
}

/** Ableitungs-/Stammfunktionsräume übernehmen keine unbegründeten Wertebereichsschranken der Ursprungsmethode. */
private fun analytischerErgebnisRaum(wert: MathematischesObjekt): MengenAusdruck = when (wert) {
    is ZahlAusdruck -> KomplexeZahlen
    is Tupel -> Tupelraum(wert.elemente.map(::analytischerErgebnisRaum))
    is SpaltenVektor -> Vektorraum(VektorOrientierung.Spalte, wert.werte.size, KomplexeZahlen)
    is ZeilenVektor -> Vektorraum(VektorOrientierung.Zeile, wert.werte.size, KomplexeZahlen)
    is Matrix -> Matrizenraum(wert.zeilenAnzahl, wert.spaltenAnzahl, KomplexeZahlen)
    is Tensor -> Tensorraum(KomplexeZahlen, wert.dimensionen.map { RationaleZahl.von(it.toLong()) })
    else -> error("Kein analytischer Ergebnisraum registriert.")
}
