package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KanonischeMethodenanwendungTest {
    private val x = Variable("x", bindungsId = "quelle-x")
    private val y = Variable("y", bindungsId = "quelle-y")
    private fun methode() = MathematischeMethode("f", listOf(x, y), Tupel(listOf(x, y)),
        Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        listOf("erstes", "zweites"), DefinierteMenge(listOf(
            GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen),
        ), Vergleich(x, VergleichsArt.Kleiner, y)))

    @Test fun `gemeinsame Domaene wird vor Anwendung geprueft`() {
        val f = methode()
        assertFailsWith<IllegalArgumentException> { f.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.von(5), RationaleZahl.von(2)))) }
        assertEquals(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(5))),
            f.wendeMitIdentitätenAn(mapOf("quelle-x" to RationaleZahl.von(2), "quelle-y" to RationaleZahl.von(5))).ergebnis)
    }
    @Test fun `Umordnung behaelt Komponentenidentitaeten`() {
        val f = methode()
        val neu = f.copy(parameter = listOf(y, x))
        assertEquals(listOf("quelle-y", "quelle-x"), neu.signatur.argumente.map { it.id })
    }
    @Test fun `explizite Bindung verwendet keinen Namen`() {
        val a = Variable("x", bindungsId = "a")
        val b = Variable("x", bindungsId = "b")
        val term = Tupel(listOf(a, b))
        assertEquals(Tupel(listOf(RationaleZahl.Eins, b)), ersetze(term, mapOf("a" to RationaleZahl.Eins)))
        assertEquals(term, ersetze(term, mapOf("x" to RationaleZahl.Eins)))
    }
    @Test fun `partielle Anwendung erhaelt restliche Identitaet und Domaene`() {
        val teil = methode().bindeIdentitäten(mapOf("quelle-x" to RationaleZahl.von(3)))
        assertEquals(listOf("quelle-y"), teil.signatur.argumente.map { it.id })
        assertFailsWith<IllegalArgumentException> { teil.wendeMitIdentitätenAn(mapOf("quelle-y" to RationaleZahl.von(2))) }
    }
    @Test fun `unbekannte Voraussetzungen werden erhalten`() {
        val h = Variable("h")
        val f = MathematischeMethode("f", listOf(h), h, ReelleZahlen, mapOf("h" to ReelleZahlen),
            voraussetzungen = setOf(Ungleichheit(h, RationaleZahl.Null)))
        val symbol = Variable("a")
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in f.wendeMitBedingungenAn(Tupel(listOf(symbol))).bedingungen)
        assertFailsWith<IllegalArgumentException> { f.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.Null))) }
    }

    @Test fun `verschachtelte Aufrufe erhalten Voraussetzungen der eingesetzten Methode`() {
        val sig = MethodenSignatur(listOf(MethodenKomponente("t", "t", 0, ReelleZahlen.elementTypAusdruck())), listOf(MethodenKomponente("r", "r", 0, ReelleZahlen.elementTypAusdruck())))
        val f = MethodenVariable("f", "f-id", sig)
        val a = Variable("a", bindungsId = "a-id")
        val h = MathematischeMethode("H", listOf(f, a), MethodenZahlKomponente(StrukturellerMethodenAufruf(f, Tupel(listOf(a))), 1), ReelleZahlen,
            mapOf("f" to Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)), Tupelraum(listOf(ReelleZahlen))), "a" to ReelleZahlen))
        val t = Variable("t")
        val konkret = MathematischeMethode("f", listOf(t), RationaleZahl.Eins, ReelleZahlen, mapOf("t" to ReelleZahlen), voraussetzungen = setOf(Ungleichheit(t, RationaleZahl.Null)))
        val symbol = Variable("u")
        val ergebnis = h.wendeMitIdentitätenAn(mapOf("f-id" to konkret, "a-id" to symbol))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in ergebnis.bedingungen)
        val partiell = h.bindeIdentitäten(mapOf("f-id" to konkret))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in partiell.wendeMitIdentitätenAn(mapOf("a-id" to symbol)).bedingungen)
        val mitAufrufVoraussetzung = h.copy(vorschrift = RationaleZahl.Eins, voraussetzungen = setOf(Gleichheit(h.vorschrift, RationaleZahl.Eins)))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in mitAufrufVoraussetzung.wendeMitIdentitätenAn(mapOf("f-id" to konkret, "a-id" to symbol)).bedingungen)
        val teilVoraussetzung = mitAufrufVoraussetzung.bindeIdentitäten(mapOf("f-id" to konkret))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in teilVoraussetzung.wendeMitIdentitätenAn(mapOf("a-id" to symbol)).bedingungen)
        val inneresX = Variable("inneresX")
        val mittel = MathematischeMethode("mittel", listOf(inneresX), RationaleZahl.Eins, ReelleZahlen, mapOf("inneresX" to ReelleZahlen),
            voraussetzungen = setOf(Gleichheit(MethodenZahlKomponente(StrukturellerMethodenAufruf(konkret, Tupel(listOf(inneresX))), 1), RationaleZahl.Eins)))
        val aussen = MathematischeMethode("aussen", listOf(a), MethodenZahlKomponente(StrukturellerMethodenAufruf(mittel, Tupel(listOf(a))), 1), ReelleZahlen, mapOf("a" to ReelleZahlen))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in aussen.wendeMitIdentitätenAn(mapOf("a-id" to symbol)).bedingungen)
        val konjugiert = h.copy(vorschrift = Konjugierte(h.vorschrift as ZahlAusdruck))
        assertTrue(Ungleichheit(symbol, RationaleZahl.Null) in konjugiert.wendeMitIdentitätenAn(mapOf("f-id" to konkret, "a-id" to symbol)).bedingungen)
    }
    @Test fun `Indexumbenennung behaelt Komponentenidentitaet`() {
        val i = Variable("i")
        val f = MathematischeMethode("index", listOf(i), i, ReelleZahlen, mapOf("i" to ReelleZahlen))
        val neu = f.benenneIndexParameter(listOf("j"))
        assertEquals(f.signatur.argumente.single().id, neu.signatur.argumente.single().id)
        assertEquals(RationaleZahl.Eins, neu.wendeAn(listOf(RationaleZahl.Eins)))
    }
}
