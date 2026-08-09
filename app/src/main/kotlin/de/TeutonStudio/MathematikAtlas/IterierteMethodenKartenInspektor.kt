package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.formatiereAuswertungsDauerNanos
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

internal data class IterierteMethodenKartenOption(
    val karte: KartenDaten,
    val kompatibel: Boolean,
    val grund: String? = null,
)

private val ITERIERTE_METHODEN_ARTEN = setOf<KnotenArtId>(
    "mathematik.iterierteSumme",
    "mathematik.iteriertesProdukt",
    MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART,
    "mathematik.iterierteVereinigung",
    "mathematik.iterierterSchnitt",
    "mathematik.iteriertesKartesischesProdukt",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IterierteMethodenKartenInspektor(knoten: KnotenDaten, zustand: AtlasZustand) {
    KnotenAuswertungsDiagnose(knoten, zustand)
    if (knoten.art !in ITERIERTE_METHODEN_ARTEN) return
    val methodeVerbunden = zustand.istMethodenEingangVerbunden(knoten)
    val ausgewählt = knoten.eingangsKartenVerweise["methode"]
    val ausgewählteKarte = ausgewählt?.let(zustand.speicher::lade)
    val optionen = zustand.iterierteMethodenKartenOptionen(knoten)
    var geöffnet by remember(knoten.id, ausgewählt) { mutableStateOf(false) }

    HorizontalDivider()
    Text("Methode aus Karte", style = MaterialTheme.typography.titleSmall)
    Text(
        if (methodeVerbunden) {
            "Eine verbundene Methode hat Vorrang. Die gespeicherte Kartenauswahl bleibt als Fallback erhalten."
        } else {
            "Solange der Methoden-Eingang unverbunden ist, wird die ausgewählte Karte als einwertige Methode verwendet."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ExposedDropdownMenuBox(
        expanded = geöffnet,
        onExpandedChange = { if (!methodeVerbunden) geöffnet = it },
    ) {
        OutlinedTextField(
            value = ausgewählteKarte?.let { "${it.name} · v${it.version}" } ?: "Keine Karte ausgewählt",
            onValueChange = {},
            readOnly = true,
            enabled = !methodeVerbunden,
            label = { Text("Kartenmethode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
            DropdownMenuItem(
                text = { Text("Keine Karte ausgewählt") },
                onClick = {
                    zustand.setzeIterierteMethodenKarte(knoten, null)
                    geöffnet = false
                },
            )
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${option.karte.name} · v${option.karte.version}")
                            option.grund?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    enabled = option.kompatibel,
                    onClick = {
                        zustand.setzeIterierteMethodenKarte(
                            knoten,
                            KartenVerweis(option.karte.id, option.karte.version),
                        )
                        geöffnet = false
                    },
                )
            }
        }
    }

    if (ausgewählt != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { zustand.öffne(ausgewählt) },
                enabled = ausgewählteKarte != null,
                modifier = Modifier.weight(1f),
            ) { Text("Karte öffnen") }
            OutlinedButton(
                onClick = { zustand.setzeIterierteMethodenKarte(knoten, null) },
                enabled = !methodeVerbunden,
                modifier = Modifier.weight(1f),
            ) { Text("Auswahl entfernen") }
        }
        val neueste = zustand.karten.firstOrNull { it.id == ausgewählt.kartenId }
        if (neueste != null && neueste.version > ausgewählt.version) {
            val prüfung = zustand.prüfeIterierteMethodenKarte(knoten, neueste)
            Button(
                onClick = {
                    zustand.setzeIterierteMethodenKarte(knoten, KartenVerweis(neueste.id, neueste.version))
                },
                enabled = !methodeVerbunden && prüfung.kompatibel,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Auf Version ${neueste.version} aktualisieren") }
            if (!prüfung.kompatibel) {
                Text(
                    prüfung.grund ?: "Die neueste Version ist nicht kompatibel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (ausgewählteKarte == null) {
            Text(
                "Die festgehaltene Kartenversion ist nicht mehr vorhanden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun KnotenAuswertungsDiagnose(knoten: KnotenDaten, zustand: AtlasZustand) {
    val dauer = zustand.auswertung.knoten[knoten.id]?.auswertungsDauerNanos
    HorizontalDivider()
    Text("Auswertung", style = MaterialTheme.typography.titleSmall)
    Text(
        dauer?.let { "Letzte Auswertung: ${formatiereAuswertungsDauerNanos(it)}" }
            ?: "Noch keine Auswertungsdauer gemessen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = { zustand.berechneKnotenCacheNeu(knoten.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Cache neu errechnen")
    }
}

internal fun AtlasZustand.istMethodenEingangVerbunden(knoten: KnotenDaten): Boolean {
    val methode = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return false
    return editor.karte.verbindungen.any {
        it.zu.knotenId == knoten.id && it.zu.anschlussId == methode.id
    }
}

internal fun AtlasZustand.iterierteMethodenKartenOptionen(knoten: KnotenDaten): List<IterierteMethodenKartenOption> =
    karten.asSequence()
        .filter { !it.archiviert && it.id != editor.karte.id }
        .map { prüfeIterierteMethodenKarte(knoten, it) }
        .sortedBy { it.karte.name.lowercase() }
        .toList()

internal fun AtlasZustand.prüfeIterierteMethodenKarte(
    knoten: KnotenDaten,
    karte: KartenDaten,
): IterierteMethodenKartenOption {
    if (karte.id == editor.karte.id || karte.archiviert) {
        return IterierteMethodenKartenOption(karte, false, "Die aktuelle oder archivierte Karte ist nicht auswählbar.")
    }
    if (referenziertMitEingangsFallback(karte, editor.karte.id, mutableSetOf())) {
        return IterierteMethodenKartenOption(karte, false, "Die Auswahl würde einen Kartenzyklus erzeugen.")
    }
    val eingänge = karte.knoten.filter { it.art == "mathematik.kartenEingang" }
        .sortedWith(compareBy({ it.position.y }, { it.position.x }, { it.id.wert }))
    val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }
        .sortedWith(compareBy({ it.position.y }, { it.position.x }, { it.id.wert }))
    if (eingänge.size != 1) {
        return IterierteMethodenKartenOption(karte, false, "Benötigt genau einen öffentlichen Karten-Eingang.")
    }
    if (ausgänge.size != 1) {
        return IterierteMethodenKartenOption(karte, false, "Benötigt genau einen öffentlichen Karten-Ausgang.")
    }
    val eingangsArt = eingänge.single().anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }?.art
    if (eingangsArt != MathematikAnschlussArten.Zahl.id) {
        return IterierteMethodenKartenOption(karte, false, "Der Methodenparameter muss eine Zahl sein.")
    }
    val erwarteteAusgabe = erwarteteIterierteMethodenAusgabe(knoten)
    val ausgangsArt = ausgänge.single().anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang }?.art
    if (ausgangsArt != erwarteteAusgabe) {
        val erwartet = MathematikAnschlussArten.alle.firstOrNull { it.id == erwarteteAusgabe }?.name ?: erwarteteAusgabe.wert
        val tatsächlich = MathematikAnschlussArten.alle.firstOrNull { it.id == ausgangsArt }?.name ?: ausgangsArt?.wert.orEmpty()
        return IterierteMethodenKartenOption(karte, false, "Erwartet $erwartet, liefert ${tatsächlich.ifBlank { "keinen passenden Wert" }}.")
    }
    return IterierteMethodenKartenOption(karte, true)
}

internal fun AtlasZustand.setzeIterierteMethodenKarte(
    knoten: KnotenDaten,
    verweis: KartenVerweis?,
): Boolean {
    if (verweis != null) {
        val karte = speicher.lade(verweis) ?: return false
        if (!prüfeIterierteMethodenKarte(knoten, karte).kompatibel) return false
    }
    val neueVerweise = if (verweis == null) {
        knoten.eingangsKartenVerweise - "methode"
    } else {
        knoten.eingangsKartenVerweise + ("methode" to verweis)
    }
    editor.führeAus(KartenAktion.KnotenErsetzen(knoten.copy(eingangsKartenVerweise = neueVerweise)))
    return true
}

private fun erwarteteIterierteMethodenAusgabe(knoten: KnotenDaten): AnschlussArtId = when (knoten.art) {
    "mathematik.iterierteSumme", "mathematik.iteriertesProdukt" -> MathematikAnschlussArten.Zahl.id
    MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART -> MathematikAnschlussArten.Aussage.id
    else -> MathematikAnschlussArten.Menge.id
}

private fun AtlasZustand.referenziertMitEingangsFallback(
    karte: KartenDaten,
    gesuchteId: KartenId,
    besucht: MutableSet<KartenVerweis>,
): Boolean {
    val refs = karte.knoten.flatMap { knoten ->
        listOfNotNull(knoten.kartenVerweis) + knoten.eingangsKartenVerweise.values
    }
    if (refs.any { it.kartenId == gesuchteId }) return true
    return refs.any { ref ->
        if (!besucht.add(ref)) false
        else speicher.lade(ref)?.let { referenziertMitEingangsFallback(it, gesuchteId, besucht) } == true
    }
}
