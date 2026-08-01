package de.TeutonStudio.KnotenKartenVerwalter.daten

data class KartenVerweis(val kartenId: KartenId, val version: Int)

data class KnotenDaten(
    val id: KnotenId = neueKnotenId(),
    val art: KnotenArtId,
    val name: String,
    val position: GraphPunkt = GraphPunkt(80f, 80f),
    val größe: GraphGröße = GraphGröße(),
    val anschlüsse: List<AnschlussDaten> = emptyList(),
    val parameter: Map<String, String> = emptyMap(),
    val eigenschaften: Map<String, KnotenEigenschaft> = emptyMap(),
    val kartenVerweis: KartenVerweis? = null,
    /** Versionsfeste Karten-Fallbacks für einzelne Eingänge; eine Edge besitzt stets Vorrang. */
    val eingangsKartenVerweise: Map<String, KartenVerweis> = emptyMap(),
)

data class VerbindungDaten(
    val id: VerbindungsId = neueVerbindungsId(),
    val von: AnschlussVerweis,
    val zu: AnschlussVerweis,
)

data class VisuelleKnotenGruppeDaten(
    val id: VisuelleGruppenId = neueVisuelleGruppenId(),
    val knotenIds: Set<KnotenId>,
)

data class KartenDaten(
    val id: KartenId = neueKartenId(),
    val name: String,
    val version: Int = 1,
    val erstelltAm: Long = System.currentTimeMillis(),
    val knoten: List<KnotenDaten> = emptyList(),
    val verbindungen: List<VerbindungDaten> = emptyList(),
    val visuelleGruppen: List<VisuelleKnotenGruppeDaten> = emptyList(),
    val ansicht: AnsichtsFenster = AnsichtsFenster.Standard,
    val archiviert: Boolean = false,
)
