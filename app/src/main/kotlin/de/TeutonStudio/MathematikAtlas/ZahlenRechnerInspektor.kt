package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelCodec
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussBelegung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.enzyklopädie.RechnerFamilienKatalog
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialBegriff
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialOperator
import de.TeutonStudio.MathematikRechenSystem.kern.DivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

internal object ZahlenRechnerInspektor : KnotenInspektor {
    override fun hatBereich(knoten: KnotenDaten, bereich: KnotenInspektorBereich): Boolean = when (bereich) {
        KnotenInspektorBereich.Knotendaten -> true
        KnotenInspektorBereich.Eingangsdaten -> hatRechnerEingangsEinstellungen(knoten)
        KnotenInspektorBereich.Ausgangsdaten -> UniversellerZahlenOperator.vonIdOderNull(knoten.parameter[ZAHLENRECHNER_OPERATOR]) == UniversellerZahlenOperator.DIFFERENTIAL
    }
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) = FachInhalt(knoten, ergebnis, aktionen, true)
    @Composable override fun BereichInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen, bereich: KnotenInspektorBereich) {
        when (bereich) {
            KnotenInspektorBereich.Knotendaten -> FachInhalt(knoten, ergebnis, aktionen, false)
            KnotenInspektorBereich.Eingangsdaten -> { ZahlenRechnerEingänge(knoten, ergebnis, aktionen); ZahlenRechnerKomplexEingabe(knoten, ergebnis, aktionen) }
            KnotenInspektorBereich.Ausgangsdaten -> { ZahlenRechnerAusgabe(knoten, ergebnis, aktionen) }
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
        val operatorId = knoten.parameter[ZAHLENRECHNER_OPERATOR]
        val gespeicherterStandardOperator = UniversellerZahlenOperator.entries.firstOrNull { operator ->
            operatorId == operator.stabileId || operatorId.equals(operator.name, ignoreCase = true)
        }
        // `zahl.komplexerRadius` bleibt als historische ID lesbar, wird in der Oberfläche aber
        // auf den kanonischen Begriff 0-Distanz (`zahl.betrag`) abgebildet.
        val standardOperator = if (gespeicherterStandardOperator == UniversellerZahlenOperator.KOMPLEXER_RADIUS) {
            UniversellerZahlenOperator.BETRAG
        } else {
            gespeicherterStandardOperator
        }
        val erweiterterOperator = ErweiterterZahlenOperator.vonId(operatorId)
        val formelModus = operatorId == ZAHLENRECHNER_FORMEL_ID
        var operatorDialog by remember(knoten.id, operatorId) { mutableStateOf(false) }
        var formelDialog by remember(knoten.id) { mutableStateOf(false) }
        var formelKandidat by remember(knoten.id, operatorId) { mutableStateOf<KnotenDaten?>(null) }
        val operatorEinträge = remember(knoten) { rechnerOperatorEinträge(knoten, RechnerFamilie.ZAHL) }
        val dialogEinträge = operatorEinträge.map { eintrag ->
            if (eintrag.id == ZAHLENRECHNER_FORMEL_ID) {
                eintrag.copy(kandidat = formelKandidat)
            } else {
                eintrag
            }
        }

        val titel = when {
            formelModus -> "Formel"
            erweiterterOperator != null -> erweiterterOperator.titel
            standardOperator == UniversellerZahlenOperator.BETRAG -> "0-Distanz"
            else -> standardOperator?.titel ?: operatorId?.let { "Unbekannter gespeicherter Operator" } ?: "Addition"
        }
        val symbol = when {
            formelModus -> "f(x)"
            erweiterterOperator != null -> erweiterterOperator.symbolLatex
            standardOperator == UniversellerZahlenOperator.BETRAG -> "d(0,·)"
            else -> standardOperator?.symbolLatex ?: operatorId?.let { "?" } ?: "+"
        }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(titel, modifier = Modifier.weight(1f))
                Text(symbol, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (formelModus) {
            val latex = knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX].orEmpty().ifBlank { "x" }
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.fillMaxWidth().padding(LocalAtlasAbstände.current.bereich), verticalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
                    Text("Gespeicherte Formel", style = MaterialTheme.typography.labelLarge)
                    LatexText(latex, style = MaterialTheme.typography.titleMedium)
                    Text(
                        knoten.parameter[ZAHLENRECHNER_FORMEL_VARIABLEN]
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "Eingänge: $it" }
                            ?: "Konstante Formel ohne freie Eingänge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = {
                        operatorDialog = true
                        formelDialog = true
                    }) { Text("Formel bearbeiten") }
                }
            }
        }

        if (standardOperator == UniversellerZahlenOperator.DIVISION) {
            val seite = divisionsSeiteOderStandard(knoten)
            Text("Divisionsseite", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
            ) {
                FilterChip(
                    selected = seite == DivisionsSeite.RECHTS && !divisionsSeiteIstHistorischOffen(knoten),
                    onClick = {
                        aktionen.knoten(konfiguriereDivisionsSeite(knoten, DivisionsSeite.RECHTS))
                    },
                    label = { Text("Rechts") },
                )
                FilterChip(
                    selected = seite == DivisionsSeite.LINKS && !divisionsSeiteIstHistorischOffen(knoten),
                    onClick = {
                        aktionen.knoten(konfiguriereDivisionsSeite(knoten, DivisionsSeite.LINKS))
                    },
                    label = { Text("Links") },
                )
            }
            Text(
                when (seite) {
                    DivisionsSeite.RECHTS -> "a ÷ᵣ b = a · b⁻¹"
                    DivisionsSeite.LINKS -> "a ÷ₗ b = b⁻¹ · a"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (divisionsSeiteIstHistorischOffen(knoten)) {
                Text(
                    "Historische nichtkommutative Division: Wähle die ursprünglich gemeinte Seite.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (standardOperator == UniversellerZahlenOperator.DIFFERENTIAL) {
            val ergebnisArt = aktuelleZahlenRechnerDifferentialErgebnisArt(knoten)
            if (mitAnschlüssen) ZahlenRechnerAusgabe(knoten, ergebnis, aktionen)

            if (ergebnisArt != null) {
                val differentialOperator = aktuelleZahlenRechnerDifferentialOperator(knoten)
                Text("Differentiation", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
                ) {
                    FilterChip(
                        selected = differentialOperator == DifferentialOperator.Total,
                        onClick = {
                            aktionen.parameter(
                                DIFFERENTIAL_OPERATOR_PARAMETER,
                                DifferentialOperator.Total.operatorId,
                            )
                        },
                        label = { Text("Total") },
                    )
                    FilterChip(
                        selected = differentialOperator is DifferentialOperator.Partiell,
                        onClick = {
                            aktionen.parameter(
                                DIFFERENTIAL_OPERATOR_PARAMETER,
                                DifferentialOperator.Partiell(1).operatorId,
                            )
                        },
                        label = { Text("Partiell") },
                    )
                }

                if (differentialOperator is DifferentialOperator.Partiell) {
                    val indexText = knoten.parameter[DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER] ?: "1"
                    OutlinedTextField(
                        value = indexText,
                        onValueChange = { eingabe ->
                            val bereinigt = eingabe.filter(Char::isDigit).ifBlank { "1" }
                            aktionen.parameter(DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER, bereinigt)
                        },
                        label = { Text("Argumentindex i") },
                        supportingText = { Text("Sichtbar einsbasiert: ∂ᵢf bzw. dᵢf") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                val ordnung = knoten.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER] ?: "1"
                OutlinedTextField(
                    value = ordnung,
                    onValueChange = { eingabe ->
                        aktionen.parameter(DIFFERENTIAL_ORDNUNG_PARAMETER, eingabe.trim().ifBlank { "1" })
                    },
                    label = { Text("Ordnung n") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                val begriff = aktuellerZahlenRechnerDifferentialBegriff(knoten)
                Text("Differentialbegriff", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
                ) {
                    FilterChip(
                        selected = begriff == DifferentialBegriff.REELL_FRECHET,
                        onClick = {
                            aktionen.parameter(
                                DIFFERENTIAL_BEGRIFF_PARAMETER,
                                DifferentialBegriff.REELL_FRECHET.name,
                            )
                        },
                        label = { Text("Reell / Fréchet") },
                    )
                    FilterChip(
                        selected = begriff == DifferentialBegriff.KOMPLEX,
                        onClick = {
                            aktionen.parameter(
                                DIFFERENTIAL_BEGRIFF_PARAMETER,
                                DifferentialBegriff.KOMPLEX.name,
                            )
                        },
                        label = { Text("Komplex") },
                    )
                }
            }
        }

        ergebnis?.warnungen.orEmpty().firstOrNull { it.startsWith("Definition:") }?.let { definition ->
            Text(
                definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (mitAnschlüssen) ZahlenRechnerEingänge(knoten, ergebnis, aktionen)

        if (mitAnschlüssen) ZahlenRechnerKomplexEingabe(knoten, ergebnis, aktionen)

        if (standardOperator == UniversellerZahlenOperator.SINUS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sinus auf der Gradmenge", modifier = Modifier.weight(1f))
                Switch(checked = knoten.parameter[SINUS_WINKELRAUM] == "grad", onCheckedChange = {
                    aktionen.knoten(konfiguriereSinusWinkelraum(knoten, it))
                })
            }
        }
        val verwendetGrad = standardOperator?.let(::verwendetGradWinkel) == true ||
            erweiterterOperator?.let(::verwendetGradWinkel) == true
        if (verwendetGrad && knoten.parameter[SINUS_WINKELRAUM] != "grad") {
            val grad = knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
            val auswerten = knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
            ) {
                Text("Gradwinkel", modifier = Modifier.weight(1f))
                Switch(
                    checked = grad,
                    onCheckedChange = { aktionen.parameter(ZAHLENRECHNER_GRADWINKEL, it.toString()) },
                )
            }
            if (grad) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
                ) {
                    Text("Als x·π÷180 auswerten", modifier = Modifier.weight(1f))
                    Switch(
                        checked = auswerten,
                        onCheckedChange = {
                            aktionen.parameter(ZAHLENRECHNER_GRAD_AUSWERTEN, it.toString())
                        },
                    )
                }
                Text(
                    "° := π/180",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val differentialHistorisch = standardOperator == UniversellerZahlenOperator.DIFFERENTIAL &&
            aktuelleZahlenRechnerDifferentialErgebnisArt(knoten) == null
        if (standardOperator == UniversellerZahlenOperator.INTEGRAL || differentialHistorisch) {
            val variable = knoten.parameter[ZAHLENRECHNER_VARIABLE] ?: "x"
            OutlinedTextField(
                value = variable,
                onValueChange = {
                    aktionen.parameter(ZAHLENRECHNER_VARIABLE, it.trim().ifBlank { "x" })
                },
                label = { Text("Variable") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        ergebnis?.warnungen.orEmpty()
            .filterNot { it.startsWith("Definition:") }
            .forEach { regel ->
                Text(
                    regel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        ergebnis?.fehler?.let { fehler ->
            Text(
                fehler,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (operatorDialog) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Zahlenrechner",
                einträge = dialogEinträge,
                aktuelleId = when {
                    formelModus -> ZAHLENRECHNER_FORMEL_ID
                    erweiterterOperator != null -> erweiterterOperator.stabileId
                    standardOperator != null -> standardOperator.stabileId
                    else -> operatorId
                },
                auswirkungFür = { eintrag ->
                    eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen)
                },
                schließen = {
                    formelKandidat = null
                    operatorDialog = false
                },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knotenNachVerlustbestätigung)
                    formelKandidat = null
                    operatorDialog = false
                },
                formelÖffnen = {
                    formelDialog = true
                },
            )
        }

        if (formelDialog) {
            FormelBauerDialog(
                startLatex = knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX].orEmpty().ifBlank { "x" },
                startZeichnung = knoten.eigenschaften[FORMEL_ZEICHNUNG_EIGENSCHAFT],
                startAusdruck = ladeFormelAst(knoten),
                startKnoten = knoten,
                übernehmenGebunden = { f, zeichnung ->
                    formelKandidat = konfiguriereGebundeneKnotenFormel(knoten,f,zeichnung)
                    formelDialog = false; operatorDialog = true
                },
                übernehmenMitZeichnung = { ast, latex, zeichnung ->
                    val neu = konfiguriereZahlenRechnerFormel(knoten, latex)
                    formelKandidat = neu.copy(eigenschaften = neu.eigenschaften + (FORMEL_ZEICHNUNG_EIGENSCHAFT to zeichnung) + (FORMEL_AST_EIGENSCHAFT to KnotenEigenschaft.Text(StrukturFormelCodec.kodieren(ast))))
                    formelDialog = false; operatorDialog = true
                },
                schließen = { formelDialog = false },
                übernehmen = { latex ->
                    formelKandidat = konfiguriereZahlenRechnerFormel(knoten, latex)
                    formelDialog = false
                    operatorDialog = true
                },
            )
        }
    }
}

private fun erweiterteZahlenKategorie(operator: ErweiterterZahlenOperator): String = when (operator) {
    ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL,
    ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL,
    -> "Grundrechenarten"
    ErweiterterZahlenOperator.STAMMFUNKTION -> "Analysis"
    ErweiterterZahlenOperator.POLYNOM -> "Algebra"
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.SEKANS,
    ErweiterterZahlenOperator.KOSEKANS,
    ErweiterterZahlenOperator.ARCTANGENS,
    -> "Trigonometrie"
    else -> "Hyperbelfunktionen"
}

@Composable
private fun ZahlenRechnerEingänge(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    de.TeutonStudio.MathematikKnoten.InspektorZahlenRechnerEingänge(knoten, aktionen::knoten)
}

@Composable
private fun ZahlenRechnerKomplexEingabe(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    de.TeutonStudio.MathematikKnoten.InspektorKomplexEingabe(knoten, aktionen::knoten)
}

@Composable
private fun ZahlenRechnerAusgabe(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    de.TeutonStudio.MathematikKnoten.InspektorAusgangsEinstellungen(knoten, ergebnis, aktionen::knoten)
}
