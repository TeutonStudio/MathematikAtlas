# Globales Spacing-System und natives Falk-Schema

## Status

Abgeschlossen am 2026-10-04 auf `samai/v3.0.0/alpha`.

## Ziel und Nutzerwirkung

Die Android-Oberfläche verwendet eine kompakte, einheitliche Abstandsskala. Der
Falk-Reiter des Matrixprodukts zeigt ein echtes, interaktives Falk-Schema statt
einer unveränderlichen Konzeptkarte aus Dokumentationsknoten. Auswertbare
Eingaben des geöffneten Matrixprodukt-Knotens werden direkt dargestellt;
ansonsten erscheint ein gekennzeichnetes Standardbeispiel.

## Nicht-Ziele

- Keine Änderung an Desktop-UI oder neutralen Editor-Renderern.
- Keine neue Matrixproduktsemantik, Persistenz- oder Kartenformatänderung.
- Keine benutzerseitig umschaltbare Oberflächendichte.
- Keine Änderung graphischer Koordinaten, Touch-Ziele oder mathematisch
  bedingter Zellgrößen.

## Untersuchter Istzustand

- App-Abstände sind als lokale `dp`-Werte über zahlreiche Compose-Dateien verteilt.
- `MathematikAtlasTheme` stellt nur Farbe und Darstellungssteuerung bereit.
- Der Reiter mit der stabilen ID
  `mathematik.matrixProdukt|Matrixprodukt|.4c459827.falksches-schema` lädt
  ein Asset mit fünf unverbundenen `konzept.regel`-Knoten.
- `detailliertesFalkSchema` und `alsMatrixFaktor` bilden die benötigte Semantik
  bereits im Compose-freien Rechenkern ab.
- Der Arbeitsbaum enthält fremde IDE-Änderungen; sie bleiben unangetastet.

## Fachliche Semantik und Verträge

Das UI liest ausschließlich die vom Auswerter protokollierten Eingänge `a` und
`b`. Matrizen und orientierte Vektoren werden mit `alsMatrixFaktor` normalisiert.
`detailliertesFalkSchema` liefert Ergebnis, Summanden und Dimensionsdiagnose.
Die Faktorfolge wird weder im UI vereinfacht noch vertauscht.

Der Karten-, Knoten-, Anschluss- und JSON-Vertrag bleibt unverändert. App-intern
erhält ein Konzeptreiter eine Darstellungsart `Karte` oder `FalkSchema`.

## Architekturentscheidungen

- `AtlasAbstände` wird als CompositionLocal im App-Theme bereitgestellt.
- Die Skala besitzt 2, 4, 6, 8, 12, 16, 20 und 24 dp sowie semantische
  Padding-Werte für Listen, Bereiche, Dialoge und Aktionsleisten.
- Nur Layout-Weißräume der Android-App werden migriert; fachliche Rastermaße
  bleiben lokal.
- Das alte Falk-Asset bleibt zur Manifest- und ID-Kompatibilität erhalten, wird
  im Falk-Reiter aber weder gerendert noch als bearbeitbare Karte angeboten.
- Reale inkompatible Eingaben erzeugen eine Diagnose. Nur fehlende oder nicht
  auswertbare Eingaben führen zum numerischen 2×3·3×2-Standardbeispiel.
- Matrizen bis 8×8 werden vollständig gezeigt. Darüber werden mittlere
  Zeilen/Spalten ausgelassen, wobei die aktuelle Auswahl sichtbar bleibt.
- Matrixspalten verwenden die intrinsische Breite ihres längsten sichtbaren
  Terms. Das Schema verbreitert sich horizontal, statt Terme abzuschneiden.
- Knotenbibliothek und Rechner-Operatorauswahl besitzen auch an Tabs,
  Kategorien und scrollbaren Inhaltskanten explizite kompakte Innenabstände.

## Betroffene Bereiche

- Theme und App-Compose-Oberflächen unter `app/src/main/kotlin`.
- App-internes Konzeptmodell und Enzyklopädieadapter.
- Neue native Falk-Ansicht und deren reine Datenaufbereitung.
- App-Unit-Tests; vorhandene Kern-Falktests bleiben kanonisch.

## Meilensteine und Fortschritt

- [x] Zentrale kompakte Abstandsskala im Theme bereitgestellt.
- [x] Sichtbare App-Shells, Dialoge, Listen, Inspektoren und Konzepte migriert.
- [x] Falk-Reiter typisiert, Datenmodell abgeleitet und native Ansicht gebaut.
- [x] Unit-Tests ergänzt und gezielte Prüfungen erfolgreich ausgeführt.
- [x] Vollständige Repository-/Gradle-Prüfungen und Abschlussdiff ausgewertet.

## Konkrete Umsetzungsschritte

1. `AtlasAbstände` mit kompakter Skala, semantischen Padding-Werten und
   CompositionLocal anlegen und in `MathematikAtlasTheme` bereitstellen.
2. Harte Layout-Abstände in den Android-App-Composables auf die neue Skala
   umstellen, ohne Größen- oder Graphsemantik mechanisch zu verändern.
3. Konzeptreiter um die Darstellungsart erweitern und die stabile Falk-ID im
   Adapter typisieren.
4. Reines Falk-UI-Modell aus Knotenauswertung oder Standardbeispiel erzeugen;
   Auswahl und begrenzte Achsenprojektion ohne Compose testbar halten.
5. Native Compose-Matrixraster in klassischer Falk-Anordnung implementieren,
   Auswahl und Summenformel verbinden und Diagnose-/Beispielstatus anzeigen.
6. Kopieraktion im nativen Reiter entfernen, sonstige Konzeptaktionen erhalten.

## Tests und Validierung

- Unit-Tests für Abstandsskala und semantische Padding-Werte.
- Unit-Tests für echte Matrizen/Vektoren, Fallback, Dimensionsfehler,
  Auswahlprojektion und 8×8-Begrenzung.
- Gezielte App- und Kern-Tests.
- `python3 scripts/pruefe_repository.py`
- `./gradlew test`
- `./gradlew :app:assembleDebug`
- Visuelle Laufzeitprüfung auf Telefon/Tablet, soweit Emulator oder Gerät verfügbar.

## Persistenz und Migration

Nicht betroffen. Das Konzeptkarten-Asset und seine Prüfsumme bleiben bestehen;
der neue Darstellungsvertrag ist nicht persistiert.

## Risiken und Rückfallstrategie

- Breite Spacing-Migration kann einzelne dichte Speziallayouts verschlechtern;
  deshalb werden nur Weißräume, nicht fachliche Maße ersetzt und der Diff
  dateiweise geprüft.
- Große Matrizen können teuer zu rendern sein; die 8×8-Projektion begrenzt
  sichtbare Zellen.
- Bei UI-Regression kann der Falk-Reiter vorübergehend wieder als Karte gerendert
  werden, ohne Daten oder Persistenz zurückzurollen.

## Entscheidungsprotokoll

- 2026-10-04: Android-weit, kompakte Dichte; Desktop bleibt außerhalb des Umfangs.
- 2026-10-04: Reale Knoteneingaben haben Vorrang; fehlende Werte verwenden ein Beispiel.
- 2026-10-04: Große Matrizen werden begrenzt, ungültige reale Eingaben diagnostiziert.

## Abweichungen, Ergebnis und Verifikation

Die Produktsumme wird bei mehr als acht Summanden ebenfalls mit einer
Auslassungsmarke begrenzt, damit die für große Matrizen zugesagte Rendergrenze
nicht durch eine unbegrenzt lange Formel umgangen wird. Reihenfolge, erste und
letzte Faktoren sowie der exakte Ergebniswert bleiben sichtbar.

Ergebnis:

- `AtlasAbstände.Kompakt` wird vom App-Theme bereitgestellt und in den sichtbaren
  Android-App-Composables verwendet. Fachliche Spezialmaße blieben lokal.
- Der Falk-Reiter rendert native Matrixraster, echte Knoteneingaben oder ein
  gekennzeichnetes Beispiel, Auswahlzustand, Produktsumme und Diagnose.
- Das alte Asset und Kartenformat wurden nicht verändert.

Verifikation:

- Neue gezielte Spacing- und Falk-Unit-Tests: erfolgreich.
- `python3 scripts/pruefe_repository.py`: erfolgreich.
- `./gradlew test :app:assembleDebug`: erfolgreich.
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, da `kotlinc` in der
  Skriptumgebung fehlt; die Gradle-Kern- und Gesamttests waren erfolgreich.
- `git diff --check`: erfolgreich.
- Laufzeitprüfung auf Gerät/Emulator nicht möglich; `adb` ist in der Umgebung
  nicht installiert. Das Debug-APK wurde erzeugt.
