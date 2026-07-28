package de.TeutonStudio.MathematikAtlas

import kotlin.test.*

class JsonEditorAnalyseTest {
    @Test
    fun `Faltungen ignorieren Klammern in Zeichenketten und erkennen Verschachtelung`() {
        val text = """
            {
              "text": "[keine Faltung] und {auch nicht}",
              "knoten": [
                {
                  "id": "eins"
                }
              ]
            }
        """.trimIndent()

        val faltungen = analysiereJsonFaltungen(text)

        assertEquals(3, faltungen.size)
        assertEquals(listOf(0, 1, 2), faltungen.map { it.tiefe })
        assertTrue(faltungen.all { it.startZeile < it.endeZeile })
    }

    @Test
    fun `Eingeklappte Faltung überspringt ihre inneren Originalzeilen`() {
        val text = """
            {
              "knoten": [
                {
                  "id": "eins"
                }
              ],
              "name": "Test"
            }
        """.trimIndent()
        val faltungen = analysiereJsonFaltungen(text)
        val array = faltungen.single { text[it.startOffset] == '[' }

        val sichtbar = sichtbareJsonZeilen(
            zeilenAnzahl = text.lines().size,
            faltungen = faltungen,
            eingeklappt = setOf(array.startOffset),
        )

        assertEquals(listOf(1, 2, 7, 8), sichtbar.map { it.originalZeile })
        assertTrue(sichtbar.single { it.originalZeile == 2 }.eingeklappt)
    }

    @Test
    fun `Offset wird in einsbasierte Zeile und Spalte übersetzt`() {
        val text = "erste\nzweite\ndritte"

        assertEquals(JsonPosition(1, 1), offsetZuZeileSpalte(text, 0))
        assertEquals(JsonPosition(2, 1), offsetZuZeileSpalte(text, 6))
        assertEquals(JsonPosition(2, 4), offsetZuZeileSpalte(text, 9))
        assertEquals(JsonPosition(3, 7), offsetZuZeileSpalte(text, text.length))
    }

    @Test
    fun `Unvollständige JSON Eingabe bleibt analysierbar`() {
        val analyse = analysiereJson("{\n  \"name\": \"Test\",\n  \"knoten\": [\n")

        assertNotNull(analyse.fehler)
        assertEquals(4, analyse.zeilenAnzahl)
        assertTrue(analyse.faltungen.isEmpty())
    }
}
