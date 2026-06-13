package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt

sealed class Verbindung(
    override val graph: Graph,
    override val daten: VerbindungDaten,
): GraphVerbindungObjekt<VerbindungDaten> {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
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