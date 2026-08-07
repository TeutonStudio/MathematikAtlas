package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TensorOperationRegistryTest {
    @Test
    fun `sichtbare Achsen beginnen bei eins und negative Indizes zaehlen von hinten`() {
        assertEquals(0, normalisiereTensorAchse(1, 4).position)
        assertEquals(3, normalisiereTensorAchse(4, 4).position)
        assertEquals(3, normalisiereTensorAchse(-1, 4).position)
        assertEquals(1, normalisiereTensorAchse(-3, 4).position)
        assertFailsWith<IllegalArgumentException> { normalisiereTensorAchse(0, 4) }
        assertFailsWith<IllegalArgumentException> { normalisiereTensorAchse(5, 4) }
        assertFailsWith<IllegalArgumentException> { normalisiereTensorAchse(-5, 4) }
    }

    @Test
    fun `Mehrfachachsen bewahren Reihenfolge und Duplikate bis zur Operatorpruefung`() {
        val normalisiert = normalisiereTensorAchsen(listOf(-1, 1, -1), 3)
        assertEquals(listOf(2, 0, 2), normalisiert.map { it.position })
    }

    @Test
    fun `Permutation verlangt jede Achse genau einmal`() {
        assertEquals(listOf(2, 0, 1), normalisiereTensorPermutation(listOf(-1, 1, 2), 3))
        assertFailsWith<IllegalArgumentException> {
            normalisiereTensorPermutation(listOf(1, 1, 2), 3)
        }
        assertFailsWith<IllegalArgumentException> {
            normalisiereTensorPermutation(listOf(1, 2), 3)
        }
    }

    @Test
    fun `Registry enthaelt stabile Rollen fuer Zerlegungen`() {
        val svd = assertNotNull(StandardTensorOperationen.registry.definition(TensorOperationId("matrix.svd")))
        assertEquals(TensorSignaturFamilie.ZERLEGUNG, svd.familie)
        assertEquals(
            listOf("svd.u", "svd.s", "svd.vAdjungiert"),
            svd.ausgangsRollen.map { it.wert },
        )
        assertEquals(TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT, svd.unterstuetzungsStatus)
    }

    @Test
    fun `Indexauswertung nutzt Komponentenparameter statt Achseneingang`() {
        val index = assertNotNull(
            StandardTensorOperationen.registry.definition(TensorOperationId("tensor.indexauswertung")),
        )

        assertEquals(0, index.minimaleAchsenAnzahl)
        assertEquals(0, index.maximaleAchsenAnzahl)
        assertEquals(listOf("indizes"), index.parameter.map { it.id })
    }

    @Test
    fun `Skalare sind nur fuer ausdruecklich erlaubte Operationen zugelassen`() {
        val produkt = assertNotNull(
            StandardTensorOperationen.registry.definition(TensorOperationId("tensor.tensorprodukt")),
        )
        val skalierung = assertNotNull(
            StandardTensorOperationen.registry.definition(TensorOperationId("tensor.skalarmultiplikation")),
        )
        val spur = assertNotNull(StandardTensorOperationen.registry.definition(TensorOperationId("tensor.spur")))

        assertTrue(produkt.erlaubtSkalar)
        assertTrue(skalierung.erlaubtSkalar)
        assertFalse(spur.erlaubtSkalar)
    }

    @Test
    fun `Achsenidentitaeten bewegen sich mit der Permutation`() {
        val tensor = Tensor(
            dimensionen = listOf(2, 3),
            werte = List(6) { RationaleZahl.von((it + 1).toLong()) },
        ).mitStabilenAchsen(
            listOf(TensorAchsenId("zeile"), TensorAchsenId("spalte")),
        )

        val transponiert = tensor.permutiereAchsen(listOf(1, 0))

        assertEquals(listOf(3, 2), transponiert.tensor.dimensionen)
        assertEquals(listOf(TensorAchsenId("spalte"), TensorAchsenId("zeile")), transponiert.achsenIds)
    }

    @Test
    fun `Signaturfamilien koennen gemeinsam gefiltert werden`() {
        val zerlegungen = StandardTensorOperationen.registry.familie(TensorSignaturFamilie.ZERLEGUNG)
        assertEquals(
            setOf("matrix.jordan", "matrix.qr", "matrix.spektral", "matrix.svd"),
            zerlegungen.map { it.id.wert }.toSet(),
        )
    }

    @Test
    fun `bestehende Tensorrechner Enum bildet eindeutig auf Registry IDs ab`() {
        TensorRechnerOperator.entries.forEach { operator ->
            val definition = assertNotNull(
                StandardTensorOperationen.registry.definition(operator.alsTensorOperationId()),
                "Für ${operator.stabileId} fehlt ein Registryeintrag.",
            )
            assertEquals(operator, definition.alsBestehenderTensorOperatorOderNull())
        }
    }

    @Test
    fun `symbolisch gueltige Operation bewahrt Operanden Achsen und Status`() {
        val tensor = Tensor(listOf(1), listOf(RationaleZahl.Eins))
        val ausdruck = TensorOperation(
            operationId = TensorOperationId("matrix.svd"),
            operanden = mapOf(TensorHandleRolle("tensor") to tensor),
            achsen = TensorAchsenSpezifikation.Tupel(listOf(1)),
            unterstuetzungsStatus = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT,
        )

        assertTrue(ausdruck.zuLatex().contains("matrix.svd"))
        assertTrue(ausdruck.zuLatex().contains(";1"))
        assertEquals(TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT, ausdruck.unterstuetzungsStatus)
    }
}
