package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageAnschlussDaten
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik

typealias AussageAuswerten = auswerten.AussageAuswertenDaten

class auswerten(
    override val graph: Graph,
    override val daten: definition.AussageDefinitionDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
    override val layoutCoordinates: MutableState<LayoutCoordinates?>,
) : GraphDatenObjektKnoten<definition.AussageDefinitionDaten> {
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
//    val cacheAnschlüsse: SnapshotStateMap<String, PullErgebnis<Aussage>> = mutableStateMapOf()
    override val anschlüsse: List<GraphDatenObjektAnschluss<*>> = emptyList()
    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    class AussageAuswertenDaten(
        override val id: GraphDatenId,
        override val name: String = "Auswerten",
    ): GraphDatenKnoten, GraphDatenKnoten.gerichteteGDK<AussageAnschlussDaten> {
        override var klasse: KnotenArt? = auswerten.KNOTEN_ART

        override var beweglich = true
        override val anschlussIdx = mutableStateMapOf<String,Int>()
        override val data = mutableMapOf<String,Any>()
        override var position = GraphPosition.Zero
        override var breite = 0f
        override var tiefe = 0f
        override val richtung = Richtung.Eingang
        override val anschlussLabel = mutableStateMapOf<Kante,Map<Int,String>>()
/*        fun anschlussKorrektur(a: EingangDaten) {
            super.anschlussKorrektur(a)
            a.klasse = AussageEingang.ANSCHLUSS_ART
            // TODO schlaueren Weg überlegen
        }*/

        override fun erhateAnschluss(
            idx: Int,
            kante: Kante,
            label: String,
        ): AussageAnschlussDaten {
            TODO("Not yet implemented")
        }


        init {
            anschlussLabel[Kante.Links] = mapOf(0 to "Aussage")
        }
    }

    private var anzeige by mutableStateOf("Nicht ausgewertet")

    /**
     * Dieser Knoten besitzt keinen Ausgang.
     */
/*    fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<Aussage>>,
    ): PullErgebnis<Aussage> =
        PullErgebnis.Fehler(
            "Der Auswerten-Knoten besitzt keinen Ausgang"
        )*/

    private fun werteAus() {
        val eingänge = daten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }

        val eingang = eingänge.singleOrNull()

        if (eingang == null) {
            anzeige = when {
                eingänge.isEmpty() -> "Kein Eingang vorhanden"
                else -> "Mehrere Eingänge vorhanden"
            }

            return
        }

/*        anzeige = when (
            val ergebnis = pullEingang(eingang.id)
        ) {
            is PullErgebnis.Fehler ->
                "Fehler: ${ergebnis.meldung}"

            is PullErgebnis.Wert -> {
                try {
                    when {
                        ergebnis.wert.istWahr() -> "Wahr"
                        ergebnis.wert.istLüge() -> "Lüge"
                        else -> "Unentscheidbar"
                    }
                } catch (fehler: Throwable) {
                    "Auswertung fehlgeschlagen: " + (fehler.message ?: fehler::class.simpleName)
                }
            }
        }*/
    }

    @Composable
    fun Textzeile() {
        Column {
            Text(anzeige)

/*            Button(
                onClick = ::werteAus,
            ) {
                Text("Auswerten")
            }*/
        }
    }

    @Composable
    fun Inspektor() {
        Card(Modifier.padding(25.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Inpektor: ${daten.name}")
                daten.anschlüsse.forEach {
                    Text("${it.label} an der Seite ${it.kante}")
                }

                Button(
                    onClick = ::werteAus,
                ) {
                    Text("Auswerten")
                }
            }
        }
    }

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "auswertenAussage"
    }
}