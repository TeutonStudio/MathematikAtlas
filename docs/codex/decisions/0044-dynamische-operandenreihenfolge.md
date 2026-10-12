# Dynamische Operandenreihenfolge

Datum: 2026-10-12. Status: angenommen.

Die persistierte Anschlussreihenfolge ist die verbindliche Reihenfolge dynamischer Operanden. Anschlussnamen bezeichnen Identität und werden beim Sortieren nicht umbenannt. Auswerter, Vorschau und Konfiguratoren müssen diese Reihenfolge erhalten und verwenden.

Assoziativität wird im fachlichen Operatorvertrag ausdrücklich beschrieben und ist unabhängig von Kommutativität. Nur gleichartige Operanden einer dynamischen Gruppe sind sortierbar; feste Zusatzrollen und freie Schlussreserven bleiben ausgeschlossen. Sortieren geschieht atomar über eine validierte Kartenaktion; Drag-Vorschau bleibt flüchtig.

Redundante Einzeloperationen werden nur bei nachgewiesener Semantikgleichheit migriert. Knoten- und Anschluss-IDs, Parameter und individuelle Darstellung bleiben erhalten. Die gemeinsame mathematische Codec-Pipeline gilt für beide Plattformen. Alternative namensbasierte Sortierung würde Nutzerentscheidungen beim Laden überschreiben und wird verworfen.
