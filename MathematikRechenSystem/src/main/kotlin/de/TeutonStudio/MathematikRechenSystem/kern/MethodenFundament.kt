package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonischer Fachname des einzigen methodenartigen Laufzeitobjekts.
 *
 * Der bisherige Quellname [Funktion] bleibt als identischer Typ bestehen, damit
 * bestehende Karten, Tests und Erweiterungen nicht durch eine reine Umbenennung
 * gebrochen werden. Es entsteht ausdrücklich keine zweite Objektklasse.
 */
typealias Methode = Funktion
typealias MethodenParameter = FunktionsParameter
typealias GebundeneMethode = GebundeneFunktion

/** Die eine wiederverwendbare Zielmenge aller Prädikate. */
val WahrheitsMenge: MengenAusdruck = EndlicheMenge(
    setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
)

data class MethodenArgument(
    val parameter: FunktionsParameter,
    val werteVorrat: MengenAusdruck,
)

data class MethodenSignatur(
    val argumente: List<MethodenArgument>,
    val zielMenge: MengenAusdruck,
) {
    /**
     * Der Wertevorrat besteht aus geordneten Argumenttupeln. Auch ein einzelnes
     * Argument bleibt dadurch eine eindimensionale Tupelkomponente. Nullstellige
     * Methoden verwenden gemäß Atlas-Konvention die leere Menge.
     */
    val werteVorrat: MengenAusdruck
        get() = if (argumente.isEmpty()) LeereMenge else Tupelraum(argumente.map { it.werteVorrat })
}

/** Fachliche, ausschließlich abgeleitete Nutzerbegriffe unterhalb von Methode. */
enum class MethodenAlias(val anzeigeName: String) {
    Funktion("Funktion"),
    Abbildung("Abbildung"),
    Prädikat("Prädikat"),
}

/**
 * Eine Methode besitzt fachlich genau eine Vorschrift. Historische benannte
 * Mehrfachausgaben werden rückwärtskompatibel als ein geordnetes Ergebnistupel
 * gelesen und nicht als mehrere Methodenrückgaben behandelt.
 */
val Funktion.vorschrift: MathematischesObjekt
    get() = when (ausgaben.size) {
        0 -> error("Die Methode '$name' besitzt keine Vorschrift.")
        1 -> ausgaben.values.single()
        else -> Tupel(ausgaben.values.toList())
    }

/** Genau eine Zielmenge; historische Mehrfachziele bilden einen Tupelraum. */
val Funktion.zielMenge: MengenAusdruck
    get() = when (ausgaben.size) {
        0 -> error("Die Methode '$name' besitzt keine Zielmenge.")
        1 -> zielMengeFür(ausgaben.keys.single())
        else -> Tupelraum(ausgaben.keys.map(::zielMengeFür))
    }

/** Die Parameterreihenfolge ist Teil der Semantik und wird nie aus einer Map abgeleitet. */
fun Funktion.methodenSignatur(): MethodenSignatur = MethodenSignatur(
    argumente = parameter.map { parameter ->
        MethodenArgument(
            parameter = parameter,
            werteVorrat = werteVorräte[parameter.name]
                ?: error("Für das Methodenargument '${parameter.name}' konnte kein Wertevorrat ermittelt werden."),
        )
    },
    zielMenge = zielMenge,
)

/**
 * Wendet die Methode vollständig an und vereinheitlicht historische
 * Mehrfachausgaben zu genau einem Ergebnisobjekt.
 */
fun Funktion.wendeKanonischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val fehlend = parameter.map(FunktionsParameter::name).filterNot(argumente::containsKey)
    require(fehlend.isEmpty()) {
        "Für die Methode '$name' fehlen die Argumente ${fehlend.joinToString()}."
    }
    val ergebnisse = wendeAn(argumente)
    return when (ergebnisse.size) {
        0 -> error("Die Methode '$name' hat für ein Element ihres Wertevorrats kein Ergebnis geliefert.")
        1 -> ergebnisse.values.single()
        else -> Tupel(ausgaben.keys.map { ergebnisse.getValue(it) })
    }
}

/** Prädikate werden nicht durch Entscheidbarkeit, sondern nur durch ihre Signatur erkannt. */
fun Funktion.istPrädikat(): Boolean =
    runCatching { vorschrift is Aussage && zielMenge == WahrheitsMenge }.getOrDefault(false)

/** Funktionen verarbeiten und liefern ausschließlich Zahl-, Vektor-, Matrix- oder Tensorräume. */
fun Funktion.istFunktion(): Boolean = runCatching {
    val signatur = methodenSignatur()
    signatur.argumente.all { it.werteVorrat.istFunktionalerRaum() } &&
        signatur.zielMenge.istFunktionalerRaum()
}.getOrDefault(false)

/** Abbildungen nehmen Mengenobjekte entgegen und liefern ein Mengenobjekt. */
fun Funktion.istAbbildung(): Boolean = runCatching {
    parameter.isNotEmpty() &&
        parameter.all { it.istMengenArgument() } &&
        vorschrift is MengenAusdruck
}.getOrDefault(false)

fun Funktion.aliase(): Set<MethodenAlias> = buildSet {
    if (istFunktion()) add(MethodenAlias.Funktion)
    if (istAbbildung()) add(MethodenAlias.Abbildung)
    if (istPrädikat()) add(MethodenAlias.Prädikat)
}

fun Funktion.aliasAnzeige(): String = buildList {
    add("Methode")
    addAll(aliase().map(MethodenAlias::anzeigeName))
}.joinToString(" · ")

private fun FunktionsParameter.istMengenArgument(): Boolean = when (this) {
    is MengenParameter -> true
    is TypisiertesElement -> anschlussArt == "mathematik.menge" || anschlussArt.startsWith("mathematik.menge.")
    else -> false
}

private fun MengenAusdruck.istFunktionalerRaum(): Boolean = when (this) {
    NatürlicheZahlen,
    GanzeZahlen,
    RationaleZahlen,
    ReelleZahlen,
    KomplexeZahlen -> true
    is Vektorraum -> skalarMenge.istFunktionalerRaum()
    is Matrizenraum -> skalarMenge.istFunktionalerRaum()
    is Tupelraum -> komponenten.all { it.istFunktionalerRaum() }
    else -> this::class.simpleName?.contains("Tensor", ignoreCase = true) == true
}

/** Semantische Eingangsprüfung statt immer weiterer Methoden-Anschlussunterarten. */
fun interface MethodenAnforderung {
    fun prüfe(methode: Funktion): String?

    data object Beliebig : MethodenAnforderung {
        override fun prüfe(methode: Funktion): String? = null
    }

    data class Stelligkeit(val anzahl: Int) : MethodenAnforderung {
        init { require(anzahl >= 0) }
        override fun prüfe(methode: Funktion): String? =
            if (methode.parameter.size == anzahl) null
            else "Die Methode '${methode.name}' muss genau $anzahl Argumente besitzen."
    }

    data object Prädikat : MethodenAnforderung {
        override fun prüfe(methode: Funktion): String? =
            if (methode.istPrädikat()) null
            else "Die Methode '${methode.name}' erfüllt das Prädikatskriterium nicht."
    }

    data object Endomorphismus : MethodenAnforderung {
        override fun prüfe(methode: Funktion): String? = runCatching {
            val signatur = methode.methodenSignatur()
            if (signatur.argumente.size == 1 && signatur.argumente.single().werteVorrat == signatur.zielMenge) null
            else "Die Methode '${methode.name}' ist kein Endomorphismus."
        }.getOrElse { it.message ?: "Die Methodensignatur ist unvollständig." }
    }

    data class Alle(val anforderungen: List<MethodenAnforderung>) : MethodenAnforderung {
        override fun prüfe(methode: Funktion): String? =
            anforderungen.firstNotNullOfOrNull { it.prüfe(methode) }
    }
}
