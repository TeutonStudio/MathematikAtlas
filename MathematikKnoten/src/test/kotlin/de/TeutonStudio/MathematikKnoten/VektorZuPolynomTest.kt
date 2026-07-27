package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VektorZuPolynomTest {
    @Test fun `Vorlage und Auswerter akzeptieren beide Vektororientierungen`() {
        val knoten = MathematikKnotenVorlagen.VektorZuPolynom.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val koeffizienten = listOf(RationaleZahl.von(2), RationaleZahl.von(-3), RationaleZahl.von(5))

        val spalte = auswerter.auswerten(KnotenAuswertungsKontext(knoten, mapOf("vektor" to BedingterWert(SpaltenVektor(koeffizienten))), RechenKontext()))
        val zeile = auswerter.auswerten(KnotenAuswertungsKontext(knoten, mapOf("vektor" to BedingterWert(ZeilenVektor(koeffizienten))), RechenKontext()))
        val ohnePersistierteVariable = auswerter.auswerten(KnotenAuswertungsKontext(knoten.copy(parameter = emptyMap()), mapOf("vektor" to BedingterWert(SpaltenVektor(koeffizienten))), RechenKontext()))

        assertEquals("mathematik.vektorZuPolynom", knoten.art)
        assertEquals("x", knoten.parameter.getValue("variable"))
        assertEquals(listOf("vektor", "wert"), knoten.anschlüsse.map { it.name })
        assertTrue(MathematikKnotenVorlagen.VektorZuPolynom in MathematikKnotenVorlagen.alle)
        assertEquals("5 \\cdot {x}^{2} + -3 \\cdot x + 2", spalte.ausgaben.getValue("wert").objekt.zuLatex())
        assertEquals(spalte.ausgaben.getValue("wert").objekt, zeile.ausgaben.getValue("wert").objekt)
        assertEquals(spalte.ausgaben.getValue("wert").objekt, ohnePersistierteVariable.ausgaben.getValue("wert").objekt)
    }

    @Test fun `Vektoreingang hat den gemeinsamen Vektortyp`() {
        val ziel = MathematikKnotenVorlagen.VektorZuPolynom.erzeuge(GraphPunkt.Zero)
        val spalte = MathematikKnotenVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val zeile = MathematikKnotenVorlagen.ZeilenVektor.erzeuge(GraphPunkt.Zero)
        val matrix = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        val eingang = ziel.anschlüsse.single { it.name == "vektor" }

        fun prüfe(quellKnoten: KnotenDaten, ausgangName: String) = prüfung.prüfe(
            KartenDaten(name = "Test", knoten = listOf(quellKnoten, ziel)),
            AnschlussVerweis(quellKnoten.id, quellKnoten.anschlüsse.single { it.name == ausgangName }.id),
            AnschlussVerweis(ziel.id, eingang.id),
        )

        assertEquals(VerbindungsPrüfung.Erlaubt, prüfe(spalte, "vektor"))
        assertEquals(VerbindungsPrüfung.Erlaubt, prüfe(zeile, "vektor"))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfe(matrix, "matrix"))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfe(tupel, "tupel"))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfe(zahl, "wert"))
    }

    @Test fun `fehlender Vektor und leere Variable sind sichtbare Auswertungsfehler`() {
        val basis = MathematikKnotenVorlagen.VektorZuPolynom.erzeuge(GraphPunkt.Zero)
        val auswerter = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister())

        val ohneVektor = auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(basis))).knoten.getValue(basis.id)
        val quelle = MathematikKnotenVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val ohneVariableKnoten = basis.copy(parameter = mapOf("variable" to "  "))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single { it.name == "vektor" }.id),
            zu = AnschlussVerweis(ohneVariableKnoten.id, ohneVariableKnoten.anschlüsse.single { it.name == "vektor" }.id),
        )
        val ohneVariable = auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(quelle, ohneVariableKnoten), verbindungen = listOf(verbindung))).knoten.getValue(ohneVariableKnoten.id)

        assertTrue(ohneVektor.fehler.orEmpty().contains("Vektoreingang fehlt"))
        assertTrue(ohneVariable.fehler.orEmpty().contains("Polynomvariable darf nicht leer sein"))
    }
}
