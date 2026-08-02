package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ZahlengeradenVisualisierungTest {
    private fun konfiguration(minimum: Double = -3.0, maximum: Double = 3.0) = VisualisierungsKonfiguration(
        dimension = RaumDimension.R1,
        achsen = AchsenZuordnung("x", "y", "z"),
        bereiche = AchsenBereiche(ZahlenBereich(minimum, maximum), ZahlenBereich(-1.0, 1.0), null),
        sampling = SamplingKonfiguration(32, 12, 1e-8, auflösung1D = 120),
    )

    @Test
    fun `offene und geschlossene Intervallgrenzen bleiben sichtbar`() {
        val menge = ReellesIntervall(RationaleZahl.von(-2), true, RationaleZahl.von(2), false)

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )
        val intervall = ergebnis.intervalle.single()

        assertEquals(-2.0, intervall.von)
        assertEquals(2.0, intervall.bis)
        assertFalse(intervall.linksGeschlossen)
        assertTrue(intervall.rechtsGeschlossen)
        assertTrue(ergebnis.punkte.isEmpty())
    }

    @Test
    fun `Vereinigung und Differenz werden als Segmente statt Punktwolken normalisiert`() {
        val vereinigung = Vereinigung(
            listOf(
                ReellesIntervall(RationaleZahl.von(-2), false, RationaleZahl.Null, false),
                ReellesIntervall(RationaleZahl.Null, true, RationaleZahl.von(2), false),
            ),
        )
        val differenz = MengenDifferenz(
            ReellesIntervall(RationaleZahl.von(-2), false, RationaleZahl.von(2), false),
            ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Eins, false),
        )

        val vereinigt = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(vereinigung, konfiguration()),
        )
        val getrennt = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(differenz, konfiguration()),
        )

        assertEquals(1, vereinigt.intervalle.size)
        assertEquals(-2.0, vereinigt.intervalle.single().von)
        assertEquals(2.0, vereinigt.intervalle.single().bis)
        assertEquals(2, getrennt.intervalle.size)
        assertEquals(-2.0, getrennt.intervalle[0].von)
        assertEquals(-1.0, getrennt.intervalle[0].bis)
        assertFalse(getrennt.intervalle[0].rechtsGeschlossen)
        assertEquals(1.0, getrennt.intervalle[1].von)
        assertFalse(getrennt.intervalle[1].linksGeschlossen)
        assertEquals(2.0, getrennt.intervalle[1].bis)
    }

    @Test
    fun `skalare Tupel und eindimensionale Vektoren werden als Punkte dargestellt`() {
        val menge = EndlicheMenge(
            setOf(
                RationaleZahl.von(-1),
                Tupel(listOf(RationaleZahl.Null)),
                SpaltenVektor(listOf(RationaleZahl.Eins)),
                ZeilenVektor(listOf(RationaleZahl.von(2))),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )

        assertEquals(setOf(-1.0, 0.0, 1.0, 2.0), ergebnis.punkte.map { it.x }.toSet())
        assertTrue(ergebnis.punkte.all { it.y == 0.0 })
        assertTrue(ergebnis.intervalle.isEmpty())
    }

    @Test
    fun `unbeschraenkte Mengen werden am sichtbaren Fenster markiert`() {
        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(ReelleZahlen, konfiguration(-5.0, 7.0)),
        )
        val intervall = ergebnis.intervalle.single()

        assertEquals(-5.0, intervall.von)
        assertEquals(7.0, intervall.bis)
        assertTrue(intervall.linksAmFensterrand)
        assertTrue(intervall.rechtsAmFensterrand)
    }

    @Test
    fun `mathematisch leer und ausserhalb des Fensters bleiben unterscheidbar`() {
        val leer = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(LeereMenge, konfiguration()),
        )
        val unsichtbar = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(EndlicheMenge(setOf(RationaleZahl.von(20))), konfiguration()),
        )

        assertEquals(VisualisierungsQualität.MathematischLeer, leer.qualität)
        assertEquals(VisualisierungsQualität.KeineTrefferImFenster, unsichtbar.qualität)
    }

    @Test
    fun `nicht exakt darstellbare dichte Grundmenge liefert Diagnose`() {
        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(RationaleZahlen, konfiguration()),
        )

        assertTrue(ergebnis.grund.contains("dicht"))
    }
}
