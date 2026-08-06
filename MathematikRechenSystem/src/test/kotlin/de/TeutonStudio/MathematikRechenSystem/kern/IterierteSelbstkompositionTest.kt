package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IterierteSelbstkompositionTest {
    private val x = Variable("x")

    private fun reelleMethode(): Methode = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = addition(x, RationaleZahl.Eins),
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `einstellige reelle Methode ist total selbstkomponierbar`() {
        val pruefung = pruefeSelbstkomposition(reelleMethode())

        assertTrue(pruefung.istZulaessig)
        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, pruefung.status)
        assertEquals(1, pruefung.argumentAnzahl)
        assertEquals(1, pruefung.ergebnisKomponenten)
        assertTrue(pruefung.voraussetzungen.isEmpty())
    }

    @Test
    fun `zweite konkrete Komposition baut verschachtelte Vorschrift`() {
        val ergebnis = werteSelbstkompositionAus(
            reelleMethode(),
            IterationsOrdnung.Konkret(2),
        )
        val methode = requireNotNull(ergebnis.methode)

        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, ergebnis.status)
        assertEquals(addition(x, RationaleZahl.von(2)), methode.vorschrift)
        assertEquals(ReelleZahlen, methode.werteVorräte.getValue("x"))
        assertEquals(ReelleZahlen, methode.zielMenge)
    }

    @Test
    fun `mehrstellige Vertauschung komponiert zu Identitaet`() {
        val y = Variable("y")
        val vertauschung = Methode(
            name = "s",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(y, x)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        val pruefung = pruefeSelbstkomposition(vertauschung)
        val ergebnis = werteSelbstkompositionAus(
            vertauschung,
            IterationsOrdnung.Konkret(2),
        )
        val methode = requireNotNull(ergebnis.methode)

        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, pruefung.status)
        assertEquals(Tupel(listOf(x, y)), methode.vorschrift)
        assertEquals(vertauschung.parameter, methode.parameter)
    }

    @Test
    fun `Zeilenvektor wird genau eine Ebene als Argumentquelle verwendet`() {
        val y = Variable("y")
        val methode = Methode(
            name = "z",
            parameter = listOf(x, y),
            vorschrift = ZeilenVektor(listOf(y, x)),
            zielMenge = Vektorraum(VektorOrientierung.Zeile, 2, ReelleZahlen),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        val pruefung = pruefeSelbstkomposition(methode)

        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, pruefung.status)
        assertEquals(2, pruefung.ergebnisKomponenten)
    }

    @Test
    fun `verschachteltes Tupel wird nicht rekursiv abgeflacht`() {
        val y = Variable("y")
        val z = Variable("z")
        val methode = Methode(
            name = "n",
            parameter = listOf(x, y, z),
            vorschrift = Tupel(listOf(Tupel(listOf(x, y)), z)),
            zielMenge = Tupelraum(
                listOf(
                    Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
                    ReelleZahlen,
                ),
            ),
            werteVorräte = mapOf(
                "x" to ReelleZahlen,
                "y" to ReelleZahlen,
                "z" to ReelleZahlen,
            ),
        )

        val pruefung = pruefeSelbstkomposition(methode)

        assertEquals(SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH, pruefung.status)
        assertEquals(2, pruefung.ergebnisKomponenten)
    }

    @Test
    fun `falsche Komponentenzahl wird mathematisch abgelehnt`() {
        val y = Variable("y")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        val pruefung = pruefeSelbstkomposition(methode)
        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(2),
        )

        assertEquals(SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH, pruefung.status)
        assertNull(ergebnis.methode)
    }

    @Test
    fun `endlicher maximaler Wertevorrat wird exakt eingeschraenkt`() {
        val domain = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val methode = Methode(
            name = "h",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = GanzeZahlen,
            werteVorräte = mapOf("x" to domain),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(2),
            bereichsModus = KompositionsBereichsModus.MAXIMAL_ZULAESSIG,
        )
        val komponiert = requireNotNull(ergebnis.methode)

        assertEquals(SelbstkompositionsStatus.EINGESCHRAENKT_GUELTIG, ergebnis.status)
        assertEquals(EndlicheMenge(setOf(RationaleZahl.Null)), ergebnis.maximalerWertevorrat)
        assertEquals(EndlicheMenge(setOf(RationaleZahl.Null)), komponiert.werteVorräte.getValue("x"))
        assertEquals(addition(x, RationaleZahl.von(2)), komponiert.vorschrift)
    }

    @Test
    fun `vollstaendiger Bereich lehnt notwendige Einschraenkung ab`() {
        val domain = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val methode = Methode(
            name = "h",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = GanzeZahlen,
            werteVorräte = mapOf("x" to domain),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(2),
            bereichsModus = KompositionsBereichsModus.VOLLSTAENDIGER_URSPRUNGSBEREICH,
        )

        assertEquals(SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH, ergebnis.status)
        assertNull(ergebnis.methode)
    }

    @Test
    fun `leerer maximaler Wertevorrat wird eigener Status`() {
        val domain = EndlicheMenge(setOf(RationaleZahl.Null))
        val methode = Methode(
            name = "leer",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = GanzeZahlen,
            werteVorräte = mapOf("x" to domain),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(2),
        )

        assertEquals(SelbstkompositionsStatus.LEERER_WERTEVORRAT, ergebnis.status)
        assertEquals(LeereMenge, ergebnis.maximalerWertevorrat)
        assertEquals(LeereMenge, requireNotNull(ergebnis.methode).werteVorräte.getValue("x"))
    }

    @Test
    fun `Ordnung null liefert getrennte Identitaet mit getrennten Ausgaengen`() {
        val y = Variable("y")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(x, y)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(0),
            eingangsModus = KompositionsEingangsModus.GETRENNTE_ARGUMENTE,
            ausgangsModus = KompositionsAusgangsModus.ENTPACKT,
        )
        val identitaet = requireNotNull(ergebnis.methode)

        assertEquals(2, identitaet.parameter.size)
        assertEquals(2, identitaet.ausgabeNamen.size)
        assertEquals(Tupel(listOf(x, y)), identitaet.vorschrift)
        assertEquals(aeussererMethodenWertevorrat(methode), identitaet.zielMenge)
    }

    @Test
    fun `Ordnung null kann als gepackte Identitaet dargestellt werden`() {
        val y = Variable("y")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(x, y)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(0),
            eingangsModus = KompositionsEingangsModus.GEPACKTES_TUPEL,
            ausgangsModus = KompositionsAusgangsModus.GEPACKT,
        )
        val identitaet = requireNotNull(ergebnis.methode)

        assertEquals(1, identitaet.parameter.size)
        assertEquals(1, identitaet.ausgabeNamen.size)
        assertEquals(aeussererMethodenWertevorrat(methode), identitaet.zielMenge)
    }

    @Test
    fun `Ordnung eins liefert dieselbe Methode`() {
        val methode = reelleMethode()
        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(1),
        )

        assertSame(methode, ergebnis.methode)
        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, ergebnis.status)
    }

    @Test
    fun `symbolische Ordnung bleibt strukturierte Methode`() {
        val n = Variable("n")
        val annahme = UnentscheidbareAussage("n\\in\\mathbb N_0", "Iteration")
        val ergebnis = werteSelbstkompositionAus(
            reelleMethode(),
            IterationsOrdnung.Symbolisch(n, setOf(annahme)),
        )
        val methode = requireNotNull(ergebnis.methode)

        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, ergebnis.status)
        assertIs<IterierteSelbstkomposition>(methode.vorschrift)
        assertTrue(annahme in ergebnis.voraussetzungen)
    }

    @Test
    fun `grosse konkrete Ordnung bleibt jenseits des Budgets strukturiert`() {
        val ergebnis = werteSelbstkompositionAus(
            reelleMethode(),
            IterationsOrdnung.Konkret(100),
            auswertungsBudget = 8,
        )

        assertEquals(SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT, ergebnis.status)
        assertIs<IterierteSelbstkomposition>(requireNotNull(ergebnis.methode).vorschrift)
    }

    @Test
    fun `korrelierter mehrstelliger Teilbereich bleibt transparent unimplementiert`() {
        val y = Variable("y")
        val domain = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val methode = Methode(
            name = "k",
            parameter = listOf(x, y),
            vorschrift = Tupel(listOf(addition(x, RationaleZahl.Eins), y)),
            zielMenge = Tupelraum(listOf(GanzeZahlen, domain)),
            werteVorräte = mapOf("x" to domain, "y" to domain),
        )

        val ergebnis = werteSelbstkompositionAus(
            methode,
            IterationsOrdnung.Konkret(2),
        )

        assertEquals(SelbstkompositionsStatus.NOCH_NICHT_IMPLEMENTIERT, ergebnis.status)
        assertNull(ergebnis.methode)
        assertTrue(ergebnis.begruendung.contains("korrelierte"))
    }
}
