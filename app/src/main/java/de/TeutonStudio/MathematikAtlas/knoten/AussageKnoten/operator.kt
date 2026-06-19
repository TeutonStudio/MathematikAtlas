package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.disjunktion
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren.konjunktion
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAnschlussDaten
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageEingang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik

typealias OperatorDaten = operator.AussageOperatorDatenBasis

class operator(
    override val graph: Graph,
    override val daten: OperatorDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
): GraphDatenObjektKnoten<OperatorDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: BildschirmPosition) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    class AussageOperatorDatenBasis(
        override val id: String,
        override val name: String = "Verknüpfung",
    ): GraphDatenKnoten, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = operator.KNOTEN_ART
//        override val anschlussCache: SnapshotStateMap<String, PullSystemDaten.PullDaten<*>> = erhalteCache().value
/*        override fun baueCache(
            ausgang: AussageAnschlussDaten,
            eingänge: List<AussageAnschlussDaten>
        ): PullSystemDaten.PullDaten<*> {
            eingänge.map { it.cache }
            TODO("Not yet implemented")
        }*/
        override var beweglich: Boolean = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableStateMapOf<String,Any>()
        override var position: KartenPosition = KartenPosition.Zero
        override var breite: Float = 0f
        override var tiefe: Float = 0f


        private fun eingangIdx() = anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }.maxBy { anschlussIdx[it.id] ?: 0 }.id.split("-").last().toInt()
        private fun eingangId() = listOf(id,"eingang",eingangIdx() + 1).joinToString("-")
        public fun eingang() = AussageAnschlussDaten(eingangId(), Kante.Links, Richtung.Eingang)

        init {
            val anschlussListe = listOf(
                eingang().apply { anschlussIdx[this.id] = eingangIdx() },
                eingang().apply { anschlussIdx[this.id] = eingangIdx() },
                AussageAnschlussDaten("$id-ausgang-0",Kante.Rechts, Richtung.Ausgang
                ),
            ).apply { forEach { it.apply { klasse = if (richtung == Richtung.Eingang) AussageEingang.ANSCHLUSS_ART else AussageAusgang.ANSCHLUSS_ART } } }

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

    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

/*    override val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<Aussage>> =
        mutableStateMapOf()*/

//    override val wertKlasse = Aussage::class

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

    public fun berechne(
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
    public fun Textzeile() {
        Button(
            onClick = {
                verknüpfung = verknüpfung.nächste()
                daten.data[OPERATOR_SCHLÜSSEL] = verknüpfung.name
//                cacheAnschlüsse.clear()
            },
        ) {
            Text(verknüpfung.anzeige)
        }
    }

    public companion object {
        const val KNOTEN_ART: KnotenArt = "operatorAussage"
        const val OPERATOR_SCHLÜSSEL = "aussagen-operator"
    }
}