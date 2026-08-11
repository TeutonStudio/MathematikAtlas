package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class KartenDatenTypJsonTest {
    @Test fun `Format 8 erhält Typvertrag Literale Vereinigung und Inferenz`() {
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.methode"),
            vertrag = AnschlussVertrag(
                typ = TypAusdruck.Parameterisiert(
                    TypId("math.methode"),
                    listOf(
                        TypAusdruck.Parameterisiert(
                            TypKernIds.Tupel,
                            listOf(
                                TypAusdruck.Parameterisiert(
                                    TypId("math.matrix"),
                                    listOf(
                                        TypAusdruck.Atom(TypId("math.zahl.reell")),
                                        TypAusdruck.Literal("3"),
                                        TypAusdruck.Literal("4"),
                                    ),
                                ),
                            ),
                        ),
                        TypAusdruck.Vereinigung(
                            listOf(
                                TypAusdruck.Atom(TypId("math.zahl.reell")),
                                TypAusdruck.Atom(TypId("math.zahl.komplex")),
                            ),
                        ),
                    ),
                ),
                anforderungen = listOf(
                    TypAnforderung.Struktur("topologie"),
                    TypAnforderung.Axiom("stetigkeit"),
                ),
            ),
            typInferenz = TypInferenzRegel.Priorisierung(
                eingänge = listOf("a", "b"),
                prioritäten = listOf(TypAusdruck.Atom(TypId("math.methode"))),
            ),
        )
        val karte = KartenDaten(
            name = "Typen",
            knoten = listOf(KnotenDaten(art = "test", name = "Knoten", anschlüsse = listOf(anschluss))),
        )

        val json = KartenDatenJson.schreibe(karte)
        val geladen = KartenDatenJson.lese(json)
        val geladenAnschluss = geladen.knoten.single().anschlüsse.single()

        assertEquals(8, KartenDatenJson.formatVersion(json))
        assertEquals(anschluss.vertrag, geladenAnschluss.vertrag)
        assertEquals(anschluss.typInferenz, geladenAnschluss.typInferenz)
    }

    @Test fun `Format 7 ohne Typfelder bleibt lesbar`() {
        val format7 = """
            {
              "formatVersion": 7,
              "id": "karte-alt",
              "name": "Alt",
              "version": 1,
              "erstelltAm": 0,
              "archiviert": false,
              "ansicht": {"x": 0, "y": 0, "zoom": 1},
              "knoten": [
                {
                  "id": "knoten-alt",
                  "art": "test",
                  "name": "Alt",
                  "position": {"x": 0, "y": 0},
                  "größe": {"breite": 180, "höhe": 100},
                  "parameter": {},
                  "eigenschaften": {},
                  "anschlüsse": [
                    {
                      "id": "anschluss-alt",
                      "name": "wert",
                      "richtung": "Ausgang",
                      "kante": "Rechts",
                      "art": "objekt",
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

        val geladen = KartenDatenJson.lese(format7)

        assertEquals(7, KartenDatenJson.formatVersion(format7))
        assertEquals(TypAusdruck.Unbekannt, geladen.knoten.single().anschlüsse.single().vertrag.typ)
        assertNull(geladen.knoten.single().anschlüsse.single().typInferenz)
    }
}
