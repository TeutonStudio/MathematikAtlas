package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.istAusgang
import com.TeutonStudio.KnotenKartenVerwalter.istEingang

// Composables
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.Anschluss
import kotlin.collections.component1
import kotlin.collections.filter

/**
 * Standardgröße und Außenabstand eines Anschlusses.
 *
 * Der Modifier wird von `Knoten.kt` erweitert, wenn ein Anschluss zusätzlich als
 * Drag-Startpunkt für Verbindungen dienen soll.
 */
val AnschlussModifierStandard = Modifier
    .padding(vertical = 4.dp)
    .size(10.dp)



/**
 * Anschluss als Graphobjekt und Elternklasse aller Anschlüsse
 */
sealed interface Anschluss: GraphObjekt {
    public val daten: AnschlussDaten
    public val besitzer: Knoten
//    public var partner: Anschluss?

    public fun erhaltePosition(): KartenPosition {
        return TODO()
    }

    public fun erlaubtVerbindung(daten: Anschluss): Boolean
    public fun erstelleVerbindung(zu: Anschluss)

    public fun istSelbst(zielBesitzer: Knoten?): Boolean = (besitzer.daten.id == zielBesitzer?.daten?.id) ?: false
}

/**
 * Standard für Anschlüsse
 */
open class BasisAnschluss(
    override val daten: AnschlussDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): Anschluss {
    @Composable
    override fun zuComposable(modifier: Modifier) = Anschluss(daten,Color.Black,modifier)

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = !istSelbst(daten.besitzer)

    override fun erstelleVerbindung(zu: Anschluss) {
        TODO("Not yet implemented")
    }
}

/**
 * Ein Anschluss, der sich nur mit einem Ausgang verbinden lässt.
 * Wenn Verbindung bereits besteht, wird für die neue verbindung die alte gelöscht.
 */
open class BasisEingang(
    override val daten: EingangDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = super.erlaubtVerbindung(daten) && daten.istAusgang()
}

/**
 * Ein Anschluss, der sich nur mit Eingängen verbinden lässt
 */
open class BasisAusgang(
    override val daten: AusgangDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = super.erlaubtVerbindung(daten) && daten.istEingang()
}
