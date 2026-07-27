package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrixKnotenTest {
    private val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde("mathematik.matrix")!!

    @Test fun `Einzel-Eingaben werden in Zeilen und Spaltenreihenfolge ausgewertet`() {
        val matrix = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val eingänge = matrix.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associate { anschluss ->
            anschluss.name to BedingterWert(RationaleZahl.von((anschluss.reihenfolge + 1).toLong()))
        }

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(matrix, eingänge, RechenKontext()))

        assertEquals(
            listOf(listOf(RationaleZahl.von(1), RationaleZahl.von(2)), listOf(RationaleZahl.von(3), RationaleZahl.von(4))),
            assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen,
        )
    }

    @Test fun `Methodenmodus benötigt eine zweistellige Zahlmethode`() {
        val basis = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val matrix = konfiguriereMatrix(basis, MATRIX_METHODE, höhe = 2, breite = 2)
        val i = Variable("i"); val j = Variable("j")
        val methode = Funktion("f", listOf(i, j), mapOf("wert" to addition(multiplikation(RationaleZahl.von(10), i), j)))

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(matrix, mapOf("methode" to BedingterWert(methode)), RechenKontext()))

        assertEquals(listOf(listOf(RationaleZahl.von(0), RationaleZahl.von(1)), listOf(RationaleZahl.von(10), RationaleZahl.von(11))), assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen)
        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(matrix, mapOf("methode" to BedingterWert(Funktion("g", listOf(i), mapOf("wert" to i)))), RechenKontext()))
        }
    }

    @Test fun `Größenänderung erhält überlappende Eingänge und entfernt wegfallende Kanten mit Undo`() {
        val matrix = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val quellen = List(2) { index -> KnotenDaten(
            art = "test.zahl", name = "q$index",
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)),
        ) }
        val eintrag00 = matrix.anschlüsse.first { it.name == matrixEintragName(0, 0) }
        val eintrag11 = matrix.anschlüsse.first { it.name == matrixEintragName(1, 1) }
        val karte = KartenDaten(
            name = "Test", knoten = quellen + matrix,
            verbindungen = listOf(
                VerbindungDaten(von = AnschlussVerweis(quellen[0].id, quellen[0].anschlüsse.single().id), zu = AnschlussVerweis(matrix.id, eintrag00.id)),
                VerbindungDaten(von = AnschlussVerweis(quellen[1].id, quellen[1].anschlüsse.single().id), zu = AnschlussVerweis(matrix.id, eintrag11.id)),
            ),
        )
        val zustand = KartenEditorZustand(karte, GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))

        zustand.setzeMatrixKonfiguration(matrix.id, MATRIX_EINZEL_EINGABEN, höhe = 2, breite = 1)

        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(eintrag00.id, zustand.karte.knoten.first { it.id == matrix.id }.anschlüsse.first { it.name == matrixEintragName(0, 0) }.id)
        zustand.rückgängig()
        assertEquals(2, zustand.karte.verbindungen.size)
    }

    @Test fun `Moduswechsel entfernt nicht passende Einzel-Eingabekanten`() {
        val matrix = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val quelle = KnotenDaten(
            art = "test.zahl", name = "Quelle",
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)),
        )
        val eintrag = matrix.anschlüsse.first { it.name == matrixEintragName(0, 0) }
        val zustand = KartenEditorZustand(
            KartenDaten(
                name = "Test", knoten = listOf(quelle, matrix),
                verbindungen = listOf(VerbindungDaten(von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id), zu = AnschlussVerweis(matrix.id, eintrag.id))),
            ),
            GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)),
        )

        zustand.setzeMatrixKonfiguration(matrix.id, MATRIX_METHODE, höhe = 2, breite = 2)

        val konfiguriert = zustand.karte.knoten.first { it.id == matrix.id }
        assertEquals(emptyList(), zustand.karte.verbindungen)
        assertEquals(listOf("methode", "matrix"), konfiguriert.anschlüsse.map { it.name })
    }
}
