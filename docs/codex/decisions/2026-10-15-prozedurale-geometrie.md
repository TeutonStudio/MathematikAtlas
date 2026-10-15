# Numerische prozedurale Geometrie als eigener Atlas-Wert

Datum: 2026-10-15. Status: angenommen für die Geometry-Nodes-Grundlage.

Die vorhandene Geometrie modelliert intrinsische mathematische Objekte und exakte Ausdrücke. Endliche numerische Meshes, Punktwolken und referenzierte Instanzen brauchen Topologie und kontextabhängige Feldberechnung. Sie implementieren deshalb `DarstellungsWert` im reinen Rechenkern und nutzen den vorhandenen `AtlasWert`-Transportkanal. Sie erhalten weder künstliche mathematische Zielmengen noch eine implizite Konversion zu intrinsischer Geometrie.

Ein Geometry Set enthält höchstens eine Mesh- und eine Punktkomponente sowie Instanzreferenzen. Join führt Komponenten mit versetzten Indizes zusammen. Unveränderliche Snapshots, gecachte Strukturhashwerte und paarweise memoisierter Geometrievergleich sichern den Auswertungscache auch für geteilte Instanzgraphen ab.

Felder sind typisierte unveränderliche Syntaxbäume. Position und Index werden erst am Verbraucher gegen die ursprüngliche Geometrie ausgewertet; Index ist komponentenlokal. Konstruktion, Feldauswertung und Instanzrealisierung besitzen explizite Struktur-/Arbeits-/Speichergrenzen. Instanzen werden nur auf ausdrücklichen Wunsch realisiert. Die Anzeige traversiert ein separat begrenztes Renderabbild und verändert die fachliche Ausgabe nicht.

Knotenvorlagen, Anschlussarten, Auswerter und Konfigurationsdefinitionen liegen in `MathematikKnoten/geometrynodes`; Android und Desktop verwenden denselben Inspector und Canvasrenderer. Die vorhandenen Graph-, Undo- und Codec-Verträge bleiben erhalten. Persistiert werden nur die vorhandenen Graphdaten und Stringparameter; das Kartenformat bleibt unverändert.

Die Alternative, numerische Meshes als symbolische `GeometrieGruppe` oder als untypisierte Punktlisten zu transportieren, würde Topologie, Instanzidentität und verzögerte Felder verlieren. Eine externe 3D-Engine ist für die zunächst unterstützte Drahtvorschau nicht erforderlich.
