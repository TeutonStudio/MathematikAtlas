package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import kotlin.test.*

class DivisionMigrationV240Test {
    @Test
    fun `alte Division erhält Nullfalleingang und verliert den Aussageausgang`() {
        val dividend = AnschlussDaten(name = "dividend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)
        val divisor = AnschlussDaten(name = "divisor", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1)
        val wert = AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)
        val divisorNull = AnschlussDaten(name = "divisorNull", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Aussage.id, reihenfolge = 1)
        val division = KnotenDaten(id = KnotenId("division"), art = "mathematik.division", name = "Division", anschlüsse = listOf(dividend, divisor, wert, divisorNull))

        val migriert = migriereDivisionV240(KartenDaten(name = "Alt", knoten = listOf(division)))
        val anschlüsse = migriert.knoten.single().anschlüsse

        assertEquals(listOf("dividend", "divisor", "fallsNennerNull"), anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.map { it.name })
        assertEquals(listOf("wert"), anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name })
        assertEquals(dividend.id, anschlüsse.single { it.name == "dividend" }.id)
        assertEquals(divisor.id, anschlüsse.single { it.name == "divisor" }.id)
        assertEquals(wert.id, anschlüsse.single { it.name == "wert" }.id)
    }

    @Test
    fun `Divisionsmigration ist idempotent`() {
        val division = de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Neu", knoten = listOf(division))
        assertEquals(karte, migriereDivisionV240(migriereDivisionV240(karte)))
    }
    @Test
    fun `Verbindungen des alten Nullaussageausgangs werden auf Gleichheit umgeleitet`() {
        val divisorQuelle = de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(0f, 0f)).copy(
            id = KnotenId("divisor-quelle"),
            parameter = mapOf("wert" to "2"),
        )
        val dividend = AnschlussDaten(name = "dividend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)
        val divisor = AnschlussDaten(name = "divisor", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1)
        val wert = AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)
        val divisorNull = AnschlussDaten(name = "divisorNull", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Aussage.id, reihenfolge = 1)
        val division = KnotenDaten(id = KnotenId("division"), art = "mathematik.division", name = "Division", anschlüsse = listOf(dividend, divisor, wert, divisorNull))
        val ziel = KnotenDaten(
            id = KnotenId("ziel"),
            art = "test.aussage-ziel",
            name = "Aussageziel",
            anschlüsse = listOf(
                AnschlussDaten(name = "aussage", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Aussage.id),
            ),
        )
        val quelleAusgang = divisorQuelle.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        val zielEingang = ziel.anschlüsse.single()
        val karte = KartenDaten(
            name = "Alt mit Nullaussage",
            knoten = listOf(divisorQuelle, division, ziel),
            verbindungen = listOf(
                VerbindungDaten(
                    id = VerbindungsId("divisor"),
                    von = AnschlussVerweis(divisorQuelle.id, quelleAusgang.id),
                    zu = AnschlussVerweis(division.id, divisor.id),
                ),
                VerbindungDaten(
                    id = VerbindungsId("nullaussage"),
                    von = AnschlussVerweis(division.id, divisorNull.id),
                    zu = AnschlussVerweis(ziel.id, zielEingang.id),
                ),
            ),
        )

        val migriert = migriereDivisionV240(karte)
        val gleichheit = migriert.knoten.single { it.art == "mathematik.gleichheit" }
        val aussageAusgang = gleichheit.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertTrue(migriert.knoten.any { it.art == "mathematik.zahl" && it.id != divisorQuelle.id && it.parameter["wert"] == "0" })
        assertTrue(migriert.verbindungen.any {
            it.id == VerbindungsId("nullaussage") && it.von == AnschlussVerweis(gleichheit.id, aussageAusgang.id) && it.zu == AnschlussVerweis(ziel.id, zielEingang.id)
        })
        assertTrue(migriert.verbindungen.any {
            it.von == AnschlussVerweis(divisorQuelle.id, quelleAusgang.id) && it.zu.knotenId == gleichheit.id
        })
    }

}
