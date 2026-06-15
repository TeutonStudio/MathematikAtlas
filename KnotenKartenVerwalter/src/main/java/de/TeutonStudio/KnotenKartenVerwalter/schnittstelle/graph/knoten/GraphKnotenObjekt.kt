package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import android.graphics.RectF
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten.Companion.erhalteSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss.Companion.findeNachId
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

/**
 * Vertrag für Knotenobjekte, die [KnotenAnschlussDaten] in einer [Karte] darstellen.
 * [Knoten] und [BasisKnoten] sind die vorgesehenen Erweiterungspunkte für konkrete Knoten.
 *
 * Ein Knoten verwaltet seine Anschlüsse über eine [AnschlussFabrik], beteiligt sich an Auswahl,
 * Verschiebung und Verbindungserstellung und liefert seine lokale Darstellung an die Kartenebene.
 */
interface GraphKnotenObjekt<K: KnotenAnschlussDaten<out AnschlussDaten>>: GraphDatenObjekt<K> {
    public abstract val besitzer: Karte
    public abstract val anschlussFabrik: AnschlussFabrik
    public val dimension get() = daten.erhalteSize()

    /** Aktualisiert den Knoten nach dem Erstellen oder Ändern einer Verbindung. */
    abstract fun definiereVerbindung()

    /** Positioniert und skaliert den Knoten innerhalb der Kartenebene. */
    @Composable override fun Modifier.vorher(): Modifier = offset { daten.position.round() }.size(with(LocalDensity.current) { dimension.toDpSize() })


    override fun beiKlick(klickPos: Offset) {
        besitzer.wähle(EinzelAuswahl(this))
        besitzer.keinKontext()
    }

    override fun beiHalten(klickPos: Offset) {
        besitzer.ctx = daten.id to klickPos.zuBildAusKnoten()
        besitzer.wähle(EinzelAuswahl(this))
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        if (daten.beweglich) besitzer.verschiebeKnoten(daten.id,panDelta)
        besitzer.wähle(EinzelAuswahl(this))
        besitzer.keinKontext()
    }

    /** Prüft, ob der Knoten den sichtbaren Kartenbereich überschneidet. */
    fun istImViewport(viewport: RectF = besitzer.zustand.erhalteViewportRect()): Boolean = RectF(
        daten.position.x,
        daten.position.y,
        daten.position.x + daten.breite,
        daten.position.y + daten.tiefe,
    ).overlaps(viewport)

    public fun KartenPosition.zuBildAusKnoten(): BildschirmPosition = (this + daten.position).round()

    public companion object {
        @Composable public fun Iterable<Knoten>.zuComposable(/*modifier: Modifier = Modifier*/) = forEach { it.zuComposable(/*modifier*/) }

        public fun Iterable<Knoten>.sichtbar() = filter { it.istImViewport() }

        public fun Iterable<Knoten>.findeNachId(id:String) = find { it.daten.id == id }
        public fun Iterable<Knoten>.anschlussNachId(idKnoten:String,idAnschluss:String) = findeNachId(idKnoten)?.anschlussNachId(idAnschluss)
        public fun Knoten.anschlussNachId(id:String) = anschlüsse.findeNachId(id)
        public fun Iterable<Knoten>.anschlüsseNachIDEhe(ids: IDEhe) =
            anschlussNachId(ids.knotenIdMann,ids.anschlussIdMann)?.let { aM ->
                anschlussNachId(ids.knotenIdWeib,ids.anschlussIdWeib)?.let { aW ->
                    aM to aW
                }
            }
    }
}
