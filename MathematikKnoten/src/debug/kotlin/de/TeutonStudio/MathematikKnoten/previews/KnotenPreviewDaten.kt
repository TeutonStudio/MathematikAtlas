package de.TeutonStudio.MathematikKnoten.previews

import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen

internal object KnotenPreviewDaten {
    private val gruppen: Map<String, KnotenArtPreviewGruppe> =
        alleMathematikDefinitionsVorlagen()
            .groupBy { it.art }
            .mapValues { (art, varianten) ->
                KnotenArtPreviewGruppe(
                    art = art,
                    varianten = varianten.sortedWith(compareBy({ it.name }, { it.standardParameter.toSortedMap().toString() })),
                )
            }

    fun für(art: String): KnotenArtPreviewGruppe =
        requireNotNull(gruppen[art]) { "Keine Previewdaten für Knotenart $art registriert." }

    val arten: Set<String> get() = gruppen.keys
}
