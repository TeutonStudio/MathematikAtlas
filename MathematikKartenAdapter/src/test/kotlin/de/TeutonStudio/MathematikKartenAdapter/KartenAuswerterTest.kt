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

    @Test fun `gezielte Cache Invalidierung berechnet nur den gewählten Knoten neu`() {
        val zahlArt = AnschlussArtId("zahl")
        fun quelle(art: String, name: String) = KnotenDaten(
            art = art,
            name = name,
            anschlüsse = listOf(
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = zahlArt),
            ),
        )
        fun durchlauf(art: String, name: String) = KnotenDaten(
            art = art,
            name = name,
            anschlüsse = listOf(
                AnschlussDaten(name = "eingang", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahlArt),
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = zahlArt),
            ),
        )

        val a = quelle("test.a", "A")
        val b = durchlauf("test.b", "B")
        val c = durchlauf("test.c", "C")
        val aufrufe = mutableMapOf("test.a" to 0, "test.b" to 0, "test.c" to 0)
        val register = MathematikAuswerterRegister().apply {
            registriere("test.a") {
                aufrufe["test.a"] = aufrufe.getValue("test.a") + 1
                KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.von(7))))
            }
            listOf("test.b", "test.c").forEach { art ->
                registriere(art) { kontext ->
                    aufrufe[art] = aufrufe.getValue(art) + 1
                    KnotenAuswertungsErgebnis(mapOf("wert" to kontext.eingänge.getValue("eingang")))
                }
            }
        }
        val karte = KartenDaten(
            name = "Cache-Test",
            knoten = listOf(a, b, c),
            verbindungen = listOf(
                verbinde(a, "wert", b, "eingang"),
                verbinde(b, "wert", c, "eingang"),
            ),
        )
        var jetzt = 0L
        var schritt = 10L
        val auswerter = KartenAuswerter(
            register = register,
            nanoZeit = { jetzt.also { jetzt += schritt } },
        )

        val zuerst = auswerter.auswerten(karte)
        assertEquals(mapOf("test.a" to 1, "test.b" to 1, "test.c" to 1), aufrufe)
        assertEquals(10L, zuerst.knoten.getValue(a.id).auswertungsDauerNanos)
        assertEquals(10L, zuerst.knoten.getValue(b.id).auswertungsDauerNanos)
        assertEquals(10L, zuerst.knoten.getValue(c.id).auswertungsDauerNanos)

        val nurCache = auswerter.auswerten(karte)
        assertEquals(mapOf("test.a" to 1, "test.b" to 1, "test.c" to 1), aufrufe)
        assertEquals(10L, nurCache.knoten.getValue(b.id).auswertungsDauerNanos)

        schritt = 25L
        auswerter.verwerfeCache(b.id)
        val erneut = auswerter.auswerten(karte)

        assertEquals(1, aufrufe.getValue("test.a"), "Vorgelagerte Knoten müssen im Cache bleiben.")
        assertEquals(2, aufrufe.getValue("test.b"), "Der gewählte Knoten muss neu berechnet werden.")
        assertEquals(1, aufrufe.getValue("test.c"), "Ein unverändertes Ergebnis darf nachgelagerte Knoten im Cache lassen.")
        assertEquals(10L, erneut.knoten.getValue(a.id).auswertungsDauerNanos)
        assertEquals(25L, erneut.knoten.getValue(b.id).auswertungsDauerNanos)
        assertEquals(10L, erneut.knoten.getValue(c.id).auswertungsDauerNanos)
    }

    @Test fun `Fehlerauswertung erhält ebenfalls eine Dauer`() {
        var jetzt = 0L
        val knoten = KnotenDaten(art = "test.fehler", name = "Fehler")
        val register = MathematikAuswerterRegister().apply {
            registriere("test.fehler") { error("Absichtlicher Testfehler") }
        }
        val ergebnis = KartenAuswerter(
            register = register,
            nanoZeit = { jetzt.also { jetzt += 17L } },
        ).auswerten(KartenDaten(name = "Test", knoten = listOf(knoten)))

        assertEquals(17L, ergebnis.knoten.getValue(knoten.id).auswertungsDauerNanos)
        assertTrue(ergebnis.knoten.getValue(knoten.id).fehler.orEmpty().contains("Absichtlicher Testfehler"))
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
