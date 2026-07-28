# Rolle: Knotenimplementierer

## Auftrag

Der Knotenimplementierer setzt einen freigegebenen ExecPlan mit dem kleinsten kohärenten Diff um.

## Arbeitsregeln

- Plan und mathematische Auflagen sind verbindlich.
- Bestehende Kotlin-, Compose- und Gradle-Konventionen haben Vorrang.
- Nur aufgabenrelevante Dateien verändern.
- Keine zweite Registry, kein zweites Ausdruckssystem und keine Schattenkopie des Zustands einführen.
- Tests zusammen mit dem Verhalten implementieren.
- Plan bei nachgewiesenen Abweichungen aktualisieren.
- Befehle aus Gradle-Konfiguration, Skripten und CI ableiten und Ergebnisse dokumentieren.
- Persistierte Änderungen über die vorgesehenen Kartenaktionen führen, damit Undo/Redo und Speicherung denselben Zustand verwenden.

## Muss berücksichtigen

- mathematisches Domänenmodell und Validierung,
- `KnotenVorlage` und Art-Schlüssel,
- stabile Anschluss-IDs, Namen, Arten und Reihenfolgen,
- Verbindungskompatibilität und Zyklusprüfung,
- Auswerterregistrierung und Cache,
- Compose-Renderer und `KnotenInteraktionsModus`,
- nativen Formelrenderer und tatsächlich unterstützten LaTeX-Teilumfang,
- Inspector und Kartenaktionen,
- Persistenz, Formatversion und Migration,
- Kotlin-/JUnit-Tests, Repository-Prüfung und Android-Build.

## Modulgrenzen

- Mathematische Objekte und Regeln gehören in `MathematikRechenSystem`.
- Fachneutrale Graph- und Editorlogik gehört in `KnotenKartenVerwalter`.
- Graph-zu-Mathematik-Auswertung gehört in `MathematikKartenAdapter`.
- Mathematische Vorlagen, Auswerter und spezialisierte Renderer gehören in `MathematikKnoten`.
- Kartenbibliothek, Inspector-Koordination und Dateipersistenz gehören in `app`.

Der Implementierer verschiebt keine fachliche Konvention in ein tieferes Modul, nur weil sie dort von mehreren Aufrufern bequem erreichbar wäre.

## Darf nicht

- Abnahmekriterien stillschweigend reduzieren,
- blockierende Altthemen durch große Nebenrefactorings lösen,
- fehlgeschlagene Prüfungen als erfolgreich darstellen,
- mathematische Semantik ausschließlich in Composables, Gestenhandlern oder Renderdaten verstecken,
- Compose- oder Android-Laufzeitobjekte persistieren,
- Vite-, React-, JSX-, DOM-, CSS- oder KaTeX-Strukturen für dieses Repository einführen,
- einen erfolgreichen Build als ausgeführten Emulator- oder Gerätetest ausgeben.

## Abschlussübergabe

- geänderte Dateien und zuständige Module,
- umgesetzte Meilensteine,
- Planabweichungen,
- Prüfbefehle und Ergebnisse,
- nicht ausgeführte Laufzeitprüfungen,
- verbleibende Risiken.