# Gemeinsame Mathematik-Kartenlaufzeit

## 1. Status und Basis

**Status:** in Umsetzung als dritte Architekturphase von `v2.32.1`.

**Basis:** `release/v2.32.1-architektur-refactor` nach Übernahme der gemeinsamen Persistenzpipeline, Commit `bc10d7da6effcb1bf8626a0f8d31ce0c590df36a`.

## 2. Ziel

Android und Desktop sollen die gemeinsame mathematische Karteninfrastruktur nicht mehr unabhängig zusammensetzen. Anschlussartenregister, Graphprüfung, vollständiges Auswerterregister, Auswertungscache und kanonischer Mathematikkatalog werden in `MathematikKartenLaufzeit` genau einmal verdrahtet.

Plattformzustände bleiben für UI, Navigation, Dateisystem und plattformspezifische Synchronisierung verantwortlich.

## 3. Ausgangslage

`AtlasZustand` und `DesktopAtlasZustand` erzeugten jeweils separat:

- `AnschlussArtRegister(MathematikAnschlussArten.alle)`,
- `GraphPrüfung`,
- `KartenAuswerter` mit `GesamterMathematikAuswerter`,
- Knotenkatalog beziehungsweise Katalogzugriff,
- Cache-Neuberechnung.

Dadurch konnte sich die Laufzeitkonfiguration trotz bereits gemeinsamer Fachmodule zwischen den Plattformen auseinanderentwickeln.

## 4. Nicht-Ziele

- keine Zusammenlegung von Android- und Desktop-UI-Zuständen,
- keine Änderung des Editorzustands oder seiner Undo-/Redo-Semantik,
- keine Änderung der zusätzlichen Android-Synchronisierung von Restriktions- und Methodenanschlüssen,
- keine Änderung der Persistenzpipeline,
- keine Kotlin-Multiplatform-Migration der Shadowmodule.

## 5. Umsetzung

- [x] `MathematikKartenLaufzeit` unter `MathematikKnoten/laufzeit` einführen.
- [x] Anschlussarten, Graphprüfung, Gesamtauswerter, Cache und kanonischen Katalog dort zusammensetzen.
- [x] Android `AtlasZustand` auf die Laufzeit umstellen.
- [x] Desktop `DesktopAtlasZustand` auf dieselbe Laufzeit umstellen.
- [x] Android-spezifische Nachsynchronisierung unverändert erhalten.
- [x] Laufzeit und Cachezugriffe regressionsprüfen.
- [x] Architekturprüfung gegen erneute Plattformverdrahtung härten.
- [ ] GitHub-Actions-Verifikation auswerten.

## 6. Abhängigkeitsvertrag

`MathematikKartenLaufzeit` darf ausschließlich bereits erlaubte Abhängigkeiten von `MathematikKnoten` nutzen:

- Graphdaten und `GraphPrüfung` aus `KnotenKartenVerwalter`,
- topologische Auswertung aus `MathematikKartenAdapter`,
- mathematische Anschlussarten, Auswerter und Katalog aus `MathematikKnoten`.

Sie kennt keine Android-Kontexte, Desktopfenster, Dateisystempfade oder App-Dialoge.

## 7. Plattformzustände danach

### Android

Behält Kartenbibliothek, Brotkrumen, Suche, Auswahl-UI, Dateispeicher und die bestehende zweistufige Auswertung mit Anschluss-Synchronisierung. Die eigentliche mathematische Auswertung und Cacheverwaltung laufen über `MathematikKartenLaufzeit`.

### Desktop

Behält Desktop-Speicher, Meldungszustand und UI-Aktionen. Katalog, Graphprüfung, Auswertung und Cacheverwaltung kommen vollständig aus derselben Laufzeit wie auf Android.

## 8. Tests

- Laufzeit enthält den kanonischen Katalog und kann einen Standard-Zahlknoten auswerten.
- gezieltes und vollständiges Cache-Verwerfen bleibt funktionsfähig.
- Architekturprüfung verbietet direkte Plattformkonstruktion von `KartenAuswerter`, Gesamtauswerter, Anschlussregister und kanonischem Katalog.
- vollständige JVM-, Android- und Desktop-CI nach Integration in den Releasebranch.

## 9. Risiken

Das Hauptrisiko ist eine unbemerkte Verhaltensänderung des Android-Auswertungspfads. Deshalb bleibt dessen Synchronisierungsreihenfolge exakt bestehen; lediglich die zugrunde liegenden `auswerten`, `leereCache` und `verwerfeCache`-Operationen werden delegiert.

## 10. Ergebnis

Wird nach CI-Abnahme ergänzt.
