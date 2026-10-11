package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenGraphTest {
    private fun tupelRaum(vararg komponenten: MengenAusdruck) = Tupelraum(komponenten.toList())

    @Test
    fun `einstellige methode behaelt ihren einertupelraum als argumentraum`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = multiplikation(listOf(x, x)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val argumentRaum = tupelRaum(ReelleZahlen)
        val zielRaum = tupelRaum(ReelleZahlen)

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(tupelRaum(argumentRaum, zielRaum), methode.graphRaum())
        assertEquals("\\operatorname{Graph}\\left(f\\right)", methode.graphMenge().zuLatex())
        assertEquals(methode, methode.graphMenge().methode)
    }

    @Test
    fun `graphmenge besitzt den erwarteten geschachtelten tupel elementraum`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = GanzeZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(
            tupelRaum(tupelRaum(ReelleZahlen), tupelRaum(GanzeZahlen)),
            inferiereZielmenge(methode.graphMenge()),
        )
    }

    @Test
    fun `mehrere argumente bleiben als geordneter argumentraum erhalten`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(listOf(x, y)),
            zielMenge = ReelleZahlen,
            werteVorräte = linkedMapOf(
                x.name to ReelleZahlen,
                y.name to GanzeZahlen,
            ),
        )
        val argumentRaum = tupelRaum(ReelleZahlen, GanzeZahlen)
        val zielRaum = tupelRaum(ReelleZahlen)

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(tupelRaum(argumentRaum, zielRaum), methode.graphRaum())
        assertIs<Tupelraum>((methode.graphRaum() as Tupelraum).komponenten.first())
    }

    @Test
    fun `effektiver wertevorrat einer restriktion bestimmt den graphen`() {
        val x = Variable("x")
        val intervall = ReellesIntervall(
            links = RationaleZahl.Null,
            linksOffen = false,
            rechts = RationaleZahl.Eins,
            rechtsOffen = false,
        )
        val methode = Methode(
            name = "f|_M",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
            effektiverWerteVorrat = intervall,
        )
        val argumentRaum = tupelRaum(intervall)

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(tupelRaum(argumentRaum, tupelRaum(ReelleZahlen)), methode.graphRaum())
    }

    @Test
    fun `aktueller erweiterter wertevorrat wird ohne graph sonderzustand uebernommen`() {
        val x = Variable("x")
        val erweitert = BenannteMenge("W_tilde", "\\widetilde W")
        val methode = Methode(
            name = "f_tilde",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
            effektiverWerteVorrat = erweitert,
        )
        val argumentRaum = tupelRaum(erweitert)

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(tupelRaum(argumentRaum, tupelRaum(ReelleZahlen)), methode.graphRaum())
    }

    @Test
    fun `mehrere ausgaben bleiben als strukturierter zielraum erhalten`() {
        val x = Variable("x")
        val ziel = tupelRaum(ReelleZahlen, GanzeZahlen)
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(x, RationaleZahl.Eins)),
            zielMenge = ziel,
            werteVorräte = mapOf(x.name to ReelleZahlen),
            ausgabeNamen = listOf("reell", "ganz"),
        )

        assertEquals(
            tupelRaum(tupelRaum(ReelleZahlen), ziel),
            methode.graphRaum(),
        )
    }

    @Test
    fun `fehlender wertevorrat wird nicht geraten`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
        )

        val fehler = assertFailsWith<IllegalStateException> { methode.graphRaum() }
        assertTrue(fehler.message.orEmpty().contains("x"))
        assertTrue(fehler.message.orEmpty().contains("Definitionsmenge"))
    }

    @Test
    fun `komplex nach reell besitzt einertupel C kreuz R als graphraum`() {
        val z = Variable("z")
        val methode = Methode(
            name = "f",
            parameter = listOf(z),
            vorschrift = RationaleZahl.Null,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(z.name to KomplexeZahlen),
        )

        assertEquals(
            tupelRaum(tupelRaum(KomplexeZahlen), tupelRaum(ReelleZahlen)),
            methode.graphRaum(),
        )
    }

    @Test
    fun `reell nach komplex behaelt einertupel R vor C`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(
            tupelRaum(tupelRaum(ReelleZahlen), tupelRaum(KomplexeZahlen)),
            methode.graphRaum(),
        )
    }

    @Test
    fun `komplex nach komplex bleibt mathematisch gueltige graphmenge mit einertupel`() {
        val z = Variable("z")
        val methode = Methode(
            name = "f",
            parameter = listOf(z),
            vorschrift = z,
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf(z.name to KomplexeZahlen),
        )

        assertEquals(
            tupelRaum(tupelRaum(KomplexeZahlen), tupelRaum(KomplexeZahlen)),
            methode.graphRaum(),
        )
        assertIs<MethodenGraphMenge>(methode.graphMenge())
    }
}
