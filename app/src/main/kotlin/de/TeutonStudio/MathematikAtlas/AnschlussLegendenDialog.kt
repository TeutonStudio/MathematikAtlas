package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister

internal data class AnschlussLegendenEintrag(
    val arten: List<AnschlussArt>,
) {
    init {
        require(arten.isNotEmpty()) { "Ein Legendeneintrag benötigt mindestens eine Anschlussart." }
    }

    val schlüssel: String = arten.joinToString("|") { it.id.wert }
    val titel: String = arten.joinToString(" oder ") { it.name }
    val gestreift: Boolean = arten.size > 1
}

/**
 * Leitet die Legende ausschließlich aus den Anschlüssen der aktuell geöffneten Karte ab.
 * Dieselbe Regel wie im Editor gilt: [de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten.zulässigeArten]
 * ersetzt bei einer ODER-Signatur die einzelne deklarierte Anschlussart.
 */
internal fun anschlussLegendenEinträge(
    karte: KartenDaten,
    register: AnschlussArtRegister,
): List<AnschlussLegendenEintrag> {
    val signaturen = karte.knoten
        .flatMap { it.anschlüsse }
        .map { anschluss ->
            val arten = if (anschluss.zulässigeArten.isNotEmpty()) {
                anschluss.zulässigeArten
            } else {
                setOf(anschluss.art)
            }
            arten.sortedBy { it.wert }
        }
        .distinct()

    return signaturen
        .map { ids ->
            AnschlussLegendenEintrag(ids.map { id -> register.legendenArt(id) })
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
            LegendenAnschlussSymbol(
                farben = eintrag.arten.map { art -> anschlussFarbe(art.id.wert) },
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

/** Entspricht der diagonalen Mehrfarbdarstellung der Anschlüsse im Karteneditor. */
@Composable
private fun LegendenAnschlussSymbol(
    farben: List<Color>,
) {
    val sichereFarben = farben.ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp).clip(CircleShape)) {
            if (sichereFarben.size == 1) {
                drawRect(sichereFarben.single())
            } else {
                val streifenBreite = (size.minDimension / sichereFarben.size.coerceAtLeast(2)).coerceAtLeast(2f)
                rotate(-45f) {
                    var x = -size.width * 1.5f
                    var index = 0
                    while (x < size.width * 2.5f) {
                        drawRect(
                            color = sichereFarben[index % sichereFarben.size],
                            topLeft = Offset(x, -size.height),
                            size = Size(streifenBreite, size.height * 3f),
                        )
                        x += streifenBreite
                        index += 1
                    }
                }
            }
        }
        Box(
            Modifier.size(20.dp)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}
