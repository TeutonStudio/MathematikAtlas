package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ÜbersetzungsZeugnisIntegrationTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(n:Long)=RationaleZahl.von(n)
    private fun eval(ziel:String,w:MathematischesObjekt,p:Map<String,String> = emptyMap(),extra:Map<String,BedingterWert> = emptyMap()):KnotenAuswertungsErgebnis {
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),ziel).let { it.copy(parameter=it.parameter+p) }
        return register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, mapOf("wert" to BedingterWert(w))+extra,RechenKontext()))
    }
    private fun bezeugt(r:KnotenAuswertungsErgebnis) {
        if(r.fehler!=null) { assertNotNull(r.übersetzungsDiagnose);assertTrue(r.ausgaben.isEmpty()) }
        else { assertTrue(r.ausgaben.isNotEmpty());r.ausgaben.values.forEach {
            assertNotNull(it.übersetzungen.aktuell)
            val export=it.alsÜbertragenerMathematischerWert()
            assertEquals(export,ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(export)))
        } }
    }
    @Test fun `jedes bestehende Ziel liefert Zeugnis oder strukturierte Nichtdefiniertheit`() {
        val t=Tupel(listOf(z(1),z(2)));val raum=EuklidischerRaum("E",2)
        val punkt=GeometriePunkt("A",raum,t);val strecke=GeometrieStrecke(punkt,GeometriePunkt("B",raum,Tupel(listOf(z(3),z(2)))))
        val matrix=Matrix(listOf(listOf(z(1),z(0)),listOf(z(0),z(1))))
        val beispiele=mapOf<String,MathematischesObjekt>("tupel" to t,"zeile" to t,"spalte" to t,"methode" to z(3),"indexmethode" to matrix,"matrixzerlegen" to matrix,"polar" to t,"kartesisch" to Tupel(listOf(z(2),Tupel(listOf(z(1),GradWinkel(z(0)))))),"grad" to z(0),"bogenmass" to GradWinkel(z(90)),"koordinatenmenge" to strecke,"mittelpunkt" to strecke,"objektpunkt" to strecke,"geometrie" to t)
        assertEquals(ÜBERSETZER_ZIELE.toSet()-"rueckfuehrung",beispiele.keys)
        beispiele.forEach { (ziel,w) ->
            val r=eval(ziel,w,extra=if(ziel=="objektpunkt") mapOf("anker" to BedingterWert(punkt)) else emptyMap())
            bezeugt(r)
            assertNull(r.fehler,"$ziel: ${r.fehler}")
            r.ausgaben.values.forEach { wert ->
                val export=wert.alsÜbertragenerMathematischerWert()
                assertEquals(export,ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(export)),ziel)
            }
        }
    }
    @Test fun `fruehe Indexrueckgaben und Varianten sind vollstaendig bezeugt oder diagnostiziert`() {
        val matrix=Matrix(listOf(listOf(z(1),z(0)),listOf(z(0),z(1))))
        val bausteine=listOf("materialisieren:tupel","materialisieren:zeile","materialisieren:spalte","materialisieren:matrix","materialisieren:tensor","materialisieren:vorbild","diagonalKomponenten","potenzQuotient","potenzRest","kommutativerVektor","schnitte:0","polynomQuelle","spaltenSammeln","vektorisieren","partikulaereLoesung","monomIndex","alsMatrix","hermitescheProjektoren","orthogonaleProjektoren","einheitSpalten","nullMatrix","konjugiertTransponiert","unteresDreieck","ausserdiagonal","nichtnullSpalten","vektorfeldKomponenten","jordanBedingungen","nichtnegativeDiagonale")+listOf("Q","R","U","Sigma","V","lambdas","Projektoren","Lambda","P","J").map { "kandidat:$it" }
        for(v in bausteine) bezeugt(eval("indexmethode",matrix,mapOf("indexmethode.baustein" to v)))
        val auswahl=listOf("nichtnullIndikator","rationaleMatrix","matrixQuelle","symbolischeMatrix","iterationsbereich","umformen","zyklus3:0","zyklus3:1","zyklus3:2","minoren:komponenten","minoren:vorzeichen","minoren:grade","einheit","alternierend","komponenten","vorzeichen","minor","minorvorzeichen")
        for(v in auswahl) bezeugt(eval("indexmethode",matrix,mapOf("indexmethode.auswahl" to v,"indexmethode.form" to "4")))
        for(v in listOf("permutation","diagonale","hauptdiagonale","gegendiagonale","schnitt")) bezeugt(eval("indexmethode",matrix,mapOf("indexmethode.achsenModus" to v,"indexmethode.achsen" to "1,2","indexmethode.schnittIndex" to "1")))
    }
    @Test fun `nachfolgende Berechnung fuehrt Zeugnis nur als Herkunft`() {
        val r=eval("zeile",Tupel(listOf(z(1)))).ausgaben.getValue("ergebnis")
        val berechnung=KnotenAuswertungsErgebnis(mapOf("wert" to r.copy(objekt=z(9)))).führeÜbersetzungsHerkunftFort(listOf(r)).ausgaben.getValue("wert")
        assertNull(berechnung.übersetzungen.aktuell)
        assertEquals(listOf(r.übersetzungen.aktuell),berechnung.übersetzungen.herkunft)
    }
    @Test fun `Formelzeugnis behaelt Identitaeten beim Kopieren Undo Redo und Kartenladen`() {
        val r=ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(Tupel(listOf(z(1))),ÜbersetzungsZiel.ZEILE,
            ÜbersetzungsKontext(quellen=listOf(ÜbersetzungsQuelle("semantisch-x","x",ReelleZahlen))))) as ÜbersetzungsErgebnis.Erfolg
        val literal=FormelAusdruck.Literal("l",r.wert,FormelTyp.OBJEKT,ÜbersetzungsMetadaten(r.zeugnis,kontext=r.kontext))
        val k=konfiguriereStrukturRechnerFormel(StrukturFormelRechnerVorlagen.Vektor.erzeuge(GraphPunkt.Zero),StrukturRechnerKnotenFamilie.VEKTOR,literal)
        val karte=KartenDaten(name="Zeugnis ohne Ursprung",knoten=listOf(k))
        val ablage=de.TeutonStudio.KnotenKartenVerwalter.logik.AtlasZwischenablage()
        assertTrue(ablage.kopiere(karte,setOf(k.id)))
        val kopie=ablage.erzeugeEinfügung(GraphPunkt(200f,0f))!!.knoten.single()
        assertNotEquals(k.id,kopie.id)
        val editor=de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(karte,MathematikKartenLaufzeit().graphPrüfung)
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KartenInhaltErsetzen(karte.copy(knoten=listOf(kopie))))
        editor.rückgängig();assertEquals(karte,editor.karte)
        editor.wiederholen()
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(editor.karte))
        assertEquals(literal,ladeStrukturRechnerFormel(geladen.knoten.single()))
        val aus=KartenAuswerter(register).auswerten(geladen).knoten.getValue(kopie.id).ausgaben.getValue("wert")
        assertEquals(r.zeugnis,aus.übersetzungen.aktuell)
        assertEquals(mapOf("semantisch-x" to "semantisch-x"),aus.übersetzungen.aktuell!!.identitätsZuordnung)
    }

}
