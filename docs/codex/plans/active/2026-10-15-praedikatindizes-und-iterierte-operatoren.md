# Prädikatindizes und Lösungsmengen iterierter Operatoren

## Status

2026-10-15: Bestand untersucht; Umsetzung wartet auf zulässige Versionszuordnung durch `master_verwalter`. Keine Produktänderung aus dieser Aufgabe. Arbeitsbranch bei Untersuchung: `samai/v3.4.0/methodenknoten`, HEAD `c4b23c69c431370b600cd4bd810c8d5135c43ff7` (veröffentlichte v3.3.1). Umfangreiche bereits vorhandene uncommittete Änderungen sind zu bewahren. Dieser Plan reserviert keine Version.

## Ziel und Nutzerwirkung

Alle vorhandenen iterierten Operatoren unterstützen einen typisierten Prädikatindex. Definition, Bestimmbarkeit der Lösungsmenge und Verwendbarkeit des Operators erhalten getrennte, begründete Rückmeldungen. Mengen können Indexelemente sein. Die bestehenden Lösungsmenge- und Peano-Pfade sowie ausführbare Definitionskarten werden erweitert. Beide Definitionsvarianten liefern für Start 1 die projektkonforme ℕ und für Start 0 die ℕ₀.

## Nicht-Ziele

Keine neuen Knotentypen, Register, Produktionsabhängigkeiten oder Dialoge. Keine Veröffentlichung. Keine allgemeine automatische Konvergenzentscheidung. Fremde Methoden-, Visualisierungs-, Geometrie- und IDE-Änderungen gehören nicht zum Aufgabenumfang.

## Untersuchter Istzustand

- `MathematikRechenSystem/.../kern/IterierteOperatoren.kt` modelliert acht symbolische Varianten über `methode` und `indexMenge`. Zahlen- und Mengenfaltungen erzwingen teilweise `ZahlAusdruck` für Indexelemente; die Aussagenfaltung bindet bereits allgemeine Werte. Das ist der vergleichbare vorhandene Methodenbindungspfad.
- `PraedikatsMengen.kt` besitzt `MethodenParameter`-Implementierungen für Mengen, Aussagen und allgemeine typisierte Elemente sowie `FehlendeObermenge`. `definierePrädikatsMenge` löst einfache Gleichheitsfälle; die symbolische `PrädikatsMenge` trägt keinen expliziten Definitionsbereich.
- `Methoden.kt` enthält Substitution und Parameteranalyse für bestehende Iterationsausdrücke. Beide Pfade müssen den neuen gebundenen Index beachten.
- `MathematikKnoten/.../MathematikAuswerter.kt` liest bei iterierten Knoten `indexmenge`. Der bestehende Auswerter für `mathematik.lösungsmenge` verwendet `freieVariablen()` und bei fehlender Grundmengenangabe ℝ. Dieser Default darf nicht ungeprüft auf allgemeine Prädikatbindungen übertragen werden.
- `AxiomOperatoren.kt` enthält `axiom.peano.null`, Nachfolgerabschluss, Nachfolgerfreiheit, Injektivität und eine Induktionsinstanz. Namen und Formeltexte sind keine Beweiszertifikate.
- `app/.../konzepte/IndexRechnerDefinitionsKarten.kt` ist ein vorhandener verbundener Definitionskartenpfad. `ZahlbereichsDefinitionen.kt` und die zugehörigen Tests enthalten die Zahlbereichskonventionen.
- Die Roadmap reserviert v3.4.0 als y-Version für `mathematik.methode`. Der neue Auftrag erweitert bestehende Typen und ist für sich eine x-Änderung. Eine gesonderte Version darf keinen versionsübergreifenden Branchstapel erzeugen.

## Fachliche und mathematische Semantik

Der gemeinsame Index besteht aus gebundenem `MethodenParameter`, explizitem beziehungsweise aus der Signatur belegtem Definitionsbereich und `Aussage`. Historische Indexmengen werden als Mitgliedschaftsprädikat eingebettet. Ein fehlender Bereich wird als fehlende Voraussetzung geführt, niemals als Universalmenge.

Endliche Indizes werden nur bei vollständig entschiedener Mitgliedschaft und ausreichendem Budget gefaltet. Offene Entscheidungen, Definitionslücken oder Budgetabbruch erhalten den vollständigen Originalausdruck; Teilergebnisse dürfen keinen vollständigen Wert vortäuschen. Unendliche Familien bleiben symbolisch, außer eine strukturierte Kernregel belegt die Vereinfachung. Indexgültigkeit sagt nichts über Summen- oder Produktkonvergenz aus.

Ein Lösungsvorschlag wird im Definitionsbereich auf Äquivalenz zu `P` geprüft: bestätigt nur bei Beweis, widerlegt bei nachgewiesenem Gegenbeispiel, andernfalls unbewiesen. Ein unbekannter Wahrheitswert wird nie zu falsch. Vorschläge begrenzen nicht stillschweigend den Suchbereich.

Die Regel für induktive Obermengen prüft strukturell Träger ℝ, Start 0 oder 1, Nachfolger x↦x+1, Startmitgliedschaft, Abschluss und Identitätsrumpf. Die kleinste abgeschlossene Startmenge ist Teil jeder Familie; sie ist selbst Familienmitglied. Diese beiden Inklusionen begründen den Schnitt. Die vollständige Peano-Variante quantifiziert über alle Teilmengen des Kandidaten und macht die Familie einelementig. Eine einzelne Induktionsinstanz oder multiplikative Neutralität genügt nicht.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

- Bestehende Typ-Schlüssel und Anschluss-IDs erhalten. Je iteriertem Knoten optionaler Eingang für eine einstellige aussagewertige Methode; `indexmenge` bleibt lesbar. Beide verbunden: konkreter Mehrdeutigkeitsfehler. Keiner verbunden: fehlende Indexdefinition.
- `mathematik.lösungsmenge` bekommt den optionalen Mengeneingang `bekannteLösungsmenge` und allgemeine typisierte Bindungen. Bestehende Konfigurationen brauchen einen nachgewiesenen Kompatibilitätspfad.
- Ergebnisanforderungen von Zahlen-, Mengen- und Aussagenoperatoren bleiben bestehen. Allgemeine Indexelemente verwenden den bestehenden Methodenvertrag und dessen Typprüfung.
- Definition: gültig / ungültig / unter offenen Voraussetzungen. Lösungsmenge: explizit bestimmt / exakt symbolisch / noch ungeklärt. Operator: auswertbar / symbolisch verwendbar / nicht verwendbar. Jeder Status erhält einen konkreten Grund aus Kerndaten.
- Mächtigkeit und Aufzählbarkeit werden nur mit Nachweis angegeben. Unendlich impliziert weder überabzählbar noch eine bekannte Aufzählung.
- Änderungen laufen über bestehende Kartenaktionen. Diagnosen, Begründungen und Lösungscaches sind abgeleiteter Laufzeitzustand.

## Architekturentscheidungen

Siehe ADR-Entwurf `docs/codex/decisions/2026-10-15-praedikatindizes-und-nachweisstatus.md`. Semantik und Beweise liegen im Rechenkern; Adapter und Knoten vermitteln Werte, UI zeigt dieselben strukturierten Ergebnisse und die bestehende gemeinsame Δzeit-Fußzeile.

## Betroffene Dateien und Symbole

Kern: `IterierteOperatoren.kt`, `PraedikatsMengen.kt`, `Methoden.kt`, `MathematischerWertCodec.kt`, `AxiomOperatoren.kt`, `ZahlbereichsDefinitionen.kt`, vorhandene Lösungsverfahren und Typprüfung. Knoten: `MathematikAuswerter.kt`, `MathematikKnotenVorlagen.kt`, `AussagenLogikKnoten.kt`, effektive Anschlussverträge, `MathematikKnotenRenderer.kt`, `migration/MathematikKartenCodec.kt`. Adapter: Kartenmethodenprüfung, `KartenAuswerter.kt`, `MethodenAufrufAuswerter.kt` und Cacheidentität. App: bestehende Mengen-/Methodeninspektoren und Definitionskarten. Desktop: gemeinsame Shadowmodule und Laufzeittests. Die genaue Änderungsliste wird nach zulässiger Reservierung anhand der Aufrufpfade eingegrenzt.

## Meilensteine

- [x] Bestand und vergleichbare allgemeine Aussagenbindung untersucht.
- [-] Releaseaudit: lokale Basis und aktive v3.4.0 identifiziert; verbindliche Zuordnung noch ausstehend.
- [ ] Strukturierter Index, Statusmodell, Lösungsvorschlagsprüfung und Kernregressionen.
- [ ] Alle Operatorfamilien einschließlich Mengenindizes, leeren Familien und Budgets.
- [ ] Strukturierte Regeln für beide Zahlbereiche und beide Definitionsvarianten.
- [ ] Knoten, Kartenmethoden, Codec, historische Verbindungen, Cache und Undo/Redo.
- [ ] Gemeinsame Formel-/Inspector-Diagnosen und verbundene Definitionskarten.
- [ ] Vollständige Prüfungen und getrennte Android-/Desktop-Laufzeitabnahme.
- [ ] Abschlussdiff, erneute x-Klassifikation und unabhängige Abnahme.

## Konkrete Umsetzungsschritte

1. `master_verwalter` legt zulässige Version und Branch fest. Vor Produktänderungen den Ausgangsdiff aller bereits geänderten relevanten Dateien sichern; keine fremden Änderungen stagen.
2. Vor Inspectorarbeiten `.agents/skills/inspektor-aendern/SKILL.md` lesen. Gemeinsame Indexstruktur und konservative Statusableitung zuerst im Kern implementieren und testen.
3. Bestehende Mengen-APIs als kompatible Adapter erhalten. Substitution, freie Parameter, strukturelle Gleichheit und Wertcodec gemeinsam erweitern; Bindung darf freie Parameter nicht einfangen.
4. Vollständige endliche Familien auswerten; Aufwand von kartesischen Produkten vor Expansion begrenzen. Bei Abbruch den vollständigen Ausdruck erhalten.
5. Lösungsvorschläge getrennt von eigentlichen Lösungsverfahren prüfen. Gegenbeispiele und Beweisgrenzen als Daten führen.
6. Nachweisregeln und Quantifizierung für induktive beziehungsweise vollständige Peano-Familien ergänzen, dann die bestehenden Definitionskarten verbinden.
7. Vorlagen, Methodenprüfung und Migrationspfade erweitern; Formel und Inspector beziehen sich auf dieselbe Auswertung. Historische Kartenversionsreferenzen unverändert lassen.
8. Gezielte Tests, Gesamttests, Lint, Builds und Laufzeitprüfungen ausführen, Enddiff gegen Ausgangsdiff abgleichen und Einschränkungen dokumentieren.

## Tests und Validierung

Gezielte Ausgangspunkte sind `IterierteOperatorenTest`, `IterierteAussagenOperatorenTest`, `IndexiertesKartesischesProduktTest`, Zahlbereichs-, Lösungsmenge-, Kartenmethoden- und Codec-Tests. Ergänzungen prüfen endliche Prädikate, Mengenindizes, leere Familien, Typfehler, offene Parameter, fehlende Bereiche, Definitionslücken, unbekannte Wahrheitswerte und Budgetabbrüche. Lösungsvorschläge erhalten je einen bestätigten, widerlegten und unbewiesenen Fall; der unbewiesene Fall darf keine Vereinfachung auslösen.

Referenzfälle: alle vier Schnitte liefern ℕ beziehungsweise ℕ₀; ℝ erfüllt beide Abschlussprädikate, aber keine vollständige Peano-Minimalität; einzelne Induktionsinstanzen belegen keine vollständige Induktion. Gegenbeispiele umfassen falschen Träger, falschen Start, falschen Nachfolger und fehlende Startmitgliedschaft.

Integration prüft alle acht Operatorvarianten, Prädikat-/Mengenindex-Mehrdeutigkeit, allgemeine Kartenparameter, versionsfeste Referenzen, Cacheinvalidierung, JSON-Roundtrip, historische Anschluss-IDs, Kopieren und atomare Undo/Redo-Aktionen.

Nach Umsetzung ausführen:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/test_release_guards.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :KnotenKartenVerwalterDesktop:test :MathematikKartenAdapterDesktop:test :MathematikKnotenDesktop:test :desktopApp:test
./gradlew :KnotenKartenVerwalter:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Desktoplaufzeit mit den vorhandenen Compose-Test-/Smoke-Pfaden aus `.github/workflows/desktop-linux.yml` prüfen. Visuell auf Android und Desktop: Prädikatindex, alle drei Diagnosegruppen, Einermenge der Peano-Familie und vollständige Δzeit-Fußzeile bei kleinen Größen und vergrößerter Schrift. Build und visuelle Abnahme ausdrücklich getrennt berichten. Keine alten Prüfergebnisse als Ergebnis dieser Aufgabe übernehmen.

## Persistenz und Migration

Gemeinsamen Android-/Desktop-Codec erweitern; historische Mengenindizes und Methodenversionsreferenzen behalten. Neue optionale Anschlüsse dürfen bestehende Anschluss-IDs nicht neu erzeugen. Speichern, Laden, Kopieren und Undo/Redo müssen neue Konfigurationen erhalten. Laufzeitdiagnosen und Lösungscaches bleiben außerhalb gespeicherter Karten. Ob ein Formatsprung nötig ist, entscheidet erst der konkrete persistierte Vertrag, nicht dieser Plan.

## Risiken und Rückfallstrategie

Aktive niedrigere Version und überlappende uncommittete Arbeit verhindern eine ungeprüfte eigenständige Versionsreservierung. Keine Stashes, Resets oder Sammelcommits verwenden. Bei fachlich offenen Entscheidungen symbolische Ausdrücke erhalten; bei Implementierungsproblemen ausschließlich eigene Änderungen zurücknehmen. Explosive Produktgrößen und Quantorprüfung benötigen explizite Budgets. Die vorhandene Grundmengen-Konvention mengenwertiger Methoden muss mit der Identität über Mengen abgestimmt werden, ohne stillen API-Bruch.

## Fortschritt und Entscheidungsprotokoll

2026-10-15: Vorbereitung auf tatsächlichem Arbeitsstand begonnen. Fachlicher Umfang als x bestätigt; kein neuer Typ geplant. Releasezuordnung vor Produktänderungen ist zwingend. Zwei vorbereitende Dokumente angelegt, keine Produktionsdateien geändert.

## Abweichungen vom ursprünglichen Plan

Implementierung noch nicht begonnen, da die verlangte vorgezogene Versionszuordnung offen ist. Keine Abnahmekriterien gestrichen.

## Ergebnis und Verifikation

Noch kein implementiertes Verhalten und keine Laufzeitabnahme. Ausgeführt am 2026-10-15: `git diff --check`, `python3 scripts/pruefe_repository.py`, `python3 scripts/pruefe_releaseplan.py`, `python3 scripts/pruefe_versionsfolge.py` und `python3 scripts/test_release_guards.py`: erfolgreich, einschließlich aller 14 Guardregressionen. Die dort ausgegebenen Fehlermeldungen gehören zu erwarteten Negativfällen. Der Releaseplan prüft 106 Einträge mit aktueller veröffentlichter Version 3.3.1; die Versionsfolge bestätigt den bestehenden Arbeitsbranch der reservierten 3.4.0. Das belegt keine Reservierung für diesen Auftrag. Produkt-/JVM-Tests, Lint, Builds und Android-/Desktop-Laufzeitprüfungen wurden für diesen Auftrag noch nicht ausgeführt, da die Produktimplementierung auf die Versionszuordnung wartet. Kein neuer Commit, Push oder Release. Der Plan bleibt aktiv, bis alle Abnahmekriterien nachgewiesen und die unabhängige Prüfung abgeschlossen sind.
