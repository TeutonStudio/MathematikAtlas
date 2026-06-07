package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.filterKante
import com.TeutonStudio.KnotenKartenVerwalter.istHorizontal
import com.TeutonStudio.KnotenKartenVerwalter.istVertikal
import kotlin.collections.filterKeys
import kotlin.collections.maxBy

/**
 * Rendert eine gemischte Anschlussliste als vertikale Anschluss-Spalte.
 */
/*
@JvmName("list_of_connectdata_2_path")
@Composable
public fun List<AnschlussDaten>.zuPfad(istEingang: Boolean, modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = AnschlussSpalte(this,modifier,istEingang)
*/

/**
 * Positioniert Anschlüsse gleichmäßig an einer Knotenkante.
 */
@Composable
public fun Map<AnschlussDaten,Int>.zuLeiste(kante: AnschlussKante, modifier: AnschlussModifier) {
    val liste = this.filterKante(kante); if (liste.isEmpty()) return
    val maxIdx = liste.maxBy { it.value }.value
    val keyByIdx = { idx: Int -> liste.filterKeys { liste[it] == idx }.firstNotNullOfOrNull { it.key } }
    val anschlussListe = List(maxIdx) { keyByIdx(it) }.filterNotNull()
    if (kante.istVertikal()) {
        AnschlussSpalte(anschlussListe) { idx: Int -> modifier(anschlussListe[idx], idx) }
    } else if (kante.istHorizontal()) {
        AnschlussZeile(anschlussListe) { idx: Int -> modifier(anschlussListe[idx], idx) }
    } else {
        TODO()
    }
}

/**
 * Rendert Eingänge als linke Anschluss-Spalte.
 */
/*
@JvmName("list_of_inputdata_2_path")
@Composable
public fun List<EingangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = (this as List<AnschlussDaten>).zuPfad(true,modifier)
*/

/**
 * Rendert Ausgänge als rechte Anschluss-Spalte.
 */
/*
@JvmName("list_of_outputdata_2_path")
@Composable
public fun List<AusgangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = (this as List<AnschlussDaten>).zuPfad(false,modifier)
*/

/**
 * Zum Ausgleich des Zooms der Karte auf dem Graph
 */
internal fun Modifier.anschlussModifierSkaliert(skalierung: Float): Modifier {
    val faktor = skalierung.coerceAtLeast(0.1f)
    val vFaktor = faktor * 4f
    val sFaktor = faktor * 10f
    return this
        .padding(vertical = vFaktor.dp)
        .size(sFaktor.dp)
}


/**
 * Grundlegende Darstellung eines Anschlusses
 */
@Composable
public fun Anschluss(
    daten: AnschlussDaten,
    farbe: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(farbe, CircleShape),
        contentAlignment = Alignment.Center,
    ) { /* */ }
}

/**
 * Gemeinsames Anschluss-Rendering.
 *
 * Eingänge und Ausgänge unterscheiden sich nur in der Farbe. Die runde Form
 * macht den Anschluss als interaktiven Handle erkennbar.
 */
@Composable
public fun RichtungsAnschluss(
    daten: RichtungsAnschlussDaten,
    modifier: Modifier = Modifier,
) {
    Anschluss(
        daten, when (daten.richtung) {
            AnschlussRichtung.Eingang -> Color(0xFF2563EB)
            AnschlussRichtung.Ausgang -> Color(0xFF059669)
        }, modifier,

        )
}

@Composable
public fun Eingang(
    daten: EingangDaten,
    modifier: Modifier = Modifier,
) = RichtungsAnschluss(daten,modifier)

@Composable
public fun Ausgang(
    daten: AusgangDaten,
    modifier: Modifier = Modifier,
) = RichtungsAnschluss(daten,modifier)

/**
 * Ordnet Anschlüsse gleichmäßig über die Kante eines Knotens an.
 */
@Composable
public fun AnschlussSpalte(
    anschlüsse: List<AnschlussDaten>,
    modifier: (Int) -> Modifier = { Modifier.padding(vertical = 4.dp).size(10.dp) },
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        anschlüsse.forEachIndexed { idx, anschluss ->
            when {
                anschluss is EingangDaten -> Eingang(anschluss,modifier(idx))
                anschluss is AusgangDaten -> Ausgang(anschluss,modifier(idx))
            }
        }
    }
}

/**
 * Ordnet Anschlüsse gleichmäßig über die Kante eines Knotens an.
 */
@Composable
public fun AnschlussZeile(
    anschlüsse: List<AnschlussDaten>,
    modifier: (Int) -> Modifier = { Modifier.padding(vertical = 4.dp).size(10.dp) },
) {
    Row(
        modifier = Modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        anschlüsse.forEachIndexed { idx, anschluss ->
            when {
                anschluss is EingangDaten -> Eingang(anschluss,modifier(idx))
                anschluss is AusgangDaten -> Ausgang(anschluss,modifier(idx))
            }
        }
    }
}