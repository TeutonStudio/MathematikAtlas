package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MethodenFundamentTest {
    @Test
    fun `wertevorrat wird als geordneter tupelraum abgeleitet`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to addition(listOf(x, y))),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = linkedMapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
        )

        val signatur = methode.mathematischeMethodenSignatur()
        assertEquals(listOf("x", "y"), signatur.argumente.map { it.name })
        assertEquals(Tupelraum(listOf(ReelleZahlen, GanzeZahlen)), signatur.definitionsRaum)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), signatur.zielRaum)
    }

    @Test
    fun `nullstellige methode verwendet leeren tupelraum statt leere menge`() {
        val methode = Methode(
            name = "c",
            parameter = emptyList(),
            ausgaben = mapOf("wert" to RationaleZahl.Eins),
            zielMengen = mapOf("wert" to ReelleZahlen),
        )

        val signatur = methode.mathematischeMethodenSignatur()
        assertEquals(Tupelraum(emptyList()), signatur.definitionsRaum)
        assertNotEquals(LeereMenge, signatur.definitionsRaum)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), signatur.zielRaum)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), methode.wendeKanonischAn(Tupel(emptyList())))
    }

    @Test
    fun `einstellige methode kollabiert weder argument noch ergebnis`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to GanzeZahlen),
        )

        val mathematisch = methode.mathematischeMethodenSignatur()
        val neutral = methode.neutraleMethodenSignatur()
        assertEquals(Tupelraum(listOf(GanzeZahlen)), mathematisch.definitionsRaum)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), mathematisch.zielRaum)
        assertEquals(1, neutral.argumentTupelTyp.argumente.size)
        assertEquals(1, neutral.ergebnisTupelTyp.argumente.size)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), methode.wendeKanonischAn(Tupel(listOf(RationaleZahl.Eins))))
    }

    @Test
    fun `historische mehrfachausgaben sind ein ergebnistupel`() {
        val methode = Methode(
            name = "paar",
            parameter = emptyList(),
            ausgaben = linkedMapOf("links" to RationaleZahl.Null, "rechts" to RationaleZahl.Eins),
            zielMengen = linkedMapOf("links" to ReelleZahlen, "rechts" to ReelleZahlen),
        )

        assertIs<Tupel>(methode.vorschrift)
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), methode.zielMenge)
        assertEquals(Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)), methode.wendeKanonischAn(Tupel(emptyList())))
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), methode.mathematischeMethodenSignatur().zielRaum)
    }

    @Test
    fun `aliase werden nur aus der methodensemantik berechnet`() {
        val x = Variable("x")
        val funktion = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val menge = MengenParameter("A")
        val abbildung = Methode(
            name = "P",
            parameter = listOf(menge),
            ausgaben = mapOf("wert" to Potenzmenge(menge)),
            zielMengen = mapOf("wert" to BenannteMenge("mengenfamilien", "\\mathfrak{M}")),
            werteVorräte = mapOf(menge.name to BenannteMenge("mengen", "\\mathfrak{M}")),
        )
        val aussage = AussagenParameter("A")
        val prädikat = Methode(
            name = "Q",
            parameter = listOf(aussage),
            ausgaben = mapOf("aussage" to aussage),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(aussage.name to WahrheitsMenge),
        )

        assertTrue(MethodenAlias.Funktion in funktion.aliase())
        assertFalse(MethodenAlias.Prädikat in funktion.aliase())
        assertTrue(MethodenAlias.Abbildung in abbildung.aliase())
        assertTrue(MethodenAlias.Prädikat in prädikat.aliase())
        assertEquals("Methode · Prädikat", prädikat.aliasAnzeige())
    }

    @Test
    fun `legacy einzelziel bleibt komponentenprojektion waehrend zielraum tupel bleibt`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(addition(RationaleZahl.von(2), RationaleZahl.Eins), methode.wendeAn(listOf(RationaleZahl.von(2))))
        assertEquals(ReelleZahlen, methode.zielMenge)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.mathematischeMethodenSignatur().zielRaum)
        assertEquals(listOf("wert"), methode.ausgabeNamen)
    }

    @Test
    fun `mehrere kartenausgaenge bleiben ein geordnetes ergebnistupel`() {
        val methode = Methode(
            name = "paar",
            parameter = emptyList(),
            vorschrift = Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)),
            zielMenge = Tupelraum(listOf(GanzeZahlen, GanzeZahlen)),
            ausgabeNamen = listOf("links", "rechts"),
        )

        assertEquals(Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)), methode.wendeAn(emptyList()))
        assertEquals(RationaleZahl.Null, methode.vorschriftFür("links"))
        assertEquals(GanzeZahlen, methode.zielMengeFür("rechts"))
        assertEquals(Tupelraum(listOf(GanzeZahlen, GanzeZahlen)), methode.mathematischeMethodenSignatur().zielRaum)
    }

    @Test
    fun `methode rendert kanonische raeume und tupelterm gemeinsam in cases Umgebung`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
        )

        val latex = methode.zuLatex()
        assertTrue(latex.startsWith("f:\\begin{cases}"))
        assertTrue(latex.contains("\\mathbb{R} \\times \\mathbb{Z} \\longrightarrow \\operatorname{Tupelraum}"))
        assertTrue(latex.contains("\\left(x,y\\right) \\mapsto \\left(x + y\\right)"))
        assertTrue(latex.endsWith("\\end{cases}"))
    }

    @Test
    fun `symbolische totale Ableitungsfunktion besitzt f Strich und ausgewertete Termzeile`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        val ableitung = differenziereMethodeStrukturiert(
            methode,
            DifferentialOrdnung.Konkret(1),
            DifferentialOperator.Total,
        ).methode
        val latex = ableitung.alsMathematischeMethode("Methodendarstellung").zuLatex()

        assertEquals("f'", ableitung.name)
        assertTrue(latex.startsWith("f':\\begin{cases}"))
        assertTrue(latex.contains("f'\\left(x,y\\right)"))
        assertTrue(latex.contains("\\mathcal L"))
    }
}
