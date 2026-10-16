# Methoden, Grundräume und Prädikate – ExecPlan

## Status, Ziel und Nutzerwirkung
Abgeschlossen und unabhängig abgenommen. Die zwei vom Nutzer gelieferten Karten sollen einen Filter mit genau einem Tupelargument bzw. die Gleichheit von R×R×R und R³ ergeben. Variablen erben ihren vollständigen Grundraum; Methodenfarben und Standardnamen folgen der Signatur.

## Nicht-Ziele
Keine neuen Knotentypen, Produktionsabhängigkeiten, Versionsänderungen oder Veröffentlichung. Fremde IDE-Dateien bleiben unangetastet.

## Untersuchter Istzustand
VariablenStruktur.kt erzeugt einzelne Komponentenvariablen, außer beim Winkelraum. termAlsMethode kennt bereits StrukturQuellenBindung für ganze Argumente. Die Ausgangsprojektion beeinflusst Methodenaufrufe, nicht Filterparameter. ObjektVergleich.kt vergleicht die drei Produktdarstellungen nur innerhalb derselben Klasse. AnschlussSymbol.kt zeichnet bereits schräge Streifen; es fehlen tatsächliche Signaturen und konsequente logische Typen. Die Namensmigration erkennt historische Vorlagen, keine Laufzeitsemantik.

## Fachliche Semantik
Eine Variable ist ein Element einer verpflichtenden Menge. Strukturprojektionen behalten eine gemeinsame Quelle. Unabhängige Variablen bleiben unabhängige Argumente. Produkte bewahren ihre Verschachtelung sowie Null-/Einertupel. Unentscheidbare Gleichheit bleibt unbekannt.

## Daten-, Knoten- und Anschlussvertrag
Bestehende StrukturQuellenBindung und TypisiertesElement werden weiterverwendet. Historische Struktur-/Bereichsparameter sind nur Migrationsdaten. Neue Variablen ohne Grundraum liefern eine Diagnose. Ausgangs-IDs bleiben erhalten, Typverträge werden aus der Auswertung aktualisiert. Keine Änderung von formatVersion 9.

## Architekturentscheidung
Gemeinsame Quellenidentität für skalare und strukturierte Variablen, Komponenten als symbolische Zugriffe; siehe ADR `../../decisions/2026-10-15-variablen-grundraum-quellenidentitaet.md`. Mathematik im Kern, Quellen und Migration in MathematikKnoten, neutrale Farbzeichnung im Editor.

## Betroffene Bereiche und Umsetzungsschritte
1. Variablenwert aus Grundraum und gemeinsame Bindung; Produktraumvergleich im Kern.
2. Idempotente Migration historischer Grundräume über bestehende Mengen-/Formelvorlagen; Integration nur im Ladepfad, nicht als Reparatur neu angelegter unvollständiger Variablen.
3. Signatur-/Variablenvertrags- und Standardnamenssynchronisierung; Inspector vereinfachen; Anschlussfarben prüfen.
4. Regressionen für Karten, Migration, Quellenidentität, Grundraumwechsel und Symbolfarben; Prüfungen und Review.

## Meilensteine und Fortschritt
- [x] Istzustand und Nutzerentscheidungen geprüft.
- [x] Fachliche Korrekturen und Migration: Import-, Mitgliedschafts-, Gleichheits- und Quellenregressionen grün.
- [x] Oberfläche und Verträge: 2/2 Compose-Gerätetests auf SM-X920 / Android 16, Prüfbild kontrolliert.
- [x] Tests, unabhängige Prüfung und Abschlussdiff: 1882 JVM-Tests, Debug-/Test-APK grün; unabhängiger Verifizierer freigegeben; diff --check sauber.

## Tests und Validierung
Beide Beispielgraphen durch Import/Auswertung prüfen, Filterpunkte innen/außen, zwei echte Variablen, beide Projektionen, Produktvergleich in beiden Richtungen inkl. 0/1 und Verschachtelung. Migration inkl. Roundtrip/Idempotenz und IDs; fehlender/wechselnder Grundraum, Undo/Redo. Anschlussfarben und automatische Namen. Repository-/Kern-/Methodenprüfungen, ./gradlew test und :app:assembleDebug. Laufzeitprüfung nach Geräteverfügbarkeit.

## Persistenz und Migration
Legacykonfigurationen ohne Verbindung werden in explizite Mengenquellen übersetzt. Deterministische neue IDs, bestehende IDs unverändert. Kennzeichnung des neuen Grundraumvertrags verhindert Wiederherstellung absichtlich entfernter Grundraumverbindungen. Bestehende verbundene Grundräume haben Vorrang.

## Risiken und Rückfallstrategie
Strengere Grundraumpflicht berührt bisherige Tests/Vorlagen; diese müssen explizite Grundräume liefern. Keine pauschalen Fallbacks. Änderungen bleiben auf Aufgabenbranch und können als Diff verworfen werden; kein Merge.

## Entscheidungsprotokoll
2026-10-15: Nutzer wählt Grundraumpflicht, automatische Ergänzung alter Mengenquellen und logische Typfarben mit schrägen Teilungen. Kein Release beauftragt. Umfang x ohne neue Knotentypen.

## Abweichungen
- Die JSON-Ressourcen rekonstruieren die fachlich relevanten Teilgraphen der Nutzerexporte mit den ursprünglichen Knoten-/Anschluss-IDs. Layout, die nachgeschaltete Visualisierung und die unverbundene N-Quelle der ersten Karte sind für diese Rechenregression reduziert. Die Anschlussdarstellung wird zusätzlich auf dem Gerät geprüft.
- Der bestehende Zerleger adressierte dynamische Komponenten nach der Ausgangsposition. Ein durch Migration ergänzter Methodenausgang verschob diese Position. Auswertung und ID-Synchronisierung verwenden nun die Komponentenrolle; normale benannte Ausgänge bleiben unterstützt.
- Der ursprüngliche Geräteinstallationsversuch wurde wegen anderer Signatur der installierten App abgewiesen. Die tatsächliche Abnahme erfolgte mittels temporärem Gradle-Init-Skript unter `de.TeutonStudio.MathematikAtlas.pruefung`; vorhandene App und Kartendaten blieben erhalten. Das Repository enthält keine geänderte Paketkennung.
- `pruefe_kern.py` war wegen fehlendem Kommando `kotlinc` nicht ausführbar. Die 581 Gradle-Rechenkern-Tests sind erfolgreich.
- Der letzte erfolgreiche Variablentyp bleibt bei fehlendem Grundraum erhalten, damit keine bestehenden Folgeverbindungen gelöscht werden. Die Berechnung liefert weiterhin die explizite Diagnose.


## Ergebnis und Verifikation
- `python3 scripts/pruefe_repository.py`: erfolgreich, einschließlich Architektur und Methodenmodell.
- `python3 scripts/pruefe_methodenmodell.py`: erfolgreich.
- `python3 scripts/pruefe_releaseplan.py`: erfolgreich; veröffentlichte Version 3.3.1 unverändert.
- `python3 scripts/pruefe_versionsfolge.py`: erfolgreich für den nicht versionsbeanspruchenden Arbeitsbranch.
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, siehe Abweichungen.
- `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest`: erfolgreich in 18s. JVM: TypSystem 10, Kern 581, Editor 137, Adapter 66, Knoten 722, App 337, Desktop 29 = 1882; keine Fehler/Skips.
- Geräteprüfung `GrundraumMethodenGeraeteTest`: 2 Tests, 0 Fehler/Skips. Schmaler Variableninspector mit Schriftfaktor 1.6; fehlender/verbundener Grundraum; Pixelprüfung beider Halbkreise und schräger Teilungen. Bild aus Gerätelog rekonstruiert und visuell geprüft.
- Unabhängiger Verifizierer hat alle gemeldeten Findings nachgeprüft und freigegeben. Abgedeckt sind auch gleichnamige unabhängige skalare Quellen, endliche numerische Grundräume, leere Produkte, Nulltupel, Grundraumverlust ohne Kantenverlust und Methodenlifting zurück zu Zahlen und Grundraumtypwechsel ohne Verlust unabhängiger Kanten.
- `git diff --check`: erfolgreich. Fremde `.idea`-Änderungen und `scripts/__pycache__` wurden nicht verändert.
- Branch `samai/methoden-grundraeume-praedikate`, Basis `c4b23c69c431370b600cd4bd810c8d5135c43ff7` (v3.3.1). Kein Commit, Push, PR oder Release; Autor-/Committermetadaten deshalb unverändert. Umfang weiterhin x, keine neuen Knotentypen.
