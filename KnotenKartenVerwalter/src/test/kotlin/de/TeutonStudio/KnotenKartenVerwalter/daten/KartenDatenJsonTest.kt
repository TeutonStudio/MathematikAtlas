package de.TeutonStudio.KnotenKartenVerwalter.daten

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KartenDatenJsonTest {
    @Test
    fun `Roundtrip erhält Identitäten Anschlüsse Eigenschaften und Gruppen`() {
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
        assertEquals(KartenDatenJson.FORMAT_VERSION, KartenDatenJson.formatVersion(text))
        assertTrue(text.contains("artAbbildungVonEingang"))
        assertTrue(text.contains("artPriorisiertEingänge"))
    }
}
