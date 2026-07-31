package de.TeutonStudio.KnotenKartenVerwalter.daten

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KartenKopieTest {
    @Test
    fun `Arbeitskopie ersetzt alle internen Identitäten und erhält die Struktur`() {
        val quelle = KnotenDaten(
            art = "test.quelle",
            name = "Quelle",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = AnschlussArtId("test.wert"),
                ),
            ),
        )
        val ziel = KnotenDaten(
            art = "test.ziel",
            name = "Ziel",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("test.wert"),
                ),
            ),
        )
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
        )
        val gruppe = VisuelleKnotenGruppeDaten(knotenIds = setOf(quelle.id, ziel.id))
        val original = KartenDaten(
            name = "Definition",
            version = 7,
            knoten = listOf(quelle, ziel),
            verbindungen = listOf(verbindung),
            visuelleGruppen = listOf(gruppe),
            archiviert = true,
        )

        val kopie = original.alsNeueKarte("Definition – Kopie")

        assertNotEquals(original.id, kopie.id)
        assertEquals("Definition – Kopie", kopie.name)
        assertEquals(1, kopie.version)
        assertFalse(kopie.archiviert)
        assertEquals(original.knoten.map { it.name }, kopie.knoten.map { it.name })
        assertTrue(original.knoten.map { it.id }.toSet().intersect(kopie.knoten.map { it.id }.toSet()).isEmpty())
        assertTrue(
            original.knoten.flatMap { it.anschlüsse }.map { it.id }.toSet()
                .intersect(kopie.knoten.flatMap { it.anschlüsse }.map { it.id }.toSet())
                .isEmpty(),
        )
        assertNotEquals(original.verbindungen.single().id, kopie.verbindungen.single().id)
        assertNotEquals(original.visuelleGruppen.single().id, kopie.visuelleGruppen.single().id)

        val vorhandeneVerweise = kopie.knoten
            .flatMap { knoten -> knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) } }
            .toSet()
        assertTrue(kopie.verbindungen.all { it.von in vorhandeneVerweise && it.zu in vorhandeneVerweise })
        assertEquals(kopie.knoten.map { it.id }.toSet(), kopie.visuelleGruppen.single().knotenIds)
    }
}
