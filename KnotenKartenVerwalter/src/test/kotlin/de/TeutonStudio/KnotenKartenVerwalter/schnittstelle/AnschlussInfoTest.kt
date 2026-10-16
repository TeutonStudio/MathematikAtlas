package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.TypSystem.*
import org.junit.Test
import kotlin.test.*

class AnschlussInfoTest {
    private val art = AnschlussArtId("test")
    private val out = AnschlussDaten(name = "Wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)
    private val eingang = out.copy(id = neueAnschlussId(), richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        belegung = AnschlussBelegung.Mehrfach)
    private val q = KnotenDaten(art = "test", name = "Quelle", anschlüsse = listOf(out))
    private val z = KnotenDaten(art = "test", name = "Ziel", anschlüsse = listOf(eingang))
    private val v = VerbindungDaten(von = AnschlussVerweis(q.id, out.id), zu = AnschlussVerweis(z.id, eingang.id))
    private val karte = KartenDaten(name = "Info", knoten = listOf(q, z), verbindungen = listOf(v))

    @Test fun beschreibungenUndVerbindungenStammenAusDemModell() {
        val register = AnschlussArtRegister(listOf(AnschlussArt(art, "Testtyp", beschreibung = "Eine Testbeschreibung.")))
        val info = assertNotNull(anschlussBasisInfo(karte, v.zu, register))
        assertEquals("Testtyp", info.arten.single().name)
        assertEquals("Eine Testbeschreibung.", info.arten.single().beschreibung)
        assertEquals(AnschlussBelegung.Mehrfach, info.belegung)
        assertEquals("Quelle: Quelle · Wert", info.verbindungen.single().beschreibung)
        assertEquals(v.id, info.verbindungen.single().id)
        assertNull(info.eingabeHinweis)
        assertEquals("Ziel: Ziel · Wert", anschlussBasisInfo(karte, v.von)!!.verbindungen.single().beschreibung)
    }
    @Test fun unbekannterTypLeereVerbindungenUndEntfernterAnschluss() {
        val info = assertNotNull(anschlussBasisInfo(karte.copy(verbindungen = emptyList()), v.zu))
        assertTrue(info.verbindungen.isEmpty())
        assertEquals("Noch nicht bestimmt", info.typ)
        assertTrue(info.arten.single().beschreibung.isNotBlank())
        assertNull(anschlussBasisInfo(karte.copy(knoten = listOf(q)), v.zu))
    }
    @Test fun strukturierteTypenBehaltenParameterUndAlternativen() {
        val typ = TypAusdruck.Parameterisiert(TypId("Container"), listOf(
            TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(TypId("A")), TypAusdruck.Atom(TypId("B")))),
            TypAusdruck.Literal("3"), TypAusdruck.Variable(TypVariablenId("T"))))
        assertEquals("Container<A oder B, 3, Typvariable T>", anschlussTypText(typ) { it })
        assertEquals("Beliebiger Typ", anschlussTypText(TypAusdruck.Beliebig) { it })
    }
}
