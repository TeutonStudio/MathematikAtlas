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
        assertIs<CursorPosition.InPlatzhalter>(editor.cursor.position)
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

    @Test
    fun `links und rechts durchlaufen semantische Cursorziele ohne Undo Eintrag`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x+1"))
        val nachImportKannRueckgaengig = editor.kannRueckgaengig
        val start = editor.cursor

        assertTrue(editor.bewegeCursor(FormelCursorRichtung.Links))
        assertNotEquals(start, editor.cursor)
        assertEquals(nachImportKannRueckgaengig, editor.kannRueckgaengig)
        assertTrue(editor.bewegeCursor(FormelCursorRichtung.Rechts))
        assertEquals(start, editor.cursor)
    }

    @Test
    fun `oben und unten wechseln zwischen Zaehler und Nenner`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("\\frac{x+1}{y-2}"))
        val division = assertIs<FormelAusdruck.Operation>(editor.wurzel)
        val zaehler = division.argumente.sortedBy { it.position }[0].ausdruck
        val nenner = division.argumente.sortedBy { it.position }[1].ausdruck
        val zaehlerKind = assertIs<FormelAusdruck.Operation>(zaehler).argumente.first().ausdruck
        assertTrue(editor.setzeCursorAufAusdruck(zaehlerKind.id, CursorPosition.NachAusdruck))

        assertTrue(editor.bewegeCursor(FormelCursorRichtung.Unten))
        assertTrue(editor.cursor.pfad.ids.contains(nenner.id))
        assertTrue(editor.bewegeCursor(FormelCursorRichtung.Oben))
        assertTrue(editor.cursor.pfad.ids.contains(zaehler.id))
    }

    @Test
    fun `Operator vor Ausdruck setzt bestehenden Ausdruck rechts ein`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x"))
        val xId = editor.wurzel.id
        assertTrue(editor.setzeCursorAufAusdruck(xId, CursorPosition.VorAusdruck))
        val plus = FormelTastatur.standard.single { it.id == "plus" }

        assertTrue(editor.druecke(plus))

        val addition = assertIs<FormelAusdruck.Operation>(editor.wurzel)
        assertIs<FormelAusdruck.Platzhalter>(addition.argumente.sortedBy { it.position }[0].ausdruck)
        assertEquals(xId, addition.argumente.sortedBy { it.position }[1].ausdruck.id)
    }

    @Test
    fun `Operator nach Ausdruck setzt bestehenden Ausdruck links ein`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x"))
        val xId = editor.wurzel.id
        assertTrue(editor.setzeCursorAufAusdruck(xId, CursorPosition.NachAusdruck))
        val plus = FormelTastatur.standard.single { it.id == "plus" }

        assertTrue(editor.druecke(plus))

        val addition = assertIs<FormelAusdruck.Operation>(editor.wurzel)
        assertEquals(xId, addition.argumente.sortedBy { it.position }[0].ausdruck.id)
        assertIs<FormelAusdruck.Platzhalter>(addition.argumente.sortedBy { it.position }[1].ausdruck)
    }

    @Test
    fun `Undo und Redo stellen Ausdruck und Cursorposition wieder her`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x"))
        val xId = editor.wurzel.id
        assertTrue(editor.setzeCursorAufAusdruck(xId, CursorPosition.VorAusdruck))
        val cursorVorMutation = editor.cursor
        val plus = FormelTastatur.standard.single { it.id == "plus" }
        assertTrue(editor.druecke(plus))
        val cursorNachMutation = editor.cursor

        assertTrue(editor.rueckgaengig())
        assertEquals(cursorVorMutation, editor.cursor)
        assertTrue(editor.wiederholen())
        assertEquals(cursorNachMutation, editor.cursor)
    }

    @Test
    fun `Backspace hinterlaesst erreichbaren Pflichtplatzhalter`() {
        val editor = FormelEditorZustand()
        assertIs<FormelLatexImportErgebnis.Erfolg>(editor.importiere("x+1"))
        val addition = assertIs<FormelAusdruck.Operation>(editor.wurzel)
        val rechts = addition.argumente.sortedBy { it.position }[1].ausdruck
        assertTrue(editor.setzeCursorAufAusdruck(rechts.id, CursorPosition.NachAusdruck))

        assertTrue(editor.loescheRueckwaerts())

        assertIs<FormelAusdruck.Platzhalter>(
            assertIs<FormelAusdruck.Operation>(editor.wurzel).argumente.sortedBy { it.position }[1].ausdruck,
        )
        assertIs<CursorPosition.InPlatzhalter>(editor.cursor.position)
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
