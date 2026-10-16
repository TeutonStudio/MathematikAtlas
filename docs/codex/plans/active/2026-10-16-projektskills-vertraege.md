# ExecPlan: Projektskills für kanonische Knoten und Verträge

## 1. Titel und Status
2026-10-16; Skillüberarbeitung zur PR-Prüfung, keine Veröffentlichung. Branch `samai/v3.4.1-projektskills-vertraege`, Basis `d37d479f8f51a1d153f962eeca77838262b88a09` (`v3.4.0`).

## 2. Ziel und Nutzerwirkung
Vorhandene Skills sollen bei Knotenarbeit kanonische Bestandspfade bevorzugen, Redundanzbefunde vor einer Bereinigung belegen und Methoden-, Tupel- und Formelverträge einschließlich Identitäten und Metadaten erhalten. Ergebnis ist ein eigener Prüf-PR.

## 3. Nicht-Ziele
Keine Produktbereinigung, neuen Skills/Knotentypen, Migrationen, Schemaänderungen, Produktionsabhängigkeiten, Änderungen historischer Releases, Zusammenführung alter PRs oder Merge nach master. Keine Gesamtfreigabe des offenen Formel-/Methoden-/Tupelvertrags.

## 4. Untersuchter Istzustand
Fünf Skills vorhanden: `neuer-knoten`, `inspektor-aendern`, `dialog-erstellen`, `release-verwalten`, `readme-lokalisieren`. Der README-Skill betrifft diesen Auftrag nicht und bleibt unverändert. Katalog, Rechner-/Prädikatmigrationen, Methoden-Capabilities, ID-basierte bedingte Anwendung, gemeinsame Kartenpipeline und aktuelle Mathematikcodecs existieren bereits. Frühere Chatanalysen sind nur als Inhaltsverweise verfügbar; ihre Aussagen werden nicht erfunden. Quellen sind aktueller Code, ADRs und bestehende Vertragspläne.

## 5. Fachliche und mathematische Semantik
Regeln präzisieren vorhandene Grenzen: sichtbare Erzeugung gegenüber Ladbarkeit; Operator/Übersetzer/Prädikat/Methodenquelle vor neuem Typ; strukturtreue Argumente/Ergebnisse; offene Voraussetzungen und Übersetzungszeugnisse. Komponentendomänen bleiben Metadaten, der explizite effektive Gesamtdefinitionsraum bleibt für Mitgliedschaft maßgeblich. Mathematikcodecs lehnen alte Wire-Versionen ab; unterstützte Graphmigrationen bleiben davon unabhängig.

## 6. Daten-, Node-, Handle- und Edge-Vertrag
Unverändert. Der Skill verlangt für künftige betroffene Änderungen stabile IDs, geordnete Beiträge, verlustfreie Metadatenübernahme, eindeutiges Remapping und diagnostizierbare nicht migrierbare Zustände.

## 7. Architekturentscheidungen
Bestehende Skills gezielt ergänzen. Umfangreiche gemeinsame Regeln liegen einmal unter `neuer-knoten/references/katalog-und-vertraege.md`, von allen betroffenen Skills erreichbar. Keine neue Architekturentscheidung; bestehende ADRs werden operationalisiert.

## 8. Betroffene Dateien und Symbole
Vier vorhandene `SKILL.md`, eine gemeinsame Referenz und dieser Plan. Governancebedingt zusätzlich `release/roadmap.toml`, Android-/Desktop-Version und Linux-Artefaktname. Keine Produktlogik, Tests, CI-Gates oder Knotenmanifeste ändern.

## 9. Meilensteine
- [x] M1 Bestand/Governance prüfen: master, fünf Skills, Katalog/Migration/Verträge gelesen; unabhängiger `master_verwalter` bestätigt v3.4.1/x und Basis.
- [x] M2 Regeln ergänzen: vier Skills und gemeinsame Referenz erstellt; README-Skill unverändert.
- [-] M3 Verifikation: Skillvalidierung, Links, Repository-, Standardkarten- und Releasechecks bestehen; unabhängige Nachprüfung und PR-Kontext folgen.
- [ ] M4 SamAI-Commit, Push und Draft-PR; CI-Status dem Head zuordnen.

## 10. Konkrete Umsetzungsschritte
`neuer-knoten` um Katalogentscheidung, Migrations-/Vertragsevidenz und korrekte x/y-Ablaufregel ergänzen. Inspector/Dialog an dieselbe Referenz anbinden; Release-Skill um Manifest-, Migrations-, CI- und Vollständigkeitsgate ergänzen. Prüfungen ausführen, Abschlussdiff prüfen, über SamAI-Werkzeug committen, nur eigenen Branch pushen und Draft-PR gegen master eröffnen.

## 11. Tests und Validierung
Alle fünf Skills mit `skill-creator/scripts/quick_validate.py` prüfen; lokale Links der bearbeiteten Skills und konkrete Quellsymbole/Testanker prüfen. Repository-, Standardkarten-, Releaseplan-, Versionsfolge- und 14 Guardregressionen ausführen, danach Versionsfolge im PR-Kontext. Keine neuen Produktverhaltenstests für diese reine Skill-/Metadatenänderung. Bestehende Pflicht-CI bleibt maßgeblich.

## 12. Persistenz und Migration
Keine Datenänderung. Referenz unterscheidet historische Kartenmigration von bewusster Ablehnung älterer Mathematik-Wire-Versionen; sie verlangt Daten-/Verbindungs-/ID-Erhalt und Idempotenz, bevor künftige Bereinigungen freigegeben werden.

## 13. Risiken und Rückfallstrategie
Skillregeln garantieren kein Laufzeitverhalten. Historische Pläne/ADRs widersprechen teils aktuellem Code; neue Regeln verlangen Code-/Testprüfung. PR bleibt prüfbar und ungemergt; Rücknahme ist auf diesen Diff begrenzt. Fehlende Toolchain/Geräte werden nicht als Erfolg ausgegeben.

## 14. Fortschritt
Repository-/Architektur-/Methodenmodell-/Desktop-Shadowprüfungen, 15 Standardkarten, Releaseplan (107 Einträge), Versionsfolge und 14 Guardregressionen erfolgreich. Alle fünf Skillvalidierungen erfolgreich; sechs lokale Links der bearbeiteten Skills auflösbar. Ein erster Linkprüfer erfasste README-Beispieltext in einem Codeblock; auf tatsächliche Links der geänderten Skills eingegrenzt. Kein README-Produktfehler daraus abgeleitet.

## 15. Entscheidungsprotokoll
2026-10-16: `master_verwalter` bestätigt v3.4.1/x, weil `.agents/skills` nicht unter die versionsfreie Markdown-Ausnahme des Guards fällt. Release-Metadaten einschließlich Desktop/Artefaktname werden kohärent mitgeführt. Roadmapstatus `released` beschreibt nur den PR-Zielbaum. Offene historische Drafts #433/#435/#436 werden nicht integriert.

## 16. Abweichungen vom ursprünglichen Plan
Frühere Chatanalyse nicht als Text verfügbar; stattdessen aktuelle Quellpfade und versionierte Vertragsanalysen geprüft. Zusätzlich nötige Release-Metadaten folgen dem vorhandenen Guard, ohne dessen Ausnahmeregel zu erweitern.

## 17. Ergebnis und Verifikation
Skillregeln umgesetzt; abschließende Nachprüfung/Commit/PR und CI-Status noch einzutragen. Standalone-Kernprüfung nicht ausführbar: `kotlinc` fehlt. Gradle-Gesamtprüfung `bash gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin --no-daemon --console=plain` nach Netzwerkfreigabe gestartet, vor der Projektprüfung abgebrochen: benötigtes JDK 21 fehlt und Toolchain-Download ist nicht konfiguriert (lokal JDK 25). Keine Build-/Laufzeitabnahme behauptet.
