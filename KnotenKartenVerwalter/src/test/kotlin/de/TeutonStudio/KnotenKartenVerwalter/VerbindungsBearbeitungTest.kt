package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class VerbindungsBearbeitungTest {
    private val x = AnschlussArtId("x")
    private val y = AnschlussArtId("y")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(x, "X"), AnschlussArt(y, "Y"))))
    private fun port(name: String, eingang: Boolean, art: AnschlussArtId = x) = AnschlussDaten(
        name = name, richtung = if (eingang) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
        kante = if (eingang) AnschlussKante.Links else AnschlussKante.Rechts, art = art,
    )
    private fun knoten(name: String, input: AnschlussArtId = x, output: AnschlussArtId = x) = KnotenDaten(
        art = "test", name = name, anschlüsse = listOf(port("in", true, input), port("out", false, output)),
    )
    private fun ref(k: KnotenDaten, name: String) = AnschlussVerweis(k.id, k.anschlüsse.single { it.name == name }.id)
    private fun kante(a: KnotenDaten, b: KnotenDaten) = VerbindungDaten(von = ref(a, "out"), zu = ref(b, "in"))
    private val a = knoten("A")
    private val b = knoten("B")
    private val c = knoten("C")
    private val ab = kante(a, b)
    private fun basis() = KartenDaten(name = "Test", knoten = listOf(a, b, c), verbindungen = listOf(ab))
    private fun bewegen(editor: KartenEditorZustand, id: KnotenId) {
        editor.führeAus(KartenAktion.KnotenVerschieben(id, GraphPunkt(500f, 300f)), mitHistorie = false)
    }

    @Test fun joinBeitragBleibtBeimEinfuegenHerausziehenUndNeuverbindenAnSeinerPosition() {
        val ziel = b.copy(anschlüsse = b.anschlüsse.map { if (it.name == "in") it.copy(belegung = AnschlussBelegung.Mehrfach) else it })
        val d = knoten("D")
        val original = kante(a, ziel).copy(eingangsReihenfolge = 3, eingangsFallback = "in@karte:alt")
        val weiterer = kante(d, ziel).copy(eingangsReihenfolge = 8)
        val vorher = basis().copy(knoten = listOf(a, ziel, c, d), verbindungen = listOf(original, weiterer))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(c.id); bewegen(editor, c.id)
        editor.aktualisiereKnotenEinfügeziel(original.id); editor.beendeKnotenZiehen()
        val eingefügt = editor.karte
        assertEquals(3, eingefügt.verbindungen.size)
        assertEquals(original.copy(von = ref(c, "out")), eingefügt.verbindungen.single { it.id == original.id })
        assertTrue(weiterer in eingefügt.verbindungen)
        assertFalse(eingefügt.verbindungen.any { it.von == original.von && it.zu == original.zu })
        editor.rückgängig(); assertEquals(vorher, editor.karte)
        editor.wiederholen(); assertEquals(eingefügt, editor.karte)
        val bypass = assertNotNull(planeKnotenHerausziehen(eingefügt, c.id, prüfung)).karte
        assertEquals(vorher.verbindungen.toSet(), bypass.verbindungen.toSet())
        val neu = vorher.wendeAn(KartenAktion.VerbindungNeuVerbinden(original.id, original.copy(von = ref(c, "out"))))
        assertEquals(original.copy(von = ref(c, "out")), neu.verbindungen.single { it.id == original.id })
        assertTrue(weiterer in neu.verbindungen)
        val paar = KompatiblesAnschlussPaar(c.anschlüsse.first().id, c.anschlüsse.last().id, x, x)
        val kandidat = EinfuegeKandidat(c.art, null, c, listOf(paar))
        val ohneC = vorher.copy(knoten = vorher.knoten.filterNot { it.id == c.id })
        val dienst = assertIs<KnotenEinfuegeErgebnis.Erfolg>(KnotenInEdgeEinfueger(prüfung).einfuegen(ohneC, EdgeEinfuegeKontext.aus(original), kandidat, paar))
        assertEquals(original.copy(von = ref(c, "out")), dienst.karte.verbindungen.single { it.id == original.id })
        assertTrue(weiterer in dienst.karte.verbindungen)
    }

    @Test fun adapterUmbindungErhaeltJoinBeitragUndFallbackrolle() {
        prüfung.mehrfachDarstellung = { _, _ -> false }
        val quelle = knoten("Y", output = y)
        val ziel = b.copy(anschlüsse = b.anschlüsse.map { if (it.name == "in") it.copy(belegung = AnschlussBelegung.Mehrfach) else it })
        val original = kante(a, ziel).copy(eingangsReihenfolge = 3, eingangsFallback = "in@karte:alt")
        val weiterer = kante(c, ziel).copy(eingangsReihenfolge = 8)
        val vorher = basis().copy(knoten = listOf(a, ziel, c, quelle), verbindungen = listOf(original, weiterer))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.verbindungsPlaner = { basis, von, zu ->
            val adapter = knoten("Y nach X", input = y)
            val mitAdapter = basis.copy(knoten = basis.knoten + adapter)
            val erster = verbindeGeprüft(mitAdapter, von, ref(adapter, "in"), prüfung)!!
            listOf(VerbindungsVorschlag("Adapter", verbindeGeprüft(erster, ref(adapter, "out"), zu, prüfung)!!))
        }
        editor.beginneVerbindung(ref(quelle, "out"))
        val beitrag = editor.darstellungsKarte.knoten.single { it.id == ziel.id }.anschlüsse.single { it.beitragsVerbindung == original.id }
        editor.anschlussAngeklickt(AnschlussVerweis(ziel.id, beitrag.id))
        assertEquals(3, editor.karte.verbindungen.size)
        val ersetzt = editor.karte.verbindungen.single { it.id == original.id }
        assertEquals(original.zu, ersetzt.zu)
        assertEquals(original.eingangsReihenfolge, ersetzt.eingangsReihenfolge)
        assertEquals(original.eingangsFallback, ersetzt.eingangsFallback)
        assertTrue(weiterer in editor.karte.verbindungen)
        editor.rückgängig(); assertEquals(vorher, editor.karte)
    }

    @Test fun einfuegenIstMitBewegungEinUndoSchritt() {
        val vorher = basis()
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(c.id)
        bewegen(editor, c.id)
        editor.aktualisiereKnotenEinfügeziel(ab.id)
        editor.beendeKnotenZiehen()
        val nachher = editor.karte
        assertEquals(2, nachher.verbindungen.size)
        assertTrue(nachher.verbindungen.any { it.von == ref(a, "out") && it.zu == ref(c, "in") })
        assertTrue(nachher.verbindungen.any { it.von == ref(c, "out") && it.zu == ref(b, "in") })
        editor.rückgängig(); assertEquals(vorher, editor.karte); assertFalse(editor.kannRückgängig())
        editor.wiederholen(); assertEquals(nachher, editor.karte)
    }

    @Test fun mehrereEingangsportsWerdenAusgewaehltUndAtomarUebernommen() {
        val kandidat = c.copy(anschlüsse = c.anschlüsse + port("zweiter", true))
        val vorher = basis().copy(knoten = listOf(a, b, kandidat))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(c.id); bewegen(editor, c.id)
        editor.aktualisiereKnotenEinfügeziel(ab.id); editor.beendeKnotenZiehen()
        assertEquals(2, editor.verbindungsAuswahl?.vorschläge?.size)
        assertEquals(listOf(ab), editor.karte.verbindungen)
        editor.wähleVerbindungsVorschlag(1)
        assertTrue(editor.karte.verbindungen.any { it.zu == ref(kandidat, "zweiter") })
        editor.rückgängig(); assertEquals(vorher, editor.karte); assertFalse(editor.kannRückgängig())
    }

    @Test fun abbruchDerPortauswahlBehaeltNurDieBewegung() {
        val kandidat = c.copy(anschlüsse = c.anschlüsse + port("zweiter", true))
        val vorher = basis().copy(knoten = listOf(a, b, kandidat))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(c.id); bewegen(editor, c.id)
        editor.aktualisiereKnotenEinfügeziel(ab.id); editor.beendeKnotenZiehen()
        editor.verwerfeVerbindungsAuswahl()
        assertEquals(listOf(ab), editor.karte.verbindungen)
        assertEquals(GraphPunkt(500f, 300f), editor.karte.knoten.single { it.id == c.id }.position)
        editor.rückgängig(); assertEquals(vorher, editor.karte)
    }

    @Test fun herausziehenUeberbruecktUndBleibtEinUndoSchritt() {
        val vorher = basis().copy(verbindungen = listOf(kante(a, c), kante(c, b)))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.merkeKnotenZumHerausziehen(c.id)
        editor.beginneKnotenZiehen(c.id); bewegen(editor, c.id); editor.beendeKnotenZiehen()
        assertEquals(3, editor.karte.knoten.size)
        assertEquals(ref(a, "out"), editor.karte.verbindungen.single().von)
        assertEquals(ref(b, "in"), editor.karte.verbindungen.single().zu)
        val nachher = editor.karte
        editor.rückgängig(); assertEquals(vorher, editor.karte); assertFalse(editor.kannRückgängig())
        editor.wiederholen(); assertEquals(nachher, editor.karte)
    }

    @Test fun normalesZiehenTrenntKeineVerbindungenUndCancelRolltBewegungZurueck() {
        val vorher = basis().copy(verbindungen = listOf(kante(a, c), kante(c, b)))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(c.id); bewegen(editor, c.id); editor.beendeKnotenZiehen()
        assertEquals(vorher.verbindungen, editor.karte.verbindungen)
        editor.rückgängig(); assertEquals(vorher, editor.karte)
        editor.beginneKnotenZiehen(c.id, lösen = true); bewegen(editor, c.id); editor.verwerfeKnotenZiehen()
        assertEquals(vorher, editor.karte)
    }

    @Test fun inkompatiblerBypassUndMehrereQuellenTrennenNichts() {
        val konverter = knoten("X nach Y", output = y)
        val ziel = knoten("Y", input = y)
        val vorher = KartenDaten(name = "Typen", knoten = listOf(a, konverter, ziel), verbindungen = listOf(kante(a, konverter), kante(konverter, ziel)))
        assertNull(planeKnotenHerausziehen(vorher, konverter.id, prüfung))
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.beginneKnotenZiehen(konverter.id, lösen = true); bewegen(editor, konverter.id); editor.beendeKnotenZiehen()
        assertEquals(vorher.verbindungen, editor.karte.verbindungen)
        assertNotNull(editor.letzteMeldung)
        val mehrfach = c.copy(anschlüsse = c.anschlüsse + port("zweiter", true))
        val mitZweiQuellen = basis().copy(knoten = listOf(a, b, mehrfach), verbindungen = listOf(kante(a, mehrfach),
            VerbindungDaten(von = ref(b, "out"), zu = ref(mehrfach, "zweiter"))))
        assertNull(planeKnotenHerausziehen(mitZweiQuellen, mehrfach.id, prüfung))
    }

    @Test fun fanoutWirdBeimHerausziehenErhalten() {
        val d = knoten("D")
        val vorher = basis().copy(knoten = listOf(a,b,c,d), verbindungen = listOf(kante(a,c), kante(c,b), kante(c,d)))
        val plan = assertNotNull(planeKnotenHerausziehen(vorher, c.id, prüfung))
        assertEquals(setOf(ref(b,"in"),ref(d,"in")), plan.karte.verbindungen.map { it.zu }.toSet())
        assertTrue(plan.karte.verbindungen.all { it.von == ref(a,"out") })
    }

    @Test fun einfuegenUeberschreibtKeineBelegtenPortsUndErzeugtKeineZyklen() {
        val belegt = basis().copy(verbindungen = listOf(ab, kante(b,c)))
        assertTrue(planeKnotenInVerbindung(belegt, c.id, ab.id, prüfung).isEmpty())
        val zyklus = basis().copy(verbindungen = listOf(ab, kante(b,c).copy(zu = ref(c,"in"))))
        assertTrue(planeKnotenInVerbindung(zyklus, c.id, ab.id, prüfung).isEmpty())
        assertTrue(planeKnotenInVerbindung(basis(), a.id, ab.id, prüfung).isEmpty())
    }

    private fun mitVorschlägen(): Pair<KartenEditorZustand, KnotenDaten> {
        val ziel = knoten("Y", input = y)
        val editor = KartenEditorZustand(KartenDaten(name = "Auswahl", knoten = listOf(a,ziel)), prüfung)
        editor.verbindungsPlaner = { basis, von, zu -> (1..2).map { nummer ->
            val adapter = knoten("Adapter $nummer", output = y)
            val mitKnoten = basis.copy(knoten = basis.knoten + adapter)
            val teil = verbindeGeprüft(mitKnoten, von, ref(adapter,"in"), prüfung)!!
            VerbindungsVorschlag("Weg $nummer", verbindeGeprüft(teil, ref(adapter,"out"), zu, prüfung)!!)
        } }
        return editor to ziel
    }

    @Test fun mehrdeutigeWegeUndVeralteteAuswahlVeraendernNichtsUnbestaetigt() {
        val (editor,ziel) = mitVorschlägen()
        val vorher = editor.karte
        editor.beginneVerbindung(ref(a,"out")); assertTrue(editor.kompatibelMitStart(ref(ziel,"in")))
        editor.anschlussAngeklickt(ref(ziel,"in"))
        assertNotNull(editor.verbindungsAuswahl); assertEquals(vorher, editor.karte)
        editor.verwerfeVerbindungsAuswahl(); assertEquals(vorher,editor.karte); assertFalse(editor.kannRückgängig())
        editor.beginneVerbindung(ref(a,"out")); editor.anschlussAngeklickt(ref(ziel,"in"))
        editor.führeAus(KartenAktion.KnotenVerschieben(a.id,GraphPunkt(999f,999f)))
        val verändert = editor.karte
        editor.wähleVerbindungsVorschlag(0); assertEquals(verändert,editor.karte); assertNotNull(editor.letzteMeldung)
    }

    @Test fun ausgewaehlteKetteIstEinUndoSchritt() {
        val (editor,ziel) = mitVorschlägen()
        val vorher = editor.karte
        editor.beginneVerbindung(ref(a,"out")); editor.anschlussAngeklickt(ref(ziel,"in")); editor.wähleVerbindungsVorschlag(1)
        assertEquals(3,editor.karte.knoten.size); assertEquals(2,editor.karte.verbindungen.size)
        assertTrue(editor.karte.knoten.any { it.name == "Adapter 2" })
        val nachher = editor.karte
        editor.rückgängig(); assertEquals(vorher,editor.karte); assertFalse(editor.kannRückgängig())
        editor.wiederholen(); assertEquals(nachher,editor.karte)
    }

    @Test fun einfuegenUndFanoutBypassErhaltenAbhaengigeAusgaengeWaehrenDerPlanung() {
        val objekt=AnschlussArtId("objekt")
        val p=GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(objekt,"Objekt"),AnschlussArt(x,"X",elternArt=objekt))))
        fun pass(name:String)=KnotenDaten(art="test",name=name,anschlüsse=listOf(
            port("in",true,objekt),port("out",false,objekt).copy(artFolgtEingang="in")))
        val erster=pass("Erster")
        val zweiter=pass("Zweiter")
        val unverbunden=knoten("Neu")
        val abhaengig=KartenDaten(name="Abhängig",knoten=listOf(a,erster,zweiter,b,unverbunden),
            verbindungen=listOf(kante(a,erster),kante(erster,zweiter),kante(zweiter,b)))
        val mittlere=abhaengig.verbindungen[1]
        assertTrue(istGültigeVerdrahtung(abhaengig,p))
        assertEquals(1,planeKnotenInVerbindung(abhaengig,unverbunden.id,mittlere.id,p).size)
        val d=knoten("D")
        val verzweigt=abhaengig.copy(knoten=abhaengig.knoten+d,verbindungen=abhaengig.verbindungen+kante(erster,d))
        val plan=assertNotNull(planeKnotenHerausziehen(verzweigt,erster.id,p))
        assertTrue(istGültigeVerdrahtung(plan.karte,p))
        assertEquals(2,plan.karte.verbindungen.count { it.von==ref(a,"out") })
    }

}
