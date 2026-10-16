package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldungsArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*

class GeometryNodesVorschauRenderer(private val ergebnisFuer: (KnotenDaten) -> KnotenAuswertungsErgebnis?) : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.NurKopfzeileZiehbar
    @Composable
    override fun Inhalt(knoten: KnotenDaten,ausgewählt: Boolean,aktionen: KnotenRendererAktionen) {
        val ergebnis=ergebnisFuer(knoten)
        val geometrie=ergebnis?.ausgaben?.get("geometrie")?.objekt as? ProzeduraleGeometrie
        val render=remember(geometrie) { geometrie?.let { runCatching { geometryVorschau(it) } } }
        val meldungen = ergebnis.knotenMeldungen() + buildList {
            if (render?.getOrNull()?.begrenzt == true) add(KnotenMeldung("Vorschau begrenzt; Geometrie bleibt vollständig.", KnotenMeldungsArt.Warnung))
            render?.exceptionOrNull()?.message?.let { add(KnotenMeldung(it, KnotenMeldungsArt.Fehler)) }
            if (geometrie == null && ergebnis?.fehler == null) add(KnotenMeldung("Verbinde ein Geometry Set."))
        }
        SideEffect { aktionen.meldungenMelden(meldungen) }
        val ansicht=remember(render) { render?.getOrNull()?.let(::GeometryAnsicht) }
        var kamera by remember(knoten.id) { mutableStateOf(GeometryKamera()) }
        var verschieben by remember(knoten.id) { mutableStateOf(false) }
        var punkteZeigen by remember(knoten.id) { mutableStateOf(true) }
        var kantenZeigen by remember(knoten.id) { mutableStateOf(true) }
        val linienfarbe=MaterialTheme.colorScheme.primary;val punktfarbe=MaterialTheme.colorScheme.tertiary
        val hintergrund=MaterialTheme.colorScheme.surfaceVariant
        val textMeasurer=rememberTextMeasurer();val achsenStil=MaterialTheme.typography.labelSmall
        KnotenViewportInhalt(aktionen=aktionen,
            kopf={ Text(knoten.name,style=MaterialTheme.typography.titleMedium) },
            ansicht={ modifier ->
                Canvas(modifier.testTag("geometry-preview").clipToBounds()
                    .semantics { contentDescription="Geometry Nodes 3D-Vorschau; ein Finger dreht, zwei Finger verschieben und zoomen" }
                    .geometryNavigation(kamera,verschieben) { kamera=it }) {
                    drawRect(hintergrund)
                    val projektion=GeometryProjektion(kamera,size.width.toDouble(),size.height.toDouble())
                    val bild=ansicht?.let { projektion.alle(it) }.orEmpty()
                    fun offset(index: Int)=bild[index].let { Offset(it.x.toFloat(),it.y.toFloat()) }
                    if(kantenZeigen) ansicht?.kanten?.forEach { drawLine(linienfarbe,offset(it.a),offset(it.b),1.5.dp.toPx()) }
                    if(punkteZeigen) ansicht?.punktIndizes?.forEach { drawCircle(punktfarbe,2.5.dp.toPx(),offset(it)) }
                    // Orientierungshilfe, kein Koordinatenursprung der Geometrie.
                    val ursprung=Offset(40.dp.toPx(),size.height-40.dp.toPx())
                    val achsen=listOf(Triple("X",GeometrieVektor3(1.0,0.0,0.0),Color(0xffc62828)),Triple("Y",GeometrieVektor3(0.0,1.0,0.0),Color(0xff2e7d32)),Triple("Z",GeometrieVektor3(0.0,0.0,1.0),Color(0xff1565c0)))
                    for((name,vektor,farbe) in achsen) {
                        val r=projektion.richtung(vektor)
                        val ende=ursprung+Offset((r.x*25.dp.toPx()).toFloat(),(-r.y*25.dp.toPx()).toFloat())
                        drawLine(farbe,ursprung,ende,2.dp.toPx())
                        drawText(textMeasurer,name,ende,style=achsenStil.copy(color=farbe))
                    }
                }
            },
            fuß={ Column {
                if(geometrie!=null) Text("Direkt: ${geometrie.mesh?.punkte?.size ?: 0} Eckpunkte · ${geometrie.mesh?.flaechen?.size ?: 0} Flächen · ${geometrie.punktwolke?.punkte?.size ?: 0} Punkte · ${geometrie.instanzen.size} Instanzen",style=MaterialTheme.typography.labelSmall)
                Row {
                    TextButton(onClick={ kamera=kamera.zoomen(0.8) }) { Text("−") }
                    TextButton(onClick={ kamera=kamera.zoomen(1.25) }) { Text("+") }
                    TextButton(onClick={ kamera=kamera.einpassen() }) { Text("Einpassen") }
                    TextButton(onClick={ kamera=GeometryKamera();verschieben=false }) { Text("Standardansicht") }
                }
                Row {
                    TextButton(onClick={ kamera=GeometryKamera();verschieben=false }) { Text("3D") }
                    TextButton(onClick={ kamera=GeometryKamera.XY;verschieben=true }) { Text("XY") }
                    TextButton(onClick={ kamera=GeometryKamera.XZ;verschieben=true }) { Text("XZ") }
                    TextButton(onClick={ kamera=GeometryKamera.YZ;verschieben=true }) { Text("YZ") }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=!verschieben,onClick={ verschieben=false },label={ Text("Drehen") })
                    FilterChip(selected=verschieben,onClick={ verschieben=true },label={ Text("Verschieben") })
                    FilterChip(selected=punkteZeigen,onClick={ punkteZeigen=!punkteZeigen },label={ Text("Punkte") })
                    FilterChip(selected=kantenZeigen,onClick={ kantenZeigen=!kantenZeigen },label={ Text("Kanten") })
                }
            } },
        )
    }
}
