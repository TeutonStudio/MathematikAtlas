from pathlib import Path


def ersetzen(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    gefunden = text.count(alt)
    if gefunden != anzahl:
        raise SystemExit(f"{pfad}: erwartete {anzahl} Treffer, gefunden: {gefunden}")
    datei.write_text(text.replace(alt, neu), encoding="utf-8")


def vor_klassenende_einfuegen(pfad: str, inhalt: str) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    position = text.rfind("\n}")
    if position < 0:
        raise SystemExit(f"{pfad}: Klassenende nicht gefunden")
    datei.write_text(text[:position] + "\n" + inhalt.rstrip() + text[position:], encoding="utf-8")


# 1. Inspector-Zahnrad: transparenter Button, nur das Icon wird aus der Profilfarbe abgeleitet.
editor = "KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/schnittstelle/KnotenKartenEditor.kt"
ersetzen(
    editor,
    "            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),\n        ) {",
    "            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),\n            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),\n        ) {",
)
ersetzen(
    editor,
    '''@Composable
private fun KnotenInspektorSchaltfläche(
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = beiKlick,
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = "Inspektor öffnen" },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_knoten_inspektor),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}
''',
    '''@Composable
private fun KnotenInspektorSchaltfläche(
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val knotenHintergrund = MaterialTheme.colorScheme.surfaceContainerLow
    val iconFarbe = kontrastAdaptiveProfilFarbe(
        profilFarbe = MaterialTheme.colorScheme.primary,
        hintergrund = knotenHintergrund,
    )
    IconButton(
        onClick = beiKlick,
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = "Inspektor öffnen" },
        colors = IconButtonDefaults.iconButtonColors(contentColor = iconFarbe),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_knoten_inspektor),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal fun farbKontrastVerhältnis(vordergrund: Color, hintergrund: Color): Float {
    val vordergrundLuminanz = vordergrund.copy(alpha = 1f).luminance()
    val hintergrundLuminanz = hintergrund.copy(alpha = 1f).luminance()
    val heller = max(vordergrundLuminanz, hintergrundLuminanz)
    val dunkler = min(vordergrundLuminanz, hintergrundLuminanz)
    return (heller + 0.05f) / (dunkler + 0.05f)
}

/**
 * Bewahrt die Profilfarbe, solange sie auf dem Knoten ausreichend kontrastiert.
 * Andernfalls wird sie nur so weit in Richtung Schwarz oder Weiß verschoben,
 * wie für die Erkennbarkeit erforderlich ist. Ein zusätzlicher Icon-Hintergrund
 * ist dadurch weder nötig noch erlaubt.
 */
internal fun kontrastAdaptiveProfilFarbe(
    profilFarbe: Color,
    hintergrund: Color,
    mindestKontrast: Float = 4.5f,
): Color {
    val deckendeProfilFarbe = profilFarbe.copy(alpha = 1f)
    if (farbKontrastVerhältnis(deckendeProfilFarbe, hintergrund) >= mindestKontrast) {
        return deckendeProfilFarbe
    }
    val ziel = listOf(Color.Black, Color.White)
        .maxBy { farbKontrastVerhältnis(it, hintergrund) }
    var unten = 0f
    var oben = 1f
    repeat(18) {
        val mitte = (unten + oben) / 2f
        if (farbKontrastVerhältnis(lerp(deckendeProfilFarbe, ziel, mitte), hintergrund) >= mindestKontrast) {
            oben = mitte
        } else {
            unten = mitte
        }
    }
    return lerp(deckendeProfilFarbe, ziel, oben).copy(alpha = 1f)
}
''',
)

farbe_test = Path("KnotenKartenVerwalter/src/test/kotlin/de/TeutonStudio/KnotenKartenVerwalter/schnittstelle/KnotenInspektorFarbeTest.kt")
farbe_test.parent.mkdir(parents=True, exist_ok=True)
farbe_test.write_text('''package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnotenInspektorFarbeTest {
    @Test
    fun `profilfarbe bleibt ohne zusaetzlichen hintergrund kontrastreich`() {
        val hintergruende = listOf(Color(0xFFF7F2FA), Color(0xFF211F26))
        val profilFarben = listOf(
            Color(0xFF6750A4),
            Color(0xFFFFF59D),
            Color(0xFF101010),
            Color(0xFFFF0000),
            Color(0xFF00A000),
            Color(0xFF0047FF),
        )

        hintergruende.forEach { hintergrund ->
            profilFarben.forEach { profilFarbe ->
                val ergebnis = kontrastAdaptiveProfilFarbe(profilFarbe, hintergrund)
                assertTrue(farbKontrastVerhältnis(ergebnis, hintergrund) >= 4.49f)
                assertEquals(1f, ergebnis.alpha)
            }
        }
    }

    @Test
    fun `bereits kontrastreiche profilfarbe bleibt unveraendert`() {
        val profilFarbe = Color(0xFF002060)
        val hintergrund = Color(0xFFF7F2FA)

        assertEquals(profilFarbe, kontrastAdaptiveProfilFarbe(profilFarbe, hintergrund))
    }
}
''', encoding="utf-8")


# 2. Gemeinsamer LaTeX-Kern: Begrenzungen, ungruppiertes mathbb und historische Delimiter.
latex = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/LatexText.kt"
ersetzen(
    latex,
    "fun latexZuAnnotiertemText(\n    latex: String,\n    wahrFarbe: Color = STANDARD_WAHR_FARBE,\n    lügeFarbe: Color = STANDARD_LÜGE_FARBE,\n): AnnotatedString = buildAnnotatedString {\n    LatexParser(latex, this, wahrFarbe, lügeFarbe).schreibe()\n}\n",
    '''fun normalisiereLatexQuelltext(latex: String): String {
    val getrimmt = latex.trim()
    return when {
        getrimmt.length >= 4 && getrimmt.startsWith("$$") && getrimmt.endsWith("$$") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\[") && getrimmt.endsWith("\\]") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\(") && getrimmt.endsWith("\\)") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 2 && getrimmt.startsWith('$') && getrimmt.endsWith('$') ->
            getrimmt.substring(1, getrimmt.length - 1).trim()
        else -> getrimmt
    }
}

fun latexZuAnnotiertemText(
    latex: String,
    wahrFarbe: Color = STANDARD_WAHR_FARBE,
    lügeFarbe: Color = STANDARD_LÜGE_FARBE,
): AnnotatedString = buildAnnotatedString {
    LatexParser(normalisiereLatexQuelltext(latex), this, wahrFarbe, lügeFarbe).schreibe()
}
''',
)
ersetzen(
    latex,
    '            "mathop", "mathbin" -> schreibeArgument()\n',
    '            "mathop", "mathbin", "mathopen", "mathclose" -> schreibeArgument()\n',
)
ersetzen(
    latex,
    '            "mathbb" -> ausgabe.append(zahlbereich(liesGruppenText()))\n',
    '            "mathbb" -> schreibeDoppelstrich()\n',
)
ersetzen(
    latex,
    "    private fun schreibeMathcal() {\n",
    '''    private fun schreibeDoppelstrich() {
        while (position < quelltext.length && quelltext[position].isWhitespace()) position++
        val inhalt = when {
            position >= quelltext.length -> ""
            quelltext[position] == '{' -> liesGruppenText()
            else -> quelltext[position++].toString()
        }
        ausgabe.append(zahlbereich(inhalt))
    }

    private fun schreibeMathcal() {
''',
)

latex_test = "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/LatexTextTest.kt"
vor_klassenende_einfuegen(
    latex_test,
    '''    @Test
    fun `mathbb funktioniert gruppiert und ungruppiert`() {
        assertEquals("ℝ ℕ", vereinfacheLatexAnzeige("\\mathbb R \\mathbb{N}"))
    }

    @Test
    fun `Formelbegrenzer werden in Reitertiteln entfernt`() {
        assertEquals("ℝ", vereinfacheLatexAnzeige("$\\mathbb{R}$"))
        assertEquals("ℕ", vereinfacheLatexAnzeige("\\(\\mathbb{N}\\)"))
    }

    @Test
    fun `historische Intervall Delimiter bleiben lesbar`() {
        assertEquals("]1,3[", vereinfacheLatexAnzeige("\\mathopen{]}1,3\\mathclose{[}"))
    }

    @Test
    fun `annotierte reelle Intervalle rendern Zahlbereich und Randrelationen`() {
        assertEquals("1≤ℝ<3", vereinfacheLatexAnzeige("{}^{1\\leq}\\mathbb{R}^{<3}"))
    }
''',
)


# 3. Definitionskarten-Tabs: persistierte Doppel-Escapes nur an der UI-Grenze normalisieren.
konzept = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/KonzeptUi.kt"
ersetzen(
    konzept,
    "import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen\n",
    "import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen\nimport de.TeutonStudio.MathematikKnoten.normalisiereLatexQuelltext\n",
)
ersetzen(
    konzept,
    "                                            latex = reiter.titel,\n",
    "                                            latex = normalisiereKonzeptReiterTitel(reiter.titel),\n",
)
ersetzen(
    konzept,
    "\n@Composable\nprivate fun UnveränderlicheKonzeptKarte(\n",
    r'''
private val doppeltEscapterLatexBefehl = Regex("""\\\\(?=[A-Za-z])""")

internal fun normalisiereKonzeptReiterTitel(titel: String): String =
    normalisiereLatexQuelltext(titel).replace(doppeltEscapterLatexBefehl, "\\")

@Composable
private fun UnveränderlicheKonzeptKarte(
''',
)

reiter_test = Path("app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/KonzeptReiterLatexTest.kt")
reiter_test.parent.mkdir(parents=True, exist_ok=True)
reiter_test.write_text(r'''package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.text.style.BaselineShift
import de.TeutonStudio.MathematikKnoten.latexZuAnnotiertemText
import de.TeutonStudio.MathematikKnoten.vereinfacheLatexAnzeige
import kotlin.test.Test
import kotlin.test.assertEquals

class KonzeptReiterLatexTest {
    @Test
    fun `persistierte Doppel Escapes werden vor dem Rendern normalisiert`() {
        val normalisiert = normalisiereKonzeptReiterTitel("""a_{\\mathbb{N}} \\in \\mathbb R""")
        val anzeige = latexZuAnnotiertemText(normalisiert)

        assertEquals("aℕ ∈ ℝ", anzeige.text)
        assertEquals(BaselineShift.Subscript, anzeige.spanStyles.single().item.baselineShift)
    }

    @Test
    fun `Formelbegrenzer verschwinden aus Reitertiteln`() {
        assertEquals("ℝ", vereinfacheLatexAnzeige(normalisiereKonzeptReiterTitel("\\(\\mathbb{R}\\)")))
    }
}
''', encoding="utf-8")


# 4. Reelle Intervalle gemäß Issue #157 als annotierte beschränkte reelle Zahlmenge.
mengen = "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Mengen.kt"
ersetzen(
    mengen,
    '''    override fun zuLatex(): String {
        val linkeKlammer = if (linksOffen) "\\mathopen{]}" else "\\mathopen{[}"
        val rechteKlammer = if (rechtsOffen) "\\mathclose{[}" else "\\mathclose{]}"
        return "$linkeKlammer${links.zuLatex()},${rechts.zuLatex()}$rechteKlammer"
    }
''',
    '''    override fun zuLatex(): String {
        val linkeRelation = if (linksOffen) "<" else "\\leq"
        val rechteRelation = if (rechtsOffen) "<" else "\\leq"
        return "{}^{${links.zuLatex()}$linkeRelation}\\mathbb{R}^{${rechteRelation}${rechts.zuLatex()}}"
    }
''',
)

intervall_test = "MathematikRechenSystem/src/test/kotlin/de/TeutonStudio/MathematikRechenSystem/ReellesIntervallTest.kt"
ersetzen(
    intervall_test,
    '''        assertEquals("\\mathopen{[}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), false)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), false)).zuLatex())
        assertEquals("\\mathopen{[}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), true)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), true)).zuLatex())
''',
    '''        assertEquals("{}^{1\\leq}\\mathbb{R}^{\\leq3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), false)).zuLatex())
        assertEquals("{}^{1<}\\mathbb{R}^{\\leq3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), false)).zuLatex())
        assertEquals("{}^{1\\leq}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), true)).zuLatex())
        assertEquals("{}^{1<}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), true)).zuLatex())
''',
)

knoten_intervall_test = "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/ReellesIntervallKnotenTest.kt"
ersetzen(
    knoten_intervall_test,
    '''        assertEquals(
            "\\mathopen{[}1,3\\mathclose{]}",
            assertIs<ReellesIntervall>(ergebnis).zuLatex(),
        )
''',
    '''        assertEquals(
            "{}^{1\\leq}\\mathbb{R}^{\\leq3}",
            assertIs<ReellesIntervall>(ergebnis).zuLatex(),
        )
''',
)
ersetzen(
    knoten_intervall_test,
    '''        assertEquals("\\mathopen{]}1,3\\mathclose{[}", assertIs<ReellesIntervall>(beideOffen).zuLatex())
        assertEquals("\\mathopen{[}1,3\\mathclose{[}", assertIs<ReellesIntervall>(nurRechtsOffen).zuLatex())
''',
    '''        assertEquals("{}^{1<}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(beideOffen).zuLatex())
        assertEquals("{}^{1\\leq}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(nurRechtsOffen).zuLatex())
''',
)
