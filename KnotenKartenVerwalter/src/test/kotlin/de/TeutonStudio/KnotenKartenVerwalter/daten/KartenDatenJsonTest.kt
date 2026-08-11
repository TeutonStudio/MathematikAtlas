package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.TypSystem.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KartenDatenJsonTest {
    @Test
    fun `Roundtrip erhält Identitäten Anschlüsse Eigenschaften Gruppen und Typverträge`() {
        val reell = TypAusdruck.Atom(TypId("mathematik.zahl.reell"))
        val komplex = TypAusdruck.Atom(TypId("mathematik.zahl.komplex"))
        val quelle = KnotenDaten(
            id = KnotenId("quelle"),
            art = "test.quelle",
            name = "Quelle",
            position = GraphPunkt(12f, 34f),
            größe = GraphGröße(220f, 110f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("quelle-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = AnschlussArtId("mathematik.zahl"),
                    zulässigeArten = setOf(AnschlussArtId("mathematik.zahl")),
                    vertrag = AnschlussVertrag(reell),
                ),
            ),
            parameter = mapOf("wert" to "2"),
            eigenschaften = mapOf(
                "titel" to KnotenEigenschaft.Text("Beispiel"),
                "werte" to KnotenEigenschaft.Liste(
                    listOf(KnotenEigenschaft.Ganzzahl(1), KnotenEigenschaft.Wahrheitswert(true)),
                ),
            ),
        )
        val ziel = KnotenDaten(
            id = KnotenId("ziel"),
            art = "test.ziel",
            name = "Ziel",
            position = GraphPunkt(340f, 34f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("ziel-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("mathematik.objekt"),
                    artFolgtEingang = "wert",
                    artVereinigtEingänge = listOf("wert"),
                    artAbbildungVonEingang = AnschlussArtAbbildung(
                        eingang = "wert",
                        abbildung = mapOf(
                            AnschlussArtId("mathematik.zahl") to AnschlussArtId("mathematik.objekt"),
                        ),
                    ),
                    artPriorisiertEingänge = AnschlussArtPriorisierung(
                        eingänge = listOf("wert"),
                        prioritäten = listOf(AnschlussArtId("mathematik.methode")),
                    ),
                    vertrag = AnschlussVertrag(
                        typ = TypAusdruck.Vereinigung(listOf(reell, komplex)),
                        anforderungen = listOf(TypAnforderung("struktur.test", mapOf("rang" to "2"))),
                    ),
                    typInferenz = TypInferenzRegel.FolgtEingang("wert"),
                ),
            ),
        )
        val karte = KartenDaten(
            id = KartenId("roundtrip"),
            name = "Roundtrip",
            version = 3,
            erstelltAm = 1234L,
            knoten = listOf(quelle, ziel),
            verbindungen = listOf(
                VerbindungDaten(
                    id = VerbindungsId("kante"),
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                ),
            ),
            visuelleGruppen = listOf(
                VisuelleKnotenGruppeDaten(
                    id = VisuelleGruppenId("gruppe"),
                    knotenIds = setOf(quelle.id, ziel.id),
                    titel = "Gruppe",
                    position = GraphPunkt(-10f, -30f),
                    größe = GraphGröße(640f, 320f),
                ),
            ),
            ansicht = AnsichtsFenster(GraphPunkt(8f, 9f), 1.25f),
        )

        val text = KartenDatenJson.schreibe(karte)
        val gelesen = KartenDatenJson.lese(text)

        assertEquals(karte, gelesen)
        assertEquals(8, KartenDatenJson.FORMAT_VERSION)
        assertEquals(KartenDatenJson.FORMAT_VERSION, KartenDatenJson.formatVersion(text))
        assertTrue(text.contains("artAbbildungVonEingang"))
        assertTrue(text.contains("artPriorisiertEingänge"))
        assertTrue(text.contains("typInferenz"))
        assertTrue(text.contains("struktur.test"))
    }

    @Test
    fun `Format 7 ohne Typvertrag bleibt lesbar und unbekannt`() {
        val alt = """
            {
              "formatVersion": 7,
              "id": "alt",
              "name": "Alt",
              "version": 1,
              "knoten": [{
                "id": "k",
                "art": "test",
                "name": "K",
                "position": {"x":0,"y":0},
                "größe": {"breite":200,"höhe":100},
                "anschlüsse": [{
                  "id":"a",
                  "name":"wert",
                  "richtung":"Ausgang",
                  "kante":"Rechts",
                  "art":"mathematik.zahl",
                  "reihenfolge":0
                }]
              }],
              "verbindungen": [],
              "visuelleGruppen": []
            }
        """.trimIndent()

        val gelesen = KartenDatenJson.lese(alt)
        assertEquals(TypAusdruck.Unbekannt, gelesen.knoten.single().anschlüsse.single().vertrag.typ)
    }
}
