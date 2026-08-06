package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PotenzDienstTest {
    @Test
    fun `Zahlpotenz wird automatisch ueber Zahlbereichsstruktur ausgewertet`() {
        val ergebnis = assertIs<PotenzDienstErgebnis.ObjektWert>(
            PotenzDienst.werteAus(
                basis = RationaleZahl.von(3),
                ordnung = IterationsOrdnung.Konkret(4),
            ),
        )

        assertEquals(RationaleZahl.von(81), ergebnis.wert)
        assertEquals("potenz.zahlbereich.N", ergebnis.strukturId)
    }

    @Test
    fun `Matrixpotenz verwendet denselben Dienst und Matrixprodukt`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val ergebnis = assertIs<PotenzDienstErgebnis.ObjektWert>(
            PotenzDienst.werteAus(matrix, IterationsOrdnung.Konkret(3)),
        )

        assertEquals(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(8), RationaleZahl.Null),
                    listOf(RationaleZahl.Null, RationaleZahl.von(27)),
                ),
            ),
            ergebnis.wert,
        )
        assertTrue(ergebnis.strukturId.startsWith("potenz.matrix.2x2"))
    }

    @Test
    fun `Methodenpotenz bleibt punktweise und behaelt Signatur`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val ergebnis = assertIs<PotenzDienstErgebnis.MethodenWert>(
            PotenzDienst.werteAus(methode, IterationsOrdnung.Konkret(2)),
        )

        assertEquals(methode.werteVorräte, ergebnis.methode.werteVorräte)
        assertEquals(methode.zielMenge, ergebnis.methode.zielMenge)
        assertEquals("{f}^{2}", ergebnis.methode.name)
    }

    @Test
    fun `Tupel verlangt explizite Produktstruktur`() {
        val ergebnis = assertIs<PotenzDienstErgebnis.Ungueltig>(
            PotenzDienst.werteAus(
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                IterationsOrdnung.Konkret(2),
            ),
        )

        assertEquals("potenzstruktur_nicht_eindeutig", ergebnis.code)
        assertTrue(ergebnis.grund.contains("hadamard"))
    }

    @Test
    fun `explizite Struktur mit unbekanntem Laufzeitoperator wird transparent abgelehnt`() {
        val struktur = PotenzStruktur(
            id = "test.explizit",
            traeger = PotenzTraeger.Explizit(BenannteMenge("M")),
            multiplikationsOperatorId = "test.mul",
            abgeschlossenheit = NachweisStatus.Nachgewiesen,
            assoziativitaet = NachweisStatus.Nachgewiesen,
            neutralesElement = AllgemeinerParameter("e"),
            neutralitaet = NachweisStatus.Nachgewiesen,
        )
        val ergebnis = assertIs<PotenzDienstErgebnis.Ungueltig>(
            PotenzDienst.werteAus(
                basis = AllgemeinerParameter("a"),
                ordnung = IterationsOrdnung.Konkret(2),
                expliziteStruktur = struktur,
            ),
        )

        assertEquals("potenzstruktur_fehlt", ergebnis.code)
        assertTrue(ergebnis.grund.contains("test.mul"))
    }
}
