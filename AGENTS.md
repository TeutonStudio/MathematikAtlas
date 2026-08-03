# Mathematik Atlas – verbindliche Agentenanweisungen

## Zweck dieser Datei

Diese Datei ist die kurze, verbindliche Einstiegskarte für Agenten. Ausführliche Abläufe, Rollen und technische Verträge liegen in den unter **Verbindliche Lektüre** genannten Dokumenten. Wiederhole deren vollständigen Inhalt nicht hier; lies nur die für die aktuelle Aufgabe erforderlichen Quellen.

## Projektidentität

Der Mathematik Atlas ist eine native Android-Anwendung, die mathematische Objekte und Vorgänge als interaktive Knotenkarten modelliert, verbindet, auswertet und versioniert.

Projektbegriffe:

- **Knoten** sind persistierbare `KnotenDaten`-Instanzen.
- **Anschlüsse** sind typisierte `AnschlussDaten`-Instanzen.
- **Verbindungen** sind `VerbindungDaten` zwischen zwei `AnschlussVerweis`-Instanzen.
- **Karten** sind versionierte `KartenDaten` mit Knoten, Verbindungen, Ansicht und optionalen visuellen Gruppen.

Technologien:

- Kotlin, Gradle Kotlin-DSL, Android und Jetpack Compose,
- Material 3,
- Android- und Compose-freier Kotlin-Rechenkern,
- nativer Compose-Renderer für den unterstützten LaTeX-Teilumfang.

Das Repository verwendet ausdrücklich **nicht** Vite, React, React Flow, shadcn/ui oder KaTeX. Ermittle Versionen, Plugins, Tasks, Pakete und Verzeichnisnamen aus dem Repository statt aus älteren Projektbeschreibungen.

## Verbindliche Lektüre

Lies vor der Arbeit nur die aufgabenspezifisch relevanten Quellen:

1. Grundkontext: `docs/codex/PROJECT_CONTEXT.md`, `docs/codex/CURRENT_STATE.md`
2. Architektur: `docs/codex/ARCHITECTURE.md`
3. Knotenverträge: `docs/codex/NODE_CONTRACT.md`
4. Release-, Branch- oder Mergearbeit: `release/roadmap.toml`, `docs/codex/RELEASE_WORKFLOW.md`
5. Git-Identität und Branchherkunft: `docs/codex/GIT_IDENTITY.md`
6. Neue Knoten: `docs/codex/NEW_NODE_WORKFLOW.md`
7. Größere Änderungen: `docs/codex/PLANS.md`
8. Reviews: `docs/codex/CODE_REVIEW.md`
9. Tests: `docs/codex/TEST_STRATEGY.md`

Dokumentation ist Orientierung, nicht Ersatz für Code- und Git-Prüfung. Widersprechen Dokumentation und Code einander, gilt der nachweisbare Istzustand. Aktualisiere widersprüchliche Dokumentation im Rahmen der Aufgabe.

## Git-Identität von SamAI

Alle durch SamAI lokal erzeugten Commits müssen folgende Metadaten besitzen:

```text
Author:    SamAI <46108494+TeutonStudio@users.noreply.github.com>
Committer: SamAI <46108494+TeutonStudio@users.noreply.github.com>
```

Verbindliche Regeln:

- Erzeuge SamAI-Branches mit `bash scripts/samai-git.sh branch ...`.
- SamAI-Branches beginnen mit `samai/`.
- Erzeuge SamAI-Commits ausschließlich mit `bash scripts/samai-git.sh commit ...`.
- Stage nur ausdrücklich zur Aufgabe gehörende Dateien; das Skript führt kein automatisches Staging aus.
- Prüfe vor dem Push mit `bash scripts/samai-git.sh verify HEAD`.
- Keine direkten Commits auf `master` oder `main`.
- Verändere weder globale noch Repository-lokale `user.name`- oder `user.email`-Werte für SamAI.
- Der GitHub-Connector kann Autor und Committer derzeit nicht auf `SamAI` setzen. Connector-Commits dürfen daher nicht als korrekt signierte SamAI-Commits bezeichnet werden und müssen im Abschlussbericht ausdrücklich genannt werden.

Ein Branch besitzt technisch keinen Autor. Die SamAI-Herkunft wird deshalb über den Präfix `samai/` kenntlich gemacht. Vollständige Regeln und Ausnahmen stehen in `docs/codex/GIT_IDENTITY.md`.

## Modulgrenzen

- `MathematikRechenSystem`: mathematische Domäne; frei von Android, Compose und Karteneditor-Abhängigkeiten.
- `KnotenKartenVerwalter`: fachneutraler Karteneditor, Graphdaten, Interaktion und Undo/Redo; keine Mathematikregeln.
- `MathematikKartenAdapter`: Verbindung zwischen Graph und Rechenkern, topologische Ausführung und Auswertungscache.
- `MathematikKnoten`: mathematische Vorlagen, Anschlussarten, Auswerter und spezialisierte Renderer.
- `app`: Navigation, Inspector, Bibliothek, Persistenz, Import/Export und anwendungsspezifische Dialoge.

Abhängigkeiten dürfen nur in der durch die Gradle-Module vorgegebenen Richtung erweitert werden.

## Unverhandelbare Architekturregeln

- Trenne mathematische Semantik, Graphzustand, Anwendungskoordination, Darstellung und Persistenz.
- Implementiere Mathematik im Rechenkern oder vorgesehenen Auswertern, nicht ausschließlich in Composables oder Pointer-Handlern.
- Ein Knoten visualisiert und konfiguriert ein fachliches Modell; er ist nicht alleinige Quelle mathematischer Wahrheit.
- Verwende stabile IDs für Karten, Knoten, Anschlüsse, Verbindungen und visuelle Gruppen.
- Definiere für Anschlüsse Richtung, Datentyp, Kardinalität, Reihenfolge und Kompatibilität.
- Persistiere keine Composables, Funktionen, `State`-Objekte oder andere Laufzeitobjekte.
- Verwende bestehende Vorlagenkataloge und Auswerterregister; erzeuge keine parallelen Ersatzsysteme.
- Berücksichtige Laden, Speichern, Kopieren, Löschen, Undo/Redo und Migrationen, sofern die Änderung diese Pfade berührt.
- Der Inspector verändert validierte `KnotenDaten` über vorgesehene Kartenaktionen, nicht über unabhängigen UI-Schattenzustand.
- Der fachneutrale Karteneditor darf keine mathematischen Parameterkonventionen kennen.
- Keine neue Produktionsabhängigkeit ohne nachgewiesene Notwendigkeit.
- Keine beiläufigen Refactorings außerhalb des Aufgabenumfangs.

## Arbeitsweise

1. Prüfe vor Änderungen tatsächliche Aufrufpfade und mindestens einen vergleichbaren vorhandenen Pfad.
2. Ermittle passende Prüfungen aus Gradle, Skripten und CI.
3. Ändere nur erforderliche Dateien und wahre bestehende öffentliche APIs, sofern keine bewusste Änderung verlangt ist.
4. Dokumentiere Annahmen, Risiken und nicht ausführbare Prüfungen konkret.
5. Verwende bei längeren Aufgaben einen ExecPlan nach `docs/codex/PLANS.md`.
6. Aktualisiere `CURRENT_STATE.md` nur mit nachweisbaren Fakten.
7. Halte dauerhafte Architekturentscheidungen als ADR unter `docs/codex/decisions/` fest.
8. Prüfe den Abschlussdiff auf unbeabsichtigte Änderungen.

## Release- und Branchverwaltung

Bei Versionsnummern, neuen Release- oder SamAI-Branches, Pull Requests gegen `master` und Veröffentlichungen verwende den Skill `release-verwalten` und den Agenten `master_verwalter`.

Versionsschema `vM.y.x`:

- `M`: ausdrücklich beschlossener Versionsraum,
- `y`: mindestens ein neuer, separat erzeugbarer und registrierter Knotentyp oder eine neue Knotenfamilie,
- `x`: Änderung ohne neuen Knotentyp, etwa Fehlerkorrektur, UI, Dokumentation, Refactoring oder Erweiterung vorhandener Knoten.

Verbindlich:

- Neue Knoten erhöhen `y` und setzen `x` auf `0`.
- Ohne neue Knoten wird nur `x` erhöht.
- Maßgeblich sind vollständiger Plan und Abschlussdiff.
- Keine direkten Produktionscommits auf `master`.
- Pro PR gegen `master` genau eine Version.
- Pro Release genau ein finaler Commit `v<version>` auf `master`.
- Android-`versionName`, `versionCode` und `release/roadmap.toml` müssen übereinstimmen.
- Ein technisch mergebarer PR ist nicht automatisch ein zulässiger Release.
- Der `master_verwalter` verwaltet Version, Basis, Branch und Integration; der fachliche Implementierer bleibt für Produktcode verantwortlich.

Branchmuster:

```text
release/v<version>-<kurzname>
samai/v<version>/<aufgabe>
samai/v<version>-<kurzname>
repair/v<version>/<aufgabe>
```

Historische `agent/`-Branches bleiben gültige Historie, werden von SamAI aber nicht mehr neu erzeugt.

## Neuer-Knoten-Workflow

Bei einem neuen Knotentyp verwende den Skill `neuer-knoten`.

Reihenfolge:

1. `master_verwalter` klassifiziert und reserviert die Version.
2. `node_planner` untersucht den Bestand und spezifiziert Typ-Schlüssel, Anschlüsse und Semantik.
3. `math_reviewer` prüft mathematisch nicht triviale Semantik.
4. `node_implementer` setzt den bestätigten Plan um.
5. `node_verifier` prüft unabhängig Diff, Verhalten, Architektur, Tests und Versionsklassifikation.
6. Blockierende Findings gehen zurück an den Implementierer; anschließend erfolgt die Abschlussprüfung.

Planer, Mathematikprüfer und Verifizierer verändern keine Produktionsdateien. Es arbeitet höchstens ein schreibender Implementierungsagent gleichzeitig.

## Standardprüfungen

Leite die tatsächlich passenden Befehle aus dem Bestand ab. Der übliche Prüfpfad ist:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Ein nicht ausführbarer Befehl wird mit konkretem Grund dokumentiert. Ein erfolgreicher Build ersetzt keinen Laufzeittest auf Emulator oder Gerät.

## Definition of Done

Eine Aufgabe ist abgeschlossen, wenn:

- das beauftragte Verhalten umgesetzt ist,
- fachliche und technische Randfälle behandelt sind,
- relevante Tests ergänzt oder begründet nicht ergänzt wurden,
- passende Prüfungen ausgeführt und Fehler aus der Änderung behoben wurden,
- Persistenz und Migration berücksichtigt wurden, falls Daten betroffen sind,
- Dokumentation und Code denselben Zustand beschreiben,
- der Diff frei von unbeabsichtigten Änderungen ist,
- Git-Identität und Branchherkunft den Regeln entsprechen,
- bei Releases Versionsplan, Android-Version, Basis und Versionsklassifikation übereinstimmen.

## Abschlussbericht

Berichte knapp und überprüfbar:

1. umgesetztes Verhalten,
2. zentrale geänderte Dateien,
3. ausgeführte Prüfungen und Ergebnis,
4. verbleibende Risiken oder bewusst nicht bearbeitete Punkte,
5. verwendeter Branch und Commit-SHA,
6. Autor- und Committeridentität oder ausdrücklich die Connector-Einschränkung,
7. bei Releases Version, Klassifikation, Basis und Release-Guard-Status.
