package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VektorUnterraumDefinitionTest {
    @Test fun `Potenzdefinition zerlegt konkret und ist ausfuehrbar`() {
        for (p in 0..4) {
            val original=vektorRechnerVorlage(VektorRechnerOperator.POTENZ).erzeuge(GraphPunkt.Zero).copy(parameter=mapOf(VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.POTENZ.stabileId,VEKTOR_POTENZ_EXPONENT to p.toString()))
            val karte=vektorPotenzDefinitionsKarte(original)
            assertEquals(karte,vektorPotenzDefinitionsKarte(original))
            assertFalse(karte.knoten.any { it.parameter[VEKTOR_RECHNER_OPERATOR]==VektorRechnerOperator.POTENZ.stabileId })
            val eingang=karte.knoten.single { it.art==KonzeptKnotenArten.EINGANG }
            val r=KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte,mapOf(eingang.id to mapOf("wert" to BedingterWert(Tupel(listOf(RationaleZahl.von(2),RationaleZahl.von(3)))))))
            assertTrue(r.fehler.isEmpty(),"p=$p: ${r.fehler}")
            val wert=r.knoten.getValue(karte.knoten.last().id).ausgaben.getValue("wert").objekt
            if(p==2) assertEquals(RationaleZahl.von(13),vereinfache(wert as ZahlAusdruck))
            if(p==3) assertEquals(listOf(RationaleZahl.von(26),RationaleZahl.von(39)),(wert as SpaltenVektor).werte.map { vereinfache(it) })
            assertEquals(karte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)))
        }
    }
    @Test fun `Unterraumdefinition besteht aus Linearkombination und Bildmenge`() {
        val original=konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.UNTERRAUM)
        val karte=unterraumDefinitionsKarte(original)
        assertFalse(karte.knoten.any { it.parameter[MENGENRECHNER_OPERATOR_PARAMETER]==MengenRechnerOperator.UNTERRAUM.stabileId })
        assertTrue(karte.knoten.any { it.art=="mathematik.abbild" })
        val inputs=karte.knoten.filter { it.art==KonzeptKnotenArten.EINGANG }.associate { k -> k.id to mapOf("wert" to BedingterWert(Tupel(if(k.name=="a.1") listOf(RationaleZahl.Eins,RationaleZahl.Null) else listOf(RationaleZahl.Null,RationaleZahl.Eins)))) }
        val r=KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte,inputs)
        assertTrue(r.fehler.isEmpty(),r.fehler.toString())
        assertIs<MengenAusdruck>(r.knoten.getValue(karte.knoten.last().id).ausgaben.getValue("wert").objekt)
    }
    @Test fun `Affiner Spann ohne Richtungen liefert Koordinatensingleton`() {
        val k=konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.UNTERRAUM).copy(parameter=mapOf("definition.richtungsanzahl" to "0","unterraum.affin" to "true"))
        val karte=unterraumDefinitionsKarte(k)
        val input=karte.knoten.single { it.art==KonzeptKnotenArten.EINGANG }
        val r=KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte,mapOf(input.id to mapOf("wert" to BedingterWert(SpaltenVektor(listOf(RationaleZahl.Eins,RationaleZahl.von(2)))))))
        assertTrue(r.fehler.isEmpty(),r.fehler.toString())
        assertEquals(EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.Eins,RationaleZahl.von(2))))),r.knoten.getValue(karte.knoten.last().id).ausgaben.getValue("wert").objekt)
    }
}
