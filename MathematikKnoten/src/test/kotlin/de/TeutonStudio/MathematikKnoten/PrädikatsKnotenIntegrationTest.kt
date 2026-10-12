package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class PrädikatsKnotenIntegrationTest {
    private fun node(v: KnotenVorlage, id: String) = v.erzeuge(GraphPunkt.Zero).copy(id=KnotenId(id))
    private fun edge(a: KnotenDaten, b: KnotenDaten, name: String) = VerbindungDaten(
        von=AnschlussVerweis(a.id,a.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }.id),
        zu=AnschlussVerweis(b.id,b.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang && it.name == name }.id))
    private fun fixture(): Pair<KartenDaten,KnotenId> {
        val x=node(MathematikKnotenVorlagen.Variable,"x").copy(parameter=mapOf("name" to "x", "werteVorrat" to "R"))
        val a=node(MathematikKnotenVorlagen.AllgemeinerParameter,"a").copy(parameter=mapOf("name" to "a"))
        val mul=node(MathematikKnotenVorlagen.Multiplikation,"mul")
        val four=node(MathematikKnotenVorlagen.Zahl,"four").copy(parameter=mapOf("wert" to "4"))
        val eq=node(MathematikKnotenVorlagen.Gleichheit,"eq")
        val solve=node(MathematikKnotenVorlagen.Auflösen,"solve")
        val operands=mul.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
        return KartenDaten(name="Prädikatskarte",knoten=listOf(x,a,mul,four,eq,solve), verbindungen=listOf(
            edge(x,mul,operands[0].name),edge(a,mul,operands[1].name),edge(mul,eq,"links"),edge(four,eq,"rechts"),edge(eq,solve,"relation"))) to solve.id
    }
    @Test fun `connected graph distinguishes numeric parameter from target variable`() {
        val (karte,id)=fixture()
        val r=MathematikKartenLaufzeit().auswerten(karte)
        assertTrue(r.fehler.isEmpty(),r.fehler.joinToString())
        val e=r.knoten.getValue(id)
        assertEquals(listOf("x"),e.lösungsVariablen.map { it.variable.name })
        assertEquals(listOf("a"),e.lösungsParameter.map { it.name })
        assertIs<DefinierteMenge>(e.ausgaben.getValue("lösungsmenge").objekt)
    }
    @Test fun `json reload regenerates roles and stable connections`() {
        val (karte,id)=fixture()
        val geladen=MathematikKartenCodec.lese(MathematikKartenCodec.schreibe(karte))
        assertEquals(karte.verbindungen,geladen.verbindungen)
        val laufzeit=MathematikKartenLaufzeit()
        val vorher=laufzeit.auswerten(karte).knoten.getValue(id)
        val nachher=laufzeit.auswerten(geladen).knoten.getValue(id)
        assertEquals(vorher.lösungsVariablen,nachher.lösungsVariablen)
        assertEquals(vorher.lösungsParameter,nachher.lösungsParameter)
        assertEquals(vorher.ausgaben,nachher.ausgaben)
    }
    @Test fun `parameter rename invalidates cache and undo restores source role`() {
        val (karte,id)=fixture()
        val laufzeit=MathematikKartenLaufzeit()
        val editor=KartenEditorZustand(karte,laufzeit.graphPrüfung)
        val original=laufzeit.auswerten(editor.karte).knoten.getValue(id)
        val a=karte.knoten.first { it.id == KnotenId("a") }
        editor.führeAus(KartenAktion.KnotenErsetzen(a.copy(parameter=a.parameter + ("name" to "b"))))
        assertEquals(listOf("b"),laufzeit.auswerten(editor.karte).knoten.getValue(id).lösungsParameter.map { it.name })
        editor.rückgängig()
        assertEquals(original.ausgaben,laufzeit.auswerten(editor.karte).knoten.getValue(id).ausgaben)
    }
    @Test fun `auswerten separates undefined predicate and an open predicate`() {
        val knoten=node(MathematikKnotenVorlagen.Auswerten,"eval")
        val auswerter=GesamterMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        fun eval(p:Aussage)=auswerter.auswerten(KnotenAuswertungsKontext(knoten,mapOf("term" to BedingterWert(p)),RechenKontext()))
        val undefined=Division(RationaleZahl.Eins,RationaleZahl.Null)
        val r=eval(Gleichheit(undefined,undefined))
        assertNotNull(r.fehler)
        assertEquals(EntscheidungsStatus.NichtAuswertbar,r.entscheidung?.status)
        val offen=eval(Vergleich(Variable("x"),VergleichsArt.Größer,RationaleZahl.Eins))
        assertNull(offen.fehler)
        assertNull(offen.entscheidung?.wahrheitswert)
        assertIs<Vergleich>(offen.ausgaben.getValue("term").objekt)
    }
    @Test fun `conflicting variable and parameter origins cannot silently merge`() {
        val knoten=node(MathematikKnotenVorlagen.Auflösen,"solve")
        val p=Gleichheit(Variable("x"),RationaleZahl.Eins)
        val eingang=BedingterWert(p,variablenQuellen=listOf(
            VariablenQuelle(KnotenId("v"),"x",ReelleZahlen,lösungsRolle=LösungsQuellenRolle.Variable),
            VariablenQuelle(KnotenId("p"),"x",ReelleZahlen,lösungsRolle=LösungsQuellenRolle.Parameter)))
        assertFailsWith<IllegalArgumentException> {
            StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten,mapOf("relation" to eingang),RechenKontext()))
        }
    }
}
