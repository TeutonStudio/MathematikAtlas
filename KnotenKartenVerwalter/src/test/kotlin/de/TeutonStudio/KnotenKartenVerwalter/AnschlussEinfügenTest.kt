package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class AnschlussEinfügenTest {
    private val art = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(art)))

    @Test fun `Anschluss wird oberhalb eingefügt und bestehende Verbindung bleibt erhalten`() {
        val erster = eingang("a", 0, erweiterbar = true)
        val zweiter = eingang("b", 1, erweiterbar = true)
        val ziel = KnotenDaten(
            art = "test.operator",
            name = "Operator",
            anschlüsse = listOf(erster, zweiter),
            parameter = mapOf("festeEingänge" to "2"),
        )
        val ausgang = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = art.id,
        )
        val quelle = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(ausgang))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, ausgang.id),
            zu = AnschlussVerweis(ziel.id, zweiter.id),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, ziel), verbindungen = listOf(verbindung)),
            prüfung,
        )

        zustand.fügeAnschlussRelativEin(
            AnschlussVerweis(ziel.id, zweiter.id),
            AnschlussEinfügePosition.Davor,
        )

        val aktualisiert = zustand.karte.knoten.single { it.id == ziel.id }
        val eingänge = aktualisiert.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        assertEquals(listOf(erster.id, eingänge[1].id, zweiter.id), eingänge.map { it.id })
        assertEquals("input3", eingänge[1].name)
        assertFalse(eingänge[1].dynamischErzeugt)
        assertEquals("3", aktualisiert.parameter.getValue("festeEingänge"))
        assertEquals(listOf(verbindung), zustand.karte.verbindungen)
    }

    @Test fun `Undo und Redo stellen dieselbe neue Anschluss-ID wieder her`() {
        val erster = eingang("a", 0, erweiterbar = true)
        val zweiter = eingang("b", 1, erweiterbar = true)
        val knoten = KnotenDaten(art = "test", name = "Operator", anschlüsse = listOf(erster, zweiter))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(knoten)), prüfung)

        zustand.fügeAnschlussRelativEin(
            AnschlussVerweis(knoten.id, erster.id),
            AnschlussEinfügePosition.Danach,
        )
        val neueId = zustand.karte.knoten.single().anschlüsse.single { it.id !in setOf(erster.id, zweiter.id) }.id

        zustand.rückgängig()
        assertEquals(listOf(erster.id, zweiter.id), zustand.karte.knoten.single().anschlüsse.map { it.id })
        zustand.wiederholen()
        assertTrue(zustand.karte.knoten.single().anschlüsse.any { it.id == neueId })
    }

    @Test fun `Fester Operator wird nicht verändert und erzeugt keinen Undo-Schritt`() {
        val fest = eingang("dividend", 0, erweiterbar = false)
        val knoten = KnotenDaten(art = "mathematik.division", name = "Division", anschlüsse = listOf(fest))
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(knoten)), prüfung)
        val ref = AnschlussVerweis(knoten.id, fest.id)

        assertFalse(zustand.kannAnschlussRelativEinfügen(ref))
        assertFalse(zustand.kannAnschlussVernichten(ref))
        zustand.fügeAnschlussRelativEin(ref, AnschlussEinfügePosition.Danach)
        zustand.vernichteAnschluss(ref)

        assertEquals(listOf(fest), zustand.karte.knoten.single().anschlüsse)
        assertFalse(zustand.kannRückgängig())
    }

    @Test fun `Verbundener Anschluss und nur seine Verbindung werden vernichtet`() {
        val erster = eingang("a", 0, erweiterbar = true)
        val zweiter = eingang("b", 1, erweiterbar = true)
        val dritter = eingang("c", 2, erweiterbar = true)
        val ziel = KnotenDaten(
            art = "test.operator",
            name = "Operator",
            anschlüsse = listOf(erster, zweiter, dritter),
            parameter = mapOf("festeEingänge" to "3"),
        )
        val ausgangA = ausgang("a")
        val ausgangB = ausgang("b")
        val quelle = KnotenDaten(
            art = "test.quelle",
            name = "Quelle",
            anschlüsse = listOf(ausgangA, ausgangB),
        )
        val zuVernichten = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, ausgangA.id),
            zu = AnschlussVerweis(ziel.id, zweiter.id),
        )
        val zuErhalten = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, ausgangB.id),
            zu = AnschlussVerweis(ziel.id, dritter.id),
        )
        val vorher = KartenDaten(
            name = "Test",
            knoten = listOf(quelle, ziel),
            verbindungen = listOf(zuVernichten, zuErhalten),
        )
        val zustand = KartenEditorZustand(vorher, prüfung)
        val ref = AnschlussVerweis(ziel.id, zweiter.id)

        assertTrue(zustand.kannAnschlussVernichten(ref))
        zustand.vernichteAnschluss(ref)

        val aktualisiert = zustand.karte.knoten.single { it.id == ziel.id }
        val eingänge = aktualisiert.anschlüsse.sortedBy { it.reihenfolge }
        assertEquals(listOf(erster.id, dritter.id), eingänge.map { it.id })
        assertEquals(listOf(0, 1), eingänge.map { it.reihenfolge })
        assertEquals("2", aktualisiert.parameter.getValue("festeEingänge"))
        assertEquals(listOf(zuErhalten), zustand.karte.verbindungen)

        zustand.rückgängig()
        assertEquals(vorher, zustand.karte)
        zustand.wiederholen()
        assertFalse(zustand.karte.knoten.single { it.id == ziel.id }.anschlüsse.any { it.id == zweiter.id })
        assertEquals(listOf(zuErhalten), zustand.karte.verbindungen)
    }

    @Test fun `Mindestzahl fester Eingänge verhindert Vernichtung`() {
        val erster = eingang("a", 0, erweiterbar = true)
        val zweiter = eingang("b", 1, erweiterbar = true)
        val knoten = KnotenDaten(
            art = "test.operator",
            name = "Operator",
            anschlüsse = listOf(erster, zweiter),
            parameter = mapOf("festeEingänge" to "2"),
        )
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(knoten)), prüfung)
        val ref = AnschlussVerweis(knoten.id, erster.id)

        assertFalse(zustand.kannAnschlussVernichten(ref))
        zustand.vernichteAnschluss(ref)

        assertEquals(listOf(erster, zweiter), zustand.karte.knoten.single().anschlüsse)
        assertFalse(zustand.kannRückgängig())
    }

    @Test fun `Dynamischer Eingang kann zusätzlich zu zwei festen Eingängen vernichtet werden`() {
        val erster = eingang("a", 0, erweiterbar = true)
        val zweiter = eingang("b", 1, erweiterbar = true)
        val dynamisch = eingang("input3", 2, erweiterbar = true, dynamisch = true)
        val ziel = KnotenDaten(
            art = "test.operator",
            name = "Operator",
            anschlüsse = listOf(erster, zweiter, dynamisch),
            parameter = mapOf("festeEingänge" to "2"),
        )
        val ausgang = ausgang("wert")
        val quelle = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(ausgang))
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, ausgang.id),
            zu = AnschlussVerweis(ziel.id, dynamisch.id),
        )
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, ziel), verbindungen = listOf(verbindung)),
            prüfung,
        )
        val ref = AnschlussVerweis(ziel.id, dynamisch.id)

        assertTrue(zustand.kannAnschlussVernichten(ref))
        zustand.vernichteAnschluss(ref)

        val aktualisiert = zustand.karte.knoten.single { it.id == ziel.id }
        assertEquals(listOf(erster.id, zweiter.id), aktualisiert.anschlüsse.map { it.id })
        assertEquals("2", aktualisiert.parameter.getValue("festeEingänge"))
        assertTrue(zustand.karte.verbindungen.isEmpty())
    }

    private fun eingang(
        name: String,
        reihenfolge: Int,
        erweiterbar: Boolean,
        dynamisch: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art.id,
        reihenfolge = reihenfolge,
        kannSichErweitern = erweiterbar,
        dynamischErzeugt = dynamisch,
    )

    private fun ausgang(name: String) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art.id,
    )
}
