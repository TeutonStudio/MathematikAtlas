package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class FormelBearbeitungTest {
    @Test
    fun `latex importiert bruch mit trigonometrischer funktion strukturiert`() {
        val ergebnis = FormelLatexCodec.importiere("\\frac{x+1}{\\sin(y)}")
        val wurzel = assertIs<FormelLatexImportErgebnis.Erfolg>(ergebnis).ausdruck
        val division = assertIs<FormelAusdruck.Operation>(wurzel)
        assertEquals("zahl.division", division.operatorId)
        assertEquals("zahl.addition", assertIs<FormelAusdruck.Operation>(division.argumente[0].ausdruck).operatorId)
        assertEquals("zahl.sin", assertIs<FormelAusdruck.Operation>(division.argumente[1].ausdruck).operatorId)
        assertEquals("\\frac{x + 1}{\\sin\\left(y\\right)}", FormelLatexCodec.exportiere(wurzel))
    }

    @Test
    fun `import beachtet punkt vor strich und potenz`() {
        val wurzel = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere("a+b\\cdot c^2"),
        ).ausdruck
        val addition = assertIs<FormelAusdruck.Operation>(wurzel)
        assertEquals("zahl.addition", addition.operatorId)
        val multiplikation = assertIs<FormelAusdruck.Operation>(addition.argumente[1].ausdruck)
        assertEquals("zahl.multiplikation", multiplikation.operatorId)
        assertEquals("zahl.potenz", assertIs<FormelAusdruck.Operation>(multiplikation.argumente[1].ausdruck).operatorId)
    }

    @Test
    fun `formeltaste erzeugt operation mit navigierbaren platzhaltern`() {
        val editor = FormelEditorZustand()
        val taste = FormelTastatur.standard.single { it.id == "geteilt" }
        assertTrue(editor.druecke(taste))
        val division = assertIs<FormelAusdruck.Operation>(editor.wurzel)
        assertEquals("zahl.division", division.operatorId)
        assertEquals(listOf("zaehler", "nenner"), division.argumente.map { it.rollenId })
        assertEquals(2, editor.offenePlatzhalter.size)
        assertEquals(editor.offenePlatzhalter.first().id, editor.auswahlId)
    }

    @Test
    fun `editor kann import rueckgaengig machen und wiederholen`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x+1"))
        val importiert = editor.exportiere()
        assertTrue(editor.rueckgaengig())
        assertTrue(editor.exportiere().contains("square"))
        assertTrue(editor.wiederholen())
        assertEquals(importiert, editor.exportiere())
    }

    @Test
    fun `exportierter betrag und logarithmus lassen sich erneut importieren`() {
        val original = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere("|x-1|+\\log_{2}(y)"),
        ).ausdruck
        val exportiert = FormelLatexCodec.exportiere(original)
        val erneut = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere(exportiert),
        ).ausdruck
        assertEquals(FormelLatexCodec.exportiere(original), FormelLatexCodec.exportiere(erneut))
    }

    @Test
    fun `unbekannter latex befehl liefert positionierten fehler`() {
        val fehler = assertIs<FormelLatexImportErgebnis.Fehler>(
            FormelLatexCodec.importiere("x+\\geheim{y}"),
        )
        assertTrue(fehler.position >= 2)
        assertTrue(fehler.nachricht.contains("Nicht unterstützter"))
    }

    @Test
    fun `hyperbolische und reziproke trigonometrische funktionen bleiben eigene operatoren`() {
        val wurzel = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere("sech(x)+cosec(y)+csch(z)"),
        ).ausdruck
        val export = FormelLatexCodec.exportiere(wurzel)
        assertTrue(export.contains("zahl.sech") || export.contains("sech"))
        val ids = sammleOperatorIds(wurzel)
        assertTrue("zahl.sech" in ids)
        assertTrue("zahl.csc" in ids)
        assertTrue("zahl.csch" in ids)
    }

    private fun sammleOperatorIds(ausdruck: FormelAusdruck): Set<String> = buildSet {
        fun besuche(aktuell: FormelAusdruck) {
            if (aktuell is FormelAusdruck.Operation) {
                add(aktuell.operatorId)
                aktuell.argumente.forEach { besuche(it.ausdruck) }
            }
        }
        besuche(ausdruck)
    }
}
