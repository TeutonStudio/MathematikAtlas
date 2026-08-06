package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import kotlin.test.Test
import kotlin.test.assertEquals

class DokumentationsInhaltArtTest {
    @Test
    fun `mathbb typ wird als inline latex erkannt`() {
        val knoten = KnotenDaten(
            art = KonzeptKnotenArten.EINGANG,
            name = "Wertevorrat",
            parameter = mapOf("typ" to """A_{\mathbb{N}} \subseteq \mathbb{R}"""),
        )

        assertEquals(
            DokumentationsInhaltArt.INLINE_LATEX,
            dokumentationsInhaltArt(knoten, "typ", DokumentationsInhaltArt.INLINE_LATEX),
        )
    }

    @Test
    fun `expliziter text bleibt trotz latex aehnlicher zeichen text`() {
        val knoten = KnotenDaten(
            art = KonzeptKnotenArten.REGEL,
            name = "Hinweis",
            parameter = mapOf(
                "regel" to """Der Pfad \mathbb ist hier nur Dokumentation.""",
                "regelInhaltArt" to "text",
            ),
        )

        assertEquals(
            DokumentationsInhaltArt.TEXT,
            dokumentationsInhaltArt(knoten, "regel", DokumentationsInhaltArt.DISPLAY_LATEX),
        )
    }
}
