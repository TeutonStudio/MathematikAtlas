package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class GraphPrüfungTest {
    private val objekt = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val zahl = AnschlussArt(AnschlussArtId("zahl"), "Zahl", objekt.id)
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(objekt, zahl)))

    @Test fun unterartDarfAnOberartAngeschlossenWerden() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val karte = KartenDaten(name = "Test", knoten = listOf(quelle, ziel))
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, ref(quelle), ref(ziel)))
    }

    @Test fun zweiterEingangWirdAbgelehnt() {
        val q1 = knoten("q1", AnschlussRichtung.Ausgang, zahl.id)
        val q2 = knoten("q2", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(q1), zu = ref(ziel))
        val karte = KartenDaten(name = "Test", knoten = listOf(q1, q2, ziel), verbindungen = listOf(bestehend))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(karte, ref(q2), ref(ziel)))
    }

    @Test fun zyklusWirdAbgelehnt() {
        val a = knotenMitEinUndAus("a")
        val b = knotenMitEinUndAus("b")
        val ab = VerbindungDaten(von = ref(a, "aus"), zu = ref(b, "ein"))
        val karte = KartenDaten(name = "Test", knoten = listOf(a,b), verbindungen = listOf(ab))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(karte, ref(b,"aus"), ref(a,"ein")))
    }

    @Test fun assoziativerKnotenErhältNurBeimZiehenEinenDynamischenEingang() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val operator = KnotenDaten(
            art = "test.addition", name = "Plus", anschlüsse = listOf(
                AnschlussDaten(name = "a", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahl.id, reihenfolge = 0, kannSichErweitern = true),
                AnschlussDaten(name = "b", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahl.id, reihenfolge = 1, kannSichErweitern = true),
            ),
        )
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(quelle, operator)), prüfung)

        zustand.beginneVerbindung(ref(quelle))
        val dynamisch = zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.single { it.dynamischErzeugt }
        assertEquals(3, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        zustand.anschlussAngeklickt(AnschlussVerweis(operator.id, dynamisch.id))
        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(3, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        zustand.führeAus(KartenAktion.VerbindungLöschen(zustand.karte.verbindungen.single().id))
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        zustand.setzeFesteEingangAnzahl(operator.id, 4)
        assertEquals(4, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
        zustand.setzeFesteEingangAnzahl(operator.id, 2)
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
    }

    private fun knoten(name: String, richtung: AnschlussRichtung, art: AnschlussArtId) = KnotenDaten(
        art = "test", name = name, anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = richtung, kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts, art = art)),
    )
    private fun knotenMitEinUndAus(name: String) = KnotenDaten(art="test", name=name, anschlüsse=listOf(
        AnschlussDaten(name="ein", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=zahl.id),
        AnschlussDaten(name="aus", richtung=AnschlussRichtung.Ausgang, kante=AnschlussKante.Rechts, art=zahl.id),
    ))
    private fun ref(k: KnotenDaten, name: String = "wert") = AnschlussVerweis(k.id, k.anschlüsse.first { it.name == name }.id)
}
