package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class FormelFunktionspotenzen410Test {
    @Test
    fun `iterierte Komposition nutzt Winkelklammerpotenz und roundtrip`() {
        val importiert = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere("f^{\\langle 3\\rangle}"),
        ).ausdruck
        val operation = assertIs<FormelAusdruck.Operation>(importiert)

        assertEquals("iteration.selbstkomposition", operation.operatorId)
        assertEquals("f^{\\langle 3\\rangle}", FormelLatexCodec.exportiere(operation))
        val erneut = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere(FormelLatexCodec.exportiere(operation)),
        ).ausdruck
        assertEquals("iteration.selbstkomposition", assertIs<FormelAusdruck.Operation>(erneut).operatorId)
    }

    @Test
    fun `Umkehrfunktion ist kein Kehrwert und roundtrip`() {
        val importiert = assertIs<FormelLatexImportErgebnis.Erfolg>(
            FormelLatexCodec.importiere("f^{\\langle -1\\rangle}"),
        ).ausdruck
        val operation = assertIs<FormelAusdruck.Operation>(importiert)

        assertEquals("methode.umkehrfunktion", operation.operatorId)
        assertEquals(1, operation.argumente.size)
        assertEquals("methode", operation.argumente.single().rollenId)
        assertEquals("f^{\\langle -1\\rangle}", FormelLatexCodec.exportiere(operation))
        assertNotEquals("zahl.kehrwert", operation.operatorId)
    }

    @Test
    fun `CAS Tasten tragen die kanonische LaTeX Beschriftung`() {
        val iteration = FormelTastatur.standard.single { it.id == "selbstkomposition" }
        val inverse = FormelTastatur.standard.single { it.id == "umkehrfunktion" }

        assertEquals("f^{\\langle n\\rangle}", iteration.beschriftungLatex)
        assertEquals("iteration.selbstkomposition", iteration.operatorId)
        assertEquals("f^{\\langle -1\\rangle}", inverse.beschriftungLatex)
        assertEquals("methode.umkehrfunktion", inverse.operatorId)
        assertEquals(FormelTyp.METHODE, inverse.ergebnisTyp)
    }
}
