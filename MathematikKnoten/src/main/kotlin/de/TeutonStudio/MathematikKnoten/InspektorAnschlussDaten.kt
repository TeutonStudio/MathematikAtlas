package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl

/** Alle Belegungen, einschließlich durch eine Verbindung übersteuerter Kartenfallbacks. */
data class InspektorAnschlussBeitrag(val schlüssel: String, val position: Int, val titel: String, val wert: BedingterWert?, val hinweis: String? = null)

fun inspektorAnschlussBeiträge(karte: KartenDaten, knoten: KnotenDaten, port: AnschlussDaten,
    ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>): List<InspektorAnschlussBeitrag> {
    val ref = AnschlussVerweis(knoten.id, port.id)
    val eingang = port.richtung == AnschlussRichtung.Eingang
    val ergebnis = ergebnisse[knoten.id]
    val verbindungen = karte.verbindungen.filter { if (eingang) it.zu == ref else it.von == ref }
    val beiträge = verbindungen.map { v ->
        val andere = if (eingang) v.von else v.zu
        val andererKnoten = karte.knoten.firstOrNull { it.id == andere.knotenId }
        val andererPort = karte.findeAnschluss(andere)
        val wert = if (eingang) ergebnisse[andere.knotenId]?.takeIf { it.fehler == null }?.ausgaben?.get(andererPort?.name)
            else ergebnis?.takeIf { it.fehler == null }?.ausgaben?.get(port.name)
        InspektorAnschlussBeitrag(v.id.wert, v.eingangsReihenfolge,
            "${if (eingang) "Quelle" else "Ziel"}: ${andererKnoten?.name ?: "Fehlender Knoten"} · ${andererPort?.name ?: "Fehlender Anschluss"}", wert,
            if (wert == null) "Kein aktueller Wert verfügbar." else null)
    }.toMutableList()
    if (eingang) {
        val mehrfach = port.belegung == AnschlussBelegung.Mehrfach
        if (mehrfach) knoten.parameter.filterKeys { it.startsWith("join.lokal.") }.forEach { (key, text) ->
            val id = key.removePrefix("join.lokal.")
            beiträge += InspektorAnschlussBeitrag("lokal:$id", knoten.parameter["join.position.$id"]?.toIntOrNull() ?: Int.MAX_VALUE,
                "Lokaler Operand: $text", ergebnis?.eingänge?.get(port.name + "@lokal:" + id))
        }
        knoten.eingangsKartenVerweise.filterKeys { it == port.name || mehrfach && it.startsWith(port.name + "@karte:") }.forEach { (name, verweis) ->
            val id = name.substringAfter("@karte:", "standard")
            val übersteuert = if (mehrfach) verbindungen.any { it.eingangsFallback == name } || knoten.parameter.containsKey("join.lokal.$id")
                else verbindungen.isNotEmpty()
            beiträge += InspektorAnschlussBeitrag("karte:$id", knoten.parameter["join.position.karte:$id"]?.toIntOrNull() ?: Int.MAX_VALUE,
                "Kartenfallback: ${verweis.kartenId.wert} · v${verweis.version}",
                if (übersteuert) null else ergebnis?.eingänge?.get(if (mehrfach && "@" !in name) "$name@karte:standard" else name),
                if (übersteuert) "Inaktiv: durch eine Verbindung oder einen lokalen Wert ersetzt." else null)
        }
        if (!mehrfach && verbindungen.isEmpty() && port.name !in knoten.eingangsKartenVerweise) {
            val wert = ergebnis?.eingänge?.get(port.name)
            val text = knoten.parameter[inspektorStandardwertSchlüssel(knoten, port)]
            beiträge += InspektorAnschlussBeitrag("standard", 0,
                if (text != null) "Standardwert: $text" else if (wert != null) "Aktueller Eingangswert" else "Nicht verbunden",
                wert, if (wert == null) "Keine ausgewertete Eingabe verfügbar." else null)
        }
    } else if (verbindungen.isEmpty()) {
        beiträge += InspektorAnschlussBeitrag("ausgabe", 0, "Keine Ziele verbunden",
            ergebnis?.takeIf { it.fehler == null }?.ausgaben?.get(port.name))
    }
    return beiträge.sortedBy { it.position }
}

fun hatInspektorStandardwert(knoten: KnotenDaten, port: AnschlussDaten): Boolean =
    port.richtung == AnschlussRichtung.Eingang && port.belegung != AnschlussBelegung.Mehrfach &&
        (port.art == MathematikAnschlussArten.Aussage.id || MathematikAnschlussArten.Aussage.id in port.zulässigeArten ||
            port.art == MathematikAnschlussArten.Zahl.id || MathematikAnschlussArten.Zahl.id in port.zulässigeArten ||
            knoten.art == "mathematik.tupel" && port.art == MathematikAnschlussArten.Objekt.id)

private fun inspektorStandardwertSchlüssel(knoten: KnotenDaten, port: AnschlussDaten): String =
    if (knoten.art == RICHTUNG_WINKEL_ART && port.name == "n") RICHTUNG_WINKEL_ANZAHL else "standardwert.${port.name}"

fun setzeInspektorStandardwert(knoten: KnotenDaten, port: AnschlussDaten, text: String): KnotenDaten {
    require(knoten.anschlüsse.any { it.id == port.id } && hatInspektorStandardwert(knoten, port))
    val aussage = port.art == MathematikAnschlussArten.Aussage.id || MathematikAnschlussArten.Aussage.id in port.zulässigeArten
    val wert = text.trim()
    if (knoten.art == RICHTUNG_WINKEL_ART && port.name == "n") require(wert.toIntOrNull()?.let { it in 0..31 } == true) {
        "n muss eine ganze Zahl zwischen 0 und 31 sein."
    }
    require(if (aussage) wert.toBooleanStrictOrNull() != null else wert.isEmpty() || runCatching { RationaleZahl.parse(wert) }.isSuccess) {
        if (aussage) "Wahr oder Falsch erforderlich." else "Eine ganze Zahl oder ein Bruch mit Nenner ungleich null ist erforderlich."
    }
    val key = inspektorStandardwertSchlüssel(knoten, port)
    return knoten.copy(parameter = if (wert.isEmpty()) knoten.parameter - key else knoten.parameter + (key to wert))
}

@Composable
fun InspektorAnschlussDaten(karte: KartenDaten, knoten: KnotenDaten, port: AnschlussDaten,
    ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis>, übernehmen: (KnotenDaten) -> Unit) {
    val beiträge = inspektorAnschlussBeiträge(karte, knoten, port, ergebnisse)
    Text("Typ: ${MathematikAnschlussArten.alle.firstOrNull { it.id == port.art }?.name ?: port.art.wert}")
    if (port.zulässigeArten.isNotEmpty()) Text("Zulässig: " + port.zulässigeArten.joinToString { art ->
        MathematikAnschlussArten.alle.firstOrNull { it.id == art }?.name ?: art.wert })
    if (port.belegung == AnschlussBelegung.Mehrfach) Text("Mehrfacheingang · ${beiträge.size} Beitrag/Beiträge")
    beiträge.forEach { beitrag -> key(beitrag.schlüssel) {
        Text(beitrag.titel, style = MaterialTheme.typography.labelLarge)
        beitrag.wert?.let { wert ->
            LatexText(wert.anzeigeLatex())
            wert.annahmen.forEach { LatexText(it.zuLatex()) }
        }
        beitrag.hinweis?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    } }
    if (port.richtung == AnschlussRichtung.Ausgang && port.art == MathematikAnschlussArten.Methode.id)
        MethodenProjektionsEinstellungen(knoten, ergebnisse[knoten.id], übernehmen, port.id)
    if (hatInspektorStandardwert(knoten, port)) {
        val verbunden = karte.verbindungen.any { it.zu == AnschlussVerweis(knoten.id, port.id) }
        val aussage = port.art == MathematikAnschlussArten.Aussage.id || MathematikAnschlussArten.Aussage.id in port.zulässigeArten
        if (aussage) {
            val wert = knoten.parameter["standardwert.${port.name}"] == "true"
            Row { Switch(checked = wert, enabled = !verbunden,
                onCheckedChange = { übernehmen(setzeInspektorStandardwert(knoten, port, it.toString())) }); Text("Standardwert: ${if (wert) "Wahr" else "Falsch"}") }
        } else {
            val wert = knoten.parameter[inspektorStandardwertSchlüssel(knoten, port)].orEmpty()
            var text by remember(knoten.id, port.id, wert) { mutableStateOf(wert) }
            val kandidat = runCatching { setzeInspektorStandardwert(knoten, port, text) }
            OutlinedTextField(text, { text = it }, label = { Text("Standardwert: ${port.name}") }, enabled = !verbunden,
                isError = kandidat.isFailure, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(kandidat.exceptionOrNull()?.message ?: "Ganze Zahl oder Bruch; leer entfernt den Standardwert.") })
            TextButton(enabled = !verbunden && kandidat.isSuccess && kandidat.getOrNull() != knoten,
                onClick = { kandidat.getOrNull()?.let(übernehmen) }) { Text("Standardwert übernehmen") }
        }
        if (verbunden) Text("Inaktiv: Die Verbindung bestimmt den Eingangswert.", style = MaterialTheme.typography.bodySmall)
    }
    if (port.belegung == AnschlussBelegung.Mehrfach && port.richtung == AnschlussRichtung.Eingang)
        JoinOperandenEditor(knoten, karte.verbindungen.count { it.zu == AnschlussVerweis(knoten.id, port.id) }, übernehmen)
}
