# ADR-0001: Visualisierung im MathematikKnoten-Modul

- Status: akzeptiert
- Datum: 2026-07-27
- Beteiligte: Codex
- Bezug zu ExecPlan: `plans/active/2026-07-27-loesungsmenge-visualisierung.md`

## Kontext

Die erste Mengenvisualisierung benötigt Compose, darf aber weder von `app` abhängen noch mathematische oder persistierbare UI-Typen in den Kern einführen.

## Entscheidung

Die Visualisierung wird zunächst unter `MathematikKnoten/visualisierung` in die Pakete `modell`, `sampling` und `ui` gegliedert.

## Alternativen

### Alternative A

Ein neues Android-Compose-Modul `MathematikVisualisierung` einführen.

### Alternative B

Visualisierungslogik direkt in `MathematikAtlasApp.kt` einbetten.

## Begründung

`MathematikKnoten` besitzt bereits genau die benötigten zulässigen Abhängigkeiten. Ein zusätzliches Modul würde die Gradle- und Registrierungsgrenzen vergrößern, ohne in dieser ersten Version zusätzliche Wiederverwendung zu schaffen. Die Einbettung in die App würde die Abhängigkeitsrichtung verletzen.

## Konsequenzen

### Positiv

- Der Rechenkern bleibt Android- und Compose-frei.
- Sampler und Konfiguration bleiben unabhängig von der App testbar.

### Negativ

- Ein späteres eigenständiges Visualisierungsmodul erfordert das Verschieben der bereits klar abgegrenzten Pakete.

### Risiken

- Adaptive Netze können die gewünschte Zieldichte vor Erreichen des konfigurierten
  Auswertungsbudgets verfehlen. Dieser Zustand muss als teilweise Darstellung
  sichtbar bleiben; numerische Füllung darf nicht als analytischer Nachweis gelten.

## Umsetzung und Verifikation

Die Sampler- und Knotentests decken die Trennung, adaptive Kurven und Methodenflächen,
gefüllte R²-Regionen sowie R³-Isoflächen und Volumenzellen ab. Laufzeitgeometrie und
Statistik bleiben nicht persistiert; die vorhandenen Qualitätsparameter bleiben
rückwärtskompatible Knoteneigenschaften.

## Ersetzt durch

_Falls später ersetzt._
