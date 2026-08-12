package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend

/**
 * Domänenneutraler Oberbegriff für methodenartige Atlaswerte.
 *
 * Der Vertrag kennt weder mathematische Objekte noch Mengen, Vorschriften oder
 * Substitution. Script- und Engine-Methoden können ihn deshalb ohne Mathematikmodell
 * implementieren.
 */
interface Methode : AtlasWert, TypTragend {
    val name: String

    override val typAusdruck: TypAusdruck
        get() = methodenTypAusdruck()
}

/** Capability für Methoden mit neutraler semantischer Typ-Signatur. */
interface SignaturtragendeMethode : Methode {
    val signatur: MethodenSignatur
}

/**
 * Zusätzliche Capability ausschließlich für mathematische Raum-/Mengensemantik.
 * Die neutrale [signatur] wird vollständig daraus abgeleitet, nicht umgekehrt.
 */
interface MathematischeSignaturtragendeMethode : SignaturtragendeMethode {
    val mathematischeSignatur: MathematischeMethodenSignatur

    override val signatur: MethodenSignatur
        get() = mathematischeSignatur.typSignatur
}

/**
 * Übergangs-Capability für mathematische Methoden, die eine strukturierte Herkunft
 * aus einer Bereichsanpassung tragen. Sie gehört ausdrücklich nicht zum allgemeinen
 * [Methode]-Vertrag und kann von Script-/Engine-Methoden vollständig ignoriert werden.
 */
interface BereichsanpassungsTragendeMethode : Methode {
    val bereichsanpassung: MethodenBereichsanpassung?
        get() = null
}

/** Quellkompatible Projektion der optionalen Bereichsanpassungs-Herkunft. */
val Methode.bereichsanpassung: MethodenBereichsanpassung?
    get() = (this as? BereichsanpassungsTragendeMethode)?.bereichsanpassung

/**
 * Mathematische Auswertung ist eine optionale Capability einer allgemeinen Methode.
 * Sie macht die konkrete mathematische Methode zugleich zu einem mathematischen Objekt,
 * ohne diese Forderung auf [Methode] oder [SignaturtragendeMethode] zu übertragen.
 */
interface MathematischAuswertbareMethode :
    MathematischeSignaturtragendeMethode,
    BereichsanpassungsTragendeMethode,
    MathematischesObjekt {
    fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt
}

/**
 * Symbolische Mathematik-Capability mit der heute noch benötigten Parameter- und
 * Vorschriftsoberfläche. Sie bündelt die Legacy-Felder an genau einer Mathematikgrenze,
 * statt sie im allgemeinen [Methode]-Vertrag zu verewigen.
 */
interface SymbolischMathematischeMethode : MathematischAuswertbareMethode {
    val parameter: List<MethodenParameter>
    val vorschrift: MathematischesObjekt
    val zielMenge: MengenAusdruck
    val werteVorräte: Map<String, MengenAusdruck>
    val ausgabeNamen: List<String>
    val effektiverWerteVorrat: MengenAusdruck?

    fun vorschriftFür(ausgabe: String): MathematischesObjekt
    fun zielMengeFür(ausgabe: String): MengenAusdruck
    fun zielMengeFür(ausgabe: String, bindungen: Map<String, MathematischesObjekt>): MengenAusdruck
    val einzigeZielMenge: MengenAusdruck
    val grundMenge: MengenAusdruck
    fun grundMengeFürMengenAusgabe(): MengenAusdruck
    fun binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode
    fun wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt
    fun einzigeAusgabe(): Pair<String, MathematischesObjekt>
    fun prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt>
}

/**
 * Liefert die konkrete symbolische Mathematikimplementierung oder einen fachlich
 * eindeutigen Fehler. Strukturierte mathematische Methodenoperatoren werden an dieser
 * Grenze materialisiert; der allgemeine [Methode]-Vertrag erfährt davon nichts.
 */
fun Methode.alsMathematischeMethode(operation: String = "diese Operation"): MathematischeMethode = when (this) {
    is MathematischeMethode -> this
    is MethodenRestriktion -> materialisiere()
    is MethodenBereichsanpassung -> materialisiere()
    else -> error("Die Methode '$name' unterstützt $operation nicht als symbolische mathematische Methode.")
}

/*
 * Zentralisierte Legacy-Projektionen. Sie liegen bewusst NICHT mehr auf [Methode].
 * Alte Mathematikaufrufer bleiben damit quellkompatibel, während neue generische
 * Methodenimplementierungen keinerlei Mengen- oder Vorschriftsfelder erfüllen müssen.
 */
@Deprecated("Mathematische Legacy-Projektion; verwende MathematischeSignaturtragendeMethode.mathematischeSignatur.")
val Methode.parameter: List<MethodenParameter>
    get() = alsMathematischeMethode("mathematische Parameter").parameter

@Deprecated("Mathematische Legacy-Projektion; verwende die symbolische MathematischeMethode explizit.")
val Methode.vorschrift: MathematischesObjekt
    get() = alsMathematischeMethode("eine symbolische Vorschrift").vorschrift

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.zielRaum bzw. zielMengeFür(...).")
val Methode.zielMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Zielmenge").zielMenge

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.argumente.")
val Methode.werteVorräte: Map<String, MengenAusdruck>
    get() = alsMathematischeMethode("mathematische Definitionsmengen").werteVorräte

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.ergebnisse.")
val Methode.ausgabeNamen: List<String>
    get() = alsMathematischeMethode("mathematische Ausgaben").ausgabeNamen

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.effektiverDefinitionsRaum.")
val Methode.effektiverWerteVorrat: MengenAusdruck?
    get() = alsMathematischeMethode("einen mathematischen Definitionsraum").effektiverWerteVorrat

@Deprecated("Mathematische Legacy-Projektion; verwende MathematischeMethode.vorschriftFür(...).")
fun Methode.vorschriftFür(ausgabe: String): MathematischesObjekt =
    alsMathematischeMethode("eine symbolische Vorschrift").vorschriftFür(ausgabe)

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.zielMengeFür(...).")
fun Methode.zielMengeFür(ausgabe: String): MengenAusdruck =
    alsMathematischeMethode("eine mathematische Zielmenge").zielMengeFür(ausgabe)

@Deprecated("Mathematische Legacy-Projektion; verwende die mathematische Capability explizit.")
fun Methode.zielMengeFür(
    ausgabe: String,
    bindungen: Map<String, MathematischesObjekt>,
): MengenAusdruck = alsMathematischeMethode("eine mathematische Zielmenge")
    .zielMengeFür(ausgabe, bindungen)

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.ergebnisse.")
val Methode.einzigeZielMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Zielmenge").einzigeZielMenge

@Deprecated("Mathematische Legacy-Projektion für Iteration.")
val Methode.grundMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Grundmenge").grundMenge

@Deprecated("Mathematische Legacy-Projektion für Iteration.")
fun Methode.grundMengeFürMengenAusgabe(): MengenAusdruck =
    alsMathematischeMethode("eine mengenwertige mathematische Vorschrift").grundMengeFürMengenAusgabe()

@Deprecated("Mathematische Legacy-Projektion; verwende MathematischeMethode.binde(...).")
fun Methode.binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode =
    alsMathematischeMethode("symbolische Parameterbindung").binde(bindungen)

@Deprecated("Mathematische Legacy-Projektion; verwende MathematischAuswertbareMethode.")
fun Methode.wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt =
    alsMathematischeMethode("mathematische Auswertung").wendeAn(argumente)

@Deprecated("Mathematische Legacy-Projektion; verwende MathematischAuswertbareMethode.")
fun Methode.wendeAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val auswertbar = this as? MathematischAuswertbareMethode
        ?: error("Die Methode '$name' besitzt keine mathematische Auswertungs-Capability.")
    return auswertbar.wendeMathematischAn(argumente)
}

@Deprecated("Mathematische Legacy-Projektion.")
fun Methode.einzigeAusgabe(): Pair<String, MathematischesObjekt> =
    alsMathematischeMethode("eine symbolische mathematische Ausgabe").einzigeAusgabe()

@Deprecated("Mathematische Legacy-Projektion für Iteration.")
fun Methode.prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt> =
    alsMathematischeMethode("mathematische Iteration").prüfeAlsIterationsMethode(erwartetMengenwert)

/** Quellkompatibler kanonischer Konstruktor für bestehende Mathematikaufrufer. */
@Suppress("FunctionName")
fun Methode(
    name: String,
    parameter: List<MethodenParameter>,
    vorschrift: MathematischesObjekt,
    zielMenge: MengenAusdruck,
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    ausgabeNamen: List<String> = listOf("wert"),
    effektiverWerteVorrat: MengenAusdruck? = null,
    bereichsanpassung: MethodenBereichsanpassung? = null,
): MathematischeMethode = MathematischeMethode(
    name = name,
    parameter = parameter,
    vorschrift = vorschrift,
    zielMenge = zielMenge,
    werteVorräte = werteVorräte,
    ausgabeNamen = ausgabeNamen,
    effektiverWerteVorrat = effektiverWerteVorrat,
    bereichsanpassung = bereichsanpassung,
)

/** Quellkompatibler historischer Konstruktor ausschließlich für Lademigrationen/Testdaten. */
@Deprecated("Nur für historische Daten; verwende den kanonischen Methoden-Konstruktor.")
@Suppress("FunctionName")
fun Methode(
    name: String,
    parameter: List<MethodenParameter>,
    ausgaben: Map<String, MathematischesObjekt>,
    zielMengen: Map<String, MengenAusdruck> = emptyMap(),
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
): MathematischeMethode = MathematischeMethode(
    name = name,
    parameter = parameter,
    ausgaben = ausgaben,
    zielMengen = zielMengen,
    werteVorräte = werteVorräte,
)

/** Übergangsadapter für statisch als [Methode] typisierte mathematische Werte. */
@Deprecated("Mathematische Legacy-Projektion; verenge zuerst auf MathematischeMethode.")
fun Methode.copy(
    name: String = this.name,
    parameter: List<MethodenParameter> = this.parameter,
    vorschrift: MathematischesObjekt = this.vorschrift,
    zielMenge: MengenAusdruck = this.zielMenge,
    werteVorräte: Map<String, MengenAusdruck> = this.werteVorräte,
    ausgabeNamen: List<String> = this.ausgabeNamen,
    effektiverWerteVorrat: MengenAusdruck? = this.effektiverWerteVorrat,
    bereichsanpassung: MethodenBereichsanpassung? = this.bereichsanpassung,
): MathematischeMethode = alsMathematischeMethode("mathematische Kopie").copy(
    name = name,
    parameter = parameter,
    vorschrift = vorschrift,
    zielMenge = zielMenge,
    werteVorräte = werteVorräte,
    ausgabeNamen = ausgabeNamen,
    effektiverWerteVorrat = effektiverWerteVorrat,
    bereichsanpassung = bereichsanpassung,
)
