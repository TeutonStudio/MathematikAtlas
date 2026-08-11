package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.SVG_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.SvgOperatorDefinition
import de.TeutonStudio.MathematikKnoten.SvgOperatoren
import de.TeutonStudio.MathematikKnoten.konfiguriereSvgKnoten
import de.TeutonStudio.MathematikRechenSystem.kern.SvgGrafik

internal object SvgKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[SVG_OPERATOR_PARAMETER]
        val operator = SvgOperatoren.alle.firstOrNull { it.id == operatorId } ?: SvgOperatoren.Dokument
        var operatorDialog by remember(knoten.id, operatorId) { mutableStateOf(false) }

        Text("SVG-Operator", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { operatorDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(operator.titel, modifier = Modifier.weight(1f))
            Text(operator.symbolLatex, style = MaterialTheme.typography.labelMedium)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(operator.kategorie, style = MaterialTheme.typography.labelLarge)
                Text(operator.beschreibung, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Der SVG-Eingang darf unverbunden bleiben; dann beginnt dieser Knoten einen neuen SVG-AST.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OperatorParameter(knoten, operator, aktionen)

        (ergebnis?.ausgaben?.get("svg")?.objekt as? SvgGrafik)?.let { grafik ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("SVG-Zwischenstand", style = MaterialTheme.typography.labelLarge)
                    Text("${grafik.elemente.size} sichtbare Elemente · ${grafik.definitionen.size} Definitionen")
                    Text(
                        "viewBox ${grafik.viewport.minX} ${grafik.viewport.minY} ${grafik.viewport.breite} ${grafik.viewport.höhe}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ergebnis?.warnungen.orEmpty().forEach { warnung ->
            Text(warnung, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (operatorDialog) {
            val einträge = SvgOperatoren.alle.map { definition ->
                RechnerOperatorAuswahlEintrag(
                    id = definition.id,
                    titel = definition.titel,
                    symbolLatex = definition.symbolLatex,
                    kategorie = definition.kategorie,
                    beschreibung = definition.beschreibung,
                    suchbegriffe = buildSet {
                        add("SVG")
                        addAll(definition.anschlüsse.map { it.name })
                    },
                    kandidat = konfiguriereSvgKnoten(knoten, definition.id),
                )
            }
            RechnerOperatorAuswahlDialog(
                familienTitel = "SVG",
                einträge = einträge,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag -> eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen) },
                schließen = { operatorDialog = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    operatorDialog = false
                },
                formelÖffnen = { _ -> },
            )
        }
    }

    @Composable
    private fun OperatorParameter(
        knoten: KnotenDaten,
        operator: SvgOperatorDefinition,
        aktionen: KnotenInspektorAktionen,
    ) {
        when (operator) {
            SvgOperatoren.Dokument -> {
                Abschnitt("ViewBox")
                SvgFeld("min X", knoten, "viewBoxMinX", aktionen)
                SvgFeld("min Y", knoten, "viewBoxMinY", aktionen)
                SvgFeld("Breite", knoten, "viewBoxBreite", aktionen)
                SvgFeld("Höhe", knoten, "viewBoxHöhe", aktionen)
                Abschnitt("Mathematischer Koordinatenraum")
                SvgFeld("x min", knoten, "xMin", aktionen)
                SvgFeld("x max", knoten, "xMax", aktionen)
                SvgFeld("y min", knoten, "yMin", aktionen)
                SvgFeld("y max", knoten, "yMax", aktionen)
                Abschnitt("Dokument")
                SvgFeld("SVG-Breite", knoten, "breite", aktionen)
                SvgFeld("SVG-Höhe", knoten, "höhe", aktionen)
                SvgFeld("Seitenverhältnis", knoten, "preserveAspectRatio", aktionen)
            }
            SvgOperatoren.Rechteck -> {
                Abschnitt("Abgerundete Ecken")
                SvgFeld("Radius X", knoten, "radiusX", aktionen)
                SvgFeld("Radius Y", knoten, "radiusY", aktionen)
            }
            SvgOperatoren.Polygon, SvgOperatoren.Linienzug -> {
                Abschnitt("Punkte")
                SvgFeld("Punktliste x,y", knoten, "punkte", aktionen, einzeilig = false)
                Text(
                    "Beispiel: -1,-1 1,-1 0,1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SvgOperatoren.Pfad -> {
                Abschnitt("Pfadsegmente")
                SvgFeld("M/L/C/Q/A/Z", knoten, "pfad", aktionen, einzeilig = false)
                Text(
                    "Die Eingabe wird in strukturierte Pfadsegmente geparst und nicht als XML-Fragment gespeichert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SvgOperatoren.Text -> {
                Abschnitt("Beschriftung")
                SvgFeld("Text / LaTeX", knoten, "text", aktionen, einzeilig = false)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Mathematisches LaTeX", modifier = Modifier.weight(1f))
                    Switch(
                        checked = knoten.parameter["mathematikLatex"]?.toBooleanStrictOrNull() ?: true,
                        onCheckedChange = { aktionen.parameter("mathematikLatex", it.toString()) },
                    )
                }
            }
            else -> Unit
        }
    }
}

internal object SvgStilKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Wiederverwendbarer SVG-Stil", style = MaterialTheme.typography.titleSmall)
        SvgFeld("Füllung", knoten, "füllung", aktionen)
        SvgFeld("Kontur", knoten, "kontur", aktionen)
        SvgFeld("Konturbreite", knoten, "konturBreite", aktionen)
        SvgFeld("Deckkraft", knoten, "deckkraft", aktionen)
        SvgFeld("Strichmuster", knoten, "strichMuster", aktionen)
        SvgFeld("Linienende", knoten, "linienEnde", aktionen)
        SvgFeld("Linienverbindung", knoten, "linienVerbindung", aktionen)
        Text(
            "Derselbe Stil-Ausgang kann mit beliebig vielen SVG-Ergänzungsknoten verbunden werden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Abschnitt(titel: String) {
    Text(titel, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun SvgFeld(
    label: String,
    knoten: KnotenDaten,
    schlüssel: String,
    aktionen: KnotenInspektorAktionen,
    einzeilig: Boolean = true,
) {
    OutlinedTextField(
        value = knoten.parameter[schlüssel].orEmpty(),
        onValueChange = { aktionen.parameter(schlüssel, it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = einzeilig,
        minLines = if (einzeilig) 1 else 2,
        maxLines = if (einzeilig) 1 else 5,
    )
}
