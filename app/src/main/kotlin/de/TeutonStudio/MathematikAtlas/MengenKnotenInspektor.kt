package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.BEGRIFF_SKALARPRODUKT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_AUSWAHL
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MengenKnotenAuswahl
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_EINGANG
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_STANDARD
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_ZERTIFIZIERT
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_LINEARITAET_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_NACHWEIS_LINEARITAET
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_NACHWEIS_POSITIV
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_NACHWEIS_SYMMETRIE
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_ZAHLBEREICH_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_AUSGANG
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.FundamentalerZahlbereich
import de.TeutonStudio.MathematikRechenSystem.kern.SKALARPRODUKT_ZERTIFIKAT_VERSION
import de.TeutonStudio.MathematikRechenSystem.kern.SkalarproduktLinearitaet
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator

internal object MengenKnotenKartenQuelle {
    private var kartenLieferant: () -> List<KartenDaten> = { emptyList() }
    private var versionenLieferant: (KartenId) -> List<KartenDaten> = { emptyList() }
    private var aktuelleKarteLieferant: () -> KartenId? = { null }
    private var inspektorenRegistriert = false

    fun installieren(zustand: AtlasZustand) {
        kartenLieferant = { zustand.karten }
        versionenLieferant = zustand::kartenVersionen
        aktuelleKarteLieferant = { zustand.editor.karte.id }
        registriereInspektoren()
    }

    @Suppress("UNCHECKED_CAST")
    private fun registriereInspektoren() {
        if (inspektorenRegistriert) return
        synchronized(this) {
            if (inspektorenRegistriert) return
            val feld = KnotenInspektorRegister::class.java.getDeclaredField("inspektoren")
            feld.isAccessible = true
            val register = feld.get(KnotenInspektorRegister) as MutableMap<String, KnotenInspektor>
            register[MENGEN_KNOTEN_ART] = MengenKnotenInspektor
            register[VektorRechner.KNOTEN_ART] = VektorRechnerInspektor
            register[BEGRIFF_SKALARPRODUKT_KNOTEN_ART] = SkalarproduktBegriffInspektor
            inspektorenRegistriert = true
        }
    }

    fun kandidaten(): List<EigeneMengenKarte> {
        val aktuelleId = aktuelleKarteLieferant()
        return kartenLieferant().asSequence()
            .filter { !it.archiviert && it.id != aktuelleId }
            .flatMap { karte -> versionenLieferant(karte.id).asSequence() }
            .mapNotNull(::alsEigeneMengenKarte)
            .distinctBy(EigeneMengenKarte::verweis)
            .sortedWith(compareBy<EigeneMengenKarte> { it.name.lowercase() }.thenByDescending { it.verweis.version })
            .toList()
    }

    fun skalarproduktKandidaten(): List<SkalarproduktZertifikatsKarte> {
        val aktuelleId = aktuelleKarteLieferant()
        return kartenLieferant().asSequence()
            .filter { !it.archiviert && it.id != aktuelleId }
            .flatMap { karte -> versionenLieferant(karte.id).asSequence() }
            .map(::alsSkalarproduktZertifikatsKarte)
            .distinctBy(SkalarproduktZertifikatsKarte::verweis)
            .sortedWith(
                compareBy<SkalarproduktZertifikatsKarte> { it.name.lowercase() }
                    .thenByDescending { it.verweis.version },
            )
            .toList()
    }
}

internal data class EigeneMengenKarte(
    val name: String,
    val verweis: KartenVerweis,
    val ausgangsName: String,
)

internal data class SkalarproduktZertifikatsKarte(
    val name: String,
    val verweis: KartenVerweis,
    val kompatibel: Boolean,
    val grund: String?,
    val zahlbereich: FundamentalerZahlbereich,
    val linearitaet: SkalarproduktLinearitaet,
)

private fun alsEigeneMengenKarte(karte: KartenDaten): EigeneMengenKarte? {
    if (karte.knoten.any { it.art == "mathematik.kartenEingang" }) return null
    val ausgang = karte.knoten.singleOrNull { it.art == "mathematik.kartenAusgang" } ?: return null
    val wertEingang = ausgang.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "wert"
    } ?: return null
    if (wertEingang.art != MathematikAnschlussArten.Menge.id) return null
    val name = ausgang.parameter["name"]?.trim().orEmpty().ifBlank { ausgang.name }
    return EigeneMengenKarte(
        name = karte.name,
        verweis = KartenVerweis(karte.id, karte.version),
        ausgangsName = name,
    )
}

private fun alsSkalarproduktZertifikatsKarte(karte: KartenDaten): SkalarproduktZertifikatsKarte {
    val begriff = karte.knoten.singleOrNull { it.art == BEGRIFF_SKALARPRODUKT_KNOTEN_ART }
    val zahlbereich = FundamentalerZahlbereich.entries.firstOrNull {
        it.id == begriff?.parameter?.get(SKALARPRODUKT_ZAHLBEREICH_PARAMETER) ||
            it.name == begriff?.parameter?.get(SKALARPRODUKT_ZAHLBEREICH_PARAMETER)
    } ?: FundamentalerZahlbereich.REELL
    val linearitaet = SkalarproduktLinearitaet.entries.firstOrNull {
        it.name == begriff?.parameter?.get(SKALARPRODUKT_LINEARITAET_PARAMETER)
    } ?: SkalarproduktLinearitaet.RECHTSLINEAR

    fun kandidat(kompatibel: Boolean, grund: String? = null) = SkalarproduktZertifikatsKarte(
        name = karte.name,
        verweis = KartenVerweis(karte.id, karte.version),
        kompatibel = kompatibel,
        grund = grund,
        zahlbereich = zahlbereich,
        linearitaet = linearitaet,
    )

    if (karte.knoten.any { it.art == "mathematik.kartenEingang" }) {
        return kandidat(false, "Eine Zertifikatskarte darf keine öffentlichen Eingänge besitzen.")
    }
    val ausgaenge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }
    if (ausgaenge.size != 1) {
        return kandidat(false, "Eine Zertifikatskarte benötigt genau einen öffentlichen Aussageausgang.")
    }
    val ausgang = ausgaenge.single()
    val wertEingang = ausgang.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "wert"
    } ?: return kandidat(false, "Der öffentliche Aussageausgang besitzt keinen Werteingang.")
    if (wertEingang.art != MathematikAnschlussArten.Aussage.id) {
        return kandidat(false, "Der öffentliche Ausgang muss vom Typ Aussage sein.")
    }
    if (begriff == null) {
        return kandidat(false, "Der Begriffsknoten „Skalarprodukt überprüfen“ fehlt oder kommt mehrfach vor.")
    }

    val methodenEingang = begriff.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    }
    val aussageAusgang = begriff.anschlüsse.singleOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "aussage"
    }
    if (methodenEingang == null || aussageAusgang == null) {
        return kandidat(false, "Der Skalarprodukt-Begriffsknoten besitzt nicht seine kanonische Schnittstelle.")
    }
    val methodeVerbunden = karte.verbindungen.any {
        it.zu.knotenId == begriff.id && it.zu.anschlussId == methodenEingang.id
    }
    if (!methodeVerbunden) {
        return kandidat(false, "Die zu zertifizierende Methode ist nicht mit dem Begriffsknoten verbunden.")
    }
    val aussageVerbunden = karte.verbindungen.any {
        it.von.knotenId == begriff.id &&
            it.von.anschlussId == aussageAusgang.id &&
            it.zu.knotenId == ausgang.id &&
            it.zu.anschlussId == wertEingang.id
    }
    if (!aussageVerbunden) {
        return kandidat(false, "Die Begriffsaussage ist nicht direkt mit dem öffentlichen Ausgang verbunden.")
    }
    val version = begriff.parameter[SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER]?.toIntOrNull()
    if (version != SKALARPRODUKT_ZERTIFIKAT_VERSION) {
        return kandidat(false, "Die Zertifikatversion ist veraltet oder fehlt.")
    }
    val fehlendeNachweise = listOf(
        SKALARPRODUKT_NACHWEIS_LINEARITAET,
        SKALARPRODUKT_NACHWEIS_SYMMETRIE,
        SKALARPRODUKT_NACHWEIS_POSITIV,
    ).filter { begriff.parameter[it].isNullOrBlank() }
    if (fehlendeNachweise.isNotEmpty()) {
        return kandidat(false, "Mindestens eine Nachweisreferenz fehlt.")
    }
    return kandidat(true)
}

private data class MengenAuswahlEintrag(
    val schlüssel: String,
    val titel: String,
    val beschreibung: String,
    val eingebaut: MengenKnotenAuswahl? = null,
    val eigeneKarte: EigeneMengenKarte? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
internal object MengenKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val eingebaute = MengenKnotenAuswahl.entries.map { auswahl ->
            MengenAuswahlEintrag(
                schlüssel = "intern:${auswahl.stabileId}",
                titel = auswahl.titel,
                beschreibung = auswahl.beschreibung,
                eingebaut = auswahl,
            )
        }
        val eigene = MengenKnotenKartenQuelle.kandidaten().map { karte ->
            MengenAuswahlEintrag(
                schlüssel = "karte:${karte.verweis.kartenId.wert}:${karte.verweis.version}",
                titel = "${karte.name} · v${karte.verweis.version}",
                beschreibung = "Eigene Karte ohne Eingänge mit dem Mengenausgang „${karte.ausgangsName}“; die Version wird fest referenziert.",
                eigeneKarte = karte,
            )
        }
        val einträge = eingebaute + eigene
        val aktuell = knoten.kartenVerweis?.let { ref ->
            eigene.firstOrNull { it.eigeneKarte?.verweis == ref }
        } ?: eingebaute.firstOrNull {
            it.eingebaut == MengenKnotenAuswahl.vonId(knoten.parameter[MENGEN_KNOTEN_AUSWAHL])
        } ?: eingebaute.first()
        var geöffnet by remember(knoten.id, knoten.kartenVerweis, knoten.parameter[MENGEN_KNOTEN_AUSWAHL]) {
            mutableStateOf(false)
        }

        Text("Menge", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = aktuell.titel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Ausgewählte Menge") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                einträge.forEach { eintrag ->
                    DropdownMenuItem(
                        text = { Text(eintrag.titel, modifier = Modifier.padding(vertical = 2.dp)) },
                        onClick = {
                            geöffnet = false
                            eintrag.eingebaut?.let { auswahl ->
                                aktionen.knoten(
                                    knoten.copy(
                                        parameter = knoten.parameter + (MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
                                        kartenVerweis = null,
                                        anschlüsse = knoten.anschlüsse.map { anschluss ->
                                            if (anschluss.richtung == AnschlussRichtung.Ausgang) {
                                                anschluss.copy(
                                                    name = "menge",
                                                    kante = AnschlussKante.Rechts,
                                                    art = MathematikAnschlussArten.Menge.id,
                                                )
                                            } else anschluss
                                        },
                                    ),
                                )
                            }
                            eintrag.eigeneKarte?.let { karte ->
                                aktionen.knoten(
                                    knoten.copy(
                                        parameter = knoten.parameter + (MENGEN_KNOTEN_AUSWAHL to eintrag.schlüssel),
                                        kartenVerweis = karte.verweis,
                                        anschlüsse = knoten.anschlüsse.map { anschluss ->
                                            if (anschluss.richtung == AnschlussRichtung.Ausgang) {
                                                anschluss.copy(
                                                    name = karte.ausgangsName,
                                                    kante = AnschlussKante.Rechts,
                                                    art = MathematikAnschlussArten.Menge.id,
                                                )
                                            } else anschluss
                                        },
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
        Text(
            aktuell.beschreibung,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (eigene.isEmpty()) {
            Text(
                "Eigene Karten erscheinen hier, sobald sie keine öffentlichen Eingänge und genau einen öffentlichen Mengenausgang besitzen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal object VektorRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operator = VektorRechnerOperator.entries.firstOrNull {
            it.stabileId == knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        } ?: VektorRechnerOperator.SKALARPRODUKT
        var operatorGeöffnet by remember(knoten.id, operator) { mutableStateOf(false) }
        Text("Vektoroperation", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = operatorGeöffnet, onExpandedChange = { operatorGeöffnet = it }) {
            OutlinedTextField(
                value = operator.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Operator") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorGeöffnet) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = operatorGeöffnet, onDismissRequest = { operatorGeöffnet = false }) {
                DropdownMenuItem(
                    text = { Text("Skalarprodukt") },
                    onClick = {
                        operatorGeöffnet = false
                        aktionen.parameter(VEKTOR_RECHNER_OPERATOR, VektorRechnerOperator.SKALARPRODUKT.stabileId)
                    },
                )
            }
        }

        if (operator == VektorRechnerOperator.SKALARPRODUKT) {
            SkalarproduktDefinitionAuswahl(knoten, ergebnis, aktionen)
        }

        Text(
            "Zeilenvektoren, Spaltenvektoren und kartesische Zahlentupel sind zulässig; die Komponentenreihenfolge bleibt erhalten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkalarproduktDefinitionAuswahl(
    knoten: KnotenDaten,
    ergebnis: KnotenAuswertungsErgebnis?,
    aktionen: KnotenInspektorAktionen,
) {
    val kandidaten = MengenKnotenKartenQuelle.skalarproduktKandidaten()
    val ausgewaehlterVerweis = knoten.eingangsKartenVerweise[SKALARPRODUKT_DEFINITION_EINGANG]
    val ausgewaehlt = kandidaten.firstOrNull { it.verweis == ausgewaehlterVerweis }
    val istStandard = knoten.parameter[SKALARPRODUKT_DEFINITION_PARAMETER] !=
        SKALARPRODUKT_DEFINITION_ZERTIFIZIERT || ausgewaehlt == null
    var geöffnet by remember(knoten.id, ausgewaehlterVerweis, kandidaten) { mutableStateOf(false) }

    Text("Skalarproduktdefinition", style = MaterialTheme.typography.titleSmall)
    ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
        OutlinedTextField(
            value = if (istStandard) "Standardskalarprodukt" else "${ausgewaehlt?.name} · v${ausgewaehlt?.verweis?.version}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Definition") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
            DropdownMenuItem(
                text = { Text("Standardskalarprodukt") },
                onClick = {
                    geöffnet = false
                    aktionen.knoten(knoten.ohneSkalarproduktZertifikat())
                },
            )
            kandidaten.forEach { kandidat ->
                DropdownMenuItem(
                    enabled = kandidat.kompatibel,
                    text = {
                        Text(
                            buildString {
                                append("${kandidat.name} · v${kandidat.verweis.version}")
                                kandidat.grund?.let { append("\n$it") }
                            },
                        )
                    },
                    onClick = {
                        geöffnet = false
                        aktionen.knoten(knoten.mitSkalarproduktZertifikat(kandidat))
                    },
                )
            }
        }
    }

    if (kandidaten.isEmpty()) {
        Text(
            "Zertifikatskarten benötigen keinen öffentlichen Eingang, genau einen Aussageausgang und einen vollständig belegten Skalarprodukt-Begriffsknoten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        ausgewaehlt?.grund?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    val ergebnisBereich = ergebnis?.ausgaben?.get(VEKTOR_RECHNER_AUSGANG)?.zielMenge?.zuLatex()
    val deklarierterBereich = FundamentalerZahlbereich.entries.firstOrNull {
        it.id == knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER]
    }
    val quaternionisch = ausgewaehlt?.zahlbereich == FundamentalerZahlbereich.QUATERNION ||
        deklarierterBereich == FundamentalerZahlbereich.QUATERNION ||
        ergebnisBereich == FundamentalerZahlbereich.QUATERNION.latex
    if (quaternionisch) {
        val aktuell = SkalarproduktLinearitaet.entries.firstOrNull {
            it.name == knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER]
        } ?: SkalarproduktLinearitaet.RECHTSLINEAR
        Text("Quaternionische Linearitätsseite", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = aktuell == SkalarproduktLinearitaet.RECHTSLINEAR,
                onClick = {
                    aktionen.parameter(
                        SKALARPRODUKT_LINEARITAET_PARAMETER,
                        SkalarproduktLinearitaet.RECHTSLINEAR.name,
                    )
                },
                label = { Text("rechtslinear") },
            )
            FilterChip(
                selected = aktuell == SkalarproduktLinearitaet.LINKSLINEAR,
                onClick = {
                    aktionen.parameter(
                        SKALARPRODUKT_LINEARITAET_PARAMETER,
                        SkalarproduktLinearitaet.LINKSLINEAR.name,
                    )
                },
                label = { Text("linkslinear") },
            )
        }
    }
}

private fun KnotenDaten.ohneSkalarproduktZertifikat(): KnotenDaten = copy(
    parameter = parameter + mapOf(
        SKALARPRODUKT_DEFINITION_PARAMETER to SKALARPRODUKT_DEFINITION_STANDARD,
        SKALARPRODUKT_LINEARITAET_PARAMETER to SkalarproduktLinearitaet.RECHTSLINEAR.name,
    ),
    eingangsKartenVerweise = eingangsKartenVerweise - SKALARPRODUKT_DEFINITION_EINGANG,
    anschlüsse = anschlüsse.filterNot {
        it.richtung == AnschlussRichtung.Eingang && it.name == SKALARPRODUKT_DEFINITION_EINGANG
    },
)

private fun KnotenDaten.mitSkalarproduktZertifikat(
    kandidat: SkalarproduktZertifikatsKarte,
): KnotenDaten {
    val definitionEingang = anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == SKALARPRODUKT_DEFINITION_EINGANG
    } ?: AnschlussDaten(
        name = SKALARPRODUKT_DEFINITION_EINGANG,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = 2,
    )
    return copy(
        parameter = parameter + mapOf(
            SKALARPRODUKT_DEFINITION_PARAMETER to SKALARPRODUKT_DEFINITION_ZERTIFIZIERT,
            SKALARPRODUKT_ZAHLBEREICH_PARAMETER to kandidat.zahlbereich.id,
            SKALARPRODUKT_LINEARITAET_PARAMETER to kandidat.linearitaet.name,
            SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER to SKALARPRODUKT_ZERTIFIKAT_VERSION.toString(),
        ),
        eingangsKartenVerweise = eingangsKartenVerweise +
            (SKALARPRODUKT_DEFINITION_EINGANG to kandidat.verweis),
        anschlüsse = if (definitionEingang in anschlüsse) anschlüsse else anschlüsse + definitionEingang,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal object SkalarproduktBegriffInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val zahlbereich = FundamentalerZahlbereich.entries.firstOrNull {
            it.id == knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER]
        } ?: FundamentalerZahlbereich.REELL
        var bereichGeöffnet by remember(knoten.id, zahlbereich) { mutableStateOf(false) }

        Text("Zertifikat", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = bereichGeöffnet, onExpandedChange = { bereichGeöffnet = it }) {
            OutlinedTextField(
                value = zahlbereich.latex,
                onValueChange = {},
                readOnly = true,
                label = { Text("Skalarkörper") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bereichGeöffnet) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = bereichGeöffnet, onDismissRequest = { bereichGeöffnet = false }) {
                listOf(
                    FundamentalerZahlbereich.RATIONAL,
                    FundamentalerZahlbereich.REELL,
                    FundamentalerZahlbereich.KOMPLEX,
                    FundamentalerZahlbereich.QUATERNION,
                ).forEach { bereich ->
                    DropdownMenuItem(
                        text = { Text(bereich.latex) },
                        onClick = {
                            bereichGeöffnet = false
                            aktionen.parameter(SKALARPRODUKT_ZAHLBEREICH_PARAMETER, bereich.id)
                            if (bereich != FundamentalerZahlbereich.QUATERNION) {
                                aktionen.parameter(
                                    SKALARPRODUKT_LINEARITAET_PARAMETER,
                                    SkalarproduktLinearitaet.RECHTSLINEAR.name,
                                )
                            }
                        },
                    )
                }
            }
        }

        if (zahlbereich == FundamentalerZahlbereich.QUATERNION) {
            val linearitaet = SkalarproduktLinearitaet.entries.firstOrNull {
                it.name == knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER]
            } ?: SkalarproduktLinearitaet.RECHTSLINEAR
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = linearitaet == SkalarproduktLinearitaet.RECHTSLINEAR,
                    onClick = {
                        aktionen.parameter(
                            SKALARPRODUKT_LINEARITAET_PARAMETER,
                            SkalarproduktLinearitaet.RECHTSLINEAR.name,
                        )
                    },
                    label = { Text("rechtslinear") },
                )
                FilterChip(
                    selected = linearitaet == SkalarproduktLinearitaet.LINKSLINEAR,
                    onClick = {
                        aktionen.parameter(
                            SKALARPRODUKT_LINEARITAET_PARAMETER,
                            SkalarproduktLinearitaet.LINKSLINEAR.name,
                        )
                    },
                    label = { Text("linkslinear") },
                )
            }
        }

        NachweisFeld("Linearität", SKALARPRODUKT_NACHWEIS_LINEARITAET, knoten, aktionen)
        NachweisFeld("Konjugierte Symmetrie", SKALARPRODUKT_NACHWEIS_SYMMETRIE, knoten, aktionen)
        NachweisFeld("Positive Definitheit", SKALARPRODUKT_NACHWEIS_POSITIV, knoten, aktionen)
        Text(
            "Zertifikatversion $SKALARPRODUKT_ZERTIFIKAT_VERSION. Leere Referenzen bleiben unvollständig und sind im Vektorrechner nicht auswählbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NachweisFeld(
    titel: String,
    schlüssel: String,
    knoten: KnotenDaten,
    aktionen: KnotenInspektorAktionen,
) {
    OutlinedTextField(
        value = knoten.parameter[schlüssel].orEmpty(),
        onValueChange = { aktionen.parameter(schlüssel, it.trim()) },
        label = { Text("$titel: Nachweisreferenz") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
