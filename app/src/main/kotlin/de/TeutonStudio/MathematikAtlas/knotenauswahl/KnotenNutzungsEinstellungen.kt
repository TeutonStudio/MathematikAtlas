package de.TeutonStudio.MathematikAtlas.knotenauswahl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun KnotenNutzungsEinstellungen(dienst: KnotenNutzungsDienst) {
    val stand by dienst.stand.collectAsState()
    var löschenBestätigen by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Knotenauswahl-Historie",style=MaterialTheme.typography.titleLarge)
        Text("Die Standardknotenliste bleibt unverändert. Ihre Nutzungs- und Suchhistorie wird nur auf diesem Gerät gespeichert, außerhalb von Kartenexporten und Android-Backups.")
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Nutzung erfassen",Modifier.weight(1f))
            Switch(checked=stand.einstellungen.erfassen,onCheckedChange=dienst::erfassungÄndern,enabled=stand.bereit && !stand.geschützt,
                modifier=Modifier.semantics { contentDescription="Nutzung der Standardknotenliste erfassen" })
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Suchtexte speichern",Modifier.weight(1f))
            Switch(checked=stand.einstellungen.suchtexte,onCheckedChange=dienst::suchtexteÄndern,enabled=stand.bereit && !stand.geschützt,
                modifier=Modifier.semantics { contentDescription="Suchtexte lokal speichern" })
        }
        Text("Beim Abschalten der Suchtextspeicherung werden auch bereits gespeicherte Suchtexte entfernt. Reine Eintragszähler bleiben erhalten.",style=MaterialTheme.typography.bodySmall)
        Text("${stand.sitzungen} Sitzungen · ${stand.schritte} Suchschritte · ${stand.auswahlen} erfolgreiche Auswahlen")
        Text("Detailhistorie: höchstens 90 Tage und 10.000 Schritte; Datei höchstens 2 MiB. Lange Sitzungen, sichtbare Einträge und Suchtexte werden zusätzlich begrenzt.",style=MaterialTheme.typography.bodySmall)
        stand.fehler?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick={ löschenBestätigen=true },enabled=stand.bereit) { Text("Gesamte Knotenauswahl-Statistik löschen") }
    }
    if(löschenBestätigen) AlertDialog(onDismissRequest={ löschenBestätigen=false },title={ Text("Statistik löschen?") },
        text={ Text("Alle Sitzungen, Suchtexte und Eintragszähler werden lokal gelöscht. Karten bleiben erhalten.") },
        confirmButton={ TextButton(onClick={ dienst.löschen();löschenBestätigen=false }) { Text("Statistik löschen") } },
        dismissButton={ TextButton(onClick={ löschenBestätigen=false }) { Text("Abbrechen") } })
}
