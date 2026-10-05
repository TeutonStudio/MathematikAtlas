package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsErgebnis
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsSampler
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrixProduktUndBildmengeKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Matrixprodukt Vorlage akzeptiert Matrix Zeile und Spalte`() {
        val knoten = MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(2, eingänge.size)
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
    }

    @Test
    fun `Matrixprodukt Knoten berechnet Zeile mal Spalte als eins mal eins Matrix`() {
        val knoten = MathematikKnotenVorlagen.MatrixProdukt.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde("mathematik.matrixProdukt")!!
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "a" to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2), zahl(3)))),
                    "b" to BedingterWert(SpaltenVektor(listOf(zahl(4), zahl(5), zahl(6)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            listOf(listOf(zahl(32))),
            assertIs<Matrix>(ergebnis.ausgaben.getValue("matrix").objekt).zeilen,
        )
    }

    @Test
    fun `Bildmenge Produktmodus erzeugt und visualisiert zweistellige Methode`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(multiplikation(x, x), multiplikation(y, y)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde("mathematik.abbild")!!
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))),
                    "methode" to BedingterWert(methode),
                ),
                RechenKontext(),
            ),
        )
        val bild = assertIs<Abbild>(ergebnis.ausgaben.getValue("menge").objekt)

        val visualisiert = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(bild, visualisierungsKonfiguration()),
        )
        assertTrue(visualisiert.punkte.size <= 25)
        assertTrue(visualisiert.dreiecke.isNotEmpty())
        assertTrue(visualisiert.statistik.budgetErschöpft)
        assertTrue(visualisiert.punkte.all { punkt -> punkt.z == punkt.x * punkt.x + punkt.y * punkt.y })
    }

    @Test
    fun `Bildmenge Einzelmengenmodus bildet intern das kartesische Produkt`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "summe",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_EINZELMENGEN),
        )
        val auswerter = register.finde("mathematik.abbild")!!
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    bildmengeArgumentName(0) to BedingterWert(EndlicheMenge(setOf(zahl(1), zahl(2)))),
                    bildmengeArgumentName(1) to BedingterWert(EndlicheMenge(setOf(zahl(10), zahl(20)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            EndlicheMenge(setOf(zahl(11), zahl(21), zahl(12), zahl(22))),
            ergebnis.ausgaben.getValue("menge").objekt,
        )
    }

    private fun visualisierungsKonfiguration() = VisualisierungsKonfiguration(
        dimension = RaumDimension.R3,
        achsen = AchsenZuordnung("x", "y", "z"),
        bereiche = AchsenBereiche(
            ZahlenBereich(-1.0, 1.0),
            ZahlenBereich(-1.0, 1.0),
            ZahlenBereich(0.0, 2.0),
        ),
        sampling = SamplingKonfiguration(
            auflösung1D = 5,
            auflösung2D = 5,
            auflösung3D = 5,
            maximalesRasterBudget = 25,
            toleranz = 1e-9,
        ),
    )

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
