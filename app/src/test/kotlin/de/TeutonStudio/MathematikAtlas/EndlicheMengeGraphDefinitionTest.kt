package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class EndlicheMengeGraphDefinitionTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    @Test fun `Graphdefinition erhaelt Eingangsidentitaeten und semantischen Ergebnistyp`() {
        val typ=TypAusdruck.Parameterisiert(MathematischeTypen.Menge,listOf(TypAusdruck.Atom(MathematischeTypen.Zahl)))
        val original=konfiguriereEndlicheMenge(MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero),ENDLICHE_MENGE_GRAPH,1).let {k->
            k.copy(anschlüsse=k.anschlüsse.map {p->if(p.richtung==AnschlussRichtung.Ausgang) p.copy(vertrag=AnschlussVertrag(typ),semantischeId="menge-id")
                else p.copy(semantischeId="element-id",vertrag=AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.Objekt)),zulässigeArten=setOf(MathematikAnschlussArten.Objekt.id))})}
        val definition=endlicheMengeKonzept(original).reiter.single().karte
        val eingang=definition.knoten.single {it.art==KonzeptKnotenArten.EINGANG}
        val ausgang=definition.knoten.single {it.art=="mathematik.kartenAusgang"}
        assertEquals(original.anschlüsse.first().vertrag,eingang.anschlüsse.single().vertrag)
        assertEquals("element-id",eingang.anschlüsse.single().semantischeId)
        assertEquals(original.anschlüsse.first().zulässigeArten,eingang.anschlüsse.single().zulässigeArten)
        assertEquals(AnschlussVertrag(typ),ausgang.anschlüsse.single().vertrag)
        assertEquals("menge-id",ausgang.anschlüsse.single().semantischeId)
        for(wert in listOf<MathematischesObjekt>(RationaleZahl.Eins,Tupel(listOf(RationaleZahl.Eins)))) {
            val direkt=register.finde(original.art)!!.auswerten(KnotenAuswertungsKontext(original,mapOf("element1" to BedingterWert(wert)),RechenKontext()))
            val r=KartenAuswerter(register).auswerten(definition,mapOf(eingang.id to mapOf("wert" to BedingterWert(wert))))
            if(wert is Tupel) {assertNotNull(direkt.fehler);assertTrue(r.fehler.isNotEmpty())}
            else {assertNull(direkt.fehler);assertTrue(r.fehler.isEmpty(),r.fehler.toString());assertEquals(direkt.ausgaben.getValue("menge").objekt,r.knoten.getValue(ausgang.id).ausgaben.getValue("wert").objekt)}
        }
    }
    @Test fun `mehrere Graphobjekte bleiben in der ausfuehrbaren Definition ganze Elemente`() {
        val original=konfiguriereEndlicheMenge(MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero),ENDLICHE_MENGE_GRAPH,2)
        val definition=endlicheMengeKonzept(original).reiter.single().karte
        assertTrue(definition.knoten.none {it.art=="mathematik.einzelmenge" || it.art=="mathematik.vereinigung"})
        val werte=mapOf("element1" to Tupel(listOf(RationaleZahl.Eins)),"element2" to EndlicheMenge(setOf(RationaleZahl.Eins)))
        val vorgaben=definition.knoten.filter {it.art==KonzeptKnotenArten.EINGANG}.associate {it.id to mapOf("wert" to BedingterWert(werte.getValue(it.parameter.getValue("rolle"))))}
        val r=KartenAuswerter(register).auswerten(definition,vorgaben)
        assertTrue(r.fehler.isEmpty(),r.fehler.toString())
        val ausgang=definition.knoten.single {it.art=="mathematik.kartenAusgang"}
        assertEquals(EndlicheMenge(werte.values.toSet()),r.knoten.getValue(ausgang.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `defekte Graphausgaenge liefern auch beim Oeffnen der Definition eine konkrete Diagnose`() {
        val basis=konfiguriereEndlicheMenge(MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero),ENDLICHE_MENGE_GRAPH,1)
        val output=basis.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
        for(k in listOf(basis.copy(anschlüsse=basis.anschlüsse-output),basis.copy(anschlüsse=basis.anschlüsse+output.copy(id=AnschlussId("extra"))))) {
            val definition=endlicheMengeKonzept(k)
            assertEquals("fehler",definition.reiter.single().id)
            assertTrue(definition.reiter.single().karte.knoten.single().parameter.getValue("regel").contains("ausgang",ignoreCase=true))
        }
    }
}
