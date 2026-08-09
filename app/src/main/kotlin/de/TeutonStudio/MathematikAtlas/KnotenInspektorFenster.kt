package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.vorschauKnotenErsetzen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.InspektorSichtbarkeit
import de.TeutonStudio.KnotenKartenVerwalter.zustand.MINDESTEINGÄNGE_PARAMETER
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.ANALYSIS_EIGENSCHAFT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MATRIX_EINZEL_EINGABEN
import de.TeutonStudio.MathematikKnoten.MATRIX_METHODE
import de.TeutonStudio.MathematikKnoten.MATRIX_SPALTEN
import de.TeutonStudio.MathematikKnoten.MATRIX_ZEILEN
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.RESTRIKTIONS_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.TUPEL_EINZEL_EINGABEN
import de.TeutonStudio.MathematikKnoten.TUPEL_METHODE
import de.TeutonStudio.MathematikKnoten.matrixKonfiguration
import de.TeutonStudio.MathematikKnoten.setzeMatrixKonfiguration
import de.TeutonStudio.MathematikKnoten.setzeTupelEingangAnzahl
import de.TeutonStudio.MathematikKnoten.setzeTupelKonfiguration
import de.TeutonStudio.MathematikKnoten.tupelKonfiguration
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import de.TeutonStudio.MathematikRechenSystem.kern.methodenSignatur

private const val STANDARDWERT_PREFIX = "standardwert."
private val INSPEKTOR_BREITE = 310.dp
private val INSPEKTOR_OBEN = 60.dp
private val INSPEKTOR_UNTEN = 152.dp
private val INSPEKTOR_RAND = 16.dp

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun Inspektor(zustand: AtlasZustand, modifier: Modifier) {
    val knoten = zustand.ausgewählterKnoten
    LaunchedEffect(knoten?.id) {
        if (knoten == null) InspektorSichtbarkeit.schließen()
    }
    if (!InspektorSichtbarkeit.offen || knoten == null) return

    Layout(
        modifier = Modifier.fillMaxHeight().zIndex(2f),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.large,
            ) {
                var knotenUmbenennenGeöffnet by remember(knoten.id) { mutableStateOf(false) }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Inspektor", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = InspektorSichtbarkeit::schließen) { Text("Schließen") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(knoten.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { knotenUmbenennenGeöffnet = true }) { Text("Umbenennen") }
                    }
                    if (knotenUmbenennenGeöffnet) {
                        NameÄndernDialog(
                            titel = "Knoten umbenennen",
                            aktuellerName = knoten.name,
                            schließen = { knotenUmbenennenGeöffnet = false },
                            bestätigen = { name ->
                                zustand.ersetzeKarteMitAuswahl(
                                    zustand.editor.karte.copy(
                                        knoten = zustand.editor.karte.knoten.map { aktuell ->
                                            if (aktuell.id == knoten.id) aktuell.copy(name = name) else aktuell
                                        },
                                    ),
                                )
                                zustand.speichereAktuell()
                                knotenUmbenennenGeöffnet = false
                            },
                        )
                    }
                    Text(knoten.art, style = MaterialTheme.typography.labelMedium)
                    StandardwerteEditor(knoten, zustand)
                    MethodenAusgangProjektionEditor(knoten, zustand)
                    if (knoten.kartenVerweis != null) KartenKnotenInspektor(knoten, zustand)
                    IterierteMethodenKartenInspektor(knoten, zustand)
                    if (knoten.art == MENGENKONSTRUKTOR_ART) MengenkonstruktorEditor(knoten, zustand)
                    val inspektor = when {
                        knoten.art == NOTIZ_KNOTEN_ART -> NotizKnotenInspektor
                        knoten.art == RESTRIKTIONS_KNOTEN_ART -> RestriktionsKnotenInspektor
                        knoten.art == ANALYSIS_EIGENSCHAFT_KNOTEN_ART -> AnalysisEigenschaftInspektor
                        else -> KnotenInspektorRegister.finde(knoten.art)
                    }
                    inspektor?.let {
                        it.Inhalt(
                            knoten,
                            zustand.auswertung.knoten[knoten.id],
                            object : KnotenInspektorAktionen {
                                override fun parameter(schlüssel: String, wert: String) {
                                    zustand.editor.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, wert))
                                }
                                override fun name(wert: String) {
                                    zustand.ersetzeKarteMitAuswahl(
                                        zustand.editor.karte.copy(
                                            knoten = zustand.editor.karte.knoten.map { aktuell ->
                                                if (aktuell.id == knoten.id) aktuell.copy(name = wert) else aktuell
                                            },
                                        ),
                                    )
                                }
                                override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) {
                                    zustand.editor.führeAus(KartenAktion.KnotenEigenschaftenErsetzen(knoten.id, eigenschaften))
                                }
                                override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) {
                                    zustand.editor.ändereAnschlussArt(verweis, art)
                                }
                                override fun knoten(knoten: KnotenDaten) {
                                    zustand.editor.führeAus(KartenAktion.KnotenErsetzen(knoten))
                                }
                                override fun vorschauKnotenErsetzen(knoten: KnotenDaten) =
                                    zustand.editor.karte.vorschauKnotenErsetzen(knoten)
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = zustand::dupliziereAuswahlMitMengendefinition) { Text("Duplizieren") }
                            Button(
                                onClick = zustand::löscheAuswahlMitMengendefinition,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) { Text("Löschen") }
                        }
                        return@Column
                    }
                    if (knoten.art == "mathematik.matrix") MatrixInspektor(knoten, zustand)
                    if (knoten.art == "mathematik.tupel") TupelInspektor(knoten, zustand)
                    if (knoten.art in setOf("mathematik.addition", "mathematik.multiplikation", "mathematik.extremwert", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.vektor", "mathematik.zeilenVektor") ||
                        knoten.art == "mathematik.tupel" && tupelKonfiguration(knoten).erzeugungsArt == TUPEL_EINZEL_EINGABEN
                    ) {
                        val mindestAnzahl = if (knoten.art == "mathematik.tupel") 1 else 2
                        val wert = knoten.parameter["festeEingänge"] ?: "2"
                        var text by remember(knoten.id, wert) { mutableStateOf(wert) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                it.toIntOrNull()
                                    ?.takeIf { anzahl -> anzahl >= mindestAnzahl }
                                    ?.let { anzahl ->
                                        if (knoten.art == "mathematik.tupel") {
                                            zustand.editor.setzeTupelEingangAnzahl(knoten.id, anzahl)
                                        } else {
                                            zustand.editor.setzeFesteEingangAnzahl(knoten.id, anzahl)
                                        }
                                    }
                            },
                            label = { Text("Feste Eingänge") },
                            supportingText = {
                                Text("Mindestens $mindestAnzahl; weitere Eingänge entstehen beim Verbinden.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val zeigeWerte = knoten.parameter["operatorAnzeige"] != "name"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Anzeige: Namen", modifier = Modifier.weight(1f))
                            Switch(
                                checked = zeigeWerte,
                                onCheckedChange = { werte ->
                                    zustand.editor.führeAus(
                                        KartenAktion.KnotenParameterÄndern(
                                            knoten.id,
                                            "operatorAnzeige",
                                            if (werte) "wert" else "name",
                                        ),
                                    )
                                },
                            )
                            Text("Werte")
                        }
                    }
                    if (knoten.art == "mathematik.extremwert") {
                        Text("Modus: ${if (knoten.parameter["modus"] == "minimum") "Minimum" else "Maximum"}")
                    }
                    knoten.parameter.filterKeys {
                        it !in setOf(
                            "festeEingänge", "operatorAnzeige", "modus", "erzeugungsArt", "höhe", "breite",
                            "werteVorrat", "zielmenge", "argumentReihenfolge", MINDESTEINGÄNGE_PARAMETER,
                            MENGENDEFINITION_PAAR, MENGENDEFINITION_MENGENNAME, MENGENDEFINITION_ELEMENTNAME,
                            MENGENDEFINITION_ELEMENTART, MENGENDEFINITION_ELEMENTMENGE,
                        ) && !it.startsWith(STANDARDWERT_PREFIX) &&
                            !it.startsWith("faltung.") && !it.startsWith("methodenAnwendung.") &&
                            !it.startsWith(METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX)
                    }.forEach { (schlüssel, wert) ->
                        var text by remember(knoten.id, schlüssel, wert) { mutableStateOf(wert) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                zustand.editor.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
                            },
                            label = { Text(schlüssel) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = zustand::dupliziereAuswahlMitMengendefinition) { Text("Duplizieren") }
                        Button(
                            onClick = zustand::löscheAuswahlMitMengendefinition,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Löschen") }
                    }
                }
            }
        },
    ) { messbare, beschränkungen ->
        val breite = INSPEKTOR_BREITE.roundToPx().coerceAtMost(beschränkungen.maxWidth)
        val oben = INSPEKTOR_OBEN.roundToPx()
        val unten = INSPEKTOR_UNTEN.roundToPx()
        val rand = INSPEKTOR_RAND.roundToPx()
        val höhe = (beschränkungen.maxHeight - oben - unten).coerceAtLeast(0)
        val panel = messbare.single().measure(Constraints.fixed(breite, höhe))
        layout(0, beschränkungen.maxHeight) {
            panel.placeRelative(x = -breite - rand, y = oben)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TupelInspektor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val konfiguration = tupelKonfiguration(knoten)
    val erzeugungsArten = listOf(
        TUPEL_EINZEL_EINGABEN to "Elemente einzeln",
        TUPEL_METHODE to "Dimension und Indexmethode",
    )
    var menüGeöffnet by remember(knoten.id, konfiguration.erzeugungsArt) { mutableStateOf(false) }
    HorizontalDivider()
    Text("Tupel erzeugen", style = MaterialTheme.typography.titleSmall)
    ExposedDropdownMenuBox(
        expanded = menüGeöffnet,
        onExpandedChange = { menüGeöffnet = it },
    ) {
        OutlinedTextField(
            value = erzeugungsArten.first { it.first == konfiguration.erzeugungsArt }.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("Erzeugungsart") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menüGeöffnet) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = menüGeöffnet,
            onDismissRequest = { menüGeöffnet = false },
        ) {
            erzeugungsArten.forEach { (art, bezeichnung) ->
                DropdownMenuItem(
                    text = { Text(bezeichnung) },
                    onClick = {
                        menüGeöffnet = false
                        if (art != konfiguration.erzeugungsArt) {
                            zustand.editor.setzeTupelKonfiguration(knoten.id, art)
                        }
                    },
                )
            }
        }
    }
    Text(
        if (konfiguration.erzeugungsArt == TUPEL_METHODE) {
            "Erzeugt für eine konkrete ganze Dimension n ≥ 1 das Tupel (f(1), …, f(n)). Die einstellige Methode darf beliebige mathematische Objekte liefern."
        } else {
            "Erzeugt ein Zahlentupel aus den geordneten Eingängen."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "Beim Wechsel der Erzeugungsart werden inkompatible Verbindungen gemeinsam mit der Änderung entfernt und können per Rückgängig wiederhergestellt werden.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MengenkonstruktorEditor(knoten: KnotenDaten, zustand: AtlasZustand) {
    HorizontalDivider()
    Text("Mengendefinition", style = MaterialTheme.typography.titleSmall)
    ParameterTextFeld("Mengenname", knoten, MENGENDEFINITION_MENGENNAME, "M", zustand)
    ParameterTextFeld("Elementname", knoten, MENGENDEFINITION_ELEMENTNAME, "x", zustand)

    val aktuelleArtId = AnschlussArtId(
        knoten.parameter[MENGENDEFINITION_ELEMENTART]?.takeIf(String::isNotBlank)
            ?: MathematikAnschlussArten.Zahl.id.wert,
    )
    val aktuelleArt = MathematikAnschlussArten.alle.firstOrNull { it.id == aktuelleArtId }
    var geöffnet by remember(knoten.id, aktuelleArtId) { mutableStateOf(false) }

    Text("Elementtyp", style = MaterialTheme.typography.titleSmall)
    ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
        OutlinedTextField(
            value = aktuelleArt?.name ?: aktuelleArtId.wert,
            onValueChange = {},
            readOnly = true,
            label = { Text("Anschlussart") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
            MathematikAnschlussArten.alle.forEach { art ->
                DropdownMenuItem(
                    text = { Text(art.name) },
                    onClick = {
                        geöffnet = false
                        val elementAusgang = knoten.anschlüsse.firstOrNull {
                            it.richtung == AnschlussRichtung.Ausgang && it.name == "element"
                        } ?: return@DropdownMenuItem
                        zustand.editor.führeAus(
                            KartenAktion.KnotenKonfigurationErsetzen(
                                id = knoten.id,
                                parameter = (knoten.parameter - MENGENDEFINITION_ELEMENTMENGE) +
                                    (MENGENDEFINITION_ELEMENTART to art.id.wert),
                                anschlüsse = knoten.anschlüsse,
                            ),
                        )
                        zustand.editor.ändereAnschlussArt(
                            AnschlussVerweis(knoten.id, elementAusgang.id),
                            art.id,
                        )
                    },
                )
            }
        }
    }
    Text(
        "Der Konstruktor bindet nur Name und Anschlussart des Arguments. Eine Obermenge wird bei Bedarf als Prädikat wie x ∈ A in der Aussagekette modelliert; Operationen mit zwingender Obermenge melden deren Fehlen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "Nur ein Aussageelement kann direkt an den Aussageeingang des Definators angeschlossen werden.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ParameterTextFeld(
    label: String,
    knoten: KnotenDaten,
    schlüssel: String,
    standard: String,
    zustand: AtlasZustand,
) {
    val wert = knoten.parameter[schlüssel] ?: standard
    var text by remember(knoten.id, schlüssel, wert) { mutableStateOf(wert) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            zustand.editor.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StandardwerteEditor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val zahlEingänge = knoten.anschlüsse.filter {
        it.richtung == AnschlussRichtung.Eingang &&
            it.art == MathematikAnschlussArten.Zahl.id &&
            !it.dynamischErzeugt
    }
    if (zahlEingänge.isEmpty()) return

    HorizontalDivider()
    Text("Standardwerte", style = MaterialTheme.typography.titleSmall)
    Text(
        "Ein Standardwert wird nur verwendet, solange der zugehörige Zahl-Eingang nicht verbunden ist.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    zahlEingänge.forEach { anschluss ->
        val schlüssel = "$STANDARDWERT_PREFIX${anschluss.name}"
        val wert = knoten.parameter[schlüssel].orEmpty()
        var text by remember(knoten.id, anschluss.id, wert) { mutableStateOf(wert) }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                zustand.editor.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
            },
            label = { Text("Standardwert: ${anschluss.name}") },
            supportingText = { Text("Ganze Zahl oder Bruch, beispielsweise -1 oder 1/2.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MethodenAusgangProjektionEditor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val methodenAusgänge = knoten.anschlüsse.filter {
        it.richtung == AnschlussRichtung.Ausgang && it.art == MathematikAnschlussArten.Methode.id
    }
    if (methodenAusgänge.isEmpty()) return

    HorizontalDivider()
    Text("Methodenprojektion", style = MaterialTheme.typography.titleSmall)
    Text(
        "Argument- und Ergebnisprojektion verändern die Karten-Schnittstelle der Methode, nicht ihre geordnete Parameterliste oder Rechenvorschrift.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    methodenAusgänge.forEach { ausgang ->
        if (methodenAusgänge.size > 1) {
            Text(ausgang.name, style = MaterialTheme.typography.labelLarge)
        }

        Text("Argumente", style = MaterialTheme.typography.labelMedium)
        val argumentProjektion = knoten.methodenAusgangArgumentprojektion(ausgang.name)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = argumentProjektion == METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
                onClick = {
                    zustand.editor.führeAus(
                        KartenAktion.KnotenParameterÄndern(
                            knoten.id,
                            methodenAusgangArgumentprojektionSchlüssel(ausgang.name),
                            METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
                        ),
                    )
                },
                label = { Text("Separiert") },
            )
            FilterChip(
                selected = argumentProjektion == METHODEN_ARGUMENTPROJEKTION_TUPEL,
                onClick = {
                    zustand.editor.führeAus(
                        KartenAktion.KnotenParameterÄndern(
                            knoten.id,
                            methodenAusgangArgumentprojektionSchlüssel(ausgang.name),
                            METHODEN_ARGUMENTPROJEKTION_TUPEL,
                        ),
                    )
                },
                label = { Text("Ein Tupel") },
            )
        }

        val methode = zustand.auswertung.knoten[knoten.id]
            ?.ausgaben
            ?.get(ausgang.name)
            ?.objekt as? Methode
        val zielIstTupel = methode?.let {
            runCatching { it.methodenSignatur().zielMenge is Tupelraum }.getOrDefault(false)
        } == true
        Text("Ergebnis", style = MaterialTheme.typography.labelMedium)
        if (zielIstTupel) {
            Text(
                "Die Zielmenge ist bereits ein Tupelraum; das Ergebnis bleibt ein Tupel und wird nicht nochmals verpackt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val ergebnisProjektion = knoten.methodenAusgangErgebnisprojektion(ausgang.name)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_DIREKT,
                    onClick = {
                        zustand.editor.führeAus(
                            KartenAktion.KnotenParameterÄndern(
                                knoten.id,
                                methodenAusgangErgebnisprojektionSchlüssel(ausgang.name),
                                METHODEN_ERGEBNISPROJEKTION_DIREKT,
                            ),
                        )
                    },
                    label = { Text("Direkt") },
                )
                FilterChip(
                    selected = ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL,
                    onClick = {
                        zustand.editor.führeAus(
                            KartenAktion.KnotenParameterÄndern(
                                knoten.id,
                                methodenAusgangErgebnisprojektionSchlüssel(ausgang.name),
                                METHODEN_ERGEBNISPROJEKTION_TUPEL,
                            ),
                        )
                    },
                    label = { Text("1D-Tupel") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatrixInspektor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val konfiguration = matrixKonfiguration(knoten)
    val erzeugungsArten = listOf(
        MATRIX_EINZEL_EINGABEN to "Elemente einzeln",
        MATRIX_ZEILEN to "Zeilen anschließen",
        MATRIX_SPALTEN to "Spalten anschließen",
        MATRIX_METHODE to "Indexfunktion f(x, y)",
    )
    var artMenüGeöffnet by remember(knoten.id, konfiguration.erzeugungsArt) { mutableStateOf(false) }
    var höheText by remember(knoten.id, konfiguration.höhe) { mutableStateOf(konfiguration.höhe.toString()) }
    var breiteText by remember(knoten.id, konfiguration.breite) { mutableStateOf(konfiguration.breite.toString()) }
    HorizontalDivider()
    Text("Matrix erzeugen", style = MaterialTheme.typography.titleSmall)
    ExposedDropdownMenuBox(
        expanded = artMenüGeöffnet,
        onExpandedChange = { artMenüGeöffnet = it },
    ) {
        OutlinedTextField(
            value = erzeugungsArten.first { it.first == konfiguration.erzeugungsArt }.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("Erzeugungsart") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = artMenüGeöffnet) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = artMenüGeöffnet,
            onDismissRequest = { artMenüGeöffnet = false },
        ) {
            erzeugungsArten.forEach { (art, bezeichnung) ->
                DropdownMenuItem(
                    text = { Text(bezeichnung) },
                    onClick = {
                        artMenüGeöffnet = false
                        if (art != konfiguration.erzeugungsArt) {
                            zustand.editor.setzeMatrixKonfiguration(
                                knoten.id,
                                art,
                                konfiguration.höhe,
                                konfiguration.breite,
                            )
                        }
                    },
                )
            }
        }
    }
    Text(
        "Beim Wechsel der Erzeugungsart werden inkompatible Verbindungen gemeinsam mit der Änderung entfernt und können per Rückgängig wiederhergestellt werden.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = höheText,
            onValueChange = { text ->
                höheText = text
                text.toIntOrNull()?.takeIf { it > 0 }?.let { höhe ->
                    zustand.editor.setzeMatrixKonfiguration(knoten.id, konfiguration.erzeugungsArt, höhe, konfiguration.breite)
                }
            },
            label = { Text("Höhe") },
            modifier = Modifier.weight(1f),
            supportingText = { Text("≥ 1") },
        )
        OutlinedTextField(
            value = breiteText,
            onValueChange = { text ->
                breiteText = text
                text.toIntOrNull()?.takeIf { it > 0 }?.let { breite ->
                    zustand.editor.setzeMatrixKonfiguration(knoten.id, konfiguration.erzeugungsArt, konfiguration.höhe, breite)
                }
            },
            label = { Text("Breite") },
            modifier = Modifier.weight(1f),
            supportingText = { Text("≥ 1") },
        )
    }
    when (konfiguration.erzeugungsArt) {
        MATRIX_METHODE -> Text(
            "Indexmenge: {0,…,${konfiguration.höhe - 1}} × {0,…,${konfiguration.breite - 1}}. Die Zahlmethode wird als f(Zeile, Spalte) ausgewertet.",
            style = MaterialTheme.typography.bodySmall,
        )
        MATRIX_ZEILEN -> Text(
            "Erwartet ${konfiguration.höhe} Zeilenvektoren mit jeweils ${konfiguration.breite} Elementen.",
            style = MaterialTheme.typography.bodySmall,
        )
        MATRIX_SPALTEN -> Text(
            "Erwartet ${konfiguration.breite} Spaltenvektoren mit jeweils ${konfiguration.höhe} Elementen.",
            style = MaterialTheme.typography.bodySmall,
        )
        else -> Text(
            "Erwartet ${konfiguration.höhe * konfiguration.breite} einzelne Zahlwerte.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
