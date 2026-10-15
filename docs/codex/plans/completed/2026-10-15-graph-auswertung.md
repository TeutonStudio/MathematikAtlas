# Graphauswertung nach Analyse von Blender

## Status

Abgeschlossen und unabhängig abgenommen, 2026-10-15. Auftrag: Blenders Knotensystem untersuchen und nachweisbar geeignete Optimierungen im Mathematik Atlas einbauen.

## Ziel und Nutzerwirkung

Wiederholte Auswertungen großer Karten sollen weniger Graphverwaltung benötigen und weiterhin dieselben Ergebnisse, Fehler und deterministische Reihenfolge liefern. Ungleiche Eingaben mit gleichen Hashwerten dürfen kein altes Ergebnis zurückliefern.

## Nicht-Ziele

Keine neuen Knotentypen, Produktversion, Releaseveröffentlichung, Parallelisierung, Persistenzänderung oder Änderung von UI-/Mathematikverträgen. Kein Kopieren von Blender-Code.

## Untersuchter Istzustand

`KartenAuswerter.auswertenIntern` bildet bei jedem Aufruf Eingangs-/Nachfolgertabellen und eine Kahn-Sortierung. Es sucht jeden Knoten und jede Verbindungsquelle linear. `sammleEingänge` sucht zusätzlich Quellanschlüsse linear. Das kann bei V Knoten und E Verbindungen O(V² + EV) Verwaltungsaufwand erzeugen. Ergebnis-Caches vergleichen lediglich `31 * knoten.hashCode() + eingänge.hashCode()`; Hashkollisionen können falsche Treffer erzeugen. Unterkarten verwenden denselben Auswerter.

Blender 4.5.0 (festes Release als Quellenbasis):

- [node_runtime.cc](https://github.com/blender/blender/blob/v4.5.0/source/blender/blenkernel/intern/node_runtime.cc#L535): `ensure_topology_cache` bereitet Knoten-/Anschlussindizes und topologische Sortierungen vor.
- [node_tree_update.cc](https://github.com/blender/blender/blob/v4.5.0/source/blender/blenkernel/intern/node_tree_update.cc#L49): Änderungsarten und Invalidierung abgeleiteter Daten.
- [lazy_function_graph_executor.cc](https://github.com/blender/blender/blob/v4.5.0/source/blender/functions/intern/lazy_function_graph_executor.cc#L5): bedarfsgesteuerte Auswertung und adaptive Parallelisierung.

Unser Editor trennt Geometrieänderungen bereits von `auswertungsRevision`. Die übertragene Idee ist ein separater Laufzeitplan mit indizierten Zugriffen. Lazy Evaluation und parallele Knotenauswerter benötigen eigene Verträge für sichtbare Fehler, Nebeneffekte und Threadsicherheit und werden nicht eingeführt.

## Fachliche und mathematische Semantik

Alle bisher ausgewerteten Knoten werden weiter ausgewertet, auch unverbundene. Kahn-Sortierung mit lexikographischem ID-Tiebreak bleibt erhalten. Mehrere Kanten zwischen demselben Knotenpaar zählen als eine Abhängigkeit, transportieren aber weiterhin alle Anschlusswerte. Zyklen und fehlende Quellen bleiben Fehler. Standardwerte, Annahmen, Herkunft, Methodenkarten und Zeitmessung bleiben bestehen.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

Keine persistierten Felder ändern sich. Die strukturelle Identität des Laufzeitplans enthält geordnete Knoten-IDs und vollständige Verbindungen. Aktuelle Knoten-/Anschlussdaten werden pro Aufruf neu indiziert, sodass umbenannte/dynamische Anschlüsse nie aus einem alten Snapshot gelesen werden. Ergebnisidentität vergleicht Knoten und Eingabewerte vollständig, zusätzlich zur Identität des Laufzeitplans. Strukturänderungen invalidieren Ergebnisse konservativ.

## Architekturentscheidungen

Der Plan bleibt intern im bestehenden Adapter, in einem begrenzten Cache je Kartenverweis. Keine neue Produktionsabhängigkeit. Android und Desktop verwenden denselben Adaptercode. Details und Alternativen werden im zugehörigen ADR festgehalten.

## Betroffene Dateien und Symbole

- `MathematikKartenAdapter/.../KartenAuswerter.kt`: Ablauf und Ergebnis-Cache.
- Neue interne Plan-/Indexklassen im Adapter, wenn für Übersicht erforderlich.
- Adaptertests für Wiederverwendung, strukturelle Änderungen, Randfälle und begrenzte Listen-Zugriffe.
- Analyse/ADR unter `docs/codex/decisions/`.

## Meilensteine

- [x] Blenderquellen und Bestand untersucht.
- [x] Plan, Indizes und exakte Ergebnisidentität implementiert; 13 neue Regressionstests grün.
- [x] Regressionen, Aufwandsnachweis und Android-/Desktop-Prüfungen: 1.753 Tests ohne Fehler/Überspringen; Android-Debug-Build erfolgreich.
- [x] Unabhängige Prüfung durch `node_verifier`: Speicher-Finding behoben; finale Abnahme ohne offene Findings erteilt.

## Umsetzungsschritte

1. Regressionstests vor der Änderung ausführen und die bisherigen Fehler/Listen-Zugriffe nachweisen.
2. Graphplanung vom Durchlauf trennen und begrenzt wiederverwenden.
3. Knoten-/Anschlusszugriffe auf aktuelle Maps umstellen; Reihenfolge und Fehler erhalten.
4. Int-Signatur durch vollständigen Vergleich ersetzen.
5. Gezielte Tests, Gesamtprüfungen und Builds ausführen; Review durchführen.

## Tests und Validierung

Bestehende Adaptertests plus Tests für Hashkollisionen, Ketten/Fan-out, Mehrfachkanten, Zyklen, fehlende Quellen/Anschlüsse, Knoten-/Portänderungen, Unterkarten, Cache-Löschung und Undo-ähnliche Snapshots. Deterministischer Aufwandstest zählt tatsächliche Listenzugriffe statt flüchtiger Zeitlimits. Geplante Befehle: Repository-/Releaseprüfungen, `:MathematikKartenAdapter:testDebugUnitTest`, anschließend `test :app:assembleDebug` mit aktiviertem Cloud-Setup und Xvfb. Keine Android-Geräteprüfung behaupten.

## Persistenz und Migration

Plan und Indizes sind reine Laufzeitdaten; kein JSON-Schemawechsel und keine Migration. Alte Karten werden wie bisher aus ihren Daten geplant. Rückkehr zu früheren Snapshots muss sicher funktionieren.

## Risiken und Rückfallstrategie

Veraltete Pläne, unbegrenzter Speicher, verdeckte Kontextabhängigkeiten und veränderte Reihenfolgen: struktureller Vergleich, begrenzte Anzahl von Plänen, Invalidierung alter Ergebnisreferenzen sowie Regressionen. Rückfall: lokale Änderungen am Adapter und den neuen Tests rückgängig machen; gespeicherte Karten bleiben unverändert.

## Fortschritt

2026-10-15: Implementierung, 13 Regressionen, gesamte Test-/Buildprüfung und unabhängige Abnahme abgeschlossen. Ein zunächst falsch erwarteter Cachetreffer bei 21 verschachtelten Karten wurde nach Prüfung des 16er-Limits auf korrekte zwei Basisberechnungen korrigiert.

2026-10-15: Projektanweisungen und Blender 4.5.0-Quellen gelesen, Vergleich eingegrenzt. Arbeitsbranch `work`, Ausgangscommit `e535190058014810617ea2989d27838afe54692b`; keine Branch-/Commit-/Releaseänderung beauftragt.

## Entscheidungsprotokoll

2026-10-15: Graphplanung optimieren, alle Knoten weiter auswerten. Alternative Lazy Evaluation verworfen für diesen Auftrag, weil sie bisherige Fehler-/Vorschauverträge ändern würde. Vollständiger Vergleich statt Hashgleichheit sichert Ergebnis-Cache ab.

## Abweichungen

Der zusätzlich nachgewiesene veraltete Reihenfolgekontext wurde durch Einbezug der Planidentität in den Ergebnisvergleich behoben. Das Review fand den Sonderfall nachträglich gespeicherter Ergebnisse bereits verdrängter Pläne; ein Residenztest vor dem Speichern behebt ihn.

## Ergebnis und Verifikation

- Vor Änderung: Acht neue Tests ausgeführt, drei bewusst nachweisbare Fehlschläge: Hashkollision (`Aa`/`BB`), veralteter Reihenfolgekontext und 161.199 statt linearer Knoten-Listenzugriffe bei 400 Knoten.
- Nach Änderung: 13 neue Tests erfolgreich. 400er-Kette: 1.200 Knoten-Listenzugriffe kalt/warm; Fan-out mit 300 Quellports: 1.500 kalt/1.200 warm. Die Tests zählen Eingabelistenzugriffe; es wurde kein pauschaler App-Laufzeitgewinn gemessen.
- Gezielter Adapterlauf erfolgreich; vollständiger Lauf `bash gradlew test :app:assembleDebug --console=plain` erfolgreich in 54 Sekunden (149 Tasks, 21 ausgeführt, 128 unverändert wiederverwendet). XML-Berichte: 1.753 Tests, 0 Fehler, 0 übersprungen; davon Adapter 66 und Desktop 20. Unveränderte Prüfergebnisse wurden von Gradle berechtigt wiederverwendet.
- `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py`, `pruefe_standardkarten.py` und `git diff --check` erfolgreich.
- Android- und Desktop-Produktionsquellen kompiliert, Android-Debug-APK erzeugt. Keine Android-Emulator-/Geräteprüfung; für die interne Adapteroptimierung durch gemeinsame Unit-/Integrationstests abgedeckt und als zusätzliche Plattformprüfung ausdrücklich ungetestet.
- `node_verifier` hat den Endstand samt ADR, Cachebegrenzung und Tests unabhängig abgenommen. Keine offenen Findings.
- Architekturentscheidung und Blenderquellen: [ADR](../../decisions/2026-10-15-indizierte-graphauswertung.md).
- Kein neues Datenformat, Knotentyp oder Release; keine Versionsmetadaten geändert. Änderungen verbleiben ohne Commit auf `work` über Basis `e535190058014810617ea2989d27838afe54692b`. Basisautor Alexander, Basiscommitter GitHub; kein neuer SamAI-/Connector-Commit erzeugt.
