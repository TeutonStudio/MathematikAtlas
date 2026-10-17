package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class PolynomArgumentVertragTest {
    private fun phasen(alt:KnotenDaten):List<KnotenDaten> {
        val k=KartenDaten(name="Original",knoten=listOf(alt));val text=KartenDatenJson.schreibe(k)
        return listOf(MathematikKartenCodec.lese(text),MathematikKartenCodec.lade(text),MathematikKartenCodec.importiere(text),MathematikKartenMigrationen.vorSpeichern(k)).map {it.knoten.single()}
    }
    @Test fun `alle vorhandenen erweiterten Zahlenoperatoren bleiben durch den Codec bekannt`() {
        for(op in ErweiterterZahlenOperator.entries) {
            val n=konfiguriereErweitertenZahlenRechner(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),op)
            for(neu in phasen(n)) assertNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE],"${op.stabileId}: ${neu.parameter}")
        }
    }
    @Test fun `intrinsischer Altvertrag erhaelt Namen Ports Fallbacks und symbolische Koeffizienten beider Orientierungen`() {
        val basis=MathematikKnotenVorlagen.VektorZuPolynom.erzeuge(GraphPunkt.Zero)
        val alt=basis.copy(name="Eigenes Polynom",parameter=basis.parameter+mapOf("variable" to "sin","standardwert.vektor" to "lokal"),
            eingangsKartenVerweise=mapOf("vektor@karte:extern" to KartenVerweis(KartenId("extern"),4)))
        val a=Variable("a");val argument:ZahlAusdruck=Variable("sin")
        for(n in phasen(alt)) {
            assertNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(ZAHLENRECHNER_ART,n.art)
            assertEquals(POLYNOM_ARGUMENT_VARIABLE,n.parameter[POLYNOM_ARGUMENT_MODUS]);assertEquals(alt.name,n.name)
            assertEquals(alt.anschlüsse.map {it.id},n.anschlüsse.map {it.id});assertEquals(listOf("koeffizienten","wert"),n.anschlüsse.map {it.name})
            assertEquals("lokal",n.parameter["standardwert.koeffizienten"]);assertEquals(alt.eingangsKartenVerweise.values.single(),n.eingangsKartenVerweise["koeffizienten@karte:extern"])
            for(v in listOf<OrientierterVektor>(SpaltenVektor(listOf(a,RationaleZahl.Eins)),ZeilenVektor(listOf(a,RationaleZahl.Eins)))) {
                val r=GesamterMathematikAuswerter.erzeugeRegister().finde(n.art)!!.auswerten(KnotenAuswertungsKontext(n,mapOf("koeffizienten" to BedingterWert(v)),RechenKontext()))
                assertEquals(polynomAusKoeffizienten(v.werte,argument),r.ausgaben.getValue("wert").objekt)
                assertTrue(r.ausgaben.getValue("wert").variablenQuellen.isEmpty())
                assertEquals(alt.anschlüsse.first().vertrag,n.anschlüsse.first().vertrag)
            }
        }
    }
    @Test fun `unbekannter Argumentmodus und explizit leerer intrinsischer Name werden bewahrt und gesperrt`() {
        val n=konfigurierePolynomArgument(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),POLYNOM_ARGUMENT_VARIABLE)
        for(alt in listOf(n.copy(parameter=n.parameter+(POLYNOM_ARGUMENT_MODUS to "unbekannt")),n.copy(parameter=n.parameter+("variable" to " ")))) {
            for(neu in phasen(alt)) {assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(alt,neu.copy(parameter=neu.parameter-RECHNER_MIGRATION_DIAGNOSE))}
        }
    }
    @Test fun `widerspruechlicher Polynomausgang und fremde Diagnose bleiben gesperrt`() {
        val basis=konfigurierePolynomArgument(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),POLYNOM_ARGUMENT_VARIABLE)
        val falsch=basis.copy(anschlüsse=basis.anschlüsse.map {if(it.richtung==AnschlussRichtung.Ausgang) it.copy(art=MathematikAnschlussArten.Aussage.id) else it})
        for(neu in phasen(falsch)) {assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(falsch,neu.copy(parameter=falsch.parameter))}
        val gesperrt=basis.copy(parameter=basis.parameter+(RECHNER_MIGRATION_DIAGNOSE to "Fremder Graphfehler: $POLYNOM_KONFIG_DIAGNOSE"))
        assertEquals(gesperrt.parameter[RECHNER_MIGRATION_DIAGNOSE],konfigurierePolynomArgument(gesperrt,POLYNOM_ARGUMENT_EINGANG).parameter[RECHNER_MIGRATION_DIAGNOSE])
    }

    @Test fun `intrinsischer Name darf keine fremde Koeffizientenidentitaet als Alias verwenden`() {
        val k=konfigurierePolynomArgument(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),POLYNOM_ARGUMENT_VARIABLE,"x")
        val u=Variable("u",bindungsId="x")
        val quelle=VariablenQuelle(KnotenId("koeff"),"u",ReelleZahlen,bindungsId="x")
        for(werte in listOf(listOf<ZahlAusdruck>(u),listOf<ZahlAusdruck>(u,RationaleZahl.Eins))) {
            val r=GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf(
                "koeffizienten" to BedingterWert(SpaltenVektor(werte),variablenQuellen=listOf(quelle))),RechenKontext()))
            val w=r.ausgaben.getValue("wert")
            assertEquals(if(werte.size==1) ReelleZahlen else KomplexeZahlen,w.werteVorrat)
            assertEquals(listOf(quelle),w.variablenQuellen)
            if(werte.size==1) assertEquals(u,w.objekt)
        }
    }

}
