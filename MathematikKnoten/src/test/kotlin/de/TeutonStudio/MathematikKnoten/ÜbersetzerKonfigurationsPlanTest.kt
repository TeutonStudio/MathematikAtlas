package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class ÜbersetzerKonfigurationsPlanTest {
    @Test fun `Zielwechsel zeigt Ausgangsverlust vorab und Undo stellt ganze Karte wieder her`() {
        val laufzeit=MathematikKartenLaufzeit()
        val quelle=konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter+mapOf("standardwert.a" to "2","standardwert.b" to "3")) },TUPEL_EINZEL_EINGABEN)
        val übersetzer=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val verbraucher=MathematikKnotenVorlagen.Skalarprodukt.erzeuge(GraphPunkt.Zero)
        fun port(k: KnotenDaten,name: String)=AnschlussVerweis(k.id,k.anschlüsse.single { it.name==name }.id)
        val eingang=VerbindungDaten(von=port(quelle,"tupel"),zu=port(übersetzer,"wert"))
        val ausgang=VerbindungDaten(von=port(übersetzer,"ergebnis"),zu=port(verbraucher,"a"))
        val bisher=KartenDaten(name="Verlust",knoten=listOf(quelle,übersetzer,verbraucher),verbindungen=listOf(eingang,ausgang))
        val kandidat=konfiguriereÜbersetzer(übersetzer,"zeile")
        val plan=planeKonfiguration(bisher,kandidat,laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(ausgang.id),plan.auswirkung.entfallendeVerbindungen.map { it.id })
        assertEquals(listOf(eingang),plan.karte.verbindungen)
        assertEquals(übersetzer.anschlüsse.map { it.id },kandidat.anschlüsse.map { it.id })
        assertEquals(2,bisher.verbindungen.size)
        val editor=KartenEditorZustand(bisher,laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig();assertEquals(bisher,editor.karte)
        editor.wiederholen();assertEquals(plan.karte.knoten,editor.karte.knoten);assertEquals(plan.karte.verbindungen,editor.karte.verbindungen)
    }
}
