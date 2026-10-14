package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VariablenGrundraumTest {
    @Test fun `verbundene Menge ersetzt Inspectorintervall und propagiert Quellenbereich`() {
        val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {
            it.copy(parameter = it.parameter + (VARIABLE_BEREICH_MODUS to VARIABLE_BEREICH_INTERVALL))
        }
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(3)))
        val wert = GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(
            KnotenAuswertungsKontext(k, mapOf("grundraum" to BedingterWert(menge)), RechenKontext())
        ).ausgaben.getValue("wert")
        assertEquals(menge, wert.werteVorrat)
        assertEquals(menge, wert.variablenQuellen.single().werteVorrat)
    }

    @Test fun `Migration erhaelt Ausgang und ist idempotent auch nach JSON Roundtrip`() {
        val k = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {
            it.copy(anschlüsse = it.anschlüsse.filter { p -> p.richtung == AnschlussRichtung.Ausgang })
        }
        val karte = KartenDaten(name = "Test", knoten = listOf(k))
        val neu = karte.migriereVariablenGrundraum()
        assertEquals(k.anschlüsse.single(), neu.knoten.single().anschlüsse.first())
        assertEquals(neu, neu.migriereVariablenGrundraum())
        assertEquals(neu, MathematikKartenCodec.lese(MathematikKartenCodec.schreibe(neu)))
    }
    @Test fun `Tupelstandardwerte folgen stabilen Anschluessen bei Umordnung und Undo`() {
        val k = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero).let {
            it.copy(parameter = it.parameter + mapOf("standardwert.a" to "2", "standardwert.b" to "1/2"))
        }
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(KartenDaten(name = "Tupel", knoten = listOf(k)), de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister(MathematikAnschlussArten.alle)))
        val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())
        fun elemente() = (auswerter.auswerten(editor.karte).knoten.getValue(k.id).ausgaben.getValue("tupel").objekt as Tupel).elemente
        assertEquals(listOf(RationaleZahl.von(2), RationaleZahl.parse("1/2")), elemente())
        val ids = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.id }.reversed()
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.ordneDynamischeOperanden(editor.karte, k.id, ids)!!)
        assertEquals(listOf(RationaleZahl.parse("1/2"), RationaleZahl.von(2)), elemente())
        editor.setzeTupelEingangAnzahl(k.id, 3)
        assertEquals(ids, editor.karte.knoten.single().anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.take(2).map { it.id })
        assertEquals(listOf(RationaleZahl.parse("1/2"), RationaleZahl.von(2)), elemente().take(2))
        editor.rückgängig()
        editor.rückgängig()
        assertEquals(listOf(RationaleZahl.von(2), RationaleZahl.parse("1/2")), elemente())
    }
}
