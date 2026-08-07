package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.RESTRIKTIONS_ERGÄNZUNG_PREFIX
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
        val ergänzungen = knoten.anschlüsse
            .filter {
                it.richtung == AnschlussRichtung.Eingang &&
                    it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX)
            }
            .sortedBy { it.reihenfolge }
            .mapNotNull { anschluss -> ergebnis?.eingänge?.get(anschluss.name)?.objekt as? Methode }
        val diagnose = if (basis != null && menge != null) {
            runCatching { restriktiereMethode(basis, menge, ergänzungen) }.getOrNull()
        } else null

        HorizontalDivider()
        Text("Bereichsvertrag", style = MaterialTheme.typography.titleSmall)
        if (basis == null || menge == null) {
            Text(
                "Methode und Menge verbinden, um Restriktion und gegebenenfalls notwendige Erweiterungen zu bestimmen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (diagnose != null) {
            DiagnoseZeile("W", diagnose.basisWerteVorrat.zuLatex())
            DiagnoseZeile("M", diagnose.gewünschterWerteVorrat.zuLatex())
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

            if (diagnose.ergänzungen.isNotEmpty()) {
                HorizontalDivider()
                Text("Ergänzungen", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    diagnose.ergänzungen.forEachIndexed { index, ergänzung ->
                        Text(
                            "${index + 1}. ${ergänzung.methode.name}: " +
                                "W${index + 1}=${ergänzung.werteVorrat.zuLatex()}, " +
                                "effektiv=${ergänzung.effektiverBereich.zuLatex()}, " +
                                "Ziel=${statusText(ergänzung.zielPrüfung)}",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DiagnoseZeile(name: String, wert: String) {
    Text(
        "$name = $wert",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun statusText(ergebnis: AussageErgebnis): String = when (ergebnis.wahrheitswert) {
    Wahrheitswert.Wahr -> "gültig"
    Wahrheitswert.Lüge -> "verletzt"
    null -> "offen"
}
