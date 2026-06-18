package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

//typealias Kante = AnschlussGraphDaten.AnschlussKante

sealed class Verbindung(
    override val graph: Graph,
    override val daten: VerbindungDaten,
): GraphVerbindungObjekt<VerbindungDaten> {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    /**
     * Erstellt das Kontextfenster dieser Verbindung.
     * Es wird von der Karte an der übergebenen Bildschirmposition geöffnet.
     *
     * @param pos Position des Kontextfensters im Bildschirmkoordinatenraum
     */
    @Composable
    public override fun KontextFenster(pos: BildschirmPosition) {
        Box(modifier = Modifier.offset { pos }.padding(vertical = 4.dp)) {
            Card() {
                Column(Modifier.padding(5.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("id: ${daten.id}",Modifier.scale(.9f),Color.Gray)
                    Text("löschen",Modifier.clickable() { graph.karte.vernichteVerbindung(this@Verbindung) })
                }
            }
        }
    }
}
