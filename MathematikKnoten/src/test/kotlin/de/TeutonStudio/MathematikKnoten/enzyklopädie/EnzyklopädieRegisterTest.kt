package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnzyklopädieRegisterTest {
    @Test
    fun `Standardregister ist vollständig und widerspruchsfrei`() {
        val register = MathematikEnzyklopädie.standard

        assertEquals(emptyList(), register.validierungsFehler())
        assertTrue(register.alle.isNotEmpty())
        assertNotNull(register.finde("konzept.zahlenrechner"))
        assertNotNull(register.finde("konzept.tensorrechner"))
        assertTrue(register.fürKnotenArt(ZAHLENRECHNER_ART).isNotEmpty())
        assertTrue(register.fürKnotenArt(TensorRechner.KNOTEN_ART).isNotEmpty())
    }

    @Test
    fun `jeder ausführbare Rechneroperator besitzt genau einen Wissenseintrag`() {
        val register = MathematikEnzyklopädie.standard

        UniversellerZahlenOperator.entries.forEach { operator ->
            val eintrag = assertNotNull(register.finde(WissensId("operator.${operator.stabileId}")))
            assertEquals(operator.titel, eintrag.titel)
            assertTrue(ZAHLENRECHNER_ART in eintrag.knotenArten)
        }
        TensorRechnerOperator.entries.forEach { operator ->
            val eintrag = assertNotNull(register.finde(WissensId("operator.${operator.stabileId}")))
            assertTrue(TensorRechner.KNOTEN_ART in eintrag.knotenArten)
        }
    }

    @Test
    fun `Suche berücksichtigt Titel Aliasse Fachpfade und Knotenvorlagen`() {
        val register = EnzyklopädieRegister.ausVorlagen(alleMathematikDefinitionsVorlagen())

        assertTrue(register.suche("CAS Zahlenoperator").any { it.id.wert == "konzept.zahlenrechner" })
        assertTrue(register.suche("lineare-algebra tensoren").any { it.id.wert == "konzept.tensorrechner" })
        assertTrue(register.suche("offene Menge").any { it.id.wert == "geplant.topologie.offene-menge" })
        assertEquals(null, register.finde("   "))
    }

    @Test
    fun `Rechnerkatalog deckt sämtliche Zahlen- und Tensoroperatoren ab`() {
        assertEquals(emptyList(), RechnerFamilienKatalog.validierungsFehler())
        assertEquals(
            UniversellerZahlenOperator.entries.map { it.stabileId }.toSet(),
            RechnerFamilienKatalog.zahlenOperatoren.map { it.stabileId }.toSet(),
        )
        assertEquals(
            TensorRechnerOperator.entries.map { it.stabileId }.toSet(),
            RechnerFamilienKatalog.tensorOperatoren.map { it.stabileId }.toSet(),
        )
    }
}
