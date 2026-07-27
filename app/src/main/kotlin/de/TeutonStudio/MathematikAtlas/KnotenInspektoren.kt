package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.Funktion

interface KnotenInspektor {
    @Composable fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen)
}
interface KnotenInspektorAktionen {
    fun parameter(schlüssel: String, wert: String)
    fun eigenschaften(eigenschaften: Map<String, de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft>)
    fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId)
}
object KnotenInspektorRegister {
    private val inspektoren = mapOf<String, KnotenInspektor>(
        "mathematik.lösungsmenge" to LösungsmengeInspektor,
        "mathematik.visualisierung" to VisualisierungsInspektor,
        "mathematik.kartenEingang" to KartenSchnittstellenInspektor,
        "mathematik.kartenAusgang" to KartenSchnittstellenInspektor,
        "mathematik.variable" to VariablenInspektor,
        "mathematik.termZuMethode" to TermZuMethodeInspektor,
    )
    fun finde(art: String) = inspektoren[art]
}

private object VariablenInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "x") { aktionen.parameter("name", it.trim()) }
        GrundmengenAuswahl("Wertevorrat", knoten.parameter["werteVorrat"] ?: "R") { aktionen.parameter("werteVorrat", it) }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object TermZuMethodeInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "f") { aktionen.parameter("name", it.trim().ifBlank { "f" }) }
        GrundmengenAuswahl("Zielmenge", knoten.parameter["zielmenge"] ?: "R") { aktionen.parameter("zielmenge", it) }
        val parameter = (ergebnis?.ausgaben?.get("methode")?.objekt as? Funktion)?.parameter.orEmpty()
        Text("Argumentreihenfolge", style = MaterialTheme.typography.titleSmall)
        if (parameter.isEmpty()) Text("Keine freien Variablen erkannt.", style = MaterialTheme.typography.bodySmall)
        parameter.forEachIndexed { index, variable ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${index + 1}. ${variable.name}", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    val neu = parameter.map { it.name }.toMutableList().also { namen -> java.util.Collections.swap(namen, index, index - 1) }
                    aktionen.parameter("argumentReihenfolge", neu.joinToString(","))
                }, enabled = index > 0) { Text("↑") }
                OutlinedButton(onClick = {
                    val neu = parameter.map { it.name }.toMutableList().also { namen -> java.util.Collections.swap(namen, index, index + 1) }
                    aktionen.parameter("argumentReihenfolge", neu.joinToString(","))
                }, enabled = index < parameter.lastIndex) { Text("↓") }
            }
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun GrundmengenAuswahl(label: String, aktuell: String, ändern: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("N", "Z", "Q", "R", "C").forEach { menge ->
            FilterChip(aktuell == menge, { ändern(menge) }, label = { Text(menge) })
        }
    }
}

private object KartenSchnittstellenInspektor : KnotenInspektor {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val wertAnschluss = knoten.anschlüsse.firstOrNull { it.name == "wert" } ?: return
        val aktuelleArt = MathematikAnschlussArten.alle.firstOrNull { it.id == wertAnschluss.art }
        var geöffnet by remember(knoten.id, wertAnschluss.art) { mutableStateOf(false) }
        ParameterFeld("Name", knoten.parameter["name"] ?: knoten.name) { aktionen.parameter("name", it) }
        Text("Schnittstellen-Typ", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = aktuelleArt?.name ?: wertAnschluss.art.wert,
                onValueChange = {},
                readOnly = true,
                label = { Text("Typ") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                MathematikAnschlussArten.alle.forEach { art ->
                    DropdownMenuItem(
                        text = { Text(art.name) },
                        onClick = {
                            geöffnet = false
                            aktionen.anschlussArt(AnschlussVerweis(knoten.id, wertAnschluss.id), art.id)
                        },
                    )
                }
            }
        }
        Text(
            "Der Typ gilt für den Anschluss „wert“ und wird von Gruppenknoten übernommen.",
            style = MaterialTheme.typography.bodySmall,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object LösungsmengeInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val automatisch = knoten.parameter["automatisch"]?.toBooleanStrictOrNull() ?: true
        Text("Variablen", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Freie Variablen automatisch übernehmen", modifier = Modifier.weight(1f))
            Switch(automatisch, onCheckedChange = { aktionen.parameter("automatisch", it.toString()) })
        }
        if (!automatisch) ParameterFeld("Variablen (geordnet, mit Komma)", knoten.parameter["variablen"].orEmpty()) { aktionen.parameter("variablen", it) }
        ParameterFeld("Grundmengen (N, Z, Q, R)", knoten.parameter["grundmengen"] ?: "R") { aktionen.parameter("grundmengen", it) }
        Text("Eine Grundmenge gilt für alle Variablen; mehrere folgen der Variablenreihenfolge.", style = MaterialTheme.typography.bodySmall)
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object VisualisierungsInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        var config by remember(knoten.id, knoten.eigenschaften) { mutableStateOf(VisualisierungsKonfiguration.aus(knoten.eigenschaften)) }
        fun ändern(neu: VisualisierungsKonfiguration) { config = neu; aktionen.eigenschaften(neu.zuEigenschaften()) }
        Text("Raum", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(config.dimension == RaumDimension.R2, { ändern(config.copy(dimension = RaumDimension.R2)) }, label = { Text("R²") })
            FilterChip(config.dimension == RaumDimension.R3, { ändern(config.copy(dimension = RaumDimension.R3)) }, label = { Text("R³") })
        }
        Text("Achsen", style = MaterialTheme.typography.titleSmall)
        ParameterFeld("X-Variable", config.achsen.x) { ändern(config.copy(achsen = config.achsen.copy(x = it.trim()))) }
        ParameterFeld("Y-Variable", config.achsen.y) { ändern(config.copy(achsen = config.achsen.copy(y = it.trim()))) }
        if (config.dimension == RaumDimension.R3) ParameterFeld("Z-Variable", config.achsen.z.orEmpty()) { ändern(config.copy(achsen = config.achsen.copy(z = it.trim().ifBlank { null }))) }
        BereichFeld("X-Bereich", config.bereiche.x) { ändern(config.copy(bereiche = config.bereiche.copy(x = it))) }
        BereichFeld("Y-Bereich", config.bereiche.y) { ändern(config.copy(bereiche = config.bereiche.copy(y = it))) }
        if (config.dimension == RaumDimension.R3) BereichFeld("Z-Bereich", config.bereiche.z ?: ZahlenBereich(-10.0, 10.0)) { ändern(config.copy(bereiche = config.bereiche.copy(z = it))) }
        Text("Sampling", style = MaterialTheme.typography.titleSmall)
        ParameterFeld("R²-Auflösung", config.sampling.auflösung2D.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(auflösung2D = n.coerceIn(16, 240)))) } }
        ParameterFeld("R³-Auflösung", config.sampling.auflösung3D.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(auflösung3D = n.coerceIn(8, 64)))) } }
        ParameterFeld("Toleranz", config.sampling.toleranz.toString()) { it.toDoubleOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(toleranz = n.coerceIn(1e-5, 2.0)))) } }
        Text("Farbe", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FarbModus.entries.forEach { modus -> FilterChip(config.farbe.modus == modus, { ändern(config.copy(farbe = config.farbe.copy(modus = modus))) }, label = { Text(when (modus) { FarbModus.Keine -> "Keine"; FarbModus.FesteFarbe -> "Fest"; FarbModus.Spektrum -> "Spektrum" }) }) } }
        if (config.farbe.modus == FarbModus.Spektrum) {
            ParameterFeld("Farbvariable", config.farbe.variable.orEmpty()) { ändern(config.copy(farbe = config.farbe.copy(variable = it.ifBlank { null }))) }
            Text("Palette", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Ozean", "Sonnenuntergang", "Wald").forEach { palette -> FilterChip(config.farbe.palette == palette, { ändern(config.copy(farbe = config.farbe.copy(palette = palette))) }, label = { Text(palette) }) } }
            BereichFeld("Farbwertbereich", config.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)) { ändern(config.copy(farbe = config.farbe.copy(bereich = it))) }
        }
        OutlinedButton(onClick = { ändern(config.copy(kamera = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0))) }) { Text("Standardansicht wiederherstellen") }
    }
}

@Composable private fun ParameterFeld(label: String, wert: String, ändern: (String) -> Unit) { var text by remember(label, wert) { mutableStateOf(wert) }; OutlinedTextField(text, { text = it; ändern(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
@Composable private fun BereichFeld(label: String, bereich: ZahlenBereich, ändern: (ZahlenBereich) -> Unit) { var text by remember(label, bereich) { mutableStateOf("${bereich.minimum}, ${bereich.maximum}") }; OutlinedTextField(text, { text = it; val p = it.split(',').map(String::trim); if (p.size == 2) { val a = p[0].toDoubleOrNull(); val b = p[1].toDoubleOrNull(); if (a != null && b != null && a < b) ändern(ZahlenBereich(a, b)) } }, label = { Text("$label: Minimum, Maximum") }, modifier = Modifier.fillMaxWidth()) }
