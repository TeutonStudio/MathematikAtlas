package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
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

        val sichtbar = sichtbareJsonZeilen(text.lines().size, faltungen, setOf(array.startOffset))

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

    @Test
    fun `Listenanalyse erkennt den zugehörigen Schlüssel`() {
        val text = """
            {
              "knoten": [],
              "visuelleGruppen": [
                { "knotenIds": [] }
              ]
            }
        """.trimIndent()

        val listen = analysiereJsonListen(text)

        assertEquals(listOf("knoten", "visuelleGruppen", "knotenIds"), listen.map { it.schlüssel })
    }

    @Test
    fun `Plus ergänzt leere Knotenliste als gültiges JSON`() {
        val text = """
            {
              "formatVersion": 4,
              "id": "karte",
              "name": "Test",
              "version": 1,
              "erstelltAm": 1,
              "archiviert": false,
              "ansicht": { "x": 0, "y": 0, "zoom": 1 },
              "knoten": [],
              "verbindungen": [],
              "visuelleGruppen": []
            }
        """.trimIndent()
        val liste = analysiereJsonListen(text).single { it.schlüssel == "knoten" }

        val eingefügt = fügeJsonListenEintragEin(text, liste, KartenDaten(name = "Test"))

        assertNull(analysiereJson(eingefügt.text).fehler)
        assertTrue(eingefügt.text.contains("mathematik.zahl"))
    }

    @Test
    fun `ID Kontext erkennt Knoten und Anschlusswerte`() {
        val text = """
            {
              "von": {
                "knotenId": "knoten-1",
                "anschlussId": "anschluss-1"
              }
            }
        """.trimIndent()

        val cursor = text.indexOf("anschluss-1") + 3
        val kontext = assertNotNull(jsonIdKontext(text, cursor))

        assertEquals("anschlussId", kontext.schlüssel)
        assertEquals("knoten-1", kontext.knotenId)
        assertEquals("anschluss-1", kontext.aktuellerWert)
    }
}
