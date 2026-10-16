package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class AnschlussInfoLaufzeitTest {
    private val laufzeit = MathematikKartenLaufzeit()
    @Test fun infoVerwendetRegistrierteBeschreibungenOhneAuswertung() {
        val knoten = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(0f, 0f))
        val port = knoten.anschlüsse.single()
        val karte = KartenDaten(name = "Info", knoten = listOf(knoten))
        val info = assertNotNull(laufzeit.anschlussInfoFür(karte, AnschlussVerweis(knoten.id, port.id)))
        assertEquals(MathematikAnschlussArten.Zahl.name, info.arten.single().name)
        assertEquals(MathematikAnschlussArten.Zahl.beschreibung, info.arten.single().beschreibung)
        assertNull(info.eingabeHinweis)
        assertTrue(info.verbindungen.isEmpty())
    }
    @Test fun infoZeigtEffektivenTypEinesFolgendenAusgangs() {
        val zahl = AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Zahl.id, vertrag = AnschlussVertrag(TypAusdruck.Atom(TypId("mathematik.zahl"))))
        val eingang = AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id)
        val ausgang = eingang.copy(id = neueAnschlussId(), name = "out", richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts, artFolgtEingang = "in", typInferenz = TypInferenzRegel.FolgtEingang("in"),
            vertrag = AnschlussVertrag(TypAusdruck.Beliebig))
        val q = KnotenDaten(art = "test", name = "Quelle", anschlüsse = listOf(zahl))
        val z = KnotenDaten(art = "test", name = "Weitergabe", anschlüsse = listOf(eingang, ausgang))
        val v = VerbindungDaten(von = AnschlussVerweis(q.id, zahl.id), zu = AnschlussVerweis(z.id, eingang.id))
        val info = laufzeit.anschlussInfoFür(KartenDaten(name = "Info", knoten = listOf(q, z), verbindungen = listOf(v)), AnschlussVerweis(z.id, ausgang.id))!!
        assertEquals(MathematikAnschlussArten.Zahl.name, info.typ)
        assertEquals(MathematikAnschlussArten.Zahl.name, info.arten.single().name)
    }
    @Test fun entfernterAnschlussHatKeineInfo() {
        assertNull(laufzeit.anschlussInfoFür(KartenDaten(name = "Leer"), AnschlussVerweis(KnotenId("fehlt"), AnschlussId("fehlt"))))
    }
}
