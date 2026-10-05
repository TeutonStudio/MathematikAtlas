# Android-Bestand systematisch absichern

Status: abgeschlossen am 2026-09-30; Befunde und Folgepakete sind verifiziert beziehungsweise ausdrücklich als Prüfgrenze markiert.

## 1. Ziel und Nutzerwirkung

Die Android-App wurde als Werkzeug für anspruchsvolle eigene Knotenkarten auf Datenerhalt, Absturzpfade, Editorverträge, mathematische Anbindung und blockierende Arbeit im UI-Thread geprüft. Das Ergebnis ist eine priorisierte Reparaturliste mit umsetzungsreifen Abnahmekriterien.

## 2. Nicht-Ziele

- keine neuen Knotentypen oder mathematischen Funktionen,
- keine Änderung öffentlicher APIs, des Kartenformats oder der Releaseversion,
- keine allgemeine Architektur- oder Desktopbereinigung,
- keine Behauptung einer Geräteprüfung ohne tatsächlich gestartete App.

## 3. Untersuchter Istzustand

- Basis war Commit `af083c49` auf `master`, vor Auditbeginn identisch mit `origin/master`.
- Bereits vorhandene lokale Änderungen aktualisieren AGP 9.3.1 auf 9.4.0, Gradle 9.5 auf 9.6 und Android-Studio-Projektdateien. Sie blieben erhalten.
- `app` veröffentlicht Version 2.33.0; `release/roadmap.toml` führt dieselbe Version als aktuell.
- Karten werden als Format 8 geschrieben. Die bisherige `CURRENT_STATE.md` nannte v2.1.9, Gradle 8.13 und Format 2 und wurde deshalb ersetzt.
- Unter den untersuchten Modulen liegen 244 Kotlin-Testdateien. Android-Instrumentierungstestquellen wurden nicht gefunden.
- Mehrere Dateien unter `plans/active` beschreiben bereits veröffentlichte oder laut eigenem Status abgeschlossene Arbeiten. Die Ablage ist keine verlässliche aktuelle Aufgabenliste.

## 4. Fachliche und mathematische Semantik

Der Audit hat keine neue mathematische Semantik eingeführt. Der vollständige JVM-Testlauf prüfte unter anderem exakte rationale Rechenoperationen, Mengenfilter, Aussagenlogik, Methoden- und Analysisverträge sowie Vektor- und Matrixoperationen. Alle 14 ausgelieferten Standardkarten wurden durch den echten Gesamtauswerter ohne gemeldete Auswertungsfehler verarbeitet.

Zusätzlich sichert nun ein Querschnittstest ab, dass jede im kanonischen Erstellen-Katalog sichtbare mathematische Knotenart entweder im `MathematikAuswerterRegister` registriert oder bewusst direkt im `KartenAuswerter` behandelt wird. Der Test ist erfolgreich. Das ist ein Anbindungsnachweis, kein Beweis vollständiger mathematischer Korrektheit aller möglichen Karten.

## 5. Daten-, Knoten-, Anschluss- und Verbindungsvertrag

- Persistierte Karten-, Knoten-, Anschluss- und Verbindungsmodelle wurden nicht geändert.
- Vorhandene Tests bestätigen stabile IDs, typisierte Verbindungskompatibilität, dynamische Anschlüsse, Kopieren, Löschen sowie atomare Undo-/Redo-Schritte.
- Die gefundenen Datenerhaltungsfehler liegen in Koordination, Import und Dateiauswahl, nicht im JSON-Datenmodell. Eine Formatmigration ist für die ersten Reparaturpakete nicht erforderlich.

## 6. Bestätigte Befunde

### [Blockierend] Kartenwechsel kann den noch nicht gespeicherten Stand verwerfen

- Ort: `MathematikAtlasApp.kt:111`, `AtlasZustand.kt:77`, `AtlasZustand.kt:89`, `AtlasZustand.kt:100`
- Beobachtung: Der Autosave reagiert auf Kartenänderungen, wartet 650 ms und speichert danach. `öffne` und `geheZuBrotkrume` ersetzen den Editorzustand sofort und setzen `letzterGespeicherterStand` auf das neue Ziel. Ein vorheriger Flush fehlt.
- Auslöser: Karte ändern und innerhalb von 650 ms eine andere Karte oder Brotkrume öffnen. Derselbe Verlust ist beim Activity-/Prozessende vor Ablauf der Verzögerung möglich.
- Zusätzlicher Pfad: `speichereAktuell` kehrt bei offener Verbindungsinteraktion still zurück. Wird eine ältere Änderung dadurch übersprungen und ändert sich danach die Karte nicht erneut, wird kein neuer Autosave geplant.
- Nutzerwirkung: Die letzte Bearbeitung kann ohne Warnung verloren gehen.
- Korrekturbedingung: Jeder Kartenwechsel und `ON_STOP` sichern den letzten stabilen Kartenstand; ein offener Verbindungsmodus wird definiert beendet; Speicherfehler verhindern den Wechsel oder werden sichtbar gemeldet.

### [Hoch] `.matlas` kann exportiert, aber nicht importiert werden

- Ort: `MathematikAtlasApp.kt:63`, `MathematikAtlasApp.kt:76`, `MatlasKartenContainer.kt:24`, `KartenSpeicher.kt:67`
- Beobachtung: Export erzeugt einen binären ZIP-Container mit Manifest, Prüfsumme und `karte.json`. Der Importdialog bietet nur JSON/Text an, liest den Inhalt als Text und übergibt ihn an JSON-/Freigabepaket-Parser. Ein `.matlas`-Reader existiert nicht.
- Nutzerwirkung: Das als reguläre Exportoption angebotene Format besitzt keinen Rückweg in die App.
- Korrekturbedingung: Exportierte `.matlas`-Bytes werden wieder eingelesen; Formatkennung, Version, erwartete Einträge, Größen und SHA-256 werden validiert; erst danach läuft die bestehende Kartenmigration.

### [Hoch] Ungültige Importe können den UI-Callback abbrechen

- Ort: `MathematikAtlasApp.kt:76`, `AtlasZustand.kt:189`, `KartenSpeicher.kt:67`
- Beobachtung: Öffnen, Lesen, Dekodieren, Migration und Speichern sind nicht von einer Fehlergrenze umgeben. Anders als Exportfehler erhalten Importfehler keinen Dialog.
- Auslöser: ungültiges JSON, nicht unterstützte Datei, Lesefehler oder ungültiges Freigabepaket.
- Nutzerwirkung: Der Import endet mit einer ungefangenen Ausnahme statt einer verständlichen Meldung; je nach Android-Callback kann die App beendet werden.
- Korrekturbedingung: Jeder Importfehler wird in ein sichtbares Ergebnis überführt; die bestehende Karte und Bibliothek bleiben unverändert.

### [Hoch] Beschädigte neueste Version verdeckt valide ältere Daten und Sicherungen

- Ort: `KartenSpeicher.kt:29`, `KartenSpeicher.kt:45`, `KartenSpeicher.kt:230`, `KartenSpeicher.kt:271`
- Beobachtung: `liste` und `ladeAktuell` wählen zuerst die höchste Versionsdatei und versuchen nur diese zu dekodieren. Schlägt das fehl, verschwindet die Karte beziehungsweise liefert `null`. Ältere Versionen werden nicht geprüft. Sicherungen werden geschrieben, aber nur beim endgültigen Löschen wieder betrachtet.
- Nutzerwirkung: Eine einzelne beschädigte Datei macht eine Karte unsichtbar, obwohl eine valide ältere Version oder Sicherung vorhanden sein kann.
- Korrekturbedingung: Ladevorgänge wählen die neueste valide Version, melden die beschädigte Datei und bieten eine kontrollierte Wiederherstellung an; Dateien werden nicht automatisch gelöscht.

### [Mittel] Umfangreiche Arbeit läuft synchron auf dem UI-Thread

- Ort: `MainActivity.kt:25`, `MathematikAtlasApp.kt:76`, `MathematikAtlasApp.kt:111`, `AtlasZustand.kt:348`, `KartenSpeicher.kt:29`, `KartenSpeicher.kt:51`
- Beobachtung: `AtlasZustand` wird während Composition aufgebaut, lädt die gesamte Bibliothek und wertet die Startkarte aus. Jede Kartenänderung startet die Auswertung vor dem Debounce. Import liest und dekodiert im Activity-Result-Callback; Autosave schreibt und lädt anschließend erneut alle Karten.
- Nutzerwirkung: Große Karten oder Bibliotheken können Start, Bearbeitung und Import sichtbar blockieren. Eine gerätebezogene Zeitmessung war nicht möglich.
- Korrekturbedingung: Datei-I/O und reine Auswertung laufen außerhalb des Main-Dispatchers; Ergebnisse werden nur auf den weiterhin passenden Kartenstand angewendet; Speicheraufträge bleiben serialisiert.

### [Mittel] Activity-Neuerstellung verliert den Arbeitskontext

- Ort: `MainActivity.kt:25`, `AtlasZustand.kt:42`
- Beobachtung: `AtlasZustand` wird nur mit `remember` gehalten. Nach Activity-Neuerstellung wird die alphabetisch erste gespeicherte Karte geöffnet; aktive Karte, Brotkrumen und kurzlebige Auswahl werden nicht wiederhergestellt.
- Nutzerwirkung: Drehung, Konfigurationswechsel oder Prozesswiederherstellung setzen den Nutzer in einen anderen Arbeitskontext zurück. Zusammen mit dem verzögerten Autosave verstärkt das den Blocker oben.
- Korrekturbedingung: Mindestens der aktive `KartenVerweis` und die Brotkrumen werden über speicherbaren Zustand restauriert; ungültige Verweise fallen nachvollziehbar auf eine vorhandene Karte zurück.

## 7. Prüfgrenzen und nicht bestätigte Vermutungen

- Der vorhandene AVD `Medium_Tablet` wurde headless gestartet, endete aber vor abgeschlossenem Boot mit Exitcode 139. Start, Touch-Gesten, Hintergrundwechsel und Dialoge sind deshalb nicht praktisch ausgeführt worden.
- Es existieren keine Android-Instrumentierungstestquellen. Touch- und Lebenszyklusverhalten ist nur aus Code und JVM-Verträgen abgeleitet.
- Die UI-Thread-Pfade sind bestätigt; eine konkrete ANR-Schwelle oder Bildrate ist ohne Gerätetest nicht behauptet.
- Der grüne Testlauf beweist die geprüften Fälle, nicht die Vollständigkeit der Mathematik.

## 8. Architekturentscheidungen für Folgearbeiten

- Datenerhalt wird vor Performance behandelt.
- Der Karteneditor bleibt fachneutral; Speicher-, Lebenszyklus- und Importkoordination bleiben in `app`.
- `.matlas`-Dekodierung gehört neben den Writer in `MathematikKnoten`; Androids Dokumentauswahl und Fehlermeldung bleiben in `app`.
- Kartenformat 8 bleibt unverändert. Wiederherstellungsmetadaten werden nicht in `KartenDaten` eingebettet.
- Datei-I/O erhält eine interne, serialisierte Koordination; Auswertung verwendet unveränderliche Kartensnapshots.

## 9. Reparaturpaket A – Speichern und Arbeitskontext (Priorität 0)

1. In `AtlasZustand` einen einzigen internen Übergangspfad einführen, den Öffnen, Brotkrumenwechsel, neue Karte, bearbeitbare Kopie und erfolgreicher Import verwenden.
2. Vor dem Übergang eine offene Verbindungsinteraktion kontrolliert abbrechen, den bereinigten aktuellen Stand speichern und bei Fehlern das Ziel nicht öffnen.
3. Den Autosave so auslösen, dass ein wegen Verbindungsmodus übersprungener Auftrag nach Ende des Modus erneut geplant wird; kein stiller endgültiger Rücksprung.
4. Bei `ON_STOP` einen Flush des letzten stabilen Snapshots anstoßen. Der Lifecycle-Pfad verwendet dieselbe Speicherlogik wie der manuelle Befehl.
5. Aktiven `KartenVerweis` und Brotkrumen mit speicherbarem Activity-Zustand restaurieren; nicht mehr vorhandene Ziele fallen auf die neueste valide Karte zurück.
6. Regressionstests: Änderung und sofortiger Kartenwechsel; Änderung und `ON_STOP`; Änderung, offene Verbindung, Abbruch; Speicherfehler blockiert Wechsel; Wiederherstellung nach Activity-Neuerstellung.

Abnahme: Kein getesteter Navigations- oder Lifecycle-Pfad kann eine bestätigte Kartenänderung ohne Meldung verwerfen.

## 10. Reparaturpaket B – Symmetrischer, fehlertoleranter Import (Priorität 1)

1. `MatlasKartenContainer` um einen Byte-Reader ergänzen. Er akzeptiert ausschließlich die beiden erwarteten Einträge, begrenzt Anzahl und dekomprimierte Größe, validiert Manifestkennung/-version und SHA-256 und liefert die enthaltene Karten-JSON.
2. Androids Dokumentdialog um den `.matlas`-MIME-Typ erweitern und den Inhalt als Bytes auf einem I/O-Dispatcher lesen.
3. Anhand von Signatur und Inhalt zwischen `.matlas`, JSON und Freigabepaket unterscheiden; Dateiendungen allein entscheiden nicht.
4. Parser- und Speicherfehler als typisiertes internes Importergebnis an die UI geben. Der Fehlerdialog nennt Format und Ursache, ohne Roh-Stacktrace.
5. Regressionstests: JSON-Roundtrip, `.matlas`-Roundtrip, falsche Prüfsumme, fehlender/zusätzlicher Eintrag, unbekannte Version, übergroßer Eintrag, ungültiges JSON sowie unveränderte Bibliothek nach jedem Fehler.

Abnahme: Jede angebotene Exportart kann wieder importiert werden; beschädigte oder fremde Dateien führen zu einer sichtbaren Meldung und keiner Teilmutation.

## 11. Reparaturpaket C – Wiederherstellung beschädigter Karten (Priorität 1)

1. Versionen beim Listen und `ladeAktuell` absteigend dekodieren und die neueste valide Karte zurückgeben.
2. Dekodierfehler mit Pfad und Ursache intern erfassen und der Kartenverwaltung als Wiederherstellungsstatus bereitstellen.
3. Für eine beschädigte explizit referenzierte Version zuerst die passende Sicherung prüfen; nie unbemerkt eine andere Semantik unter demselben festen `KartenVerweis` einsetzen.
4. Wiederherstellung erzeugt eine neue Version aus der gewählten validen Quelle. Beschädigte Originale bleiben bis zur ausdrücklichen endgültigen Löschung erhalten.
5. Den Schreibpfad mit atomarem Move im selben Verzeichnis absichern; der Fallback darf das Ziel nicht über eine teilweise Kopie sichtbar machen.
6. Regressionstests mit temporärem Basisverzeichnis: beschädigte neueste Version, valide ältere Version, passende Sicherung, alle Versionen beschädigt, unterbrochene temporäre Datei und feste Gruppenknotenreferenz.

Abnahme: Eine beschädigte Datei lässt valide ältere Daten nicht verschwinden und wird sichtbar diagnostiziert.

## 12. Reparaturpaket D – UI-Thread und große Karten (Priorität 2)

1. Dateilesen/-schreiben auf `Dispatchers.IO`, reine Kartenauswertung auf `Dispatchers.Default` verlagern.
2. Speichervorgänge pro Karte serialisieren; spätere Snapshots dürfen nicht von früher abgeschlossenen Aufträgen überschrieben werden.
3. Auswertungsergebnisse mit Karten-ID und Snapshot-Signatur verknüpfen und veraltete Ergebnisse verwerfen.
4. `speichereAktuell` soll nach erfolgreichem Schreiben nicht die komplette Bibliothek synchron neu dekodieren; stattdessen den gespeicherten Eintrag gezielt im Zustand ersetzen.
5. Reproduzierbare Messfälle für Start mit großer Bibliothek, große Karte, schnelle Änderungsfolge und großen Import ergänzen. Schwellenwerte werden erst anhand eines realen Referenzgeräts festgelegt.

Abnahme: Kein Datei-I/O läuft auf dem Main-Dispatcher; veraltete Auswertungs- oder Speicherergebnisse können den aktuellen Zustand nicht ersetzen.

## 13. Reparaturpaket E – Android-Laufzeitnachweise (Priorität 2)

1. Instrumentierungstests für Start, Karte bearbeiten und wechseln, Hintergrund/Vordergrund, Activity-Neuerstellung, ungültigen Import und sichtbare Fehlerdialoge ergänzen.
2. Compose-Tests für Knotenwahl, Verbindungsgeste, Inspectoränderung sowie Undo/Redo nur an stabilen Semantik- oder Test-Tags ausrichten.
3. Auf mindestens einem Tablet- und einem Telefonprofil manuell prüfen: Touch-Ziele, Zoomen/Verschieben, große Karte, System-Back, Dateiauswahl und Prozesswiederherstellung.
4. Geräteprüfung getrennt von JVM-Test und APK-Build dokumentieren.

Abnahme: Die kritischen Datenerhaltungsabläufe laufen auf einem Android-Ziel reproduzierbar grün; verbleibende manuelle Prüfungen sind konkret benannt.

## 14. Betroffene Dateien und Symbole

- Speichern und Navigation: `MainActivity`, `MathematikAtlasApp`, `AtlasZustand`.
- Import und Wiederherstellung: `MatlasKartenContainer`, `KartenSpeicher` sowie deren Tests.
- Querschnittsvertrag: `KanonischerMathematikKnotenKatalogTest`.
- Dokumentation: `CURRENT_STATE.md` und dieser abgeschlossene ExecPlan.

## 15. Ausgeführte Prüfungen

- `python3 scripts/pruefe_repository.py` – erfolgreich.
- `python3 scripts/pruefe_releaseplan.py` – erfolgreich.
- `python3 scripts/pruefe_versionsfolge.py` – erfolgreich.
- `python3 scripts/pruefe_standardkarten.py` – erfolgreich, 14 Karten.
- `python3 scripts/pruefe_methodenmodell.py` – erfolgreich.
- `python3 scripts/pruefe_kern.py` – Exitcode 2, weil das Skript `kotlinc` und `java` in seiner Umgebung nicht gemeinsam fand.
- `./gradlew --stacktrace test :app:assembleDebug` – erfolgreich; 1.149 Tests ohne Fehler, Debug-APK erzeugt.
- gezielter `KanonischerMathematikKnotenKatalogTest` – nach Ergänzung des Querschnittstests erfolgreich.
- AVD `Medium_Tablet` – Bootversuch durchgeführt, Emulator vor Bootabschluss mit Exitcode 139 beendet.

## 16. Fortschritt und Entscheidungsprotokoll

- [x] Git-, Versions-, Dokumentations- und Planstand abgeglichen.
- [x] Speicher-, Import-, Wiederherstellungs- und Lebenszykluspfade geprüft.
- [x] Editorverträge und bestehende Tests geprüft.
- [x] Mathematikfamilien und Standardkarten über den vollständigen Testlauf geprüft.
- [x] Katalog-/Auswerter-Querschnittstest ergänzt und ausgeführt.
- [x] UI-Thread-Pfade untersucht.
- [!] Android-Laufzeittest: vorhandener Emulator stürzt vor Bootabschluss ab.
- [x] Befunde priorisiert und Folgepakete entscheidungsvollständig beschrieben.

Entscheidung 2026-09-30: Die erste Produktionsänderung soll Reparaturpaket A sein, weil laut Reviewregeln möglicher Datenverlust vor Importkomfort und Performance liegt. Paket B und C dürfen anschließend unabhängig geplant werden, solange beide denselben sichtbaren Fehlervertrag verwenden.

## 17. Abweichungen und Ergebnis

Die geplante Emulatorprüfung konnte trotz vorhandenem AVD nicht abgeschlossen werden. Stattdessen ist die Grenze mit Exitcode und fehlenden Instrumentierungstests dokumentiert. Zusätzlich zum ursprünglich geforderten Bericht wurde ein dauerhafter Katalog-/Auswerter-Regressionstest ergänzt.

Der Audit verändert keine Produktionsdatei, öffentliche API, Persistenzversion oder Releaseversion. Der nächste sichere Umsetzungsschritt ist Reparaturpaket A; danach folgen symmetrischer Import und Wiederherstellung beschädigter Karten.
