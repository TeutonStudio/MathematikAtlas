# Architektur des Mathematik Atlas

## Zweck

Dieses Dokument beschreibt die beabsichtigten Verantwortungsgrenzen. Es behauptet nicht, dass der aktuelle Code sie bereits vollständig erfüllt. Abweichungen werden im ExecPlan und gegebenenfalls als technische Schuld dokumentiert.

## Schichten

### 1. Mathematische Domäne

Verantwortlich für:

- mathematische Objekte und Ausdrücke,
- Operationen, Relationen, Mengen und Funktionen,
- Typen und Gültigkeitsbedingungen,
- Auswertung oder symbolische Transformation,
- fachliche Fehlerzustände,
- Darstellung als LaTeX oder eine neutrale Zwischenrepräsentation.

Nicht verantwortlich für:

- React-Komponenten,
- React-Flow-Positionen,
- Auswahlzustand,
- Inspector-Layout,
- Browserereignisse.

### Mengenwertige Iterationsmethoden

Eine einwertige mengenwertige Methode verwendet ihre deklarierte Zielmenge als feste Grundmenge ihrer Mengenausgaben. Das Modell liest `A : I -> G` damit als Familie von Teilmengen von `G`, auch wenn `A : I -> P(G)` mathematisch präziser wäre. Die Grundmenge wird ausschließlich aus der Zielmenge abgeleitet, nie separat gespeichert oder über einen weiteren Anschluss gesetzt.

Die Zielmenge darf nicht vom einzigen Iterationsparameter abhängen. Der Schnitt über eine leere Indexmenge ergibt diese feste Grundmenge. Die Rechenkernvalidierung prüft diese Invariante zentral; Kartenadapter und Renderer verwenden nur die abgesicherte abgeleitete Grundmenge.

### 2. Anwendungslogik

Verantwortlich für:

- Erzeugen und Ändern fachlicher Graphobjekte,
- Kommandos oder Aktionen,
- Validierung von Benutzeroperationen,
- Koordination zwischen Graph und mathematischer Domäne,
- Undo/Redo, sofern vorhanden,
- Laden, Speichern und Migration auf Anwendungsebene.

### 3. Graphintegration

Verantwortlich für:

- Zuordnung zwischen fachlichen Graphobjekten und React Flow,
- Node-, Handle- und Edge-IDs,
- Verbindungskompatibilität,
- Registry oder Fabrik,
- Graphlayout und Position,
- Auswahl und Interaktion.

Die Graphintegration darf fachliche Regeln aufrufen, soll sie aber nicht als zweite Wahrheit duplizieren.

### 4. Darstellung

Verantwortlich für:

- Node-Karten,
- Handles,
- Edge-Darstellung,
- Inspector-Komponenten,
- KaTeX-Rendering,
- shadcn/ui-Komponenten,
- Barrierefreiheit und Interaktionsfeedback.

Darstellungskomponenten sollen möglichst deterministisch aus Props und Zustand rendern.

### 5. Persistenz

Verantwortlich für:

- serialisierbare Datenformen,
- Schema- oder Versionsinformationen,
- Migrationen,
- robuste Behandlung unbekannter oder älterer Node-Typen.

Persistierte Daten enthalten keine React-Komponenten, Callbacks, DOM-Objekte oder sonstige reine Laufzeitwerte.

## Abhängigkeitsrichtung

Bevorzugte Richtung:

```text
Darstellung -> Graphintegration -> Anwendungslogik -> Mathematische Domäne
Persistenz  -> serialisierbare Verträge der Anwendung und Domäne
```

Die mathematische Domäne kennt React Flow nicht.

## Node-Registry

Es soll genau einen maßgeblichen Weg geben, einen Node-Typ zu registrieren oder zu erzeugen. Ein Registry-Eintrag sollte, abhängig von der bestehenden Architektur, mindestens zuordnen können:

- stabiler Typ- oder Schema-Schlüssel,
- Datenvalidator oder Datenfabrik,
- React-Komponente,
- Inspector-Komponente oder Inspector-Schema,
- Handle-Vertrag,
- Version oder Migration,
- optional Icon, Titel und Kategorie.

Codex erweitert den vorhandenen Mechanismus. Es führt kein zweites Register ein, nur weil das erste unbequem ist.

## Zustandsführung

Trenne:

- persistierte fachliche Node-Daten,
- abgeleitete fachliche Ergebnisse,
- React-Flow-spezifische Layoutdaten,
- kurzlebigen UI-Zustand.

Doppelte, unabhängig veränderbare Kopien derselben Information sind zu vermeiden.

## Fehlerzustände

Fehler werden klassifiziert:

1. ungültige Konfiguration des Nodes,
2. inkompatible Verbindung,
3. fehlende Eingabe,
4. mathematisch undefinierter Zustand,
5. Auswertungsfehler,
6. unbekannter oder nicht migrierbarer persistierter Typ.

Der Node soll einen fachlich verständlichen Zustand anzeigen, ohne den gesamten Graph zu beschädigen.

## Erweiterungsregel

Eine neue Abstraktion ist gerechtfertigt, wenn mindestens eines gilt:

- mehrere vorhandene und neue Nodes benötigen denselben fachlichen Vertrag,
- ein aktueller Typ kann die neue Semantik nicht korrekt ausdrücken,
- eine bestehende Kopplung verhindert Tests oder Persistenz,
- eine neue Versionierungs- oder Kompatibilitätsgrenze ist erforderlich.

„Die Datei war lang“ genügt nicht als Architekturbegründung.
