package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgErweiterteOperatorenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Operatorregister ist eindeutig und deckt SVG Kategorien breit ab`() {
        val ids = SvgOperatoren.alle.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.size >= 45)
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Transformation" })
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Definitionen" })
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Marker" })
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Clipping & Masken" })
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Verläufe" })
        assertTrue(SvgOperatoren.alle.any { it.kategorie == "Pattern" })
        assertEquals(SvgFilterPrimitivArt.entries.size, SvgOperatoren.alle.count { it.kategorie == "Filter" } - 1)
    }

    @Test
    fun `Verschieben haengt strukturierte Transformation an Ziel an`() {
        val basis = SvgGrafik.standard().mitElement(
            SvgKreis("ziel", SvgPunkt(500.0, 500.0), 20.0),
        )
        val knoten = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgErweiterteOperatoren.Verschieben.id,
        ).copy(parameter = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgErweiterteOperatoren.Verschieben.id,
        ).parameter + ("zielId" to "ziel"))

        val grafik = auswerten(
            knoten,
            mapOf(
                "svg" to BedingterWert(basis),
                "x" to zahl(2),
                "y" to zahl(3),
            ),
        )

        val ziel = assertIs<SvgKreis>(grafik.findeElement("ziel"))
        assertIs<SvgTransformation.Verschieben>(ziel.transformationen.single())
    }

    @Test
    fun `Symbol definieren und verwenden bleibt strukturiert`() {
        val basis = SvgGrafik.standard().mitElement(
            SvgKreis("punkt", SvgPunkt(500.0, 500.0), 20.0),
        )
        val definieren = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgErweiterteOperatoren.SymbolDefinieren.id,
        ).copy(parameter = SvgErweiterteOperatoren.SymbolDefinieren.standardParameter + mapOf(
            SVG_OPERATOR_PARAMETER to SvgErweiterteOperatoren.SymbolDefinieren.id,
            "zielId" to "punkt",
            "definitionId" to "P",
        ))

        val definiert = auswerten(definieren, mapOf("svg" to BedingterWert(basis)))
        assertTrue(definiert.definitionen.any { it is SvgSymbolDefinition && it.id == "P" })

        val verwenden = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgErweiterteOperatoren.SymbolVerwenden.id,
        ).copy(parameter = SvgErweiterteOperatoren.SymbolVerwenden.standardParameter + mapOf(
            SVG_OPERATOR_PARAMETER to SvgErweiterteOperatoren.SymbolVerwenden.id,
            "definitionId" to "P",
        ))

        val verwendet = auswerten(
            verwenden,
            mapOf(
                "svg" to BedingterWert(definiert),
                "x" to zahl(1),
                "y" to zahl(2),
            ),
        )
        assertNotNull(verwendet.elemente.filterIsInstance<SvgVerwendung>().singleOrNull())
        assertTrue(verwendet.zuSvg().contains("href=\"#P\""))
    }

    @Test
    fun `Filterprimitive gleicher ID werden geordnet an Definition angehaengt`() {
        val blurDefinition = SvgOperatoren.finde("filterGaussianBlur")
        val blur = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            blurDefinition.id,
        ).copy(parameter = blurDefinition.standardParameter + mapOf(
            SVG_OPERATOR_PARAMETER to blurDefinition.id,
            "definitionId" to "fx",
            "attribute" to "stdDeviation=3",
        ))
        val mitBlur = auswerten(blur, emptyMap())

        val offsetDefinition = SvgOperatoren.finde("filterOffset")
        val offset = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            offsetDefinition.id,
        ).copy(parameter = offsetDefinition.standardParameter + mapOf(
            SVG_OPERATOR_PARAMETER to offsetDefinition.id,
            "definitionId" to "fx",
            "attribute" to "dx=2;dy=4",
        ))
        val mitOffset = auswerten(offset, mapOf("svg" to BedingterWert(mitBlur)))

        val filter = mitOffset.definitionen.filterIsInstance<SvgFilterDefinition>().single { it.id == "fx" }
        assertEquals(
            listOf(SvgFilterPrimitivArt.GaussianBlur, SvgFilterPrimitivArt.Offset),
            filter.primitive.map { it.art },
        )
    }

    private fun auswerten(
        knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ): SvgGrafik {
        val auswerter = register.finde(SVG_KNOTEN_ART)
        assertNotNull(auswerter)
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext()))
        return ergebnis.ausgaben.getValue("svg").objekt as SvgGrafik
    }

    private fun zahl(wert: Long) = BedingterWert(RationaleZahl.von(wert))
}
