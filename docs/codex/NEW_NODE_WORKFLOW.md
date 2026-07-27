# Workflow für neue Knoten

## Zweck

Dieser Workflow verhindert, dass fachliche Semantik, UI und Persistenz während einer spontanen Implementierung ineinander verknotet werden. Er gilt für neue Node-Typen und wesentliche Änderungen an bestehenden Node-Verträgen.

## Phase 0: Auftrag erfassen

Der Koordinator sammelt:

- fachliches Ziel,
- Ein- und Ausgaben,
- erwartete Interaktion,
- Inspector-Anforderungen,
- Persistenzbedarf,
- bekannte Grenzen,
- messbare Abnahmekriterien.

Er entscheidet noch keine Architektur ohne Untersuchung des Repositories.

## Phase 1: Bestand kartieren

`node_planner` untersucht mindestens:

1. einen fachlich ähnlichen Node,
2. einen Node mit ähnlichem Inspector,
3. Registry oder Fabrik,
4. Handle- und Edge-Validierung,
5. Ausdrucks- oder Auswertungssystem,
6. Persistenz,
7. Testkonventionen,
8. package.json und Lockdatei.

Er dokumentiert konkrete Dateien und Symbole.

## Phase 2: ExecPlan

Der Plan folgt `PLANS.md` und wird unter folgendem Pfad angelegt:

```text
docs/codex/plans/active/YYYY-MM-DD-kurzer-node-name.md
```

Der Plan enthält die vollständige Node-Spezifikation nach `NODE_CONTRACT.md`.

## Phase 3: Mathematische Freigabe

`math_reviewer` prüft mathematisch nicht triviale Nodes.

Mögliche Ergebnisse:

- **freigegeben:** Umsetzung darf beginnen.
- **freigegeben mit Auflagen:** Auflagen werden verbindliche Abnahmekriterien.
- **nicht freigegeben:** Plan muss überarbeitet werden.

## Phase 4: Implementierung

`node_implementer` setzt den bestätigten Plan um.

Regeln:

- nur ein schreibender Agent,
- kleinster kohärenter Diff,
- keine unabhängige Neugestaltung des Projekts,
- Plan fortlaufend aktualisieren,
- relevante Tests während der Implementierung ausführen.

## Phase 5: Unabhängige Verifikation

`node_verifier` prüft:

- Anforderung gegen Plan,
- Plan gegen Diff,
- Diff gegen Architektur und Node-Vertrag,
- Tests gegen tatsächliches Verhalten,
- Persistenz und Migration,
- mathematische Auflagen,
- unbeabsichtigte Änderungen.

Der Verifizierer repariert nichts selbst.

## Phase 6: Korrektur

Findings werden mit Priorität, Datei, Symbol, Fehlverhalten und Korrekturbedingung an `node_implementer` übergeben.

Danach erfolgt eine neue unabhängige Verifikation.

## Phase 7: Abschluss

Nach Abnahme:

- `CURRENT_STATE.md` aktualisieren,
- gegebenenfalls `ARCHITECTURE.md` oder `NODE_CONTRACT.md` aktualisieren,
- dauerhafte Entscheidungen als ADR festhalten,
- ExecPlan nach `plans/completed/` verschieben,
- Abschlussbericht erstellen.

## Übergabeformat Planer → Implementierer

```md
## Freigegebener Umfang

## Betroffene Dateien und Symbole

## Verbindliche Semantik

## Daten- und Handle-Vertrag

## Implementierungsschritte

## Prüfungen

## Abnahmekriterien

## Risiken und Nicht-Ziele
```

## Übergabeformat Implementierer → Verifizierer

```md
## Umgesetzter Plan

## Geänderte Dateien

## Abweichungen und Begründung

## Ausgeführte Prüfungen

## Bekannte Restpunkte

## Vollständiger Diff-Bezug
```

## Übergabeformat Verifizierer → Implementierer

```md
## Priorität

## Datei oder Symbol

## Beobachtetes Fehlverhalten

## Warum es gegen Plan oder Vertrag verstößt

## Reproduktions- oder Prüfschritt

## Korrekturbedingung
```
