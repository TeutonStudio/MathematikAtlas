package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

internal const val NOTIZ_KNOTEN_ART = "karte.notiz"
internal const val NOTIZ_TEXT_PARAMETER = "text"
internal const val NOTIZ_AUSRICHTUNG_PARAMETER = "textAusrichtung"
internal const val NOTIZ_SCHRIFTGROESSE_PARAMETER = "schriftgrößeSp"

internal const val NOTIZ_SCHRIFTGROESSE_MIN = 8
internal const val NOTIZ_SCHRIFTGROESSE_MAX = 96
internal const val NOTIZ_SCHRIFTGROESSE_STANDARD = 16

/** Nichtmathematische, frei erzeugbare Werkzeuge für die Kartenoberfläche. */
internal object KartenWerkzeugVorlagen {
    val Notiz = KnotenVorlage(
        art = NOTIZ_KNOTEN_ART,
        name = "Notiz",
        kategorie = "Darstellung",
        beschreibung = "Freier Text für Hinweis, Kommentar und Dokumentation innerhalb einer Karte.",
        standardGröße = GraphGröße(280f, 160f),
        anschlüsse = emptyList(),
        standardParameter = mapOf(
            NOTIZ_TEXT_PARAMETER to "",
            NOTIZ_AUSRICHTUNG_PARAMETER to "links",
            NOTIZ_SCHRIFTGROESSE_PARAMETER to NOTIZ_SCHRIFTGROESSE_STANDARD.toString(),
        ),
    )

    val alle = listOf(Notiz)
    val nichtAuswertbareArten = setOf(NOTIZ_KNOTEN_ART)
}

internal fun notizSchriftgrößeSp(wert: String?): Int =
    wert?.toIntOrNull()?.takeIf { it in NOTIZ_SCHRIFTGROESSE_MIN..NOTIZ_SCHRIFTGROESSE_MAX }
        ?: NOTIZ_SCHRIFTGROESSE_STANDARD

internal fun notizTextAusrichtung(wert: String?): TextAlign = when (wert) {
    "rechts" -> TextAlign.End
    "zentriert" -> TextAlign.Center
    "blocksatz" -> TextAlign.Justify
    else -> TextAlign.Start
}

internal object NotizKnotenRenderer : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.GanzeFlächeZiehbar

    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ausgewählt: Boolean,
        aktionen: KnotenRendererAktionen,
    ) {
        Text(
            text = knoten.parameter[NOTIZ_TEXT_PARAMETER].orEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            fontSize = notizSchriftgrößeSp(knoten.parameter[NOTIZ_SCHRIFTGROESSE_PARAMETER]).sp,
            textAlign = notizTextAusrichtung(knoten.parameter[NOTIZ_AUSRICHTUNG_PARAMETER]),
            overflow = TextOverflow.Clip,
        )
    }
}

internal object NotizKnotenInspektor : KnotenInspektor {
    private val ausrichtungen = listOf(
        "links" to "Linksbündig",
        "zentriert" to "Zentriert",
        "rechts" to "Rechtsbündig",
        "blocksatz" to "Blocksatz",
    )

    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        OutlinedTextField(
            value = knoten.parameter[NOTIZ_TEXT_PARAMETER].orEmpty(),
            onValueChange = { aktionen.parameter(NOTIZ_TEXT_PARAMETER, it) },
            label = { Text("Text") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            minLines = 7,
        )

        Text("Ausrichtung", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ausrichtungen.chunked(2).forEach { zeile ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    zeile.forEach { (schlüssel, bezeichnung) ->
                        FilterChip(
                            selected = (knoten.parameter[NOTIZ_AUSRICHTUNG_PARAMETER] ?: "links") == schlüssel,
                            onClick = { aktionen.parameter(NOTIZ_AUSRICHTUNG_PARAMETER, schlüssel) },
                            label = { Text(bezeichnung) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        val schriftgröße = notizSchriftgrößeSp(knoten.parameter[NOTIZ_SCHRIFTGROESSE_PARAMETER])
        Text("Schriftgröße", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    aktionen.parameter(
                        NOTIZ_SCHRIFTGROESSE_PARAMETER,
                        (schriftgröße - 1).coerceAtLeast(NOTIZ_SCHRIFTGROESSE_MIN).toString(),
                    )
                },
                enabled = schriftgröße > NOTIZ_SCHRIFTGROESSE_MIN,
            ) { Text("−") }
            Text(
                "$schriftgröße sp",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(
                onClick = {
                    aktionen.parameter(
                        NOTIZ_SCHRIFTGROESSE_PARAMETER,
                        (schriftgröße + 1).coerceAtMost(NOTIZ_SCHRIFTGROESSE_MAX).toString(),
                    )
                },
                enabled = schriftgröße < NOTIZ_SCHRIFTGROESSE_MAX,
            ) { Text("+") }
        }
        Text(
            "${NOTIZ_SCHRIFTGROESSE_MIN}–${NOTIZ_SCHRIFTGROESSE_MAX} sp. Größe und Zeilenumbruch folgen der gezogenen Knotengröße.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
