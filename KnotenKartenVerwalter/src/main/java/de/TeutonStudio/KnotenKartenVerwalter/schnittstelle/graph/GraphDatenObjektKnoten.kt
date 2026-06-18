package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss.Companion.findeNachId
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.erzeugeAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.erzeugeVerbindung

interface GraphDatenObjektKnoten<D: GraphDatenKnoten>: GraphDatenObjekt<GraphDatenKnoten> {
    public abstract val besitzer: GraphDatenObjektKarte<*>
    public abstract val anschlussFabrik: AnschlussFabrik


    val anschlüsse get() = GraphCache(daten.anschlüsse) { d: GraphDatenAnschluss ->
        anschlussFabrik.erzeugeAnschluss(graph,d,this)?.apply { registriere() }
    }.erhalte()
    /** Aktualisiert den Knoten nach dem Erstellen oder Ändern einer Verbindung. */
    public fun definiereVerbindung()

    /** Positioniert und skaliert den Knoten innerhalb der Kartenebene. */
    @Composable public override fun Modifier.vorher(): Modifier = offset { daten.position.round() }.apply {
        if (daten is GraphDaten.orthogoneGD) size(with(LocalDensity.current) { daten.dimension.size.toDpSize() })
    }


    public override fun beiKlick(klickPos: Offset) {
//        besitzer.wähle(EinzelAuswahl(this))
//        besitzer.keinKontext()
    }

    public override fun beiHalten(klickPos: Offset) {
//        besitzer.ctx = daten.id to klickPos.zuBildAusKnoten()
//        besitzer.wähle(EinzelAuswahl(this))
    }

    public override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        if (daten is GraphDaten.bewegbareGD) besitzer.verschiebeKnoten(daten.id,panDelta)
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

    public fun KartenPosition.zuBildAusKnoten(): BildschirmPosition = (this + daten.position).round()

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