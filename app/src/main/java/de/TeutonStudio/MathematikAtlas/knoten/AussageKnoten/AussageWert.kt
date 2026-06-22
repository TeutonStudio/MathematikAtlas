package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.disjunktion
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.konjunktion
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung

enum class AussageWert(
    val anzeige: String,
) {
    WAHR("Wahr"),
    LUEGE("Lüge"),
    UNENTSCHEIDBAR("Unentscheidbar");

    fun zuAussage(): Aussage? =
        when (this) {
            WAHR -> Aussage.WAHR
            LUEGE -> Aussage.LÜGE
            UNENTSCHEIDBAR -> null
        }

    companion object {
        fun ausAussage(aussage: Aussage): AussageWert =
            when {
                aussage.istWahr() -> WAHR
                aussage.istLüge() -> LUEGE
                else -> UNENTSCHEIDBAR
            }

        fun ausBoolean(istWahr: Boolean): AussageWert =
            if (istWahr) WAHR else LUEGE
    }
}

fun GraphDatenKarte.werteAussageAnschlussAus(
    knotenId: GraphDatenId,
    anschlussId: GraphDatenId,
): AussageWert =
    werteAussageAnschlussAus(knotenId, anschlussId, emptySet())

private fun GraphDatenKarte.werteAussageAnschlussAus(
    knotenId: GraphDatenId,
    anschlussId: GraphDatenId,
    besucht: Set<Pair<GraphDatenId, GraphDatenId>>,
): AussageWert {
    val schlüssel = knotenId to anschlussId
    if (schlüssel in besucht) return AussageWert.UNENTSCHEIDBAR

    val knoten = knoten.find { it.id == knotenId }
        ?: return AussageWert.UNENTSCHEIDBAR

    val anschluss = knoten.anschlüsse.find { it.id == anschlussId }
        ?: return AussageWert.UNENTSCHEIDBAR

    if (anschluss is GraphDatenAnschluss.gerichteteGDA && anschluss.richtung == Richtung.Eingang) {
        val quelle = verbindungen
            .asSequence()
            .mapNotNull { verbindung ->
                verbindung.andereSeiteVon(knotenId, anschlussId)
            }
            .firstOrNull { (quellKnotenId, quellAnschlussId) ->
                val quellKnoten = this.knoten.find { it.id == quellKnotenId }
                val quellAnschluss = quellKnoten
                    ?.anschlüsse
                    ?.find { it.id == quellAnschlussId }
                quellAnschluss is GraphDatenAnschluss.gerichteteGDA &&
                        quellAnschluss.richtung == Richtung.Ausgang
            } ?: return AussageWert.UNENTSCHEIDBAR

        return werteAussageAnschlussAus(
            quelle.first,
            quelle.second,
            besucht + schlüssel,
        )
    }

    return werteAussageKnotenAus(knoten, besucht + schlüssel)
}

private fun GraphDatenKarte.werteAussageKnotenAus(
    knoten: GraphDatenKnoten,
    besucht: Set<Pair<GraphDatenId, GraphDatenId>>,
): AussageWert =
    when (knoten) {
        is AussageDefinition ->
            AussageWert.ausBoolean(
                knoten.data[definition.WERT_SCHLÜSSEL] as? Boolean ?: true
            )

        is OperatorDaten -> {
            val eingänge = knoten.anschlüsse
                .filterIsInstance<GraphDatenAnschluss.gerichteteGDA>()
                .filter { it.richtung == Richtung.Eingang }
                .sortedBy { knoten.anschlussIdx[it.id] ?: Int.MAX_VALUE }

            if (eingänge.size != 2) return AussageWert.UNENTSCHEIDBAR

            val links = werteAussageAnschlussAus(knoten.id, eingänge[0].id, besucht)
            val rechts = werteAussageAnschlussAus(knoten.id, eingänge[1].id, besucht)
            val linkeAussage = links.zuAussage()
            val rechteAussage = rechts.zuAussage()

            if (linkeAussage == null || rechteAussage == null) {
                return AussageWert.UNENTSCHEIDBAR
            }

            AussageWert.ausAussage(when (knoten.aussagenVerknüpfung()) {
                operator.AussagenVerknüpfung.UND ->
                    konjunktion(linkeAussage, rechteAussage).auswertung()

                operator.AussagenVerknüpfung.ODER ->
                    disjunktion(linkeAussage, rechteAussage).auswertung()
            })
        }

        else -> AussageWert.UNENTSCHEIDBAR
    }

private fun de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung.andereSeiteVon(
    knotenId: GraphDatenId,
    anschlussId: GraphDatenId,
): Pair<GraphDatenId, GraphDatenId>? =
    when {
        ids.knotenIdWeib == knotenId && ids.anschlussIdWeib == anschlussId ->
            ids.knotenIdMann to ids.anschlussIdMann

        ids.knotenIdMann == knotenId && ids.anschlussIdMann == anschlussId ->
            ids.knotenIdWeib to ids.anschlussIdWeib

        else -> null
    }
