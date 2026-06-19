package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import kotlin.collections.set

typealias AussageDefinition = definition.AussageDefinitionDaten

class definition(
    override val graph: Graph,
    override val daten: AussageDefinitionDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
): GraphDatenObjektKnoten<AussageDefinition> {
    class AussageDefinitionDaten(
        initialWahr: Boolean = true,
        override val id: GraphDatenId,
        override val name: String = "Aussage",
        override var beweglich: Boolean,
        override val anschlüsse: SnapshotStateList<GraphDatenAnschluss>,
        override val anschlussIdx: SnapshotStateMap<String, Int>,
        override val data: MutableMap<String, Any>,
        override var position: KartenPosition,
        override var breite: Float,
        override var tiefe: Float,
        override val richtung: Richtung,
        override val anschlussLabel: SnapshotStateMap<Kante, Map<Int, String>>,
    ): GraphDatenKnoten, GraphDatenKnoten.gerichteteGDK<GraphDatenAnschluss>, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = definition.KNOTEN_ART
        override fun erhateAnschluss(
            idx: Int,
            kante: Kante,
            label: String
        ): GraphDatenAnschluss {
            TODO("Not yet implemented")
        }

        /*    fun anschlussKorrektur(a: AusgangDaten) {
                super.anschlussKorrektur(a)
                a.apply { klasse = AussageAusgang.ANSCHLUSS_ART }
            }*/

        init {
            anschlussLabel[Kante.Rechts] = mapOf(0 to "Aussage")

            data[definition.WERT_SCHLÜSSEL] = initialWahr
        }
    }

    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

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

    override val anschlussFabrik: AnschlussFabrik
        get() = MatheAnschlussFabrik

    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<Aussage>> =
        mutableStateMapOf()

    val wertKlasse = Aussage::class

    private var istWahr by mutableStateOf(
        daten.data[WERT_SCHLÜSSEL] as? Boolean ?: true
    )

    fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<Aussage>>,
    ): PullErgebnis<Aussage> {
        val aussage = if (istWahr) {
            Aussage.WAHR
        } else {
            Aussage.LÜGE
        }

        return PullErgebnis.Wert(aussage)
    }

    @Composable
    fun Textzeile() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (istWahr) "Wahr" else "Lüge"
            )

            Switch(
                checked = istWahr,
                onCheckedChange = { neuerWert ->
                    istWahr = neuerWert
                    daten.data[WERT_SCHLÜSSEL] = neuerWert

                    // Der Cache ist nur Anzeigezustand.
                    cacheAnschlüsse.clear()
                },
            )
        }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "eingabeAussage"
        const val WERT_SCHLÜSSEL = "aussage-ist-wahr"
    }
}