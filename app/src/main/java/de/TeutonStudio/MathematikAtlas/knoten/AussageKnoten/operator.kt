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
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.BasisKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem as PullSystemGraph
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.PullSystem as PullSystemDaten
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageEingang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik


class operator(
    graph: Graph,
    daten: AussageOperatorDatenBasis,
    besitzer: Karte,
) : BasisKnoten(
    graph = graph,
    daten = daten,
    besitzer = besitzer,
), PullSystemGraph<Aussage> {
    class AussageOperatorDatenBasis(
        id: String,
        name: String = "Verknüpfung",
    ): BasisKnotenDaten<AussageAnschlussDaten>(
        id = id,
        name = name,
    ), PullSystemDaten<AussageAnschlussDaten> {
        override val anschlussCache: SnapshotStateMap<String, PullSystemDaten.PullDaten<*>> = erhalteCache().value
        override fun baueCache(
            ausgang: AussageAnschlussDaten,
            eingänge: List<AussageAnschlussDaten>
        ): PullSystemDaten.PullDaten<*> {
            eingänge.map { it.cache }
            TODO("Not yet implemented")
        }

        override var klasse: KnotenArt? = operator.KNOTEN_ART

        private fun eingangIdx() = anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.maxBy { anschlussIdx[it.id] ?: 0 }.id.split("-").last().toInt()
        private fun eingangId() = listOf(id,"eingang",eingangIdx() + 1).joinToString("-")
        public fun eingang() = AussageAnschlussDaten(eingangId(),AnschlussKante.Links,AnschlussRichtung.Eingang)

        init {
            val anschlussListe = listOf(
                eingang().apply { anschlussIdx[this.id] = eingangIdx() },
                eingang().apply { anschlussIdx[this.id] = eingangIdx() },
                AussageAnschlussDaten("$id-ausgang-0",AnschlussKante.Rechts, AnschlussRichtung.Ausgang
                ),
            ).apply { forEach { it.apply { klasse = if (richtung == AnschlussRichtung.Eingang) AussageEingang.ANSCHLUSS_ART else AussageAusgang.ANSCHLUSS_ART } } }

            anschlüsse.addAll(anschlussListe)
            data[operator.OPERATOR_SCHLÜSSEL] = operator.AussagenVerknüpfung.UND.name
        }
    }

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