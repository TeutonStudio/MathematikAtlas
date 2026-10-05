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

### 8. Formelbauer: Foto- und Stifterkennung

Der Formelbauer erhält neben Tastatur und direkter strukturierter Bearbeitung
zwei zusätzliche Eingabewege: fotografierte Formeln und handschriftliche
Stifteingabe. Beide Wege dürfen keine zweite mathematische Wahrheitsschicht
einführen. Persistiert wird weiterhin nur die vom Atlas validierte strukturierte
Formel.

- Eine gemeinsame Erkennungspipeline einführen:

  ```text
  Foto / Stift
      -> FormelErkennungsDienst
      -> FormelErkennungsErgebnis
      -> LaTeX-/Token-Normalisierung
      -> kontrollierter Formelimport
      -> FormelAusdruck
      -> FormelAusdruckPruefer
      -> Formelbauer
  ```

- Den Erkennungsanbieter hinter einem austauschbaren Vertrag kapseln. Ein erster
  Remote-Adapter darf beispielsweise Mathpix verwenden; Provider-API,
  Netzwerkzugriff und Authentifizierung bleiben Implementierungsdetails der
  Infrastruktur und dürfen nicht in `FormelBauerDialog` oder den mathematischen
  Rechenkern einsickern.
- Keine dauerhaften Provider-Geheimnisse in APK oder Desktop-Binary hinterlegen.
  Falls ein externer Dienst verwendet wird, nur kurzlebige Zugriffstokens oder
  einen vergleichbaren serverseitig abgesicherten Mechanismus verwenden.
- Erkanntes LaTeX nie ungeprüft in einen Knoten schreiben. Externe Ausgabe vor
  dem Import auf die unterstützte Atlas-Syntax normalisieren und unbekannte
  Konstrukte als sichtbaren Fehler bzw. Warnzustand behandeln.
- Den derzeit auf Zahlenformeln zugeschnittenen kontrollierten LaTeX-Import zu
  einer typgeprüften Importgrenze erweitern, damit erkannte Formeln auch in
  Strukturformeln für Zahl, Menge, Aussage, Tupel, Vektor, Matrix, Tensor,
  Methode und allgemeines Objekt übernommen werden können.
- Einen Importkontext mit erwartetem `FormelTyp` und zulässigen Operatoren
  verwenden. Ein erkanntes Matrix-, Vektor- oder Aussageobjekt darf nicht nur
  deshalb als Zahlenformel behandelt werden, weil die Erkennung LaTeX liefert.
- Neben dem Ersetzen der gesamten Formel einen Import in die aktuell ausgewählte
  Teilformel bzw. einen offenen Platzhalter ermöglichen.

#### Stifteingabe

- Ein eigenes kurzlebiges Ink-Modell aus Dokument, Strichen und Rohpunkten
  einführen. Rohpunkte für die Erkennung beibehalten; visuelle Glättung darf die
  gespeicherten Erkennungsdaten nicht verfälschen.
- `PointerType.Stylus` und `PointerType.Eraser` direkt unterstützen. Zusätzlich
  einen sichtbaren Radierer-Modus für Geräte ohne Hardware-Radierer anbieten.
- In der ersten Ausbaustufe beim Radieren vollständige betroffene Striche
  entfernen; Teilstrich-Radierung kann später ergänzt werden.
- Für das Eingabefeld lokales Undo/Redo getrennt vom Undo/Redo des strukturierten
  Formelbaums führen.
- Die Erkennung nach `Pen-Up` mit kurzem Debounce auslösen statt für jeden
  Pointer-Move eine Anfrage zu senden.
- Jede Anfrage mit einer lokalen Generation bzw. Revision versehen und
  verspätete Ergebnisse älterer Revisionen verwerfen, damit alte Antworten
  neuere Handschrift niemals überschreiben.

#### Fotoeingabe

- Auf Android zunächst Systemkamera und Bildauswahl verwenden und anschließend
  einen Zuschneide-/Rotationsschritt anbieten.
- Die Erkennung auf den ausgewählten Formelausschnitt anwenden und das
  Originalbild nicht als Teil des Knotens persistieren.
- Eine Photomath-artige Live-Kamera mit automatischer Formelerkennung erst als
  späteres Inkrement auf derselben Pipeline aufbauen; sie ist keine
  Voraussetzung für den ersten produktiven Fotoimport.

#### Eingabe- und Interpretationsfeld

- Eingabe und Interpretation sichtbar trennen. Handschrift oder Foto bleiben im
  Eingabefeld, während das Interpretationsfeld die erkannte und vom Atlas
  parsebare Formel rendert.
- Eine Erkennung verändert den eigentlichen `FormelEditorZustand` nicht
  automatisch. Der Nutzer übernimmt die Interpretation ausdrücklich.
- Mindestens die Aktionen `An Auswahl einsetzen` und `Gesamte Formel ersetzen`
  anbieten, wenn der erkannte Ausdruck typkompatibel ist.
- Konfidenz, Parserfehler, nicht unterstützte Syntax, Netzwerkfehler und leere
  Erkennung als unterschiedliche Zustände anzeigen. Niedrige Konfidenz darf
  keine stille Übernahme auslösen.

#### Plattform- und Persistenzgrenze

- Plattformneutral halten: Ink-Datenmodell, Erkennungsanfrage,
  Erkennungsergebnis, Normalisierung und strukturierter Import.
- Android-spezifisch halten: Kamera, Photo Picker, Stylus-/Eraser-Anbindung und
  gegebenenfalls Tokenbeschaffung.
- Desktop später über dieselben Verträge mit Bilddatei, Maus/Grafiktablett und
  optional Webcam anbinden, ohne einen zweiten Formelimport zu entwickeln.
- Rohbilder, Ink-Dokumente, Netzwerkantworten und Konfidenzen standardmäßig nur
  als kurzlebigen UI-Zustand behandeln. Persistiert wird nach Bestätigung nur
  der strukturierte `FormelAusdruck` bzw. dessen bestehende kanonische
  Repräsentation.

#### Umsetzungsreihenfolge und Abnahme

1. gemeinsamen typisierten Formelimport und Normalisierung vervollständigen,
2. provider-neutralen `FormelErkennungsDienst` und Fake-Implementierung für Tests
   einführen,
3. Stiftfeld mit Stylus, Touch, Radierer und lokalem Undo/Redo umsetzen,
4. inkrementelle Handschrifterkennung mit Debounce und Revisionsschutz anbinden,
5. Fotoaufnahme/-auswahl, Zuschnitt und Rotation ergänzen,
6. Interpretationsfeld und ausdrückliche Übernahme in Auswahl oder Gesamtformel
   integrieren,
7. Strukturformeln jenseits reiner Zahlenformeln abdecken,
8. Datenschutz-, Authentifizierungs-, Offline- und Fehlerpfade härten,
9. gemeinsame Verträge für spätere Desktop-Eingabe erhalten.

Die Funktion gilt für die Beta als abgenommen, wenn mindestens:

- eine fotografierte gedruckte oder handschriftliche Formel erkannt,
  interpretiert und kontrolliert übernommen werden kann,
- eine Formel mit Stylus oder Finger geschrieben und mit Hardware- oder
  UI-Radierer korrigiert werden kann,
- das Interpretationsfeld automatisch nach Schreibpausen aktualisiert wird,
- verspätete Erkennungsantworten keine neuere Eingabe überschreiben,
- erkannte Formeln immer den Atlas-Parser und `FormelAusdruckPruefer`
  durchlaufen,
- ungültige oder nicht unterstützte Erkennung die bestehende Formel nicht
  verändert,
- Teilformel- und Gesamtersatz unterstützt werden,
- Bild- und Stifteingabe denselben Erkennungsvertrag verwenden,
- keine Provider-Geheimnisse im Client ausgeliefert werden,
- keine neuen Knotentypen oder Anschluss-IDs allein für die Erkennung
  eingeführt werden.

### 9. Beta-Abnahme

- vollständige Repository-, Release-, Migrations-, JVM-, Desktop-, Lint- und
  APK-Prüfungen ausführen,
- Android-Instrumentierungs- und Geräteprüfungen für Touch, Lebenszyklus,
  Import, Stifteingabe, Radierer, Fotoerkennung und Abstraktionsdialog ergänzen,
- verbleibende Deprecation-Warnungen nach Risiko priorisieren,
- erst danach einen formellen v3-Release im Releaseplan reservieren.
