package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.enzyklopädie.RechnerFamilienKatalog
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialBegriff
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialOperator
import de.TeutonStudio.MathematikRechenSystem.kern.DivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

internal object ZahlenRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
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
        val operatorEinträge = remember(knoten) {
            val katalogNachId = RechnerFamilienKatalog.zahlenOperatoren.associateBy { it.stabileId }
            buildList {
                if (!formelModus && standardOperator == null && erweiterterOperator == null && !operatorId.isNullOrBlank()) {
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
                UniversellerZahlenOperator.entries
                    .filterNot { it == UniversellerZahlenOperator.KOMPLEXER_RADIUS }
                    .forEach { operator ->
                        val kandidat = if (operator == UniversellerZahlenOperator.DIFFERENTIAL) {
                            konfiguriereZahlenRechnerDifferential(
                                knoten,
                                ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
                            )
                        } else {
                            konfiguriereStandardZahlenRechner(knoten, operator)
                        }
                        val nullDistanz = operator == UniversellerZahlenOperator.BETRAG
                        add(
                            RechnerOperatorAuswahlEintrag(
                                id = operator.stabileId,
                                titel = if (nullDistanz) "0-Distanz" else operator.titel,
                                symbolLatex = if (nullDistanz) "d(0,\\dots)" else operator.vorschauLatex,
                                kategorie = katalogNachId[operator.stabileId]?.kategorie ?: "Weitere Funktionen",
                                beschreibung = if (nullDistanz) {
                                    "Abstand eines Zahlwerts vom Nullelement; umfasst Betrag, komplexen Radius und Quaternionenradius."
                                } else {
                                    katalogNachId[operator.stabileId]
                                        ?.signaturen
                                        ?.mapNotNull { it.beschreibung }
                                        ?.joinToString(" ")
                                        ?.ifBlank { null }
                                        ?: "Konfiguriert den Zahlenrechner für ${operator.titel}."
                                },
                                suchbegriffe = if (nullDistanz) {
                                    setOf(
                                        operator.name,
                                        "Betrag",
                                        "Absolutbetrag",
                                        "0-Distanz",
                                        "Abstand zu 0",
                                        "komplexer Radius",
                                        "Quaternionenradius",
                                        "Radius",
                                        "Modulus",
                                    )
                                } else {
                                    setOf(operator.name)
                                },
                                kandidat = kandidat,
                            ),
                        )
                    }
                ErweiterterZahlenOperator.entries.forEach { operator ->
                    add(
                        RechnerOperatorAuswahlEintrag(
                            id = operator.stabileId,
                            titel = operator.titel,
                            symbolLatex = operator.vorschauLatex,
                            kategorie = erweiterteZahlenKategorie(operator),
                            beschreibung = "Erweiterter Zahlenoperator ${operator.titel}.",
                            suchbegriffe = setOf(operator.name),
                            kandidat = konfiguriereErweitertenZahlenRechner(knoten, operator),
                        ),
                    )
                }
                add(
                    RechnerOperatorAuswahlEintrag(
                        id = ZAHLENRECHNER_FORMEL_ID,
                        titel = "Eigene Formel",
                        symbolLatex = "f(x)",
                        kategorie = "Eigene Formeln",
                        beschreibung = "Erstellt oder bearbeitet einen eigenen Zahlenausdruck im CAS-Formelbauer.",
                        suchbegriffe = setOf("CAS", "Formelbauer"),
                        art = RechnerOperatorAuswahlArt.FORMEL,
                    ),
                )
            }
        }
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
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Text("Ergebnisart", style = MaterialTheme.typography.titleSmall)
            if (ergebnisArt == null) {
                Text(
                    "Historischer Termmodus. Die bisherige skalare Ableitung bleibt unverändert, bis eine strukturierte Ergebnisart gewählt wird.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = ergebnisArt == ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechnerDifferential(
                                knoten,
                                ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
                            ),
                        )
                    },
                    label = { Text("f′") },
                )
                FilterChip(
                    selected = ergebnisArt == ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL,
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechnerDifferential(
                                knoten,
                                ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL,
                            ),
                        )
                    },
                    label = { Text("df") },
                )
            }

            if (ergebnisArt != null) {
                val differentialOperator = aktuelleZahlenRechnerDifferentialOperator(knoten)
                Text("Differentiation", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        if (standardOperator != null && istVariadischerZahlenOperator(standardOperator)) {
            val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            Text("Feste Operanden", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                festeEingänge = (anzahl - 1).coerceAtLeast(2),
                            ),
                        )
                    },
                    enabled = anzahl > 2,
                ) { Text("−") }
                Text(anzahl.toString(), modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                festeEingänge = anzahl + 1,
                            ),
                        )
                    },
                ) { Text("+") }
            }
        }

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
                        aktionen.knoten(
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
                        aktionen.knoten(
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

        val verwendetGrad = standardOperator?.let(::verwendetGradWinkel) == true ||
            erweiterterOperator?.let(::verwendetGradWinkel) == true
        if (verwendetGrad) {
            val grad = knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
            val auswerten = knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    eintrag.kandidat?.let(aktionen::knoten)
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
    ErweiterterZahlenOperator.POLYNOM -> "Algebra"
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.SEKANS,
    ErweiterterZahlenOperator.KOSEKANS,
    ErweiterterZahlenOperator.ARCTANGENS,
    -> "Trigonometrie"
    else -> "Hyperbelfunktionen"
}
