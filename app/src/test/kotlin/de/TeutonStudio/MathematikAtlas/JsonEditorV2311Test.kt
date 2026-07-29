package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import kotlin.test.*

class JsonEditorV2311Test {
    @Test
    fun `Syntax und Kartenschema werden getrennt geprüft`() {
        val syntax = prüfeJsonV2311("{ kaputt")
        assertNotNull(syntax.syntaxFehler)
        assertNull(syntax.schemaFehler)

        val schema = prüfeJsonV2311("{}")
        assertNull(schema.syntaxFehler)
        assertNotNull(schema.schemaFehler)
    }

    @Test
    fun `Listenanalyse ordnet Array dem Schlüssel zu`() {
        val text = """
            {
              "knoten": [],
              "visuelleGruppen": [
                { "knotenIds": [] }
              ]
            }
        """.trimIndent()

        assertEquals(
            listOf("knoten", "visuelleGruppen", "knotenIds"),
            analysiereJsonListenV2311(text).map { it.schlüssel },
        )
    }

    @Test
    fun `ID Kontext erkennt Anschluss und zugehörigen Knoten`() {
        val text = """
            {
              "von": {
                "knotenId": "knoten-1",
                "anschlussId": "anschluss-1"
              }
            }
        """.trimIndent()
        val cursor = text.indexOf("anschluss-1") + 4

        val kontext = assertNotNull(jsonIdKontextV2311(text, cursor))

        assertEquals("anschlussId", kontext.schlüssel)
        assertEquals("knoten-1", kontext.knotenId)
    }

    @Test
    fun `Listeneinfügung erzeugt weiterhin syntaktisch gültiges JSON`() {
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
        val liste = analysiereJsonListenV2311(text).single { it.schlüssel == "knoten" }

        val eingefügt = fügeJsonListenEintragV2311(text, liste, KartenDaten(name = "Test"))

        assertNull(prüfeJsonV2311(eingefügt.text).syntaxFehler)
        assertTrue(eingefügt.text.contains("mathematik.zahl"))
    }
}
