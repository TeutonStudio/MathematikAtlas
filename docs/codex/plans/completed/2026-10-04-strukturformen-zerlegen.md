# Strukturformen und rechnerabhängiges Zerlegen

## Status

Abgeschlossen am 2026-10-04.

## Ziel und Nutzerwirkung

Gebundene Tupel, Vektoren, Matrizen und Tensoren können im Mengenkonstruktor eine unbekannte, symbolische oder konkrete Form tragen. Die zuständige Rechnerfamilie zerlegt konkrete Formen in stabile Ausgänge und symbolische Formen in eine endliche Indexmethode.

## Nicht-Ziele

- Keine unendliche Folge aus einem endlichen Strukturwert ableiten.
- Keine neuen Knotentypen und keine Änderung fachfremder Editorlogik.
- Keine beliebigen Dimensionsformeln; nur positive Ganzzahlen und Variablen.

## Untersuchter Istzustand

Der Mengenkonstruktor speicherte nur die Anschlussart und erzeugte sonst ein `TypisiertesElement`. Vektor-Zerlegen synchronisierte Ausgänge erst nach einer konkreten Tupel-/Vektorauswertung. Matrix- und Tensorrechner besaßen noch keinen Zerlegen-Operator.

## Fachliche und mathematische Semantik

Strukturformen sind geordnete positive natürliche Achsenlängen. Komponenten- und Schnittmethoden sind 1-basiert und besitzen den endlichen Bereich `{1,…,n}`. Matrixzerlegung liefert wahlweise Zeilen- oder Spaltenvektoren; Tensorzerlegung fixiert einen Index einer gewählten Achse und senkt die Ordnung um eins.

## Daten-, Node-, Handle- und Edge-Vertrag

`TypisiertesElement` trägt optionale Formmetadaten. Der Mengenkonstruktor persistiert Formmodus, Eingabeart und Inspectorwerte. Einzelne Dimensionseingänge heißen `dimension.1`, …; der Tupelanschluss heißt `dimensionen`. Dynamische Zerlegerausgänge verwenden deterministische, knotengebundene IDs; ein symbolischer Zerleger besitzt genau den Ausgang `methode`.

## Architekturentscheidungen

Mathematische Indexmengen und Zugriffe liegen im Rechenkern. Knotenverträge und Auswerter liegen in `MathematikKnoten`, die gemeinsame Laufzeitsynchronisierung in der App. Der fachneutrale Editor erhielt keine Mathematikregeln.

## Betroffene Dateien und Symbole

- Rechenkern: `TypisiertesElement`, `EndlicheIndexMenge`, Strukturzugriffe, Matrix-/Tensoroperatoren und Typinferenz.
- Mengenkonstruktor: persistierte Formparameter, atomare Anschlusskonfiguration und Inspector.
- Rechner: Vektor-, Matrix- und Tensor-Zerlegen sowie gemeinsame `synchronisiereStrukturZerleger`-Logik.
- Tests: Kernsemantik, Mengenkonstruktor, Rechnerauswertung und dynamische Anschluss-IDs.

## Meilensteine

- [x] Kernmodell und symbolische Indexmethoden.
- [x] Mengenkonstruktor-Konfiguration und Auswertung.
- [x] Vektor-, Matrix- und Tensorzerleger mit dynamischen Anschlüssen.
- [x] Legacy-Fallback, Tests und Gesamtprüfung.

## Persistenz und Migration

Fehlende Formparameter bedeuten weiterhin `unbekannt`. Die Form wird über vorhandene Knotenparameter persistiert; eine Formatversionserhöhung war nicht erforderlich. Modus- und Typwechsel laufen als atomare `KnotenKonfigurationErsetzen`-Aktion und bleiben damit Undo-fähig.

## Fortschritt und Ergebnis

- Form- und Indexmodelle einschließlich Substitution, LaTeX, freier Parameter, Zielmenge und Typinferenz ergänzt.
- Inspector-, Einzelanschluss- und Tupelanschlussmodus umgesetzt; Null, negative und gebrochene Literalformen werden abgelehnt.
- Konkrete Formen erzeugen deterministische Einzelausgänge, symbolische Formen endliche Indexmethoden.
- Matrixzeilen/-spalten und Tensor-Schnitte werden materialisiert; Tensorordnung drei ergibt Matrizen, Ordnung eins Skalare.
- Gemeinsame Strukturzerleger-Synchronisierung bewahrt stabile IDs und entfernt über die Graphprüfung ungültige Kanten.

## Entscheidungsprotokoll

- 2026-10-04: Rechnerfamilien bleiben getrennt; Alternative allgemeiner Flatten-Rechner verworfen.
- 2026-10-04: Symbolische endliche Indexmenge statt `ℕ`; verhindert falsche Unendlichkeitssemantik.
- 2026-10-04: Dimensionsquelle kann Inspector, Einzelanschlüsse oder Tupelanschluss sein.
- 2026-10-04: Tensorachsen bleiben im Inspector sichtbar 1-basiert und werden vor dem Kernzugriff normalisiert.

## Abweichungen vom ursprünglichen Plan

Keine fachliche Abweichung. Die vorhandene Tensor-Operationsregistry wurde als Such- und Inspectorvertrag weiterverwendet; Matrix-Zerlegen hängt am bestehenden Strukturrechnerkatalog.

## Verifikation

- `./gradlew :MathematikRechenSystem:test :MathematikKnoten:testDebugUnitTest :app:testDebugUnitTest` erfolgreich.
- `./gradlew test :app:assembleDebug` erfolgreich.
- `python3 scripts/pruefe_repository.py` erfolgreich.
- `python3 scripts/pruefe_releaseplan.py` erfolgreich.
- `python3 scripts/pruefe_versionsfolge.py` erfolgreich.
- `python3 scripts/pruefe_kern.py` nicht ausführbar: `kotlinc` fehlt in der Umgebung; die Gradle-Kern- und Gesamttests waren erfolgreich.
- `git diff --check` ohne Beanstandung.
