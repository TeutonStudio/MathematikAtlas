package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*
import kotlin.test.*

class PolynomNumerikIntegrationTest {
    @Test fun `Gleichungsauswerter leitet Numerikproblem aus echtem Eingang ab`()=runBlocking {
        val x=Variable("x")
        val p=Gleichheit(Potenz(x,RationaleZahl.von(2)),RationaleZahl.von(2))
        val knoten=MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val r=StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten,
            mapOf("relation" to BedingterWert(p,variablenQuellen=listOf(VariablenQuelle(KnotenId("x"),"x",ReelleZahlen,lösungsRolle=LösungsQuellenRolle.Variable)))),RechenKontext()))
        assertNotNull(r.polynomProblem)
        val zahlen=PolynomNumerikDienst.berechne(r.polynomProblem!!,PolynomNumerikKonfiguration(8))
        assertEquals(listOf("-1,41421356","1,41421356"),zahlen.loesungen.map { it.dezimalstellen!!.single() })
        assertTrue(zahlen.analytischeFormeln.isNotEmpty())
    }
    @Test fun `Matrixvariable behaelt Anschluss ID und Eintragsquellen nach JSON`() {
        val alt=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        val neu=konfiguriereMatrixVariable(alt.copy(parameter=alt.parameter+("name" to "X")),2)
        assertEquals(alt.anschlüsse.map { it.id },neu.anschlüsse.map { it.id })
        assertEquals(MathematikAnschlussArten.Matrix.id,neu.anschlüsse.single { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang }.art)
        val karte=KartenDaten(name="Matrixvariable",knoten=listOf(neu))
        val geladen=MathematikKartenCodec.lese(MathematikKartenCodec.schreibe(karte))
        val r=MathematikKartenLaufzeit().auswerten(geladen).knoten.getValue(neu.id)
        val wert=r.ausgaben.getValue("wert")
        assertIs<Matrix>(wert.objekt)
        assertEquals(listOf("X_1_1","X_1_2","X_2_1","X_2_2"),wert.variablenQuellen.map { it.name })
        assertTrue(wert.variablenQuellen.all { it.lösungsRolle==LösungsQuellenRolle.Variable })
    }
    @Test fun `Genauigkeit ist persistent und atomar rueckgaengig`() {
        val alt=MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val neu=konfigurierePolynomNumerik(alt,PolynomNumerikKonfiguration(30,7))
        val editor=KartenEditorZustand(KartenDaten(name="Numerik",knoten=listOf(alt)),MathematikKartenLaufzeit().graphPrüfung)
        editor.führeAus(KartenAktion.KnotenErsetzen(neu))
        assertEquals(PolynomNumerikKonfiguration(30,7),polynomNumerikKonfiguration(editor.karte.knoten.single().parameter))
        val geladen=MathematikKartenCodec.lese(MathematikKartenCodec.schreibe(editor.karte))
        assertEquals(neu.parameter,geladen.knoten.single().parameter)
        editor.rückgängig()
        assertEquals(alt.parameter,editor.karte.knoten.single().parameter)
    }
    @Test fun `Dienst beachtet geaenderte Eingaben und Abbruch`()=runBlocking {
        val x=Variable("x")
        fun problem(n:Long)=PolynomGleichungsProblem(Gleichheit(x,RationaleZahl.von(n)),listOf(x),listOf(subtraktion(x,RationaleZahl.von(n))),false)
        val a=PolynomNumerikDienst.berechne(problem(1),PolynomNumerikKonfiguration())
        val b=PolynomNumerikDienst.berechne(problem(2),PolynomNumerikKonfiguration())
        assertNotEquals(a.loesungen,b.loesungen)
        val job=launch(start=CoroutineStart.LAZY) { PolynomNumerikDienst.berechne(problem(3),PolynomNumerikKonfiguration()) }
        job.cancelAndJoin(); assertTrue(job.isCancelled)
    }
    @Test fun `Auswerten einer Matrix bietet Eigenwerte als fluechtiges Problem`() {
        val knoten=MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val a=Matrix(listOf(listOf(RationaleZahl.von(3))))
        val r=StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten,mapOf("term" to BedingterWert(a)),RechenKontext()))
        assertEquals(a,r.polynomProblem?.eigenwertMatrix)
        assertEquals(a,r.ausgaben.values.single().objekt)
    }
    @Test fun `Matrixvariable laeuft durch den echten Gleichheitsgraphen`() {
        val x=konfiguriereMatrixVariable(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("name" to "X","werteVorrat" to "R")),2)
        val eq=MathematikKnotenVorlagen.Gleichheit.erzeuge(GraphPunkt.Zero)
        val solver=MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        fun edge(a:KnotenDaten,b:KnotenDaten,name:String)=VerbindungDaten(
            von=AnschlussVerweis(a.id,a.anschlüsse.first { it.richtung==AnschlussRichtung.Ausgang }.id),
            zu=AnschlussVerweis(b.id,b.anschlüsse.first { it.richtung==AnschlussRichtung.Eingang && it.name==name }.id))
        val karte=KartenDaten(name="Matrixgleichung",knoten=listOf(x,eq,solver),verbindungen=listOf(edge(x,eq,"links"),edge(x,eq,"rechts"),edge(eq,solver,"relation")))
        val r=MathematikKartenLaufzeit().auswerten(karte)
        assertTrue(r.fehler.isEmpty(),r.fehler.joinToString())
        val p=r.knoten.getValue(solver.id).polynomProblem!!
        assertEquals(4,p.variablen.size)
        assertEquals(4,loesePolynomGleichungsSystem(p).dimension)
    }

}
