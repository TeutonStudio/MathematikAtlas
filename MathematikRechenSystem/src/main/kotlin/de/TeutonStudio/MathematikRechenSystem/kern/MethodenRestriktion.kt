package de.TeutonStudio.MathematikRechenSystem.kern

enum class AbdeckungsStatus {
    Vollständig,
    Unvollständig,
    Unbekannt,
}

data class MethodenErgänzungsBereich(
    val methode: Methode,
    val werteVorrat: MengenAusdruck,
    val effektiverBereich: MengenAusdruck,
    val zielPrüfung: AussageErgebnis,
)

data class MethodenRestriktionsErgebnis(
    val methode: Methode?,
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

/**
 * Gesamtdefinitionsbereich einer Methode für Bereichsoperationen.
 *
 * Bei einstelligen Methoden ist der natürliche Wertevorrat der eine Parameterbereich,
 * bei mehrstelligen Methoden das geordnete Produkt. Eine bereits explizit gesetzte
 * effektive Gesamtmenge hat immer Vorrang.
 */
fun Methode.bereichsWerteVorrat(): MengenAusdruck = effektiverWerteVorrat ?: when (parameter.size) {
    0 -> LeereMenge
    1 -> werteVorräte[parameter.single().name]
        ?: error("Für das Methodenargument '${parameter.single().name}' fehlt der Wertevorrat.")
    else -> Tupelraum(parameter.map { parameter ->
        werteVorräte[parameter.name]
            ?: error("Für das Methodenargument '${parameter.name}' fehlt der Wertevorrat.")
    })
}

/**
 * Restriktion beziehungsweise priorisierte Erweiterung einer Methode auf [menge].
 *
 * Die Basismethode besitzt höchste Priorität. Jede Ergänzung wird nur auf dem Teil
 * ihres Wertevorrats wirksam, der nach allen früheren Zweigen noch offen ist.
 * Dadurch kann eine spätere Ergänzung niemals einen früher definierten Wert überschreiben.
 */
fun restriktiereMethode(
    basis: Methode,
    menge: MengenAusdruck,
    ergänzungen: List<Methode> = emptyList(),
    kontext: RechenKontext = RechenKontext(),
): MethodenRestriktionsErgebnis {
    val basisWerteVorrat = basis.bereichsWerteVorrat()
    val zielMenge = basis.zielMenge
    val ergänzungsErgebnisse = mutableListOf<MethodenErgänzungsBereich>()
    val bedingungen = linkedSetOf<Aussage>()
    val warnungen = mutableListOf<String>()

    var abgedeckteGrundlage: MengenAusdruck = basisWerteVorrat
    ergänzungen.forEachIndexed { index, ergänzung ->
        prüfeEingabeform(basis, ergänzung, index)
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
            warnungen += "Ergänzung ${index + 1} deckt keinen noch offenen Teil des gewünschten Wertevorrats ab."
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
        val vorschrift = priorisierteVorschrift(basis, ergänzungsErgebnisse)
        basis.copy(
            name = "${basis.name}\\vert_{${menge.zuLatex()}}",
            vorschrift = vorschrift,
            effektiverWerteVorrat = menge,
        )
    } else null

    return MethodenRestriktionsErgebnis(
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

private fun prüfeEingabeform(basis: Methode, ergänzung: Methode, index: Int) {
    require(ergänzung.parameter.size == basis.parameter.size) {
        "Ergänzung ${index + 1} besitzt ${ergänzung.parameter.size} Argumente, benötigt werden ${basis.parameter.size}."
    }
    require(ergänzung.ausgabeNamen.size == basis.ausgabeNamen.size) {
        "Ergänzung ${index + 1} besitzt eine andere Anzahl öffentlicher Ausgaben als die Basismethode."
    }
    basis.parameter.zip(ergänzung.parameter).forEachIndexed { parameterIndex, (erwartet, tatsächlich) ->
        require(gleicheParameterArt(erwartet, tatsächlich)) {
            "Ergänzung ${index + 1} besitzt an Argument ${parameterIndex + 1} eine inkompatible Parameterart."
        }
    }
}

private fun gleicheParameterArt(links: MethodenParameter, rechts: MethodenParameter): Boolean = when {
    links is TypisiertesElement && rechts is TypisiertesElement -> links.anschlussArt == rechts.anschlussArt
    else -> links::class == rechts::class
}

private fun prüfeErgänzungsBild(
    methode: Methode,
    effektiverBereich: MengenAusdruck,
    zielMenge: MengenAusdruck,
    kontext: RechenKontext,
): AussageErgebnis {
    if (effektiverBereich == LeereMenge) {
        return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
    }
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
    return prüfeTeilmenge(methode.zielMenge, zielMenge, kontext)
}

private fun Methode.wendeAufGesamtArgumentAn(argument: MathematischesObjekt): MathematischesObjekt = when (parameter.size) {
    0 -> wendeAn(emptyList())
    1 -> wendeAn(listOf(argument))
    else -> {
        val tupel = argument as? Tupel
            ?: error("Eine mehrstellige Methode benötigt im Gesamtwertebereich Tupelargumente.")
        require(tupel.elemente.size == parameter.size) {
            "Das Tupelargument besitzt ${tupel.elemente.size} Komponenten, benötigt werden ${parameter.size}."
        }
        wendeAn(tupel.elemente)
    }
}

private fun priorisierteVorschrift(
    basis: Methode,
    ergänzungen: List<MethodenErgänzungsBereich>,
): MathematischesObjekt {
    if (ergänzungen.isEmpty()) return basis.vorschrift

    val argument = when (basis.parameter.size) {
        0 -> Tupel(emptyList())
        1 -> basis.parameter.single()
        else -> Tupel(basis.parameter.map { it as MathematischesObjekt })
    }
    val zweige = buildList {
        add(basis.bereichsWerteVorrat() to basis.vorschrift)
        ergänzungen.forEach { ergänzung ->
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
