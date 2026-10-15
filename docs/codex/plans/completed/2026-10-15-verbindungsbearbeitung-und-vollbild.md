# Verbindungsbearbeitung und Android-Vollbild

## Status
Abgeschlossen und unabhängig abgenommen, 2026-10-15.

## Ziel und Nutzerwirkung
Unpassende Anschlüsse über vorhandene Übersetzerketten verbinden, bei mehreren gleich kurzen Wegen auswählen. Unverbundene Knoten auf Verbindungen ziehen zum Einfügen; bewusstes Herausziehen schließt die Lücke. Android nutzt den Bildschirm ohne dauerhaft sichtbare Systemleisten.

## Nicht-Ziele
Keine neuen Knotentypen, mathematischen Übersetzungsverträge oder Versions-/Releaseänderungen. Vorherige uncommittete Graphoptimierung erhalten.

## Untersuchter Istzustand
`KartenEditorZustand.anschlussAngeklickt` übernimmt nur direkt erlaubte Kanten, `kompatibelMitStart` steuert Snap-Ziele. `KnotenKartenEditor` bündelt Drag-Positionen bereits über `beginneInteraktion`/`beendeInteraktion`. `KartenInhaltErsetzen` erlaubt atomare Graphänderungen. Vorhandene Übersetzer werden über `konfiguriereÜbersetzer` erzeugt; Mathematik gehört in `MathematikKnoten`, nicht in den neutralen Editor. `MainActivity` verwendet derzeit statusBarsPadding und keinen immersiven Modus.

## Fachliche und mathematische Semantik
Direkte Kanten haben Vorrang. Typgeprüfte kürzeste azyklische Übersetzerketten verwenden bestehende Auswerter und ihre Parameter. Gleiche Anschlussart ist keine Berechtigung für beliebige Koordinatenumdeutung. Keine Rückführung ohne Zeugnis. Die Anschlussverträge bestimmen strukturelle Zulässigkeit; mathematische Datenvoraussetzungen bleiben in den vorhandenen Auswertern überprüft.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag
Stabile vorhandene IDs erhalten; neue Übersetzer/Kanten bekommen neue IDs. Kein JSON-Wechsel. Jede komplette Verdrahtung wird vor Übernahme gegen die aktuelle Karte geprüft, Auswahlvorschläge werden bei veraltetem Stand verworfen. Abbruch verändert keine Kanten. Einfügen/Herausziehen respektiert Richtungen, Typen, Zyklen und belegte Ports.

## Architekturentscheidungen
Neutraler Vorschlagsvertrag und Auswahlzustand im Editor; mathematischer Routenplaner in MathematikKnoten, von Android und Desktop installiert. Gemeinsamer Dialog im Editor. Bestehende History und Vollkartentransaktion verwenden. Herausziehen ausdrücklich aktiviert (Nutzerantwort): Desktop Alt-Ziehen, Tablet Kontextaktion für den nächsten Drag. Automatisches Einfügen nur beim Ziehen eines zunächst unverbundenen Knotens; bei mehreren Portpaaren auswählen.

## Betroffene Dateien und Symbole
KartenEditorZustand, KnotenKartenEditor, neue neutrale Verbindungsplanung, Übersetzer-Verbindungsplaner, Android-/Desktop-Zustand und Kontextaktionen, MainActivity, Tests und Dokumentation.

## Meilensteine
- [x] Bestand und Verträge untersucht; Nutzerentscheidung zum bewussten Herausziehen übernommen.
- [x] Neutrale Planung/Auswahl und mathematische Kettensuche implementiert; Vorschau getrennt und begrenzt zwischengespeichert.
- [x] Drag-Einfügen/Herausziehen, Hervorhebung, Alt-/Touch-Aktion und Android-Vollbild implementiert.
- [x] Regressionen, Gesamtbuild und unabhängige Abnahme durch node_verifier abgeschlossen, keine offenen Findings.

## Konkrete Umsetzungsschritte
Vorschläge rein planen; automatische Verbindung oder Dialog; bei Übernahme Basis und Graph prüfen; Gesten mit Weltkoordinaten und tatsächlicher Béziergeometrie verbinden; bewussten Lösemodus anbieten; Android WindowInsetsController aktivieren; Tests und Review abschließen.

## Tests und Validierung
Gezielte Graph-/Historytests, Übersetzerketten samt mehrdeutigen Wegen, abgebrochene/veraltete Dialoge, ungültige Graphen, Insert-/Bypass-Portpaare, Zoom-/Drag-Verhalten. Desktop-Compose-Tests und `gradlew test :app:assembleDebug`; Android-Instrumentierung mindestens kompilieren, tatsächlichen Gerätestatus ausdrücklich berichten.

## Persistenz und Migration
Vorhandene Übersetzer-/Kartenserialisierung weiterverwenden. Neue Auswahl-/Gestenzustände sind flüchtig. Kein Formatwechsel.

## Risiken und Rückfallstrategie
Routensuche begrenzen durch kürzeste Wege ohne wiederholte Typzustände; keine stillen Kantentrennungen. Mehrdeutigkeit bleibt Auswahl. Normales Verschieben behält Verbindungen. Bypass nur bei eindeutiger Eingangsquelle und geprüft kompatiblen Zielen. Bei Fehler unveränderten Graph beibehalten. Vorherige unabhängige Änderungen nicht zurücksetzen.

## Fortschritt
2026-10-15: 24 neue JVM-/Desktoptests grün (11 neutraler Graph, 10 Mathematik, 3 Desktop). Ein Android-Vollbild-Instrumentierungstest ergänzt und kompiliert. Echte Portklicks, Auswahl/Abbruch, Touch-Drag bei Zoom 0,5 und Alt-Maus-Drag bei Zoom 1,5 samt Undo getestet.
2026-10-15: Quellen und Gestenpfad geprüft; Nutzer bestätigt bewusste Aktivierung des Herausziehens.

## Entscheidungsprotokoll
2026-10-15: Objektpunkt benötigt Anker und wird nicht automatisch angeboten. Aufgelöste Portverträge ermöglichen eine kleine Typvorschau; vollständige Planung erst beim Ablegen. Synthetisch direkter Typvergleich wird konservativ als möglich betrachtet, weil echte abhängige Ausgänge einen Übersetzer benötigen können. Vorherige Zielbelegungen bleiben während der Planung bestehen und werden erst abschließend ersetzt. Bekannte Dimensionswidersprüche und Anschlussart-Umwege sind ausgeschlossen.
2026-10-15: Direkte Kanten priorisieren und kürzeste Übersetzerketten anbieten. Herausziehen aktivieren statt normales Verschieben umzudeuten. Kein neuer Knotentyp.

## Abweichungen
Die UI prüft Adapterverfügbarkeit über eine separate begrenzte Typvorschau, damit nicht jeder sichtbare Port vollständige Graphen plant. Der Review fand vorzeitig entfernte Zielkanten und eine zu enge synthetische Vorschau; beide Fälle sind behoben und regressiongetestet. Ein Dimensions-Regressionsfall erforderte zusätzliche Schranken gegen Typverlust über Zwischenformen. Schnelle Drag-Ereignisse prüfen nun den ursprünglichen Down-Punkt statt der Slop-Position. Android-Laufzeittest bleibt mangels Emulator/Gerät und /dev/kvm ungetestet; alle unabhängig ausführbaren Prüfungen wurden abgeschlossen.

## Ergebnis und Verifikation
- `bash gradlew test :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`: erfolgreich in 41 s, 161 Tasks (11 ausgeführt, 150 berechtigt wiederverwendet).
- Aktuelle XML-Ergebnisse: 1.777 Tests, 0 Fehler, 0 übersprungen: Graph 135, Adapter 66, Knoten 686, Kern 556, TypSystem 10, App 301, Desktop 23. Davon 24 neue Tests für diese Aufgabe; die vorherige Graphoptimierung bleibt erhalten.
- `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py`, `pruefe_standardkarten.py` und `git diff --check`: erfolgreich.
- Diagnoselauf mit 221 Knoten/199 Kanten: isoliert etwa 24 ms für 20 Vorschauziele/198 ms für die konkrete Planung; im parallelen Gesamtprüflauf 79 ms/498 ms. Kein hardwareunabhängiges Zeitlimit behauptet.
- Android-Debug-APK unter `app/build/outputs/apk/debug/app-debug.apk` erzeugt. Android-Instrumentierung einschließlich `VollbildGeraeteTest` kompiliert, nicht auf Emulator/Gerät ausgeführt. Systemleisten müssen dort noch praktisch bestätigt werden.
- `node_verifier` hat den finalen Code, Regressionen, ADR und erfolgreichen Gesamtprüflauf unabhängig abgenommen; keine offenen Findings.
- [Architekturentscheidung](../../decisions/2026-10-15-automatische-verbindungsplaene.md); Bedienung unter `docs/DEVELOPMENT.md`.
- Kein neuer Knotentyp, Schema, Release oder Versionswechsel. Arbeitsbranch `work`, Basis `e535190058014810617ea2989d27838afe54692b`. Kein neuer Commit erzeugt; Basisautor Alexander, Basiscommitter GitHub. Vorherige unabhängige Änderungen erhalten.
