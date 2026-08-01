# Karten-Dateiformat

Jede Version einer Karte ist eine eigenständige JSON-Datei. Das Format beginnt mit `formatVersion: 1`.

```json
{
  "formatVersion": 1,
  "id": "stabile-karten-id",
  "name": "Doppeln",
  "version": 2,
  "erstelltAm": 1785000000000,
  "archiviert": false,
  "ansicht": { "x": 0, "y": 0, "zoom": 1 },
  "knoten": [],
  "verbindungen": []
}
```

Ein Knoten enthält seine generische Art, Position, Größe, Parameter, Anschlüsse und optional einen festen Kartenverweis:

```json
{
  "id": "knoten-id",
  "art": "gruppe.karten-id",
  "name": "Doppeln",
  "kartenVerweis": {
    "kartenId": "karten-id",
    "version": 1
  }
}
```

Da das Editorformat Knotentypen generisch als Artkennung plus Parameter speichert, bleiben unbekannte Knotentypen ladbar und erneut speicherbar. Die App zeigt für nicht registrierte Typen den allgemeinen Renderer; die Auswertung meldet einen fehlenden Auswerter, statt Daten zu entfernen.

## Versionierungsregel

- Ist die aktuelle Version in keiner anderen Karte als Gruppenknoten referenziert, wird sie weiterbearbeitet.
- Wird sie referenziert, erzeugt die nächste Speicherung eine neue Version mit neuem Erstellungsdatum.
- Bestehende Referenzen werden niemals automatisch umgebogen.
- Archivierte Karten bleiben für vorhandene Referenzen lesbar, werden aber nicht mehr als neue Gruppenknoten angeboten.

## KartenKnoten-Zustände

Ein fest versionierter Kartenverweis besitzt zwei kompatible Darstellungen, ohne das JSON-Schema zu erweitern:

- `gruppe.<karten-id>` spiegelt die öffentlichen `KartenEingang`- und `KartenAusgang`-Knoten als Anschlüsse.
- `methode.<karten-id>` besitzt keine Eingänge und genau einen Funktionsausgang. Alle öffentlichen Karteneingänge werden geordnet nach ihrer Kartenposition zu Methodenparametern; mehrere benannte Kartenausgänge bleiben erhalten.

Karten- und Versionswechsel behalten Anschluss-IDs nur bei gleicher Richtung und gleichem öffentlichen Namen. Dadurch bleiben ausschließlich weiterhin gültige Verbindungen bestehen. Ein Zustandswechsel ersetzt die Schnittstelle atomar und entfernt die betroffenen Verbindungen in einem Undo-Schritt.
