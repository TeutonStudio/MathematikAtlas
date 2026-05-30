package com.TeutonStudio.MathematikAtlas.knoten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AuswertungsKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FormelKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.FunktionKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.MathematikEingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.RechenKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.UnbekannteKnoten

@Composable
fun MathematikInspector(
    knoten: KnotenDaten,
    karte: KarteDaten,
    onDataAendern: (String, Any) -> Unit,
) {
    fun stringWert(key: String): String = knoten.data[key]?.toString().orEmpty()

    when (knoten.art) {
        MathematikEingabeKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Wert", stringWert("wert")) { onDataAendern("wert", it) }
            ZahlenraumAuswahl(knoten.zahlenTyp().raum) { onDataAendern("zahlenTyp", knoten.zahlenTyp().copy(raum = it)) }
        }
        UnbekannteKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Variable", stringWert("variable").ifBlank { knoten.name }) { onDataAendern("variable", it) }
            ZahlenraumAuswahl(knoten.zahlenTyp().raum) { onDataAendern("zahlenTyp", knoten.zahlenTyp().copy(raum = it)) }
        }
        RechenKnoten.KNOTEN_ART -> {
            BasicText("Operator", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)))
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RechnenAtlasKnoten.operationen.forEach { op ->
                    InspectorKnopf(text = op, onClick = { onDataAendern("operator", op) }, modifier = Modifier.weight(1f))
                }
            }
        }
        FormelKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Formel", stringWert("formel")) { onDataAendern("formel", it) }
        }
        AuswertungsKnoten.KNOTEN_ART -> {
            BasicText(
                text = "Status: ${stringWert("status").ifBlank { "nicht ausgewertet" }}",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
            )
        }
        FunktionKnoten.KNOTEN_ART -> {
            BeschriftetesFeld("Referenz-Karten-ID", stringWert("kartenId")) { onDataAendern("kartenId", it) }
            val funktionName = stringWert("funktion").ifBlank { "Funktion" }
            BasicText(
                text = "$funktionName (${karte.knoten.count { it.art == UnbekannteKnoten.KNOTEN_ART }} lokale Unbekannte)",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF6B7280)),
            )
        }
        LOESEN_KNOTEN_ART -> {
            BeschriftetesFeld("Karten-IDs", stringWert("kartenIds")) { onDataAendern("kartenIds", it) }
            BeschriftetesFeld("Argumente", stringWert("argumente").ifBlank { "a,c,a" }) { onDataAendern("argumente", it) }
        }
    }

    val cache = karte.cache.eintrag(knoten.id)
    if (cache != null) {
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = if (cache.gueltig) "Cache: ${cache.daten.values.firstOrNull().orEmpty()}" else "Cache-Fehler: ${cache.fehler}",
            style = TextStyle(fontSize = 12.sp, color = if (cache.gueltig) Color(0xFF047857) else Color(0xFFB91C1C)),
        )
    }
}

@Composable
private fun BeschriftetesFeld(
    label: String,
    wert: String,
    onWertAendern: (String) -> Unit,
) {
    BasicText(
        text = label,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)),
    )
    Spacer(Modifier.height(4.dp))
    BasicTextField(
        value = wert,
        onValueChange = onWertAendern,
        textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF111827)),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ZahlenraumAuswahl(
    aktuellerRaum: Zahlenraum,
    onRaumAendern: (Zahlenraum) -> Unit,
) {
    BasicText(
        text = "Zahlenraum: ${aktuellerRaum.kurzform}",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280)),
    )
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "N" to Zahlenraum.Natuerlich,
            "Z" to Zahlenraum.Ganz,
            "Q" to Zahlenraum.Rational,
            "R" to Zahlenraum.Reell,
            "C" to Zahlenraum.Komplex,
        ).forEach { (label, raum) ->
            InspectorKnopf(text = label, onClick = { onRaumAendern(raum) }, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InspectorKnopf(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF2563EB), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
        )
    }
}
