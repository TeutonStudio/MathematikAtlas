# UI: Gebundener Formelkontext und semantische Anschlussprojektionen

Status: implementiert, Plattformabnahme siehe aktiver ExecPlan.

Android und Desktop verwenden denselben Compose-Formelbauer und dieselben Vertragsinhalte. Der bearbeitete Zustand ist eine `GebundeneFormel` mit Zeichnungsentwurf. Quellen, vollständige Typen, Methodensignaturen und Voraussetzungen werden strukturell übernommen. LaTeX bleibt eine Darstellung beziehungsweise ein ausdrücklich angeforderter kontextabhängiger Import.

`AnschlussDaten.semantischeId` ist eine optionale fachneutrale Zuordnung zu einer fachlichen Komponente. Anschlussinstanz-ID und Quellenidentität bleiben getrennt; Kopieren erzeugt neue Instanzen und erhält die fachliche Zuordnung. Technische Portnamen sind eindeutig, auch bei gleichnamigen Quellen. Inspectoränderungen und Graphumformungen verwenden bestehende Kartenaktionen und Konfliktprüfungen.

Ein Ergebnis kann einen ausdrücklich aktuellen Formelsnapshot tragen. Gewöhnliche nachfolgende Berechnungen entfernen alte Snapshots. Neue Formelrechner übernehmen verbleibende eigene und angeschlossene Quellen sowie normalisierte Eingangsannahmen. Übersetzungshistorie wird am Snapshot und Ergebnis gemeinsam fortgeführt; sie ist kein aktueller Nachweis. Wertpaket v2 ergänzt den optionalen versionierten Formelkontext; nur dieses aktuelle Paketformat wird gelesen. Persistiert werden Daten, keine UI-Zustände oder Funktionen.

Rückführung ist ein zusätzlicher Modus des bestehenden Übersetzers. Ein gewähltes Herkunftszeugnis wird als Datensatz gespeichert und bei jeder Anwendung erneut geprüft. Ein fehlender oder unpassender Plan liefert eine Diagnose. Die Visualisierung verändert den ursprünglichen Wert nicht; verschachtelte Tupel benötigen eine explizite Auswahl geordneter Blattkoordinaten.

Nicht unterstützte Binder-/Operatorauswertungen werden diagnostiziert. Diese UI-Brücke führt keine neuen mathematischen Algorithmen ein und zertifiziert keine unbekannten Eigenschaften. Insbesondere wird eine allgemeine symbolische Quotientenkürzung nicht durch den Renderer nachgebildet.
