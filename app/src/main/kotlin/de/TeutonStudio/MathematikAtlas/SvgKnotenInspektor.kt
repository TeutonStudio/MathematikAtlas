package de.TeutonStudio.MathematikAtlas

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.SVG_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.SvgErweiterteOperatoren
import de.TeutonStudio.MathematikKnoten.SvgOperatorAblauf
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
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(operator.kategorie, style = MaterialTheme.typography.labelLarge)
                Text(operator.beschreibung, style = MaterialTheme.typography.bodySmall)
                Text("Ablauf", style = MaterialTheme.typography.labelMedium)
                Text(
                    SvgOperatorAblauf.für(operator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Ein unbelegter SVG-Eingang beginnt bei SvgGrafik.standard(); jeder Operator gibt anschließend wieder den vollständigen AST aus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OperatorParameter(knoten, operator, aktionen)

        (ergebnis?.ausgaben?.get("svg")?.objekt as? SvgGrafik)?.let { grafik ->
            SvgZwischenstand(grafik, operator)
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
                    beschreibung = buildString {
                        append(definition.beschreibung)
                        append("\n\nAblauf: ")
                        append(SvgOperatorAblauf.für(definition))
                    },
                    suchbegriffe = buildSet {
                        add("SVG")
                        add(definition.kategorie)
                        addAll(definition.anschlüsse.map { it.name })
                        addAll(definition.standardParameter.keys)
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
    private fun SvgZwischenstand(grafik: SvgGrafik, operator: SvgOperatorDefinition) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("SVG-Zwischenstand", style = MaterialTheme.typography.labelLarge)
                Text("${grafik.elemente.size} sichtbare Elemente · ${grafik.definitionen.size} Definitionen")
                Text(
                    "viewBox ${grafik.viewport.minX} ${grafik.viewport.minY} ${grafik.viewport.breite} ${grafik.viewport.höhe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Koordinatenraum x=[${grafik.koordinatenraum.xMin}, ${grafik.koordinatenraum.xMax}], y=[${grafik.koordinatenraum.yMin}, ${grafik.koordinatenraum.yMax}]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val ids = grafik.elementIds().take(6)
                if (ids.isNotEmpty()) {
                    Text(
                        "IDs: ${ids.joinToString()}${if (grafik.elementIds().size > ids.size) " …" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("Vorschau nach: ${operator.titel}", style = MaterialTheme.typography.labelMedium)
                SvgVisualisierer(grafik)
            }
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
                Hinweis("Beispiel: -1,-1 1,-1 0,1")
            }
            SvgOperatoren.Pfad -> {
                Abschnitt("Pfadsegmente")
                SvgFeld("M/L/C/Q/A/Z", knoten, "pfad", aktionen, einzeilig = false)
                Hinweis("Die Eingabe wird in strukturierte Pfadsegmente geparst und nicht als XML-Fragment gespeichert.")
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
            else -> GenerischeOperatorParameter(knoten, operator, aktionen)
        }
    }

    @Composable
    private fun GenerischeOperatorParameter(
        knoten: KnotenDaten,
        operator: SvgOperatorDefinition,
        aktionen: KnotenInspektorAktionen,
    ) {
        val sichtbareSchlüssel = operator.standardParameter.keys.filterNot { it in VERSTECKTE_PARAMETER }
        if (sichtbareSchlüssel.isEmpty()) return
        Abschnitt("Operatorparameter")
        sichtbareSchlüssel.forEach { schlüssel ->
            when (schlüssel) {
                "mitZentrum" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Rotationszentrum verwenden", modifier = Modifier.weight(1f))
                        Switch(
                            checked = knoten.parameter[schlüssel]?.toBooleanStrictOrNull() ?: false,
                            onCheckedChange = { aktionen.parameter(schlüssel, it.toString()) },
                        )
                    }
                }
                "zentrumX", "zentrumY" -> if (knoten.parameter["mitZentrum"]?.toBooleanStrictOrNull() == true) {
                    SvgFeld(labelFür(schlüssel), knoten, schlüssel, aktionen)
                }
                else -> SvgFeld(
                    labelFür(schlüssel),
                    knoten,
                    schlüssel,
                    aktionen,
                    einzeilig = schlüssel !in MEHRZEILIGE_PARAMETER,
                )
            }
        }
        if ("zielId" in sichtbareSchlüssel) {
            Hinweis("Leere Ziel-ID bedeutet bei Transformation/Stil die oberste SVG-Ebene; Operatoren wie Entfernen oder Duplizieren verlangen eine konkrete ID.")
        }
        if (operator.kategorie == "Filter") {
            Hinweis("Filterattribute: key=value;key=value. Mehrere Filterprimitive mit derselben Definitions-ID werden in Reihenfolge an denselben Filter angehängt.")
        }
    }
}

@Composable
internal fun SvgVisualisierer(grafik: SvgGrafik, modifier: Modifier = Modifier) {
    val textFarbe = MaterialTheme.colorScheme.onSurface.zuCssFarbe()
    val svg = remember(grafik) { grafik.zuSvg() }
    val html = remember(svg, textFarbe) {
        """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
            <style>
              html, body { margin:0; padding:0; width:100%; height:100%; overflow:hidden; background:transparent; color:$textFarbe; }
              svg { display:block; width:100%; height:100%; }
            </style>
          </head>
          <body>$svg</body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(240.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        },
    )
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
        Hinweis("Derselbe Stil-Ausgang kann mit beliebig vielen SVG-Ergänzungs- und Bearbeitungsknoten verbunden werden.")
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
private fun Hinweis(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
        maxLines = if (einzeilig) 1 else 6,
    )
}

private fun labelFür(schlüssel: String): String = when (schlüssel) {
    "zielId" -> "Ziel-ID"
    "neueId" -> "Neue ID"
    "definitionId" -> "Definitions-ID"
    "text" -> "Text"
    "breite" -> "Breite"
    "höhe" -> "Höhe"
    "refX" -> "Referenz X"
    "refY" -> "Referenz Y"
    "markerBreite" -> "Markerbreite"
    "markerHöhe" -> "Markerhöhe"
    "orientierung" -> "Orientierung"
    "x1" -> "X₁"
    "y1" -> "Y₁"
    "x2" -> "X₂"
    "y2" -> "Y₂"
    "cx" -> "Mittelpunkt X"
    "cy" -> "Mittelpunkt Y"
    "radius" -> "Radius"
    "stopps" -> "Farbstopps"
    "attribute" -> "SVG-Attribute"
    "zentrumX" -> "Zentrum X"
    "zentrumY" -> "Zentrum Y"
    else -> schlüssel
}

private fun Color.zuCssFarbe(): String = "#%06X".format(toArgb() and 0x00FFFFFF)

private val VERSTECKTE_PARAMETER = setOf("referenzAttribut", "stilAttribut")
private val MEHRZEILIGE_PARAMETER = setOf("text", "stopps", "attribute")
