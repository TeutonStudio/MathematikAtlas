package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class MatrixProduktKonzeptResolverTest {
    @Test fun `kanonisches Matrixprodukt hat im tatsaechlichen Definitionsresolver den Falk Reiter`() {
        val knoten=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.MATRIX,"matrix.produkt")
        // Der registrierte Schlüssel ist kleingeschrieben; derselbe Prädikatpfad steuert Desktop.
        assertEquals("mathematik.matrixrechner",knoten.art)
        assertTrue(istMatrixProduktKnoten(knoten))
        val konzept=assertNotNull(dynamischesKonzeptFürKnoten(knoten))
        assertEquals(1,konzept.reiter.count { it.rolle==KonzeptReiterRolle.Definition })
        val falk=konzept.reiter.single { it.darstellung==KonzeptReiterDarstellung.FalkSchema }
        assertEquals(MATRIXPRODUKT_FALK_REITER_ID,falk.id)
        assertTrue(falk.karte.knoten.isEmpty()) // Keine stillen Beispiele anstelle echter Eingabewerte.
    }
    @Test fun `Sichtbarkeit verwendet registrierte Familie und gespeicherten Operatorschluessel`() {
        assertTrue(istMatrixProduktKnoten(MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)))
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val addition=konfiguriereStrukturRechner(basis,StrukturRechnerKnotenFamilie.MATRIX,"matrix.addition")
        assertFalse(istMatrixProduktKnoten(addition))
        assertTrue(assertNotNull(dynamischesKonzeptFürKnoten(addition)).reiter.none { it.darstellung==KonzeptReiterDarstellung.FalkSchema })
    }
}
