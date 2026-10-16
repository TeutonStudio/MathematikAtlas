# v3.4.0 nach master integrieren

## Status und Ziel

Lokale Releaseabnahme abgeschlossen, GitHub-Integration ausstehend. Nutzerauftrag vom 16.10.2026: den vorhandenen Gesamtstand als genau einen geprüften Squashcommit `v3.4.0` auf `master` veröffentlichen. Dieser Plan dokumentiert den vor dem Push geprüften Zielbaum; tatsächliche Veröffentlichung und CI-Ergebnisse sind über den zugehörigen Release-PR und finalen Mastercommit nachzuweisen.

## Ziel und Nutzerwirkung

Methodenquelle/Identität, angeschlossene Variablengrundräume, Mengenvisualisierung, Join-Mehrfacheingänge, Anschlussinfos, Anschluss-Pie, Einfügelayout, Inspectorbereiche und globale Meldungsebenen auf Android und Desktop gemeinsam integrieren.

## Nicht-Ziele

Keine zusätzliche Produktentwicklung. Prädikatindex-/Operatorplanung ist kein implementiertes Releasefeature. Persönliche IDE-Dateien, `.kotlin/` und Python-Bytecode bleiben außerhalb des Commits.

## Untersuchter Istzustand

Live-GitHub-master: `c4b23c69c431370b600cd4bd810c8d5135c43ff7` (`v3.3.1`). Der frühere Produktcommit `04925fc1352ad551622c1916fbd8f36c9ab83105` liegt direkt auf dieser Basis und besitzt SamAI-Autor und -Committer. Weitere Produktänderungen werden auf dem über das vorgeschriebene Skript angelegten Abschlussbranch `samai/v3.4.0/master-abschluss` integriert. Releasebranch: `release/v3.4.0-methoden-grundraeume-visualisierung`. Historische Draft-PRs #433/#435/#436 sind kein Integrationsumfang und beanspruchen keine niedrigere aktive Releaseversion im aktuellen Plan.

## Fachliche Semantik

Genau ein neuer separat erzeugbarer und registrierter Typ: `mathematik.methode`. Der vollständige Release ist daher eine Knoten-Version y: `3.3.1 → 3.4.0`. Die vorbereitete Prädikatindexplanung ist keine neue Registrierung. Der Abschluss übernimmt vorhandene Fachmodelle und behebt ausschließlich nachgewiesene Integrationsfehler.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

Join-Mehrfachbelegung und Beitragsreihenfolge werden in Kartenformat 10 persistiert und über den gemeinsamen mathematischen Codec idempotent migriert. Beitragsidentität, Reihenfolge und Fallbackrolle bleiben beim Einfügen, Herausziehen und Umverdrahten erhalten. Eingebettete Methodenquellen materialisieren feste Argumentrollen und verwenden auf Android und Desktop dieselbe validierte Argumentanzahl. Details stehen in den Fachplänen und ADRs zu Methodenquelle, Grundräumen und Join-Eingängen.

## Architekturentscheidungen

Bestehenden Release-Workflow verwenden: SamAI-Arbeitscommits, Integration in genau einen Releasebranch, Squash-PR gegen master. Keine neue Abhängigkeit, kein paralleles Modell. Releaseverwalter besitzt Metadaten und Git-/PR-Integration; der Koordinator besitzt Produktkorrekturen. Unabhängiger Verifizierer verändert keine Produktionsdateien.

## Betroffene Dateien und Symbole

Rechenkern, Kartenadapter, neutraler Editor, Mathematikknoten, Android, Desktop und zugehörige Tests/ADRs/Fachpläne. Release-Metadaten: `release/roadmap.toml`, Android `3.4.0/3004000`, Desktoppaket und CI-Artefaktname `3.4.0`; Typmanifest ergänzt ausschließlich `mathematik.methode`.

## Meilensteine

- [x] Livebasis, vollständigen Umfang, Registrydiff und y-Klassifikation geprüft.
- [x] Release-Metadaten und vollständige Umfangsbeschreibung abgeglichen.
- [x] Nachgewiesene Integrationsfehler gezielt korrigiert und unabhängig nachgeprüft; keine offenen Findings.
- [x] Vollständige lokale JVM-/Desktop-/Lint-/APK-Prüfung auf finalem Produktstand erfolgreich.
- [ ] Explizite Aufgabenpfade committen, SamAI-Identität verifizieren, Releasebranch pushen und PR eröffnen.
- [ ] Fünf Pflichtjobs auf finalem PR-Head prüfen, Squash `v3.4.0` integrieren und Masterstand nachprüfen.

## Konkrete Umsetzungsschritte

Ausgewählte Dateien über `samai-git.sh` committen, Identität verifizieren, Releasebranch per Fast-forward aktualisieren und ausschließlich diesen Branch pushen. PR gegen die exakte Vorgängerbasis eröffnen; kein Merge bei fehlender oder roter Pflichtprüfung. Finaler Titel exakt `v3.4.0`; kein direkter Masterpush.

## Tests und Validierung

Finaler Gesamtaufruf: `./gradlew --no-parallel test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :KnotenKartenVerwalter:assembleDebugAndroidTest :desktopApp:compileKotlin` – BUILD SUCCESSFUL, 2m56s, 277 Tasks (44 ausgeführt, 233 aktuell). XML-Berichte: **2112 Fälle, 0 Fehler, 1 vorhandener Opt-in-Skip; 2111 bestanden**. Repository-/Architekturprüfung, 15 Standardkarten, Releaseplan, Versionsfolge inklusive synthetischem PR-Kontext, 14 Guardregressionen und Diffprüfung erfolgreich. Neue Regressionen prüfen reale Methodenquellenkandidaten und Join-Ersetzungspfade; gezielter Methodenknotentest mit 8 Fällen erfolgreich.

Ein vorheriger Gesamtversuch meldete einen 5000-ms-Timeout in `VisualisierungsBerechnungTest.letzterVerbraucherBrichtAbUndNeuerAuftragIstMoeglich`. Der isolierte Wiederholungslauf mit `--rerun-tasks` bestand (45s, 42 Tasks ausgeführt), anschließend bestand der vollständige oben genannte Nachlauf. Dafür erfolgte keine Produktionsänderung; eine Last- oder Schedulingursache ist nicht nachgewiesen.

Standalone-`pruefe_kern.py` lokal mangels `kotlinc` nicht ausführbar; Gradle-Kerntests sind erfolgreich. Android-Test-APKs gebaut; neue Gesten werden durch verpflichtende Android-Emulator-CI geprüft. Frühere gesperrte/fehlende Geräte und lokale Emulatorabbrüche ersetzen keinen Laufzeitnachweis. Physische Stiftabnahme und vollständiges 60-Hz-Ziel der Visualisierung bleiben offen.

## Persistenz und Migration

Format-10-Roundtrip, ältere Defaultbelegung, stabile Referenzen, Fallbackrollen und idempotente Joinmigration sind durch vorhandene/neue Tests abgedeckt. Der Releaseabschluss selbst führt keinen weiteren Datenvertrag ein.

## Risiken und Rückfallstrategie

Kein Merge bei falscher Basis, roten/fehlenden Checks, falscher Klassifikation oder unbeabsichtigten Dateien. Keine pauschale Rücknahme bestehender Änderungen. Der einmalige lokale Timeout bleibt dokumentiert; finaler Wiederholungslauf ist grün. GitHub-Laufzeit und Linux-Paketprüfung bleiben Pflicht vor Veröffentlichung.

## Fortschritt und Entscheidungsprotokoll

16.10.2026: Skill `release-verwalten` und verbindliche Quellen gelesen, Live-Git geprüft. Version bleibt `3.4.0`/y auf exakter veröffentlichter Basis v3.3.1. Keine zusätzliche Version reserviert.

16.10.2026: Unabhängige Prüfung fand Join-Doppelzählung und verlorene Zielbeitragsmetadaten sowie einstelligen Export kanonischer Join-Methodenquellen. Koordinator korrigierte alle betroffenen Einfüge-/Bypass-/Umverdrahtungs-/Übersetzer-/Piepfade und die feste Argumentmaterialisierung einschließlich Inspector. Gezielte unabhängige Nachprüfung ohne offene Findings. Ersatzspezifische Adapterpläne umgehen den gewöhnlichen Verbindungscache; ein zunächst behaupteter Abbruch→Neuverbindungs-Repro wurde zurückgezogen, weil `beginneVerbindung` den Cache bereits leert.

16.10.2026: Abschließender vollständiger lokaler Nachlauf nach allen Produktkorrekturen grün. Artefakte ausgeschlossen; verbleibende GitHub-Integration ausschließlich nach erfolgreicher Pflicht-CI.

## Abweichungen vom ursprünglichen Plan

Der Gesamtumfang wurde um die bereits vorhandenen lokalen Join-/Graph-/Inspector-/Pieänderungen erweitert und im bestehenden Releaseeintrag beschrieben. Zwei nachgewiesene Integrationsfehlerfamilien wurden vor Veröffentlichung gezielt repariert. Keine fachliche Erweiterung außerhalb des vorhandenen Releaseumfangs.

## Ergebnis und Verifikation

Lokaler finaler Releasekandidat vollständig geprüft und unabhängig statisch abgenommen. Verbindlicher Online-Nachweis: Release-PR des oben benannten Releasebranches, dessen finaler geprüfter Head sowie anschließender Squashcommit `v3.4.0` auf master. Diese GitHub-Schritte bleiben bis zum nachweisbaren Abschluss offen.
