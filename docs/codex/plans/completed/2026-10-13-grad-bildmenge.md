# Grad-Richtungsraum und Bildmenge
Status: abgeschlossen.

## Ziel und Nutzerwirkung
Die gemeldete Diagonalmatrix-/Polar-Karte erzeugt eine numerisch visualisierbare Bildmenge. Die gruppierte Fallunterscheidung wird als ausführbare Definitionskarte bereitgestellt.

## Nicht-Ziele
Keine neuen Knotentypen, Karten-JSON-Formatversion oder Änderung der Tensorstufe. Diese Arbeit ist in den gemeinsamen v3.2.1-Änderungsrelease aufgenommen.

## Istzustand
Grad.dim erzeugte G^n; die signierte Polarabbildung erwartet W_(d-1) mit Vorzeichen und Halbkreiswinkeln. Winkelvariablen ignorieren den angeschlossenen Grundraum. Das Beispiel verbindet Tensorstufe statt Komponentenanzahl. Sein reelles Indexintervall ist keine endliche Indexmenge.

## Semantik und Vertrag
Ohne dim bleibt G die Winkelgruppe. Mit positiver Koordinatendimension d (1..65) entsteht W_(d-1). Der verbundene kanonische Richtungsraum bestimmt die Winkelvariablendimension. Bildmengen verwenden die gesamte strukturierte Argumentdomäne. Allgemeine iterierte Produkte bleiben Mengen von Auswahlfunktionen; nur nachgewiesene endliche kanonische Richtungsprodukte dürfen für strukturierte Argumente gepackt werden.

## Architektur und Dateien
Bestehender Kern-Winkelraum, KonsolidierteKnoten, VariablenStruktur, Bildmengenauswerter, GradDefinitionen und vorhandene Tests. Keine Abhängigkeit oder zusätzliche Produktionsdatei.

## Meilensteine und Umsetzung
- [x] Dimensionsvertrag und Grundraum durchgängig korrigieren.
- [x] Gruppenkonstruktion als ausführbare Definition mit endlicher Indexmenge und Halbkreisintervall.
- [x] Regression über echten Auswerter, Codec und Sampler für beide Domänenpfade.
- [x] Prüfungen und unabhängige Verifikation.

## Tests und Validierung
Gezielte Grad-/Winkel-/Bildmengen-/Konzepttests; vollständige Gradle-Tests, Repositoryprüfung und Debug-Build. Keine Geräteprüfung behaupten.

## Persistenz und Migration
Bestehende IDs und JSON-Version 9 erhalten. Bereits ergänzter dim-Eingang bleibt unverändert. Keine versteckte Umverdrahtung von Benutzerkarten.

## Risiken und Rückfallstrategie
Frühere dim-Semantik ist unveröffentlicht und wird korrigiert. Reelle Indexintervalle werden nicht still als ganzzahlige Mengen ausgegeben. Fehler bleiben sichtbar.

## Fortschritt / Entscheidungen / Abweichungen
2026-10-13: Nutzergruppe als Vorlage, korrigiert um diskrete Indexmenge und [0,180)-Vertreter gemäß bestehendem SIGNED-Vertrag. Tensorstufe bleibt mathematisch korrekt.

## Ergebnis und Verifikation
Finaler Lauf `./gradlew test :app:assembleDebug`: erfolgreich. Gradle-XML-Berichte: 1664 Tests, keine Fehler oder Überspringungen. Repository-/Architekturprüfung, Releaseplan, Versionsfolge und `git diff --check` erfolgreich. Unabhängiger node_verifier: keine blockierenden Findings; dim1 ausdrücklich geprüft. Kein Emulator-/Gerätelauf.

Konkreter zusätzlicher Befund: VektorOrientierungsKnotenV2300.tupelZuVektorErgebnis verlor Variablenquellen. Der Pfad verwendet jetzt dieselbe Metadatenübertragung wie der gemeinsame Übersetzer. Die Regression verwendet ausdrücklich diesen historischen Adapter aus der Nutzerkarte.

Importierbares korrigiertes Beispiel: build/artefakte/Neue-Karte-16-korrigiert.json (lokales Buildartefakt, vollständiger Graph vor Export durch den Integrationstest geprüft). Die Nutzerkarte wurde nicht in einer gespeicherten Bibliothek überschrieben. Ihre Stufe→dim-Verbindung muss durch die tatsächliche Koordinatenanzahl ersetzt werden; das Beispiel verwendet die korrigierte Definitionsgruppe mit drei Koordinaten.

Keine neue Knotenart, Produktionsabhängigkeit oder Formatversion. Anschließend in Branchcommit `8465f6c9` integriert und als Bestandteil des v3.2.1-Releasekandidaten dokumentiert; noch nicht auf `master` veröffentlicht.
