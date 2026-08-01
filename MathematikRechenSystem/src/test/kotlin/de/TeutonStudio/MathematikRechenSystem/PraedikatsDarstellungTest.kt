package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PraedikatsDarstellungTest {
    @Test
    fun `reines aussagenpraedikat zeigt den aufgeloesten term`() {
        val a = AussagenParameter("A")
        val b = AussagenParameter("B")
        val methode = Methode(
            name = "P",
            parameter = listOf(a, b),
            ausgaben = mapOf("aussage" to Disjunktion(listOf(a, b))),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(a.name to WahrheitsMenge, b.name to WahrheitsMenge),
        )

        assertEquals("P:A \\lor B", methode.kompaktePrädikatsDarstellung())
        assertTrue(methode.istOffenesPrädikat())
    }

    @Test
    fun `gemischtes praedikat zeigt geordnete argumentbereiche`() {
        val x = Variable("x")
        val a = AussagenParameter("A")
        val methode = Methode(
            name = "P",
            parameter = listOf(x, a),
            ausgaben = mapOf("aussage" to Konjunktion(listOf(Gleichheit(x, RationaleZahl.Null), a))),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(x.name to ReelleZahlen, a.name to WahrheitsMenge),
        )

        assertEquals("P:\\mathbb{R}\\times\\{A\\}", methode.kompaktePrädikatsDarstellung())
    }

    @Test
    fun `stabile quellenidentitaet dedupliziert mehrfach verwendetes argument`() {
        val x = Variable("x")
        val methode = Methode(
            name = "P",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Gleichheit(x, x)),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val quelle = PrädikatsArgument.Wert("x", ReelleZahlen, "knoten-1:wert")

        assertEquals(
            "P:\\mathbb{R}",
            methode.kompaktePrädikatsDarstellung(argumentQuellen = listOf(quelle, quelle)),
        )
    }

    @Test
    fun `fehlender wertevorrat ist ein fachlicher fehler`() {
        val x = Variable("x")
        val methode = Methode(
            name = "P",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Gleichheit(x, RationaleZahl.Null)),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
        )

        assertFailsWith<MethodenSignaturFehler> { methode.kompaktePrädikatsDarstellung() }
    }

    @Test
    fun `nicht entscheidbare aussage bleibt eine gueltige tabellenzelle`() {
        val a = AussagenParameter("A")
        val methode = Methode(
            name = "P",
            parameter = listOf(a),
            ausgaben = mapOf("aussage" to a),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(a.name to WahrheitsMenge),
        )
        val gebunden = methode.binde(mapOf(a.name to AussagenParameter("U")))

        val ergebnis = gebunden.wahrheitstabellenErgebnis()
        assertNull(ergebnis.wahrheitswert)
        assertEquals(EntscheidungsStatus.Unbekannt, ergebnis.status)
    }
}
