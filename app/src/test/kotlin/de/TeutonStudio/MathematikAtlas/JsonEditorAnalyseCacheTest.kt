package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class JsonEditorAnalyseCacheTest {
    @Test
    fun `identischer Text wird nur einmal analysiert`() {
        var aufrufe = 0
        val cache = JsonEditorAnalyseCache { text: String ->
            aufrufe++
            text.length
        }

        val zuerst = cache.sofort("abc")
        val erneut = cache.sofort("abc")
        val auftrag = cache.beauftrage("abc")
        val worker = cache.analysiere(auftrag)
        val übernommen = cache.übernehme(worker)

        assertEquals(3, zuerst)
        assertEquals(3, erneut)
        assertEquals(3, übernommen)
        assertEquals(1, aufrufe)
    }

    @Test
    fun `Ergebnis einer alten Textrevision wird verworfen`() {
        val cache = JsonEditorAnalyseCache<String> { it.uppercase() }
        val alt = cache.beauftrage("alt")
        val neu = cache.beauftrage("neu")

        val altesErgebnis = cache.analysiere(alt)
        val neuesErgebnis = cache.analysiere(neu)

        assertNull(cache.übernehme(altesErgebnis))
        assertEquals("NEU", cache.übernehme(neuesErgebnis))
    }

    @Test
    fun `ID Kontext nutzt vorberechnete Bereiche`() {
        val text = """{"von":{"knotenId":"k-1","anschlussId":"a-1"}}"""
        val bereiche = analysiereJsonIdBereicheV2311(text)
        val anschluss = bereiche.single { it.schlüssel == "anschlussId" }

        val kontext = jsonIdKontextV2311(bereiche, anschluss.wertStart)

        assertEquals("anschlussId", kontext?.schlüssel)
        assertEquals("a-1", kontext?.aktuellerWert)
        assertEquals("k-1", kontext?.knotenId)
    }

    @Test
    fun `Cache gibt dieselbe Analyseinstanz zurück`() {
        data class Analyse(val text: String)
        val cache = JsonEditorAnalyseCache(::Analyse)

        val eins = cache.sofort("gleich")
        val zwei = cache.sofort("gleich")

        assertSame(eins, zwei)
    }
}
