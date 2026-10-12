package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class DynamischeOperandenReihenfolgeTest {
    private val art = AnschlussArtId("test.wert")
    private fun port(name: String, position: Int, dynamisch: Boolean = false, erweitert: Boolean = true) = AnschlussDaten(
        name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art,
        reihenfolge = position, kannSichErweitern = erweitert, dynamischErzeugt = dynamisch)

    @Test fun `Sortieren erhaelt Identitaet und Kabel mit genau einem Undo-Schritt`() {
        val ports = listOf(port("links", 0), port("rechts", 1), port("dritter", 2, true), port("reserve", 3, true), port("fest", 4, erweitert = false))
        val k = KnotenDaten(art = "test.operation", name = "Operation", anschlüsse = ports)
        val quelle = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val verbindungen = ports.take(3).map { VerbindungDaten(von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id), zu = AnschlussVerweis(k.id, it.id)) }
        val karte = KartenDaten(name = "Testkarte", knoten = listOf(quelle, k), verbindungen = verbindungen)
        assertEquals(listOf(ports.take(3)), dynamischeOperandengruppen(karte, k))
        val ids = ports.take(3).reversed().map { it.id }
        val aktion = assertNotNull(ordneDynamischeOperanden(karte, k.id, ids))
        assertEquals(ports.map { it.id }, karte.wendeAn(aktion).knoten.single { it.id == k.id }.anschlüsse.map { it.id })
        val dauerhaft = karte
        val editor = KartenEditorZustand(dauerhaft, GraphPrüfung(AnschlussArtRegister(emptyList())))
        editor.führeAus(aktion)
        val neu = editor.karte.knoten.single { it.id == k.id }
        assertEquals(ids, neu.anschlüsse.filter { it.id in ids }.sortedBy { it.reihenfolge }.map { it.id })
        assertEquals(ports.map { it.id }, neu.anschlüsse.map { it.id })
        assertEquals(ports.map { it.name }, neu.anschlüsse.map { it.name })
        assertEquals(3, neu.anschlüsse.single { it.name == "reserve" }.reihenfolge)
        assertEquals(4, neu.anschlüsse.single { it.name == "fest" }.reihenfolge)
        assertEquals(verbindungen, editor.karte.verbindungen)
        val sortiert = editor.karte
        val kopie = sortiert.alsNeueKarte()
        val kopierterRechner = kopie.knoten.single { it.art == k.art }
        assertEquals(neu.anschlüsse.map { it.name to it.reihenfolge }, kopierterRechner.anschlüsse.map { it.name to it.reihenfolge })
        assertEquals(verbindungen.size, kopie.verbindungen.size)
        assertTrue(kopie.verbindungen.all { v -> kopie.knoten.any { n -> n.id == v.zu.knotenId && n.anschlüsse.any { it.id == v.zu.anschlussId } } })
        editor.rückgängig()
        assertEquals(dauerhaft, editor.karte)
        assertFalse(editor.kannRückgängig())
        editor.wiederholen()
        assertEquals(sortiert, editor.karte)
        assertEquals(sortiert, KartenDatenJson.lese(KartenDatenJson.schreibe(sortiert)))
    }

    @Test fun `unvollstaendige fremde und doppelte IDs sowie Reserve werden abgelehnt`() {
        val ports = listOf(port("a", 0), port("b", 1), port("reserve", 2, true))
        val k = KnotenDaten(art = "test", name = "Test", anschlüsse = ports)
        val karte = KartenDaten(name = "Testkarte", knoten = listOf(k))
        listOf(listOf(ports[0].id), listOf(ports[0].id, ports[0].id), listOf(ports[1].id, AnschlussId("fremd")), ports.map { it.id }).forEach {
            assertNull(ordneDynamischeOperanden(karte, k.id, it))
        }
        assertNull(ordneDynamischeOperanden(karte, k.id, ports.take(2).map { it.id }))
    }
    @Test fun `gespeicherte Quellen zaehlen als Belegung und werden beim Ordnen nicht entfernt`() {
        val ports = listOf(port("a", 0), port("b", 1), port("c", 2, true))
        val k = KnotenDaten(art = "test", name = "Quellen", anschlüsse = ports)
        val karte = KartenDaten(name = "Quellen", knoten = listOf(k))
        val ids = ports.reversed().map { it.id }
        val aktion = assertNotNull(ordneDynamischeOperanden(karte, k.id, ids, setOf(ports.last().id)))
        val editor = KartenEditorZustand(karte, GraphPrüfung(AnschlussArtRegister(emptyList())))
        editor.führeAus(aktion)
        assertEquals(3, editor.karte.knoten.single().anschlüsse.size)
        val neu = editor.karte
        editor.rückgängig(); assertEquals(karte, editor.karte)
        editor.wiederholen(); assertEquals(neu, editor.karte)
    }

}
