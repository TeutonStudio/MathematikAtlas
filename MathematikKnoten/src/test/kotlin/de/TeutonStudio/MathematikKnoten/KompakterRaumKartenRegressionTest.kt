package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KompakterRaumKartenRegressionTest {
    @Test fun gleicheKarteMitKompakterUndAusgeschriebenerGrundmenge() {
        val json = javaClass.getResource("/regression/tupel-filter-visualisierung-v3.json")!!.readText()
        val produktKarte = MathematikKartenCodec.lade(json)
        val produkt = produktKarte.knoten.single { it.parameter["operator"] == "menge.kartesischesProdukt" }
        val quelle = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val kompakt = produkt.copy(art = MENGEN_KNOTEN_ART,
            parameter = mapOf(MENGEN_KNOTEN_AUSWAHL to "R", "standardwert.dimension" to "2"),
            anschlüsse = quelle.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang } + produkt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang })
        val kompakteKarte = MathematikKartenCodec.lade(KartenDatenJson.schreibe(produktKarte.copy(
            knoten = produktKarte.knoten.map { if (it.id == produkt.id) kompakt else it },
            verbindungen = produktKarte.verbindungen.filterNot { it.zu.knotenId == produkt.id })))
        val c = VisualisierungsKonfiguration(dimension = RaumDimension.R2, achsen = AchsenZuordnung("x", "y", null),
            bereiche = AchsenBereiche(ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0), null),
            sampling = SamplingKonfiguration(32, 8, .001, maximalesRasterBudget = 8000, bildBreite = 400.0, bildHöhe = 400.0))
        val ergebnisse = mutableListOf<VisualisierungsErgebnis>()
        for (karte in listOf(produktKarte, kompakteKarte)) {
            val auswertung = MathematikKartenLaufzeit().auswerten(karte)
            assertTrue(auswertung.fehler.isEmpty(), auswertung.fehler.joinToString())
            val vis = karte.knoten.single { it.art == "mathematik.visualisierung" }
            val menge = assertIs<GefilterteMenge>(auswertung.knoten.getValue(vis.id).eingänge.getValue("menge").objekt)
            for ((p, erwartet) in listOf(listOf(0L,1L) to Wahrheitswert.Wahr, listOf(1L,0L) to Wahrheitswert.Lüge)) {
                val punkt = Tupel(p.map(RationaleZahl::von))
                val cas = ElementBeziehung(punkt, menge).entscheide()
                assertEquals(erwartet, cas.wahrheitswert)
            }
            val repariert = VisualisierungsSampler.normalisiere(menge, c)
            val region = assertIs<VisualisierungsDefinition.Region>(repariert)
            assertEquals(NumerischeMitgliedschaft.Enthalten, region.mitgliedschaft(listOf(0.0,1.0)))
            assertEquals(NumerischeMitgliedschaft.NichtEnthalten, region.mitgliedschaft(listOf(1.0,0.0)))
            val bild = VisualisierungsSampler.sample(menge, c)
            val zellen = when (bild) {
                is VisualisierungsErgebnis.Erfolgreich -> bild.zellen
                is VisualisierungsErgebnis.Teilweise -> bild.zellen
                else -> fail("Die Prädikatsmenge erzeugt keine Fläche: $bild")
            }
            assertTrue(zellen.any { it.status == ZellenStatus.Enthalten }, "Die Fläche benötigt bewiesen enthaltene Zellen.")
            assertTrue(zellen.any { it.status == ZellenStatus.Ausgeschlossen }, "Die Parabel muss auch Punkte ausschließen.")
            ergebnisse += bild
            assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(menge.menge, c))
            assertEquals(RaumDimension.R2, empfohleneRaumDimension(menge))
        }
        assertEquals(ergebnisse[0], ergebnisse[1], "Nach der Reparatur identische Fläche, Rand und Statistik für beide Graphvarianten")

    }
}
