package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_AUSWAHL
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MengenKnotenAuswahl
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator

internal object MengenKnotenKartenQuelle {
    private var kartenLieferant: () -> List<KartenDaten> = { emptyList() }
    private var versionenLieferant: (KartenId) -> List<KartenDaten> = { emptyList() }
    private var aktuelleKarteLieferant: () -> KartenId? = { null }

    fun installieren(zustand: AtlasZustand) {
        kartenLieferant = { zustand.karten }
        versionenLieferant = zustand::kartenVersionen
        aktuelleKarteLieferant = { zustand.editor.karte.id }
    }

    fun kandidaten(): List<EigeneMengenKarte> {
        val aktuelleId = aktuelleKarteLieferant()
        return kartenLieferant().asSequence()
            .filter { !it.archiviert && it.id != aktuelleId }
            .flatMap { karte -> versionenLieferant(karte.id).asSequence() }
            .mapNotNull(::alsEigeneMengenKarte)
            .distinctBy(EigeneMengenKarte::verweis)
            .sortedWith(compareBy<EigeneMengenKarte> { it.name.lowercase() }.thenByDescending { it.verweis.version })
            .toList()
    }

    fun lade(verweis: KartenVerweis): KartenDaten? =
        versionenLieferant(verweis.kartenId).firstOrNull { it.version == verweis.version }
}

internal data class EigeneMengenKarte(
    val name: String,
    val verweis: KartenVerweis,
    val ausgangsName: String,
)

private fun alsEigeneMengenKarte(karte: KartenDaten): EigeneMengenKarte? {
    if (karte.knoten.any { it.art == "mathematik.kartenEingang" }) return null
    val ausgang = karte.knoten.singleOrNull { it.art == "mathematik.kartenAusgang" } ?: return null
    val wertEingang = ausgang.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "wert"
    } ?: return null
    if (wertEingang.art != MathematikAnschlussArten.Menge.id) return null
    val name = ausgang.parameter["name"]?.trim().orEmpty().ifBlank { ausgang.name }
    return EigeneMengenKarte(
        name = karte.name,
        verweis = KartenVerweis(karte.id, karte.version),
        ausgangsName = name,
    )
}

private data class MengenAuswahlEintrag(
    val schlüssel: String,
    val titel: String,
    val beschreibung: String,
    val eingebaut: MengenKnotenAuswahl? = null,
    val eigeneKarte: EigeneMengenKarte? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
internal object MengenKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val eingebaute = MengenKnotenAuswahl.entries.map { auswahl ->
            MengenAuswahlEintrag(
                schlüssel = "intern:${auswahl.stabileId}",
                titel = auswahl.titel,
                beschreibung = auswahl.beschreibung,
                eingebaut = auswahl,
            )
        }
        val eigene = MengenKnotenKartenQuelle.kandidaten().map { karte ->
            MengenAuswahlEintrag(
                schlüssel = "karte:${karte.verweis.kartenId.wert}:${karte.verweis.version}",
                titel = "${karte.name} · v${karte.verweis.version}",
                beschreibung = "Eigene Karte ohne Eingänge mit dem Mengenausgang „${karte.ausgangsName}“; die Version wird fest referenziert.",
                eigeneKarte = karte,
            )
        }
        val einträge = eingebaute + eigene
        val aktuell = knoten.kartenVerweis?.let { ref ->
            eigene.firstOrNull { it.eigeneKarte?.verweis == ref }
        } ?: eingebaute.firstOrNull {
            it.eingebaut == MengenKnotenAuswahl.vonId(knoten.parameter[MENGEN_KNOTEN_AUSWAHL])
        } ?: eingebaute.first()
        var geöffnet by remember(knoten.id, knoten.kartenVerweis, knoten.parameter[MENGEN_KNOTEN_AUSWAHL]) {
            mutableStateOf(false)
        }

        Text("Menge", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = aktuell.titel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Ausgewählte Menge") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                einträge.forEach { eintrag ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                eintrag.titel,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        },
                        onClick = {
                            geöffnet = false
                            eintrag.eingebaut?.let { auswahl ->
                                aktionen.knoten(
                                    knoten.copy(
                                        parameter = knoten.parameter + (MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
                                        kartenVerweis = null,
                                        anschlüsse = knoten.anschlüsse.map { anschluss ->
                                            if (anschluss.richtung == AnschlussRichtung.Ausgang) {
                                                anschluss.copy(
                                                    name = "menge",
                                                    kante = AnschlussKante.Rechts,
                                                    art = MathematikAnschlussArten.Menge.id,
                                                )
                                            } else anschluss
                                        },
                                    ),
                                )
                            }
                            eintrag.eigeneKarte?.let { karte ->
                                aktionen.knoten(
                                    knoten.copy(
                                        parameter = knoten.parameter + (MENGEN_KNOTEN_AUSWAHL to eintrag.schlüssel),
                                        kartenVerweis = karte.verweis,
                                        anschlüsse = knoten.anschlüsse.map { anschluss ->
                                            if (anschluss.richtung == AnschlussRichtung.Ausgang) {
                                                anschluss.copy(
                                                    name = karte.ausgangsName,
                                                    kante = AnschlussKante.Rechts,
                                                    art = MathematikAnschlussArten.Menge.id,
                                                )
                                            } else anschluss
                                        },
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
        Text(
            aktuell.beschreibung,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (eigene.isEmpty()) {
            Text(
                "Eigene Karten erscheinen hier, sobald sie keine öffentlichen Eingänge und genau einen öffentlichen Mengenausgang besitzen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal object VektorRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operator = VektorRechnerOperator.entries.firstOrNull {
            it.stabileId == knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        } ?: VektorRechnerOperator.SKALARPRODUKT
        var geöffnet by remember(knoten.id, operator) { mutableStateOf(false) }
        Text("Vektoroperation", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = operator.name.lowercase().replaceFirstChar(Char::uppercase),
                onValueChange = {},
                readOnly = true,
                label = { Text("Operator") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                DropdownMenuItem(
                    text = { Text("Skalarprodukt") },
                    onClick = {
                        geöffnet = false
                        aktionen.parameter(VEKTOR_RECHNER_OPERATOR, VektorRechnerOperator.SKALARPRODUKT.stabileId)
                    },
                )
            }
        }
        Text(
            "Zeilenvektoren, Spaltenvektoren und kartesische Zahlentupel sind zulässig; die Komponentenreihenfolge bleibt erhalten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
