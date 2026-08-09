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
        raise RuntimeError(f"{path}: genau eine Fundstelle erwartet, gefunden {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


# #288: theme-basiertes Mengenlehre-Icon in die Hauptkategoriekachel hängen.
replace_once(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KonzeptBibliothekUi.kt",
    '''            Text(\n                kategorieSymbol(kategorie.id),\n                style = MaterialTheme.typography.displaySmall,\n                color = MaterialTheme.colorScheme.primary,\n                textAlign = TextAlign.Center,\n            )''',
    '''            if (hatFachgebietsIcon(kategorie.id)) {\n                FachgebietsIcon(\n                    id = kategorie.id,\n                    modifier = Modifier.fillMaxWidth(.72f).aspectRatio(1f),\n                )\n            } else {\n                Text(\n                    kategorieSymbol(kategorie.id),\n                    style = MaterialTheme.typography.displaySmall,\n                    color = MaterialTheme.colorScheme.primary,\n                    textAlign = TextAlign.Center,\n                )\n            }''',
)

# #354: Definierte Mengen in R1 durch denselben Prädikatpfad wie höhere Räume schicken.
replace_once(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/visualisierung/sampling/VisualisierungsSampler.kt",
    '''        if (menge is KoordinatenBild) return normalisiereKoordinatenBild(menge, dimension)\n        if (konfiguration.dimension == RaumDimension.R1) {\n            return ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)\n        }''',
    '''        if (menge is KoordinatenBild) return normalisiereKoordinatenBild(menge, dimension)\n        if (konfiguration.dimension == RaumDimension.R1 && menge is DefinierteMenge) {\n            return normalisiereDefinierteMenge(menge, konfiguration)\n        }\n        if (konfiguration.dimension == RaumDimension.R1) {\n            return ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)\n        }''',
)

# #182: Name, Dimension und gemeinsamer Wertevorrat sind im Inspector editierbar.
inspektor = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektoren.kt"
replace_once(
    inspektor,
    'import de.TeutonStudio.MathematikKnoten.TUPEL_ERGÄNZEN_ART',
    'import de.TeutonStudio.MathematikKnoten.TUPEL_ERGÄNZEN_ART\nimport de.TeutonStudio.MathematikKnoten.TUPEL_VARIABLE_ART',
)
replace_once(
    inspektor,
    '        "mathematik.variable" to VariablenInspektor,',
    '        "mathematik.variable" to VariablenInspektor,\n        TUPEL_VARIABLE_ART to TupelVariablenInspektor,',
)
replace_once(
    inspektor,
    '''private object VariablenInspektor : KnotenInspektor {\n    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {\n        ParameterFeld("Name", knoten.parameter["name"] ?: "x") { aktionen.parameter("name", it.trim()) }\n        GrundmengenAuswahl("Wertevorrat", knoten.parameter["werteVorrat"] ?: "R") { aktionen.parameter("werteVorrat", it) }\n        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }\n    }\n}\n''',
    '''private object VariablenInspektor : KnotenInspektor {\n    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {\n        ParameterFeld("Name", knoten.parameter["name"] ?: "x") { aktionen.parameter("name", it.trim()) }\n        GrundmengenAuswahl("Wertevorrat", knoten.parameter["werteVorrat"] ?: "R") { aktionen.parameter("werteVorrat", it) }\n        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }\n    }\n}\n\nprivate object TupelVariablenInspektor : KnotenInspektor {\n    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {\n        ParameterFeld("Name", knoten.parameter["name"] ?: "x") {\n            aktionen.parameter("name", it.trim().ifBlank { "x" })\n        }\n        ParameterFeld("Dimension", knoten.parameter["dimension"] ?: "2") { roh ->\n            roh.trim().toIntOrNull()?.takeIf { it >= 1 }?.let { aktionen.parameter("dimension", it.toString()) }\n        }\n        GrundmengenAuswahl("Komponenten-Wertevorrat", knoten.parameter["werteVorrat"] ?: "R") {\n            aktionen.parameter("werteVorrat", it)\n        }\n        Text(\n            "Die Komponenten bleiben ein einzelnes Tupelobjekt; bei der Methodenbildung werden nur Tupelvariablen geordnet destrukturiert.",\n            style = MaterialTheme.typography.bodySmall,\n            color = MaterialTheme.colorScheme.onSurfaceVariant,\n        )\n        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }\n    }\n}\n''',
)

# #165: Legacy-Tupelquelle auswerten und Migration zentral aktivieren.
mengenraum = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MengenraumKnoten.kt"
replace_once(
    mengenraum,
    '''    registriere("mathematik.tensorraum") { k ->\n        mengenraumErgebnis(Tensorraum(k.mengenraumEingabe("grundmenge"), k.mengenraumDimensionen()), k)\n    }''',
    '''    registriere(TENSORRAUM_LEGACY_DIMENSIONEN_ART) { k ->\n        val dimensionen = k.knoten.parameter["werte"].orEmpty()\n            .split(',')\n            .map(String::trim)\n            .filter(String::isNotBlank)\n            .map { it.toLongOrNull() ?: error("Legacy-Tensorraumdimension '$it' ist keine ganze Zahl.") }\n        require(dimensionen.isNotEmpty() && dimensionen.all { it > 0 }) {\n            "Legacy-Tensorraumdimensionen müssen positive ganze Zahlen sein."\n        }\n        KnotenAuswertungsErgebnis(\n            mapOf("tupel" to BedingterWert(objekt = Tupel(dimensionen.map(RationaleZahl::von)))),\n        )\n    }\n    registriere("mathematik.tensorraum") { k ->\n        mengenraumErgebnis(Tensorraum(k.mengenraumEingabe("grundmenge"), k.mengenraumDimensionen()), k)\n    }''',
)
codec = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/migration/MathematikKartenCodec.kt"
replace_once(
    codec,
    '''    fun nachLaden(karte: KartenDaten): KartenDaten = karte\n        .migrierePraedikatStandardname()''',
    '''    fun nachLaden(karte: KartenDaten): KartenDaten = karte\n        .migriereTensorraumDimensionen()\n        .migrierePraedikatStandardname()''',
)

# #168: gemeinsame Viewport-Transformation bleibt Quelle für Achsen und Geometrie;
# sichtbare ganzzahlige Ticks und Beschriftungen werden aus derselben Projektion abgeleitet.
geo = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/geometrie/GeometrieVisualisierungsKnotenRenderer.kt"
source = read(geo)
source = source.replace(
    'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.unit.dp',
    'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.text.TextMeasurer\nimport androidx.compose.ui.text.TextStyle\nimport androidx.compose.ui.text.drawText\nimport androidx.compose.ui.text.rememberTextMeasurer\nimport androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp',
    1,
)
source = source.replace(
    '''    val render = remember(objekt) { renderDaten(objekt) }\n    Canvas(modifier.pointerInput(objekt.raum.dimension, kamera) {\n        detectTransformGestures { _, pan, zoom, _ ->\n            onKamera(\n                if (objekt.raum.dimension == 3) kamera.copy(\n                    rotationY = kamera.rotationY + pan.x * 0.45,\n                    rotationX = kamera.rotationX + pan.y * 0.45,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ) else kamera.copy(\n                    verschiebungX = kamera.verschiebungX + pan.x,\n                    verschiebungY = kamera.verschiebungY + pan.y,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ),\n            )\n        }\n    }) {\n        drawRect(hintergrund)\n        zeichneAchsen(objekt.raum.dimension, kamera, achsenFarbe)''',
    '''    val render = remember(objekt) { renderDaten(objekt) }\n    val textMeasurer = rememberTextMeasurer()\n    val beschriftungsFarbe = MaterialTheme.colorScheme.onSurfaceVariant\n    Canvas(modifier.pointerInput(objekt.raum.dimension, kamera) {\n        detectTransformGestures { _, pan, zoom, rotation ->\n            onKamera(\n                kamera.copy(\n                    rotationY = if (objekt.raum.dimension == 3) kamera.rotationY + rotation * 4.0 else kamera.rotationY,\n                    verschiebungX = kamera.verschiebungX + pan.x,\n                    verschiebungY = kamera.verschiebungY + pan.y,\n                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),\n                ),\n            )\n        }\n    }) {\n        drawRect(hintergrund)\n        zeichneAchsen(objekt.raum.dimension, kamera, achsenFarbe, beschriftungsFarbe, textMeasurer)''',
    1,
)
if 'val textMeasurer = rememberTextMeasurer()' not in source:
    raise RuntimeError("GeometrieCanvas konnte nicht aktualisiert werden")
axis_pattern = re.compile(r'''private fun DrawScope\.zeichneAchsen\(dimension: Int, kamera: GeometrieKamera, farbe: Color\) \{.*?\n\}\n\nprivate fun renderDaten''', re.S)
axis_replacement = r'''internal fun geometrieGanzzahlSchritt(pixelProEinheit: Double): Int {
    if (!pixelProEinheit.isFinite() || pixelProEinheit <= 0.0) return 1
    val ziel = max(1.0, 54.0 / pixelProEinheit)
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
    }

    val s = skala(size.width, size.height, kamera)
    val schritt = geometrieGanzzahlSchritt(s)
    val textStil = TextStyle(color = textFarbe, fontSize = 10.sp)

    if (dimension <= 2) {
        val minX = (0.0 - size.width / 2.0 - kamera.verschiebungX) / s
        val maxX = (size.width - size.width / 2.0 - kamera.verschiebungX) / s
        var x = erstesVielfaches(minX, schritt)
        var budget = 0
        while (x <= maxX && budget++ < 256) {
            val p = projekt(listOf(x.toDouble(), 0.0), size.width, size.height, kamera)
            drawLine(farbe, Offset(p.x, nullpunkt.y - 4f), Offset(p.x, nullpunkt.y + 4f), 1f)
            if (x != 0) drawText(textMeasurer, x.toString(), topLeft = Offset(p.x + 3f, nullpunkt.y + 5f), style = textStil)
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
                if (y != 0) drawText(textMeasurer, y.toString(), topLeft = Offset(nullpunkt.x + 5f, p.y + 2f), style = textStil)
                y += schritt
            }
        }
    } else {
        val weltHalb = (max(size.width, size.height) / (2.0 * s) + schritt).coerceAtMost(1000.0)
        val maxIndex = floor(weltHalb / schritt).toInt().coerceAtMost(32)
        for (i in -maxIndex..maxIndex) {
            if (i == 0) continue
            val wert = i * schritt
            listOf(
                listOf(wert.toDouble(), 0.0, 0.0),
                listOf(0.0, wert.toDouble(), 0.0),
                listOf(0.0, 0.0, wert.toDouble()),
            ).forEachIndexed { achse, koordinate ->
                val p = projekt(koordinate, size.width, size.height, kamera)
                if (p.x in -12f..(size.width + 12f) && p.y in -12f..(size.height + 12f)) {
                    drawCircle(farbe, 2.5f, p)
                    if (i % 2 == 0) {
                        val präfix = when (achse) { 0 -> "x="; 1 -> "y="; else -> "z=" }
                        drawText(textMeasurer, "$präfix$wert", topLeft = Offset(p.x + 3f, p.y + 3f), style = textStil)
                    }
                }
            }
        }
    }

    if (nullpunkt.x in 0f..size.width && nullpunkt.y in 0f..size.height) {
        drawText(textMeasurer, "0", topLeft = Offset(nullpunkt.x + 4f, nullpunkt.y + 5f), style = textStil)
    }
}

private fun renderDaten'''
source, count = axis_pattern.subn(axis_replacement, source, count=1)
if count != 1:
    raise RuntimeError("Geometrie-Achsenfunktion nicht eindeutig gefunden")
write(geo, source)

# Kleine, pure Regressionstests für die neuen Randverträge.
write(
    "app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/KartenExportFormatTest.kt",
    '''package de.TeutonStudio.MathematikAtlas\n\nimport kotlin.test.Test\nimport kotlin.test.assertEquals\n\nclass KartenExportFormatTest {\n    @Test\n    fun `exportendung wird genau einmal normalisiert`() {\n        assertEquals("Atlas.json", normalisiereExportDateiname("Atlas.matlas", KartenExportFormat.JSON))\n        assertEquals("Atlas.matlas", normalisiereExportDateiname("Atlas.json", KartenExportFormat.MATLAS))\n    }\n}\n''',
)
write(
    "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/GeometrieAchsenIndexTest.kt",
    '''package de.TeutonStudio.MathematikKnoten\n\nimport kotlin.test.Test\nimport kotlin.test.assertEquals\n\nclass GeometrieAchsenIndexTest {\n    @Test\n    fun `ganzzahlige Schrittweite wird beim Herauszoomen ausgeduennt`() {\n        assertEquals(1, geometrieGanzzahlSchritt(80.0))\n        assertEquals(2, geometrieGanzzahlSchritt(30.0))\n        assertEquals(10, geometrieGanzzahlSchritt(6.0))\n    }\n}\n''',
)

print("Finalisierungspatch angewendet")
