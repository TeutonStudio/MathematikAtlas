# Indizierte Graphauswertung mit wiederverwendbarer Topologie

- Status: akzeptiert
- Datum: 2026-10-15
- Bezug: [ExecPlan](../plans/completed/2026-10-15-graph-auswertung.md)

## Kontext und Blender-Vergleich

Die Untersuchung bezieht sich auf den veröffentlichten Blender-Stand **4.5.0**. Blender besitzt unterschiedliche Node-Auswertungssysteme; das Verhalten des Geometry-Nodes-Lazy-Executors ist nicht pauschal das Verhalten aller Blender-Node-Editoren.

| Blender-Mechanismus / Quelle | Mathematik Atlas | Entscheidung |
|---|---|---|
| [`ensure_topology_cache`](https://github.com/blender/blender/blob/v4.5.0/source/blender/blenkernel/intern/node_runtime.cc#L535) erzeugt Indizes, Verknüpfungsinformationen und topologische Sortierungen | Der Adapter hat bisher bei jeder Auswertung sortiert und Knoten/Ports linear gesucht | Strukturellen Plan wiederverwenden und Datenzugriffe indizieren |
| [Änderungsarten](https://github.com/blender/blender/blob/v4.5.0/source/blender/blenkernel/intern/node_tree_update.cc#L49) markieren abgeleitete Daten als ungültig | Unveränderliche Kartensnapshots und `auswertungsRevision` trennen bereits Editor-Geometrie von Auswertung | Vorhandenen Aktionsweg beibehalten; Plan anhand tatsächlicher IDs und Kanten prüfen |
| [Lazy Executor](https://github.com/blender/blender/blob/v4.5.0/source/blender/functions/intern/lazy_function_graph_executor.cc#L5) berechnet nur benötigte Werte | Auch unverbundene Knoten zeigen Ergebnisse/Fehler; Auswerter erwarten eingesammelte Eingänge | Kein stilles Überspringen sichtbarer Knoten/Zweige; bedarfsgesteuerte Auswertung braucht einen gesonderten Produkt-/Auswertervertrag |
| Derselbe Executor schaltet bei Bedarf von Einzelthread auf parallele Verarbeitung um | Kleine Kotlin-Auswerter, gemeinsamer veränderlicher Cache und rekursive Kartenaufrufe | Keine Parallelisierung ohne Lastmessung und neuen Threadsicherheitsvertrag |

Die Implementierung übernimmt die beschriebenen Architekturideen; es wird kein Blender-Quellcode eingebunden. Keine neue Bibliothek oder Toolchain ist nötig.

## Entscheidung

`GraphAuswertungsPlan` im bestehenden `MathematikKartenAdapter` speichert eingehende Verbindungen und die deterministische Kahn-Reihenfolge. Der Auswerter hält höchstens 16 Pläne insgesamt in einem LRU-Cache, indiziert nach `KartenVerweis`. Ein Treffer verlangt geordnete Knoten-IDs und vollständige Verbindungsgleichheit. Parameter, Portnamen, Geometrie und Ergebnisse sind nicht Teil des Plans. Ein Plan ist bewusst keine `data class`: Seine Objektidentität bezeichnet die gültige Struktur-/Reihenfolgegeneration für Ergebnisse.

`GraphAuswertungsIndex` bildet Knoten und Anschlüsse auf die **aktuellen** Daten des jeweiligen Aufrufs ab. Quellen und Zielports werden über Maps gelesen. So können Parameter oder Ports geändert werden, ohne den Sortierplan neu zu bauen oder alte Portdaten zu verwenden. Bestehende semantische Eingangsreihenfolgen werden weiter anhand der aktuellen Anschlussdaten sortiert.

Der Ergebnis-Cache vergleicht Knoten und Eingabewerte vollständig, zusätzlich zur Planidentität. Ein alleiniger 32-Bit-Hash ist keine Identitätsprüfung. Strukturänderungen invalidieren konservativ sämtliche Ergebnisse dieses Plans, weil Knotenauswerter auch den topologischen Kontext beobachten können. Unabhängige Knoten behalten bei reinen Parameteränderungen ihre Ergebnisse.

Beim Ersetzen/Verdrängen eines Plans werden seine Ergebnisse entfernt. Nach rekursiver Auswertung dürfen nur noch Ergebnisse residenter Pläne gespeichert werden: Tiefe Unterkarten können den äußeren Plan während seiner Berechnung verdrängen. `leereCache()` entfernt Ergebnis- und Plancaches; `verwerfeCache(id)` bleibt auf den gewünschten Knoten beschränkt.

## Alternativen

Ein einzelner letzter Plan wäre kleiner, würde aber bei jedem Wechsel zwischen Außen- und Unterkarte verdrängt. Ein unbegrenzter Cache könnte bei wechselnden Karten/Historienständen wachsen. 16 ist ein bewusst kleines Speicherlimit, keine empirisch optimale Größe. Mehr als 16 gleichzeitig benötigte Karten bleiben korrekt, können aber erneut geplant werden.

Explizite Dirty-Flags an allen Editoraktionen würden zusätzliche Kopplung schaffen und müssten jeden Import-/Undo-/Programmierpfad erfassen. Der Vergleich tatsächlicher Graphdaten funktioniert auch bei direkt übergebenen Kartensnapshots.

Nur den Hashalgorithmus auszutauschen würde Kollisionen nicht ausschließen. Eine volle topologische Map pro Ergebnissignatur zu vergleichen würde wieder quadratischen Aufwand riskieren. Die referenzielle Planidentität vermeidet beides.

## Aufwand und nachgewiesene Wirkung

Vorher: Graphplanung bei jedem Aufruf plus lineare Knotensuche pro Knoten und Kante, im ungünstigen Fall O(V² + EV). Außerdem lineare Quellportsuche pro Kante.

Jetzt: Beim Strukturwechsel einmal O(E + V log V) für die deterministische Sortierung. Bei wiederholter Auswertung O(V + E + P) für Strukturprüfung und Datenindizes, mit erwarteten O(1)-Mapzugriffen pro Verbindung; P ist die gesamte Anschlussanzahl. Die vorhandene Sortierung semantischer Eingänge und die eigentliche mathematische Arbeit kommen weiterhin hinzu. Diese Änderung macht keine Aussage über deren Laufzeit.

Ein deterministischer Test zählt Zugriffe auf die tatsächlichen Eingabelisten, ohne Zeitlimits oder CPU-Annahmen:

- Kette aus 400 Knoten: **161.199 vorher**, **1.200 nachher** (kalter und warmer Durchlauf). Das sind rund 99,3 % weniger Knoten-Listenzugriffe, kein gleich großer behaupteter Geschwindigkeitsgewinn für die gesamte Anwendung.
- Fan-out mit 300 Quellports: 1.500 Portzugriffe kalt, 1.200 warm; kein erneutes lineares Suchen des Quellports je Verbindung.
- Vor der Änderung scheiterten außerdem Regressionen für kollidierende Parameterhashes und geänderten topologischen Kontext mit veralteten Ergebnissen.

## Verträge und Risiken

Es gibt keine neuen Knotentypen, öffentliche APIs, persistierten Daten oder Formatversionen. Android und Desktop verwenden denselben Adaptercode. Vollständige Auswertung, lexikographische ID-Reihenfolge, Mehrfachkanten, Standardwerte, Fehlermeldungen und Laufzeitmessung bleiben erhalten. Fehlende Zielknoten erzeugen keinen zusätzlichen ausführbaren Knoten.

Die Listen in Kartensnapshots werden entsprechend dem bestehenden Datenmodell als unveränderlich behandelt. Die Signatur hält ihre Knoten-/Eingabedaten bis zur Invalidierung; die Zahl gehaltener Graphpläne ist begrenzt. Die Cache-Instanz bleibt wie zuvor für sequenzielle Verwendung vorgesehen.

## Umsetzung und Verifikation

`GraphAuswertungsPlan.kt`, `KartenAuswerter.kt` und `GraphAuswertungOptimierungTest.kt`. Tests behandeln Hashkollisionen auch bei Gruppenverweisen, lokale Parameteränderungen, Neubenennung/Entfernung von Ports, Umverdrahten/Löschen/Wiederherstellen, Reihenfolgekontext, Mehrfachkanten, Zyklen, fehlende Quellen, lange Ketten, Fan-out, Cacheverdrängung und 20 verschachtelte Unterkarten. Die finalen Befehle und Ergebnisse stehen im ExecPlan.
