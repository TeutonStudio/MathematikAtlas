---
name: neuer-knoten
description: Plant, implementiert und verifiziert neue Knotentypen einschließlich ihrer mathematischen Semantik, Persistenz und Versionswirkung.
---

# Neuen Knoten entwickeln

Verwende diesen Skill für jeden Auftrag, der mindestens einen neuen Knotentyp oder eine neue Knotenfamilie plant, implementiert oder prüft. Lies zuerst `AGENTS.md`, `docs/codex/NEW_NODE_WORKFLOW.md`, `docs/codex/NODE_CONTRACT.md` und die relevanten Architekturdateien.

## Versionswirkung

Ein neuer, separat erzeugbarer und registrierter Knotentyp ist immer eine `y`-Änderung im Schema `vM.y.x`:

- `y` wird um eins erhöht,
- `x` wird auf `0` gesetzt,
- der `master_verwalter` reserviert die Version vor Produktionsimplementierung.

Änderungen an einem vorhandenen Knotentyp ohne neuen Typ-Schlüssel sind `x`-Änderungen. Dazu gehören insbesondere neue Anschlüsse, Parameter, Inspector-Felder, Renderer, Auswertungsfälle und Sonderfälle. Enthält ein Release neue Knoten und weitere Änderungen, gilt der gesamte Release als `y`-Version.

Ein Implementierungsagent darf weder einen neuen Knotentyp in eine reservierte `x`-Version aufnehmen noch selbstständig die Versionsnummer ändern. Bei abweichendem Umfang wird zuerst der `master_verwalter` eingeschaltet.

## Ablauf

1. `master_verwalter` prüft Releasezustand, Versionsraum und reserviert die passende `y`-Version.
2. `node_planner` untersucht vergleichbare Knoten, Registry, Auswerter, Inspector, Persistenz und Tests.
3. Der Plan nennt alle neuen Typ-Schlüssel und enthält einen Abschnitt **Versionswirkung**.
4. `math_reviewer` prüft mathematisch nicht triviale Semantik.
5. `node_implementer` setzt nur den freigegebenen Umfang um.
6. `node_verifier` prüft unabhängig Verhalten, Architektur, Tests, Persistenz und Versionsklassifikation.
7. Blockierende Findings werden korrigiert und erneut verifiziert.

## Pflichtangaben des Plans

- fachliches Ziel und Semantik,
- neue Typ-Schlüssel oder ausdrücklich keine neuen Typ-Schlüssel,
- Anschlüsse und Verbindungskompatibilität,
- Auswertung und Fehlerfälle,
- Inspector und Darstellung,
- Persistenz und Migration,
- Tests und Prüfbefehle,
- reservierte Version und Begründung für `y` oder `x`.

## Stopbedingungen

Stoppe die Arbeit und fordere eine Entscheidung des `master_verwalter` an, wenn:

- die reservierte Version eine `x`-Version ist, aber ein neuer Typ-Schlüssel benötigt wird,
- ein geplanter neuer Knotentyp entfällt und die `y`-Version dadurch unbegründet wird,
- zusätzliche neue Knotentypen außerhalb des freigegebenen Plans entstehen,
- Releaseplan, Branch oder tatsächlicher Git-Zustand einander widersprechen.