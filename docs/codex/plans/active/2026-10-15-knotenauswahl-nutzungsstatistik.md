# Lokale Nutzungs- und Suchhistorie der Standardknotenliste

## 1. Status

Implementiert und unabhängig statisch abgenommen; Android-Laufzeitnachlauf vor Integration offen. Umsetzung nach dem vom Nutzer gelieferten Plan. Aktuelle Basis ist `d435b4fcf62ce83409911f98c08f0a096e78c0dd`, veröffentlichte 3.2.2; Arbeitsbranch `samai/v3.3.0-geometry-nodes` mit bereits verifizierten ungecommitteten Geometrieänderungen. Die im Nutzerplan genannte 3.2.1-Basis ist historisch.

## 2. Ziel

Eine lokale, vom Kartengraphen getrennte Historie zeichnet Standardsitzungen, Suchschritte, Ergebnislage, tatsächliche Sichtbarkeit, abgewiesene und erfolgreiche Entscheidungen auf. Bestehende Suche, Tabs und Sortierung bleiben gleich. Ein zusammengesetzter Eintrag ist eine Entscheidung mit mehreren erzeugten Knotentypen.

## 3. Nicht-Ziele

Keine Nutzungserfassung für Konzeptbibliothek, Drag-and-drop, Probeknoten, Import, Laden, Duplizieren oder Undo/Redo. Keine Umsortierung, Suchbegriffserweiterung oder Netzübertragung. Keine neuen Knotentypen.

## 4. Istzustand

`KnotenAuswahlFenster.kt` besitzt Standard-/Konzeptmodus, Suchfeld, Tabs mit automatischem Ausweichen und gemischte einfache/zusammengesetzte Einträge. `AtlasZustand` filtert Vorlagen und fügt auch für andere Herkunft ein. `OperatorKnotenSuchindex` erzeugt vorkonfigurierte Varianten. Einfügefunktionen schließen selbst den Dialog. Statistik darf deshalb weder an `KnotenVorlage.erzeuge()` noch pauschal am Schließen ansetzen.

## 5. Semantik

Pro Öffnung des Standardmodus eine zufällige Sitzung. Suchtext nach 400 ms Ruhe protokollieren; Auswahl, Reset, Tab-/Moduswechsel, Schließen und Unterbrechung übernehmen den letzten Zustand unmittelbar. Bereits vorhandener Text wird als übernommen markiert. Gleiche aufeinanderfolgende Zustände werden zusammengeführt, spätere Rückkehr bleibt erkennbar. Originaltext und konservative Unicode-Normalisierung bleiben getrennt; mathematische Zeichen werden nicht gleichgesetzt. Leere Suche ist ein eigener Zustand.

Auswahlauftrag mit zufälliger ID und expliziter Herkunft Standarddialog; Erfolg erst nach tatsächlicher Einfügung. Pro Sitzung höchstens ein erfolgreicher Abschluss, wiederholte Aufträge zählen nicht doppelt. Undo löscht keine Entscheidung, Redo zählt keine neue.

## 6. Datenvertrag

Sitzung, Suchschritt, Auswahlergebnis und Statistik besitzen eine eigene Schemafassung. Eintragsidentität beruht auf Typ/Operator/ausdrücklicher Variante/Aktion/Karten-ID, nicht auf Anzeigename, Instanz-ID oder kompletter Parametermap. Ergebniszustand unterscheidet Texttreffer, Anschlussfilter, gewünschten/aktiven Tab, einfügbare/deaktivierte Einträge, Reihenfolge und tatsächliche Sichtbarkeit. Speicherung verwendet die Ergebnisdarstellung der sichtbaren Liste.

## 7. Architektur

App-eigene Datenmodelle, reiner Verwalter und serialisierter Hintergrunddienst. Ein geordneter IO-Worker lädt zuerst und verarbeitet danach Ereignisse; zusammengefasste vollständige Momentaufnahmen verhindern Rennen mit Laden und Löschen. Android-AtomicFile-Backend unter `noBackupFilesDir/MathematikAtlas/nutzung/knotenauswahl.json`; Synchronisation im Speicher/Dienst. Kein Dateizugriff auf dem UI-Thread. Unbekanntes neueres oder beschädigtes Schema wird geschützt, bis ausdrücklich gelöscht wird. Speicherfehler beeinträchtigen die Knoteneinfügung nicht.

## 8. Dateien

Neue `app/.../knotenauswahl/`-Modelle, Eintrags-/Ergebnisdarstellung, Verwalter und Dienst; `speicher/KnotenNutzungsSpeicher.kt`; Integration in Standarddialog, AtlasZustand, Lebenszyklus und Einstellungen. Additive Operatormetadaten im bestehenden Suchindex. Neue JVM-/Android-Tests.

## 9. Meilensteine

- [x] Aufrufpfade erneut geprüft.
- [x] Eintrags-/Datenvertrag und Verwalter: stabile Katalogschlüssel und deterministische Zustandsautomatentests erfolgreich.
- [x] Atomare Speicherung, Grenzen, Schema- und Löschschutz: Codec-/IO-Regressionen erfolgreich.
- [x] Gemeinsame Dialogdarstellung und Herkunft/Einfügeerfolg: gemeinsame Ergebnisdarstellung und expliziter Standard-Wrapper; JVM-Regressionen erfolgreich.
- [x] Einstellungen, Lebenszyklus und unabhängige Codeverifikation: keine offenen konkreten Findings.
- [!] Erster PR452-Emulatorlauf: vier von fünf Dialogtests bestanden, ein LazyColumn-Scrolltest korrigiert; erfolgreicher Nachlauf noch erforderlich. Lokal kein eingerichteter Emulator/AVD oder KVM-Gerät.

## 10. Umsetzung

Zuerst reine Modelle mit steuerbarer Zeit, danach Codec/IO-Dienst, dann Ergebnisdarstellung und Dialoganbindung. Vorlagen-Suchschnittstelle erhalten. Standardauswahl über expliziten Wrapper; andere Aufrufe bleiben ungezählt. Statistikabschluss vor erfolgreichem Dialogende; einfache Schließpfade dürfen erfolgreiche Auswahlen nicht zu Abbrüchen machen.

## 11. Prüfungen

Debounce/sofortige Auswahl, Umformulierungen und Rückkehr, Einzeichen-Symbole, übernommen/leere Suche, Ergebnis-/Tab-/Sichtbarkeitslage, zusammengesetzte Aktionen, Varianten, Ablehnung/Doppelzählung/Undo, abweichende Herkunft, JSON-Roundtrip, Prozessunterbrechung, Alter/Größenlimits, unbekanntes Schema, Schreibfehler und Löschen mit ausstehenden Ereignissen. UI-/Instrumentierung getrennt von JVM-/Buildnachweisen berichten.

## 12. Persistenz und Datenschutz

Installationsweiter lokaler No-Backup-Speicher; keine Karten-/Export-/Freigabeänderung. Startgrenzen: 90 Tage Detailhistorie, 10.000 Suchschritte und 2 MiB Datei. Zusätzliche begrenzte Sitzung-/Eintragsgrößen schützen lange Sitzungen. Reine Eintragszähler können länger bleiben, Tageszählungen werden begrenzt. Suchbeziehungen nur aus direkten Auswahlen innerhalb der behaltenen Historie ableiten, nie aus früheren Umformulierungen als vermeintlichen Synonymen.

Erfassung insgesamt sowie Suchtextspeicherung separat abschaltbar; vollständiges Löschen mit geordnetem Schreibschutz. Deaktivierte Suchtextspeicherung entfernt auch vorhandene Texte aus der lokalen Statistik. Keine Suchtexte in Fehlermeldungen oder Logs.

## 13. Risiken

Dialogeffekte können beim Schließen abbrechen: letzter Zustand wird synchron als Befehl übergeben, nicht allein durch debounce gespeichert. Auf Prozessende vor Datenträgerschreiben besteht keine Persistenzgarantie. Ereignisverarbeitung und Löschbefehl bleiben strikt geordnet; alte Sitzungstokens werden nach Löschung nicht wieder aktiviert.

## 14. Fortschritt

34 neue JVM-Tests bestanden; Gesamtlauf `test :app:assembleDebug :app:compileDebugAndroidTestKotlin`: 1.853 Tests ohne Fehler/Überspringungen und erfolgreicher APK-Build. Repository-, Releaseplan-, Versionsfolge- und Diffprüfungen erfolgreich. Fünf Android-Dialogtests ergänzt, kompiliert und als Test-APK gebaut; Lint und Test-APK-Build erfolgreich (`:app:lintDebug :app:assembleDebugAndroidTest`, 5m31s). Separate Standalone-Kernprüfung erfolgreich. Unabhängiger `node_verifier` hat nach Korrektur dreier Befunde die Codeabnahme erteilt: partielle Einstellungsbefehle verhindern gegenseitiges Überschreiben, NFC+trim erhält Π/π getrennt, flüchtige vollständige Zustände erhalten Umformulierungen ohne gespeicherte Suchtexte. Unveränderte Tippentwürfe schreiben die Datei nicht erneut.

## 15. Entscheidungen

2026-10-15: Vorhandene offene 3.3.0-Arbeit fortsetzen, keine zusätzliche Version oder Knotenfamilie. Statistik ausschließlich für Android-Standarddialog gemäß Nutzerplan. Wiederverwendung der Einfügefunktionen über Standardauswahl-Wrapper erfasst zusammengesetzte Aktionen ohne Zähler in allgemeinen Einfügepfaden.

## 16. Abweichungen

Aktuelle Repositorybasis statt veralteter 3.2.1. Konkrete Schutzgrenzen und IO-Backend werden mit ihren Tests dokumentiert. Bestehender Karten- und Sortiervertrag bleibt maßgeblich.

## 17. Ergebnis

Implementiertes Verhalten und Schutzgrenzen: [Dokumentation](../../../KNOTENAUSWAHL_HISTORIE.md), [ADR](../../decisions/2026-10-15-knotenauswahl-historie.md). Nach der Implementierung hat der Nutzer Commit, Push und Masterintegration beauftragt; dieser Schritt wird im [Integrationsplan](2026-10-15-v3-3-0-master-integration.md) verwaltet. Branch und Basis wie oben. Plan bleibt aktiv, bis die fünf Android-Dialogtests auf Gerät/Emulator erfolgreich bestanden sind. Diese offene Laufzeitabnahme wird nicht durch erfolgreiche JVM-Tests oder Kompilierung ersetzt.


### Emulator-Nachprüfung im Releaseauftrag

PR452 führte alle fünf neuen Dialogtests tatsächlich auf API35 aus: vier bestanden, keine übersprungenen Tests. Der schnelle Auswahltest scheiterte am Testzugriff auf eine durch Bildschirmtastatur außerhalb des komponierten LazyColumn-Bereichs liegende Zeile. Der Test scrollt nun zuerst über `performScrollToNode` auf dem Listencontainer; die Auswahl-/Typ-/Undo-Assertions bleiben unverändert. Ein zusätzlicher JVM-Test mit echtem Katalog und Operatorindex bestätigt Tab-Ausweichen und einfügbaren Eintrag. Vollständiger lokaler Nachlauf auf3.3.0 grün19s, 1.853 Tests ohne Fehler/Skips, Debug-/Test-APK erfolgreich. Erfolgreicher CI-Nachlauf vor Merge weiterhin erforderlich.


### Zweiter Emulatorbefund: Sammelgraph

Nach dem Scrollfix blieb der Auswahlabschluss im zweiten API35-Lauf offen. Die tatsächliche Sammelgraph-Verbindung ließ sich im JVM-Test als Typfehler reproduzieren (`typ.tupel<…>` gegenüber grobem `mathematik.tupel`). Die Statistik hat die nicht erfolgte Einfügung korrekt nicht gezählt. Minimaler Fix in `MathematikKartenTypSystem.kt` verbindet kanonische Tupel mit der registrierten groben Anschlussart; der neutrale Strukturvergleich bleibt unverändert. Zwei neue Mathematik-Brückentests und eine Sammelgraphregression sichern zulässige Verbindungen sowie unveränderte Längen-/Komponenten-/Zahlenausschlüsse. Unabhängige Codeabnahme ohne Findings. Gesamtnachlauf1.856Tests ohne Fehler/Skips und Debug-/Test-APK grün21s; erneuter Emulatornachweis weiterhin erforderlich.
