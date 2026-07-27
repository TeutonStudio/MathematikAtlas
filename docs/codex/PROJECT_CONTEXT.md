# Projektkontext: Mathematik Atlas

## Produktvision

Der Mathematik Atlas ist eine Anwendung zur Visualisierung mathematischer Vorgänge als interaktiver Node-Graph. Nutzer sollen mathematische Objekte und Verarbeitungsschritte als Knoten zusammensetzen, über Anschlüsse verbinden und die resultierenden Zusammenhänge visuell, symbolisch und später gegebenenfalls numerisch untersuchen können.

## Gemeinsame Sprache

| Projektbegriff | Technischer Begriff | Bedeutung |
|---|---|---|
| Knoten | Node | Karte oder Element im Graph |
| Anschluss | Handle | definierter Ein- oder Ausgang eines Knotens |
| Verbindung | Edge | gerichtete oder fachlich definierte Verbindung zwischen Anschlüssen |
| Inspector | Eigenschaftenansicht | Bearbeitung der Konfiguration eines ausgewählten Graphobjekts |
| mathematischer Ausdruck | Expression | strukturierte fachliche Repräsentation, nicht bloß ein LaTeX-String |
| Darstellung | View | visuelle Repräsentation eines fachlichen Objekts |

## Technischer Rahmen

- Build- und Entwicklungsumgebung: Vite
- UI: React
- Node-Graph: React Flow
- Komponenten: shadcn/ui
- Formeldarstellung: KaTeX
- Sprache, konkrete Paketnamen und Versionen: aus dem Repository ermitteln
- Paketmanager: anhand der vorhandenen Lockdatei bestimmen

## Produktprinzipien

1. **Mathematik ist strukturiert.** Ein Ausdruck wird fachlich modelliert und nicht ausschließlich als String behandelt.
2. **Darstellung ist austauschbar.** Node-UI, KaTeX und spätere Visualisierungen greifen auf dasselbe fachliche Modell zurück.
3. **Verbindungen sind typisiert.** Nicht jede Edge ist fachlich zulässig.
4. **Fehler sind sichtbar.** Undefinierte oder inkompatible Zustände werden als Zustand modelliert und nicht verschluckt.
5. **Bearbeitung ist nachvollziehbar.** Änderungen über Inspector oder Graphoperationen sollen stabil speicherbar und soweit vorhanden rückgängig machbar sein.
6. **Erweiterbarkeit erfolgt über Verträge.** Neue Node-Typen verwenden gemeinsame Registrierungs-, Daten- und Testkonventionen.

## Noch aus dem Repository zu vervollständigen

Diese Abschnitte dürfen nur nach Codeprüfung aktualisiert werden:

### Tatsächliche Quellverzeichnisse

- Anwendungseinstieg: _noch nicht verifiziert_
- Node-Komponenten: _noch nicht verifiziert_
- Node-Datentypen: _noch nicht verifiziert_
- Handle- und Edge-Typen: _noch nicht verifiziert_
- Inspector: _noch nicht verifiziert_
- mathematisches Domänenmodell: _noch nicht verifiziert_
- Persistenz: _noch nicht verifiziert_
- Tests: _noch nicht verifiziert_

### Tatsächliche zentrale Symbole

- Node-Registry oder Fabrik: _noch nicht verifiziert_
- Graphzustand: _noch nicht verifiziert_
- Serialisierungsschema: _noch nicht verifiziert_
- Ausdruckstyp: _noch nicht verifiziert_
- Validierungsmechanismus: _noch nicht verifiziert_

## Nicht automatisch annehmen

- Dass das Projekt TypeScript statt JavaScript verwendet.
- Dass das aktuelle React-Flow-Paket einen bestimmten Importnamen besitzt.
- Dass bereits ein mathematisches CAS oder Ausdruckssystem existiert.
- Dass Persistenz, Undo/Redo oder Migrationen bereits implementiert sind.
- Dass die in `ARCHITECTURE.md` beschriebene Zieltrennung schon vollständig umgesetzt ist.

Codex muss den Istzustand untersuchen und Abweichungen explizit benennen.
