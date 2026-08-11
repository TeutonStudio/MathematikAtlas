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
            standard != null && standard in angulusStandardOperatoren -> werteAngulusStandardAus(kontext, standard)
            erweitert != null && erweitert in angulusErweiterteOperatoren -> werteAngulusErweitertAus(kontext, erweitert)
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
    val methode = objekt as? SignaturtragendeMethode
    val angulus = objekt as? Angulus
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = eingang.annahmen,
                zielMenge = methode?.signatur?.zielMenge,
                werteVorrat = when {
                    methode != null -> methode.signatur.werteVorrat
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
    if (tupelModus) return wertePolarTupelAus(kontext)

    val radiusQuelle = kontext.eingänge["a"]?.objekt ?: error("Der Radius fehlt.")
    val winkelQuelle = kontext.eingänge["b"]?.objekt ?: error("Der Winkel fehlt.")
    val objekt: MathematischesObjekt = if (radiusQuelle is Methode || winkelQuelle is Methode) {
        PolarKomplexMethode(radiusQuelle, winkelQuelle)
    } else {
        val radius = radiusQuelle as? ZahlAusdruck ?: error("Der Radius muss eine Zahl sein.")
        val winkel = winkelQuelle as? Angulus ?: error("Der Winkel muss ein Angulus sein.")
        komplexAusPolar(radius, winkel)
    }
    val methode = objekt as? SignaturtragendeMethode
    val quellWerte = kontext.eingänge.values
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = quellWerte.flatMap { it.annahmen }.toSet(),
                zielMenge = methode?.signatur?.zielMenge,
                werteVorrat = methode?.signatur?.werteVorrat ?: KomplexeZahlen,
                reelleVariablen = reelleVariablen(quellWerte),
                variablenQuellen = quellWerte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf("Der Polarwinkel wird für die komplexe Auswertung zwangsweise in Radian konvertiert."),
    )
}

private fun wertePolarTupelAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val tupelWert = kontext.eingänge["tupel"] ?: error("Der PolarTupel-Eingang fehlt.")
    val tupel = tupelWert.objekt as? Tupel ?: error("Der PolarTupel-Eingang erwartet ein Tupel.")
    require(tupel.koordinatenArt() == TupelKoordinatenArt.POLAR && tupel.elemente.size == 2) {
        "Die komplexe Polarform erwartet einen PolarTupel aus genau (Zahl, Angulus)."
    }
    val objekt = komplexAusPolar(tupel.elemente[0] as ZahlAusdruck, tupel.elemente[1] as Angulus)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = tupelWert.annahmen,
                werteVorrat = KomplexeZahlen,
                reelleVariablen = tupelWert.reelleVariablen,
                variablenQuellen = tupelWert.variablenQuellen,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf("Der Polarwinkel wird für die komplexe Auswertung zwangsweise in Radian konvertiert."),
    )
}
