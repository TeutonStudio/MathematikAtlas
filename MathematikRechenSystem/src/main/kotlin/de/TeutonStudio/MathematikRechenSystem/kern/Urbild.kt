package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypPrüfung
import de.TeutonStudio.TypSystem.TypAusdruck

/** Gemeinsame Urbildmenge aller Ziele; eine einwertige Methode behält strukturierte Werte. */
fun bildeUrbild(
    methode: Methode,
    zielmenge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
): PrädikatsLösung {
    val auswertbar = methode as? MathematischAuswertbareMethode
        ?: throw MethodenSignaturFehler("Urbild benötigt eine mathematisch auswertbare Methode mit Mengenvertrag.")
    val signatur = auswertbar.mathematischeSignatur
    val zielRaum = signatur.ergebnisse.singleOrNull()?.zielMenge ?: signatur.zielRaum
    val zielTyp = zielmenge.elementTypAusdruck()
    val erwartet = zielRaum.elementTypAusdruck()
    require(MathematischeTypen.typSystem.prüfe(zielTyp, erwartet) !is TypPrüfung.Inkompatibel ||
        MathematischeTypen.typSystem.prüfe(erwartet, zielTyp) !is TypPrüfung.Inkompatibel) {
        "Die Zielmenge besitzt eine zur Methodenausgabe inkompatible Elementstruktur."
    }
    if (zielmenge is EndlicheMenge) for (ziel in zielmenge.elemente) {
        val typ = urbildWertTyp(ziel)
        require(MathematischeTypen.typSystem.prüfe(typ, erwartet) !is TypPrüfung.Inkompatibel ||
            MathematischeTypen.typSystem.prüfe(erwartet, typ) !is TypPrüfung.Inkompatibel) {
            "Der Zielwert besitzt eine zur Methodenausgabe inkompatible Struktur."
        }
    }
    fun fertig(menge: MengenAusdruck, warnungen: List<String> = emptyList()) =
        PrädikatsLösung(menge, emptyList(), warnungen)
    if (zielmenge == LeereMenge || zielmenge is EndlicheMenge && zielmenge.elemente.isEmpty()) return fertig(LeereMenge)
    val konkret = runCatching { methode.alsMathematischeMethode("Urbild") }.getOrNull()
    val kanonischeZiele = if (signatur.ergebnisse.size == 1) Tupelraum(listOf(zielmenge)) else zielmenge
    fun ausgabe(wert: Tupel): MathematischesObjekt =
        if (signatur.ergebnisse.size == 1) wert.elemente.single() else wert
    fun argumentWert(wert: Tupel): MathematischesObjekt =
        if (signatur.argumente.size == 1) wert.elemente.single() else wert

    // Der gebundene Name darf weder freie Zielparameter noch Parameter der Methode einfangen.
    val belegt = zielmenge.enthalteneMethodenParameter().map { it.name }.toSet() +
        signatur.argumente.map { it.parameter.name } +
        (konkret?.freieMethodenParameter()?.map { it.name }.orEmpty())
    val binderName = generateSequence("urbildArgument") { it + "_" }.first { it !in belegt }
    val einzelParameter = signatur.argumente.singleOrNull()?.parameter
    val binder: MethodenParameter = if (einzelParameter != null) when (einzelParameter) {
        is Variable -> Variable(binderName)
        is MengenParameter -> MengenParameter(binderName)
        is AussagenParameter -> AussagenParameter(binderName)
        is MethodenVariable -> MethodenVariable(binderName, binderName, einzelParameter.signatur)
        is TypisiertesElement -> einzelParameter.copy(name = binderName, latex = binderName)
        else -> TypisiertesElement(binderName, "mathematik.objekt")
    } else TypisiertesElement(binderName, "mathematik.tupel")
    val symbolArgumente = if (einzelParameter != null) Tupel(listOf(binder)) else Tupel(
        signatur.argumente.mapIndexed { i, k ->
            val index = RationaleZahl.von((i + 1).toLong())
            if (k.parameter is ZahlAusdruck) SymbolischeZahlKomponente(binder, index)
            else IndexKomponente(binder, listOf(index))
        },
    )
    fun symbolisch(grund: String): PrädikatsLösung {
        // Der Aufruf bindet den gesamten Methodenvertrag erst nach einer konkreten
        // Belegung des gemeinsamen Binders. Mengen-/Aussageprojektionen sind keine Zahlen.
        val aufruf = StrukturellerMethodenAufruf(auswertbar, symbolArgumente)
        return fertig(PrädikatsMenge(binder, ElementBeziehung(aufruf, kanonischeZiele)), listOf(grund))
    }

    // Enumeration wird nur begonnen, wenn der gesamte Raum innerhalb des Budgets liegt.
    val endlich = endlicheUrbildArgumente(signatur.definitionsRaum)
    if (endlich != null) {
        val gefunden = linkedSetOf<MathematischesObjekt>()
        for (argumente in endlich) {
            require(argumente.anzahl == signatur.argumente.size) { "Der Definitionsraum besitzt eine falsche Argumentstruktur." }
            val ids = signatur.argumente.mapIndexed { i, k -> k.id to argumente.elemente[i] }.toMap()
            val bedingungen = auswertbar.prüfeDefinitionsBedingungen(ids, kontext)
            val substitution = signatur.argumente.mapIndexed { i, k ->
                (k.parameter.bindungsId ?: k.parameter.name) to argumente.elemente[i]
            }.toMap()
            val definitionen = konkret?.ergebnisTupel?.let { original ->
                definitionsBedingungen(original).map { ersetze(it, substitution) }
            }.orEmpty()
            when (Konjunktion(bedingungen.toList() + definitionen).entscheide(kontext).wahrheitswert) {
                Wahrheitswert.Lüge -> continue
                null -> return symbolisch("Die vollständige Urbilddefinition erhält offene Definitionsbedingungen.")
                Wahrheitswert.Wahr -> Unit
            }
            val anwendung = auswertbar.wendeMitIdentitätenAn(ids, kontext)
            val probe = Konjunktion(anwendung.bedingungen.toList() +
                ElementBeziehung(ausgabe(anwendung.ergebnis), zielmenge)).entscheide(kontext)
            when (probe.wahrheitswert) {
                Wahrheitswert.Wahr -> gefunden += argumentWert(argumente)
                Wahrheitswert.Lüge -> Unit
                null -> return symbolisch("Die vollständige Urbilddefinition erhält unentscheidbare Mitgliedschaften.")
            }
        }
        return fertig(if (gefunden.isEmpty()) LeereMenge else EndlicheMenge(gefunden))
    }

    // Zahlvariablen werden explizit neu gebunden: der Prädikatslöser arbeitet namensbasiert.
    require(konkret != null) { "Ein nichtendlicher Definitionsraum benötigt für das Urbild eine symbolische mathematische Vorschrift." }
    val faktoren = signatur.definitionsRaum.kartesischeFaktorenOderNull()
    if (faktoren != null && faktoren.size == signatur.argumente.size &&
        signatur.argumente.all { it.parameter is Variable } && faktoren.all { it.istZahlenmenge() }) {
        val freieNamen = zielmenge.enthalteneMethodenParameter().map { it.name }.toSet() +
            konkret.freieMethodenParameter().map { it.name }
        val variablen = signatur.argumente.mapIndexed { i, k ->
            val name = generateSequence("urbild${i + 1}") { it + "_" }.first { it !in freieNamen }
            Variable(name, k.parameter.zuLatex())
        }
        val substitution = signatur.argumente.mapIndexed { i, k ->
            (k.parameter.bindungsId ?: k.parameter.name) to variablen[i]
        }.toMap()
        val ausdruck = ersetze(ausgabe(konkret.ergebnisTupel), substitution)
        val zielBedingung = if (zielmenge is EndlicheMenge) Disjunktion(zielmenge.elemente.map {
            urbildGleichheit(ausdruck, it)
        }) else ElementBeziehung(ausdruck, zielmenge)
        val guards = konkret.voraussetzungen.map { ersetze(it, substitution) } +
            definitionsBedingungen(konkret.ergebnisTupel).map { ersetze(it, substitution) } +
            listOfNotNull(signatur.effektiverDefinitionsRaum?.let {
                ElementBeziehung(Tupel(variablen), ersetze(it, substitution) as MengenAusdruck)
            }) + konkret.aufgerufeneVoraussetzungen(substitution, kontext)
        val relevant = guards.filter { it.entscheide(kontext).wahrheitswert != Wahrheitswert.Wahr }
        val bedingung = if (relevant.isEmpty()) zielBedingung else Konjunktion(relevant + zielBedingung)
        return lösePrädikat(bedingung, signatur.argumente.mapIndexed { i, k ->
            GebundeneMengenVariable(variablen[i], ersetze(faktoren[i], substitution) as MengenAusdruck)
        }, kontext)
    }
    return symbolisch("Das Urbild bleibt als exakte strukturierte Mengendefinition erhalten.")
}

private fun urbildGleichheit(links: MathematischesObjekt, rechts: MathematischesObjekt): Aussage =
    if (links is Tupel && rechts is Tupel && links.anzahl == rechts.anzahl)
        Konjunktion(links.elemente.zip(rechts.elemente).map { (l, r) -> urbildGleichheit(l, r) })
    else Gleichheit(links, rechts)

/** Null Argumente besitzen genau die Belegung (); verschachtelte Elemente bleiben unberührt. */
private fun endlicheUrbildArgumente(raum: MengenAusdruck, budget: Int = 10000): List<Tupel>? {
    if (raum == LeereMenge) return emptyList()
    if (raum is EndlicheMenge) {
        if (raum.elemente.size > budget || raum.elemente.any { it !is Tupel }) return null
        return raum.elemente.filterIsInstance<Tupel>()
    }
    val faktoren = raum.kartesischeFaktorenOderNull() ?: return null
    if (faktoren.any { it == LeereMenge || it is EndlicheMenge && it.elemente.isEmpty() }) return emptyList()
    var argumente = listOf(emptyList<MathematischesObjekt>())
    for (faktor in faktoren) {
        val werte = when (faktor) {
            LeereMenge -> emptySet()
            is EndlicheMenge -> faktor.elemente
            else -> return null
        }
        if (argumente.size.toLong() * werte.size > budget) return null
        argumente = argumente.flatMap { bisher -> werte.map { bisher + it } }
    }
    return argumente.map(::Tupel)
}

/** Strukturprüfung ohne numerische Näherung oder implizite Tupelprojektion. */
private fun urbildWertTyp(wert: MathematischesObjekt): TypAusdruck {
    val zahl = TypAusdruck.Atom(MathematischeTypen.Zahl)
    return when (wert) {
        is SpaltenVektor -> TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor,
            listOf(zahl, TypAusdruck.Literal(wert.werte.size.toString())))
        is ZeilenVektor -> TypAusdruck.Parameterisiert(MathematischeTypen.ZeilenVektor,
            listOf(zahl, TypAusdruck.Literal(wert.werte.size.toString())))
        is Matrix -> TypAusdruck.Parameterisiert(MathematischeTypen.Matrix,
            listOf(zahl, TypAusdruck.Literal(wert.zeilen.size.toString()), TypAusdruck.Literal((wert.zeilen.firstOrNull()?.size ?: 0).toString())))
        is Tensor -> TypAusdruck.Parameterisiert(MathematischeTypen.Tensor,
            listOf(zahl) + wert.dimensionen.map { TypAusdruck.Literal(it.toString()) })
        is Tupel -> TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, wert.elemente.map(::urbildWertTyp))
        else -> EndlicheMenge(setOf(wert)).elementTypAusdruck()
    }
}
