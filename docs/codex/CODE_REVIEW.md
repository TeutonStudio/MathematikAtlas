# Code-Review-Regeln

## Reihenfolge

Reviews melden konkrete Findings vor Zusammenfassungen. Priorisiere tatsächliches Fehlverhalten gegenüber Stilfragen.

## Schweregrade

### Blockierend

- mathematisch falsches Ergebnis,
- Datenverlust oder nicht ladbare gespeicherte Karten,
- inkompatible oder instabile Anschluss-IDs,
- Build oder zentrale Tests schlagen wegen der Änderung fehl,
- verbotene Modulabhängigkeit,
- Sicherheitsproblem,
- wesentliche Anforderung fehlt.

### Hoch

- häufige Randfälle sind falsch,
- Vorlage, Auswerter, Renderer oder Inspector sind nur teilweise angebunden,
- Undo/Redo, Kopieren oder Löschen beschädigt Zustand,
- fehlende Migration bei geändertem Schema,
- der neutrale Karteneditor enthält mathematische oder app-spezifische Konventionen,
- Architekturkopplung erzeugt absehbar widersprüchliche Zustände.

### Mittel

- seltener Randfall,
- unzureichende Validierung,
- wichtige Tests fehlen,
- Fehleranzeige ist unklar,
- unnötige Duplikation mit Wartungsrisiko,
- Dokumentation weist Agenten auf falsche Werkzeuge oder Module.

### Niedrig

- lokale Verständlichkeit,
- Benennung,
- kleine Robustheitsverbesserung,
- Dokumentationslücke ohne aktuelles Fehlverhalten.

## Prüffelder

### Fachlichkeit

- Entspricht die Semantik der Anforderung?
- Sind Definitions- und Wertebereich korrekt?
- Sind undefinierte, unbekannte und unentscheidbare Zustände getrennt?
- Werden mathematische Gesetze nur verwendet, wenn ihre Voraussetzungen gelten?
- Stammt die mathematische Wahrheit aus Rechenkern oder Auswerter statt aus der Darstellung?

### Knotenvertrag

- stabiler Art-Schlüssel,
- valide serialisierbare Daten,
- stabile Anschluss-IDs,
- korrekte Richtung, Anschlussart, Reihenfolge und Kardinalität,
- vorhandener Vorlagen- und Auswerterweg,
- konsistente Defaults.

### Modulgrenzen

- `MathematikRechenSystem` bleibt Android- und Compose-frei,
- `KnotenKartenVerwalter` bleibt frei von Mathematik- und App-Konventionen,
- `MathematikKartenAdapter` enthält keine UI- oder Persistenzdialoge,
- `MathematikKnoten` versteckt keine App-Zustände,
- Bibliotheksmodule importieren nicht aus `app`.

### Zustand

- keine konkurrierenden Kopien derselben Daten,
- abgeleitete Werte werden abgeleitet,
- persistierte Kartendaten und kurzlebiger Compose-Zustand sind getrennt,
- Änderungen laufen über die vorgesehenen Kartenaktionen,
- Drag- und Mehrfachaktionen ergeben nachvollziehbare Undo-Schritte.

### Darstellung

- Anschlüsse entsprechen dem Vertrag,
- nativer Formeltext entspricht der fachlichen Semantik,
- der Renderer erwartet keinen tatsächlich nicht unterstützten LaTeX-Umfang,
- Fehler- und Ladezustände sind sichtbar,
- der Inspector schreibt validierte Daten,
- Gesten verursachen keine unbeabsichtigte Knoten-, Karten- oder Verbindungsaktion.

### Persistenz

- JSON-Roundtrip erhält Bedeutung und Referenzen,
- Formatversion oder Migration ist korrekt,
- unbekannte Daten werden robust behandelt,
- Kopieren erzeugt neue Instanz-IDs und behält Art und Konfiguration,
- Anschlussänderungen lassen bestehende Verbindungen nicht unbeabsichtigt verwaisen,
- Auswertungscaches und Compose-Laufzeitobjekte werden nicht gespeichert.

### Tests

- testen Verhalten statt nur die Existenz eines Renderers,
- enthalten mindestens einen Fehler- oder Randfall,
- schlagen bei einer realistischen fehlerhaften Implementierung fehl,
- verwenden vorhandene Kotlin-, JUnit- und Gradle-Konventionen,
- behaupten keine Emulator- oder Geräteprüfung, wenn nur JVM-Tests liefen.

## Format eines Findings

```md
### [Schweregrad] Kurzer Titel

- Ort: `pfad/Datei.kt:Symbol`
- Beobachtung:
- Auswirkung:
- Reproduktion oder Evidenz:
- Korrekturbedingung:
```

Keine Findings ohne konkrete Auswirkung. Geschmack ist kein Defekt, auch wenn Softwareteams seit Jahrzehnten erstaunlich viel Arbeitszeit in dieses Missverständnis investieren.