package de.TeutonStudio.MathematikRechenSystem.kern

enum class AbdeckungsStatus {
    Vollständig,
    Unvollständig,
    Unbekannt,
}

/**
 * Strukturierter Methodenoperator für die mathematisch reine Restriktion `f|_M`.
 *
 * [werteVorrat] bleibt als persistenznaher Legacy-Name erhalten. Fachlich ist er der
 * eingeschränkte Definitionsraum. Bei einstelligen historischen Karten darf weiterhin
 * die Komponentenmenge W übergeben werden; die mathematische Signatur normalisiert sie
 * unmittelbar zu Tupelraum(W).
 */
data class MethodenRestriktion(
    val basis: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
) : MathematischAuswertbareMethode {
    override val name: String
        get() = "${basis.name}\\vert_{${werteVorrat.zuLatex()}}"

    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = basis.mathematischeSignatur.copy(
            effektiverDefinitionsRaum = kanonisiereBereichsRaum(werteVorrat, basis.parameter.size),
        )

    override fun zuLatex(): String = name

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
 * `f|_M`. Zielraum und Ergebnisstruktur stammen unverändert von der Basismethode.
 */
data class MethodenBereichsanpassung(
    val basis: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val ergänzungen: List<MethodenBereichsergänzung>,
) : MathematischAuswertbareMethode {
    override val name: String
        get() = "\\operatorname{Bereichsanpassung}\\!\\left(${basis.name},${werteVorrat.zuLatex()}\\right)"

    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = basis.mathematischeSignatur.copy(
            effektiverDefinitionsRaum = kanonisiereBereichsRaum(werteVorrat, basis.parameter.size),
        )

    override val bereichsanpassung: MethodenBereichsanpassung
        get() = this

    override fun zuLatex(): String = name

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

/** Kanonischer mathematischer Definitionsraum einer Methode. */
fun Methode.definitionsRaum(): MengenAusdruck =
    (this as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur?.definitionsRaum
        ?: error("Die Methode '$name' besitzt keine mathematische Raum-/Mengensignatur.")

/**
 * Persistenznaher Bereich für die vorhandenen Bereichsoperatoren. Bei einstelligen
 * Methoden wird der Einertupelraum bewusst an der Adaptergrenze auf seine Komponente
 * projiziert, damit historische Karten und endliche Mengenoperationen stabil bleiben.
 */
@Deprecated("Verwende definitionsRaum(); diese Projektion existiert nur für Legacy-Bereichsoperatoren.")
fun Methode.bereichsWerteVorrat(): MengenAusdruck {
    val mathematisch = alsMathematischeMethode("mathematische Bereichsoperationen")
    val effektiver = mathematisch.effektiverWerteVorrat
    if (effektiver != null) return effektiver
    return when (mathematisch.parameter.size) {
        0 -> Tupelraum(emptyList())
        1 -> mathematisch.werteVorräte[mathematisch.parameter.single().name]
            ?: error("Für das Methodenargument '${mathematisch.parameter.single().name}' fehlt die Definitionsmenge.")
        else -> Tupelraum(mathematisch.parameter.map { parameter ->
            mathematisch.werteVorräte[parameter.name]
                ?: error("Für das Methodenargument '${parameter.name}' fehlt die Definitionsmenge.")
        })
    }
}

private fun kanonisiereBereichsRaum(menge: MengenAusdruck, stelligkeit: Int): MengenAusdruck = when {
    stelligkeit == 0 && menge == LeereMenge -> Tupelraum(emptyList())
    stelligkeit == 1 && menge !is Tupelraum -> Tupelraum(listOf(menge))
    else -> menge
}

/**
 * Mathematisch reine Restriktion von [basis] auf [menge].
 *
 * Genau der Vertrag `M ⊆ D_f` ist zulässig. Für historische einstellige Karten wird
 * die Komponentenmenge an dieser ergonomischen Grenze geprüft und anschließend in der
 * Signatur zum Einertupelraum kanonisiert. Eine Restriktion verändert niemals den Zielraum.
 */
fun restriktiereMethode(
    basis: Methode,
    menge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
): MethodenRestriktionsErgebnis {
    val mathematischeBasis = basis.alsMathematischeMethode("mathematische Restriktion")
    val basisWerteVorrat = mathematischeBasis.bereichsWerteVorrat()
    val prüfMenge = if (mathematischeBasis.parameter.size == 0 && menge == LeereMenge) Tupelraum(emptyList()) else menge
    val teilmengenPrüfung = prüfeTeilmenge(prüfMenge, basisWerteVorrat, kontext)
    val bedingungen = linkedSetOf<Aussage>()
    if (teilmengenPrüfung.wahrheitswert == null) {
        bedingungen += TeilmengenBeziehung(prüfMenge, basisWerteVorrat)
    }
    val methode = if (teilmengenPrüfung.wahrheitswert == Wahrheitswert.Lüge) null
    else MethodenRestriktion(mathematischeBasis, menge)

    return MethodenRestriktionsErgebnis(
        methode = methode,
        basisWerteVorrat = basisWerteVorrat,
        gewünschterWerteVorrat = menge,
        zielMenge = mathematischeBasis.zielMenge,
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
    val basisWerteVorrat = mathematischeBasis.bereichsWerteVorrat()
    val zielMenge = mathematischeBasis.zielMenge
    val ergänzungsErgebnisse = mutableListOf<MethodenErgänzungsBereich>()
    val bedingungen = linkedSetOf<Aussage>()
    val warnungen = mutableListOf<String>()

    var abgedeckteGrundlage: MengenAusdruck = basisWerteVorrat
    mathematischeErgänzungen.forEachIndexed { index, ergänzung ->
        prüfeEingabeform(mathematischeBasis, ergänzung, index)
        val restVorher = mengenDifferenz(menge, abgedeckteGrundlage)
        val ergänzungsWerteVorrat = ergänzung.bereichsWerteVorrat()
        val effektiverBereich = schneide(listOf(restVorher, ergänzungsWerteVorrat))
        val zielPrüfung = prüfeErgänzungsBild(
            methode = ergänzung,
            effektiverBereich = effektiverBereich,
            zielMenge = zielMenge,
            kontext = kontext,
        )
        if (zielPrüfung.wahrheitswert == null) {
            bedingungen += TeilmengenBeziehung(Abbild(effektiverBereich, ergänzung), zielMenge)
        }
        if (effektiverBereich == LeereMenge) {
            warnungen += "Ergänzung ${index + 1} deckt keinen noch offenen Teil des gewünschten Definitionsbereichs ab."
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
        zielMenge = zielMenge,
        abgedeckterBereich = abgedeckterBereich,
        restMenge = restMenge,
        abdeckungsPrüfung = abdeckungsPrüfung,
        ergänzungen = ergänzungsErgebnisse,
        bedingungen = bedingungen,
        warnungen = warnungen,
    )
}

private fun prüfeEingabeform(basis: MathematischeMethode, ergänzung: MathematischeMethode, index: Int) {
    val typPrüfung = prüfeMethodenTypKomposition(
        außen = object : SignaturtragendeMethode {
            override val name = "Basis-Eingabe"
            override val signatur = MethodenSignatur(basis.signatur.argumente, emptyList())
        },
        innen = object : SignaturtragendeMethode {
            override val name = "Ergänzungs-Eingabe"
            override val signatur = MethodenSignatur(emptyList(), ergänzung.signatur.argumente)
        },
    ).typPrüfung
    require(typPrüfung !is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel) {
        "Ergänzung ${index + 1} besitzt eine inkompatible neutrale Eingabesignatur: ${typPrüfung.grund}"
    }
    require(ergänzung.ausgabeNamen.size == basis.ausgabeNamen.size) {
        "Ergänzung ${index + 1} besitzt eine andere Anzahl öffentlicher Ausgaben als die Basismethode."
    }
}

private fun prüfeErgänzungsBild(
    methode: MathematischeMethode,
    effektiverBereich: MengenAusdruck,
    zielMenge: MengenAusdruck,
    kontext: RechenKontext,
): AussageErgebnis {
    if (effektiverBereich == LeereMenge) {
        return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
    }

    val deklarierteZielPrüfung = prüfeTeilmenge(methode.zielMenge, zielMenge, kontext)
    if (deklarierteZielPrüfung.wahrheitswert == Wahrheitswert.Wahr) return deklarierteZielPrüfung

    if (effektiverBereich is EndlicheMenge) {
        val ergebnisse = effektiverBereich.elemente.map { argument ->
            val wert = methode.wendeAufGesamtArgumentAn(argument)
            ElementBeziehung(wert, zielMenge).entscheide(kontext)
        }
        return when {
            ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } ->
                AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } ->
                AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    return prüfeTeilmenge(Abbild(effektiverBereich, methode), zielMenge, kontext)
}

private fun MathematischeMethode.wendeAufGesamtArgumentAn(argument: MathematischesObjekt): MathematischesObjekt = when (parameter.size) {
    0 -> wendeAn(emptyList())
    1 -> wendeAn(listOf(argument))
    else -> {
        val tupel = argument as? Tupel
            ?: error("Eine mehrstellige Methode benötigt im Gesamtdefinitionsbereich Tupelargumente.")
        require(tupel.elemente.size == parameter.size) {
            "Das Tupelargument besitzt ${tupel.elemente.size} Komponenten, benötigt werden ${parameter.size}."
        }
        wendeAn(tupel.elemente)
    }
}

private fun priorisierteVorschrift(anpassung: MethodenBereichsanpassung): MathematischesObjekt {
    if (anpassung.ergänzungen.isEmpty()) return anpassung.basis.vorschrift

    val basis = anpassung.basis
    val argument = when (basis.parameter.size) {
        0 -> Tupel(emptyList())
        1 -> basis.parameter.single()
        else -> Tupel(basis.parameter.map { it as MathematischesObjekt })
    }
    val zweige = buildList {
        add(
            schneide(listOf(anpassung.werteVorrat, basis.bereichsWerteVorrat())) to
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
