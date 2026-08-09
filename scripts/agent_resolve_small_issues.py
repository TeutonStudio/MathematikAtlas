from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: erwartete genau 1 Fundstelle, gefunden {count}: {old[:90]!r}")
    write(path, text.replace(old, new, 1))


# #361: Aussage zu Prädikat, stabile Art beibehalten.
vorlagen = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt"
replace_once(
    vorlagen,
    '"mathematik.termZuMethode", "Aussage zu Methode", "Methoden", "Erzeugt aus einer Aussage eine typisierte Aussagenmethode."',
    '"mathematik.termZuMethode", "Aussage zu Prädikat", "Methoden", "Erzeugt aus einer Aussage ein typisiertes Prädikat."',
)

konzept = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/konzeptknoten/TermZuMethodeKonzeptDateiEF3062BD.kt"
text = read(konzept)
text = text.replace(
    'VariantenId("mathematik.termZuMethode|argumentReihenfolge=|name=P|Aussage zu Methode")',
    'VariantenId("mathematik.termZuMethode|argumentReihenfolge=|name=P|Aussage zu Prädikat")',
)
text = text.replace(
    '"Erzeugt aus einer Aussage eine typisierte Aussagenmethode."',
    '"Erzeugt aus einer Aussage ein typisiertes Prädikat."',
)
text = text.replace(
    '"mathematik.termZuMethode|Aussage zu Methode|argumentReihenfolge=;name=P"',
    '"mathematik.termZuMethode|Aussage zu Prädikat|argumentReihenfolge=;name=P", "mathematik.termZuMethode|Aussage zu Methode|argumentReihenfolge=;name=P"',
)
# Der alte sichtbare Begriff bleibt Suchalias.
if '"Aussage zu Methode"' not in text:
    text = text.replace('suchbegriffe = setOf(', 'suchbegriffe = setOf("Aussage zu Methode", ', 1)
write(konzept, text)

# Statische Karten referenzieren die neue sichtbare Variante. Historischer Alias lebt im Wissenseintrag.
static_index = ROOT / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/konzeptkarte/StatischeKonzeptKarten.kt"
if static_index.exists():
    static_index.write_text(static_index.read_text(encoding="utf-8").replace("Aussage zu Methode", "Aussage zu Prädikat"), encoding="utf-8")
for asset in (ROOT / "MathematikKnoten/src/main/assets/de/TeutonStudio/MathematikKnoten/konzeptkarte").glob("*.json"):
    raw = asset.read_text(encoding="utf-8")
    if "Aussage zu Methode" in raw:
        asset.write_text(raw.replace("Aussage zu Methode", "Aussage zu Prädikat"), encoding="utf-8")

write(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/migration/PraedikatNamensMigration.kt",
    r'''package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

/** Migriert ausschließlich den historischen Standardnamen der Prädikatvariante. */
fun KartenDaten.migrierePraedikatStandardname(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        val istPraedikatVariante = knoten.art == "mathematik.termZuMethode" &&
            knoten.anschlüsse.any {
                it.richtung == AnschlussRichtung.Eingang &&
                    it.name == "term" &&
                    it.art == MathematikAnschlussArten.Aussage.id
            } &&
            knoten.anschlüsse.any {
                it.richtung == AnschlussRichtung.Ausgang &&
                    it.name == "methode" &&
                    it.art == MathematikAnschlussArten.AussageMethode.id
            }
        if (istPraedikatVariante && knoten.name == "Aussage zu Methode") {
            knoten.copy(name = "Aussage zu Prädikat")
        } else knoten
    },
)
''',
)
replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/migration/MathematikKartenCodec.kt",
    '''fun nachLaden(karte: KartenDaten): KartenDaten = karte\n        .migriereMethodenAnschlüsse()''',
    '''fun nachLaden(karte: KartenDaten): KartenDaten = karte\n        .migrierePraedikatStandardname()\n        .migriereMethodenAnschlüsse()''',
)

# #288: wiederverwendbares, vollständig theme-basiertes Fachgebietsicon.
write(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/FachgebietsIcons.kt",
    r'''package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

internal data class FachgebietsIconFarben(
    val hintergrundStart: Color,
    val hintergrundEnde: Color,
    val innenflaeche: Color,
    val mengeLinks: Color,
    val mengeRechts: Color,
    val schnitt: Color,
    val kontur: Color,
    val element: Color,
)

@Composable
private fun fachgebietsIconFarben() = FachgebietsIconFarben(
    hintergrundStart = MaterialTheme.colorScheme.primaryContainer,
    hintergrundEnde = MaterialTheme.colorScheme.secondaryContainer,
    innenflaeche = MaterialTheme.colorScheme.surface,
    mengeLinks = MaterialTheme.colorScheme.primary,
    mengeRechts = MaterialTheme.colorScheme.secondary,
    schnitt = MaterialTheme.colorScheme.tertiary,
    kontur = MaterialTheme.colorScheme.onSurfaceVariant,
    element = MaterialTheme.colorScheme.onPrimaryContainer,
)

internal fun hatFachgebietsIcon(id: String): Boolean = id == "mengenlehre"

@Composable
internal fun FachgebietsIcon(
    id: String,
    modifier: Modifier = Modifier,
) {
    if (id != "mengenlehre") return
    val farben = fachgebietsIconFarben()
    Canvas(modifier) {
        val min = size.minDimension
        val rand = min * 0.07f
        val radius = min * 0.12f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(farben.hintergrundStart, farben.hintergrundEnde),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = farben.innenflaeche.copy(alpha = .88f),
            topLeft = Offset(rand, rand),
            size = Size(size.width - 2 * rand, size.height - 2 * rand),
            cornerRadius = CornerRadius(radius * .72f, radius * .72f),
        )

        val kreisRadius = min * .255f
        val links = Offset(size.width * .39f, size.height * .50f)
        val rechts = Offset(size.width * .61f, size.height * .50f)
        drawCircle(farben.mengeLinks.copy(alpha = .36f), kreisRadius, links)
        drawCircle(farben.mengeRechts.copy(alpha = .36f), kreisRadius, rechts)
        // Die Schnittlinse erhält eine eigene Theme-Rolle. Die Geometrie bleibt bewusst symmetrisch.
        drawOval(
            color = farben.schnitt.copy(alpha = .50f),
            topLeft = Offset(size.width * .43f, size.height * .31f),
            size = Size(size.width * .14f, size.height * .38f),
        )
        drawCircle(farben.kontur, kreisRadius, links, style = Stroke(min * .018f))
        drawCircle(farben.kontur, kreisRadius, rechts, style = Stroke(min * .018f))

        val punktRadius = min * .026f
        listOf(
            Offset(size.width * .27f, size.height * .43f),
            Offset(size.width * .34f, size.height * .58f),
            Offset(size.width * .50f, size.height * .49f),
            Offset(size.width * .66f, size.height * .39f),
            Offset(size.width * .72f, size.height * .59f),
        ).forEach { drawCircle(farben.element, punktRadius, it) }
    }
}
''',
)
ui = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KonzeptBibliothekUi.kt"
old = '''                Text(\n                    kategorieSymbol(kategorie.id),\n                    style = MaterialTheme.typography.displaySmall,\n                    color = MaterialTheme.colorScheme.primary,\n                    textAlign = TextAlign.Center,\n                )'''
new = '''                if (hatFachgebietsIcon(kategorie.id)) {\n                    FachgebietsIcon(\n                        id = kategorie.id,\n                        modifier = Modifier.fillMaxWidth(.72f).aspectRatio(1f),\n                    )\n                } else {\n                    Text(\n                        kategorieSymbol(kategorie.id),\n                        style = MaterialTheme.typography.displaySmall,\n                        color = MaterialTheme.colorScheme.primary,\n                        textAlign = TextAlign.Center,\n                    )\n                }'''
if old not in read(ui):
    # toleranter Fallback für leicht anders eingerückte aktuelle Datei
    pattern = re.compile(r'Text\(\s*kategorieSymbol\(kategorie\.id\),\s*style = MaterialTheme\.typography\.displaySmall,\s*color = MaterialTheme\.colorScheme\.primary,\s*textAlign = TextAlign\.Center,\s*\)', re.S)
    source = read(ui)
    source, n = pattern.subn(new.strip(), source, count=1)
    if n != 1:
        raise RuntimeError("KonzeptBibliothekUi.kt: Hauptkategorie-Symbol nicht eindeutig gefunden")
    write(ui, source)
else:
    replace_once(ui, old, new)

# #354: Definierte Mengen in R1 nicht am exakten Zahlengeradenpfad vorbeischicken.
sampler = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/visualisierung/sampling/VisualisierungsSampler.kt"
replace_once(
    sampler,
    '''        if (konfiguration.dimension == RaumDimension.R1) {\n            return ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)\n        }''',
    '''        if (konfiguration.dimension == RaumDimension.R1 && menge is DefinierteMenge) {\n            return normalisiereDefinierteMenge(menge, konfiguration)\n        }\n        if (konfiguration.dimension == RaumDimension.R1) {\n            return ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)\n        }''',
)

# #358: instanzabhängige Definitionskarte für endliche Mengen.
write(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/EndlicheMengeKonzept.kt",
    r'''package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

internal fun endlicheMengeKonzept(ursprung: KnotenDaten): KonzeptDefinition {
    val gelesen = leseEndlicheMengeKonfiguration(ursprung)
    if (gelesen.fehler != null) return endlicheMengeFehlerKonzept(ursprung, gelesen.fehler)
    val normalisiert = normalisiereEndlicheMengeKonfiguration(gelesen.konfiguration)
    return KonzeptDefinition(
        id = KonzeptId("endliche-menge-${ursprung.id.wert}"),
        name = "Endliche Menge",
        beschreibung = "Konstruiert die konfigurierte endliche Menge als Vereinigung ihrer Einzelmengen.",
        pfad = listOf("Mengenlehre", "Mengen"),
        tags = setOf("Endliche Menge", "Einzelmenge", "Vereinigung") + normalisiert.warnungen,
        knotenArten = setOf("mathematik.endlicheMenge"),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = endlicheMengeDefinitionsKarte(ursprung, normalisiert.konfiguration),
            ),
        ),
    )
}

private fun endlicheMengeDefinitionsKarte(
    ursprung: KnotenDaten,
    konfiguration: EndlicheMengeKonfiguration,
): KartenDaten {
    val prefix = "definition-endliche-menge-${ursprung.id.wert}"
    val eintraege = konfiguration.einträge
    val knoten = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()

    val ausgangId = KnotenId("$prefix-ausgang")
    val ausgangAnschluss = AnschlussId("$prefix-ausgang-menge")
    val ausgang = KnotenDaten(
        id = ausgangId,
        art = KonzeptKnotenArten.AUSGANG,
        name = "Menge",
        position = GraphPunkt(if (eintraege.size <= 1) 650f else 980f, 100f),
        größe = GraphGröße(210f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = ausgangAnschluss,
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
        parameter = mapOf("typ" to MathematikAnschlussArten.Menge.id.wert, "rolle" to "menge"),
    )

    if (eintraege.isEmpty()) {
        val leerId = KnotenId("$prefix-leer")
        val leerAusgang = AnschlussId("$prefix-leer-menge")
        knoten += KnotenDaten(
            id = leerId,
            art = "mathematik.leereMenge",
            name = "Leere Menge",
            position = GraphPunkt(320f, 100f),
            größe = GraphGröße(210f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = leerAusgang,
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                ),
            ),
        )
        knoten += ausgang
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-leer-ausgang"),
            von = AnschlussVerweis(leerId, leerAusgang),
            zu = AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
        return KartenDaten(KartenId(prefix), "Definition: Endliche Menge", knoten = knoten, verbindungen = verbindungen)
    }

    val einzelAusgaenge = mutableListOf<Pair<KnotenId, AnschlussId>>()
    eintraege.forEachIndexed { index, eintrag ->
        val safe = eintrag.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val y = 45f + index * 145f
        val inputId = KnotenId("$prefix-$safe-eingang")
        val inputOut = AnschlussId("$prefix-$safe-eingang-wert")
        val art = AnschlussArtId(eintrag.art)
        knoten += KnotenDaten(
            id = inputId,
            art = KonzeptKnotenArten.EINGANG,
            name = "Element ${index + 1}",
            position = GraphPunkt(35f, y),
            größe = GraphGröße(210f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(inputOut, "wert", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, art),
            ),
            parameter = mapOf("typ" to art.wert, "rolle" to "element-${eintrag.id}"),
        )

        val einzelId = KnotenId("$prefix-$safe-einzelmenge")
        val einzelIn = AnschlussId("$prefix-$safe-einzelmenge-element")
        val einzelOut = AnschlussId("$prefix-$safe-einzelmenge-menge")
        knoten += KnotenDaten(
            id = einzelId,
            art = "mathematik.einzelmenge",
            name = "Einzelmenge",
            position = GraphPunkt(330f, y),
            größe = GraphGröße(220f, 105f),
            anschlüsse = listOf(
                AnschlussDaten(einzelIn, "element", AnschlussRichtung.Eingang, AnschlussKante.Links, MathematikAnschlussArten.Objekt.id),
                AnschlussDaten(einzelOut, "menge", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, MathematikAnschlussArten.Menge.id),
            ),
        )
        verbindungen += VerbindungDaten(
            VerbindungsId("$prefix-$safe-element"),
            AnschlussVerweis(inputId, inputOut),
            AnschlussVerweis(einzelId, einzelIn),
        )
        einzelAusgaenge += einzelId to einzelOut
    }

    if (einzelAusgaenge.size == 1) {
        knoten += ausgang
        verbindungen += VerbindungDaten(
            VerbindungsId("$prefix-einzel-ausgang"),
            AnschlussVerweis(einzelAusgaenge.single().first, einzelAusgaenge.single().second),
            AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
    } else {
        val unionId = KnotenId("$prefix-vereinigung")
        val unionOut = AnschlussId("$prefix-vereinigung-menge")
        val unionInputs = einzelAusgaenge.indices.map { index ->
            AnschlussDaten(
                id = AnschlussId("$prefix-vereinigung-${index + 1}"),
                name = if (index == 0) "a" else if (index == 1) "b" else "input${index + 1}",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
                reihenfolge = index,
                kannSichErweitern = true,
            )
        }
        knoten += KnotenDaten(
            id = unionId,
            art = "mathematik.vereinigung",
            name = "Vereinigung",
            position = GraphPunkt(650f, 45f + ((einzelAusgaenge.size - 1) * 72f)),
            größe = GraphGröße(250f, 96f + einzelAusgaenge.size * 24f),
            anschlüsse = unionInputs + AnschlussDaten(
                unionOut, "menge", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, MathematikAnschlussArten.Menge.id,
            ),
            parameter = mapOf("festeEingänge" to einzelAusgaenge.size.toString(), "operatorAnzeige" to "wert"),
        )
        einzelAusgaenge.forEachIndexed { index, quelle ->
            verbindungen += VerbindungDaten(
                VerbindungsId("$prefix-vereinigung-kante-${index + 1}"),
                AnschlussVerweis(quelle.first, quelle.second),
                AnschlussVerweis(unionId, unionInputs[index].id),
            )
        }
        knoten += ausgang
        verbindungen += VerbindungDaten(
            VerbindungsId("$prefix-vereinigung-ausgang"),
            AnschlussVerweis(unionId, unionOut),
            AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: Endliche Menge",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun endlicheMengeFehlerKonzept(ursprung: KnotenDaten, fehler: String): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("endliche-menge-fehler-${ursprung.id.wert}"),
    name = "Endliche Menge",
    beschreibung = "Die gespeicherte Elementkonfiguration ist beschädigt.",
    pfad = listOf("Mengenlehre", "Mengen"),
    tags = setOf("Endliche Menge", "Fehler"),
    knotenArten = setOf("mathematik.endlicheMenge"),
    reiter = listOf(
        KonzeptReiter(
            id = "fehler",
            titel = "Konfigurationsfehler",
            rolle = KonzeptReiterRolle.Definition,
            karte = KartenDaten(
                id = KartenId("definition-endliche-menge-fehler-${ursprung.id.wert}"),
                name = "Fehler: Endliche Menge",
                knoten = listOf(
                    KnotenDaten(
                        id = KnotenId("definition-endliche-menge-fehler-${ursprung.id.wert}-regel"),
                        art = KonzeptKnotenArten.REGEL,
                        name = "Ungültige Elementkonfiguration",
                        position = GraphPunkt(60f, 60f),
                        größe = GraphGröße(620f, 190f),
                        parameter = mapOf("regel" to fehler, "knotenArt" to "mathematik.endlicheMenge"),
                    ),
                ),
            ),
        ),
    ),
)
''',
)
replace_once(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/KonsolidierteKnotenKonzepte.kt",
    '''internal fun dynamischesKonzeptFürKnoten(zustand: AtlasZustand, knoten: KnotenDaten): KonzeptDefinition? {\n    if (knoten.art == MENGEN_KNOTEN_ART) {''',
    '''internal fun dynamischesKonzeptFürKnoten(zustand: AtlasZustand, knoten: KnotenDaten): KonzeptDefinition? {\n    if (knoten.art == "mathematik.endlicheMenge") return endlicheMengeKonzept(knoten)\n    if (knoten.art == MENGEN_KNOTEN_ART) {''',
)

# #168: gemeinsame Viewport-Transformation plus ganzzahlige Achsenindexierung.
geo = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/geometrie/GeometrieVisualisierungsKnotenRenderer.kt"
source = read(geo)
source = source.replace('import androidx.compose.ui.unit.dp\n', 'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.ui.text.TextMeasurer\nimport androidx.compose.ui.text.TextStyle\nimport androidx.compose.ui.text.drawText\nimport androidx.compose.ui.text.rememberTextMeasurer\n')
source = source.replace('''    Canvas(modifier.pointerInput(objekt.raum.dimension, kamera) {\n        detectTransformGestures { _, pan, zoom, _ ->\n            onKamera(\n                if (objekt.raum.dimension == 3) kamera.copy(\n                    rotationY = kamera.rotationY + pan.x * 0.45,\n                    rotationX = kamera.rotationX + pan.y * 0.45,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ) else kamera.copy(\n                    verschiebungX = kamera.verschiebungX + pan.x,\n                    verschiebungY = kamera.verschiebungY + pan.y,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ),\n            )\n        }\n    }) {\n        drawRect(hintergrund)\n        zeichneAchsen(objekt.raum.dimension, kamera, achsenFarbe)''', '''    val textMeasurer = rememberTextMeasurer()\n    val beschriftungsFarbe = MaterialTheme.colorScheme.onSurfaceVariant\n    Canvas(modifier.pointerInput(objekt.raum.dimension, kamera) {\n        detectTransformGestures { _, pan, zoom, rotation ->\n            onKamera(\n                kamera.copy(\n                    rotationY = if (objekt.raum.dimension == 3) kamera.rotationY + rotation.toDouble() else kamera.rotationY,\n                    verschiebungX = kamera.verschiebungX + pan.x,\n                    verschiebungY = kamera.verschiebungY + pan.y,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ),\n            )\n        }\n    }) {\n        drawRect(hintergrund)\n        zeichneAchsen(objekt.raum.dimension, kamera, achsenFarbe, beschriftungsFarbe, textMeasurer)''')
old_axis = re.compile(r'''private fun DrawScope\.zeichneAchsen\(dimension: Int, kamera: GeometrieKamera, farbe: Color\) \{.*?\n\}\n\nprivate fun renderDaten''', re.S)
new_axis = r'''internal fun geometrieGanzzahlSchritt(pixelProEinheit: Double): Int {
    if (!pixelProEinheit.isFinite() || pixelProEinheit <= 0.0) return 1
    val ziel = max(1.0, 52.0 / pixelProEinheit)
    val basis = 10.0.pow(floor(log10(ziel)))
    val norm = ziel / basis
    val faktor = when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 5.0 -> 5.0
        else -> 10.0
    }
    return max(1, (basis * faktor).roundToInt())
}

private fun erstesVielfaches(minimum: Double, schritt: Int): Int =
    (ceil(minimum / schritt.toDouble()) * schritt).toInt()

private fun DrawScope.zeichneAchsen(
    dimension: Int,
    kamera: GeometrieKamera,
    farbe: Color,
    textFarbe: Color,
    textMeasurer: TextMeasurer,
) {
    val nullpunkt = projekt(List(dimension) { 0.0 }, size.width, size.height, kamera)
    drawLine(farbe, Offset(0f, nullpunkt.y), Offset(size.width, nullpunkt.y), 1f)
    if (dimension >= 2) drawLine(farbe, Offset(nullpunkt.x, 0f), Offset(nullpunkt.x, size.height), 1f)
    if (dimension == 3) {
        val z0 = projekt(listOf(0.0, 0.0, -10.0), size.width, size.height, kamera)
        val z1 = projekt(listOf(0.0, 0.0, 10.0), size.width, size.height, kamera)
        drawLine(farbe, z0, z1, 1f)
        return
    }

    val s = skala(size.width, size.height, kamera)
    val schritt = geometrieGanzzahlSchritt(s)
    val minX = (0.0 - size.width / 2.0 - kamera.verschiebungX) / s
    val maxX = (size.width - size.width / 2.0 - kamera.verschiebungX) / s
    var x = erstesVielfaches(minX, schritt)
    var budget = 0
    while (x <= maxX && budget++ < 256) {
        val p = projekt(listOf(x.toDouble(), 0.0), size.width, size.height, kamera)
        drawLine(farbe, Offset(p.x, nullpunkt.y - 4f), Offset(p.x, nullpunkt.y + 4f), 1f)
        if (x != 0) {
            drawText(
                textMeasurer = textMeasurer,
                text = x.toString(),
                topLeft = Offset(p.x + 3f, nullpunkt.y + 5f),
                style = TextStyle(color = textFarbe, fontSize = 10.sp),
            )
        }
        x += schritt
    }

    if (dimension >= 2) {
        val minY = -(size.height - size.height / 2.0 - kamera.verschiebungY) / s
        val maxY = -(0.0 - size.height / 2.0 - kamera.verschiebungY) / s
        var y = erstesVielfaches(minY, schritt)
        budget = 0
        while (y <= maxY && budget++ < 256) {
            val p = projekt(listOf(0.0, y.toDouble()), size.width, size.height, kamera)
            drawLine(farbe, Offset(nullpunkt.x - 4f, p.y), Offset(nullpunkt.x + 4f, p.y), 1f)
            if (y != 0) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = y.toString(),
                    topLeft = Offset(nullpunkt.x + 5f, p.y + 2f),
                    style = TextStyle(color = textFarbe, fontSize = 10.sp),
                )
            }
            y += schritt
        }
    }
    if (nullpunkt.x in 0f..size.width && nullpunkt.y in 0f..size.height) {
        drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(nullpunkt.x + 4f, nullpunkt.y + 5f),
            style = TextStyle(color = textFarbe, fontSize = 10.sp),
        )
    }
}

private fun renderDaten'''
source, n = old_axis.subn(new_axis, source, count=1)
if n != 1:
    raise RuntimeError("Geometrie-Achsenfunktion konnte nicht ersetzt werden")
write(geo, source)

# #197: explizite Exportformatwahl; .matlas bleibt bis zum Writer aus #198 deaktiviert.
write(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenExportFormatDialog.kt",
    r'''package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class KartenExportFormat(
    val anzeigeName: String,
    val endung: String,
    val beschreibung: String,
    val verfügbar: Boolean,
) {
    JSON("JSON", ".json", "Rohe, kompatible Karten-JSON für Bearbeitung und Debugging.", true),
    MATLAS(".matlas", ".matlas", "Versioniertes Kartenpaket. Wird mit dem Container-Writer aus #198 aktiviert.", false),
}

internal fun normalisiereExportDateiname(name: String, format: KartenExportFormat): String {
    val basis = name.trim().ifBlank { "Karte" }
        .removeSuffix(".json")
        .removeSuffix(".matlas")
    return basis + format.endung
}

@Composable
internal fun KartenExportFormatDialog(
    schließen: () -> Unit,
    exportieren: (KartenExportFormat) -> Unit,
) {
    var format by remember { mutableStateOf(KartenExportFormat.JSON) }
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Karte exportieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KartenExportFormat.entries.forEach { kandidat ->
                    Surface(
                        onClick = { if (kandidat.verfügbar) format = kandidat },
                        enabled = kandidat.verfügbar,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = if (format == kandidat) 3.dp else 0.dp,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadioButton(
                                selected = format == kandidat,
                                onClick = { if (kandidat.verfügbar) format = kandidat },
                                enabled = kandidat.verfügbar,
                            )
                            Column {
                                Text(kandidat.anzeigeName, style = MaterialTheme.typography.titleSmall)
                                Text(kandidat.beschreibung, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { exportieren(format) }, enabled = format.verfügbar) { Text("Exportieren") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}
''',
)
app = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/MathematikAtlasApp.kt"
replace_once(
    app,
    '''    var graphKontext by remember { mutableStateOf<GraphKontext?>(null) }''',
    '''    var exportDialogOffen by remember { mutableStateOf(false) }\n    var graphKontext by remember { mutableStateOf<GraphKontext?>(null) }''',
)
replace_once(
    app,
    '''    Row(\n        Modifier.fillMaxSize()''',
    '''    if (exportDialogOffen) {\n        KartenExportFormatDialog(\n            schließen = { exportDialogOffen = false },\n            exportieren = { format ->\n                exportDialogOffen = false\n                if (format == KartenExportFormat.JSON) {\n                    export.launch(normalisiereExportDateiname(zustand.editor.karte.name, format))\n                }\n            },\n        )\n    }\n\n    Row(\n        Modifier.fillMaxSize()''',
)
replace_once(
    app,
    '''                onExport = { export.launch("${zustand.editor.karte.name}.json") },''',
    '''                onExport = { exportDialogOffen = true },''',
)

# #182/#183: Tupelvariable mit stabilen Komponentenidentitäten. Die bestehende Methodenbildung
# destrukturiert deren freie Komponenten und rekonstruiert das Tupel in der Vorschrift automatisch.
write(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/TupelVariableKnoten.kt",
    r'''package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TUPEL_VARIABLE_ART = "mathematik.tupelvariable"

object TupelVariableKnotenVorlagen {
    val standard = KnotenVorlage(
        TUPEL_VARIABLE_ART,
        "Tupelvariable",
        "Tupel",
        "Erzeugt ein homogenes symbolisches Tupel mit stabilen Komponentenidentitäten.",
        GraphGröße(245f, 110f),
        listOf(
            AnschlussDaten(
                name = "tupel",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            ),
        ),
        mapOf("name" to "x", "dimension" to "2", "werteVorrat" to "R"),
    )
}

internal fun MathematikAuswerterRegister.registriereTupelVariable() {
    registriere(TUPEL_VARIABLE_ART) { k ->
        val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val dimension = k.knoten.parameter["dimension"]?.toIntOrNull()
            ?: error("Die Tupeldimension muss eine ganze Zahl sein.")
        require(dimension >= 1) { "Die Tupeldimension muss mindestens 1 sein." }
        val werteVorrat = tupelVariablenWerteVorrat(k.knoten.parameter["werteVorrat"])
        val komponenten = List(dimension) { index ->
            Variable("${name}_${index + 1}", "${name}_{${index + 1}}")
        }
        KnotenAuswertungsErgebnis(
            mapOf(
                "tupel" to BedingterWert(
                    objekt = Tupel(komponenten),
                    variablenQuellen = komponenten.mapIndexed { index, variable ->
                        VariablenQuelle(
                            knotenId = k.knoten.id,
                            name = variable.name,
                            werteVorrat = werteVorrat,
                            bindungsId = "${k.knoten.id.wert}:tupel",
                            bindungsName = "komponente-${index + 1}",
                            reihenfolge = index,
                        )
                    },
                ),
            ),
        )
    }
}

private fun tupelVariablenWerteVorrat(id: String?): MengenAusdruck = when (id?.trim()?.uppercase()) {
    "N", "ℕ" -> NatürlicheZahlen
    "Z", "ℤ" -> GanzeZahlen
    "Q", "ℚ" -> RationaleZahlen
    "C", "ℂ" -> KomplexeZahlen
    else -> ReelleZahlen
}

internal fun tupelVariablenFormel(knoten: KnotenDaten): String {
    val name = knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
    val dimension = knoten.parameter["dimension"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return if (dimension == 1) "${name}=\\left(${name}_{1}\\right)" else
        "${name}=\\left(${name}_{1},\\ldots,${name}_{${dimension}}\\right)"
}
''',
)
replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/katalog/MathematikAuswerterPakete.kt",
    '''        MathematikAuswerterPaket("tupeloperationen") { registriereTupelOperationKnoten() },''',
    '''        MathematikAuswerterPaket("tupeloperationen") { registriereTupelOperationKnoten() },\n        MathematikAuswerterPaket("tupelvariable") { registriereTupelVariable() },''',
)
replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/katalog/KanonischerMathematikKnotenKatalog.kt",
    '''        return bereinigt +\n            VektorKonstruktorVorlagen.standard +''',
    '''        return bereinigt +\n            TupelVariableKnotenVorlagen.standard +\n            VektorKonstruktorVorlagen.standard +''',
)
renderer = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenRenderer.kt"
replace_once(
    renderer,
    '''                knoten.art == "mathematik.variable" -> LatexFormel(''',
    '''                knoten.art == TUPEL_VARIABLE_ART -> LatexFormel(\n                    tupelVariablenFormel(knoten),\n                    style = MaterialTheme.typography.bodyLarge,\n                )\n                knoten.art == "mathematik.variable" -> LatexFormel(''',
)
# Gleiche Topologiequelle: Komponentenindex entscheidet deterministisch die Reihenfolge.
method_file = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt"
replace_once(
    method_file,
    '''                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } },''',
    '''                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } }\n                    .thenBy { entry -> entry.value.minOf { quelle -> quelle.reihenfolge } },''',
)

# #165: Tensorraum erhält einen echten Tupel-Eingang. Historische Karten dürfen den alten
# Stringparameter noch lesen; neue Vorlagen schreiben ihn nicht mehr.
mengenraum = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MengenraumKnoten.kt"
source = read(mengenraum)
source = source.replace('''    private fun ausgang(name: String = "menge") = AnschlussDaten(''', '''    private fun tupelEingang(name: String, reihe: Int = 0) = AnschlussDaten(\n        name = name,\n        richtung = AnschlussRichtung.Eingang,\n        kante = AnschlussKante.Links,\n        art = MathematikAnschlussArten.Tupel.id,\n        reihenfolge = reihe,\n    )\n\n    private fun ausgang(name: String = "menge") = AnschlussDaten(''', 1)
source = source.replace('''        "Erzeugt A^{n×m×k×…} aus einer kommagetrennten Liste positiver Dimensionen.",\n        GraphGröße(260f, 115f),\n        listOf(eingang("grundmenge"), ausgang()),\n        mapOf("dimensionen" to "2,2,2"),''', '''        "Erzeugt A^{n×m×k×…} aus einem Tupel positiver natürlicher Dimensionen.",\n        GraphGröße(270f, 125f),\n        listOf(eingang("grundmenge", 0), tupelEingang("dimensionen", 1), ausgang()),''')
old_fun = re.compile(r'''private fun KnotenAuswertungsKontext\.mengenraumDimensionen\(\): List<Int> \{.*?\n\}''', re.S)
new_fun = r'''private fun KnotenAuswertungsKontext.mengenraumDimensionen(): List<Int> {
    val tupel = eingänge["dimensionen"]?.objekt as? Tupel
    if (tupel != null) {
        require(tupel.elemente.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
        return tupel.elemente.mapIndexed { index, element ->
            val zahl = element as? RationaleZahl
                ?: error("Tensorraumdimension ${index + 1} ist keine konkrete natürliche Zahl.")
            require(zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() > 0 && zahl.zähler.bitLength() < 31) {
                "Tensorraumdimension ${index + 1} muss eine positive natürliche Zahl sein."
            }
            zahl.zähler.toInt()
        }
    }

    // Lade-Kompatibilität für alte Karten. Neue Knoten besitzen diesen Parameter nicht mehr.
    val historisch = knoten.parameter["dimensionen"]
        ?: error("Der Tupel-Eingang 'dimensionen' fehlt.")
    val dimensionen = historisch.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { it.toIntOrNull() ?: error("Tensorraumdimension '$it' ist keine ganze Zahl.") }
    require(dimensionen.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
    require(dimensionen.all { it > 0 }) { "Alle Tensorraumdimensionen müssen positiv sein." }
    return dimensionen
}'''
source, n = old_fun.subn(new_fun, source, count=1)
if n != 1:
    raise RuntimeError("Tensorraum-Dimensionsfunktion nicht gefunden")
write(mengenraum, source)

# Kleine Regressionstests für pure/strukturelle Verträge.
write(
    "app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/KartenExportFormatTest.kt",
    r'''package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals

class KartenExportFormatTest {
    @Test fun `endung wird genau einmal normalisiert`() {
        assertEquals("Atlas.json", normalisiereExportDateiname("Atlas.matlas", KartenExportFormat.JSON))
        assertEquals("Atlas.matlas", normalisiereExportDateiname("Atlas.json", KartenExportFormat.MATLAS))
    }
}
''',
)
write(
    "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/GeometrieAchsenIndexTest.kt",
    r'''package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertEquals

class GeometrieAchsenIndexTest {
    @Test fun `ganzzahlige schrittweite wird bei zoom ausgeduennt`() {
        assertEquals(1, geometrieGanzzahlSchritt(80.0))
        assertEquals(2, geometrieGanzzahlSchritt(30.0))
        assertEquals(10, geometrieGanzzahlSchritt(6.0))
    }
}
''',
)

print("Small-issues patch vorbereitet.")
