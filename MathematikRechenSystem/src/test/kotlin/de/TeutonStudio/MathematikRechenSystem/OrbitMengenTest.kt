package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrbitMengenTest {
    private val z = Variable("z")
    private val c = Variable("c")
    private fun q(wert: Long) = RationaleZahl.von(wert)

    private fun mandelbrotMenge(): OrbitBeschraenktheitsMenge {
        val schritt = Methode(
            name = "f",
            parameter = listOf(z, c),
            vorschrift = addition(Potenz(z, q(2)), c),
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf("z" to KomplexeZahlen, "c" to KomplexeZahlen),
        )
        return OrbitBeschraenktheitsMenge(OrbitFamilie(schritt, RationaleZahl.Null), KomplexeZahlen)
    }

    @Test
    fun `quadratische Nullstartfamilie trennt Beweise Flucht und offenen Lauf`() {
        val menge = mandelbrotMenge()

        assertIs<OrbitEntscheidung.Enthalten>(entscheideOrbitBeschraenktheit(menge, q(0)))
        assertIs<OrbitEntscheidung.Enthalten>(entscheideOrbitBeschraenktheit(menge, q(-2)))
        assertIs<OrbitEntscheidung.Ausgeschlossen>(entscheideOrbitBeschraenktheit(menge, q(1)))
        assertIs<OrbitEntscheidung.Unbekannt>(entscheideOrbitBeschraenktheit(menge, q(1), maximaleSchritte = 1))
    }

    @Test
    fun `komplexe Zellen erhalten ausschließlich allquantifizierte Zertifikate`() {
        val menge = mandelbrotMenge()
        val innen = entscheideQuadratischeNullstartZelle(
            menge,
            RationalesIntervall(RationaleZahl.von(-1, 10), RationaleZahl.von(1, 10)),
            RationalesIntervall(RationaleZahl.von(-1, 10), RationaleZahl.von(1, 10)),
        )
        val außen = entscheideQuadratischeNullstartZelle(
            menge,
            RationalesIntervall(q(1)),
            RationalesIntervall(q(0)),
        )

        assertIs<OrbitEntscheidung.Enthalten>(innen)
        assertIs<OrbitEntscheidung.Ausgeschlossen>(außen)
    }

    @Test
    fun `generische Familie beweist exakten Zyklus ohne endlichen Lauf umzudeuten`() {
        val identität = Methode(
            name = "id",
            parameter = listOf(z, c),
            vorschrift = z,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to ReelleZahlen),
        )
        val menge = OrbitBeschraenktheitsMenge(OrbitFamilie(identität, q(3)), ReelleZahlen)

        val entscheidung = entscheideOrbitBeschraenktheit(menge, q(7))
        assertIs<OrbitEntscheidung.Enthalten>(entscheidung)
        assertTrue("periodisch" in entscheidung.grund)
    }

    @Test
    fun `generische Familien unterscheiden Zyklusbeweis und offenen endlichen Lauf`() {
        val konstantNull = Methode(
            name = "null",
            parameter = listOf(z, c),
            vorschrift = RationaleZahl.Null,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to ReelleZahlen),
        )
        val verschiebung = Methode(
            name = "plusEins",
            parameter = listOf(z, c),
            vorschrift = addition(z, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to ReelleZahlen),
        )

        assertIs<OrbitEntscheidung.Enthalten>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(OrbitFamilie(konstantNull, q(3)), ReelleZahlen),
                q(7),
            ),
        )
        assertIs<OrbitEntscheidung.Unbekannt>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(OrbitFamilie(verschiebung, q(0)), ReelleZahlen),
                q(7),
                maximaleSchritte = 16,
            ),
        )
    }

    @Test
    fun `undefinierte Parameter und Zustände werden nicht als beschränkte Orbits bewiesen`() {
        val nurNatürlicherParameter = Methode(
            name = "parameterbereich",
            parameter = listOf(z, c),
            vorschrift = z,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to NatürlicheZahlen),
        )
        val nurNatürlicherZustand = Methode(
            name = "zustandsbereich",
            parameter = listOf(z, c),
            vorschrift = z,
            zielMenge = NatürlicheZahlen,
            werteVorräte = mapOf("z" to NatürlicheZahlen, "c" to ReelleZahlen),
        )

        assertIs<OrbitEntscheidung.Unbekannt>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(OrbitFamilie(nurNatürlicherParameter, q(0)), ReelleZahlen),
                RationaleZahl.von(1, 2),
            ),
        )
        assertIs<OrbitEntscheidung.Unbekannt>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(OrbitFamilie(nurNatürlicherZustand, q(0)), ReelleZahlen),
                q(1),
            ),
        )
    }

    @Test
    fun `parametrisierter Start respektiert seinen Definitionsbereich`() {
        val p = Variable("p")
        val start = Methode(
            name = "start",
            parameter = listOf(p),
            vorschrift = p,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("p" to NatürlicheZahlen),
        )
        val identität = Methode(
            name = "id",
            parameter = listOf(z, c),
            vorschrift = z,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("z" to ReelleZahlen, "c" to ReelleZahlen),
        )

        assertIs<OrbitEntscheidung.Unbekannt>(
            entscheideOrbitBeschraenktheit(
                OrbitBeschraenktheitsMenge(OrbitFamilie(identität, start), ReelleZahlen),
                RationaleZahl.von(1, 2),
            ),
        )
    }
}
