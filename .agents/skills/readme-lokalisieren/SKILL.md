---
name: readme-lokalisieren
description: Erstellt und synchronisiert ein mehrsprachiges README-System mit README.md als Sprachverzeichnis, README_ger.md als deutscher Hauptfassung und README_eng.md als englischer Übersetzung.
---

# README lokalisieren

Verwende diesen Skill, wenn ein Repository erstmals eine öffentliche README erhalten soll, eine bestehende README in mehrere Sprachen aufgeteilt werden soll oder vorhandene Sprachfassungen synchronisiert werden müssen.

## Zielstruktur

Im Repository-Root gilt folgende Struktur:

```text
README.md
README_ger.md
README_eng.md
README_<weitere-sprache>.md
```

`README.md` ist ausschließlich das Sprachverzeichnis. Es enthält keine eigenständige Projektbeschreibung, die anschließend mit den Sprachfassungen auseinanderlaufen könnte.

Die Reihenfolge im Sprachverzeichnis ist verbindlich:

1. `README_ger.md`
2. `README_eng.md`
3. weitere vorhandene oder ausdrücklich angeforderte Sprachfassungen

Weitere Sprachen verwenden dasselbe Namensschema `README_<sprachcode>.md`. Bestehende abweichende Konventionen werden nicht stillschweigend gelöscht; sie werden zunächst als mögliche Altstruktur erkannt und nur im Rahmen des Auftrags migriert.

## Quelle der Wahrheit

Standardmäßig ist `README_ger.md` die kanonische Inhaltsfassung.

- Fehlt sie, erstelle sie aus dem nachweisbaren Repositoryzustand und einer vorhandenen Projekt-README, sofern eine solche existiert.
- Gibt der Nutzer ausdrücklich eine andere Sprachfassung als Quelle vor, gilt diese für den aktuellen Lauf.
- Widersprechen sich Sprachfassungen ohne ausdrückliche Vorgabe, übernimm nicht einfach die längste oder jüngste Datei. Ermittle nachweisbare Projektfakten aus Code und Dokumentation; für rein redaktionelle Formulierungen hat `README_ger.md` Vorrang.
- `README.md` ist niemals Quelle für Projektinhalt, sobald das Sprachverzeichnis eingerichtet wurde.

## Inhaltliche Gleichheit

Alle lokalisierten README-Dateien müssen inhaltlich dieselbe öffentliche Aussage enthalten.

Verbindlich gleich bleiben:

- Abschnittsreihenfolge und Informationsumfang,
- beschriebene Funktionen und Grenzen,
- Installations- und Startanweisungen,
- Voraussetzungen und unterstützte Plattformen,
- Beispiele und deren Bedeutung,
- Tabellen, Listen und Warnhinweise,
- Bilder, Diagramme und relative Linkziele,
- Lizenz-, Beitrags- und Supportinformationen,
- Versions- oder Statusangaben, sofern sie überhaupt in die README gehören.

Sprachabhängig angepasst werden dürfen:

- Überschriften und Fließtext,
- sichtbare Linktexte,
- Beschriftungen in Tabellen,
- natürlichsprachliche Hinweise in Beispielen, sofern sie nicht Teil eines technischen Literals sind.

Nicht übersetzen:

- Quellcode,
- Befehle,
- Dateinamen und Pfade,
- Paket-, Modul-, Klassen-, Methoden- und API-Namen,
- Konfigurationsschlüssel,
- URLs,
- Issue- und PR-Nummern,
- Marken-, Produkt- und Eigennamen, wenn keine etablierte Übersetzung existiert.

Eine Übersetzung darf stilistisch natürlich sein. Sie muss nicht Satz für Satz identisch sein, aber weder Informationen hinzufügen noch entfernen.

## Ablauf

### 1. Repository verstehen

Lies zuerst:

1. `AGENTS.md`, falls vorhanden,
2. vorhandene `README*`-Dateien im Root,
3. öffentliche Projekt- und Entwicklungsdokumentation,
4. Build-, Paket- und Startkonfiguration,
5. relevante CI- oder Releaseinformationen, wenn die README deren Nutzung beschreibt.

Prüfe Aussagen gegen den tatsächlichen Repositoryzustand. Erfinde keine Funktionen, Befehle, Plattformunterstützung oder Versionsstände, nur damit die README weniger leer aussieht. Menschen können Marketing bereits selbst ausreichend zuverlässig übertreiben.

### 2. Bestehende README-Struktur klassifizieren

Unterscheide:

- nur `README.md` vorhanden,
- `README.md` plus einzelne Sprachfassungen,
- bereits vollständiges Sprachverzeichnis,
- mehrere Sprachfassungen mit inhaltlicher Drift.

Ist `README.md` noch die bisherige Projektbeschreibung, verwende ihren belegbaren Inhalt als Ausgangsmaterial, überführe die deutsche Fassung nach `README_ger.md`, erstelle `README_eng.md` und ersetze anschließend `README.md` durch das Sprachverzeichnis.

### 3. Deutsche Hauptfassung erstellen oder aktualisieren

`README_ger.md` soll eine eigenständig verständliche öffentliche Projektbeschreibung sein.

Bevorzuge eine klare Struktur wie:

- Projektname und Kurzbeschreibung,
- zentrale Fähigkeiten,
- Voraussetzungen,
- Installation oder Build,
- Verwendung,
- Projektstruktur oder Architektur nur soweit für Nutzer bzw. Mitwirkende sinnvoll,
- Entwicklungs- und Testhinweise,
- Mitwirkung,
- Lizenz.

Diese Gliederung ist kein Zwang. Übernimm die tatsächlich sinnvolle Struktur des Projekts und bewahre bereits gute bestehende Abschnitte.

### 4. Englische Fassung synchronisieren

Erstelle oder aktualisiere `README_eng.md` aus der bestätigten deutschen Hauptfassung.

- gleiche Abschnittsreihenfolge,
- gleicher Informationsgehalt,
- gleiche Beispiele,
- gleiche Linkziele,
- gleiche technischen Literale,
- natürliches technisches Englisch statt wörtlicher Maschinenübersetzung.

### 5. Weitere Sprachen synchronisieren

Bearbeite zusätzliche `README_<sprachcode>.md` nur, wenn sie bereits Teil des Repositories sind oder ausdrücklich angefordert wurden.

Jede weitere Sprachfassung folgt denselben Synchronitätsregeln. Neue Sprachfassungen werden im Sprachverzeichnis hinter Deutsch und Englisch eingetragen.

### 6. Sprachverzeichnis erzeugen

`README.md` bleibt bewusst klein. Empfohlene Form:

```markdown
# Sprachen / Languages

- [Deutsch](README_ger.md)
- [English](README_eng.md)
- [Weitere Sprache](README_xyz.md)
```

Regeln:

- Deutsch steht immer zuerst.
- Englisch steht immer an zweiter Stelle.
- Danach folgen alle weiteren tatsächlich vorhandenen Sprachfassungen.
- Verlinke keine Datei, die im Zielstand nicht existiert.
- Füge keine zweite vollständige Projektbeschreibung in `README.md` ein.

### 7. Synchronität prüfen

Vergleiche nach den Änderungen alle Sprachfassungen miteinander.

Prüfe mindestens:

- gleiche Hauptabschnitte in gleicher Reihenfolge,
- keine nur in einer Sprache vorhandenen Features oder Einschränkungen,
- identische Codeblöcke und Befehle,
- identische Bilder und Linkziele,
- keine veralteten Installationsschritte in einzelnen Sprachen,
- alle Links aus `README.md` zeigen auf vorhandene Dateien,
- Markdown-Struktur ist syntaktisch plausibel.

Kann eine Aussage nicht belegt oder sinnvoll übersetzt werden, kennzeichne sie im Arbeitsbericht statt still einen Inhalt zu erfinden.

## Änderungsregel

Wird bei einer späteren Aufgabe eine lokalisierte README inhaltlich verändert, synchronisiere im selben Arbeitsgang alle übrigen vorhandenen Sprachfassungen. Eine README-Änderung ist erst abgeschlossen, wenn die Sprachfassungen wieder denselben fachlichen Stand beschreiben.

Reine sprachliche Korrekturen, die den Informationsgehalt nicht verändern, müssen nur in der betroffenen Sprachfassung vorgenommen werden.

## Abschlussbericht

Berichte knapp:

1. verwendete kanonische Sprachfassung,
2. erstellte oder aktualisierte README-Dateien,
3. zusätzlich gefundene Sprachfassungen,
4. relevante inhaltliche Konflikte und ihre Auflösung,
5. durchgeführte Struktur- und Linkprüfungen,
6. nicht belegbare oder bewusst unveränderte Aussagen.
