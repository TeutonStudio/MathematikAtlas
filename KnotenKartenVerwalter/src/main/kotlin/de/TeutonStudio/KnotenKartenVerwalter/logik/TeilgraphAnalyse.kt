package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten

/** Eine Verbindung, die genau einen Knoten eines untersuchten Teilgraphs berührt. */
data class TeilgraphGrenzVerbindung(
    val verbindung: VerbindungDaten,
    val innererAnschluss: AnschlussVerweis,
    val äußererAnschluss: AnschlussVerweis,
)

/** Fachneutrale Zerlegung eines Knotensatzes in innere und grenzüberschreitende Kanten. */
data class TeilgraphGrenzen(
    val knoten: Set<KnotenId>,
    val innereVerbindungen: List<VerbindungDaten>,
    val eingänge: List<TeilgraphGrenzVerbindung>,
    val ausgänge: List<TeilgraphGrenzVerbindung>,
)

fun KartenDaten.analysiereTeilgraph(knotenIds: Set<KnotenId>): TeilgraphGrenzen {
    val vorhandeneIds = knoten.asSequence().map { it.id }.filter { it in knotenIds }.toCollection(linkedSetOf())
    val innere = mutableListOf<VerbindungDaten>()
    val eingänge = mutableListOf<TeilgraphGrenzVerbindung>()
    val ausgänge = mutableListOf<TeilgraphGrenzVerbindung>()

    verbindungen.forEach { verbindung ->
        val vonInnen = verbindung.von.knotenId in vorhandeneIds
        val zuInnen = verbindung.zu.knotenId in vorhandeneIds
        when {
            vonInnen && zuInnen -> innere += verbindung
            !vonInnen && zuInnen -> eingänge += TeilgraphGrenzVerbindung(
                verbindung = verbindung,
                innererAnschluss = verbindung.zu,
                äußererAnschluss = verbindung.von,
            )
            vonInnen && !zuInnen -> ausgänge += TeilgraphGrenzVerbindung(
                verbindung = verbindung,
                innererAnschluss = verbindung.von,
                äußererAnschluss = verbindung.zu,
            )
        }
    }
    return TeilgraphGrenzen(vorhandeneIds, innere, eingänge, ausgänge)
}
