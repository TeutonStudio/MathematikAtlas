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

## Erster GitHub-Lauf auf PR #454

Release-PR: https://github.com/TeutonStudio/MathematikAtlas/pull/454, geprüfter Arbeitscommit `41d94f04c58afd56f15b73d47436de901ddb276d` (SamAI/SamAI). Release-Guard, Android-Build, Mathematikkern und Linux-Paketierung erfolgreich. Android-Laufzeitjob 117149321273 / Run38953156592 fehlgeschlagen; kein Merge. Editor-Gerätetests 7/7 bestanden; App51Tests mit6Fehlern.

Konkrete Fehler: Methodenquellen-Dialogtest findet `Abbrechen` nicht; drei Pie-Tests erwarten eine noch offene Sitzung; zwei optionale Visualisierungsleistungsmessungen werden trotz AssumptionViolatedException vom Instrumentationspfad als Fehler gemeldet. Der Emulator besitzt laut Startlog320×640 bei160dpi. Die feste Pie-Dragtestgeometrie legt den Startport bereits außerhalb dieser Breite. Die zwei Pie-Mehrfachauswahltests wählen Addition, deren inzwischen einstelliger Join-Eingang korrekt unmittelbar verbindet; sie prüfen damit keinen mehrdeutigen Anschlussfall mehr. Dialog-/Instrumentationsdetails müssen gezielt geklärt werden, bevor ein korrigierter Head erneut alle Pflichtprüfungen durchläuft. Artefaktnachweis lokal `/tmp/atlas-v340-runtime-454.zip`; kein pauschaler Rerun und keine bewiesene Emulatorstörung.

## Gezielte Android-CI-Korrektur

Die Stift-Testszene verwendet jetzt eine Position und Größe innerhalb des tatsächlichen Hosts und begrenzt das Dragziel auf dessen Grenzen. Die Mehranschluss-/Abbruchtests verwenden Potenz mit tatsächlich mehreren geordneten Eingängen anstelle der einportigen Join-Addition. Die optionale Messannahme wird über eine äußere RuleChain vor der Compose-Coroutine ausgewertet; Messablauf und Opt-in-Argument bleiben erhalten.

Der Methodenquellen-Dialogtest wählt die beabsichtigte Potenzquelle anhand eines stabilen Operator-ID-Testtags, scrollt zur Kachel. Ein zuvor mehrdeutiger oder außerhalb des sichtbaren Bereichs liegender Tap ist eine begründete Hypothese, keine bewiesene Fehlerursache. Weder Tastaturlayout noch Produktfehlerfreiheit werden aus dieser Testanpassung abgeleitet; erst der neue Android-CI-Lauf kann den Bedienpfad bestätigen. Die einzige produktive Anpassung ist ein inertes Semantik-Testtag. Lokale App-Tests, Lint und App-/Test-APK erfolgreich (45s, 207Tasks); abschließende Test-APK-Kompilierung der endgültigen Testfassung ebenfalls erfolgreich (113Tasks aktuell,708ms). Es wurde weder eine zusätzliche Espresso-Abhängigkeit aufgenommen noch ein IME-Schließen in der Testfassung verwendet. Sämtliche fünf Pflichtjobs müssen auf dem neuen Head erneut grün sein.

## Zweiter GitHub-Lauf auf Head91f69aa3

Vier Pflichtjobs erneut erfolgreich, AndroidruntimeJob117158451923/Run38955867990 erneut fehlgeschlagen. Editor7/7 bestanden; App51Tests mit3Fehlern. Alle zuvor roten Pie-Fälle sind nun erfolgreich. Methodenquellenauswahl scheitert beim `performScrollTo()` bereits an einem nicht gefundenen Lazy-Zielnode. Das beweist keinen Null-Viewport: ein noch nicht komponiertes Grid-Element muss über den Scroll-To-Node-Vertrag des Lazycontainers gesucht werden, wie es der vorhandene Knotenauswahl-Gerätetest tut. Der enge Bedien-/Layoutpfad wird weiter geprüft.

Beide optionalen Messungen werden weiterhin wegen AssumptionViolatedException als Fehler gemeldet, nun nachweislich außerhalb der Compose-Coroutine in der äußeren RuleChain. Die frühere Coroutine-Ursachenannahme ist damit nicht bestätigt. Die Messungen wurden nicht ausgeführt und dürfen nicht als erfolgreiche Hardwaremessung gelten. Weitere gezielte Instrumentationsklassifikation beziehungsweise ausdrückliche Auswahl der regulären Funktionstests erforderlich; kein Merge und kein blinder Wiederholungslauf. Artefaktnachweis `/tmp/atlas-v340-runtime-454-91f69aa3.zip`.

## Zweite gezielte Android-CI-Korrektur

Der Methodenquellen-Gerätetest verwendet jetzt `performScrollToNode` auf dem markierten LazyGrid-Container und klickt erst anschließend den eindeutig gefundenen Quellnode. Dies entspricht dem vorhandenen Knotenauswahl-Gerätetest; kein Layoutrefactor und keine Behauptung eines bewiesenen Tastaturlayoutfehlers. Die produktive Änderung beschränkt sich auf ein inertes Container-Testtag.

Die widerlegte Compose-RuleChain-Hypothese wird zurückgenommen; der Messcode bleibt im ursprünglichen Opt-in-Pfad. Der reguläre CI-Instrumentationsaufruf schließt ausschließlich `VisualisierungsLeistungsTest` explizit über `notClass` aus. Beide Hardwaremessungen bleiben separat mit `atlas.measureVisualization=true` ausführbar. Sie sind in der regulären CI **nicht ausgeführt**, weder bestanden noch als Skip nachgewiesen. Sämtliche funktionalen Android-/Compose-Regressionen bleiben im Pflichtlauf. Lokaler App-Test-/Lint-/APK-Nachlauf und anschließend alle fünf Pflichtjobs auf dem neuen Head erforderlich.

Lokaler Nachlauf der zweiten CI-Korrektur erfolgreich: App-Unit-Tests, App-Lint, Debug-APK und Test-APK, BUILD SUCCESSFUL in44s,207Tasks(24ausgeführt/183aktuell). Repository-, Releaseplan-, Versionsfolge- und Diffprüfung ebenfalls erfolgreich. Keine weiteren Produktänderungen geplant; neuer SamAI-Head und vollständige Pflicht-CI bleiben Voraussetzung für Merge.

## Begrenzter schmaler Operator-Auswahldialog

Die enge Quellprüfung zeigte einen strukturellen Größenkonflikt: Die nicht gewichteten Kategorien und der durch `fillMaxSize` vollständig beanspruchte 230-dp-Detailbereich konnten vor dem gewichteten Such-/Trefferbereich dessen verbleibende Höhe aufbrauchen. Der Koordinator korrigierte den schmalen Dialog: Kategorien, Suche und weiterhin begrenzte Details liegen in einem scrollbaren Steuerbereich mit höchstens45Prozent der inneren Inhaltshöhe; die Trefferliste erhält den restlichen Platz. Breite Darstellung verwendet dieselben fachlichen Daten und Aktionen. Titel und Abschlussaktionen bleiben in den gemeinsamen AtlasDialog-Bausteinen; keine neue Abhängigkeit, keine öffentliche API- oder Zustandsänderung.

Unabhängige gezielte Nachprüfung durch den Releaseverwalter nach Skill `dialog-erstellen`: gemeinsamer AtlasDialog-Rahmen und vergleichbarer KartenExportFormatDialog geprüft; bounded Detail-LazyColumn innerhalb des begrenzten Steuerbereichs, eigener Scrollbesitzer der Trefferliste, vorhandene Such-/Auswahl-/Übernahme-/Abbruchfunktionen und stabile IDs unverändert. Keine offenen statischen Findings. Neue Androidregression prüft sichtbare Listenhöhe, echte Quellenauswahl, Abbruch ohne Modelländerung und Übernahme bei Schriftfaktor1und2. Native Laufzeit, IME-Layout sowie vollständige visuelle Prüfung sämtlicher Theme-/Fenstervarianten sind damit noch nicht als bestanden behauptet.

Lokaler Nachlauf erfolgreich: App-Tests, Lint, Debug-APK und Test-APK, BUILD SUCCESSFUL in48s,207Tasks(24ausgeführt/183aktuell). Diffprüfung erfolgreich. Der davor gepushte Headb22267cc erhält ausdrücklich keine Mergefreigabe, unabhängig von dessen CI-Ergebnis: Dieser Produktfix muss auf einem neuen SamAI-Head vollständig geprüft werden.
