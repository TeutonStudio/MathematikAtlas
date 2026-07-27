package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.*

class KartenJsonTest {
    @Test fun `Version zwei rundet verschachtelte Eigenschaften`() {
        val eigenschaften = mapOf("konfiguration" to KnotenEigenschaft.Objekt(mapOf(
            "kamera" to KnotenEigenschaft.Objekt(mapOf("zoom" to KnotenEigenschaft.Dezimalzahl(1.25), "aktiv" to KnotenEigenschaft.Wahrheitswert(true))),
            "farben" to KnotenEigenschaft.Liste(listOf(KnotenEigenschaft.Farbe(0xFF2563EB), KnotenEigenschaft.Text("Ozean"))),
        )))
        val karte = KartenDaten(name = "Test", knoten = listOf(KnotenDaten(art = "mathematik.visualisierung", name = "Visualisierung", eigenschaften = eigenschaften)))
        val text = KartenJson.schreibe(karte)
        assertTrue(text.contains("\"formatVersion\": 2"))
        assertEquals(karte, KartenJson.lese(text))
    }

    @Test fun `Version eins ohne Eigenschaften bleibt lesbar`() {
        val text = """{"formatVersion":1,"id":"karte","name":"Alt","version":1,"erstelltAm":1,"ansicht":{"x":0,"y":0,"zoom":1},"knoten":[{"id":"knoten","art":"test","name":"Test","position":{"x":0,"y":0},"größe":{"breite":200,"höhe":100},"parameter":{"wert":"2"},"anschlüsse":[]}],"verbindungen":[]}"""
        val knoten = KartenJson.lese(text).knoten.single()
        assertEquals(mapOf("wert" to "2"), knoten.parameter)
        assertTrue(knoten.eigenschaften.isEmpty())
    }

    @Test fun `Extremwert erhält Modus und Anschluss IDs beim Roundtrip`() {
        val extremwert = MathematikKnotenVorlagen.Minimum.erzeuge(GraphPunkt.Zero)
        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(extremwert)))).knoten.single()

        assertEquals("mathematik.extremwert", gelesen.art)
        assertEquals("minimum", gelesen.parameter.getValue("modus"))
        assertEquals(extremwert.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test fun `Reelles Intervall behält Art und Anschluss IDs beim Roundtrip`() {
        val intervall = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(intervall)))).knoten.single()

        assertEquals("mathematik.reellesIntervall", gelesen.art)
        assertEquals(listOf("untereGrenze", "obereGrenze", "menge"), gelesen.anschlüsse.map { it.name })
        assertEquals(intervall.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }
}
