package de.TeutonStudio.MathematikRechenSystem.kern

/** Punktweise Operationen, die Angulus-Werte erzeugen, verbrauchen oder konvertieren. */
sealed interface AngulusMethodenOperation {
    val name: String
    val zielMenge: MengenAusdruck
    fun wendeAn(wert: MathematischesObjekt): MathematischesObjekt

    data object Argument : AngulusMethodenOperation {
        override val name = "Angulus"
        override val zielMenge = AngulusRaum(AngulusEinheit.RADIAN)
        override fun wendeAn(wert: MathematischesObjekt): Angulus {
            val zahl = wert as? ZahlAusdruck ?: error("Angulus erwartet einen Zahlenwert.")
            val argument = (zahl as? KomplexeZahl)?.let {
                de.TeutonStudio.MathematikRechenSystem.kern.Argument(it)
            } ?: symbolischerZahlterm("arg-${zahl.zuLatex()}", "\\arg\\left(${zahl.zuLatex()}\\right)")
            return Angulus(argument, AngulusEinheit.RADIAN)
        }
    }

    data object ArcSinus : AngulusMethodenOperation {
        override val name = "arcsin"
        override val zielMenge = AngulusRaum(AngulusEinheit.RADIAN)
        override fun wendeAn(wert: MathematischesObjekt): Angulus = Angulus(
            de.TeutonStudio.MathematikRechenSystem.kern.ArcSinus(
                wert as? ZahlAusdruck ?: error("Arcus Sinus erwartet eine Zahl."),
            ),
        )
    }

    data object ArcCosinus : AngulusMethodenOperation {
        override val name = "arccos"
        override val zielMenge = AngulusRaum(AngulusEinheit.RADIAN)
        override fun wendeAn(wert: MathematischesObjekt): Angulus = Angulus(
            de.TeutonStudio.MathematikRechenSystem.kern.ArcCosinus(
                wert as? ZahlAusdruck ?: error("Arcus Cosinus erwartet eine Zahl."),
            ),
        )
    }

    data object ArcTangens : AngulusMethodenOperation {
        override val name = "arctan"
        override val zielMenge = AngulusRaum(AngulusEinheit.RADIAN)
        override fun wendeAn(wert: MathematischesObjekt): Angulus {
            val zahl = wert as? ZahlAusdruck ?: error("Arcus Tangens erwartet eine Zahl.")
            return Angulus(symbolischerZahlterm("arctan-${zahl.zuLatex()}", "\\arctan\\left(${zahl.zuLatex()}\\right)"))
        }
    }

    data object Sinus : AngulusMethodenOperation {
        override val name = "sin"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck =
            de.TeutonStudio.MathematikRechenSystem.kern.Sinus(wert.alsRadian().wert)
    }

    data object Cosinus : AngulusMethodenOperation {
        override val name = "cos"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck =
            de.TeutonStudio.MathematikRechenSystem.kern.Cosinus(wert.alsRadian().wert)
    }

    data object Tangens : AngulusMethodenOperation {
        override val name = "tan"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck {
            val x = wert.alsRadian().wert
            return symbolischerZahlterm("tan-${x.zuLatex()}", "\\tan\\left(${x.zuLatex()}\\right)")
        }
    }

    data object Cotangens : AngulusMethodenOperation {
        override val name = "cot"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck {
            val x = wert.alsRadian().wert
            return symbolischerZahlterm("cot-${x.zuLatex()}", "\\cot\\left(${x.zuLatex()}\\right)")
        }
    }

    data object Sekans : AngulusMethodenOperation {
        override val name = "sec"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck {
            val x = wert.alsRadian().wert
            return symbolischerZahlterm("sec-${x.zuLatex()}", "\\sec\\left(${x.zuLatex()}\\right)")
        }
    }

    data object Kosekans : AngulusMethodenOperation {
        override val name = "csc"
        override val zielMenge = ReelleZahlen
        override fun wendeAn(wert: MathematischesObjekt): ZahlAusdruck {
            val x = wert.alsRadian().wert
            return symbolischerZahlterm("csc-${x.zuLatex()}", "\\csc\\left(${x.zuLatex()}\\right)")
        }
    }

    data class Konvertiere(val ziel: AngulusEinheit) : AngulusMethodenOperation {
        override val name = "Angulus→${ziel.kurzname}"
        override val zielMenge = AngulusRaum(ziel)
        override fun wendeAn(wert: MathematischesObjekt): Angulus =
            (wert as? Angulus ?: error("Die Einheitenumwandlung erwartet einen Angulus.")).inEinheit(ziel)
    }
}

private fun MathematischesObjekt.alsRadian(): Angulus =
    (this as? Angulus ?: error("Die trigonometrische Methode erwartet einen Angulus."))
        .inEinheit(AngulusEinheit.RADIAN)

/**
 * Leichte Methoden-Capability statt Kopie der symbolischen Methodenimplementierung.
 * Die Basismethode bestimmt Argumente und Definitionsbereich, die Operation nur den
 * punktweisen Wert und den Zielraum.
 */
data class AngulusTransformierteMethode(
    val basis: Methode,
    val operation: AngulusMethodenOperation,
) : MathematischAuswertbareMethode {
    private val basisSignatur: MethodenSignatur
        get() = (basis as? SignaturtragendeMethode)?.signatur
            ?: error("Die Methode '${basis.name}' besitzt keine mathematische Signatur.")

    override val name: String
        get() = "${operation.name}(${basis.name})"

    override val signatur: MethodenSignatur
        get() = basisSignatur.copy(zielMenge = operation.zielMenge)

    override fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val auswertbar = basis as? MathematischAuswertbareMethode
            ?: error("Die Methode '${basis.name}' ist nicht mathematisch auswertbar.")
        return operation.wendeAn(auswertbar.wendeMathematischAn(argumente))
    }

    override fun zuLatex(): String = "$name:${signatur.werteVorrat.zuLatex()}\\to${signatur.zielMenge.zuLatex()}"
}

/** Punktweise Polar-Konstruktion aus skalarem oder methodischem Radius/Winkel. */
data class PolarKomplexMethode(
    val radiusQuelle: MathematischesObjekt,
    val winkelQuelle: MathematischesObjekt,
) : MathematischAuswertbareMethode {
    private val methoden = listOf(radiusQuelle, winkelQuelle).filterIsInstance<Methode>()
    private val basis: SignaturtragendeMethode = methoden.firstOrNull() as? SignaturtragendeMethode
        ?: error("Eine PolarKomplexMethode benötigt mindestens eine signaturtragende Methode.")

    init {
        require(methoden.all { it is MathematischAuswertbareMethode && it is SignaturtragendeMethode }) {
            "Polar verknüpfte Methoden müssen mathematisch auswertbar sein."
        }
        val basisSignatur = basis.signatur
        require(methoden.drop(1).all { methode ->
            val signatur = (methode as SignaturtragendeMethode).signatur
            signatur.argumente.map { it.parameter.name } == basisSignatur.argumente.map { it.parameter.name } &&
                signatur.argumente.map { it.werteVorrat } == basisSignatur.argumente.map { it.werteVorrat }
        }) {
            "Radius- und Winkelmethode der Polarform benötigen dieselbe Argument-Signatur."
        }
    }

    override val name: String
        get() = "polar(${quellenName(radiusQuelle)},${quellenName(winkelQuelle)})"

    override val signatur: MethodenSignatur
        get() = basis.signatur.copy(zielMenge = KomplexeZahlen)

    override fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val radius = werteQuelleAus(radiusQuelle, argumente) as? ZahlAusdruck
            ?: error("Die Polar-Radiusquelle muss eine Zahl liefern.")
        val winkel = werteQuelleAus(winkelQuelle, argumente) as? Angulus
            ?: error("Die Polar-Winkelquelle muss einen Angulus liefern.")
        return komplexAusPolar(radius, winkel)
    }

    override fun zuLatex(): String = "$name:${signatur.werteVorrat.zuLatex()}\\to${KomplexeZahlen.zuLatex()}"
}

private fun werteQuelleAus(
    quelle: MathematischesObjekt,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt = when (quelle) {
    is MathematischAuswertbareMethode -> quelle.wendeMathematischAn(argumente)
    else -> quelle
}

private fun quellenName(quelle: MathematischesObjekt): String =
    (quelle as? Methode)?.name ?: quelle.zuLatex()

fun Methode.erzwingeAngulusEinheit(ziel: AngulusEinheit): Methode =
    AngulusTransformierteMethode(this, AngulusMethodenOperation.Konvertiere(ziel))
