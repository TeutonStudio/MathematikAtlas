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
    fun `Registry enthaelt stabile Rollen fuer Zerlegungen`() {
        val svd = assertNotNull(StandardTensorOperationen.registry.definition(TensorOperationId("matrix.svd")))
        assertEquals(TensorSignaturFamilie.ZERLEGUNG, svd.familie)
        assertEquals(
            listOf("svd.u", "svd.s", "svd.vAdjungiert"),
            svd.ausgangsRollen.map { it.wert },
        )
    }

    @Test
    fun `Skalare sind nur fuer ausdruecklich erlaubte Operationen zugelassen`() {
        val produkt = assertNotNull(StandardTensorOperationen.registry.definition(TensorOperationId("tensor.produkt")))
        val spur = assertNotNull(StandardTensorOperationen.registry.definition(TensorOperationId("tensor.spur")))

        assertTrue(produkt.erlaubtSkalar)
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
            setOf("matrix.jordan", "matrix.qr", "matrix.svd"),
            zerlegungen.map { it.id.wert }.toSet(),
        )
    }
}
