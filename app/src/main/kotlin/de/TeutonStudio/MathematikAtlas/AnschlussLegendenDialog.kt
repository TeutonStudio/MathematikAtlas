package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AnschlussSymbol
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.sichtbareAnschlussArtIds

internal data class AnschlussLegendenEintrag(
    val arten: List<AnschlussArt>,
    val symbolAnschluss: AnschlussDaten,
) {
    init {
        require(arten.isNotEmpty()) { "Ein Legendeneintrag benötigt mindestens eine Anschlussart." }
    }

    val schlüssel: String = arten.joinToString("|") { it.id.wert }
    val titel: String = arten.joinToString(" oder ") { it.name }
    val gestreift: Boolean = arten.size > 1
}

private data class AnschlussLegendenSignatur(
    val ids: List<AnschlussArtId>,
    val symbolAnschluss: AnschlussDaten,
)

/**
 * Leitet die Legende aus den tatsächlich sichtbaren Anschlusssymbolen der geöffneten Karte ab.
 * Semantische Methoden- und Tupelverträge werden dabei als solche geführt, selbst wenn ihre
 * physische Anschlussart allgemeiner ist. Dadurch kann insbesondere eine Methode nicht mehr
 * als bloßes "Mathematisches Objekt" aus der Legende verschwinden.
 */
internal fun anschlussLegendenEinträge(
    karte: KartenDaten,
    register: AnschlussArtRegister,
): List<AnschlussLegendenEintrag> {
    val signaturen = karte.knoten
        .flatMap { it.anschlüsse }
        .map { anschluss ->
            AnschlussLegendenSignatur(
                ids = sichtbareAnschlussArtIds(anschluss),
                symbolAnschluss = anschluss,
            )
        }
        .distinctBy { it.ids }

    return signaturen
        .map { signatur ->
            AnschlussLegendenEintrag(
                arten = signatur.ids.map { id -> register.legendenArt(id) },
                symbolAnschluss = signatur.symbolAnschluss,
            )
        }
        .sortedWith(
            compareBy<AnschlussLegendenEintrag>(
                { it.gestreift },
                { it.titel.lowercase() },
                { it.schlüssel },
            ),
        )
}

private fun AnschlussArtRegister.legendenArt(id: AnschlussArtId): AnschlussArt {
    val gefunden = finde(id) ?: return AnschlussArt(
        id = id,
        name = id.wert,
        beschreibung = "Nicht registrierte Anschlussart '${id.wert}'.",
    )
    return if (gefunden.beschreibung.isNotBlank()) {
        gefunden
    } else {
        gefunden.copy(beschreibung = "Anschlussart ${gefunden.name}.")
    }
}

@Composable
internal fun AnschlussLegendenDialog(
    zustand: AtlasZustand,
    schließen: () -> Unit,
) {
    val einträge = remember(zustand.editor.karte) {
        anschlussLegendenEinträge(zustand.editor.karte, zustand.anschlussArten)
    }

    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Legende") },
        text = {
            if (einträge.isEmpty()) {
                Text("Die aktuelle Karte enthält keine Anschlüsse.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (einträge.any { it.gestreift }) {
                        Text(
                            "Gestreifte Anschlüsse bedeuten ODER: Eine der dargestellten Anschlussarten ist zulässig.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Anschluss",
                            modifier = Modifier.width(84.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "Erklärung",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    ) {
                        items(einträge, key = { it.schlüssel }) { eintrag ->
                            AnschlussLegendenZeile(eintrag)
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = schließen) { Text("Schließen") }
        },
    )
}

@Composable
private fun AnschlussLegendenZeile(eintrag: AnschlussLegendenEintrag) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .semantics { contentDescription = "Anschluss ${eintrag.titel}" },
            contentAlignment = Alignment.Center,
        ) {
            AnschlussSymbol(
                anschluss = eintrag.symbolAnschluss,
                fallbackFarben = eintrag.arten.map { art -> anschlussFarbe(art.id.wert) },
                größe = 20f,
                zoom = 1f,
                aktiviert = true,
                farbeFürAnschluss = { anschluss -> anschlussFarbe(anschluss.art.wert) },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(eintrag.titel, style = MaterialTheme.typography.labelLarge)
            if (eintrag.gestreift) {
                eintrag.arten.forEach { art ->
                    Text(
                        "${art.name}: ${art.beschreibung}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    eintrag.arten.single().beschreibung,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
