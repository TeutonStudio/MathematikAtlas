package de.TeutonStudio.MathematikRechenSystem.kern

data class TermVereinfachung(
    val objekt: MathematischesObjekt,
    val schritte: List<UmformungsSchritt> = emptyList(),
    val entscheidung: AussageErgebnis? = null,
)

fun vereinfacheTerm(
    objekt: MathematischesObjekt,
    kontext: RechenKontext,
): TermVereinfachung {
    val lage = prüfeDefinitionslage(objekt, kontext)
    if (lage.wahrheitswert != Wahrheitswert.Wahr) return TermVereinfachung(objekt,
        listOf(UmformungsSchritt(objekt, objekt, "auswertung.definitionslage", "Definitionsbedingungen prüfen",
            lage.begründung.ifBlank { "Vereinfachung nur unter den angezeigten Definitionsbedingungen möglich." },
            bedingungen = definitionsBedingungen(objekt))),
        lage.copy(wahrheitswert = null))
    if (objekt is Aussage) return vereinfachePrädikatMitSchritten(objekt, kontext)
    if (objekt is ZahlAusdruck) return vereinfacheZahlMitDetails(objekt, kontext)

    val vereinfacht: MathematischesObjekt = when (objekt) {
        is Aussage -> vereinfacheAussage(objekt, kontext)
        is Matrix -> Matrix(objekt.zeilen.map { zeile ->
            zeile.map { wert -> vereinfache(wert, kontext) }
        })
        is SpaltenVektor -> SpaltenVektor(objekt.werte.map { wert -> vereinfache(wert, kontext) })
        is ZeilenVektor -> ZeilenVektor(objekt.werte.map { wert -> vereinfache(wert, kontext) })
        is Tupel -> Tupel(objekt.elemente.map { element -> vereinfacheTerm(element, kontext).objekt })
        is EndlicheMenge -> {
            val elemente = objekt.elemente.mapTo(linkedSetOf()) { element ->
                vereinfacheTerm(element, kontext).objekt
            }
            if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente)
        }
        is ReellesIntervall -> reellesIntervall(
            vereinfache(objekt.links, kontext),
            objekt.linksOffen,
            vereinfache(objekt.rechts, kontext),
            objekt.rechtsOffen,
            kontext,
        )
        is Vereinigung -> vereinige(objekt.mengen.map { menge ->
            vereinfacheTerm(menge, kontext).objekt as MengenAusdruck
        })
        is Schnitt -> schneide(
            objekt.mengen.map { menge -> vereinfacheTerm(menge, kontext).objekt as MengenAusdruck },
            objekt.grundMenge?.let { grundMenge ->
                vereinfacheTerm(grundMenge, kontext).objekt as MengenAusdruck
            },
        )
        is MengenDifferenz -> mengenDifferenz(
            vereinfacheTerm(objekt.links, kontext).objekt as MengenAusdruck,
            vereinfacheTerm(objekt.rechts, kontext).objekt as MengenAusdruck,
        )
        is DefinierteMenge -> {
            val variablen = objekt.variablen.map { gebunden ->
                gebunden.copy(
                    grundMenge = vereinfacheTerm(gebunden.grundMenge, kontext).objekt as MengenAusdruck,
                )
            }
            when (val bedingung = vereinfacheAussage(objekt.bedingung, kontext)) {
                is WahrheitsKonstante -> if (bedingung.wert) lösungsRaum(variablen) else LeereMenge
                else -> DefinierteMenge(variablen, bedingung)
            }
        }
        else -> objekt
    }

    if (vereinfacht == objekt) return if (objekt is Aussage) {
        val e = objekt.entscheide(kontext)
        TermVereinfachung(objekt, listOf(UmformungsSchritt(objekt, objekt,
            "aussage.entscheidung", "Prädikat prüfen", e.begründung.ifBlank {
                if (objekt.freieMethodenParameter().isNotEmpty()) "Freie Symbole bleiben im Prädikat erhalten."
                else "Mit den verfügbaren Verfahren nicht entscheidbar."
            })))
    } else TermVereinfachung(objekt)
    return TermVereinfachung(
        objekt = vereinfacht,
        schritte = listOf(
            UmformungsSchritt(
                vorher = objekt,
                nachher = vereinfacht,
                regelId = "standard.term-vereinfachen",
                titel = "Term vereinfachen",
                erklärung = "Unterterme wurden mit den für ihren Typ registrierten Regeln vereinfacht.",
            ),
        ),
    )
}

fun vereinfacheAussage(
    aussage: Aussage,
    kontext: RechenKontext,
): Aussage {
    if (prüfeDefinitionslage(aussage, kontext).wahrheitswert != Wahrheitswert.Wahr) return aussage
    if (aussage in kontext.annahmen) return WahrheitsKonstante(true)
    if (Negation(aussage) in kontext.annahmen) return WahrheitsKonstante(false)
    val strukturell = when (aussage) {
        is Selbigkeit -> Selbigkeit(
            vereinfacheTerm(aussage.links, kontext).objekt,
            vereinfacheTerm(aussage.rechts, kontext).objekt,
        )
        is Gleichheit -> Gleichheit(
            vereinfacheTerm(aussage.links, kontext).objekt,
            vereinfacheTerm(aussage.rechts, kontext).objekt,
        )
        is Ungleichheit -> Ungleichheit(
            vereinfacheTerm(aussage.links, kontext).objekt,
            vereinfacheTerm(aussage.rechts, kontext).objekt,
        )
        is Vergleich -> Vergleich(
            vereinfache(aussage.links, kontext),
            aussage.art,
            vereinfache(aussage.rechts, kontext),
        )
        is Negation -> Negation(vereinfacheAussage(aussage.aussage, kontext))
        is Konjunktion -> Konjunktion(aussage.aussagen.map { teil -> vereinfacheAussage(teil, kontext) })
        is Disjunktion -> Disjunktion(aussage.aussagen.map { teil -> vereinfacheAussage(teil, kontext) })
        is Implikation -> Implikation(
            vereinfacheAussage(aussage.voraussetzung, kontext),
            vereinfacheAussage(aussage.folgerung, kontext),
        )
        is Äquivalenz -> Äquivalenz(
            vereinfacheAussage(aussage.links, kontext),
            vereinfacheAussage(aussage.rechts, kontext),
        )
        is Adjunktion -> Adjunktion(
            vereinfacheAussage(aussage.links, kontext),
            vereinfacheAussage(aussage.rechts, kontext),
        )
        else -> aussage
    }
    val normal = when (strukturell) {
        is Konjunktion -> strukturell.aussagen.filterNot { it == WahrheitsKonstante(true) }.let {
            if (it.size == 1) it.single() else Konjunktion(it)
        }
        is Disjunktion -> strukturell.aussagen.filterNot { it == WahrheitsKonstante(false) }.let {
            if (it.size == 1) it.single() else Disjunktion(it)
        }
        is Negation -> (strukturell.aussage as? Negation)?.aussage ?: strukturell
        else -> strukturell
    }
    return when (normal.entscheide(kontext).wahrheitswert) {
        Wahrheitswert.Wahr -> WahrheitsKonstante(true)
        Wahrheitswert.Lüge -> WahrheitsKonstante(false)
        null -> normal
    }
}


private fun vereinfacheZahlMitDetails(objekt: ZahlAusdruck, kontext: RechenKontext): TermVereinfachung {
    val details = mutableListOf<UmformungsSchritt>()
    fun teil(x: ZahlAusdruck): ZahlAusdruck {
        val r = vereinfacheTerm(x, kontext)
        details += r.schritte
        return r.objekt as ZahlAusdruck
    }
    val vorbereitet = when (objekt) {
        is Addition -> addition(objekt.summanden.map(::teil))
        is Multiplikation -> multiplikation(objekt.faktoren.map(::teil))
        is Division -> Division(teil(objekt.dividend), teil(objekt.divisor))
        is Potenz -> Potenz(teil(objekt.basis), teil(objekt.exponent))
        is Sinus -> Sinus(teil(objekt.argument))
        is Cosinus -> Cosinus(teil(objekt.argument))
        else -> objekt
    }
    val neu = vereinfache(vorbereitet, kontext)
    return if (neu == objekt) TermVereinfachung(objekt) else TermVereinfachung(neu,
        listOf(UmformungsSchritt(objekt, neu, "term.exakt", "Zahlterm vereinfachen",
            "Unterterme vereinfachen und die verbleibende Operation exakt ausführen.", unterschritte = details)))
}

private fun vereinfachePrädikatMitSchritten(original: Aussage, kontext: RechenKontext): TermVereinfachung {
    val schritte = mutableListOf<UmformungsSchritt>()
    var stand = original
    fun ersetzeTeil(teil: MathematischesObjekt, baue: (MathematischesObjekt) -> Aussage) {
        val r = vereinfacheTerm(teil, kontext)
        val neu = baue(r.objekt)
        if (neu != stand) {
            schritte += UmformungsSchritt(stand, neu, "prädikat.unterterm", "Unterterm vereinfachen",
                "Der vereinfachte Unterterm wird an derselben Stelle eingesetzt.", unterschritte = r.schritte, beziehung = SchrittBeziehung.Äquivalenz)
            stand = neu
        }
    }
    when (original) {
        is Gleichheit -> {
            ersetzeTeil(original.links) { Gleichheit(it, original.rechts) }
            ersetzeTeil(original.rechts) { Gleichheit((stand as Gleichheit).links, it) }
        }
        is Selbigkeit -> {
            ersetzeTeil(original.links) { Selbigkeit(it, original.rechts) }
            ersetzeTeil(original.rechts) { Selbigkeit((stand as Selbigkeit).links, it) }
        }
        is Ungleichheit -> {
            ersetzeTeil(original.links) { Ungleichheit(it, original.rechts) }
            ersetzeTeil(original.rechts) { Ungleichheit((stand as Ungleichheit).links, it) }
        }
        is Vergleich -> {
            ersetzeTeil(original.links) { Vergleich(it as ZahlAusdruck, original.art, original.rechts) }
            ersetzeTeil(original.rechts) { Vergleich((stand as Vergleich).links, original.art, it as ZahlAusdruck) }
        }
        is Negation -> ersetzeTeil(original.aussage) { Negation(it as Aussage) }
        is Konjunktion -> original.aussagen.forEachIndexed { i, a ->
            ersetzeTeil(a) { neu -> Konjunktion((stand as Konjunktion).aussagen.toMutableList().also { it[i] = neu as Aussage }) }
        }
        is Disjunktion -> original.aussagen.forEachIndexed { i, a ->
            ersetzeTeil(a) { neu -> Disjunktion((stand as Disjunktion).aussagen.toMutableList().also { it[i] = neu as Aussage }) }
        }
        else -> Unit
    }
    val neu = vereinfacheAussage(stand, kontext)
    val e = neu.entscheide(kontext)
    val grund = e.begründung.ifBlank { if (e.wahrheitswert != null) "Exakte Prüfung des vereinfachten Prädikats."
        else if (neu.freieMethodenParameter().isNotEmpty()) "Freie Symbole bleiben im vereinfachten Prädikat erhalten."
        else "Mit den verfügbaren Verfahren nicht entscheidbar." }
    schritte += UmformungsSchritt(stand, neu, "prädikat.entscheiden", "Prädikat prüfen", grund)
    return TermVereinfachung(neu, schritte, e.copy(begründung = grund))
}
