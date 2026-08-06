package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IterierteSelbstkompositionTest {
    private val x = Variable("x")
    private val y = Variable("y")

    private fun zweistelligeTupelMethode(): Methode = Methode(
        name = "f",
        parameter = listOf(x, y),
        vorschrift = Tupel(listOf(x, y)),
        zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
        werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
    )

    @Test
    fun `mehrstellige Methode bleibt aeusserlich mehrstellig`() {
        val methode = zweistelligeTupelMethode()
        val iteration = IterierteSelbstkomposition(
            methode,
            IterationsOrdnung.Konkret(2),
            eingangsModus = KompositionsEingangsModus.GEPACKTES_TUPEL,
            ausgangsModus = KompositionsAusgangsModus.ENTPACKT,
        )

        assertEquals(SelbstkompositionsStatus.TOTAL_GUELTIG, iteration.pruefung.status)
        assertEquals(listOf(x, y), iteration.aeussereParameter)
        assertEquals("{f}^{\\langle 2\\rangle}", iteration.zuLatex())
    }

    @Test
    fun `Ordnung null liefert Identitaet auf dem Produktraum`() {
        val iteration = IterierteSelbstkomposition(
            zweistelligeTupelMethode(),
            IterationsOrdnung.Konkret(0),
        )

        val identitaet = requireNotNull(iteration.nullteIdentitaetOderNull())
        assertEquals(
            "\\operatorname{id}\\vert_{\\mathbb R \\times \\mathbb R}",
            identitaet.zuLatex(),
        )
        assertEquals(iteration.werteVorrat, iteration.zielMenge)
    }

    @Test
    fun `Zeile und Spalte werden als geordnete Komponentenquelle akzeptiert`() {
        val zeile = Methode(
            name = "z",
            parameter = listOf(x, y),
            vorschrift = ZeilenVektor(listOf(x, y)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val spalte = zeile.copy(name = "s", vorschrift = SpaltenVektor(listOf(x, y)))

        assertTrue(pruefeSelbstkomposition(zeile).istZulaessig)
        assertTrue(pruefeSelbstkomposition(spalte).istZulaessig)
        assertEquals(listOf(x, y), entpackeEineEbene(zeile.vorschrift))
        assertEquals(listOf(x, y), entpackeEineEbene(spalte.vorschrift))
    }

    @Test
    fun `abweichende Komponentenzahl ist mathematisch unmoeglich`() {
        val methode = zweistelligeTupelMethode().copy(
            vorschrift = Tupel(listOf(x, y, RationaleZahl.Null)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen, ReelleZahlen)),
        )
        val pruefung = pruefeSelbstkomposition(methode)

        assertEquals(SelbstkompositionsStatus.MATHEMATISCH_UNMOEGLICH, pruefung.status)
        assertFailsWith<IllegalArgumentException> {
            IterierteSelbstkomposition(methode, IterationsOrdnung.Konkret(2))
        }
    }

    @Test
    fun `Packen und Entpacken arbeitet genau eine Strukturebene tief`() {
        val innen = Tupel(listOf(y, RationaleZahl.Eins))
        val gepackt = packeMethodenArgumente(listOf(x, innen))
        val entpackt = entpackeEineEbene(gepackt)

        assertEquals(2, entpackt.size)
        assertSame(innen, entpackt[1])
        assertIs<Tupel>(entpackt[1])
    }

    @Test
    fun `hoeherer Wertevorrat bleibt rekursiv und nicht als Stichprobe gespeichert`() {
        val bereich = maximalerKompositionsWertevorrat(
            zweistelligeTupelMethode(),
            IterationsOrdnung.Konkret(4),
        )

        val rekursiv = assertIs<RekursiverKompositionsWertevorrat>(bereich)
        assertEquals("W_{4}", rekursiv.zuLatex())
        assertTrue(rekursiv.definitionsLatex().contains("^{-1}"))
    }
}
