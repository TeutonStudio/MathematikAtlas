package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GradKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun aus(k: KnotenDaten, wert: BedingterWert, name: String = "wert") = register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, mapOf(name to wert), RechenKontext()))
    @Test fun `Uebersetzer und Sinus nutzen echte Winkel und erhalten IDs`() {
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val grad = konfiguriereÜbersetzer(basis, "grad")
        val w = aus(grad, BedingterWert(Division(Pi, RationaleZahl.von(2)))).ausgaben.getValue("ergebnis")
        assertEquals(GradWinkel(RationaleZahl.von(90)), w.objekt)
        assertEquals(Gradmenge, w.zielMenge)
        val sin = konfiguriereSinusWinkelraum(KnotenDaten(art = ZAHLENRECHNER_ART, name = "Sinus Grad"), true)
        assertEquals(MathematikAnschlussArten.Winkel.id, sin.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }.art)
        assertIs<SinusGrad>(aus(sin, w, "a").ausgaben.getValue("wert").objekt)
        assertFailsWith<IllegalStateException> { aus(sin, BedingterWert(RationaleZahl.Eins), "a") }
        val rad = konfiguriereÜbersetzerVertreter(konfiguriereÜbersetzer(grad, "bogenmass"), WinkelVertreter.SYMMETRISCH)
        assertEquals(basis.anschlüsse.map { it.id }, rad.anschlüsse.map { it.id })
        val karte = KartenDaten(name = "Grad", knoten = listOf(rad))
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(rad, geladen.knoten.single())
        assertEquals(WinkelVertreter.SYMMETRISCH, übersetzerVertreter(geladen.knoten.single()))
    }
    @Test fun `Vertreterwechsel ist atomar rueckgaengig und Winkelparameter persistiert`() {
        val laufzeit = MathematikKartenLaufzeit()
        val basis = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "bogenmass")
        val vorher = KartenDaten(name = "Winkel", knoten = listOf(basis))
        val neu = konfiguriereÜbersetzerVertreter(basis, WinkelVertreter.SYMMETRISCH)
        val plan = planeKonfiguration(vorher, neu, laufzeit.graphPrüfung, laufzeit::auswerten)
        val editor = KartenEditorZustand(vorher, laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig(); assertEquals(vorher, editor.karte)
        editor.wiederholen(); assertEquals(neu, editor.karte.knoten.single())
        val bereich = WertebereichKonfiguration.Winkel
        assertEquals(bereich, WertebereichKonfiguration.vonEigenschaft(bereich.zuEigenschaft()))
        assertEquals(Gradmenge, bereich.zuMenge())
    }
    @Test fun `Gradquelle ist im gemeinsamen Mengenknoten registriert`() {
        val knoten = MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.GRADMENGE).erzeuge(GraphPunkt.Zero)
        val result = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()))
        assertEquals(Gradmenge, result.ausgaben.getValue("menge").objekt)
        val sin = (RechnerOperatorRegistry.finde(RechnerFamilie.ZAHL, "zahl.sin") as RechnerOperatorAuflösung.Bekannt).vertrag
        assertEquals(MathematikAnschlussArten.Winkel.id, sin.signaturen.single { it.id == "grad" }.anschlüsse.first().art)
    }
    @Test fun `Strukturwechsel bewahrt gelieferte Obermengen und praezise Komponenten`() {
        val raum = Tupelraum(listOf(ReelleZahlen, RationaleZahlen))
        val wert = BedingterWert(Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))), zielMenge = raum, komponentenMengen = raum.komponenten)
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val zeile = aus(konfiguriereÜbersetzer(basis, "zeile"), wert).ausgaben.getValue("ergebnis")
        assertEquals(ReelleZahlen, (zeile.zielMenge as Vektorraum).skalarMenge)
        val tupel = aus(konfiguriereÜbersetzer(basis, "tupel"), zeile).ausgaben.getValue("ergebnis")
        assertEquals(raum, tupel.zielMenge)
        assertEquals(raum.komponenten, tupel.komponentenMengen)
    }
}
