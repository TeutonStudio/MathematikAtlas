# Anschlussinformationen – ExecPlan

## Status, Ziel und Nutzerwirkung
Abgenommen, 2026-10-16. Maus-/Stift-Hover zeigt nach 500 ms eine Kurzinfo;
Tippen öffnet eine lesende Detailansicht; Ziehen behält den bestehenden Ablauf.

## Nicht-Ziele
Keine Auswertungswerte, Persistenz-, Versions-, Abhängigkeitsänderung, Commits oder Pushes.

## Untersuchter Istzustand
`AnschlussGriff` kombiniert Click/LongClick und Drag. Click ruft bisher
`anschlussAngeklickt` auf; Drag verwendet denselben Zustandsabschluss.
Android und Desktop teilen diesen Editor. Join-Enden und Gruppen werden projiziert.
Vergleichsdialog: AnschlussLegendenDialog; Rahmen: AtlasBestätigungsDialog.
Der Arbeitsbaum enthält umfangreiche fremde Änderungen. Ausgangskopien der
berührten Dateien liegen unter `/tmp/anschlussinfo-baseline`.

## Fachliche Semantik und Verträge
Die Info ist eine Modellansicht ohne Kartenaktion. Zulässige Arten und effektive
semantische Typen stammen aus vorhandenen Registern/Auflösungen. Nur ausdrücklich
registrierte optionale Eingänge erhalten einen Optional-Hinweis. Keine Pflichtannahme.
Stabile Originalreferenzen identifizieren Ports; Beitrags-IDs identifizieren Join-Leitungen.

## Architekturentscheidungen
Fachneutrales Infomodell und UI im Editor, optionaler Provider pro Editorinstanz.
MathematikKartenLaufzeit liefert die fachlichen Metadaten für beide Plattformen.
Popup außerhalb skalierter Knoten, nicht fokussierend und innerhalb des Fensters.
Dialog ohne Eingaben, primäre Aktion Schließen; alle Dismiss-Wege identisch.

## Betroffene Dateien und Symbole
KnotenKartenEditor/AnschlussGriff; neue AnschlussInfo-Modell-/UI-Helfer;
MathematikKartenLaufzeit und Android-/Desktop-Aufrufstellen; Join- und
Adapterauswahl-UI-Tests sowie neue Info-/Hover-/Gestentests.

## Meilensteine und Umsetzungsschritte
- [x] Bestand, gemeinsame Dialoge und tatsächliche Gestenpfade untersucht.
- [x] Infomodell/Provider und Hover-/Dialogintegration implementiert.
- [x] Click-Tests auf Drag umgestellt; Desktop- und Android-Regressionen geprüft.
- [x] Unabhängiger Review inklusive Nachprüfung abgenommen; finaler Prüflauf und Dokumentation abgeschlossen.

## Tests und Validierung
Gezielte Modell-, Desktop-Compose- und Android-MotionEvent-Tests; danach
`./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :KnotenKartenVerwalter:assembleDebugAndroidTest :app:lintDebug`.
Repository-/Architekturprüfung, Releaseplan-/Versionsfolgeprüfung, Diffprüfung.
Gerätestatus separat feststellen; physischer Stiftlauf nur bei verfügbarer Hardware.
Abnahme: Hover-Abbruch, Click ohne Graphänderung, Drag ohne Dialog/Kamerabewegung,
Kontextmenü, Join/Adapter/Undo/Redo, Projektion, Schließen und Layoutvarianten.

## Persistenz und Migration
Nur kurzlebiger UI-Zustand und abgeleitete Daten; Format 10 bleibt unverändert.

## Risiken und Rückfallstrategie
Pointer-Konsumierung und projizierte Join-Ports sind besonders prüfbedürftig.
Bei Fehlern nur aufgabeneigene Änderungen anhand der Ausgangskopien zurücknehmen.

## Fortschritt
Infomodell, Provider für Android/Desktop und gemeinsamen Editor sowie Infofenster
implementiert. AtlasDialog behandelt Escape unter Beachtung von dismissOnBackPress.
Desktop-Info-, Join-, Adapter-, Navigations- und Dialogregressionen ausgeführt;
Screenshots 390×600 und 800×350 mit Schriftfaktor 2 in beiden Themes visuell geprüft.
Android auf SM-X920 / Android 16: 3 Info-/Stift-/Touchtests und 1 Join-Test bestanden.
Der globale Zurück-Test war wegen fehlendem Systemfokus der Testfenster nicht
anwendbar; direkte native Dialog-KeyEvents bestätigen den Dialogabschluss.
Keine interaktive Bedienung mit physischem Stift behauptet.

## Entscheidungsprotokoll
2026-10-16: Nutzer bestätigt Hover ausschließlich ohne Berührung und Dialog ohne
Auswertungswerte. LongPress/Rechtsklick behalten Kontextmenü. 500 ms Hover-Verzögerung.

## Abweichungen vom ursprünglichen Plan
- Shared Escape-Korrektur am zuständigen AtlasDialog-Rahmen nach nachgewiesenem
  Desktop-Tastenevent-Fehler; bestehende Dialoglayouttests weiterhin grün.
- Android-HoverExit läuft über einen nativ verzögerten Handler; Tests warten mit
  begrenztem waitUntil auf dessen Abschluss. Künstliche Clock allein genügte nicht.
- Globale Back-Key-Injektion erreicht die unfokussierten Testfenster nicht. Back
  daher direkt an deren native Dialog-DecorView senden; Systemnavigation bleibt offen.
- Domain-Test fand, dass effektiverMathematikAnschluss bei unveränderten Verträgen
  null liefert: vorhandener Originalanschluss ist nun der korrekte Fallback.
- Review korrigierte eine zu starke Identitätsaussage für optionale Methoden; es
  wird ausschließlich „Optionaler Methodeneingang.“ angezeigt.

## Ergebnis und Verifikation
Erster Gradle-Gesamtlauf mit Debug-/App-Test-/Editor-Test-APKs und Android-Lint grün.
Finale JVM-Ergebnisse nach Kartenwechselkorrektur und zusätzlichem Popup-Zoomtest:
2090 Testfälle, davon 2089 bestanden und 1 bestehender Opt-in-Skip, 0 Fehler.
Finaler Gesamtlauf einschließlich Debug-/App-Test-/Editor-Test-APKs und Android-Lint:
BUILD SUCCESSFUL (2m 2s). Gezielte Desktop-Info-Tests 5/5 grün.
`pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py` erfolgreich.
`pruefe_kern.py` nicht ausführbar: kotlinc fehlt; Gradle-Kerntests ausgeführt.
Taskdiff gegenüber Ausgangskopien geprüft, git diff --check erfolgreich.
Unabhängiger read-only node_verifier: keine offenen Produktionsfindings, Abnahme und
Nachprüfung der letzten Ergänzungen erteilt.
Branch release/v3.4.0-methoden-grundraeume-visualisierung; HEAD
04925fc1352ad551622c1916fbd8f36c9ab83105 unverändert. Keine Commits, daher keine
neu erzeugte Autor-/Committeridentität. Keine Versions- oder Persistenzänderung.

## Bewusst offene, nicht blockierende Prüfgrenzen
- Physischer Stiftkontakt/Hover durch einen Menschen wurde nicht ausgeführt. Die
  entsprechende MotionEvent-Verarbeitung ist auf echter Android-16-Hardware geprüft.
- Systemweite Zurück-Eingabe mit regulärem Fensterfokus ist nicht nachgewiesen; der
  native Dialog-Backpfad wurde direkt ausgeführt, der Produktions-Backpfad blieb unverändert.
- Tastatur-Einblendung ist für den rein lesenden Dialog ohne Eingabefelder nicht
  relevant. Die vorhandene gemeinsame Safe-Area-/IME-Behandlung bleibt erhalten.
- Das zusätzliche Standalone-Kernskript benötigt ein nicht vorhandenes kotlinc.
  Die vollständigen Gradle-Kerntests sind erfolgreich; keine Kernänderung.
