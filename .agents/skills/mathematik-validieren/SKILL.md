---
name: mathematik-validieren
description: Prüft mathematische Semantik von Operatoren, Methoden, Mengen, Relationen, Vektoren, Matrizen, Tensoren und Visualisierungen im Mathematik Atlas. Verwenden bei fachlichen Änderungen auch an vorhandenen Knoten.
---
# Mathematische Semantik prüfen

Lies `AGENTS.md`, den relevanten Kerncode und `docs/codex/roles/MATH_REVIEWER.md`. Dieser Skill ist eine fachliche Prüfroutine, kein Ersatz für den Implementierer.

## Vertrag vor Code
Beschreibe Eingabemengen, Ausgabemengen, Parameterbindung, Strukturform/Dimension, exakte vs. approximative Operation, undefinierte/unentscheidbare Fälle und Fehlersemantik. Prüfe zuerst, ob ähnliche Semantik bereits existiert. Unterscheide wertmäßige Gleichheit, Typidentität, Term und Methode sowie Definitions- und Wertebereich.

## Fallmatrix
- Einfacher gültiger Fall und mindestens ein nichttrivialer gültiger Fall.
- Grenz- und leere Fälle (sofern definiert).
- Typ- und Dimensionsfehler; verschachtelte Tupel/Zeilen/Spalten, wenn betroffen.
- Symbolische Variablen, Bindung und Reihenfolge mehrerer Argumente.
- Widersprechende oder unentscheidbare Informationen: niemals still als falsch oder leere Menge ausgeben.
- Für numerische Approximationen: Toleranzen nur in der Darstellung/Approximation, nicht als Ersatz exakter Semantik; Abbruch- und Laufzeitgrenzen.
- Für Transformationen: Bedeutung, Ausgabeart und gültige Rücktransformation bzw. Gegenbeispiel.
- Für Visualisierung: dargestelltes Bild von tatsächlicher mathematischer Aussage trennen.

## Review
Mathematische Auflagen mit reproduzierbarem Gegenbeispiel, erwartetem Resultat und betroffenem Symbol dokumentieren. Bei nichttrivialem neuem Vertrag vor Implementierung an `math_reviewer` geben; nach Implementierung Kern- und Regressionstests prüfen. Der Reviewer verändert keine Produktionsdateien.

## Ergebnis
Urteil: freigegeben / mit Auflagen / nicht freigegeben. Jede Auflage besitzt einen prüfbaren Test oder ein ausdrückliches Begründungsdokument. Keine Behauptung eines Beweises durch Stichprobentests.
