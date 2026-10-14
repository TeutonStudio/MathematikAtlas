package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class FormelVertragsabschlussTest {
    private val r = ReelleZahlen.elementTypAusdruck()
    private val skalar = MethodenSignatur(listOf(MethodenKomponente("t", "t", 0, r)),
        listOf(MethodenKomponente("wert", "wert", 0, r)))
    private fun wert(name: String) = FormelVariablenQuelle("quelle-$name", name, FormelVariablenArt.FREI, FormelTyp.ZAHL, ReelleZahlen)
    private fun methode(name: String, signatur: MethodenSignatur) = FormelVariablenQuelle("quelle-$name", name,
        FormelVariablenArt.FREI, FormelTyp.METHODE, vollstaendigerTyp = signatur.typAusdruck, methodenSignatur = signatur)

    @Test fun `unbekannter Aufruf verlangt Deklaration statt Multiplikation`() {
        val fehler = assertFails { FormelLatexCodec.importiereGebunden("f(x)", listOf(wert("x"))) }
        assertContains(fehler.message.orEmpty(), "Mehrdeutig")
        assertFails { FormelLatexCodec.importiereGebunden("(f)(x)", listOf(wert("x"))) }
        val produkt = FormelLatexCodec.importiereGebunden("f*(x)", listOf(wert("f"), wert("x")))
        assertEquals("zahl.multiplikation", assertIs<FormelAusdruck.Operation>(produkt.wurzel).operatorId)
    }

    @Test fun `deklarierte e und sin haben Vorrang vor eingebauten Namen`() {
        val e = FormelLatexCodec.importiereGebunden("e", listOf(wert("e")))
        assertEquals(RationaleZahl.von(7), e.werteAus(mapOf("quelle-e" to RationaleZahl.von(7))).objekt)
        val sin = FormelLatexCodec.importiereGebunden("sin(x)", listOf(wert("sin"), wert("x")))
        assertEquals(RationaleZahl.von(6), sin.werteAus(mapOf("quelle-sin" to RationaleZahl.von(2), "quelle-x" to RationaleZahl.von(3))).objekt)
        val latexSin = FormelLatexCodec.importiereGebunden("\\sin(x)", listOf(wert("sin"), wert("x")))
        assertEquals(RationaleZahl.von(6), latexSin.werteAus(mapOf("quelle-sin" to RationaleZahl.von(2), "quelle-x" to RationaleZahl.von(3))).objekt)
    }

    @Test fun `nullstellige und verschachtelte Tupel werden strukturell gelesen`() {
        val nullstellig = MethodenSignatur(emptyList(), skalar.ergebnisse)
        assertIs<MethodenZahlKomponente>(FormelLatexCodec.importiereGebunden("f()", listOf(methode("f", nullstellig))).werteAus().objekt)
        val t = FormelLatexCodec.importiereGebunden("((1,2),(),(3,))", emptyList())
        assertEquals(Tupel(listOf(Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))), Tupel(emptyList()), Tupel(listOf(RationaleZahl.von(3))))), t.werteAus().objekt)
        assertEquals(t, StrukturFormelCodec.dekodierenGebunden(StrukturFormelCodec.kodieren(t)))
    }

    @Test fun `methodenwertiges Ergebnis bleibt symbolisch aufrufbar und persistierbar`() {
        val höher = MethodenSignatur(skalar.argumente, listOf(MethodenKomponente("funktion", "funktion", 0, skalar.typAusdruck, skalar)))
        val formel = FormelLatexCodec.importiereGebunden("f(x)(y)", listOf(methode("f", höher), wert("x"), wert("y")))
        val ergebnis = assertIs<MethodenZahlKomponente>(formel.werteAus().objekt)
        assertEquals(skalar, assertIs<MethodenWertKomponente>(ergebnis.aufruf.methode).signatur)
        val gruppiert = FormelLatexCodec.importiereGebunden("(f)(x)(y)", formel.quellen)
        assertIs<MethodenZahlKomponente>(gruppiert.werteAus().objekt)
        assertEquals(formel, FormelKontextCodec.dekodieren(FormelKontextCodec.kodieren(formel)))
        assertEquals(ergebnis, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(ergebnis)))
        assertEquals(formel, (GraphZuFormel.konvertiere(FormelZuGraph.konvertiere(formel)) as GraphZuFormelErgebnis.Erfolg).gebundeneFormel)
        val t = Variable("t", bindungsId = "inner-t")
        val quadrat = Methode("quadrat", listOf(t), Potenz(t, RationaleZahl.von(2)), ReelleZahlen, mapOf("t" to ReelleZahlen))
        val x = Variable("x", bindungsId = "outer-x")
        val konstant = Methode("konstant", listOf(x), quadrat,
            Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)), Tupelraum(listOf(ReelleZahlen))), mapOf("x" to ReelleZahlen))
        assertEquals(RationaleZahl.von(9), formel.werteAus(mapOf("quelle-f" to konstant,
            "quelle-x" to RationaleZahl.Eins, "quelle-y" to RationaleZahl.von(3))).objekt)
        val alsMethode = formel.alsMethode("folge")
        assertEquals(Tupel(listOf(RationaleZahl.von(9))), alsMethode.wendeMitIdentitätenAn(mapOf(
            "quelle-f" to konstant, "quelle-x" to RationaleZahl.Eins, "quelle-y" to RationaleZahl.von(3))).ergebnis)
    }

    @Test fun `nullstellige Bildmenge akzeptiert ausschliesslich leeres Gesamtargument`() {
        val konstant = Methode("f", emptyList(), RationaleZahl.von(5), ReelleZahlen)
        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(5))), bildeAb(EndlicheMenge(setOf(Tupel(emptyList()))), konstant))
        assertEquals(EndlicheMenge(emptySet()), bildeAb(EndlicheMenge(emptySet()), konstant))
        assertFailsWith<IllegalArgumentException> { bildeAb(EndlicheMenge(setOf(RationaleZahl.Eins)), konstant) }
    }

    @Test fun `effektiver Gesamtraum kann den Komponentenraum gezielt erweitern`() {
        val x = Variable("x", bindungsId = "x-id")
        val positiv = ReellesIntervall(RationaleZahl.Null, true, RationaleZahl.von(10), false)
        val m = Methode("id", listOf(x), x, ReelleZahlen, mapOf("x" to positiv),
            effektiverWerteVorrat = Tupelraum(listOf(ReelleZahlen)))
        assertEquals(Tupel(listOf(RationaleZahl.von(-1))), m.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.von(-1)))).ergebnis)
    }

    @Test fun `endliche Bildmenge verliert keine offenen Voraussetzungen`() {
        val x = Variable("x", bindungsId = "x-id")
        val z = Variable("z", bindungsId = "z-id")
        val m = Methode("id", listOf(x), x, ReelleZahlen, mapOf("x" to ReelleZahlen))
        assertIs<Abbild>(bildeAb(EndlicheMenge(setOf(z)), m))
    }
}
