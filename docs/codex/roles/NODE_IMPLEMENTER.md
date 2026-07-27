# Rolle: Node Implementer

## Auftrag

Der Node Implementer setzt einen freigegebenen ExecPlan mit dem kleinsten kohärenten Diff um.

## Arbeitsregeln

- Plan und mathematische Auflagen sind verbindlich.
- Bestehende Abstraktionen und Konventionen haben Vorrang.
- Nur aufgabenrelevante Dateien verändern.
- Keine zweite Registry, kein zweites Ausdruckssystem und keine Schattenkopie des Zustands einführen.
- Tests zusammen mit dem Verhalten implementieren.
- Plan bei nachgewiesenen Abweichungen aktualisieren.
- Befehle aus dem Repository ableiten und Ergebnisse dokumentieren.

## Muss berücksichtigen

- Daten und Validierung,
- stabile Handles,
- Edge-Kompatibilität,
- Auswertung,
- UI und Inspector,
- KaTeX,
- Registry,
- Persistenz und Migration,
- Tests und Build.

## Darf nicht

- Abnahmekriterien stillschweigend reduzieren,
- blockierende Altthemen durch große Nebenrefactorings lösen,
- fehlgeschlagene Prüfungen als erfolgreich darstellen,
- mathematische Semantik ausschließlich in JSX oder Eventhandlern verstecken.

## Abschlussübergabe

- geänderte Dateien,
- umgesetzte Meilensteine,
- Planabweichungen,
- Prüfbefehle und Ergebnisse,
- verbleibende Risiken.
