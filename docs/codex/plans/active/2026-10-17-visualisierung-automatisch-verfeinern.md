# Visualisierung automatisch verfeinern und beschleunigen

## Status
Implementierung und funktionale Prüfungen am 17.10.2026 abgeschlossen, einschließlich 19/19 Android-Gerätetests auf dem entsperrten SM-X920. Drei vollständige Android-Leistungsläufe durchgeführt; das 60-Hz-Ziel wird noch verfehlt. Separate erste Vorschau-/Present-Messung und vollständige Leistungsabnahme bleiben offen. Keine Veröffentlichung.

## Ziel und Nutzerwirkung
Android und Desktop: sofort brauchbare Vorschau, fortsetzbare automatische Verfeinerung, flüssige Navigation, verständliche technische Grenzen.

## Nicht-Ziele
Keine neuen Knotentypen, Anschlüsse, Module, Abhängigkeiten oder Versionen; kein GPU-Backend.

## Untersuchter Istzustand
Vorhandene Default-Coroutinen, maximal zwei Aufträge, 32-MiB-LRU und adaptive Sampler. Feste 8000/64000 Auswertungen und Teilung zwischen Ebenen. Umfangreiche fremde Änderungen einschließlich abgeschlossener numerischer Rauminterpretation erhalten.

## Fachliche und mathematische Semantik
Keine Gleichsetzung von Näherung und Nachweis. Polstellen, offene Grenzen, unbekannte Regionen und Orbitlänge erhalten. Keine still ausgelassenen diskreten Zweige.

## Daten-, Node-, Handle- und Edge-Vertrag
Keine Änderung persistierter Typen/IDs. Alte Budgetfelder lesbar und erhalten; Laufzeitsitzungen nicht speichern.

## Architekturentscheidungen
Vorhandene Samplingkoordination um fortsetzbare Sitzungen erweitern. Rechenquanten 8 ms, kooperative Prüfung spätestens alle 256 Schritte, zwei aktive Schritte, faire Rotation. Zwischenstände höchstens alle 50 ms. Bestehender Cache 32 MiB, aktive Daten/Zeichenpakete zusammen 64 MiB.

## Betroffene Dateien und Symbole
visualisierung/sampling: VisualisierungsBerechnung, VisualisierungsSampler, AdaptiveMethodenGeometrie, RasterGeometrie, StrukturierteBildmengen; modell/VisualisierungsKonfiguration; ui/VisualisierungsKnotenRenderer und VisualisierungsDialog; bestehende Tests und Messskript.

## Meilensteine
[x] Ausgangsquellen und fremden Diff unter /tmp/atlas-visual-optimierung gesichert; ursprüngliche Visualisierungs-JVM-Suite grün.
[x] Fortsetzbare synchrone Stacks, kooperative 8 ms-Quanten, zwei aktive Berechnungen, priorisierte einzigartige Admission und geteilte Verbraucher implementiert.
[x] Gemeinsamer Proben-/Geometrie-LRU, vorreservierte Arbeitsgeometrie, native 2D-Pfade und zusammenhängende 3D-Interaktionsnetze implementiert.
[x] Automatik, alte Budgetfelder erhalten, technische Budgeteingabe entfernt, Samplingkamera getrennt und konkrete Statusursachen ergänzt.
[x] 19 neue Regressionen und alle Desktop-Interaktionen grün; unabhängige Code- und funktionale Abnahme ohne offene Blocker. Finale Gesamtsuite 2333 Fälle, 0 Fehler, 2 opt-in Skips.
[x] Native Desktopmessung: drei Läufe je Fall mit jeweils 20 Geometrieanfragen und 300 Eingaben; Canvas-CPU und Capture-Latenz getrennt ausgewiesen.
[ ] Vollständige Display-/Present-Framezeit und separate erste Vorschauzeit gerätebezogen abnehmen.
[x] Android-Laufzeit: nach Entsperren durch den Nutzer 17 allgemeine Bild-/Gestentests und 2 Typ-/Themeprüfungen bestanden. Drei Leistungsläufe mit insgesamt 720 Geometrieanfragen und 17737 erfassten Frames vollständig gesichert.
[ ] Android-Leistungsziel 16,7 ms: gemessene instrumentierte FrameMetrics-p95 für 2D 25,36–26,66 ms, 3D 67,86–68,90 ms; Ziel noch nicht erreicht.

## Konkrete Umsetzungsschritte
1. Istzustand und Baseline erfassen.
2. Sitzungen mit Wiederverwendung und Abbruch implementieren, Nachweise separat begrenzen.
3. UI integrieren, automatische Qualität und konkrete Diagnosen.
4. Zeichenarbeit vorbereiten und unnötige Allokationen reduzieren.
5. Regressionen, Plattformprüfungen und Messungen durchführen.

## Tests und Validierung
Fortsetzungsgleichheit, A/B/C, Abbruch, geteilte Verbraucher, Fairness, Stilcache, Ebenen, diskrete Zweige, Nachweise, Polstellen, Persistenz und Plattforminteraktionen. Ziel: gesamte p95-Framezeit 16,7 ms, einfache kalte Vorschau p95 250 ms; drei Läufe, mindestens 300 Interaktionsframes/20 kalte Vorschauen.

## Persistenz und Migration
Kein Formatwechsel. Historische Budgetwerte erhalten, im automatischen Laufzeitpfad kein Gesamtlimit.

## Risiken und Rückfallstrategie
Fortsetzbare Algorithmen müssen fachliche Unbekanntheit und technische Grenzen getrennt halten. Speicher vor Wachstum begrenzen. Bestehenden synchronen Sampler kompatibel erhalten. Unbelegte Leistungsziele nicht als erreicht melden.

## Fortschritt
17.10.: Repositoryanweisungen und Arbeitsstand geprüft.

## Entscheidungsprotokoll
17.10.: Nutzer wählt Android+Desktop, automatische Verfeinerung, flüssige Bedienung und automatische Budgetsteuerung.

## Abweichungen vom ursprünglichen Plan
- Fortsetzbare synchrone Kerncallbacks verwenden begrenzte IO-Stacks und eine faire Rechenplatzwarteschlange; keine umfassende suspend-Signaturmigration. Bis zu 10 Worker, maximal 2 rechnen; atomare Kernoperationen können ein Quantum überschreiten.
-64 MiB bezeichnet geschätzte verwaltete Arbeits-/Zeichengeometrie, nicht exakten Prozess-RSS.
- Desktop-Eingabe-bis-Capture und Canvas-CPU-Zeit werden getrennt berichtet; keine Gleichsetzung mit vollständiger Display-Framezeit.
- Bekannte Definitionslücken sperren grobe Interaktionsnetze. Bei einer neuen Lücke seit dem letzten Snapshot und gleichzeitigem Speicherabbruch werden veraltete Flächen konservativ verworfen.
- Der erste Androidversuch war am Sperrbildschirm blockiert. Nach der späteren Entsperrung durch den Nutzer wurden Geräte- und Leistungstests erfolgreich ausgeführt; der frühere Geräteblocker besteht nicht mehr. FrameMetrics unter instrumentierter Debug-App sind keine gesonderte Present-Latenz oder allgemeine FPS-Garantie.

## Ergebnis und Verifikation
- `./gradlew test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin --offline`: erfolgreich in 2m 11s, 248 Tasks. 2333 Testfälle, 0 Fehler, 2 opt-in Skips; 2331 bestanden. `AutomatischeVerfeinerungTest`: 19/19, einschließlich echter Speichergrenze, Fairness, Formelcache, vollständiger LOD-Abdeckung und neuer Definitionslücke nach Snapshot vor Speicherabbruch.
- Repository-/Architektur-, Desktop-Whitelist-, Releaseplan-, Versionsfolge- und Diffprüfungen grün. Standalone-Kernskript nicht ausführbar; Gradle-Kerntests erfolgreich.
- Native Desktop-Visualisierungsinteraktionen grün. Messung in `measurements/2026-10-17-visualisierung-desktop.json`: je drei Läufe für Kurve, Kurve+Gebiet und 3D-Fläche, insgesamt 180 Geometrieanfragen und 2700 Eingabeschritte. Canvas-CPU p95: Kurve 0,07–0,20 ms, Kurve+Gebiet 0,16–0,19 ms, 3D 3,61–3,72 ms. Eingabe-bis-Capture p95: 6,18–7,91 ms, 13,06–13,40 ms, 14,21–14,58 ms. 3D nutzt 1024 Interaktionsdreiecke; vor der Snapshot-LOD-Korrektur waren es rund 25000 Detaildreiecke und 176–189 ms CPU-Zeichenzeit. Diese Zwischenstandsmessung ist kein Vergleich gegen den ursprünglichen Release.
- Detail-bis-Bild p95: Kurve 12–33 ms, Kurve+Gebiet 49–101 ms, 3D 256–259 ms. Dies ist nicht die separat gemessene erste Vorschau und keine Display-/Present-Framezeit. Die allgemeine 60Hz-Leistungsabnahme bleibt offen.
- Android SM-X920/Android 16 nach Entsperrung: 17/17 Visualisierungs-Bild-/Gestentests und separat 2/2 Typ-/Themeprüfungen ohne Fehler oder Skips. Die kombinierte Klassenauswahl führte nur die erste Klasse aus; deshalb zweite Klasse separat geprüft und XML-Berichte getrennt gesichert. Pan/Pinch/Mausrad/Tastatur, Detailerhalt bei Touch, 3D-Rotation, Ebenen/Transparenz, Dialog/Undo/Abbruch, Schriftgröße und Themes damit nativ geprüft.
- Drei erfolgreiche Android-Leistungsläufe (je 2 opt-in Tests), 720 Geometrieanfragen und 17737 erfasste Frames; pro gemessenem Gestenfall mindestens 300 tatsächlich erfasste Frames. [Vollständige Androidmessung](../../measurements/2026-10-17-visualisierung-android.json). FrameMetrics-p95 2D alt 24,04–26,03 ms / neu 25,36–26,66 ms; 3D alt 499,18–508,86 ms / neu 67,86–68,90 ms. Gebiete: Standard 26,01–28,68 ms, Hoch 31,99–51,53 ms. Kein Nachweis des 16,7-ms-Ziels.
- Anfrage-bis-fertig-p95 2D-Detail alt 504–522 ms / neu 158–161 ms; 3D-Detail alt 1189–1198 ms / neu 3241–3699 ms. Vollständige automatische 3D-Verfeinerung benötigt damit länger; die erste brauchbare Vorschau wurde nicht separat gemessen. Die neuen Anfragen können Funktionsproben wiederverwenden. Kumulierte Allokationen sind kein gleichzeitiger Heap/RSS. Tatsächliche Android-LOD-Dreiecksanzahl wurde nicht separat gemessen.
- Messskript sammelt vollständige Renderer-/Filterberichte über Logcat und prüft für beide dieselbe UUID. Fehlende oder gekürzte Berichte führen ausdrücklich zum Fehler. UTP entfernt die isolierte App automatisch nach Testende; deshalb ist run-as nur während der Tests, nicht als abschließender Export verfügbar. Der Parser wurde mit den tatsächlichen Rohlogs aus Lauf 2/3 gegen die gesicherten JSON-Berichte geprüft. Der erste Filterbericht ist über denselben Instrumentierungs-PID zugeordnet, Lauf 2/3 zusätzlich über UUID. Messdaten, Rohbilder und XMLs unter /tmp/atlas-visual-optimierung erhalten. 2D-Testaufnahme visuell geprüft.
- Codeprüfung identifiziert verbleibenden Aufwand: Tiefensortierung berechnet Tiefe wiederholt im Comparator, Kameratrigonometrie/temporäre Listen werden pro Punkt wiederholt. Deren genauer Zeitanteil ist nicht profiliert. Nächster Ansatz: Tiefe einmal je Dreieck, Kamerawerte einmal je Frame, weniger temporäre Objekte; separate Vorschau- und Present-Messung ergänzen.
- Abschlussnachlauf: reguläre App-/Test-APKs erfolgreich wiederhergestellt, Metadaten bestätigen die regulären IDs und unveränderte Version 3.4.11. UTP hat die isolierten Testpakete automatisch entfernt; Paketliste für Testbenutzer 0 bestätigt ausschließlich die bestehende Produktinstallation. Repository-/Releaseplan-/Versionsfolge-/Diffprüfungen erneut grün.
- Unveränderter Branch `samai/v3.4.12/pie-methoden-matrizen`, HEAD `20356f1c3c8e996fae03ed10ff10d9fe8005debe`. Keine eigenen Commits (damit keine neue Autor-/Committeridentität), Pushes, PRs oder Veröffentlichung. Fremde Änderungen erhalten; eigener Visualisierungsdiff gegen die tatsächliche Ausgangskopie separat geprüft.


## Releaseentscheidung am 17.10.2026

Der Nutzer hat für den Releaseabschluss ausdrücklich angewiesen: „überspringe die weitere Leistungsverbesserung und erstelle nur den master Commit“. Weitere Optimierung, das bisher verfehlte 16,7-ms-Ziel und separate erste Vorschau-/Present-Abnahme bleiben Folgearbeit außerhalb des Abschlussumfangs von v3.4.12. Die obigen offenen Kriterien und ursprünglichen Messwerte bleiben gültig; keine vollständige Leistungsfreigabe. Nachfolgende unfertige Optimierungs-/Mesh-/Presentversuche wurden vor Release zurückgenommen. Funktionale und reguläre CI-Gates bleiben verpflichtend.
