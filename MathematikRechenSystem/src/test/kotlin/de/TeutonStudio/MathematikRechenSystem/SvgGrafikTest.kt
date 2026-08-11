package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgGrafikTest {
    @Test
    fun `mathematischer Koordinatenraum bildet y nach oben auf SVG y nach unten ab`() {
        val viewport = SvgViewport(0.0, 0.0, 200.0, 100.0)
        val raum = SvgKoordinatenraum(-10.0, 10.0, -5.0, 5.0)

        assertEquals(SvgPunkt(100.0, 50.0), raum.bildeAb(SvgPunkt(0.0, 0.0), viewport))
        assertEquals(SvgPunkt(200.0, 0.0), raum.bildeAb(SvgPunkt(10.0, 5.0), viewport))
        assertEquals(SvgPunkt(0.0, 100.0), raum.bildeAb(SvgPunkt(-10.0, -5.0), viewport))
    }

    @Test
    fun `SVG wird unveraenderlich um Elemente ergaenzt`() {
        val basis = SvgGrafik.standard()
        val erweitert = basis.mitElement(
            SvgKreis(
                id = "kreis-1",
                mittelpunkt = SvgPunkt(500.0, 500.0),
                radius = 25.0,
                stil = SvgStil(kontur = "#123456"),
            ),
        )

        assertTrue(basis.elemente.isEmpty())
        assertEquals(1, erweitert.elemente.size)
        assertTrue(erweitert.elemente.single() is SvgKreis)
    }

    @Test
    fun `Serializer ist deterministisch und escaped Text sowie Attribute`() {
        val grafik = SvgGrafik.standard().mitElement(
            SvgText(
                id = "text-1",
                position = SvgPunkt(10.0, 20.0),
                inhalt = "x < y & z",
                mathematikLatex = true,
                stil = SvgStil(füllung = "currentColor", kontur = "none"),
            ),
        )

        val erster = grafik.zuSvg()
        val zweiter = grafik.zuSvg()

        assertEquals(erster, zweiter)
        assertTrue(erster.contains("viewBox=\"0 0 1000 1000\""))
        assertTrue(erster.contains("x &lt; y &amp; z"))
        assertTrue(erster.contains("data-mathematik-latex=\"x &lt; y &amp; z\""))
        assertFalse(erster.contains("< y & z"))
    }

    @Test
    fun `kombinieren behaelt Dokumentraum des ersten SVG und macht IDs eindeutig`() {
        val a = SvgGrafik.standard().mitElement(
            SvgLinie("linie", SvgPunkt(0.0, 0.0), SvgPunkt(1.0, 1.0)),
        )
        val b = SvgGrafik(
            viewport = SvgViewport(10.0, 20.0, 300.0, 400.0),
            elemente = listOf(SvgLinie("linie", SvgPunkt(2.0, 2.0), SvgPunkt(3.0, 3.0))),
        )

        val kombiniert = a.kombiniere(b)

        assertEquals(a.viewport, kombiniert.viewport)
        assertEquals(listOf("linie", "linie-2"), kombiniert.elemente.map { it.id })
    }
}
