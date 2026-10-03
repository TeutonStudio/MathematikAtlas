package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.METHODEN_BEREICHS_OPERATOR_ANPASSUNG
import de.TeutonStudio.MathematikKnoten.RESTRIKTIONS_ERGÄNZUNG_PREFIX
import de.TeutonStudio.MathematikKnoten.methodenBereichsOperator
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal object RestriktionsKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val basis = ergebnis?.eingänge?.get("methode")?.objekt as? Methode
        val menge = ergebnis?.eingänge?.get("menge")?.objekt as? MengenAusdruck

        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (knoten.methodenBereichsOperator() == METHODEN_BEREICHS_OPERATOR_ANPASSUNG) {
                BereichsanpassungsInhalt(knoten, ergebnis, basis, menge, aktionen)
            } else {
                RestriktionsInhalt(ergebnis, basis, menge)
            }
        }
    }
}

@Composable
private fun RestriktionsInhalt(
    ergebnis: KnotenAuswertungsErgebnis?,
    basis: Methode?,
    menge: MengenAusdruck?,
) {
    Text("Reine Methodenrestriktion", style = MaterialTheme.typography.titleSmall)
    if (basis == null || menge == null) {
        Text(
            "Methode und Teilmenge verbinden. Die Restriktion besitzt keine Ergänzungsmethoden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val diagnose = runCatching { restriktiereMethode(basis, menge) }.getOrNull()
        if (diagnose != null) {
            DiagnoseZeile("D_f", diagnose.basisWerteVorrat.zuLatex())
            DiagnoseZeile("M", diagnose.gewünschterWerteVorrat.zuLatex())
            DiagnoseZeile("Z", diagnose.zielMenge.zuLatex())
            Text(
                "Voraussetzung M ⊆ D_f: ${statusText(diagnose.teilmengenPrüfung)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            diagnose.bedingungen.forEach { bedingung ->
                Text(
                    "Offene Bedingung: ${bedingung.zuLatex()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    ergebnis?.fehler?.let { fehler ->
        Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BereichsanpassungsInhalt(
    knoten: KnotenDaten,
    ergebnis: KnotenAuswertungsErgebnis?,
    basis: Methode?,
    menge: MengenAusdruck?,
    aktionen: KnotenInspektorAktionen,
) {
    Text("Methoden-Bereichsanpassung", style = MaterialTheme.typography.titleSmall)
    val ergänzungsPaare = knoten.anschlüsse
        .filter {
            it.richtung == AnschlussRichtung.Eingang &&
                it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX)
        }
        .sortedBy { it.reihenfolge }
        .mapNotNull { anschluss ->
            val methode = ergebnis?.eingänge?.get(anschluss.name)?.objekt as? Methode
            methode?.let { anschluss to it }
        }

    if (basis == null || menge == null) {
        Text(
            "Basis-Methode und gewünschte Definitionsmenge verbinden. Ergänzungen werden anschließend in sichtbarer Reihenfolge priorisiert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val diagnose = runCatching {
            passeMethodenBereichAn(basis, menge, ergänzungsPaare.map { it.second })
        }.getOrNull()
        if (diagnose != null) {
            DiagnoseZeile("D_f", diagnose.basisWerteVorrat.zuLatex())
            DiagnoseZeile("G", diagnose.gewünschterWerteVorrat.zuLatex())
            DiagnoseZeile("Z", diagnose.zielMenge.zuLatex())
            DiagnoseZeile("Abgedeckt", diagnose.abgedeckterBereich.zuLatex())
            DiagnoseZeile("Rest", diagnose.restMenge.zuLatex())
            Text(
                "Abdeckung: ${when (diagnose.abdeckungsStatus) {
                    AbdeckungsStatus.Vollständig -> "vollständig"
                    AbdeckungsStatus.Unvollständig -> "unvollständig"
                    AbdeckungsStatus.Unbekannt -> "noch nicht beweisbar"
                }}",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (ergänzungsPaare.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Geordnete Ergänzungen · erste passende Methode gewinnt", style = MaterialTheme.typography.titleSmall)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ergänzungsPaare.forEachIndexed { index, (anschluss, methode) ->
                        val fachErgebnis = diagnose.ergänzungen.getOrNull(index)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                buildString {
                                    append("${index + 1}. ${methode.name}")
                                    fachErgebnis?.let {
                                        append(": W=${it.werteVorrat.zuLatex()}, effektiv=${it.effektiverBereich.zuLatex()}, Ziel=${statusText(it.zielPrüfung)}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(
                                enabled = index > 0,
                                onClick = {
                                    aktionen.knoten(
                                        tauscheErgänzungsReihenfolge(
                                            knoten,
                                            anschluss,
                                            ergänzungsPaare[index - 1].first,
                                        ),
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) { Text("↑") }
                            OutlinedButton(
                                enabled = index < ergänzungsPaare.lastIndex,
                                onClick = {
                                    aktionen.knoten(
                                        tauscheErgänzungsReihenfolge(
                                            knoten,
                                            anschluss,
                                            ergänzungsPaare[index + 1].first,
                                        ),
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) { Text("↓") }
                        }
                    }
                }
            }

            diagnose.methode?.let { methode ->
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Resultierende Fallvorschrift", style = MaterialTheme.typography.titleSmall)
                Text(
                    methode.alsMathematischeMethode("Darstellung der Bereichsanpassung").zuFallunterscheidungsLatex(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            diagnose.bedingungen.forEach { bedingung ->
                Text(
                    "Offene Bedingung: ${bedingung.zuLatex()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            diagnose.warnungen.forEach { warnung ->
                Text(
                    warnung,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
    ergebnis?.fehler?.let { fehler ->
        Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

internal fun tauscheErgänzungsReihenfolge(
    knoten: KnotenDaten,
    links: AnschlussDaten,
    rechts: AnschlussDaten,
): KnotenDaten = knoten.copy(
    anschlüsse = knoten.anschlüsse.map { anschluss ->
        when (anschluss.id) {
            links.id -> anschluss.copy(reihenfolge = rechts.reihenfolge)
            rechts.id -> anschluss.copy(reihenfolge = links.reihenfolge)
            else -> anschluss
        }
    },
)

@Composable
private fun DiagnoseZeile(name: String, wert: String) {
    Text(
        "$name = $wert",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun statusText(ergebnis: AussageErgebnis): String = when (ergebnis.wahrheitswert) {
    Wahrheitswert.Wahr -> "gültig"
    Wahrheitswert.Lüge -> "verletzt"
    null -> "offen"
}
