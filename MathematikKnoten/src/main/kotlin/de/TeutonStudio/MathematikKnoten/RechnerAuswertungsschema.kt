package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Gemeinsamer Zugang für registrierte Rechner und vorhandene Prädikatsnachweise. */
fun besitztRechnerSchema(knoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten): Boolean =
    RechnerOperatorRegistry.familieFür(knoten.art) != null || besitztPrädikatsRechenweg(knoten.art) || knoten.art in setOf(ÜBERSETZER_ART, TUPEL_ABFLACHEN_ART, "mathematik.termZuMethode", "mathematik.tupelZuSpalte", "mathematik.tupelZuZeile", "mathematik.spalteZuTupel", "mathematik.zeileZuTupel", "mathematik.geometrie.zuTrägermenge")

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
        auswertung?.fehler ?: if (familie != null && vertrag == null && FORMEL_KONTEXT_EIGENSCHAFT !in knoten.eigenschaften) "Unbekannter Rechneroperator: ${knoten.parameter[familie.operatorParameter].orEmpty()}" else if (auswertung == null) "Noch kein Auswertungsergebnis vorhanden." else null)
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
            wert.übersetzungen.aktuell?.let { zeugnis ->
                Text("Übersetzungszeugnis", style = MaterialTheme.typography.titleSmall)
                zeugnis.hinweise().forEach { Text(it) }
                zeugnis.zielTräger?.let { Text("Zielträger"); LatexText(it.zuLatex()) }
                zeugnis.bildRaum?.let { Text("Exakter Bildraum"); LatexText(it.zuLatex()) }
            }
            if (wert.übersetzungen.herkunft.isNotEmpty()) Text("Herkunft: ${wert.übersetzungen.herkunft.size} frühere Übersetzungen; kein Nachweis für diese Berechnung.")
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

/** Kompatibler Singularname der bestehenden öffentlichen Komponentenprojektion. */
fun schemaKomponente(objekt:AtlasWert,index:List<Int>):String?=schemaKomponenten(objekt,index)
