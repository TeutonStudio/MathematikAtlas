package de.TeutonStudio.MathematikKnoten.previews

import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnotenPreviewDateienTest {
    @Test
    fun `jede registrierte Knotenart besitzt genau eine physische Previewdatei`() {
        assertEquals(alleMathematikDefinitionsVorlagen().map { it.art }.toSet(), KnotenPreviewDaten.arten)
    }

    @Test
    fun `jede registrierte Variante erscheint genau einmal in den Previewdaten`() {
        val erwartet = alleMathematikDefinitionsVorlagen().map { Triple(it.art, it.name, it.standardParameter.toSortedMap()) }
        val tatsächlich = KnotenPreviewDaten.arten.flatMap { art ->
            KnotenPreviewDaten.für(art).varianten.map { Triple(it.art, it.name, it.standardParameter.toSortedMap()) }
        }
        assertEquals(erwartet.toSet(), tatsächlich.toSet())
        assertEquals(erwartet.size, tatsächlich.size)
        assertTrue(KnotenPreviewDaten.arten.isNotEmpty())
    }
}
