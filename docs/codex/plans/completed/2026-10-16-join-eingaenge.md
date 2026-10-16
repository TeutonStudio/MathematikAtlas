# Join-Eingänge – ExecPlan

Status: implementiert und unabhängig codegeprüft, 2026-10-16. Gesamtprüfpfad einschließlich Lint erfolgreich. Keine Veröffentlichung oder Versionsreservierung.

## Ziel und Nutzerwirkung
Geeignete kommutative und assoziative Operatoren erhalten einen wachsenden Mehrfacheingang mit einzeln greifbaren Leitungen. Eine Quelle genügt, null Quellen sind unvollständig. Explizite lokale Werte bleiben erhalten.

## Nicht-Ziele
Neue Knotentypen, Abhängigkeiten, Veröffentlichung und beiläufige Änderungen vorhandener lokaler Arbeiten.

## Untersuchter Istzustand
Graphprüfung und Kartenaktionen ersetzen belegte Eingänge. Die Auswertung sammelt Map<String,BedingterWert> und würde Mehrfachwerte überschreiben. KartenDatenJson schreibt Format 9. Der gemeinsame mathematische Codec und der kanonische Katalog bedienen Android und Desktop. Gruppenprojektionen bieten bereits eine Abbildung von Darstellungsanschlüssen auf Originale.

## Fachliche Semantik
Join nur für geschlossene assoziative und kommutative Operationen. Matrixprodukt und Tensorprodukt bleiben geordnet. Zahlenmultiplikation und komponentenweise Produkte benötigen Bereichsnachweise; sonst geordnete Darstellung. Beiträge werden nie dedupliziert. Keine impliziten Neutralwerte oder Platzhalter im Join.

## Daten-, Anschluss- und Verbindungskontrakt
Explizite Einzel-/Mehrfachbelegung, gespeicherte Beitragsreihenfolge, stabile Verbindungsidentitäten. Lokale Operanden bleiben mathematische Knotendaten. Format 10; Altdefault Einzelbelegung. Auswertungskontext ergänzt vollständige Beitragslisten. Bestehende Einzeleingänge behalten ihre API.

## Architekturentscheidungen
Mathematische Eignung im Kern/Knotenmodul; neutrale Kardinalität und Darstellungsprojektion im Editor; Sammlung und Caches im Adapter. Die Projektion bildet einzelne Leitungsenden auf ihren gemeinsamen Eingang zurück. Runtime-Projektionen werden nicht gespeichert.

## Betroffene Bereiche
Graphdaten/Codec, Graphprüfung/Kartenaktionen/Editorprojektion, Adapter-Auswertung, Operatorregister/Konfiguration/Migration und gemeinsame Inspectorbausteine.

## Meilensteine und Fortschritt
- [x] Kardinalität, Persistenz und Graphaktionen
- [x] Operatorvertrag, Sammlung, Migration und lokale Werte
- [x] Darstellung und Inspector
- [x] Tests, Laufzeitprüfung und unabhängige Abnahme (Android-Gestenprüfung mangels Gerät offen)

## Umsetzungsschritte
1. Neutrale Mehrfachbelegung und Reihenfolge ergänzen; Einzelfälle unverändert.
2. Sammlung mit Listen und deterministischer Entfaltung für bestehende Auswerter ergänzen.
3. Gemeinsame mathematische Normalisierung und Migration implementieren.
4. Projektion mit eindeutigen Andockpunkten und Kapsel; Gesten zu Beitrags-IDs zurückführen.
5. Inspectorwerte validiert übernehmen, feste Operandenanzahl im Join ausblenden.
6. Gezielte Tests, Gesamtprüfungen und unabhängige Diffprüfung.

## Tests und Validierung
Null/ein/viele, doppelte Quelle, Typen/Dimensionen, Quaternionenwechsel, Bedingungen/Herkunft, Cache, Umverdrahten, Zyklus, Undo, Altformat/Roundtrip/Idempotenz, Kopieren, Gruppen/Methoden, Desktop/Android-Gesten und Zoom/Themes.

## Persistenz und Migration
Gemeinsame Codec-Pipeline, idempotent. Verbindungs-IDs erhalten; Zielanschlüsse und Reihenfolge übernehmen; explizite lokale Werte übernehmen, leere Plätze entfernen. Historische Dateien nicht automatisch überschreiben.

## Risiken und Rückfallstrategie
Mehrfachwerte dürfen in keinem Sammelpfad verloren gehen. Bestehende lokale Änderungen werden bewahrt. Noch nicht ausgeführte Prüfungen werden ausdrücklich offen dokumentiert.

## Entscheidungsprotokoll
2026-10-16: Nutzer wählt alle geeigneten Operatoren, Ein-Quellen-Identität, wachsende Kapsel, automatische Altdatenmigration, geordneten Fallback bei fehlendem Kommutativitätsnachweis und lokale Operandenliste.

## Abweichungen
Der Bereichswechsel wird als reine Projektion durchgeführt; die persistierte Beitragsfolge bleibt stabil. Bestehende Fachauswerter werden am Registerrand mit vollständigen Listen entfaltet. Der vorhandene Konfliktvertrag wurde um entfallende lokale Werte erweitert. Historische Methodenquellen behalten ihre öffentliche Signatur und normalisieren erst die interne Karte.

Die Android-Gestenprüfung wurde ergänzt und kompiliert, kann ohne angeschlossenes Gerät jedoch nicht ausgeführt werden. Der gemeinsame Desktop-Gestenpfad ist tatsächlich geprüft.

## Ergebnis und Verifikation
- 21 gezielte Join-Regressionen: alle bestanden. Familien-Singletons, lokale Aussagen, Cache, Migration/Idempotenz, doppelte Quelle, Reihenfolge, Kopien/Fallbacks, Operatorwechsel/Überhang, eingebettete Methoden und Darstellung ohne Reserveoperand.
- Desktop-Gestentest tatsächlich ausgeführt: Touch/Maus, Zoom 0,5/1,25, hell/dunkel und normale/doppelte Schrift. Screenshot `/tmp/atlas-join-desktop.png` visuell geprüft.
- Gesamte JVM-/Desktop-Testberichte: 2.027 bestanden, 0 Fehler, 1 vorhandene übersprungene Opt-in-Messung.
- Android-App-, App-Test- und Editor-Test-APKs gebaut; Android-Lint erfolgreich. Finaler Gradle-Aufruf: `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :KnotenKartenVerwalter:assembleDebugAndroidTest :app:lintDebug :KnotenKartenVerwalterDesktop:test :MathematikKartenAdapterDesktop:test :MathematikKnotenDesktop:test :desktopApp:test` – BUILD SUCCESSFUL.
- Repository/Architektur, Releaseplan und Versionsfolge erfolgreich. `git diff --check` erfolgreich.
- Zusätzliche Standalone-Kernprüfung nicht ausführbar: `kotlinc` fehlt; Kern-JVM-Tests sind im erfolgreichen Gradle-Testpfad enthalten.
- ADB meldet keine angeschlossenen Geräte. Android-Laufzeitprüfung daher ausdrücklich offen, kein Android-Funktionsnachweis behauptet.
- Unabhängige Nachprüfung nach Behebung sämtlicher Findings abgenommen; abschließende Formeländerung separat nachgeprüft, keine neuen Findings.
- Bestehender Branch `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1352ad551622c1916fbd8f36c9ab83105`. Keine neuen Branches, Commits, Pushes oder Veröffentlichung; keine neuen Autor-/Committermetadaten. Fremde lokale Änderungen erhalten.
