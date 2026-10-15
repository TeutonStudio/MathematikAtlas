# Geometry Nodes: endliche Geometrie, Felder und Instanzen

## 1. Status

Abgeschlossen und unabhängig abgenommen am 2026-10-15. Auftrag: Blenders Geometry Nodes und zugehöriges Modell nachbilden. Nutzerentscheidung: erste integrierte Stufe mit Mesh, Punktwolken, Instanzen und Feldern. Planung durch `node_planner`, Freigabe mit Auflagen durch `math_reviewer`, getrennte Abschlussprüfung durch `node_verifier`.

## 2. Ziel und Nutzerwirkung

Eine Karte kann ein Mesh-Gitter erzeugen, anhand von Index/Position verändern, einen Würfel auf seinen Punkten instanzieren und das Ergebnis als 3D-Drahtmodell anzeigen. Eine zweite Karte kann denselben Feldausgang an anderer Geometrie auswerten. Speichern, Laden, Gruppieren und Undo verwenden die vorhandenen Kartenpfade.

## 3. Nicht-Ziele

Diese Stufe bildet keine vollständige Blender-Installation nach. Kurven, Volumen, Materialien, Simulationen, Mesh-Booleans, Named Attributes und Blender-Dateiimport folgen möglichen weiteren Stufen. Keine stillen Approximationen dieser Funktionen. Kein neuer Renderer oder eine Engine-Abhängigkeit im Rechenkern.

## 4. Untersuchte Ausgangslage

Masterbasis `d435b4fcf62ce83409911f98c08f0a096e78c0dd` (v3.2.2). `kern/geometrie/GeometrieModell.kt` modelliert intrinsische mathematische Geometrie; dieses Modell bleibt erhalten. Der allgemeine `AtlasWert`-/`DarstellungsWert`-Kanal trägt numerische Geometrie ohne künstliche mathematische Metadaten. Zentraler Erstellenkatalog: `katalog/KanonischerMathematikKnotenKatalog.kt`; Auswertung: `katalog/MathematikAuswerterPakete.kt`. Android und Desktop teilen den Bibliotheksquelltext, aber besitzen eigene Inspector- und Renderer-Auswahlpfade.

## 5. Semantik und Quellen

Endliche Double-Koordinaten, ein Mesh und eine Punktwolke pro Geometry Set plus referenzierte Instanzen. Mesh: Positionen, explizite ungerichtete Kanten, orientierte Polygon-Corners. Alle Face-Randkanten müssen vorhanden sein. Leere, offene und nichtmannigfaltige Meshes sind erlaubt. Numerisch degenerierte Flächen dürfen aus Nullskalierung entstehen.

Felder sind typisierte unveränderliche Syntaxbäume für Skalare, Vektoren und Auswahl. Position und Index werden am Verbraucher auf der ursprünglichen Komponente ausgewertet; Index beginnt pro Komponente bei null. Erste unterstützte Field-Domäne: Mesh-Vertex/Punkt. Keine implizite Domaininterpolation. Set Position berechnet für ausgewählte Elemente `PositionFeld + OffsetFeld` auf demselben unveränderten Input. Instanzhaltige Eingaben bei reinen Punktoperationen werden vorerst ausdrücklich abgelehnt, statt Komponenten zu ignorieren.

Instanzen teilen unveränderliche Referenzen. Transformationen verwenden Spaltenvektoren und `T × Rz × Ry × Rx × S`, Winkel in Radiant. Elternmatrizen werden links multipliziert. Negative Skalierung dreht Face-Corners nicht automatisch um. Realize erhält direkte Komponenten, expandiert jede Referenz pro Vorkommen und führt Topologie mit versetzten Indizes zusammen. Globale Budgets werden vor Ausgabeallokation geprüft, einschließlich Instanztiefe, Traversierungsarbeit und Feldarbeit.

Geprüfte Blender-Dokumentation (2026-10-15): [Felder](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/fields.html), [Set Position](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/geometry/write/set_position.html), [Instance on Points](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/instances/instance_on_points.html), [Realize Instances](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/instances/realize_instances.html), [Transform Geometry](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/geometry/operations/transform_geometry.html). Diese Stufe unterstützt nur den ausdrücklich beschriebenen Teilumfang dieser Verträge.

## 6. Daten-, Knoten- und Anschlussvertrag

Neue Typ-Schlüssel (Präfix `mathematik.geometryNodes.`): `meshCube`, `meshGrid`, `points`, `meshToPoints`, `joinGeometry`, `transformGeometry`, `position`, `index`, `scalarMath`, `vectorMath`, `compare`, `setPosition`, `instanceOnPoints`, `realizeInstances`, `preview`.

Eigene Anschlussarten unter `atlas.wert`: Geometrie, Skalarfeld, Vektorfeld, Auswahlfeld. Ein Feld ist keine Zahl und kein Tupel; die vorhandenen Übersetzer dürfen keine implizite Auswertung ohne Geometriekontext anbieten. Jeder Eingang hat höchstens eine Verbindung. Join besitzt die optionalen Eingänge `a` und `b`. Sonstige Geometrieeingänge sind erforderlich. Unverbundene Felder verwenden validierte gespeicherte Defaults; fehlerhafte verbundene Quellen dürfen nicht durch Defaults ersetzt werden.

| Knoten | Eingänge | Ausgänge |
|---|---|---|
| Mesh-Würfel, Mesh-Gitter | keine | geometrie |
| Punkte | position | geometrie |
| Mesh zu Punkten, Transformieren, Realisieren, Vorschau | geometrie | geometrie |
| Verbinden | a, b | geometrie |
| Position, Index | keine | feld |
| Feldrechner | a, b | feld |
| Vektorfeldrechner | a, b, faktor, x, y, z | feld |
| Feldvergleich | a, b | auswahl |
| Position setzen | geometrie, auswahl, position, offset | geometrie |
| Instanzen auf Punkten | punkte, instanz, auswahl, rotation, skalierung | geometrie |

Vektorrechner unterstützt Addieren, Subtrahieren, Skalieren und XYZ-Kombination (Skalarfelder). Defaults und Operator-IDs liegen zentral bei den Vorlagen. Parameteränderungen behalten die Anschluss-IDs. Ungültige Zahlen/Operatoren liefern sichtbare Auswertungsfehler. Konfiguration läuft über vorhandene Kartenaktionen mit Undo/Redo.

## 7. Architekturentscheidungen

Numerische Geometrie und Felder implementieren `DarstellungsWert`, nicht `GeometrischerAusdruck`. Das Kernmodell liegt unter `kern/prozeduralegeometrie/`. Pro Set genau eine Komponente je unterstütztem Typ wie in Blender; Join verschweißt keine geometrisch gleichen Vertices. Listen werden beim Konstruieren kopiert und vor nachträglicher Mutation geschützt. Kein persistierter Laufzeitcache, keine Closures im Feldmodell. Gemeinsame Compose-Konfiguration und Vorschau für beide Plattformen.

## 8. Betroffene Dateien

Neue Kernmodell-/Feld-/Operationsdateien und Kerntests. Neue Vorlagen, Anschlussarten, Auswerter, Konfiguration und Vorschau unter `MathematikKnoten/.../geometrynodes/`. Ergänzung von Anschlussregister, kanonischem Katalog und Auswerterpaketen. Kleine Integrationen in Android-Inspector und `AtlasZustand`, Desktop-Inspector und `DesktopAtlasZustand`. Neue fachliche, Codec- und Desktop-Tests. Release-Knotenmanifest/Roadmap enthalten genau die neue Familie.

## 9. Meilensteine

- [x] Bestand und Schwerpunkt untersucht; Nutzerwahl und unabhängige mathematische Freigabe liegen vor.
- [x] Master-Verwalter reservierte 3.3.0 auf `samai/v3.3.0-geometry-nodes`.
- [x] Kernmodell und Operationen: 17 neue Kernregressionen, alle 573 Kerntests grün.
- [x] 15 Knoten mit zentraler Registrierung und Konfiguration; vier neue beschriebene Anschlussarten.
- [x] Gemeinsame Vorschau und Inspector integriert; drei Desktop-Laufzeittests erfolgreich.
- [x] Zehn Integrationsprüfungen inklusive Codec, Gruppierung, Felder und Undo erfolgreich; unabhängige Abnahme ohne offene Findings.

## 10. Umsetzungsschritte

Kernimplementierer arbeitet ausschließlich an Modell/Feldern/Operationen und Kerntests. Danach übernimmt der Koordinator als einziger weiterer Implementierer die Knoten-/UI-Integration. Keine gleichzeitigen Produktedits und keine parallelen Gradle-Läufe. Fachliche Korrekturen aus der Verifikation werden anschließend erneut geprüft.

## 11. Tests und Validierung

Topologie, endliche Werte, Gridreihenfolge, Cubeorientierung, leere Geometrie, komponentenlokale Felder, Originalkontext, Transformation einschließlich negativer/Nullskalierung, nichtkommutierende Instanztransformationen, Referenzteilung, Indexversetzung, Realize-Budget und Feldbudget. Katalog/Register/Anschlusskompatibilität, durchgängiger Beispielgraph, Fehlerfortpflanzung, Codec-Roundtrip, Gruppenknoten, Inspector-Undo und Desktop-Vorschau/-Konfiguration. Repository-/Architektur-/Releaseprüfungen und `test :app:assembleDebug :app:compileDebugAndroidTestKotlin`; Desktop-Tests mit Xvfb. Android-Laufzeit nur melden, wenn tatsächlich ausgeführt.

## 12. Persistenz und Migration

Ausschließlich vorhandene Stringparameter und Graphdaten werden gespeichert. Geometrien, Feld-ASTs, Kamera und Auswertungsergebnisse bleiben Laufzeitwerte. Kartenformat bleibt 9. Historische Typen und Anschlüsse bleiben unverändert. Codec-Tests sichern neue IDs und Konfigurationen sowie unbekannte Zusatzparameter ab.

## 13. Risiken und Rückfallstrategie

Große Instanzgraphen und tiefe Felder erfordern gemeinsame harte Arbeits-/Speichergrenzen. Keine partiellen fachlichen Ausgaben. Vorschau darf begrenzt sein, muss dies sichtbar kennzeichnen; sie darf keine vollständige Realisierung nur für die Anzeige erzwingen. Bei ungültiger Konfiguration bleibt die Karte bearbeitbar und meldet den Fehler am Knoten.

## 14. Fortschritt

Alle Meilensteine abgeschlossen. Zuerst Kernimplementierung und mathematische Auflagen, anschließend gemeinsame Knoten-/UI-Integration. Findings zu DAG-Gleichheit, Feld-Vorabgrenzen und Anschlussbeschreibungen korrigiert; finale unabhängige Abnahme erfolgt.

## 15. Entscheidungsprotokoll

2026-10-15: Nutzer wählte ausdrücklich Mesh/Punktwolken/Instanzen/Felder als erste Grundlage. Zusammenführung in je eine Mesh-/Punktkomponente statt einer Liste gleicher Komponenten, damit Domänen und Indexverhalten Blender entsprechen. Vektor-XYZ-Kombination im bestehenden Vektorfeldrechner ermöglicht aus Indexfeldern prozedurale Punktpositionen, ohne einen sechzehnten Knotentyp einzuführen.

## 16. Versionswirkung und Abweichungen

3.3.0, Achse y, Vorgänger 3.2.2. Genau 15 neue registrierte Typen. Noch kein Auftrag für erneute Veröffentlichung oder APK-Release in diesem Featureturn; Veröffentlichung ist ein eigener geprüfter Schritt. Keine vollständige Blender-Knotenabdeckung behaupten.

## 17. Ergebnis und Verifikation

`test :app:assembleDebug :app:compileDebugAndroidTestKotlin` erfolgreich (42 s); XML-Summe 1.807 Tests, keine Fehler und keine übersprungenen Tests. Darin 17 neue Kerntests, zehn neue Graph-/Codec-/Gruppen-/Undo-/Vorschauprüfungen und drei neue Desktop-Compose-Tests. Vorschaurotation verändert das tatsächliche Canvasbild; Reset stellt es wieder her. Inspector prüft ungültige Entwürfe und atomare rückgängig machbare Übernahme. Screenshot `/workspace/artifacts/geometry-nodes-preview.png` visuell kontrolliert.

Repository-, Architektur-, Methodenmodell-, Desktop-Shadowmodule-, Standardkarten-, Releaseplan- und Versionsfolgeprüfungen erfolgreich. Zusätzliche direkte `pruefe_kern.py`-Kompilierung und Kernprüfungen erfolgreich. `git diff --check` erfolgreich. `node_verifier` erteilte finale Abnahme ohne offene Findings; `master_verwalter` bestätigte exakt 15 neue Typen, konsistente Reservierung und unveränderte Masterbasis. Android-Instrumentierung wurde kompiliert, für diesen neuen Stand nicht auf Emulator/Gerät ausgeführt.

Arbeitsstand auf `samai/v3.3.0-geometry-nodes`, Basis `d435b4fcf62ce83409911f98c08f0a096e78c0dd`; kein Featurecommit, Push oder Release erzeugt. 3.3.0 bleibt reserviert, veröffentlichte Metadaten 3.2.2. Persistenzformat unverändert. Importierbares Würfelgitter und Bedienungsanleitung unter `docs/GEOMETRY_NODES.md`.


### Nachfolgender Integrationsauftrag

2026-10-15: Nach Abschluss dieses Funktionsplans hat der Nutzer einen neuen Master-Commit und Push beauftragt. Die vorstehenden Angaben zu Reservierung/ungecommittetem Stand sind historische Prüfstände. Die nachfolgende3.3.0-Releasevorbereitung mit vollständigem Funktionsumfang und Remote-Prüfungen wird im [Integrationsplan](../active/2026-10-15-v3-3-0-master-integration.md) dokumentiert.
