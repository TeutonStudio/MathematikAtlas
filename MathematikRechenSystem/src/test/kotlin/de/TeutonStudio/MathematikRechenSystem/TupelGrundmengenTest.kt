package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class TupelGrundmengenTest {
    @Test fun `kartesische Potenz bewahrt Einertupel und leeres Tupel`() {
        val raum = kartesischePotenz(ReelleZahlen, RationaleZahl.Eins)
        assertEquals("\\mathbb{R}^{1}", raum.zuLatex())
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Tupel(listOf(RationaleZahl.Eins)), raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Tupel(emptyList()), kartesischePotenz(ReelleZahlen, RationaleZahl.Null)).entscheide().wahrheitswert)
    }

    @Test fun `kartesische Potenz weist falsche Tupeldimension ab`() {
        val raum = kartesischePotenz(ReelleZahlen, RationaleZahl.von(2))
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Tupel(listOf(RationaleZahl.Eins)), raum).entscheide().wahrheitswert)
    }

    @Test fun `Potenzdimension validiert konkrete Werte und bindet symbolische Dimension`() {
        assertFailsWith<IllegalArgumentException> { kartesischePotenz(ReelleZahlen, RationaleZahl.von(-1)) }
        assertFailsWith<IllegalArgumentException> { kartesischePotenz(ReelleZahlen, RationaleZahl.von(1, 2)) }
        val symbolisch = kartesischePotenz(ReelleZahlen, Variable("n"))
        assertEquals(setOf(Variable("n")), symbolisch.enthalteneMethodenParameter())
        assertEquals(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Tupel), symbolisch.elementTypAusdruck())
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Tupel(listOf(RationaleZahl.Eins)), ersetze(symbolisch, mapOf("n" to RationaleZahl.Eins)) as MengenAusdruck).entscheide().wahrheitswert)
        assertEquals(EntscheidungsStatus.Unbekannt, ElementBeziehung(Tupel(emptyList()), symbolisch).entscheide().status)
    }

    @Test fun `Bool Einertupel wird als Praedikat erkannt und filtert`() {
        val x = Variable("x")
        val methode = Methode(name = "positiv", parameter = listOf(x),
            ausgaben = mapOf("wert" to Tupel(listOf(Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null)))),
            zielMengen = mapOf("wert" to Tupelraum(listOf(WahrheitsMenge))),
            werteVorräte = mapOf("x" to ReelleZahlen))
        assertTrue(methode.istPrädikat())
        assertEquals("positiv:\\mathbb{R}", methode.kompaktePrädikatsDarstellung())
        assertEquals(Wahrheitswert.Wahr, GebundeneMethode(methode, mapOf("x" to RationaleZahl.Eins)).wahrheitstabellenErgebnis().wahrheitswert)
        assertEquals(EndlicheMenge(setOf(RationaleZahl.Eins)), filtereMenge(EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(-1))), methode))
        assertNull(Tupel(listOf(WahrheitsKonstante(true), WahrheitsKonstante(false))).alsPrädikatsAussageOderNull())
    }

    @Test fun `gemeinsame Grundmenge ist Vereinigung der Komponenten`() {
        val raum = Tupelraum(listOf(ReelleZahlen, RationaleZahlen))
        assertEquals(vereinige(raum.komponenten), raum.gemeinsameGrundmenge())
        assertTrue(raum.potenzLatex().endsWith("^{2}"))
    }

    @Test fun `strukturierte Bereiche behalten Tupelform und Orientierung`() {
        val raum = Tupelraum(listOf(ReelleZahlen, ReelleZahlen))
        val a = RationaleZahl.Eins
        val b = RationaleZahl.von(2)
        assertEquals(raum, strukturRaumÜbersetzung(raum, Tupel(listOf(a,b))))
        val zeile = strukturRaumÜbersetzung(raum, ZeilenVektor(listOf(a,b))) as Vektorraum
        assertEquals(VektorOrientierung.Zeile, zeile.orientierung)
        assertEquals(2, zeile.dimension)
        assertEquals(ReelleZahlen, zeile.skalarMenge)
    }

    @Test fun `Abflachung bewahrt verschachtelte Komponentenbereiche`() {
        val raum = Tupelraum(listOf(ReelleZahlen, Tupelraum(listOf(RationaleZahlen, ReelleZahlen))))
        val tupel = Tupel(listOf(RationaleZahl.Eins, Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))))
        assertEquals(Tupelraum(listOf(ReelleZahlen, RationaleZahlen, ReelleZahlen)), tupelAbflachenRaum(tupel, raum))
    }
}
