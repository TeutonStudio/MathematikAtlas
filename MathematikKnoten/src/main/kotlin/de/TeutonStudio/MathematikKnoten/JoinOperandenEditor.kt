package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import java.util.UUID

/** Gemeinsamer Inspector: gültige lokale Beiträge werden atomar als Knotendaten übernommen. */
@Composable
fun JoinOperandenEditor(knoten: KnotenDaten, verbindungen: Int, übernehmen: (KnotenDaten) -> Unit) {
    val port = knoten.anschlüsse.firstOrNull { it.belegung == AnschlussBelegung.Mehrfach } ?: return
    Text("Operanden", style = MaterialTheme.typography.titleSmall)
    Text("$verbindungen verbundene Quellen", style = MaterialTheme.typography.bodySmall)
    val aussage = port.art == MathematikAnschlussArten.Aussage.id || MathematikAnschlussArten.Aussage.id in port.zulässigeArten
    val zahlen = port.art == MathematikAnschlussArten.Zahl.id || MathematikAnschlussArten.Zahl.id in port.zulässigeArten
    if (!aussage && !zahlen) return
    fun gültig(text: String) = if (aussage) text.toBooleanStrictOrNull() != null
        else runCatching { RationaleZahl.parse(text) }.isSuccess
    val lokale = knoten.parameter.filterKeys { it.startsWith("join.lokal.") }.entries.sortedBy {
        knoten.parameter["join.position." + it.key.removePrefix("join.lokal.")]?.toIntOrNull() ?: Int.MAX_VALUE
    }
    lokale.forEachIndexed { index, (key, wert) ->
        key(key) {
            var entwurf by remember(wert) { mutableStateOf(wert) }
            OutlinedTextField(entwurf, { entwurf = it }, label = { Text("Lokaler Operand ${index + 1}") },
                isError = !gültig(entwurf), modifier = Modifier.fillMaxWidth())
            Row {
                TextButton(enabled = gültig(entwurf) && entwurf != wert, onClick = {
                    übernehmen(knoten.copy(parameter = knoten.parameter + (key to entwurf)))
                }) { Text("Übernehmen") }
                TextButton(onClick = {
                    übernehmen(knoten.copy(parameter = knoten.parameter - key - ("join.position." + key.removePrefix("join.lokal."))))
                }) { Text("Entfernen") }
            }
        }
    }
    var neuerWert by remember(knoten.id) { mutableStateOf("") }
    OutlinedTextField(neuerWert, { neuerWert = it }, label = { Text(if (aussage) "Neuer Operand (true/false)" else "Neuer Operand (Zahl)") },
        isError = neuerWert.isNotEmpty() && !gültig(neuerWert), modifier = Modifier.fillMaxWidth())
    TextButton(enabled = gültig(neuerWert), onClick = {
        val id = UUID.randomUUID().toString()
        val position = maxOf(port.nächsteBeitragsReihenfolge, lokale.maxOfOrNull { knoten.parameter["join.position." + it.key.removePrefix("join.lokal.")]?.toIntOrNull() ?: 0 }?.plus(1) ?: 0)
        übernehmen(knoten.copy(parameter = knoten.parameter + mapOf("join.lokal.$id" to neuerWert, "join.position.$id" to position.toString())))
        neuerWert = ""
    }) { Text("Lokalen Operanden hinzufügen") }
}
