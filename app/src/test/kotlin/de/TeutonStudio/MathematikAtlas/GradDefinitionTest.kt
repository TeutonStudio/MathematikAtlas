package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GradDefinitionTest {
    @Test fun `Sinusdefinition ist azyklisch ausfuehrbar und rundreisebestaendig`() {
        val karte = sinusReellDefinitionsKarte()
        assertEquals(karte, sinusReellDefinitionsKarte())
        assertEquals(karte, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)))
        assertEquals(1, karte.knoten.count { it.art == ZAHLENRECHNER_ART })
        assertEquals("grad", karte.knoten.single { it.art == ZAHLENRECHNER_ART }.parameter[SINUS_WINKELRAUM])
        val graph = MathematikKartenLaufzeit().graphPrüfung
        karte.verbindungen.forEach { v -> assertIs<de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung.Erlaubt>(
            graph.prüfe(karte.copy(verbindungen = emptyList()), v.von, v.zu)) }
        val laufzeit = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())
        val eingang = karte.knoten.first()
        val ausgang = karte.knoten.last()
        val konkret = laufzeit.auswerten(karte, mapOf(eingang.id to mapOf("wert" to BedingterWert(Division(Pi, RationaleZahl.von(2))))))
        assertTrue(konkret.fehler.isEmpty(), konkret.fehler.toString())
        val wert = konkret.knoten.getValue(ausgang.id).ausgaben.getValue("wert").objekt as ZahlAusdruck
        val numerisch = ((DomaenenAuswerter.wert(wert) as DomaenenErgebnis.Wert).wert as DomaenenWert.Reell).wert.toDouble()
        assertEquals(1.0, numerisch, 1e-14)
        val symbolisch = laufzeit.werteKonzeptKarteAus(karte)
        assertTrue(symbolisch.fehler.isEmpty(), symbolisch.fehler.toString())
    }
}
