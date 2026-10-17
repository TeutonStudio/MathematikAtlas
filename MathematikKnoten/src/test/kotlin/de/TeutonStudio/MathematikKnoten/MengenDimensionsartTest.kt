package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MengenDimensionsartTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun eval(k: KnotenDaten, dimension: BedingterWert) = register.findeNachMigration(k.art).auswerten(
        KnotenAuswertungsKontext(k, mapOf("grundmenge" to BedingterWert(ReelleZahlen), "dimension" to dimension),RechenKontext()))
    @Test fun `Raumoperatoren besitzen typisierte Dimensionseingaenge und werten aus`() {
        val basis = MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        for (n in listOf(0L,1L,5L)) {
            val k = konfiguriereMengenRechner(basis,MengenRechnerOperator.TUPELRAUM)
            assertContains(k.anschlüsse.single { it.name=="dimension" }.zulässigeArten, MathematikAnschlussArten.Zahl.id)
            val r = assertIs<KartesischerPotenzraum>(eval(k,BedingterWert(z(n))).ausgaben.getValue("menge").objekt)
            assertEquals(z(n),r.dimension)
        }
        val tensor = konfiguriereMengenRechner(basis,MengenRechnerOperator.TENSORRAUM)
        assertTrue(tensor.anschlüsse.single { it.name=="dimension" }.let { it.art == MathematikAnschlussArten.Tupel.id || MathematikAnschlussArten.Tupel.id in it.zulässigeArten })
        for (rang in listOf(1,2,4,9)) {
            val dimensionen = List(rang) { z(2) }
            assertEquals(dimensionen,assertIs<Tensorraum>(eval(tensor,BedingterWert(Tupel(dimensionen))).ausgaben.getValue("menge").objekt).dimensionen)
        }
        for (d in listOf(Tupel(emptyList()),Tupel(listOf(z(0))),Tupel(listOf(z(-2))),Tupel(listOf(RationaleZahl.von(1,2))))) assertFails { eval(tensor,BedingterWert(d)) }
        assertFalse(MengenRechnerOperator.MATRIZENMENGE in sichtbareMengenRechnerOperatoren())
        assertTrue(sichtbareMengenAuswahlen().none { it.istMatrizenmenge })
    }
    @Test fun `Dimensionsart erhaelt Portidentitaet und verwirft inkompatible lokale Werte`() {
        val k = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter+("standardwert.dimension" to "3")) }
        val port = k.anschlüsse.single { it.name=="dimension" }
        val tensor = konfiguriereMengenDimensionsart(k,MENGEN_DIMENSION_TENSOR)
        assertEquals(port.id,tensor.anschlüsse.single { it.name=="dimension" }.id)
        assertTrue(tensor.anschlüsse.single { it.name=="dimension" }.let { it.art == MathematikAnschlussArten.Tupel.id || MathematikAnschlussArten.Tupel.id in it.zulässigeArten })
        assertFalse("standardwert.dimension" in tensor.parameter)
        assertEquals(MathematikAnschlussArten.Zahl.id,konfiguriereMengenDimensionsart(tensor,MENGEN_DIMENSION_TUPEL).anschlüsse.single { it.name=="dimension" }.art)
        assertEquals(tensor,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name="Tensor",knoten=listOf(tensor)))).knoten.single())
        val grund = register.findeNachMigration(tensor.art).auswerten(KnotenAuswertungsKontext(tensor,emptyMap(),RechenKontext()))
        assertEquals(ReelleZahlen,grund.ausgaben.getValue("menge").objekt)
    }
    @Test fun `symbolische Dimensionen erhalten eindeutige Quellen und Voraussetzungen`() {
        val n = Variable("n",bindungsId="n-id")
        val q = VariablenQuelle(KnotenId("q"),"n",NatürlicheZahlen,bindungsId="n-id")
        val k = konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.TENSORRAUM)
        val d = BedingterWert(Tupel(listOf(n,z(2))),variablenQuellen=listOf(q))
        val r = eval(k,d).ausgaben.getValue("menge")
        assertEquals(listOf(q),r.variablenQuellen)
        assertContains(r.annahmen,Vergleich(n,VergleichsArt.Größer,z(0)))
        assertFails { eval(k,d.copy(variablenQuellen=listOf(q.copy(bindungsId="falsch")))) }
        assertFails { eval(k,d.copy(annahmen=setOf(WahrheitsKonstante(false)))) }
        assertFails { eval(k,d.copy(annahmen=setOf(Vergleich(n,VergleichsArt.KleinerGleich,z(0))))) }
    }
    @Test fun `alte Matrixports werden mit ihren IDs und Kanten in ein Tupel migriert`() {
        val k = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.REELLE_MATRIZEN).erzeuge(GraphPunkt.Zero).let {
            it.copy(parameter=it.parameter+mapOf("standardwert.n" to "2","standardwert.m" to "3")) }
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=mapOf("wert" to "4")) }
        val port = k.anschlüsse.single { it.name=="n" }
        val v = VerbindungDaten(von=AnschlussVerweis(zahl.id,zahl.anschlüsse.single().id),zu=AnschlussVerweis(k.id,port.id))
        val original = KartenDaten(name="Alt",knoten=listOf(k,zahl),verbindungen=listOf(v))
        val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))
        val hilfe = neu.knoten.single { it.art=="mathematik.tupel" }
        assertEquals(port.id,hilfe.anschlüsse.single { it.name=="n" }.id)
        assertEquals(v.id,neu.verbindungen.single { it.id==v.id }.id)
        assertEquals(hilfe.id,neu.verbindungen.single { it.id==v.id }.zu.knotenId)
        assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        val r = KartenAuswerter(register).auswerten(neu).knoten.getValue(k.id)
        assertNull(r.fehler)
        assertEquals(listOf(z(4),z(3)),assertIs<Tensorraum>(r.ausgaben.getValue("menge").objekt).dimensionen)
    }

    @Test fun `Dimensionswechsel zeigt Kanten und lokale Verluste und bleibt gemeinsam rueckgaengig`() {
        val laufzeit = MathematikKartenLaufzeit()
        val k = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).let { it.copy(
            parameter=it.parameter+("standardwert.dimension" to "3"),
            eingangsKartenVerweise=mapOf("dimension" to KartenVerweis(KartenId("extern"),7))) }
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v = VerbindungDaten(von=AnschlussVerweis(zahl.id,zahl.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single { it.name=="dimension" }.id))
        val alt = KartenDaten(name="Wechsel",knoten=listOf(k,zahl),verbindungen=listOf(v))
        val plan = planeKonfiguration(alt,konfiguriereMengenDimensionsart(k,MENGEN_DIMENSION_TENSOR),laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(v),plan.auswirkung.entfallendeVerbindungen)
        assertEquals(2,plan.auswirkung.entfallendeLokaleWerte.size)
        assertEquals(listOf(v),alt.verbindungen) // Abbruch/Vorschau verändert den Ausgang nicht.
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(alt,laufzeit.graphPrüfung)
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig(); assertEquals(alt,editor.karte)
        editor.wiederholen(); assertEquals(plan.karte,editor.karte)
        val kopie = plan.karte.alsNeueKarte()
        assertNotEquals(k.id,kopie.knoten.single { it.art==MENGEN_KNOTEN_ART }.id)
        assertEquals(MENGEN_DIMENSION_TENSOR,mengenDimensionsart(kopie.knoten.single { it.art==MENGEN_KNOTEN_ART }))
    }

    @Test fun `symbolische Altkarte behaelt Dimensionsquellen nach Tupelmigration`() {
        val basis = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.NATUERLICHE_ZAHLEN).erzeuge(GraphPunkt.Zero)
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        val alt = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.REELLE_MATRIZEN).erzeuge(GraphPunkt.Zero).let {
            it.copy(parameter=it.parameter+("standardwert.m" to "2")) }
        val verbindungen = listOf(
            VerbindungDaten(von=AnschlussVerweis(basis.id,basis.anschlüsse.single { it.name=="menge" }.id),zu=AnschlussVerweis(variable.id,variable.anschlüsse.single { it.name=="grundraum" }.id)),
            VerbindungDaten(von=AnschlussVerweis(variable.id,variable.anschlüsse.single { it.name=="wert" }.id),zu=AnschlussVerweis(alt.id,alt.anschlüsse.single { it.name=="n" }.id)))
        val karte = MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name="Symbolisch",knoten=listOf(basis,variable,alt),verbindungen=verbindungen)))
        val r = KartenAuswerter(register).auswerten(karte).knoten.getValue(alt.id)
        assertNull(r.fehler)
        val dimension = assertIs<Tensorraum>(r.ausgaben.getValue("menge").objekt).dimensionen.first()
        assertIs<Variable>(dimension)
        assertEquals(1,r.ausgaben.getValue("menge").variablenQuellen.size)
        assertContains(r.ausgaben.getValue("menge").annahmen,Vergleich(dimension,VergleichsArt.Größer,z(0)))
    }

    @Test fun `Methodendimensionen bleiben mit Diagnose im Original und explizite Auswahl loest Matrixbindung`() {
        val alt = konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.MATRIZENMENGE)
        val quelle = KnotenDaten(art="test.methode",name="Methode",anschlüsse=listOf(AnschlussDaten(name="methode",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Methode.id)))
        val v = VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(alt.id,alt.anschlüsse.single { it.name=="n" }.id))
        val karte = KartenDaten(name="Methodendimension",knoten=listOf(quelle,alt),verbindungen=listOf(v))
        val neu = karte.migriereMatrizenDimensionstupel()
        assertEquals(2,neu.knoten.size); assertEquals(karte.verbindungen,neu.verbindungen)
        val diagnose = neu.knoten.single { it.id==alt.id }
        assertContains(diagnose.parameter.getValue(RECHNER_MIGRATION_DIAGNOSE),"Methodenwertige")
        assertEquals(alt.anschlüsse,diagnose.anschlüsse)
        val weiter = KnotenDaten(art="test.weiter",name="Weiter",anschlüsse=listOf(
            AnschlussDaten(name="x",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Methode.id),
            AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Zahl.id,
                artPriorisiertEingänge=AnschlussArtPriorisierung(eingänge=listOf("x"),prioritäten=listOf(MathematikAnschlussArten.Methode.id)))))
        val eingang = VerbindungDaten(von=v.von,zu=AnschlussVerweis(weiter.id,weiter.anschlüsse.first().id))
        val ausgang = v.copy(von=AnschlussVerweis(weiter.id,weiter.anschlüsse.last().id))
        val gehoben = karte.copy(knoten=listOf(quelle,weiter,alt),verbindungen=listOf(eingang,ausgang))
        val bewahrt = gehoben.migriereMatrizenDimensionstupel()
        assertEquals(gehoben.verbindungen,bewahrt.verbindungen)
        assertContains(bewahrt.knoten.single { it.id==alt.id }.parameter,RECHNER_MIGRATION_DIAGNOSE)
        val fallback = karte.copy(verbindungen=emptyList(),knoten=listOf(alt.copy(eingangsKartenVerweise=mapOf("n" to KartenVerweis(KartenId("extern"),1)))))
        assertContains(fallback.migriereMatrizenDimensionstupel().knoten.single().parameter,RECHNER_MIGRATION_DIAGNOSE)

        val historisch = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter+("raum.matrixHistorisch" to "true")) }
        val tensor = konfiguriereMengenDimensionsart(historisch,MENGEN_DIMENSION_TENSOR)
        assertFalse("raum.matrixHistorisch" in tensor.parameter)
        assertEquals(3,assertIs<Tensorraum>(eval(tensor,BedingterWert(Tupel(List(3) { z(2) }))).ausgaben.getValue("menge").objekt).dimensionen.size)
    }
}
