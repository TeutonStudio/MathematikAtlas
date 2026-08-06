package de.TeutonStudio.MathematikKnoten.konzeptkarte

import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister
import de.TeutonStudio.MathematikKnoten.konzeptknoten.istPrimäreDefinitionFür
import de.TeutonStudio.MathematikKnoten.konzeptknoten.stabileVariantenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StatischeKonzeptKartenTest {
    @Test
    fun `jede Definitionsvariante besitzt genau eine primaere Definition`() {
        val vorlagen = alleMathematikDefinitionsVorlagen()
        val varianten = vorlagen.map { it.stabileVariantenId() }.toSet()
        val einträge = KonzeptKnotenRegister.erstelle(vorlagen)
        assertTrue(varianten.isNotEmpty())

        varianten.forEach { variante ->
            val eintrag = assertNotNull(
                einträge.singleOrNull { variante in it.varianten },
                "Kein eindeutiger Konzept-Wissenseintrag für $variante",
            )
            assertEquals(
                1,
                eintrag.karten.count { karte -> karte.istPrimäreDefinitionFür(variante) },
                "Primärdefinition für $variante",
            )
        }
    }

    @Test
    fun `statische Kartenassets besitzen eindeutige Kennungen`() {
        assertEquals(StatischeKonzeptKarten.alle.size, StatischeKonzeptKarten.alle.map { it.id }.distinct().size)
        StatischeKonzeptKarten.alle.forEach { asset ->
            assertTrue(asset.datei.isNotBlank(), "Leerer Assetpfad für ${asset.id}")
            assertTrue(asset.formatVersion > 0, "Ungültige Formatversion für ${asset.id}")
        }
    }
}
