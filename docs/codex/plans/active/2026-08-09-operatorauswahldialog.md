# Gemeinsamer Operatorauswahldialog für Rechnerfamilien

## Status

Aktiv. Issue #359 ist für Version v2.28.6 als x-Änderung reserviert. Arbeitsbranch ist `samai/v2.28.6/operatorauswahldialog`, Integrationsbranch ist `release/v2.28.6-operatorauswahldialog`, Basis ist `17819fedaa735b851657cb9c0c8b776f88e3296a` (`v2.28.5`).

## Ziel und Nutzerwirkung

Die unmittelbaren Dropdown-Wechsel in den Rechner-Inspectoren werden durch einen großen gemeinsamen Dialog ersetzt. Nutzer können Operatoren der aktuellen Rechnerfamilie durchsuchen, kategorisieren, deren Ein- und Ausgangssignatur prüfen und vor dem Übernehmen sehen, welche Anschlüsse sowie tatsächlich verbundenen Verbindungen entfallen. Eine Kachelauswahl verändert den Graph noch nicht. Zahlen-, Aussage-, Vektor-, Matrix- und aktive Tensorrechner verwenden denselben bestätigten Ablauf.

## Nicht-Ziele

- keine neuen mathematischen Operatoren oder Knotentypen,
- kein Wechsel der Rechnerfamilie,
- keine neue Tensor-Formelsemantik,
- keine Persistenz von Such-, Kategorie- oder Dialogzustand,
- keine Änderung der Auswertungssemantik,
- kein PR, Merge oder Releaseabschluss in diesem Arbeitsauftrag.

## Untersuchter Istzustand

- `ZahlenRechnerInspektor.kt` listet 40 `UniversellerZahlenOperator`, 12 `ErweiterterZahlenOperator` und den Formelmodus in einem sofort mutierenden Dropdown.
- `StrukturRechnerInspektor.kt` verwendet dasselbe Muster für Aussage, Vektor und Matrix sowie deren Formelpfade.
- `TensorOperationRechnerInspektor.kt` listet 22 Einträge aus `StandardTensorOperationen.registry` und besitzt eine eigene Warnung anhand entfallender Anschlüsse.
- `RechnerFamilienKatalog.kt` kennt nur die universellen Zahlenoperatoren und die historische kleinere Tensorliste.
- `KartenAktion.KnotenErsetzen` entfernt Verbindungen zu Anschluss-IDs, die im neuen Knoten nicht mehr vorhanden sind.
- Der Inspectorvertrag stellt aktuell nur Knotenersetzung, aber keine fachneutrale Auswirkungsvorschau bereit.

## Fachliche und mathematische Semantik

Der Dialog führt keine Mathematik aus. Ein Auswahleintrag beschreibt einen bereits vorhandenen Operatorvertrag. Kandidaten entstehen ausschließlich über die vorhandenen Konfigurationsfunktionen `konfiguriereStandardZahlenRechner`, `konfiguriereErweitertenZahlenRechner`, `konfiguriereStrukturRechner` und `konfiguriereTensorOperation`. Formelzustände werden durch die vorhandenen Formelbauer als Kandidaten erzeugt und erst nach einer zweiten Bestätigung samt Auswirkungsvorschau übernommen.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

- Knotenart und Rechnerfamilie bleiben unverändert.
- Kandidaten behalten die Knoten-ID.
- Erhalten, hinzugefügt oder entfernt wird ausschließlich anhand stabiler Anschluss-IDs bestimmt.
- Eine Verbindung ist betroffen, wenn einer ihrer Verweise auf den zu ersetzenden Knoten und eine im Kandidaten fehlende Anschluss-ID zeigt.
- Die reine Vorschau verwendet dieselbe Regel wie `KartenAktion.KnotenErsetzen` und mutiert die Karte nicht.
- Erst die bestätigte Knotenersetzung erzeugt einen Undo-/Redo-Schritt.

## Architekturentscheidungen

- Die Auswirkungsvorschau liegt fachneutral im Modul `KnotenKartenVerwalter`.
- Der gemeinsame Dialog und sein kurzlebiger Zustand liegen im App-Modul.
- Die Dialogoberfläche erhält fertige, UI-neutrale Auswahlmodelle und Kandidaten; sie erzeugt keine Anschlüsse selbst.
- Die aktive Tensorregistry bleibt maßgeblich.
- Bestehende Formelpfade bleiben eigenständige Dialoge, deren Ergebnis in den gemeinsamen Bestätigungsablauf zurückkehrt; der Tensorpfad erhält keine neue Formelkachel.

## Betroffene Dateien und Symbole

- neu: `KnotenErsetzungsVorschau.kt` und zugehörige Tests,
- neu: `RechnerOperatorAuswahlModell.kt`, `RechnerOperatorAuswahlDialog.kt` und Tests/Previews,
- `KnotenInspektoren.kt` und `KnotenInspektorFenster.kt` für den Vorschaukanal,
- `ZahlenRechnerInspektor.kt`, `StrukturRechnerInspektor.kt`, `TensorOperationRechnerInspektor.kt`,
- `RechnerFamilienKatalog.kt` nur soweit der tatsächliche gemeinsame Metadatenvertrag ohne parallele Ausführungsregistry erweitert werden kann,
- `release/roadmap.toml` für die geplante v2.28.6-Reservierung.

## Meilensteine

- [x] M1: Version, Branch und ExecPlan reservieren.
- [x] M2: Fachneutrale Ersetzungsvorschau samt Tests einführen.
- [x] M3: Gemeinsames Such-, Kategorie-, Signatur- und Dialogmodell implementieren.
- [x] M4: Zahlen-, Aussage-, Vektor-, Matrix- und Tensorinspector anbinden; Formel-Handoff erhalten.
- [-] M5: responsive Produktionsoberfläche, Previews und Accessibility ergänzen.
- [-] M6: gezielte und vollständige Prüfungen ausführen, Diff und Identität verifizieren.

## Konkrete Umsetzungsschritte

1. Releaseplan und aktiven Plan in einem ersten SamAI-Commit festhalten.
2. `KnotenErsetzungsAuswirkung` aus aktueller Karte und Kandidatenknoten berechnen und gegen `KnotenErsetzen` testen.
3. einen gemeinsamen Auswahleintrag mit ID, Titel, Symbol, Kategorie, Beschreibung, Suchbegriffen, Ein-/Ausgangsrollen, Status, Kandidat und Aktionsart modellieren.
4. Filterung als reine Funktion testen, einschließlich Groß-/Kleinschreibung und deutscher Sonderzeichen.
5. einen großen Dialog mit lokaler Entwurfsauswahl, Kategorieauswahl, Suche, adaptivem Raster beziehungsweise kompakter Liste und Detailbereich bauen.
6. Inspectoraktionen um eine reine Auswirkungsvorschau ergänzen.
7. die drei bestehenden Inspectorpfade auf den gemeinsamen Dialog umstellen und die Tensor-Sonderwarnung entfernen.
8. Formelkacheln in Zahlen-, Aussage-, Vektor- und Matrixfamilie an die vorhandenen Formelbauer übergeben; Mutation erst bei deren Bestätigung.
9. repräsentative Debug-Previews und fokussierte Tests ergänzen.
10. alle Pflichtprüfungen ausführen und Planfortschritt sowie Ergebnis aktualisieren.

## Tests und Validierung

- gezielte Tests für `KnotenErsetzungsAuswirkung`,
- reine Tests für Suche, Kategorien und Auswahlmodell,
- bestehende Rechner-, Formel-, Tensor- und Knotenersetzungstests,
- `python3 scripts/pruefe_releaseplan.py`,
- `python3 scripts/pruefe_versionsfolge.py`,
- `python3 scripts/pruefe_repository.py`,
- `python3 scripts/pruefe_kern.py`,
- `./gradlew test`,
- `./gradlew :app:assembleDebug`,
- Compose-Interaktion nur als nicht manuell verifiziert ausweisen, sofern kein Emulator oder Gerät verfügbar ist.

## Persistenz und Migration

Es werden keine neuen persistierten Felder eingeführt. Bestehende Operator-IDs, Formelzustände und Anschluss-IDs bleiben erhalten. Öffnen oder Abbrechen des Dialogs verändert keine Karte. Eine Formatmigration ist nach aktuellem Plan nicht erforderlich.

## Risiken und Rückfallstrategie

- Drift zwischen UI-Listen und aktiven Registern wird durch Ableitung aus den vorhandenen Quellen und Vollständigkeitstests begrenzt.
- Veraltete Vorschauen werden beim Bestätigen erneut aus dem aktuellen Kartenstand abgeleitet.
- Formelmodi dürfen nicht vorzeitig mutieren; bei Problemen bleibt der vorhandene Formelbauerpfad unverändert und die Kachel wird zurückgenommen.
- Der Dialog kann vollständig entfernt und die bisherigen Dropdowns wiederhergestellt werden, ohne persistierte Daten zurückzurollen.

## Fortschritt

- 2026-08-09: Repository, Issue #359, Dialogpfade, Operatorquellen, Releasebasis und Versionsachse untersucht.
- 2026-08-09: v2.28.6 als geplante x-Version und Branchstruktur festgelegt.
- 2026-08-09: Fachneutrale Ersetzungsvorschau, gemeinsames Auswahlmodell und responsive Oberfläche implementiert.
- 2026-08-09: Zahlen-, Aussage-, Vektor-, Matrix- und aktive Tensorrechner auf den gemeinsamen Dialog umgestellt; unbekannte gespeicherte IDs werden sichtbar diagnostiziert.
- 2026-08-09: Formel-Handoff nach unabhängiger Prüfung auf Kandidat, Auswirkungsvorschau und zweite Bestätigung umgestellt. Der hohe Befund ist geschlossen.

## Entscheidungsprotokoll

- 2026-08-09: Alle fünf Rechnerfamilien verwenden denselben Dialog. Alternative war ein Zahlenrechner-Prototyp; verworfen wegen fortgesetzter paralleler UI-Pfade.
- 2026-08-09: Kachelauswahl ist ein lokaler Entwurf und benötigt ausdrückliches Übernehmen. Alternative Sofortmutation wurde wegen möglicher Verbindungsverluste verworfen.
- 2026-08-09: `Eigene Formel` bleibt eine hervorgehobene Auswahlaktion bestehender Formelpfade. Für die aktive Tensorregistry wird kein neuer Formelmodus erfunden.
- 2026-08-09: v2.28.6 bleibt eine x-Version, weil kein neuer registrierter und separat erzeugbarer Knotentyp entsteht.

## Abweichungen vom ursprünglichen Plan

- Der vollständige UI-neutrale Familienkatalog ist noch nicht zentralisiert. Die Inspectoren leiten ihre Kandidaten bereits aus den aktiven Operatorquellen ab, halten ergänzende UI-Metadaten aber vorerst lokal.
- Die Produktionsoberfläche besitzt Escape, `Ctrl+F`, `Alt+Enter` und Auswahlsemantik. Explizite Pfeiltastennavigation im Raster, Fokusrückgabe an das öffnende Operatorfeld und vollständig beschreibende Kachelsemantik bleiben offen.
- Debug-Previews decken breite und kompakte Zahlenrechnerzustände ab. Zustände aller fünf Familien, leere Suche und unbekannte ID sowie Inspector-Interaktionstests bleiben offen.

## Ergebnis und Verifikation

Ein erster integrierter Dialogstand liegt auf `samai/v2.28.6/operatorauswahldialog`. Die fünf Rechnerfamilien verwenden denselben Auswahl- und Bestätigungsablauf. Auswirkungsvorschau und Modell besitzen fokussierte Tests; Formelkandidaten mutieren die Karte erst nach der abschließenden Bestätigung.

- `python3 scripts/pruefe_repository.py`: erfolgreich.
- `python3 scripts/pruefe_releaseplan.py`: erfolgreich, 82 Einträge, aktuelle Version bleibt 2.28.5.
- `python3 scripts/pruefe_versionsfolge.py`: erfolgreich.
- `git diff --check`: erfolgreich.
- `bash scripts/samai-git.sh verify HEAD`: erfolgreich.
- GitHub Actions `Android-Build` Lauf 31306438437: erfolgreich einschließlich Architekturprüfung, `./gradlew test` und `:app:assembleDebug` für den Stand vor der Formel-Handoff-Korrektur.
- Die Formel-Handoff-Korrektur wurde unabhängig nachgeprüft; keine neuen oder blockierenden Probleme.
- `python3 scripts/pruefe_kern.py` ist lokal ohne `kotlinc` nicht ausführbar. Der lokale Gradle-Lauf kann die Android-Plugin-Abhängigkeiten in dieser Umgebung nicht beziehen; deshalb ist GitHub Actions die vollständige Build- und Testinstanz.
