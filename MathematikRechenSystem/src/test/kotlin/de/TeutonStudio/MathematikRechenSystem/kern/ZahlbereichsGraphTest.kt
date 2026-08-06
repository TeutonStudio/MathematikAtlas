package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZahlbereichsGraphTest {
    @Test
    fun `fundamentale Kette bleibt rueckwaertskompatibel`() {
        assertTrue(
            FundamentaleZahlbereiche.istTeilbereich(
                FundamentalerZahlbereich.GANZ,
                FundamentalerZahlbereich.KOMPLEX,
            ),
        )
        assertFalse(
            FundamentaleZahlbereiche.istTeilbereich(
                FundamentalerZahlbereich.KOMPLEX,
                FundamentalerZahlbereich.REELL,
            ),
        )
        assertEquals(
            FundamentalerZahlbereich.REELL,
            FundamentaleZahlbereiche.kleinsterGemeinsamerBereich(
                listOf(FundamentalerZahlbereich.GANZ, FundamentalerZahlbereich.REELL),
            ),
        )
    }

    @Test
    fun `gaussche ganze Zahlen und reelle Zahlen treffen sich in C`() {
        val ergebnis = StandardZahlbereichsGraph.graph.gemeinsameMinimaleZielbereiche(
            listOf(ZahlbereichsIds.GAUSS_GANZ, ZahlbereichsIds.REELL),
        )

        assertEquals(GemeinsamerBereichStatus.EINDEUTIG, ergebnis.status)
        assertEquals(ZahlbereichsIds.KOMPLEX, ergebnis.bereich)
    }

    @Test
    fun `Standardeinbettungen waehlen Hyperbereiche`() {
        assertEquals(
            ZahlbereichsIds.HYPER_REELL,
            StandardZahlbereichsGraph.graph.gemeinsameMinimaleZielbereiche(
                listOf(ZahlbereichsIds.REELL, ZahlbereichsIds.HYPER_REELL),
            ).bereich,
        )
        assertEquals(
            ZahlbereichsIds.HYPER_KOMPLEX,
            StandardZahlbereichsGraph.graph.gemeinsameMinimaleZielbereiche(
                listOf(ZahlbereichsIds.KOMPLEX, ZahlbereichsIds.HYPER_KOMPLEX),
            ).bereich,
        )
    }

    @Test
    fun `Quaternionen und Hyperkomplexe besitzen ohne Hyperquaternionen kein gemeinsames Ziel`() {
        val ergebnis = StandardZahlbereichsGraph.graph.gemeinsameMinimaleZielbereiche(
            listOf(ZahlbereichsIds.QUATERNION, ZahlbereichsIds.HYPER_KOMPLEX),
        )

        assertEquals(GemeinsamerBereichStatus.NICHT_VORHANDEN, ergebnis.status)
        assertNull(ergebnis.bereich)
    }

    @Test
    fun `Matrixdarstellungen werden nicht als automatische Einbettungen missbraucht`() {
        assertFalse(
            StandardZahlbereichsGraph.graph.istAutomatischErreichbar(
                ZahlbereichsIds.KOMPLEX,
                ZahlbereichsIds.KOMPLEX_ALS_M2_REELL,
            ),
        )
        assertTrue(
            StandardZahlbereichsGraph.graph.relationen().any {
                it.quelle == ZahlbereichsIds.KOMPLEX &&
                    it.ziel == ZahlbereichsIds.KOMPLEX_ALS_M2_REELL &&
                    it.art == BereichsRelationArt.DARSTELLUNG
            },
        )
    }

    @Test
    fun `nicht vergleichbare minimale Zielbereiche bleiben mehrdeutig`() {
        val a = ZahlbereichsId("A")
        val b = ZahlbereichsId("B")
        val x = ZahlbereichsId("X")
        val y = ZahlbereichsId("Y")
        val graph = ZahlbereichsGraph(
            knoten = listOf(a, b, x, y).map { ZahlbereichsKnoten(it, it.wert, it.wert) },
            relationen = listOf(
                BereichsRelation(a, x, BereichsRelationArt.KANONISCHE_EINBETTUNG),
                BereichsRelation(b, x, BereichsRelationArt.KANONISCHE_EINBETTUNG),
                BereichsRelation(a, y, BereichsRelationArt.KANONISCHE_EINBETTUNG),
                BereichsRelation(b, y, BereichsRelationArt.KANONISCHE_EINBETTUNG),
            ),
        )

        val ergebnis = graph.gemeinsameMinimaleZielbereiche(listOf(a, b))

        assertEquals(GemeinsamerBereichStatus.MEHRDEUTIG, ergebnis.status)
        assertEquals(listOf(x, y), ergebnis.alternativen)
    }

    @Test
    fun `Definitionskarten erhalten beide Matrixkorrespondenzen`() {
        val latex = StandardZahlbereichsGraph.darstellungen.map { it.definitionsLatex }

        assertTrue(latex.any { it.contains("a+bi") })
        assertTrue(latex.any { it.contains("a+bi+cj+dk") })
        assertTrue(latex.all { it.contains("pmatrix") })
    }
}
