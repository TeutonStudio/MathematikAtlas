package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.round
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeAnschluss

interface GraphDatenObjektKnoten<D: GraphDatenKnoten>: GraphDatenObjekt<D> {
    public abstract val besitzer: GraphDatenObjektKarte<*>
    public abstract val anschlussFabrik: AnschlussFabrik


    val anschlüsse get() = graph.anschlüsse.filter { it.besitzer.daten.id == daten.id }
    /** Aktualisiert den Knoten nach dem Erstellen oder Ändern einer Verbindung. */
    public fun definiereVerbindung()

    /** Positioniert und skaliert den Knoten innerhalb der Kartenebene. */
    @Composable public override fun Modifier.vorher(): Modifier =
        offset { daten.position.round() }

    @Composable public override fun Modifier.position(): Modifier =
        onGloballyPositioned {
            layoutCoordinates.value = it
            daten.breite = it.size.width.toFloat()
            daten.tiefe = it.size.height.toFloat()
        }


    public override fun beiKlick(klickPos: Offset) {
        besitzer.auswahl.wähleKnoten(daten.id)
        besitzer.ctx.objektDatenId = null
    }

    public override fun beiHalten(klickPos: Offset) {
        besitzer.auswahl.wähleKnoten(daten.id)
        besitzer.ctx.pos = klickPos.round()
        besitzer.ctx.objektDatenId = daten.id
    }

    public override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        besitzer.zustand.transformiere(panDelta,zoomDelta)
//        besitzer.verschiebeKnoten(daten.id, panDelta)
//        besitzer.wähle(EinzelAuswahl(this))
//        besitzer.keinKontext()
    }

    /** Prüft, ob der Knoten den sichtbaren Kartenbereich überschneidet. */
    public fun istImViewport(viewport: RectF = besitzer.zustand.erhalteViewportRect()): Boolean = RectF(
        daten.position.x,
        daten.position.y,
        daten.position.x + daten.breite,
        daten.position.y + daten.tiefe,
    ).overlaps(viewport)

//    public fun GraphPosition.zuBildAusKnoten(): BildschirmPosition = (this + daten.position).round()

    public companion object {
        public fun Iterable<GraphDatenObjektKnoten<*>>.sichtbar() = filter { it.istImViewport() }

        public fun <D: GraphDatenObjekt<*>> Iterable<D>.findeNachId(id: String) = find { it.daten.id == id }
        public fun Iterable<GraphDatenObjektKnoten<*>>.anschlussNachId(idKnoten: String, idAnschluss: String) = (findeNachId(idKnoten) as GraphDatenObjektKnoten<*>).anschlussNachId(idAnschluss)

        public fun GraphDatenObjektKnoten<*>.anschlussNachId(id: String) = anschlüsse.findeNachId(id)
        public fun Iterable<GraphDatenObjektKnoten<*>>.anschlüsseNachIDEhe(ids: GraphDatenVerbindung.IDEhe) =
            anschlussNachId(ids.knotenIdMann, ids.anschlussIdMann)?.let { aM ->
                anschlussNachId(ids.knotenIdWeib, ids.anschlussIdWeib)?.let { aW ->
                    aM to aW
                }
            }
    }
}
