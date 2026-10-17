package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UniversellerZahlenRechnerTest {
    @Test
    fun `Multiplikationsregeln unterscheiden unbekannt von nicht kommutativ`() {
        ZahlenRechnerBereich.entries.forEach { bereich ->
            val regeln = rechenRegeln(UniversellerZahlenOperator.MULTIPLIKATION, bereich)
            val erwartet = when (bereich) {
                ZahlenRechnerBereich.UNBEKANNT -> listOf(
                    "Der Zahlbereich ist unbekannt. Kommutativität ist nicht nachgewiesen; die Faktorordnung bleibt erhalten.",
                )
                ZahlenRechnerBereich.QUATERNION -> listOf(
                    "Die Faktorordnung bleibt erhalten; Multiplikation ist auf ${bereich.latex} nicht kommutativ.",
                )
                else -> emptyList()
            }
            assertEquals(erwartet, regeln.drop(1), bereich.name)
            assertTrue(regeln.first().startsWith("Definition: "))
        }
    }

    @Test
    fun `unbekannter Faktor wird nicht als ganzzahlig angenommen`() {
        val unbekannt = inferiereZahlenRechnerBereich(Variable("x"))
        val ganz = inferiereZahlenRechnerBereich(Variable("z"), GanzeZahlen)
        assertEquals(ZahlenRechnerBereich.UNBEKANNT, unbekannt)
        assertEquals(ZahlenRechnerBereich.GANZ, ganz)
        assertEquals(ZahlenRechnerBereich.UNBEKANNT, gemeinsamerZahlenRechnerBereich(listOf(ganz, unbekannt)))
        assertEquals(ZahlenRechnerBereich.UNBEKANNT, gemeinsamerZahlenRechnerBereich(listOf(unbekannt, ganz)))
    }

    @Test
    fun `kanonische fundamentale Zahlmengen werden strukturell inferiert`() {
        val fälle = listOf(
            NatürlicheZahlen to ZahlenRechnerBereich.NATUERLICH,
            NichtnegativeGanzeZahlenSemantik.menge to ZahlenRechnerBereich.NATUERLICH_MIT_NULL,
            GanzeZahlen to ZahlenRechnerBereich.GANZ,
            RationaleZahlen to ZahlenRechnerBereich.RATIONAL,
            ReelleZahlen to ZahlenRechnerBereich.REELL,
            KomplexeZahlen to ZahlenRechnerBereich.KOMPLEX,
            FundamentalerZahlbereich.QUATERNION.alsMenge() to ZahlenRechnerBereich.QUATERNION,
        )

        fälle.forEach { (menge, erwartet) ->
            assertEquals(erwartet, inferiereZahlenRechnerBereich(RationaleZahl.Eins, menge))
        }
    }
}
