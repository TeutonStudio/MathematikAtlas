package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.EIGENSCHAFT_GELTUNG_PARAMETER
import de.TeutonStudio.MathematikKnoten.EIGENSCHAFT_PARAMETER
import de.TeutonStudio.MathematikKnoten.EIGENSCHAFT_STRENGE_PARAMETER
import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftRegister

internal data class AnalysisEigenschaftAuswahl(
    val wert: String,
    val titel: String,
    val zeigtGeltung: Boolean,
    val zeigtStrenge: Boolean,
)

internal object AnalysisEigenschaftInspektorModell {
    val eigenschaften: List<AnalysisEigenschaftAuswahl> = listOf(
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Minimum.id, "Minima", zeigtGeltung = true, zeigtStrenge = true),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Maximum.id, "Maxima", zeigtGeltung = true, zeigtStrenge = true),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Extremum.id, "Extrema", zeigtGeltung = true, zeigtStrenge = true),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Sattelpunkt.id, "Sattelstellen", zeigtGeltung = false, zeigtStrenge = false),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Konvexitaetsbereich.id, "Konvexitätsbereich", zeigtGeltung = false, zeigtStrenge = true),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Konkavitaetsbereich.id, "Konkavitätsbereich", zeigtGeltung = false, zeigtStrenge = true),
        AnalysisEigenschaftAuswahl(MathematischeEigenschaftRegister.Wendestelle.id, "Wendestellen", zeigtGeltung = false, zeigtStrenge = false),
    )

    val geltungen = listOf(
        InspektorAuswahlOption("lokal", "Lokal"),
        InspektorAuswahlOption("global", "Global"),
    )

    val strengen = listOf(
        InspektorAuswahlOption("nicht-streng", "Nicht streng"),
        InspektorAuswahlOption("streng", "Streng"),
    )

    fun eigenschaft(rohwert: String?): AnalysisEigenschaftAuswahl {
        val normalisiert = when (rohwert) {
            "minimum" -> MathematischeEigenschaftRegister.Minimum.id
            "maximum" -> MathematischeEigenschaftRegister.Maximum.id
            "extremum" -> MathematischeEigenschaftRegister.Extremum.id
            "sattelstelle" -> MathematischeEigenschaftRegister.Sattelpunkt.id
            "konvex" -> MathematischeEigenschaftRegister.Konvexitaetsbereich.id
            "konkav" -> MathematischeEigenschaftRegister.Konkavitaetsbereich.id
            "wendestelle" -> MathematischeEigenschaftRegister.Wendestelle.id
            else -> rohwert
        }
        return eigenschaften.firstOrNull { it.wert == normalisiert }
            ?: eigenschaften.first { it.wert == MathematischeEigenschaftRegister.Extremum.id }
    }
}

internal data class InspektorAuswahlOption(val wert: String, val titel: String)

@OptIn(ExperimentalMaterial3Api::class)
internal object AnalysisEigenschaftInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val rohwert = knoten.parameter[EIGENSCHAFT_PARAMETER]
        val eigenschaft = AnalysisEigenschaftInspektorModell.eigenschaft(rohwert)

        LaunchedEffect(knoten.id, rohwert, eigenschaft.wert) {
            if (rohwert != eigenschaft.wert) {
                aktionen.parameter(EIGENSCHAFT_PARAMETER, eigenschaft.wert)
            }
        }

        AuswahlDropdown(
            label = "Eigenschaft",
            wert = eigenschaft.wert,
            optionen = AnalysisEigenschaftInspektorModell.eigenschaften.map {
                InspektorAuswahlOption(it.wert, it.titel)
            },
            beiAuswahl = { aktionen.parameter(EIGENSCHAFT_PARAMETER, it) },
        )

        if (eigenschaft.zeigtGeltung) {
            AuswahlDropdown(
                label = "Geltung",
                wert = knoten.parameter[EIGENSCHAFT_GELTUNG_PARAMETER] ?: "lokal",
                optionen = AnalysisEigenschaftInspektorModell.geltungen,
                beiAuswahl = { aktionen.parameter(EIGENSCHAFT_GELTUNG_PARAMETER, it) },
            )
        }

        if (eigenschaft.zeigtStrenge) {
            AuswahlDropdown(
                label = "Strenge",
                wert = knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER] ?: "nicht-streng",
                optionen = AnalysisEigenschaftInspektorModell.strengen,
                beiAuswahl = { aktionen.parameter(EIGENSCHAFT_STRENGE_PARAMETER, it) },
            )
        }

        Text(
            when {
                eigenschaft.zeigtGeltung -> "Lokale oder globale Stellen sowie die Strenge werden ausdrücklich ausgewählt."
                eigenschaft.zeigtStrenge -> "Der Knotenausgang bleibt der lokale Bereich; nur die Strenge ist wählbar."
                else -> "Für diese Eigenschaft sind keine weiteren Auswahlparameter erforderlich."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuswahlDropdown(
    label: String,
    wert: String,
    optionen: List<InspektorAuswahlOption>,
    beiAuswahl: (String) -> Unit,
) {
    val aktuell = optionen.firstOrNull { it.wert == wert } ?: optionen.first()
    var geöffnet by remember(label, wert) { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = geöffnet,
        onExpandedChange = { geöffnet = it },
    ) {
        OutlinedTextField(
            value = aktuell.titel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = geöffnet,
            onDismissRequest = { geöffnet = false },
        ) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.titel) },
                    onClick = {
                        geöffnet = false
                        beiAuswahl(option.wert)
                    },
                )
            }
        }
    }
}
