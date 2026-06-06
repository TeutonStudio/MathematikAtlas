package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten

// Composables
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.Anschluss

/**
 * Standardgröße und Außenabstand eines Anschlusses.
 *
 * Der Modifier wird von `Knoten.kt` erweitert, wenn ein Anschluss zusätzlich als
 * Drag-Startpunkt für Verbindungen dienen soll.
 */
val AnschlussModifierStandard = Modifier.padding(vertical = 4.dp).size(10.dp)


typealias KnotenAschlüsse = Map<AnschlussDaten,Int>
public fun KnotenAschlüsse.filterKante(kante: AnschlussKante): KnotenAschlüsse = this.filter { (daten,idx) -> daten.kante == kante }

/**
 * Anschluss als Graphobjekt und Elternklasse aller Anschlüsse
 */
sealed interface Anschluss: GraphObjekt {
    public val daten: AnschlussDaten
    public val besitzer: Knoten
    public var partner: Anschluss?
    public fun erlaubtVerbindung(daten: Anschluss): Boolean
}

/**
 * Standard für Anschlüsse
 */
open class BasisAnschluss(
    override val daten: AnschlussDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): Anschluss {
    @Composable
    override fun zuComposable(modifier: Modifier) {
        Anschluss(daten,Color.Black,modifier)
    }

    override fun erstelleVerbindung(
        von: Anschluss,
        zu: Anschluss,
    ) {
        if (this == von && erlaubtVerbindung(zu)) partner = zu
        if (this == zu && erlaubtVerbindung(von)) partner = von
    }

    private fun istSelbst(zielBesitzer: Knoten?): Boolean {
        return (besitzer.daten.id == zielBesitzer?.daten?.id) ?: false
    }
    override fun erlaubtVerbindung(daten: Anschluss): Boolean {
        return !istSelbst(daten.besitzer)
    }
}

/**
 * Ein Anschluss, der sich nur mit einem Ausgang verbinden lässt.
 * Wenn Verbindung bereits besteht, wird für die neue verbindung die alte gelöscht.
 */
open class BasisEingang(
    override val daten: EingangDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer, partner) {

    private fun istAusgang(ziel: Anschluss): Boolean {
        return ziel is BasisAusgang
    }
    override fun erlaubtVerbindung(daten: Anschluss): Boolean {
        return super.erlaubtVerbindung(daten) && istAusgang(daten)
    }
}

/**
 * Ein Anschluss, der sich nur mit Eingängen verbinden lässt
 */
open class BasisAusgang(
    override val daten: AusgangDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer, partner) {

    private fun istEingang(ziel: Anschluss): Boolean {
        return ziel is BasisEingang
    }
    override fun erlaubtVerbindung(daten: Anschluss): Boolean {
        return super.erlaubtVerbindung(daten) && istEingang(daten)
    }
}
