package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.*

class KartenJsonTest {
    @Test fun `Version vier rundet verschachtelte Eigenschaften`() {
        val eigenschaften = mapOf("konfiguration" to KnotenEigenschaft.Objekt(mapOf(
            "kamera" to KnotenEigenschaft.Objekt(mapOf("zoom" to KnotenEigenschaft.Dezimalzahl(1.25), "aktiv" to KnotenEigenschaft.Wahrheitswert(true))),
            "farben" to KnotenEigenschaft.Liste(listOf(KnotenEigenschaft.Farbe(0xFF2563EB), KnotenEigenschaft.Text("Ozean"))),
        )))
        val karte = KartenDaten(name = "Test", knoten = listOf(KnotenDaten(art = "mathematik.visualisierung", name = "Visualisierung", eigenschaften = eigenschaften)))
        val text = KartenJson.schreibe(karte)
        assertTrue(text.contains("\"formatVersion\": 4"))
        assertEquals(karte, KartenJson.lese(text))
    }

    @Test fun `Visuelle Gruppen bleiben beim Roundtrip erhalten und werden bereinigt`() {
        val a = KnotenDaten(id = KnotenId("a"), art = "test", name = "A")
        val b = KnotenDaten(id = KnotenId("b"), art = "test", name = "B")
        val gruppe = VisuelleKnotenGruppeDaten(VisuelleGruppenId("g"), setOf(a.id, b.id, KnotenId("fehlt")))
        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(
            name = "Test",
            knoten = listOf(a, b),
            visuelleGruppen = listOf(gruppe),
        )))

        assertEquals(setOf(a.id, b.id), gelesen.visuelleGruppen.single().knotenIds)
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

    @Test fun `Reelles Intervall behält Art und vier Eingangs IDs beim Roundtrip`() {
        val intervall = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(intervall)))).knoten.single()

        assertEquals("mathematik.reellesIntervall", gelesen.art)
        assertEquals(listOf("links", "linksOffen", "rechts", "rechtsOffen", "menge"), gelesen.anschlüsse.map { it.name })
        assertEquals(intervall.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test fun `Vektor zu Polynom behält Variable und Anschlussverbindung beim Roundtrip`() {
        val vektor = MathematikKnotenVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val polynom = MathematikKnotenVorlagen.VektorZuPolynom.erzeuge(GraphPunkt(300f, 0f)).copy(parameter = mapOf("variable" to "t"))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(vektor.id, vektor.anschlüsse.single { it.name == "vektor" }.id),
            zu = AnschlussVerweis(polynom.id, polynom.anschlüsse.single { it.name == "vektor" }.id),
        )

        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(vektor, polynom), verbindungen = listOf(verbindung))))

        assertEquals("mathematik.vektorZuPolynom", gelesen.knoten.single { it.id == polynom.id }.art)
        assertEquals("t", gelesen.knoten.single { it.id == polynom.id }.parameter.getValue("variable"))
        assertEquals(polynom.anschlüsse.map { it.id }, gelesen.knoten.single { it.id == polynom.id }.anschlüsse.map { it.id })
        assertEquals(listOf(verbindung), gelesen.verbindungen)
    }

    @Test fun `Darstellungsoptimierung behält abhängige Ausgangsart und LaTeX`() {
        val alias = MathematikKnotenVorlagen.Darstellungsoptimierung.erzeuge(GraphPunkt.Zero)
            .copy(parameter = mapOf("latex" to "u_{1}"))

        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(alias)))).knoten.single()
        val ausgang = gelesen.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("u_{1}", gelesen.parameter.getValue("latex"))
        assertEquals("wert", ausgang.artFolgtEingang)
        assertEquals(alias.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test fun `Fallunterscheidung behält vereinigte Zweigarten`() {
        val fall = MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)

        val gelesen = KartenJson.lese(KartenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(fall)))).knoten.single()
        val ausgang = gelesen.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(listOf("wahr", "lüge"), ausgang.artVereinigtEingänge)
        assertEquals(fall.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

}
