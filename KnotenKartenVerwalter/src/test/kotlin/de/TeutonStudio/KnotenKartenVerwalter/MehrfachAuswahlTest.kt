package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class MehrfachAuswahlTest {
    private val art = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(art)))

    @Test fun `Gruppenauswahl wird gemeinsam verschoben und atomar rückgängig gemacht`() {
        val a = knoten("a", GraphPunkt(0f, 0f))
        val b = knoten("b", GraphPunkt(100f, 20f))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)

        zustand.beginneInteraktion()
        zustand.führeAus(KartenAktion.KnotenVerschieben(a.id, GraphPunkt(15f, 8f)), mitHistorie = false)
        zustand.beendeInteraktion()

        assertEquals(GraphPunkt(15f, 8f), zustand.karte.knoten.first { it.id == a.id }.position)
        assertEquals(GraphPunkt(115f, 28f), zustand.karte.knoten.first { it.id == b.id }.position)
        zustand.rückgängig()
        assertEquals(GraphPunkt(0f, 0f), zustand.karte.knoten.first { it.id == a.id }.position)
        assertEquals(GraphPunkt(100f, 20f), zustand.karte.knoten.first { it.id == b.id }.position)
    }

    @Test fun `Visuelle Gruppe wird beim Löschen eines Mitglieds bereinigt`() {
        val a = knoten("a")
        val b = knoten("b")
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b)), prüfung)
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)
        zustand.gruppiereAuswahlVisuell()
        assertEquals(setOf(a.id, b.id), zustand.karte.visuelleGruppen.single().knotenIds)

        zustand.setzeAuswahlModus(AuswahlModus.Einzeln)
        zustand.wähleKnoten(a.id)
        zustand.löscheAuswahl()
        assertTrue(zustand.karte.visuelleGruppen.isEmpty())
    }

    @Test fun `Duplikation kopiert nur interne Verbindungen mit neuen IDs`() {
        val aus = AnschlussDaten(
            name = "aus",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = art.id,
        )
        val ein = AnschlussDaten(
            name = "ein",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art.id,
        )
        val a = KnotenDaten(art = "test", name = "A", anschlüsse = listOf(aus))
        val b = KnotenDaten(art = "test", name = "B", anschlüsse = listOf(ein))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(a.id, aus.id),
            zu = AnschlussVerweis(b.id, ein.id),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(a, b), verbindungen = listOf(verbindung)),
            prüfung,
        )
        zustand.setzeAuswahlModus(AuswahlModus.Gruppe)
        zustand.wähleKnoten(a.id)
        zustand.wähleKnoten(b.id)

        zustand.dupliziereAuswahl()

        assertEquals(4, zustand.karte.knoten.size)
        assertEquals(2, zustand.karte.verbindungen.size)
        val kopie = zustand.karte.verbindungen.single { it.id != verbindung.id }
        assertNotEquals(a.id, kopie.von.knotenId)
        assertNotEquals(b.id, kopie.zu.knotenId)
        assertEquals(zustand.ausgewählteKnoten, setOf(kopie.von.knotenId, kopie.zu.knotenId))
    }

    private fun knoten(name: String, position: GraphPunkt = GraphPunkt.Zero) =
        KnotenDaten(art = "test", name = name, position = position)
}
