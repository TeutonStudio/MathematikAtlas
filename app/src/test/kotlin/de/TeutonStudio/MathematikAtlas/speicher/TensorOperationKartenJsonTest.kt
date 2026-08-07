package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.AchsenEingabeModus
import de.TeutonStudio.MathematikRechenSystem.kern.StandardTensorOperationen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TensorOperationKartenJsonTest {
    @Test
    fun `SVD Rollen Achsenmodus und stabile IDs bleiben im Karten JSON erhalten`() {
        val basis = StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero)
        val svd = konfiguriereTensorOperation(
            knoten = basis,
            definition = requireNotNull(StandardTensorOperationen.registry.definition("matrix.svd")),
            achsenModus = AchsenEingabeModus.TUPEL,
        )
        val karte = KartenDaten(name = "SVD", knoten = listOf(svd))

        val text = KartenJson.schreibe(karte)
        val gelesen = KartenJson.lese(text).knoten.single()

        assertEquals("matrix.svd", gelesen.parameter[TENSOR_OPERATION_ID])
        assertEquals(svd.parameter[TENSOR_SIGNATUR_FAMILIE], gelesen.parameter[TENSOR_SIGNATUR_FAMILIE])
        assertEquals(svd.parameter[TENSOR_ACHSEN_EINGABE_MODUS], gelesen.parameter[TENSOR_ACHSEN_EINGABE_MODUS])
        assertEquals(svd.parameter[TENSOR_ACHSEN_IDS], gelesen.parameter[TENSOR_ACHSEN_IDS])
        assertEquals(
            setOf("svd.u", "svd.s", "svd.vAdjungiert"),
            gelesen.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Ausgang }
                .map { it.name }
                .toSet(),
        )
        assertEquals(svd.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `historischer Tensorrechner wird beim Lesen auf Registryparameter migriert`() {
        val alt = StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero).copy(
            parameter = StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero).parameter + mapOf(
                "operator" to "tensor.kontraktion",
                "achsen" to "0,1",
            ),
        )
        val roh = de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson.schreibe(
            KartenDaten(name = "Alt", knoten = listOf(alt)),
        )

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals("tensor.kontraktion", gelesen.parameter[TENSOR_OPERATION_ID])
        assertEquals("1,2", gelesen.parameter[TENSOR_ACHSEN_SPEZIFIKATION])
        assertFalse("operator" in gelesen.parameter)
        assertTrue(gelesen.parameter[TENSOR_ACHSEN_IDS].orEmpty().isNotBlank())
    }
}
