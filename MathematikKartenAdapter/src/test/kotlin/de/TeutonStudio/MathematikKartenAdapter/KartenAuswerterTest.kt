package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KartenAuswerterTest {
    @Test fun einfacheAdditionWirdTopologischAusgewertet() {
        val zahlArt = AnschlussArtId("zahl")
        fun zahl(wert: String) = KnotenDaten(
            art = "zahl", name = wert, parameter = mapOf("wert" to wert),
            anschlüsse = listOf(AnschlussDaten(name="wert", richtung=AnschlussRichtung.Ausgang, kante=AnschlussKante.Rechts, art=zahlArt)),
        )
        val a = zahl("2")
        val b = zahl("3")
        val plus = KnotenDaten(
            art="plus", name="Plus", anschlüsse=listOf(
                AnschlussDaten(name="a", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=zahlArt, reihenfolge=0),
                AnschlussDaten(name="b", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=zahlArt, reihenfolge=1),
                AnschlussDaten(name="wert", richtung=AnschlussRichtung.Ausgang, kante=AnschlussKante.Rechts, art=zahlArt),
            )
        )
        val register = MathematikAuswerterRegister().apply {
            registriere("zahl") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.parse(k.knoten.parameter.getValue("wert"))))) }
            registriere("plus") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(addition(k.eingänge.getValue("a").objekt as ZahlAusdruck, k.eingänge.getValue("b").objekt as ZahlAusdruck)))) }
        }
        val karte = KartenDaten(name="Test", knoten=listOf(a,b,plus), verbindungen=listOf(verbinde(a,"wert",plus,"a"),verbinde(b,"wert",plus,"b")))
        val ergebnis = KartenAuswerter(register).auswerten(karte)
        assertTrue(ergebnis.fehler.isEmpty())
        assertEquals(RationaleZahl.von(5), ergebnis.knoten.getValue(plus.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `nicht auswertbare Knotenarten bleiben fehlerfrei`() {
        val notiz = KnotenDaten(art = "karte.notiz", name = "Notiz")
        val ergebnis = KartenAuswerter(
            register = MathematikAuswerterRegister(),
            nichtAuswertbareKnotenArten = setOf("karte.notiz"),
        ).auswerten(KartenDaten(name = "Test", knoten = listOf(notiz)))

        assertTrue(ergebnis.fehler.isEmpty())
        assertTrue(ergebnis.knoten.getValue(notiz.id).ausgaben.isEmpty())
    }

    @Test fun `unbekannte auswertbare Knotenart bleibt Fehler`() {
        val unbekannt = KnotenDaten(art = "test.unbekannt", name = "Unbekannt")
        val ergebnis = KartenAuswerter(MathematikAuswerterRegister())
            .auswerten(KartenDaten(name = "Test", knoten = listOf(unbekannt)))

        assertEquals(1, ergebnis.fehler.size)
        assertTrue(ergebnis.fehler.single().contains("Kein Auswerter für test.unbekannt registriert."))
    }

    private fun verbinde(von: KnotenDaten, a: String, zu: KnotenDaten, b: String) = VerbindungDaten(
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == a }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == b }.id),
    )
}