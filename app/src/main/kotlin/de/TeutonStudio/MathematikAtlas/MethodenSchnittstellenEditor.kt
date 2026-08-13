package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.MathematikRechenSystem.kern.MethodenSignatur
import de.TeutonStudio.MathematikRechenSystem.kern.typAusdruck
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck

internal object AtlasAbstände {
    val SehrKlein = 4.dp
    val Klein = 6.dp
    val Steuerung = 8.dp
    val Inhalt = 12.dp
    val RahmenInnen = 16.dp
    val DialogInnen = 20.dp
}

@Composable
internal fun MethodenAufrufArgumentProjektionEditor(
    knoten: KnotenDaten,
    zustand: AtlasZustand,
) {
    if (knoten.art != METHODEN_AUFRUF_ART) return
    val stelligkeit = knoten.parameter[METHODEN_AUFRUF_STELLIGKEIT]?.toIntOrNull()
    val aktuell = knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]
        ?.takeIf { it == METHODEN_ARGUMENTPROJEKTION_TUPEL || it == METHODEN_ARGUMENTPROJEKTION_SEPARIERT }
        ?: METHODEN_ARGUMENTPROJEKTION_TUPEL
    var ausstehend by remember(knoten.id) { mutableStateOf<String?>(null) }

    HorizontalDivider()
    Text("Argumentdarstellung", style = MaterialTheme.typography.titleSmall)
    Text(
        "Die Methode erhält semantisch weiterhin genau ein geordnetes Argumenttupel. Die Einstellung verändert nur die sichtbare Pack-/Projektionsdarstellung dieses Aufrufknotens.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (stelligkeit == null) {
        Text(
            "Separierte Anschlüsse werden erst angeboten, wenn eine Methodensignatur bekannt ist.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilterChip(
            selected = true,
            onClick = {},
            enabled = false,
            label = { Text("Ein Tupel") },
        )
        return
    }

    fun verbundenesArgumentVorhanden(): Boolean {
        val argumentIds = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
            .mapTo(mutableSetOf()) { it.id }
        return zustand.editor.karte.verbindungen.any { verbindung ->
            verbindung.zu.knotenId == knoten.id && verbindung.zu.anschlussId in argumentIds
        }
    }

    fun anfordern(projektion: String) {
        if (projektion == aktuell) return
        if (verbundenesArgumentVorhanden()) ausstehend = projektion
        else zustand.editor.führeAus(
            KartenAktion.KnotenParameterÄndern(knoten.id, METHODEN_AUFRUF_ARGUMENTPROJEKTION, projektion),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AtlasAbstände.Klein),
    ) {
        FilterChip(
            selected = aktuell == METHODEN_ARGUMENTPROJEKTION_TUPEL,
            onClick = { anfordern(METHODEN_ARGUMENTPROJEKTION_TUPEL) },
            label = { Text("Ein Tupel") },
        )
        FilterChip(
            selected = aktuell == METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
            onClick = { anfordern(METHODEN_ARGUMENTPROJEKTION_SEPARIERT) },
            enabled = stelligkeit > 0,
            label = { Text("Separierte Anschlüsse") },
        )
    }
    Text(
        when (stelligkeit) {
            0 -> "Die Methode ist nullstellig und besitzt daher in beiden Darstellungen keine Argumentanschlüsse."
            1 -> "Bekannt ist ein Argument. Auch dieses bleibt semantisch ein Einertupel."
            else -> "Bekannt sind $stelligkeit geordnete Argumentkomponenten."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ausstehend?.let { projektion ->
        AlertDialog(
            onDismissRequest = { ausstehend = null },
            title = { Text("Argumentdarstellung wechseln?") },
            text = {
                Text(
                    "Am Aufruf sind bereits Argumente verbunden. Beim Wechsel werden nur Verbindungen erhalten, die nach der neuen Tupelprojektion weiterhin typ- und anschlusskompatibel sind.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ausstehend = null
                    zustand.editor.führeAus(
                        KartenAktion.KnotenParameterÄndern(
                            knoten.id,
                            METHODEN_AUFRUF_ARGUMENTPROJEKTION,
                            projektion,
                        ),
                    )
                }) { Text("Wechseln") }
            },
            dismissButton = { TextButton(onClick = { ausstehend = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
internal fun KarteneingangMethodenSignaturEditor(
    knoten: KnotenDaten,
    zustand: AtlasZustand,
) {
    if (knoten.art != KARTEN_EINGANG_ART) return
    val methodenAusgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.art == MathematikAnschlussArten.Methode.id
    } ?: return

    val aktiv = knoten.parameter[KARTEN_METHODEN_SIGNATUR_AKTIV] == "true"
    val anzahl = knoten.parameter[KARTEN_METHODEN_ARGUMENT_ANZAHL]
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 1
    val signatur = deklarierteMethodenSignatur(knoten)
    val erwarteterVertrag = methodenVertrag(signatur)

    LaunchedEffect(knoten.id, methodenAusgang.id, erwarteterVertrag) {
        if (methodenAusgang.vertrag != erwarteterVertrag) {
            val anschlüsse = knoten.anschlüsse.map { anschluss ->
                if (anschluss.id == methodenAusgang.id) anschluss.copy(vertrag = erwarteterVertrag) else anschluss
            }
            zustand.editor.führeAus(
                KartenAktion.KnotenKonfigurationErsetzen(
                    id = knoten.id,
                    parameter = knoten.parameter,
                    anschlüsse = anschlüsse,
                ),
            )
        }
    }

    fun setze(schlüssel: String, wert: String) {
        val parameter = knoten.parameter + (schlüssel to wert)
        val probe = knoten.copy(parameter = parameter)
        val vertrag = methodenVertrag(deklarierteMethodenSignatur(probe))
        val anschlüsse = knoten.anschlüsse.map { anschluss ->
            if (anschluss.id == methodenAusgang.id) anschluss.copy(vertrag = vertrag) else anschluss
        }
        zustand.editor.führeAus(
            KartenAktion.KnotenKonfigurationErsetzen(
                id = knoten.id,
                parameter = parameter,
                anschlüsse = anschlüsse,
            ),
        )
    }

    HorizontalDivider()
    Text("Methodentyp", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AtlasAbstände.Steuerung),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Signatur detailliert definieren", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Ohne Deklaration bleibt die Methode bewusst unbestimmt. Der Atlas ergänzt dann weder Stelligkeit noch ℝ als Wertevorrat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = aktiv,
            onCheckedChange = { setze(KARTEN_METHODEN_SIGNATUR_AKTIV, it.toString()) },
        )
    }

    if (!aktiv) return

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(AtlasAbstände.Inhalt),
            verticalArrangement = Arrangement.spacedBy(AtlasAbstände.Steuerung),
        ) {
            Text("Argumenttupel", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AtlasAbstände.Steuerung),
            ) {
                Text("Komponenten", modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { setze(KARTEN_METHODEN_ARGUMENT_ANZAHL, (anzahl - 1).coerceAtLeast(0).toString()) },
                    enabled = anzahl > 0,
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(anzahl.toString(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { setze(KARTEN_METHODEN_ARGUMENT_ANZAHL, (anzahl + 1).toString()) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (anzahl == 0) {
                Text("Leeres Argumenttupel ().", style = MaterialTheme.typography.bodySmall)
            }
            repeat(anzahl) { index ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(AtlasAbstände.Inhalt),
                        verticalArrangement = Arrangement.spacedBy(AtlasAbstände.Steuerung),
                    ) {
                        Text("Argument ${index + 1}", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = knoten.parameter[kartenMethodenArgumentNameSchlüssel(index)] ?: "x${index + 1}",
                            onValueChange = { setze(kartenMethodenArgumentNameSchlüssel(index), it) },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = knoten.parameter[kartenMethodenArgumentWerteVorratSchlüssel(index)].orEmpty(),
                            onValueChange = { setze(kartenMethodenArgumentWerteVorratSchlüssel(index), it) },
                            label = { Text("Wertevorrat") },
                            supportingText = { Text("z. B. R, C, Z, N oder ein benannter Mengen-Ausdruck") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(AtlasAbstände.Inhalt),
            verticalArrangement = Arrangement.spacedBy(AtlasAbstände.Steuerung),
        ) {
            Text("Zielmenge", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = knoten.parameter[KARTEN_METHODEN_ZIELMENGE].orEmpty(),
                onValueChange = { setze(KARTEN_METHODEN_ZIELMENGE, it) },
                label = { Text("Zielmenge") },
                supportingText = { Text("Leer bedeutet: vorhanden, aber typologisch noch unbekannt.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    signatur?.let { methode ->
        val argumentText = if (methode.argumente.isEmpty()) "()" else methode.argumente.joinToString(
            prefix = "(",
            postfix = ")",
        ) { argument -> "${argument.parameter.name}∈${argument.werteVorrat.zuLatex()}" }
        Text(
            "$argumentText → ${methode.zielMenge.zuLatex()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun methodenVertrag(signatur: MethodenSignatur?): AnschlussVertrag = AnschlussVertrag(
    typ = signatur?.typAusdruck() ?: TypAusdruck.Atom(MathematischeTypen.Methode),
)
