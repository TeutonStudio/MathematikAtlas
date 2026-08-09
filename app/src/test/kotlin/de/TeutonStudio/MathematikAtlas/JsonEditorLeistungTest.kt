package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonEditorLeistungTest {
    @Test
    fun `Zeilenindex bestimmt Position und maximale Laenge einmal pro Revision`() {
        val text = "kurz\nzweite Zeile\n"
        val index = JsonZeilenIndex.erzeuge(text)

        assertEquals(3, index.zeilenAnzahl)
        assertEquals(12, index.maximaleZeilenLänge)
        assertEquals(JsonPosition(1, 1), index.position(0))
        assertEquals(JsonPosition(2, 1), index.position(5))
        assertEquals(JsonPosition(2, 7), index.position(11))
        assertEquals(JsonPosition(3, 1), index.position(text.length))
        assertEquals(11, index.offset(2, 7))
    }

    @Test
    fun `Zeilenindex begrenzt Positionen auf gueltige Dokumentbereiche`() {
        val index = JsonZeilenIndex.erzeuge("abc\nde")

        assertEquals(JsonPosition(1, 1), index.position(-10))
        assertEquals(JsonPosition(2, 3), index.position(999))
        assertEquals(3, index.offset(1, 99))
        assertEquals(6, index.offset(99, 99))
    }

    @Test
    fun `Zeilenrand umfasst bei grossen Dokumenten nur Viewport und Puffer`() {
        val bereich = sichtbarerJsonZeilenbereich(
            scrollYpx = 200_000,
            viewportHöhePx = 400,
            zeilenHöhePx = 20f,
            zeilenAnzahl = 20_000,
            innenabstandPx = 8f,
        )

        assertTrue(bereich.ersteZeile in 9_999..10_001)
        assertTrue(bereich.letzteZeile in 10_020..10_022)
        assertTrue(bereich.anzahl <= 24)
    }

    @Test
    fun `Sichtbarer Bereich bleibt an Dokumentgrenzen gueltig`() {
        assertEquals(
            JsonSichtbarerZeilenbereich(1, 6),
            sichtbarerJsonZeilenbereich(0, 100, 20f, 6),
        )
        assertEquals(
            JsonSichtbarerZeilenbereich(6, 6),
            sichtbarerJsonZeilenbereich(10_000, 100, 20f, 6),
        )
    }

    @Test
    fun `Zeilenrand folgt der Schriftgroessenskalierung des Textfelds`() {
        assertEquals(20f, jsonZeilenHöheDp(1f))
        assertEquals(30f, jsonZeilenHöheDp(1.5f))
    }

    @Test
    fun `Analysemodi wechseln an Datei- und Zeilengrenzen`() {
        assertEquals(
            JsonAnalyseModus.Vollständig,
            jsonAnalyseModus(JSON_VOLLSTAENDIG_MAX_BYTES, JSON_VOLLSTAENDIG_MAX_ZEILEN),
        )
        assertEquals(
            JsonAnalyseModus.Verzögert,
            jsonAnalyseModus(JSON_VOLLSTAENDIG_MAX_BYTES + 1, JSON_VOLLSTAENDIG_MAX_ZEILEN),
        )
        assertEquals(
            JsonAnalyseModus.Verzögert,
            jsonAnalyseModus(JSON_VERZOEGER_MAX_BYTES, JSON_VERZOEGER_MAX_ZEILEN),
        )
        assertEquals(
            JsonAnalyseModus.Reduziert,
            jsonAnalyseModus(JSON_VERZOEGER_MAX_BYTES + 1, 1),
        )
        assertEquals(
            JsonAnalyseModus.Reduziert,
            jsonAnalyseModus(1, JSON_VERZOEGER_MAX_ZEILEN + 1),
        )
    }

    @Test
    fun `Reduzierte Pruefung laesst teure Komfortanalysen aus`() {
        val text = "{\n  \"knoten\": []\n}"
        val prüfung = reduzierteJsonPrüfungV2311(text)

        assertFalse(prüfung.vollständig)
        assertEquals(text, prüfung.analysierterText)
        assertTrue(prüfung.tokens.isEmpty())
        assertTrue(prüfung.listen.isEmpty())
        assertTrue(prüfung.idBereiche.isEmpty())
    }
}
