package de.TeutonStudio.MathematikRechenSystem.kern

enum class AbdeckungsStatus {
    Vollständig,
    Unvollständig,
    Unbekannt,
}

/** Dauerhafte semantische Beschreibung eines tatsächlich verwendeten Ergänzungszweigs. */
data class MethodenBereichsergänzung(
    val methode: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val effektiverBereich: MengenAusdruck,
)

/**
 * Strukturierte Herkunft einer auf einen neuen Wertevorrat angepassten mathematischen Methode.
 *
 * Die kompakte Darstellung `f\vert_M` bleibt damit von der vollständigen Semantik
 * getrennt. Basis, M, Reihenfolge und effektive Ergänzungsbereiche bleiben auch
 * downstream rekonstruierbar. Engine-/Scriptmethoden sind keine mathematischen
 * Restriktionszweige und können hier nicht versehentlich gespeichert werden.
 */
data class MethodenBereichsanpassung(
    val basis: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val ergänzungen: List<MethodenBereichsergänzung>,
)

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

data class MethodenErgänzungsBereich(
    val methode: MathematischeMethode,
    val werteVorrat: MengenAusdruck,
    val effektiverBereich: MengenAusdruck,
    val zielPrüfung: AussageErgebnis,
)

data class MethodenRestriktionsErgebnis(
    val methode: MathematischeMethode?,
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
 * Gesamtdefinitionsbereich einer mathematischen Methode für Bereichsoperationen.
 */
fun Methode.bereichsWerteVorrat(): MengenAusdruck {
    val mathematisch = alsMathematischeMethode("mathematische Bereichsoperationen")
    return mathematisch.effektiverWerteVorrat ?: when (mathematisch.parameter.size) {
        0 -> LeereMenge
        1 -> mathematisch.werteVorräte[mathematisch.parameter.single().name]
            ?: error("Für das Methodenargument '${mathematisch.parameter.single().name}' fehlt der Wertevorrat.")
        else -> Tupelraum(mathematisch.parameter.map { parameter ->
            mathematisch.werteVorräte[parameter.name]
                ?: error("Für das Methodenargument '${parameter.name}' fehlt der Wertevorrat.")
        })
    }
}

/**
 * Restriktion beziehungsweise priorisierte Erweiterung einer Methode auf [menge].
 *
 * Die öffentliche Signatur bleibt aus Kompatibilitätsgründen bei [Methode], aber die
 * Funktion validiert am Einstieg, dass Basis und Ergänzungen echte symbolische
 * [MathematischeMethode]n sind. Damit kann eine spätere ScriptMethod nicht durch
 * `copy(vorschrift = ...)` in eine mathematische Fallvorschrift umgedeutet werden.
 */
fun restriktiereMethode(
    basis: Methode,
    menge: MengenAusdruck,
    ergänzungen: List<Methode> = emptyList(),
    kontext: RechenKontext = RechenKontext(),
): MethodenRestriktionsErgebnis {
    val mathematischeBasis = basis.alsMathematischeMethode("mathematische Restriktion")
    val mathematischeErgänzungen = ergänzungen.mapIndexed { index, ergänzung ->
        ergänzung.alsMathematischeMethode("mathematische Restriktion als Ergänzung ${index + 1}")
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
        val vorschrift = priorisierteVorschrift(mathematischeBasis, ergänzungsErgebnisse)
        mathematischeBasis.copy(
            name = "${mathematischeBasis.name}\\vert_{${menge.zuLatex()}}",
            vorschrift = vorschrift,
            effektiverWerteVorrat = menge,
            bereichsanpassung = MethodenBereichsanpassung(
                basis = mathematischeBasis,
                werteVorrat = menge,
                ergänzungen = ergänzungsErgebnisse.map { ergänzung ->
                    MethodenBereichsergänzung(
                        methode = ergänzung.methode,
                        werteVorrat = ergänzung.werteVorrat,
                        effektiverBereich = ergänzung.effektiverBereich,
                    )
                },
            ),
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

private fun prüfeEingabeform(basis: MathematischeMethode, ergänzung: MathematischeMethode, index: Int) {
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
            ?: error("Eine mehrstellige Methode benötigt im Gesamtwertebereich Tupelargumente.")
        require(tupel.elemente.size == parameter.size) {
            "Das Tupelargument besitzt ${tupel.elemente.size} Komponenten, benötigt werden ${parameter.size}."
        }
        wendeAn(tupel.elemente)
    }
}

private fun priorisierteVorschrift(
    basis: MathematischeMethode,
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
