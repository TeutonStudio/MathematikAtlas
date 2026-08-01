package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class KnotenErsetzenTest {
    @Test fun `Knotenersatz entfernt nur Verbindungen zu entfallenen Anschlüssen`() {
        val art = AnschlussArtId("test")
        val quelle = KnotenDaten(art = "quelle", name = "Quelle", anschlüsse = listOf(
            AnschlussDaten(id = AnschlussId("q"), name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
        ))
        val alt = KnotenDaten(id = KnotenId("karte"), art = "gruppe.a", name = "Karte", anschlüsse = listOf(
            AnschlussDaten(id = AnschlussId("behalten"), name = "x", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(id = AnschlussId("entfernen"), name = "y", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
        ))
        val verbindungen = alt.anschlüsse.mapIndexed { index, anschluss -> VerbindungDaten(
            id = VerbindungsId("v$index"),
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(alt.id, anschluss.id),
        ) }
        val neu = alt.copy(art = "gruppe.b", anschlüsse = listOf(alt.anschlüsse.first()))

        val ergebnis = KartenDaten(name = "Test", knoten = listOf(quelle, alt), verbindungen = verbindungen)
            .wendeAn(KartenAktion.KnotenErsetzen(neu))

        assertEquals(listOf(VerbindungsId("v0")), ergebnis.verbindungen.map { it.id })
        assertEquals("gruppe.b", ergebnis.knoten.single { it.id == alt.id }.art)
    }
}
