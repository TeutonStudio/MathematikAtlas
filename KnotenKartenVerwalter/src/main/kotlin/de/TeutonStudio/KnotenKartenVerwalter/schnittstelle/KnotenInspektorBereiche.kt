package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

enum class KnotenInspektorBereich { Knotendaten, Eingangsdaten, Ausgangsdaten }

/** Ausschließlich Sitzung, niemals Bestandteil von Karte oder Undo-Historie. */
@Stable
class InspektorKlappZustand {
    private data class Schlüssel(val karte: KartenId, val knoten: KnotenId, val teil: String)
    private val zustände = mutableStateMapOf<Schlüssel, Boolean>()
    fun offen(karte: KartenId, knoten: KnotenId, teil: String, standard: Boolean) =
        zustände[Schlüssel(karte, knoten, teil)] ?: standard
    fun umschalten(karte: KartenId, knoten: KnotenId, teil: String, standard: Boolean) {
        val key = Schlüssel(karte, knoten, teil)
        zustände[key] = !(zustände[key] ?: standard)
    }
}

@Composable
fun InspektorAbschnitt(
    titel: String,
    offen: Boolean,
    umschalten: () -> Unit,
    modifier: Modifier = Modifier,
    hinweis: String? = null,
    zelle: Boolean = false,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = if (zelle) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.small) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = umschalten)
                .semantics { stateDescription = if (offen) "Aufgeklappt" else "Eingeklappt" }
                .padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (offen) "▾" else "▸", Modifier.padding(end = 8.dp).clearAndSetSemantics {})
                Column(Modifier.weight(1f)) {
                    Text(titel, style = if (zelle) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                    hinweis?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (offen) Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = inhalt)
    }
}

/** Gemeinsame Gliederung; Fachoptionen und Werte werden durch Slots geliefert. */
@Composable
fun KnotenInspektorBereiche(
    karte: KartenDaten,
    knoten: KnotenDaten,
    klappzustand: InspektorKlappZustand,
    standarddaten: @Composable () -> Unit,
    knotendaten: (@Composable () -> Unit)?,
    eingangsEinstellungen: (@Composable () -> Unit)? = null,
    ausgangsEinstellungen: (@Composable () -> Unit)? = null,
    anschlussTyp: (AnschlussDaten) -> String = { it.art.wert },
    diagnose: String? = null,
    anschlussInhalt: @Composable (AnschlussDaten) -> Unit,
) {
    @Composable fun abschnitt(titel: String, teil: String, standard: Boolean = true,
        hinweis: String? = null, zelle: Boolean = false, inhalt: @Composable ColumnScope.() -> Unit) {
        InspektorAbschnitt(titel, klappzustand.offen(karte.id, knoten.id, teil, standard),
            { klappzustand.umschalten(karte.id, knoten.id, teil, standard) }, hinweis = hinweis, zelle = zelle, inhalt = inhalt)
    }
    abschnitt("Standarddaten", "standard") { standarddaten() }
    knotendaten?.let { abschnitt("Knotendaten", "knoten", hinweis = diagnose) { it() } }
    listOf(AnschlussRichtung.Eingang to eingangsEinstellungen, AnschlussRichtung.Ausgang to ausgangsEinstellungen).forEach { (richtung, gemeinsam) ->
        val ports = knoten.anschlüsse.filter { it.richtung == richtung }.sortedBy { it.reihenfolge }
        if (ports.isNotEmpty() || gemeinsam != null) {
            val teil = richtung.name
            abschnitt(if (richtung == AnschlussRichtung.Eingang) "Eingangsdaten" else "Ausgangsdaten", teil) {
                gemeinsam?.let { abschnitt("Gemeinsame Einstellungen", "$teil.gemeinsam", false, zelle = true) { it() } }
                ports.forEach { port -> key(port.id) {
                    val anzahl = karte.verbindungen.count { if (richtung == AnschlussRichtung.Eingang)
                        it.zu == AnschlussVerweis(knoten.id, port.id) else it.von == AnschlussVerweis(knoten.id, port.id) }
                    abschnitt(port.name, "port.${port.id.wert}", false,
                        "${anschlussTyp(port)} · $anzahl Verbindung(en)", true) { anschlussInhalt(port) }
                } }
            }
        }
    }
}

@Composable
fun KnotenStandardDatenEditor(
    knoten: KnotenDaten,
    erlaubteFormen: Set<KnotenDarstellungsForm>,
    modifier: Modifier = Modifier,
    namensModifier: Modifier = Modifier,
    übernehmen: (KnotenDaten) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var name by remember(knoten.id, knoten.name) { mutableStateOf(knoten.name) }
        OutlinedTextField(name, { name = it }, label = { Text("Knotenname") }, singleLine = true,
            isError = name.isBlank(), modifier = Modifier.fillMaxWidth().then(namensModifier))
        TextButton(enabled = name.isNotBlank() && name.trim() != knoten.name,
            onClick = { übernehmen(knoten.copy(name = name.trim())) }) { Text("Name übernehmen") }
        listOf(KNOTEN_HINTERGRUND_EIGENSCHAFT to "Hintergrundfarbe", KNOTEN_RAHMEN_EIGENSCHAFT to "Rahmenfarbe").forEach { (schlüssel, titel) ->
            key(schlüssel) {
                val argb = (knoten.eigenschaften[schlüssel] as? KnotenEigenschaft.Farbe)?.argb
                var text by remember(knoten.id, argb) { mutableStateOf(argb?.let { "%06X".format(it and 0xFFFFFF) }.orEmpty()) }
                val kandidat = parseKnotenFarbe(text)
                Text(titel, style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0xFFF5F5F5L, 0xFF202020L, 0xFFEF9A9AL, 0xFFFFCC80L, 0xFFFFF59DL, 0xFFA5D6A7L, 0xFF90CAF9L, 0xFFCE93D8L).forEach { farbe ->
                        Box(Modifier.size(48.dp).background(Color(farbe), MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) { übernehmen(knoten.mitDarstellungsFarbe(schlüssel, farbe)) }
                            .semantics { contentDescription = "$titel #%06X".format(farbe and 0xFFFFFF) })
                    }
                }
                OutlinedTextField(text, { text = it }, label = { Text("$titel · #RRGGBB") },
                    singleLine = true, isError = text.isNotEmpty() && kandidat == null,
                    leadingIcon = kandidat?.let { { Box(Modifier.size(24.dp).background(Color(it))) } },
                    supportingText = { Text("Sechs Hexadezimalziffern; Automatisch verwendet die Themefarbe.") },
                    modifier = Modifier.fillMaxWidth())
                FlowRow {
                    TextButton(enabled = kandidat != null && kandidat != argb,
                        onClick = { kandidat?.let { übernehmen(knoten.mitDarstellungsFarbe(schlüssel, it)) } }) { Text("$titel übernehmen") }
                    TextButton(enabled = argb != null, onClick = { übernehmen(knoten.mitDarstellungsFarbe(schlüssel, null)) }) { Text("$titel automatisch") }
                }
            }
        }
        Text("Form", style = MaterialTheme.typography.titleSmall)
        if (erlaubteFormen.size <= 1) Text("${knoten.darstellungsForm.titel} · durch Knotentyp festgelegt")
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            erlaubteFormen.forEach { form -> FilterChip(selected = knoten.darstellungsForm == form,
                onClick = { übernehmen(knoten.mitDarstellungsForm(form, erlaubteFormen)) }, label = { Text(form.titel) }) }
        }
    }
}
