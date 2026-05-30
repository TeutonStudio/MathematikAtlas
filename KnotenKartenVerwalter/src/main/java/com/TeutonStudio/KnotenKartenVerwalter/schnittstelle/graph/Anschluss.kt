package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten

/**
 * Standardgröße und Außenabstand eines Anschlusses.
 *
 * Der Modifier wird von `Knoten.kt` erweitert, wenn ein Anschluss zusätzlich als
 * Drag-Startpunkt für Verbindungen dienen soll.
 */
val AnschlussModifier = Modifier.padding(vertical = 4.dp).size(10.dp)

internal fun anschlussModifierSkaliert(skalierung: Float): Modifier {
    val faktor = skalierung.coerceAtLeast(0.1f)
    return Modifier
        .padding(vertical = (4f * faktor).dp)
        .size((10f * faktor).dp)
}

/**
 * Rendert eine gemischte Anschlussliste als vertikale Anschluss-Spalte.
 */
@JvmName("list_of_connectdata_2_path")
@Composable
public fun List<AnschlussDaten>.zuPfad(istEingang: Boolean, modifier: (Int) -> Modifier = { AnschlussModifier }) = AnschlussSpalte(this,modifier,istEingang)

/**
 * Rendert Eingänge als linke Anschluss-Spalte.
 */
@JvmName("list_of_inputdata_2_path")
@Composable
public fun List<EingangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifier }) = (this as List<AnschlussDaten>).zuPfad(true,modifier)

/**
 * Rendert Ausgänge als rechte Anschluss-Spalte.
 */
@JvmName("list_of_outputdata_2_path")
@Composable
public fun List<AusgangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifier }) = (this as List<AnschlussDaten>).zuPfad(false,modifier)


/**
 * Rendert einen einzelnen Eingang.
 */
@Composable
public fun EingangDaten.zuPfad(modifier: Modifier = Modifier) = Eingang(this, modifier)

/**
 * Rendert einen einzelnen Ausgang.
 */
@Composable
public fun AusgangDaten.zuPfad(modifier: Modifier = Modifier) = Ausgang(this, modifier)

/**
 * Rendert einen beliebigen Anschluss anhand seiner Richtung.
 */
@Composable
public fun AnschlussDaten.zuPfad(modifier: Modifier = Modifier) = Anschluss(this,modifier)


sealed interface Anschluss: GraphObjekt {
    public val daten: AnschlussDaten
    public val besitzer: Knoten
    public var partner: Anschluss?
    public fun erlaubtVerbindung(daten: Anschluss): Boolean
}

open class BasisAnschluss(
    override val daten: AnschlussDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): Anschluss {
    @Composable
    override fun zuComposable(modifier: Modifier) {
        daten.zuPfad(modifier)
    }

    override fun erstelleVerbindung(
        von: Anschluss,
        zu: Anschluss,
    ) {
        if (this == von && erlaubtVerbindung(zu)) partner = zu
        if (this == zu && erlaubtVerbindung(von)) partner = von
    }

    override fun erlaubtVerbindung(daten: Anschluss): Boolean {
        return besitzer.daten.id != daten.besitzer.daten.id && this.daten.richtung != daten.daten.richtung
    }
}

open class BasisEingang(
    override val daten: EingangDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer, partner)

open class BasisAusgang(
    override val daten: AusgangDaten,
    override val besitzer: Knoten,
    override var partner: Anschluss? = null,
): BasisAnschluss(daten, besitzer, partner)


/**
 * Gemeinsames Anschluss-Rendering.
 *
 * Eingänge und Ausgänge unterscheiden sich nur in der Farbe. Die runde Form
 * macht den Anschluss als interaktiven Handle erkennbar.
 */
@Composable
private fun Anschluss(
    daten: AnschlussDaten,
    modifier: Modifier = Modifier,
) {
    val farbe = when (daten.richtung) {
        AnschlussRichtung.Eingang -> Color(0xFF2563EB)
        AnschlussRichtung.Ausgang -> Color(0xFF059669)
    }
    Box(
        modifier = modifier.background(farbe, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
    }
}

@Composable
private fun Eingang(
    daten: EingangDaten,
    modifier: Modifier = Modifier,
) = Anschluss(daten = daten, modifier = modifier)

@Composable
private fun Ausgang(
    daten: AusgangDaten,
    modifier: Modifier = Modifier,
) = Anschluss(daten = daten, modifier = modifier)

/**
 * Ordnet Anschlüsse gleichmäßig über die Höhe eines Knotens an.
 */
@Composable
private fun AnschlussSpalte(
    anschlüsse: List<AnschlussDaten>,
    modifier: (Int) -> Modifier = { Modifier.padding(vertical = 4.dp).size(10.dp) },
    istEingang: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        anschlüsse.forEachIndexed { idx, anschluss ->
            when {
                istEingang && anschluss is EingangDaten -> anschluss.zuPfad(modifier(idx))
                !istEingang && anschluss is AusgangDaten -> anschluss.zuPfad(modifier(idx))
            }
        }
    }
}
