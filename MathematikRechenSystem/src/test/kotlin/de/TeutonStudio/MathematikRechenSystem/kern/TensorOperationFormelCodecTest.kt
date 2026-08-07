package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TensorOperationFormelCodecTest {
    @Test
    fun `Tensoroperation durchlaeuft Formelgraph mit Rollen Achsen und Parametern`() {
        val tensor = Tensor(
            dimensionen = listOf(2, 2),
            werte = listOf(
                RationaleZahl.Eins,
                RationaleZahl.Null,
                RationaleZahl.Null,
                RationaleZahl.Eins,
            ),
        )
        val original = TensorOperation(
            operationId = TensorOperationId("tensor.achsenschnitt"),
            operanden = mapOf(TensorHandleRolle("tensor") to tensor),
            achsen = TensorAchsenSpezifikation.Tupel(listOf(-1)),
            parameter = mapOf("index" to RationaleZahl.von(2)),
            unterstuetzungsStatus = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT,
        )
        val formel = TensorOperationFormelCodec.zuFormel(original)
        val graph = FormelZuGraph.konvertiere(formel)
        val literale = formel.argumente
            .map(FormelArgument::ausdruck)
            .filterIsInstance<FormelAusdruck.Literal>()
            .associate { it.id to it.wert }

        val rueckweg = assertIs<GraphZuFormelErgebnis.Erfolg>(
            GraphZuFormel.konvertiere(graph, literale),
        )
        val rekonstruiert = TensorOperationFormelCodec.ausFormel(
            assertIs<FormelAusdruck.Operation>(rueckweg.wurzel),
        )

        assertEquals(original, rekonstruiert)
        assertEquals(
            listOf("tensor", "__tensor.achsen.tupel", "__tensor.parameter.index"),
            formel.argumente.sortedBy { it.position }.map { it.rollenId },
        )
    }

    @Test
    fun `dynamische Achsenrollen bleiben im Formelgraph stabil`() {
        val tensor = Tensor(listOf(2, 2), List(4) { RationaleZahl.von(it.toLong()) })
        val original = TensorOperation(
            operationId = TensorOperationId("tensor.kontraktion"),
            operanden = mapOf(TensorHandleRolle("tensor") to tensor),
            achsen = TensorAchsenSpezifikation.Dynamisch(
                linkedMapOf(
                    TensorHandleRolle("achse.1") to 1,
                    TensorHandleRolle("achse.2") to -1,
                ),
            ),
            unterstuetzungsStatus = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT,
        )

        val formel = TensorOperationFormelCodec.zuFormel(original)
        val rekonstruiert = TensorOperationFormelCodec.ausFormel(formel)

        assertEquals(original, rekonstruiert)
        assertTrue(formel.argumente.any { it.rollenId == "__tensor.achse.achse.1" })
        assertTrue(formel.argumente.any { it.rollenId == "__tensor.achse.achse.2" })
    }

    @Test
    fun `Mehrfachausgangsrollen werden aus derselben Registry rekonstruiert`() {
        val tensor = Tensor(listOf(1, 1), listOf(RationaleZahl.Eins))
        val operation = TensorOperation(
            operationId = TensorOperationId("matrix.svd"),
            operanden = mapOf(TensorHandleRolle("tensor") to tensor),
            unterstuetzungsStatus = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT,
        )

        val rekonstruiert = TensorOperationFormelCodec.ausFormel(
            TensorOperationFormelCodec.zuFormel(operation),
        )
        val definition = requireNotNull(
            StandardTensorOperationen.registry.definition(rekonstruiert.operationId),
        )

        assertEquals(
            listOf("svd.u", "svd.s", "svd.vAdjungiert"),
            definition.ausgangsRollen.map { it.wert },
        )
    }
}
