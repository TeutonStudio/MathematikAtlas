package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsTabelle
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsTabellenBlock
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsTabellenSpalte
import kotlinx.coroutines.launch

@Composable
internal fun UmformungsTabellenAnsicht(
    tabelle: UmformungsTabelle,
    modifier: Modifier = Modifier,
) {
    var ausgewählterBlock by remember(tabelle) { mutableIntStateOf(tabelle.bloecke.lastIndex) }
    val listenZustand = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(ausgewählterBlock, tabelle) {
        listenZustand.animateScrollToItem(ausgewählterBlock)
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { ausgewählterBlock = (ausgewählterBlock - 1).coerceAtLeast(0) },
                enabled = ausgewählterBlock > 0,
            ) { Text("Zurück") }
            Text(
                "Zustand ${ausgewählterBlock + 1} von ${tabelle.bloecke.size}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
                onClick = { ausgewählterBlock = (ausgewählterBlock + 1).coerceAtMost(tabelle.bloecke.lastIndex) },
                enabled = ausgewählterBlock < tabelle.bloecke.lastIndex,
            ) { Text("Weiter") }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listenZustand) {
            itemsIndexed(tabelle.bloecke, key = { _, block -> block.schritt }) { index, block ->
                if (index > 0) HorizontalDivider(thickness = 2.dp)
                UmformungsTabellenBlockAnsicht(
                    spalten = tabelle.spalten,
                    block = block,
                    ausgewählt = index == ausgewählterBlock,
                    auswählen = {
                        ausgewählterBlock = index
                        scope.launch { listenZustand.animateScrollToItem(index) }
                    },
                )
            }
        }
    }
}

@Composable
private fun UmformungsTabellenBlockAnsicht(
    spalten: List<UmformungsTabellenSpalte>,
    block: UmformungsTabellenBlock,
    ausgewählt: Boolean,
    auswählen: () -> Unit,
) {
    val hintergrund = if (ausgewählt) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(hintergrund)
            .clickable(onClick = auswählen)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (block.schritt == 0) "Ausgangsmatrix" else "Nach Schritt ${block.schritt}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.widthIn(min = 420.dp)) {
                TabellenKopf(spalten)
                HorizontalDivider()
                block.zeilen.forEach { zeile ->
                    val zeilenFarbe = if (zeile.operation != null) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    }
                    Row(
                        modifier = Modifier.heightIn(min = 48.dp).background(zeilenFarbe),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.requiredWidth(190.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                zeile.operation?.zuKurztext().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                            )
                        }
                        Box(Modifier.requiredWidth(52.dp), contentAlignment = Alignment.Center) {
                            Text(zeile.name, fontWeight = FontWeight.SemiBold)
                        }
                        zeile.werte.forEachIndexed { index, wert ->
                            if (spalten[index].istRechteSeite && (index == 0 || !spalten[index - 1].istRechteSeite)) {
                                VerticalDivider(Modifier.height(48.dp).width(2.dp))
                            }
                            Box(
                                modifier = Modifier.requiredWidth(82.dp).padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LatexText(wert.zuLatex(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabellenKopf(spalten: List<UmformungsTabellenSpalte>) {
    Row(modifier = Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.requiredWidth(190.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            Text("Operation", fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.requiredWidth(52.dp), contentAlignment = Alignment.Center) {
            Text("Zeile", fontWeight = FontWeight.SemiBold)
        }
        spalten.forEachIndexed { index, spalte ->
            if (spalte.istRechteSeite && (index == 0 || !spalten[index - 1].istRechteSeite)) {
                VerticalDivider(Modifier.height(44.dp).width(2.dp))
            }
            Box(Modifier.requiredWidth(82.dp), contentAlignment = Alignment.Center) {
                LatexText(spalte.titel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
