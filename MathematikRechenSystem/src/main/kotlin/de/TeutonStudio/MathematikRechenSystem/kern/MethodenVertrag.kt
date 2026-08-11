package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Offener Oberbegriff für methodenartige Werte im Atlas.
 *
 * Eine Methode muss nicht automatisch eine symbolische mathematische Vorschrift
 * besitzen oder durch Variablensubstitution ausführbar sein. Die mathematischen
 * Member unten sind ein Übergangsvertrag für den bestehenden Code: Fremdmethoden
 * müssen sie nicht überschreiben und erhalten bei ihrer Verwendung einen klaren
 * Capability-Fehler.
 *
 * Bis G0.2 transportiert der gemeinsame Kartenruntime-Wertkanal ausschließlich
 * [MathematischesObjekt]. Deshalb bleibt Methode vorerst ein indirekter Untertyp
 * davon. Die Ausführungssemantik ist bereits vollständig über Capabilities getrennt.
 */
interface Methode : MathematischesObjekt {
    val name: String

    /** Sichere Minimaldarstellung für Methoden ohne mathematischen Formelrumpf. */
    override fun zuLatex(): String = name

    /*
     * Quellkompatible mathematische Übergangsoberfläche. Diese Eigenschaften sind
     * absichtlich nicht abstrakt: Eine neue Domänenmethode implementiert weiterhin
     * nur den kleinen Obervertrag. MathematischeMethode überschreibt alle Member mit
     * echten Daten. G0.2 ersetzt diese Übergangsfläche durch den allgemeinen Typkern.
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

    val bereichsanpassung: MethodenBereichsanpassung?
        get() = alsMathematischeMethode("mathematische Bereichsanpassungen").bereichsanpassung

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
 * bereitstellen. G0.2 verallgemeinert diesen Vertrag auf den neuen Typkern.
 */
interface SignaturtragendeMethode : Methode {
    val signatur: MethodenSignatur
}

/**
 * Capability für Methoden, die der Mathematikkern rein durch Bindung mathematischer
 * Argumente auswerten darf. Script- oder Engine-Methoden dürfen diese Capability
 * ausdrücklich nicht allein aufgrund von [Methode] erhalten.
 */
interface MathematischAuswertbareMethode : SignaturtragendeMethode {
    fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt
}

/**
 * Liefert die konkrete mathematische Implementierung oder einen fachlich eindeutigen
 * Fehler. Mathematische Operatoren benutzen diese Grenze, statt bei jeder Methode
 * stillschweigend eine symbolische Vorschrift vorauszusetzen.
 */
fun Methode.alsMathematischeMethode(operation: String = "diese Operation"): MathematischeMethode =
    this as? MathematischeMethode
        ?: error("Die Methode '$name' unterstützt $operation nicht als symbolische mathematische Methode.")

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
