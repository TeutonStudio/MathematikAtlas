package de.TeutonStudio.MathematikAtlas

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

internal enum class FalkDatenQuelle { Knotendaten, Standardbeispiel }

internal data class MatrixproduktFalkDaten(
    val links: Matrix,
    val rechts: Matrix,
    val quelle: FalkDatenQuelle,
)

internal sealed interface FalkAchsenElement {
    data class Index(val wert: Int) : FalkAchsenElement
    data object Auslassung : FalkAchsenElement
}

internal fun matrixproduktFalkDaten(
    auswertung: KnotenAuswertungsErgebnis?,
): MatrixproduktFalkDaten {
    val links = auswertung?.eingänge?.get("a")?.objekt?.alsMatrixFaktorOderNull()
    val rechts = auswertung?.eingänge?.get("b")?.objekt?.alsMatrixFaktorOderNull()
    return if (links != null && rechts != null) {
        MatrixproduktFalkDaten(links, rechts, FalkDatenQuelle.Knotendaten)
    } else {
        standardFalkDaten()
    }
}

private fun MathematischesObjekt.alsMatrixFaktorOderNull(): Matrix? =
    runCatching { alsMatrixFaktor() }.getOrNull()

internal fun standardFalkDaten(): MatrixproduktFalkDaten {
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

internal fun projiziereFalkAchse(
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

internal fun gekürzteFalkSummeLatex(
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

@Composable
internal fun MatrixproduktFalkSchemaInhalt(
    auswertung: KnotenAuswertungsErgebnis?,
    modifier: Modifier = Modifier,
) {
    val abstände = LocalAtlasAbstände.current
    val daten = remember(auswertung) { matrixproduktFalkDaten(auswertung) }
    var zeile by remember(daten) { mutableIntStateOf(0) }
    var spalte by remember(daten) { mutableIntStateOf(0) }
    val schema = remember(daten, zeile, spalte) {
        detailliertesFalkSchema(daten.links, daten.rechts, zeile, spalte)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(abstände.dialogInnen),
        verticalArrangement = Arrangement.spacedBy(abstände.bereich),
    ) {
        Text("Falksches Schema", style = MaterialTheme.typography.titleMedium)
        Text(
            if (daten.quelle == FalkDatenQuelle.Knotendaten) {
                "Die Matrizen stammen aus den aktuell ausgewerteten Eingängen a und b."
            } else {
                "Standardbeispiel, da am geöffneten Knoten keine zwei auswertbaren Faktoren vorliegen."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (schema) {
            is DetailliertesFalkSchemaErgebnis.Inkompatibel -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        schema.meldung,
                        modifier = Modifier.padding(abstände.bereichInnen),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            is DetailliertesFalkSchemaErgebnis.Gültig -> {
                val modell = schema.modell
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FalkMatrix(
                        name = "A",
                        matrix = modell.linkerFaktor,
                        ausgewählteZeile = zeile,
                        ausgewählteSpalte = null,
                        onZelle = { z, _ -> zeile = z },
                    )
                    Text(
                        "·",
                        modifier = Modifier.padding(horizontal = abstände.standard, vertical = 34.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(abstände.standard),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FalkMatrix(
                            name = "B",
                            matrix = modell.rechterFaktor,
                            ausgewählteZeile = null,
                            ausgewählteSpalte = spalte,
                            onZelle = { _, s -> spalte = s },
                        )
                        HorizontalDivider()
                        FalkMatrix(
                            name = "C = A·B",
                            matrix = modell.ergebnis,
                            ausgewählteZeile = zeile,
                            ausgewählteSpalte = spalte,
                            ergebnisZelleHervorheben = true,
                            onZelle = { z, s -> zeile = z; spalte = s },
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(abstände.bereichInnen),
                        verticalArrangement = Arrangement.spacedBy(abstände.eng),
                    ) {
                        Text(
                            "Zeile ${zeile + 1} × Spalte ${spalte + 1}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        LatexText(
                            latex = "c_{${zeile + 1},${spalte + 1}}=${gekürzteFalkSummeLatex(modell)}=${modell.ergebnisEintrag.zuLatex()}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
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
    val abstände = LocalAtlasAbstände.current
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
            modifier = Modifier.padding(horizontal = LocalAtlasAbstände.current.standard),
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
