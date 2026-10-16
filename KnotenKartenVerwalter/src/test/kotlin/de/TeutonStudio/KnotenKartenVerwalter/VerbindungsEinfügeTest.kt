package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class VerbindungsEinfügeTest {
    private val art = AnschlussArtId("test")
    private val andere = AnschlussArtId("anders")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test"), AnschlussArt(andere, "Andere"))))
    private fun knoten(name: String, richtung: AnschlussRichtung, typ: AnschlussArtId = art, anzahl: Int = 1) =
        KnotenDaten(art = "test", name = name, anschlüsse = List(anzahl) { AnschlussDaten(name = "p$it", richtung = richtung, art = typ, kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts) })
    private fun ref(k: KnotenDaten, i: Int = 0) = AnschlussVerweis(k.id, k.anschlüsse[i].id)

    @Test fun beideStartrichtungenFilternGegenanschluesseUndVerbindenAtomar() {
        for (richtung in listOf(AnschlussRichtung.Eingang, AnschlussRichtung.Ausgang)) {
            val alt = knoten("Alt", richtung)
            val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(alt)), prüfung)
            val basis = editor.karte
            editor.beginneVerbindung(ref(alt))
            val kontext = assertNotNull(editor.übergibVerbindungZurEinfügung())
            val neu = knoten("Neu", if (richtung == AnschlussRichtung.Eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang, anzahl = 2)
            val probe = basis.copy(knoten = basis.knoten + neu)
            assertEquals(2, editor.verbindungsEinfügeTreffer(kontext, probe, neu).size)
            val falsch = neu.copy(anschlüsse = neu.anschlüsse.map { it.copy(art = andere) })
            assertTrue(editor.verbindungsEinfügeTreffer(kontext, basis.copy(knoten = basis.knoten + falsch), falsch).isEmpty())
            assertTrue(editor.beginneVerbindungsEinfügeZiehen(neu, kontext))
            editor.aktualisiereKnotenZiehen(GraphPunkt(400f, 230f)); editor.pausiereVerbindungsEinfügeZiehen()
            assertFalse(editor.kannRückgängig())
            assertTrue(editor.bestätigeVerbindungsEinfügung(kontext, editor.karte, neu.id, ref(neu, 1), 0))
            val fertig = editor.karte
            assertEquals(1, fertig.verbindungen.size)
            assertEquals(if (richtung == AnschlussRichtung.Ausgang) ref(neu, 1) else ref(alt), fertig.verbindungen.single().zu)
            editor.rückgängig(); assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
            editor.wiederholen(); assertEquals(fertig, editor.karte)
        }
    }

    @Test fun belegterEingangBleibtBisBestaetigungErhaltenUndAbbruchIstVerlustfrei() {
        val a = knoten("A", AnschlussRichtung.Ausgang); val b = knoten("B", AnschlussRichtung.Eingang)
        val kante = VerbindungDaten(von = ref(a), zu = ref(b))
        val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b), verbindungen = listOf(kante)), prüfung)
        val basis = editor.karte
        editor.beginneVerbindung(ref(b))
        val kontext = assertNotNull(editor.übergibVerbindungZurEinfügung())
        assertEquals(ref(a), kontext.start); assertEquals(kante, kontext.ersetzt)
        assertEquals(basis, editor.karte)
        val neu = knoten("Neu", AnschlussRichtung.Eingang)
        editor.beginneVerbindungsEinfügeZiehen(neu, kontext); editor.verwerfeKnotenZiehen()
        assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
        editor.beginneVerbindungsEinfügeZiehen(neu, kontext)
        assertTrue(editor.bestätigeVerbindungsEinfügung(kontext, editor.karte, neu.id, ref(neu), 0))
        assertEquals(kante.id, editor.karte.verbindungen.single().id)
        assertEquals(ref(neu), editor.karte.verbindungen.single().zu)
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }

    @Test fun verbindungsauswahlVerhindertZusaetzlichesEinfuegenInEineLeitung() {
        val a = knoten("A", AnschlussRichtung.Ausgang); val b = knoten("B", AnschlussRichtung.Eingang)
        val edge = VerbindungDaten(von = ref(a), zu = ref(b))
        val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a, b), verbindungen = listOf(edge)), prüfung)
        editor.beginneVerbindung(ref(a)); val c = assertNotNull(editor.übergibVerbindungZurEinfügung())
        val neu = knoten("N", AnschlussRichtung.Eingang)
        editor.beginneVerbindungsEinfügeZiehen(neu, c)
        editor.aktualisiereKnotenEinfügeziel(edge.id)
        assertNull(editor.einfügeVerbindungsZiel)
    }

    @Test fun veralteteDialogbasisUndVeralteterKontextWerdenNichtUebernommen() {
        val a = knoten("A", AnschlussRichtung.Ausgang)
        val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a)), prüfung)
        editor.beginneVerbindung(ref(a)); val c = assertNotNull(editor.übergibVerbindungZurEinfügung())
        val neu = knoten("N", AnschlussRichtung.Eingang)
        editor.beginneVerbindungsEinfügeZiehen(neu, c)
        val dialog = editor.karte
        editor.aktualisiereKnotenZiehen(GraphPunkt(400f, 200f))
        assertFalse(editor.bestätigeVerbindungsEinfügung(c, dialog, neu.id, ref(neu), 0))
        assertTrue(editor.karte.verbindungen.isEmpty())
        editor.beendeInteraktion()
        assertFalse(editor.beginneVerbindungsEinfügeZiehen(knoten("X", AnschlussRichtung.Eingang), c))
    }

    @Test fun vorhandeneMehrfachBeitraegeBleibenBeiDerEinfügungGueltigUndErhalten() {
        val a = knoten("A", AnschlussRichtung.Ausgang); val b = knoten("B", AnschlussRichtung.Ausgang)
        val ziel = knoten("Join", AnschlussRichtung.Eingang).let { it.copy(anschlüsse = it.anschlüsse.map { p -> p.copy(belegung = AnschlussBelegung.Mehrfach) }) }
        val edges = listOf(VerbindungDaten(von = ref(a), zu = ref(ziel)), VerbindungDaten(von = ref(b), zu = ref(ziel)))
        val editor = KartenEditorZustand(KartenDaten(name = "Mehrfach", knoten = listOf(a, b, ziel), verbindungen = edges), prüfung)
        val basis = editor.karte
        editor.beginneVerbindung(ref(a)); val c = assertNotNull(editor.übergibVerbindungZurEinfügung())
        val neu = knoten("Neu", AnschlussRichtung.Eingang)
        assertTrue(editor.beginneVerbindungsEinfügeZiehen(neu, c))
        assertEquals(1, editor.verbindungsEinfügeTreffer(c, editor.karte, neu).size)
        assertTrue(editor.bestätigeVerbindungsEinfügung(c, editor.karte, neu.id, ref(neu), 0))
        assertTrue(editor.karte.verbindungen.containsAll(edges))
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }

    @Test fun freierMehrfacheingangNimmtNeuenBeitragAmEndeAuf() {
        val a = knoten("A", AnschlussRichtung.Ausgang)
        val ziel = knoten("Join", AnschlussRichtung.Eingang).let { it.copy(anschlüsse = it.anschlüsse.map { p -> p.copy(belegung = AnschlussBelegung.Mehrfach) }) }
        val edge = VerbindungDaten(von = ref(a), zu = ref(ziel), eingangsReihenfolge = 7)
        val editor = KartenEditorZustand(KartenDaten(name = "Mehrfach", knoten = listOf(a, ziel), verbindungen = listOf(edge)), prüfung)
        val basis = editor.karte
        editor.beginneVerbindung(ref(ziel)); val c = assertNotNull(editor.übergibVerbindungZurEinfügung())
        assertNull(c.ersetzt)
        val neu = knoten("Neu", AnschlussRichtung.Ausgang)
        editor.beginneVerbindungsEinfügeZiehen(neu, c)
        assertTrue(editor.bestätigeVerbindungsEinfügung(c, editor.karte, neu.id, ref(neu), 0))
        assertEquals(2, editor.karte.verbindungen.size)
        assertEquals(8, editor.karte.verbindungen.single { it.id != edge.id }.eingangsReihenfolge)
        assertTrue(edge in editor.karte.verbindungen)
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }

    @Test fun ungueltigeAdaptervorschlaegeWerdenNichtAlsTrefferAngeboten() {
        val a = knoten("A", AnschlussRichtung.Ausgang)
        val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(a)), prüfung)
        editor.beginneVerbindung(ref(a)); val c = assertNotNull(editor.übergibVerbindungZurEinfügung())
        val neu = knoten("N", AnschlussRichtung.Eingang, andere)
        editor.verbindungÜberAdapterMöglich = { _, _, _ -> true }
        editor.verbindungsPlaner = { karte, von, zu -> listOf(VerbindungsVorschlag("Ungültig", karte.copy(verbindungen = listOf(VerbindungDaten(von = von, zu = zu))))) }
        assertTrue(editor.verbindungsEinfügeTreffer(c, editor.karte.copy(knoten = editor.karte.knoten + neu), neu).isEmpty())
    }
}
