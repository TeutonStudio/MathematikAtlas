package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IndexiertesKartesischesProduktTest {
    private val i = Variable("i")

    @Test
    fun `endliches indexiertes Produkt besteht aus Auswahlfunktionen`() {
        val werte = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))
        val methode = Methode(
            name = "A",
            parameter = listOf(i),
            ausgaben = mapOf("wert" to werte),
            zielMengen = mapOf("wert" to werte),
            werteVorräte = mapOf(i.name to werte),
        )
        val indexMenge = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))

        val produkt = assertIs<EndlicheMenge>(iteriertesKartesischesProdukt(methode, indexMenge))

        assertEquals(4, produkt.elemente.size)
        assertTrue(produkt.elemente.all { it is Methode })
        val koordinaten = produkt.elemente.map { it as Methode }.map { auswahl ->
            assertEquals(indexMenge, auswahl.werteVorräte.getValue(i.name))
            listOf(
                auswahl.wendeAn(listOf(RationaleZahl.von(1))),
                auswahl.wendeAn(listOf(RationaleZahl.von(2))),
            )
        }.toSet()
        assertEquals(
            setOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(2), RationaleZahl.von(1)),
                listOf(RationaleZahl.von(2), RationaleZahl.von(2)),
            ),
            koordinaten,
        )
    }

    @Test
    fun `leeres indexiertes Produkt enthält genau die leere Methode`() {
        val methode = Methode(
            name = "A",
            parameter = listOf(i),
            ausgaben = mapOf("wert" to EndlicheMenge(setOf(RationaleZahl.Eins))),
            zielMengen = mapOf("wert" to EndlicheMenge(setOf(RationaleZahl.Eins))),
            werteVorräte = mapOf(i.name to ReelleZahlen),
        )

        val produkt = assertIs<EndlicheMenge>(iteriertesKartesischesProdukt(methode, LeereMenge))
        val leereFunktion = assertIs<Methode>(produkt.elemente.single())

        assertTrue(leereFunktion.parameter.isEmpty())
        assertEquals(Tupel(emptyList()), leereFunktion.vorschrift)
    }

    @Test
    fun `Ergebnis ist unabhängig von Einfügereihenfolge der Indexmenge`() {
        val methode = Methode(
            name = "A",
            parameter = listOf(i),
            ausgaben = mapOf("wert" to EndlicheMenge(setOf(i))),
            zielMengen = mapOf("wert" to EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))),
            werteVorräte = mapOf(i.name to ReelleZahlen),
        )
        val erste = EndlicheMenge(linkedSetOf(RationaleZahl.von(2), RationaleZahl.von(1)))
        val zweite = EndlicheMenge(linkedSetOf(RationaleZahl.von(1), RationaleZahl.von(2)))

        assertEquals(
            iteriertesKartesischesProdukt(methode, erste),
            iteriertesKartesischesProdukt(methode, zweite),
        )
    }
}
