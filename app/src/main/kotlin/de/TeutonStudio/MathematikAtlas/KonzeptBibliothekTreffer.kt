package de.TeutonStudio.MathematikAtlas

internal data class KonzeptHauptkategorieTreffer(
    val kategorie: KonzeptKategorie,
    val anzahl: Int,
)

internal val KonzeptBibliothekFilter.istAktiv: Boolean
    get() = suchtext.isNotBlank() || erforderlicherEingang != null || erforderlicherAusgang != null

internal fun hauptkategorieTreffer(
    kategorien: List<KonzeptKategorie>,
    einträge: List<KonzeptBibliothekEintrag>,
    filter: KonzeptBibliothekFilter,
): List<KonzeptHauptkategorieTreffer> {
    val fachgebietsÜbergreifenderFilter = filter.copy(kategoriePfad = null)
    val passendeEinträge = einträge.filter { it.passt(fachgebietsÜbergreifenderFilter) }

    return kategorien.map { kategorie ->
        KonzeptHauptkategorieTreffer(
            kategorie = kategorie,
            anzahl = passendeEinträge.count { eintrag ->
                eintrag.kategoriePfade.any { pfad -> pfad.firstOrNull() == kategorie.id }
            },
        )
    }.filter { treffer -> !filter.istAktiv || treffer.anzahl > 0 }
}
