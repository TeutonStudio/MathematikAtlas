# Mathematik Atlas – v3 Alpha

## Zweck

v3 Alpha ist der erste integrierte Arbeitsstand des v3-Versionsraums. Er
stabilisiert Kartenlebenszyklus, große Graphen, Visualisierung und
Erweiterungsarchitektur, ohne neue persistierbare Knotentypen einzuführen.

## Enthaltene Änderungsschritte

### 1. Strukturformen und Zerlegung

- Strukturrechner unterscheiden Tupel-, Vektor-, Matrix- und Tensorformen
  präziser.
- Matrix- und Tensorzerlegung sowie die zugehörige Indexierung wurden ergänzt
  und mit Regressionstests abgesichert.
- Bestehende Knotenarten und stabile Anschluss-IDs bleiben erhalten.

### 2. Kartenlebenszyklus und Bedienung

- Die zuletzt aktive Karte wird über Activity- und Prozessneuerstellung hinweg
  anhand ihrer stabilen ID wiederhergestellt.
- Vor Kartenwechsel und bei `ON_STOP` wird die flüchtige
  Verbindungsvorschau beendet und der aktuelle Kartenstand gesichert.
- Mehrfinger-Zoom auf visuellen Gruppen und die Renderauswahl während
  Kamerabewegungen wurden stabilisiert.

### 3. Einheitliche Oberfläche

- Semantische Atlas-Abstände vereinheitlichen Dialoge, Karten, Listen und
  Aktionsbereiche.
- API-27-spezifische Systemleistenattribute liegen in qualifizierten
  Ressourcen und verursachen auf API 26 keinen Lint-Verstoß.
- Das Matrixprodukt-Konzept besitzt eine native Falk-Schema-Darstellung.

### 4. Adaptive mathematische Visualisierung

- Kurven, implizite Flächen und Volumen werden bildraumbezogen und adaptiv
  abgetastet.
- Triangulierte Methoden- und Isoflächen sowie gefüllte R²-Regionen vermeiden
  die vorherigen rasterbedingten Lücken.
- Budgets, unbekannte Bereiche und Coroutine-Abbruch bleiben explizit.

### 5. Semantisches Abstraktionswerkzeug

- Eine fachneutrale Teilgraphanalyse erkennt innere sowie ein- und ausgehende
  Grenzverbindungen.
- Teilgraphersetzungen werden vor der Anwendung gegen den analysierten Snapshot,
  Anschlussverträge, Kardinalität und Zyklusfreiheit validiert und bilden genau
  einen Undo-Schritt.
- Ein erweiterbares Abstraktionsregister trennt Graphmechanik, mathematische
  Erkennung und App-Darstellung.
- Die erste Regel erkennt Polynome exakt aus Konstanten, Variablen, Addition,
  Multiplikation und nichtnegativen ganzzahligen Potenzen. Sie ersetzt sichere
  Rechengraphen durch vorhandene Tupel- und Polynomknoten.
- Gemeinsam genutzte Zwischenknoten werden nicht gelöscht. Nachgeschaltete
  `TermZuMethode`-Knoten und ihre Methodenschnittstelle bleiben erhalten.
- Der Dialog „Graph vereinfachen“ sortiert Vorschläge nach realer
  Knotenersparnis, markiert Kandidaten im Graph und analysiert nach einer
  Ersetzung neu.

### 6. Import- und Speicherstabilisierung

- `.matlas`-Container können zusätzlich zum Export wieder importiert werden.
- Pfade, Manifest, Formatversion, Größen und SHA-256-Prüfsumme werden vor der
  Übernahme validiert; aufgeblähte ZIP-Einträge werden begrenzt gelesen.
- Datei-I/O für Import und Export läuft außerhalb des UI-Threads und Fehler
  erscheinen als sichtbarer Dialogzustand.
- Ist die neueste Kartenversion beschädigt, verwendet Bibliothek und
  `ladeAktuell` die jüngste noch lesbare Version.

## Verifikation

- Repository-, Releaseplan- und Versionsfolgeprüfung
- vollständige JVM-Tests einschließlich Desktop-Shadowmodule
- Android-Lint für Debug
- Android-Debug-APK

Nicht durch die lokale Prüfung ersetzt werden Gerätebedienung, Frame-Timing und
Lebenszyklustests auf einem realen Android-Gerät.
