package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektAnschluss.AussageAnschlussDaten
import de.TeutonStudio.MathematikAtlas.anschlüsse.AussageObjektEingang
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik

typealias AussageAuswerten = auswerten.AussageAuswertenDaten

class auswerten(
    override val graph: Graph,
    override val daten: AussageAuswerten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<AussageAuswerten>, GraphDatenObjektInspektor<AussageAuswerten> {
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite: Float get() = 200f
    override val minimaleTiefe: Float get() = 80f

    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    class AussageAuswertenDaten(
        override val id: GraphDatenId,
        override val name: String = "Auswerten",
    ): GraphDatenKnoten, GraphDatenKnoten.gerichteteGDK<AussageAnschlussDaten> {
        override var klasse: KnotenArt? = auswerten.KNOTEN_ART

        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String,Int>()
        override val data = mutableMapOf<String,Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(30f)
        override var tiefe by mutableFloatStateOf(12f)
        override val richtung = Richtung.Eingang
        override val anschlussLabel = mutableStateMapOf<Kante,Map<Int,String>>()

        override fun erhalteAnschluss(
            idx: Int,
            kante: Kante,
            label: String,
        ): AussageAnschlussDaten {
            return AussageAnschlussDaten("$id-eingang-$idx", kante, Richtung.Eingang).apply {
                this.label = label
                klasse = AussageObjektEingang.ANSCHLUSS_ART
            }
        }


        init {
            anschlussLabel[Kante.Links] = mapOf(0 to "Aussage")

            val eingang = erhalteAnschluss(0, Kante.Links, "Aussage")
            anschlüsse.add(eingang)
            anschlussIdx[eingang.id] = 0
        }

        fun hauptEingang(): GraphDatenAnschluss.auswertbarerGDA? =
            anschlüsse
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .firstOrNull { it.id == "$id-eingang-0" && it.istEingang }

        fun istUnbekannterEingang(anschluss: GraphDatenAnschluss): Boolean =
            anschluss.id.startsWith("$id-$UNBEKANNTER_EINGANG_MARKER-")

        fun aktualisiereUnbekannteEingänge(anzahl: Int) {
            val sichereAnzahl = anzahl.coerceAtLeast(0)
            val vorhandene = anschlüsse.filter(::istUnbekannterEingang)
            if (vorhandene.size == sichereAnzahl) return

            vorhandene.forEach {
                anschlussIdx.remove(it.id)
            }
            anschlüsse.removeAll(vorhandene.toSet())

            repeat(sichereAnzahl) { index ->
                val anschlussIndex = index + 1
                val anschluss = AussageAnschlussDaten(
                    "$id-$UNBEKANNTER_EINGANG_MARKER-$anschlussIndex",
                    Kante.Links,
                    Richtung.Eingang,
                ).apply {
                    label = "Unbekannt $anschlussIndex"
                    klasse = AussageObjektEingang.ANSCHLUSS_ART
                    cache = AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten(AussageWert.UNBEKANNT)
                }
                anschlüsse.add(anschluss)
                anschlussIdx[anschluss.id] = anschlussIndex
            }
        }
    }

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

    private fun aktuellerWert(): AussageWert {
        val eingang = daten.hauptEingang() ?: return AussageWert.UNBEKANNT

        return eingang
            .cache
            .let { it as? AussageObjektAnschluss.AussageAnschlussDaten.CacheDaten }
            ?.wert
            ?: AussageWert.UNBEKANNT
    }

    private fun anzeigeText(): String = when (aktuellerWert()) {
        AussageWert.UNENTSCHEIDBAR -> "Soviel Weisheit erlauben die Axiome nicht"
        else -> aktuellerWert().anzeige
    }

    @Composable
    fun Textzeile() {
        Column {
            Text(anzeigeText())
        }
    }

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier.matchParentSize()) {
            Column {
                Text(daten.name)
                LaTeXFormelText(
                    formel = besitzer.daten.latexFormelFuer(daten),
                    karte = besitzer.daten,
                )
                Textzeile()
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
            latexInfo("Formel", besitzer.daten, besitzer.daten.latexFormelFuer(daten)),
            info("Wert", anzeigeText()),
            info(
                "Anschlüsse",
                daten.anschlüsse.joinToString { "${it.label}: ${it.kante}" },
            ),
        ).Inhalt()
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "auswertenAussage"
        private const val UNBEKANNTER_EINGANG_MARKER = "unbekannt-eingang"
    }
}
