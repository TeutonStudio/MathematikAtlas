package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class RechnerSchemaTest {
    @Test fun `Schema zeigt echte Werte und meldet fehlende Auswertung`() {
        val knoten = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        assertTrue(besitztRechnerSchema(knoten))
        assertNotNull(rechnerSchemaModell(knoten, null).fehler)
        val matrix = Matrix(listOf(listOf(RationaleZahl.von(7))))
        val ergebnis = KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(matrix)),
            eingänge = mapOf("links" to BedingterWert(matrix)))
        val modell = rechnerSchemaModell(knoten, ergebnis)
        assertNull(modell.fehler)
        assertEquals(listOf("links" to matrix.zuLatex()), modell.eingänge)
        assertEquals(listOf("wert" to matrix.zuLatex()), modell.ausgänge)
        assertEquals("7", schemaKomponenten(matrix, listOf(0, 0)))
        assertNull(schemaKomponenten(matrix, listOf(1, 0)))
        assertNull(schemaKomponenten(matrix, listOf(0)))
    }

    @Test fun `Tensor-Komponenten verwenden echte Achsen statt flacher Ersatzwerte`() {
        val tensor = Tensor(listOf(2, 1, 2), (1L..4L).map { RationaleZahl.von(it) })
        assertEquals("4", schemaKomponenten(tensor, listOf(1, 0, 1)))
        assertNull(schemaKomponenten(tensor, listOf(1, 1, 1)))
    }
    @Test fun `Methodenprodukt verwendet keinen konkreten Falkpfad`() {
        val knoten = MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)
        val x = Variable("x")
        val methode = Methode("f", listOf(x), x, ReelleZahlen)
        val auswertung = KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(methode)),
            eingänge = mapOf("f" to BedingterWert(methode)))
        assertFalse(verwendetFalkSchema(knoten, auswertung))
        assertNull(rechnerSchemaModell(knoten, auswertung).fehler)
    }

    @Test fun `Registryoperatoren haben Schema und unbekannte IDs eine Diagnose`() {
        RechnerOperatorRegistry.alle.forEach { vertrag ->
            val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
            val knoten = basis.copy(art = vertrag.familie.knotenArt,
                parameter = mapOf(vertrag.familie.operatorParameter to vertrag.operatorId))
            assertTrue(besitztRechnerSchema(knoten), vertrag.operatorId)
            val modell = rechnerSchemaModell(knoten, KnotenAuswertungsErgebnis(emptyMap()))
            assertEquals(vertrag.titel, modell.titel)
            assertNull(modell.fehler, vertrag.operatorId)
        }
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        assertTrue(rechnerSchemaModell(basis.copy(parameter = mapOf(RECHNER_OPERATOR_PARAMETER to "unbekannt")),
            KnotenAuswertungsErgebnis(emptyMap())).fehler.orEmpty().contains("Unbekannter"))
    }

    @Test fun `Zahlenformel zeigt gespeicherte Vorschrift und Quadrat keinen falschen Symbolaufruf`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val formel = basis.copy(art = ZAHLENRECHNER_ART, parameter = mapOf(
            ZAHLENRECHNER_OPERATOR to "zahl.formel", ZAHLENRECHNER_FORMEL_LATEX to "x+1"))
        assertEquals("x+1", rechnerSchemaModell(formel, null).definition)
        val quadrat = formel.copy(parameter = mapOf(ZAHLENRECHNER_OPERATOR to "zahl.quadrat"))
        val ergebnis = KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.von(9))),
            eingänge = mapOf("x" to BedingterWert(RationaleZahl.von(3))))
        val einsetzung = assertNotNull(rechnerSchemaModell(quadrat, ergebnis).einsetzung)
        assertFalse(einsetzung.contains("^2"))
        assertTrue(einsetzung.contains("mapsto"))
    }


    @Test fun `Matrixeinsetzung ergaenzt keine aeusseren Klammern`() {
        val k=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val m=Matrix(listOf(listOf(RationaleZahl.Eins)))
        val e=KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(m)),eingänge=mapOf("links" to BedingterWert(m),"rechts" to BedingterWert(m)))
        assertEquals("${m.zuLatex()} + ${m.zuLatex()} = ${m.zuLatex()}",rechnerSchemaModell(k,e).einsetzung)
    }
    @Test fun `Unaere Matrixeinsetzung braucht keine Tupelklammern`() {
        val k=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.MATRIX,"matrix.inverse")
        val m=Matrix(listOf(listOf(RationaleZahl.Eins)))
        val e=KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(m)),eingänge=mapOf("matrix" to BedingterWert(m)))
        assertEquals("${m.zuLatex()} \\mapsto ${m.zuLatex()}",rechnerSchemaModell(k,e).einsetzung)
    }

}
