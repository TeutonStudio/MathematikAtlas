package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals

class SelbstkompositionsZielmengenTest {
    @Test
    fun `strukturierte Selbstkomposition verwendet ihre deklarierte Zielmenge`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val ausdruck = IterierteSelbstkomposition(
            methode = methode,
            ordnung = IterationsOrdnung.Konkret(3),
            zielMenge = ReelleZahlen,
        )

        assertEquals(ReelleZahlen, inferiereZielmenge(ausdruck))
    }

    @Test
    fun `rekursiver Wertevorrat bleibt als Mengenvertrag erhalten`() {
        val bereich = RekursiverKompositionsWertevorrat(
            methodeName = "f",
            ordnung = IterationsOrdnung.Konkret(4),
            grundbereich = ReelleZahlen,
        )

        assertEquals(bereich, inferiereZielmenge(bereich))
    }
}
