package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend

/**
 * Offener Oberbegriff für methodenartige Werte im Atlas.
 *
 * Eine Methode muss nicht automatisch eine symbolische mathematische Vorschrift
 * besitzen oder durch Variablensubstitution ausführbar sein. Der semantische Typ
 * ist seit G0.2 Teil des gemeinsamen neutralen Typkerns; fremde Methoden können
 * ihn überschreiben, ohne mathematische Auswertungs-Capabilities zu erben.
 */
interface Methode : MathematischesObjekt, TypTragend {
    val name: String

    /** Sichere Minimaldarstellung für Methoden ohne mathematischen Formelrumpf. */
    override fun zuLatex(): String = name

    /** Allgemeiner Typkanal für Mathematik-, Script- und spätere Engine-Methoden. */
    override val typAusdruck: TypAusdruck
        get() = methodenTypAusdruck()

    /*
     * Quellkompatible mathematische Übergangsoberfläche. Diese Eigenschaften sind
     * absichtlich nicht abstrakt: Eine neue Domänenmethode implementiert weiterhin
     * nur den kleinen Obervertrag. MathematischeMethode überschreibt alle Member mit
     * echten Daten. Fachcode soll neue Typanforderungen über [typAusdruck] ausdrücken.
     */
    val parameter: List<MethodenParameter>
        get() = alsMathematischeMethode("mathematische Parameter").parameter

    val vorschrift: MathematischesObjekt
        get() = alsMathematischeMethode("eine symbolische Vorschrift").vorschrift

    val zielMenge: MengenAusdruck
        get() = alsMathematischeMethode("eine mathematische Zielmenge").zielMenge

    val werteVorräte: Map<String, MengenAusdruck>
        get() = alsMathematischeMethode("mathematische Wertevorräte").werteVorräte

    val ausgabeNamen: List<String>
        get() = alsMathematischeMethode("mathematische Ausgaben").ausgabeNamen

    val effektiverWerteVorrat: MengenAusdruck?
        get() = alsMathematischeMethode("einen mathematischen Wertevorrat").effektiverWerteVorrat

    fun vorschriftFür(ausgabe: String): MathematischesObjekt =
        alsMathematischeMethode("eine symbolische Vorschrift").vorschriftFür(ausgabe)

    fun zielMengeFür(ausgabe: String): MengenAusdruck =
        alsMathematischeMethode("eine mathematische Zielmenge").zielMengeFür(ausgabe)

    fun zielMengeFür(
        ausgabe: String,
        bindungen: Map<String, MathematischesObjekt>,
    ): MengenAusdruck = alsMathematischeMethode("eine mathematische Zielmenge")
        .zielMengeFür(ausgabe, bindungen)

    val einzigeZielMenge: MengenAusdruck
        get() = alsMathematischeMethode("eine mathematische Zielmenge").einzigeZielMenge

    val grundMenge: MengenAusdruck
        get() = alsMathematischeMethode("eine mathematische Grundmenge").grundMenge

    fun grundMengeFürMengenAusgabe(): MengenAusdruck =
        alsMathematischeMethode("eine mengenwertige mathematische Vorschrift").grundMengeFürMengenAusgabe()

    fun binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode =
        alsMathematischeMethode("symbolische Parameterbindung").binde(bindungen)

    fun wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt =
        alsMathematischeMethode("mathematische Auswertung").wendeAn(argumente)

    fun wendeAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val auswertbar = this as? MathematischAuswertbareMethode
            ?: error("Die Methode '$name' besitzt keine mathematische Auswertungs-Capability.")
        return auswertbar.wendeMathematischAn(argumente)
    }

    fun einzigeAusgabe(): Pair<String, MathematischesObjekt> =
        alsMathematischeMethode("eine symbolische mathematische Ausgabe").einzigeAusgabe()

    fun prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt> =
        alsMathematischeMethode("mathematische Iteration").prüfeAlsIterationsMethode(erwartetMengenwert)
}

/**
 * Capability für Methoden, die die heutige mathematische [MethodenSignatur]
 * bereitstellen. Sie liefert über G0.2 automatisch einen strukturierten Methodentyp.
 */
interface SignaturtragendeMethode : Methode {
    val signatur: MethodenSignatur
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
 * Capability für Methoden, die der Mathematikkern rein durch Bindung mathematischer
 * Argumente auswerten darf. Script- oder Engine-Methoden dürfen diese Capability
 * ausdrücklich nicht allein aufgrund von [Methode] erhalten.
 *
 * Die zusätzliche Herkunfts-Capability ist ein Übergangsvertrag für die konkrete
 * Mathematikimplementierung; ihr Default ist leer und sie erweitert [Methode] nicht.
 */
interface MathematischAuswertbareMethode : SignaturtragendeMethode, BereichsanpassungsTragendeMethode {
    fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt
}

/**
 * Liefert die konkrete mathematische Implementierung oder einen fachlich eindeutigen
 * Fehler. Strukturierte mathematische Methodenoperatoren werden an dieser expliziten
 * Mathematikgrenze materialisiert; der allgemeine [Methode]-Vertrag erfährt davon nichts.
 */
fun Methode.alsMathematischeMethode(operation: String = "diese Operation"): MathematischeMethode = when (this) {
    is MathematischeMethode -> this
    is MethodenRestriktion -> materialisiere()
    is MethodenBereichsanpassung -> materialisiere()
    else -> error("Die Methode '$name' unterstützt $operation nicht als symbolische mathematische Methode.")
}

/** Quellkompatibler kanonischer Konstruktor für bestehende Aufrufer. */
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

/**
 * `copy` kann wegen der von Kotlin generierten Data-Class-Methode nicht Bestandteil
 * des Interface sein. Für statisch als Methode typisierte mathematische Werte bleibt
 * deshalb genau dieser eine Übergangsadapter bestehen.
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
