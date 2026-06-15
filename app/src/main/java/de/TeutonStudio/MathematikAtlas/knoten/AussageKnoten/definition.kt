package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.EingabeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullErgebnis
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageDefinitionDaten

class definition(
    graph: Graph,
    daten: AussageDefinitionDaten,
    besitzer: Karte,
) : EingabeKnoten(
    graph = graph,
    daten = daten,
    besitzer = besitzer,
), PullSystem<Aussage> {

    override val anschlussFabrik: AnschlussFabrik
        get() = MatheAnschlussFabrik

    override val cacheAnschlüsse:
            SnapshotStateMap<String, PullErgebnis<Aussage>> =
        mutableStateMapOf()

    override val wertKlasse = Aussage::class

    private var istWahr by mutableStateOf(
        daten.data[WERT_SCHLÜSSEL] as? Boolean ?: true
    )

    override fun berechne(
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
    override fun Textzeile() {
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