package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class KartenKnotenTest {
    @Test fun `öffentliche Anschlüsse folgen der Position auf der Karte`() {
        fun eingang(id: String, name: String, x: Float, y: Float) = KnotenDaten(
            id = KnotenId(id),
            art = "mathematik.kartenEingang",
            name = name,
            position = GraphPunkt(x, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.zahl"))),
        )
        val karte = KartenDaten(name = "Test", knoten = listOf(
            eingang("c", "c", 0f, 200f),
            eingang("b", "b", 200f, 100f),
            eingang("a", "a", 100f, 100f),
        ))

        val anschlüsse = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links)

        assertEquals(listOf("a", "b", "c"), anschlüsse.map { it.name })
        assertEquals(listOf(0, 1, 2), anschlüsse.map { it.reihenfolge })
    }
}
