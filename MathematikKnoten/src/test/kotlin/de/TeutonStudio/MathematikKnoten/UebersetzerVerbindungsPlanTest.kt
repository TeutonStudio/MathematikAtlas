package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class UebersetzerVerbindungsPlanTest {
    private val laufzeit = MathematikKartenLaufzeit()
    private val prüfung = laufzeit.graphPrüfung
    private fun knoten(art: AnschlussArtId, eingang: Boolean) = KnotenDaten(art = "test", name = if (eingang) "Ziel" else "Quelle",
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = if (eingang) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
            kante = if (eingang) AnschlussKante.Links else AnschlussKante.Rechts, art = art)))
    private fun ref(k: KnotenDaten) = AnschlussVerweis(k.id,k.anschlüsse.single().id)
    private fun plane(von: KnotenDaten, zu: KnotenDaten, weitere: List<KnotenDaten> = emptyList()): Pair<KartenDaten,List<VerbindungsVorschlag>> {
        val karte = KartenDaten(name = "Plan",knoten = listOf(von,zu) + weitere)
        return karte to planeÜbersetzerVerbindungen(karte,ref(von),ref(zu),prüfung)
    }

    @Test fun zahlZuWinkelVerwendetGenauEinenGradUebersetzer() {
        val a = knoten(MathematikAnschlussArten.Zahl.id,false)
        val b = knoten(MathematikAnschlussArten.Winkel.id,true)
        val (basis,pläne) = plane(a,b)
        val plan = pläne.single()
        val neu = plan.karte.knoten.single { it.id !in basis.knoten.map { it.id } }
        assertEquals("grad",übersetzerZiel(neu)); assertEquals(2,plan.karte.verbindungen.size)
        assertTrue(istGültigeVerdrahtung(plan.karte,prüfung))
        val editor = KartenEditorZustand(basis,prüfung)
        editor.verbindungsPlaner = { k,v,z -> planeÜbersetzerVerbindungen(k,v,z,prüfung) }
        editor.beginneVerbindung(ref(a)); editor.anschlussAngeklickt(ref(b))
        assertNull(editor.verbindungsAuswahl); assertEquals(3,editor.karte.knoten.size)
        editor.rückgängig(); assertEquals(basis,editor.karte)
    }

    @Test fun geometrieZuSpalteBenoetigtEineKetteOhneFehlendenObjektanker() {
        val a = knoten(GeometrieAnschlussArten.Punkt.id,false)
        val b = knoten(MathematikAnschlussArten.SpaltenVektor.id,true)
        val (basis,pläne) = plane(a,b)
        assertTrue(pläne.isNotEmpty())
        for (plan in pläne) {
            val ziele = plan.karte.knoten.filter { it.id !in basis.knoten.map { k -> k.id } }.map(::übersetzerZiel)
            assertEquals(listOf("mittelpunkt","spalte"),ziele)
            assertEquals(3,plan.karte.verbindungen.size)
            assertTrue(istGültigeVerdrahtung(plan.karte,prüfung))
        }
    }

    @Test fun automatischeStrukturkettenFuegenKeineAbflachungEin() {
        val quelle=knoten(MathematikAnschlussArten.Tupel.id,false)
        val ziel=knoten(MathematikAnschlussArten.SpaltenVektor.id,true)
        val (basis,pläne)=plane(quelle,ziel)
        assertTrue(pläne.isNotEmpty())
        for(plan in pläne) {
            val adapter=plan.karte.knoten.filter {it.id !in basis.knoten.map {k->k.id}}
            assertEquals(listOf("spalte"),adapter.map(::übersetzerZiel))
        }
        val t=Tupel(listOf(Tupel(listOf(RationaleZahl.Eins))))
        val explizit=assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(t,ÜbersetzungsZiel.ABFLACHEN)))
        assertEquals(Tupel(listOf(RationaleZahl.Eins)),explizit.wert)
        assertFalse(explizit.zeugnis.fürAutomatikGeeignet())
        assertIs<ÜbersetzungsErgebnis.NichtDefiniert>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(t,ÜbersetzungsZiel.ABFLACHEN,automatisch=true)))
    }

    @Test fun matrixZuMethodeBietetMehrereKurzeWegeAlsAuswahl() {
        val a = knoten(MathematikAnschlussArten.Matrix.id,false)
        val b = knoten(MathematikAnschlussArten.Methode.id,true)
        val (basis,pläne) = plane(a,b)
        assertTrue(pläne.size >= 2,pläne.map { it.titel }.toString())
        assertTrue(pläne.all { it.karte.knoten.size == 3 })
        assertTrue(pläne.map { it.titel }.any { it.contains("Matrix zerlegen") })
        val editor = KartenEditorZustand(basis,prüfung)
        editor.verbindungsPlaner = { k,v,z -> planeÜbersetzerVerbindungen(k,v,z,prüfung) }
        editor.beginneVerbindung(ref(b)); editor.anschlussAngeklickt(ref(a))
        assertNotNull(editor.verbindungsAuswahl); assertEquals(basis,editor.karte)
        editor.wähleVerbindungsVorschlag(0)
        assertEquals(3,editor.karte.knoten.size); assertEquals(2,editor.karte.verbindungen.size)
    }

    @Test fun direkterWegHatVorrangUndUnmoeglicheTypenBleibenUnverbunden() {
        val a = knoten(MathematikAnschlussArten.Zahl.id,false)
        val b = knoten(MathematikAnschlussArten.Zahl.id,true)
        assertTrue(plane(a,b).second.isEmpty())
        val editor = KartenEditorZustand(plane(a,b).first,prüfung)
        editor.verbindungsPlaner = { _,_,_ -> error("Direkte Verbindung braucht keine Routensuche") }
        editor.beginneVerbindung(ref(a)); editor.anschlussAngeklickt(ref(b))
        assertEquals(2,editor.karte.knoten.size); assertEquals(1,editor.karte.verbindungen.size)
        assertTrue(plane(knoten(MathematikAnschlussArten.Aussage.id,false),b).second.isEmpty())
    }

    @Test fun belegterEingangWirdErstBeiBestaetigungErsetztUndUndoStelltIhnWiederHer() {
        val a = knoten(MathematikAnschlussArten.Matrix.id,false)
        val b = knoten(MathematikAnschlussArten.Methode.id,true)
        val alt = knoten(MathematikAnschlussArten.Methode.id,false)
        val kante = VerbindungDaten(von=ref(alt),zu=ref(b))
        val basis = KartenDaten(name="Belegt",knoten=listOf(a,b,alt),verbindungen=listOf(kante))
        val editor = KartenEditorZustand(basis,prüfung)
        editor.verbindungsPlaner = { k,v,z -> planeÜbersetzerVerbindungen(k,v,z,prüfung) }
        editor.beginneVerbindung(ref(a));editor.anschlussAngeklickt(ref(b)); assertEquals(basis,editor.karte)
        editor.verwerfeVerbindungsAuswahl();assertEquals(basis,editor.karte)
        editor.beginneVerbindung(ref(a));editor.anschlussAngeklickt(ref(b));editor.wähleVerbindungsVorschlag(0)
        assertTrue(editor.karte.verbindungen.none { it.id == kante.id })
        assertEquals(1,editor.karte.verbindungen.count { it.zu == ref(b) })
        editor.rückgängig();assertEquals(basis,editor.karte)
    }

    @Test fun echteUebersetzungUndJsonRundreiseBleibenAuswertbar() {
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter + ("wert" to "90")) }
        val ziel = KnotenDaten(art="test",name="Winkelziel",anschlüsse=listOf(AnschlussDaten(name="wert",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Winkel.id)))
        val ausgang = AnschlussVerweis(zahl.id,zahl.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id)
        val basis = KartenDaten(name="Grad",knoten=listOf(zahl,ziel))
        val plan = planeÜbersetzerVerbindungen(basis,ausgang,ref(ziel),prüfung).single()
        val geladen = KartenDatenJson.lese(KartenDatenJson.schreibe(plan.karte))
        assertEquals(plan.karte,geladen)
        val ergebnis = laufzeit.auswerten(geladen)
        val übersetzer = geladen.knoten.single { it.art == ÜBERSETZER_ART }
        val wert = ergebnis.knoten.getValue(übersetzer.id)
        assertNull(wert.fehler,wert.fehler)
        assertTrue(wert.ausgaben.isNotEmpty())
    }

    @Test fun typvorschauIstBeiVielenPortsVonKonkreterGraphplanungGetrennt() {
        val a=knoten(MathematikAnschlussArten.Matrix.id,false)
        val ziele=(0 until 20).map { knoten(MathematikAnschlussArten.Methode.id,true) }
        val kette=(0 until 200).map { i -> KnotenDaten(art="test",name="K$i",anschlüsse=listOf(
            AnschlussDaten(name="in",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Zahl.id),
            AnschlussDaten(name="out",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Zahl.id))) }
        val verbindungen=kette.zipWithNext { q,z -> VerbindungDaten(
            von=AnschlussVerweis(q.id,q.anschlüsse[1].id),zu=AnschlussVerweis(z.id,z.anschlüsse[0].id)) }
        val basis=KartenDaten(name="Große Karte",knoten=listOf(a)+ziele+kette,verbindungen=verbindungen)
        val planer=ÜbersetzerVerbindungsPlaner(prüfung)
        val start=System.nanoTime()
        assertTrue(ziele.all { planer.möglich(basis,ref(a),ref(it)) })
        println("Typvorschau für 20 Ziele bei 221 Knoten/199 Kanten: ${(System.nanoTime()-start)/1_000_000} ms")
        val konkret=System.nanoTime()
        val pläne=planer.vorschläge(basis,ref(a),ref(ziele.first()))
        println("Konkrete Planung derselben Karte: ${(System.nanoTime()-konkret)/1_000_000} ms")
        assertTrue(pläne.isNotEmpty())
        assertTrue(pläne.all { it.karte.verbindungen.containsAll(verbindungen) && it.karte.verbindungen.size==verbindungen.size+2 })
        val unpassend=ziele.first().copy(anschlüsse=ziele.first().anschlüsse.map { it.copy(art=MathematikAnschlussArten.Aussage.id) })
        assertFalse(planer.möglich(basis.copy(knoten=basis.knoten.map { if(it.id==unpassend.id) unpassend else it }),ref(a),ref(unpassend)))
    }

    @Test fun vorschauCacheUebersiehtKeinenNeuEntstandenenZyklus() {
        val a=knoten(MathematikAnschlussArten.Matrix.id,false)
        val b=knoten(MathematikAnschlussArten.Methode.id,true)
        val planer=ÜbersetzerVerbindungsPlaner(prüfung)
        val basis=KartenDaten(name="Zyklus",knoten=listOf(a,b))
        assertTrue(planer.möglich(basis,ref(a),ref(b)))
        val eingang=AnschlussDaten(name="rueck",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Matrix.id)
        val ausgang=AnschlussDaten(name="rueck",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Matrix.id)
        val zyklus=basis.copy(knoten=listOf(a.copy(anschlüsse=a.anschlüsse+eingang),b.copy(anschlüsse=b.anschlüsse+ausgang)),
            verbindungen=listOf(VerbindungDaten(von=AnschlussVerweis(b.id,ausgang.id),zu=AnschlussVerweis(a.id,eingang.id))))
        assertFalse(planer.möglich(zyklus,ref(a),ref(b)))
        assertTrue(planer.vorschläge(zyklus,ref(a),ref(b)).isEmpty())
    }


    @Test fun uebersetzungErhaeltAbhaengigenAusgangBeimErsetzenEinesBelegtenEingangs() {
        val zeile=knoten(MathematikAnschlussArten.ZeilenVektor.id,false)
        val spalte=knoten(MathematikAnschlussArten.SpaltenVektor.id,false)
        val ziel=knoten(MathematikAnschlussArten.SpaltenVektor.id,true)
        val pass=KnotenDaten(art="test",name="Passthrough",anschlüsse=listOf(
            AnschlussDaten(name="wert",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Objekt.id),
            AnschlussDaten(name="out",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Objekt.id,artFolgtEingang="wert")))
        val ein=AnschlussVerweis(pass.id,pass.anschlüsse[0].id)
        val aus=AnschlussVerweis(pass.id,pass.anschlüsse[1].id)
        val alt=VerbindungDaten(von=ref(spalte),zu=ein)
        val folge=VerbindungDaten(von=aus,zu=ref(ziel))
        val basis=KartenDaten(name="Abhängiger Ausgang",knoten=listOf(zeile,spalte,pass,ziel),verbindungen=listOf(alt,folge))
        assertTrue(istGültigeVerdrahtung(basis,prüfung))
        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(basis,ref(zeile),ein))
        val planer=ÜbersetzerVerbindungsPlaner(prüfung)
        assertTrue(planer.möglich(basis,ref(zeile),ein))
        val plan=planer.vorschläge(basis,ref(zeile),ein).single()
        assertEquals("spalte",übersetzerZiel(plan.karte.knoten.single { it.art==ÜBERSETZER_ART }))
        assertTrue(plan.karte.verbindungen.contains(folge))
        assertFalse(plan.karte.verbindungen.contains(alt))
        assertTrue(istGültigeVerdrahtung(plan.karte,prüfung))
    }


    @Test fun bekannteDimensionsfehlerWerdenNichtDurchTypverlustInUmwegenUmgangen() {
        val zahl=de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl)
        val drei=de.TeutonStudio.TypSystem.TypAusdruck.Literal("3")
        val zwei=de.TeutonStudio.TypSystem.TypAusdruck.Literal("2")
        val quellTyp=de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor,listOf(zahl,drei))
        val zielTyp=de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.ZeilenVektor,listOf(zahl,zwei))
        val a=knoten(MathematikAnschlussArten.SpaltenVektor.id,false).let { k -> k.copy(anschlüsse=k.anschlüsse.map { it.copy(vertrag=it.vertrag.copy(typ=quellTyp)) }) }
        val b=knoten(MathematikAnschlussArten.ZeilenVektor.id,true).let { k -> k.copy(anschlüsse=k.anschlüsse.map { it.copy(vertrag=it.vertrag.copy(typ=zielTyp)) }) }
        val (basis,pläne)=plane(a,b)
        assertTrue(pläne.isEmpty(),pläne.map { it.titel }.toString())
        assertFalse(ÜbersetzerVerbindungsPlaner(prüfung).möglich(basis,ref(a),ref(b)))
    }

}
