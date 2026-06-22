package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
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
        val verbindung = verbindungen.firstOrNull {
            (it.ids.knotenIdWeib == knotenId && it.ids.anschlussIdWeib == anschlussId) ||
                    (it.ids.knotenIdMann == knotenId && it.ids.anschlussIdMann == anschlussId)
        } ?: return AussageWert.UNENTSCHEIDBAR

        val quellKnotenId: GraphDatenId
        val quellAnschlussId: GraphDatenId
        if (verbindung.ids.knotenIdWeib == knotenId && verbindung.ids.anschlussIdWeib == anschlussId) {
            quellKnotenId = verbindung.ids.knotenIdMann
            quellAnschlussId = verbindung.ids.anschlussIdMann
        } else {
            quellKnotenId = verbindung.ids.knotenIdWeib
            quellAnschlussId = verbindung.ids.anschlussIdWeib
        }

        return werteAussageAnschlussAus(
            quellKnotenId,
            quellAnschlussId,
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

            when (knoten.aussagenVerknüpfung()) {
                operator.AussagenVerknüpfung.UND ->
                    when {
                        links == AussageWert.LUEGE || rechts == AussageWert.LUEGE -> AussageWert.LUEGE
                        links == AussageWert.WAHR && rechts == AussageWert.WAHR -> AussageWert.WAHR
                        else -> AussageWert.UNENTSCHEIDBAR
                    }

                operator.AussagenVerknüpfung.ODER ->
                    when {
                        links == AussageWert.WAHR || rechts == AussageWert.WAHR -> AussageWert.WAHR
                        links == AussageWert.LUEGE && rechts == AussageWert.LUEGE -> AussageWert.LUEGE
                        else -> AussageWert.UNENTSCHEIDBAR
                    }
            }
        }

        else -> AussageWert.UNENTSCHEIDBAR
    }
