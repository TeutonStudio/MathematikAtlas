package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

@Composable
fun InspektorEndlicheMengeEingänge(knoten:KnotenDaten,übernehmen:(KnotenDaten)->Unit) {
    val form=runCatching {prüfeEndlicheMengeForm(knoten)}
    if(form.isFailure) {Text(form.exceptionOrNull()?.message.orEmpty(),color=MaterialTheme.colorScheme.error);return}
    val modus=runCatching {endlicheMengeEingabe(knoten)}
    modus.exceptionOrNull()?.let {Text(it.message.orEmpty(),color=MaterialTheme.colorScheme.error)}
    val lokal=runCatching {konfiguriereEndlicheMenge(knoten,ENDLICHE_MENGE_LOKAL)}
    val graph=runCatching {konfiguriereEndlicheMenge(knoten,ENDLICHE_MENGE_GRAPH,1)}
    Text("Mengeneingabe",style=MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(selected=modus.getOrNull()==ENDLICHE_MENGE_LOKAL,
            enabled=lokal.isSuccess,onClick={lokal.getOrNull()?.let(übernehmen)},label={Text("Lokale Einträge")})
        FilterChip(selected=modus.getOrNull()==ENDLICHE_MENGE_GRAPH,
            enabled=modus.getOrNull()==ENDLICHE_MENGE_GRAPH || graph.isSuccess,
            onClick={if(modus.getOrNull()!=ENDLICHE_MENGE_GRAPH) graph.getOrNull()?.let(übernehmen)},
            label={Text("Graph-Elemente")})
    }
    Text("Der Wechsel zu Graph-Elementen beginnt mit einem Pflichtanschluss. Gespeicherte lokale Einträge bleiben für den Lokalmodus erhalten.",style=MaterialTheme.typography.bodySmall)
    if(lokal.isFailure) Text("Lokalmodus: ${lokal.exceptionOrNull()?.message}",color=MaterialTheme.colorScheme.error)
    if(graph.isFailure && modus.getOrNull()!=ENDLICHE_MENGE_GRAPH) Text("Graphmodus: ${graph.exceptionOrNull()?.message}",color=MaterialTheme.colorScheme.error)
    if(modus.getOrNull()!=ENDLICHE_MENGE_GRAPH) return
    var entwurf by remember(knoten.id,knoten.parameter[ENDLICHE_MENGE_ANZAHL]) {mutableStateOf(knoten.parameter[ENDLICHE_MENGE_ANZAHL].orEmpty())}
    val anzahl=entwurf.toIntOrNull()?.takeIf {it in 0..64}
    val kandidat=anzahl?.let {runCatching {konfiguriereEndlicheMenge(knoten,ENDLICHE_MENGE_GRAPH,it)}}
    OutlinedTextField(value=entwurf,onValueChange={entwurf=it},label={Text("Graph-Elemente (0 bis 64)")},
        supportingText={Text(if(anzahl==null) "Eine ganze Zahl zwischen 0 und 64 eingeben." else "Jeder Anschluss benötigt ein vollständiges mathematisches Objekt. Nur 0 ergibt ausdrücklich die leere Menge.")},
        isError=anzahl==null || kandidat?.isFailure==true,singleLine=true,modifier=Modifier.fillMaxWidth())
    kandidat?.exceptionOrNull()?.let {Text(it.message.orEmpty(),color=MaterialTheme.colorScheme.error)}
    OutlinedButton(enabled=kandidat?.isSuccess==true && entwurf!=knoten.parameter[ENDLICHE_MENGE_ANZAHL],
        onClick={kandidat?.getOrNull()?.let(übernehmen)}) {Text("Elementanzahl übernehmen")}
}
