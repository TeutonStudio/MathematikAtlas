package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogListenEintrag

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasBestätigungsDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import de.TeutonStudio.MathematikAtlas.knotenauswahl.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@Composable
internal fun KnotenAuswahlDialog(zustand: AtlasZustand, position: GraphPunkt) {
    val context = LocalContext.current
    val modusSpeicher = remember(context) { KnotenWählerModusSpeicher(context) }
    var modus by remember(modusSpeicher) { mutableStateOf(modusSpeicher.lade()) }
    val setzeModus: (KnotenWählerModus) -> Unit = { neu ->
        if(neu!=modus) zustand.beendeStandardSitzung(SitzungsAbschluss.MODUSWECHSEL)
        modus = neu
        modusSpeicher.speichere(neu)
        if (neu == KnotenWählerModus.Konzeptbibliothek) zustand.setzeSuchText("")
    }

    when (modus) {
        KnotenWählerModus.Standard -> AtlasBestätigungsDialog(
            inhaltScrolltSelbst = true,
            onDismissRequest = zustand::schließeKnotenAuswahl,
            title = { Text("Knoten einfügen") },
            untertitel = { Text(modus.beschreibung, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = {
                StandardKnotenAuswahlInhalt(zustand, position) {
                    KnotenWählerModusAuswahl(modus = modus, onModus = setzeModus)
                    HorizontalDivider()
                }
            },
            confirmButton = {
                TextButton(onClick = zustand::schließeKnotenAuswahl) { Text("Schließen") }
            },
        )

        KnotenWählerModus.Konzeptbibliothek -> KonzeptBibliothekDialog(
            zustand = zustand,
            position = position,
            vorlagen = zustand.sichtbareVorlagen(),
            onStandardWähler = { setzeModus(KnotenWählerModus.Standard) },
        )
    }
}

@Composable
internal fun KnotenWählerModusAuswahl(
    modus: KnotenWählerModus,
    onModus: (KnotenWählerModus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung)) {
        KnotenWählerModus.entries.forEach { eintrag ->
            FilterChip(
                selected = modus == eintrag,
                onClick = { onModus(eintrag) },
                label = { Text(eintrag.anzeigeName) },
            )
        }
    }
}

@Composable
private fun StandardKnotenAuswahlInhalt(zustand: AtlasZustand, position: GraphPunkt, kopf: @Composable () -> Unit) {
    val sitzung=zustand.standardSitzung ?: zustand.beginneStandardSitzung()
    val anzeige=zustand.standardErgebnis()
    val liste=rememberLazyListState()
    DisposableEffect(sitzung) {
        onDispose { sitzung?.let { zustand.knotenNutzung.schließen(it,null,SitzungsAbschluss.UNTERBROCHEN) } }
    }
    LaunchedEffect(sitzung,anzeige.zustand) {
        if(sitzung!=null) {
            zustand.knotenNutzung.aktualisieren(sitzung,anzeige.zustand)
            if(anzeige.zustand.originaltext.isNotBlank()) delay(SUCH_RUHE_MS)
            zustand.knotenNutzung.protokollieren(sitzung,anzeige.zustand,SuchAnlass.SUCHRUHE)
        }
    }
    LaunchedEffect(sitzung,anzeige.zustand) {
        if(sitzung!=null) snapshotFlow { liste.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet() }
            .distinctUntilChanged().collect { zustand.knotenNutzung.sichtbar(sitzung,anzeige.zustand,it) }
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(AtlasDialogAbstände.beschreibung)) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f,fill=false),state=liste,contentPadding=PaddingValues(0.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            item(key="kopf/modus") { kopf() }
            item(key="kopf/suche") {
                OutlinedTextField(value=zustand.suchText,onValueChange=zustand::setzeSuchText,label={ Text("Knoten suchen") },
                    supportingText={ Text("Name, Kategorie oder Beschreibung") },trailingIcon={
                        if(zustand.suchText.isNotEmpty()) IconButton(onClick={ zustand.setzeSuchText("") },modifier=Modifier.semantics { contentDescription="Suche zurücksetzen" }) {
                            Text("×",style=MaterialTheme.typography.titleLarge)
                        }
                    },modifier=Modifier.fillMaxWidth())
            }
            item(key="kopf/tabs") {
                PrimaryScrollableTabRow(selectedTabIndex=anzeige.tabs.indexOf(anzeige.aktiverTab),edgePadding=AtlasDialogAbstände.beschreibung) {
                    anzeige.tabs.forEach { tab -> Tab(selected=tab==anzeige.aktiverTab,onClick={ zustand.wähleStandardTab(tab.name) },enabled=tab.anzahl>0,text={ Text("${tab.name} (${tab.anzahl})") }) }
                }
            }
            items(anzeige.zeilen.size,key={ index -> anzeige.zeilen[index].eintrag?.schluessel ?: "kopf/gruppe/$index" }) { index ->
                val zeile=anzeige.zeilen[index]
                val eintrag=zeile.eintrag
                if(eintrag==null) Text(zeile.titel.orEmpty(),style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=AtlasDialogAbstände.beschreibung,start=4.dp))
                else AtlasDialogListenEintrag(headlineContent={ Text(eintrag.name) },supportingContent={ Text(eintrag.beschreibung) },
                    modifier=Modifier.standardKnotenEintragFläche().clickable(enabled=eintrag.einfügbar) {
                        sitzung?.let { zustand.wähleStandardEintrag(it,eintrag,position,anzeige) }
                    })
            }
            if(anzeige.aktiverTab.anzahl==0) item(key="kopf/leer") { Text("Keine passenden Knoten",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(AtlasDialogAbstände.eintragHorizontal)) }
        }
    }
}

@Composable
private fun Modifier.standardKnotenEintragFläche(): Modifier = clip(MaterialTheme.shapes.small)
    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    .border(1.dp,MaterialTheme.colorScheme.outlineVariant,MaterialTheme.shapes.small)
