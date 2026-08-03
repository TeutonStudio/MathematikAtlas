package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals

class KonzeptBibliothekRasterTest {
    @Test
    fun `Hauptkategorien verwenden feste bildschirmabhaengige Spaltenzahlen`() {
        assertEquals(2, konzeptRasterSpalten(400f, KonzeptRasterEbene.Hauptkategorien))
        assertEquals(3, konzeptRasterSpalten(600f, KonzeptRasterEbene.Hauptkategorien))
        assertEquals(4, konzeptRasterSpalten(900f, KonzeptRasterEbene.Hauptkategorien))
        assertEquals(5, konzeptRasterSpalten(1200f, KonzeptRasterEbene.Hauptkategorien))
    }

    @Test
    fun `Unterkategorien und Konzepte bleiben jeweils ein gemeinsames vertikales Raster`() {
        assertEquals(1, konzeptRasterSpalten(500f, KonzeptRasterEbene.Unterkategorien))
        assertEquals(3, konzeptRasterSpalten(1000f, KonzeptRasterEbene.Unterkategorien))
        assertEquals(1, konzeptRasterSpalten(650f, KonzeptRasterEbene.Konzepte))
        assertEquals(2, konzeptRasterSpalten(900f, KonzeptRasterEbene.Konzepte))
        assertEquals(3, konzeptRasterSpalten(1200f, KonzeptRasterEbene.Konzepte))
    }
}
