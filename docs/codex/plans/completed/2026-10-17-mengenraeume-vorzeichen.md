# Mengenräume und vorzeichenrichtige Formelausgabe

## Status, Ziel und Nutzerwirkung
Abgeschlossen, 17.10.2026. Tupel-/Tensorraum als Mengenrechneroperatoren, Dimensionsart
am Menge-Knoten, korrekte Mengenklammern und Subtraktion negativer Summanden.

## Nicht-Ziele
Keine neuen Knotentypen, Abhängigkeiten, Formatversion, Releases oder Commits.
Vorhandene lokale Änderungen bleiben erhalten; Ausgangsdiff /tmp/atlas-mengen-before.patch.

## Untersuchter Istzustand
MengenRechnerOperator bietet Matrizenmenge; Tensorraum trägt beliebig viele
Achsen, matrixForm trennt historische Matrixwerte. Addition.zuLatex verbindet
alle Summanden mit Plus. Renderer 1.3.0 löst brace-Aliase bei Delimitern nicht auf.

## Fachliche und mathematische Semantik
A^n für n>=0; A^0 enthält das leere Tupel. Tensorachsen sind positive natürliche
Zahlen; Stufe ist die Länge eines nichtleeren Tupels. Stufe zwei akzeptiert auch
Matrixwerte. Symbolische Dimensionen behalten Quellen und Voraussetzungen.
Explizit negative Summanden werden mit Minus ausgegeben, erforderliche Klammern
bleiben erhalten; unbekannte Vorzeichen werden nicht erraten.

## Daten-, Knoten-, Anschluss- und Kantenvertrag
Bestehende Arten, neue Operator-IDs. Menge speichert dimensionsart=tupel/tensor,
Dimensionseingang behält logische Identität, wechselt Zahl/Tupel. Verbundene
Wechsel verwenden Verlustbestätigung und einen Undo-Schritt.
Historische n/m-Ports wandern in eine explizite Tupelzusammenstellung mit
stabilen Original-IDs; neue Hilfs-IDs sind deterministisch.

## Architekturentscheidungen
Semantik im Rechenkern, validierte Konfiguration/Auswertung im Knotenmodul,
UI über bestehende Aktionen. Gemeinsamer Codec migriert Android und Desktop.
Keine zweite Stringsemantik; Vorzeichenausgabe aus dem Ausdrucksbaum.

## Betroffene Dateien und Symbole
MengenRechner, Mengenraeume, Mengen/ObjektVergleich, Operatoren;
MengenOperatorKnoten, KonsolidierteKnoten, TensorraumDimensionenMigration,
MathematikKartenCodec, LatexText, Katalog/Suchindex; Android-/Desktopinspector.

## Meilensteine und Fortschritt
- [x] Istzustand und Nutzerentscheidungen erfasst.
- [x] Kern, Operatoren und Konfiguration erweitert; gezielte Regressionen grün.
- [x] Gemeinsame Migration, Verlustvorschau und beide Inspectoren integriert.
- [x] 2.350 JVM-/Desktopfälle: 0 Fehler, 2 bestehende Opt-in-Skips; Android-Lint, reguläre App-/Test-APKs und Desktopkompilierung erfolgreich.
- [x] Unabhängiger node_verifier hat ursprüngliche und ergänzte Korrekturen abgenommen; Diffprüfung grün.

## Konkrete Umsetzungsschritte
Kernverträge und LaTeX korrigieren, gemeinsame Auswertung und Konfiguration
anschließen, sichtbare Matrizenvarianten ersetzen, historische Ports migrieren,
UI integrieren, Verhalten testen, Prüfungen dokumentieren.

## Tests und Validierung
Gezielte Kern-/Knoten-/Appregressionen: Dimensionen, Symbolbindungen, Migration,
Vorzeichen/Klammern und Formelroundtrip. JVM/Desktop, Android-Lint/APKs,
Repository/Standardkarten/Releaseguards. Native Darstellung soweit verfügbar.

## Persistenz und Migration
Keine Formatänderung. Gemeinsame idempotente Lademigration, Alt-IDs lesbar;
mehrdeutige Originale mit Diagnose erhalten. Kopieren/Undo über Standardaktionen.

## Risiken und Rückfallstrategie
Großer vorhandener Arbeitsdiff: Änderungen gegen gesicherten Ausgang prüfen.
Symbolische Quellen dürfen nicht per Anzeigenamen verwechselt werden.
Historische Matrixräume behalten ihren engeren Vertrag; neue allgemeine
Tensorräume erlauben beide Darstellungen. Keine Datenreparatur durch Ersatzwerte.

## Entscheidungsprotokoll
17.10.2026: Nutzer wählt endliches Dimensionstupel, zwei Operatoren und
Matrixmitgliedschaft im zweidimensionalen Tensorraum. Addition bleibt intern
Addition negativer Faktoren; Ausgabe verwendet Subtraktion.

## Abweichungen vom ursprünglichen Plan
Methodenwertige oder nicht eindeutig auflösbare historische Dimensionen werden
mit Diagnose im Original erhalten: Der gewöhnliche Tupelknoten kann Methoden
nicht punktweise zu einer Dimensionsmethode kombinieren. Die Prüfung folgt auch
methodenpriorisierten Rechnerausgängen; reine Zahlenquellen bleiben migrierbar.
Dimensions-Kartenfallbacks ohne auflösbare Inhaltsgarantie bleiben konservativ
erhalten. Dadurch entstehen keine stillen Ersatzwerte und keine beschädigten Karten.

Im Renderer müssen einfache brace-Symbole als Namen, skalierte Delimiter als
escaped Klammern übergeben werden. Das indizierte Kreuz benötigt mathop mit
Unicode-Glyphe, weil der Renderer im mathop-Argument die times-Symbolnode nicht
als Text extrahiert. Native Bilder haben beide Unterschiede nachgewiesen.

## Ergebnis und Verifikation
Gezielte und anschließend breite JVM-Prüfungen bestehen. Neue Fälle betreffen
Tupelnullraum, Tensorstufen/Matrixmitgliedschaft, Quellenbindungen, widersprochene
Dimensionsannahmen, Migrationsidentitäten/Idempotenz, direkte und weitergereichte
Methodendimensionen, Fallbackdiagnosen, Verlustvorschau/Undo/Redo/Kopieren,
eigene Mengenkarten und Vorzeichen-/Potenz-/Formelroundtrips.

Native Desktopbilder /tmp/atlas-mengen-vorzeichen-{hell,dunkel}.png wurden
visuell geprüft. Isolierte Androidprüfung am SM-X920/Android16: 4 Fälle,
0 Fehler/Skips, einschließlich echtem Dimensionsart-Schalter und Formeln beider
Themes; /tmp/atlas-mengen-device-final.log und .xml. Einzelbilder
/tmp/mengen-formel-5-hell.png bis -8-dunkel.png visuell geprüft. Prüf-App im
Hauptprofil automatisch entfernt, Original-App und Daten nicht verändert.
Ein früher Geräteversuch scheiterte einmal beim Activity-/Compose-Start; ein
anderer deckte die inzwischen behobene limits-Glyphe auf. Der finale Lauf ist
vollständig grün; die Ursache des einzelnen Startfehlers wurde nicht bewiesen.

Repository/Architektur/Shadowmodulprüfung, 15 Standardkarten, Releaseplan,
Versionsfolge und 14 Guardregressionen grün; /tmp/atlas-mengen-*-abschluss.log.
Zusätzliche Standalone-Kernprüfung nicht ausführbar: kotlinc fehlt im PATH,
Java ist vorhanden; Gradle-Kerntests sind ausgeführt. Finaler vollständiger
Gesamtlauf /tmp/atlas-mengen-final-complete.log erfolgreich: 1m55s, 248 Tasks;
`./gradlew test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin`.
2.348 bestandene JVM-/Desktopfälle, 0 Fehler, 2 bestehende Opt-in-Skips.
Der zusätzliche negative-Potenz-/Vorzeichenroundtrip nutzt für den vorhandenen
Iterationsoperator den vorgesehenen Operationscallback zur Kern-Potenz.
`git diff --check` erfolgreich; keine neuen Typen oder Abhängigkeiten.

Keine Versions-/Branchwechsel, Commits, Pushes oder Veröffentlichung. Branch
samai/v3.4.12/pie-methoden-matrizen, HEAD
20356f1c3c8e996fae03ed10ff10d9fe8005debe unverändert. Der bestehende HEAD trägt
Alexander/GitHub, es wurde kein neuer SamAI-Commit erzeugt. Vorhandene Änderungen
an Releaseplan, Visualisierung, Pie und anderen Bereichen wurden bewahrt.


## Abschließende Quellabnahme
Der unabhängige node_verifier prüfte die Änderungen gegen den vorhandenen
Arbeitsstand und nach Korrekturen nochmals die Methodenquellen-/Fallbackgrenze,
Quellenmetadaten, Formelbedingungen/-bereiche, negative Potenzbasen, Doppelminus
und eigene Mengenkarten. Keine offenen Findings. Die zuvor entdeckten Probleme
wurden korrigiert und regressionsgeprüft. Der gesicherte Ausgangsdiff dient zur
Abgrenzung der bereits vorhandenen Änderungen, nicht als Grundlage zum Zurücksetzen.

Siehe [Modellentscheidung](../../decisions/2026-10-17-mengenraeume-vorzeichen.md).
