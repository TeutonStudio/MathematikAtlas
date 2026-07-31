package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrdnungsrelationDefinitionsKarteTest {
    @Test
    fun `vier Dialogvorlagen erzeugen eine gemeinsame parametrierte Knotenart`() {
        val vorlagen = listOf(
            MathematikKnotenVorlagen.Kleiner,
            MathematikKnotenVorlagen.KleinerGleich,
            MathematikKnotenVorlagen.Größer,
            MathematikKnotenVorlagen.GrößerGleich,
        )
        assertEquals(setOf(MathematikKnotenVorlagen.ORDNUNGSRELATION_ART), vorlagen.map { it.art }.toSet())
        assertEquals(
            listOf("kleiner", "kleinerGleich", "größer", "größerGleich"),
            vorlagen.map { it.standardParameter.getValue("relation") },
        )
    }

    @Test
    fun `Knotendefinition folgt der ausgewählten Relation`() {
        val kleiner = MathematikKnotenVorlagen.Kleiner.erzeuge(GraphPunkt.Zero)
        val größerGleich = kleiner.copy(parameter = kleiner.parameter + ("relation" to "größerGleich"))

        val kleinerKonzept = assertNotNull(TestDefinitionsKarten.fürKnoten(kleiner))
        val größerKonzept = assertNotNull(TestDefinitionsKarten.fürKnoten(größerGleich))
        val kleinerKarte = kleinerKonzept.reiter.single().karte
        val größerKarte = größerKonzept.reiter.single().karte

        assertNotEquals(kleinerKonzept.id, größerKonzept.id)
        assertTrue(kleinerKarte.id.wert.contains("kleiner"))
        assertTrue(größerKarte.id.wert.contains("größerGleich"))
        assertEquals(1, kleinerKarte.knoten.count { it.art == MathematikKnotenVorlagen.Differenz.art })
        assertEquals(0, größerKarte.knoten.count { it.art == MathematikKnotenVorlagen.Differenz.art })
        assertFalse(kleinerKarte.knoten.any { it.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART })
        assertFalse(größerKarte.knoten.any { it.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART })
    }
}
