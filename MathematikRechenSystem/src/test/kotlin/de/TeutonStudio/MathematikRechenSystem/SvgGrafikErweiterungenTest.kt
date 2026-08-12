package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgGrafikErweiterungenTest {
    @Test
    fun `Transformation Stil und Attribute bearbeiten AST unveraenderlich`() {
        val basis = SvgGrafik.standard().mitElement(
            SvgKreis("kreis", SvgPunkt(100.0, 100.0), 25.0),
        )

        val geändert = basis
            .transformiere("kreis", SvgTransformation.Verschieben(10.0, 20.0))
            .stilisiere("kreis", SvgStil(füllung = "red"))
            .setzeAttribut("kreis", "clip-path", "url(#clip)")

        val original = basis.findeElement("kreis") as SvgKreis
        val kreis = geändert.findeElement("kreis") as SvgKreis
        assertTrue(original.transformationen.isEmpty())
        assertEquals(null, original.stil)
        assertEquals(1, kreis.transformationen.size)
        assertEquals("red", kreis.stil?.füllung)
        assertEquals("url(#clip)", kreis.attribute["clip-path"])
    }

    @Test
    fun `strukturierte Definitionen werden als gueltiges SVG serialisiert`() {
        val element = SvgLinie("linie", SvgPunkt(0.0, 0.0), SvgPunkt(10.0, 10.0))
        val grafik = SvgGrafik.standard()
            .mitDefinition(SvgSymbolDefinition("symbol", listOf(element)))
            .mitDefinition(SvgMarkerDefinition("marker", listOf(element)))
            .mitDefinition(SvgClipPfadDefinition("clip", listOf(element)))
            .mitDefinition(SvgMaskenDefinition("maske", listOf(element)))
            .mitDefinition(
                SvgLinearerVerlaufDefinition(
                    "linear",
                    stopps = listOf(SvgFarbStopp(0.0, "#000"), SvgFarbStopp(1.0, "#fff")),
                ),
            )
            .mitDefinition(
                SvgFilterDefinition(
                    "filter",
                    listOf(SvgFilterPrimitiv(SvgFilterPrimitivArt.GaussianBlur, mapOf("stdDeviation" to "4"))),
                ),
            )
            .mitElement(SvgVerwendung("use", "symbol"))

        val svg = grafik.zuSvg()
        assertTrue(svg.contains("<symbol id=\"symbol\">"))
        assertTrue(svg.contains("<marker id=\"marker\""))
        assertTrue(svg.contains("<clipPath id=\"clip\">"))
        assertTrue(svg.contains("<mask id=\"maske\">"))
        assertTrue(svg.contains("<linearGradient id=\"linear\""))
        assertTrue(svg.contains("<feGaussianBlur stdDeviation=\"4\"/>"))
        assertTrue(svg.contains("<use id=\"use\" href=\"#symbol\""))
    }

    @Test
    fun `kombinieren schreibt kollidierende Definitionsreferenzen um`() {
        val links = SvgGrafik.standard()
            .mitDefinition(SvgSymbolDefinition("punkt", listOf(SvgKreis("a", SvgPunkt(0.0, 0.0), 1.0))))
            .mitElement(SvgVerwendung("links", "punkt"))
        val rechts = SvgGrafik.standard()
            .mitDefinition(SvgSymbolDefinition("punkt", listOf(SvgKreis("b", SvgPunkt(0.0, 0.0), 2.0))))
            .mitElement(SvgVerwendung("rechts", "punkt"))

        val kombiniert = links.kombiniere(rechts)
        val rechtsUse = kombiniert.findeElement("rechts") as? SvgVerwendung

        assertNotNull(rechtsUse)
        assertEquals("punkt-2", rechtsUse.referenzId)
        assertTrue(kombiniert.definitionen.any { it.id == "punkt-2" })
        assertTrue(kombiniert.zuSvg().contains("href=\"#punkt-2\""))
    }

    @Test
    fun `Elemente koennen dupliziert entfernt und in der Zeichenreihenfolge bewegt werden`() {
        val basis = SvgGrafik.standard()
            .mitElement(SvgKreis("a", SvgPunkt(0.0, 0.0), 1.0))
            .mitElement(SvgKreis("b", SvgPunkt(0.0, 0.0), 1.0))

        val dupliziert = basis.dupliziereElement("a", "a-kopie")
        assertEquals(listOf("a", "a-kopie", "b"), dupliziert.elemente.map { it.id })

        val vorne = dupliziert.ordneElement("a", +1)
        assertEquals(listOf("a-kopie", "a", "b"), vorne.elemente.map { it.id })

        val entfernt = vorne.entferneElement("a-kopie")
        assertEquals(listOf("a", "b"), entfernt.elemente.map { it.id })
    }

    @Test
    fun `Titel Beschreibung und Metadaten werden escaped serialisiert`() {
        val svg = SvgGrafik(
            titel = "A < B",
            beschreibung = "x & y",
            metadaten = "quelle=\"Atlas\"",
        ).zuSvg()

        assertTrue(svg.contains("<title>A &lt; B</title>"))
        assertTrue(svg.contains("<desc>x &amp; y</desc>"))
        assertTrue(svg.contains("<metadata>quelle=&quot;Atlas&quot;</metadata>"))
    }
}
