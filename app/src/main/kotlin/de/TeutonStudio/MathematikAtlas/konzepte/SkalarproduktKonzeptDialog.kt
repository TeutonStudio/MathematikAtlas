package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.BEGRIFF_SKALARPRODUKT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_DEFINITION_ZERTIFIZIERT
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_LINEARITAET_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_ZAHLBEREICH_PARAMETER
import de.TeutonStudio.MathematikKnoten.SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_AUSGANG
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.BegriffsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.FundamentalerZahlbereich
import de.TeutonStudio.MathematikRechenSystem.kern.NachweisStatus
import de.TeutonStudio.MathematikRechenSystem.kern.NumerischeKomponentenAnsicht
import de.TeutonStudio.MathematikRechenSystem.kern.SKALARPRODUKT_ZERTIFIKAT_VERSION
import de.TeutonStudio.MathematikRechenSystem.kern.SkalarproduktFalkAblauf
import de.TeutonStudio.MathematikRechenSystem.kern.SkalarproduktLinearitaet
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturPruefung
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.numerischeKomponentenAnsicht
import de.TeutonStudio.MathematikRechenSystem.kern.skalarproduktZahlbereichOderNull

private enum class SkalarproduktKonzeptReiter(val titel: String) {
    DEFINITION("Definition"),
    FALK("Falksches Schema"),
    ZERTIFIKAT("Zertifikat"),
}

internal fun besitztSkalarproduktKonzeptDialog(knoten: KnotenDaten): Boolean {
    if (knoten.art == BEGRIFF_SKALARPRODUKT_KNOTEN_ART) return true
    if (knoten.art != VektorRechner.KNOTEN_ART) return false
    val operator = VektorRechnerOperator.entries.firstOrNull {
        it.stabileId == knoten.parameter[VEKTOR_RECHNER_OPERATOR]
    } ?: VektorRechnerOperator.SKALARPRODUKT
    return operator == VektorRechnerOperator.SKALARPRODUKT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkalarproduktKonzeptDialog(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    schließen: () -> Unit,
) {
    val auswertung = zustand.auswertung.knoten[knoten.id]
    val daten = remember(knoten.id, knoten.parameter, auswertung) {
        skalarproduktDialogDaten(knoten, auswertung)
    }
    var reiter by remember(knoten.id) { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f).fillMaxHeight(.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Skalarprodukt", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (knoten.art == BEGRIFF_SKALARPRODUKT_KNOTEN_ART) {
                                "Begriffsnachweis für eine Methode V × V → K."
                            } else {
                                "Standarddefinition, komponentenweiser Falk-Ablauf und Zertifikatsstatus."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                HorizontalDivider()
                PrimaryTabRow(selectedTabIndex = reiter) {
                    SkalarproduktKonzeptReiter.entries.forEachIndexed { index, eintrag ->
                        Tab(
                            selected = reiter == index,
                            onClick = { reiter = index },
                            text = { Text(eintrag.titel) },
                        )
                    }
                }
                when (SkalarproduktKonzeptReiter.entries[reiter]) {
                    SkalarproduktKonzeptReiter.DEFINITION -> SkalarproduktDefinitionInhalt(daten)
                    SkalarproduktKonzeptReiter.FALK -> SkalarproduktFalkInhalt(daten)
                    SkalarproduktKonzeptReiter.ZERTIFIKAT -> SkalarproduktZertifikatInhalt(knoten, auswertung, daten)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.dupliziereAuswahl()
                            schließen()
                        },
                    ) { Text("Knoten duplizieren") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.löscheAuswahl()
                            schließen()
                        },
                    ) { Text("Knoten löschen") }
                }
            }
        }
    }
}

private data class SkalarproduktDialogDaten(
    val ablauf: SkalarproduktFalkAblauf,
    val zahlbereich: FundamentalerZahlbereich,
    val linearitaet: SkalarproduktLinearitaet,
    val standardDefinition: Boolean,
    val konkreteKomponenten: Boolean,
    val ergebnisLatex: String?,
    val fehler: String?,
)

private fun skalarproduktDialogDaten(
    knoten: KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?,
): SkalarproduktDialogDaten {
    val linkeAnsicht = auswertung?.eingänge?.get("links")?.objekt?.komponentenAnsichtOderNull()
    val rechteAnsicht = auswertung?.eingänge?.get("rechts")?.objekt?.komponentenAnsichtOderNull()
    val dimension = when {
        linkeAnsicht != null && rechteAnsicht != null -> minOf(linkeAnsicht.laenge, rechteAnsicht.laenge)
        else -> 3
    }.coerceAtLeast(1)
    val links = linkeAnsicht?.komponenten?.take(dimension)?.map { it.zuLatex() }
        ?: List(dimension) { index -> "u_{$index}" }
    val rechts = rechteAnsicht?.komponenten?.take(dimension)?.map { it.zuLatex() }
        ?: List(dimension) { index -> "v_{$index}" }
    val parametrierterBereich = FundamentalerZahlbereich.entries.firstOrNull {
        it.id == knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER] ||
            it.name == knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER]
    }
    val zahlbereich = auswertung?.ausgaben?.get(VEKTOR_RECHNER_AUSGANG)
        ?.zielMenge
        ?.skalarproduktZahlbereichOderNull()
        ?: linkeAnsicht?.zahlBereich?.skalarproduktZahlbereichOderNull()
        ?: rechteAnsicht?.zahlBereich?.skalarproduktZahlbereichOderNull()
        ?: parametrierterBereich
        ?: FundamentalerZahlbereich.REELL
    val linearitaet = SkalarproduktLinearitaet.entries.firstOrNull {
        it.name == knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER]
    } ?: SkalarproduktLinearitaet.RECHTSLINEAR
    val konjugiert = zahlbereich in setOf(
        FundamentalerZahlbereich.KOMPLEX,
        FundamentalerZahlbereich.QUATERNION,
    )
    return SkalarproduktDialogDaten(
        ablauf = SkalarproduktFalkAblauf(links, rechts, linearitaet, konjugiert),
        zahlbereich = zahlbereich,
        linearitaet = linearitaet,
        standardDefinition = knoten.parameter[SKALARPRODUKT_DEFINITION_PARAMETER] !=
            SKALARPRODUKT_DEFINITION_ZERTIFIZIERT,
        konkreteKomponenten = linkeAnsicht != null && rechteAnsicht != null &&
            linkeAnsicht.laenge == rechteAnsicht.laenge,
        ergebnisLatex = auswertung?.ausgaben?.get(VEKTOR_RECHNER_AUSGANG)?.objekt?.zuLatex(),
        fehler = auswertung?.fehler,
    )
}

private fun de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt.komponentenAnsichtOderNull(): NumerischeKomponentenAnsicht? =
    when (val ansicht = numerischeKomponentenAnsicht()) {
        is StrukturPruefung.Gueltig -> ansicht.wert
        else -> null
    }

@Composable
private fun SkalarproduktDefinitionInhalt(daten: SkalarproduktDialogDaten) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Definition", style = MaterialTheme.typography.titleMedium)
        LatexText(
            latex = when (daten.linearitaet) {
                SkalarproduktLinearitaet.RECHTSLINEAR ->
                    "\\langle u,v\\rangle=\\sum_{i=0}^{${daten.ablauf.dimension - 1}}" +
                        if (daten.ablauf.konjugiert) "\\overline{u_i}v_i" else "u_iv_i"
                SkalarproduktLinearitaet.LINKSLINEAR ->
                    "\\langle u,v\\rangle=\\sum_{i=0}^{${daten.ablauf.dimension - 1}}" +
                        if (daten.ablauf.konjugiert) "u_i\\overline{v_i}" else "u_iv_i"
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Skalarkörper: ${daten.zahlbereich.latex}. " +
                when {
                    daten.zahlbereich == FundamentalerZahlbereich.QUATERNION ->
                        "Die Faktorenreihenfolge ist wesentlich; ${if (daten.linearitaet == SkalarproduktLinearitaet.RECHTSLINEAR) "der linke" else "der rechte"} Faktor wird konjugiert."
                    daten.zahlbereich == FundamentalerZahlbereich.KOMPLEX ->
                        "Die Konjugation stellt die hermitesche Symmetrie sicher."
                    else -> "Über reellen und rationalen Skalaren ist keine Konjugation nötig."
                },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!daten.standardDefinition) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    "Der Rechner verwendet derzeit eine zertifizierte eigene Methode. Das Falk-Schema zeigt weiterhin die kanonische Standarddefinition zum Vergleich.",
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        daten.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SkalarproduktFalkInhalt(daten: SkalarproduktDialogDaten) {
    var index by remember(daten.ablauf) { mutableIntStateOf(0) }
    val sichererIndex = index.coerceIn(0, daten.ablauf.dimension - 1)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Index", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(daten.ablauf.dimension) { kandidat ->
                    FilterChip(
                        selected = kandidat == sichererIndex,
                        onClick = { index = kandidat },
                        label = { Text(kandidat.toString()) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FalkZeile("u", daten.ablauf.linkeKomponenten, sichererIndex) { index = it }
            FalkZeile("v", daten.ablauf.rechteKomponenten, sichererIndex) { index = it }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aktueller Summand p$sichererIndex", style = MaterialTheme.typography.labelLarge)
                LatexText(
                    latex = "p_{$sichererIndex}=${daten.ablauf.produktLatex(sichererIndex)}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Text("Geordnete Teilsummen", style = MaterialTheme.typography.titleMedium)
        repeat(sichererIndex + 1) { teilsummenIndex ->
            val ausgewählt = teilsummenIndex == sichererIndex
            Surface(
                color = if (ausgewählt) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                LatexText(
                    latex = "s_{$teilsummenIndex}=${daten.ablauf.teilsummeLatex(teilsummenIndex)}",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        HorizontalDivider()
        Text("Vollständiges Skalarprodukt", style = MaterialTheme.typography.titleMedium)
        LatexText(
            latex = "\\langle u,v\\rangle=${daten.ablauf.vollständigeSummeLatex()}",
            style = MaterialTheme.typography.titleLarge,
        )
        daten.ergebnisLatex?.let { ergebnis ->
            LatexText(
                latex = "\\langle u,v\\rangle=$ergebnis",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (!daten.konkreteKomponenten) {
            Text(
                "Die Knoteneingänge sind noch nicht als gleich lange numerische Komponentenfolgen auswertbar. Daher werden symbolische Komponenten gezeigt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FalkZeile(
    name: String,
    komponenten: List<String>,
    ausgewählterIndex: Int,
    auswählen: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.Center) {
            LatexText(latex = name, style = MaterialTheme.typography.titleMedium)
        }
        komponenten.forEachIndexed { index, komponente ->
            val ausgewählt = index == ausgewählterIndex
            Surface(
                onClick = { auswählen(index) },
                modifier = Modifier.width(104.dp).height(62.dp),
                color = if (ausgewählt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    width = if (ausgewählt) 2.dp else 1.dp,
                    color = if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    LatexText(latex = "${name}_{$index}=$komponente")
                }
            }
        }
    }
}

@Composable
private fun SkalarproduktZertifikatInhalt(
    knoten: KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?,
    daten: SkalarproduktDialogDaten,
) {
    val aussage = auswertung?.ausgaben?.get("aussage")?.objekt as? BegriffsAussage
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Zertifikatsstatus", style = MaterialTheme.typography.titleMedium)
        ZertifikatZeile("Definition", if (daten.standardDefinition) "Standardskalarprodukt" else "Eigene zertifizierte Methode")
        ZertifikatZeile("Skalarkörper", daten.zahlbereich.latex)
        ZertifikatZeile(
            "Linearitätsseite",
            if (daten.linearitaet == SkalarproduktLinearitaet.RECHTSLINEAR) "rechtslinear" else "linkslinear",
        )
        ZertifikatZeile(
            "Zertifikatversion",
            knoten.parameter[SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER]
                ?: SKALARPRODUKT_ZERTIFIKAT_VERSION.toString(),
        )
        if (aussage != null) {
            val statusText = when (aussage.pruefung.status) {
                NachweisStatus.Nachgewiesen -> "nachgewiesen"
                NachweisStatus.Widerlegt -> "widerlegt"
                is NachweisStatus.Bedingt -> "bedingt"
                NachweisStatus.Unvollstaendig -> "unvollständig"
                NachweisStatus.Unentscheidbar -> "unentscheidbar"
            }
            ZertifikatZeile("Begriffsnachweis", statusText)
            aussage.pruefung.axiomPruefungen.forEach { pruefung ->
                Surface(
                    color = when (pruefung.status) {
                        NachweisStatus.Nachgewiesen -> MaterialTheme.colorScheme.primaryContainer
                        NachweisStatus.Widerlegt -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(pruefung.name, style = MaterialTheme.typography.labelLarge)
                        Text(pruefung.begruendung, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Text(
                if (knoten.art == BEGRIFF_SKALARPRODUKT_KNOTEN_ART) {
                    "Der Begriffsknoten ist noch nicht vollständig auswertbar. Verbinde eine Methode und hinterlege alle Nachweisreferenzen im Inspector."
                } else if (daten.standardDefinition) {
                    "Die Standarddefinition ist fest im Rechenkern implementiert. Eine eigene Definition benötigt eine versionsfeste Zertifikatskarte."
                } else {
                    "Die ausgewählte Zertifikatskarte wird vor jeder Ausführung auf Begriff, Version, Skalarkörper und Linearitätsseite geprüft."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        daten.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ZertifikatZeile(bezeichnung: String, wert: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(bezeichnung, modifier = Modifier.width(170.dp), style = MaterialTheme.typography.labelLarge)
        Text(wert, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}