package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TensorOperationKnotenKonfigurationTest {
    private fun tensorRechner(): KnotenDaten =
        StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero)

    private fun definition(id: String): TensorOperationDefinition =
        requireNotNull(StandardTensorOperationen.registry.definition(id))

    @Test
    fun `Wechsel von Tensorprodukt zu Skalierung erhaelt semantische Altrollen`() {
        val alt = tensorRechner()
        val linksId = alt.anschlüsse.single { it.name == "links" }.id
        val rechtsId = alt.anschlüsse.single { it.name == "rechts" }.id
        val wertId = alt.anschlüsse.single { it.name == "wert" }.id

        val skaliert = konfiguriereTensorOperation(
            alt,
            definition("tensor.skalarmultiplikation"),
        )

        assertEquals(setOf("skalar", "tensor", "ergebnis"), skaliert.anschlüsse.map { it.name }.toSet())
        assertEquals(linksId, skaliert.anschlüsse.single { it.name == "skalar" }.id)
        assertEquals(rechtsId, skaliert.anschlüsse.single { it.name == "tensor" }.id)
        assertEquals(wertId, skaliert.anschlüsse.single { it.name == "ergebnis" }.id)
        assertEquals("tensor.skalarmultiplikation", skaliert.parameter[TENSOR_OPERATION_ID])
        assertEquals(TensorSignaturFamilie.BINAER.name, skaliert.parameter[TENSOR_SIGNATUR_FAMILIE])
    }

    @Test
    fun `Zerlegungswechsel erhaelt Eingang aber keine semantisch fremden Ausgaenge`() {
        val svd = konfiguriereTensorOperation(tensorRechner(), definition("matrix.svd"))
        val tensorId = svd.anschlüsse.single { it.name == "tensor" }.id
        val svdAusgaenge = svd.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .associate { it.name to it.id }

        val qr = konfiguriereTensorOperation(svd, definition("matrix.qr"))

        assertEquals(tensorId, qr.anschlüsse.single { it.name == "tensor" }.id)
        assertEquals(setOf("qr.q", "qr.r"), qr.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name }.toSet())
        assertTrue(qr.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.none { it.id in svdAusgaenge.values })
    }

    @Test
    fun `Achsenmodus wechselt zwischen Tupel und dynamischen Einzelhandles`() {
        val kontraktion = definition("tensor.kontraktion")
        val tupel = konfiguriereTensorOperation(
            tensorRechner(),
            kontraktion,
            AchsenEingabeModus.TUPEL,
        )
        val dynamisch = konfiguriereTensorOperation(
            tupel,
            kontraktion,
            AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
            dynamischeAchsenAnzahl = 2,
        )

        assertTrue(tupel.anschlüsse.any { it.name == "achsen" && it.art == MathematikAnschlussArten.Tupel.id })
        assertEquals(
            listOf("achse.1", "achse.2"),
            dynamisch.anschlüsse.filter { it.name.startsWith("achse.") }.map { it.name },
        )
        assertTrue(dynamisch.anschlüsse.filter { it.name.startsWith("achse.") }.all {
            it.art == MathematikAnschlussArten.Zahl.id
        })
        assertEquals("achse-1,achse-2", dynamisch.parameter[TENSOR_ACHSEN_IDS])
        assertEquals("2", dynamisch.parameter[TENSOR_DYNAMISCHE_ACHSEN_ANZAHL])
    }

    @Test
    fun `Diagnose meldet verbundene Achsentupel vor notwendiger Trennung`() {
        val kontraktion = definition("tensor.kontraktion")
        val tupel = konfiguriereTensorOperation(
            tensorRechner(),
            kontraktion,
            AchsenEingabeModus.TUPEL,
        )
        val achsenId = tupel.anschlüsse.single { it.name == "achsen" }.id

        val diagnose = diagnostiziereTensorSignaturWechsel(
            knoten = tupel,
            definition = kontraktion,
            achsenModus = AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
            dynamischeAchsenAnzahl = 2,
            verbundeneAnschlussIds = setOf(achsenId),
        )

        assertTrue(diagnose.trenntVerbindungen)
        assertEquals(setOf(achsenId), diagnose.verbundeneEntfernteAnschlussIds)
    }

    @Test
    fun `Spezialisierter Tensorprodukt Knoten migriert ohne Anschlussverlust`() {
        val alt = StrukturRechnerKnotenVorlagen.Tensorprodukt.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Tensorprodukt", knoten = listOf(alt))

        val migriert = karte.migriereTensorOperationKnoten()
        val rechner = migriert.knoten.single()

        assertEquals(TensorRechner.KNOTEN_ART, rechner.art)
        assertEquals("tensor.tensorprodukt", rechner.parameter[TENSOR_OPERATION_ID])
        assertEquals(alt.anschlüsse.map { it.id }, rechner.anschlüsse.map { it.id })
        assertEquals(migriert, migriert.migriereTensorOperationKnoten())
    }

    @Test
    fun `eindeutige historische nullbasierte Achsen werden sichtbar einsbasiert`() {
        val alt = tensorRechner().copy(
            parameter = tensorRechner().parameter + mapOf(
                "operator" to TensorRechnerOperator.KONTRAKTION.stabileId,
                "achsen" to "0,2",
            ),
        )

        val migriert = KartenDaten(name = "Altachsen", knoten = listOf(alt))
            .migriereTensorOperationKnoten()
            .knoten.single()

        assertEquals("1,3", migriert.parameter[TENSOR_ACHSEN_SPEZIFIKATION])
        assertNull(migriert.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER])
    }

    @Test
    fun `historische negative Achsen bleiben bestaetigungspflichtig`() {
        val alt = tensorRechner().copy(
            parameter = tensorRechner().parameter + mapOf(
                "operator" to TensorRechnerOperator.KONTRAKTION.stabileId,
                "achsen" to "-1,0",
            ),
        )

        val migriert = KartenDaten(name = "Mehrdeutig", knoten = listOf(alt))
            .migriereTensorOperationKnoten()
            .knoten.single()

        assertEquals("-1,0", migriert.parameter[TENSOR_ACHSEN_SPEZIFIKATION])
        assertTrue(migriert.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER].orEmpty().contains("mehrdeutig"))
    }

    @Test
    fun `Registry Rollen sind nicht positionsgebunden`() {
        val qr = konfiguriereTensorOperation(tensorRechner(), definition("matrix.qr"))
        val svd = konfiguriereTensorOperation(qr, definition("matrix.svd"))

        val qrQ = qr.anschlüsse.single { it.name == "qr.q" }
        val svdU = svd.anschlüsse.single { it.name == "svd.u" }
        assertNotEquals(qrQ.id, svdU.id)
        assertFalse(svd.anschlüsse.any { it.name == "qr.q" || it.name == "qr.r" })
    }
}
