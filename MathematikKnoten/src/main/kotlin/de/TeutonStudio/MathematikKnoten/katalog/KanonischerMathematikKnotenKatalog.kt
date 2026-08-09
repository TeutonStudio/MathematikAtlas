package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Einzige fachliche Quelle für den sichtbaren mathematischen Erstellen-Katalog.
 *
 * Der lade-kompatible Basiskatalog darf historische Vorlagen enthalten. Diese
 * Fassade entscheidet zentral und plattformunabhängig, welche kanonischen
 * Varianten Android und Desktop tatsächlich zum Erzeugen anbieten.
 */
object KanonischerMathematikKnotenKatalog {
    private val historischeOrientierungsDuplikate = setOf(
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

    fun alle(): List<KnotenVorlage> {
        val basis = de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()
            .map(::kanonisierePraedikatVorlage)
        val bereinigt = basis.filterNot { vorlage ->
            vorlage.art in historischeOrientierungsDuplikate ||
                vorlage.art == MULTINOMVEKTOR_ART ||
                (
                    vorlage.art == ZAHLENRECHNER_ART &&
                        vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                        UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
                    )
        }

        return bereinigt +
            TupelVariableKnotenVorlagen.standard +
            VektorKonstruktorVorlagen.standard +
            VektorOrientierungsVorlagen.alle +
            MultinomVektorKnotenVorlagen.standard
    }

    private fun kanonisierePraedikatVorlage(vorlage: KnotenVorlage): KnotenVorlage =
        if (
            vorlage.art == "mathematik.termZuMethode" &&
            vorlage.standardParameter["name"] == "P" &&
            vorlage.name == "Aussage zu Methode"
        ) {
            vorlage.copy(
                name = "Aussage zu Prädikat",
                beschreibung = "Erzeugt aus einer Aussage ein typisiertes Prädikat.",
            )
        } else vorlage
}
