# Mathematik Atlas – v3 Beta

## Zweck

v3 Beta baut auf dem Alpha-Stand auf und schließt die noch offenen
Semantik-, Plattform- und Laufzeitverträge vor einer stabilen v3-Version. Diese
Datei beschreibt die geplante Reihenfolge; sie behauptet keine bereits
implementierten Funktionen.

## Geplante Änderungsschritte

### 1. Inhaltsgleichheit und Selbigkeit

- V3-004 aus `V3_AENDERUNGEN.md` als zentralen Vergleichsdienst umsetzen.
- Inhaltsgleichheit typübergreifend nur für ausdrücklich definierte Familien
  erlauben.
- Selbigkeit rekursiv typ-, orientierungs- und formtreu modellieren.
- Gleichheit, Ungleichheit, endliche Mengen und Definitionskarten gemeinsam
  migrieren und das Gesetz `selbig(a, b) => gleich(a, b)` testen.

### 2. Aussage-Standardwerte

- V3-005 mit persistierten Wahr/Falsch-Standardwerten für unverbundene
  Aussageeingänge umsetzen.
- Priorität Verbindung vor Standardwert vor sicherem Fallback gewährleisten.
- Inspector, dynamische Anschlüsse, Kopieren, Migration und Undo/Redo abdecken.

### 3. Einheitliche Adaptergeometrie

- V3-006 für Typ-, Orientierungs- und Repräsentationsadapter abschließen.
- Die fachneutrale Formklassifikation im Karteneditor halten und mathematische
  Zuordnung im Mathematikmodul registrieren.
- Trefferflächen, Auswahlrahmen, Anschlusspositionen und MiniMap prüfen.

### 4. Große Graphen vollständig entkoppeln

- Viewportzustand und persistierbaren Karteninhalt vollständig trennen.
- Räumlichen Index, stabile Weltgeometrie- und Anschlusscaches sowie
  MiniMap-Inhaltsgrenzen an Inhaltsrevisionen koppeln.
- Pan und Zoom ohne mathematische Auswertung oder neue fachliche Kartenversion
  ausführen.
- Eine reproduzierbare Karte mit mindestens 1.000 Knoten und 1.500 Verbindungen
  auf einem festgelegten Android-Referenzgerät messen.

### 5. Abstraktionsregeln erweitern

- Bereits methodengehobene Rechnergraphen unter vollständigem Vergleich von
  Signatur, Argumentreihenfolge, Wertevorrat, Zielmenge und Ausgangsprojektion
  unterstützen.
- Weitere sichere Regeln für Potenzen, Skalar- und Matrixprodukte,
  Tupelkonstruktionen und wiederkehrende Teilgraphen ergänzen.
- Überlappende Vorschläge weiter einzeln anwenden; eine globale Optimierung erst
  nach einem nachgewiesenen konfliktfreien Auswahlverfahren anbieten.

### 6. Zustands- und I/O-Härtung

- Persistierenden Import- und Speicheranteil vollständig aus dem UI-Thread
  verlagern.
- Navigation und aktive Karte über einen lifecycle-festen Zustandsbesitzer mit
  `SavedState` wiederherstellen.
- Beschädigte Versionen und vorhandene Sicherungen in der Oberfläche sichtbar
  diagnostizieren und kontrolliert wiederherstellen.

### 7. Plattformmodernisierung

- Issue #395 erst umsetzen, wenn Kotlin und Android Gradle Plugin einen
  offiziell unterstützten gemeinsamen KMP-Pfad besitzen.
- Dann Desktop-Shadowmodule durch echte gemeinsame Quellsets ersetzen und die
  Whitelist schrittweise auf null reduzieren.
- Kein verstecktes AGP-Downgrade und keine nicht unterstützte
  Kotlin-/AGP-Kombination verwenden.

### 8. Beta-Abnahme

- vollständige Repository-, Release-, Migrations-, JVM-, Desktop-, Lint- und
  APK-Prüfungen ausführen,
- Android-Instrumentierungs- und Geräteprüfungen für Touch, Lebenszyklus,
  Import und Abstraktionsdialog ergänzen,
- verbleibende Deprecation-Warnungen nach Risiko priorisieren,
- erst danach einen formellen v3-Release im Releaseplan reservieren.
