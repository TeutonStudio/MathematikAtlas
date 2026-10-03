package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MethodenFundamentTest {
    @Test
    fun `definitionsmengen werden als geordneter tupelraum abgeleitet`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to addition(listOf(x, y))),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = linkedMapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
        )

        val signatur = methode.methodenSignatur()
        val mathematisch = methode.mathematischeMethodenSignatur()
        assertEquals(listOf("x", "y"), signatur.argumente.map { it.name })
        assertEquals(Tupelraum(listOf(ReelleZahlen, GanzeZahlen)), mathematisch.kanonischerArgumentRaum)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), mathematisch.zielRaum)
    }

    @Test
    fun `nullstellige methode verwendet leeren tupelraum und nicht leere menge`() {
        val methode = Methode(
            name = "c",
            parameter = emptyList(),
            vorschrift = RationaleZahl.Eins,
            zielMenge = ReelleZahlen,
        )

        val mathematisch = methode.mathematischeMethodenSignatur()
        assertEquals(Tupelraum(emptyList()), mathematisch.definitionsRaum)
        assertFalse(mathematisch.definitionsRaum == LeereMenge)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), mathematisch.zielRaum)
        assertEquals(RationaleZahl.Eins, methode.wendeKanonischAn(emptyMap()))
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), methode.wendeMathematischAlsTupelAn(Tupel(emptyList())))
    }

    @Test
    fun `einstellige methode kollabiert definitionsraum nicht zur komponentenmenge`() {
        val x = Variable("x")
        val methode = Methode(
            name = "id",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.mathematischeMethodenSignatur().definitionsRaum)
        assertFalse(methode.mathematischeMethodenSignatur().definitionsRaum == ReelleZahlen)
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
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), methode.zielRaum)
        assertEquals(Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)), methode.wendeKanonischAn(emptyMap()))
    }

    @Test
    fun `aliase werden nur aus der methodensemantik berechnet`() {
        val x = Variable("x")
        val funktion = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val menge = MengenParameter("A")
        val abbildung = Methode(
            name = "P",
            parameter = listOf(menge),
            vorschrift = Potenzmenge(menge),
            zielMenge = BenannteMenge("mengenfamilien", "\\mathfrak{M}"),
            werteVorräte = mapOf(menge.name to BenannteMenge("mengen", "\\mathfrak{M}")),
        )
        val aussage = AussagenParameter("A")
        val prädikat = Methode(
            name = "Q",
            parameter = listOf(aussage),
            vorschrift = aussage,
            zielMenge = WahrheitsMenge,
            werteVorräte = mapOf(aussage.name to WahrheitsMenge),
            ausgabeNamen = listOf("aussage"),
        )

        assertTrue(MethodenAlias.Funktion in funktion.aliase())
        assertFalse(MethodenAlias.Prädikat in funktion.aliase())
        assertTrue(MethodenAlias.Abbildung in abbildung.aliase())
        assertTrue(MethodenAlias.Prädikat in prädikat.aliase())
        assertEquals("Methode · Prädikat", prädikat.aliasAnzeige())
    }

    @Test
    fun `einzelausgabe besitzt intern einertupel und einertupel zielraum`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(Tupel(listOf(addition(x, RationaleZahl.Eins))), methode.ergebnisTupel)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.zielRaum)
        assertEquals(addition(RationaleZahl.von(2), RationaleZahl.Eins), methode.wendeAn(listOf(RationaleZahl.von(2))))
        assertEquals(ReelleZahlen, methode.zielMengeFür("wert"))
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
        assertEquals(Tupelraum(listOf(GanzeZahlen, GanzeZahlen)), methode.mathematischeSignatur.zielRaum)
    }

    @Test
    fun `methode rendert kanonischen raumvertrag und term gemeinsam`() {
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
        assertTrue(latex.contains("\\mathbb{R} \\times \\mathbb{Z} \\longrightarrow"))
        assertTrue(latex.contains("\\operatorname{Tupelraum}"))
        assertTrue(latex.contains("\\left(x,y\\right) \\mapsto x + y"))
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
        val latex = ableitung.zuLatex()

        assertEquals("f'", ableitung.name)
        assertTrue(latex.startsWith("f':\\begin{cases}"))
        assertTrue(latex.contains("\\left(x,y\\right) \\mapsto f'\\left(x,y\\right)"))
        assertTrue(latex.contains("\\mathcal L"))
    }

    @Test
    fun `fremde scriptmethode benoetigt weder mathematikobjekt noch mengen`() {
        val float = TypAusdruck.Atom(TypId("engine.float"))
        val node3d = TypAusdruck.Atom(TypId("engine.node3d"))
        val vector3 = TypAusdruck.Atom(TypId("engine.vector3"))
        val methode = TestScriptMethod(
            MethodenSignatur(
                argumente = listOf(
                    MethodenKomponente("owner", "owner", 0, node3d),
                    MethodenKomponente("delta", "delta", 1, float),
                ),
                ergebnisse = listOf(MethodenKomponente("position", "position", 0, vector3)),
            ),
        )

        assertFalse((methode as Any) is MathematischesObjekt)
        assertFalse((methode as Any) is MathematischeSignaturtragendeMethode)
        assertEquals(
            TypAusdruck.Parameterisiert(
                MathematischeTypen.Methode,
                listOf(
                    TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(node3d, float)),
                    TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(vector3)),
                ),
            ),
            methode.methodenTypAusdruck(),
        )
    }

    @Test
    fun `neutrale signatur unterstuetzt null ein und n ergebnisse ohne kollaps`() {
        val wert = TypAusdruck.Atom(TypId("engine.wert"))
        val nullErgebnisse = MethodenSignatur(emptyList(), emptyList())
        val einErgebnis = MethodenSignatur(emptyList(), listOf(MethodenKomponente("r", "r", 0, wert)))
        val mehrere = MethodenSignatur(
            emptyList(),
            listOf(
                MethodenKomponente("r0", "r0", 0, wert),
                MethodenKomponente("r1", "r1", 1, wert),
            ),
        )

        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, emptyList()), nullErgebnisse.ergebnisTyp)
        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(wert)), einErgebnis.ergebnisTyp)
        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(wert, wert)), mehrere.ergebnisTyp)
    }

    @Test
    fun `restriktion aendert definitionsraum aber nicht zielraum`() {
        val x = Variable("x")
        val basisMenge = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val teilMenge = EndlicheMenge(setOf(RationaleZahl.Null))
        val basis = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to basisMenge),
        )

        val restriktion = assertNotNull(restriktiereMethode(basis, teilMenge).methode)
        assertEquals(Tupelraum(listOf(teilMenge)), restriktion.mathematischeSignatur.definitionsRaum)
        assertEquals(basis.mathematischeSignatur.zielRaum, restriktion.mathematischeSignatur.zielRaum)
        assertEquals(null, restriktion.bereichsanpassung)
    }

    @Test
    fun `komposition akzeptiert nachgewiesene bildteilmenge ohne zielmengengleichheit`() {
        val x = Variable("x")
        val t = Variable("t")
        val nullMenge = EndlicheMenge(setOf(RationaleZahl.Null))
        val nullEins = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val innen = Methode(
            name = "f",
            parameter = listOf(t),
            vorschrift = RationaleZahl.Null,
            zielMenge = nullEins,
            werteVorräte = mapOf(t.name to nullEins),
        )
        val außen = Methode(
            name = "g",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to nullMenge),
        )

        val prüfung = prüfeMathematischeKomposition(außen, innen)
        assertEquals(Wahrheitswert.Wahr, prüfung.bildPrüfung.wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, prüfung.zielraumPrüfung.wahrheitswert)
        assertNotNull(komponiere(außen, innen))
    }

    @Test
    fun `scriptmethodenkomposition prueft ausschliesslich neutrale typkompatibilitaet`() {
        val float = TypAusdruck.Atom(TypId("engine.float"))
        val innen = TestScriptMethod(
            MethodenSignatur(
                argumente = emptyList(),
                ergebnisse = listOf(MethodenKomponente("r", "r", 0, float)),
            ),
        )
        val außen = TestScriptMethod(
            MethodenSignatur(
                argumente = listOf(MethodenKomponente("x", "x", 0, float)),
                ergebnisse = emptyList(),
            ),
        )

        assertTrue(prüfeMethodenTypKomposition(außen, innen).kompatibel)
    }

    private class TestScriptMethod(
        override val signatur: MethodenSignatur,
    ) : SignaturtragendeMethode {
        override val name: String = "script"
    }
}
