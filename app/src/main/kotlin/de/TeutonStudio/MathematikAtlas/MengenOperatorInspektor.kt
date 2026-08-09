package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal object MengenOperatorInspektorRegistrierung {
    @Suppress("UNCHECKED_CAST")
    fun installieren() {
        val feld = KnotenInspektorRegister::class.java.getDeclaredField("inspektoren")
        feld.isAccessible = true
        val register = feld.get(KnotenInspektorRegister) as MutableMap<String, KnotenInspektor>
        register[MengenRechner.KNOTEN_ART] = MengenRechnerInspektor
        register[MengenRelationRechner.KNOTEN_ART] = MengenRelationsInspektor
        register[MENGEN_MASS_KNOTEN_ART] = MengenMassInspektor
    }
}

internal object MengenRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[MENGENRECHNER_OPERATOR_PARAMETER]
        val operator = MengenRechnerOperator.vonIdOderNull(operatorId) ?: MengenRechnerOperator.VEREINIGUNG
        var dialogOffen by remember(knoten.id, operatorId) { mutableStateOf(false) }
        val eintraege = remember(knoten) {
            sichtbareMengenRechnerOperatoren().map { kandidatOperator ->
                RechnerOperatorAuswahlEintrag(
                    id = kandidatOperator.stabileId,
                    titel = kandidatOperator.titel(),
                    symbolLatex = kandidatOperator.vorschauLatex(),
                    kategorie = if (kandidatOperator in setOf(
                            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
                            MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
                            MengenRechnerOperator.ITERIERTER_SCHNITT,
                        )
                    ) "Iterierte Mengenoperatoren" else "Mengenoperatoren",
                    beschreibung = kandidatOperator.definitionLatex(),
                    suchbegriffe = setOf(kandidatOperator.name, kandidatOperator.stabileId),
                    kandidat = konfiguriereMengenRechner(knoten, kandidatOperator),
                )
            }
        }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { dialogOffen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(operator.titel(), modifier = Modifier.weight(1f))
                LatexText(operator.vorschauLatex(), style = MaterialTheme.typography.labelMedium)
            }
        }
        LatexText(operator.definitionLatex(), style = MaterialTheme.typography.bodyMedium)

        if (dialogOffen) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Mengenrechner",
                einträge = eintraege,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag -> eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen) },
                schließen = { dialogOffen = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    dialogOffen = false
                },
                formelÖffnen = {},
            )
        }

        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

internal object MengenRelationsInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[MENGENRELATION_OPERATOR_PARAMETER]
        val operator = MengenRelationsOperator.vonIdOderNull(operatorId) ?: MengenRelationsOperator.TEILMENGE
        var dialogOffen by remember(knoten.id, operatorId) { mutableStateOf(false) }
        val eintraege = remember(knoten) {
            MengenRelationsOperator.entries.map { kandidatOperator ->
                RechnerOperatorAuswahlEintrag(
                    id = kandidatOperator.stabileId,
                    titel = kandidatOperator.titel,
                    symbolLatex = kandidatOperator.symbolLatex,
                    kategorie = if (kandidatOperator == MengenRelationsOperator.ELEMENT) {
                        "Elementrelation"
                    } else {
                        "Mengenrelationen"
                    },
                    beschreibung = when (kandidatOperator) {
                        MengenRelationsOperator.UEBERMENGE -> "A ist echte Übermenge von B."
                        MengenRelationsOperator.TEILMENGE -> "A ist echte Teilmenge von B."
                        MengenRelationsOperator.UEBER_ODER_GLEICHMENGE -> "A enthält B einschließlich Gleichheit."
                        MengenRelationsOperator.TEIL_ODER_GLEICHMENGE -> "A ist in B enthalten einschließlich Gleichheit."
                        MengenRelationsOperator.ELEMENT -> "Prüft, ob ein mathematisches Objekt Element einer Menge ist."
                        MengenRelationsOperator.DISJUNKT -> "Die beiden Mengen besitzen kein gemeinsames Element."
                    },
                    suchbegriffe = setOf(kandidatOperator.name, kandidatOperator.stabileId),
                    kandidat = konfiguriereMengenRelation(knoten, kandidatOperator),
                )
            }
        }

        Text("Relation", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { dialogOffen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(operator.titel, modifier = Modifier.weight(1f))
                LatexText(operator.symbolLatex, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (dialogOffen) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Mengenrelation",
                einträge = eintraege,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag -> eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen) },
                schließen = { dialogOffen = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    dialogOffen = false
                },
                formelÖffnen = {},
            )
        }

        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

internal object MengenMassInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val aktuell = IntegralMassModus.entries.firstOrNull {
            it.name == knoten.parameter[MENGEN_MASS_MODUS_PARAMETER]
        } ?: IntegralMassModus.ALLGEMEIN
        Text("Maß", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(6f)),
        ) {
            listOf(
                IntegralMassModus.STANDARD_REELL to "Lebesgue",
                IntegralMassModus.ZAEHLMASS to "Zählmaß",
                IntegralMassModus.ALLGEMEIN to "Allgemein",
            ).forEach { (modus, titel) ->
                FilterChip(
                    selected = aktuell == modus,
                    onClick = { aktionen.parameter(MENGEN_MASS_MODUS_PARAMETER, modus.name) },
                    label = { Text(titel) },
                )
            }
        }
        if (aktuell == IntegralMassModus.ALLGEMEIN) {
            Text(
                "Das Maßsymbol kann im Parameter „$MENGEN_MASS_SYMBOL_PARAMETER“ geändert werden.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
