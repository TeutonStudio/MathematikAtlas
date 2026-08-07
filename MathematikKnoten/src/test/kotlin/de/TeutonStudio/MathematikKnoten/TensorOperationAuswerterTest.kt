package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TensorOperationAuswerterTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun basisKnoten(): KnotenDaten =
        StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero)

    private fun definition(id: String): TensorOperationDefinition =
        requireNotNull(StandardTensorOperationen.registry.definition(id))

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `konkrete Addition verwendet Registry Rollen und bestehenden Tensorrechner`() {
        val knoten = konfiguriereTensorOperation(basisKnoten(), definition("tensor.addition"))
        val links = Tensor(listOf(2), listOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val rechts = Tensor(listOf(2), listOf(RationaleZahl.von(3), RationaleZahl.von(4)))

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "links" to BedingterWert(links),
                    "rechts" to BedingterWert(rechts),
                ),
            ),
        )

        val tensor = assertIs<Tensor>(ergebnis.ausgaben.getValue("ergebnis").objekt)
        assertEquals(listOf(RationaleZahl.von(4), RationaleZahl.von(6)), tensor.werte)
        assertTrue(ergebnis.warnungen.any { it.contains("konkret implementiert") })
    }

    @Test
    fun `sichtbare Kontraktionsachsen werden einsbasiert normalisiert`() {
        val basis = konfiguriereTensorOperation(
            basisKnoten(),
            definition("tensor.kontraktion"),
        )
        val knoten = basis.copy(
            parameter = basis.parameter + (TENSOR_ACHSEN_SPEZIFIKATION to "1,2"),
        )
        val matrix = Tensor(
            listOf(2, 2),
            listOf(
                RationaleZahl.Eins,
                RationaleZahl.von(2),
                RationaleZahl.von(3),
                RationaleZahl.von(4),
            ),
        )

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(knoten, mapOf("tensor" to BedingterWert(matrix))),
        )

        assertEquals(RationaleZahl.von(5), ergebnis.ausgaben.getValue("ergebnis").objekt)
    }

    @Test
    fun `ungueltiges Achsentoken wird nicht still verworfen`() {
        val basis = konfiguriereTensorOperation(
            basisKnoten(),
            definition("tensor.kontraktion"),
        )
        val knoten = basis.copy(
            parameter = basis.parameter + (TENSOR_ACHSEN_SPEZIFIKATION to "1,spalte"),
        )
        val matrix = Tensor(
            listOf(2, 2),
            listOf(RationaleZahl.Eins, RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
        )

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(knoten, mapOf("tensor" to BedingterWert(matrix))),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("spalte"))
    }

    @Test
    fun `nichtganzzahlige Tupelachse wird als Fehler gemeldet`() {
        val knoten = konfiguriereTensorOperation(
            basisKnoten(),
            definition("tensor.kontraktion"),
        )
        val matrix = Tensor(
            listOf(2, 2),
            listOf(RationaleZahl.Eins, RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
        )

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "tensor" to BedingterWert(matrix),
                    "achsen" to BedingterWert(
                        Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(3, 2))),
                    ),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("2. Achsenkomponente"))
    }

    @Test
    fun `Legacy Tensorrechner bedient weiterhin den Ausgang wert`() {
        val alt = basisKnoten().copy(
            parameter = basisKnoten().parameter +
                ("operator" to TensorRechnerOperator.ADDITION.stabileId),
        )
        val links = Tensor(listOf(1), listOf(RationaleZahl.Eins))
        val rechts = Tensor(listOf(1), listOf(RationaleZahl.von(2)))

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(
                alt,
                mapOf(
                    "links" to BedingterWert(links),
                    "rechts" to BedingterWert(rechts),
                ),
            ),
        )

        assertTrue("wert" in ergebnis.ausgaben)
        assertEquals(RationaleZahl.von(3), assertIs<Tensor>(ergebnis.ausgaben.getValue("wert").objekt).werte.single())
    }

    @Test
    fun `SVD liefert stabile symbolische Mehrfachausgaenge`() {
        val knoten = konfiguriereTensorOperation(basisKnoten(), definition("matrix.svd"))
        val matrix = Tensor(
            listOf(2, 2),
            listOf(RationaleZahl.Eins, RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
        )

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(knoten, mapOf("tensor" to BedingterWert(matrix))),
        )

        assertEquals(setOf("svd.u", "svd.s", "svd.vAdjungiert"), ergebnis.ausgaben.keys)
        assertTrue(ergebnis.ausgaben.values.all { it.objekt is TensorOperation })
        assertTrue(ergebnis.warnungen.any { it.contains("Symbolisch gültig") })
    }

    @Test
    fun `mehrdeutige Altachsen blockieren die Auswertung bis zur Bestaetigung`() {
        val knoten = basisKnoten().copy(
            parameter = basisKnoten().parameter + mapOf(
                TENSOR_OPERATION_ID to TensorRechnerOperator.KONTRAKTION.stabileId,
                TENSOR_ACHSEN_MIGRATIONSFEHLER to "Historische negative Achsenindizes sind mehrdeutig.",
            ),
        )
        val matrix = Tensor(
            listOf(2, 2),
            listOf(RationaleZahl.Eins, RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
        )

        val ergebnis = register.finde(TensorRechner.KNOTEN_ART)!!.auswerten(
            kontext(knoten, mapOf("links" to BedingterWert(matrix))),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("bestätige"))
    }
}
