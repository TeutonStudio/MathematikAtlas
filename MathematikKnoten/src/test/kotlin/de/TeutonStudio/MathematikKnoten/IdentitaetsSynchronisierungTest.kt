package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class IdentitaetsSynchronisierungTest {
    @Test fun `allgemeine Identitaet erhaelt Ganzzahl und projizierte Tupelvertraege`() {
        val ganz = TypAusdruck.Atom(MathematischeTypen.Ganz)
        for (argumentTupel in listOf(false, true)) for (ergebnisTupel in listOf(false, true)) {
            val sourcePort = AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Objekt.id,
                vertrag = AnschlussVertrag(if (argumentTupel) TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(ganz)) else ganz))
            val source = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(sourcePort))
            val argument = AnschlussDaten(name = "argument-0", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.AtlasWert.id, semantischeId = IdentitaetsMethode.ARGUMENT)
            val aufruf = KnotenDaten(art = METHODEN_AUFRUF_ART, name = "Aufruf", anschlüsse = listOf(
                AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Methode.id), argument,
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.AtlasWert.id)),
                parameter = mapOf(METHODEN_AUFRUF_ARGUMENTPROJEKTION to if (argumentTupel) METHODEN_ARGUMENTPROJEKTION_TUPEL else METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
                    METHODEN_AUFRUF_ERGEBNISPROJEKTION to if (ergebnisTupel) METHODEN_ERGEBNISPROJEKTION_TUPEL else METHODEN_ERGEBNISPROJEKTION_DIREKT))
            // Projection preference is normally supplied by a connected method source.
            val methodPort = AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Methode.id)
            val methodSource = KnotenDaten(art = "test.methode", name = "Identität", anschlüsse = listOf(methodPort), parameter = mapOf(
                methodenAusgangArgumentprojektionSchlüssel("methode") to if (argumentTupel) METHODEN_ARGUMENTPROJEKTION_TUPEL else METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
                methodenAusgangErgebnisprojektionSchlüssel("methode") to if (ergebnisTupel) METHODEN_ERGEBNISPROJEKTION_TUPEL else METHODEN_ERGEBNISPROJEKTION_DIREKT))
            val karte = KartenDaten(name = "Test", knoten = listOf(source, methodSource, aufruf), verbindungen = listOf(
                VerbindungDaten(von = AnschlussVerweis(source.id, sourcePort.id), zu = AnschlussVerweis(aufruf.id, argument.id)),
                VerbindungDaten(von = AnschlussVerweis(methodSource.id, methodPort.id), zu = AnschlussVerweis(aufruf.id, aufruf.anschlüsse.first().id))))
            val wert = BedingterWert(if (ergebnisTupel) Tupel(listOf(RationaleZahl.Eins)) else Variable("n"), werteVorrat = if (!argumentTupel && !ergebnisTupel) GanzeZahlen else null)
            val auswertung = KartenAuswertungsErgebnis(mapOf(aufruf.id to KnotenAuswertungsErgebnis(mapOf("wert" to wert), eingänge = mapOf("methode" to BedingterWert(IdentitaetsMethode)))), emptyList())
            val synchron = synchronisiereMethodenAufrufe(karte, auswertung, MathematikKartenLaufzeit().graphPrüfung)
            val ausgang = synchron.knoten.single { it.id == aufruf.id }.anschlüsse.single { it.name == "wert" }
            assertEquals(if (ergebnisTupel) TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(ganz)) else ganz, ausgang.vertrag.typ,
                "Argumenttupel=$argumentTupel, Ergebnistupel=$ergebnisTupel")
        }
    }
}
