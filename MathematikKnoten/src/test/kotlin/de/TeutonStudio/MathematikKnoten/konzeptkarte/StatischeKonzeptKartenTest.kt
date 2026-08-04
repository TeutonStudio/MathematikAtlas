package de.TeutonStudio.MathematikKnoten.konzeptkarte

import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikKnoten.konzeptknoten.stabileVariantenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatischeKonzeptKartenTest {
    @Test
    fun `jede statische Definitionsvariante besitzt genau eine primaere Definition`() {
        val varianten = alleMathematikDefinitionsVorlagen().map { it.stabileVariantenId() }.toSet()
        assertTrue(varianten.isNotEmpty())
        varianten.forEach { variante ->
            val karten = StatischeKonzeptKarten.fürVariante(variante)
            assertTrue(karten.isNotEmpty(), "Keine statische Karte für $variante")
            assertEquals(1, karten.count { it.primär && it.rolle.name == "Definition" }, "Primärdefinition für $variante")
        }
        assertEquals(StatischeKonzeptKarten.alle.size, StatischeKonzeptKarten.alle.map { it.id }.distinct().size)
    }
}
