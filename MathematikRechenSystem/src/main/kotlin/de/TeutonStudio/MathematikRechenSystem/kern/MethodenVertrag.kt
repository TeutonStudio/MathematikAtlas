package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend

/**
 * Domänenneutraler Oberbegriff für methodenartige Atlaswerte.
 *
 * Der allgemeine Vertrag kennt ausschließlich Name und semantischen Typ. Er setzt
 * weder [MathematischesObjekt] noch [MengenAusdruck], symbolische Vorschriften oder
 * mathematische Auswertung voraus. Damit können spätere Script-/Engine-Methoden den
 * Methodenkanal verwenden, ohne künstlich zu mathematischen Objekten zu werden.
 */
interface Methode : AtlasWert, TypTragend {
    val name: String

    override val typAusdruck: TypAusdruck
        get() = methodenTypAusdruck()
}

/** Capability für Methoden mit neutraler, ausschließlich typbasierter Signatur. */
interface SignaturtragendeMethode : Methode {
    val signatur: MethodenSignatur
}

/**
 * Zusätzliche Capability ausschließlich für mathematische Mengen-/Raumsemantik.
 * Typinformation und mathematische Definitions-/Zielmengen bleiben dadurch getrennt.
 */
interface MathematischeSignaturtragendeMethode : Methode {
    val mathematischeSignatur: MathematischeMethodenSignatur
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

/** Projektion der optionalen Bereichsanpassungs-Herkunft. */
val Methode.bereichsanpassung: MethodenBereichsanpassung?
    get() = (this as? BereichsanpassungsTragendeMethode)?.bereichsanpassung

/**
 * Capability für Methoden, die der Mathematikkern durch Bindung mathematischer
 * Argumente auswerten darf. Eine mathematisch auswertbare Methode ist zugleich ein
 * mathematisches Objekt; eine bloß [SignaturtragendeMethode] ausdrücklich nicht.
 */
interface MathematischAuswertbareMethode :
    SignaturtragendeMethode,
    MathematischeSignaturtragendeMethode,
    BereichsanpassungsTragendeMethode,
    MathematischesObjekt {
    fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt
}

/**
 * Liefert die konkrete mathematische Implementierung oder einen fachlich eindeutigen
 * Fehler. Strukturierte mathematische Methodenoperatoren werden nur an dieser Grenze
 * materialisiert; der allgemeine [Methode]-Vertrag erfährt davon nichts.
 */
fun Methode.alsMathematischeMethode(operation: String = "diese Operation"): MathematischeMethode = when (this) {
    is MathematischeMethode -> this
    is MethodenRestriktion -> materialisiere()
    is MethodenBereichsanpassung -> materialisiere()
    else -> error("Die Methode '$name' unterstützt $operation nicht als symbolische mathematische Methode.")
}

/*
 * Zentral gebündelte mathematische Legacy-Projektionen.
 *
 * Sie sind absichtlich Extension-Adapter und damit NICHT Teil des allgemeinen
 * Methode-Vertrags. Bestehender Mathematikcode kann schrittweise migriert werden;
 * neuer generischer Graph-/Scriptcode soll diese Adapter nicht verwenden.
 */

@Deprecated("Mathematische Legacy-Projektion; verwende MathematischeSignaturtragendeMethode.mathematischeSignatur.")
val Methode.parameter: List<MethodenParameter>
    get() = alsMathematischeMethode("mathematische Parameter").parameter

@Deprecated("Mathematische Legacy-Projektion; verwende die konkrete mathematische Capability.")
val Methode.vorschrift: MathematischesObjekt
    get() = alsMathematischeMethode("eine symbolische Vorschrift").vorschrift

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.zielRaum bzw. zielMengeFür().")
val Methode.zielMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Zielmenge").zielMenge

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.argumente.")
val Methode.werteVorräte: Map<String, MengenAusdruck>
    get() = alsMathematischeMethode("mathematische Definitionsmengen").werteVorräte

@Deprecated("Mathematische Legacy-Projektion; verwende mathematischeSignatur.ergebnisse.")
val Methode.ausgabeNamen: List<String>
    get() = alsMathematischeMethode("mathematische Ausgaben").ausgabeNamen

@Deprecated("Umbenannt; verwende mathematischeSignatur.effektiverDefinitionsRaum.")
val Methode.effektiverWerteVorrat: MengenAusdruck?
    get() = alsMathematischeMethode("einen mathematischen Definitionsraum").effektiverWerteVorrat

@Deprecated("Mathematische Legacy-Projektion; verwende die konkrete mathematische Capability.")
fun Methode.vorschriftFür(ausgabe: String): MathematischesObjekt =
    alsMathematischeMethode("eine symbolische Vorschrift").vorschriftFür(ausgabe)

fun Methode.zielMengeFür(ausgabe: String): MengenAusdruck =
    alsMathematischeMethode("eine mathematische Zielmenge").zielMengeFür(ausgabe)

fun Methode.zielMengeFür(
    ausgabe: String,
    bindungen: Map<String, MathematischesObjekt>,
): MengenAusdruck = alsMathematischeMethode("eine mathematische Zielmenge")
    .zielMengeFür(ausgabe, bindungen)

val Methode.einzigeZielMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Zielmenge").einzigeZielMenge

@Deprecated("Mathematische Legacy-Projektion; nur für mengenwertige Iterationsmethoden.")
val Methode.grundMenge: MengenAusdruck
    get() = alsMathematischeMethode("eine mathematische Grundmenge").grundMenge

fun Methode.grundMengeFürMengenAusgabe(): MengenAusdruck =
    alsMathematischeMethode("eine mengenwertige mathematische Vorschrift").grundMengeFürMengenAusgabe()

fun Methode.binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode =
    alsMathematischeMethode("symbolische Parameterbindung").binde(bindungen)

fun Methode.wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt =
    alsMathematischeMethode("mathematische Auswertung").wendeAn(argumente)

fun Methode.wendeAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val auswertbar = this as? MathematischAuswertbareMethode
        ?: error("Die Methode '$name' besitzt keine mathematische Auswertungs-Capability.")
    return auswertbar.wendeMathematischAn(argumente)
}

fun Methode.einzigeAusgabe(): Pair<String, MathematischesObjekt> =
    alsMathematischeMethode("eine symbolische mathematische Ausgabe").einzigeAusgabe()

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

/** Historischer Konstruktor ausschließlich für Lademigrationen/Testdaten. */
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

/**
 * `copy` kann wegen der von Kotlin generierten Data-Class-Methode nicht Bestandteil
 * des Interface sein. Für statisch als Methode typisierte mathematische Werte bleibt
 * deshalb genau dieser zentrale Übergangsadapter bestehen.
 */
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
