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
    UNBEKANNT("Unbekannt"),
    UNENTSCHEIDBAR("Unentscheidbar");

    fun zuAussage(): Aussage? =
        when (this) {
            WAHR -> Aussage.WAHR
            LUEGE -> Aussage.LÜGE
            UNBEKANNT -> null
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

            val werte = eingänge.map {
                werteAussageAnschlussAus(knoten.id, it.id, besucht)
            }

            when (knoten.aussagenVerknüpfung()) {
                operator.AussagenVerknüpfung.UND -> when {
                    werte.any { it == AussageWert.LUEGE } -> AussageWert.LUEGE
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte.all { it == AussageWert.WAHR } -> AussageWert.WAHR
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                operator.AussagenVerknüpfung.ODER -> when {
                    werte.any { it == AussageWert.WAHR } -> AussageWert.WAHR
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte.all { it == AussageWert.LUEGE } -> AussageWert.LUEGE
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                operator.AussagenVerknüpfung.IMPLIKATION -> when {
                    werte.size < 2 -> AussageWert.UNBEKANNT
                    werte[0] == AussageWert.LUEGE -> AussageWert.WAHR
                    werte[1] == AussageWert.WAHR -> AussageWert.WAHR
                    werte[0] == AussageWert.WAHR && werte[1] == AussageWert.LUEGE -> AussageWert.LUEGE
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    else -> AussageWert.UNENTSCHEIDBAR
                }

                operator.AussagenVerknüpfung.KONTRAJUNKTION -> when {
                    werte.size < 2 -> AussageWert.UNBEKANNT
                    werte.any { it == AussageWert.UNBEKANNT } -> AussageWert.UNBEKANNT
                    werte[0] == AussageWert.UNENTSCHEIDBAR || werte[1] == AussageWert.UNENTSCHEIDBAR -> AussageWert.UNENTSCHEIDBAR
                    werte[0] != werte[1] -> AussageWert.WAHR
                    else -> AussageWert.LUEGE
                }

                operator.AussagenVerknüpfung.NEGATION -> when (werte.firstOrNull()) {
                    AussageWert.WAHR -> AussageWert.LUEGE
                    AussageWert.LUEGE -> AussageWert.WAHR
                    AussageWert.UNBEKANNT, null -> AussageWert.UNBEKANNT
                    else -> AussageWert.UNENTSCHEIDBAR
                }
            }
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
