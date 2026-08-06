package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

enum class KompositionsEingangsModus {
    GETRENNTE_ARGUMENTE,
    GEPACKTES_TUPEL,
}

enum class KompositionsAusgangsModus {
    GEPACKT,
    ENTPACKT,
}

enum class KompositionsBereichsModus {
    MAXIMAL_ZULAESSIG,
    VOLLSTAENDIGER_URSPRUNGSBEREICH,
}

enum class SelbstkompositionsStatus {
    TOTAL_GUELTIG,
    EINGESCHRAENKT_GUELTIG,
    BEDINGT_GUELTIG,
    LEERER_WERTEVORRAT,
    MATHEMATISCH_UNMOEGLICH,
    NOCH_NICHT_IMPLEMENTIERT,
}

data class SelbstkompositionsPruefung(
    val status: SelbstkompositionsStatus,
    val argumentAnzahl: Int,
    val ergebnisKomponenten: Int?,
    val ursprungsWertevorrat: MengenAusdruck,
    val zielMenge: MengenAusdruck,
    val voraussetzungen: Set<Aussage> = emptySet(),
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
    val bereichsModus: KompositionsBereichsModus = KompositionsBereichsModus.MAXIMAL_ZULAESSIG,
    val werteVorrat: MengenAusdruck = maximalerKompositionsWertevorrat(methode, ordnung),
    val zielMenge: MengenAusdruck = zielMengeDerSelbstkomposition(methode, ordnung),
    val status: SelbstkompositionsStatus = SelbstkompositionsStatus.BEDINGT_GUELTIG,
    val voraussetzungen: Set<Aussage> = emptySet(),
) : MathematischesObjekt {
    val operatorId: String = IterationsArt.SELBSTKOMPOSITION.operatorId

    override fun zuLatex(): String = IterierterAusdruck(
        basis = methode,
        art = IterationsArt.SELBSTKOMPOSITION,
        ordnung = ordnung,
    ).zuLatex()
}

data class SelbstkompositionsErgebnis(
    val ausdruck: IterierteSelbstkomposition,
    val methode: Methode?,
    val status: SelbstkompositionsStatus,
    val ursprungsWertevorrat: MengenAusdruck,
    val maximalerWertevorrat: MengenAusdruck,
    val zielMenge: MengenAusdruck,
    val voraussetzungen: Set<Aussage> = emptySet(),
    val verwendeteEinschraenkungen: List<MengenAusdruck> = emptyList(),
    val begruendung: String,
)

fun pruefeSelbstkomposition(
    methode: Methode,
    kontext: RechenKontext = RechenKontext(),
): SelbstkompositionsPruefung {
    val signatur = runCatching { methode.methodenSignatur() }.getOrElse { fehler ->
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = methode.parameter.size,
            ergebnisKomponenten = null,
            ursprungsWertevorrat = LeereMenge,
            zielMenge = methode.zielMenge,
            grund = fehler.message ?: "Die Methodensignatur ist unvollständig.",
        )
    }
    if (signatur.argumente.isEmpty()) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = 0,
            ergebnisKomponenten = null,
            ursprungsWertevorrat = LeereMenge,
            zielMenge = methode.zielMenge,
            grund = "Nullstellige Methoden besitzen keinen erneuten Aufrufvertrag.",
        )
    }

    val argumentBereiche = signatur.argumente.map(MethodenArgument::werteVorrat)
    val grundbereich = aeussererMethodenWertevorrat(methode)
    val komponenten = ergebnisKomponentenFuerNaechstenAufruf(methode)
    if (komponenten == null) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = argumentBereiche.size,
            ergebnisKomponenten = null,
            ursprungsWertevorrat = grundbereich,
            zielMenge = methode.zielMenge,
            grund = "Das Ergebnis besitzt keinen eindeutigen einleistigen Aufrufvertrag für ${argumentBereiche.size} Argumente.",
        )
    }
    if (komponenten.size != argumentBereiche.size) {
        return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = argumentBereiche.size,
            ergebnisKomponenten = komponenten.size,
            ursprungsWertevorrat = grundbereich,
            zielMenge = methode.zielMenge,
            grund = "Das Ergebnis besitzt ${komponenten.size} Komponenten, die Methode erwartet aber ${argumentBereiche.size} Argumente.",
        )
    }

    val zielKomponenten = zielKomponentenFuerNaechstenAufruf(methode)
        ?: return SelbstkompositionsPruefung(
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            argumentAnzahl = argumentBereiche.size,
            ergebnisKomponenten = komponenten.size,
            ursprungsWertevorrat = grundbereich,
            zielMenge = methode.zielMenge,
            grund = "Die Zielmenge besitzt keinen eindeutigen Komponentenvertrag für den nächsten Aufruf.",
        )

    val voraussetzungen = linkedSetOf<Aussage>()
    var alleUebergaengeNachgewiesen = true
    zielKomponenten.zip(argumentBereiche).forEachIndexed { index, (ziel, argumentBereich) ->
        if (ziel == argumentBereich) return@forEachIndexed
        val beziehung = TeilmengenBeziehung(ziel, argumentBereich)
        when (beziehung.entscheide(kontext).wahrheitswert) {
            Wahrheitswert.Wahr -> Unit
            Wahrheitswert.Lüge -> {
                alleUebergaengeNachgewiesen = false
                voraussetzungen += UnentscheidbareAussage(
                    bezeichnung = "Bildkomponente_${index + 1}(${methode.name})\\subseteq${argumentBereich.zuLatex()}",
                    system = "Selbstkomposition",
                )
            }
            null -> {
                alleUebergaengeNachgewiesen = false
                voraussetzungen += beziehung
            }
        }
    }

    return SelbstkompositionsPruefung(
        status = if (alleUebergaengeNachgewiesen) {
            SelbstkompositionsStatus.TOTAL_GUELTIG
        } else {
            SelbstkompositionsStatus.BEDINGT_GUELTIG
        },
        argumentAnzahl = argumentBereiche.size,
        ergebnisKomponenten = komponenten.size,
        ursprungsWertevorrat = grundbereich,
        zielMenge = methode.zielMenge,
        voraussetzungen = voraussetzungen,
        grund = if (alleUebergaengeNachgewiesen) {
            "Alle deklarierten Ergebniskomponenten liegen in den Argumentbereichen."
        } else {
            "Der tatsächliche Bildabschluss muss für mindestens eine Komponente nachgewiesen werden."
        },
    )
}

fun werteSelbstkompositionAus(
    methode: Methode,
    ordnung: IterationsOrdnung,
    eingangsModus: KompositionsEingangsModus = KompositionsEingangsModus.GETRENNTE_ARGUMENTE,
    ausgangsModus: KompositionsAusgangsModus = KompositionsAusgangsModus.GEPACKT,
    bereichsModus: KompositionsBereichsModus = KompositionsBereichsModus.MAXIMAL_ZULAESSIG,
    kontext: RechenKontext = RechenKontext(),
    auswertungsBudget: Int = 12,
): SelbstkompositionsErgebnis {
    require(auswertungsBudget > 0)
    val pruefung = pruefeSelbstkomposition(methode, kontext)
    val grundbereich = pruefung.ursprungsWertevorrat
    if (!pruefung.istZulaessig) {
        val ausdruck = IterierteSelbstkomposition(
            methode = methode,
            ordnung = ordnung,
            eingangsModus = eingangsModus,
            ausgangsModus = ausgangsModus,
            bereichsModus = bereichsModus,
            werteVorrat = grundbereich,
            zielMenge = zielMengeDerSelbstkomposition(methode, ordnung),
            status = pruefung.status,
            voraussetzungen = pruefung.voraussetzungen,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = null,
            status = pruefung.status,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = grundbereich,
            zielMenge = ausdruck.zielMenge,
            voraussetzungen = pruefung.voraussetzungen,
            begruendung = pruefung.grund,
        )
    }

    if (ordnung is IterationsOrdnung.Konkret && ordnung.wert == BigInteger.ZERO) {
        val identitaet = erzeugeNullteKompositionsIdentitaet(methode, eingangsModus, ausgangsModus)
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            werteVorrat = grundbereich,
            zielMenge = grundbereich,
            status = SelbstkompositionsStatus.TOTAL_GUELTIG,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = identitaet,
            status = SelbstkompositionsStatus.TOTAL_GUELTIG,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = grundbereich,
            zielMenge = grundbereich,
            begruendung = "Ordnung null liefert die Identität auf dem ursprünglichen Argumentraum.",
        )
    }

    if (ordnung is IterationsOrdnung.Konkret && ordnung.wert == BigInteger.ONE) {
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            werteVorrat = grundbereich,
            zielMenge = methode.zielMenge,
            status = SelbstkompositionsStatus.TOTAL_GUELTIG,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = methode,
            status = SelbstkompositionsStatus.TOTAL_GUELTIG,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = grundbereich,
            zielMenge = methode.zielMenge,
            begruendung = "Ordnung eins liefert die ursprüngliche Methode.",
        )
    }

    if (ordnung is IterationsOrdnung.Symbolisch) {
        val status = if (pruefung.status == SelbstkompositionsStatus.TOTAL_GUELTIG) {
            SelbstkompositionsStatus.TOTAL_GUELTIG
        } else {
            SelbstkompositionsStatus.BEDINGT_GUELTIG
        }
        val werteVorrat = if (status == SelbstkompositionsStatus.TOTAL_GUELTIG) {
            grundbereich
        } else {
            RekursiverKompositionsWertevorrat(methode.name, ordnung, grundbereich)
        }
        val voraussetzungen = pruefung.voraussetzungen + ordnung.annahmen
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            werteVorrat,
            methode.zielMenge,
            status,
            voraussetzungen,
        )
        val symbolischeMethode = methode.copy(
            name = ausdruck.zuLatex(),
            vorschrift = ausdruck,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = symbolischeMethode,
            status = status,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = werteVorrat,
            zielMenge = methode.zielMenge,
            voraussetzungen = voraussetzungen,
            verwendeteEinschraenkungen = if (werteVorrat == grundbereich) emptyList() else listOf(werteVorrat),
            begruendung = "Symbolische Ordnung bleibt als strukturierte Selbstkomposition erhalten.",
        )
    }

    val konkreteOrdnung = (ordnung as IterationsOrdnung.Konkret).wert
    if (konkreteOrdnung > BigInteger.valueOf(auswertungsBudget.toLong())) {
        val werteVorrat = maximalerKompositionsWertevorrat(methode, ordnung)
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            werteVorrat,
            methode.zielMenge,
            SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            pruefung.voraussetzungen,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = methode.copy(name = ausdruck.zuLatex(), vorschrift = ausdruck),
            status = SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = werteVorrat,
            zielMenge = methode.zielMenge,
            voraussetzungen = pruefung.voraussetzungen,
            verwendeteEinschraenkungen = listOf(werteVorrat),
            begruendung = "Die konkrete Ordnung überschreitet das Auswertungsbudget und bleibt strukturiert.",
        )
    }

    val endlicheAnalyse = analysiereEndlichenKompositionsBereich(
        methode = methode,
        ordnung = konkreteOrdnung.toInt(),
        kontext = kontext,
    )
    val maximalerBereich = endlicheAnalyse?.werteVorrat
        ?: if (pruefung.status == SelbstkompositionsStatus.TOTAL_GUELTIG) {
            grundbereich
        } else {
            maximalerKompositionsWertevorrat(methode, ordnung)
        }
    val status = endlicheAnalyse?.status ?: pruefung.status

    if (bereichsModus == KompositionsBereichsModus.VOLLSTAENDIGER_URSPRUNGSBEREICH &&
        status in setOf(
            SelbstkompositionsStatus.EINGESCHRAENKT_GUELTIG,
            SelbstkompositionsStatus.LEERER_WERTEVORRAT,
        )
    ) {
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            maximalerBereich,
            methode.zielMenge,
            SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            pruefung.voraussetzungen,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = null,
            status = SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = maximalerBereich,
            zielMenge = methode.zielMenge,
            voraussetzungen = pruefung.voraussetzungen,
            verwendeteEinschraenkungen = listOf(maximalerBereich),
            begruendung = "Die verlangte Iteration ist nicht auf dem vollständigen ursprünglichen Wertevorrat definiert.",
        )
    }

    if (methode.parameter.size > 1 && maximalerBereich != grundbereich) {
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            maximalerBereich,
            methode.zielMenge,
            SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            pruefung.voraussetzungen,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = null,
            status = SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = maximalerBereich,
            zielMenge = methode.zielMenge,
            voraussetzungen = pruefung.voraussetzungen,
            verwendeteEinschraenkungen = listOf(maximalerBereich),
            begruendung = "Der korrelierte mehrstellige Teilbereich kann im aktuellen Methodenmodell nicht verlustfrei als getrennte Parameterdomänen gespeichert werden.",
        )
    }

    val vorschrift = runCatching {
        baueKonkreteSelbstkompositionsVorschrift(methode, konkreteOrdnung.toInt())
    }.getOrElse { fehler ->
        val ausdruck = IterierteSelbstkomposition(
            methode,
            ordnung,
            eingangsModus,
            ausgangsModus,
            bereichsModus,
            maximalerBereich,
            methode.zielMenge,
            SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            pruefung.voraussetzungen,
        )
        return SelbstkompositionsErgebnis(
            ausdruck = ausdruck,
            methode = null,
            status = SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT,
            ursprungsWertevorrat = grundbereich,
            maximalerWertevorrat = maximalerBereich,
            zielMenge = methode.zielMenge,
            voraussetzungen = pruefung.voraussetzungen,
            verwendeteEinschraenkungen = if (maximalerBereich == grundbereich) emptyList() else listOf(maximalerBereich),
            begruendung = fehler.message ?: "Die konkrete Kompositionsvorschrift konnte nicht aufgebaut werden.",
        )
    }

    val neueWerteVorräte = if (methode.parameter.size == 1) {
        methode.werteVorräte + (methode.parameter.single().name to maximalerBereich)
    } else {
        methode.werteVorräte
    }
    val ausdruck = IterierteSelbstkomposition(
        methode,
        ordnung,
        eingangsModus,
        ausgangsModus,
        bereichsModus,
        maximalerBereich,
        methode.zielMenge,
        status,
        pruefung.voraussetzungen,
    )
    val ergebnisMethode = methode.copy(
        name = ausdruck.zuLatex(),
        vorschrift = vorschrift,
        werteVorräte = neueWerteVorräte,
    )
    return SelbstkompositionsErgebnis(
        ausdruck = ausdruck,
        methode = ergebnisMethode,
        status = status,
        ursprungsWertevorrat = grundbereich,
        maximalerWertevorrat = maximalerBereich,
        zielMenge = methode.zielMenge,
        voraussetzungen = pruefung.voraussetzungen,
        verwendeteEinschraenkungen = if (maximalerBereich == grundbereich) emptyList() else listOf(maximalerBereich),
        begruendung = endlicheAnalyse?.begruendung ?: pruefung.grund,
    )
}

private data class EndlicheBereichsAnalyse(
    val werteVorrat: MengenAusdruck,
    val status: SelbstkompositionsStatus,
    val begruendung: String,
)

private fun analysiereEndlichenKompositionsBereich(
    methode: Methode,
    ordnung: Int,
    kontext: RechenKontext,
): EndlicheBereichsAnalyse? {
    if (ordnung <= 1) return EndlicheBereichsAnalyse(
        aeussererMethodenWertevorrat(methode),
        SelbstkompositionsStatus.TOTAL_GUELTIG,
        "Für Ordnung null oder eins ist keine erneute Einsetzung erforderlich.",
    )
    val mengen = methode.parameter.map { parameter ->
        methode.werteVorräte[parameter.name] as? EndlicheMenge ?: return null
    }
    val kandidaten = kartesischeArgumentListen(mengen.map { it.elemente.toList() })
    val zulaessig = mutableListOf<List<MathematischesObjekt>>()
    var unentscheidbar = false
    kandidaten.forEach { startArgumente ->
        var argumente = startArgumente
        var kandidatZulaessig = true
        repeat(ordnung) { index ->
            val ergebnis = methode.wendeAn(argumente)
            if (index < ordnung - 1) {
                val naechsteArgumente = runCatching {
                    argumenteFuerNaechstenAufruf(ergebnis, methode.parameter.size)
                }.getOrElse {
                    kandidatZulaessig = false
                    emptyList()
                }
                if (!kandidatZulaessig) return@repeat
                methode.parameter.zip(naechsteArgumente).forEach { (parameter, wert) ->
                    val bereich = methode.werteVorräte.getValue(parameter.name)
                    when (ElementBeziehung(wert, bereich).entscheide(kontext).wahrheitswert) {
                        Wahrheitswert.Wahr -> Unit
                        Wahrheitswert.Lüge -> kandidatZulaessig = false
                        null -> unentscheidbar = true
                    }
                }
                argumente = naechsteArgumente
            }
        }
        if (kandidatZulaessig) zulaessig += startArgumente
    }
    if (unentscheidbar) return null

    val elemente = zulaessig.mapTo(linkedSetOf()) { argumente ->
        if (argumente.size == 1) argumente.single() else Tupel(argumente)
    }
    val werteVorrat = EndlicheMenge(elemente)
    val status = when {
        elemente.isEmpty() -> SelbstkompositionsStatus.LEERER_WERTEVORRAT
        zulaessig.size == kandidaten.size -> SelbstkompositionsStatus.TOTAL_GUELTIG
        else -> SelbstkompositionsStatus.EINGESCHRAENKT_GUELTIG
    }
    return EndlicheBereichsAnalyse(
        werteVorrat = werteVorrat,
        status = status,
        begruendung = when (status) {
            SelbstkompositionsStatus.TOTAL_GUELTIG -> "Alle endlichen Ausgangsargumente bleiben während der Iteration zulässig."
            SelbstkompositionsStatus.EINGESCHRAENKT_GUELTIG -> "Nur ein echter endlicher Teilbereich bleibt während aller Zwischenaufrufe zulässig."
            SelbstkompositionsStatus.LEERER_WERTEVORRAT -> "Kein Ausgangsargument erlaubt alle erforderlichen Zwischenaufrufe."
            else -> "Endliche Bereichsanalyse."
        },
    )
}

private fun kartesischeArgumentListen(
    komponenten: List<List<MathematischesObjekt>>,
): List<List<MathematischesObjekt>> = komponenten.fold(listOf(emptyList())) { akkumuliert, werte ->
    akkumuliert.flatMap { vorher -> werte.map { wert -> vorher + wert } }
}

private fun baueKonkreteSelbstkompositionsVorschrift(
    methode: Methode,
    ordnung: Int,
): MathematischesObjekt {
    require(ordnung >= 1)
    var ausdruck = methode.vorschrift
    repeat(ordnung - 1) {
        val argumente = argumenteFuerNaechstenAufruf(ausdruck, methode.parameter.size)
        ausdruck = vereinfacheObjekt(
            ersetze(
                methode.vorschrift,
                methode.parameter.map(MethodenParameter::name).zip(argumente).toMap(),
            ),
        )
    }
    return ausdruck
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

private fun argumenteFuerNaechstenAufruf(
    objekt: MathematischesObjekt,
    argumentAnzahl: Int,
): List<MathematischesObjekt> = if (argumentAnzahl == 1) {
    listOf(objekt)
} else {
    val komponenten = entpackeEineEbene(objekt)
    require(komponenten.size == argumentAnzahl) {
        "Das Zwischenergebnis besitzt ${komponenten.size} Komponenten, erwartet werden $argumentAnzahl."
    }
    komponenten
}

private fun ergebnisKomponentenFuerNaechstenAufruf(
    methode: Methode,
): List<MathematischesObjekt>? = if (methode.parameter.size == 1) {
    listOf(methode.vorschrift)
} else {
    entpackeEineEbeneOderNull(methode.vorschrift)
}

private fun zielKomponentenFuerNaechstenAufruf(
    methode: Methode,
): List<MengenAusdruck>? {
    if (methode.parameter.size == 1) return listOf(methode.zielMenge)
    return when (val ziel = methode.zielMenge) {
        is Tupelraum -> ziel.komponenten
        is Vektorraum -> List(ziel.dimension) { ziel.skalarMenge }
        else -> null
    }
}

fun aeussererMethodenWertevorrat(methode: Methode): MengenAusdruck {
    val argumente = methode.methodenSignatur().argumente
    return if (argumente.size == 1) {
        argumente.single().werteVorrat
    } else {
        Tupelraum(argumente.map(MethodenArgument::werteVorrat))
    }
}

fun maximalerKompositionsWertevorrat(
    methode: Methode,
    ordnung: IterationsOrdnung,
): MengenAusdruck {
    val grundbereich = aeussererMethodenWertevorrat(methode)
    return when (ordnung) {
        is IterationsOrdnung.Konkret -> if (ordnung.wert <= BigInteger.ONE) {
            grundbereich
        } else {
            RekursiverKompositionsWertevorrat(methode.name, ordnung, grundbereich)
        }
        is IterationsOrdnung.Symbolisch -> RekursiverKompositionsWertevorrat(
            methode.name,
            ordnung,
            grundbereich,
        )
    }
}

private fun zielMengeDerSelbstkomposition(
    methode: Methode,
    ordnung: IterationsOrdnung,
): MengenAusdruck = when (ordnung) {
    is IterationsOrdnung.Konkret -> if (ordnung.wert == BigInteger.ZERO) {
        aeussererMethodenWertevorrat(methode)
    } else {
        methode.zielMenge
    }
    is IterationsOrdnung.Symbolisch -> methode.zielMenge
}

private fun erzeugeNullteKompositionsIdentitaet(
    methode: Methode,
    eingangsModus: KompositionsEingangsModus,
    ausgangsModus: KompositionsAusgangsModus,
): Methode {
    val signatur = methode.methodenSignatur()
    val grundbereich = aeussererMethodenWertevorrat(methode)
    if (eingangsModus == KompositionsEingangsModus.GEPACKTES_TUPEL) {
        val parameter = AllgemeinerParameter("argumente")
        return Methode(
            name = "\\operatorname{id}\\vert_{${grundbereich.zuLatex()}}",
            parameter = listOf(parameter),
            vorschrift = parameter,
            zielMenge = grundbereich,
            werteVorräte = mapOf(parameter.name to grundbereich),
        )
    }

    val parameter = methode.parameter
    val vorschrift = if (parameter.size == 1) {
        parameter.single()
    } else {
        Tupel(parameter)
    }
    val ausgabeNamen = if (
        parameter.size > 1 && ausgangsModus == KompositionsAusgangsModus.ENTPACKT
    ) {
        parameter.map { "id_${it.name}" }
    } else {
        listOf("wert")
    }
    return Methode(
        name = "\\operatorname{id}\\vert_{${grundbereich.zuLatex()}}",
        parameter = parameter,
        vorschrift = vorschrift,
        zielMenge = if (parameter.size == 1) signatur.argumente.single().werteVorrat else grundbereich,
        werteVorräte = methode.werteVorräte,
        ausgabeNamen = ausgabeNamen,
    )
}
