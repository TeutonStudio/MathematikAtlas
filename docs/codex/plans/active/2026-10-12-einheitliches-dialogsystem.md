# Einheitliches Dialogsystem

## 1. Titel und Status

2026-10-12. Implementierung und technische Prüfung fertig; Android-Geräteabnahme offen. Kein Releaseauftrag.

## 2. Ziel und Nutzerwirkung

Einheitliche, feste Abstände für Titel, Untertitel, Beschreibung, Abschnitte,
Einträge und Aktionen in allen Compose-Dialogen auf Android und Desktop.
Lange Texte und große Schrift sollen keine Aktionen verdrängen.

## 3. Nicht-Ziele

Keine Änderungen mathematischer Semantik, Knotentypen, Persistenz oder nativer
Dateidialoge. Keine Versionsreservierung, Veröffentlichung oder Toolchainmigration.

## 4. Untersuchter Istzustand

Material-AlertDialog und eigene Dialog/Surface-Hüllen existieren nebeneinander.
AtlasAbstände besitzt nur Kompakt. Formeleditor verwendet einen eigenen Rahmen.
Desktop verwendet bestehende gemeinsame Quellverzeichnisse; deren Whitelist bleibt
unverändert. Der Arbeitsbaum enthält umfangreiche fremde Änderungen, auch in
MathematikAtlasApp, VerwaltungsFenster, RechnerOperatorAuswahlDialog und Desktop Main.
Die Ausgangsfassungen betroffener Dialogdateien wurden vor Bearbeitung unter
/tmp/atlas-dialog-baseline gesichert; fremde Änderungen werden nicht zurückgesetzt.

## 5. Fachliche und mathematische Semantik

Unverändert. Dialoge koordinieren weiterhin ihre bestehenden Aktionen.

## 6. Daten-, Node-, Handle- und Edge-Vertrag

Unverändert. Keine neuen serialisierbaren Daten oder IDs.

## 7. Architekturentscheidungen

ADR 0047 definiert Ablage, Abstände, Größen und Scrollbesitz.
Kopfaktionen stehen unter dem Titel. Kopf und Fuß sind bei Überlänge begrenzt scrollbar.

## 8. Betroffene Dateien und Symbole

Gemeinsame API: KnotenKartenVerwalter/schnittstelle/AtlasDialog.kt.
Android-Dialoge und Dialogunterseiten in app, Formeleditorrahmen in MathematikKnoten,
Compose-Dialoge in desktopApp/Main. Bestehende Preview-Aufrufer verwenden weiterhin
die echten migrierten Produktkomponenten. Große-Schrift-Preview ergänzt.

## 9. Meilensteine

- [x] Bestand, Aufrufpfade, vergleichbare Dialoge und Modulgrenzen untersucht.
- [x] Gemeinsame Hülle, Kopf, Abschnitt, Eintrag, Listenzeile und Aktionen implementiert.
- [x] Compose-Dialogaufrufe migriert; eigene Scrollbereiche erhalten.
- [x] Gesamtprüfung erfolgreich: test, assembleDebug, lintDebug, desktopApp classes.
- [x] Grafische Desktopprüfung: 12 Layoutszenarien und aktivierter/deaktivierter Außenklick.
- [x] Unabhängige statische Prüfung; Außenklick-Finding korrigiert und freigegeben.
- [x] Android-Nachprüfung der gemeinsam scrollenden Such-/Legendenköpfe: Unit-Tests, assembleDebug und lintDebug erfolgreich (35s); unabhängig statisch freigegeben.
- [!] Android-Geräteabnahme: kein verbundenes Gerät und keine installierte AVD.
- [x] Eigener Diff gegen Ausgangsfassungen geprüft; fremde Änderungen erhalten. Dokumentation und ADR angelegt.

## 10. Konkrete Umsetzungsschritte

AtlasDialog und AtlasBestätigungsDialog ersetzen bisherige Hüllen.
Kopfzeilen und Fußaktionen delegieren an gemeinsame Bausteine.
Einstellungsseiten verwenden Abschnitte; Verwaltungs-/Auswahllisten gemeinsame
Einträge. Dialogdateien verwenden feste Dialogwerte statt allgemeiner App-Abstände.
Lazy-Inhalte erhalten begrenzte Resthöhe; doppelte Fenster-/Surface-Paddings entfallen.

## 11. Tests und Validierung

Repository- und Desktop-Shadowmodulprüfung; Gradle test, app assembleDebug/lintDebug,
desktopApp classes. Grafischer Desktoptest mit langen Texten, 1x/2x Schrift,
hell/dunkel, Hoch-/Querformat und großem Fenster; Scroll bis zum letzten Eintrag
bei unverändertem Footer. Kein Test bloßer Konstanten.
Android-Laufzeitprüfung setzt einen verbundenen Emulator/ein Gerät voraus.

## 12. Persistenz und Migration

Keine Änderung und keine Migration erforderlich.

## 13. Risiken und Rückfallstrategie

Große Texte und niedrige Fenster können mehrzeilige Kopf-/Fußbereiche benötigen.
Eigene Listen dürfen nicht in zusätzliche vertikale Scrollcontainer gelangen.
Rücknahme nur eigener Dialogänderungen, niemals fremder Arbeitsbaumänderungen.

## 14. Fortschritt

Repository-/Architektur-/Shadowmodulprüfung sowie Releaseplan und Versionsfolge grün.
Gesamtprüfung `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:classes`
mit grafischer Sitzung erfolgreich (1m 19s). Desktoptest: zwei Tests, keine Fehler
oder übersprungenen Fälle; Layouttest umfasst 390×800, 800×390 und 1100×760,
jeweils 1x/2x Schrift und hell/dunkel. Letzter Eintrag erreichbar, Footer bleibt
stehen. Innen-/Außenklicktest verwendet direkt zugestellte AWT-Mausereignisse;
Robot-Eingabe war in der Sitzung nicht zuverlässig zustellbar.

Erste Lintfehler durch überflüssige äußere BoxWithConstraints-Hüllen wurden
korrigiert; die innere Tabellenbreitenmessung bleibt bestehen. Unabhängige Prüfung
fand einen verlorenen Außenklick; explizite Außenflächenbehandlung unter Beachtung
der Property und Innenflächenkonsum wurden ergänzt, nachgeprüft und getestet.

Die letzte Android-Nachprüfung `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
ist erfolgreich (35s). Die letzten Scrollkorrekturen wurden unabhängig statisch
freigegeben. GUI-Tests werden in rein headless laufender CI ausdrücklich übersprungen;
der hier dokumentierte grafische Lauf hatte keine übersprungenen Dialogtests.

ADB meldet keine Geräte; `emulator -list-avds` liefert keine AVD. Android-Geräte-
und Tastaturabnahme ist deshalb offen; APK-Build ist kein Ersatz dafür.

## 15. Entscheidungsprotokoll

2026-10-12: Nutzer wählt alle Compose-Dialoge und feste Dialogabstände.
2026-10-12: Bestehenden SamAI-Branch weiterverwenden; keine Release-/Gitmutation.
2026-10-12: Kopfaktionen grundsätzlich unter dem Text; gemeinsame Implementierung
verhindert breitenabhängige Titelverdrängung und wahrt volle Textbreite.

## 16. Abweichungen vom ursprünglichen Plan

Kopfaktionen stehen auch in breiten Dialogen unter dem Text. Bei extremer Höhe sind
Kopf und Fuß selbst scrollbar, statt den Inhaltsbereich vollständig zu verdrängen.
Keine Behauptung einer Android-Geräteprüfung ohne verbundenes Gerät.
Such-/Modus-/Tabsteuerung und Legendenbeschreibung scrollen bei niedrigen Fenstern
mit ihren Lazy-Listen, statt den Einträgen den gesamten Restplatz zu nehmen.

## 17. Ergebnis und Verifikation

Gemeinsame Hülle und semantische Layoutbausteine implementiert; alle gefundenen
Compose-Dialoge migriert, einschließlich fachneutralem Gruppeninspektor.
Keine neuen Produktionsabhängigkeiten, Versions-/Persistenzänderungen oder Commits.
Arbeitsbranch: samai/v3.1.1/rechneroperatoren-konsolidierung.
Ausgangs-HEAD: 7123235cc911c838bfbd98117d597af442f9bde3.

Prüfnachweise: Gradle-Testberichte und app/build/reports/lint-results-debug.html.
Lokale Ablaufprotokolle: /tmp/atlas-dialog-final2.log (Gesamtprüfung),
/tmp/atlas-dialog-android-final.log (letzte Android-Nachprüfung),
/tmp/atlas-dialog-repository-final.log, /tmp/atlas-dialog-releaseplan.log und
/tmp/atlas-dialog-versionsfolge.log. /tmp ist kein dauerhafter Artefaktspeicher.

Offene, nicht durch Codeänderung lösbare Prüfgrenze: Android-Geräteabnahme und
manuelles Öffnen jeder einzelnen Fachseite. Daher bleibt der Plan zur Abnahme aktiv;
die technische Implementierung wird nicht als vollständige Geräteabnahme ausgegeben.
