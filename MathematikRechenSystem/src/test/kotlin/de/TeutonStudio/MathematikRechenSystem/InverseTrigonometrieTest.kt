package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InverseTrigonometrieTest {
    @Test
    fun `latex und klartext verwenden mathematische Namen`() {
        val x = Variable("x")

        assertEquals("\\arcsin\\left(x\\right)", ArcSinus(x).zuLatex())
        assertEquals("\\arccos\\left(x\\right)", ArcCosinus(x).zuLatex())
        assertEquals("arcsin(x)", ArcSinus(x).toString())
        assertEquals("arccos(x)", ArcCosinus(x).toString())
    }

    @Test
    fun `kanonische Hauptwerte werden exakt vereinfacht`() {
        val halbesPi = Division(Pi, RationaleZahl.von(2))

        assertEquals(RationaleZahl.Null, vereinfache(ArcSinus(RationaleZahl.Null)))
        assertEquals(halbesPi, vereinfache(ArcSinus(RationaleZahl.Eins)))
        assertEquals(negation(halbesPi), vereinfache(ArcSinus(RationaleZahl.von(-1))))
        assertEquals(RationaleZahl.Null, vereinfache(ArcCosinus(RationaleZahl.Eins)))
        assertEquals(halbesPi, vereinfache(ArcCosinus(RationaleZahl.Null)))
        assertEquals(Pi, vereinfache(ArcCosinus(RationaleZahl.von(-1))))
    }

    @Test
    fun `bekannte Argumente ausserhalb des reellen Bereichs schlagen konkret fehl`() {
        assertFailsWith<IllegalArgumentException> { vereinfache(ArcSinus(RationaleZahl.von(2))) }
        assertFailsWith<IllegalArgumentException> { vereinfache(ArcCosinus(RationaleZahl.von(-2))) }

        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(ArcSinus(RationaleZahl.von(2))),
        )
        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(ArcCosinus(RationaleZahl.von(-2))),
        )
    }

    @Test
    fun `numerische Auswertung respektiert Hauptwerte und Randtoleranz`() {
        assertEquals(
            PI / 6.0,
            assertIs<NumerischesErgebnis.Wert<Double>>(
                NumerischerAuswerter.wert(ArcSinus(RationaleZahl.von(1, 2))),
            ).wert,
            1e-12,
        )
        assertEquals(
            2.0 * PI / 3.0,
            assertIs<NumerischesErgebnis.Wert<Double>>(
                NumerischerAuswerter.wert(ArcCosinus(RationaleZahl.von(-1, 2))),
            ).wert,
            1e-12,
        )
    }

    @Test
    fun `Substitution und Variablenanalyse folgen dem Argument`() {
        val x = Variable("x")
        val ausdruck = ArcSinus(addition(x, RationaleZahl.Eins))

        assertEquals(setOf(x), ausdruck.enthalteneVariablen())
        assertEquals(
            ArcSinus(RationaleZahl.von(3)),
            ersetze(ausdruck, mapOf("x" to RationaleZahl.von(2))),
        )
    }

    @Test
    fun `symbolische Definitionsbedingung bleibt explizit und steuert Reellheitsnachweis`() {
        val x = Variable("x")
        val bedingung = inverseTrigonometrischeDefinitionsBedingung(x)

        assertEquals(
            Konjunktion(
                listOf(
                    Vergleich(RationaleZahl.von(-1), VergleichsArt.KleinerGleich, x),
                    Vergleich(x, VergleichsArt.KleinerGleich, RationaleZahl.Eins),
                ),
            ),
            bedingung,
        )
        assertFalse(istNachweisbarReell(ArcSinus(x), variableIstReell = { true }))
        assertTrue(istNachweisbarReell(ArcSinus(x), { true }, setOf(bedingung)))
        assertEquals(ReelleZahlen, inferiereZahlenWertevorrat(ArcCosinus(x), mapOf("x" to ReelleZahlen)))
    }

    @Test
    fun `trigonometrische Funktionen werden ohne Bereichsannahmen nicht naiv gekuerzt`() {
        val x = Variable("x")

        assertEquals(ArcSinus(Sinus(x)), vereinfache(ArcSinus(Sinus(x))))
        assertEquals(ArcCosinus(Cosinus(x)), vereinfache(ArcCosinus(Cosinus(x))))
    }

    @Test
    fun `Ableitungen verwenden die Hauptzweigformeln`() {
        val x = Variable("x")
        val nenner = wurzel(subtraktion(RationaleZahl.Eins, Potenz(x, RationaleZahl.von(2))))

        assertEquals(Division(RationaleZahl.Eins, nenner), ableiten(ArcSinus(x), x).ergebnis)
        assertEquals(negation(Division(RationaleZahl.Eins, nenner)), ableiten(ArcCosinus(x), x).ergebnis)
    }
}
