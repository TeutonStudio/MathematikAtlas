package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

fun besitztPrädikatsRechenweg(art: String): Boolean = art in setOf("mathematik.auswerten", "mathematik.gleichungLösen")

/** Quellkompatibler Einstieg für bisherige Aufrufer. */
@Composable
fun PrädikatsRechenwegDialog(titel: String, ergebnis: KnotenAuswertungsErgebnis?, schließen: () -> Unit) =
    PrädikatsRechenwegDialog(titel, ergebnis, null, schließen)

/** Beide Plattformen verwenden dieselbe Auswertung und denselben budgetierten Numerikdienst. */
@Composable
fun PrädikatsRechenwegDialog(titel: String, ergebnis: KnotenAuswertungsErgebnis?, konfigurationAendern: ((PolynomNumerikKonfiguration) -> Unit)?, schließen: () -> Unit) {
    AtlasDialog(onDismissRequest = schließen) {
        Column(Modifier.fillMaxWidth().heightIn(max = 840.dp)) {
            AtlasDialogKopf(titel = { Text("$titel · Rechenweg") }, aktionen = {
                TextButton(onClick = schließen) { Text("Schließen") }
            })
            if (ergebnis?.polynomProblem != null)
                PolynomRechenwegInhalt(ergebnis, Modifier.weight(1f, fill = false).fillMaxWidth(), konfigurationAendern)
            else PrädikatsRechenwegInhalt(ergebnis, Modifier.weight(1f, fill = false).fillMaxWidth())
        }
    }
}

@Composable
fun PrädikatsRechenwegInhalt(ergebnis: KnotenAuswertungsErgebnis?, modifier: Modifier = Modifier) {
    var auswahl by remember(ergebnis) { mutableStateOf<UmformungsSchritt?>(null) }
    val schritte = remember(ergebnis) {
        ergebnis?.schritte.orEmpty().ifEmpty {
            val objekt = (ergebnis?.ausgaben?.values?.firstOrNull()?.objekt as? MathematischesObjekt)
                ?: AllgemeinerParameter("eingabe", "?")
            listOf(UmformungsSchritt(objekt, objekt, "auswertung.status", "Auswertung prüfen",
                ergebnis?.fehler ?: ergebnis?.entscheidung?.begründung
                ?: if (ergebnis == null) "Noch kein Auswertungsergebnis vorhanden." else "Keine weitere Umformung erforderlich."))
        }
    }
    LazyColumn(modifier.padding(AtlasDialogAbstände.inhalt), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ergebnis?.entscheidung?.let { e ->
                Text(when (e.wahrheitswert) {
                    Wahrheitswert.Wahr -> "Wahr"
                    Wahrheitswert.Lüge -> "Falsch"
                    null -> if (e.status == EntscheidungsStatus.NichtAuswertbar) "Nicht auswertbar"
                        else if (ergebnis.ausgaben.values.any { (it.objekt as? Aussage)?.freieMethodenParameter()?.isNotEmpty() == true }) "Vereinfacht / offen"
                        else "Mit verfügbaren Verfahren nicht entscheidbar"
                }, style = MaterialTheme.typography.titleMedium)
                Text(e.begründung)
            }
            ergebnis?.lösungsVariablen?.takeIf { it.isNotEmpty() }?.let { vars ->
                Text("Zielvariablen (Tupelreihenfolge):", style = MaterialTheme.typography.labelLarge)
                vars.forEach { LatexZeile("${it.variable.zuLatex()} \\in ${it.grundMenge.zuLatex()}") }
            }
            ergebnis?.lösungsParameter?.takeIf { it.isNotEmpty() }?.let { quellen ->
                Text("Parameter:", style = MaterialTheme.typography.labelLarge)
                quellen.forEach { LatexZeile("${it.name} \\in ${it.werteVorrat.zuLatex()}") }
            }
            ergebnis?.ausgaben?.forEach { (name, wert) ->
                Text(name, style = MaterialTheme.typography.labelLarge)
                LatexZeile(wert.anzeigeLatex())
                wert.annahmen.forEach { LatexZeile(it.zuLatex()) }
            }
            ergebnis?.warnungen?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Rechenweg · Zeile für Details antippen", style = MaterialTheme.typography.titleSmall)
        }
        itemsIndexed(schritte) { index, schritt ->
            Surface(Modifier.fillMaxWidth().clickable { auswahl = schritt },
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${index + 1}. ${schritt.titel}", style = MaterialTheme.typography.titleSmall)
                    LatexZeile(schritt.nachher.zuLatex())
                    Text(schritt.erklärung, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    auswahl?.let { schritt ->
        AtlasDialog(onDismissRequest = { auswahl = null }, größe = AtlasDialogGröße.Kompakt) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
                AtlasDialogKopf(titel = { Text(schritt.titel) }, aktionen = {
                    TextButton(onClick = { auswahl = null }) { Text("Zurück") }
                })
                LazyColumn(Modifier.weight(1f, fill = false).padding(AtlasDialogAbstände.inhalt),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { SchrittDetails(schritt) }
                    itemsIndexed(untergeordneteSchritte(schritt)) { index, teil ->
                        Text("${index + 1}. ${teil.titel}", style = MaterialTheme.typography.titleSmall)
                        SchrittDetails(teil)
                    }
                }
            }
        }
    }
}

@Composable
private fun LatexZeile(latex: String) {
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        LatexText(latex, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SchrittDetails(schritt: UmformungsSchritt) {
    LatexZeile(schritt.vorher.zuLatex())
    Text(when (schritt.beziehung) {
        SchrittBeziehung.Äquivalenz -> "⇔ Äquivalenzumformung"
        SchrittBeziehung.Folgerung -> "⇒ Folgerung"
        SchrittBeziehung.Zusatzrechnung -> "↓ Zusatzrechnung"
        SchrittBeziehung.Beschreibung -> "↓ Ergebnis / Beschreibung"
    })
    LatexZeile(schritt.nachher.zuLatex())
    Text(schritt.erklärung)
    schritt.bedingungen.forEach { LatexZeile(it.zuLatex()) }
    schritt.operationen.forEach { Text(it.zuKurztext()) }
    schritt.tabelle?.let { tabelle ->
        Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row { Text("Schritt / Zeile", Modifier.width(110.dp)); tabelle.spalten.forEach {
                Text((if (it.istRechteSeite) "│ " else "") + it.titel, Modifier.width(88.dp))
            } }
            tabelle.bloecke.forEach { block ->
                block.zeilen.forEach { zeile ->
                    Row {
                        Text("${block.schritt} / ${zeile.name}", Modifier.width(110.dp))
                        zeile.werte.forEach { wert -> Box(Modifier.width(88.dp)) { LatexZeile(wert.zuLatex()) } }
                    }
                    zeile.operation?.let { Text(it.zuKurztext(), color = MaterialTheme.colorScheme.primary) }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun untergeordneteSchritte(schritt: UmformungsSchritt): List<UmformungsSchritt> = buildList {
    fun sammle(s: UmformungsSchritt) {
        s.unterschritte.forEach { add(it); sammle(it) }
    }
    sammle(schritt)
}
