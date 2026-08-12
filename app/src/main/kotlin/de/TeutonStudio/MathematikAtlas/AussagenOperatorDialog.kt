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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.AussagenOperatorArt
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.Wahrheitstabelle
import de.TeutonStudio.MathematikRechenSystem.kern.Wahrheitswert
import java.math.BigInteger

private val WAHRHEITSTABELLEN_SEITENGRÖSSE = BigInteger.valueOf(256)
private val EINGANGS_ZELLEN_BREITE = 104.dp
private val ERGEBNIS_ZELLEN_BREITE = 136.dp
private val ZEILENINDEX_ZELLEN_BREITE = 56.dp
private val TABELLEN_TRENNER_BREITE = 2.dp

@OptIn(ExperimentalLayoutApi::class)
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

    val anzahlEingänge = knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }
    val tabellenBreite = ZEILENINDEX_ZELLEN_BREITE +
        EINGANGS_ZELLEN_BREITE * anzahlEingänge.toFloat() +
        TABELLEN_TRENNER_BREITE + ERGEBNIS_ZELLEN_BREITE

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val gewünschteBreite = if (direkteArt != null) {
                (tabellenBreite + 40.dp).coerceAtLeast(520.dp)
            } else {
                680.dp
            }
            val dialogBreite = gewünschteBreite.coerceAtMost(maxWidth * .92f)

            Surface(
                Modifier
                    .width(dialogBreite)
                    .heightIn(max = maxHeight * .88f)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
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
                        WahrheitstabellenInhalt(knoten, direkteArt, Modifier.fillMaxWidth())
                    } else {
                        IterationsInhalt(knoten, Modifier.fillMaxWidth())
                    }

                    HorizontalDivider()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    val tabellenBreite = ZEILENINDEX_ZELLEN_BREITE +
        EINGANGS_ZELLEN_BREITE * eingänge.size.toFloat() +
        TABELLEN_TRENNER_BREITE + ERGEBNIS_ZELLEN_BREITE
    val listenHöhe = (zeilenAufSeite.coerceIn(1, 10) * 42).dp

    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LatexText(art.ergebnisLatex(eingänge.size), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Zeilen ${seitenStart + BigInteger.ONE}–${seitenStart + BigInteger.valueOf(zeilenAufSeite.toLong())} von ${tabelle.zeilenAnzahl}",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
        )
        if (tabelle.zeilenAnzahl > WAHRHEITSTABELLEN_SEITENGRÖSSE) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val sichtbareBreite = tabellenBreite.coerceAtMost(maxWidth)
            Box(
                Modifier
                    .width(sichtbareBreite)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Column(Modifier.width(tabellenBreite)) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        TabellenKopfZelle("Nr.", ZEILENINDEX_ZELLEN_BREITE)
                        eingänge.indices.forEach { index -> TabellenKopfZelle("A${index + 1}") }
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            thickness = TABELLEN_TRENNER_BREITE,
                        )
                        TabellenKopfZelle("Ergebnis", ERGEBNIS_ZELLEN_BREITE)
                    }
                    HorizontalDivider(thickness = TABELLEN_TRENNER_BREITE)
                    LazyColumn(Modifier.height(listenHöhe)) {
                        items(zeilen, key = { it.index.toString() }) { zeile ->
                            Row(Modifier.height(IntrinsicSize.Min)) {
                                TabellenIndexZelle(zeile.index + BigInteger.ONE)
                                zeile.eingänge.forEach { wert -> WahrheitswertZelle(wert) }
                                VerticalDivider(
                                    modifier = Modifier.fillMaxHeight(),
                                    thickness = TABELLEN_TRENNER_BREITE,
                                )
                                WahrheitswertZelle(zeile.ergebnis, ERGEBNIS_ZELLEN_BREITE)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabellenIndexZelle(index: BigInteger) {
    Box(
        Modifier.width(ZEILENINDEX_ZELLEN_BREITE).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            index.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TabellenKopfZelle(text: String, breite: Dp = EINGANGS_ZELLEN_BREITE) {
    Box(
        Modifier.width(breite).padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WahrheitswertZelle(wert: Boolean, breite: Dp = EINGANGS_ZELLEN_BREITE) {
    Box(
        Modifier.width(breite).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        LatexText(
            if (wert) Wahrheitswert.Wahr.latex else Wahrheitswert.Lüge.latex,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun IterationsInhalt(knoten: KnotenDaten, modifier: Modifier) {
    val operator = knoten.parameter["operator"]
    val (formel, neutralesElement, erklärung) = when (operator) {
        "konjunktion" -> Triple(
            "\\bigwedge_{idx \\in \\Set{A}} methode(idx)",
            Wahrheitswert.Wahr,
            "Wahr genau dann, wenn alle von der Methode erzeugten Aussagen wahr sind.",
        )
        "disjunktion" -> Triple(
            "\\bigvee_{idx \\in \\Set{A}} methode(idx)",
            Wahrheitswert.Lüge,
            "Wahr, sobald mindestens eine von der Methode erzeugten Aussage wahr ist.",
        )
        "adjunktion" -> Triple(
            "\\stackrel{\\bullet}{\\bigvee}_{idx \\in \\Set{A}} methode(idx)",
            Wahrheitswert.Lüge,
            "Wahr genau dann, wenn eine ungerade Anzahl der erzeugten Aussagen wahr ist.",
        )
        else -> Triple("?", null, "Unbekannte iterierte Aussagenverknüpfung.")
    }

    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LatexText(formel, style = MaterialTheme.typography.headlineSmall)
        Text(erklärung, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Neutrales Element der leeren Indexmenge:")
            neutralesElement?.let {
                LatexText(it.latex, style = MaterialTheme.typography.titleMedium)
            } ?: Text("?")
        }
        if (operator == "adjunktion") {
            Text("Binäre Definition", style = MaterialTheme.typography.titleMedium)
            LatexText(
                "a \\stackrel{\\bullet}{\\lor} b \\Leftrightarrow (a \\lor b) \\land \\neg(a \\land b)",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
