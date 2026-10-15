# Geometry Nodes

Die erste Ausbaustufe übernimmt Blenders Grundprinzip für **Meshes, Punktwolken, Instanzen und Felder**. Die Knoten stehen auf Android und Desktop im Erstellenkatalog unter „Geometry Nodes“. Sie sind eine eigenständige numerische 3D-Domäne neben den vorhandenen exakten mathematischen Geometrieknoten.

## Beispiel öffnen

[Würfel auf einem Gitter](beispiele/geometry-nodes-wuerfelgitter.json) ist eine importierbare Atlas-Karte. Sie verbindet ein Gitter über „Position setzen“ mit „Instanzen auf Punkten“, einem Würfel, „Instanzen realisieren“ und der Vorschau. Das Ergebnis enthält sechs Würfel mit zusammen 48 Eckpunkten und 36 Flächen. Im Inspector lassen sich Gitterauflösung, Würfelgröße und Höhenversatz ändern. „Geometry Nodes übernehmen“ erzeugt eine rückgängig machbare Änderung.

## Verfügbare Knoten

| Bereich | Knoten |
|---|---|
| Mesh | Mesh-Würfel, Mesh-Gitter |
| Punkte | Punkte, Mesh zu Punkten |
| Geometrie | Geometrie verbinden, Geometrie transformieren, Position setzen, Geometrievorschau |
| Felder | Position, Index, Feldrechner, Vektorfeldrechner, Feldvergleich |
| Instanzen | Instanzen auf Punkten, Instanzen realisieren |

Ein Geometry Set kann gleichzeitig ein Mesh, eine Punktwolke und Instanzen enthalten. Meshes besitzen Eckpunkte, ungerichtete Kanten und geordnete Polygonflächen. „Geometrie verbinden“ versetzt die Topologieindizes und fasst Komponenten zusammen; gleiche räumliche Punkte werden dabei nicht verschweißt.

## Mit Feldern arbeiten

„Position“ und „Index“ liefern eine Berechnungsvorschrift. Der Verbraucher wertet sie pro Mesh-Eckpunkt beziehungsweise Punktwolkenpunkt aus. Ein gemeinsames Indexfeld kann zwei unterschiedlich große Geometrien speisen; beide beginnen bei Index 0. Skalar-, Vektor- und Auswahlfelder besitzen unterschiedliche Anschlussarten.

Eine Punktlinie lässt sich so erzeugen:

1. „Index“ mit `x` eines Vektorfeldrechners verbinden.
2. Im Vektorfeldrechner „XYZ kombinieren“ wählen; Y und Z bleiben 0.
3. Dessen `feld` mit `position` eines Punkte-Knotens verbinden und die Anzahl einstellen.

Für eine Auswahl „Index“ an `a` eines Feldvergleichs anschließen, „Kleiner“ und den Grenzwert unter „Standard B“ einstellen. `auswahl` kann „Position setzen“ oder „Instanzen auf Punkten“ steuern.

„Position setzen“ berechnet für ausgewählte Punkte **Position + Offset**. Ohne angeschlossenes Positionsfeld bleibt die ursprüngliche Position die Basis. Auswahl, Position und Offset beziehen sich gleichzeitig auf die unveränderte Eingabe. Verbundene Felder ersetzen ihre jeweiligen Inspector-Standardwerte. Ein Fehler einer verbundenen Quelle wird weitergegeben.

## Instanzen und Vorschau

„Instanzen auf Punkten“ referenziert dieselbe Geometrie mehrfach. Erst „Instanzen realisieren“ erzeugt die vollständige Mesh-/Punktgeometrie. Verschachtelte Instanzen und deren Transformationen werden dabei berücksichtigt.

Die Vorschau zeigt Meshkanten und Punkte einschließlich Instanzen. Ziehen dreht die Kamera; der Modus „Verschieben“ oder Shift-Ziehen verschiebt sie. Zwei Finger verschieben und zoomen, das Mausrad zoomt am Mauszeiger. +/− zoomen um die Bildmitte. „Einpassen“ zentriert die Geometrie bei gleicher Blickrichtung; „Standardansicht“ setzt die Kamera vollständig zurück. 3D/XY/XZ/YZ wählen feste Blickrichtungen, die kleine XYZ-Hilfe zeigt deren Orientierung. Punkte und Kanten sind unabhängig ein-/ausblendbar. Alle Optionen bleiben lokale Ansichtsoptionen.

Die Vorschau passt auch kleine, weit vom Ursprung entfernte Objekte anhand ihrer Ausdehnung ein und erhält denselben Maßstab auf allen Achsen. Die Statistik bezeichnet die direkt gespeicherten Komponenten. Eine begrenzte Vorschau ist sichtbar gekennzeichnet und verändert den ausgegebenen Geometry Set nicht. Hintergründe und weitere Optimierungsmöglichkeiten stehen im [Vergleich mit Desmos und GeoGebra](GEOMETRIE_DESMOS_GEOGEBRA.md).

Transformationen verwenden Translation × Rotation Z × Rotation Y × Rotation X × Skalierung, Winkel in **Radiant**. Negative und Nullskalierung sind zulässig; Flächenreihenfolgen bleiben erhalten.

## Grenzen dieser Stufe

Felder unterstützen zunächst die Punktdomäne: Mesh-Eckpunkte und Punktwolkenpunkte. Instanzen am Eingang von „Position setzen“, „Mesh zu Punkten“ oder am Punkteingang von „Instanzen auf Punkten“ müssen vorher realisiert werden; sonst erscheint eine Fehlermeldung. Es gibt keine automatische Domaininterpolation.

Je Mesh/Punktwolke sind höchstens 100.000 Punkte erlaubt, außerdem 200.000 Kanten, 100.000 Flächen, 500.000 Polygon-Corners und 10.000 direkte Instanzen. Realisierung prüft den gesamten expandierten Umfang und die Traversierungsarbeit. Felder haben zusätzliche Struktur-, Tiefen- und Arbeitsgrenzen. Die konservative Feld-Vorabprüfung zählt auch Felder für später abgewählte Punkte. Grenzüberschreitungen erzeugen Fehler statt abgeschnittener fachlicher Ergebnisse.

Kurven, Volumen, Materialien, benannte Attribute, Simulationen, Mesh-Booleans und `.blend`-Import gehören nicht zu dieser ersten Ausbaustufe. Gespeichert werden die Karte und die Knotenkonfiguration; Geometrie, Feldberechnungen und Kamera werden daraus neu abgeleitet.

Die Semantik orientiert sich an der [Blender-Dokumentation zu Feldern](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/fields.html), [Set Position](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/geometry/write/set_position.html) und [Realize Instances](https://docs.blender.org/manual/en/latest/modeling/geometry_nodes/instances/realize_instances.html).
