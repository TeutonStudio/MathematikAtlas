package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikAtlas.speicher.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FarbModelleTest {
    @Test
    fun `HSL Grundfarben und Grau entsprechen den erwarteten Grenzwerten`() {
        val faelle = listOf(
            "#FF0000" to HslFarbe(0.0, 1.0, .5),
            "#00FF00" to HslFarbe(120.0, 1.0, .5),
            "#0000FF" to HslFarbe(240.0, 1.0, .5),
            "#00FFFF" to HslFarbe(180.0, 1.0, .5),
            "#FF00FF" to HslFarbe(300.0, 1.0, .5),
            "#FFFF00" to HslFarbe(60.0, 1.0, .5),
            "#000000" to HslFarbe(0.0, 0.0, 0.0),
            "#FFFFFF" to HslFarbe(0.0, 0.0, 1.0),
            "#808080" to HslFarbe(0.0, 0.0, 128.0 / 255.0),
        )

        faelle.forEach { (hex, erwartet) ->
            val hsl = rgb(hex).zuHsl()
            assertEquals(erwartet.farbton, hsl.farbton, .01, hex)
            assertEquals(erwartet.saettigung, hsl.saettigung, .01, hex)
            assertEquals(erwartet.helligkeit, hsl.helligkeit, .01, hex)
            assertEquals(hex, hsl.zuRgb().rgbHex)
        }
    }

    @Test
    fun `HSL Rundlauf bleibt fuer chromatische Farben stabil`() {
        listOf("#1D4ED8", "#FF8000", "#18A36B", "#7C3AED").forEach { hex ->
            val start = rgb(hex)
            val ende = start.zuHsl().zuRgb()
            assertRgbNahe(start, ende, 1e-10, hex)
            val hsl = ende.zuHsl()
            val erwartet = start.zuHsl()
            assertEquals(erwartet.farbton, hsl.farbton, 1e-8, hex)
            assertEquals(erwartet.saettigung, hsl.saettigung, 1e-8, hex)
            assertEquals(erwartet.helligkeit, hsl.helligkeit, 1e-8, hex)
        }
    }

    @Test
    fun `achromatisches HSL behaelt bevorzugten Farbton`() {
        val grau = rgb("#808080").zuHsl(217.0)
        assertEquals(217.0, grau.farbton)
        assertEquals(0.0, grau.saettigung)
    }

    @Test
    fun `HSB und HSL sind bei gleichen Zahlen nicht pauschal dieselbe Farbe`() {
        val hsb = HsbFarbe(30f, .5f, .5f).zuRgb()
        val hsl = HslFarbe(30.0, .5, .5).zuRgb()
        assertTrue(hsb.rgbHex != hsl.rgbHex)
    }

    @Test
    fun `Bradford Adaption bildet D65 Weiss auf D50 Weiss ab und zurueck`() {
        val d65 = XyzFarbe(.95047, 1.0, 1.08883)
        val d50 = adaptiereD65NachD50(d65)
        assertEquals(.96422, d50.x, 5e-5)
        assertEquals(1.0, d50.y, 5e-5)
        assertEquals(.82521, d50.z, 5e-5)
        val rundlauf = adaptiereD50NachD65(d50)
        assertEquals(d65.x, rundlauf.x, 5e-5)
        assertEquals(d65.y, rundlauf.y, 5e-5)
        assertEquals(d65.z, rundlauf.z, 5e-5)
    }

    @Test
    fun `Lab D50 stimmt fuer sRGB Primaerfarben mit Referenzwerten ueberein`() {
        val referenzen = listOf(
            "#FFFFFF" to LabFarbe(100.0, .0169, .0227),
            "#000000" to LabFarbe(0.0, 0.0, 0.0),
            "#FF0000" to LabFarbe(54.2943, 80.8192, 69.8968),
            "#00FF00" to LabFarbe(87.8177, -79.2608, 80.9981),
            "#0000FF" to LabFarbe(29.5647, 68.2889, -112.0126),
        )
        referenzen.forEach { (hex, erwartet) ->
            val lab = rgb(hex).zuLabD50()
            assertEquals(erwartet.helligkeit, lab.helligkeit, .03, hex)
            assertEquals(erwartet.a, lab.a, .03, hex)
            assertEquals(erwartet.b, lab.b, .03, hex)
        }
    }

    @Test
    fun `Lab D50 Rundlauf bleibt innerhalb der sRGB Toleranz`() {
        listOf("#FFFFFF", "#000000", "#808080", "#FF0000", "#00FF00", "#0000FF", "#7C3AED").forEach { hex ->
            val start = rgb(hex)
            val ende = assertNotNull(start.zuLabD50().zuRgbOderNull(), hex)
            assertRgbNahe(start, ende, 2e-6, hex)
        }
    }

    @Test
    fun `Lab ausserhalb sRGB wird nicht still begrenzt`() {
        assertNull(LabFarbe(50.0, 127.0, 127.0).zuRgbOderNull())
        assertNull(LabFarbe(50.0, -128.0, -128.0).zuRgbOderNull())
    }

    @Test
    fun `generisches CMYK behandelt Schwarz Weiss und Grundfarben`() {
        assertEquals(CmykFarbe(0.0, 0.0, 0.0, 1.0), rgb("#000000").zuCmyk())
        assertEquals(CmykFarbe(0.0, 0.0, 0.0, 0.0), rgb("#FFFFFF").zuCmyk())
        assertEquals(CmykFarbe(1.0, 0.0, 0.0, 0.0), rgb("#00FFFF").zuCmyk())
        assertEquals(CmykFarbe(0.0, 1.0, 0.0, 0.0), rgb("#FF00FF").zuCmyk())
        assertEquals(CmykFarbe(0.0, 0.0, 1.0, 0.0), rgb("#FFFF00").zuCmyk())
    }

    @Test
    fun `CMYK Rundlauf bleibt stabil und Kanaele bleiben im Bereich`() {
        listOf("#000000", "#FFFFFF", "#1D4ED8", "#FF8000", "#18A36B").forEach { hex ->
            val start = rgb(hex)
            val cmyk = start.zuCmyk()
            assertTrue(listOf(cmyk.cyan, cmyk.magenta, cmyk.gelb, cmyk.schwarz).all { it in 0.0..1.0 })
            assertRgbNahe(start, cmyk.zuRgb(), 1e-10, hex)
        }
    }

    private fun rgb(hex: String): RgbFarbe = RgbFarbe.aus(requireNotNull(ProfilFarbe.parse(hex)))

    private fun assertRgbNahe(erwartet: RgbFarbe, tatsaechlich: RgbFarbe, toleranz: Double, nachricht: String) {
        assertEquals(erwartet.rot, tatsaechlich.rot, toleranz, nachricht)
        assertEquals(erwartet.gruen, tatsaechlich.gruen, toleranz, nachricht)
        assertEquals(erwartet.blau, tatsaechlich.blau, toleranz, nachricht)
    }
}
