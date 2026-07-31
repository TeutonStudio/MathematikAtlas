package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.AussagenOperatorArt
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.Wahrheitstabelle
import java.math.BigInteger

private val WAHRHEITSTABELLEN_SEITENGRÖSSE = BigInteger.valueOf(256)

@Composable
internal fun AussagenOperatorDialog(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    definitionÖffnen: () -> Unit,
    schließen: () -> Unit,
) {
    val direkteArt = AussagenOperatorArt.für(knoten)
    val istIteration = knoten.art == MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART
    if (direkteArt == null && !istIteration) return

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.88f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(knoten.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (istIteration) "Iterationsregel" else "Wahrheitstabelle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                HorizontalDivider()

                if (direkteArt != null) {
                    WahrheitstabellenInhalt(knoten, direkteArt, Modifier.weight(1f).fillMaxWidth())
                } else {
                    IterationsInhalt(knoten, Modifier.weight(1f).fillMaxWidth())
                }

                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = definitionÖffnen) { Text("Definition") }
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.dupliziereAuswahl()
                            schließen()
                        },
                    ) { Text("Knoten duplizieren") }
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.isoliereAusgewähltenKnoten()
                            schließen()
                        },
                    ) { Text("Knoten isolieren") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.löscheAuswahl()
                            schließen()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Knoten löschen") }
                }
            }
        }
    }
}

@Composable
private fun WahrheitstabellenInhalt(
    knoten: KnotenDaten,
    art: AussagenOperatorArt,
    modifier: Modifier,
) {
    val eingänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val schlüssel = eingänge.map { it.id.wert }
    val tabelle = remember(knoten.id, art, schlüssel) { Wahrheitstabelle(art, eingänge.size) }
    var seitenStart by remember(knoten.id, art, schlüssel) { mutableStateOf(BigInteger.ZERO) }
    val letzterStart = remember(tabelle.zeilenAnzahl) {
        tabelle.zeilenAnzahl.subtract(BigInteger.ONE)
            .divide(WAHRHEITSTABELLEN_SEITENGRÖSSE)
            .multiply(WAHRHEITSTABELLEN_SEITENGRÖSSE)
    }
    val zeilenAufSeite = tabelle.zeilenAnzahl.subtract(seitenStart)
        .min(WAHRHEITSTABELLEN_SEITENGRÖSSE).toInt()
    val zeilen = remember(tabelle, seitenStart) {
        List(zeilenAufSeite) { offset -> tabelle.zeile(seitenStart + BigInteger.valueOf(offset.toLong())) }
    }

    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LatexText(art.ergebnisLatex(eingänge.size), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Zeilen ${seitenStart + BigInteger.ONE}–${seitenStart + BigInteger.valueOf(zeilenAufSeite.toLong())} von ${tabelle.zeilenAnzahl}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { seitenStart = BigInteger.ZERO }, enabled = seitenStart > BigInteger.ZERO) { Text("Erste") }
            TextButton(
                onClick = { seitenStart = seitenStart.subtract(WAHRHEITSTABELLEN_SEITENGRÖSSE).max(BigInteger.ZERO) },
                enabled = seitenStart > BigInteger.ZERO,
            ) { Text("Zurück") }
            TextButton(
                onClick = { seitenStart = seitenStart.add(WAHRHEITSTABELLEN_SEITENGRÖSSE).min(letzterStart) },
                enabled = seitenStart < letzterStart,
            ) { Text("Weiter") }
            TextButton(onClick = { seitenStart = letzterStart }, enabled = seitenStart < letzterStart) { Text("Letzte") }
        }

        Column(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            Row {
                eingänge.indices.forEach { index -> TabellenZelle("A${index + 1}", kopf = true) }
                TabellenZelle("Ergebnis", kopf = true, breite = 120.dp)
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxHeight()) {
                items(zeilen, key = { it.index.toString() }) { zeile ->
                    Row {
                        zeile.eingänge.forEach { wert -> TabellenZelle(if (wert) "W" else "L") }
                        TabellenZelle(if (zeile.ergebnis) "W" else "L", breite = 120.dp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TabellenZelle(text: String, kopf: Boolean = false, breite: androidx.compose.ui.unit.Dp = 72.dp) {
    Box(
        Modifier.width(breite).padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = if (kopf) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun IterationsInhalt(knoten: KnotenDaten, modifier: Modifier) {
    val operator = knoten.parameter["operator"]
    val (formel, neutralesElement, erklärung) = when (operator) {
        "konjunktion" -> Triple(
            "\\bigwedge_{idx \\in \\Set{A}} methode(idx)",
            "\\top",
            "Wahr genau dann, wenn alle von der Methode erzeugten Aussagen wahr sind.",
        )
        "disjunktion" -> Triple(
            "\\bigvee_{idx \\in \\Set{A}} methode(idx)",
            "\\bot",
            "Wahr, sobald mindestens eine von der Methode erzeugte Aussage wahr ist.",
        )
        "adjunktion" -> Triple(
            "\\stackrel{\\circ}{\\bigvee}_{idx \\in \\Set{A}} methode(idx)",
            "\\bot",
            "Wahr genau dann, wenn eine ungerade Anzahl der erzeugten Aussagen wahr ist.",
        )
        else -> Triple("?", "?", "Unbekannte iterierte Aussagenverknüpfung.")
    }

    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LatexText(formel, style = MaterialTheme.typography.headlineSmall)
        Text(erklärung, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Neutrales Element der leeren Indexmenge:")
            LatexText(neutralesElement, style = MaterialTheme.typography.titleMedium)
        }
        if (operator == "adjunktion") {
            Text("Binäre Definition", style = MaterialTheme.typography.titleMedium)
            LatexText(
                "a \\stackrel{\\circ}{\\lor} b \\Leftrightarrow (a \\lor b) \\land \\neg(a \\land b)",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
