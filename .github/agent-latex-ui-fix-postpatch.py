from pathlib import Path


def ersetzen(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    gefunden = text.count(alt)
    if gefunden != anzahl:
        raise SystemExit(f"{pfad}: erwartete {anzahl} Treffer, gefunden: {gefunden}")
    datei.write_text(text.replace(alt, neu), encoding="utf-8")


# Die Python-Generatorzeichenketten besitzen eine zusätzliche Escape-Ebene.
# Nach ihrer Ausführung müssen Kotlin-Quellen deshalb ausdrücklich zwei
# Backslashes enthalten, nicht einen ungültigen Kotlin-Escape.
ersetzen(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/LatexText.kt",
    r'getrimmt.startsWith("\[") && getrimmt.endsWith("\]")',
    r'getrimmt.startsWith("\\[") && getrimmt.endsWith("\\]")',
)
ersetzen(
    "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/LatexText.kt",
    r'getrimmt.startsWith("\(") && getrimmt.endsWith("\)")',
    r'getrimmt.startsWith("\\(") && getrimmt.endsWith("\\)")',
)

latex_test = "MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/LatexTextTest.kt"
ersetzen(
    latex_test,
    r'vereinfacheLatexAnzeige("\mathbb R \mathbb{N}")',
    r'vereinfacheLatexAnzeige("\\mathbb R \\mathbb{N}")',
)
ersetzen(
    latex_test,
    r'vereinfacheLatexAnzeige("$\mathbb{R}$")',
    r'vereinfacheLatexAnzeige("$\\mathbb{R}$")',
)
ersetzen(
    latex_test,
    r'vereinfacheLatexAnzeige("\(\mathbb{N}\)")',
    r'vereinfacheLatexAnzeige("\\(\\mathbb{N}\\)")',
)
ersetzen(
    latex_test,
    r'vereinfacheLatexAnzeige("\mathopen{]}1,3\mathclose{[}")',
    r'vereinfacheLatexAnzeige("\\mathopen{]}1,3\\mathclose{[}")',
)
ersetzen(
    latex_test,
    r'vereinfacheLatexAnzeige("{}^{1\leq}\mathbb{R}^{<3}")',
    r'vereinfacheLatexAnzeige("{}^{1\\leq}\\mathbb{R}^{<3}")',
)

# Regex-Ersatztexte interpretieren Backslashes selbst. Eine Lambda-Ersetzung
# gibt den einzelnen kanonischen Backslash dagegen unverändert aus.
ersetzen(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/konzepte/KonzeptUi.kt",
    r'normalisiereLatexQuelltext(titel).replace(doppeltEscapterLatexBefehl, "\\")',
    r'normalisiereLatexQuelltext(titel).replace(doppeltEscapterLatexBefehl) { "\\" }',
)
