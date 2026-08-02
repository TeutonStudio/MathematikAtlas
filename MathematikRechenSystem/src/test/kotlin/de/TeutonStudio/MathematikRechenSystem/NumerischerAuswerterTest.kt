package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.E
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NumerischerAuswerterTest {
    @Test
    fun `skalare Terme verwenden eine gemeinsame Umgebung`() {
        val x = Variable("x")
        val term = addition(
            multiplikation(RationaleZahl.von(2), x),
            Division(RationaleZahl.von(3), RationaleZahl.von(2)),
        )

        val ergebnis = NumerischerAuswerter.wert(term, NumerischeUmgebung(mapOf("x" to 4.0)))

        assertEquals(9.5, assertIs<NumerischesErgebnis.Wert<Double>>(ergebnis).wert, 1e-12)
        assertIs<NumerischesErgebnis.BindungFehlt>(NumerischerAuswerter.wert(term))
    }

    @Test
    fun `unterstuetzte Funktionen werden rekursiv ausgewertet`() {
        val term = addition(
            Betrag(RationaleZahl.von(-2)),
            Sinus(Pi),
            Cosinus(RationaleZahl.Null),
            Exponentialfunktion(RationaleZahl.Eins),
            NatürlicherLogarithmus(EulerscheZahl),
            Wurzel(RationaleZahl.von(9)),
            Logarithmus(RationaleZahl.von(2), RationaleZahl.von(8)),
            maximum(RationaleZahl.von(1), RationaleZahl.von(4)),
            minimum(RationaleZahl.von(3), RationaleZahl.von(5)),
        )

        val wert = assertIs<NumerischesErgebnis.Wert<Double>>(NumerischerAuswerter.wert(term)).wert

        assertEquals(17.0 + E, wert, 1e-9)
        assertEquals(PI, assertIs<NumerischesErgebnis.Wert<Double>>(NumerischerAuswerter.wert(Pi)).wert, 1e-12)
    }

    @Test
    fun `Definitionsfehler bleiben unterscheidbar`() {
        assertIs<NumerischesErgebnis.Undefiniert>(
            NumerischerAuswerter.wert(Division(RationaleZahl.Eins, RationaleZahl.Null)),
        )
        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(NatürlicherLogarithmus(RationaleZahl.Null)),
        )
        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(Wurzel(RationaleZahl.von(-1))),
        )
        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(Logarithmus(RationaleZahl.Eins, RationaleZahl.von(2))),
        )
        assertIs<NumerischesErgebnis.Definitionsbereich>(
            NumerischerAuswerter.wert(Potenz(RationaleZahl.von(-1), RationaleZahl.von(1, 2))),
        )
    }

    @Test
    fun `Fallausdruecke waehlen genau einen numerisch entschiedenen Zweig`() {
        val x = Variable("x")
        val fall = FallAusdruck(
            wahr = RationaleZahl.von(7),
            aussage = Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null),
            lüge = RationaleZahl.von(-7),
        ) as ZahlAusdruck

        assertEquals(
            7.0,
            assertIs<NumerischesErgebnis.Wert<Double>>(
                NumerischerAuswerter.wert(fall, NumerischeUmgebung(mapOf("x" to 2.0))),
            ).wert,
        )
        assertEquals(
            -7.0,
            assertIs<NumerischesErgebnis.Wert<Double>>(
                NumerischerAuswerter.wert(fall, NumerischeUmgebung(mapOf("x" to -2.0))),
            ).wert,
        )
        assertIs<NumerischesErgebnis.BindungFehlt>(NumerischerAuswerter.wert(fall))
    }

    @Test
    fun `Aussagen verwenden Toleranz Logik und Mengenbeziehungen`() {
        val optionen = NumerischeOptionen(toleranz = 1e-6)
        val fastGleich = Gleichheit(
            Division(RationaleZahl.Eins, RationaleZahl.von(3)),
            RationaleZahl.von(333333, 1_000_000),
        )
        val intervall = ReellesIntervall(
            links = RationaleZahl.Null,
            linksOffen = false,
            rechts = RationaleZahl.von(2),
            rechtsOffen = true,
        )
        val aussage = Konjunktion(
            listOf(
                fastGleich,
                ElementBeziehung(RationaleZahl.Eins, intervall),
                ElementBeziehung(RationaleZahl.von(3), GanzeZahlen),
                Negation(ElementBeziehung(RationaleZahl.von(-1), NatürlicheZahlen)),
            ),
        )

        assertTrue(assertIs<NumerischesErgebnis.Wert<Boolean>>(NumerischerAuswerter.aussage(aussage, optionen = optionen)).wert)
        assertFalse(
            assertIs<NumerischesErgebnis.Wert<Boolean>>(
                NumerischerAuswerter.aussage(ElementBeziehung(RationaleZahl.von(2), intervall)),
            ).wert,
        )
    }

    @Test
    fun `nicht unterstuetzte symbolische Objekte liefern strukturierte Fehler`() {
        val methode = Methode(
            name = "f",
            parameter = listOf(Variable("x")),
            ausgaben = mapOf("wert" to Variable("x")),
        )
        assertIs<NumerischesErgebnis.NichtUnterstützt>(
            NumerischerAuswerter.wert(IterierteSumme(methode, ReelleZahlen)),
        )
        assertIs<NumerischesErgebnis.NichtUnterstützt>(
            NumerischerAuswerter.aussage(UnentscheidbareAussage("P", "T")),
        )
    }
}
