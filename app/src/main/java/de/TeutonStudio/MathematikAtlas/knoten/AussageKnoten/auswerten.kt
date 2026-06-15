package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageAuswertenDaten

class auswerten(
    graph: Graph,
    daten: AussageAuswertenDaten,
    besitzer: Karte,
) : BasisKnoten(
    graph = graph,
    daten = daten,
    besitzer = besitzer,
), PullSystem<Aussage> {
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val cacheAnschlüsse: SnapshotStateMap<String, PullErgebnis<Aussage>> = mutableStateMapOf()
    override val wertKlasse = Aussage::class

    private var anzeige by mutableStateOf("Nicht ausgewertet")

    /**
     * Dieser Knoten besitzt keinen Ausgang.
     */
    override fun berechne(
        ausgangId: String,
        eingänge: Map<String, PullErgebnis<Aussage>>,
    ): PullErgebnis<Aussage> =
        PullErgebnis.Fehler(
            "Der Auswerten-Knoten besitzt keinen Ausgang"
        )

    private fun werteAus() {
        val eingänge = daten.anschlüsse.filterIsInstance<EingangDaten>()

        val eingang = eingänge.singleOrNull()

        if (eingang == null) {
            anzeige = when {
                eingänge.isEmpty() -> "Kein Eingang vorhanden"
                else -> "Mehrere Eingänge vorhanden"
            }

            return
        }

        anzeige = when (
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
        }
    }

    @Composable
    override fun Textzeile() {
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
    override fun erhalteInspektor() {
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

    companion object {
        const val KNOTEN_ART: KnotenArt = "auswertenAussage"
    }
}