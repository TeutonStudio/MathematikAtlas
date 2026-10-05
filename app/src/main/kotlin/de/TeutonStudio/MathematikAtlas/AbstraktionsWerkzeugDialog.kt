package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikKnoten.abstraktion.AbstraktionsVorschlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AbstraktionsWerkzeugDialog(
    zustand: AtlasZustand,
    schließen: () -> Unit,
) {
    val abstände = LocalAtlasAbstände.current
    val revision = zustand.editor.auswertungsRevision
    var vorschläge by remember { mutableStateOf<List<AbstraktionsVorschlag>>(emptyList()) }
    var lädt by remember { mutableStateOf(true) }
    var fehler by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(revision) {
        lädt = true
        fehler = null
        vorschläge = runCatching {
            withContext(Dispatchers.Default) { zustand.analysiereAbstraktionen() }
        }.getOrElse { ursache ->
            fehler = ursache.message ?: "Die Karte konnte nicht auf Abstraktionen untersucht werden."
            emptyList()
        }
        lädt = false
    }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.9f).fillMaxHeight(.86f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = abstände.haarlinie,
        ) {
            Column(Modifier.fillMaxSize().padding(abstände.dialog), verticalArrangement = Arrangement.spacedBy(abstände.bereich)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Graph vereinfachen", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Nur strukturell bewiesene, kürzere Darstellungen werden angeboten.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                HorizontalDivider()
                when {
                    lädt -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    fehler != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(requireNotNull(fehler), color = MaterialTheme.colorScheme.error)
                    }
                    vorschläge.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Für die aktuelle Karte wurden keine kürzeren gleichwertigen Darstellungen gefunden.")
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(abstände.bereich)) {
                        items(vorschläge, key = AbstraktionsVorschlag::id) { vorschlag ->
                            AbstraktionsVorschlagsKarte(
                                vorschlag = vorschlag,
                                imGraphZeigen = { zustand.zeigeAbstraktion(vorschlag) },
                                ersetzen = {
                                    fehler = zustand.ersetzeDurchAbstraktion(vorschlag)
                                    if (fehler != null) {
                                        vorschläge = zustand.analysiereAbstraktionen()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AbstraktionsVorschlagsKarte(
    vorschlag: AbstraktionsVorschlag,
    imGraphZeigen: () -> Unit,
    ersetzen: () -> Unit,
) {
    val abstände = LocalAtlasAbstände.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(abstände.bereich), verticalArrangement = Arrangement.spacedBy(abstände.standard)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(vorschlag.titel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${vorschlag.ersparnis} Knoten weniger", color = MaterialTheme.colorScheme.primary)
            }
            vorschlag.vorherLatex?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            vorschlag.nachherLatex?.let { Text("→ $it", style = MaterialTheme.typography.bodyLarge) }
            Text(vorschlag.beschreibung, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Entfernt: ${vorschlag.ersetzungsPlan.entfernteKnoten.size} · Neu: ${vorschlag.ersetzungsPlan.neueKnoten.size} · ${vorschlag.vorherSchnittstelle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = imGraphZeigen) { Text("Im Graph zeigen") }
                Button(onClick = ersetzen) { Text("Ersetzen") }
            }
        }
    }
}
