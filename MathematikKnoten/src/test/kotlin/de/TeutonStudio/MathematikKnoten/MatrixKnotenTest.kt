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

    @Test fun `nichtquadratische Matrix wird aus Zeilenvektoren aufgebaut`() {
        val matrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero), MATRIX_ZEILEN, höhe = 2, breite = 3)
        val eingänge = mapOf(
            matrixZeileName(0) to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2), zahl(3)))),
            matrixZeileName(1) to BedingterWert(ZeilenVektor(listOf(zahl(4), zahl(5), zahl(6)))),
        )

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(matrix, eingänge, RechenKontext()))

        assertEquals(
            listOf(listOf(zahl(1), zahl(2), zahl(3)), listOf(zahl(4), zahl(5), zahl(6))),
            assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen,
        )
        assertEquals(listOf("zeile_0", "zeile_1", "matrix"), matrix.anschlüsse.map { it.name })
        assertTrue(matrix.anschlüsse.take(2).all { it.art == MathematikAnschlussArten.ZeilenVektor.id })
    }

    @Test fun `nichtquadratische Matrix wird aus Spaltenvektoren aufgebaut`() {
        val matrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero), MATRIX_SPALTEN, höhe = 2, breite = 3)
        val eingänge = mapOf(
            matrixSpalteName(0) to BedingterWert(SpaltenVektor(listOf(zahl(1), zahl(4)))),
            matrixSpalteName(1) to BedingterWert(SpaltenVektor(listOf(zahl(2), zahl(5)))),
            matrixSpalteName(2) to BedingterWert(SpaltenVektor(listOf(zahl(3), zahl(6)))),
        )

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(matrix, eingänge, RechenKontext()))

        assertEquals(
            listOf(listOf(zahl(1), zahl(2), zahl(3)), listOf(zahl(4), zahl(5), zahl(6))),
            assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen,
        )
        assertEquals(listOf("spalte_0", "spalte_1", "spalte_2", "matrix"), matrix.anschlüsse.map { it.name })
        assertTrue(matrix.anschlüsse.take(3).all { it.art == MathematikAnschlussArten.SpaltenVektor.id })
    }

    @Test fun `Zeilen und Spalten melden fehlende Eingänge und falsche Längen konkret`() {
        val zeilenMatrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero), MATRIX_ZEILEN, 2, 3)
        val fehlendeZeile = assertFailsWith<IllegalStateException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    zeilenMatrix,
                    mapOf(matrixZeileName(0) to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2), zahl(3))))),
                    RechenKontext(),
                ),
            )
        }
        assertContains(fehlendeZeile.message.orEmpty(), "Zeile 2 fehlt")

        val falscheZeile = assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    zeilenMatrix,
                    mapOf(
                        matrixZeileName(0) to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2)))),
                        matrixZeileName(1) to BedingterWert(ZeilenVektor(listOf(zahl(3), zahl(4), zahl(5)))),
                    ),
                    RechenKontext(),
                ),
            )
        }
        assertContains(falscheZeile.message.orEmpty(), "muss 3 Elemente besitzen, hat aber 2")

        val spaltenMatrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero), MATRIX_SPALTEN, 2, 3)
        val falscheSpalte = assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    spaltenMatrix,
                    mapOf(
                        matrixSpalteName(0) to BedingterWert(SpaltenVektor(listOf(zahl(1)))),
                        matrixSpalteName(1) to BedingterWert(SpaltenVektor(listOf(zahl(2), zahl(3)))),
                        matrixSpalteName(2) to BedingterWert(SpaltenVektor(listOf(zahl(4), zahl(5)))),
                    ),
                    RechenKontext(),
                ),
            )
        }
        assertContains(falscheSpalte.message.orEmpty(), "muss 2 Elemente besitzen, hat aber 1")
    }

    @Test fun `Größenänderung bewahrt gültige Zeilen- und Spaltenanschlüsse`() {
        val basis = MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val zeilen = konfiguriereMatrix(basis, MATRIX_ZEILEN, 2, 3)
        val ersteZeile = zeilen.anschlüsse.first { it.name == matrixZeileName(0) }
        val erweitert = konfiguriereMatrix(zeilen, MATRIX_ZEILEN, 3, 4)
        assertEquals(ersteZeile.id, erweitert.anschlüsse.first { it.name == matrixZeileName(0) }.id)

        val spalten = konfiguriereMatrix(erweitert, MATRIX_SPALTEN, 3, 2)
        val ersteSpalte = spalten.anschlüsse.first { it.name == matrixSpalteName(0) }
        val verkleinert = konfiguriereMatrix(spalten, MATRIX_SPALTEN, 2, 1)
        assertEquals(ersteSpalte.id, verkleinert.anschlüsse.first { it.name == matrixSpalteName(0) }.id)
        assertEquals(listOf("spalte_0", "matrix"), verkleinert.anschlüsse.map { it.name })
    }

    @Test fun `Moduswechsel zwischen Zeilen und Spalten entfernt Kanten in einer Undo Aktion`() {
        val zeilenMatrix = konfiguriereMatrix(MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero), MATRIX_ZEILEN, 2, 3)
        val quelle = KnotenDaten(
            art = "test.zeile",
            name = "Zeile",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "vektor",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.ZeilenVektor.id,
                ),
            ),
        )
        val zeile0 = zeilenMatrix.anschlüsse.first { it.name == matrixZeileName(0) }
        val zustand = KartenEditorZustand(
            KartenDaten(
                name = "Test",
                knoten = listOf(quelle, zeilenMatrix),
                verbindungen = listOf(
                    VerbindungDaten(
                        von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                        zu = AnschlussVerweis(zeilenMatrix.id, zeile0.id),
                    ),
                ),
            ),
            GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)),
        )

        zustand.setzeMatrixKonfiguration(zeilenMatrix.id, MATRIX_SPALTEN, 2, 3)

        assertTrue(zustand.karte.verbindungen.isEmpty())
        assertEquals(MATRIX_SPALTEN, matrixKonfiguration(zustand.karte.knoten.first { it.id == zeilenMatrix.id }).erzeugungsArt)
        zustand.rückgängig()
        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(MATRIX_ZEILEN, matrixKonfiguration(zustand.karte.knoten.first { it.id == zeilenMatrix.id }).erzeugungsArt)
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)

}
