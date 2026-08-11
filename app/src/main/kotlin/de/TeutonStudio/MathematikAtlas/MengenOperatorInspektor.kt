package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        register[RelationsOperatoren.KNOTEN_ART] = PraedikatRelationsInspektor
        register[VektorRechner.KNOTEN_ART] = VektorRechnerErweiterterInspektor
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

/** Historische Karten können diesen Inspector bis zur Lademigration noch kurz sehen. */
internal object MengenRelationsInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[MENGENRELATION_OPERATOR_PARAMETER]
        val operator = MengenRelationsOperator.vonIdOderNull(operatorId) ?: MengenRelationsOperator.TEILMENGE
        Text("Historische Mengenrelation", style = MaterialTheme.typography.titleSmall)
        LatexText(operator.symbolLatex, style = MaterialTheme.typography.bodyMedium)
        Text("Beim nächsten Laden wird dieser Knoten in den kanonischen Prädikat-Knoten migriert.")
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

internal object PraedikatRelationsInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER]
        val operator = RelationsOperatoren.vonIdOderNull(operatorId) ?: RelationsOperatoren.standard()
        var dialogOffen by remember(knoten.id, operatorId) { mutableStateOf(false) }
        val eintraege = remember(knoten) {
            RelationsOperatoren.alle.map { kandidat ->
                RechnerOperatorAuswahlEintrag(
                    id = kandidat.stabileId,
                    titel = kandidat.titel,
                    symbolLatex = kandidat.symbolLatex,
                    kategorie = kandidat.kategorie,
                    beschreibung = kandidat.argumente.joinToString(prefix = "Argumente: ") { it.rolle },
                    suchbegriffe = kandidat.suchbegriffe + kandidat.stabileId,
                    kandidat = konfigurierePraedikat(knoten, kandidat),
                )
            }
        }

        Text("Relation", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { dialogOffen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(operator.titel, modifier = Modifier.weight(1f))
            LatexText(operator.symbolLatex, style = MaterialTheme.typography.labelMedium)
        }
        operator.relationsStruktur?.kompakteKlassen()?.takeIf { it.isNotEmpty() }?.let { klassen ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                klassen.forEach { klasse ->
                    FilterChip(selected = true, onClick = {}, label = { Text(klasse.titel) })
                }
            }
        }
        if (dialogOffen) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Prädikat · Relationen",
                einträge = eintraege,
                aktuelleId = operator.stabileId,
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

internal object VektorRechnerErweiterterInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operatorId = knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        val operator = VektorRechnerOperator.vonIdOderNull(operatorId) ?: VektorRechnerOperator.SKALARPRODUKT
        var dialogOffen by remember(knoten.id, operatorId) { mutableStateOf(false) }
        val eintraege = remember(knoten) {
            VektorRechnerOperator.entries.map { kandidat ->
                RechnerOperatorAuswahlEintrag(
                    id = kandidat.stabileId,
                    titel = kandidat.titel,
                    symbolLatex = vektorOperatorLatex(kandidat),
                    kategorie = vektorOperatorKategorie(kandidat),
                    beschreibung = vektorOperatorBeschreibung(kandidat),
                    suchbegriffe = setOf(kandidat.name, kandidat.stabileId, kandidat.titel),
                    kandidat = konfiguriereVektorRechner(knoten, kandidat),
                )
            }
        }

        Text("Vektoroperation", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { dialogOffen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(operator.titel, modifier = Modifier.weight(1f))
            LatexText(vektorOperatorLatex(operator), style = MaterialTheme.typography.labelMedium)
        }
        if (dialogOffen) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Vektorrechner",
                einträge = eintraege,
                aktuelleId = operator.stabileId,
                auswirkungFür = { eintrag -> eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen) },
                schließen = { dialogOffen = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    dialogOffen = false
                },
                formelÖffnen = {},
            )
        }

        if (operator == VektorRechnerOperator.DISTANZ) {
            Text("Metrik", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val aktuell = VektorMetriken.vonIdOderStandard(knoten.parameter[VEKTOR_RECHNER_METRIK])
                VektorMetriken.alle.forEach { metrik ->
                    FilterChip(
                        selected = aktuell == metrik,
                        onClick = { aktionen.parameter(VEKTOR_RECHNER_METRIK, metrik.stabileId) },
                        label = { Text(metrik.titel) },
                    )
                }
            }
        }
        if (operator == VektorRechnerOperator.WINKEL_ZU_ACHSE) {
            var achse by remember(knoten.id, knoten.parameter[VEKTOR_RECHNER_ACHSE]) {
                mutableStateOf(knoten.parameter[VEKTOR_RECHNER_ACHSE] ?: "1")
            }
            OutlinedTextField(
                value = achse,
                onValueChange = { text ->
                    if (text.all(Char::isDigit)) {
                        achse = text
                        text.toIntOrNull()?.takeIf { it >= 1 }?.let {
                            aktionen.parameter(VEKTOR_RECHNER_ACHSE, it.toString())
                        }
                    }
                },
                label = { Text("Achse (1-basiert)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (operator == VektorRechnerOperator.ZUSAMMENFUEHREN) {
            Text("Ausgabe", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val aktuell = VektorStrukturAusgabe.vonIdOderStandard(knoten.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE])
                VektorStrukturAusgabe.entries.forEach { ausgabe ->
                    FilterChip(
                        selected = aktuell == ausgabe,
                        onClick = { aktionen.parameter(VEKTOR_RECHNER_STRUKTUR_AUSGABE, ausgabe.stabileId) },
                        label = { Text(ausgabe.titel) },
                    )
                }
            }
        }
        if (operator == VektorRechnerOperator.VEKTORFELD_INTEGRIEREN) {
            Text(
                "Das Integrationsmaß ist ein eigener Anschluss. Ohne Maß wird absichtlich nicht geraten.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun vektorOperatorKategorie(operator: VektorRechnerOperator): String = when (operator) {
    VektorRechnerOperator.ZERLEGEN, VektorRechnerOperator.ZUSAMMENFUEHREN -> "Struktur"
    VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> "Analysis"
    VektorRechnerOperator.DISTANZ, VektorRechnerOperator.NORM, VektorRechnerOperator.NORMALISIERUNG -> "Metrik und Norm"
    VektorRechnerOperator.WINKEL, VektorRechnerOperator.WINKEL_ZU_ACHSE, VektorRechnerOperator.PROJEKTION -> "Geometrie"
    else -> "Vektoroperationen"
}

private fun vektorOperatorLatex(operator: VektorRechnerOperator): String = when (operator) {
    VektorRechnerOperator.ADDITION -> "u+v"
    VektorRechnerOperator.SUBTRAKTION -> "u-v"
    VektorRechnerOperator.SKALARMULTIPLIKATION -> "\\lambda v"
    VektorRechnerOperator.NEGATION -> "-v"
    VektorRechnerOperator.SKALARPRODUKT -> "\\langle u,v\\rangle"
    VektorRechnerOperator.KREUZPRODUKT -> "u\\times v"
    VektorRechnerOperator.NORM -> "\\lVert v\\rVert"
    VektorRechnerOperator.NORMALISIERUNG -> "v/\\lVert v\\rVert"
    VektorRechnerOperator.HADAMARD_PRODUKT -> "u\\odot v"
    VektorRechnerOperator.PROJEKTION -> "\\operatorname{proj}_v(u)"
    VektorRechnerOperator.WINKEL -> "\\angle(u,v)"
    VektorRechnerOperator.DISTANZ -> "d(u,v)"
    VektorRechnerOperator.WINKEL_ZU_ACHSE -> "\\angle(v,e_i)"
    VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> "\\int_M F\\,d\\mu"
    VektorRechnerOperator.ZERLEGEN -> "(x_i)\\mapsto x_1,\\ldots,x_n"
    VektorRechnerOperator.ZUSAMMENFUEHREN -> "x_1,\\ldots,x_n\\mapsto(x_i)"
}

private fun vektorOperatorBeschreibung(operator: VektorRechnerOperator): String = when (operator) {
    VektorRechnerOperator.DISTANZ -> "Berechnet die Distanz in der im Inspector gewählten Metrik."
    VektorRechnerOperator.WINKEL_ZU_ACHSE -> "Berechnet den Winkel zu einer 1-basiert gewählten Koordinatenachse."
    VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> "Integriert ein vektorwertiges Feld komponentenweise über Menge und explizites Maß."
    VektorRechnerOperator.ZERLEGEN -> "Überführt Vektor oder Tupel in eine geordnete Tupelstruktur der Komponenten."
    VektorRechnerOperator.ZUSAMMENFUEHREN -> "Führt Elemente, Tupel und Vektoren geordnet zu Tupel, Spalten- oder Zeilenvektor zusammen."
    else -> operator.titel
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
