package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Eine parametrisierte diskrete Dynamik `z_(n+1) = schritt(z_n, p)`.
 *
 * Die Reihenfolge der beiden Methodenparameter ist Teil des Vertrags: zuerst der
 * Zustand, danach der Parameter. [start] darf ein skalarer Startwert oder eine
 * einstellige skalare Methode des Parameters sein.
 */
data class OrbitFamilie(
    val schritt: Methode,
    val start: MathematischesObjekt,
) : MathematischesObjekt {
    init {
        val mathematisch = schritt.alsMathematischeMethode("eine Orbititeration")
        require(mathematisch.parameter.size == 2) {
            "Eine Orbititeration benötigt genau zwei Parameter: Zustand und Parameter."
        }
        require(mathematisch.einzigeAusgabe().second is ZahlAusdruck) {
            "Eine Orbititeration muss einen skalaren Zustand ausgeben."
        }
        if (start is Methode) {
            val startMethode = start.alsMathematischeMethode("einen parametrisierten Orbitstart")
            require(startMethode.parameter.size == 1 && startMethode.einzigeAusgabe().second is ZahlAusdruck) {
                "Eine Orbit-Startmethode benötigt genau den Parameter und muss eine Zahl ausgeben."
            }
        } else require(start is ZahlAusdruck) {
            "Ein Orbitstart muss eine Zahl oder eine einstellige Zahlmethode sein."
        }
    }

    override fun zuLatex(): String =
        "\\operatorname{Orb}_{${schritt.name}}\\left(${start.zuLatex()}\\right)"
}

/** Menge aller Parameter, für die die zugehörige Orbitfolge beschränkt ist. */
data class OrbitBeschraenktheitsMenge(
    val orbit: OrbitFamilie,
    val parameterRaum: MengenAusdruck,
) : MengenAusdruck {
    override fun zuLatex(): String =
        "\\left\\{p\\in ${parameterRaum.zuLatex()}\\mid " +
            "\\left( ${orbit.zuLatex()}(p) \\right)_{n\\in\\mathbb N_0}\\;\\text{beschränkt}\\right\\}"
}

sealed interface OrbitEntscheidung {
    val grund: String

    data class Enthalten(override val grund: String, val nachSchritt: Int? = null) : OrbitEntscheidung
    data class Ausgeschlossen(override val grund: String, val nachSchritt: Int? = null) : OrbitEntscheidung
    data class Unbekannt(override val grund: String) : OrbitEntscheidung
}

/**
 * Entscheidet nur mit mathematisch tragfähigen Zertifikaten. Ein endlicher Lauf
 * ohne Flucht oder Zyklus ist deshalb ausdrücklich kein Einschlussbeweis.
 */
fun entscheideOrbitBeschraenktheit(
    menge: OrbitBeschraenktheitsMenge,
    parameter: MathematischesObjekt,
    maximaleSchritte: Int = 256,
    abbruchPrüfen: () -> Unit = {},
): OrbitEntscheidung {
    require(maximaleSchritte > 0)
    when (ElementBeziehung(parameter, menge.parameterRaum).entscheide().wahrheitswert) {
        Wahrheitswert.Lüge -> return OrbitEntscheidung.Ausgeschlossen("Der Parameter liegt nicht im Parameterraum.")
        null -> return OrbitEntscheidung.Unbekannt("Die Zugehörigkeit zum Parameterraum ist nicht entscheidbar.")
        Wahrheitswert.Wahr -> Unit
    }

    val start = when (val wert = menge.orbit.start) {
        is Methode -> {
            prüfeMethodenArgumente(wert, listOf(parameter), "Der Orbitstart")?.let {
                return OrbitEntscheidung.Unbekannt(it)
            }
            val ausgabe = runCatching { wert.wendeAn(listOf(parameter)) }.getOrNull()
                ?: return OrbitEntscheidung.Unbekannt("Der Orbitstart ist nicht auswertbar.")
            prüfeMethodenAusgabe(wert, ausgabe, "Der Orbitstart")?.let {
                return OrbitEntscheidung.Unbekannt(it)
            }
            ausgabe
        }
        else -> wert
    }
    val exakterStart = start.alsGaußRational()
        ?: return OrbitEntscheidung.Unbekannt("Der Orbitstart ist nicht exakt rational auswertbar.")
    prüfeMethodenArgumente(
        menge.orbit.schritt,
        listOf(exakterStart.zuObjekt(), parameter),
        "Die erste Iteration",
    )?.let { return OrbitEntscheidung.Unbekannt(it) }

    val mandelbrotParameter = parameter.alsGaußRational()
    if (mandelbrotParameter != null && menge.orbit.istQuadratischeNullstartFamilie()) {
        return entscheideQuadratischeNullstartFamilie(mandelbrotParameter, maximaleSchritte, abbruchPrüfen)
    }
    return entscheideExaktenZyklus(menge.orbit, parameter, exakterStart, maximaleSchritte, abbruchPrüfen)
}

/** Zellzertifikat für die komplexe quadratische Nullstartfamilie. */
fun entscheideQuadratischeNullstartZelle(
    menge: OrbitBeschraenktheitsMenge,
    realteil: RationalesIntervall,
    imaginärteil: RationalesIntervall,
    maximaleSchritte: Int = 256,
    abbruchPrüfen: () -> Unit = {},
): OrbitEntscheidung {
    require(maximaleSchritte > 0)
    if (!menge.orbit.istQuadratischeNullstartFamilie() || menge.parameterRaum != KomplexeZahlen) {
        return OrbitEntscheidung.Unbekannt("Für diese Orbitfamilie ist kein komplexes Zellzertifikat registriert.")
    }
    val viertel = RationaleZahl.von(1, 4)
    val sechzehntel = RationaleZahl.von(1, 16)
    val xMinusViertel = realteil - RationalesIntervall(viertel)
    val yQuadrat = imaginärteil.quadrat()
    val q = xMinusViertel.quadrat() + yQuadrat
    val cardioid = q * (q + xMinusViertel) - RationalesIntervall(viertel) * yQuadrat
    if (cardioid.maximum <= RationaleZahl.Null) {
        return OrbitEntscheidung.Enthalten("Die gesamte Zelle liegt nach dem Hauptkardioiden-Zertifikat in der Menge.")
    }
    val periodeZwei = (realteil + RationalesIntervall(RationaleZahl.Eins)).quadrat() + yQuadrat
    if (periodeZwei.maximum <= sechzehntel) {
        return OrbitEntscheidung.Enthalten("Die gesamte Zelle liegt in der Periode-2-Komponente.")
    }

    var z = GaußIntervall(RationalesIntervall(RationaleZahl.Null), RationalesIntervall(RationaleZahl.Null))
    val c = GaußIntervall(realteil, imaginärteil)
    repeat(maximaleSchritte) { index ->
        abbruchPrüfen()
        z = z.quadrat() + c
        if (z.betragsQuadrat().minimum > RationaleZahl.von(4)) {
            return OrbitEntscheidung.Ausgeschlossen(
                "Alle Parameter der Zelle überschreiten den Fluchtradius 2.",
                index + 1,
            )
        }
        if (z.zuGroß()) return OrbitEntscheidung.Unbekannt("Das exakte Intervallbudget wurde ausgeschöpft.")
    }
    return OrbitEntscheidung.Unbekannt("Weder Einschluss noch Flucht ist für die gesamte Zelle bewiesen.")
}

private data class GaußRational(val re: RationaleZahl, val im: RationaleZahl) {
    operator fun plus(andere: GaußRational) = GaußRational(re + andere.re, im + andere.im)
    fun quadrat() = GaußRational(re * re - im * im, RationaleZahl.von(2) * re * im)
    fun betragsQuadrat() = re * re + im * im
    fun zuObjekt(): ZahlAusdruck = if (im == RationaleZahl.Null) re else KomplexeZahl(re, im)
    fun zuGroß() = listOf(re, im).any { it.zähler.bitLength() > 4096 || it.nenner.bitLength() > 4096 }
}

private data class GaußIntervall(val re: RationalesIntervall, val im: RationalesIntervall) {
    operator fun plus(andere: GaußIntervall) = GaußIntervall(re + andere.re, im + andere.im)
    fun quadrat() = GaußIntervall(re.quadrat() - im.quadrat(), RationalesIntervall(RationaleZahl.von(2)) * re * im)
    fun betragsQuadrat() = re.quadrat() + im.quadrat()
    fun zuGroß() = listOf(re.minimum, re.maximum, im.minimum, im.maximum).any {
        it.zähler.bitLength() > 4096 || it.nenner.bitLength() > 4096
    }
}

private fun RationalesIntervall.quadrat(): RationalesIntervall {
    val a = minimum * minimum
    val b = maximum * maximum
    return RationalesIntervall(
        if (enthältNull()) RationaleZahl.Null else minOf(a, b),
        maxOf(a, b),
    )
}

private fun MathematischesObjekt.alsGaußRational(): GaußRational? = when (this) {
    is RationaleZahl -> GaußRational(this, RationaleZahl.Null)
    is KomplexeZahl -> {
        val re = realteil as? RationaleZahl
        val im = imaginärteil as? RationaleZahl
        if (re == null || im == null) null else GaußRational(re, im)
    }
    else -> null
}

private fun OrbitFamilie.istQuadratischeNullstartFamilie(): Boolean {
    if (start != RationaleZahl.Null) return false
    val methode = runCatching { schritt.alsMathematischeMethode("Quadratik-Erkennung") }.getOrNull() ?: return false
    val signatur = methode.methodenSignatur()
    if (signatur.effektiverWerteVorrat != null ||
        signatur.argumente.map { it.werteVorrat } != listOf(KomplexeZahlen, KomplexeZahlen) ||
        signatur.zielMenge != KomplexeZahlen
    ) return false
    val zustand = methode.parameter.getOrNull(0) as? Variable ?: return false
    val parameter = methode.parameter.getOrNull(1) as? Variable ?: return false
    val term = methode.einzigeAusgabe().second as? Addition ?: return false
    if (term.summanden.size != 2) return false
    val quadrat = term.summanden.filterIsInstance<Potenz>().singleOrNull() ?: return false
    val parameterTerm = term.summanden.singleOrNull { it == parameter } ?: return false
    return parameterTerm == parameter && quadrat.basis == zustand && quadrat.exponent == RationaleZahl.von(2)
}

private fun entscheideQuadratischeNullstartFamilie(
    c: GaußRational,
    maximaleSchritte: Int,
    abbruchPrüfen: () -> Unit,
): OrbitEntscheidung {
    val viertel = RationaleZahl.von(1, 4)
    val q = (c.re - viertel) * (c.re - viertel) + c.im * c.im
    if (q * (q + c.re - viertel) <= viertel * c.im * c.im) {
        return OrbitEntscheidung.Enthalten("Der Parameter erfüllt exakt das Hauptkardioiden-Zertifikat.")
    }
    if ((c.re + RationaleZahl.Eins) * (c.re + RationaleZahl.Eins) + c.im * c.im <= RationaleZahl.von(1, 16)) {
        return OrbitEntscheidung.Enthalten("Der Parameter liegt exakt in der Periode-2-Komponente.")
    }
    var z = GaußRational(RationaleZahl.Null, RationaleZahl.Null)
    val besucht = linkedSetOf(z)
    repeat(maximaleSchritte) { index ->
        abbruchPrüfen()
        z = z.quadrat() + c
        if (z.betragsQuadrat() > RationaleZahl.von(4)) {
            return OrbitEntscheidung.Ausgeschlossen("Der Orbit überschreitet exakt den Fluchtradius 2.", index + 1)
        }
        if (!besucht.add(z)) {
            return OrbitEntscheidung.Enthalten("Der exakte Orbit erreicht einen periodischen Zyklus.", index + 1)
        }
        if (z.zuGroß()) return OrbitEntscheidung.Unbekannt("Das exakte Zahlenbudget wurde ausgeschöpft.")
    }
    return OrbitEntscheidung.Unbekannt("Der endliche Orbitlauf beweist weder Beschränktheit noch Flucht.")
}

private fun entscheideExaktenZyklus(
    orbit: OrbitFamilie,
    parameter: MathematischesObjekt,
    start: GaußRational,
    maximaleSchritte: Int,
    abbruchPrüfen: () -> Unit,
): OrbitEntscheidung {
    var zustand = start
    val besucht = linkedSetOf(zustand)
    repeat(maximaleSchritte) { index ->
        abbruchPrüfen()
        prüfeMethodenArgumente(
            orbit.schritt,
            listOf(zustand.zuObjekt(), parameter),
            "Die Iteration ${index + 1}",
        )?.let { return OrbitEntscheidung.Unbekannt(it) }
        val nächster = runCatching {
            orbit.schritt.wendeAn(listOf(zustand.zuObjekt(), parameter)).alsGaußRational()
        }.getOrNull() ?: return OrbitEntscheidung.Unbekannt("Die Iterationsvorschrift ist nicht exakt rational auswertbar.")
        prüfeMethodenAusgabe(orbit.schritt, nächster.zuObjekt(), "Die Iteration ${index + 1}")?.let {
            return OrbitEntscheidung.Unbekannt(it)
        }
        zustand = nächster
        if (!besucht.add(zustand)) {
            return OrbitEntscheidung.Enthalten("Der exakte Orbit erreicht einen periodischen Zyklus.", index + 1)
        }
        if (zustand.zuGroß()) return OrbitEntscheidung.Unbekannt("Das exakte Zahlenbudget wurde ausgeschöpft.")
    }
    return OrbitEntscheidung.Unbekannt("Der endliche Orbitlauf beweist keine Beschränktheit.")
}

private fun prüfeMethodenArgumente(
    methode: Methode,
    argumente: List<MathematischesObjekt>,
    rolle: String,
): String? {
    val signatur = runCatching { methode.methodenSignatur() }.getOrNull()
        ?: return "$rolle besitzt keine prüfbare Methodensignatur."
    if (signatur.argumente.size != argumente.size) return "$rolle besitzt eine unpassende Stelligkeit."
    signatur.argumente.zip(argumente).forEach { (argument, wert) ->
        when (ElementBeziehung(wert, argument.werteVorrat).entscheide().wahrheitswert) {
            Wahrheitswert.Lüge -> return "$rolle ist für '${argument.parameter.name}' außerhalb des deklarierten Wertevorrats nicht definiert."
            null -> return "$rolle kann für '${argument.parameter.name}' nicht als definiert bewiesen werden."
            Wahrheitswert.Wahr -> Unit
        }
    }
    signatur.effektiverWerteVorrat?.let { bereich ->
        val argumentObjekt = if (argumente.size == 1) argumente.single() else Tupel(argumente)
        when (ElementBeziehung(argumentObjekt, bereich).entscheide().wahrheitswert) {
            Wahrheitswert.Lüge -> return "$rolle liegt außerhalb des effektiven Methodenbereichs."
            null -> return "$rolle kann im effektiven Methodenbereich nicht bewiesen werden."
            Wahrheitswert.Wahr -> Unit
        }
    }
    return null
}

private fun prüfeMethodenAusgabe(
    methode: Methode,
    ausgabe: MathematischesObjekt,
    rolle: String,
): String? = when (ElementBeziehung(ausgabe, methode.methodenSignatur().zielMenge).entscheide().wahrheitswert) {
    Wahrheitswert.Lüge -> "$rolle liefert einen Wert außerhalb der deklarierten Zielmenge."
    null -> "$rolle kann nicht als Wert der deklarierten Zielmenge bewiesen werden."
    Wahrheitswert.Wahr -> null
}
