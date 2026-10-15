# Lokale Knotenauswahl-Historie

Die Android-Standardknotenliste erfasst lokale Sitzungen, Suchschritte und erfolgreiche Eintragsauswahlen. Suche, Tabwechsel, Anschlussfilter und alphabetische Reihenfolge verwenden weiterhin die bisherigen Regeln. Die Konzeptbibliothek und andere Einfügewege erzeugen keine Standarddialog-Nutzungen.

Unter **Einstellungen → Anwendung → Knotenauswahl-Historie** lassen sich die Erfassung und die Suchtextspeicherung unabhängig abschalten sowie alle Statistikdaten löschen. Das Abschalten der Suchtextspeicherung entfernt auch zuvor gespeicherte Texte und daraus ableitbare direkte Suchbeziehungen. Suchschritte und reine Eintragszähler bleiben erhalten; bei abgeschalteter Erfassung werden keine weiteren Ereignisse aufgenommen.

## Zählregeln

Eine tatsächlich übernommene Standarddialogauswahl zählt einmal. „Spaltenvektor erzeugen“ zählt daher als eine Entscheidung mit zwei erzeugten Knoten. Die erzeugten Typen und stabilen Varianten werden zusätzlich protokolliert. Abgewiesene Aufträge zählen nicht als Nutzung, wiederholte Auftrags-IDs nicht doppelt. Undo entfernt keine historische Entscheidung, Redo erzeugt keine neue. Probeknoten, Auswertung, Laden, Import, Kopieren und Drag-and-drop werden nicht gezählt.

Jede Öffnung des Standardmodus beginnt eine Sitzung. Nach 400 ms unverändertem Text wird ein Suchschritt aufgenommen; die sichtbare Suche reagiert unmittelbar. Auswahl, Zurücksetzen, Tab-/Moduswechsel, Schließen und Hintergrundwechsel sichern den aktuellen Zustand ohne diese Wartezeit. Übernommener Text wird von neu eingegebenem Text unterschieden. Wiederholte Darstellung erzeugt keine weiteren Schritte; A → B → A bleibt als Verlauf erhalten, auch ohne gespeicherte Suchtexte.

Erfasst werden Texttreffer vor und nach dem Anschlussfilter, Anschlussrichtung/-typ, gewünschter und tatsächlich angezeigter Tab, geordnete Einträge einschließlich deaktivierter Sammelaktionen, ausgewählter Rang sowie eine begrenzte Menge tatsächlich sichtbarer Eintrags-IDs. Dialog und Statistik verwenden dieselbe Ergebnisdarstellung. Überschriften und Scrollpositionen sind keine Suchereignisse.

## Technischer Vertrag

- Eigener JSON-Codec mit Schema 1, außerhalb der Kartendaten: `noBackupFilesDir/MathematikAtlas/nutzung/knotenauswahl.json`. Keine Aufnahme in Kartenexporte, Freigabepakete oder Android Auto Backup.
- Ein installationsweiter IO-Dienst lädt vor dem ersten Ereignis, verarbeitet Befehle geordnet und schreibt geänderte Momentaufnahmen atomar. Activity-Neuerstellungen erzeugen keinen zweiten Dateischreiber. Schreibfehler lassen den Editor und bereits gezählte Ereignisse im Speicher erhalten; spätere Schreibaufträge versuchen erneut zu sichern.
- Unbekannte Schemata, beschädigte und übergroße Dateien werden geschützt. Erst ausdrückliches Löschen hebt diesen Schutz auf. Alte Ereignisse und schon laufende Schreibaufträge können gelöschte Sitzungen nicht wiederbeleben.
- Detailhistorie: höchstens 90 Tage, 10.000 Schritte, 1.000 Sitzungen, 250 Schritte und 64 Auswahlversuche je Sitzung. Textgrenze: 2.048 Zeichen mit Kürzungsmarkierung; höchstens 512 angebotene und 100 sichtbare IDs je Zustand. Datei höchstens 2 MiB, reine Eintragszähler höchstens 4.096 Schlüssel; Tageszählungen höchstens 90 Tageswerte je Eintrag. Bei Platzmangel werden alte Details zuerst entfernt und verwaiste Schrittverweise gelöst. Zuletzt können alte Eintragszähler entfallen.
- Stabile Eintragsschlüssel verwenden Knotentyp, explizite Variante, vorhandene Operator-ID, Sammelaktions-ID oder Karten-ID. Anzeigenamen, aktuelle Zahlenwerte und Knoteninstanz-IDs sind keine Identität. Kartenversionen sind Metadaten.
- NFC und Rand-Leerzeichenbereinigung normalisieren Suchtexte konservativ. Mathematisch verschiedene Zeichen wie Π/π oder ×/* bleiben getrennt. Nur die direkte Suche einer erfolgreichen Auswahl bildet eine Such-/Auswahlbeziehung; frühere Suchschritte werden nicht als Synonyme behandelt.
- Unterbrechung beendet die Sitzung als unterbrochen. Eine erneute Anzeige übernimmt vorhandenen Suchtext in eine neue Sitzung. Beim Laden werden nicht abgeschlossene Sitzungen ebenfalls als unterbrochen markiert. Ein abruptes Prozessende kann noch nicht geschriebene Änderungen verlieren.

## Nachweise

34 neue JVM-Tests prüfen Verwalter, tatsächliche Katalogidentitäten, Ergebnisdarstellung, Codec, Grenzen, Schreibfehler, konkurrierende Einstellungsänderungen und Löschreihenfolge. Der vollständige JVM-/Desktoplauf umfasst 1.856 Tests ohne Fehler oder Überspringen; Debug-APK, Android-Test-APK und Lint sind erfolgreich. Repository-, Releaseplan-, Versionsfolge-, Diff- und separate Kernprüfung sind ebenfalls erfolgreich. Fünf ergänzte Android-Dialogtests decken schnelle Sammelauswahl, Undo/Redo, Zurücksetzen/Abbrechen, Herkunftsabgrenzung, Moduswechsel sowie Tab-/Unterbrechungsverhalten ab. Erster API35-CI-Lauf in [PR452](https://github.com/TeutonStudio/MathematikAtlas/pull/452): vier von fünf bestanden, einer scheiterte beim Scrollen zu einer noch nicht komponierten Listenzeile. Dieser Test scrollt jetzt über den LazyColumn-Container; sämtliche fachlichen Assertions bleiben bestehen. Die zusätzliche JVM-Katalogprüfung bestätigt den angebotenen Sammeleintrag. Der erfolgreiche Emulatornachlauf bleibt vor der Integration erforderlich.

Releaseumfang3.3.0: Branch `samai/v3.3.0-geometry-nodes`, Basis `d435b4fcf62ce83409911f98c08f0a096e78c0dd`. Veröffentlichung und Remote-CI werden im [Integrationsplan](codex/plans/active/2026-10-15-v3-3-0-master-integration.md) getrennt von diesen lokalen Funktionsnachweisen dokumentiert. [ExecPlan](codex/plans/active/2026-10-15-knotenauswahl-nutzungsstatistik.md).


Die zweite Emulatorprüfung deckte zusätzlich einen bestehenden Tupelanschlussfehler auf: Die echte Sammelaktion konnte ein strukturelles Tupel nicht mit dem groben Tupeladapter verbinden und blieb ohne Einfügeerfolg offen. Eine gezielte Mathematik-Typbrücke sowie zwei Typgrenzen- und eine Sammelgraphregression beheben und sichern diesen Pfad. Strukturierte Längen-/Komponentenwidersprüche bleiben abgewiesen. Lokaler Gesamtnachlauf1.856Tests grün; erfolgreicher erneuter CI-Emulatorlauf vor Veröffentlichung erforderlich.
