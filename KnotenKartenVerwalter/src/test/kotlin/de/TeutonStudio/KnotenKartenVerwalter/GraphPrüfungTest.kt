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

    @Test fun belegterEingangKannAtomarErsetztWerden() {
        val q1 = knoten("q1", AnschlussRichtung.Ausgang, zahl.id)
        val q2 = knoten("q2", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(q1), zu = ref(ziel))
        val karte = KartenDaten(name = "Test", knoten = listOf(q1, q2, ziel), verbindungen = listOf(bestehend))
        val neu = VerbindungDaten(von = ref(q2), zu = ref(ziel))

        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, neu.von, neu.zu))
        val ersetzt = karte.wendeAn(KartenAktion.VerbindungEinfügen(neu))

        assertEquals(listOf(neu), ersetzt.verbindungen)
    }

    @Test fun zyklusWirdAbgelehnt() {
        val a = knotenMitEinUndAus("a")
        val b = knotenMitEinUndAus("b")
        val ab = VerbindungDaten(von = ref(a, "aus"), zu = ref(b, "ein"))
        val karte = KartenDaten(name = "Test", knoten = listOf(a,b), verbindungen = listOf(ab))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(karte, ref(b,"aus"), ref(a,"ein")))
    }

    @Test fun neueVerbindungErsetztBelegtenEingangMitEinemUndoSchritt() {
        val q1 = knoten("q1", AnschlussRichtung.Ausgang, zahl.id)
        val q2 = knoten("q2", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(q1), zu = ref(ziel))
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(q1, q2, ziel), verbindungen = listOf(bestehend)),
            prüfung,
        )

        zustand.beginneVerbindung(ref(q2))
        assertTrue(zustand.kompatibelMitStart(ref(ziel)))
        zustand.anschlussAngeklickt(ref(ziel))

        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(ref(q2), zustand.karte.verbindungen.single().von)
        assertEquals(ref(ziel), zustand.karte.verbindungen.single().zu)
        zustand.rückgängig()
        assertEquals(listOf(bestehend), zustand.karte.verbindungen)
    }

    @Test fun dragVomBelegtenEingangZiehtDieBestehendeVerbindungUm() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val erstesZiel = knoten("z1", AnschlussRichtung.Eingang, objekt.id)
        val zweitesZiel = knoten("z2", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(quelle), zu = ref(erstesZiel))
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, erstesZiel, zweitesZiel), verbindungen = listOf(bestehend)),
            prüfung,
        )

        zustand.beginneVerbindung(ref(erstesZiel), GraphPunkt.Zero)

        assertEquals(ref(quelle), zustand.verbindungsStart)
        assertTrue(zustand.kompatibelMitStart(ref(zweitesZiel)))
        zustand.anschlussAngeklickt(ref(zweitesZiel))

        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(ref(quelle), zustand.karte.verbindungen.single().von)
        assertEquals(ref(zweitesZiel), zustand.karte.verbindungen.single().zu)
        zustand.rückgängig()
        assertEquals(listOf(bestehend), zustand.karte.verbindungen)
    }

    @Test fun dragVomBelegtenEingangAufHintergrundLöschtDieVerbindung() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(quelle), zu = ref(ziel))
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, ziel), verbindungen = listOf(bestehend)),
            prüfung,
        )

        zustand.beginneVerbindung(ref(ziel), GraphPunkt.Zero)
        zustand.beendeVerbindungsVorschau()

        assertTrue(zustand.karte.verbindungen.isEmpty())
        zustand.rückgängig()
        assertEquals(listOf(bestehend), zustand.karte.verbindungen)
    }

    @Test fun abgebrochenerDragVomBelegtenEingangBehältDieVerbindung() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, objekt.id)
        val bestehend = VerbindungDaten(von = ref(quelle), zu = ref(ziel))
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, ziel), verbindungen = listOf(bestehend)),
            prüfung,
        )

        zustand.beginneVerbindung(ref(ziel), GraphPunkt.Zero)
        zustand.brecheVerbindungsVorschauAb()

        assertEquals(listOf(bestehend), zustand.karte.verbindungen)
        assertNull(zustand.verbindungsStart)
        assertNull(zustand.verbindungsVorschau)
        assertFalse(zustand.kannRückgängig())
    }

    @Test fun aktiverStartBleibtZumAbbrechenAnklickbar() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(quelle)), prüfung)

        zustand.beginneVerbindung(ref(quelle))

        assertTrue(zustand.kompatibelMitStart(ref(quelle)))
        zustand.anschlussAngeklickt(ref(quelle))
        assertNull(zustand.verbindungsStart)
    }

    @Test fun assoziativerKnotenErhältDynamischenEingangErstNachZweiBelegtenFestenEingängen() {
        val quelle1 = knoten("q1", AnschlussRichtung.Ausgang, zahl.id)
        val quelle2 = knoten("q2", AnschlussRichtung.Ausgang, zahl.id)
        val quelle3 = knoten("q3", AnschlussRichtung.Ausgang, zahl.id)
        val operator = KnotenDaten(
            art = "test.addition", name = "Plus", anschlüsse = listOf(
                AnschlussDaten(name = "a", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahl.id, reihenfolge = 0, kannSichErweitern = true),
                AnschlussDaten(name = "b", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahl.id, reihenfolge = 1, kannSichErweitern = true),
            ),
        )
        val zustand = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(quelle1, quelle2, quelle3, operator)), prüfung)

        zustand.beginneVerbindung(ref(quelle1))
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
        zustand.anschlussAngeklickt(ref(operator, "a"))

        zustand.beginneVerbindung(ref(quelle2))
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
        zustand.anschlussAngeklickt(ref(operator, "b"))

        zustand.beginneVerbindung(ref(quelle3))
        val dynamisch = zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.single { it.dynamischErzeugt }
        assertEquals(3, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        zustand.anschlussAngeklickt(AnschlussVerweis(operator.id, dynamisch.id))
        assertEquals(3, zustand.karte.verbindungen.size)
        assertEquals(3, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        val dynamischeVerbindung = zustand.karte.verbindungen.single {
            it.zu == AnschlussVerweis(operator.id, dynamisch.id)
        }
        zustand.führeAus(KartenAktion.VerbindungLöschen(dynamischeVerbindung.id))
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)

        zustand.setzeFesteEingangAnzahl(operator.id, 4)
        assertEquals(4, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
        zustand.setzeFesteEingangAnzahl(operator.id, 2)
        assertEquals(2, zustand.karte.knoten.first { it.id == operator.id }.anschlüsse.size)
    }

    @Test fun isolierenEntferntNurDieVerbindungenDesGewähltenKnotens() {
        val a = knotenMitEinUndAus("a")
        val b = knotenMitEinUndAus("b")
        val c = knotenMitEinUndAus("c")
        val ab = VerbindungDaten(von = ref(a, "aus"), zu = ref(b, "ein"))
        val bc = VerbindungDaten(von = ref(b, "aus"), zu = ref(c, "ein"))
        val karte = KartenDaten(name = "Test", knoten = listOf(a, b, c), verbindungen = listOf(ab, bc))
        val zustand = KartenEditorZustand(karte, prüfung)

        zustand.wähleKnoten(b.id)
        zustand.isoliereAusgewähltenKnoten()

        assertEquals(listOf(a, b, c), zustand.karte.knoten)
        assertTrue(zustand.karte.verbindungen.isEmpty())
        zustand.rückgängig()
        assertEquals(listOf(ab, bc), zustand.karte.verbindungen)
    }

    @Test fun typwechselEntferntInkompatibleKantenUndIstRückgängigMachbar() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, zahl.id)
        val ziel = knoten("z", AnschlussRichtung.Eingang, zahl.id)
        val verbindung = VerbindungDaten(von = ref(quelle), zu = ref(ziel))
        val zustand = KartenEditorZustand(
            KartenDaten(name = "Test", knoten = listOf(quelle, ziel), verbindungen = listOf(verbindung)),
            prüfung,
        )

        zustand.ändereAnschlussArt(ref(quelle), objekt.id)

        val geänderterAnschluss = zustand.karte.knoten.first { it.id == quelle.id }.anschlüsse.single()
        assertEquals(quelle.anschlüsse.single().id, geänderterAnschluss.id)
        assertEquals(objekt.id, geänderterAnschluss.art)
        assertTrue(zustand.karte.verbindungen.isEmpty())

        zustand.rückgängig()
        assertEquals(zahl.id, zustand.karte.knoten.first { it.id == quelle.id }.anschlüsse.single().art)
        assertEquals(listOf(verbindung), zustand.karte.verbindungen)
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
