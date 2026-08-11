package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import org.junit.Assert.*
import org.junit.Test

class TypPersistenzTest {
    @Test
    fun format_8_erhaelt_typvertrag_anforderungen_und_inferenz() {
        val r = TypAusdruck.Atom(TypId("mathematik.zahl.R"))
        val c = TypAusdruck.Atom(TypId("mathematik.zahl.C"))
        val anschluss = AnschlussDaten(
            id = AnschlussId("out"),
            name = "Ausgang",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.zahl"),
            vertrag = AnschlussVertrag(
                typ = TypAusdruck.Vereinigung(listOf(r, c)),
                anforderungen = setOf(
                    TypAnforderung(TypAnforderungsArt.Struktur, TypId("struktur.topologie")),
                    TypAnforderung(TypAnforderungsArt.Axiom, TypId("axiom.test")),
                ),
            ),
            typInferenz = TypInferenzRegel.FolgtEingang("x"),
        )
        val karte = KartenDaten(
            id = KartenId("karte"),
            name = "G0.2",
            erstelltAm = 0L,
            knoten = listOf(
                KnotenDaten(
                    id = KnotenId("knoten"),
                    art = "test",
                    name = "Test",
                    anschlüsse = listOf(anschluss),
                ),
            ),
        )

        val json = KartenDatenJson.schreibe(karte)
        assertEquals(8, KartenDatenJson.formatVersion(json))
        val gelesen = KartenDatenJson.lese(json)
        val gelesenAnschluss = gelesen.knoten.single().anschlüsse.single()

        assertEquals(anschluss.vertrag, gelesenAnschluss.vertrag)
        assertEquals(anschluss.typInferenz, gelesenAnschluss.typInferenz)
    }

    @Test
    fun format_7_ohne_semantische_typfelder_bleibt_ladbar() {
        val json = """
            {
              "formatVersion": 7,
              "id": "alt",
              "name": "Altkarte",
              "version": 1,
              "erstelltAm": 0,
              "archiviert": false,
              "ansicht": {"x": 0, "y": 0, "zoom": 1},
              "knoten": [
                {
                  "id": "k",
                  "art": "test",
                  "name": "Alt",
                  "position": {"x": 0, "y": 0},
                  "größe": {"breite": 120, "höhe": 72},
                  "parameter": {},
                  "eigenschaften": {},
                  "anschlüsse": [
                    {
                      "id": "a",
                      "name": "A",
                      "richtung": "Ausgang",
                      "kante": "Rechts",
                      "art": "mathematik.zahl",
                      "reihenfolge": 0,
                      "kannSichErweitern": false,
                      "dynamischErzeugt": false
                    }
                  ]
                }
              ],
              "verbindungen": [],
              "visuelleGruppen": []
            }
        """.trimIndent()

        val gelesen = KartenDatenJson.lese(json)
        val anschluss = gelesen.knoten.single().anschlüsse.single()
        assertEquals(AnschlussVertrag(), anschluss.vertrag)
        assertNull(anschluss.typInferenz)
        assertEquals(AnschlussArtId("mathematik.zahl"), anschluss.art)
    }
}
