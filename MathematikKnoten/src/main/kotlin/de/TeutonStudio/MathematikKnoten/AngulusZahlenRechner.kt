package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Letzte Verfeinerung des Zahlenrechners für den semantischen Angulus-Typ.
 * Bestehende nicht-winkelbezogene Operatoren werden unverändert delegiert.
 */
fun MathematikAuswerterRegister.registriereAngulusZahlenRechner() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART))
    registriere(ZAHLENRECHNER_ART) { kontext ->
        val id = kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR]
        val standard = UniversellerZahlenOperator.vonIdOderNull(id)
        val erweitert = ErweiterterZahlenOperator.vonId(id)
        when {
            standard in angulusStandardOperatoren -> werteAngulusStandardAus(kontext, requireNotNull(standard))
            erweitert in angulusErweiterteOperatoren -> werteAngulusErweitertAus(kontext, requireNotNull(erweitert))
            else -> basis.auswerten(kontext)
        }
    }
}

private val angulusStandardOperatoren = setOf(
    UniversellerZahlenOperator.KOMPLEXER_WINKEL,
    UniversellerZahlenOperator.ARCSINUS,
    UniversellerZahlenOperator.ARCCOSINUS,
    UniversellerZahlenOperator.SINUS,
    UniversellerZahlenOperator.COSINUS,
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
)

private val angulusErweiterteOperatoren = setOf(
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.SEKANS,
    ErweiterterZahlenOperator.KOSEKANS,
    ErweiterterZahlenOperator.ARCTANGENS,
)

private fun werteAngulusStandardAus(
    kontext: KnotenAuswertungsKontext,
    operator: UniversellerZahlenOperator,
): KnotenAuswertungsErgebnis = when (operator) {
    UniversellerZahlenOperator.KOMPLEXER_WINKEL ->
        transformiereEinzelEingang(kontext, AngulusMethodenOperation.Argument)
    UniversellerZahlenOperator.ARCSINUS ->
        transformiereEinzelEingang(kontext, AngulusMethodenOperation.ArcSinus)
    UniversellerZahlenOperator.ARCCOSINUS ->
        transformiereEinzelEingang(kontext, AngulusMethodenOperation.ArcCosinus)
    UniversellerZahlenOperator.SINUS ->
        transformiereEinzelEingang(kontext, AngulusMethodenOperation.Sinus)
    UniversellerZahlenOperator.COSINUS ->
        transformiereEinzelEingang(kontext, AngulusMethodenOperation.Cosinus)
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> wertePolarAus(kontext)
    else -> error("Kein Angulus-Standardoperator: ${operator.stabileId}")
}

private fun werteAngulusErweitertAus(
    kontext: KnotenAuswertungsKontext,
    operator: ErweiterterZahlenOperator,
): KnotenAuswertungsErgebnis = transformiereEinzelEingang(
    kontext,
    when (operator) {
        ErweiterterZahlenOperator.TANGENS -> AngulusMethodenOperation.Tangens
        ErweiterterZahlenOperator.COTANGENS -> AngulusMethodenOperation.Cotangens
        ErweiterterZahlenOperator.SEKANS -> AngulusMethodenOperation.Sekans
        ErweiterterZahlenOperator.KOSEKANS -> AngulusMethodenOperation.Kosekans
        ErweiterterZahlenOperator.ARCTANGENS -> AngulusMethodenOperation.ArcTangens
        else -> error("Kein Angulus-Erweiterungsoperator: ${operator.stabileId}")
    },
)

private fun transformiereEinzelEingang(
    kontext: KnotenAuswertungsKontext,
    operation: AngulusMethodenOperation,
): KnotenAuswertungsErgebnis {
    val eingang = kontext.eingänge["a"] ?: error("Der Eingang a fehlt.")
    val objekt = when (val wert = eingang.objekt) {
        is Methode -> AngulusTransformierteMethode(wert, operation)
        else -> operation.wendeAn(wert)
    }
    val methode = objekt as? Methode
    val angulus = objekt as? Angulus
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = eingang.annahmen,
                zielMenge = methode?.let { (it as SignaturtragendeMethode).signatur.zielMenge },
                werteVorrat = when {
                    methode is SignaturtragendeMethode -> methode.signatur.werteVorrat
                    angulus != null -> angulus.raum
                    else -> operation.zielMenge
                },
                reelleVariablen = eingang.reelleVariablen,
                variablenQuellen = eingang.variablenQuellen,
                latexDarstellung = if (methode == null) objekt.zuLatex() else null,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = if (operation in angulusVerbraucherOperationen) {
            listOf("Angulus wird vor der Auswertung zwangsweise in Radian normalisiert.")
        } else emptyList(),
    )
}

private val angulusVerbraucherOperationen: Set<AngulusMethodenOperation> = setOf(
    AngulusMethodenOperation.Sinus,
    AngulusMethodenOperation.Cosinus,
    AngulusMethodenOperation.Tangens,
    AngulusMethodenOperation.Cotangens,
    AngulusMethodenOperation.Sekans,
    AngulusMethodenOperation.Kosekans,
)

private fun wertePolarAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val tupelModus = kontext.knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE] == ZAHLENRECHNER_KOMPLEX_TUPEL
    val (radius, winkel) = if (tupelModus) {
        val tupel = kontext.eingänge["tupel"]?.objekt as? Tupel
            ?: error("Der PolarTupel-Eingang fehlt.")
        require(tupel.koordinatenArt() == TupelKoordinatenArt.POLAR && tupel.elemente.size == 2) {
            "Die komplexe Polarform erwartet einen PolarTupel aus genau (Zahl, Angulus)."
        }
        (tupel.elemente[0] as ZahlAusdruck) to (tupel.elemente[1] as Angulus)
    } else {
        val radius = kontext.eingänge["a"]?.objekt as? ZahlAusdruck
            ?: error("Der Radius muss eine Zahl sein.")
        val winkel = kontext.eingänge["b"]?.objekt as? Angulus
            ?: error("Der Winkel muss ein Angulus sein.")
        radius to winkel
    }
    val objekt = komplexAusPolar(radius, winkel)
    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = annahmen,
                werteVorrat = KomplexeZahlen,
                reelleVariablen = reelleVariablen(kontext.eingänge.values),
                variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf("Der Polarwinkel wird für die komplexe Auswertung zwangsweise in Radian konvertiert."),
    )
}
