package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KartenKnotenInspektor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val ref = knoten.kartenVerweis ?: return
    val referenziert = zustand.speicher.lade(ref)
    val versionen = zustand.kartenVersionen(ref.kartenId)
    val kartenOptionen = zustand.kartenKandidaten(knoten)
    var karteGeöffnet by remember(knoten.id, ref) { mutableStateOf(false) }
    var versionGeöffnet by remember(knoten.id, ref) { mutableStateOf(false) }
    val aktuellerZustand = knoten.kartenKnotenZustand()
    val verbindungen = zustand.editor.karte.verbindungen.count { it.von.knotenId == knoten.id || it.zu.knotenId == knoten.id }

    HorizontalDivider()
    Text("KartenKnoten", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = aktuellerZustand == KartenKnotenZustand.Schnittstelle,
            onClick = { zustand.setzeKartenKnotenZustand(knoten, KartenKnotenZustand.Schnittstelle) },
            label = { Text("Schnittstelle") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = aktuellerZustand == KartenKnotenZustand.Methode,
            onClick = { zustand.setzeKartenKnotenZustand(knoten, KartenKnotenZustand.Methode) },
            label = { Text("Methode") },
            modifier = Modifier.weight(1f),
        )
    }
    if (verbindungen > 0) {
        Text(
            "Ein Zustandswechsel entfernt $verbindungen bestehende ${if (verbindungen == 1) "Verbindung" else "Verbindungen"} atomar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    ExposedDropdownMenuBox(expanded = karteGeöffnet, onExpandedChange = { karteGeöffnet = it }) {
        OutlinedTextField(
            value = referenziert?.name ?: ref.kartenId.wert,
            onValueChange = {},
            readOnly = true,
            label = { Text("Karte") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = karteGeöffnet) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = karteGeöffnet, onDismissRequest = { karteGeöffnet = false }) {
            kartenOptionen.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.karte.name)
                            option.grund?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    enabled = option.kompatibel,
                    onClick = {
                        val ziel = zustand.kartenVersionen(option.karte.id)
                            .firstOrNull { zustand.prüfeKartenKandidat(knoten, it).kompatibel }
                        if (ziel != null) zustand.setzeKartenKnotenKarte(knoten, ziel)
                        karteGeöffnet = false
                    },
                )
            }
        }
    }

    ExposedDropdownMenuBox(expanded = versionGeöffnet, onExpandedChange = { versionGeöffnet = it }) {
        OutlinedTextField(
            value = "Version ${ref.version}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kartenversion") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionGeöffnet) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = versionGeöffnet, onDismissRequest = { versionGeöffnet = false }) {
            versionen.forEach { version ->
                val prüfung = zustand.prüfeKartenKandidat(knoten, version)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Version ${version.version}")
                            prüfung.grund?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    enabled = prüfung.kompatibel,
                    onClick = {
                        zustand.setzeKartenKnotenKarte(knoten, version)
                        versionGeöffnet = false
                    },
                )
            }
        }
    }

    val neueste = versionen.maxByOrNull { it.version }
    val updatePrüfung = neueste?.let { zustand.prüfeKartenKandidat(knoten, it) }
    Button(
        onClick = { neueste?.let { zustand.setzeKartenKnotenKarte(knoten, it) } },
        enabled = neueste != null && neueste.version > ref.version && updatePrüfung?.kompatibel == true,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (neueste != null && neueste.version > ref.version) "Auf Version ${neueste.version} aktualisieren" else "Aktuelle Version") }
    if (neueste != null && neueste.version > ref.version && updatePrüfung?.kompatibel == false) {
        Text(updatePrüfung.grund ?: "Die neueste Version ist nicht kompatibel.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    OutlinedButton(onClick = { zustand.öffne(ref) }, modifier = Modifier.fillMaxWidth()) { Text("Unterkarte öffnen") }
}
