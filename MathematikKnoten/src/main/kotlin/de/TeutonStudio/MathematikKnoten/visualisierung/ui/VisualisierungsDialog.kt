package de.TeutonStudio.MathematikKnoten.visualisierung.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck

class VisualisierungsFeldEntwürfe {
    val texte = mutableStateMapOf<String, String>()
    val modelle = mutableStateMapOf<String, String>()
    val fehler = mutableStateMapOf<String, String>()
    val geändert get() = fehler.isNotEmpty() || texte.any { (k, v) -> modelle[k] != v }
    fun gültig(c: VisualisierungsKonfiguration, dimension: RaumDimension): Boolean = fehler.keys.none { label ->
        when (label) {
            "Z-Achse", "Z-Bereich", "Volumenauflösung" -> dimension == RaumDimension.R3
            "Y-Achse", "Y-Bereich", "Flächenauflösung" -> dimension != RaumDimension.R1
            "Farbvariable", "Farbwertbereich" -> c.farbe.modus == FarbModus.Spektrum
            else -> true
        }
    }
    fun zurücksetzen() { texte.clear(); modelle.clear(); fehler.clear() }
}

/** Entwurf bleibt von laufenden Kartenänderungen getrennt. Vergleich nur eigener Konfigurationsfelder. */
class VisualisierungsEntwurf(eigenschaften: Map<String, KnotenEigenschaft>) {
    var basis by mutableStateOf(VisualisierungsKonfiguration.aus(eigenschaften)); private set
    var konfiguration by mutableStateOf(basis)
    val geändert get() = basis != konfiguration
    fun konflikt(aktuell: Map<String, KnotenEigenschaft>) = VisualisierungsKonfiguration.aus(aktuell) != basis
    fun neuLaden(aktuell: Map<String, KnotenEigenschaft>) { basis = VisualisierungsKonfiguration.aus(aktuell); konfiguration = basis }
    fun übernehmen(aktuell: Map<String, KnotenEigenschaft>): Map<String, KnotenEigenschaft> {
        check(!konflikt(aktuell)) { "Die Einstellungen wurden außerhalb des Dialogs verändert." }
        return konfiguration.inEigenschaften(aktuell)
    }
}

@Composable
fun VisualisierungsDialog(knoten: KnotenDaten?, auswertung: KnotenAuswertungsErgebnis?,
    übernehmen: (Map<String, KnotenEigenschaft>) -> Unit, schließen: () -> Unit,
    definitionÖffnen: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val entwurf = remember { VisualisierungsEntwurf(knoten?.eigenschaften.orEmpty()) }
    val felder = remember { VisualisierungsFeldEntwürfe() }
    var verwerfen by remember { mutableStateOf(false) }
    var definitionNachAbbruch by remember { mutableStateOf(false) }
    var einstellungenOffen by remember { mutableStateOf(false) }
    val konflikt = knoten != null && entwurf.konflikt(knoten.eigenschaften)
    fun abbrechen(definition: Boolean = false) {
        if (entwurf.geändert || felder.geändert) { definitionNachAbbruch = definition; verwerfen = true }
        else if (definition) definitionÖffnen?.invoke() else schließen()
    }
    AtlasDialog(onDismissRequest = { abbrechen() }, modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            AtlasDialogKopf(titel = { Text("Visualisierung", style = MaterialTheme.typography.titleLarge) }, untertitel = { Text("Ansicht und Einstellungen werden erst mit Übernehmen gespeichert.") },
                aktionen = { if (definitionÖffnen != null) TextButton(onClick = { abbrechen(true) }) { Text("Definition ansehen") } })
            if (knoten == null) Text("Der Knoten wurde gelöscht.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            if (konflikt) Row {
                Text("Einstellungen wurden außerhalb des Dialogs geändert.", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { entwurf.neuLaden(checkNotNull(knoten).eigenschaften); felder.zurücksetzen() }) { Text("Neu laden") }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(AtlasDialogAbstände.inhalt)) {
                val maximaleEinstellHöhe = maxHeight * .45f
                val menge = auswertung?.eingänge?.get("menge")?.objekt as? MengenAusdruck
                val c = entwurf.konfiguration.mitWirksamerDimension(menge)
                val ansicht: @Composable (Modifier) -> Unit = { modifier ->
                    if (knoten != null) VisualisierungsDetailansicht(knoten.copy(eigenschaften = entwurf.konfiguration.inEigenschaften(knoten.eigenschaften)), auswertung,
                        { entwurf.konfiguration = VisualisierungsKonfiguration.aus(it) }, modifier)
                }
                val einstellungen: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder)) {
                        VisualisierungsEinstellungen(entwurf.konfiguration, c.dimension, { entwurf.konfiguration = it }, felder = felder, ebenenNamen = knoten?.anschlüsse.orEmpty().filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.map { it.name })
                        auswertung?.eingänge?.get("menge")?.annahmen?.takeIf { it.isNotEmpty() }?.let { annahmen ->
                            LatexText("Annahmen: " + annahmen.joinToString(", ") { it.zuLatex() }) }
                        Text("Ebenenfarbe: enthalten · Orange schraffiert: gemischt · Grau schraffiert: unbestimmt. Die Legende kennzeichnet Näherungen. Gestrichelte Grenzen sind offen.", style = MaterialTheme.typography.bodySmall)
                        Text("R³: Ziehen rotiert; zwei Finger oder Umschalt-Ziehen verschieben. Pinch/Mausrad zoomt. Tippen zeigt Koordinaten.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (maxWidth >= 760.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ansicht(Modifier.weight(1f).fillMaxHeight())
                    einstellungen(Modifier.width(300.dp).fillMaxHeight())
                } else Column(Modifier.fillMaxSize()) {
                    ansicht(Modifier.weight(1f).fillMaxWidth())
                    TextButton(onClick = { einstellungenOffen = !einstellungenOffen }) { Text(if (einstellungenOffen) "Einstellungen schließen" else "Einstellungen öffnen") }
                    if (einstellungenOffen) einstellungen(Modifier.fillMaxWidth().heightIn(max = maximaleEinstellHöhe))
                }
            }
            AtlasDialogAktionen {
                TextButton(onClick = { abbrechen() }) { Text("Abbrechen") }
                Button(enabled = knoten != null && !konflikt && felder.gültig(entwurf.konfiguration, entwurf.konfiguration.mitWirksamerDimension(auswertung?.eingänge?.get("menge")?.objekt as? MengenAusdruck).dimension), onClick = {
                    if (knoten != null) { if (entwurf.geändert) übernehmen(entwurf.übernehmen(knoten.eigenschaften)); schließen() }
                }) { Text("Übernehmen") }
            }
        }
    }
    if (verwerfen) AtlasBestätigungsDialog(onDismissRequest = { verwerfen = false }, title = { Text("Änderungen verwerfen?") },
        text = { Text("Die Ansicht und Einstellungen wurden noch nicht übernommen.") },
        confirmButton = { TextButton(onClick = { verwerfen = false; if (definitionNachAbbruch) definitionÖffnen?.invoke() else schließen() }) { Text("Verwerfen") } },
        dismissButton = { TextButton(onClick = { verwerfen = false }) { Text("Weiter bearbeiten") } })
}

/** Gemeinsame Felder für Dialog und Inspector. Unvollständiger Text verändert die Konfiguration nicht. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualisierungsEinstellungen(c: VisualisierungsKonfiguration, dimension: RaumDimension,
    ändern: (VisualisierungsKonfiguration) -> Unit, gültigkeit: (Boolean) -> Unit = {},
    felder: VisualisierungsFeldEntwürfe = remember { VisualisierungsFeldEntwürfe() },
    ebenenNamen: List<String> = listOf("menge")) {
    val fehler = felder.fehler
    SideEffect { gültigkeit(felder.gültig(c, dimension)) }
    @Composable fun feld(label: String, wert: String, setzen: (String) -> Unit) {
        val text = felder.texte[label] ?: wert
        SideEffect {
            if (label !in felder.texte) felder.texte[label] = wert
            if (felder.modelle[label] != wert && felder.texte[label] == felder.modelle[label]) felder.texte[label] = wert
            felder.modelle[label] = wert
        }
        OutlinedTextField(text, { felder.texte[label] = it; runCatching { setzen(it) }.onSuccess { fehler.remove(label) }.onFailure { f -> fehler[label] = f.message ?: "Ungültige Eingabe" } },
            label = { Text(label) }, isError = label in fehler, supportingText = fehler[label]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
    }
    @Composable fun bereich(label: String, b: ZahlenBereich, setzen: (ZahlenBereich) -> Unit) {
        feld(label, "${b.minimum}, ${b.maximum}") { text ->
            val zahlen = text.split(',').map { it.trim().toDoubleOrNull() ?: error("Zwei endliche Grenzen angeben.") }
            require(zahlen.size == 2) { "Zwei Grenzen mit Komma trennen." }; setzen(ZahlenBereich(zahlen[0], zahlen[1]))
        }
    }
    Text("Mengen", style = MaterialTheme.typography.titleSmall)
    ebenenNamen.forEachIndexed { index, name ->
        val stil = c.ebenenStil(name, index)
        fun setze(s: EbenenStil) = ändern(c.copy(ebenen = c.ebenen + (name to s)))
        Text(stil.name.ifBlank { "Menge ${index + 1}" }, style = MaterialTheme.typography.titleSmall)
        Row { Checkbox(stil.sichtbar, { setze(stil.copy(sichtbar = it)) }); Text("Sichtbar") }
        feld("Legende · $name", stil.name) { setze(stil.copy(name = it)) }
        feld("Farbe #RRGGBB · $name", "%06X".format((stil.farbe ?: c.farbe.festeFarbe ?: 0xFF2563EB) and 0xFFFFFF)) {
            require(it.removePrefix("#").matches(Regex("[0-9a-fA-F]{6}"))) { "Sechs hexadezimale Stellen erforderlich." }
            setze(stil.copy(farbe = 0xFF000000 or it.removePrefix("#").toLong(16)))
        }
        feld("Deckkraft 0–100 % · $name", (stil.deckkraft * 100).toInt().toString()) {
            val n = it.toDoubleOrNull(); require(n != null && n.isFinite() && n in 0.0..100.0) { "Deckkraft zwischen 0 und 100 angeben." }
            setze(stil.copy(deckkraft = n / 100))
        }
        FlowRow { FüllArt.entries.forEach { art -> FilterChip(stil.füllArt == art, { setze(stil.copy(füllArt = art)) }, label = { Text(art.name) }) } }
    }
    Text("Darstellung", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(c.dimensionsModus == DimensionsModus.Automatisch, { ändern(c.copy(dimensionsModus = DimensionsModus.Automatisch)) }, label = { Text("Auto") })
        RaumDimension.entries.forEach { d -> FilterChip(c.dimensionsModus == DimensionsModus.Manuell && c.dimension == d,
            { ändern(c.copy(dimension = d, dimensionsModus = DimensionsModus.Manuell, kamera = standardKamera(d),
                achsen = if (d == RaumDimension.C) AchsenZuordnung("re", "im", null) else c.achsen)) }, label = { Text(d.name) }) }
    }
    FlowRow { MethodenDarstellungsModus.entries.forEach { m -> FilterChip(c.methodenModus == m, { ändern(c.copy(methodenModus = m)) }, label = { Text(when(m) {
        MethodenDarstellungsModus.Automatisch -> "Auto"; MethodenDarstellungsModus.Bild -> "Bild"; MethodenDarstellungsModus.Funktionsgraph -> "Graph"; MethodenDarstellungsModus.Koordinatenausgabe -> "Koordinaten"
    }) }) } }
    feld("Ausgabeprojektion", c.ausgabeKoordinaten.joinToString(", ")) { ändern(c.copy(ausgabeKoordinaten = parseAusgabeKoordinaten(it))) }
    feld("Feste Parameter: Name=Wert", c.festeSchnitte.entries.joinToString("; ") { "${it.key}=${it.value}" }) { ändern(c.copy(festeSchnitte = parseFesteSchnitte(it))) }
    Text("Achsen und Parameterbereiche", style = MaterialTheme.typography.titleSmall)
    feld("X-Achse", c.achsen.x) { require(it.isNotBlank()) { "Achsenname erforderlich." }; ändern(c.copy(achsen = c.achsen.copy(x = it.trim()))) }
    bereich("X-Bereich", c.bereiche.x) { ändern(c.copy(bereiche = c.bereiche.copy(x = it))) }
    if (dimension != RaumDimension.R1) {
        feld("Y-Achse", c.achsen.y) { require(it.isNotBlank()) { "Achsenname erforderlich." }; ändern(c.copy(achsen = c.achsen.copy(y = it.trim()))) }
        bereich("Y-Bereich", c.bereiche.y) { ändern(c.copy(bereiche = c.bereiche.copy(y = it))) }
    }
    if (dimension == RaumDimension.R3) {
        feld("Z-Achse", c.achsen.z.orEmpty()) { require(it.isNotBlank()); ändern(c.copy(achsen = c.achsen.copy(z = it.trim()))) }
        bereich("Z-Bereich", c.bereiche.z ?: ZahlenBereich(-10.0, 10.0)) { ändern(c.copy(bereiche = c.bereiche.copy(z = it))) }
    }
    Row { Checkbox(c.achsenSichtbar, { ändern(c.copy(achsenSichtbar = it)) }); Text("Achsen") }
    Row { Checkbox(c.rasterSichtbar, { ändern(c.copy(rasterSichtbar = it)) }); Text("Raster") }
    Row { Checkbox(c.sampling.fensterBegrenztePrädikatsMengen, { ändern(c.copy(sampling = c.sampling.copy(fensterBegrenztePrädikatsMengen = it))) }); Text("Prädikatsmengen im Sichtfenster") }
    Text("Qualität", style = MaterialTheme.typography.titleSmall)
    FlowRow { DarstellungsQualität.entries.forEach { q -> FilterChip(c.qualität == q, {
        ändern(c.copy(qualität = q, sampling = c.sampling.copy(maximalesRasterBudget = if (q == DarstellungsQualität.Hoch) HOHES_VISUALISIERUNGS_DETAIL_BUDGET else MAXIMALES_VISUALISIERUNGS_DETAIL_BUDGET,
            auflösung1D = if(q == DarstellungsQualität.Hoch) 2000 else 160,
            auflösung2D = if(q == DarstellungsQualität.Hoch) 240 else 72,
            auflösung3D = if(q == DarstellungsQualität.Hoch) 40 else 22)))
    }, label = { Text(q.name) }) } }
    fun zahl(text: String, min: Int, max: Int): Int = (text.toIntOrNull() ?: error("Ganze Zahl erforderlich.")).also { require(it in min..max) { "Wert zwischen $min und $max erforderlich." } }
    feld("Maximale Auswertungen", c.sampling.maximalesRasterBudget.toString()) { ändern(c.copy(sampling = c.sampling.copy(maximalesRasterBudget = zahl(it, 1_000, c.detailBudget)))) }
    feld("Orbit-Schritte je Prüfpunkt", c.sampling.maximaleOrbitSchritte.toString()) { ändern(c.copy(sampling = c.sampling.copy(maximaleOrbitSchritte = zahl(it, 1, 100_000)))) }
    feld("Kurvenauflösung", c.sampling.auflösung1D.toString()) { ändern(c.copy(sampling = c.sampling.copy(auflösung1D = zahl(it, 16, 2_000)))) }
    if (dimension != RaumDimension.R1) feld("Flächenauflösung", c.sampling.auflösung2D.toString()) { ändern(c.copy(sampling = c.sampling.copy(auflösung2D = zahl(it, 16, 240)))) }
    if (dimension == RaumDimension.R3) feld("Volumenauflösung", c.sampling.auflösung3D.toString()) { ändern(c.copy(sampling = c.sampling.copy(auflösung3D = zahl(it, 8, 64)))) }
    feld("Numerische Grenztoleranz", c.sampling.toleranz.toString()) {
        val n = it.toDoubleOrNull() ?: error("Endliche Zahl erforderlich."); require(n.isFinite() && n in 1e-5..2.0); ändern(c.copy(sampling = c.sampling.copy(toleranz = n))) }
    Text("Farbe", style = MaterialTheme.typography.titleSmall)
    FlowRow { FarbModus.entries.forEach { m -> FilterChip(c.farbe.modus == m, { ändern(c.copy(farbe = c.farbe.copy(modus = m))) }, label = { Text(m.name) }) } }
    if (c.farbe.modus == FarbModus.Spektrum) {
        feld("Farbvariable", c.farbe.variable.orEmpty()) { require(it.isNotBlank()); ändern(c.copy(farbe = c.farbe.copy(variable = it.trim()))) }
        FlowRow { listOf("Ozean", "Sonnenuntergang", "Wald").forEach { p -> FilterChip(c.farbe.palette == p, { ändern(c.copy(farbe = c.farbe.copy(palette = p))) }, label = { Text(p) }) } }
        bereich("Farbwertbereich", c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)) { ändern(c.copy(farbe = c.farbe.copy(bereich = it))) }
    }
}

/** Explizite Umstellung; der Aufrufer verwendet die vorhandene Verlustbestätigung. */
fun konfiguriereVisualisierungsEingänge(knoten: KnotenDaten): KnotenDaten {
    require(knoten.art == "mathematik.visualisierung")
    val eingänge = knoten.anschlüsse.filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
        .map { it.copy(kannSichErweitern = true) }.toMutableList()
    while (eingänge.size < 2) eingänge += de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten(
        name = if (eingänge.isEmpty()) "menge" else "menge2", richtung = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang,
        kante = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Links,
        art = de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten.Menge.id, reihenfolge = eingänge.size, kannSichErweitern = true)
    return knoten.copy(anschlüsse = eingänge)
}

@Composable
fun VisualisierungsAnschlussEinstellungen(knoten: KnotenDaten, ändern: (KnotenDaten) -> Unit) {
    if (knoten.art != "mathematik.visualisierung") return
    if (knoten.anschlüsse.any { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang } ||
        knoten.anschlüsse.count { it.kannSichErweitern } < 2) {
        Text("Die Umstellung entfernt den historischen Ausgang. Betroffene Verbindungen werden vorab angezeigt.")
        OutlinedButton(onClick = { ändern(konfiguriereVisualisierungsEingänge(knoten)) }) { Text("Mehrere Mengen aktivieren") }
    }
}
