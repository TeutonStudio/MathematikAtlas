package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class WinkelraumAufrufMigrationTest {
    private fun port(name: String, i: Int) = AnschlussDaten(name=name, richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=MathematikAnschlussArten.AtlasWert.id, reihenfolge=i)
    private fun karte(benannt: Boolean): KartenDaten {
        val quelle = konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), VARIABLE_STRUKTUR_WINKEL).let { it.copy(parameter=it.parameter+mapOf("name" to "w", VARIABLE_DIMENSION to "1")) }
        val methode = port("methode",0)
        val aufruf = KnotenDaten(art=METHODEN_AUFRUF_ART, name="Historischer Aufruf", anschlüsse=listOf(methode,port("zweiter",2),port("erster",1)), parameter=if(benannt) mapOf("${METHODEN_AUFRUF_PARAMETER_PREFIX}0.name" to "w_1", "${METHODEN_AUFRUF_PARAMETER_PREFIX}1.name" to "w_2") else emptyMap())
        return KartenDaten(name="Alt",knoten=listOf(quelle,aufruf),verbindungen=listOf(VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang }.id),zu=AnschlussVerweis(aufruf.id,methode.id))))
    }
    @Test fun `benannte historische Komponenten werden ohne Anschlussaenderung gepackt`() {
        val alt=karte(true); val neu=alt.migriereWinkelraumAufrufe()
        assertEquals(alt.verbindungen,neu.verbindungen)
        val call=neu.knoten.last(); assertEquals(alt.knoten.last().anschlüsse,call.anschlüsse)
        assertTrue(METHODEN_STRUKTUR_PACKUNG in call.eigenschaften)
        assertEquals(neu,neu.migriereWinkelraumAufrufe())
        val w=TypisiertesElement("w",MathematikAnschlussArten.Tupel.id.wert)
        val m=MathematischeMethode("F",listOf(w),Tupel(listOf(SymbolischeZahlKomponente(w,RationaleZahl.Eins),SymbolischeZahlKomponente(w,RationaleZahl.von(2)))),Tupelraum(listOf(ReelleZahlen,ReelleZahlen)),mapOf("w" to winkelraum(1)), ausgabeNamen=listOf("sign","winkel"))
        val result=GesamterMathematikAuswerter.erzeugeRegister().finde(METHODEN_AUFRUF_ART)!!.auswerten(KnotenAuswertungsKontext(call,mapOf("methode" to BedingterWert(m),"erster" to BedingterWert(RationaleZahl.von(-1)),"zweiter" to BedingterWert(GradWinkel(RationaleZahl.von(90)))),RechenKontext()))
        assertNull(result.fehler,result.fehler)
        assertTrue(result.ausgaben.isNotEmpty())
    }
    @Test fun `uneindeutige historische Komponenten bleiben erhalten und erhalten Diagnose`() {
        val alt=karte(false);val neu=alt.migriereWinkelraumAufrufe()
        assertEquals(alt.verbindungen,neu.verbindungen)
        assertEquals(alt.knoten.last().anschlüsse,neu.knoten.last().anschlüsse)
        assertTrue(METHODEN_STRUKTUR_MIGRATIONSFEHLER in neu.knoten.last().parameter)
        assertFalse(METHODEN_STRUKTUR_PACKUNG in neu.knoten.last().eigenschaften)
    }
}
