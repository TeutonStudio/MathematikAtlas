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
import de.TeutonStudio.MathematikKnoten.GeometrieTeilobjektTyp
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.WertebereichKonfiguration
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.Abbild
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.Tensor
import de.TeutonStudio.MathematikRechenSystem.kern.VektorOrientierung
import de.TeutonStudio.MathematikRechenSystem.kern.parseTensorPermutationOderNull
import de.TeutonStudio.MathematikRechenSystem.kern.standardTensorPermutation

interface KnotenInspektor {
    @Composable fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen)
}
interface KnotenInspektorAktionen {
    fun parameter(schlüssel: String, wert: String)
    fun name(wert: String)
    fun eigenschaften(eigenschaften: Map<String, de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft>)
    fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId)
    fun knoten(knoten: KnotenDaten)
}
object KnotenInspektorRegister {
    private val inspektoren = mapOf<String, KnotenInspektor>(
        "mathematik.lösungsmenge" to LösungsmengeInspektor,
        "mathematik.visualisierung" to VisualisierungsInspektor,
        "mathematik.kartenEingang" to KartenSchnittstellenInspektor,
        "mathematik.kartenAusgang" to KartenSchnittstellenInspektor,
        "mathematik.variable" to VariablenInspektor,
        "mathematik.allgemeinerParameter" to AllgemeineParameterInspektor,
        "mathematik.termZuMethode" to TermZuMethodeInspektor,
        "mathematik.methodeAufrufen" to MethodenAufrufInspektor,
        "mathematik.ordnungsrelation" to OrdnungsrelationInspektor,
        "mathematik.endlicheMenge" to EndlicheMengeInspektor,
        "mathematik.transponieren" to TransponierenInspektor,
        "mathematik.matrixdiagonale" to MatrixdiagonaleInspektor,
        ZAHLENRECHNER_ART to ZahlenRechnerInspektor,
        GeometrieTeilobjektTyp.Ecke.knotenArt to GeometrieTeilobjektInspektor,
        GeometrieTeilobjektTyp.Kante.knotenArt to GeometrieTeilobjektInspektor,
        GeometrieTeilobjektTyp.Fläche.knotenArt to GeometrieTeilobjektInspektor,
    )
    fun finde(art: String) = inspektoren[art]
}

private object OrdnungsrelationInspektor : KnotenInspektor {
    private val relationen = listOf(
        Triple("kleiner", "<", "Kleiner"),
        Triple("kleinerGleich", "≤", "Kleiner oder gleich"),
        Triple("größer", ">", "Größer"),
        Triple("größerGleich", "≥", "Größer oder gleich"),
    )

    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val aktuell = knoten.parameter["relation"] ?: "kleiner"
        Text("Relation", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            relationen.forEach { (schlüssel, zeichen, standardName) ->
                FilterChip(
                    selected = aktuell == schlüssel,
                    onClick = {
                        val bisherStandard = relationen.any { it.third == knoten.name }
                        aktionen.parameter("relation", schlüssel)
                        if (bisherStandard) aktionen.name(standardName)
                    },
                    label = { Text(zeichen) },
                )
            }
        }
        Text(
            "Die mathematische Definition im Knotendialog folgt der ausgewählten Relation.",
            style = MaterialTheme.typography.bodySmall,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private object VariablenInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "x") { aktionen.parameter("name", it.trim()) }
        GrundmengenAuswahl("Wertevorrat", knoten.parameter["werteVorrat"] ?: "R") { aktionen.parameter("werteVorrat", it) }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object AllgemeineParameterInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "a") { aktionen.parameter("name", it.trim()) }
        val bereich = WertebereichKonfiguration.vonEigenschaft(knoten.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT])
        WertebereichEditor(bereich) { neu ->
            aktionen.eigenschaften(knoten.eigenschaften + (WertebereichKonfiguration.EIGENSCHAFT to neu.zuEigenschaft()))
        }
        Text("Der Wertebereich bestimmt die Zielmenge von Term zu Methode.", style = MaterialTheme.typography.bodySmall)
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object TermZuMethodeInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "f") { aktionen.parameter("name", it.trim().ifBlank { "f" }) }
        Text("Die Zielmenge wird aus dem Term und den Wertebereichen seiner Parameter abgeleitet.", style = MaterialTheme.typography.bodySmall)
        val parameter = (ergebnis?.ausgaben?.get("methode")?.objekt as? Methode)?.parameter.orEmpty()
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

private object MethodenAufrufInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Methodenvertrag", style = MaterialTheme.typography.titleSmall)
        knoten.parameter[METHODEN_AUFRUF_VERTRAGSFEHLER]?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        val stelligkeit = knoten.parameter[METHODEN_AUFRUF_STELLIGKEIT]?.toIntOrNull()
        if (stelligkeit == null) {
            Text(
                "Noch kein konkreter Methodenvertrag erkannt. Die allgemeinen Argumentanschlüsse bleiben erweiterbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Stelligkeit: $stelligkeit", style = MaterialTheme.typography.bodyMedium)
            if (stelligkeit == 0) {
                Text("Die Methode besitzt keine Argumente.", style = MaterialTheme.typography.bodySmall)
            }
            repeat(stelligkeit) { index ->
                val präfix = "$METHODEN_AUFRUF_PARAMETER_PREFIX$index."
                val name = knoten.parameter["${präfix}name"] ?: "Argument ${index + 1}"
                val artId = AnschlussArtId(knoten.parameter["${präfix}art"].orEmpty())
                val art = MathematikAnschlussArten.alle.firstOrNull { it.id == artId }?.name
                    ?: artId.wert.ifBlank { "Mathematisches Objekt" }
                val werteVorrat = knoten.parameter["${präfix}werteVorrat"]
                Text(
                    buildString {
                        append("${index + 1}. $name: $art")
                        if (!werteVorrat.isNullOrBlank()) append(" ∈ $werteVorrat")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            knoten.parameter[METHODEN_AUFRUF_ZIELMENGE]?.let { zielMenge ->
                Text("Zielmenge: $zielMenge", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Stelligkeit, Anschlussarten und Zielmenge werden aus der verbundenen Methode übernommen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object TransponierenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Transposition", style = MaterialTheme.typography.titleSmall)
        val tensor = ergebnis?.ausgaben?.get("wert")?.objekt as? Tensor
        val rang = tensor?.rang
        when {
            rang == null -> Text(
                "Vektoren und Matrizen benötigen keine Konfiguration. Bei einem Tensor höheren Rangs erscheint hier seine Achsenpermutation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rang <= 2 -> {
                val standard = standardTensorPermutation(rang).joinToString(",")
                LaunchedEffect(knoten.id, rang, knoten.parameter["achsenPermutation"]) {
                    if (knoten.parameter["achsenPermutation"] != standard) {
                        aktionen.parameter("achsenPermutation", standard)
                    }
                }
                Text(
                    "Tensor Rang $rang verwendet die kanonische Transposition [$standard].",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> TensorPermutationEditor(knoten, rang, aktionen)
        }
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TensorPermutationEditor(
    knoten: KnotenDaten,
    rang: Int,
    aktionen: KnotenInspektorAktionen,
) {
    val standard = standardTensorPermutation(rang)
    val standardText = standard.joinToString(",")
    val gespeichert = knoten.parameter["achsenPermutation"]
    val gespeichertGültig = parseTensorPermutationOderNull(gespeichert, rang)
    var text by remember(knoten.id, rang) {
        mutableStateOf((gespeichertGültig ?: standard).joinToString(","))
    }
    var zurückgesetzt by remember(knoten.id, rang) { mutableStateOf(false) }

    LaunchedEffect(knoten.id, rang, gespeichert) {
        val gültig = parseTensorPermutationOderNull(gespeichert, rang)
        if (gültig == null) {
            text = standardText
            zurückgesetzt = true
            aktionen.parameter("achsenPermutation", standardText)
        } else {
            text = gültig.joinToString(",")
        }
    }

    val eingabeGültig = parseTensorPermutationOderNull(text, rang) != null
    OutlinedTextField(
        value = text,
        onValueChange = { neu ->
            text = neu
            parseTensorPermutationOderNull(neu, rang)?.let { permutation ->
                zurückgesetzt = false
                aktionen.parameter("achsenPermutation", permutation.joinToString(","))
            }
        },
        label = { Text("Achsenpermutation") },
        isError = !eingabeGültig,
        supportingText = {
            Text(
                when {
                    !eingabeGültig -> "Jede Achse von 0 bis ${rang - 1} muss genau einmal vorkommen."
                    zurückgesetzt -> "Eine veraltete Permutation wurde auf den Standard [$standardText] zurückgesetzt."
                    else -> "Eintrag i gibt an, welche alte Achse zur neuen Achse i wird."
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = {
            text = standardText
            zurückgesetzt = false
            aktionen.parameter("achsenPermutation", standardText)
        },
    ) { Text("Standardpermutation") }
}

@Composable private fun WertebereichEditor(
    bereich: WertebereichKonfiguration,
    ändern: (WertebereichKonfiguration) -> Unit,
) {
    Text("Wertebereich", style = MaterialTheme.typography.titleSmall)
    val arten = listOf(
        "Zahl" to WertebereichKonfiguration.Zahl(),
        "Aussage" to WertebereichKonfiguration.Aussage,
        "Menge" to WertebereichKonfiguration.Menge(),
        "Tupel" to WertebereichKonfiguration.Tupel(),
        "Spalte" to WertebereichKonfiguration.Vektor(),
        "Zeile" to WertebereichKonfiguration.Vektor(orientierung = VektorOrientierung.Zeile),
        "Matrix" to WertebereichKonfiguration.Matrix(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        arten.take(3).forEach { (name, wert) -> FilterChip(bereich::class == wert::class, { ändern(wert) }, label = { Text(name) }) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        arten.drop(3).forEach { (name, wert) -> FilterChip(bereich::class == wert::class && (bereich !is WertebereichKonfiguration.Vektor || wert !is WertebereichKonfiguration.Vektor || bereich.orientierung == wert.orientierung), { ändern(wert) }, label = { Text(name) }) }
    }
    when (bereich) {
        is WertebereichKonfiguration.Zahl -> GrundmengenAuswahl("Zahlgrundmenge", bereich.grundmenge) { ändern(bereich.copy(grundmenge = it)) }
        WertebereichKonfiguration.Aussage -> Text("{⊤, ⊥}", style = MaterialTheme.typography.bodySmall)
        is WertebereichKonfiguration.Menge -> {
            Text("Elementbereich", style = MaterialTheme.typography.bodySmall)
            WertebereichEditor(bereich.elementBereich) { ändern(bereich.copy(elementBereich = it)) }
        }
        is WertebereichKonfiguration.Tupel -> {
            bereich.komponenten.forEachIndexed { index, komponente ->
                Text("Komponente ${index + 1}", style = MaterialTheme.typography.bodySmall)
                WertebereichEditor(komponente) { neu -> ändern(bereich.copy(komponenten = bereich.komponenten.mapIndexed { i, alt -> if (i == index) neu else alt })) }
            }
            OutlinedButton(onClick = { ändern(bereich.copy(komponenten = bereich.komponenten + WertebereichKonfiguration.Zahl())) }) { Text("Komponente hinzufügen") }
        }
        is WertebereichKonfiguration.Vektor -> {
            PositiveGanzzahlFeld("Dimension", bereich.dimension) { ändern(bereich.copy(dimension = it)) }
            GrundmengenAuswahl("Skalarmenge", bereich.skalarMenge) { ändern(bereich.copy(skalarMenge = it)) }
        }
        is WertebereichKonfiguration.Matrix -> {
            PositiveGanzzahlFeld("Zeilen", bereich.zeilen) { ändern(bereich.copy(zeilen = it)) }
            PositiveGanzzahlFeld("Spalten", bereich.spalten) { ändern(bereich.copy(spalten = it)) }
            GrundmengenAuswahl("Skalarmenge", bereich.skalarMenge) { ändern(bereich.copy(skalarMenge = it)) }
        }
    }
}

@Composable private fun PositiveGanzzahlFeld(label: String, wert: Int, ändern: (Int) -> Unit) {
    var text by remember(label, wert) { mutableStateOf(wert.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { neu ->
            text = neu
            neu.toIntOrNull()?.takeIf { it > 0 }?.let(ändern)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
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
        val methode = ergebnis?.eingänge?.values
            ?.mapNotNull { wert -> (wert.objekt as? Abbild)?.methode }
            ?.firstOrNull()
        Text("Raum", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(config.dimension == RaumDimension.R1, { ändern(config.copy(dimension = RaumDimension.R1)) }, label = { Text("R¹") })
            FilterChip(config.dimension == RaumDimension.R2, { ändern(config.copy(dimension = RaumDimension.R2)) }, label = { Text("R²") })
            FilterChip(config.dimension == RaumDimension.R3, { ändern(config.copy(dimension = RaumDimension.R3)) }, label = { Text("R³") })
        }
        Text("Methodenvisualisierung", style = MaterialTheme.typography.titleSmall)
        if (methode == null) {
            Text("Noch keine Methodensignatur erkannt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                "${methode.name}(${methode.parameter.joinToString { it.name }}) → ${methode.ausgabeNamen.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
            )
            methode.parameter.forEachIndexed { index, parameter ->
                Text(
                    "${index + 1}. ${parameter.name} ∈ ${methode.werteVorräte[parameter.name]?.zuLatex() ?: "nicht angegeben"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                MethodenDarstellungsModus.Automatisch to "Auto",
                MethodenDarstellungsModus.Bild to "Bild",
                MethodenDarstellungsModus.Funktionsgraph to "Graph",
            ).forEach { (modus, label) ->
                FilterChip(config.methodenModus == modus, { ändern(config.copy(methodenModus = modus)) }, label = { Text(label) })
            }
        }
        FilterChip(
            config.methodenModus == MethodenDarstellungsModus.Koordinatenausgabe,
            { ändern(config.copy(methodenModus = MethodenDarstellungsModus.Koordinatenausgabe)) },
            label = { Text("Koordinatenausgabe") },
        )
        Text("Achsen und Parameterfenster", style = MaterialTheme.typography.titleSmall)
        ParameterFeld("X-Achse / erste Ausgabe", config.achsen.x) { ändern(config.copy(achsen = config.achsen.copy(x = it.trim()))) }
        if (config.dimension != RaumDimension.R1) ParameterFeld("Y-Achse / zweite Ausgabe", config.achsen.y) { ändern(config.copy(achsen = config.achsen.copy(y = it.trim()))) }
        if (config.dimension == RaumDimension.R3) ParameterFeld("Z-Achse / dritte Ausgabe", config.achsen.z.orEmpty()) { ändern(config.copy(achsen = config.achsen.copy(z = it.trim().ifBlank { null }))) }
        BereichFeld("${methode?.parameter?.getOrNull(0)?.name ?: "X"}-Bereich", config.bereiche.x) { ändern(config.copy(bereiche = config.bereiche.copy(x = it))) }
        if (config.dimension != RaumDimension.R1) BereichFeld("${methode?.parameter?.getOrNull(1)?.name ?: "Y"}-Bereich", config.bereiche.y) { ändern(config.copy(bereiche = config.bereiche.copy(y = it))) }
        if (config.dimension == RaumDimension.R3) BereichFeld("${methode?.parameter?.getOrNull(2)?.name ?: "Z"}-Bereich", config.bereiche.z ?: ZahlenBereich(-10.0, 10.0)) { ändern(config.copy(bereiche = config.bereiche.copy(z = it))) }
        Text("Sampling", style = MaterialTheme.typography.titleSmall)
        if (config.dimension == RaumDimension.R1) ParameterFeld("R¹-Auflösung", config.sampling.auflösung1D.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(auflösung1D = n.coerceIn(16, 2_000)))) } }
        if (config.dimension == RaumDimension.R2) ParameterFeld("R²-Auflösung", config.sampling.auflösung2D.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(auflösung2D = n.coerceIn(16, 240)))) } }
        if (config.dimension == RaumDimension.R3) ParameterFeld("R³-Auflösung", config.sampling.auflösung3D.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(auflösung3D = n.coerceIn(8, 64)))) } }
        ParameterFeld("Gesamtbudget", config.sampling.maximalesRasterBudget.toString()) { it.toIntOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(maximalesRasterBudget = n.coerceIn(1_000, 2_000_000)))) } }
        ParameterFeld("Toleranz", config.sampling.toleranz.toString()) { it.toDoubleOrNull()?.let { n -> ändern(config.copy(sampling = config.sampling.copy(toleranz = n.coerceIn(1e-5, 2.0)))) } }
        Text("Farbe", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FarbModus.entries.forEach { modus -> FilterChip(config.farbe.modus == modus, { ändern(config.copy(farbe = config.farbe.copy(modus = modus))) }, label = { Text(when (modus) { FarbModus.Keine -> "Keine"; FarbModus.FesteFarbe -> "Fest"; FarbModus.Spektrum -> "Spektrum" }) }) } }
        if (config.farbe.modus == FarbModus.Spektrum) {
            ParameterFeld("Farbvariable", config.farbe.variable.orEmpty()) { ändern(config.copy(farbe = config.farbe.copy(variable = it.ifBlank { null }))) }
            Text("Palette", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Ozean", "Sonnenuntergang", "Wald").forEach { palette -> FilterChip(config.farbe.palette == palette, { ändern(config.copy(farbe = config.farbe.copy(palette = palette))) }, label = { Text(palette) }) } }
            BereichFeld("Farbwertbereich", config.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)) { ändern(config.copy(farbe = config.farbe.copy(bereich = it))) }
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onClick = { ändern(config.copy(kamera = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0))) }) { Text("Standardansicht wiederherstellen") }
    }
}

@Composable private fun ParameterFeld(label: String, wert: String, ändern: (String) -> Unit) { var text by remember(label, wert) { mutableStateOf(wert) }; OutlinedTextField(text, { text = it; ändern(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
@Composable private fun BereichFeld(label: String, bereich: ZahlenBereich, ändern: (ZahlenBereich) -> Unit) { var text by remember(label, bereich) { mutableStateOf("${bereich.minimum}, ${bereich.maximum}") }; OutlinedTextField(text, { text = it; val p = it.split(',').map(String::trim); if (p.size == 2) { val a = p[0].toDoubleOrNull(); val b = p[1].toDoubleOrNull(); if (a != null && b != null && a < b) ändern(ZahlenBereich(a, b)) } }, label = { Text("$label: Minimum, Maximum") }, modifier = Modifier.fillMaxWidth()) }
