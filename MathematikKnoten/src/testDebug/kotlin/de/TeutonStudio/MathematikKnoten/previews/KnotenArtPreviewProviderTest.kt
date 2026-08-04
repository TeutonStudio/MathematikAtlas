package de.TeutonStudio.MathematikKnoten.previews

import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnotenArtPreviewProviderTest {
    @Test
    fun `Provider erzeugt genau eine Previewgruppe je Knotenart`() {
        val gruppen = KnotenArtPreviewProvider().values.toList()
        val vorlagen = alleMathematikDefinitionsVorlagen()

        assertEquals(vorlagen.map { it.art }.toSet(), gruppen.map { it.art }.toSet())
        assertEquals(gruppen.size, gruppen.map { it.art }.distinct().size)
        assertTrue(gruppen.all { it.varianten.isNotEmpty() })
    }

    @Test
    fun `jede registrierte Variante erscheint genau einmal im Previewkatalog`() {
        val previewVarianten = KnotenArtPreviewProvider().values
            .flatMap { it.varianten.asSequence() }
            .map { Triple(it.art, it.name, it.standardParameter.toSortedMap()) }
            .toList()
        val erwarteteVarianten = alleMathematikDefinitionsVorlagen()
            .map { Triple(it.art, it.name, it.standardParameter.toSortedMap()) }

        assertEquals(erwarteteVarianten.toSet(), previewVarianten.toSet())
        assertEquals(erwarteteVarianten.size, previewVarianten.size)
    }
}
