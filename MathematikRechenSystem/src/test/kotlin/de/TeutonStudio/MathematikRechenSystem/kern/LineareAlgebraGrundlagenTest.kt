package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineareAlgebraGrundlagenTest {
    private val x = Variable("x")
    private val y = Variable("y")
    private val a = Variable("a")

    private val additionAufQ = Methode(
        name = "plus",
        parameter = listOf(x, y),
        vorschrift = addition(x, y),
        zielMenge = RationaleZahlen,
        werteVorräte = mapOf(x.name to RationaleZahlen, y.name to RationaleZahlen),
    )

    private val skalareMultiplikationAufQ = Methode(
        name = "skalar",
        parameter = listOf(a, x),
        vorschrift = multiplikation(a, x),
        zielMenge = RationaleZahlen,
        werteVorräte = mapOf(a.name to RationaleZahlen, x.name to RationaleZahlen),
    )

    @Test
    fun `rationale Zahlen werden mit kanonischen Operationen als Vektorraum erkannt`() {
        val aussage = pruefeVektorraum(
            RationaleZahlen,
            additionAufQ,
            skalareMultiplikationAufQ,
        )

        assertEquals(NachweisStatus.Nachgewiesen, aussage.pruefung.status)
        assertIs<VektorraumZeugnis>(aussage.pruefung.zeugnis)
        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
        assertTrue(aussage.pruefung.axiomPruefungen.all { it.status == NachweisStatus.Nachgewiesen })
        assertEquals(VEKTORRAUM_SPEZIFIKATION, aussage.pruefung.spezifikation)
        assertEquals(setOf("menge", "addition", "skalareMultiplikation"), aussage.pruefung.kandidat!!.belegung.keys)
        assertEquals(aussage.pruefung.axiomPruefungen.size, aussage.pruefung.axiomAussagen.size)
    }

    @Test
    fun `unpassende Additionssignatur widerlegt den Vektorraumbegriff`() {
        val falscheAddition = additionAufQ.copy(
            zielMenge = ReelleZahlen,
        )

        val aussage = pruefeVektorraum(
            RationaleZahlen,
            falscheAddition,
            skalareMultiplikationAufQ,
        )

        assertEquals(NachweisStatus.Widerlegt, aussage.pruefung.status)
        assertTrue(aussage.pruefung.diagnosen.any { "nach V" in it })
    }

    @Test
    fun `Identitaet zwischen nachgewiesenen Vektorraeumen ist linear`() {
        val raum = pruefeVektorraum(
            RationaleZahlen,
            additionAufQ,
            skalareMultiplikationAufQ,
        )
        val identitaet = Methode(
            name = "id",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = RationaleZahlen,
            werteVorräte = mapOf(x.name to RationaleZahlen),
        )

        val aussage = pruefeLineareAbbildung(raum, raum, identitaet)

        assertEquals(NachweisStatus.Nachgewiesen, aussage.pruefung.status)
        assertIs<LineareAbbildungsZeugnis>(aussage.pruefung.zeugnis)
        assertEquals(LINEARE_ABBILDUNG_SPEZIFIKATION, aussage.pruefung.spezifikation)
        assertEquals(setOf("definitionsraum", "zielraum", "methode"), aussage.pruefung.kandidat!!.belegung.keys)
    }

    @Test
    fun `quadratische Methode liefert konkretes Gegenbeispiel gegen Linearitaet`() {
        val raum = pruefeVektorraum(
            RationaleZahlen,
            additionAufQ,
            skalareMultiplikationAufQ,
        )
        val quadrat = Methode(
            name = "q",
            parameter = listOf(x),
            vorschrift = Potenz(x, RationaleZahl.von(2)),
            zielMenge = RationaleZahlen,
            werteVorräte = mapOf(x.name to RationaleZahlen),
        )

        val aussage = pruefeLineareAbbildung(raum, raum, quadrat)

        assertEquals(NachweisStatus.Widerlegt, aussage.pruefung.status)
        val widerlegung = aussage.pruefung.axiomPruefungen.firstOrNull {
            it.status == NachweisStatus.Widerlegt && it.gegenbeispiel.isNotEmpty()
        }
        assertNotNull(widerlegung)
        assertTrue(widerlegung.gegenbeispiel.keys.any { it.startsWith("f(") })
    }

    @Test
    fun `Gauss Jordan erzeugt reduzierte Zeilenstufenform und strukturierte Schritte`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
            ),
        )

        val ergebnis = gauss(matrix)

        assertEquals(
            Matrix(
                listOf(
                    listOf(RationaleZahl.Eins, RationaleZahl.Null),
                    listOf(RationaleZahl.Null, RationaleZahl.Eins),
                ),
            ),
            ergebnis.matrix,
        )
        assertEquals(2, ergebnis.rang)
        assertTrue(ergebnis.schritte.isNotEmpty())
        assertTrue(ergebnis.schritte.all { it.strukturOperation is ZeilenOperation })
        val tabelle = ergebnis.verlauf.alsMatrixTabelle()
        assertEquals(2, tabelle.spalten.size)
        assertEquals(ergebnis.schritte.size + 1, tabelle.bloecke.size)
        assertTrue(tabelle.bloecke.drop(1).all { block ->
            block.zeilen.count { it.operation != null } in 1..2
        })
    }

    @Test
    fun `lineares Gleichungssystem unterscheidet eindeutige parametrische und fehlende Loesung`() {
        val eindeutig = loeseLinearesSystem(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                    listOf(RationaleZahl.von(1), RationaleZahl.von(-1)),
                ),
            ),
            SpaltenVektor(listOf(RationaleZahl.von(3), RationaleZahl.von(1))),
        )
        assertEquals(
            EindeutigeLineareLoesung(
                SpaltenVektor(listOf(RationaleZahl.von(2), RationaleZahl.von(1))),
            ),
            eindeutig.loesung,
        )

        val parametrisch = loeseLinearesSystem(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                    listOf(RationaleZahl.von(2), RationaleZahl.von(2)),
                ),
            ),
            SpaltenVektor(listOf(RationaleZahl.von(2), RationaleZahl.von(4))),
        )
        assertIs<ParametrischeLineareLoesung>(parametrisch.loesung)

        val keine = loeseLinearesSystem(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                    listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                ),
            ),
            SpaltenVektor(listOf(RationaleZahl.von(1), RationaleZahl.von(2))),
        )
        assertEquals(KeineLineareLoesung, keine.loesung)
        assertTrue(keine.rangErweitert > keine.rangKoeffizienten)
    }

    @Test
    fun `Inverse wird ueber die erweiterte Matrix bestimmt`() {
        val ergebnis = inverseMitGauss(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                    listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
                ),
            ),
        )

        assertEquals(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(-2), RationaleZahl.von(1)),
                    listOf(RationaleZahl.von(3, 2), RationaleZahl.von(-1, 2)),
                ),
            ),
            ergebnis.inverse,
        )
        val tabelle = ergebnis.verlauf.alsMatrixTabelle(rechteSeitenSpalten = 2)
        assertTrue(tabelle.spalten.takeLast(2).all { it.istRechteSeite })
        assertEquals("I", tabelle.bloecke.first().zeilen.first().name)
        assertEquals("II", tabelle.bloecke.first().zeilen[1].name)
    }

}
