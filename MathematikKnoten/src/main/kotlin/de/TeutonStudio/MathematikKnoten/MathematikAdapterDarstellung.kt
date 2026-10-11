package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Explizite fachliche Klassifikation; abweichende Anschlussarten allein genügen nicht. */
val MATHEMATISCHE_ADAPTER_ARTEN: Set<String> = setOf(
    ÜBERSETZER_ART,
    "mathematik.termZuMethode", "mathematik.tupelZuSpalte", "mathematik.tupelZuZeile",
    "mathematik.spalteZuTupel", "mathematik.zeileZuTupel", "mathematik.geometrie.zuTrägermenge",
)

internal fun KnotenVorlage.alsAdapterVorlage(): KnotenVorlage = copy(
    standardGröße = GraphGröße(220f, 96f),
    standardEigenschaften = standardEigenschaften + (KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")),
)

internal fun KartenDaten.klassifiziereAdapter(): KartenDaten = copy(knoten = knoten.map {
    if (it.art in MATHEMATISCHE_ADAPTER_ARTEN) it.copy(
        eigenschaften = it.eigenschaften + (KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")),
    ) else it
})
