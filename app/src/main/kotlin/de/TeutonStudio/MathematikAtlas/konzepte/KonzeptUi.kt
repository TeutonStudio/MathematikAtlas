package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAktionen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog

import de.TeutonStudio.MathematikKnoten.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKnoten.AussagenOperatorArt
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.LatexFormel
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.MathematikKnotenRenderer
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.normalisiereLatexQuelltext
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsDetailansicht

internal fun besitztHochauflösendenVisualisierungsReiter(knoten: KnotenDaten?): Boolean =
    knoten?.art == MathematikKnotenVorlagen.Visualisierung.art

@Composable
internal fun KnotenKonzeptDialog(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    schließen: () -> Unit,
) {
    val kartenTabellenQuelle = remember(knoten.kartenVerweis, knoten.anschlüsse) {
        knoten.kartenVerweis
            ?.let(zustand.speicher::lade)
            ?.let { karte -> ermittleKartenWahrheitstabellenQuelle(zustand.anschlussArten, knoten, karte) }
    }
    if (kartenTabellenQuelle != null) {
        KartenWahrheitstabellenDialog(
            zustand = zustand,
            knoten = knoten,
            quelle = kartenTabellenQuelle,
            schließen = schließen,
        )
        return
    }

    if (besitztSkalarproduktKonzeptDialog(knoten)) {
        SkalarproduktKonzeptDialog(
            zustand = zustand,
            knoten = knoten,
            schließen = schließen,
        )
        return
    }

    val besitztAussagenDialog = AussagenOperatorArt.für(knoten) != null ||
        knoten.art == MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART
    var definitionAnzeigen by remember(knoten.id) { mutableStateOf(false) }
    if (besitztAussagenDialog && !definitionAnzeigen) {
        AussagenOperatorDialog(
            zustand = zustand,
            knoten = knoten,
            definitionÖffnen = { definitionAnzeigen = true },
            schließen = schließen,
        )
        return
    }

    val context = LocalContext.current
    val konzept = remember(knoten.art, knoten.parameter, knoten.eigenschaften, knoten.kartenVerweis, zustand.auswertung.knoten[knoten.id]?.eingänge, context) {
        if (knoten.art == "mathematik.multinomVektor") {
            multinomVektorKonzept(knoten)
        } else {
            dynamischesKonzeptFürKnoten(zustand, knoten)
                ?: enzyklopädieKonzeptFürKnoten(context, knoten)
        }
    }
    KonzeptDialog(
        zustand = zustand,
        konzept = konzept,
        ursprungsKnoten = knoten,
        schließen = schließen,
    )
}

@Composable
internal fun KonzeptKatalogDialog(
    zustand: AtlasZustand,
    konzept: KonzeptDefinition,
    schließen: () -> Unit,
) {
    KonzeptDialog(
        zustand = zustand,
        konzept = konzept,
        ursprungsKnoten = null,
        schließen = schließen,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun KonzeptDialog(
    zustand: AtlasZustand,
    konzept: KonzeptDefinition?,
    ursprungsKnoten: KnotenDaten?,
    schließen: () -> Unit,
) {
    var reiterIndex by remember(konzept?.id, ursprungsKnoten?.art) { mutableIntStateOf(0) }
    var komplexDarstellung by remember(konzept?.id, ursprungsKnoten?.id) {
        mutableStateOf(KomplexDarstellung.Kartesisch)
    }
    val besitztVisualisierungsReiter = besitztHochauflösendenVisualisierungsReiter(ursprungsKnoten)
    val visualisierungsReiterIndex = konzept?.reiter?.size ?: 0
    val zeigtVisualisierung = besitztVisualisierungsReiter && reiterIndex == visualisierungsReiterIndex
    val aktiverReiter = if (zeigtVisualisierung) null else konzept?.reiter?.getOrNull(reiterIndex.coerceAtLeast(0))
    val aktiveKarte = aktiverReiter?.karteFür(komplexDarstellung)
    val zeigtFalkSchema = aktiverReiter?.darstellung in setOf(KonzeptReiterDarstellung.FalkSchema, KonzeptReiterDarstellung.Auswertungsschema)

    AtlasDialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize()) {
            AtlasDialogKopf(
                titel = { Text(konzept?.name ?: ursprungsKnoten?.name.orEmpty(),) },
                untertitel = { Text(
                        if (zeigtVisualisierung) {
                            "Die hochauflösende Darstellung wird nur in diesem Reiter berechnet und anschließend gecacht."
                        } else {
                            konzept?.beschreibung ?: "Für diese Knotenart ist keine Definitionskarte registriert."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) },
                aktionen = { TextButton(onClick = schließen) { Text("Schließen") } },
            )
            HorizontalDivider()

            if (besitztVisualisierungsReiter || (konzept?.reiter?.size ?: 0) > 1) {
                PrimaryTabRow(selectedTabIndex = reiterIndex) {
                    konzept?.reiter.orEmpty().forEachIndexed { index, reiter ->
                        Tab(
                            selected = reiterIndex == index,
                            onClick = { reiterIndex = index },
                            text = {
                                LatexText(
                                    latex = normalisiereKonzeptReiterTitel(reiter.titel),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                    if (besitztVisualisierungsReiter) {
                        Tab(
                            selected = reiterIndex == visualisierungsReiterIndex,
                            onClick = { reiterIndex = visualisierungsReiterIndex },
                            text = { Text("Hohe Auflösung") },
                        )
                    }
                }
            }

            if (zeigtVisualisierung) {
                val visualisierungsKnoten = checkNotNull(ursprungsKnoten)
                VisualisierungsDetailansicht(
                    knoten = visualisierungsKnoten,
                    auswertung = zustand.auswertung.knoten[visualisierungsKnoten.id],
                    eigenschaftenErsetzen = { eigenschaften ->
                        zustand.editor.führeAus(
                            KartenAktion.KnotenEigenschaftenErsetzen(visualisierungsKnoten.id, eigenschaften),
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(AtlasDialogAbstände.inhalt),
                )
            } else if (konzept == null) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(AtlasDialogAbstände.abschnitt)) {
                    Text(
                        "Fehlende Definition für ${ursprungsKnoten?.art.orEmpty()}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                if (konzept.reiter.any(KonzeptReiter::besitztDarstellungsVarianten)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = AtlasDialogAbstände.horizontal, vertical = AtlasDialogAbstände.beschreibung),
                        horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
                    ) {
                        FilterChip(
                            selected = komplexDarstellung == KomplexDarstellung.Kartesisch,
                            onClick = { komplexDarstellung = KomplexDarstellung.Kartesisch },
                            label = { Text("Kartesisch") },
                        )
                        FilterChip(
                            selected = komplexDarstellung == KomplexDarstellung.Polar,
                            onClick = { komplexDarstellung = KomplexDarstellung.Polar },
                            label = { Text("Polar") },
                        )
                    }
                }
                if (zeigtFalkSchema) {
                    if (ursprungsKnoten != null) RechnerAuswertungsschemaInhalt(
                        knoten = ursprungsKnoten,
                        auswertung = zustand.auswertung.knoten[ursprungsKnoten.id],
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) else MatrixproduktFalkSchemaInhalt(null, Modifier.weight(1f).fillMaxWidth())
                } else {
                    aktiveKarte?.let { karte ->
                        UnveränderlicheKonzeptKarte(
                            zustand = zustand,
                            karte = karte,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
            }

            HorizontalDivider()
            AtlasDialogAktionen {
                aktiveKarte?.takeUnless { zeigtFalkSchema }?.let { karte ->
                    Button(
                        onClick = {
                            zustand.öffneBearbeitbareKopie(karte)
                            schließen()
                        },
                    ) { Text("Als bearbeitbare Karte kopieren") }
                }
                ursprungsKnoten?.let { knoten ->
                    OutlinedButton(onClick = { zustand.editor.merkeKnotenZumHerausziehen(knoten.id); schließen() }) {
                        Text("Aus Verbindung herausziehen")
                    }
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.dupliziereAuswahl()
                            schließen()
                        },
                    ) { Text("Knoten duplizieren") }
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.isoliereAusgewähltenKnoten()
                            schließen()
                        },
                    ) { Text("Knoten isolieren") }
                    Button(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.löscheAuswahl()
                            schließen()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Knoten löschen") }
                }
            }
        }

    }
}

private val doppeltEscapterLatexBefehl = Regex("""\\\\(?=[A-Za-z])""")

internal fun normalisiereKonzeptReiterTitel(titel: String): String =
    normalisiereLatexQuelltext(titel).replace(doppeltEscapterLatexBefehl) { "\\" }

@Composable
private fun UnveränderlicheKonzeptKarte(
    zustand: AtlasZustand,
    karte: KartenDaten,
    modifier: Modifier = Modifier,
) {
    val editor = remember(karte.id) {
        KartenEditorZustand(karte, GraphPrüfung(zustand.anschlussArten))
    }
    val auswertung = remember(karte) {
        KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())
            .werteKonzeptKarteAus(karte)
    }
    val mathematikRenderer = remember(auswertung) {
        MathematikKnotenRenderer { knoten -> auswertung.knoten[knoten.id] }
    }

    LaunchedEffect(editor.karte) {
        val ohneAnsicht = editor.karte.copy(ansicht = karte.ansicht)
        if (ohneAnsicht != karte) {
            editor.ersetzeKarte(karte.copy(ansicht = editor.karte.ansicht))
        }
    }

    KnotenKartenEditor(
        zustand = editor,
        modifier = modifier,
        rendererFür = { knoten ->
            if (knoten.art.startsWith("konzept.")) KonzeptDokumentationsRenderer else mathematikRenderer
        },
        farbeFürAnschluss = { anschluss -> anschlussFarbe(anschluss.art.wert) },
        zeigeKnotenInspektor = false,
    )
}

internal enum class DokumentationsInhaltArt {
    TEXT,
    INLINE_LATEX,
    DISPLAY_LATEX,
}

private val dokumentationsLatexBefehl = Regex("""\\(?:[A-Za-z]+|.)""")

internal fun dokumentationsInhaltArt(
    knoten: KnotenDaten,
    schlüssel: String,
    latexStandard: DokumentationsInhaltArt,
): DokumentationsInhaltArt {
    val ausdrücklich = knoten.parameter["${schlüssel}InhaltArt"]
        ?.trim()
        ?.uppercase()
        ?.replace('-', '_')
    if (ausdrücklich != null) {
        return DokumentationsInhaltArt.entries.firstOrNull { it.name == ausdrücklich }
            ?: DokumentationsInhaltArt.TEXT
    }
    val inhalt = knoten.parameter[schlüssel].orEmpty()
    return if (dokumentationsLatexBefehl.containsMatchIn(inhalt)) latexStandard else DokumentationsInhaltArt.TEXT
}

@Composable
private fun DokumentationsInhalt(
    inhalt: String,
    art: DokumentationsInhaltArt,
    style: TextStyle,
) {
    when (art) {
        DokumentationsInhaltArt.TEXT -> Text(inhalt, style = style)
        DokumentationsInhaltArt.INLINE_LATEX -> LatexText(inhalt, style = style)
        DokumentationsInhaltArt.DISPLAY_LATEX -> LatexFormel(inhalt, style = style)
    }
}

internal object KonzeptDokumentationsRenderer : KnotenRenderer {
    @Composable
    override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        Column(
            Modifier.fillMaxSize().padding(AtlasDialogAbstände.felder),
            verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
        ) {
            when (knoten.art) {
                KonzeptKnotenArten.REGEL -> {
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    DokumentationsInhalt(
                        inhalt = knoten.parameter["regel"].orEmpty(),
                        art = dokumentationsInhaltArt(
                            knoten = knoten,
                            schlüssel = "regel",
                            latexStandard = DokumentationsInhaltArt.DISPLAY_LATEX,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        knoten.parameter["knotenArt"].orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KonzeptKnotenArten.EINGANG -> {
                    Text("Eingang", style = MaterialTheme.typography.labelLarge)
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    DokumentationsInhalt(
                        inhalt = knoten.parameter["typ"].orEmpty(),
                        art = dokumentationsInhaltArt(
                            knoten = knoten,
                            schlüssel = "typ",
                            latexStandard = DokumentationsInhaltArt.INLINE_LATEX,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (knoten.parameter["variabel"] == "true") {
                        Text("erweiterbar", style = MaterialTheme.typography.labelSmall)
                    }
                }
                else -> {
                    Text("Ausgang", style = MaterialTheme.typography.labelLarge)
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    DokumentationsInhalt(
                        inhalt = knoten.parameter["typ"].orEmpty(),
                        art = dokumentationsInhaltArt(
                            knoten = knoten,
                            schlüssel = "typ",
                            latexStandard = DokumentationsInhaltArt.INLINE_LATEX,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
