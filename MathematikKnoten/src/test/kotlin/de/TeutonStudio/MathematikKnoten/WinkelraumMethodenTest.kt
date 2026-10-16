package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class WinkelraumMethodenTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(x:Long)=RationaleZahl.von(x)
    private fun eval(k:KnotenDaten,werte:Map<String,BedingterWert>)=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,werte,RechenKontext())).also { assertNull(it.fehler,it.fehler) }
    private fun quelle(n:Int,name:String="w"):BedingterWert {
        val k=konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero),VARIABLE_STRUKTUR_WINKEL).let { it.copy(parameter=it.parameter+mapOf(VARIABLE_DIMENSION to n.toString(),"name" to name)) }
        return eval(k,testGrundraum(k)).ausgaben.getValue("wert")
    }
    @Test fun `Wn Quelle Polar Methode Argumente und oeffentliche Definitionsmenge bilden eine Struktur`() {
        for(n in listOf(0,1,2,3,64)) {
            val w=quelle(n)
            assertEquals(1,w.variablenQuellen.size)
            assertEquals(winkelraum(n),w.variablenQuellen.single().werteVorrat)
            val polar=konfiguriereÜbersetzerPolarAnschlüsse(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"kartesisch"),true)
            val koordinaten=eval(polar,mapOf("radius" to BedingterWert(z(2)),"winkel" to w)).ausgaben.getValue("ergebnis")
            val term=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
            val methode=assertIs<MathematischeMethode>(eval(term,mapOf("wert" to koordinaten)).ausgaben.getValue("ergebnis").objekt)
            assertEquals(listOf("w"),methode.parameter.map { it.name })
            assertEquals(listOf("wert"),methode.ausgabeNamen)
            val argument=Tupel(listOf(z(-1))+List(n) { z(0) })
            val result=assertIs<Tupel>(methode.wendeAn(listOf(argument)))
            assertEquals(n+1,result.elemente.size)
            assertTrue(result.freieMethodenParameter().isEmpty())
            val df=KnotenDaten(art=METHODEN_WERTEVORRAT_ART,name="Definitionsmenge")
            assertEquals(winkelraum(n),eval(df,mapOf("methode" to BedingterWert(methode))).ausgaben.getValue("menge").objekt)
        }
    }
    @Test fun `Strukturbenennung faengt unabhaengige w und w1 weder im Term noch Zusatzparameter ein`() {
        val w=quelle(1)
        val basis=w.objekt as Tupel
        val termWert=w.copy(zielMenge=null,werteVorrat=null,komponentenMengen=null,objekt=Tupel(listOf(basis.elemente[0],Variable("w"),Variable("w_1"))),variablenQuellen=w.variablenQuellen+listOf(VariablenQuelle(KnotenId("scalarw"),"w",ReelleZahlen),VariablenQuelle(KnotenId("scalarw1"),"w_1",ReelleZahlen)))
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
        val methode=assertIs<MathematischeMethode>(eval(k,mapOf("wert" to termWert)).ausgaben.getValue("ergebnis").objekt)
        assertEquals(setOf("w","w_1","w_2"),methode.parameter.map { it.name }.toSet())
        assertEquals(Tupel(listOf(z(-1),z(2),z(3))),methode.wendeMathematischAn(mapOf("w" to z(2),"w_1" to z(3),"w_2" to Tupel(listOf(z(-1),z(90))))))
    }
    @Test fun `Zusatzparameterquellen werden einmalig gemeinsam normalisiert`() {
        val w=quelle(1)
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
        for(wert in listOf(w,BedingterWert(z(3)))) {
            val m=assertIs<MathematischeMethode>(eval(k,mapOf("wert" to wert,"parameterQuelle" to w)).ausgaben.getValue("ergebnis").objekt)
            assertEquals(listOf("w"),m.parameter.map { it.name })
            val result=m.wendeAn(listOf(Tupel(listOf(z(-1),z(90)))))
            assertTrue(result.freieMethodenParameter().isEmpty())
        }
    }
    @Test fun `Historische Konventionen und Mehrfachausgaben werden explizit migriert`() {
        val roh=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero).copy(parameter=emptyMap())
        val klassisch=roh.copy(eigenschaften=roh.eigenschaften+(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("kartesisch")))
        val karte=KartenDaten(name="Migration",knoten=listOf(klassisch)).migrierePolarKonventionen()
        assertEquals(PolarKonvention.CLASSIC,übersetzerPolarKonvention(karte.knoten.single()))
        assertEquals(klassisch.anschlüsse.map { it.id },karte.knoten.single().anschlüsse.map { it.id })
        val altMethode=roh.copy(eigenschaften=roh.eigenschaften+(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("methode")))
        val migriert=KartenDaten(name="Migration",knoten=listOf(altMethode)).migrierePolarKonventionen().knoten.single()
        val methode=assertIs<MathematischeMethode>(eval(migriert,mapOf("wert" to BedingterWert(Tupel(listOf(z(1),z(2)))))).ausgaben.getValue("ergebnis").objekt)
        assertEquals(listOf("element1","element2"),methode.ausgabeNamen)
    }
}
