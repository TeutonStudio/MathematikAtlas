package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRechner
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerMigration
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRelationRechner
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRelationsMigration
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KanonischerMathematikKnotenKatalogTest {
    @Test
    fun `sichtbarer Katalog besitzt keine identischen Vorlagen doppelt`() {
        val vorlagen = KanonischerMathematikKnotenKatalog.alle()
        val signaturen = vorlagen.map { vorlage ->
            Triple(vorlage.art, vorlage.name, vorlage.standardParameter.toSortedMap())
        }

        assertEquals(signaturen.size, signaturen.distinct().size)
    }

    @Test
    fun `historischer komplexer Radius wird nicht mehr separat angeboten`() {
        val radius = KanonischerMathematikKnotenKatalog.alle().filter { vorlage ->
            vorlage.art == ZAHLENRECHNER_ART &&
                vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
        }

        assertTrue(radius.isEmpty())
    }

    @Test
    fun `kanonischer Vektorkonstruktor und Multinomvektor sind sichtbar`() {
        val vorlagen = KanonischerMathematikKnotenKatalog.alle()

        assertTrue(vorlagen.any { it.art == VEKTOR_KONSTRUKTOR_ART })
        assertTrue(vorlagen.any { it.art == MULTINOMVEKTOR_ART })
        assertFalse(vorlagen.any { it.name.isBlank() || it.kategorie.isBlank() })
    }

    @Test
    fun `mengenoperatoren sind konsolidiert und potenzmenge bleibt eigenstaendig sichtbar`() {
        val kanonischeArten = KanonischerMathematikKnotenKatalog.alle().map { it.art }.toSet()
        val mengenraumArten = MengenraumKnotenVorlagen.alle.map { it.art }.toSet()

        assertTrue(MengenRechner.KNOTEN_ART in kanonischeArten)
        assertTrue(MengenRelationRechner.KNOTEN_ART in kanonischeArten)
        assertTrue("mathematik.potenzmenge" in mengenraumArten)
        assertFalse("mathematik.potenzmenge" in MengenRechnerMigration.alteKnotenArten)
        assertFalse("mathematik.symmetrischeDifferenz" in mengenraumArten)
        MengenRechnerMigration.alteKnotenArten.keys.forEach { alt ->
            assertFalse(alt in kanonischeArten, "Historischer Mengenoperator $alt darf nicht mehr separat sichtbar sein.")
        }
        MengenRelationsMigration.alteKnotenArten.keys.forEach { alt ->
            assertFalse(alt in kanonischeArten, "Historische Mengenrelation $alt darf nicht mehr separat sichtbar sein.")
        }
    }
}
