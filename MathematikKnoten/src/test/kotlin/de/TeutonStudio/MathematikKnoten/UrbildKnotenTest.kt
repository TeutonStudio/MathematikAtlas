package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class UrbildKnotenTest {
    private fun basis() = konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), MengenRechnerOperator.URBILD)
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun quadrat(): Methode { val x = Variable("x"); return Methode("q", listOf(x), Potenz(x,z(2)),ReelleZahlen,mapOf("x" to ReelleZahlen)) }
    private fun eval(k: KnotenDaten, methode: Methode = quadrat(), ziel: MathematischesObjekt = z(4)) =
        GesamterMathematikAuswerter.erzeugeRegister().findeNachMigration(k.art).auswerten(KnotenAuswertungsKontext(k,
            mapOf("methode" to BedingterWert(methode), "ziel" to BedingterWert(ziel)), RechenKontext()))

    @Test fun `Urbild ist suchbar mit echten festen Wertanschluessen`() {
        val k = basis()
        assertEquals(listOf("methode", "ziel", "menge"), k.anschlüsse.map { it.name })
        assertEquals(MathematikAnschlussArten.Zahl.id, k.anschlüsse.single { it.name == "ziel" }.art)
        assertTrue(k.anschlüsse.none { it.kannSichErweitern || it.belegung == AnschlussBelegung.Mehrfach })
        assertEquals(MathematikAnschlussArten.Menge.id, k.anschlüsse.single { it.name == "menge" }.art)
        assertTrue(OperatorKnotenSuchindex.suche("Urbild", KanonischerMathematikKnotenKatalog.alle()).any {
            it.art == MengenRechner.KNOTEN_ART && it.standardParameter["operator"] == "menge.urbild"
        })
        assertEquals(EndlicheMenge(setOf(z(-2),z(2))), eval(k).ausgaben.getValue("menge").objekt)
    }

    @Test fun `Moduswechsel erhaelt IDs und vereinigt alle Urbilder`() {
        val k = basis()
        val menge = konfiguriereUrbildEingabeform(k, URBILD_ZIELMENGE)
        assertEquals(k.anschlüsse.map { it.id }, menge.anschlüsse.map { it.id })
        assertEquals(MathematikAnschlussArten.Menge.id, menge.anschlüsse.single { it.name == "ziel" }.art)
        assertEquals(EndlicheMenge(setOf(z(-2),z(2),z(-3),z(3))),
            eval(menge, ziel = EndlicheMenge(setOf(z(4),z(9)))).ausgaben.getValue("menge").objekt)
        assertEquals(k.anschlüsse.map { it.id }, konfiguriereUrbildEingabeform(menge,URBILD_EINZELWERT).anschlüsse.map { it.id })
        assertFails { konfiguriereUrbildEingabeform(k,"unbekannt") }
        assertFails { eval(k.copy(parameter=k.parameter+(URBILD_EINGABEFORM to "unbekannt"))) }
    }

    @Test fun `Ergebnistypen werden aus Signaturen abgeleitet und bleiben idempotent`() {
        val laufzeit = MathematikKartenLaufzeit()
        val k = basis()
        val v = TypisiertesElement("v", "mathematik.vektor.spalte", strukturForm=listOf(z(2)))
        val raum = Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen)
        val f = Methode("v",listOf(v),v,raum,mapOf("v" to raum))
        val r = KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(f)))),emptyList())
        val karte = KartenDaten(name="Typ", knoten=listOf(k))
        val neu = synchronisiereUrbildAnschluesse(karte,r,laufzeit.graphPrüfung)
        assertEquals(MathematikAnschlussArten.SpaltenVektor.id, neu.knoten.single().anschlüsse.single { it.name=="ziel" }.art)
        assertEquals(k.anschlüsse.map { it.id },neu.knoten.single().anschlüsse.map { it.id })
        assertEquals(neu,synchronisiereUrbildAnschluesse(neu,r,laufzeit.graphPrüfung))
        val mehr = Methode("pair",emptyList(),Tupel(listOf(z(1),z(2))),
            Tupelraum(listOf(ReelleZahlen,ReelleZahlen)),ausgabeNamen=listOf("a","b"))
        val rr = r.copy(knoten=mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(mehr)))))
        assertEquals(MathematikAnschlussArten.Tupel.id,synchronisiereUrbildAnschluesse(karte,rr,laufzeit.graphPrüfung).knoten.single().anschlüsse.single { it.name=="ziel" }.art)
    }

    @Test fun `Vorschau meldet lokale und verbundene Verluste und Undo stellt alles wieder her`() {
        val laufzeit = MathematikKartenLaufzeit()
        val k = basis().let { it.copy(parameter=it.parameter+("standardwert.ziel" to "4"),eingangsKartenVerweise=mapOf("ziel" to KartenVerweis(KartenId("extern"),1))) }
        val quelle = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val edge = VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="ziel"}.id))
        val alt = KartenDaten(name="Verlust",knoten=listOf(k,quelle),verbindungen=listOf(edge))
        val plan = planeKonfiguration(alt,konfiguriereUrbildEingabeform(k,URBILD_ZIELMENGE),laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(edge),plan.auswirkung.entfallendeVerbindungen)
        assertTrue(plan.auswirkung.entfallendeLokaleWerte.any {it.contains("Standardwert")})
        assertTrue(plan.auswirkung.entfallendeLokaleWerte.any {it.contains("Kartenfallback")})
        val editor = KartenEditorZustand(alt,laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig(); assertEquals(alt,editor.karte)
        editor.wiederholen(); assertEquals(plan.karte,editor.karte)
    }

    @Test fun `Altplatzhalter migriert verlustfrei und beide Modi roundtrippen`() {
        val a = AnschlussDaten(name="a",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Menge.id)
        val k = basis().copy(anschlüsse=listOf(a,basis().anschlüsse.last()),parameter=mapOf("operator" to "menge.urbild","standardwert.a" to "{4,9}"),eingangsKartenVerweise=mapOf("a" to KartenVerweis(KartenId("extern"),3)))
        val karte = KartenDaten(name="Alt",knoten=listOf(k))
        val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte))
        val nk = neu.knoten.single()
        assertEquals(a.id,nk.anschlüsse.single {it.name=="ziel"}.id)
        assertEquals(URBILD_ZIELMENGE,urbildEingabeform(nk))
        assertEquals("{4,9}",nk.parameter["standardwert.ziel"])
        assertEquals(k.eingangsKartenVerweise["a"],nk.eingangsKartenVerweise["ziel"])
        assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        for (modus in listOf(URBILD_EINZELWERT,URBILD_ZIELMENGE)) {
            val neuKarte = KartenDaten(name="Neu",knoten=listOf(konfiguriereUrbildEingabeform(basis(),modus)))
            assertEquals(neuKarte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neuKarte)))
        }
        val unklar = karte.copy(knoten=listOf(k.copy(anschlüsse=k.anschlüsse+a.copy(id=neueAnschlussId(),name="fremd"))))
        val diagnose = unklar.migriereUrbildKnoten()
        assertEquals(unklar.knoten.single().anschlüsse,diagnose.knoten.single().anschlüsse)
        assertTrue(RECHNER_MIGRATION_DIAGNOSE in diagnose.knoten.single().parameter)
        val kollision=karte.copy(knoten=listOf(k.copy(parameter=k.parameter+("standardwert.ziel" to "{16}"))))
        val erhalten=kollision.migriereUrbildKnoten().knoten.single()
        assertTrue(RECHNER_MIGRATION_DIAGNOSE in erhalten.parameter)
        assertEquals(kollision.knoten.single().anschlüsse,erhalten.anschlüsse)
        assertEquals("{16}",erhalten.parameter["standardwert.ziel"])
    }

    @Test fun `kompatibler Standardwert uebersteht die Typverfeinerung`() {
        val laufzeit = MathematikKartenLaufzeit()
        val k = basis().let { it.copy(parameter=it.parameter+("standardwert.ziel" to "4")) }
        val r = KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(quadrat())))),emptyList())
        val alt=KartenDaten(name="Standard",knoten=listOf(k))
        for (vorschau in listOf(false,true)) {
            val neu=synchronisiereUrbildAnschluesse(alt,r,laufzeit.graphPrüfung,vorschau)
            assertEquals("4",neu.knoten.single().parameter["standardwert.ziel"])
            assertNull(knotenUrbildDiagnose(neu.knoten.single()))
        }
    }

    @Test fun `mengenwertige Methode unterscheidet Mengenwert von Menge von Mengen`() {
        val a=MengenParameter("A")
        val vier=EndlicheMenge(setOf(z(4)))
        val neun=EndlicheMenge(setOf(z(9)))
        val raum=EndlicheMenge(setOf(vier,neun))
        val f=Methode("id",listOf(a),a,Potenzmenge(ReelleZahlen),mapOf("A" to raum))
        val k=basis()
        assertEquals(EndlicheMenge(setOf(vier)),eval(k,f,vier).ausgaben.getValue("menge").objekt)
        val mengenModus=konfiguriereUrbildEingabeform(k,URBILD_ZIELMENGE)
        assertEquals(raum,eval(mengenModus,f,raum).ausgaben.getValue("menge").objekt)
    }

    @Test fun `unbekannte Ergebnissignatur hat allgemeinen Anschluss mit Diagnose`() {
        val f=object : SignaturtragendeMethode {
            override val name="Script"
            override val signatur=MethodenSignatur(emptyList(),listOf(MethodenKomponente("r","r",0,de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt)))
        }
        val laufzeit=MathematikKartenLaufzeit(); val k=basis()
        val r=KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(f)))),emptyList())
        val neu=synchronisiereUrbildAnschluesse(KartenDaten(name="Unbekannt",knoten=listOf(k)),r,laufzeit.graphPrüfung).knoten.single()
        assertEquals(MathematikAnschlussArten.Objekt.id,neu.anschlüsse.single {it.name=="ziel"}.art)
        assertNotNull(knotenUrbildDiagnose(neu))
    }

    @Test fun `Methodenwechsel zeigt abhaengige Verluste vorab und Kopien bleiben auswertbar`() {
        val k=basis()
        val quelle=KnotenDaten(art="test.methode",name="Methode",anschlüsse=listOf(AnschlussDaten(
            name="methode",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Methode.id)))
        val zahl=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+("wert" to "4"))}
        val methodenEdge=VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="methode"}.id))
        val zielEdge=VerbindungDaten(von=AnschlussVerweis(zahl.id,zahl.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="ziel"}.id))
        val register=GesamterMathematikAuswerter.erzeugeRegister().also { r -> r.registriere("test.methode") { ctx ->
            val p=TypisiertesElement("v","mathematik.vektor.spalte",strukturForm=listOf(z(2)))
            val space=Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen)
            val f=if(ctx.knoten.parameter["vektor"]=="true") Methode("id",listOf(p),p,space,mapOf("v" to space)) else quadrat()
            KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(f)))
        } }
        val auswerter=KartenAuswerter(register)
        val laufzeit=MathematikKartenLaufzeit()
        val karte=KartenDaten(name="Graph",knoten=listOf(k,quelle,zahl),verbindungen=listOf(methodenEdge,zielEdge))
        val start=planeKonfiguration(karte,quelle,laufzeit.graphPrüfung,auswerter::auswerten).karte
        val startErgebnis=auswerter.auswerten(start).knoten.getValue(k.id)
        assertNull(startErgebnis.fehler)
        assertEquals(EndlicheMenge(setOf(z(-2),z(2))),startErgebnis.ausgaben.getValue("menge").objekt)
        val plan=planeKonfiguration(start,quelle.copy(parameter=mapOf("vektor" to "true")),laufzeit.graphPrüfung,auswerter::auswerten)
        assertEquals(listOf(zielEdge),plan.auswirkung.entfallendeVerbindungen)
        assertTrue(methodenEdge in plan.karte.verbindungen)
        assertEquals(MathematikAnschlussArten.SpaltenVektor.id,plan.karte.knoten.single {it.id==k.id}.anschlüsse.single {it.name=="ziel"}.art)
        val editor=KartenEditorZustand(start,laufzeit.graphPrüfung)
        editor.wähleAlleKnoten(); editor.dupliziereAuswahl()
        val kopie=editor.karte.knoten.single {it.art==k.art && it.id!=k.id}
        assertTrue(kopie.anschlüsse.none {p -> k.anschlüsse.any {it.id==p.id}})
        assertEquals(EndlicheMenge(setOf(z(-2),z(2))),auswerter.auswerten(editor.karte).knoten.getValue(kopie.id).ausgaben.getValue("menge").objekt)
    }

    @Test fun `Zahlquellen passen auch zu alternativen Ergebnistypen`() {
        val laufzeit=MathematikKartenLaufzeit(); val k=basis()
        val f=object : SignaturtragendeMethode {
            override val name="union"
            override val signatur=MethodenSignatur(emptyList(),listOf(MethodenKomponente("r","r",0,
                de.TeutonStudio.TypSystem.TypAusdruck.Vereinigung(listOf(ReelleZahlen.elementTypAusdruck(),WahrheitsMenge.elementTypAusdruck())))))
        }
        val r=KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(f)))),emptyList())
        val quelle=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val karte=KartenDaten(name="Union",knoten=listOf(k,quelle),verbindungen=listOf(VerbindungDaten(
            von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="ziel"}.id))))
        val neu=synchronisiereUrbildAnschluesse(karte,r,laufzeit.graphPrüfung)
        assertNull(knotenUrbildDiagnose(neu.knoten.first()))
        assertTrue(laufzeit.graphPrüfung.inkompatibleVerbindungen(neu).isEmpty())
    }
}
