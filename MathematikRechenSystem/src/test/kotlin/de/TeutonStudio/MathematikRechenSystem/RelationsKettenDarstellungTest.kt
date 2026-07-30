package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RelationsKettenDarstellungTest {
    private val a = Variable("a")
    private val b = Variable("b")
    private val c = Variable("c")
    private val d = Variable("d")
    private val x = Variable("x")

    @Test
    fun `beliebig lange gemischte Kette wird zusammengezogen`() {
        val aussage = Konjunktion(
            listOf(
                Vergleich(a, VergleichsArt.Kleiner, b),
                Vergleich(b, VergleichsArt.KleinerGleich, c),
                Vergleich(c, VergleichsArt.Kleiner, d),
            ),
        )

        assertEquals("a < b \\le c < d", aussage.zuLatex())
    }

    @Test
    fun `groesser Relationen werden in aufsteigende Kette gedreht`() {
        val aussage = Konjunktion(
            listOf(
                Vergleich(c, VergleichsArt.Größer, b),
                Vergleich(b, VergleichsArt.GrößerGleich, a),
            ),
        )

        assertEquals("a \\le b < c", aussage.zuLatex())
    }

    @Test
    fun `verzweigung bleibt eine Konjunktion`() {
        val aussage = Konjunktion(
            listOf(
                Vergleich(a, VergleichsArt.Kleiner, x),
                Vergleich(x, VergleichsArt.Kleiner, b),
                Vergleich(x, VergleichsArt.Kleiner, c),
            ),
        )

        assertEquals("a < x \\land x < b \\land x < c", aussage.zuLatex())
    }

    @Test
    fun `zyklus bleibt eine Konjunktion`() {
        val aussage = Konjunktion(
            listOf(
                Vergleich(a, VergleichsArt.Kleiner, b),
                Vergleich(b, VergleichsArt.Kleiner, c),
                Vergleich(c, VergleichsArt.Kleiner, a),
            ),
        )

        assertEquals("a < b \\land b < c \\land c < a", aussage.zuLatex())
    }

    @Test
    fun `getrennte Ketten werden unabhängig optimiert`() {
        val y = Variable("y")
        val z = Variable("z")
        val aussage = Konjunktion(
            listOf(
                Vergleich(a, VergleichsArt.Kleiner, b),
                Vergleich(b, VergleichsArt.Kleiner, c),
                Vergleich(x, VergleichsArt.KleinerGleich, y),
                Vergleich(y, VergleichsArt.Kleiner, z),
            ),
        )

        assertEquals("a < b < c \\land x \\le y < z", aussage.zuLatex())
    }
}
