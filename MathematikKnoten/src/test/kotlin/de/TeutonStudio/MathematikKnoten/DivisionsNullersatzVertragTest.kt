package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class DivisionsNullersatzVertragTest {
    @Test fun `offener Nullfall behaelt Ersatz ohne globale Nichtnullvoraussetzung`() {
        val y=Variable("y");val ersatz=RationaleZahl.von(99)
        val offen=divisionMitNullersatz(RationaleZahl.von(8),y,ersatz,RechenKontext())
        val fall=assertIs<ZahlFallAusdruck>(offen.wert)
        assertEquals(ersatz,fall.wahr);assertEquals(Gleichheit(y,RationaleZahl.Null),fall.aussage)
        assertTrue(offen.bedingungen.isEmpty())
        assertEquals(setOf(Ungleichheit(y,RationaleZahl.Null)),divisionMitNullersatz(RationaleZahl.Eins,y,null,RechenKontext()).bedingungen)
        assertEquals(ersatz,divisionMitNullersatz(RationaleZahl.Eins,y,ersatz,RechenKontext(annahmen=setOf(Gleichheit(y,RationaleZahl.Null)))).wert)
        assertFails {divisionMitNullersatz(RationaleZahl.Eins,RationaleZahl.Null,null,RechenKontext())}
    }
    @Test fun `widerlegte Nennerannahmen duerfen feste Werte nicht umdeuten`() {
        assertFails {divisionMitNullersatz(RationaleZahl.Eins,RationaleZahl.Null,RationaleZahl.von(99),
            RechenKontext(annahmen=setOf(Ungleichheit(RationaleZahl.Null,RationaleZahl.Null))))}
        assertFails {divisionMitNullersatz(RationaleZahl.Eins,RationaleZahl.von(2),RationaleZahl.von(99),
            RechenKontext(annahmen=setOf(Gleichheit(RationaleZahl.von(2),RationaleZahl.Null))))}
    }
    @Test fun `freie Divisionsformel erbt keinen Nullersatz aus der umgebenden Knotenkonfiguration`() {
        val k=ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("standardwert.c" to "99"))
        val b=Variable("b");val bedingungen=mutableSetOf<Aussage>()
        assertEquals(Division(RationaleZahl.Eins,b),werteFormelOperationAus("zahl.division",mapOf("a" to RationaleZahl.Eins,"b" to b),k,bedingungen))
        assertEquals(setOf<Aussage>(Ungleichheit(b,RationaleZahl.Null)),bedingungen)
        assertFails {werteFormelOperationAus("zahl.division",mapOf("a" to RationaleZahl.Eins,"b" to RationaleZahl.Null),k,mutableSetOf())}
    }
    @Test fun `gehobene Division bleibt an der Nullstelle auswertbar und erhaelt Ergebnisprojektion`() {
        val x=Variable("x")
        val f=Methode("f",listOf(x),x,ReelleZahlen,mapOf("x" to ReelleZahlen))
        val k=ZahlenRechnerKnotenVorlagen.alle.single {it.standardParameter[ZAHLENRECHNER_OPERATOR]==UniversellerZahlenOperator.DIVISION.stabileId}.erzeuge(GraphPunkt.Zero)
        val r=GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf(
            "a" to BedingterWert(RationaleZahl.von(8)),"b" to BedingterWert(f),"c" to BedingterWert(RationaleZahl.von(99))),RechenKontext()))
        assertNull(r.fehler)
        val m=assertIs<MathematischeMethode>(r.ausgaben.getValue("wert").objekt)
        assertIs<ZahlFallAusdruck>(m.vorschrift)
        assertEquals(RationaleZahl.von(99),m.wendeAn(listOf(RationaleZahl.Null)))
        assertTrue(r.ausgaben.getValue("wert").annahmen.isEmpty())
    }
}
