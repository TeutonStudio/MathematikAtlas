package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
internal fun KartenTabellenEingabeKonfiguration(
    zustand: AtlasZustand,
    quelle: KartenWahrheitstabellenQuelle,
    verbundeneFelder: Set<KartenTabellenAnschluss>,
    verbundeneWerte: Map<KartenTabellenAnschluss, BedingterWert?>,
    text: (String, String) -> String,
    speichereText: (String, String) -> Unit,
) {
    if (quelle.eingänge.isEmpty()) {
        Text(
            "Die Karte besitzt keine Eingänge; deshalb enthält die Tabelle genau eine Zeile.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Text("Eingaben", style = MaterialTheme.typography.titleMedium)
    quelle.eingänge.forEach { feld ->
        val verbunden = verbundeneWerte[feld]
        when {
            feld in verbundeneFelder && verbunden != null ->
                VerbundenerKartenTabellenEingang(feld, verbunden, text, speichereText)
            feld in verbundeneFelder -> Text(
                "${feld.name}: verbunden, aber nicht auswertbar",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            zustand.anschlussArten.istUnterart(feld.art, MathematikAnschlussArten.Aussage.id) ->
                Text("${feld.name}: freie Aussage", style = MaterialTheme.typography.bodySmall)
            feld.art == MathematikAnschlussArten.Methode.id ->
                FreieKartenTabellenPrädikatKonfiguration(feld, text, speichereText)
            else -> {
                val schlüssel = kartenTabellenWertSchlüssel(feld)
                OutlinedTextField(
                    value = text(schlüssel, standardWertFürKartenTabelle(feld.art)),
                    onValueChange = { speichereText(schlüssel, it) },
                    label = { Text("${feld.name}: ${feld.art.wert}") },
                    supportingText = { Text("Dieser Wert gilt für alle Tabellenzeilen.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun VerbundenerKartenTabellenEingang(
    feld: KartenTabellenAnschluss,
    wert: BedingterWert,
    text: (String, String) -> String,
    speichereText: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${feld.name}: verbunden und festgesetzt", style = MaterialTheme.typography.labelLarge)
        when (val objekt = wert.objekt) {
            is Aussage -> KartenAussageZelle(objekt.entscheide().wahrheitswert)
            is Methode -> {
                Text(objekt.aliasAnzeige(), style = MaterialTheme.typography.labelSmall)
                if (!objekt.istPrädikat()) {
                    Text(
                        "Diese Methode erfüllt das Prädikatskriterium nicht.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LatexText(objekt.zuLatex(), style = MaterialTheme.typography.bodyMedium)
                } else {
                    LatexText(
                        runCatching { kartenTabellenPrädikatSignatur(objekt) }.getOrElse { objekt.zuLatex() },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    objekt.parameter.forEachIndexed { index, parameter ->
                        val schlüssel = kartenTabellenPrädikatArgumentSchlüssel(feld, index)
                        val standard = when (parameter) {
                            is AussagenParameter -> "wahr"
                            else -> objekt.werteVorräte[parameter.name]
                                ?.let(::standardArgumentFürKartenTabelle)
                                ?: ""
                        }
                        OutlinedTextField(
                            value = text(schlüssel, standard),
                            onValueChange = { speichereText(schlüssel, it) },
                            label = { Text("Argument ${index + 1}: ${parameter.name}") },
                            supportingText = {
                                objekt.werteVorräte[parameter.name]?.let { Text("Wertevorrat: ${it.zuLatex()}") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
            else -> LatexText(wert.anzeigeLatex(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FreieKartenTabellenPrädikatKonfiguration(
    feld: KartenTabellenAnschluss,
    text: (String, String) -> String,
    speichereText: (String, String) -> Unit,
) {
    val mengenSchlüssel = kartenTabellenPrädikatMengenSchlüssel(feld)
    val mengenText = text(mengenSchlüssel, "R")
    val mengen = parseKartenTabellenMengenListe(mengenText)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LatexText(
            "${kartenTabellenLatexName(feld.name)}:" +
                mengen.joinToString("\\times") { it.zuLatex() },
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = mengenText,
            onValueChange = { speichereText(mengenSchlüssel, it) },
            label = { Text("Definitionsmengen, mit Komma") },
            supportingText = { Text("Fallback für einen noch unverbundenen Methodenanschluss, Beispiel: M, K, R") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        mengen.forEachIndexed { index, menge ->
            val schlüssel = kartenTabellenPrädikatArgumentSchlüssel(feld, index)
            OutlinedTextField(
                value = text(schlüssel, standardArgumentFürKartenTabelle(menge)),
                onValueChange = { speichereText(schlüssel, it) },
                label = { Text("Argument ${index + 1} aus ${menge.zuLatex()}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}
