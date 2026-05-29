package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

@Composable
public fun KnotenDaten.zuComposable(
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
) = Knoten(this, modifierKnoten, modifierAnschluss)

@Composable
private fun Knoten(
    daten: KnotenDaten,
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
) {
    val randFarbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF64748B)
    Box(
        modifier = modifierKnoten
            .border(1.dp, randFarbe, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = daten.name,
                    style = TextStyle(color = Color(0xFF0F172A)),
                )
                BasicText(
                    text = daten.typ,
                    style = TextStyle(color = Color(0xFF64748B)),
                )
            }
        }

        AnschlussSpalteAmRand(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .offset(x = (-5).dp),
        ) {
            daten.eingängeGeordnet.zuPfad { index ->
                modifierAnschluss(AnschlussRichtung.Eingang, index)
            }
        }

        AnschlussSpalteAmRand(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .offset(x = 5.dp),
        ) {
            daten.ausgängeGeordnet.zuPfad { index ->
                modifierAnschluss(AnschlussRichtung.Ausgang, index)
            }
        }
    }
}

@Composable
private fun AnschlussSpalteAmRand(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun KnotenPreview() {
    val daten = KnotenDaten(
        id = "knoten-1",
        name = "Ableitung",
        eingänge = listOf(EingangDaten("in", "Eingang")),
        ausgänge = listOf(AusgangDaten("out", "Ausgang")),
    )
    daten.zuComposable { _, _ -> AnschlussModifier }
}
