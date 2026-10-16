package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKnoten.*

import de.TeutonStudio.MathematikRechenSystem.kern.parameter
import de.TeutonStudio.MathematikRechenSystem.kern.ausgabeNamen
import de.TeutonStudio.MathematikRechenSystem.kern.werteVorräte

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.KnotenErsetzungsAuswirkung
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.BILDMENGE_ARGUMENT_MODUS
import de.TeutonStudio.MathematikKnoten.BILDMENGE_MODUS_EINZELMENGEN
import de.TeutonStudio.MathematikKnoten.BILDMENGE_MODUS_PRODUKT
import de.TeutonStudio.MathematikKnoten.GeometrieTeilobjektTyp
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.bildmengeArgumentName
import de.TeutonStudio.MathematikKnoten.TUPEL_ERGÄNZEN_ART
import de.TeutonStudio.MathematikKnoten.WertebereichKonfiguration
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.Abbild
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.MethodenParameter
import de.TeutonStudio.MathematikRechenSystem.kern.OrbitFamilie
import de.TeutonStudio.MathematikRechenSystem.kern.Tensor
import de.TeutonStudio.MathematikRechenSystem.kern.VektorOrientierung
import de.TeutonStudio.MathematikRechenSystem.kern.alsMathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.parseTensorPermutationOderNull
import de.TeutonStudio.MathematikRechenSystem.kern.standardTensorPermutation

interface KnotenInspektor {
    val hauptBereich: de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich
        get() = de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich.Knotendaten
    fun hatBereich(knoten: KnotenDaten, bereich: de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich): Boolean = bereich == hauptBereich
    @Composable fun BereichInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen,
        bereich: de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich) {
        if (bereich == hauptBereich) Inhalt(knoten, ergebnis, aktionen)
    }
    @Composable fun AnschlussInhalt(knoten: KnotenDaten, anschluss: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten,
        ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {}
    @Composable fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen)
}
interface KnotenInspektorAktionen {
    fun parameter(schlüssel: String, wert: String)
    fun name(wert: String)
    fun eigenschaften(eigenschaften: Map<String, de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft>)
    fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId)
    fun knoten(knoten: KnotenDaten)
    /** Nur nach einer expliziten Verlustbestätigung durch den gemeinsamen Auswahl-/Modusdialog. */
    fun knotenNachVerlustbestätigung(knoten: KnotenDaten) = knoten(knoten)
    fun vorschauKnotenErsetzen(knoten: KnotenDaten): KnotenErsetzungsAuswirkung =
        KnotenErsetzungsAuswirkung(
            erhalteneAnschlüsse = knoten.anschlüsse,
            hinzugefügteAnschlüsse = emptyList(),
            entfallendeAnschlüsse = emptyList(),
            entfallendeVerbindungen = emptyList(),
        )
}
object KnotenInspektorRegister {
    private val inspektoren = mapOf<String, KnotenInspektor>(
        de.TeutonStudio.MathematikKartenAdapter.METHODEN_QUELLE_ART to MethodenQuellenInspektor,
        de.TeutonStudio.MathematikKnoten.ÜBERSETZER_ART to ÜbersetzerInspektor,
        de.TeutonStudio.MathematikKnoten.RICHTUNG_WINKEL_ART to RichtungsWinkelInspektor,
        "mathematik.lösungsmenge" to LösungsmengeInspektor,
        "mathematik.halbesIntervall" to IntervallKnotenInspektor,
        "mathematik.reellesIntervall" to IntervallKnotenInspektor,
        "mathematik.abbild" to BildmengeInspektor,
        "mathematik.visualisierung" to VisualisierungsInspektor,
        "mathematik.kartenEingang" to KartenSchnittstellenInspektor,
        "mathematik.kartenAusgang" to KartenSchnittstellenInspektor,
        "mathematik.variable" to VariablenInspektor,
        "mathematik.allgemeinerParameter" to AllgemeineParameterInspektor,
        "mathematik.termZuMethode" to TermZuMethodeInspektor,
        "mathematik.orbit" to OrbitInspektor,
        "mathematik.methodeAufrufen" to MethodenAufrufInspektor,
        "mathematik.ordnungsrelation" to OrdnungsrelationInspektor,
        "mathematik.endlicheMenge" to EndlicheMengeInspektor,
        "mathematik.transponieren" to TransponierenInspektor,
        "mathematik.matrixdiagonale" to MatrixdiagonaleInspektor,
        "mathematik.auswerten" to AuswertenInspektor,
        "mathematik.gleichungLösen" to AuswertenInspektor,
        "mathematik.vektor" to VektorKonstruktorInspektor,
        "mathematik.zeilenVektor" to VektorKonstruktorInspektor,
        "mathematik.multinomVektor" to MultinomVektorInspektor,
        "mathematik.tupelZuSpalte" to OrientierungsKnotenInspektor,
        "mathematik.tupelZuZeile" to OrientierungsKnotenInspektor,
        "mathematik.einheitsSpalte" to OrientierungsKnotenInspektor,
        "mathematik.einheitsZeile" to OrientierungsKnotenInspektor,
        "mathematik.kreuzproduktSpalte" to OrientierungsKnotenInspektor,
        "mathematik.kreuzproduktZeile" to OrientierungsKnotenInspektor,
        "mathematik.spaltenMethodeDifferentieren" to OrientierungsKnotenInspektor,
        "mathematik.zeilenMethodeDifferentieren" to OrientierungsKnotenInspektor,
        "mathematik.spaltenMethodeIntegrieren" to OrientierungsKnotenInspektor,
        "mathematik.zeilenMethodeIntegrieren" to OrientierungsKnotenInspektor,
        TUPEL_ERGÄNZEN_ART to TupelErgänzenInspektor,
        ZAHLENRECHNER_ART to ZahlenRechnerInspektor,
        de.TeutonStudio.MathematikRechenSystem.kern.AussagenSatzRechner.KNOTEN_ART to StrukturRechnerInspektor,
        de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner.KNOTEN_ART to StrukturRechnerInspektor,
        de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner.KNOTEN_ART to StrukturRechnerInspektor,
        de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner.KNOTEN_ART to StrukturRechnerInspektor,
        de.TeutonStudio.MathematikKnoten.TANGENTIAL_KNOTEN_ART to TangentialKnotenInspektor,
        de.TeutonStudio.MathematikKnoten.RAND_KNOTEN_ART to RandKnotenInspektor,
        de.TeutonStudio.MathematikKnoten.SVG_KNOTEN_ART to SvgKnotenInspektor,
        de.TeutonStudio.MathematikKnoten.SVG_STIL_KNOTEN_ART to SvgStilKnotenInspektor,
        GeometrieTeilobjektTyp.Ecke.knotenArt to GeometrieTeilobjektInspektor,
        GeometrieTeilobjektTyp.Kante.knotenArt to GeometrieTeilobjektInspektor,
        GeometrieTeilobjektTyp.Fläche.knotenArt to GeometrieTeilobjektInspektor,
    )
    fun finde(art: String) = inspektoren[art]
}

private object BildmengeInspektor : KnotenInspektor {
    override val hauptBereich get() = de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich.Eingangsdaten
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val modus = knoten.parameter[BILDMENGE_ARGUMENT_MODUS] ?: BILDMENGE_MODUS_PRODUKT
        val methode = ergebnis?.eingänge?.get("methode")?.objekt as? Methode

        Text("Argumentbereich", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
            FilterChip(
                selected = modus == BILDMENGE_MODUS_PRODUKT,
                onClick = { aktionen.parameter(BILDMENGE_ARGUMENT_MODUS, BILDMENGE_MODUS_PRODUKT) },
                label = { Text("Kartesisches Produkt") },
            )
            FilterChip(
                selected = modus == BILDMENGE_MODUS_EINZELMENGEN,
                onClick = { aktionen.parameter(BILDMENGE_ARGUMENT_MODUS, BILDMENGE_MODUS_EINZELMENGEN) },
                label = { Text("Je Argument") },
            )
        }

        Text(
            if (modus == BILDMENGE_MODUS_PRODUKT) {
                "Der Mengeneingang enthält vollständige Methodenargumente. Bei mehreren Argumenten sind das Tupel aus einem kartesischen Produkt."
            } else {
                "Für jedes Methodenargument wird eine eigene Menge verbunden; der Knoten bildet daraus intern das kartesische Produkt."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (modus == BILDMENGE_MODUS_EINZELMENGEN && methode != null) {
            methode.parameter.forEachIndexed { index, parameter ->
                Text(
                    "${parameter.name} ← ${bildmengeArgumentName(index)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private object OrdnungsrelationInspektor : KnotenInspektor {
    private val relationen = listOf(
        Triple("kleiner", "<", "Kleiner"),
        Triple("kleinerGleich", "≤", "Kleiner oder gleich"),
        Triple("größer", ">", "Größer"),
        Triple("größerGleich", "≥", "Größer oder gleich"),
    )

    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val aktuell = knoten.parameter["relation"] ?: "kleiner"
        Text("Relation", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng), modifier = Modifier.fillMaxWidth()) {
            relationen.forEach { (schlüssel, zeichen, standardName) ->
                FilterChip(
                    selected = aktuell == schlüssel,
                    onClick = {
                        aktionen.knoten(konfiguriereOrdnungsrelation(knoten, schlüssel))
                    },
                    label = { Text(zeichen) },
                )
            }
        }
        Text(
            "Die mathematische Definition im Knotendialog folgt der ausgewählten Relation.",
            style = MaterialTheme.typography.bodySmall,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private object VariablenInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "x") { aktionen.parameter("name", it.trim()) }
        val grundraum = ergebnis?.eingänge?.get("grundraum")?.objekt as? MengenAusdruck
        Text("Grundraum", style = MaterialTheme.typography.titleSmall)
        if (grundraum == null) {
            Text("Grundraum fehlt. Verbinde eine Menge mit dem Grundraumeingang.", style = MaterialTheme.typography.bodySmall)
        } else {
            LatexText(grundraum.zuLatex())
            Text("Struktur und Dimension werden vom Grundraum übernommen.", style = MaterialTheme.typography.bodySmall)
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object AllgemeineParameterInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "a") { aktionen.parameter("name", it.trim()) }
        val bereich = WertebereichKonfiguration.vonEigenschaft(knoten.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT])
        WertebereichEditor(bereich) { neu ->
            aktionen.eigenschaften(knoten.eigenschaften + (WertebereichKonfiguration.EIGENSCHAFT to neu.zuEigenschaft()))
        }
        Text("Der Wertebereich bestimmt die Zielmenge von Term zu Methode.", style = MaterialTheme.typography.bodySmall)
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object TermZuMethodeInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        ParameterFeld("Name", knoten.parameter["name"] ?: "f") { aktionen.parameter("name", it.trim().ifBlank { "f" }) }
        Text("Die Zielmenge wird aus dem Term und den Wertebereichen seiner Parameter abgeleitet.", style = MaterialTheme.typography.bodySmall)
        val parameter = (ergebnis?.ausgaben?.get("methode")?.objekt as? Methode)?.parameter.orEmpty()
        Text("Argumentreihenfolge", style = MaterialTheme.typography.titleSmall)
        if (parameter.isEmpty()) Text("Keine freien Variablen erkannt.", style = MaterialTheme.typography.bodySmall)
        parameter.forEachIndexed { index, variable ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
                LatexText("${index + 1}.\\; ${variable.zuLatex()}", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    val neu = parameter.map { it.name }.toMutableList().also { namen -> java.util.Collections.swap(namen, index, index - 1) }
                    aktionen.parameter("argumentReihenfolge", neu.joinToString(","))
                }, enabled = index > 0) { Text("↑") }
                OutlinedButton(onClick = {
                    val neu = parameter.map { it.name }.toMutableList().also { namen -> java.util.Collections.swap(namen, index, index + 1) }
                    aktionen.parameter("argumentReihenfolge", neu.joinToString(","))
                }, enabled = index < parameter.lastIndex) { Text("↓") }
            }
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object OrbitInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Zustandsargument", style = MaterialTheme.typography.titleSmall)
        val auswahl = orbitArgumentAuswahl(knoten, ergebnis)
        val parameter = auswahl.parameter
        val gewählt = auswahl.gewählt
        if (parameter.size != 2) {
            Text(
                "Verbinde eine mathematische Methode mit genau zwei Argumenten.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            if (auswahl.veraltet) {
                Text(
                    "Die gespeicherte Auswahl '$gewählt' gehört nicht mehr zur verbundenen Methode. Wähle ein neues Zustandsargument.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng), modifier = Modifier.fillMaxWidth()) {
                parameter.forEach { argument ->
                    FilterChip(
                        selected = argument.name == gewählt,
                        onClick = { aktionen.parameter("zustandsArgument", argument.name) },
                        label = { Text(argument.name) },
                    )
                }
            }
            Text(
                "Das andere Argument ist der Parameter der Beschränktheitsmenge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

internal data class OrbitArgumentAuswahl(
    val parameter: List<MethodenParameter>,
    val gewählt: String,
    val veraltet: Boolean,
)

internal fun orbitArgumentAuswahl(
    knoten: KnotenDaten,
    ergebnis: KnotenAuswertungsErgebnis?,
): OrbitArgumentAuswahl {
    val verbundeneMethode = ergebnis?.eingänge?.get("schritt")?.objekt as? Methode
    val ausgegebeneMethode = (ergebnis?.ausgaben?.get("orbit")?.objekt as? OrbitFamilie)?.schritt
    val parameter = listOfNotNull(verbundeneMethode, ausgegebeneMethode).firstNotNullOfOrNull { methode ->
        runCatching { methode.alsMathematischeMethode("die Orbitargument-Auswahl").parameter }.getOrNull()
    }.orEmpty()
    val gespeichert = knoten.parameter["zustandsArgument"].orEmpty()
    val gewählt = gespeichert.ifBlank { parameter.firstOrNull()?.name.orEmpty() }
    return OrbitArgumentAuswahl(
        parameter = parameter,
        gewählt = gewählt,
        veraltet = gespeichert.isNotBlank() && parameter.none { it.name == gespeichert },
    )
}

private object MethodenAufrufInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Methodenvertrag", style = MaterialTheme.typography.titleSmall)
        knoten.parameter[METHODEN_AUFRUF_VERTRAGSFEHLER]?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        val stelligkeit = knoten.parameter[METHODEN_AUFRUF_STELLIGKEIT]?.toIntOrNull()
        if (stelligkeit == null) {
            Text(
                "Noch kein konkreter Methodenvertrag erkannt. Die allgemeinen Argumentanschlüsse bleiben erweiterbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Stelligkeit: $stelligkeit", style = MaterialTheme.typography.bodyMedium)
            if (stelligkeit == 0) {
                Text("Die Methode besitzt keine Argumente.", style = MaterialTheme.typography.bodySmall)
            }
            repeat(stelligkeit) { index ->
                val präfix = "$METHODEN_AUFRUF_PARAMETER_PREFIX$index."
                val name = knoten.parameter["${präfix}name"] ?: "Argument ${index + 1}"
                val artId = AnschlussArtId(knoten.parameter["${präfix}art"].orEmpty())
                val art = MathematikAnschlussArten.alle.firstOrNull { it.id == artId }?.name
                    ?: artId.wert.ifBlank { "Mathematisches Objekt" }
                val werteVorrat = knoten.parameter["${präfix}werteVorrat"]
                Text(
                    buildString {
                        append("${index + 1}. $name: $art")
                        if (!werteVorrat.isNullOrBlank()) append(" ∈ $werteVorrat")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            knoten.parameter[METHODEN_AUFRUF_ZIELMENGE]?.let { zielMenge ->
                Text("Zielmenge: $zielMenge", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Stelligkeit, Anschlussarten und Zielmenge werden aus der verbundenen Methode übernommen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object TransponierenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Transposition", style = MaterialTheme.typography.titleSmall)
        val tensor = ergebnis?.ausgaben?.get("wert")?.objekt as? Tensor
        val rang = tensor?.rang
        when {
            rang == null -> Text(
                "Vektoren und Matrizen benötigen keine Konfiguration. Bei einem Tensor höheren Rangs erscheint hier seine Achsenpermutation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rang <= 2 -> {
                val standard = standardTensorPermutation(rang).joinToString(",")
                LaunchedEffect(knoten.id, rang, knoten.parameter["achsenPermutation"]) {
                    if (knoten.parameter["achsenPermutation"] != standard) {
                        aktionen.parameter("achsenPermutation", standard)
                    }
                }
                Text(
                    "Tensor Rang $rang verwendet die kanonische Transposition [$standard].",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> TensorPermutationEditor(knoten, rang, aktionen)
        }
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TensorPermutationEditor(
    knoten: KnotenDaten,
    rang: Int,
    aktionen: KnotenInspektorAktionen,
) {
    val standard = standardTensorPermutation(rang)
    val standardText = standard.joinToString(",")
    val gespeichert = knoten.parameter["achsenPermutation"]
    val gespeichertGültig = parseTensorPermutationOderNull(gespeichert, rang)
    var text by remember(knoten.id, rang) {
        mutableStateOf((gespeichertGültig ?: standard).joinToString(","))
    }
    var zurückgesetzt by remember(knoten.id, rang) { mutableStateOf(false) }

    LaunchedEffect(knoten.id, rang, gespeichert) {
        val gültig = parseTensorPermutationOderNull(gespeichert, rang)
        if (gültig == null) {
            text = standardText
            zurückgesetzt = true
            aktionen.parameter("achsenPermutation", standardText)
        } else {
            text = gültig.joinToString(",")
        }
    }

    val eingabeGültig = parseTensorPermutationOderNull(text, rang) != null
    OutlinedTextField(
        value = text,
        onValueChange = { neu ->
            text = neu
            parseTensorPermutationOderNull(neu, rang)?.let { permutation ->
                zurückgesetzt = false
                aktionen.parameter("achsenPermutation", permutation.joinToString(","))
            }
        },
        label = { Text("Achsenpermutation") },
        isError = !eingabeGültig,
        supportingText = {
            Text(
                when {
                    !eingabeGültig -> "Jede Achse von 0 bis ${rang - 1} muss genau einmal vorkommen."
                    zurückgesetzt -> "Eine veraltete Permutation wurde auf den Standard [$standardText] zurückgesetzt."
                    else -> "Eintrag i gibt an, welche alte Achse zur neuen Achse i wird."
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = {
            text = standardText
            zurückgesetzt = false
            aktionen.parameter("achsenPermutation", standardText)
        },
    ) { Text("Standardpermutation") }
}

@Composable private fun WertebereichEditor(
    bereich: WertebereichKonfiguration,
    ändern: (WertebereichKonfiguration) -> Unit,
) {
    Text("Wertebereich", style = MaterialTheme.typography.titleSmall)
    val arten = listOf(
        "Zahl" to WertebereichKonfiguration.Zahl(),
        "Aussage" to WertebereichKonfiguration.Aussage,
        "Menge" to WertebereichKonfiguration.Menge(),
        "Tupel" to WertebereichKonfiguration.Tupel(),
        "Spalte" to WertebereichKonfiguration.Vektor(),
        "Zeile" to WertebereichKonfiguration.Vektor(orientierung = VektorOrientierung.Zeile),
        "Matrix" to WertebereichKonfiguration.Matrix(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng), modifier = Modifier.fillMaxWidth()) {
        arten.take(3).forEach { (name, wert) -> FilterChip(bereich::class == wert::class, { ändern(wert) }, label = { Text(name) }) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng), modifier = Modifier.fillMaxWidth()) {
        arten.drop(3).forEach { (name, wert) -> FilterChip(bereich::class == wert::class && (bereich !is WertebereichKonfiguration.Vektor || wert !is WertebereichKonfiguration.Vektor || bereich.orientierung == wert.orientierung), { ändern(wert) }, label = { Text(name) }) }
    }
    TextButton(onClick = { ändern(WertebereichKonfiguration.Winkel) }) { Text(if (bereich == WertebereichKonfiguration.Winkel) "✓ Winkel (Gradmenge)" else "Winkel (Gradmenge)") }
    when (bereich) {
        WertebereichKonfiguration.Winkel -> LatexText(de.TeutonStudio.MathematikRechenSystem.kern.Gradmenge.zuLatex())
        is WertebereichKonfiguration.Zahl -> GrundmengenAuswahl("Zahlgrundmenge", bereich.grundmenge) { ändern(bereich.copy(grundmenge = it)) }
        WertebereichKonfiguration.Aussage -> Text("{⊤, ⊥}", style = MaterialTheme.typography.bodySmall)
        is WertebereichKonfiguration.Menge -> {
            Text("Elementbereich", style = MaterialTheme.typography.bodySmall)
            WertebereichEditor(bereich.elementBereich) { ändern(bereich.copy(elementBereich = it)) }
        }
        is WertebereichKonfiguration.Tupel -> {
            bereich.komponenten.forEachIndexed { index, komponente ->
                Text("Komponente ${index + 1}", style = MaterialTheme.typography.bodySmall)
                WertebereichEditor(komponente) { neu -> ändern(bereich.copy(komponenten = bereich.komponenten.mapIndexed { i, alt -> if (i == index) neu else alt })) }
            }
            OutlinedButton(onClick = { ändern(bereich.copy(komponenten = bereich.komponenten + WertebereichKonfiguration.Zahl())) }) { Text("Komponente hinzufügen") }
        }
        is WertebereichKonfiguration.Vektor -> {
            PositiveGanzzahlFeld("Dimension", bereich.dimension) { ändern(bereich.copy(dimension = it)) }
            GrundmengenAuswahl("Skalarmenge", bereich.skalarMenge) { ändern(bereich.copy(skalarMenge = it)) }
        }
        is WertebereichKonfiguration.Matrix -> {
            PositiveGanzzahlFeld("Zeilen", bereich.zeilen) { ändern(bereich.copy(zeilen = it)) }
            PositiveGanzzahlFeld("Spalten", bereich.spalten) { ändern(bereich.copy(spalten = it)) }
            GrundmengenAuswahl("Skalarmenge", bereich.skalarMenge) { ändern(bereich.copy(skalarMenge = it)) }
        }
    }
}

@Composable private fun PositiveGanzzahlFeld(label: String, wert: Int, ändern: (Int) -> Unit) {
    var text by remember(label, wert) { mutableStateOf(wert.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { neu ->
            text = neu
            neu.toIntOrNull()?.takeIf { it > 0 }?.let(ändern)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable private fun GrundmengenAuswahl(label: String, aktuell: String, ändern: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.eng)) {
        listOf("N", "Z", "Q", "R", "C").forEach { menge ->
            FilterChip(aktuell == menge, { ändern(menge) }, label = { Text(menge) })
        }
    }
}

private object KartenSchnittstellenInspektor : KnotenInspektor {
    override fun hatBereich(knoten: KnotenDaten, bereich: de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich) = false
    @Composable override fun AnschlussInhalt(knoten: KnotenDaten, anschluss: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten,
        ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        if (anschluss.name == "wert") Inhalt(knoten, ergebnis, aktionen)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val wertAnschluss = knoten.anschlüsse.firstOrNull { it.name == "wert" } ?: return
        val aktuelleArt = MathematikAnschlussArten.alle.firstOrNull { it.id == wertAnschluss.art }
        var geöffnet by remember(knoten.id, wertAnschluss.art) { mutableStateOf(false) }
        ParameterFeld("Name", knoten.parameter["name"] ?: knoten.name) { aktionen.parameter("name", it) }
        Text("Schnittstellen-Typ", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = geöffnet, onExpandedChange = { geöffnet = it }) {
            OutlinedTextField(
                value = aktuelleArt?.name ?: wertAnschluss.art.wert,
                onValueChange = {},
                readOnly = true,
                label = { Text("Typ") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                MathematikAnschlussArten.alle.forEach { art ->
                    DropdownMenuItem(
                        text = { Text(art.name) },
                        onClick = {
                            geöffnet = false
                            aktionen.anschlussArt(AnschlussVerweis(knoten.id, wertAnschluss.id), art.id)
                        },
                    )
                }
            }
        }
        Text(
            "Der Typ gilt für den Anschluss „wert“ und wird von Gruppenknoten übernommen.",
            style = MaterialTheme.typography.bodySmall,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object LösungsmengeInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val automatisch = knoten.parameter["automatisch"]?.toBooleanStrictOrNull() ?: true
        Text("Variablen", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Freie Variablen automatisch übernehmen", modifier = Modifier.weight(1f))
            Switch(automatisch, onCheckedChange = { aktionen.parameter("automatisch", it.toString()) })
        }
        if (!automatisch) ParameterFeld("Variablen (geordnet, mit Komma)", knoten.parameter["variablen"].orEmpty()) { aktionen.parameter("variablen", it) }
        ParameterFeld("Grundmengen (N, Z, Q, R)", knoten.parameter["grundmengen"] ?: "R") { aktionen.parameter("grundmengen", it) }
        Text("Eine Grundmenge gilt für alle Variablen; mehrere folgen der Variablenreihenfolge.", style = MaterialTheme.typography.bodySmall)
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private object VisualisierungsInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val c = VisualisierungsKonfiguration.aus(knoten.eigenschaften)
        val menge = ergebnis?.eingänge?.get("menge")?.objekt as? MengenAusdruck
        de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsEinstellungen(c, c.mitWirksamerDimension(menge).dimension,
            { aktionen.eigenschaften(it.inEigenschaften(knoten.eigenschaften)) }, ebenenNamen = knoten.anschlüsse.filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.map { it.name })

    }
}

@Composable private fun ParameterFeld(label: String, wert: String, ändern: (String) -> Unit) { var text by remember(label, wert) { mutableStateOf(wert) }; OutlinedTextField(text, { text = it; ändern(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
internal fun parseZahlenBereich(text: String): ZahlenBereich? {
    val teile = text.split(',').map(String::trim)
    if (teile.size != 2) return null
    val minimum = teile[0].toDoubleOrNull() ?: return null
    val maximum = teile[1].toDoubleOrNull() ?: return null
    if (!minimum.isFinite() || !maximum.isFinite() || minimum >= maximum) return null
    if (!(maximum - minimum).isFinite()) return null
    return runCatching { ZahlenBereich(minimum, maximum) }.getOrNull()
}

@Composable
private fun BereichFeld(label: String, bereich: ZahlenBereich, ändern: (ZahlenBereich) -> Unit) {
    var text by remember(label, bereich) { mutableStateOf("${bereich.minimum}, ${bereich.maximum}") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            parseZahlenBereich(it)?.let(ändern)
        },
        label = { Text("$label: Minimum, Maximum") },
        modifier = Modifier.fillMaxWidth(),
    )
}
