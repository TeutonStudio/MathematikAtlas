package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MatrixProduktMigrationTest {
    @Test
    fun `historische Matrixeingänge akzeptieren nach dem Laden orientierte Vektoren`() {
        val linksId = AnschlussId("matrixprodukt-links")
        val rechtsId = AnschlussId("matrixprodukt-rechts")
        val ausgangId = AnschlussId("matrixprodukt-ausgang")
        val alt = KnotenDaten(
            art = "mathematik.matrixProdukt",
            name = "Matrixprodukt",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = linksId,
                    name = "a",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Matrix.id,
                ),
                AnschlussDaten(
                    id = rechtsId,
                    name = "b",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Matrix.id,
                    reihenfolge = 1,
                ),
                AnschlussDaten(
                    id = ausgangId,
                    name = "matrix",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Matrix.id,
                ),
            ),
        )
        val spaltenQuelle = KnotenDaten(
            art = "test.spalte",
            name = "Spalte",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "vektor",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.SpaltenVektor.id,
                ),
            ),
        )
        val zeilenQuelle = KnotenDaten(
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
        val karte = KartenDaten(name = "Alt", knoten = listOf(alt, spaltenQuelle, zeilenQuelle))

        val migriert = MathematikKartenCodec.lese(
            KartenDatenJson.schreibe(karte),
        )
        val matrixProdukt = migriert.knoten.first { it.id == alt.id }
        val eingänge = matrixProdukt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(listOf(linksId, rechtsId), eingänge.map { it.id })
        eingänge.forEach { eingang ->
            assertEquals(MathematikAnschlussArten.Objekt.id, eingang.art)
            assertEquals(
                setOf(
                    MathematikAnschlussArten.Matrix.id,
                    MathematikAnschlussArten.SpaltenVektor.id,
                    MathematikAnschlussArten.ZeilenVektor.id,
                ),
                eingang.zulässigeArten,
            )
        }
        val graphPrüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        assertIs<VerbindungsPrüfung.Erlaubt>(
            graphPrüfung.prüfe(
                migriert,
                AnschlussVerweis(spaltenQuelle.id, spaltenQuelle.anschlüsse.single().id),
                AnschlussVerweis(alt.id, rechtsId),
            ),
        )
        assertIs<VerbindungsPrüfung.Erlaubt>(
            graphPrüfung.prüfe(
                migriert,
                AnschlussVerweis(zeilenQuelle.id, zeilenQuelle.anschlüsse.single().id),
                AnschlussVerweis(alt.id, linksId),
            ),
        )
        assertEquals(migriert, migriert.migriereMatrixProduktAnschlüsse())
    }
}
