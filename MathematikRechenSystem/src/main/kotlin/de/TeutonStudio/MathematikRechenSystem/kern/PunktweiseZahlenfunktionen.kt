package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonische gemeinsame Signatur und Zahlterme für eine punktweise Operation.
 * Skalare bleiben unverändert; Methoden werden auf die Parameter der ersten
 * Methode alpha-umbenannt, ohne sichtbare LaTeX-Texte als Fachdaten auszuwerten.
 */
data class PunktweiseZahlenVorbereitung(
    val parameter: List<Variable>,
    val werteVorräte: Map<String, MengenAusdruck>,
    val effektiverWerteVorrat: MengenAusdruck,
    val operanden: Map<String, ZahlAusdruck>,
    val methodenNamen: Map<String, String>,
)

fun bereitePunktweiseZahlenfunktionVor(
    operanden: Map<String, MathematischesObjekt>,
): PunktweiseZahlenVorbereitung {
    val methoden = operanden.values.filterIsInstance<Methode>()
    require(methoden.isNotEmpty()) { "Eine punktweise Hebung benötigt mindestens eine Zahlenfunktion." }
    methoden.forEach { methode ->
        MethodenAnforderung.Zahlenfunktion.prüfe(methode)?.let { diagnose -> error(diagnose) }
        require(methode.vorschrift is ZahlAusdruck) {
            "Die Methode '${methode.name}' besitzt trotz numerischer Zielmenge keine Zahlvorschrift."
        }
    }

    val basis = methoden.first()
    val stelligkeit = basis.methodenSignatur().argumente.size
    val abweichend = methoden.firstOrNull { it.methodenSignatur().argumente.size != stelligkeit }
    require(abweichend == null) {
        "Die Zahlenfunktionen müssen dieselbe Stelligkeit besitzen; '${basis.name}' hat " +
            "$stelligkeit Argumente, '${abweichend?.name}' dagegen ${abweichend?.methodenSignatur()?.argumente?.size}."
    }
    val basisMathematisch = basis.alsMathematischeMethode("punktweise Zahlenhebung")
    val parameter = basisMathematisch.parameter.map { alt -> Variable(alt.name, alt.zuLatex()) }

    fun bindungen(methode: Methode): Map<String, MathematischesObjekt> =
        methode.alsMathematischeMethode("punktweise Zahlenhebung").parameter
            .mapIndexed { index, alt -> alt.name to parameter[index] }.toMap()

    val komponenten = parameter.indices.map { index ->
        normalisiereZahlmengenSchnitt(
            methoden.map { methode ->
                val argument = methode.mathematischeMethodenSignatur().argumente[index]
                benenneMethodenBereichUm(argument.definitionsMenge, bindungen(methode))
            },
        )
    }
    val werteVorräte = parameter.mapIndexed { index, variable ->
        variable.name to komponenten[index]
    }.toMap(LinkedHashMap())

    val expliziteBereiche = methoden.mapNotNull { methode ->
        methode.alsMathematischeMethode("punktweise Zahlenhebung").effektiverWerteVorrat?.let { bereich ->
            benenneMethodenBereichUm(bereich, bindungen(methode))
        }
    }
    val kartesischerBereich = Tupelraum(komponenten)
    val effektiverBereich = if (expliziteBereiche.isEmpty()) {
        kartesischerBereich
    } else {
        schneide(listOf(kartesischerBereich) + expliziteBereiche)
    }

    val zahlOperanden = operanden.mapValues { (_, objekt) ->
        when (objekt) {
            is ZahlAusdruck -> objekt
            is Methode -> ersetze(objekt.vorschrift as ZahlAusdruck, bindungen(objekt))
            else -> error("Punktweise Zahlenoperatoren akzeptieren nur Zahlen oder Zahlenfunktionen.")
        }
    }
    return PunktweiseZahlenVorbereitung(
        parameter = parameter,
        werteVorräte = werteVorräte,
        effektiverWerteVorrat = effektiverBereich,
        operanden = zahlOperanden,
        methodenNamen = operanden.mapNotNull { (name, objekt) ->
            (objekt as? Methode)?.let { name to it.name }
        }.toMap(),
    )
}

fun PunktweiseZahlenVorbereitung.erzeugeMethode(
    name: String,
    vorschrift: ZahlAusdruck,
    zielMenge: MengenAusdruck,
    definitionsBedingungen: List<Aussage> = emptyList(),
): Methode {
    val bereich = if (definitionsBedingungen.isEmpty()) {
        effektiverWerteVorrat
    } else {
        require(parameter.isNotEmpty()) {
            "Wertabhängige Definitionsbedingungen nullstelliger Methoden sind nicht darstellbar."
        }
        val bedingung = if (definitionsBedingungen.size == 1) definitionsBedingungen.single()
        else Konjunktion(definitionsBedingungen)
        val bedingterBereich = DefinierteMenge(
            variablen = parameter.map { variable ->
                GebundeneMengenVariable(variable, werteVorräte.getValue(variable.name))
            },
            bedingung = bedingung,
        )
        schneide(listOf(effektiverWerteVorrat, bedingterBereich))
    }
    return Methode(
        name = name,
        parameter = parameter,
        vorschrift = vorschrift,
        zielMenge = zielMenge,
        werteVorräte = werteVorräte,
        effektiverWerteVorrat = bereich,
    )
}

private fun benenneMethodenBereichUm(
    menge: MengenAusdruck,
    bindungen: Map<String, MathematischesObjekt>,
): MengenAusdruck = when (menge) {
    is Vereinigung -> vereinige(menge.mengen.map { benenneMethodenBereichUm(it, bindungen) })
    is Schnitt -> schneide(
        menge.mengen.map { benenneMethodenBereichUm(it, bindungen) },
        menge.grundMenge?.let { benenneMethodenBereichUm(it, bindungen) },
    )
    is MengenDifferenz -> mengenDifferenz(
        benenneMethodenBereichUm(menge.links, bindungen),
        benenneMethodenBereichUm(menge.rechts, bindungen),
    )
    is KartesischesProdukt -> kartesischesProdukt(
        menge.mengen.map { benenneMethodenBereichUm(it, bindungen) },
    )
    is Tupelraum -> Tupelraum(menge.komponenten.map { benenneMethodenBereichUm(it, bindungen) })
    is DefinierteMenge -> {
        val lokaleBindungen = menge.variablen.mapNotNull { gebunden ->
            (bindungen[gebunden.variable.name] as? Variable)?.let { gebunden.variable.name to it }
        }.toMap()
        menge.copy(
            variablen = menge.variablen.map { gebunden ->
                gebunden.copy(
                    variable = lokaleBindungen[gebunden.variable.name] ?: gebunden.variable,
                    grundMenge = benenneMethodenBereichUm(gebunden.grundMenge, bindungen),
                )
            },
            bedingung = ersetze(menge.bedingung, bindungen + lokaleBindungen),
        )
    }
    else -> ersetze(menge, bindungen) as MengenAusdruck
}
