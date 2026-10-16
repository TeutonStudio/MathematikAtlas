package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class IdentitaetsAufrufTest {
    private val aufruf = KnotenDaten(art = METHODEN_AUFRUF_ART, name = "Aufruf", anschlüsse = listOf(
        AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.methode")),
        AnschlussDaten(name = "argument-0", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.atlasWert"), semantischeId = IdentitaetsMethode.ARGUMENT),
        AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.atlasWert"))))
    @Test fun `allgemeiner Aufruf behaelt alle Metadaten und neutrale Methodenwerte`() {
        val neutral = object : Methode { override val name = "Neutral" }
        val wert = BedingterWert(neutral, zielMenge = ReelleZahlen, werteVorrat = GanzeZahlen, latexDarstellung = "g", symbolischeMethode = true, komponentenMengen = listOf(ReelleZahlen))
        val ergebnis = MethodenAufrufAuswerter.auswerten(KnotenAuswertungsKontext(aufruf,
            mapOf("methode" to BedingterWert(IdentitaetsMethode), "argument-0" to wert), RechenKontext()))
        assertEquals(wert, ergebnis.ausgaben.getValue("wert"))
        assertFails { MethodenAufrufAuswerter.auswerten(KnotenAuswertungsKontext(aufruf,
            mapOf("methode" to BedingterWert(IdentitaetsMethode), "argument-0" to wert.copy(annahmen = setOf(WahrheitsKonstante(false)))), RechenKontext())) }
    }
    @Test fun `nur explizit optionale Methodenports erhalten Identitaet`() {
        val register = MathematikAuswerterRegister().apply {
            registriere("test.optional") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to k.eingänge.getValue("methode"))) }
            registriereOptionaleMethodenEingänge("test.optional") { setOf("methode") }
            registriere("test.pflicht") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to k.eingänge.getValue("methode"))) }
        }
        val optional = aufruf.copy(art = "test.optional")
        val pflicht = aufruf.copy(id = KnotenId("pflicht"), art = "test.pflicht")
        val ergebnis = KartenAuswerter(register).auswerten(KartenDaten(name = "Test", knoten = listOf(optional, pflicht)))
        assertSame(IdentitaetsMethode, ergebnis.knoten.getValue(optional.id).ausgaben.getValue("wert").objekt)
        assertNotNull(ergebnis.knoten.getValue(pflicht.id).fehler)
    }
    @Test fun `optionale Identitaet respektiert explizite Methodensignatur`() {
        val zahl = de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl)
        val aussage = de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Aussage)
        fun typ(argumente: List<de.TeutonStudio.TypSystem.TypAusdruck>, ergebnisse: List<de.TeutonStudio.TypSystem.TypAusdruck>) =
            de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Methode, listOf(
                de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, argumente),
                de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, ergebnisse)))
        val register = MathematikAuswerterRegister().apply {
            registriere("test.optional") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to k.eingänge.getValue("methode"))) }
            registriereOptionaleMethodenEingänge("test.optional") { setOf("methode") }
        }
        for ((vertrag, gültig) in listOf(typ(listOf(zahl), listOf(zahl)) to true, typ(listOf(zahl, zahl), listOf(zahl)) to false, typ(listOf(zahl), listOf(aussage)) to false)) {
            val k = aufruf.copy(art = "test.optional", anschlüsse = aufruf.anschlüsse.map { if (it.name == "methode") it.copy(vertrag = de.TeutonStudio.TypSystem.AnschlussVertrag(vertrag)) else it })
            val ergebnis = KartenAuswerter(register).auswerten(KartenDaten(name = "Test", knoten = listOf(k))).knoten.getValue(k.id)
            if (gültig) {
                assertNull(ergebnis.fehler)
                val id = assertIs<TypisierteIdentitaetsMethode>(ergebnis.ausgaben.getValue("wert").objekt)
                assertEquals(zahl, id.signatur.argumente.single().typ)
            } else assertNotNull(ergebnis.fehler)
        }
    }
    @Test fun `fehlerhafte echte Quelle und fehlende Kartenquelle werden nicht durch Identitaet verdeckt`() {
        val register = MathematikAuswerterRegister().apply {
            registriere("test.optional") { k -> KnotenAuswertungsErgebnis(mapOf("wert" to k.eingänge.getValue("methode"))) }
            registriereOptionaleMethodenEingänge("test.optional") { setOf("methode") }
            registriere("test.fehler") { KnotenAuswertungsErgebnis(emptyMap(), fehler = "Quellfehler") }
        }
        val optional = aufruf.copy(art = "test.optional", eingangsKartenVerweise = mapOf("methode" to KartenVerweis(KartenId("fehlt"), 1)))
        val fehlend = KartenAuswerter(register).auswerten(KartenDaten(name = "Test", knoten = listOf(optional))).knoten.getValue(optional.id)
        assertTrue(fehlend.fehler.orEmpty().contains("Referenzierte Karte fehlt"))
        val source = KnotenDaten(art = "test.fehler", name = "Fehler", anschlüsse = listOf(AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.methode"))))
        val karte = KartenDaten(name = "Test", knoten = listOf(source, optional), verbindungen = listOf(VerbindungDaten(von = AnschlussVerweis(source.id, source.anschlüsse.single().id), zu = AnschlussVerweis(optional.id, optional.anschlüsse.first().id))))
        assertTrue(KartenAuswerter(register).auswerten(karte).knoten.getValue(optional.id).fehler.orEmpty().contains("Quellfehler"))
    }
    @Test fun `echte eingeschraenkte Identitaet behaelt Metadaten aber Ableitung nicht`() {
        val id = identitaetAuf(ReelleZahlen)
        val argument = BedingterWert(RationaleZahl.von(7), werteVorrat = GanzeZahlen, latexDarstellung = "sieben", elementArt = AnschlussArtId("mathematik.zahl"), komponentenMengen = listOf(GanzeZahlen))
        fun rufe(m: Methode) = MethodenAufrufAuswerter.auswerten(KnotenAuswertungsKontext(aufruf, mapOf("methode" to BedingterWert(m), "argument-0" to argument), RechenKontext())).ausgaben.getValue("wert")
        val direkt = rufe(id)
        assertEquals(argument.werteVorrat, direkt.werteVorrat)
        assertEquals(argument.elementArt, direkt.elementArt)
        assertEquals(argument.komponentenMengen, direkt.komponentenMengen)
        assertEquals("sieben", direkt.latexDarstellung)
        val abgeleitet = differenziereMethode(id)
        val ergebnis = rufe(abgeleitet)
        assertEquals(RationaleZahl.Eins, ergebnis.objekt)
        assertNotEquals("sieben", ergebnis.latexDarstellung)
        assertNull(ergebnis.komponentenMengen)
    }
}
