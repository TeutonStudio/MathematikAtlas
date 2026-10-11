package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class AussageStandardwerteTest {
    private val art = AnschlussArtId("mathematik.aussage")
    private fun ziel(parameter: Map<String, String> = emptyMap()) = KnotenDaten(
        art = "test.durchlauf", name = "Ziel", parameter = parameter,
        anschlüsse = listOf(
            AnschlussDaten(name = "a", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
        ),
    )
    private fun quelle() = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(
        AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
    ))
    private fun register(fehler: Boolean = false) = MathematikAuswerterRegister().apply {
        registriere("test.durchlauf") { KnotenAuswertungsErgebnis(mapOf("wert" to it.eingänge.getValue("a"))) }
        registriere("test.quelle") {
            if (fehler) KnotenAuswertungsErgebnis(emptyMap(), fehler = "Quellfehler")
            else KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(WahrheitsKonstante(true))))
        }
    }
    private fun verbinde(a: KnotenDaten, b: KnotenDaten) = VerbindungDaten(
        von = AnschlussVerweis(a.id, a.anschlüsse.single().id),
        zu = AnschlussVerweis(b.id, b.anschlüsse.first().id),
    )
    @Test fun `fehlender konkreter Aussageeingang ist Falsch und gespeichert schaltbar`() {
        listOf(false, true).forEach { wert ->
            val z = ziel(if (wert) mapOf("standardwert.a" to "true") else emptyMap())
            val ergebnis = KartenAuswerter(register()).auswerten(KartenDaten(name = "Test", knoten = listOf(z)))
            assertEquals(WahrheitsKonstante(wert), ergebnis.knoten.getValue(z.id).ausgaben.getValue("wert").objekt)
        }
    }
    @Test fun `verbundener Wert hat ausschliesslichen Vorrang`() {
        val q = quelle(); val z = ziel(mapOf("standardwert.a" to "false"))
        val karte = KartenDaten(name = "Test", knoten = listOf(q, z), verbindungen = listOf(verbinde(q, z)))
        val ergebnis = KartenAuswerter(register()).auswerten(karte)
        assertEquals(WahrheitsKonstante(true), ergebnis.knoten.getValue(z.id).ausgaben.getValue("wert").objekt)
    }
    @Test fun `Quellfehler aktiviert keinen gespeicherten Standardwert`() {
        val q = quelle(); val z = ziel(mapOf("standardwert.a" to "true"))
        val karte = KartenDaten(name = "Test", knoten = listOf(q, z), verbindungen = listOf(verbinde(q, z)))
        val ergebnis = KartenAuswerter(register(true)).auswerten(karte).knoten.getValue(z.id)
        assertTrue(ergebnis.ausgaben.isEmpty())
        assertContains(checkNotNull(ergebnis.fehler), "Quellfehler")
    }
    @Test fun `fehlende Quellanschluesse sind Fehler statt Standardwerte`() {
        val q = quelle(); val z = ziel()
        val verbindung = verbinde(q, z).copy(von = AnschlussVerweis(q.id, AnschlussId("fehlt")))
        val ergebnis = KartenAuswerter(register()).auswerten(KartenDaten(name = "Test", knoten = listOf(q, z), verbindungen = listOf(verbindung)))
        assertTrue(ergebnis.knoten.getValue(z.id).ausgaben.isEmpty())
        assertNotNull(ergebnis.knoten.getValue(z.id).fehler)
    }
}
