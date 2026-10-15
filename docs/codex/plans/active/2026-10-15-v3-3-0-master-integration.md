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
- [ ] Explizite auftragsbezogene Dateien stagen, SamAI-Commit über `scripts/samai-git.sh`, Identität prüfen und Arbeitsbranch pushen.
- [ ] PR nach master, Pflicht-CI vollständig abwarten; bei Fehlern Ursache beheben und Prüfungen wiederholen.
- [ ] Squash mit Titel `v3.3.0`; Remote-master und Master-Push-CI prüfen.

## Prüfungen und Codeabnahme

Der vor der Release-Metadatenänderung vollständig geprüfte Stand umfasst 1.852 JVM-/Desktoptests ohne Fehler oder Überspringungen, Android-Debug-APK und Test-APK, Lint (neun vorhandene Warnungen außerhalb des neuen Codes), separate Kernprüfung sowie Repository-, Releaseplan-, Versionsfolge- und Diffprüfung. `node_verifier` hat Geometry Nodes, Ansichtsoptimierungen und Nutzungshistorie unabhängig codegeprüft; keine offenen konkreten Findings. Die Metadata-/Guardprüfung wird gegen den endgültigen Zielbaum wiederholt. CI ersetzt diese lokale Prüfung nicht; ihre Ergebnisse werden gegen konkrete Head-SHAs kontrolliert.

## Laufzeitgrenzen, Risiken und Rückfall

Desktop-Compose-Interaktionen der Geometrievorschau wurden ausgeführt. Die fünf neuen Android-Dialogtests sind kompiliert und als Test-APK gebaut, **noch nicht auf Android ausgeführt**: lokal kein eingerichtetes AVD/KVM oder nutzbares Gerät. Das ist eine ausdrücklich dokumentierte Laufzeitgrenze, kein behaupteter Laufzeitnachweis. Der Nutzungshistorie-Plan bleibt für diese Abnahme aktiv.

Kein direkter Commit oder Push auf master, kein Force-Push, keine Historienumschreibung und keine Umgehung fehlender/roter Pflichtchecks. Bei veränderter Remote-Basis vor Merge erneut auditieren. Bei einer gescheiterten Prüfung bleibt der veröffentlichte Vorgänger unverändert. Bereits vorhandene lokale Änderungen außerhalb des Auftrags werden nicht überschrieben.

## Entscheidungen und Ergebnis

2026-10-15: Gesamten geprüften Funktionsstand in der bereits reservierten y-Version3.3.0 integrieren, statt die Nutzungsstatistik fälschlich als getrennte x-Version auf einer unveröffentlichten Basis zu veröffentlichen. Benutzerauftrag autorisiert Commit, Push und Masterintegration ohne zusätzliche Bestätigung. Finale Commit-SHA, PR und Master-CI sind vor dem Merge noch unbekannt und werden nicht vorweggenommen.
