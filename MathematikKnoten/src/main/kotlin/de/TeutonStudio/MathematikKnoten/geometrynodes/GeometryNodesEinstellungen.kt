package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

/** Beide Plattformen übergeben den Kandidaten an ihre bestehende Konfigurations-/Undo-Planung. */
@Composable
fun GeometryNodesEinstellungen(knoten: KnotenDaten, uebernehmen: (KnotenDaten) -> Unit) {
    val definition = GeometryNodesVorlagen.finde(knoten.art) ?: return
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Geometry Nodes",style=MaterialTheme.typography.titleSmall)
        Text(definition.vorlage.beschreibung,style=MaterialTheme.typography.bodySmall)
        if(definition.parameter.isNotEmpty()) {
            var entwurf by remember(knoten.id,knoten.parameter) { mutableStateOf(definition.vorlage.standardParameter+knoten.parameter) }
            val kandidat = knoten.copy(parameter=entwurf)
            val fehler = pruefeGeometryNodesKonfiguration(kandidat)
            definition.parameter.forEach { p ->
                if(p.optionen.isEmpty()) OutlinedTextField(
                    value=entwurf.getValue(p.schluessel),onValueChange={ entwurf=entwurf+(p.schluessel to it) },
                    label={ Text(p.titel) },singleLine=true,modifier=Modifier.fillMaxWidth().testTag("geometry-param-${p.schluessel}"),
                ) else {
                    Text(p.titel,style=MaterialTheme.typography.labelMedium)
                    p.optionen.forEach { (wert,titel) ->
                        Row {
                            RadioButton(selected=entwurf[p.schluessel]==wert,onClick={ entwurf=entwurf+(p.schluessel to wert) })
                            TextButton(onClick={ entwurf=entwurf+(p.schluessel to wert) }) { Text(titel) }
                        }
                    }
                }
            }
            fehler?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            Button(onClick={ uebernehmen(kandidat) },enabled=fehler==null && kandidat.parameter != knoten.parameter,
                modifier=Modifier.testTag("geometry-apply")) { Text("Geometry Nodes übernehmen") }
        }
        Text("Felder: Mesh-Eckpunkte und Punktwolkenpunkte. Instanzen bei Punktoperationen vorher realisieren. Verbundene Felder ersetzen die Standardwerte.",style=MaterialTheme.typography.bodySmall)
    }
}
