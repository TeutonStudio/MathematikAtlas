package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogEintrag
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikKnoten.abstraktion.AbstraktionsVorschlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AbstraktionsWerkzeugDialog(
    zustand: AtlasZustand,
    schließen: () -> Unit,
) {

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

    AtlasDialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize()) {
            AtlasDialogKopf(
                titel = { Text("Graph vereinfachen") },
                untertitel = { Text(
                        "Nur strukturell bewiesene, kürzere Darstellungen werden angeboten.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) },
                aktionen = { TextButton(onClick = schließen) { Text("Schließen") } },
            )
            HorizontalDivider()
            when {
                lädt -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                fehler != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(requireNotNull(fehler), color = MaterialTheme.colorScheme.error)
                }
                vorschläge.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Für die aktuelle Karte wurden keine kürzeren gleichwertigen Darstellungen gefunden.")
                }
                else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = AtlasDialogAbstände.inhalt, verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder)) {
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

@Composable
private fun AbstraktionsVorschlagsKarte(
    vorschlag: AbstraktionsVorschlag,
    imGraphZeigen: () -> Unit,
    ersetzen: () -> Unit,
) {

    Card(Modifier.fillMaxWidth()) {
        AtlasDialogEintrag {
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
