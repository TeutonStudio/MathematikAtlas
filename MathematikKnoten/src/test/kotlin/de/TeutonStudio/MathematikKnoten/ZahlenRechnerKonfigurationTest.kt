package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikRechenSystem.kern.DivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.*

class ZahlenRechnerKonfigurationTest {
    private fun rechner(operator: UniversellerZahlenOperator): KnotenDaten =
        ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId
        }.erzeuge(GraphPunkt.Zero)

    @Test
    fun `kompatible Rollen behalten ihre Anschluss IDs`() {
        val addition = rechner(UniversellerZahlenOperator.ADDITION)
        val a = addition.anschlüsse.single {
            it.richtung == AnschlussRichtung.Eingang && it.name == "a"
        }
        val b = addition.anschlüsse.single {
            it.richtung == AnschlussRichtung.Eingang && it.name == "b"
        }
        val wert = addition.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        val produkt = konfiguriereZahlenRechner(
            addition,
            operator = UniversellerZahlenOperator.MULTIPLIKATION,
        )

        assertEquals(a.id, produkt.anschlüsse.single { it.name == "a" }.id)
        assertEquals(b.id, produkt.anschlüsse.single { it.name == "b" }.id)
        assertEquals(wert.id, produkt.anschlüsse.single { it.name == "wert" }.id)
        assertEquals(
            UniversellerZahlenOperator.MULTIPLIKATION.stabileId,
            produkt.parameter[ZAHLENRECHNER_OPERATOR],
        )
    }

    @Test
    fun `Wechsel von Division zu Potenz entfernt nur den Fallback`() {
        val division = rechner(UniversellerZahlenOperator.DIVISION)
        val ids = division.anschlüsse.associate { it.name to it.id }

        val potenz = konfiguriereZahlenRechner(
            division,
            operator = UniversellerZahlenOperator.POTENZ,
        )

        assertEquals(setOf("a", "b", "wert"), potenz.anschlüsse.map { it.name }.toSet())
        assertEquals(ids.getValue("a"), potenz.anschlüsse.single { it.name == "a" }.id)
        assertEquals(ids.getValue("b"), potenz.anschlüsse.single { it.name == "b" }.id)
        assertEquals(ids.getValue("wert"), potenz.anschlüsse.single { it.name == "wert" }.id)
    }

    @Test
    fun `Divisionsseitenwechsel behaelt alle Anschluss IDs und markiert den Knoten`() {
        val division = rechner(UniversellerZahlenOperator.DIVISION)
        val anschlussIds = division.anschlüsse.associate { it.name to it.id }

        val links = konfiguriereDivisionsSeite(division, DivisionsSeite.LINKS)
        val rechts = konfiguriereDivisionsSeite(links, DivisionsSeite.RECHTS)

        assertEquals(anschlussIds, links.anschlüsse.associate { it.name to it.id })
        assertEquals(anschlussIds, rechts.anschlüsse.associate { it.name to it.id })
        assertEquals("links", links.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        assertEquals("rechts", rechts.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        assertEquals("false", rechts.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT])
        assertEquals("Division (links)", links.name)
        assertEquals("Division (rechts)", rechts.name)
    }

    @Test
    fun `Komplexkonstruktor schaltet zwischen getrennten und Tupel Handles`() {
        val komplex = rechner(UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH)

        val tupel = konfiguriereZahlenRechner(
            komplex,
            komplexEingabe = ZAHLENRECHNER_KOMPLEX_TUPEL,
        )
        assertEquals(setOf("tupel", "wert"), tupel.anschlüsse.map { it.name }.toSet())
        assertEquals(MathematikAnschlussArten.Tupel.id, tupel.anschlüsse.single { it.name == "tupel" }.art)

        val getrennt = konfiguriereZahlenRechner(
            tupel,
            komplexEingabe = ZAHLENRECHNER_KOMPLEX_SEPARIERT,
        )
        assertEquals(setOf("a", "b", "wert"), getrennt.anschlüsse.map { it.name }.toSet())
        assertEquals(
            ZAHLENRECHNER_KOMPLEX_SEPARIERT,
            getrennt.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE],
        )
    }

    @Test
    fun `Variadische Anzahl erzeugt stabile vorhandene und neue Rollen`() {
        val addition = rechner(UniversellerZahlenOperator.ADDITION)
        val bisherige = addition.anschlüsse.associate { it.name to it.id }

        val erweitert = konfiguriereZahlenRechner(addition, festeEingänge = 4)

        assertEquals(listOf("a", "b", "c", "d", "wert"), erweitert.anschlüsse.map { it.name })
        assertEquals(bisherige.getValue("a"), erweitert.anschlüsse.single { it.name == "a" }.id)
        assertEquals(bisherige.getValue("b"), erweitert.anschlüsse.single { it.name == "b" }.id)
        assertTrue(erweitert.anschlüsse.single { it.name == "c" }.dynamischErzeugt)
        assertTrue(erweitert.anschlüsse.single { it.name == "d" }.dynamischErzeugt)
    }

    @Test
    fun `punktweise Operatoren akzeptieren kontrolliert Zahl oder Methode und schalten den Ausgang`() {
        val addition = rechner(UniversellerZahlenOperator.ADDITION)
        val a = addition.anschlüsse.single { it.name == "a" }
        val ausgang = addition.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        assertEquals(MathematikAnschlussArten.Objekt.id, a.art)
        assertEquals(
            setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
            a.zulässigeArten,
        )
        assertEquals(listOf(MathematikAnschlussArten.Methode.id), ausgang.artPriorisiertEingänge?.prioritäten)

        val methodenQuelle = KnotenDaten(
            art = "test.methode",
            name = "f",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Methode.id,
                ),
            ),
        )
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(methodenQuelle.id, methodenQuelle.anschlüsse.single().id),
            zu = AnschlussVerweis(addition.id, a.id),
        )
        val karte = KartenDaten(name = "Punktweise", knoten = listOf(methodenQuelle, addition))
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, verbindung.von, verbindung.zu))
        assertEquals(
            MathematikAnschlussArten.Methode.id,
            prüfung.effektiveArt(
                karte.copy(verbindungen = listOf(verbindung)),
                AnschlussVerweis(addition.id, ausgang.id),
            ),
        )
    }
}
