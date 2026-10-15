# v3.3.0: Geprüften Arbeitsstand nach master integrieren

## Status und Auftrag

2026-10-15: Der Nutzer hat einen neuen Master-Commit und Push beauftragt. Releasevorbereitung und Veröffentlichung werden nach `release-verwalten`, `RELEASE_WORKFLOW.md` und `GIT_IDENTITY.md` durch den `master_verwalter` ausgeführt. Der Plan dokumentiert die vor dem Merge prüfbare Ausgangslage; Remote-Ergebnis und CI-Links werden nach dem Merge separat berichtet.

## Ziel, Umfang und Nicht-Ziele

Den vollständigen autorisierten Arbeitsstand als genau einen finalen Versionscommit `v3.3.0` per geprüftem Squash-PR in `master` integrieren. Enthalten sind 15 Geometry-Nodes-Typen mit Meshes, Punktwolken, Instanzen und Feldern, die geprüften Geometrieansichten sowie die lokale Nutzungs-/Suchhistorie der Android-Standardliste. Keine zusätzlichen Produktfunktionen, keine neue Abhängigkeit, kein Kartenformatwechsel und keine vollständige Blender-Nachbildung. Eine GitHub-Release-Seite oder ein zusätzlicher APK-Downloadrelease ist nicht Teil dieses Auftrags.

## Untersuchte Grundlage und Versionsvertrag

Remote-master und lokale Basis: `d435b4fcf62ce83409911f98c08f0a096e78c0dd`, finaler Commit `v3.2.2`. Arbeitsbranch `samai/v3.3.0-geometry-nodes`. Keine konkurrierende offene 3.3.0-Integration beim Audit. Der vollständige Diff enthält genau die 15 reservierten Typen unter `mathematik.geometryNodes.`. Deshalb Achse y: 3.3.0, Android-Code3003000. Android-/Desktop-Metadaten, Artefaktname und Roadmap werden gemeinsam aktualisiert. `released` im PR-Zielbaum beschreibt den vorgesehenen Releasezustand; Veröffentlichung erst nach dem finalen Commit auf Remote-master und grünen Releaseprüfungen.

## Architektur, Persistenz und Dateien

Die fachlichen Verträge bleiben in den bereits geprüften [Geometry-Nodes-](../completed/2026-10-15-geometry-nodes.md), [Ansichts-](../completed/2026-10-15-desmos-geogebra-geometrie.md) und [Nutzungshistorie-Plänen](2026-10-15-knotenauswahl-nutzungsstatistik.md) beschrieben. Geometrie bleibt numerisch und endlich; Statistiken bleiben im Android-No-Backup-Speicher außerhalb von Karten und Exporten. Releasearbeit ändert nur Versions-/CI-Metadaten und Dokumentation zusätzlich zum bereits abgenommenen Funktionsdiff.

## Umsetzung und Meilensteine

- [x] Autorisierung, unveränderte Masterbasis und vollständigen Umfang geprüft.
- [x] Klassifikation y/3.3.0 und angekündigte Typfamilie bestätigt.
- [x] Android/Desktop/Roadmap auf die Zielversion gebracht; Releaseguard inkl.14Regressionen grün. Finaler lokaler3.3.0-Lauf `test :app:assembleDebug :app:assembleDebugAndroidTest` erfolgreich39s, 1.852 Tests ohne Fehler/Überspringungen.
- [x] 55 auftragsbezogene Dateien als SamAI-Commit `cdaf3dc807cf9fe698c8d09d5e989959a546dfce` committed, Identität geprüft, Arbeitsbranch gepusht; generierte `.kotlin/`-Ausgaben ausgeschlossen.
- [-] [PR452](https://github.com/TeutonStudio/MathematikAtlas/pull/452) eröffnet. Erster Lauf: fünf von sechs Checks grün; Android auf API35: Editor2/2, App9/10, darunter neue Dialogtests4/5, kein Skip. Fehlgeschlagener Test fand eine noch nicht komponierte LazyColumn-Zeile mit `performScrollTo` nicht. Test verwendet nun `performScrollToNode` auf dem Listencontainer; fachliche Assertions unverändert. Zusätzliche JVM-Katalogregression bestätigt korrektes Ergebnisangebot. Lokaler Nachlauf `test :app:assembleDebug :app:assembleDebugAndroidTest` erfolgreich19s: 1.853 Tests, keine Fehler/Skips. Wiederholte CI vor Merge erforderlich.
- [ ] Squash mit Titel `v3.3.0`; Remote-master und Master-Push-CI prüfen.

## Prüfungen und Codeabnahme

Der vor der Release-Metadatenänderung vollständig geprüfte Stand umfasst 1.852 JVM-/Desktoptests ohne Fehler oder Überspringungen, Android-Debug-APK und Test-APK, Lint (neun vorhandene Warnungen außerhalb des neuen Codes), separate Kernprüfung sowie Repository-, Releaseplan-, Versionsfolge- und Diffprüfung. `node_verifier` hat Geometry Nodes, Ansichtsoptimierungen und Nutzungshistorie unabhängig codegeprüft; keine offenen konkreten Findings. Die Metadata-/Guardprüfung wird gegen den endgültigen Zielbaum wiederholt. CI ersetzt diese lokale Prüfung nicht; ihre Ergebnisse werden gegen konkrete Head-SHAs kontrolliert.

## Laufzeitgrenzen, Risiken und Rückfall

Desktop-Compose-Interaktionen der Geometrievorschau wurden ausgeführt. Die fünf neuen Android-Dialogtests wurden im ersten PR-CI-Lauf auf dem API35-Emulator ausgeführt: vier bestanden, ein Test scheiterte am Scrollen zu einer noch nicht komponierten Listenzeile. Der Test ist korrigiert, aber sein erfolgreicher Emulatornachlauf steht vor dem Merge noch aus. Lokal gibt es kein eingerichtetes AVD/KVM oder nutzbares Gerät. Der Nutzungshistorie-Plan bleibt bis zum erfolgreichen Nachlauf aktiv.

Kein direkter Commit oder Push auf master, kein Force-Push, keine Historienumschreibung und keine Umgehung fehlender/roter Pflichtchecks. Bei veränderter Remote-Basis vor Merge erneut auditieren. Bei einer gescheiterten Prüfung bleibt der veröffentlichte Vorgänger unverändert. Bereits vorhandene lokale Änderungen außerhalb des Auftrags werden nicht überschrieben.

## Entscheidungen und Ergebnis

2026-10-15: Gesamten geprüften Funktionsstand in der bereits reservierten y-Version3.3.0 integrieren, statt die Nutzungsstatistik fälschlich als getrennte x-Version auf einer unveröffentlichten Basis zu veröffentlichen. Benutzerauftrag autorisiert Commit, Push und Masterintegration ohne zusätzliche Bestätigung. Finale Commit-SHA, PR und Master-CI sind vor dem Merge noch unbekannt und werden nicht vorweggenommen.
