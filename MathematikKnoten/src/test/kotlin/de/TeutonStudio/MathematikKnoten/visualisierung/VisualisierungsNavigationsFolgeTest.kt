package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*
import kotlin.test.*

class VisualisierungsNavigationsFolgeTest {
    private val c = VisualisierungsKonfiguration()
    private val menge = KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))
    private fun ergebnis(x: Double) = VisualisierungsErgebnis.Erfolgreich(listOf(VisualisierungsPunkt(x, 0.0)))

    @Test fun erstaufbauVeröffentlichtVorschauDannDetail() = runBlocking {
        val schritte = mutableListOf<String>()
        berechneVisualisierungsFolge(c, false,
            berechne = { schritte += if(it.sampling.vorschau) "grob" else "detail"; ergebnis(1.0) },
            veröffentliche = { _, fertig -> schritte += if(fertig) "fertig" else "partiell" },
            warteAufRuhe = { schritte += "ruhe" })
        assertEquals(listOf("grob", "partiell", "ruhe", "detail", "fertig"), schritte)
    }

    @Test fun navigationFordertNachRuheNurDetailAn() = runBlocking {
        val vorher = ergebnis(1.0); val nachher = ergebnis(2.0)
        var sichtbar: VisualisierungsErgebnis = vorher
        val ruhe = CompletableDeferred<Unit>(); val gestartet = CompletableDeferred<Unit>()
        val auftrag = launch {
            berechneVisualisierungsFolge(c, true,
                berechne = { assertFalse(it.sampling.vorschau); assertSame(vorher, sichtbar); nachher },
                veröffentliche = { e, fertig -> assertTrue(fertig); sichtbar = e },
                warteAufRuhe = { gestartet.complete(Unit); ruhe.await() })
        }
        gestartet.await(); assertSame(vorher, sichtbar)
        ruhe.complete(Unit); auftrag.join(); assertSame(nachher, sichtbar)
    }

    @Test fun ersetzteNavigationWirdVorDemSamplingAbgebrochen() = runBlocking {
        var berechnungen = 0
        val gestartet = CompletableDeferred<Unit>()
        val auftrag = launch {
            berechneVisualisierungsFolge(c, true,
                berechne = { berechnungen++; ergebnis(2.0) },
                veröffentliche = { _, _ -> error("Ersetzter Auftrag darf nichts veröffentlichen") },
                warteAufRuhe = { gestartet.complete(Unit); awaitCancellation() })
        }
        gestartet.await(); auftrag.cancelAndJoin(); assertEquals(0, berechnungen)
    }

    @Test fun verspätetesNichtKooperativesErgebnisWirdNichtVeröffentlicht() = runBlocking {
        val gestartet = CompletableDeferred<Unit>(); val freigabe = CompletableDeferred<Unit>()
        var veröffentlicht = false
        val auftrag = launch {
            berechneVisualisierungsFolge(c, true,
                berechne = { withContext(NonCancellable) { gestartet.complete(Unit); freigabe.await(); ergebnis(2.0) } },
                veröffentliche = { _, _ -> veröffentlicht = true }, warteAufRuhe = {})
        }
        gestartet.await(); auftrag.cancel(); freigabe.complete(Unit); auftrag.join()
        assertFalse(veröffentlicht)
    }

    @Test fun kameraQualitätFenstergrößeUndStilBehaltenDieFachlicheIdentität() {
        val anders = c.copy(kamera = c.kamera.copy(zoom = 2.0, translationX = 5.0), qualität = DarstellungsQualität.Hoch,
            farbe = c.farbe.copy(festeFarbe = 0xFFFF0000, palette = "Wald"), rasterSichtbar = false,
            sampling = c.sampling.copy(maximalesRasterBudget = 64000, auflösung2D = 240,
                sichtBereiche = c.bereiche.copy(x = ZahlenBereich(0.0, 20.0)), bildBreite = 900.0, zielFehlerPixel = .5))
        assertEquals(c.inhaltsSignatur(menge), anders.inhaltsSignatur(menge))
    }

    @Test fun eingangProjektionAchsenUndFachlicheParameterTrennenDieGeometrie() {
        val key = c.inhaltsSignatur(menge)
        assertNotEquals(key, c.inhaltsSignatur(ReelleZahlen))
        for(anders in listOf(c.copy(dimension = RaumDimension.C), c.copy(achsen = AchsenZuordnung("u", "v", null)),
            c.copy(bereiche = c.bereiche.copy(x = ZahlenBereich(-2.0, 2.0))), c.copy(ausgabeKoordinaten = listOf(2, 1)),
            c.copy(festeSchnitte = mapOf("t" to 2.0)), c.copy(methodenModus = MethodenDarstellungsModus.Bild),
            c.copy(sampling = c.sampling.copy(toleranz = .01)), c.copy(sampling = c.sampling.copy(maximaleOrbitSchritte = 12)),
            c.copy(farbe = c.farbe.copy(modus = FarbModus.Spektrum, variable = "t")))) {
            assertNotEquals(key, anders.inhaltsSignatur(menge))
        }
    }
}
