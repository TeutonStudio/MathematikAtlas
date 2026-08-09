package de.TeutonStudio.MathematikKnoten

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class TupelEinertupelTest {
    @Test
    fun `Elementmodus erlaubt genau einen festen Eingang`() {
        val basis = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val konfiguriert = konfiguriereTupel(
            basis.copy(parameter = basis.parameter + ("festeEingänge" to "1")),
            TUPEL_EINZEL_EINGABEN,
        )
        val eingänge = konfiguriert.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(1, eingänge.size)
        assertEquals("a", eingänge.single().name)
        assertEquals("1", konfiguriert.parameter["festeEingänge"])
    }
}
