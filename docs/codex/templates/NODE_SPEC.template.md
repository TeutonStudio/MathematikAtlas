# Knotenspezifikation: <Name>

## Status

- Entwurf / mathematisch geprüft / freigegeben / implementiert / verifiziert

## Zweck

## Nicht-Ziele

## Mathematische Bedeutung

### Eingaben

### Ausgabe

### Voraussetzungen

### Undefinierte, unbekannte und leere Fälle

### Relevante mathematische Gesetze

## Knotenidentität

- Art-Schlüssel:
- Titel:
- Kategorie:
- zuständiger Vorlagenkatalog:
- zuständiger Auswerter:
- Daten- oder Formatversion:

## Modulzuordnung

| Verantwortung | Modul | bestehender Einstiegspunkt |
|---|---|---|
| Domänenmodell | `MathematikRechenSystem` | |
| Graph-/Editorvertrag | `KnotenKartenVerwalter` | |
| Auswertung | `MathematikKartenAdapter` / `MathematikKnoten` | |
| Renderer | `MathematikKnoten` | |
| App und Persistenz | `app` | |

## Persistierte Daten

```kotlin
// An vorhandene KnotenDaten-, Parameter- und KnotenEigenschaft-Typen anpassen.
```

## Abgeleitete Daten und Laufzeitzustand

## Anschlüsse

| Name | Richtung | Kante | Anschlussart | Kardinalität | Reihenfolge | erweiterbar | Pflicht |
|---|---|---|---|---|---|---|---|

### ID- und Migrationsregeln

## Verbindungskompatibilität

## Auswertung

### Eingabesammlung

### Ergebnis und Fehlerzustände

### Cacheverhalten

## Darstellung und nativer Formelrenderer

- Renderer:
- `KnotenInteraktionsModus`:
- erzeugtes LaTeX oder Formelmodell:
- benötigter unterstützter Formelteilumfang:
- Fehler- und Leerzustand:
- Größenverhalten:

## Inspector

| Feld | Datenträger | Typ | Default | Validierung | Kartenaktion | Auswirkung |
|---|---|---|---|---|---|---|

## Registrierung und Erzeugung

- `MathematikKnotenVorlagen.alle`:
- `MathematikAuswerterRegister`:
- `AnschlussArtRegister`:
- Renderer-Zuordnung:
- Kategorie und Suche:

## Persistenz und Migration

- JSON-Felder:
- Defaultwerte älterer Karten:
- Anschlussmigration:
- Auswirkung auf `formatVersion`:
- Kopieren und Duplizieren:
- Gruppenknotenverhalten:

## Tests

### Domäne

### Vorlage und Auswerter

### Graph und Undo/Redo

### Persistenz

### Compose- oder Laufzeitprüfung

## Prüfbefehle

```bash
# Nur tatsächlich vorhandene Befehle eintragen.
```

## Risiken und bewusste Grenzen

## Abnahmekriterien

- [ ]