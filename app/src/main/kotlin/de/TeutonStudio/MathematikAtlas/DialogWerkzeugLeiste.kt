package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.RaumDimension
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration

internal enum class DialogWerkzeugId {
    KONZEPTBIBLIOTHEK,
    ZAHLENFORMEL,
    DIMENSIONSVISUALISIERUNG,
}

internal sealed interface WerkzeugVerfügbarkeit {
    data object Verfügbar : WerkzeugVerfügbarkeit
    data class Deaktiviert(val grund: String) : WerkzeugVerfügbarkeit
}

private enum class AktiverWerkzeugDialog { Formel, Dimension }

private data class Mengenausgang(
    val knoten: KnotenDaten,
    val anschluss: AnschlussDaten,
) {
    val ref get() = AnschlussVerweis(knoten.id, anschluss.id)
    val label get() = "${knoten.name} · ${anschluss.name}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogWerkzeugLeiste(
    zustand: AtlasZustand,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var aktiverDialog by remember { mutableStateOf<AktiverWerkzeugDialog?>(null) }
    val quellen = remember(zustand.editor.karte, zustand.editor.ausgewählteKnoten) {
        kompatibleMengenausgänge(zustand)
    }
    val dimensionVerfügbarkeit: WerkzeugVerfügbarkeit = if (quellen.isEmpty()) {
        WerkzeugVerfügbarkeit.Deaktiviert("Wähle mindestens einen Knoten mit einem Mengenausgang aus.")
    } else WerkzeugVerfügbarkeit.Verfügbar

    Surface(
        modifier = modifier.fillMaxHeight().width(56.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.fillMaxSize()) {
            VerticalDivider()
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DialogWerkzeugKnopf(
                    id = DialogWerkzeugId.KONZEPTBIBLIOTHEK,
                    name = "Konzeptbibliothek",
                    verfügbarkeit = WerkzeugVerfügbarkeit.Verfügbar,
                    onClick = {
                        aktiverDialog = null
                        KnotenWählerModusSpeicher(context).speichere(KnotenWählerModus.Konzeptbibliothek)
                        zustand.setzeSuchText("")
                        zustand.öffneKnotenAuswahl(zustand.dialogWerkzeugEinfügePosition())
                    },
                )
                DialogWerkzeugKnopf(
                    id = DialogWerkzeugId.ZAHLENFORMEL,
                    name = "CAS-/Zahlenformelbauer",
                    verfügbarkeit = WerkzeugVerfügbarkeit.Verfügbar,
                    onClick = {
                        zustand.schließeKnotenAuswahl()
                        aktiverDialog = AktiverWerkzeugDialog.Formel
                    },
                )
                DialogWerkzeugKnopf(
                    id = DialogWerkzeugId.DIMENSIONSVISUALISIERUNG,
                    name = "Dimensionsvisualisierung",
                    verfügbarkeit = dimensionVerfügbarkeit,
                    onClick = {
                        zustand.schließeKnotenAuswahl()
                        aktiverDialog = AktiverWerkzeugDialog.Dimension
                    },
                )
            }
        }
    }

    when (aktiverDialog) {
        AktiverWerkzeugDialog.Formel -> FormelBauerDialog(
            startLatex = "",
            schließen = { aktiverDialog = null },
            übernehmen = { latex ->
                val position = zustand.dialogWerkzeugEinfügePosition()
                val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(position)
                val knoten = konfiguriereZahlenRechnerFormel(basis, latex)
                zustand.editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
                zustand.editor.wähleKnoten(knoten.id)
                zustand.aktualisiereAuswertung()
                aktiverDialog = null
            },
        )

        AktiverWerkzeugDialog.Dimension -> DimensionsWerkzeugDialog(
            zustand = zustand,
            quellen = quellen,
            schließen = { aktiverDialog = null },
            übernehmen = { quelle, dimension ->
                fügeVisualisierungAtomarEin(zustand, quelle, dimension)
                aktiverDialog = null
            },
        )

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogWerkzeugKnopf(
    id: DialogWerkzeugId,
    name: String,
    verfügbarkeit: WerkzeugVerfügbarkeit,
    onClick: () -> Unit,
) {
    val enabled = verfügbarkeit is WerkzeugVerfügbarkeit.Verfügbar
    val beschreibung = when (verfügbarkeit) {
        WerkzeugVerfügbarkeit.Verfügbar -> name
        is WerkzeugVerfügbarkeit.Deaktiviert -> "$name. Nicht verfügbar: ${verfügbarkeit.grund}"
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(beschreibung) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = beschreibung
                if (!enabled) disabled()
            },
        ) {
            WerkzeugGlyph(id, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun WerkzeugGlyph(id: DialogWerkzeugId, modifier: Modifier = Modifier) {
    val farbe = LocalContentColor.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension / 11f
        when (id) {
            DialogWerkzeugId.KONZEPTBIBLIOTHEK -> {
                drawLine(farbe, Offset(w * .18f, h * .2f), Offset(w * .82f, h * .2f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .18f, h * .5f), Offset(w * .82f, h * .5f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .18f, h * .8f), Offset(w * .82f, h * .8f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .28f, h * .12f), Offset(w * .28f, h * .88f), stroke * .7f, StrokeCap.Round)
            }
            DialogWerkzeugId.ZAHLENFORMEL -> {
                drawLine(farbe, Offset(w * .14f, h * .3f), Offset(w * .38f, h * .5f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .38f, h * .5f), Offset(w * .14f, h * .72f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .5f, h * .72f), Offset(w * .86f, h * .72f), stroke, StrokeCap.Round)
                drawCircle(farbe, radius = stroke * .7f, center = Offset(w * .65f, h * .3f))
            }
            DialogWerkzeugId.DIMENSIONSVISUALISIERUNG -> {
                drawLine(farbe, Offset(w * .2f, h * .8f), Offset(w * .8f, h * .8f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .2f, h * .8f), Offset(w * .2f, h * .2f), stroke, StrokeCap.Round)
                drawLine(farbe, Offset(w * .2f, h * .8f), Offset(w * .68f, h * .35f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DimensionsWerkzeugDialog(
    zustand: AtlasZustand,
    quellen: List<Mengenausgang>,
    schließen: () -> Unit,
    übernehmen: (Mengenausgang, RaumDimension) -> Unit,
) {
    var quelle by remember(quellen) { mutableStateOf(quellen.firstOrNull()) }
    var dimension by remember { mutableStateOf(RaumDimension.R2) }
    val quelleNochVorhanden = quelle?.let { gewählt ->
        zustand.editor.karte.knoten.any { knoten ->
            knoten.id == gewählt.knoten.id && knoten.anschlüsse.any { it.id == gewählt.anschluss.id }
        }
    } == true

    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Dimensionsvisualisierung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quelle")
                quellen.forEach { kandidat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = quelle?.ref == kandidat.ref, onClick = { quelle = kandidat })
                        Text(kandidat.label)
                    }
                }
                HorizontalDivider()
                Text("Räumliche Darstellung")
                RaumDimension.entries.forEach { kandidat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = dimension == kandidat, onClick = { dimension = kandidat })
                        Text(kandidat.name)
                    }
                }
                Text(
                    "Die mathematische Menge bleibt unverändert; dieser Dialog konfiguriert nur den bestehenden Visualisierer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!quelleNochVorhanden) {
                    Text("Die gewählte Quelle ist nicht mehr vorhanden.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = quelle != null && quelleNochVorhanden,
                onClick = { quelle?.let { übernehmen(it, dimension) } },
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}

private fun kompatibleMengenausgänge(zustand: AtlasZustand): List<Mengenausgang> {
    val ids = zustand.editor.ausgewählteKnoten.ifEmpty { setOfNotNull(zustand.editor.ausgewählterKnoten) }
    return zustand.editor.karte.knoten
        .filter { it.id in ids }
        .flatMap { knoten ->
            knoten.anschlüsse
                .filter {
                    it.richtung == AnschlussRichtung.Ausgang &&
                        (it.art == MathematikAnschlussArten.Menge.id || MathematikAnschlussArten.Menge.id in it.zulässigeArten)
                }
                .map { Mengenausgang(knoten, it) }
        }
}

private fun AtlasZustand.dialogWerkzeugEinfügePosition(): GraphPunkt {
    val bereich = kartenDragZustand.editorBereich
    val ansicht = editor.karte.ansicht
    if (bereich == null) return GraphPunkt(180f, 140f)
    val dichte = kartenDragZustand.dichte.coerceAtLeast(.0001f)
    val faktor = (dichte * ansicht.zoom).coerceAtLeast(.0001f)
    val lokalX = bereich.width / 2f
    val lokalY = bereich.height / 2f
    return GraphPunkt(
        (lokalX - ansicht.verschiebung.x) / faktor - 135f,
        (lokalY - ansicht.verschiebung.y) / faktor - 72f,
    )
}

private fun fügeVisualisierungAtomarEin(
    zustand: AtlasZustand,
    quelle: Mengenausgang,
    dimension: RaumDimension,
) {
    val position = zustand.dialogWerkzeugEinfügePosition()
    val visualisierung = MathematikKnotenVorlagen.Visualisierung.erzeuge(position).copy(
        eigenschaften = VisualisierungsKonfiguration(dimension = dimension).zuEigenschaften(),
    )
    val eingang = visualisierung.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang && it.name == "menge" }
    val ziel = AnschlussVerweis(visualisierung.id, eingang.id)

    zustand.editor.beginneInteraktion()
    zustand.editor.führeAus(KartenAktion.KnotenEinfügen(visualisierung), mitHistorie = false)
    zustand.editor.beginneVerbindung(quelle.ref)
    val kompatibel = zustand.editor.kompatibelMitStart(ziel)
    zustand.editor.beendeVerbindungsVorschau(startBeibehalten = true)
    if (kompatibel) {
        zustand.editor.führeAus(
            KartenAktion.VerbindungEinfügen(VerbindungDaten(von = quelle.ref, zu = ziel)),
            mitHistorie = false,
        )
    }
    zustand.editor.beendeVerbindungsVorschau()
    zustand.editor.beendeInteraktion()
    zustand.editor.wähleKnoten(visualisierung.id)
    zustand.aktualisiereAuswertung()
}
