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
    /**
     * Optionaler gemeinsamer Definitionsbereich der vollständigen Argumentbelegung.
     *
     * Er wird benötigt, wenn der tatsächliche Bereich nicht als kartesisches Produkt
     * der einzelnen Parameter-Wertevorräte darstellbar ist, beispielsweise bei einer
     * Restriktion auf die Diagonale in R².
     */
    val effektiverWerteVorrat: MengenAusdruck? = null,
) {
    /**
     * Der Wertevorrat besteht standardmäßig aus geordneten Argumenttupeln. Auch ein
     * einzelnes Argument bleibt dadurch strukturell ein Einertupel.
     * Nullstellige Methoden verwenden gemäß Atlas-Konvention die leere Menge.
     * Ein expliziter effektiver Gesamtbereich hat Vorrang vor dieser Ableitung.
     */
    val werteVorrat: MengenAusdruck
        get() = effektiverWerteVorrat
            ?: if (argumente.isEmpty()) LeereMenge else Tupelraum(argumente.map { it.werteVorrat })
}

/** Fachliche, ausschließlich abgeleitete Nutzerbegriffe unterhalb von Methode. */
enum class MethodenAlias(val anzeigeName: String) {
    Funktion("Funktion"),
    Abbildung("Abbildung"),
    Prädikat("Prädikat"),
}

/**
 * Anzahl der geordneten Argumentplätze. Sie ist ausdrücklich kein Dimensionsbegriff.
 *
 * Bei der klassischen mathematischen Implementierung ist die Stelligkeit bereits aus
 * der Parameterliste bekannt und bleibt deshalb auch ohne bekannte Wertevorräte
 * auslesbar. Andere Methoden dürfen sie über eine vollständige Signatur bereitstellen.
 */
val Methode.argumentAnzahl: Int
    get() = when (this) {
        is MathematischeMethode -> parameter.size
        is SignaturtragendeMethode -> signatur.argumente.size
        else -> error("Die Methode '$name' stellt noch keine mathematische Signatur bereit.")
    }

/**
 * Gemeinsame Signaturgrenze. Direkte Feldrekonstruktion bleibt ausschließlich Sache
 * der konkreten Implementierung und wird nicht mehr von generischem Methodencode dupliziert.
 */
fun Methode.methodenSignatur(): MethodenSignatur =
    (this as? SignaturtragendeMethode)?.signatur
        ?: error("Die Methode '$name' stellt keine mathematische Signatur bereit.")

/**
 * Wendet die Methode vollständig an und vereinheitlicht historische
 * Mehrfachausgaben zu genau einem Ergebnisobjekt.
 */
fun Methode.wendeKanonischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val auswertbar = this as? MathematischAuswertbareMethode
        ?: error("Die Methode '$name' besitzt keine mathematische Auswertungs-Capability.")
    return auswertbar.wendeMathematischAn(argumente)
}

/** Prädikate werden nicht durch Entscheidbarkeit, sondern nur durch ihre mathematische Semantik erkannt. */
fun Methode.istPrädikat(): Boolean {
    val mathematisch = this as? MathematischeMethode ?: return false
    return mathematisch.vorschrift is Aussage && mathematisch.zielMenge == WahrheitsMenge
}

/** Funktionen verarbeiten und liefern ausschließlich Zahl-, Vektor-, Matrix- oder Tensorräume. */
fun Methode.istFunktion(): Boolean {
    val signatur = (this as? SignaturtragendeMethode)?.signatur ?: return false
    return signatur.argumente.all { it.werteVorrat.istFunktionalerRaum() } &&
        signatur.zielMenge.istFunktionalerRaum()
}

/** Abbildungen nehmen Mengenobjekte entgegen und liefern ein Mengenobjekt. */
fun Methode.istAbbildung(): Boolean {
    val mathematisch = this as? MathematischeMethode ?: return false
    return mathematisch.parameter.isNotEmpty() &&
        mathematisch.parameter.all { it.istMengenArgument() } &&
        mathematisch.vorschrift is MengenAusdruck
}

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
    is Vektorraum -> skalarMenge.istFunktionalerRaum()
    is Matrizenraum -> skalarMenge.istFunktionalerRaum()
    is Tensorraum -> elementMenge.istFunktionalerRaum()
    is Tupelraum -> komponenten.all { it.istFunktionalerRaum() }
    else -> istZahlenmenge()
}

/**
 * Erkennt ausschließlich strukturell belegte Teilmengen der Hamilton-Quaternionen.
 *
 * Unbekannte benannte Mengen werden absichtlich nicht anhand ihres sichtbaren Namens
 * als Zahlenmenge geraten. Bei einem Schnitt genügt dagegen ein numerischer Faktor:
 * Der gesamte Schnitt ist bereits Teilmenge dieses Faktors.
 */
fun MengenAusdruck.istZahlenmenge(): Boolean = when (this) {
    LeereMenge -> true
    is BeschraenkteZahlmenge -> FundamentaleZahlbereiche.istTeilbereich(
        traeger,
        FundamentalerZahlbereich.QUATERNION,
    )
    is ReellesIntervall -> true
    is EndlicheMenge -> elemente.all { it is ZahlAusdruck }
    is DefinierteMenge ->
        variablen.size == 1 && variablen.single().grundMenge.istZahlenmenge()
    is GefilterteMenge -> menge.istZahlenmenge()
    is Vereinigung -> mengen.isNotEmpty() && mengen.all { it.istZahlenmenge() }
    is Schnitt -> mengen.any { it.istZahlenmenge() }
    is MengenDifferenz -> links.istZahlenmenge()
    else -> fundamentalerZahlbereichOderNull()?.let { bereich ->
        FundamentaleZahlbereiche.istTeilbereich(
            bereich,
            FundamentalerZahlbereich.QUATERNION,
        )
    } == true
}

/** Semantische Eingangsprüfung statt immer weiterer Methoden-Anschlussunterarten. */
fun interface MethodenAnforderung {
    fun prüfe(methode: Methode): String?

    data object Beliebig : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = null
    }

    data class Stelligkeit(val anzahl: Int) : MethodenAnforderung {
        init { require(anzahl >= 0) }
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? SignaturtragendeMethode)?.signatur
                ?: return "Die Methode '${methode.name}' besitzt keine mathematische Signatur."
            return if (signatur.argumente.size == anzahl) null
            else "Die Methode '${methode.name}' muss genau $anzahl Argumente besitzen."
        }
    }

    data class ErgebnisArt(val anschlussArt: String) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val mathematisch = methode as? MathematischeMethode
                ?: return "Die Methode '${methode.name}' besitzt keine symbolische mathematische Ausgabe."
            val passt = when (anschlussArt) {
                "mathematik.objekt" -> true
                "mathematik.zahl" -> mathematisch.vorschrift is ZahlAusdruck
                "mathematik.aussage" -> mathematisch.istPrädikat()
                "mathematik.menge" -> mathematisch.vorschrift is MengenAusdruck
                "mathematik.vektor.spalte" -> mathematisch.vorschrift is SpaltenVektor
                "mathematik.vektor.zeile" -> mathematisch.vorschrift is ZeilenVektor
                "mathematik.matrix" -> mathematisch.vorschrift is Matrix
                "mathematik.tensor" -> mathematisch.vorschrift is Tensor
                else -> (mathematisch.vorschrift as? TypisiertesElement)?.anschlussArt == anschlussArt
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

    /** Endlichstellige Methode mit ausschließlich numerischen Argument- und Zielräumen. */
    data object Zahlenfunktion : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = runCatching {
            val signatur = methode.methodenSignatur()
            val nichtNumerischesArgument = signatur.argumente.withIndex().firstOrNull { (_, argument) ->
                !argument.werteVorrat.istZahlenmenge()
            }
            when {
                nichtNumerischesArgument != null -> {
                    val (index, argument) = nichtNumerischesArgument
                    "Das ${index + 1}. Argument '${argument.parameter.name}' der Methode " +
                        "'${methode.name}' besitzt mit ${argument.werteVorrat.zuLatex()} " +
                        "keinen nachgewiesenen Zahlenraum innerhalb von \\mathbb H."
                }
                !signatur.zielMenge.istZahlenmenge() ->
                    "Die Zielmenge ${signatur.zielMenge.zuLatex()} der Methode " +
                        "'${methode.name}' ist keine nachgewiesene Teilmenge von \\mathbb H."
                else -> null
            }
        }.getOrElse { fehler ->
            fehler.message ?: "Die Methodensignatur ist unvollständig."
        }
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
