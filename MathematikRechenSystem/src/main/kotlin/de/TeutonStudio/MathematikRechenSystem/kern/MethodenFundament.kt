package de.TeutonStudio.MathematikRechenSystem.kern

/** Die eine wiederverwendbare Zielmenge aller Prädikate. */
val WahrheitsMenge: MengenAusdruck = EndlicheMenge(
    setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
)

data class MethodenArgument(
    val parameter: MethodenParameter,
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

/** Die Parameterreihenfolge ist Teil der Semantik und wird nie aus einer Map abgeleitet. */
fun Methode.methodenSignatur(): MethodenSignatur = MethodenSignatur(
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
fun Methode.wendeKanonischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt =
    wendeAn(argumente)

/** Prädikate werden nicht durch Entscheidbarkeit, sondern nur durch ihre Signatur erkannt. */
fun Methode.istPrädikat(): Boolean =
    runCatching { vorschrift is Aussage && zielMenge == WahrheitsMenge }.getOrDefault(false)

/** Funktionen verarbeiten und liefern ausschließlich Zahl-, Vektor-, Matrix- oder Tensorräume. */
fun Methode.istFunktion(): Boolean = runCatching {
    val signatur = methodenSignatur()
    signatur.argumente.all { it.werteVorrat.istFunktionalerRaum() } &&
        signatur.zielMenge.istFunktionalerRaum()
}.getOrDefault(false)

/** Abbildungen nehmen Mengenobjekte entgegen und liefern ein Mengenobjekt. */
fun Methode.istAbbildung(): Boolean = runCatching {
    parameter.isNotEmpty() &&
        parameter.all { it.istMengenArgument() } &&
        vorschrift is MengenAusdruck
}.getOrDefault(false)

fun Methode.aliase(): Set<MethodenAlias> = buildSet {
    if (istFunktion()) add(MethodenAlias.Funktion)
    if (istAbbildung()) add(MethodenAlias.Abbildung)
    if (istPrädikat()) add(MethodenAlias.Prädikat)
}

fun Methode.aliasAnzeige(): String = buildList {
    add("Methode")
    addAll(aliase().map(MethodenAlias::anzeigeName))
}.joinToString(" · ")

private fun MethodenParameter.istMengenArgument(): Boolean = when (this) {
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
    is Tensorraum -> elementMenge.istFunktionalerRaum()
    is Tupelraum -> komponenten.all { it.istFunktionalerRaum() }
    else -> false
}

/** Semantische Eingangsprüfung statt immer weiterer Methoden-Anschlussunterarten. */
fun interface MethodenAnforderung {
    fun prüfe(methode: Methode): String?

    data object Beliebig : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = null
    }

    data class Stelligkeit(val anzahl: Int) : MethodenAnforderung {
        init { require(anzahl >= 0) }
        override fun prüfe(methode: Methode): String? =
            if (methode.parameter.size == anzahl) null
            else "Die Methode '${methode.name}' muss genau $anzahl Argumente besitzen."
    }

    data class ErgebnisArt(val anschlussArt: String) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val passt = when (anschlussArt) {
                "mathematik.objekt" -> true
                "mathematik.zahl" -> methode.vorschrift is ZahlAusdruck
                "mathematik.aussage" -> methode.istPrädikat()
                "mathematik.menge" -> methode.vorschrift is MengenAusdruck
                "mathematik.vektor.spalte" -> methode.vorschrift is SpaltenVektor
                "mathematik.vektor.zeile" -> methode.vorschrift is ZeilenVektor
                "mathematik.matrix" -> methode.vorschrift is Matrix
                "mathematik.tensor" -> methode.vorschrift is Tensor
                else -> (methode.vorschrift as? TypisiertesElement)?.anschlussArt == anschlussArt
            }
            return if (passt) null else
                "Die Methode '${methode.name}' liefert kein Ergebnis der Anschlussart '$anschlussArt'."
        }
    }

    data object Prädikat : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            if (methode.istPrädikat()) null
            else "Die Methode '${methode.name}' erfüllt das Prädikatskriterium nicht."
    }

    data object Endomorphismus : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = runCatching {
            val signatur = methode.methodenSignatur()
            if (signatur.argumente.size == 1 && signatur.argumente.single().werteVorrat == signatur.zielMenge) null
            else "Die Methode '${methode.name}' ist kein Endomorphismus."
        }.getOrElse { it.message ?: "Die Methodensignatur ist unvollständig." }
    }

    data class Alle(val anforderungen: List<MethodenAnforderung>) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            anforderungen.firstNotNullOfOrNull { it.prüfe(methode) }
    }
}
