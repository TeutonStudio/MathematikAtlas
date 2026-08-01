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
        val methode = Funktion(
            name = "A",
            parameter = listOf(i),
            ausgaben = mapOf("wert" to werte),
            zielMengen = mapOf("wert" to werte),
            werteVorräte = mapOf(i.name to werte),
        )
        val indexMenge = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))

        val produkt = assertIs<EndlicheMenge>(iteriertesKartesischesProdukt(methode, indexMenge))

        assertEquals(4, produkt.elemente.size)
        assertTrue(produkt.elemente.all { it is Funktion })
        val koordinaten = produkt.elemente.map { it as Funktion }.map { auswahl ->
            assertEquals(indexMenge, auswahl.werteVorräte.getValue(i.name))
            listOf(
                auswahl.wendeAn(mapOf(i.name to RationaleZahl.von(1))).getValue("wert"),
                auswahl.wendeAn(mapOf(i.name to RationaleZahl.von(2))).getValue("wert"),
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
    fun `leeres indexiertes Produkt enthält genau die leere Funktion`() {
        val methode = Funktion(
            name = "A",
            parameter = listOf(i),
            ausgaben = mapOf("wert" to EndlicheMenge(setOf(RationaleZahl.Eins))),
            zielMengen = mapOf("wert" to EndlicheMenge(setOf(RationaleZahl.Eins))),
            werteVorräte = mapOf(i.name to ReelleZahlen),
        )

        val produkt = assertIs<EndlicheMenge>(iteriertesKartesischesProdukt(methode, LeereMenge))
        val leereFunktion = assertIs<Funktion>(produkt.elemente.single())

        assertTrue(leereFunktion.parameter.isEmpty())
        assertTrue(leereFunktion.ausgaben.isEmpty())
    }

    @Test
    fun `Ergebnis ist unabhängig von Einfügereihenfolge der Indexmenge`() {
        val methode = Funktion(
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
