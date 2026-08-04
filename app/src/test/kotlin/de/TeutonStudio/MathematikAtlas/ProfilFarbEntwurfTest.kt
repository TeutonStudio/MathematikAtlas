package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikAtlas.speicher.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilFarbEntwurfTest {
    @Test
    fun `gueltige Hex Eingabe synchronisiert alle Farbmodelle`() {
        val entwurf = FarbEntwurf.von(ProfilFarbe.Standard).mitHex("#FF8000")

        assertTrue(entwurf.istGueltig)
        assertEquals("#FF8000", entwurf.hexText)
        assertEquals("255", entwurf.texte.rgb.rot)
        assertEquals("128", entwurf.texte.rgb.gruen)
        assertEquals("0", entwurf.texte.rgb.blau)
        assertEquals("#FF8000", entwurf.kanonisch.rgbHex)
        assertEquals(30.1f, entwurf.hsb.farbton, absoluteTolerance = .2f)
        assertEquals(30.1, entwurf.hsl.farbton, absoluteTolerance = .2)
        assertTrue(entwurf.texte.lab.helligkeit.isNotBlank())
        assertTrue(entwurf.texte.cmyk.schwarz.isNotBlank())
    }

    @Test
    fun `ungueltige Texteingabe behaelt letzte Vorschaufarbe`() {
        val start = rgb("#123456")
        val entwurf = FarbEntwurf.von(start).mitHex("#12")

        assertFalse(entwurf.istGueltig)
        assertEquals(start, entwurf.kanonisch)
        assertEquals("#12", entwurf.hexText)
        assertEquals(FarbEingabeFehler.HexUngueltig, entwurf.fehler)
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
    fun `Moduswechsel durch alle Modelle veraendert die kanonische Farbe nicht`() {
        val start = FarbEntwurf.von(rgb("#7A39D2"))
        val modi = FarbEingabeModus.entries
        val gewechselt = (0 until 100).fold(start) { entwurf, index ->
            entwurf.mitModus(modi[index % modi.size])
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
            val hsb = rgb(hex).zuHsb()
            assertEquals(erwartet.farbton, hsb.farbton, absoluteTolerance = .01f, message = hex)
            assertEquals(erwartet.saettigung, hsb.saettigung, absoluteTolerance = .01f, message = hex)
            assertEquals(erwartet.helligkeit, hsb.helligkeit, absoluteTolerance = .01f, message = hex)
            assertEquals(hex, hsb.zuRgb().rgbHex)
        }
    }

    @Test
    fun `Farbton 360 wird in HSB und HSL kanonisch zu null`() {
        val hsb = FarbEntwurf.von(ProfilFarbe.Standard).mitHsbText(farbton = "360")
        val hsl = hsb.mitHslText(farbton = "360")

        assertEquals(0f, hsb.hsb.farbton)
        assertEquals(0.0, hsl.hsl.farbton)
        assertTrue(hsl.kanonisch.rot in 0.0..1.0)
        assertTrue(hsl.kanonisch.gruen in 0.0..1.0)
        assertTrue(hsl.kanonisch.blau in 0.0..1.0)
    }

    @Test
    fun `achromatische HSB und HSL Bearbeitung behaelt den gewaehlten Farbton`() {
        val hsbGrau = FarbEntwurf.von(ProfilFarbe.Standard)
            .mitHsb(farbton = 217f, saettigung = 0f, helligkeit = .5f)
        val hsbBunt = hsbGrau.mitHsb(saettigung = 1f)
        val hslGrau = hsbBunt.mitHslText(farbton = "143", saettigung = "0", helligkeit = "50")
        val hslBunt = hslGrau.mitHslText(saettigung = "100")

        assertEquals(217f, hsbGrau.hsb.farbton)
        assertTrue(abs(hsbBunt.kanonisch.zuHsb().farbton - 217f) < .5f)
        assertEquals(143.0, hslGrau.hsl.farbton)
        assertTrue(abs(hslBunt.kanonisch.zuHsl().farbton - 143.0) < .5)
    }

    @Test
    fun `Dezimalkomma und negative Lab Werte werden akzeptiert`() {
        val start = rgb("#336699")
        val lab = start.zuLabD50()
        val entwurf = FarbEntwurf.von(start, FarbEingabeModus.LAB).mitLabText(
            helligkeit = lab.helligkeit.toString().replace('.', ','),
            a = lab.a.toString().replace('.', ','),
            b = lab.b.toString().replace('.', ','),
        )

        assertTrue(entwurf.istGueltig)
        assertTrue(entwurf.texte.lab.b.startsWith("-") || entwurf.texte.lab.a.startsWith("-"))
        assertEquals(start.rot, entwurf.kanonisch.rot, absoluteTolerance = 2e-6)
        assertEquals(start.gruen, entwurf.kanonisch.gruen, absoluteTolerance = 2e-6)
        assertEquals(start.blau, entwurf.kanonisch.blau, absoluteTolerance = 2e-6)
    }

    @Test
    fun `Lab ausserhalb sRGB behaelt die letzte gueltige Farbe`() {
        val start = rgb("#336699")
        val entwurf = FarbEntwurf.von(start, FarbEingabeModus.LAB)
            .mitLabText(helligkeit = "50", a = "127", b = "127")

        assertEquals(start, entwurf.kanonisch)
        assertEquals(FarbEingabeFehler.LabAusserhalbSrgb, entwurf.fehler)
        assertFalse(entwurf.istGueltig)
    }

    @Test
    fun `unvollstaendige Dezimal und Vorzeicheneingaben aendern die Vorschau nicht`() {
        val start = rgb("#336699")
        val minus = FarbEntwurf.von(start, FarbEingabeModus.LAB).mitLabText(a = "-")
        val komma = FarbEntwurf.von(start, FarbEingabeModus.HSL).mitHslText(helligkeit = "12,")

        assertEquals(start, minus.kanonisch)
        assertEquals(start, komma.kanonisch)
        assertIs<FarbEingabeFehler.UnvollstaendigeEingabe>(minus.fehler)
        assertIs<FarbEingabeFehler.UnvollstaendigeEingabe>(komma.fehler)
    }

    @Test
    fun `direkte CMYK Zerlegung bleibt bei Moduswechsel erhalten`() {
        val entwurf = FarbEntwurf.von(rgb("#804020"), FarbEingabeModus.CMYK)
            .mitCmykText(cyan = "20", magenta = "50", gelb = "0", schwarz = "20")
        val gewechselt = entwurf
            .mitModus(FarbEingabeModus.HSL)
            .mitModus(FarbEingabeModus.RGB)
            .mitModus(FarbEingabeModus.CMYK)

        assertEquals("20", gewechselt.texte.cmyk.cyan)
        assertEquals("50", gewechselt.texte.cmyk.magenta)
        assertEquals("0", gewechselt.texte.cmyk.gelb)
        assertEquals("20", gewechselt.texte.cmyk.schwarz)
        assertEquals(CmykFarbe(.2, .5, 0.0, .2), gewechselt.metadaten.bevorzugteCmykZerlegung)
    }

    @Test
    fun `Aenderung ausserhalb CMYK verwirft alte Zerlegung und berechnet neu`() {
        val cmyk = FarbEntwurf.von(rgb("#804020"), FarbEingabeModus.CMYK)
            .mitCmykText(cyan = "20", magenta = "50", gelb = "0", schwarz = "20")
        val rgb = cmyk.mitModus(FarbEingabeModus.RGB).mitRgb(rot = "10", gruen = "20", blau = "30")

        assertNull(rgb.metadaten.bevorzugteCmykZerlegung)
        assertEquals(rgb.kanonisch.zuCmyk(), rgb.cmyk)
    }

    @Test
    fun `Schwarz wird als null null null hundert CMYK abgeleitet`() {
        val entwurf = FarbEntwurf.von(rgb("#000000"), FarbEingabeModus.CMYK)
        assertEquals(CmykFarbe(0.0, 0.0, 0.0, 1.0), entwurf.cmyk)
        assertEquals(CmykTextZustand("0", "0", "0", "100"), entwurf.texte.cmyk)
    }

    @Test
    fun `Zuruecksetzen verwendet den vom Aufrufer gelieferten Standard`() {
        val benutzerStandard = rgb("#102030")
        val entwurf = FarbEntwurf.von(ProfilFarbe.Standard)
            .mitHex("#ABCDEF")
            .zuruecksetzen(benutzerStandard)

        assertEquals("#102030", entwurf.kanonisch.rgbHex)
    }

    private fun rgb(hex: String): RgbFarbe = RgbFarbe.aus(requireNotNull(ProfilFarbe.parse(hex)))
}
