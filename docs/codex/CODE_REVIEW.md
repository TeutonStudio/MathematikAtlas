# Code-Review-Regeln

## Reihenfolge

Reviews melden konkrete Findings vor Zusammenfassungen. Priorisiere tatsächliches Fehlverhalten gegenüber Stilfragen.

## Schweregrade

### Blockierend

- mathematisch falsches Ergebnis,
- Datenverlust oder nicht ladbare gespeicherte Graphen,
- inkompatible oder instabile Handle-IDs,
- Build oder zentrale Tests fehlschlagen wegen der Änderung,
- Sicherheitsproblem,
- wesentliche Anforderung fehlt.

### Hoch

- häufige Randfälle sind falsch,
- Registry, Inspector oder Auswertung sind nur teilweise angebunden,
- Undo/Redo, Kopieren oder Löschen beschädigt Zustand,
- fehlende Migration bei geändertem Schema,
- Architekturkopplung erzeugt absehbar widersprüchliche Zustände.

### Mittel

- seltener Randfall,
- unzureichende Validierung,
- wichtige Tests fehlen,
- Fehleranzeige ist unklar,
- unnötige Duplikation mit Wartungsrisiko.

### Niedrig

- lokale Verständlichkeit,
- Benennung,
- kleine Robustheitsverbesserung,
- Dokumentationslücke ohne aktuelles Fehlverhalten.

## Prüffelder

### Fachlichkeit

- Entspricht die Semantik der Anforderung?
- Sind Definitions- und Wertebereich korrekt?
- Sind undefinierte Zustände explizit?
- Werden mathematische Gesetze nur verwendet, wenn ihre Voraussetzungen gelten?

### Node-Vertrag

- stabiler Typ-Schlüssel,
- valide serialisierbare Daten,
- stabile Handle-IDs,
- korrekte Richtung und Kardinalität,
- zentrale Registry,
- konsistente Defaults.

### Zustand

- keine konkurrierenden Kopien derselben Daten,
- abgeleitete Werte werden abgeleitet,
- React-Flow- und Domänenzustand sind sinnvoll getrennt,
- Änderungen werden über den vorgesehenen Mechanismus ausgeführt.

### Darstellung

- Handles entsprechen dem Vertrag,
- KaTeX entspricht der fachlichen Semantik,
- Fehler- und Ladezustände sind sichtbar,
- Inspector schreibt validierte Daten,
- Interaktionen verursachen keine unbeabsichtigte Graphaktion.

### Persistenz

- Roundtrip erhält Bedeutung und Referenzen,
- Schemaversion oder Migration ist korrekt,
- unbekannte Daten werden robust behandelt,
- Kopieren erzeugt neue Instanz-IDs, behält aber Typ und Konfiguration.

### Tests

- testen Verhalten statt nur Renderingexistenz,
- enthalten mindestens einen Fehler- oder Randfall,
- schlagen bei einer realistischen fehlerhaften Implementierung fehl,
- verwenden vorhandene Testkonventionen.

## Format eines Findings

```md
### [Schweregrad] Kurzer Titel

- Ort: `pfad/datei.ts:Symbol`
- Beobachtung:
- Auswirkung:
- Reproduktion oder Evidenz:
- Korrekturbedingung:
```

Keine Findings ohne konkrete Auswirkung. Geschmack ist kein Defekt, auch wenn Softwareteams seit Jahrzehnten tapfer das Gegenteil simulieren.
