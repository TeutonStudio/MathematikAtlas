package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KonfigurationsPlanTest {
    @Test fun `abhaengiger Zerlegerverlust wird vorab angezeigt und gemeinsam rueckgaengig`() {
        val laufzeit = MathematikKartenLaufzeit()
        fun bereich(n: Int) = mapOf(WertebereichKonfiguration.EIGENSCHAFT to WertebereichKonfiguration.Vektor(dimension = n).zuEigenschaft())
        val quelle = MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).copy(eigenschaften = bereich(3))
        val zerleger = konfiguriereVektorRechner(vektorRechnerVorlage(VektorRechnerOperator.ZERLEGEN).erzeuge(GraphPunkt.Zero), VektorRechnerOperator.ZERLEGEN)
        val eingang = zerleger.anschlüsse.first { it.name == "struktur" }
        val basis = KartenDaten(name = "Test", knoten = listOf(quelle, zerleger), verbindungen = listOf(VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id), zu = AnschlussVerweis(zerleger.id, eingang.id))))
        val synchron = planeKonfiguration(basis, quelle, laufzeit.graphPrüfung, laufzeit::auswerten).karte
        val dritter = synchron.knoten.first { it.id == zerleger.id }.anschlüsse.single { it.name == "element3" }
        val verbraucher = KnotenDaten(art = "test.ziel", name = "Verbraucher", anschlüsse = listOf(AnschlussDaten(
            name = "zahl", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)))
        val edge = VerbindungDaten(von = AnschlussVerweis(zerleger.id, dritter.id), zu = AnschlussVerweis(verbraucher.id, verbraucher.anschlüsse.single().id))
        val verbunden = synchron.copy(knoten = synchron.knoten + verbraucher, verbindungen = synchron.verbindungen + edge)
        val plan = planeKonfiguration(verbunden, quelle.copy(eigenschaften = bereich(2)), laufzeit.graphPrüfung, laufzeit::auswerten)
        assertTrue(edge in plan.auswirkung.entfallendeVerbindungen)
        assertFalse(plan.karte.verbindungen.any { it.id == edge.id })
        assertEquals(2, plan.karte.knoten.first { it.id == zerleger.id }.anschlüsse.count { it.richtung == AnschlussRichtung.Ausgang })
        assertTrue(verbunden.verbindungen.any { it.id == edge.id })
        val editor = KartenEditorZustand(verbunden, laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig()
        assertEquals(verbunden, editor.karte)
        editor.wiederholen()
        assertEquals(plan.karte.knoten, editor.karte.knoten)
        assertEquals(plan.karte.verbindungen, editor.karte.verbindungen)
    }
    @Test fun `Addition zu Norm meldet nur zweiten Eingangsverlust und ist atomar rückgängig`() {
        val laufzeit = MathematikKartenLaufzeit()
        val quellen = List(2) { MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).copy(
            eigenschaften = mapOf(WertebereichKonfiguration.EIGENSCHAFT to WertebereichKonfiguration.Vektor(dimension = 3).zuEigenschaft())) }
        val addition = vektorRechnerVorlage(VektorRechnerOperator.ADDITION).erzeuge(GraphPunkt.Zero)
        val edges = quellen.mapIndexed { i, q -> VerbindungDaten(
            von = AnschlussVerweis(q.id, q.anschlüsse.single().id),
            zu = AnschlussVerweis(addition.id, addition.anschlüsse.single { it.name == "vektor.${i + 1}" }.id)) }
        val karte = KartenDaten(name = "Test", knoten = quellen + addition, verbindungen = edges)
        val norm = konfiguriereVektorRechner(addition, VektorRechnerOperator.NORM)
        val plan = planeKonfiguration(karte, norm, laufzeit.graphPrüfung, laufzeit::auswerten)
        assertEquals(listOf(edges[1]), plan.auswirkung.entfallendeVerbindungen)
        assertEquals(listOf(edges[0]), plan.karte.verbindungen)
        val editor = KartenEditorZustand(karte, laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig()
        assertEquals(karte, editor.karte)
        editor.wiederholen()
        assertEquals(plan.karte.verbindungen, editor.karte.verbindungen)
    }

}
