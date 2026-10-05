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

## Geplante Alpha-Erweiterungen

### 7. Kartenbibliothek nach Herkunft filtern

Die bisher gemeinsam dargestellte Kartenbibliothek soll nach der Herkunft der
Karten getrennt werden. Die Auswahl erfolgt über ein Dropdown-Menü links neben
der Aktion zum Archivieren der aktuellen Karte.

Die Bibliothek unterscheidet drei Bereiche:

- **Eigene Karten:** lokal vom Nutzer erstellte Karten und Sammlungen.
- **Vordefinierte Karten:** mit der Anwendung ausgelieferte Standardkarten.
- **Fremdnutzerkarten:** importierte Karten und Sammlungen anderer Nutzer oder
  aus externen Quellen.

Die Zuordnung darf nicht aus sichtbaren Ordnernamen wie `Standardkarten` oder
`Freigaben` abgeleitet werden. Vorhandene Provenienzdaten für Standardkarten
und Freigabepakete bilden die Grundlage; für weitere externe JSON-, `.matlas`-
und Linkimporte wird eine app-lokale Herkunftsmetadaten-Schicht ergänzt. Das
allgemeine `KartenDaten`- und Graphformat bleibt davon unberührt.

#### Bereichsabhängige Aktionen

- Bei **Eigene Karten** bleiben `Neue Karte` und `Ordner +` erhalten.
- Bei **Vordefinierte Karten** werden weder `Neue Karte` noch `Ordner +`
  angeboten.
- Bei **Fremdnutzerkarten** wird `Neue Karte` durch `Karten importieren`
  ersetzt.
- `Ordner +` wird bei **Fremdnutzerkarten** durch
  `Eigene veröffentlichen` ersetzt.
- Der Archivieren-Button bleibt unabhängig vom gewählten Bibliotheksfilter auf
  die aktuell geöffnete Karte bezogen.

Beim Filtern werden nicht nur Karten, sondern auch die sichtbaren Ordnerbäume
reduziert. Ein Ordner wird in den Bereichen für vordefinierte oder fremde
Karten nur angezeigt, wenn er eine sichtbare Karte oder einen sichtbaren
Unterordner enthält. Eigene leere Benutzerordner bleiben dagegen sichtbar.

#### Karten importieren

Der neue Importdialog bündelt die bislang verteilten Importwege und bietet drei
Eingabearten:

1. **JSON:** Karten-JSON beziehungsweise ein bestehendes Freigabepaket direkt
   einfügen oder als Datei auswählen.
2. **.matlas:** einen versionierten `.matlas`-Container als Datei auswählen
   und über die vorhandene validierte Container-Pipeline importieren.
3. **Link:** eine HTTPS-Adresse angeben, deren Inhalt als JSON oder
   `.matlas` geladen und anschließend durch dieselbe Importpipeline geprüft
   wird.

Dateiendungen und MIME-Typen dienen nur als Hinweise; das tatsächliche Format
wird anhand des Inhalts erkannt. Linkimporte erhalten Größen-, Timeout- und
Redirect-Grenzen und akzeptieren keine lokalen oder unsicheren URL-Schemata.

Alle Importwege aktualisieren Kartenbestand, Ordnung und Herkunft atomar und
öffnen nach erfolgreichem Import die importierte Wurzelkarte. Extern
importierte Karten erscheinen anschließend unter **Fremdnutzerkarten**. Eine
erneut importierte Freigabe der eigenen lokalen Profilidentität bleibt den
eigenen Karten zugeordnet.

#### Eigene veröffentlichen

`Eigene veröffentlichen` öffnet einen zentralen Dialog für ausschließlich
eigene Karten und Sammlungen. Die erste Ausbaustufe verwendet die vorhandene
Freigabepaket- und Android-Teilen-Pipeline. Die Veröffentlichungslogik wird
hinter einem eigenen Dienst gekapselt, damit später ein servergestützter
öffentlicher Kartenlink ergänzt werden kann, ohne die Bibliotheksoberfläche
erneut umzubauen.

Toolbar-Import und Bibliotheksimport verwenden dieselbe Importlogik; parallele
Sonderimplementierungen für Dateiauswahl, JSON und `.matlas` sollen nicht
bestehen bleiben.

#### Abnahmekriterien

- Der Filter trennt eigene, vordefinierte und fremde Karten anhand stabiler
  Herkunftsdaten.
- Ordner erscheinen nur in den jeweils passenden Bereichen.
- Die Aktionsleiste entspricht für jeden Bereich exakt dem beschriebenen
  Zustand.
- JSON-, Freigabepaket-, `.matlas`- und Linkimport laufen durch eine gemeinsame
  validierte Importpipeline.
- Ein `.matlas`-Export kann wieder eingelesen werden und Prüfsummen- oder
  Manifestfehler werden abgewiesen.
- Import und Veröffentlichung verändern weder das neutrale Graphformat noch
  bestehende Karten- oder Knoten-IDs unnötig.
- Modelltests decken Herkunftspriorität und Ordnerfilterung ab; Importtests
  prüfen Format-Erkennung, Roundtrip und fehlerhafte Container.
- Die Verwaltungsfenster-Vorschau zeigt die drei Bibliothekszustände mit den
  jeweils korrekten Aktionen.

## Verifikation

- Repository-, Releaseplan- und Versionsfolgeprüfung
- vollständige JVM-Tests einschließlich Desktop-Shadowmodule
- Android-Lint für Debug
- Android-Debug-APK

Nicht durch die lokale Prüfung ersetzt werden Gerätebedienung, Frame-Timing und
Lebenszyklustests auf einem realen Android-Gerät.
