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
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
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

data class FalkAuswahl(val schritt: Int = 0, val zeile: Int = 0, val spalte: Int = 0, val summand: Int? = null)

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
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Zusammenhängendes Falk-Schema", style = MaterialTheme.typography.titleMedium)
        Text("${kette.faktoren.size} verbundene Faktoren; streng linksassoziativ, alle Zwischenprodukte.", style = MaterialTheme.typography.bodySmall)
        var auswahl by remember(auswertung) { mutableStateOf(FalkAuswahl()) }
        val (schritt, zeile, spalte, summand) = auswahl
        // Jedes P steht einmal unter seinem rechten Faktor und ist zugleich der nächste linke Operand.
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { contentDescription = "Falk-Schema Ausschnitt" }, contentAlignment = Alignment.Center) {
        Row(Modifier.semantics { contentDescription = "Falk-Schema" }, verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FalkMatrix("A1", kette.faktoren.first(), if (schritt == 0) zeile else null,
                if (schritt == 0) summand else null, onZelle = { z, _ -> auswahl = FalkAuswahl(zeile = z) })
            kette.zwischenprodukte.indices.forEach { index ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("P${index + 1} = ${if (index == 0) "A1" else "P$index"} · A${index + 2}")
                    FalkMatrix("A${index + 2}", kette.faktoren[index + 1],
                        if (schritt == index) summand else null, if (schritt == index) spalte else null,
                        onZelle = { _, s -> auswahl = FalkAuswahl(schritt = index, spalte = s) })
                    HorizontalDivider()
                    FalkMatrix("P${index + 1}", kette.zwischenprodukte[index],
                        if (schritt == index || schritt == index + 1) zeile else null,
                        if (schritt == index) spalte else if (schritt == index + 1) summand else null,
                        ergebnisZelleHervorheben = schritt == index,
                        onZelle = { z, s -> auswahl = FalkAuswahl(index, z, s) })
                }
            }
        }
        }
        val links = if (schritt == 0) kette.faktoren.first() else kette.zwischenprodukte[schritt - 1]
        val detail = (detailliertesFalkSchema(links, kette.faktoren[schritt + 1], zeile, spalte)
            as DetailliertesFalkSchemaErgebnis.Gültig).modell
        Text("P${schritt + 1}, Zeile ${zeile + 1}, Spalte ${spalte + 1}", style = MaterialTheme.typography.titleSmall)
        LatexText("${gekürzteFalkSummeLatex(detail)} = ${detail.ergebnisEintrag.zuLatex()}")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            detail.summanden.forEachIndexed { index, produkt ->
                FilterChip(selected = summand == index, onClick = { auswahl = auswahl.copy(summand = index) },
                    label = { Column { Text("k = ${index + 1}:"); LatexText(produkt.zuLatex()) } })
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
                                            (istZeile && istSpalte) -> MaterialTheme.colorScheme.secondaryContainer
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


/** Gemeinsamer Zugang für registrierte Rechner und vorhandene Prädikatsnachweise. */
fun besitztRechnerSchema(knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten): Boolean =
    RechnerOperatorRegistry.familieFür(knoten.art) != null || besitztPrädikatsRechenweg(knoten.art)

data class RechnerSchemaModell(
    val titel: String,
    val definition: String,
    val eingänge: List<Pair<String, String>>,
    val ausgänge: List<Pair<String, String>>,
    val einsetzung: String?,
    val fehler: String?,
)

fun rechnerSchemaModell(knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?): RechnerSchemaModell {
    val familie = RechnerOperatorRegistry.familieFür(knoten.art)
    val vertrag = familie?.let {
        (RechnerOperatorRegistry.finde(it, knoten.parameter[it.operatorParameter]) as? RechnerOperatorAuflösung.Bekannt)?.vertrag
    }
    return RechnerSchemaModell(vertrag?.titel ?: knoten.name,
        knoten.parameter[if (familie == RechnerFamilie.ZAHL) ZAHLENRECHNER_FORMEL_LATEX else STRUKTUR_RECHNER_FORMEL_LATEX]?.takeIf { familie?.let { f -> knoten.parameter[f.operatorParameter]?.endsWith(".formel") } == true }
            ?: vertrag?.definitionsLatex.orEmpty(),
        auswertung?.eingänge.orEmpty().map { it.key to it.value.anzeigeLatex() },
        auswertung?.ausgaben.orEmpty().map { it.key to it.value.anzeigeLatex() },
        if (vertrag != null && auswertung != null && auswertung.fehler == null && auswertung.ausgaben.isNotEmpty())
            schemaEinsetzung(vertrag, auswertung) else null,
        auswertung?.fehler ?: if (familie != null && vertrag == null) "Unbekannter Rechneroperator: ${knoten.parameter[familie.operatorParameter].orEmpty()}" else if (auswertung == null) "Noch kein Auswertungsergebnis vorhanden." else null)
}

/** Komponenten werden ausschließlich aus dem bestehenden Tensorvertrag gelesen. */
fun schemaKomponenten(objekt: AtlasWert, index: List<Int>): String? =
    (objekt as? Tensorartig)?.takeIf { t -> index.size == t.tensorForm.size &&
        index.indices.all { index[it] in 0 until t.tensorForm[it] } }?.tensorKomponente(index)?.zuLatex()

@Composable
fun RechnerAuswertungsschemaInhalt(
    knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?, modifier: Modifier = Modifier,
) {
    if (verwendetFalkSchema(knoten, auswertung)) { MatrixproduktFalkSchemaInhalt(auswertung, modifier); return }
    if (besitztPrädikatsRechenweg(knoten.art)) { PrädikatsRechenwegInhalt(auswertung, modifier); return }
    val modell = remember(knoten, auswertung) { rechnerSchemaModell(knoten, auswertung) }
    val struktur = auswertung?.ausgaben?.values?.firstOrNull()?.objekt as? Tensorartig
    var index by remember(knoten, auswertung) { mutableStateOf(struktur?.tensorForm?.map { 0 }.orEmpty()) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(modell.titel, style = MaterialTheme.typography.titleMedium)
        if (modell.definition.isNotBlank()) LatexText(modell.definition)
        modell.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Eingaben", style = MaterialTheme.typography.titleSmall)
        modell.eingänge.forEach { (name, latex) -> Text(name); LatexText(latex) }
        if (struktur != null && modell.fehler == null) {
            Text("Komponente auswählen", style = MaterialTheme.typography.titleSmall)
            struktur.tensorForm.forEachIndexed { achse, länge ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Achse ${achse + 1}: ${index[achse] + 1}")
                    TextButton(enabled = index[achse] > 0, onClick = { index = index.toMutableList().also { it[achse]-- } }) { Text("−") }
                    TextButton(enabled = index[achse] + 1 < länge, onClick = { index = index.toMutableList().also { it[achse]++ } }) { Text("+") }
                }
            }
            val komponenten = auswertung.eingänge.mapNotNull { (name, wert) -> schemaKomponenten(wert.objekt, index)?.let { name to it } }
            komponenten.forEach { (name, latex) -> Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(8.dp)) { Text("$name [${index.joinToString { (it + 1).toString() }}]"); LatexText(latex) }
            } }
            val familie = RechnerOperatorRegistry.familieFür(knoten.art)
            val addition = familie?.let { knoten.parameter[it.operatorParameter]?.endsWith(".addition") } == true
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.padding(8.dp)) {
                    Text("Ergebniskomponente [${index.joinToString { (it + 1).toString() }}]")
                    LatexText((if (addition && komponenten.size == auswertung.eingänge.size && komponenten.isNotEmpty())
                        komponenten.joinToString(" + ") { "\\left(${it.second}\\right)" } + " = " else "") + struktur.tensorKomponente(index).zuLatex())
                }
            }
        }
        modell.einsetzung?.let { Text("Einsetzung", style = MaterialTheme.typography.titleSmall); LatexText(it) }
        Text("Ergebnis", style = MaterialTheme.typography.titleSmall)
        modell.ausgänge.forEach { (name, latex) -> Text(name); LatexText(latex) }
        auswertung?.ausgaben?.values?.forEach { wert ->
            wert.annahmen.forEach { LatexText(it.zuLatex()) }
            (wert.objekt as? MathematischeSignaturtragendeMethode)?.let { methode ->
                Text("Methodenvorschrift · gemeinsame Parameter", style = MaterialTheme.typography.titleSmall)
                val signatur = methode.mathematischeMethodenSignatur()
                LatexText(signatur.argumente.joinToString(", ") { it.parameter.zuLatex() })
                LatexText(signatur.definitionsRaum.zuLatex())
            }
        }
        auswertung?.warnungen?.forEach { Text(it) }
        auswertung?.schritte?.forEach { schritt -> LatexText(schritt.vorher.zuLatex()); LatexText(schritt.nachher.zuLatex()) }
    }
}


@Composable
fun RechnerSchemaSchaltfläche(knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?) {
    var offen by remember(knoten.id) { mutableStateOf(false) }
    OutlinedButton(onClick = { offen = true }) { Text("Auswertungsschema") }
    if (offen) de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog(onDismissRequest = { offen = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 840.dp)) {
            de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf(
                titel = { Text("${knoten.name} · Auswertungsschema") },
                aktionen = { TextButton(onClick = { offen = false }) { Text("Schließen") } })
            RechnerAuswertungsschemaInhalt(knoten, auswertung, Modifier.weight(1f, fill = false).fillMaxWidth())
        }
    }
}


fun verwendetFalkSchema(knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?): Boolean = istMatrixProduktKnoten(knoten) &&
    auswertung?.eingänge.orEmpty().values.none { it.objekt is Methode }


/** UI-Symbole sind keine aufrufbaren Funktionen: nur bekannte Infixoperatoren so einsetzen. */
private fun schemaEinsetzung(vertrag: RechnerOperatorVertrag, auswertung: KnotenAuswertungsErgebnis): String {
    val werte = auswertung.eingänge.values.map { wert ->
        val latex = wert.anzeigeLatex()
        if (wert.objekt is Matrix || wert.objekt is OrientierterVektor || wert.objekt is Tupel) latex
        else "\\left($latex\\right)"
    }
    val infix = when (vertrag.operatorId.substringAfterLast('.')) {
        "addition" -> " + "
        "subtraktion" -> " - "
        else -> null
    }
    val ergebnis = auswertung.ausgaben.values.joinToString(", ") { it.anzeigeLatex() }
    return if (infix != null && werte.isNotEmpty()) werte.joinToString(infix) + " = " + ergebnis
    else {
        val eingabe=if(werte.size==1) werte.single() else "\\left(${werte.joinToString(", ")}\\right)"
        val ausgabe=if(auswertung.ausgaben.size==1) ergebnis else "\\left($ergebnis\\right)"
        "$eingabe \\mapsto $ausgabe"
    }
}
