# Workflow für neue Knoten

## Zweck

Dieser Workflow verhindert, dass fachliche Semantik, UI, Persistenz und Versionsverwaltung während einer spontanen Implementierung ineinander verknotet werden. Er gilt für neue Knotentypen und wesentliche Änderungen an bestehenden Knotenverträgen.

## Phase 0: Auftrag und Versionswirkung erfassen

Der Koordinator sammelt:

- fachliches Ziel,
- Ein- und Ausgaben,
- erwartete Interaktion,
- Inspector-Anforderungen,
- Persistenzbedarf,
- bekannte Grenzen,
- messbare Abnahmekriterien,
- geplante neue Typ-Schlüssel oder Knotenfamilien.

Anschließend wird die Versionswirkung bestimmt:

- Ein neuer, separat erzeugbarer und registrierter Knotentyp verlangt eine neue `y`-Version und setzt `x` auf `0`.
- Eine wesentliche Änderung an einem bestehenden Knotentyp ohne neuen Typ-Schlüssel bleibt eine `x`-Version.
- Enthält der Umfang neue Knoten und weitere Änderungen, wird der gesamte Release als `y`-Version behandelt.

Der `master_verwalter` bestätigt oder reserviert die passende Version, bevor Produktionsimplementierung beginnt. Der Koordinator entscheidet noch keine Architektur ohne Untersuchung des Repositories.

## Phase 1: Bestand kartieren

`node_planner` untersucht mindestens:

1. einen fachlich ähnlichen Knoten,
2. einen Knoten mit ähnlichem Inspector,
3. Vorlagenkatalog, Registry oder Fabrik,
4. Anschluss- und Verbindungsvalidierung,
5. Ausdrucks- oder Auswertungssystem,
6. Persistenz,
7. Testkonventionen,
8. Gradle-Konfiguration, Skripte und CI.

Er dokumentiert konkrete Dateien und Symbole. Er weist außerdem für jeden geplanten Typ-Schlüssel nach, ob wirklich ein neuer eigenständig erzeugbarer Knotentyp entsteht oder nur ein bestehender Typ erweitert wird.

## Phase 2: ExecPlan

Der Plan folgt `PLANS.md` und wird unter folgendem Pfad angelegt:

```text
docs/codex/plans/active/YYYY-MM-DD-kurzer-knoten-name.md
```

Der Plan enthält die vollständige Knotenspezifikation nach `NODE_CONTRACT.md` sowie einen Abschnitt **Versionswirkung** mit:

- reservierter Version,
- Klassifikation als `y`- oder `x`-Version,
- neuen Typ-Schlüsseln,
- Begründung der Klassifikation.

## Phase 3: Mathematische Freigabe

`math_reviewer` prüft mathematisch nicht triviale Knoten.

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
- relevante Tests während der Implementierung ausführen,
- keine neuen Typ-Schlüssel außerhalb der als `y` reservierten Version ergänzen,
- bei entfallenden oder zusätzlich entstehenden Knotentypen sofort den `master_verwalter` zur Neuklassifikation einschalten.

## Phase 5: Unabhängige Verifikation

`node_verifier` prüft:

- Anforderung gegen Plan,
- Plan gegen Diff,
- Diff gegen Architektur und Knotenvertrag,
- Tests gegen tatsächliches Verhalten,
- Persistenz und Migration,
- mathematische Auflagen,
- unbeabsichtigte Änderungen,
- neue Typ-Schlüssel und Registry-Einträge gegen die reservierte `y`- oder `x`-Version.

Eine `x`-Version mit neuem registriertem Knotentyp und eine `y`-Version ohne den angekündigten neuen Knotentyp sind blockierende Findings. Der Verifizierer repariert nichts selbst.

## Phase 6: Korrektur

Findings werden mit Priorität, Datei, Symbol, Fehlverhalten und Korrekturbedingung an `node_implementer` übergeben.

Danach erfolgt eine neue unabhängige Verifikation. Erfordert die Korrektur eine andere Versionsachse, wird vor weiterer Implementierung der `master_verwalter` eingeschaltet.

## Phase 7: Abschluss

Nach Abnahme:

- `CURRENT_STATE.md` aktualisieren,
- gegebenenfalls `ARCHITECTURE.md` oder `NODE_CONTRACT.md` aktualisieren,
- dauerhafte Entscheidungen als ADR festhalten,
- ExecPlan nach `plans/completed/` verschieben,
- Typ-Schlüssel und Versionsklassifikation im Abschlussbericht nennen.

## Übergabeformat Planer → Implementierer

```md
## Freigegebener Umfang

## Versionswirkung

## Neue Typ-Schlüssel

## Betroffene Dateien und Symbole

## Verbindliche Semantik

## Daten- und Anschlussvertrag

## Implementierungsschritte

## Prüfungen

## Abnahmekriterien

## Risiken und Nicht-Ziele
```

## Übergabeformat Implementierer → Verifizierer

```md
## Umgesetzter Plan

## Reservierte Version und Klassifikation

## Neue oder veränderte Typ-Schlüssel

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

## Warum es gegen Plan, Vertrag oder Versionsklassifikation verstößt

## Reproduktions- oder Prüfschritt

## Korrekturbedingung
```