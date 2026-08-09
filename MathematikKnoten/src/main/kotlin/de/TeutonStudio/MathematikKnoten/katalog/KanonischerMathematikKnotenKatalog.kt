package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerMigration
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRelationsMigration
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

    private val historischeMengenEinzelknoten: Set<String> =
        MengenRechnerMigration.alteKnotenArten.keys + MengenRelationsMigration.alteKnotenArten.keys

    fun alle(): List<KnotenVorlage> {
        val basis = de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()
        val bereinigt = basis.filterNot { vorlage ->
            vorlage.art in historischeOrientierungsDuplikate ||
                vorlage.art in historischeMengenEinzelknoten ||
                vorlage.art == MULTINOMVEKTOR_ART ||
                (
                    vorlage.art == ZAHLENRECHNER_ART &&
                        vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                        UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
                    )
        }

        return bereinigt +
            VektorKonstruktorVorlagen.standard +
            VektorOrientierungsVorlagen.alle +
            MultinomVektorKnotenVorlagen.standard +
            MengenRechnerKnotenVorlagen.standard +
            MengenRelationsKnotenVorlagen.standard +
            MengenMassKnotenVorlagen.alle
    }
}
