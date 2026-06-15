package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.disjunktion
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.konjunktion
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageOperatorDaten


class operator(
    graph: Graph,
    daten: AussageOperatorDaten,
    besitzer: Karte,
) : BasisKnoten(
    graph = graph,
    daten = daten,
    besitzer = besitzer,
), PullSystem<Aussage> {
    enum class AussagenVerknüpfung(
        val anzeige: String,
    ) {
        UND("UND"),
        ODER("ODER");

        fun nächste(): AussagenVerknüpfung =
            when (this) {
                UND -> ODER
                ODER -> UND
            }
    }

    override val anschlussFabrik: AnschlussFabrik
        get() = MatheAnschlussFabrik

    override val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<Aussage>> =
        mutableStateMapOf()

    override val wertKlasse = Aussage::class

    private var verknüpfung by mutableStateOf(
        daten.data[OPERATOR_SCHLÜSSEL]
            ?.toString()
            ?.let { gespeicherterWert ->
                AussagenVerknüpfung.entries.firstOrNull {
                    it.name == gespeicherterWert
                }
            }
            ?: AussagenVerknüpfung.UND
    )

    override fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<Aussage>>,
    ): PullErgebnis<Aussage> {
        if (eingänge.isEmpty()) {
            return PullErgebnis.Fehler(
                "Aussagenoperator ${daten.id} besitzt keine Eingänge"
            )
        }

        val eingangsFehler = eingänge.values
            .filterIsInstance<PullErgebnis.Fehler>()
            .firstOrNull()

        if (eingangsFehler != null) {
            return eingangsFehler
        }

        val aussagen = eingänge.values.mapNotNull {
            (it as? PullErgebnis.Wert<Aussage>)?.wert
        }

        if (aussagen.size != eingänge.size) {
            return PullErgebnis.Fehler(
                "Nicht alle Eingänge von ${daten.id} liefern Aussagen"
            )
        }

        return try {
            /*
             * Das Prädikat wird sofort ausgewertet.
             *
             * Dadurch liefert dieser Knoten immer Wahr oder Lüge und
             * verschachtelte Operatoren laufen noch nicht in die
             * unvollständige Prädikat-Auswertung des Rechensystems.
             */
            val ergebnis = when (verknüpfung) {
                AussagenVerknüpfung.UND ->
                    konjunktion(
                        *aussagen.toTypedArray()
                    ).auswertung()

                AussagenVerknüpfung.ODER ->
                    disjunktion(
                        *aussagen.toTypedArray()
                    ).auswertung()
            }

            PullErgebnis.Wert(ergebnis)
        } catch (fehler: Throwable) {
            PullErgebnis.Fehler(
                meldung = "Aussagenoperator ${daten.id} konnte nicht " +
                        "ausgewertet werden: ${fehler.message}",
                ursache = fehler,
            )
        }
    }

    @Composable
    override fun Textzeile() {
        Button(
            onClick = {
                verknüpfung = verknüpfung.nächste()
                daten.data[OPERATOR_SCHLÜSSEL] = verknüpfung.name
                cacheAnschlüsse.clear()
            },
        ) {
            Text(verknüpfung.anzeige)
        }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "operatorAussage"
        const val OPERATOR_SCHLÜSSEL = "aussagen-operator"
    }
}