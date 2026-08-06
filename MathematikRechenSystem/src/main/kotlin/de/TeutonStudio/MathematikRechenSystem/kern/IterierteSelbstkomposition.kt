package de.TeutonStudio.MathematikRechenSystem.kern

enum class KompositionsEingangsModus {
    GETRENNTE_ARGUMENTE,
    GEPACKTES_TUPEL,
}

enum class KompositionsAusgangsModus {
    GEPACKT,
    ENTPACKT,
}

enum class SelbstkompositionsStatus {
    TOTAL_GUELTIG,
    BEDINGT_GUELTIG,
    MATHEMATISCH_UNMOEGLICH,
}

data class SelbstkompositionsPruefung(
    val status: SelbstkompositionsStatus,
    val argumentAnzahl: Int,
    val ergebnisKomponenten: Int?,
    val voraussetzungen: Set<String> = emptySet(),
    val grund: String = "",
) {
    val istZulaessig: Boolean
        get() = status != SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH
}

data class RekursiverKompositionsWertevorrat(
    val methodeName: String,
    val ordnung: IterationsOrdnung,
    val grundbereich: MengenAusdruck,
) : MengenAusdruck {
    override fun zuLatex(): String = when (ordnung) {
        is IterationsOrdnung.Konkret -> "W_{${ordnung.wert}}"
        is IterationsOrdnung.Symbolisch -> "W_{${ordnung.zuLatex()}}"
    }

    fun definitionsLatex(): String =
        "W_{n+1}=W_n\\cap\\left(${methodeName}^{\\langle n\\rangle}\\right)^{-1}" +
            "\\left(${grundbereich.zuLatex()}\\right)"
}

data class IterierteSelbstkomposition(
    val methode: Methode,
    val ordnung: IterationsOrdnung,
    val eingangsModus: KompositionsEingangsModus = KompositionsEingangsModus.GETRENNTE_ARGUMENTE,
    val ausgangsModus: KompositionsAusgangsModus = KompositionsAusgangsModus.GEPACKT,
    val pruefung: SelbstkompositionsPruefung = pruefeSelbstkomposition(methode),
) : MathematischesObjekt {
    val operatorId: String = IterationsArt.SELBSTKOMPOSITION.operatorId
    val werteVorrat: MengenAusdruck = maximalerKompositionsWertevorrat(methode, ordnung)
    val zielMenge: MengenAusdruck = when (ordnung) {
        is IterationsOrdnung.Konkret -> if (ordnung.wert.signum() == 0) aeussererMethodenWertevorrat(methode) else methode.zielMenge
        is IterationsOrdnung.Symbolisch -> methode.zielMenge
    }

    init {
        require(pruefung.istZulaessig) { pruefung.grund }
    }

    val aeussereParameter: List<MethodenParameter>
        get() = methode.parameter

    override fun zuLatex(): String = IterierterAusdruck(
        basis = methode,
        art = IterationsArt.SELBSTKOMPOSITION,
        ordnung = ordnung,
    ).zuLatex()

    fun nullteIdentitaetOderNull(): EingeschraenkteIdentitaet? = when (ordnung) {
        is IterationsOrdnung.Konkret -> if (ordnung.wert.signum() == 0) {
            EingeschraenkteIdentitaet(aeussererMethodenWertevorrat(methode))
        } else {
            null
        }
        is IterationsOrdnung.Symbolisch -> null
    }
}

fun pruefeSelbstkomposition(methode: Methode): SelbstkompositionsPruefung {
    val argumente = runCatching { methode.methodenSignatur().argumente }
        .getOrElse {
            return SelbstkompositionsPruefung(
                status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
                argumentAnzahl = methode.parameter.size,
                ergebnisKomponenten = null,
                grund = it.message ?: "Die Methodensignatur ist unvollständig.",
            )
        }
    if (argumente.isEmpty()) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = 0,
            ergebnisKomponenten = null,
            grund = "Nullstellige Methoden besitzen keinen erneuten Aufrufvertrag.",
        )
    }

    val komponenten = entpackeEineEbeneOderNull(methode.vorschrift)
    if (argumente.size > 1 && komponenten == null) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = argumente.size,
            ergebnisKomponenten = null,
            grund = "Eine ${argumente.size}-stellige Methode muss ein geordnetes Ergebnis mit ${argumente.size} Komponenten liefern.",
        )
    }
    if (komponenten != null && komponenten.size != argumente.size) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = argumente.size,
            ergebnisKomponenten = komponenten.size,
            grund = "Das Ergebnis besitzt ${komponenten.size} Komponenten, die Methode erwartet aber ${argumente.size} Argumente.",
        )
    }

    val zielKomponenten = when (val ziel = methode.zielMenge) {
        is Tupelraum -> ziel.komponenten
        else -> if (argumente.size == 1) listOf(ziel) else null
    }
    val argumentBereiche = argumente.map { it.werteVorrat }
    val exaktKompatibel = zielKomponenten != null && zielKomponenten == argumentBereiche

    return if (exaktKompatibel) {
        SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.TOTAL_GUELTIG,
            argumentAnzahl = argumente.size,
            ergebnisKomponenten = komponenten?.size ?: 1,
        )
    } else {
        SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.BEDINGT_GUELTIG,
            argumentAnzahl = argumente.size,
            ergebnisKomponenten = komponenten?.size ?: 1,
            voraussetzungen = argumentBereiche.mapIndexedTo(linkedSetOf()) { index, bereich ->
                "Die ${index + 1}. Ergebniskomponente muss in ${bereich.zuLatex()} liegen."
            },
            grund = "Die tatsächlichen Bildmengen müssen komponentenweise in den Argumentbereichen liegen.",
        )
    }
}

fun packeMethodenArgumente(argumente: List<MathematischesObjekt>): Tupel {
    require(argumente.isNotEmpty())
    return Tupel(argumente)
}

fun entpackeEineEbene(objekt: MathematischesObjekt): List<MathematischesObjekt> =
    entpackeEineEbeneOderNull(objekt)
        ?: throw IllegalArgumentException("${objekt::class.simpleName} besitzt keine entpackbare geordnete Komponentenstruktur.")

private fun entpackeEineEbeneOderNull(objekt: MathematischesObjekt): List<MathematischesObjekt>? = when (objekt) {
    is Tupel -> objekt.elemente
    is ZeilenVektor -> objekt.werte
    is SpaltenVektor -> objekt.werte
    else -> null
}

fun aeussererMethodenWertevorrat(methode: Methode): MengenAusdruck {
    val argumente = methode.methodenSignatur().argumente
    return if (argumente.size == 1) argumente.single().werteVorrat else Tupelraum(argumente.map { it.werteVorrat })
}

fun maximalerKompositionsWertevorrat(
    methode: Methode,
    ordnung: IterationsOrdnung,
): MengenAusdruck {
    val grundbereich = aeussererMethodenWertevorrat(methode)
    return when (ordnung) {
        is IterationsOrdnung.Konkret -> if (ordnung.wert.signum() <= 1) grundbereich else {
            RekursiverKompositionsWertevorrat(methode.name, ordnung, grundbereich)
        }
        is IterationsOrdnung.Symbolisch -> RekursiverKompositionsWertevorrat(methode.name, ordnung, grundbereich)
    }
}
