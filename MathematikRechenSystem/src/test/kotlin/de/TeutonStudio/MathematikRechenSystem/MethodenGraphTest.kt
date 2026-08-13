package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenGraphTest {
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
        val argumentRaum = Tupelraum(listOf(ReelleZahlen))

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(KartesischesProdukt(listOf(argumentRaum, ReelleZahlen)), methode.graphRaum())
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
            Tupelraum(listOf(Tupelraum(listOf(ReelleZahlen)), GanzeZahlen)),
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
        val argumentRaum = Tupelraum(listOf(ReelleZahlen, GanzeZahlen))

        assertEquals(argumentRaum, methode.argumentRaum())
        assertEquals(
            KartesischesProdukt(listOf(argumentRaum, ReelleZahlen)),
            methode.graphRaum(),
        )
        assertIs<Tupelraum>((methode.graphRaum() as KartesischesProdukt).mengen.first())
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

        assertEquals(intervall, methode.argumentRaum())
        assertEquals(KartesischesProdukt(listOf(intervall, ReelleZahlen)), methode.graphRaum())
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

        assertEquals(erweitert, methode.argumentRaum())
        assertEquals(KartesischesProdukt(listOf(erweitert, ReelleZahlen)), methode.graphRaum())
    }

    @Test
    fun `mehrere ausgaben bleiben als strukturierter zielraum erhalten`() {
        val x = Variable("x")
        val ziel = Tupelraum(listOf(ReelleZahlen, GanzeZahlen))
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Tupel(listOf(x, RationaleZahl.Eins)),
            zielMenge = ziel,
            werteVorräte = mapOf(x.name to ReelleZahlen),
            ausgabeNamen = listOf("reell", "ganz"),
        )

        assertEquals(
            KartesischesProdukt(listOf(Tupelraum(listOf(ReelleZahlen)), ziel)),
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
        assertTrue(fehler.message.orEmpty().contains("Wertevorrat"))
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
            KartesischesProdukt(listOf(Tupelraum(listOf(KomplexeZahlen)), ReelleZahlen)),
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
            KartesischesProdukt(listOf(Tupelraum(listOf(ReelleZahlen)), KomplexeZahlen)),
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
            KartesischesProdukt(listOf(Tupelraum(listOf(KomplexeZahlen)), KomplexeZahlen)),
            methode.graphRaum(),
        )
        assertIs<MethodenGraphMenge>(methode.graphMenge())
    }
}
