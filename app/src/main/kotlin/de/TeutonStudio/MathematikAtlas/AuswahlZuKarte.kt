package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId
import de.TeutonStudio.KnotenKartenVerwalter.daten.neueKartenId
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import java.util.UUID

internal data class KartenGrenzAnschlussVorschlag(
    val verbindungsId: VerbindungsId,
    val innererAnschluss: AnschlussVerweis,
    val äußererAnschluss: AnschlussVerweis,
    val art: AnschlussArtId,
    val vorgeschlagenerName: String,
    val beschreibung: String,
)

internal data class AuswahlKartenVorschau(
    val quelle: KartenDaten,
    val ausgewählteKnoten: List<KnotenDaten>,
    val innereVerbindungen: List<VerbindungDaten>,
    val eingänge: List<KartenGrenzAnschlussVorschlag>,
    val ausgänge: List<KartenGrenzAnschlussVorschlag>,
    val konflikte: List<String>,
) {
    fun validierungsFehler(
        kartenName: String,
        eingangsNamen: List<String>,
        ausgangsNamen: List<String>,
    ): List<String> = buildList {
        addAll(konflikte)
        if (kartenName.isBlank()) add("Der Kartenname darf nicht leer sein.")
        if (eingangsNamen.size != eingänge.size) add("Die Zahl der Karteneingänge stimmt nicht mit der Vorschau überein.")
        if (ausgangsNamen.size != ausgänge.size) add("Die Zahl der Kartenausgänge stimmt nicht mit der Vorschau überein.")
        addAll(schnittstellenNamenFehler("Karteneingänge", eingangsNamen))
        addAll(schnittstellenNamenFehler("Kartenausgänge", ausgangsNamen))
    }
}

private fun schnittstellenNamenFehler(
    bezeichnung: String,
    namen: List<String>,
): List<String> = buildList {
    if (namen.any(String::isBlank)) add("$bezeichnung benötigen jeweils einen Namen.")
    val doppelte = namen.map(String::trim)
        .filter(String::isNotEmpty)
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (doppelte.isNotEmpty()) {
        add("$bezeichnung müssen eindeutig sein: ${doppelte.sorted().joinToString()}.")
    }
}

internal fun KartenDaten.vorschauFürNeueKarte(
    knotenIds: Set<KnotenId>,
    anschlussArten: AnschlussArtRegister,
): AuswahlKartenVorschau {
    val ausgewählteKnoten = knoten.filter { it.id in knotenIds }
    val konflikte = buildList {
        if (knotenIds.size < 2) add("Mindestens zwei Knoten werden benötigt.")
        val fehlende = knotenIds - ausgewählteKnoten.mapTo(mutableSetOf(), KnotenDaten::id)
        if (fehlende.isNotEmpty()) add("${fehlende.size} ausgewählte Knoten existieren nicht mehr.")
    }.toMutableList()
    val ausgewählteIds = ausgewählteKnoten.mapTo(mutableSetOf(), KnotenDaten::id)
    val prüfung = GraphPrüfung(anschlussArten)
    val innereVerbindungen = mutableListOf<VerbindungDaten>()
    val eingänge = mutableListOf<KartenGrenzAnschlussVorschlag>()
    val ausgänge = mutableListOf<KartenGrenzAnschlussVorschlag>()

    verbindungen.forEach { verbindung ->
        val vonInnen = verbindung.von.knotenId in ausgewählteIds
        val zuInnen = verbindung.zu.knotenId in ausgewählteIds
        when {
            vonInnen && zuInnen -> innereVerbindungen += verbindung
            !vonInnen && zuInnen -> grenzVorschlag(
                verbindung = verbindung,
                innererAnschluss = verbindung.zu,
                äußererAnschluss = verbindung.von,
                prüfung = prüfung,
                eingang = true,
                vorhandeneNamen = eingänge.mapTo(mutableSetOf(), KartenGrenzAnschlussVorschlag::vorgeschlagenerName),
            )?.let(eingänge::add) ?: konflikte.add("Eine eingehende Grenzverbindung verweist auf einen fehlenden Anschluss.")
            vonInnen && !zuInnen -> grenzVorschlag(
                verbindung = verbindung,
                innererAnschluss = verbindung.von,
                äußererAnschluss = verbindung.zu,
                prüfung = prüfung,
                eingang = false,
                vorhandeneNamen = ausgänge.mapTo(mutableSetOf(), KartenGrenzAnschlussVorschlag::vorgeschlagenerName),
            )?.let(ausgänge::add) ?: konflikte.add("Eine ausgehende Grenzverbindung verweist auf einen fehlenden Anschluss.")
        }
    }

    return AuswahlKartenVorschau(
        quelle = this,
        ausgewählteKnoten = ausgewählteKnoten,
        innereVerbindungen = innereVerbindungen,
        eingänge = eingänge,
        ausgänge = ausgänge,
        konflikte = konflikte,
    )
}

private fun KartenDaten.grenzVorschlag(
    verbindung: VerbindungDaten,
    innererAnschluss: AnschlussVerweis,
    äußererAnschluss: AnschlussVerweis,
    prüfung: GraphPrüfung,
    eingang: Boolean,
    vorhandeneNamen: Set<String>,
): KartenGrenzAnschlussVorschlag? {
    val innererKnoten = knoten.firstOrNull { it.id == innererAnschluss.knotenId } ?: return null
    val innererPort = findeAnschluss(innererAnschluss) ?: return null
    val äußererKnoten = knoten.firstOrNull { it.id == äußererAnschluss.knotenId } ?: return null
    val äußererPort = findeAnschluss(äußererAnschluss) ?: return null
    val basisName = listOf(innererKnoten.name, innererPort.name)
        .filter(String::isNotBlank)
        .joinToString(" – ")
        .ifBlank { if (eingang) "eingang" else "ausgang" }
    val name = eindeutigerName(basisName, vorhandeneNamen)
    val beschreibung = if (eingang) {
        "${äußererKnoten.name}.${äußererPort.name} → ${innererKnoten.name}.${innererPort.name}"
    } else {
        "${innererKnoten.name}.${innererPort.name} → ${äußererKnoten.name}.${äußererPort.name}"
    }
    return KartenGrenzAnschlussVorschlag(
        verbindungsId = verbindung.id,
        innererAnschluss = innererAnschluss,
        äußererAnschluss = äußererAnschluss,
        art = prüfung.effektiveArt(this, verbindung.von),
        vorgeschlagenerName = name,
        beschreibung = beschreibung,
    )
}

private fun eindeutigerName(basis: String, vorhandene: Set<String>): String {
    if (basis !in vorhandene) return basis
    var nummer = 2
    while ("$basis ($nummer)" in vorhandene) nummer += 1
    return "$basis ($nummer)"
}

internal fun AuswahlKartenVorschau.materialisiere(
    kartenName: String,
    eingangsNamen: List<String>,
    ausgangsNamen: List<String>,
): KartenDaten {
    val fehler = validierungsFehler(kartenName, eingangsNamen, ausgangsNamen)
    require(fehler.isEmpty()) { fehler.joinToString(" ") }

    val kartenId = neueKartenId()
    val namensraum = kartenId.wert
    val knotenIds = ausgewählteKnoten.associate { knoten ->
        knoten.id to KnotenId(deterministischeId(namensraum, "knoten", knoten.id.wert))
    }
    val anschlussIds = ausgewählteKnoten.flatMap { knoten ->
        knoten.anschlüsse.map { anschluss ->
            AnschlussVerweis(knoten.id, anschluss.id) to AnschlussId(
                deterministischeId(namensraum, "anschluss", "${knoten.id.wert}:${anschluss.id.wert}"),
            )
        }
    }.toMap()

    fun AnschlussVerweis.remappe(): AnschlussVerweis = AnschlussVerweis(
        knotenId = knotenIds.getValue(knotenId),
        anschlussId = anschlussIds.getValue(this),
    )

    val links = ausgewählteKnoten.minOf { it.position.x }
    val oben = ausgewählteKnoten.minOf { it.position.y }
    val verschiebung = GraphPunkt(320f - links, 80f - oben)
    val kopierteKnoten = ausgewählteKnoten.map { knoten ->
        knoten.copy(
            id = knotenIds.getValue(knoten.id),
            position = knoten.position + verschiebung,
            anschlüsse = knoten.anschlüsse.map { anschluss ->
                anschluss.copy(id = anschlussIds.getValue(AnschlussVerweis(knoten.id, anschluss.id)))
            },
        )
    }
    val kopierteVerbindungen = innereVerbindungen.map { verbindung ->
        VerbindungDaten(
            id = VerbindungsId(deterministischeId(namensraum, "innere-verbindung", verbindung.id.wert)),
            von = verbindung.von.remappe(),
            zu = verbindung.zu.remappe(),
        )
    }

    val eingangsKnoten = eingänge.mapIndexed { index, vorschlag ->
        val basis = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt(40f, 80f + index * 130f))
        val anschluss = basis.anschlüsse.single().copy(
            id = AnschlussId(deterministischeId(namensraum, "karten-eingang-anschluss", vorschlag.verbindungsId.wert)),
            art = vorschlag.art,
        )
        basis.copy(
            id = KnotenId(deterministischeId(namensraum, "karten-eingang", vorschlag.verbindungsId.wert)),
            anschlüsse = listOf(anschluss),
            parameter = basis.parameter + ("name" to eingangsNamen[index].trim()),
        )
    }
    val ausgangsX = (kopierteKnoten.maxOfOrNull { it.position.x + it.größe.breite } ?: 320f) + 120f
    val ausgangsKnoten = ausgänge.mapIndexed { index, vorschlag ->
        val basis = MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt(ausgangsX, 80f + index * 130f))
        val anschluss = basis.anschlüsse.single().copy(
            id = AnschlussId(deterministischeId(namensraum, "karten-ausgang-anschluss", vorschlag.verbindungsId.wert)),
            art = vorschlag.art,
        )
        basis.copy(
            id = KnotenId(deterministischeId(namensraum, "karten-ausgang", vorschlag.verbindungsId.wert)),
            anschlüsse = listOf(anschluss),
            parameter = basis.parameter + ("name" to ausgangsNamen[index].trim()),
        )
    }
    val eingangsVerbindungen = eingänge.mapIndexed { index, vorschlag ->
        VerbindungDaten(
            id = VerbindungsId(deterministischeId(namensraum, "karten-eingang-verbindung", vorschlag.verbindungsId.wert)),
            von = AnschlussVerweis(eingangsKnoten[index].id, eingangsKnoten[index].anschlüsse.single().id),
            zu = vorschlag.innererAnschluss.remappe(),
        )
    }
    val ausgangsVerbindungen = ausgänge.mapIndexed { index, vorschlag ->
        VerbindungDaten(
            id = VerbindungsId(deterministischeId(namensraum, "karten-ausgang-verbindung", vorschlag.verbindungsId.wert)),
            von = vorschlag.innererAnschluss.remappe(),
            zu = AnschlussVerweis(ausgangsKnoten[index].id, ausgangsKnoten[index].anschlüsse.single().id),
        )
    }

    return KartenDaten(
        id = KartenId(kartenId.wert),
        name = kartenName.trim(),
        version = 1,
        knoten = eingangsKnoten + kopierteKnoten + ausgangsKnoten,
        verbindungen = eingangsVerbindungen + kopierteVerbindungen + ausgangsVerbindungen,
        visuelleGruppen = emptyList(),
        ansicht = AnsichtsFenster.Standard,
        archiviert = false,
    )
}

private fun deterministischeId(namensraum: String, art: String, quelle: String): String =
    UUID.nameUUIDFromBytes("$namensraum|$art|$quelle".toByteArray(Charsets.UTF_8)).toString()

@Composable
internal fun AuswahlZuKarteDialog(
    zustand: AtlasZustand,
    knotenIds: Set<KnotenId>,
    schließen: () -> Unit,
    erstellt: () -> Unit,
) {
    val vorschau = remember(zustand.editor.karte, knotenIds) {
        zustand.editor.karte.vorschauFürNeueKarte(knotenIds, zustand.anschlussArten)
    }
    val vorhandeneNamen = remember(zustand.karten) { zustand.karten.mapTo(mutableSetOf(), KartenDaten::name) }
    var kartenName by remember(vorschau) {
        mutableStateOf(eindeutigerName("Karte aus Auswahl", vorhandeneNamen))
    }
    val eingangsNamen = remember(vorschau) { vorschau.eingänge.map { it.vorgeschlagenerName }.toMutableStateList() }
    val ausgangsNamen = remember(vorschau) { vorschau.ausgänge.map { it.vorgeschlagenerName }.toMutableStateList() }
    var speicherFehler by remember(vorschau) { mutableStateOf<String?>(null) }
    val fehler = vorschau.validierungsFehler(kartenName, eingangsNamen, ausgangsNamen)

    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Auswahl als neue Karte") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${vorschau.ausgewählteKnoten.size} Knoten und ${vorschau.innereVerbindungen.size} innere Verbindungen werden kopiert. Der Ursprungsgraph bleibt unverändert.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = kartenName,
                    onValueChange = { kartenName = it; speicherFehler = null },
                    label = { Text("Kartenname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SchnittstellenVorschläge(
                    titel = "Karteneingänge",
                    leerText = "Keine eingehenden Grenzverbindungen.",
                    vorschläge = vorschau.eingänge,
                    namen = eingangsNamen,
                    beiÄnderung = { index, wert -> eingangsNamen[index] = wert; speicherFehler = null },
                )
                SchnittstellenVorschläge(
                    titel = "Kartenausgänge",
                    leerText = "Keine ausgehenden Grenzverbindungen.",
                    vorschläge = vorschau.ausgänge,
                    namen = ausgangsNamen,
                    beiÄnderung = { index, wert -> ausgangsNamen[index] = wert; speicherFehler = null },
                )
                (fehler + listOfNotNull(speicherFehler)).distinct().forEach { meldung ->
                    Text(meldung, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = fehler.isEmpty(),
                onClick = {
                    runCatching {
                        val karte = vorschau.materialisiere(kartenName, eingangsNamen, ausgangsNamen)
                        zustand.speichereAktuell()
                        zustand.importiere(zustand.speicher.exportiere(karte))
                        zustand.linkerBereich = VerwaltungsBereich.Karten
                    }.onSuccess {
                        erstellt()
                    }.onFailure { ursache ->
                        speicherFehler = "Die neue Karte konnte nicht gespeichert werden: ${ursache.message ?: ursache::class.simpleName}"
                    }
                },
            ) { Text("Karte anlegen") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}

@Composable
private fun SchnittstellenVorschläge(
    titel: String,
    leerText: String,
    vorschläge: List<KartenGrenzAnschlussVorschlag>,
    namen: List<String>,
    beiÄnderung: (Int, String) -> Unit,
) {
    Text(titel, style = MaterialTheme.typography.titleSmall)
    if (vorschläge.isEmpty()) {
        Text(leerText, style = MaterialTheme.typography.bodySmall)
        return
    }
    vorschläge.forEachIndexed { index, vorschlag ->
        OutlinedTextField(
            value = namen.getOrElse(index) { "" },
            onValueChange = { beiÄnderung(index, it) },
            label = { Text("${titel.dropLast(1)} ${index + 1}") },
            supportingText = {
                Column {
                    Text(vorschlag.beschreibung)
                    Text(vorschlag.art.wert)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
