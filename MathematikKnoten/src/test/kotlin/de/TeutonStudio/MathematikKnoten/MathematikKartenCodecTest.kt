package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MathematikKartenCodecTest {
    @Test
    fun `lesen und laden besitzen bewusst getrennte Migrationsphasen`() {
        val stabileMethodenId = AnschlussId("historisch:methode:stabil")
        val historischerMethodenAnschluss = AnschlussDaten(
            id = stabileMethodenId,
            name = "methode",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.funktion.zahl"),
        )
        val methodenKnoten = KnotenDaten(
            art = "test.methode",
            name = "Historische Methode",
            anschlüsse = listOf(historischerMethodenAnschluss),
        )
        val transponieren = MathematikKnotenVorlagen.TransponiereSpalte.erzeuge(GraphPunkt.Zero)
        val alt = KartenDaten(name = "Alt", knoten = listOf(methodenKnoten, transponieren))
        val roh = KartenDatenJson.schreibe(alt)

        val dekodiert = MathematikKartenCodec.lese(roh)
        val geladen = MathematikKartenCodec.lade(roh)

        assertEquals("mathematik.transponieren", dekodiert.knoten[1].art)
        assertEquals(AnschlussArtId("mathematik.funktion.zahl"), dekodiert.knoten[0].anschlüsse.single().art)
        assertEquals(AnschlussArtId("mathematik.methode"), geladen.knoten[0].anschlüsse.single().art)
        assertEquals(stabileMethodenId, geladen.knoten[0].anschlüsse.single().id)
        assertNotEquals(dekodiert, geladen)
    }

    @Test
    fun `historischer Methodenanschluss bleibt nach Migration und erneutem Speichern stabil`() {
        val stabileId = AnschlussId("methode:ausgang:42")
        val alt = KartenDaten(
            name = "Methoden-Migration",
            knoten = listOf(
                KnotenDaten(
                    art = "test.methode",
                    name = "Historische Methode",
                    anschlüsse = listOf(
                        AnschlussDaten(
                            id = stabileId,
                            name = "methode",
                            richtung = AnschlussRichtung.Ausgang,
                            kante = AnschlussKante.Rechts,
                            art = AnschlussArtId("mathematik.funktion.zahl"),
                        ),
                    ),
                ),
            ),
        )

        val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(alt))
        val neuGespeichert = MathematikKartenCodec.schreibe(geladen)
        val erneut = MathematikKartenCodec.lade(neuGespeichert)
        val anschluss = erneut.knoten.single().anschlüsse.single()

        assertEquals(AnschlussArtId("mathematik.methode"), anschluss.art)
        assertEquals(stabileId, anschluss.id)
        assertEquals(geladen, erneut)
        kotlin.test.assertFalse(neuGespeichert.contains("mathematik.funktion.zahl"))
    }

    @Test
    fun `kanonischer Roundtrip bleibt nach vollständiger Migration stabil`() {
        val alt = KartenDaten(
            name = "Alt",
            knoten = listOf(MathematikKnotenVorlagen.TransponiereZeile.erzeuge(GraphPunkt.Zero)),
        )
        val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(alt))
        val erneut = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(geladen))

        assertEquals(geladen, erneut)
        assertEquals("mathematik.transponieren", erneut.knoten.single().art)
    }
}
