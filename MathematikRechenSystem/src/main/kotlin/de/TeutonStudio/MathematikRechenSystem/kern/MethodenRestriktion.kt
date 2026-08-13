package de.TeutonStudio.MathematikRechenSystem.kern

enum class AbdeckungsStatus {
    Vollständig,
    Unvollständig,
    Unbekannt,
}

/**
 * Strukturierter Methodenoperator für die mathematisch reine Restriktion `f|_M`.
 *
 * Die Voraussetzung `M ⊆ D_f` wird bereits beim Erzeugen geprüft. Der Operator
 * besitzt absichtlich weder Ergänzungsmethoden noch Ersatzwerte. Insbesondere bleibt
 * der Zielraum unverändert.
 */
data class MethodenRestriktion(
    val basis: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
) : MathematischAuswertbareMethode {
    override val name: String
        get() = "${basis.name}\\vert_{${werteVorrat.zuLatex()}}"

    /** Restriktion ändert keine Typkomponente. */
    override val signatur: MethodenSignatur
        get() = basis.signatur

    /** Ausschließlich der effektive mathematische Definitionsraum wird eingeschränkt. */
    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = basis.mathematischeSignatur.copy(effektiverDefinitionsRaum = werteVorrat)

    override fun zuLatex(): String = name

    override fun wendeKanonischMathematischAn(argumente: Tupel): Tupel =
        basis.wendeKanonischMathematischAn(argumente)

    override fun wendeMathematischAn(
        argumente: Map<String, MathematischesObjekt>,
    ): MathematischesObjekt = basis.wendeMathematischAn(argumente)

    internal fun materialisiere(): MathematischeMethode = basis.copy(
        name = name,
        effektiverWerteVorrat = werteVorrat,
        bereichsanpassung = null,
    )
}

/** Dauerhafte semantische Beschreibung eines tatsächlich verwendeten Ergänzungszweigs. */
data class MethodenBereichsergänzung(
    val methode: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val effektiverBereich: MengenAusdruck,
)

/**
 * Eigener höherer Methodenoperator für eine gewünschte Definitionsmenge mit
 * geordneten Ergänzungsmethoden. Frühere Methoden besitzen immer Priorität.
 *
 * Dieser Typ ist ausdrücklich keine Restriktion und rendert deshalb niemals als
 * `f|_M`. Die Fallvorschrift wird erst an der mathematischen Materialisierungsgrenze
 * erzeugt und bleibt aus Basis, Zielbereich und geordneten Zweigen rekonstruierbar.
 */
data class MethodenBereichsanpassung(
    val basis: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val ergänzungen: List<MethodenBereichsergänzung>,
) : MathematischAuswertbareMethode {
    override val name: String
        get() = "\\operatorname{Bereichsanpassung}\\!\\left(${basis.name},${werteVorrat.zuLatex()}\\right)"

    /** Bereichsanpassung ändert keine Typkomponente. */
    override val signatur: MethodenSignatur
        get() = basis.signatur

    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = basis.mathematischeSignatur.copy(effektiverDefinitionsRaum = werteVorrat)

    override val bereichsanpassung: MethodenBereichsanpassung
        get() = this

    override fun zuLatex(): String = name

    override fun wendeKanonischMathematischAn(argumente: Tupel): Tupel =
        materialisiere().wendeKanonischMathematischAn(argumente)

    override fun wendeMathematischAn(
        argumente: Map<String, MathematischesObjekt>,
    ): MathematischesObjekt = materialisiere().wendeMathematischAn(argumente)

    internal fun materialisiere(): MathematischeMethode = basis.copy(
        name = name,
        vorschrift = priorisierteVorschrift(this),
        effektiverWerteVorrat = werteVorrat,
        bereichsanpassung = this,
    )
}

internal fun MethodenBereichsanpassung.ersetze(
    bindungen: Map<String, MathematischesObjekt>,
): MethodenBereichsanpassung = copy(
    basis = ersetze(basis, bindungen) as MathematischeMethode,
    werteVorrat = ersetze(werteVorrat, bindungen) as MengenAusdruck,
    ergänzungen = ergänzungen.map { ergänzung ->
        ergänzung.copy(
            methode = ersetze(ergänzung.methode, bindungen) as MathematischeMethode,
            werteVorrat = ersetze(ergänzung.werteVorrat, bindungen) as MengenAusdruck,
            effektiverBereich = ersetze(ergänzung.effektiverBereich, bindungen) as MengenAusdruck,
        )
    },
)

data class MethodenRestriktionsErgebnis(
    val methode: MethodenRestriktion?,
    val basisWerteVorrat: MengenAusdruck,
    val gewünschterWerteVorrat: MengenAusdruck,
    /** Kanonischer Zielraum; der historische Feldname bleibt für Aufrufer stabil. */
    val zielMenge: MengenAusdruck,
    val teilmengenPrüfung: AussageErgebnis,
    val bedingungen: Set<Aussage>,
) {
    val istGültig: Boolean
        get() = teilmengenPrüfung.wahrheitswert != Wahrheitswert.Lüge
}

data class MethodenErgänzungsBereich(
    val methode: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val effektiverBereich: MengenAusdruck,
    val zielPrüfung: AussageErgebnis,
)

data class MethodenBereichsanpassungsErgebnis(
    val methode: MethodenBereichsanpassung?,
    val basisWerteVorrat: MengenAusdruck,
    val gewünschterWerteVorrat: MengenAusdruck,
    /** Kanonischer Zielraum; der historische Feldname bleibt für Aufrufer stabil. */
    val zielMenge: MengenAusdruck,
    val abgedeckterBereich: MengenAusdruck,
    val restMenge: MengenAusdruck,
    val abdeckungsPrüfung: AussageErgebnis,
    val ergänzungen: List<MethodenErgänzungsBereich>,
    val bedingungen: Set<Aussage>,
    val warnungen: List<String>,
) {
    val abdeckungsStatus: AbdeckungsStatus
        get() = when (abdeckungsPrüfung.wahrheitswert) {
            Wahrheitswert.Wahr -> AbdeckungsStatus.Vollständig
            Wahrheitswert.Lüge -> AbdeckungsStatus.Unvollständig
            null -> AbdeckungsStatus.Unbekannt
        }

    val hatZielmengenVerletzung: Boolean
        get() = ergänzungen.any { it.zielPrüfung.wahrheitswert == Wahrheitswert.Lüge }
}

/**
 * Bereichsoperatoren verwenden die mathematische Gesamtmenge der Argumentwerte,
 * nicht den neutralen Tupel-Typvertrag. Bei genau einem Argument bleibt daher die
 * übliche skalare Definitionsmenge erhalten; erst mehrstellige Methoden verwenden
 * einen Tupelraum. Ein bereits gesetzter effektiver Bereich hat immer Vorrang.
 */
private fun MathematischeMethode.bereichsDefinitionsmenge(): MengenAusdruck =
    mathematischeSignatur.effektiverDefinitionsRaum ?: when (mathematischeSignatur.argumente.size) {
        0 -> Tupelraum(emptyList())
        1 -> mathematischeSignatur.argumente.single().definitionsMenge
        else -> mathematischeSignatur.definitionsRaum
    }

/** Gesamtdefinitionsraum einer mathematischen Methode für Bereichsoperationen. */
fun Methode.bereichsWerteVorrat(): MengenAusdruck =
    alsMathematischeMethode("mathematische Bereichsoperationen").bereichsDefinitionsmenge()

/**
 * Mathematisch reine Restriktion von [basis] auf [menge].
 *
 * Genau der Vertrag `M ⊆ D_f` ist zulässig. Eine falsche Teilmengenbeziehung erzeugt
 * keine Methode; eine unentscheidbare Beziehung bleibt als sichtbare Voraussetzung
 * erhalten. Diese Funktion besitzt absichtlich keinen Ergänzungsparameter.
 */
fun restriktiereMethode(
    basis: Methode,
    menge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
): MethodenRestriktionsErgebnis {
    val mathematischeBasis = basis.alsMathematischeMethode("mathematische Restriktion")
    val basisWerteVorrat = mathematischeBasis.bereichsDefinitionsmenge()
    val teilmengenPrüfung = prüfeTeilmenge(menge, basisWerteVorrat, kontext)
    val bedingungen = linkedSetOf<Aussage>()
    if (teilmengenPrüfung.wahrheitswert == null) {
        bedingungen += TeilmengenBeziehung(menge, basisWerteVorrat)
    }
    val methode = if (teilmengenPrüfung.wahrheitswert == Wahrheitswert.Lüge) null
    else MethodenRestriktion(mathematischeBasis, menge)

    return MethodenRestriktionsErgebnis(
        methode = methode,
        basisWerteVorrat = basisWerteVorrat,
        gewünschterWerteVorrat = menge,
        zielMenge = mathematischeBasis.mathematischeSignatur.zielRaum,
        teilmengenPrüfung = teilmengenPrüfung,
        bedingungen = bedingungen,
    )
}

/**
 * Passt den Definitionsbereich einer Methode an eine gewünschte Menge an.
 *
 * Die Basismethode besitzt höchste Priorität. Danach werden [ergänzungen] in ihrer
 * Listenreihenfolge geprüft; jeder Zweig erhält ausschließlich den bis dahin offenen
 * Teil der gewünschten Menge. Damit gilt deterministisch: erste passende Methode gewinnt.
 */
fun passeMethodenBereichAn(
    basis: Methode,
    menge: MengenAusdruck,
    ergänzungen: List<Methode> = emptyList(),
    kontext: RechenKontext = RechenKontext(),
): MethodenBereichsanpassungsErgebnis {
    val mathematischeBasis = basis.alsMathematischeMethode("mathematische Bereichsanpassung")
    val mathematischeErgänzungen = ergänzungen.mapIndexed { index, ergänzung ->
        ergänzung.alsMathematischeMethode("mathematische Bereichsanpassung als Ergänzung ${index + 1}")
    }
    val basisWerteVorrat = mathematischeBasis.bereichsDefinitionsmenge()
    val zielRaum = mathematischeBasis.mathematischeSignatur.zielRaum
    val ergänzungsErgebnisse = mutableListOf<MethodenErgänzungsBereich>()
    val bedingungen = linkedSetOf<Aussage>()
    val warnungen = mutableListOf<String>()

    var abgedeckteGrundlage: MengenAusdruck = basisWerteVorrat
    mathematischeErgänzungen.forEachIndexed { index, ergänzung ->
        prüfeEingabeform(mathematischeBasis, ergänzung, index)
        val restVorher = mengenDifferenz(menge, abgedeckteGrundlage)
        val ergänzungsWerteVorrat = ergänzung.bereichsDefinitionsmenge()
        val effektiverBereich = schneide(listOf(restVorher, ergänzungsWerteVorrat))
        val zielPrüfung = prüfeErgänzungsBild(
            methode = ergänzung,
            effektiverBereich = effektiverBereich,
            zielRaum = zielRaum,
            kontext = kontext,
        )
        if (zielPrüfung.wahrheitswert == null) {
            bedingungen += TeilmengenBeziehung(Abbild(effektiverBereich, ergänzung), zielRaum)
        }
        if (effektiverBereich == LeereMenge) {
            warnungen += "Ergänzung ${index + 1} deckt keinen noch offenen Teil des gewünschten Definitionsraums ab."
        }
        ergänzungsErgebnisse += MethodenErgänzungsBereich(
            methode = ergänzung,
            werteVorrat = ergänzungsWerteVorrat,
            effektiverBereich = effektiverBereich,
            zielPrüfung = zielPrüfung,
        )
        abgedeckteGrundlage = vereinige(listOf(abgedeckteGrundlage, ergänzungsWerteVorrat))
    }

    val restMenge = mengenDifferenz(menge, abgedeckteGrundlage)
    val abdeckungsPrüfung = prüfeTeilmenge(menge, abgedeckteGrundlage, kontext)
    if (abdeckungsPrüfung.wahrheitswert == null) {
        bedingungen += TeilmengenBeziehung(menge, abgedeckteGrundlage)
    }
    val abgedeckterBereich = schneide(listOf(menge, abgedeckteGrundlage))
    val zielVerletzt = ergänzungsErgebnisse.any { it.zielPrüfung.wahrheitswert == Wahrheitswert.Lüge }
    val kannMethodeBilden = abdeckungsPrüfung.wahrheitswert != Wahrheitswert.Lüge && !zielVerletzt

    val resultierendeMethode = if (kannMethodeBilden) {
        MethodenBereichsanpassung(
            basis = mathematischeBasis,
            werteVorrat = menge,
            ergänzungen = ergänzungsErgebnisse.map { ergänzung ->
                MethodenBereichsergänzung(
                    methode = ergänzung.methode,
                    werteVorrat = ergänzung.werteVorrat,
                    effektiverBereich = ergänzung.effektiverBereich,
                )
            },
        )
    } else null

    return MethodenBereichsanpassungsErgebnis(
        methode = resultierendeMethode,
        basisWerteVorrat = basisWerteVorrat,
        gewünschterWerteVorrat = menge,
        zielMenge = zielRaum,
        abgedeckterBereich = abgedeckterBereich,
        restMenge = restMenge,
        abdeckungsPrüfung = abdeckungsPrüfung,
        ergänzungen = ergänzungsErgebnisse,
        bedingungen = bedingungen,
        warnungen = warnungen,
    )
}

private fun prüfeEingabeform(basis: MathematischeMethode, ergänzung: MathematischeMethode, index: Int) {
    val basisSignatur = basis.signatur
    val ergänzungsSignatur = ergänzung.signatur
    require(ergänzungsSignatur.argumente.size == basisSignatur.argumente.size) {
        "Ergänzung ${index + 1} besitzt ${ergänzungsSignatur.argumente.size} Argumente, benötigt werden ${basisSignatur.argumente.size}."
    }
    require(ergänzungsSignatur.ergebnisse.size == basisSignatur.ergebnisse.size) {
        "Ergänzung ${index + 1} besitzt eine andere Anzahl öffentlicher Ausgaben als die Basismethode."
    }
    basisSignatur.argumente.zip(ergänzungsSignatur.argumente).forEachIndexed { parameterIndex, (erwartet, tatsächlich) ->
        val prüfung = MathematischeTypen.typSystem.prüfe(tatsächlich.typ, erwartet.typ)
        require(prüfung is de.TeutonStudio.TypSystem.TypPrüfung.Kompatibel) {
            "Ergänzung ${index + 1} besitzt an Argument ${parameterIndex + 1} einen inkompatiblen Typ."
        }
    }
}

private fun prüfeErgänzungsBild(
    methode: MathematischeMethode,
    effektiverBereich: MengenAusdruck,
    zielRaum: Tupelraum,
    kontext: RechenKontext,
): AussageErgebnis {
    if (effektiverBereich == LeereMenge) {
        return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
    }

    val deklarierteZielPrüfung = prüfeTeilmenge(methode.mathematischeSignatur.zielRaum, zielRaum, kontext)
    if (deklarierteZielPrüfung.wahrheitswert == Wahrheitswert.Wahr) return deklarierteZielPrüfung

    if (effektiverBereich is EndlicheMenge) {
        val ergebnisse = effektiverBereich.elemente.map { argument ->
            val wert = methode.wendeAufGesamtArgumentKanonischAn(argument)
            ElementBeziehung(wert, zielRaum).entscheide(kontext)
        }
        return when {
            ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } ->
                AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } ->
                AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    return prüfeTeilmenge(Abbild(effektiverBereich, methode), zielRaum, kontext)
}

private fun MathematischeMethode.wendeAufGesamtArgumentKanonischAn(argument: MathematischesObjekt): Tupel =
    when (parameter.size) {
        0 -> wendeKanonischMathematischAn(Tupel(emptyList()))
        1 -> wendeKanonischMathematischAn(Tupel(listOf(argument)))
        else -> {
            val tupel = argument as? Tupel
                ?: error("Eine mehrstellige Methode benötigt im Gesamtdefinitionsraum Tupelargumente.")
            require(tupel.elemente.size == parameter.size) {
                "Das Tupelargument besitzt ${tupel.elemente.size} Komponenten, benötigt werden ${parameter.size}."
            }
            wendeKanonischMathematischAn(tupel)
        }
    }

private fun priorisierteVorschrift(anpassung: MethodenBereichsanpassung): MathematischesObjekt {
    if (anpassung.ergänzungen.isEmpty()) return anpassung.basis.vorschrift

    val basis = anpassung.basis
    val argument: MathematischesObjekt = when (basis.parameter.size) {
        0 -> Tupel(emptyList())
        1 -> basis.parameter.single() as MathematischesObjekt
        else -> Tupel(basis.parameter.map { it as MathematischesObjekt })
    }
    val zweige = buildList {
        add(
            schneide(listOf(anpassung.werteVorrat, basis.bereichsDefinitionsmenge())) to
                basis.vorschrift,
        )
        anpassung.ergänzungen.forEach { ergänzung ->
            val bindungen = ergänzung.methode.parameter.zip(basis.parameter)
                .associate { (quelle, ziel) -> quelle.name to ziel as MathematischesObjekt }
            add(ergänzung.effektiverBereich to ersetze(ergänzung.methode.vorschrift, bindungen))
        }
    }

    var ergebnis = zweige.last().second
    for (index in zweige.lastIndex - 1 downTo 0) {
        val (bereich, vorschrift) = zweige[index]
        ergebnis = FallAusdruck(
            wahr = vorschrift,
            aussage = ElementBeziehung(argument, bereich),
            lüge = ergebnis,
        )
    }
    return ergebnis
}
