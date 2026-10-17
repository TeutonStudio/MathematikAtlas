package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class PolynomKonfigurationsPlanTest {
    @Test fun `intrinsisches Argument meldet Verbindung Standardwert und festen Kartenfallback vor einem UndoSchritt`() {
        val laufzeit=MathematikKartenLaufzeit()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val p=konfigurierePolynomArgument(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),POLYNOM_ARGUMENT_EINGANG).let {
            it.copy(name="Mein Polynom",parameter=it.parameter+("standardwert.argument" to "3"),
                eingangsKartenVerweise=mapOf("argument@karte:extern" to KartenVerweis(KartenId("extern"),7))) }
        val kante=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(p.id,p.anschlüsse.single {it.name=="argument"}.id))
        val alt=KartenDaten(name="Polynom",knoten=listOf(q,p),verbindungen=listOf(kante))
        val kandidat=konfigurierePolynomArgument(p,POLYNOM_ARGUMENT_VARIABLE,"sin")
        assertEquals(p.name,kandidat.name)
        val plan=planeKonfiguration(alt,kandidat,laufzeit.graphPrüfung,laufzeit::auswerten)
        assertEquals(listOf(kante),plan.auswirkung.entfallendeVerbindungen)
        assertEquals(2,plan.auswirkung.entfallendeLokaleWerte.size)
        val neu=plan.karte.knoten.single {it.id==p.id}
        assertFalse("standardwert.argument" in neu.parameter);assertTrue(neu.eingangsKartenVerweise.isEmpty())
        assertEquals(p.anschlüsse.single {it.name=="koeffizienten"}.id,neu.anschlüsse.single {it.name=="koeffizienten"}.id)
        val editor=KartenEditorZustand(alt,laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte));editor.rückgängig();assertEquals(alt,editor.karte)
        editor.wiederholen();assertEquals(plan.karte.knoten,editor.karte.knoten)
    }
}
