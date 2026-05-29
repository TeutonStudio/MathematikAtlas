package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand

@Composable
public fun KarteZustand.zuComposable(daten: KarteDaten, onZoomRein: () -> Unit, onZoomRaus: () -> Unit, onZoomAufInhalt: () -> Unit) = Kontrolleiste(this,onZoomRein,onZoomRaus,onZoomAufInhalt)

@Composable
fun Kontrolleiste(
    zustand: KarteZustand,
    onZoomRein: () -> Unit,
    onZoomRaus: () -> Unit,
    onZoomAufInhalt: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KontrollKnopf("-", onZoomRaus)
            KontrollKnopf("+", onZoomRein)
            KontrollKnopf("[]", onZoomAufInhalt)
        }
    }
}

@Composable
private fun KontrollKnopf(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
