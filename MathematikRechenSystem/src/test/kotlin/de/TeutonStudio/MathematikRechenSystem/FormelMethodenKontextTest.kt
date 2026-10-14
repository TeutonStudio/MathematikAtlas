package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class FormelMethodenKontextTest {
    private val r = ReelleZahlen.elementTypAusdruck()
    private val signatur = MethodenSignatur(listOf(MethodenKomponente("t", "t", 0, r)), listOf(MethodenKomponente("wert", "wert", 0, r)))
    private val quellen = listOf(
        FormelVariablenQuelle("quelle-f", "f", FormelVariablenArt.FREI, FormelTyp.METHODE,
            Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)), Tupelraum(listOf(ReelleZahlen))),
            signatur.typAusdruck, methodenSignatur = signatur),
        FormelVariablenQuelle("quelle-x", "x", FormelVariablenArt.FREI, FormelTyp.ZAHL, ReelleZahlen),
        FormelVariablenQuelle("quelle-h", "h", FormelVariablenArt.FREI, FormelTyp.ZAHL, ReelleZahlen),
    )
    @Test fun `freie Methodennamen werden nach Deklaration aufgerufen`() {
        val q = quellen[0].copy(anzeigeName = "beliebig")
        val formel = FormelLatexCodec.importiereGebunden("beliebig(x+h)", listOf(q) + quellen.drop(1))
        val ergebnis = formel.werteAus()
        assertIs<MethodenZahlKomponente>(ergebnis.objekt)
        assertEquals("quelle-f", (ergebnis.objekt as MethodenZahlKomponente).aufruf.methode.let { (it as MethodenVariable).bindungsId })
    }
    @Test fun `Differenzenquotient erhaelt Methodenargument und Nichtnullbedingung`() {
        val formel = FormelLatexCodec.importiereGebunden("\\frac{f(x+h)-f(x)}{h}", quellen)
        val methode = formel.alsMethode("Q")
        assertIs<MethodenVariable>(methode.parameter[0])
        val t = Variable("t")
        val quadrat = MathematischeMethode("quadrat", listOf(t), Potenz(t, RationaleZahl.von(2)), ReelleZahlen, mapOf("t" to ReelleZahlen))
        val ergebnis = methode.wendeMitIdentitätenAn(mapOf("quelle-f" to quadrat, "quelle-x" to RationaleZahl.von(3), "quelle-h" to RationaleZahl.von(2)))
        assertEquals(Tupel(listOf(RationaleZahl.von(8))), ergebnis.ergebnis)
        assertFailsWith<IllegalArgumentException> {
            methode.wendeMitIdentitätenAn(mapOf("quelle-f" to quadrat, "quelle-x" to RationaleZahl.von(3), "quelle-h" to RationaleZahl.Null))
        }
        assertEquals(methode, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(methode)))
    }
    @Test fun `LaTeX und Kontext erhalten saemtliche Quellen und Annahmen`() {
        val h = Variable("h", bindungsId = "quelle-h")
        val formel = FormelLatexCodec.importiereGebunden("f(x+h)", quellen).copy(annahmen = setOf(Ungleichheit(h, RationaleZahl.Null)))
        assertEquals(formel, FormelKontextCodec.dekodieren(FormelKontextCodec.kodieren(formel)))
        val graph = FormelZuGraph.konvertiere(formel)
        assertEquals(formel, (GraphZuFormel.konvertiere(graph) as GraphZuFormelErgebnis.Erfolg).gebundeneFormel)
        assertEquals(formel, StrukturFormelCodec.dekodierenGebunden(StrukturFormelCodec.kodieren(formel)))
        val paket = FormelLatexCodec.exportiere(formel)
        assertEquals(formel, FormelLatexCodec.importiere(paket))
        assertFailsWith<IllegalArgumentException> { FormelLatexCodec.importiere(paket.copy(latex = "1")) }
    }
    @Test fun `mehrdimensionale Komposition erhaelt Tupelergebnis`() {
        val x = Variable("x"); val y = Variable("y")
        val a = Variable("a"); val b = Variable("b"); val c = Variable("c")
        val innen = MathematischeMethode("f", listOf(x, y), Tupel(listOf(x, y, addition(x, y))), Tupelraum(List(3) { ReelleZahlen }), mapOf("x" to ReelleZahlen, "y" to ReelleZahlen), listOf("u", "v", "w"))
        val außen = MathematischeMethode("g", listOf(a, b, c), Tupel(listOf(addition(a, b), c)), Tupelraum(List(2) { ReelleZahlen }), mapOf("a" to ReelleZahlen, "b" to ReelleZahlen, "c" to ReelleZahlen), listOf("s", "t"))
        val komposition = komponiere(listOf(außen, innen)) as MathematischeMethode
        assertEquals(innen.signatur.argumente.map { it.id }, komposition.signatur.argumente.map { it.id })
        assertEquals(Tupel(listOf(RationaleZahl.von(5), RationaleZahl.von(5))), komposition.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))).ergebnis)
    }
}
