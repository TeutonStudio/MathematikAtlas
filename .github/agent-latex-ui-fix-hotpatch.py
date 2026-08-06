from pathlib import Path


def ersetzen(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    gefunden = text.count(alt)
    if gefunden != anzahl:
        raise SystemExit(f"{pfad}: erwartete {anzahl} Treffer, gefunden: {gefunden}")
    datei.write_text(text.replace(alt, neu), encoding="utf-8")


# Der erste Teil des Hauptskripts wurde bereits gegen den aktuellen Quellstand
# verifiziert. Die Intervalländerungen werden hier mit rohen Python-Strings
# ausgeführt, damit die doppelten Kotlin-Backslashes nicht still halbiert werden.
hauptskript = Path(".github/agent-latex-ui-fix.py")
text = hauptskript.read_text(encoding="utf-8")
marker = "# 4. Reelle Intervalle gemäß Issue #157 als annotierte beschränkte reelle Zahlmenge."
if text.count(marker) != 1:
    raise SystemExit("Intervallabschnitt des Reparaturskripts nicht eindeutig gefunden")
hauptskript.write_text(text.split(marker, 1)[0].rstrip() + "\n", encoding="utf-8")

ersetzen(
    "MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Mengen.kt",
    r'''    override fun zuLatex(): String {
        val linkeKlammer = if (linksOffen) "\\mathopen{]}" else "\\mathopen{[}"
        val rechteKlammer = if (rechtsOffen) "\\mathclose{[}" else "\\mathclose{]}"
        return "$linkeKlammer${links.zuLatex()},${rechts.zuLatex()}$rechteKlammer"
    }
''',
    r'''    override fun zuLatex(): String {
        val linkeRelation = if (linksOffen) "<" else "\\leq"
        val rechteRelation = if (rechtsOffen) "<" else "\\leq"
        return "{}^{${links.zuLatex()}$linkeRelation}\\mathbb{R}^{${rechteRelation}${rechts.zuLatex()}}"
    }
''',
)

ersetzen(
    "MathematikRechenSystem/src/test/kotlin/de/TeutonStudio/MathematikRechenSystem/ReellesIntervallTest.kt",
    r'''        assertEquals("\\mathopen{[}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), false)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), false)).zuLatex())
        assertEquals("\\mathopen{[}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), true)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), true)).zuLatex())
''',
    r'''        assertEquals("{}^{1\\leq}\\mathbb{R}^{\\leq3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), false)).zuLatex())
        assertEquals("{}^{1<}\\mathbb{R}^{\\leq3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), false)).zuLatex())
        assertEquals("{}^{1\\leq}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), true)).zuLatex())
        assertEquals("{}^{1<}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), true)).zuLatex())
''',
)

ersetzen(
    "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/ReellesIntervallKnotenTest.kt",
    r'''        assertEquals(
            "\\mathopen{[}1,3\\mathclose{]}",
            assertIs<ReellesIntervall>(ergebnis).zuLatex(),
        )
''',
    r'''        assertEquals(
            "{}^{1\\leq}\\mathbb{R}^{\\leq3}",
            assertIs<ReellesIntervall>(ergebnis).zuLatex(),
        )
''',
)

ersetzen(
    "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/ReellesIntervallKnotenTest.kt",
    r'''        assertEquals("\\mathopen{]}1,3\\mathclose{[}", assertIs<ReellesIntervall>(beideOffen).zuLatex())
        assertEquals("\\mathopen{[}1,3\\mathclose{[}", assertIs<ReellesIntervall>(nurRechtsOffen).zuLatex())
''',
    r'''        assertEquals("{}^{1<}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(beideOffen).zuLatex())
        assertEquals("{}^{1\\leq}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(nurRechtsOffen).zuLatex())
''',
)
