package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class EndlicheMengeKonfigurationsPlanTest {
    @Test fun `Graphanzahl und Lokalmodus melden konkrete Verluste und bilden jeweils einen UndoSchritt`() {
        val laufzeit=MathematikKartenLaufzeit()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val g=konfiguriereEndlicheMenge(MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero),ENDLICHE_MENGE_GRAPH,3).let {
            it.copy(parameter=it.parameter+("standardwert.element2" to "2"),eingangsKartenVerweise=mapOf("element2" to KartenVerweis(KartenId("extern"),7))) }
        val kanten=listOf("element1","element3").map {name->VerbindungDaten(
            von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(g.id,g.anschlüsse.single {it.name==name}.id))}
        val alt=KartenDaten(name="Graphmenge",knoten=listOf(q,g),verbindungen=kanten)
        val plan=planeKonfiguration(alt,konfiguriereEndlicheMenge(g,ENDLICHE_MENGE_GRAPH,1),laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(kanten.last()),plan.auswirkung.entfallendeVerbindungen)
        assertEquals(2,plan.auswirkung.entfallendeLokaleWerte.size)
        assertTrue(plan.auswirkung.entfallendeLokaleWerte.any {it.contains("Standardwert element2")})
        assertTrue(plan.auswirkung.entfallendeLokaleWerte.any {it.contains("Kartenfallback element2") && it.contains("extern")})
        val g1=plan.karte.knoten.single {it.id==g.id}
        assertEquals(g.anschlüsse.single {it.name=="element1"}.id,g1.anschlüsse.single {it.name=="element1"}.id)
        assertTrue(g1.eingangsKartenVerweise.isEmpty());assertFalse("standardwert.element2" in g1.parameter)
        assertEquals(g.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER],g1.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER])
        val editor=KartenEditorZustand(alt,laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte));editor.rückgängig();assertEquals(alt,editor.karte)
        editor.wiederholen();assertEquals(plan.karte.knoten,editor.karte.knoten)
        val lokal=planeKonfiguration(editor.karte,konfiguriereEndlicheMenge(g1,ENDLICHE_MENGE_LOKAL),laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(kanten.first()),lokal.auswirkung.entfallendeVerbindungen)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(lokal.karte));editor.rückgängig();assertEquals(plan.karte.knoten,editor.karte.knoten)
        editor.rückgängig();assertEquals(alt,editor.karte)
    }
}
