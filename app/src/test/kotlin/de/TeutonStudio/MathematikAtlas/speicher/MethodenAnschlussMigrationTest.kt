package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MethodenAnschlussMigrationTest {
    @Test
    fun `alte methodenanschluesse werden ohne id oder kantenverlust normalisiert`() {
        val ausgang = AnschlussDaten(
            name = "methode",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.funktion.aussage"),
        )
        val eingang = AnschlussDaten(
            name = "methode",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = AnschlussArtId("mathematik.funktion.zahl"),
        )
        val quelle = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(ausgang))
        val ziel = KnotenDaten(art = "test.ziel", name = "Ziel", anschlüsse = listOf(eingang))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, ausgang.id),
            zu = AnschlussVerweis(ziel.id, eingang.id),
        )
        val alt = KartenDaten(name = "Alt", knoten = listOf(quelle, ziel), verbindungen = listOf(verbindung))

        val migriert = alt.migriereMethodenAnschlüsse()

        assertEquals(
            listOf(AnschlussArtId("mathematik.methode"), AnschlussArtId("mathematik.methode")),
            migriert.knoten.flatMap { it.anschlüsse }.map { it.art },
        )
        assertEquals(listOf(ausgang.id, eingang.id), migriert.knoten.flatMap { it.anschlüsse }.map { it.id })
        assertEquals(listOf(verbindung), migriert.verbindungen)
        assertSame(migriert, migriert.migriereMethodenAnschlüsse())
    }
}
