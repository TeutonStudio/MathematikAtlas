# Visualisierung vollständig überarbeiten

## Status
Produktänderung und funktionale Abnahme abgeschlossen am 15.10.2026; Leistungsziel für gesamte 2D-Framezeit bleibt offen. Nutzerauftrag: bestätigten Chatplan auf Android und Desktop implementieren. Keine Veröffentlichung.

## Ziel und Nutzerwirkung
Passive ziehbare Knotenvorschau, interaktiver Arbeitsdialog mit Entwurf und atomarer Übernahme; korrekt begrenzte Grafik, maßstabsgerechte Navigation, adaptive Darstellung und automatische Sichtbereichsberechnung.

## Nicht-Ziele
Keine neuen Knotentypen, Analyseoperatoren, Module oder Produktionsabhängigkeiten. Keine Releasearbeit.

## Untersuchter Istzustand
Basis c4b23c69, Branch samai/methoden-grundraeume-praedikate. Umfangreiche fremde/laufende Änderungen, Ausgangsdiff unter /tmp/visualisierung-ausgangsdiff.patch erfasst. Renderer besitzt keinen äußeren Clip, feste Gestenfaktoren und fehleranfälligen produceState-Frühabbruch. Desktop schließt den Android-Renderer wegen android.graphics aus und zeigt bislang keine entsprechende gemeinsame Vorschau.

## Fachliche Semantik
Unveränderte Mengenausgabe. R1/R2/R3/C, explizite Projektionen, Parameterdomänen, feste Schnitte, Farben und Orbitbudgets erhalten. Nachweisstatus niemals aus numerischen Bildern ableiten. Sichtfenster von Methodenparameterdomänen trennen.

## Daten-, Knoten- und Anschlussvertrag
mathematik.visualisierung und Anschlussreferenzen unverändert. Ansichtsoptionen optional persistiert. Laufzeitanfragen, Geometriecache, Fortschritt und Inspektion nicht speichern. Übernahme nur eigener Eigenschaftsschlüssel, ein Undo-Schritt.

## Architekturentscheidungen
Gemeinsame Compose-Zeichnung ohne android.graphics; bestehende Desktop-Quellfreigabe verwenden. Gemeinsame Transformation und Samplingkoordination im Visualisierungspaket. Dialog erhält aktuelle Knotendaten zur Lösch-/Konflikterkennung.

## Betroffene Dateien und Symbole
visualisierung/modell, sampling und ui; Android KonzeptUi und Inspector; Desktop Rendererzuordnung und Inspector. Keine fachliche Logik im neutralen Editor.

## Meilensteine
- [x] Ausgangszustand erfasst; alte Quellen aus c4b23c69 als temporäre Testreferenz rekonstruiert.
- [x] Clipping, passive Vorschau, gemeinsame Transformation und Ergebniswechsel; JVM- und echte Compose-Prüfungen bestanden.
- [x] Progressive Samplingkoordination, geschätztes 32-MiB-LRU-Limit, priorisierte maximal zwei Aufträge, dynamischer Sichtbereich und exakte Cachezustände.
- [x] Gemeinsamer Entwurfsdialog, Konflikte, Schließen/Verwerfen und atomare Übernahme auf beiden Plattformen.
- [x] Fachliche Tests, 9 Desktop- und 9 Android-Compose-Prüfungen; unabhängige gezielte Codeabnahme ohne konkrete Blocker.
- [-] Leistungsabnahme: Vorschau und R³-Ziel erreicht; gesamte R²-Framezeit noch über 16,7 ms.

## Umsetzungsschritte
Transformation und Laufzeitmodelle zuerst, dann gemeinsame Berechnung und Renderer. Dialogentwurf/Validierung ergänzen, Android und Desktop verdrahten. Automatisierte Prüfungen und echte Interaktionen danach.

## Tests und Validierung
Roundtrip/Zoomanker/Pan/Seitenverhältnis; A→B→C, Abbruch/Cache/Stil; Domänenerhalt und Rotation; Intervalle/Polstellen/Mengen/Flächen/Orbits; Dialogabbruch/Übernahme/Undo/Konflikt; Persistenz. Vollständige Gradle-Tests, Repositoryguards, APK/Lint/Desktop. Leistung: erste Geometrie, Detailzeit, Framezeiten und Speicher auf identischer Hardware vergleichen; Ziel p95 16,7 ms und einfache kalte Vorschau 250 ms.

## Persistenz und Migration
Optionale Ansichtskennung erlaubt alte Kameraangaben in das zentrierte Modell zu überführen. Unbekannte Eigenschaften beim Speichern erhalten. Kein Kartenformatsprung vorgesehen.

## Risiken und Rückfallstrategie
Mathematische Grenzfälle und übergroße Geometrie durch Budgets/Abbruch begrenzen. Keine unbewiesenen Leistungsversprechen. Bestehende fachliche Sampler und ihre Tests bleiben Referenz.

## Fortschritt
- Ausgangsdiff erfasst, reale Aufrufpfade und vorhandene Tests gelesen.

## Entscheidungsprotokoll
- 15.10.: Nutzer wählt passive Knotenvorschau, automatisches Nachladen und explizite Entwurfsübernahme.

## Abweichungen
- Das 60-Hz-Ziel wird für die gesamte R²-Framezeit im instrumentierten Gerätelauf nicht erreicht. Dies ist offen und wird nicht durch die niedrigere reine Zeichendauer ersetzt.
- R³ nutzt eine konservative rotationsunabhängige Abdeckung. Sie berechnet bei gleichem Budget einen größeren Bereich als die alte feste Domäne; deshalb sind Detailzeiten nicht bei identischer Geometriequalität vergleichbar.
- Die neue Detailansicht enthält absichtlich 120 ms Ruhezeit und einen Vorschauschritt; einfache kalte Detailanzeigen benötigen dadurch mehr Zeit als der alte unmittelbare Vollpfad.
- Touch-Navigation, Mausrad, Dialogübernahme und Layouts wurden automatisiert ausgeführt; keine gesonderte manuelle TalkBack-Abnahme. Ein separater Emulatorlauf wurde nicht zusätzlich ausgeführt, das physische Gerät lief unter Android 16.
- Standalone-Kernskript nicht ausführbar: eigenständiges kotlinc fehlt; 581 Kern-JVM-Tests liefen über Gradle erfolgreich.

## Ergebnis und Verifikation

### Funktion und Architektur
- Gemeinsamer Compose-Renderer ohne android.graphics. Knoten lässt sich über die Vorschau ziehen; Graphgesten bleiben im Dialog.
- Numerische Welt-/Bildtransformation, korrekte Achsen, 1–2–5-Raster, gleiche Einheitenskalierung, Zoomanker, Mausrad, fokussierte Tastatursteuerung und Koordinateninspektion.
- Lokaler Dialogentwurf, ungültige Feldtexte bleiben erhalten; atomare Übernahme/Undo/Redo, Lösch-/Konfliktschutz und Schutz vor versehentlichem Verwerfen.
- Vollständige grobe Kurvenbasis vor adaptiver Verfeinerung. Identische Gleitkomma-Mittelpunktformeln in Cache und Refinement verhindern verlorene Endsegmente. Hochfrequente Sinusregression, Polstellen, Parameterdomänen und R³-Nachladen geprüft.
- Kartencodec-Roundtrip, alte rotierte Kameras, unbekannte Schemafelder sowie Duplizieren mit neuen Anschluss-IDs geprüft. Entfernte feste Schnittparameter werden tatsächlich gelöscht.
- Während Gesten bleibt zusammenhängende Vorschaugeometrie erhalten; keine ausgedünnten Einzelkanten oder Dreiecke. Kamera liest Zustand in der Zeichenphase, Raster und feste Kurvenzüge verwenden gebündelte Pfade; unsichtbare Primitive und Schraffuren sind begrenzt.

### Ausgeführte Prüfungen
- `./gradlew test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin --offline`: erfolgreich; **1.918 JVM-Tests ohne Fehler/Skips** laut XML-Berichten.
- `:app:connectedDebugAndroidTest` mit Klassenfilter VisualisierungsGeraeteTest und isolierter Kennung: **9/9** auf **SM-X920, Android 16**. Die bestehende Haupt-App wurde nicht ersetzt.
- Desktop VisualisierungsBedienungTest: **9/9**; weitere Desktoptests im Gesamtlauf ebenfalls grün.
- Pixelprüfung: zentriertes Graphrechteck auf Magenta-Untergrund, Randprüfung innerhalb WindowInsets.safeDrawing. Der erste Android-Screenshot enthielt die halbtransparente Systemleiste; diese war keine überlaufende Graphzeichnung. Nach sauberer Trennung der Fensterfläche bestanden die Pixelassertionen unverändert für alle geprüften Ränder.
- Repository-/Architekturprüfung, Desktop-Shadowmodulguard, Releaseplan und Versionsfolge erfolgreich. Keine Versionsmetadaten geändert. `git diff --check` erfolgreich.
- Unabhängiger lesender Verifizierer: nach Korrekturen keine konkreten neuen Blocker. Leistungsziel ausdrücklich separat bewertet.

### Leistungsmessung
Reproduktion: `python3 scripts/messe_visualisierung.py --basis c4b23c69 --bericht /tmp/visualisierung-messung.json`. Das Skript erzeugt alte Visualisierungsquellen ausschließlich temporär als Testreferenz, verwendet dieselbe aktuelle mathematische Domäne, installiert eine isolierte Test-App und erweitert keine Produktionsquellen.

[Rohdaten](../../measurements/2026-10-15-visualisierung-sm-x920.json). Je Fall fünf neue Knotenanfragen; Zeit umfasst Compose-Testkoordination bis zum fertigen Darstellungsstatus. Detailzeit bedeutet vollständige Detailanzeige, nicht erste progressive Geometrie. Allokationen sind kumulierte ART-Bytes während dieser fünf Anzeigen einschließlich Testkoordination, kein dauerhaft belegter Heap. Gesten: 45 kontinuierliche Bewegungsschritte nach fünf Aufwärmschritten, FrameMetrics.TOTAL_DURATION; Zeichendauer und Layout separat.

| Fall | Alter Median | Neuer Median | Alte Gesamtframezeit p95 | Neue Gesamtframezeit p95 |
|---|---:|---:|---:|---:|
| R² Vorschau | 381,2 ms | 133,4 ms | – | – |
| R³ Vorschau | 483,8 ms | 132,8 ms | – | – |
| R² Detail | 141,3 ms | 365,8 ms | 27,73 ms | 26,85 ms |
| R³ Detail | 957,9 ms | 1.115,6 ms | 22,66 ms | 15,25 ms |

R²-Zeichendauer p95 sank von 19,65 auf 10,77 ms. Die gesamte R²-Framezeit liegt weiterhin über dem Ziel; **keine vollständige 60-Hz-Freigabe**. Vorschau liegt in dieser Messreihe unter 250 ms. Das geschätzte LRU-Speicherlimit ist durch gezielte Verdrängungstests nachgewiesen, kein allgemeines Prozessspeicherlimit.

### Git und Abschlussgrenzen
Branch `samai/methoden-grundraeume-praedikate`, Ausgangs-HEAD `c4b23c69`. Kein neuer Commit, Push, PR oder Release; daher keine neue Autor-/Committeridentität. Vorher vorhandene Änderungen wurden beibehalten; automatischer Vergleich der ursprünglichen und abschließenden Diffs außerhalb der Aufgabenpfade ergab keine Abweichungen. Der Plan bleibt wegen des offenen 2D-Leistungsziels aktiv.


## Folgeauftrag: Flächen, Ebenen und Knotendarstellungen (15.10.2026)
Status: Produktänderung umgesetzt; Android-Geräte-/Referenzleistungsabnahme offen. Ausgangsdiff /tmp/visualisierung-korrekturen-ausgang.patch; vorhandene Änderungen bleiben erhalten.

### Vertrag und Entscheidungen
Visualisierer wird eine reine Ansicht mit zwei optionalen dynamisch erweiterbaren Eingängen. Historische Ausgänge bleiben bis zur expliziten Umstellung lesbar; verbundene Ausgänge werden nur nach Verlustbestätigung entfernt. Ebenen werden über Anschlussnamen identifiziert. Gemeinsame Kamera, individuelle Farbe/Deckkraft/Füllmuster/Sichtbarkeit. Automatisch/Standard maximal 8000, Hoch maximal 64000 Auswertungen je Anfrage, unter sichtbaren Ebenen aufgeteilt. Hexagone sind ausschließlich ein abgeschnittenes Füllmuster. Graph unterstützt Methode/Term über vorhandene Übersetzungssemantik. Grad behält fachliche Dimension n-1 und stabile Referenzen. Keine Veröffentlichung.

### Umsetzung und Abnahme
- [x] Regionendarstellung und progressive Qualität, dynamische Ebenen und Stilkonfiguration; native Desktop-Bildvergleiche für fünf Füllarten.
- [x] MethodenGraphMenge, Graph-Termmodus und passende Definition; vollständige Zerlegen-Anzeige; fachliche Regressionstests.
- [x] LaTeX-Klammern/Doppelstrich, Notizfarbe, gemeinsame Kapselanschlussgeometrie; LaTeX-Pixeltest, Notiz-JSON/Undo und Konturtests.
- [x] Verlustbestätigte Umstellung und Dimensionsbereinigung; Persistenz/Undo und verbundene Legacy-Konflikte geprüft.
- [-] Gezielte Tests, Repositoryprüfungen, JVM, Lint/APK/Desktop und native Desktop-Interaktion bestanden; Android-Gerät und Referenzframevergleich fehlen. Unabhängiger Read-only-Review mit behobenen Findings abgeschlossen; keine weiteren konkreten Codeblocker.

### Prüfstrategie
Vergleiche Methode/Bildmenge/Graph, y<x² und Gebiete mit Löchern, unbekannte Zellen, diskrete Mengen, Projektionen. Prüfe Stilcache, mehrere Ebenen, Kopieren/Reihenfolge, alte Daten, Verlustvorschau, Undo/Redo und gemeinsame Anschluss-/Treffergeometrie. Bestehende Android-/Desktop-Compose-Tests ergänzen. Noch keine neuen Prüfergebnisse.

### Folgeauftrag: Verifikation und Prüfgrenzen
- Vollständiger Gradle-Pfad `test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin` erfolgreich; Logs `/tmp/visual-abnahme-final.log`. Native Desktop-Suite enthält 14 Visualisierungstests einschließlich Füllmuster, LaTeX ohne Fehlerglyphen und vertauschter 3D-Ebenen.
- Repository-, Releaseplan- und Versionsfolgeprüfungen erfolgreich, Shadowmodule unverändert. `git diff --check` sauber. Standalone-Kernskript kann weiterhin ohne eigenständiges `kotlinc` nicht ausgeführt werden; Kern-JVM-Tests laufen über Gradle.
- Die opt-in Samplingmessung liegt in `docs/codex/measurements/2026-10-15-visualisierung-korrekturen-jvm.json`. Einzelmessungen: Parabelgebiet Standard 56,1 ms Detail, Hoch 143,5 ms; zwei Gebiete Standard 19,7 ms, Hoch 50,7 ms. JIT-/Warmup-Effekte verhindern belastbare Vergleiche dieser Einzelwerte. Cachetreffer 0,07–0,19 ms; beobachteter Cache maximal etwa 2,9 MB. Keine zeitabhängigen Testassertionen.
- `/home/alex/Android/Sdk/platform-tools/adb devices -l` meldet kein Gerät; `/home/alex/.android/avd` existiert nicht. Deshalb bleiben Android-Interaktion, Tastatur-/Theme-/Schriftskalierungsabnahme auf Android, Framezeiten und Allokationsvergleich mit dem früheren SM-X920 offen. APK/AndroidTest-APK wurden gebaut, nicht als ausgeführte Gerätetests gewertet. Das frühere offene R²-60-Hz-Ziel wird nicht als erreicht behauptet.
- Keine neuen Module, Produktionsabhängigkeiten, Release-/Versionsänderungen oder Commits. Bestehender Branch `samai/methoden-grundraeume-praedikate`, unveränderter HEAD `c4b23c69c431370b600cd4bd810c8d5135c43ff7`.

Numerische Prüfgrenze: getrennte Ungleichungsrandzüge werden derzeit nur für polynomiale Ausdrücke mit abgetastetem Vorzeichenwechsel erzeugt. Tangentiale beziehungsweise niederdimensionale Nullniveaus (z. B. x²≤0 oder x²>0) behalten die Zell-/Näherungsdarstellung; deren Linienrand ist nicht vollständig rekonstruiert. Unbekannte Zellen werden nicht als bewiesen enthalten ausgegeben.

### Unabhängige Folgeabnahme
Der unabhängige Reviewer `visual_korrekturen_review` meldet nach Nachprüfung keine weiteren konkreten Codeblocker. Behoben wurden unter anderem der Term-Verbindungsvertrag, Legacy-Dimensionsbereinigung, entartete Residuen, ebenenübergreifende 3D-Tiefensortierung und konsistente Zeichenpakete beim Sichtbarkeitswechsel identischer Mengen. Die numerischen Randgrenzen und fehlende Android-Geräte-/Referenzleistungsabnahme bleiben ausdrücklich offen; deshalb bleibt dieser ExecPlan aktiv.

Die lokale Samplingmessung ist reproduzierbar mit einem temporären Gradle-Init-Skript `allprojects { tasks.withType(Test).configureEach { systemProperty 'atlas.measureSampling', 'true' } }` und `./gradlew :MathematikKnoten:testDebugUnitTest --tests '*VisualisierungsSamplingMessungTest' -I <init-skript>`. Ohne Aktivierung wird diese reine Messklasse übersprungen.

## Folgeauftrag 16.10.2026: Tupelfilter und Zeichenbereich

Status: Filterkorrektur umgesetzt und fachlich/auf beiden Plattformen geprüft; Leistungsziel weiterhin offen. Nutzerkarte 486ca2b9-41cb-4348-8d48-14c23ba3a4ed, Version 3: R×R → Variable x → Zerlegen → x₁<x₂² → Wahr ∧ Bedingung → Aussage zu Prädikat → Mengenfilter → Visualisierung. Ausgangsdiff /tmp/filter-visualisierung-ausgang.patch. Aktueller Branch samai/v3.4.0/methodenknoten; fremde/laufende Änderungen bleiben erhalten. Keine Releasearbeit.

### Befund und Umsetzung
GefilterteMenge hat bislang keinen Zellnachweis; unbekannte Zellen werden flächendeckend schraffiert und verfügbare numerische Flächenzellen verworfen. Symbolische Filterargumente werden einmal kanonisch gebunden und für unterstützte Koordinatenräume in die bestehende definierte Region überführt. Voraussetzungen und Grundmenge bleiben erhalten. Für nicht zertifizierbare Formen kein erfolgloses globales Zellraster; numerische Klassen bleiben als Näherung erkennbar.

- [x] Nutzerfixture und fachliche Reproduktion, kanonische Strukturbindung und gemeinsame Regionsaufbereitung.
- [x] Zell-/Punkt-/Randarbeit begrenzen und vollständig zählen, lokale numerische Ergänzung ohne globale Unbekanntüberlagerung.
- [x] Weltbereich/Sichtfenster/20%-Reserve diagnostizieren; Parametergrenzen erhalten.
- [x] JVM-, native Desktop-Pixel-, Android-Build-/Lint- und unabhängige Prüfung; Android-Laufzeit auf SM-X920 ausgeführt.

### Vertrag und Abnahme
Keine Persistenz-/Anschlussänderungen. x bleibt ein einzelnes Tupelargument. Erwartet: (0,1) enthalten, (1,0) und (1,1) ausgeschlossen. Direkte Definition und Filter teilen Region und offenen Rand. Prüfe Wahr/Falsch, Voraussetzungen, verschachtelte Komponenten, unentscheidbare Bedingungen, kleine Budgets und Pan/Zoom/Seitenverhältnisse. Die Diagnose trennt sichtbaren Bereich, Berechnungsreserve und Definitionsmenge und meldet tatsächlich verbrauchte Arbeit.

### Nachgewiesener Abschluss des Filterfolgeauftrags

- Nutzerfixture `MathematikKnoten/src/test/resources/regression/tupel-filter-visualisierung-v3.json` enthält die semantisch gleiche Karte mit den angegebenen IDs und Verbindungen. 14 gezielte Regressionen bestehen: Koordinatenbindung, direkte/Filter-Regiongleichheit, offener Rand, kleine Gesamtbudgets (1 bis 8000), Wahr/Falsch, falsche/partielle Voraussetzungen, identitätsverschiedene externe Symbole, gemeinsamer und abhängiger Definitionsraum, verschachteltes Tupelargument, unbekannte Prädikate, lokale numerische Flächen und Pan/Reserve. Laden/JSON-Roundtrip erhält IDs, Verbindungen und historischen Ausgang.
- Vollständiger Prüflauf `./gradlew test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin` erfolgreich (`/tmp/filter-final-checks.log`). Zu diesem Zeitpunkt 1963 JVM-/Desktop-Tests ohne Fehler; eine opt-in Messung regulär übersprungen. Anschließend die beiden zuletzt ergänzten Strukturregressionen im 14-Test-Filterlauf bestanden. Native Desktop-Visualisierersuite jetzt 15 Tests inklusive Füll-/Ausschluss-Pixeltest und Bereichsdiagnose. Sichtkontrolle `/tmp/filter-v3-desktop.png`: gefüllter Innenbereich, leeres Außengebiet, gestrichelter Parabelrand; kleine unbekannte Randzellen bleiben erkennbar.
- Android-Visualisierersuite 15/15 auf SM-X920, Android 16 erfolgreich (`/tmp/filter-device-isoliert.log`), einschließlich Dialog/Undo/Abbrechen/Konflikt, große Schrift/niedriges Fenster, Clipping, Ebenen und neuer Tupelfilterdarstellung. Die vorhandene App hatte eine andere Signatur. Ein temporäres Gradle-Init-Skript setzte ausschließlich für die Testinstallation `de.TeutonStudio.MathematikAtlas.filtertest` und separate Test-ID; ursprüngliche Installation und Daten wurden nicht ersetzt. Kein persistierter Buildkonfigurationswechsel.
- Repository-, Releaseplan-, Versionsfolge- und Diffprüfung grün. Das zusätzliche Standalone-Kernskript benötigt ein fehlendes eigenständiges `kotlinc`; die Kern-JVM-Tests liefen erfolgreich über Gradle. Keine neue Produktionsabhängigkeit, kein Modul, keine zusätzliche Shadowfreigabe, keine Release-/Versionsänderung aus diesem Auftrag.
- Unabhängiger Read-only-Review `visual_korrekturen_review`: externe Identitätskollisionen, partielle Reell-Voraussetzungen und abhängige Grundmengen gefunden und korrigiert; Nachprüfung meldet keine weiteren konkreten Blocker.
- Sampling-/Cachemessung: `measurements/2026-10-16-tupelfilter-jvm.json`. Einzelner Standardfilter: Vorschau 25,98 ms, Detail 62,87 ms, Cache 0,146 ms; hoher Filter: Detail 212,39 ms. Maximal beobachteter lokaler Geometriecache in diesen Fällen 3,43 MB. Einzelmessungen/JIT, kein Leistungsversprechen.
- Referenzgerätmessung: `measurements/2026-10-16-tupelfilter-sm-x920.json`. Kontinuierliches Pan nach fünf Aufwärmschritten, je 90 erfasste Frames: p95 Standard/eine Ebene 24,27 ms, Hoch/eine Ebene 26,56 ms, Standard/zwei Ebenen 28,32 ms, Hoch/zwei Ebenen 26,92 ms. Detailabschluss einschließlich Test-Synchronisierung ca. 323–1166 ms. Kumulative Allokationen über fünf Anfragen ca. 264–269 MB Standard und 2,07–2,14 GB Hoch; dies ist kein Live-Heapmaß. Diese Daten betreffen Filtergebiete, der alte Benchmark Funktionsgraphen; daraus wird kein identischer Vorher/Nachher-Vergleich abgeleitet. Kein zeitabhängiger Unit-Testassert.

Die Filterkorrektur ist abgenommen. Das übergeordnete p95-60-Hz-Ziel bleibt nicht erreicht; der gesamte Visualisierer-ExecPlan bleibt deshalb aktiv. Sehr kleine Löcher und tangentiale/niederdimensionale Ränder können durch endliches numerisches Sampling unvollständig bleiben; numerische Zellen werden weiterhin nicht als Beweis bewertet. Branch `samai/v3.4.0/methodenknoten`, unveränderter HEAD `c4b23c69c431370b600cd4bd810c8d5135c43ff7`; keine Commits erzeugt.
