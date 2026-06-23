package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
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
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektInspektor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.info
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAusgang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.karten.AussageKarte
import kotlin.collections.set

typealias AussageDefinition = definition.AussageDefinitionDaten

class definition(
    override val graph: Graph,
    override val daten: AussageDefinitionDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
): GraphDatenObjektKnoten<AussageDefinition>, GraphDatenObjektInspektor<AussageDefinition> {
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite: Float get() = 200f
    override val minimaleTiefe: Float get() = 80f

    class AussageDefinitionDaten(
        initialWahr: Boolean = true,
        override val id: GraphDatenId,
        override val name: String = "Aussage",
    ): GraphDatenKnoten, GraphDatenKnoten.gerichteteGDK<GraphDatenAnschluss>, GraphDatenKnoten.auswertbarerGDK {
        override var klasse: KnotenArt? = definition.KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String,Int>()
        override val data = mutableMapOf<String,Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)
        override val richtung = Richtung.Ausgang
        override val anschlussLabel = mutableStateMapOf<Kante,Map<Int,String>>()
        override fun erhalteAnschluss(
            idx: Int,
            kante: Kante,
            label: String
        ): GraphDatenAnschluss {
            return AussageObjektAusgang.AussageAusgang(
                id = "$id-ausgang-$idx",
                kante = kante,
                richtung = Richtung.Ausgang,
            ).apply {
                this.label = label
                klasse = AussageObjektAusgang.ANSCHLUSS_ART
                cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(
                    AussageWert.ausBoolean(
                        data[definition.WERT_SCHLÜSSEL] as? Boolean ?: true
                    )
                )
            }
        }

        /*    fun anschlussKorrektur(a: AusgangDaten) {
                super.anschlussKorrektur(a)
                a.apply { klasse = AussageAusgang.ANSCHLUSS_ART }
            }*/

        init {
            anschlussLabel[Kante.Rechts] = mapOf(0 to "Aussage")
            data[definition.WERT_SCHLÜSSEL] = initialWahr

            val ausgang = erhalteAnschluss(0, Kante.Rechts, "Aussage")
            anschlüsse.add(ausgang)
            anschlussIdx[ausgang.id] = 0
        }
    }

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier) {
            Column {
                Text(daten.name)
                LaTeXFormelText(
                    formel = besitzer.daten.latexFormelFuer(daten),
                    karte = besitzer.daten,
                )
            }
        }
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }

    @Composable
    override fun BoxScope.Inspektor() {
        Composable()
    }

    @Composable
    override fun Inhalt() {
        BasisObjektKontext(
            auswahl(
                name = "LaTeX",
                wert = if (daten.latexRekursiv()) "rekursiv" else "implizit",
                optionen = listOf("rekursiv", "implizit"),
            ) { auswahl ->
                daten.setzeLatexRekursiv(auswahl == "rekursiv")
            },
            info("Formel", besitzer.daten.latexFormelFuer(daten)),
            auswahl(
                name = "Wert",
                wert = if (istWahr) "Wahr" else "Lüge",
                optionen = listOf("Wahr", "Lüge"),
            ) { auswahl ->
                setzeWert(auswahl == "Wahr")
            },
        ).Inhalt()
    }

    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    private var istWahr by mutableStateOf(
        daten.data[WERT_SCHLÜSSEL] as? Boolean ?: true
    )

    private fun setzeWert(neuerWert: Boolean) {
        istWahr = neuerWert
        daten.data[WERT_SCHLÜSSEL] = neuerWert
        daten.anschlüsse
            .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
            .filter { it.richtung == Richtung.Ausgang }
            .forEach {
                it.cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(
                    AussageWert.ausBoolean(neuerWert)
                )
            }
        (besitzer.daten as? AussageKarte.AussageKarteDaten)?.aktualisierePullCaches()
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
                    setzeWert(neuerWert)
                },
            )
        }
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "definitionAussage"
        const val WERT_SCHLÜSSEL = "aussage-ist-wahr"
    }
}
