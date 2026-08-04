package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikAtlas.speicher.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfilFarbEntwurfTest {
    @Test
    fun `gueltige Hex Eingabe synchronisiert RGB und HSB`() {
        val entwurf = FarbEntwurf.von(ProfilFarbe.Standard).mitHex("#FF8000")

        assertTrue(entwurf.istGueltig)
        assertEquals("#FF8000", entwurf.hexText)
        assertEquals("255", entwurf.rotText)
        assertEquals("128", entwurf.gruenText)
        assertEquals("0", entwurf.blauText)
        assertEquals("#FF8000", entwurf.kanonisch.rgbHex)
        assertEquals(30.1f, entwurf.hsb.farbton, absoluteTolerance = .2f)
    }

    @Test
    fun `ungueltige Texteingabe behaelt letzte Vorschaufarbe`() {
        val start = RgbFarbe.aus(ProfilFarbe.parse("#123456")!!)
        val entwurf = FarbEntwurf.von(start).mitHex("#12")

        assertFalse(entwurf.istGueltig)
        assertEquals(start, entwurf.kanonisch)
        assertEquals("#12", entwurf.hexText)
    }

    @Test
    fun `RGB und HSB Aenderungen werden kanonisch zurueckgeschrieben`() {
        val start = FarbEntwurf.von(ProfilFarbe.Standard)
        val rgb = start.mitRgb(rot = "0", gruen = "255", blau = "0")
        val hsb = rgb.mitHsb(farbton = 240f, saettigung = 1f, helligkeit = 1f)

        assertEquals("#00FF00", rgb.hexText)
        assertEquals("#0000FF", hsb.hexText)
        assertEquals(ProfilFarbe.Standard, hsb.zuruecksetzen().letzteGueltigeFarbe)
    }

    @Test
    fun `Moduswechsel veraendert die kanonische Farbe auch nach vielen Wechseln nicht`() {
        val start = FarbEntwurf.von(RgbFarbe.aus(ProfilFarbe.parse("#7A39D2")!!))
        val gewechselt = (0 until 100).fold(start) { entwurf, index ->
            entwurf.mitModus(if (index % 2 == 0) FarbEingabeModus.HSB else FarbEingabeModus.RGB)
        }

        assertEquals(start.kanonisch, gewechselt.kanonisch)
        assertEquals(start.hexText, gewechselt.hexText)
    }

    @Test
    fun `Grundfarben Schwarz Weiss und Grau entsprechen den HSB Grenzwerten`() {
        val faelle = listOf(
            "#FF0000" to HsbFarbe(0f, 1f, 1f),
            "#00FF00" to HsbFarbe(120f, 1f, 1f),
            "#0000FF" to HsbFarbe(240f, 1f, 1f),
            "#00FFFF" to HsbFarbe(180f, 1f, 1f),
            "#FF00FF" to HsbFarbe(300f, 1f, 1f),
            "#FFFF00" to HsbFarbe(60f, 1f, 1f),
            "#000000" to HsbFarbe(0f, 0f, 0f),
            "#FFFFFF" to HsbFarbe(0f, 0f, 1f),
            "#808080" to HsbFarbe(0f, 0f, 128f / 255f),
        )

        faelle.forEach { (hex, erwartet) ->
            val hsb = RgbFarbe.aus(ProfilFarbe.parse(hex)!!).zuHsb()
            assertEquals(erwartet.farbton, hsb.farbton, absoluteTolerance = .01f, message = hex)
            assertEquals(erwartet.saettigung, hsb.saettigung, absoluteTolerance = .01f, message = hex)
            assertEquals(erwartet.helligkeit, hsb.helligkeit, absoluteTolerance = .01f, message = hex)
            assertEquals(hex, hsb.zuRgb().rgbHex)
        }
    }

    @Test
    fun `Farbton 360 wird kanonisch zu null und verlaesst nie den Bereich`() {
        val entwurf = FarbEntwurf.von(ProfilFarbe.Standard).mitHsb(farbton = 360f)

        assertEquals(0f, entwurf.hsb.farbton)
        assertTrue(entwurf.hsb.saettigung in 0f..1f)
        assertTrue(entwurf.hsb.helligkeit in 0f..1f)
        assertTrue(entwurf.kanonisch.rot in 0.0..1.0)
        assertTrue(entwurf.kanonisch.gruen in 0.0..1.0)
        assertTrue(entwurf.kanonisch.blau in 0.0..1.0)
    }

    @Test
    fun `achromatische Bearbeitung behaelt den vorherigen Farbton als Entwurfszustand`() {
        val bunt = FarbEntwurf.von(ProfilFarbe.Standard).mitHsb(farbton = 217f, saettigung = 1f, helligkeit = .8f)
        val grau = bunt.mitHsb(saettigung = 0f)
        val wiederGesättigt = grau.mitHsb(saettigung = 1f)

        assertEquals(217f, grau.hsb.farbton)
        assertEquals(217f, wiederGesättigt.hsb.farbton)
        assertTrue(abs(wiederGesättigt.kanonisch.zuHsb().farbton - 217f) < .5f)
    }

    @Test
    fun `Zuruecksetzen verwendet den vom Aufrufer gelieferten Standard`() {
        val benutzerStandard = RgbFarbe.aus(ProfilFarbe.parse("#102030")!!)
        val entwurf = FarbEntwurf.von(ProfilFarbe.Standard)
            .mitHex("#ABCDEF")
            .zuruecksetzen(benutzerStandard)

        assertEquals("#102030", entwurf.kanonisch.rgbHex)
    }
}
