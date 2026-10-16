---
name: neuer-knoten
description: Plant, implementiert und verifiziert neue oder wesentlich geänderte Knotenverträge sowie Knotenkonsolidierungen im Mathematik Atlas; prüft kanonischen Bestand, Methoden-, Tupel- und Formelverträge, Migration und Versionswirkung.
---

# Neuen Knoten entwickeln

Verwende diesen Skill für neue Knotentypen und Knotenfamilien sowie wesentliche Änderungen an bestehenden Knotenverträgen, Operatorzuordnung oder Knotendarstellung. Lies zuerst `AGENTS.md`, `docs/codex/NEW_NODE_WORKFLOW.md`, `docs/codex/NODE_CONTRACT.md` und die relevanten Architekturdateien.

## Kanonischen Bestand und Vertragsumfang bestimmen

Bei Typentscheidung, Konsolidierung, Legacybereinigung oder Änderungen an Methoden, Tupeln, Formeln und Übersetzungen lies [Katalog-, Migrations- und Vertragsregeln](references/katalog-und-vertraege.md). Diese Referenz beschreibt die bevorzugten Bestandspfade, verlustfreie Grenzen und die zugehörigen Prüfgates. Ein Analysebefund ist ein Prüfauftrag, keine Löschfreigabe.

Unterscheide den sichtbaren Erstellen-Katalog `KanonischerMathematikKnotenKatalog.alle()` von lade-kompatiblen Vorlagen und Auswertern. Ermittle die tatsächlich hinzugefügten Typ-Schlüssel über den Katalogdiff und `release/knotentypen.json`, nicht über Dateinamen, Vorlagenanzahl oder neue Operatoren. Verwende den Katalog auch für Suche, anschlussbezogene Varianten und Methodenquellen; keine mathematische Ersetzungslogik je Plattform.

## Rollen vor der Typentscheidung trennen

Dokumentiere zuerst die fachliche Rolle. Die technische Speicherung als `KnotenDaten` macht die folgenden Rollen nicht austauschbar:

- **Knoten:** fachliches Objekt, Quelle oder Operation. Prüfe bestehende vordefinierte Vorlagen und Rechnerfamilien; reguläre Knoten behalten ihre reguläre Kontur.
- **Übersetzer:** Überführung zwischen Darstellungen oder Strukturen über `atlas.uebersetzer` und dessen gespeichertes Ziel. Prüfe `ÜbersetzerKnoten.kt`, Konfiguration und `ÜbersetzerInspektor.kt`. Erhalte `KnotenDarstellungsForm.Adapter` und die stark gerundete Kapsel. Ein weiteres Übersetzerziel allein ist kein neuer Knotentyp; historische Spezialknoten bleiben ladbar.
- **Gruppe:** unterscheide versionierte Unterkartenreferenzen (`KartenVerweis`) von visuellen Gruppen (`VisuelleGruppen`, `GruppenProjektion`). Bündelung, Schnittstellen und Auf-/Zuklappen folgen bestehenden Gruppenpfaden. Eine Gruppe ersetzt keinen fachlichen Operator oder Übersetzer.

Untersuche pro betroffener Rolle einen vergleichbaren Bestandspfad. Rolle, Kontur und Verhalten bleiben beim Erzeugen, Konfigurieren, Kopieren, Laden und Undo/Redo erhalten. Eine Katalogkategorie ist keine Darstellungsrolle.

## Operator- und Kategoriesystem zuerst prüfen

Untersuche bereits bei der Planung `MathematikKnotenVorlagen`, `RechnerOperatorRegistry` und `RechnerFamilie` in `MathematikKnoten/.../RechnerOperatorVertrag.kt` sowie die betroffene Inspector-Operatorauswahl. Entscheide ausdrücklich zwischen vorhandenem Rechneroperator, Erweiterung eines Rechners, Übersetzerziel, vordefiniertem Knoten und neuem Knotentyp.

- Rechneroperationen verwenden stabile Operator-IDs, Familien, Signaturen, Kategorien und die bestehenden Konfigurations-/Auswertungspfade. Integriere sie in den gemeinsamen Operatorauswahldialog mit dessen Kategorie-/Suchsystem; keine parallelen Ersatzlisten.
- Für andere vordefinierte Knoten benenne Vorlage, Typ-Schlüssel, Katalogkategorie und Such-/Erstellenpfad. Prüfe Auffindbarkeit und Erzeugbarkeit über diese Pfade sowie Erhalt der gespeicherten Auswahl nach dem Laden.
- Der Plan nennt die konkreten Registry-/Vorlagen- und Inspector-Symbole. Bestimme die Versionswirkung erst danach; eine zusätzliche Operator-ID oder ein Übersetzerziel allein begründet keinen neuen Knotentyp.

## Aufgeräumter Inspektor und einheitliche Dialoge

Jeder Knoten braucht einen aufgeräumten Inspektor. Verwende für dessen Planung, Umsetzung und Prüfung verbindlich den Skill [inspektor-aendern](../inspektor-aendern/SKILL.md). Das gilt auch für Übersetzer und betroffene Gruppeninspektoren. Zeige nur fachlich relevante Einstellungen, ordne sie nach Abhängigkeit und biete ausschließlich gültige Konfigurationswechsel an. Auch Knoten ohne zusätzliche Fachparameter erhalten eine sinnvolle Einbindung in den vorhandenen Inspektorpfad; erfinde dafür keine unnötigen Einstellungen.

Benötigt ein Knoten einen Dialog, etwa zur Operatorauswahl bei Rechner-/Operatorknoten, verwende zusätzlich den Skill [dialog-erstellen](../dialog-erstellen/SKILL.md). Verwende vorhandene gemeinsame Dialogkomponenten und einen vergleichbaren Dialogpfad, insbesondere `RechnerOperatorAuswahlDialog`, für einheitlichen Aufbau und Bedienung. Erzeuge keinen zusätzlichen Dialog ohne fachliche Notwendigkeit.

Der Plan benennt Inspektoreinstellungen und deren Sichtbarkeits-/Validierungsregeln sowie gegebenenfalls Dialogzweck, verwendeten Dialog-Skill oder gemeinsamen Bestandspfad. Der Verifizierer prüft einen aufgeräumten Inspektor und bei notwendigen Dialogen die einheitliche Gestaltung und Bedienung als Abnahmekriterien.

## Δzeit gegen Layoutregressionen absichern

Verwende den gemeinsamen Pfad `KnotenRenderer.Fußzeile`, `KnotenAuswertungszeitFußzeile` und gegebenenfalls `mitAuswertungszeit`. Kein eigener Zeittext im Fachinhalt, keine zweite Anzeige und keine individuellen Positionsoffsets im Renderer.

Für reguläre Knoten und Übersetzer gilt verbindlich:

- Die Anzeige bleibt im eigenen unteren Rahmen an der Knotenunterkante, mit links ausgerichtetem Text. Die oberen Fußzeilenecken schließen an die unteren Tangentenpunkte der tatsächlich aufgelösten Kontur an, auch bei der Übersetzerkapsel.
- Prüfe in `KnotenKartenEditor.kt` den vollständigen Aufrufpfad einschließlich `unterkantenEinzüge`, Messung, Constraints und Platzierung. Kommentare oder berechnete, aber ungenutzte Breiten sind kein Geometrienachweis. Unbeschränkte Messung darf die sichtbare Fußzeile nicht über ihre verfügbare Breite hinausziehen.
- Reservierter Fußzeilenplatz, Inhaltsminimum, effektives Knotenrechteck, Anschlüsse und Trefferflächen bleiben konsistent. Inhalt und Anschlüsse dürfen die Anzeige nicht überlagern; Zoom und Schriftgröße dürfen sie weder verschieben noch abschneiden.
- Ohne Messwert erscheint kein erfundener Zeitwert. Die Diagnose bleibt abgeleiteter Laufzeitzustand.

Visuelle Gruppen behalten ihre gesonderte summierte Zeit im Gruppenkopf (`GruppenDarstellung.kt` / `VisuelleGruppenEbene.kt`). Diese ist keine Knotenfußzeile; prüfe Mitgliedschaft und Summierung über den vorhandenen Gruppenpfad.

Bei Änderungen an Renderer, Kontur, Größe oder Messung verlange Vorher-/Nachher-Laufzeitbilder für regulären Knoten, Übersetzer und betroffene Gruppen: kleine/große Abmessungen, verschiedene Zoomstufen, normale/vergrößerte Schrift und ausgewählter Zustand. Prüfe auch den Wechsel von keiner Messung zu sichtbarer Δzeit. Nutze bestehende Geometrie-/Fußzeilen- und Gruppentests; ergänze gezielte Regressionen bei geändertem Layoutvertrag. Build und Formatierungstest ersetzen keine Sichtprüfung. Ohne ausführbare Laufzeitprüfung bleibt die visuelle Abnahme ausdrücklich offen; melde die Position nicht als verifiziert.

## Versionswirkung

Ein neuer, separat erzeugbarer und registrierter Knotentyp ist immer eine `y`-Änderung im Schema `vM.y.x`:

- `y` wird um eins erhöht,
- `x` wird auf `0` gesetzt,
- der `master_verwalter` reserviert die Version vor Produktionsimplementierung.

Änderungen an einem vorhandenen Knotentyp ohne neuen Typ-Schlüssel sind `x`-Änderungen. Dazu gehören insbesondere neue Anschlüsse, Parameter, Inspector-Felder, Renderer, Auswertungsfälle und Sonderfälle. Enthält ein Release neue Knoten und weitere Änderungen, gilt der gesamte Release als `y`-Version.

Ein Implementierungsagent darf weder einen neuen Knotentyp in eine reservierte `x`-Version aufnehmen noch selbstständig die Versionsnummer ändern. Bei abweichendem Umfang wird zuerst der `master_verwalter` eingeschaltet.

## Ablauf

1. `master_verwalter` prüft Releasezustand, Versionsraum und reserviert anhand des tatsächlichen Katalogumfangs die passende `x`- oder `y`-Version.
2. `node_planner` untersucht vergleichbare Knoten, Registry, Auswerter, Inspector, Persistenz und Tests.
3. Der Plan nennt alle neuen Typ-Schlüssel und enthält einen Abschnitt **Versionswirkung**.
4. `math_reviewer` prüft mathematisch nicht triviale Semantik.
5. `node_implementer` setzt nur den freigegebenen Umfang um.
6. `node_verifier` prüft unabhängig Verhalten, Architektur, Tests, Persistenz und Versionsklassifikation sowie Rollentrennung, Operator-/Kategorieintegration und bei Darstellungsänderungen die Δzeit-Geometrie anhand der Laufzeitnachweise. Rollenvermischung, umgangene bestehende Auswahlpfade und verschobene oder abgeschnittene Zeitrahmen sind blockierende Findings.
7. Blockierende Findings werden korrigiert und erneut verifiziert.

## Pflichtangaben des Plans

- fachliches Ziel und Semantik,
- Rolle (Knoten/Übersetzer/Gruppe), Kontur und vergleichbarer Bestandspfad,
- Operator-ID und Rechnerfamilie beziehungsweise Vorlage, Kategorie und Auffindbarkeit,
- Δzeit-Layoutvertrag und visuelle Abnahme bei Darstellungsänderungen,
- neue Typ-Schlüssel oder ausdrücklich keine neuen Typ-Schlüssel,
- Anschlüsse und Verbindungskompatibilität,
- Auswertung und Fehlerfälle,
- Inspector und Darstellung,
- Persistenz und Migration,
- bei Konsolidierung: Altart/Altoperator, kanonisches Ziel, Verbraucher, Migration, Erhalt von IDs/Metadaten und belegte Entfernungsvoraussetzungen,
- bei Methoden-/Tupel-/Formeländerungen: betroffene Vertragsanforderungen, Ende-zu-Ende-Nachweis und ausdrücklich offene Restpunkte nach der verlinkten Referenz,
- Tests und Prüfbefehle,
- reservierte Version und Begründung für `y` oder `x`.

## Stopbedingungen

Stoppe die Arbeit und fordere eine Entscheidung des `master_verwalter` an, wenn:

- die reservierte Version eine `x`-Version ist, aber ein neuer Typ-Schlüssel benötigt wird,
- ein geplanter neuer Knotentyp entfällt und die `y`-Version dadurch unbegründet wird,
- zusätzliche neue Knotentypen außerhalb des freigegebenen Plans entstehen,
- Releaseplan, Branch oder tatsächlicher Git-Zustand einander widersprechen.
