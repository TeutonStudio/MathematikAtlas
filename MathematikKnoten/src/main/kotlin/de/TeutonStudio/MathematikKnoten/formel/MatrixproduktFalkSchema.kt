package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.*

enum class FalkDatenQuelle { Knotendaten, Standardbeispiel }

data class MatrixproduktFalkDaten(
    val links: Matrix,
    val rechts: Matrix,
    val quelle: FalkDatenQuelle,
)

sealed interface FalkAchsenElement {
    data class Index(val wert: Int) : FalkAchsenElement
    data object Auslassung : FalkAchsenElement
}

fun matrixproduktFalkDaten(
    auswertung: KnotenAuswertungsErgebnis?,
): MatrixproduktFalkDaten {
    val faktoren = auswertung?.eingänge.orEmpty().values.toList()
    val links = faktoren.getOrNull(0)?.objekt?.alsMatrixFaktorOderNull()
    val rechts = faktoren.getOrNull(1)?.objekt?.alsMatrixFaktorOderNull()
    return if (links != null && rechts != null) {
        MatrixproduktFalkDaten(links, rechts, FalkDatenQuelle.Knotendaten)
    } else {
        standardFalkDaten()
    }
}

private fun de.TeutonStudio.MathematikRechenSystem.kern.AtlasWert.alsMatrixFaktorOderNull(): Matrix? =
    (this as? MathematischesObjekt)?.let { runCatching { it.alsMatrixFaktor() }.getOrNull() }

fun standardFalkDaten(): MatrixproduktFalkDaten {
    fun z(wert: Long) = RationaleZahl.von(wert)
    return MatrixproduktFalkDaten(
        links = Matrix(
            listOf(
                listOf(z(1), z(2), z(3)),
                listOf(z(4), z(5), z(6)),
            ),
        ),
        rechts = Matrix(
            listOf(
                listOf(z(7), z(8)),
                listOf(z(9), z(10)),
                listOf(z(11), z(12)),
            ),
        ),
        quelle = FalkDatenQuelle.Standardbeispiel,
    )
}

fun projiziereFalkAchse(
    anzahl: Int,
    ausgewählt: Int,
    maximalSichtbar: Int = 8,
): List<FalkAchsenElement> {
    require(anzahl > 0)
    require(ausgewählt in 0 until anzahl)
    require(maximalSichtbar >= 3)
    if (anzahl <= maximalSichtbar) {
        return List(anzahl) { FalkAchsenElement.Index(it) }
    }

    val sichtbar = linkedSetOf(0, 1, anzahl - 2, anzahl - 1, ausgewählt)
    var abstand = 1
    while (sichtbar.size < maximalSichtbar) {
        val kandidaten = listOf(ausgewählt - abstand, ausgewählt + abstand)
        var hinzugefügt = false
        for (kandidat in kandidaten) {
            if (kandidat in 0 until anzahl && sichtbar.size < maximalSichtbar) {
                hinzugefügt = sichtbar.add(kandidat) || hinzugefügt
            }
        }
        if (!hinzugefügt && sichtbar.size < maximalSichtbar) {
            (0 until anzahl).firstOrNull { it !in sichtbar }?.let(sichtbar::add) ?: break
        }
        abstand++
    }

    val sortiert = sichtbar.sorted()
    return buildList {
        sortiert.forEachIndexed { index, wert ->
            if (index > 0 && wert > sortiert[index - 1] + 1) add(FalkAchsenElement.Auslassung)
            add(FalkAchsenElement.Index(wert))
        }
    }
}

fun gekürzteFalkSummeLatex(
    modell: DetailliertesFalkSchemaModell,
    maximalSichtbar: Int = 8,
): String {
    require(maximalSichtbar >= 2)
    if (modell.summanden.size <= maximalSichtbar) return modell.summenLatex()
    val links = maximalSichtbar / 2
    val rechts = maximalSichtbar - links
    return buildList {
        addAll(modell.summanden.take(links).map(GeordneterProduktSummand::zuLatex))
        add("\\cdots")
        addAll(modell.summanden.takeLast(rechts).map(GeordneterProduktSummand::zuLatex))
    }.joinToString(" + ")
}

fun matrixproduktFalkFaktoren(auswertung: KnotenAuswertungsErgebnis?): List<Matrix> {
    require(auswertung?.fehler == null) { auswertung?.fehler.orEmpty() }
    return auswertung?.eingänge.orEmpty().entries.map { requireNotNull(it.value.objekt.alsMatrixFaktorOderNull()) { "Faktor ${it.key} ist keine Matrix, Zeile oder Spalte." } }
}

data class FalkAuswahl(val schritt: Int = 0, val zeile: Int = 0, val spalte: Int = 0)

@Composable
fun MatrixproduktFalkSchemaInhalt(
    auswertung: KnotenAuswertungsErgebnis?,
    modifier: Modifier = Modifier,
) {
    val faktorenPrüfung = remember(auswertung) { runCatching {
        require(auswertung?.fehler == null) { auswertung?.fehler.orEmpty() }
        matrixproduktFalkFaktoren(auswertung)
    } }
    if (faktorenPrüfung.isFailure) {
        Text(faktorenPrüfung.exceptionOrNull()?.message ?: "Faktoren nicht auswertbar.",modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
        return
    }
    val faktoren = faktorenPrüfung.getOrThrow()
    if (faktoren.size < 2) {
        Text("Für das Falk-Schema müssen mindestens zwei Faktoren verbunden und auswertbar sein.", modifier.padding(16.dp))
        return
    }
    val berechnung = remember(faktoren) { runCatching { matrixProduktKette(faktoren) } }
    val kette = berechnung.getOrNull()
    if (kette == null) {
        Text(berechnung.exceptionOrNull()?.message ?: "Matrixprodukt nicht definiert.", modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        return
    }
    var auswahl by remember(auswertung) { mutableStateOf(FalkAuswahl()) }
    val aktiverSchritt = auswahl.schritt.coerceIn(kette.zwischenprodukte.indices)
    val aktivesLinks = if (aktiverSchritt == 0) kette.faktoren.first() else kette.zwischenprodukte[aktiverSchritt - 1]
    val aktivesRechts = kette.faktoren[aktiverSchritt + 1]
    val aktiveZeile = auswahl.zeile.coerceIn(0, aktivesLinks.zeilenAnzahl - 1)
    val aktiveSpalte = auswahl.spalte.coerceIn(0, aktivesRechts.spaltenAnzahl - 1)
    val details = remember(aktivesLinks, aktivesRechts, aktiveZeile, aktiveSpalte) {
        detailliertesFalkSchema(aktivesLinks, aktivesRechts, aktiveZeile, aktiveSpalte)
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Zusammenhängendes Falk-Schema", style = MaterialTheme.typography.titleMedium)
        Text("${kette.faktoren.size} verbundene Faktoren; streng linksassoziativ, alle Zwischenprodukte.", style = MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val breit = maxWidth >= 840.dp
            @Composable fun schritt(index: Int) {
                val links = if (index == 0) kette.faktoren.first() else kette.zwischenprodukte[index - 1]
                val rechts = kette.faktoren[index + 1]
                val aktiv = aktiverSchritt == index
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("P${index + 1} = ${if (index == 0) "A1" else "P$index"} · A${index + 2}", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FalkMatrix(if (index == 0) "A1" else "P$index", links, if (aktiv) aktiveZeile else null, null, onZelle = { z, _ -> auswahl = FalkAuswahl(index, z, aktiveSpalte) })
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FalkMatrix("A${index + 2}", rechts, null, if (aktiv) aktiveSpalte else null, onZelle = { _, sp -> auswahl = FalkAuswahl(index, aktiveZeile, sp) })
                            HorizontalDivider()
                            FalkMatrix("P${index + 1}", kette.zwischenprodukte[index], if (aktiv) aktiveZeile else null, if (aktiv) aktiveSpalte else null, ergebnisZelleHervorheben = aktiv, onZelle = { z, sp -> auswahl = FalkAuswahl(index, z, sp) })
                        }
                    }
                }
            }
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                if (breit) Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    kette.zwischenprodukte.indices.forEach { index ->
                        if (index > 0) Text("→", style = MaterialTheme.typography.headlineLarge)
                        schritt(index)
                    }
                } else Column(Modifier.horizontalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    kette.zwischenprodukte.indices.forEach { index ->
                        if (index > 0) Text("↓", style = MaterialTheme.typography.headlineLarge)
                        schritt(index)
                    }
                }
            }
        }
        if (details is DetailliertesFalkSchemaErgebnis.Gültig) {
            val modell = details.modell
            Text("P${aktiverSchritt + 1} · Zeile ${aktiveZeile + 1}, Spalte ${aktiveSpalte + 1}", style = MaterialTheme.typography.titleSmall)
            LatexText("${gekürzteFalkSummeLatex(modell)} = ${modell.ergebnisEintrag.zuLatex()}")
        }
    }
}

@Composable
private fun FalkMatrix(
    name: String,
    matrix: Matrix,
    ausgewählteZeile: Int?,
    ausgewählteSpalte: Int?,
    ergebnisZelleHervorheben: Boolean = false,
    onZelle: (Int, Int) -> Unit,
) {
    val abstände = LocalFormelEditorAbstände.current
    val zeilen = projiziereFalkAchse(matrix.zeilenAnzahl, ausgewählteZeile ?: 0)
    val spalten = projiziereFalkAchse(matrix.spaltenAnzahl, ausgewählteSpalte ?: 0)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier.padding(top = abstände.winzig)
                .falkKlammern(MaterialTheme.colorScheme.outline, abstände.eng),
        ) {
            Row(Modifier.padding(horizontal = abstände.standard, vertical = abstände.winzig)) {
                spalten.forEach { spaltenElement ->
                    Column(Modifier.width(IntrinsicSize.Max)) {
                        zeilen.forEach { zeilenElement ->
                            when {
                                zeilenElement is FalkAchsenElement.Auslassung ||
                                    spaltenElement is FalkAchsenElement.Auslassung -> FalkAuslassungsZelle(
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                else -> {
                                    val z = (zeilenElement as FalkAchsenElement.Index).wert
                                    val s = (spaltenElement as FalkAchsenElement.Index).wert
                                    val istZeile = z == ausgewählteZeile
                                    val istSpalte = s == ausgewählteSpalte
                                    val istErgebnis = ergebnisZelleHervorheben && istZeile && istSpalte
                                    FalkZelle(
                                        modifier = Modifier.fillMaxWidth(),
                                        latex = matrix.zeilen[z][s].zuLatex(),
                                        hintergrund = when {
                                            istErgebnis -> MaterialTheme.colorScheme.tertiaryContainer
                                            istZeile || istSpalte -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        },
                                        beschreibung = "$name, Zeile ${z + 1}, Spalte ${s + 1}",
                                        onClick = { onZelle(z, s) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FalkZelle(
    modifier: Modifier = Modifier,
    latex: String,
    hintergrund: Color,
    beschreibung: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(46.dp).widthIn(min = 62.dp)
            .semantics { contentDescription = beschreibung }
            .clickable(onClick = onClick),
        color = hintergrund,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = LocalFormelEditorAbstände.current.standard),
            contentAlignment = Alignment.Center,
        ) {
            LatexText(latex = latex, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FalkAuslassungsZelle(modifier: Modifier = Modifier) {
    Box(modifier.height(46.dp).widthIn(min = 62.dp), contentAlignment = Alignment.Center) {
        Text("…", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.falkKlammern(farbe: Color, innenabstand: Dp): Modifier =
    drawBehind {
        val haken = 8.dp.toPx()
        val breite = 2.dp.toPx()
        drawLine(farbe, Offset(0f, 0f), Offset(haken, 0f), breite, StrokeCap.Square)
        drawLine(farbe, Offset(0f, 0f), Offset(0f, size.height), breite, StrokeCap.Square)
        drawLine(farbe, Offset(0f, size.height), Offset(haken, size.height), breite, StrokeCap.Square)
        drawLine(farbe, Offset(size.width, 0f), Offset(size.width - haken, 0f), breite, StrokeCap.Square)
        drawLine(farbe, Offset(size.width, 0f), Offset(size.width, size.height), breite, StrokeCap.Square)
        drawLine(farbe, Offset(size.width, size.height), Offset(size.width - haken, size.height), breite, StrokeCap.Square)
    }.padding(horizontal = innenabstand)
