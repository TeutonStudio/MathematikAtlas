package de.TeutonStudio.KnotenKartenVerwalter.daten

data class KnotenVorlage(
    val art: KnotenArtId,
    val name: String,
    val kategorie: String,
    val beschreibung: String,
    val standardGröße: GraphGröße = GraphGröße(),
    val anschlüsse: List<AnschlussDaten>,
    val standardParameter: Map<String, String> = emptyMap(),
    val kartenVerweis: KartenVerweis? = null,
) {
    fun erzeuge(position: GraphPunkt): KnotenDaten = KnotenDaten(
        art = art,
        name = name,
        position = position,
        größe = standardGröße,
        anschlüsse = anschlüsse.map { it.copy(id = neueAnschlussId()) },
        parameter = standardParameter,
        kartenVerweis = kartenVerweis,
    )
}
