package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

internal object EndlicheMengeInspektor : KnotenInspektor {
    private val artRegister = AnschlussArtRegister(MathematikAnschlussArten.alle)

    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val rohdaten = knoten.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER]
        val altwert = knoten.parameter[ENDLICHE_MENGE_ALT_PARAMETER]
        val gelesen = remember(knoten.id, rohdaten, altwert) { leseEndlicheMengeKonfiguration(knoten) }
        val normalisierung = remember(gelesen.konfiguration) {
            normalisiereEndlicheMengeKonfiguration(gelesen.konfiguration)
        }
        val konfiguration = normalisierung.konfiguration
        var lokaleWarnung by remember(knoten.id) { mutableStateOf<String?>(null) }

        LaunchedEffect(knoten.id, rohdaten, altwert, normalisierung.konfiguration) {
            if (gelesen.fehler == null &&
                (gelesen.altformat || normalisierung.konfiguration != gelesen.konfiguration)
            ) {
                lokaleWarnung = normalisierung.warnungen.firstOrNull()
                aktionen.parameter(
                    ENDLICHE_MENGE_KONFIGURATION_PARAMETER,
                    normalisierung.konfiguration.zuParameter(),
                )
            }
        }

        fun speichern(einträge: List<EndlicheMengeEintrag>) {
            val normalisiert = normalisiereEndlicheMengeKonfiguration(
                konfiguration.copy(einträge = einträge).mitErkannterGemeinsamerArt(),
            )
            lokaleWarnung = normalisiert.warnungen.firstOrNull()
            aktionen.parameter(
                ENDLICHE_MENGE_KONFIGURATION_PARAMETER,
                normalisiert.konfiguration.zuParameter(),
            )
        }

        Text("Elemente", style = MaterialTheme.typography.titleSmall)
        Text(
            "Die Reihenfolge dient nur der Bearbeitung und Darstellung; mathematisch wird eine ungeordnete Menge weitergegeben.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        gelesen.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (konfiguration.einträge.isEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("∅", style = MaterialTheme.typography.headlineMedium)
                    Text("Diese Menge enthält keine Elemente.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { speichern(listOf(neuerEndlicheMengeEintrag())) }) {
                        Text("+ Erstes Element")
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                konfiguration.einträge.forEachIndexed { index, eintrag ->
                    ElementKarte(
                        index = index,
                        eintrag = eintrag,
                        anzahl = konfiguration.einträge.size,
                        fehler = ergebnis?.elementFehler?.get(eintrag.id),
                        ändern = { neu ->
                            speichern(konfiguration.einträge.map { if (it.id == eintrag.id) neu else it })
                        },
                        entfernen = { speichern(konfiguration.einträge.filterNot { it.id == eintrag.id }) },
                        einfügen = { darunter ->
                            val ziel = index + if (darunter) 1 else 0
                            val neu = konfiguration.einträge.toMutableList().apply {
                                add(ziel, neuerEndlicheMengeEintrag())
                            }
                            speichern(neu)
                        },
                        verschieben = { richtung ->
                            val ziel = (index + richtung).coerceIn(0, konfiguration.einträge.lastIndex)
                            if (ziel != index) {
                                val neu = konfiguration.einträge.toMutableList()
                                val wert = neu.removeAt(index)
                                neu.add(ziel, wert)
                                speichern(neu)
                            }
                        },
                    )
                }
            }
        }

        val artName = konfiguration.gemeinsameArt
            ?.let(::AnschlussArtId)
            ?.let { id -> MathematikAnschlussArten.alle.firstOrNull { it.id == id }?.name ?: id.wert }
            ?: "unbestimmt"
        Text("Gemeinsamer Elementtyp: $artName", style = MaterialTheme.typography.bodySmall)

        (listOfNotNull(lokaleWarnung) + ergebnis?.warnungen.orEmpty()).distinct().forEach { warnung ->
            Text(warnung, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }
        ergebnis?.fehler?.takeIf { ergebnis.elementFehler.isEmpty() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ElementKarte(
        index: Int,
        eintrag: EndlicheMengeEintrag,
        anzahl: Int,
        fehler: String?,
        ändern: (EndlicheMengeEintrag) -> Unit,
        entfernen: () -> Unit,
        einfügen: (darunter: Boolean) -> Unit,
        verschieben: (richtung: Int) -> Unit,
    ) {
        var typMenü by remember(eintrag.id) { mutableStateOf(false) }
        var einfügeMenü by remember(eintrag.id) { mutableStateOf(false) }
        var schleppweg by remember(eintrag.id) { mutableFloatStateOf(0f) }
        val schwelle = with(LocalDensity.current) { 36.dp.toPx() }
        val art = MathematikAnschlussArten.alle.firstOrNull { it.id.wert == eintrag.art }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).pointerInput(eintrag.id, index, anzahl) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { schleppweg = 0f },
                                onDragCancel = { schleppweg = 0f },
                                onDragEnd = { schleppweg = 0f },
                                onDrag = { änderung, delta ->
                                    änderung.consume()
                                    schleppweg += delta.y
                                    when {
                                        schleppweg > schwelle && index < anzahl - 1 -> {
                                            verschieben(1)
                                            schleppweg = 0f
                                        }
                                        schleppweg < -schwelle && index > 0 -> {
                                            verschieben(-1)
                                            schleppweg = 0f
                                        }
                                    }
                                },
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text("↕", style = MaterialTheme.typography.titleMedium) }
                    Text("Element ${index + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = entfernen) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Box {
                        IconButton(onClick = { einfügeMenü = true }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        DropdownMenu(expanded = einfügeMenü, onDismissRequest = { einfügeMenü = false }) {
                            DropdownMenuItem(
                                text = { Text("↑+ Darüber") },
                                onClick = { einfügeMenü = false; einfügen(false) },
                            )
                            DropdownMenuItem(
                                text = { Text("↓+ Darunter") },
                                onClick = { einfügeMenü = false; einfügen(true) },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = typMenü, onExpandedChange = { typMenü = it }) {
                    OutlinedTextField(
                        value = art?.name ?: eintrag.art,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typMenü) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = typMenü, onDismissRequest = { typMenü = false }) {
                        MathematikAnschlussArten.alle.forEach { neueArt ->
                            DropdownMenuItem(
                                text = { Text(neueArt.name) },
                                onClick = {
                                    typMenü = false
                                    ändern(wechsleTyp(eintrag, neueArt.id))
                                },
                            )
                        }
                    }
                }

                when (val quelle = eintrag.quelle) {
                    is EndlicheMengeQuelle.ZahlLiteral -> ZahlEditor(quelle, fehler) {
                        ändern(eintrag.copy(quelle = EndlicheMengeQuelle.ZahlLiteral(it)))
                    }
                    is EndlicheMengeQuelle.TupelLiteral -> TupelEditor(quelle, fehler) {
                        ändern(eintrag.copy(quelle = it))
                    }
                    is EndlicheMengeQuelle.Konstante -> KonstantenEditor(eintrag, quelle, fehler) {
                        ändern(eintrag.copy(quelle = EndlicheMengeQuelle.Konstante(it)))
                    }
                }
                fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    @Composable
    private fun ZahlEditor(
        quelle: EndlicheMengeQuelle.ZahlLiteral,
        fehler: String?,
        ändern: (String) -> Unit,
    ) {
        OutlinedTextField(
            value = quelle.wert,
            onValueChange = ändern,
            label = { Text("Zahl") },
            supportingText = { Text("Ganzzahl, Bruch, Dezimalzahl, π, e oder komplex, z. B. 2-3i") },
            isError = fehler != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun TupelEditor(
        quelle: EndlicheMengeQuelle.TupelLiteral,
        fehler: String?,
        ändern: (EndlicheMengeQuelle.TupelLiteral) -> Unit,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Dimension", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            IconButton(
                onClick = { ändern(quelle.copy(werte = quelle.werte.dropLast(1))) },
                enabled = quelle.werte.size > 1,
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(quelle.werte.size.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { ändern(quelle.copy(werte = quelle.werte + "")) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
        quelle.werte.forEachIndexed { index, wert ->
            OutlinedTextField(
                value = wert,
                onValueChange = { neu ->
                    ändern(quelle.copy(werte = quelle.werte.mapIndexed { i, alt -> if (i == index) neu else alt }))
                },
                label = { Text("Komponente ${index + 1}") },
                isError = fehler?.startsWith("Tupelkomponente ${index + 1}") == true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun KonstantenEditor(
        eintrag: EndlicheMengeEintrag,
        quelle: EndlicheMengeQuelle.Konstante,
        fehler: String?,
        ändern: (String) -> Unit,
    ) {
        var geöffnet by remember(eintrag.id) { mutableStateOf(false) }
        val ausgewählteArt = AnschlussArtId(eintrag.art)
        val optionen = remember(eintrag.art) {
            EndlicheMengeKonstanten.alle.filter { artRegister.istUnterart(it.art, ausgewählteArt) }
        }
        val aktuell = EndlicheMengeKonstanten.finde(quelle.id)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = aktuell?.name ?: "Keine Auswahl",
                onValueChange = {},
                readOnly = true,
                label = { Text("Vordefiniertes oder inputloses Element") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(geöffnet) },
                isError = fehler != null,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                if (optionen.isEmpty()) {
                    DropdownMenuItem(text = { Text("Keine kompatible Konstante vorhanden") }, onClick = {})
                } else optionen.forEach { konstante ->
                    DropdownMenuItem(
                        text = { Text(konstante.name) },
                        onClick = { geöffnet = false; ändern(konstante.id) },
                    )
                }
            }
        }
    }

    private fun wechsleTyp(eintrag: EndlicheMengeEintrag, neueArt: AnschlussArtId): EndlicheMengeEintrag {
        val quelle = when {
            neueArt == MathematikAnschlussArten.Zahl.id -> when (val bisher = eintrag.quelle) {
                is EndlicheMengeQuelle.ZahlLiteral -> bisher
                is EndlicheMengeQuelle.TupelLiteral ->
                    EndlicheMengeQuelle.ZahlLiteral(bisher.werte.singleOrNull().orEmpty())
                is EndlicheMengeQuelle.Konstante -> EndlicheMengeQuelle.ZahlLiteral("")
            }
            neueArt == MathematikAnschlussArten.Tupel.id -> when (val bisher = eintrag.quelle) {
                is EndlicheMengeQuelle.ZahlLiteral -> EndlicheMengeQuelle.TupelLiteral(listOf(bisher.wert))
                is EndlicheMengeQuelle.TupelLiteral -> bisher
                is EndlicheMengeQuelle.Konstante -> EndlicheMengeQuelle.TupelLiteral(listOf(""))
            }
            else -> {
                val bisher = (eintrag.quelle as? EndlicheMengeQuelle.Konstante)
                val bisherigeKonstante = bisher?.let { EndlicheMengeKonstanten.finde(it.id) }
                when {
                    bisherigeKonstante != null && artRegister.istUnterart(bisherigeKonstante.art, neueArt) -> bisher
                    else -> EndlicheMengeQuelle.Konstante(
                        EndlicheMengeKonstanten.alle
                            .firstOrNull { artRegister.istUnterart(it.art, neueArt) }
                            ?.id
                            .orEmpty(),
                    )
                }
            }
        }
        return eintrag.copy(art = neueArt.wert, quelle = quelle)
    }
}
