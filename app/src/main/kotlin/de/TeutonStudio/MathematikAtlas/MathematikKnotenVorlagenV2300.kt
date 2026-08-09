package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * App-seitiger Katalogadapter für v2.30.0. Historische Typen bleiben ladbar,
 * verschwinden aber aus der Erzeugung, sobald ein kanonischer parametrierter Knoten existiert.
 */
internal fun alleMathematikKnotenVorlagen(): List<KnotenVorlage> {
    val basis = de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()
    val historischeOrientierungsDuplikate = setOf(
        "mathematik.vektor",
        "mathematik.zeilenVektor",
        "mathematik.tupelZuSpalte",
        "mathematik.tupelZuZeile",
        "mathematik.einheitsSpalte",
        "mathematik.einheitsZeile",
        "mathematik.vektorRadiusSpalte",
        "mathematik.vektorRadiusZeile",
        "mathematik.kreuzproduktSpalte",
        "mathematik.kreuzproduktZeile",
        "mathematik.spaltenMethodeDifferentieren",
        "mathematik.zeilenMethodeDifferentieren",
        "mathematik.spaltenMethodeIntegrieren",
        "mathematik.zeilenMethodeIntegrieren",
        "mathematik.skalarprodukt",
        "mathematik.skalarproduktZeile",
    )
    val bereinigt = basis.filterNot { vorlage ->
        vorlage.art in historischeOrientierungsDuplikate ||
            (
                vorlage.art == ZAHLENRECHNER_ART &&
                    vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
                )
    }
    return bereinigt +
        VektorKonstruktorV2300Vorlagen.standard +
        VektorOrientierungsV2300Vorlagen.alle
}
