# Teststrategie für Knoten

## Grundsatz

Teste die fachliche Bedeutung auf der niedrigsten sinnvollen Ebene. UI-Tests ersetzen keine Tests der mathematischen Semantik.

## 1. Domänentests

Für mathematische Operationen:

- gültige Standardfälle,
- Grenzfälle,
- leere Eingaben,
- undefinierte Zustände,
- Typinkompatibilität,
- relevante algebraische Eigenschaften,
- deterministische Ausgabe.

Diese Tests sollen ohne React Flow ausführbar sein, sofern die Architektur dies ermöglicht.

## 2. Node-Vertragstests

Prüfen:

- Defaultdaten,
- Datenvalidierung,
- stabile Handle-IDs,
- Richtung und Kardinalität,
- Registry-Auflösung,
- Ausgabevertrag,
- Fehlerzustände.

## 3. Integrationstests

Prüfen:

- Verbindung kompatibler Nodes,
- Ablehnung inkompatibler Edges,
- Propagation von Änderungen,
- Inspector → Node-Daten → Auswertung,
- Laden und Wiederherstellen,
- Kopieren und Löschen,
- Undo/Redo, sofern vorhanden.

## 4. Darstellungstests

Nur für relevantes Verhalten:

- Handles werden mit korrekter ID und Richtung gerendert,
- KaTeX oder Formeltext entspricht den Daten,
- Fehlerzustand ist sichtbar,
- Inspector-Felder besitzen korrekte Werte und Validierung,
- Ereignisse lösen nicht unbeabsichtigt Node-Drag oder Canvas-Aktionen aus.

Vermeide fragile Tests, die nur zufällige DOM-Struktur oder CSS-Klassen festschreiben.

## 5. Persistenztests

Wenn Node-Daten gespeichert werden:

- Serialisierungs-Roundtrip,
- Laden älterer Schemaversionen,
- Defaultwerte fehlender Felder,
- stabile Edge-Referenzen,
- robuste Behandlung unbekannter Typen.

## 6. Prüfbefehle

Codex bestimmt Befehle aus:

1. Lockdatei,
2. `package.json`,
3. Vite- und Testkonfiguration,
4. CI-Konfiguration.

Bevorzugte Reihenfolge, sofern entsprechende Skripte existieren:

1. gezielte Tests,
2. vollständige Tests,
3. Typprüfung,
4. Lint,
5. Produktions-Build.

Ein nicht vorhandenes Skript wird nicht erfunden. Ein wegen externer Umgebung nicht ausführbarer Test wird mit konkretem Grund dokumentiert.

## 7. Mindestabdeckung für einen neuen Node

- mindestens ein fachlicher Erfolgstest,
- mindestens ein ungültiger oder unvollständiger Zustand,
- Handle- und Registry-Vertrag,
- Inspector-Änderung, falls vorhanden,
- Persistenz-Roundtrip, falls vorhanden,
- Migrationstest, falls das Schema geändert wurde.
