package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich
import de.TeutonStudio.MathematikKnoten.RechnerSchemaSchaltfläche

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasBestätigungsDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_IDS
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_MIGRATIONSFEHLER
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_SPEZIFIKATION
import de.TeutonStudio.MathematikKnoten.TENSOR_OPERATION_PARAMETER
import de.TeutonStudio.MathematikKnoten.aktuelleTensorOperationDefinition
import de.TeutonStudio.MathematikKnoten.aktuellerAchsenEingabeModus
import de.TeutonStudio.MathematikKnoten.benoetigtAchsenEingabe
import de.TeutonStudio.MathematikKnoten.konfiguriereTensorOperation
import de.TeutonStudio.MathematikRechenSystem.kern.AchsenEingabeModus
import de.TeutonStudio.MathematikRechenSystem.kern.StandardTensorOperationen
import de.TeutonStudio.MathematikRechenSystem.kern.TensorOperationDefinition

internal object TensorOperationRechnerInspektor : KnotenInspektor {
    override fun hatBereich(knoten: KnotenDaten, bereich: KnotenInspektorBereich): Boolean = when (bereich) {
        KnotenInspektorBereich.Knotendaten -> true
        KnotenInspektorBereich.Eingangsdaten -> aktuelleTensorOperationDefinition(knoten).benoetigtAchsenEingabe()
        KnotenInspektorBereich.Ausgangsdaten -> false
    }
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) = FachInhalt(knoten, ergebnis, aktionen, true)
    @Composable override fun BereichInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen, bereich: KnotenInspektorBereich) {
        when (bereich) {
            KnotenInspektorBereich.Knotendaten -> FachInhalt(knoten, ergebnis, aktionen, false)
            KnotenInspektorBereich.Eingangsdaten -> { TensorRechnerEingänge(knoten, ergebnis, aktionen) }
            KnotenInspektorBereich.Ausgangsdaten -> {  }
        }
    }

    @Composable
    private fun FachInhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
        mitAnschlüssen: Boolean,
    ) {
        de.TeutonStudio.MathematikKnoten.RechnerSchemaSchaltfläche(knoten, ergebnis)
        val definition = aktuelleTensorOperationDefinition(knoten)
        val operatorId = knoten.parameter[de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER]
            ?: knoten.parameter[de.TeutonStudio.MathematikKnoten.TENSOR_OPERATION_ID]
            ?: definition.id.wert
        val bekannteDefinition = StandardTensorOperationen.registry.definition(operatorId)
        var operatorDialog by remember(knoten.id, definition.id) { mutableStateOf(false) }
        val operatorEinträge = remember(knoten) {
            buildList {
                if (StandardTensorOperationen.registry.definition(operatorId) == null) {
                    add(
                        RechnerOperatorAuswahlEintrag(
                            id = operatorId,
                            titel = "Unbekannter gespeicherter Operator",
                            symbolLatex = "?",
                            kategorie = "Nicht verfügbar",
                            beschreibung = "Die gespeicherte Operator-ID $operatorId ist nicht registriert.",
                            art = RechnerOperatorAuswahlArt.UNBEKANNT,
                        ),
                    )
                }
                rechnerOperatorEinträge(knoten, de.TeutonStudio.MathematikKnoten.RechnerFamilie.TENSOR)
                    .filter { it.art == RechnerOperatorAuswahlArt.OPERATOR }.forEach { eintrag ->
                        val auswählbar = requireNotNull(StandardTensorOperationen.registry.definition(eintrag.id))
                        add(eintrag.copy(kandidat = konfiguriereTensorOperation(knoten, auswählbar,
                            aktuellerAchsenEingabeModus(knoten), dynamischeAchsenAnzahl(knoten, auswählbar))))
                    }

            }
        }

        Text("Tensoroperation", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    bekannteDefinition?.titel ?: "Unbekannter gespeicherter Operator",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    bekannteDefinition?.familie?.name ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        if (bekannteDefinition == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    "Die Operator-ID $operatorId ist nicht registriert. Wähle im Dialog einen gültigen Ersatz.",
                    modifier = Modifier.fillMaxWidth().padding(LocalAtlasAbstände.current.bereich),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            VertragsKarte(bekannteDefinition, knoten)
        }

        if (mitAnschlüssen) TensorRechnerEingänge(knoten, ergebnis, aktionen)

        if (bekannteDefinition != null && bekannteDefinition.parameter.isNotEmpty()) {
            OutlinedTextField(
                value = knoten.parameter[TENSOR_OPERATION_PARAMETER].orEmpty(),
                onValueChange = { aktionen.parameter(TENSOR_OPERATION_PARAMETER, it) },
                label = { Text(bekannteDefinition.parameter.joinToString { it.id }) },
                supportingText = {
                    Text("Operationsparameter, kommagetrennt. Achsen werden getrennt und sichtbar einsbasiert gespeichert.")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        ergebnis?.warnungen.orEmpty().forEach { warnung ->
            Text(
                warnung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (operatorDialog) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Tensorrechner",
                einträge = operatorEinträge,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag ->
                    eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen)
                },
                schließen = { operatorDialog = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knotenNachVerlustbestätigung)
                    operatorDialog = false
                },
                formelÖffnen = {},
            )
        }


    }
}

private fun tensorOperatorKategorie(definition: TensorOperationDefinition): String = when (definition.familie) {
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.BINAER -> "Binäre Operationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.UNAER -> "Unäre Operationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.ACHSENABHAENGIG -> "Achsenoperationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.INDEXIERUNG -> "Indexierung"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.KONSTRUKTION -> "Konstruktion"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.ZERLEGUNG,
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.MEHRFACHAUSGANG,
    -> "Zerlegungen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.VARIADISCH -> "Variadische Operationen"
}

private fun tensorStatusTitel(definition: TensorOperationDefinition): String = when (definition.unterstuetzungsStatus) {
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT -> "Konkret implementiert"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT -> "Symbolisch implementiert"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.REGISTRIERT -> "Registriert"
}

@Composable
private fun VertragsKarte(
    definition: TensorOperationDefinition,
    knoten: KnotenDaten,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LocalAtlasAbstände.current.bereich),
            verticalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.winzig),
        ) {
            Text("Signaturvertrag", style = MaterialTheme.typography.labelLarge)
            Text("Familie: ${definition.familie.name}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Eingänge: ${definition.eingangsRollen.joinToString { it.wert }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Ausgänge: ${definition.ausgangsRollen.joinToString { it.wert }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Unterstützung: ${definition.unterstuetzungsStatus.name}",
                style = MaterialTheme.typography.bodySmall,
            )
            knoten.parameter[TENSOR_ACHSEN_IDS]?.takeIf(String::isNotBlank)?.let { ids ->
                Text("Stabile Achsen-IDs: $ids", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Aktuelle Handles: ${knoten.anschlüsse.joinToString { "${it.name}:${it.id.wert}" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            knoten.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER]?.let { fehler ->
                Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AchsenKonfiguration(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
    aktionen: KnotenInspektorAktionen,
    knotenAendern: (KnotenDaten) -> Unit,
) {
    de.TeutonStudio.MathematikKnoten.InspektorTensorAchsen(knoten, knotenAendern)
}

private fun dynamischeAchsenAnzahl(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
): Int = knoten.anschlüsse.count {
    it.richtung == AnschlussRichtung.Eingang && it.name.startsWith("achse.")
}.coerceAtLeast(definition.minimaleAchsenAnzahl.coerceAtLeast(1))

@Composable
private fun TensorRechnerEingänge(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    val bekannteDefinition = aktuelleTensorOperationDefinition(knoten)
    var ausstehenderKnoten by remember(knoten.id) { mutableStateOf<KnotenDaten?>(null) }
        fun übernehmeAchsenÄnderung(neu: KnotenDaten) {
            if (aktionen.vorschauKnotenErsetzen(neu).trenntVerbindungen) {
                ausstehenderKnoten = neu
            } else {
                aktionen.knoten(neu)
            }
        }


        if (bekannteDefinition?.benoetigtAchsenEingabe() == true) {
            AchsenKonfiguration(
                knoten = knoten,
                definition = bekannteDefinition,
                aktionen = aktionen,
                knotenAendern = ::übernehmeAchsenÄnderung,
            )
        }

        ausstehenderKnoten?.let { ziel ->
            val auswirkung = aktionen.vorschauKnotenErsetzen(ziel)
            AtlasBestätigungsDialog(
                onDismissRequest = { ausstehenderKnoten = null },
                title = { Text("Achseneingabe wirklich wechseln?") },
                text = {
                    Text(
                        "${auswirkung.entfallendeVerbindungen.size} bestehende Verbindung(en) an " +
                            "${auswirkung.entfallendeAnschlüsse.joinToString { it.name }} werden getrennt.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            aktionen.knotenNachVerlustbestätigung(ziel)
                            ausstehenderKnoten = null
                        },
                    ) { Text("Wechseln und Verbindungen trennen") }
                },
                dismissButton = {
                    TextButton(onClick = { ausstehenderKnoten = null }) { Text("Abbrechen") }
                },
            )
        }
}
