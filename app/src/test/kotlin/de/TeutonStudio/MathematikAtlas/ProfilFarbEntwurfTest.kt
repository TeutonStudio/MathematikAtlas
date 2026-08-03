package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbEntwurf
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfilFarbEntwurfTest {
    @Test
    fun `gueltige Hex Eingabe synchronisiert RGB und HSV`() {
        val entwurf = ProfilFarbEntwurf.von(ProfilFarbe.Standard).mitHex("#FF8000")

        assertTrue(entwurf.istGueltig)
        assertEquals("#FF8000", entwurf.hexText)
        assertEquals("255", entwurf.rotText)
        assertEquals("128", entwurf.gruenText)
        assertEquals("0", entwurf.blauText)
        assertEquals("#FF8000", entwurf.letzteGueltigeFarbe.rgbHex)
    }

    @Test
    fun `ungueltige Texteingabe behaelt letzte Vorschaufarbe`() {
        val start = ProfilFarbe.parse("#123456")!!
        val entwurf = ProfilFarbEntwurf.von(start).mitHex("#12")

        assertFalse(entwurf.istGueltig)
        assertEquals(start, entwurf.letzteGueltigeFarbe)
        assertEquals("#12", entwurf.hexText)
    }

    @Test
    fun `RGB und HSV Aenderungen werden kanonisch zurueckgeschrieben`() {
        val start = ProfilFarbEntwurf.von(ProfilFarbe.Standard)
        val rgb = start.mitRgb(rot = "0", gruen = "255", blau = "0")
        val hsv = rgb.mitHsv(farbton = 240f, saettigung = 1f, helligkeit = 1f)

        assertEquals("#00FF00", rgb.hexText)
        assertEquals("#0000FF", hsv.hexText)
        assertEquals(ProfilFarbe.Standard, hsv.zuruecksetzen().letzteGueltigeFarbe)
    }
}
