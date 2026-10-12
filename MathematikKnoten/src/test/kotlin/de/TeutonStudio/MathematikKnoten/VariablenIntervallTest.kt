package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VariablenIntervallTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun variable(parameter: Map<String, String>) =
        MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {
            it.copy(parameter = it.parameter + parameter)
        }

    @Test fun `intervall hat offene und geschlossene Grenzen`() {
        val k = variable(mapOf(
            VARIABLE_BEREICH_MODUS to VARIABLE_BEREICH_INTERVALL,
            VARIABLE_INTERVALL_LINKS to "0", VARIABLE_INTERVALL_RECHTS to "180",
            VARIABLE_INTERVALL_LINKS_OFFEN to "false", VARIABLE_INTERVALL_RECHTS_OFFEN to "false",
        ))
        val wert = register.finde(k.art)!!.auswerten(
            KnotenAuswertungsKontext(k, emptyMap(), RechenKontext())).ausgaben.getValue("wert")
        assertEquals(ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.von(180), false), wert.werteVorrat)
        assertEquals(wert.werteVorrat, wert.variablenQuellen.single().werteVorrat)
    }

    @Test fun `ungueltiger Intervallbereich wird verworfen`() {
        val k = variable(mapOf(
            VARIABLE_BEREICH_MODUS to VARIABLE_BEREICH_INTERVALL,
            VARIABLE_INTERVALL_LINKS to "360", VARIABLE_INTERVALL_RECHTS to "0",
        ))
        assertFailsWith<IllegalArgumentException> { variablenWertevorrat(k, ReelleZahlen) }
    }

    @Test fun `alte Variablen bleiben unveraendert reell`() {
        val k = variable(emptyMap())
        assertEquals(ReelleZahlen, variablenWertevorrat(k, ReelleZahlen))
    }
}
