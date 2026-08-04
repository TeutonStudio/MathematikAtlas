package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensVerfügbarkeit
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KonzeptKnotenRegisterTest {
    @Test
    fun `jede Definitionsvorlage wird genau einmal registriert`() {
        val vorlagen = alleMathematikDefinitionsVorlagen()
        val einträge = KonzeptKnotenRegister.erstelle(vorlagen)

        assertEquals(emptyList(), KonzeptKnotenRegister.validierungsFehler(einträge, vorlagen))
        assertEquals(
            vorlagen.map { it.stabileVariantenId() }.toSet(),
            einträge.flatMap { it.varianten }.toSet(),
        )
    }

    @Test
    fun `konsolidierte Rechner besitzen je einen Eintrag mit allen Varianten`() {
        val vorlagen = alleMathematikDefinitionsVorlagen()
        val einträge = KonzeptKnotenRegister.erstelle(vorlagen)

        val zahlen = assertNotNull(einträge.singleOrNull { it.id == de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId("konzept.zahlenrechner") })
        assertEquals(
            vorlagen.filter { it.art == ZAHLENRECHNER_ART }.map { it.stabileVariantenId() }.toSet(),
            zahlen.varianten,
        )
        val tensor = assertNotNull(einträge.singleOrNull { it.id == de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId("konzept.tensorrechner") })
        assertEquals(
            vorlagen.filter { it.art == TensorRechner.KNOTEN_ART }.map { it.stabileVariantenId() }.toSet(),
            tensor.varianten,
        )
    }

    @Test
    fun `historische Bibliotheks-IDs bleiben als Aliasse auflösbar`() {
        val vorlagen = alleMathematikDefinitionsVorlagen()
        val zahlenVorlagen = vorlagen.filter { it.art == ZAHLENRECHNER_ART }
        val zahlen = KonzeptKnotenRegister.erstelle(vorlagen).single { it.id == de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId("konzept.zahlenrechner") }

        assertTrue(zahlenVorlagen.map { it.stabileKonzeptId() }.all { it in zahlen.aliase })
    }

    @Test
    fun `geplante Einträge bleiben sichtbar aber ohne Knotenvorlagen`() {
        val geplante = KonzeptKnotenRegister.erstelle(alleMathematikDefinitionsVorlagen())
            .filter { it.verfügbarkeit == WissensVerfügbarkeit.Geplant }

        assertTrue(geplante.isNotEmpty())
        assertTrue(geplante.all { it.knotenVorlagen.isEmpty() })
    }
}
