package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.vorschauKnotenErsetzen
import de.TeutonStudio.KnotenKartenVerwalter.logik.wendeAn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnotenErsetzungsVorschauTest {
    private val art = AnschlussArtId("test")
    private val quelle = KnotenDaten(
        id = KnotenId("quelle"),
        art = "quelle",
        name = "Quelle",
        anschlüsse = listOf(ausgang("quelle-ausgang")),
    )
    private val ziel = KnotenDaten(
        id = KnotenId("ziel"),
        art = "ziel",
        name = "Ziel",
        anschlüsse = listOf(eingang("behalten", "a"), eingang("entfernen", "b")),
    )
    private val verbindungen = ziel.anschlüsse.mapIndexed { index, anschluss ->
        VerbindungDaten(
            id = VerbindungsId("v$index"),
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(ziel.id, anschluss.id),
        )
    }
    private val karte = KartenDaten(
        name = "Test",
        knoten = listOf(quelle, ziel),
        verbindungen = verbindungen,
    )

    @Test
    fun `Vorschau unterscheidet erhaltene neue und entfallende Anschlüsse`() {
        val kandidat = ziel.copy(
            anschlüsse = listOf(ziel.anschlüsse.first(), eingang("neu", "c")),
        )

        val auswirkung = karte.vorschauKnotenErsetzen(kandidat)

        assertEquals(listOf(AnschlussId("behalten")), auswirkung.erhalteneAnschlüsse.map { it.id })
        assertEquals(listOf(AnschlussId("neu")), auswirkung.hinzugefügteAnschlüsse.map { it.id })
        assertEquals(listOf(AnschlussId("entfernen")), auswirkung.entfallendeAnschlüsse.map { it.id })
        assertEquals(listOf(VerbindungsId("v1")), auswirkung.entfallendeVerbindungen.map { it.id })
        assertTrue(auswirkung.trenntVerbindungen)
    }

    @Test
    fun `Vorschau und tatsächliche Knotenersetzung entfernen dieselben Verbindungen`() {
        val kandidat = ziel.copy(anschlüsse = listOf(ziel.anschlüsse.first()))
        val vorschau = karte.vorschauKnotenErsetzen(kandidat)

        val ersetzt = karte.wendeAn(KartenAktion.KnotenErsetzen(kandidat))
        val tatsächlichEntfernt = karte.verbindungen - ersetzt.verbindungen.toSet()

        assertEquals(vorschau.entfallendeVerbindungen.toSet(), tatsächlichEntfernt.toSet())
    }

    @Test
    fun `Unverbundener entfallender Anschluss erzeugt keine Verbindungswarnung`() {
        val unverbunden = ziel.copy(anschlüsse = ziel.anschlüsse + eingang("frei", "frei"))
        val auswirkung = karte.copy(knoten = listOf(quelle, unverbunden))
            .vorschauKnotenErsetzen(ziel)

        assertEquals(listOf(AnschlussId("frei")), auswirkung.entfallendeAnschlüsse.map { it.id })
        assertEquals(emptyList(), auswirkung.entfallendeVerbindungen)
        assertFalse(auswirkung.trenntVerbindungen)
    }

    @Test
    fun `Noch nicht vorhandener Knoten meldet alle Anschlüsse als neu`() {
        val neu = KnotenDaten(art = "neu", name = "Neu", anschlüsse = listOf(eingang("x", "x")))

        val auswirkung = karte.vorschauKnotenErsetzen(neu)

        assertEquals(neu.anschlüsse, auswirkung.hinzugefügteAnschlüsse)
        assertEquals(emptyList(), auswirkung.entfallendeAnschlüsse)
        assertEquals(emptyList(), auswirkung.entfallendeVerbindungen)
    }

    private fun eingang(id: String, name: String) = AnschlussDaten(
        id = AnschlussId(id),
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
    )

    private fun ausgang(id: String) = AnschlussDaten(
        id = AnschlussId(id),
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )
}
