package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class VorzeichenUndTensorraumTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private val a = Variable("a")
    private val b = Variable("b")
    private val c = Variable("c")

    @Test fun `negative Summanden und Faktoren verwenden Minus mit korrekten Klammern`() {
        assertEquals("a - b", addition(a, multiplikation(z(-1), b)).zuLatex())
        assertEquals("a - 3", addition(a, z(-3)).zuLatex())
        assertEquals("a - 2 \\cdot b", addition(a, multiplikation(z(-2), b)).zuLatex())
        assertEquals("a - \\left(b + c\\right)", addition(a, multiplikation(z(-1), addition(b,c))).zuLatex())
        assertEquals("a - \\left(b - c\\right)", addition(a, multiplikation(z(-1), addition(b,multiplikation(z(-1),c)))).zuLatex())
        assertEquals("-b - c", addition(multiplikation(z(-1),b),multiplikation(z(-1),c)).zuLatex())
        assertEquals("a - \\frac{b}{2}", addition(a,Division(b,z(-2))).zuLatex())
        assertEquals("a + b", addition(a,multiplikation(z(-1),z(-1),b)).zuLatex())
        assertEquals("{\\left(-2\\right)}^{2}", Potenz(z(-2),z(2)).zuLatex())
    }

    @Test fun `nur nachgewiesene negative Summanden werden kontextbezogen subtrahiert`() {
        val summe = addition(a,b)
        assertEquals("a + b", summe.zuLatex(RechenKontext()))
        val bedingung = Vergleich(b,VergleichsArt.Kleiner,z(0))
        assertEquals("a - \\left|b\\right|", summe.zuLatex(RechenKontext(annahmen=setOf(bedingung))))
    }

    @Test fun `Formelcodec erhaelt Klammern einer subtrahierten Summe`() {
        val import = assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere("a+(-(b+c))"))
        val latex = FormelLatexCodec.exportiere(import.ausdruck)
        assertFalse(" + " in latex.substringBefore("b"), latex)
        assertContains(latex, " - ")
        assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere(latex))
        val doppelt = assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere("a+(-(-b))"))
        val vereinfacht = FormelLatexCodec.exportiere(doppelt.ausdruck)
        assertFalse("-" in vereinfacht,vereinfacht)
        assertFalse("0" in vereinfacht,vereinfacht)
    }

    @Test fun `Formelrendern erhaelt Vorzeichenbedingungen IDs und negative Potenzbasen`() {
        fun lit(id:String,n:Long) = FormelAusdruck.Literal(id,z(n),FormelTyp.ZAHL)
        val negativ = FormelAusdruck.Operation("neg","zahl.subtraktion", listOf(FormelArgument("a",0,lit("zero",0)),FormelArgument("b",1,lit("two",2))),FormelTyp.ZAHL,bedingungen=listOf(WahrheitsKonstante(true)))
        val summe = FormelAusdruck.Operation("sum","zahl.addition",listOf(FormelArgument("a",0,lit("one",1)),FormelArgument("b",1,negativ)),FormelTyp.ZAHL)
        val render = ErweiterterFormelRenderer.render(summe)
        assertContains(render.bedingungen,WahrheitsKonstante(true))
        assertTrue(render.ausdrucksBereiche.keys.containsAll(listOf("sum","neg","zero","two","one")))
        val potenz = FormelAusdruck.Operation("pow","zahl.potenz",listOf(FormelArgument("basis",0,lit("minus",-2)),FormelArgument("exponent",1,lit("exp",2))),FormelTyp.ZAHL)
        assertEquals("\\left(-2\\right)^{2}",FormelLatexCodec.exportiere(potenz))
        assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere(FormelLatexCodec.exportiere(potenz)))
    }

    @Test fun `Vorzeichenroundtrip erhaelt numerische Bedeutung und Tupelnullraum`() {
        for (quelle in listOf("7+(-3)", "7+(-(3+2))", "7+(-(3-2))", "(-2)^2", "7+(-3)/2", "7-(-3)")) {
            val a = assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere(quelle)).ausdruck
            val latex = FormelLatexCodec.exportiere(a)
            val b = assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere(latex)).ausdruck
            val vorher = GebundeneFormel(a,emptyList(),emptyMap()).werteAus(operation = { op, args ->
                assertEquals("iteration.multiplikation",op.operatorId)
                FormelKernErgebnis(Potenz(args[0] as ZahlAusdruck,args[1] as ZahlAusdruck),emptySet())
            }).objekt as ZahlAusdruck
            val nachher = GebundeneFormel(b,emptyList(),emptyMap()).werteAus(operation = { op, args ->
                assertEquals("iteration.multiplikation",op.operatorId)
                FormelKernErgebnis(Potenz(args[0] as ZahlAusdruck,args[1] as ZahlAusdruck),emptySet())
            }).objekt as ZahlAusdruck
            assertEquals(vereinfache(vorher),vereinfache(nachher),latex)
        }
        val nullraum = kartesischePotenz(ReelleZahlen,z(0))
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(Tupel(emptyList()),nullraum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(Tupel(listOf(z(1))),nullraum).entscheide().wahrheitswert)
    }

    @Test fun `Tensorraeume pruefen Achsen und Komponenten fuer Tensoren und Matrizen`() {
        val raum = Tensorraum(ReelleZahlen,listOf(z(1),z(2)))
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Matrix(listOf(listOf(z(1),z(2)))),raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(Tensor(listOf(1,2),listOf(z(1),z(2))),raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Tensor(listOf(2,1),listOf(z(1),z(2))),raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Tupel(listOf(z(1),z(2))),raum).entscheide().wahrheitswert)
        for (rang in listOf(1,3,7)) {
            val r = Tensorraum(ReelleZahlen,List(rang) { z(1) })
            assertEquals(Wahrheitswert.Wahr,ElementBeziehung(Tensor(List(rang) { 1 },listOf(z(3))),r).entscheide().wahrheitswert)
            assertEquals(r,MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(r)))
        }
        assertFails { Tensorraum(ReelleZahlen,emptyList()) }
        assertFails { Tensorraum(ReelleZahlen,listOf(z(0))) }
    }
}
