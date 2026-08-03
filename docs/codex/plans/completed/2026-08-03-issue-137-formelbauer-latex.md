# Issue 137: Formelbauer-Kern mit LaTeX-Roundtrip

## Ziel

Die gemeinsame Ausdrucksstruktur wird erstmals produktiv bearbeitbar. Dieser Inkrement-Commit liefert den UI-neutralen Kern für eine CAS-Formeltastatur, kontrollierten LaTeX-Import, kanonischen LaTeX-Export und transaktionale Bearbeitung mit Undo/Redo.

## Umfang

- UI-neutrales Bearbeitungsmodell mit stabiler Ausdrucksauswahl und navigierbaren Platzhaltern.
- Kontrollierter LaTeX-Parser für Grundrechenarten, Potenzen, Brüche, Wurzeln, Funktionen, Konstanten und explizite Gruppierung.
- Kanonischer Export ausschließlich aus `FormelAusdruck`.
- Strukturierte CAS-Tastatur für Grundrechenarten, Potenzen, Standardfunktionen, trigonometrische, reziproke trigonometrische und hyperbolische Funktionen.
- Alias `cosec` wird beim Import auf die stabile Operator-ID `zahl.csc` abgebildet.
- Unit-Tests für Präzedenz, Roundtrip, Fehlerpositionen, Tastaturstruktur und Undo/Redo.

## Abgrenzung

- Die Compose-Oberfläche und der Eintrag `Formel` im Knoten-erstellen-Dialog folgen in einem weiteren Inkrement.
- Die Umwandlung einer vollständigen Formel in reale `KnotenDaten` und `VerbindungDaten` folgt ebenfalls separat.
- Noch nicht registrierte Zahlenrechnerzustände aus #135 werden bereits mit ihren stabilen IDs modelliert, können aber erst nach ihrer Registrierung als reale Knoten materialisiert werden.
- Freie TeX-Makros, Dokumentpräambeln und ausführbarer TeX-Inhalt werden absichtlich nicht unterstützt.

## Invarianten

- LaTeX ist Import- und Exportformat, niemals mathematische Wahrheitsquelle.
- Tastatureingaben erzeugen `FormelAusdruck`-Objekte statt Zeichenketten.
- Import führt keine Makros aus.
- Bestehende Knoten- und Persistenzverträge werden nicht verändert.

## Verifikation

- Kotlin-Kompilationsprüfung des neuen Kernmodells gegen den bestehenden Ausdrucksvertrag.
- Regressionstests für Präzedenz, Bruch-/Funktionsimport, Fehlerpositionen, Tastaturstruktur und Undo/Redo.
- Vollständiger Repository-/Android-Build ist im verbundenen GitHub-Workflow auszuführen, da in der aktuellen Werkzeuglaufzeit kein Repository-Checkout verfügbar ist.
