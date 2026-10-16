package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

fun hatRechnerEingangsEinstellungen(knoten: KnotenDaten): Boolean {
    if (knoten.art == TensorRechner.KNOTEN_ART) return aktuelleTensorOperationDefinition(knoten).benoetigtAchsenEingabe()
    if (knoten.art == MatrixRechner.KNOTEN_ART) return knoten.parameter[RECHNER_OPERATOR_PARAMETER] == "matrix.produkt" && knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach }
    if (knoten.art != ZAHLENRECHNER_ART) return false
    val standard = UniversellerZahlenOperator.vonIdOderNull(knoten.parameter[ZAHLENRECHNER_OPERATOR])
    val erweitert = ErweiterterZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR])
    return knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach } &&
        (standard?.let { istVariadischerZahlenOperator(it) || istKomplexKonstruktor(it) } == true ||
            erweitert in setOf(ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL, ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL))
}

@Composable
fun InspektorRechnerEingangsEinstellungen(knoten: KnotenDaten, übernehmen: (KnotenDaten) -> Unit) {
    when (knoten.art) {
        ZAHLENRECHNER_ART -> { InspektorZahlenRechnerEingänge(knoten, übernehmen); InspektorKomplexEingabe(knoten, übernehmen) }
        MatrixRechner.KNOTEN_ART -> InspektorStrukturRechnerEingänge(knoten, übernehmen)
        TensorRechner.KNOTEN_ART -> InspektorTensorAchsen(knoten, übernehmen)
    }
}

@Composable
fun InspektorZahlenRechnerEingänge(knoten: KnotenDaten, übernehmen: (KnotenDaten) -> Unit) {
    val operatorId = knoten.parameter[ZAHLENRECHNER_OPERATOR]
    val standardOperator = UniversellerZahlenOperator.entries.firstOrNull { operatorId == it.stabileId || operatorId.equals(it.name, ignoreCase = true) }
    val erweiterterOperator = ErweiterterZahlenOperator.vonId(operatorId)
    val erweiterterMittelwert = erweiterterOperator == ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL ||
        erweiterterOperator == ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL
    if (knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach } && ((standardOperator != null && istVariadischerZahlenOperator(standardOperator)) || erweiterterMittelwert)) {
        val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
        Text("Feste Operanden", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val neueAnzahl = (anzahl - 1).coerceAtLeast(2)
                    val basis = knoten.copy(parameter = knoten.parameter + ("festeEingänge" to neueAnzahl.toString()))
                    übernehmen(
                        if (erweiterterMittelwert) {
                            konfiguriereErweitertenZahlenRechner(basis, requireNotNull(erweiterterOperator))
                        } else {
                            konfiguriereZahlenRechner(knoten = knoten, festeEingänge = neueAnzahl)
                        },
                    )
                },
                enabled = anzahl > 2,
            ) { Text("−") }
            Text(anzahl.toString(), modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    val neueAnzahl = anzahl + 1
                    val basis = knoten.copy(parameter = knoten.parameter + ("festeEingänge" to neueAnzahl.toString()))
                    übernehmen(
                        if (erweiterterMittelwert) {
                            konfiguriereErweitertenZahlenRechner(basis, requireNotNull(erweiterterOperator))
                        } else {
                            konfiguriereZahlenRechner(knoten = knoten, festeEingänge = neueAnzahl)
                        },
                    )
                },
            ) { Text("+") }
        }
    }


}

@Composable
fun InspektorKomplexEingabe(knoten: KnotenDaten, übernehmen: (KnotenDaten) -> Unit) {
    val operatorId = knoten.parameter[ZAHLENRECHNER_OPERATOR]
    val standardOperator = UniversellerZahlenOperator.entries.firstOrNull { operatorId == it.stabileId || operatorId.equals(it.name, ignoreCase = true) }
    val erweiterterOperator = ErweiterterZahlenOperator.vonId(operatorId)
    if (standardOperator != null && istKomplexKonstruktor(standardOperator)) {
        val modus = knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE]
            ?: ZAHLENRECHNER_KOMPLEX_SEPARIERT
        Text("Komplexe Eingabe", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = modus == ZAHLENRECHNER_KOMPLEX_SEPARIERT,
                onClick = {
                    übernehmen(
                        konfiguriereZahlenRechner(
                            knoten = knoten,
                            komplexEingabe = ZAHLENRECHNER_KOMPLEX_SEPARIERT,
                        ),
                    )
                },
                label = { Text("Getrennt") },
            )
            FilterChip(
                selected = modus == ZAHLENRECHNER_KOMPLEX_TUPEL,
                onClick = {
                    übernehmen(
                        konfiguriereZahlenRechner(
                            knoten = knoten,
                            komplexEingabe = ZAHLENRECHNER_KOMPLEX_TUPEL,
                        ),
                    )
                },
                label = { Text("Tupel") },
            )
        }
    }


}

@Composable
fun InspektorStrukturRechnerEingänge(knoten: KnotenDaten, übernehmen: (KnotenDaten) -> Unit) {
    val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art) ?: return
    val operatorId = knoten.parameter[RECHNER_OPERATOR_PARAMETER]
    if (familie == StrukturRechnerKnotenFamilie.MATRIX && operatorId == "matrix.produkt" && knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach }) {
        var feste by remember(knoten.id, knoten.parameter["festeEingänge"]) { mutableStateOf(knoten.parameter["festeEingänge"] ?: "2") }
        OutlinedTextField(value = feste, onValueChange = { text ->
            feste = text
            text.toIntOrNull()?.takeIf { it >= 2 }?.let { n ->
                übernehmen(konfiguriereStrukturRechner(knoten.copy(parameter = knoten.parameter + ("festeEingänge" to n.toString())), familie, operatorId))
            }
        }, label = { Text("Feste Faktoren (mindestens 2)") })
    }

}

@Composable
fun InspektorTensorAchsen(knoten: KnotenDaten, übernehmen: (KnotenDaten) -> Unit) {
    val definition = aktuelleTensorOperationDefinition(knoten)
    if (!definition.benoetigtAchsenEingabe()) return

    val modus = aktuellerAchsenEingabeModus(knoten)
    val dynamischeAnzahl = inspektorDynamischeAchsenAnzahl(knoten, definition)

    Text("Achseneingabe", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = modus == AchsenEingabeModus.TUPEL,
            onClick = {
                übernehmen(
                    konfiguriereTensorOperation(
                        knoten,
                        definition,
                        AchsenEingabeModus.TUPEL,
                        dynamischeAnzahl,
                    ),
                )
            },
            label = { Text("Tupel") },
        )
        FilterChip(
            selected = modus == AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
            onClick = {
                übernehmen(
                    konfiguriereTensorOperation(
                        knoten,
                        definition,
                        AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
                        dynamischeAnzahl,
                    ),
                )
            },
            label = { Text("Einzelhandles") },
        )
    }

    OutlinedTextField(
        value = knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION].orEmpty(),
        onValueChange = { neu ->
            übernehmen(
                knoten.copy(
                    parameter = knoten.parameter - TENSOR_ACHSEN_MIGRATIONSFEHLER +
                        (TENSOR_ACHSEN_SPEZIFIKATION to neu),
                ),
            )
        },
        label = { Text("Gespeicherte Achsen") },
        supportingText = {
            Text("1 = erste Achse, -1 = letzte Achse, 0 ist ungültig. Verbundene Handles überschreiben diese Werte.")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    if (modus == AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES && definition.maximaleAchsenAnzahl == null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    übernehmen(
                        konfiguriereTensorOperation(
                            knoten,
                            definition,
                            modus,
                            (dynamischeAnzahl - 1).coerceAtLeast(definition.minimaleAchsenAnzahl),
                        ),
                    )
                },
                enabled = dynamischeAnzahl > definition.minimaleAchsenAnzahl,
            ) { Text("Achse entfernen") }
            OutlinedButton(
                onClick = {
                    übernehmen(
                        konfiguriereTensorOperation(
                            knoten,
                            definition,
                            modus,
                            dynamischeAnzahl + 1,
                        ),
                    )
                },
            ) { Text("Achse hinzufügen") }
        }
    }

    Text(
        "Ein Modus- oder Signaturwechsel ist eine Knotenaktion. Semantisch fremde Handles werden nicht unter derselben ID wiederverwendet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun inspektorDynamischeAchsenAnzahl(knoten: KnotenDaten, definition: TensorOperationDefinition): Int =
    knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith("achse.") }
        .coerceAtLeast(definition.minimaleAchsenAnzahl.coerceAtLeast(1))
