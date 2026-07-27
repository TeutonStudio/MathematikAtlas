# Lösungsmenge und Visualisierung – ExecPlan

Status: abgeschlossen; vor dem nächsten Planlauf nach `plans/completed/` verschieben.

## Ziel und Nutzerwirkung

Die Karte erhält die Typen `mathematik.lösungsmenge` und `mathematik.visualisierung`. Eine Aussage wird als symbolische, gebundene Menge ausgegeben; der Visualisierungsknoten reicht den Wert fachlich unverändert weiter und zeigt endliche 2D/3D-Punktmengen sowie numerisch gesampelte definierte Mengen an.

## Nicht-Ziele

- Kein allgemeiner symbolischer Gleichungslöser.
- Keine persistierten Raster, Punktwolken oder Meshes.
- Keine neue App-Abhängigkeit für die Visualisierung und keine Änderung der exakten CAS-Entscheidung von Gleichheiten.

## Untersuchte Ist-Situation

- Die Module sind `KnotenKartenVerwalter`, `MathematikRechenSystem`, `MathematikKartenAdapter`, `MathematikKnoten` und `app`.
- `KnotenDaten.parameter` ist bisher eine String-Map; `KartenJson` liegt im App-Modul und schreibt Format 1.
- Vorlagen liegen in `MathematikKnotenVorlagen`, Auswerter in `StandardMathematikAuswerter`, Rendererwahl in `AtlasZustand`.
- Das Rechensystem führt `MengenAusdruck`, `Tupel`, `KartesischesProdukt`, Aussagen und die rekursive Funktion `enthalteneVariablen`.

## Fachliche Semantik

- `DefinierteMenge` bindet mindestens eine namens-eindeutige Variable mit Grundmenge. Die Reihenfolge ist signifikant; LaTeX kürzt gleiche Grundmengen bei mehreren Variablen zu einer Potenz ab.
- `freieVariablen` berücksichtigt Bindung in Funktionen und definierten Mengen. Substitution in definierten Mengen ersetzt nicht unter gleichnamigen Bindern.
- Der Lösungsmengenknoten bildet die eingegebene Aussage symbolisch. Automatische Variablen werden stabil nach Namen sortiert; manuelle Konfiguration folgt der gespeicherten Reihenfolge.
- Visualisierung ist immer als numerische Approximation gekennzeichnet. Sie wertet Aussagen getrennt von der CAS-Entscheidung aus.

## Daten-, Node-, Handle- und Edge-Vertrag

- `KnotenEigenschaft` ist eine rekursive, mathematikfreie persistierbare Summe in `KnotenKartenVerwalter`; `KnotenDaten.eigenschaften` ergänzt die rückwärtskompatible Parameter-Map.
- Aktionen ersetzen eine Eigenschaft, ein Objektfeld oder eine Eigenschaftsmap atomar und benutzen damit die bestehende Undo/Redo-Historie.
- Lösungsmenge: Eingang `bedingung: mathematik.aussage`, Ausgang `menge: mathematik.menge`.
- Visualisierung: Eingang/Ausgang `menge: mathematik.menge`; die Ausgabe ist der original `BedingterWert` einschließlich Annahmen.
- Visualisierungsknoten nutzt `NurKopfzeileZiehbar`; Standardknoten bleiben ganzflächig ziehbar.

## Architekturentscheidungen

- Die Visualisierung wird zunächst im Compose-Modul `MathematikKnoten` unter `visualisierung` abgegrenzt. Das vermeidet ein zusätzliches Android-/Compose-Modul bei gleicher zulässiger Abhängigkeit; Sampling und Modell sind getrennte Dateien.
- Inspectoren werden über ein Register im App-Modul ausgewählt, da der App-Inspector die Auswertung und Editoraktionen bereits besitzt.

## Meilensteine

- [x] Rechenkern: definierte Mengen, freie Variablen, Aussage-Substitution und Tests.
- [x] Neutraler Eigenschaftstyp, Aktionen und JSON-Version-2-Migration samt Tests.
- [x] Knoten-Vorlagen, Auswerter und Konfiguration.
- [x] Sampling, Renderer, Interaktion und registrierte Inspectoren.
- [x] Dokumentation, vollständige Gradle-Prüfungen, Diff-Prüfung und Commit `v2.1.0`; Commit mit dieser Nachricht erstellt.

## Persistenz und Migration

`KartenJson` liest Format 1 ohne `eigenschaften` als leere Map und schreibt Format 2. Werte werden rekursiv mit einer expliziten Typkennung serialisiert. Bestehende `parameter` bleiben unverändert erhalten.

## Risiken

Der vorhandene LaTeX-Text-Renderer unterstützt nur einen Teilumfang; die Legende verwendet daher dessen existierende Formelkomponente, während der Fachkern vollständiges LaTeX liefert. R³ nutzt absichtlich eine Oberflächen-Punktwolke mit Residuumstoleranz statt exakter Treffern.

## Entscheidungsprotokoll

- 2026-07-27: Kein neues `MathematikVisualisierung`-Modul. Begründung: `MathematikKnoten` erfüllt bereits die zulässige Compose-Abhängigkeit und die notwendige App-unabhängige Schicht; ein neues Modul würde den Umfang ohne zusätzliche Verantwortungsgrenze erhöhen.

## Ergebnis und Verifikation

- `./gradlew test` mit dem lokalen JDK 17: erfolgreich.
- `./gradlew :app:assembleDebug` mit dem lokalen JDK 17: erfolgreich.
- Unit-Tests decken definierte Mengen, freie Variablen, Aussage-Substitution, Knotenverträge, Sampling, Eigenschaftsaktionen und Format-1/Format-2-Persistenz ab.
