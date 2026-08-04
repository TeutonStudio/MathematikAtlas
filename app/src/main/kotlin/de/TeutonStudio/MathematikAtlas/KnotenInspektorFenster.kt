package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.InspektorSichtbarkeit
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MATRIX_EINZEL_EINGABEN
import de.TeutonStudio.MathematikKnoten.MATRIX_METHODE
import de.TeutonStudio.MathematikKnoten.MATRIX_SPALTEN
import de.TeutonStudio.MathematikKnoten.MATRIX_ZEILEN
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.matrixKonfiguration
import de.TeutonStudio.MathematikKnoten.setzeMatrixKonfiguration

private const val STANDARDWERT_PREFIX = "standardwert."

@Composable
internal fun Inspektor(zustand: AtlasZustand, modifier: Modifier) {
    if (!InspektorSichtbarkeit.offen) return
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        val knoten = zustand.ausgewählterKnoten
        var knotenUmbenennenGeöffnet by remember(knoten?.id) { mutableStateOf(false) }
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Inspektor", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = InspektorSichtbarkeit::schließen) { Text("Schließen") }
            }
            if (knoten == null) {
                Text("Wähle einen Knoten oder eine Verbindung aus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                zustand.editor.ausgewählteVerbindung?.let { id ->
                    Text("Verbindung ${id.wert.take(8)}")
                    Button(onClick = zustand.editor::löscheAuswahl) { Text("Verbindung löschen") }
                }
                return@Column
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
            if (knoten.kartenVerweis != null) KartenKnotenInspektor(knoten, zustand)
            IterierteMethodenKartenInspektor(knoten, zustand)
            if (knoten.art == MENGENKONSTRUKTOR_ART) MengenkonstruktorEditor(knoten, zustand)
            KnotenInspektorRegister.finde(knoten.art)?.let { inspektor ->
                inspektor.Inhalt(
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
                    },
                )
                Spacer(Modifier.weight(1f))
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
            if (knoten.art in setOf("mathematik.addition", "mathematik.multiplikation", "mathematik.extremwert", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.tupel", "mathematik.vektor", "mathematik.zeilenVektor")) {
                val wert = knoten.parameter["festeEingänge"] ?: "2"
                var text by remember(knoten.id, wert) { mutableStateOf(wert) }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        it.toIntOrNull()?.let { anzahl -> zustand.editor.setzeFesteEingangAnzahl(knoten.id, anzahl) }
                    },
                    label = { Text("Feste Eingänge") },
                    supportingText = { Text("Mindestens 2; weitere Eingänge entstehen beim Verbinden.") },
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
                    "werteVorrat", "zielmenge", "argumentReihenfolge", MENGENDEFINITION_PAAR,
                    MENGENDEFINITION_MENGENNAME, MENGENDEFINITION_ELEMENTNAME,
                    MENGENDEFINITION_ELEMENTART, MENGENDEFINITION_ELEMENTMENGE,
                ) && !it.startsWith(STANDARDWERT_PREFIX) &&
                    !it.startsWith("faltung.") && !it.startsWith("methodenAnwendung.")
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
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = zustand::dupliziereAuswahlMitMengendefinition) { Text("Duplizieren") }
                Button(
                    onClick = zustand::löscheAuswahlMitMengendefinition,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Löschen") }
            }
        }
    }
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
