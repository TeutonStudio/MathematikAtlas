package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasBestätigungsDialog

import de.TeutonStudio.MathematikKnoten.*

import de.TeutonStudio.MathematikRechenSystem.kern.mathematischeMethodenSignatur
import de.TeutonStudio.MathematikRechenSystem.kern.zielMenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.vorschauKnotenErsetzen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.MINDESTEINGÄNGE_PARAMETER
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.ANALYSIS_EIGENSCHAFT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MATRIX_EINZEL_EINGABEN
import de.TeutonStudio.MathematikKnoten.MATRIX_METHODE
import de.TeutonStudio.MathematikKnoten.MATRIX_SPALTEN
import de.TeutonStudio.MathematikKnoten.MATRIX_ZEILEN
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.konfiguriereMatrix
import de.TeutonStudio.MathematikKnoten.konfiguriereTupel
import de.TeutonStudio.MathematikKnoten.konfiguriereMengenkonstruktor
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
    val klappzustand = remember { InspektorKlappZustand() }
    val knoten = zustand.ausgewählterKnoten
    var visualisierungsId by remember { mutableStateOf<de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId?>(null) }
    visualisierungsId?.let { id -> de.TeutonStudio.MathematikAtlas.AtlasVisualisierungsDialog(zustand, id) { visualisierungsId = null } }

    LaunchedEffect(knoten?.id) {
        if (knoten == null) InspektorSichtbarkeit.schließen()
    }
    if (!InspektorSichtbarkeit.offen || knoten == null) return

    zustand.ausstehendeKonfiguration?.let { kandidat ->
        val auswirkung = zustand.vorschauKonfiguration(kandidat)
        AtlasBestätigungsDialog(
            onDismissRequest = zustand::verwerfeKonfiguration,
            title = { Text("Operanden entfernen?") },
            text = { Text("Diese Konfiguration entfernt ${auswirkung.entfallendeVerbindungen.size} Verbindung(en) und ${auswirkung.entfallendeLokaleWerte.size} lokale Werte/Kartenfallbacks (${auswirkung.entfallendeLokaleWerte.joinToString()}). Rückgängig stellt den bisherigen Stand wieder her.") },
            confirmButton = { TextButton(onClick = zustand::bestätigeKonfiguration) { Text("Übernehmen") } },
            dismissButton = { TextButton(onClick = zustand::verwerfeKonfiguration) { Text("Abbrechen") } },
        )
    }

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
                val ergebnis = zustand.auswertung.knoten[knoten.id]
                val meldungen = inspektorMeldungen(knoten, ergebnis, zustand.editor.meldungenFür(knoten.id))
                val inspektor = when {
                    knoten.art == NOTIZ_KNOTEN_ART -> NotizKnotenInspektor
                    knoten.art == RESTRIKTIONS_KNOTEN_ART -> RestriktionsKnotenInspektor
                    knoten.art == ANALYSIS_EIGENSCHAFT_KNOTEN_ART -> AnalysisEigenschaftInspektor
                    else -> KnotenInspektorRegister.finde(knoten.art)
                }
                val aktionen = object : KnotenInspektorAktionen {
                    override fun parameter(schlüssel: String, wert: String) {
                        zustand.konfiguriereKnoten(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, wert))
                    }
                    override fun name(wert: String) {
                        val aktuell = zustand.editor.karte.knoten.firstOrNull { it.id == knoten.id }
                        if (wert.isNotBlank() && aktuell != null) zustand.beantrageKonfiguration(aktuell.copy(name = wert.trim()))
                    }
                    override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) {
                        zustand.konfiguriereKnoten(KartenAktion.KnotenEigenschaftenErsetzen(knoten.id, eigenschaften))
                    }
                    override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) {
                        zustand.beantrageKonfiguration(knoten.copy(anschlüsse = knoten.anschlüsse.map {
                            if (it.id == verweis.anschlussId) it.copy(art = art) else it
                        }))
                    }
                    override fun knoten(knoten: KnotenDaten) {
                        zustand.konfiguriereKnoten(KartenAktion.KnotenErsetzen(knoten))
                    }
                    override fun knotenNachVerlustbestätigung(knoten: KnotenDaten) {
                        zustand.übernehmeKonfiguration(knoten)
                    }
                    override fun vorschauKnotenErsetzen(knoten: KnotenDaten) =
                        zustand.vorschauKonfiguration(knoten)
                }
                val fachErgebnis = ergebnis?.copy(fehler = null)
                val eingangsOptionen = inspektor?.hatBereich(knoten, KnotenInspektorBereich.Eingangsdaten) == true ||
                    hatGemeinsameEingangsEinstellungen(knoten, zustand.editor.karte)
                val ausgangsOptionen = inspektor?.hatBereich(knoten, KnotenInspektorBereich.Ausgangsdaten) == true
                val hatKnotendaten = inspektor?.hatBereich(knoten, KnotenInspektorBereich.Knotendaten) == true ||
                    meldungen.isNotEmpty() || !ergebnis?.ausgaben.isNullOrEmpty() ||
                    knoten.kartenVerweis != null || inspektor == null && knoten.parameter.isNotEmpty() ||
                    knoten.art.startsWith(GEOMETRY_NODES_PREFIX)
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = LocalAtlasAbstände.current.inhalt),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Inspektor", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = InspektorSichtbarkeit::schließen) { Text("Schließen") }
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(LocalAtlasAbstände.current.inhalt),
                        verticalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.bereich)) {
                        KnotenInspektorBereiche(
                            karte = zustand.editor.karte, knoten = knoten, klappzustand = klappzustand,
                            standarddaten = {
                                KnotenStandardDatenEditor(knoten,
                                    if (knoten.art == NOTIZ_KNOTEN_ART) KnotenDarstellungsForm.entries.toSet() else setOf(knoten.darstellungsForm),
                                    übernehmen = zustand::beantrageKonfiguration)
                            },
                            knotendaten = if (!hatKnotendaten) null else ({
                                meldungen.forEach { Text(it.text, color = if (it.art == KnotenMeldungsArt.Fehler) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                                if (knoten.art == "mathematik.visualisierung") Button(onClick = { visualisierungsId = knoten.id }) { Text("Visualisierung öffnen") }
                                FormelKartenAktionen(zustand.editor.karte, knoten) { aktion ->
                                    zustand.editor.führeAus(aktion); zustand.aktualisiereAuswertung()
                                }
                                ErgebnisVertragsInhalt(fachErgebnis)
                                GeometryNodesEinstellungen(knoten, zustand::beantrageKonfiguration)
                                VisualisierungsVertragsEinstellungen(knoten, fachErgebnis, zustand::beantrageKonfiguration)
                                if (knoten.kartenVerweis != null && knoten.art != METHODEN_QUELLE_ART) KartenKnotenInspektor(knoten, zustand)
                                if (knoten.art != NOTIZ_KNOTEN_ART) KnotenAuswertungsDiagnose(knoten, zustand)
                                if (knoten.art == MENGENKONSTRUKTOR_ART) MengenkonstruktorEditor(knoten, zustand)
                                if (inspektor != null) inspektor.BereichInhalt(knoten, fachErgebnis, aktionen, KnotenInspektorBereich.Knotendaten)
                                else GenerischeKnotenDaten(knoten, zustand)
                            }),
                            eingangsEinstellungen = if (!eingangsOptionen) null else ({
                                GemeinsameEingangsEinstellungen(knoten, zustand)
                                inspektor?.BereichInhalt(knoten, fachErgebnis, aktionen, KnotenInspektorBereich.Eingangsdaten)
                            }),
                            ausgangsEinstellungen = if (!ausgangsOptionen) null else ({
                                inspektor?.BereichInhalt(knoten, fachErgebnis, aktionen, KnotenInspektorBereich.Ausgangsdaten)
                            }),
                            anschlussTyp = { port -> MathematikAnschlussArten.alle.firstOrNull { it.id == port.art }?.name ?: port.art.wert },
                            diagnose = meldungen.takeIf { it.isNotEmpty() }?.let { "${it.count { m -> m.art == KnotenMeldungsArt.Fehler }} Fehler · ${it.size} Meldung(en)" },
                        ) { port ->
                            InspektorAnschlussDaten(zustand.editor.karte, knoten, port, zustand.auswertung.knoten, zustand::beantrageKonfiguration)
                            inspektor?.AnschlussInhalt(knoten, port, fachErgebnis, aktionen)
                            if (port.richtung == AnschlussRichtung.Eingang && port.name == "methode") IterierteMethodenKartenInspektor(knoten, zustand, zeigeDiagnose = false)
                        }
                    }
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal = LocalAtlasAbstände.current.inhalt),
                        horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
                        OutlinedButton(onClick = zustand::dupliziereAuswahlMitMengendefinition) { Text("Duplizieren") }
                        TextButton(onClick = zustand::löscheAuswahlMitMengendefinition) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

        },
    ) { messbare, beschränkungen ->
        val rand = INSPEKTOR_RAND.roundToPx().coerceAtMost(beschränkungen.maxWidth / 12)
        val breite = INSPEKTOR_BREITE.roundToPx().coerceAtMost((beschränkungen.maxWidth - 2 * rand).coerceAtLeast(0))
        // MainActivity zieht die IME bereits ab. Bei geringer Resthöhe erhält
        // der Inspector den ganzen verbleibenden Platz für Titel, Scrollen und Aktionen.
        val kompakt = beschränkungen.maxHeight < 480.dp.roundToPx()
        val oben = if (kompakt) 0 else INSPEKTOR_OBEN.roundToPx()
        val unten = if (kompakt) 0 else INSPEKTOR_UNTEN.roundToPx().coerceAtMost(beschränkungen.maxHeight / 5)
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
                            zustand.beantrageKonfiguration(konfiguriereTupel(knoten, art))
                        }
                    },
                )
            }
        }
    }
    Text(
        if (konfiguration.erzeugungsArt == TUPEL_METHODE) {
            "Erzeugt für eine konkrete ganze Dimension n ≥ 0 das Tupel (f(1), …, f(n)); für n = 0 entsteht (). Die einstellige Methode darf beliebige mathematische Objekte liefern."
        } else {
            "Erzeugt ein Tupel aus den geordneten Eingängen; bei 0 Eingängen entsteht ()."
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
                        val konfiguriert = konfiguriereMengenkonstruktor(knoten, art.id)
                        zustand.konfiguriereKnoten(
                            KartenAktion.KnotenKonfigurationErsetzen(
                                id = knoten.id,
                                parameter = konfiguriert.parameter,
                                anschlüsse = konfiguriert.anschlüsse,
                            ),
                        )
                    },
                )
            }
        }
    }
    val istStruktur = aktuelleArtId in setOf(
        MathematikAnschlussArten.Tupel.id,
        MathematikAnschlussArten.SpaltenVektor.id,
        MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.Matrix.id,
        MathematikAnschlussArten.Tensor.id,
    )
    if (istStruktur) {
        val modus = knoten.parameter[MENGENDEFINITION_FORMMODUS] ?: STRUKTURFORM_UNBEKANNT
        val eingabe = knoten.parameter[MENGENDEFINITION_FORMEINGABE] ?: STRUKTURFORM_EINZELN
        val form = knoten.parameter[MENGENDEFINITION_FORM].orEmpty()

        fun aktualisiere(neuerModus: String = modus, neueEingabe: String = eingabe, neueForm: String = form) {
            val neu = konfiguriereMengenkonstruktor(knoten, aktuelleArtId, neuerModus, neueEingabe, neueForm)
            zustand.konfiguriereKnoten(
                KartenAktion.KnotenKonfigurationErsetzen(knoten.id, neu.parameter, neu.anschlüsse),
            )
        }

        Text("Strukturform", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng), modifier = Modifier.fillMaxWidth()) {
            listOf(
                STRUKTURFORM_UNBEKANNT to "Unbekannt",
                STRUKTURFORM_INSPEKTOR to "Inspector",
                STRUKTURFORM_EINGANG to "Eingang",
            ).forEach { (wert, titel) ->
                FilterChip(selected = modus == wert, onClick = { aktualisiere(neuerModus = wert) }, label = { Text(titel) })
            }
        }
        if (modus == STRUKTURFORM_INSPEKTOR || aktuelleArtId == MathematikAnschlussArten.Tensor.id) {
            OutlinedTextField(
                value = form,
                onValueChange = { aktualisiere(neueForm = it) },
                label = { Text(if (modus == STRUKTURFORM_INSPEKTOR) "Form" else "Achsen/Rang") },
                supportingText = {
                    Text(
                        if (modus == STRUKTURFORM_INSPEKTOR) "Positive Ganzzahlen oder Variablen, kommagetrennt"
                        else "Bestimmt im Einzelmodus die Anzahl der Dimensionsanschlüsse",
                    )
                },
                isError = modus == STRUKTURFORM_INSPEKTOR && runCatching { parseStrukturForm(form) }.isFailure,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (modus == STRUKTURFORM_EINGANG) {
            Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
                FilterChip(
                    selected = eingabe == STRUKTURFORM_EINZELN,
                    onClick = { aktualisiere(neueEingabe = STRUKTURFORM_EINZELN) },
                    label = { Text("Einzeln") },
                )
                FilterChip(
                    selected = eingabe == STRUKTURFORM_TUPEL,
                    onClick = { aktualisiere(neueEingabe = STRUKTURFORM_TUPEL) },
                    label = { Text("Zahlentupel") },
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
            zustand.konfiguriereKnoten(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
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
                            zustand.beantrageKonfiguration(konfiguriereMatrix(knoten, art, konfiguration.höhe, konfiguration.breite))
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
    Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
        OutlinedTextField(
            value = höheText,
            onValueChange = { text ->
                höheText = text
                text.toIntOrNull()?.takeIf { it > 0 }?.let { höhe ->
                    zustand.beantrageKonfiguration(konfiguriereMatrix(knoten, konfiguration.erzeugungsArt, höhe, konfiguration.breite))
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
                    zustand.beantrageKonfiguration(konfiguriereMatrix(knoten, konfiguration.erzeugungsArt, konfiguration.höhe, breite))
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

private fun hatGenerischeEingangsEinstellungen(knoten: KnotenDaten): Boolean =
    KnotenInspektorRegister.finde(knoten.art) == null &&
    (knoten.art in setOf("mathematik.matrixProdukt", "mathematik.addition", "mathematik.multiplikation", "mathematik.extremwert", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.vektor", "mathematik.zeilenVektor") ||
        knoten.art == "mathematik.tupel" && tupelKonfiguration(knoten).erzeugungsArt == TUPEL_EINZEL_EINGABEN)

private fun hatGemeinsameEingangsEinstellungen(knoten: KnotenDaten, karte: KartenDaten): Boolean =
    istDynamischeOperation(knoten) && de.TeutonStudio.KnotenKartenVerwalter.logik.dynamischeOperandengruppen(karte, knoten, belegteOperationsEingänge(knoten)).isNotEmpty() || hatGenerischeEingangsEinstellungen(knoten) || knoten.art == METHODEN_GRAPH_KNOTEN_ART ||
        knoten.art == METHODEN_AUFRUF_ART || knoten.art == "mathematik.visualisierung" &&
        (knoten.anschlüsse.any { it.richtung == AnschlussRichtung.Ausgang } || knoten.anschlüsse.count { it.kannSichErweitern } < 2)

@Composable
private fun GemeinsameEingangsEinstellungen(knoten: KnotenDaten, zustand: AtlasZustand) {
    if (istDynamischeOperation(knoten) || knoten.art == "mathematik.tupel" && tupelKonfiguration(knoten).erzeugungsArt == TUPEL_EINZEL_EINGABEN)
        DynamischeOperandenListe(zustand.editor.karte, knoten, belegteOperationsEingänge(knoten)) { zustand.konfiguriereKnoten(it) }
    GraphEingangsEinstellungen(knoten, zustand::beantrageKonfiguration)
    de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsAnschlussEinstellungen(knoten, zustand::beantrageKonfiguration)
    if (knoten.art == METHODEN_AUFRUF_ART) MethodenProjektionsEinstellungen(knoten, zustand.auswertung.knoten[knoten.id], zustand::beantrageKonfiguration, nurAufruf = true)
    if (hatGenerischeEingangsEinstellungen(knoten)) {
        if (knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach }) {
            val mindestAnzahl = if (knoten.art == "mathematik.tupel") 0 else 2
            val wert = knoten.parameter["festeEingänge"] ?: "2"
            var text by remember(knoten.id, wert) { mutableStateOf(wert) }
            val anzahl = text.toIntOrNull()?.takeIf { it >= mindestAnzahl }
            OutlinedTextField(text, { text = it }, label = { Text("Feste Eingänge") },
                isError = anzahl == null, supportingText = { Text("Mindestens $mindestAnzahl; weitere Eingänge entstehen beim Verbinden.") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            TextButton(enabled = anzahl != null && anzahl.toString() != wert, onClick = {
                anzahl?.let { n ->
                    if (knoten.art == "mathematik.tupel") zustand.beantrageKonfiguration(konfiguriereTupel(knoten.copy(parameter = knoten.parameter + ("festeEingänge" to n.toString())), TUPEL_EINZEL_EINGABEN))
                    else zustand.konfiguriereFesteEingänge(knoten.id, n)
                }
            }) { Text("Eingangsanzahl übernehmen") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Eingangsanzeige: Namen", Modifier.weight(1f))
            Switch(checked = knoten.parameter["operatorAnzeige"] != "name", onCheckedChange = {
                zustand.konfiguriereKnoten(KartenAktion.KnotenParameterÄndern(knoten.id, "operatorAnzeige", if (it) "wert" else "name"))
            })
            Text("Werte")
        }
    }
}

@Composable
private fun GenerischeKnotenDaten(knoten: KnotenDaten, zustand: AtlasZustand) {
    if (GeometryNodesVorlagen.finde(knoten.art) != null) return
    if (knoten.art == "mathematik.matrix") MatrixInspektor(knoten, zustand)
    if (knoten.art == "mathematik.tupel") TupelInspektor(knoten, zustand)
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
            !it.startsWith(METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX) && !it.startsWith("join.")
    }.forEach { (schlüssel, wert) ->
        var text by remember(knoten.id, schlüssel, wert) { mutableStateOf(wert) }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                zustand.konfiguriereKnoten(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
            },
            label = { Text(schlüssel) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

}
