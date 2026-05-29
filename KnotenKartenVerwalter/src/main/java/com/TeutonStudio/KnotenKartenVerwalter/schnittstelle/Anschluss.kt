package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

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

val AnschlussModifier = Modifier.padding(vertical = 4.dp).size(10.dp)

@JvmName("list_of_connectdata_2_path")
@Composable
public fun List<AnschlussDaten>.zuPfad(istEingang: Boolean, modifier: (Int) -> Modifier = { AnschlussModifier }) = AnschlussSpalte(this,modifier,istEingang)

@JvmName("list_of_inputdata_2_path")
@Composable
public fun List<EingangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifier }) = (this as List<AnschlussDaten>).zuPfad(true,modifier)

@JvmName("list_of_outputdata_2_path")
@Composable
public fun List<AusgangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifier }) = (this as List<AnschlussDaten>).zuPfad(false,modifier)


@Composable
public fun EingangDaten.zuPfad(modifier: Modifier = Modifier) = Eingang(this, modifier)

@Composable
public fun AusgangDaten.zuPfad(modifier: Modifier = Modifier) = Ausgang(this, modifier)

@Composable
public fun AnschlussDaten.zuPfad(modifier: Modifier = Modifier) = Anschluss(this,modifier)

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
