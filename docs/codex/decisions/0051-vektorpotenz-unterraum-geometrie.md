# ADR 0051: Vektorpotenz, argumentabhängige Unterräume und Geometriekoordinaten

Datum: 2026-10-12. Status: implementiert und unabhängig verifiziert.

## Entscheidung
Die bestehenden Rechner erhalten die Operatoren `vektor.potenz` und `menge.unterraum`. Es entstehen keine neuen registrierten Knotentypen. Potenz verwendet die Paritätsdefinition mit dem ausgewählten Skalarprodukt, ohne Produktberechnung für p=0/1. Der effektive Ausgangsvertrag berücksichtigt bekannte Parität.

Unterräume speichern Richtungen, optionalen Stützpunkt und Umgebungsdimension als Kernobjekt. Rang und Mitgliedschaft werden für rationale Daten exakt durch Gaußelimination entschieden. Symbolische Pivots bleiben unbestimmt. Methodensubstitution und Parametererfassung durchlaufen Komponenten; die Menge ist somit argumentabhängig.

Geometrie und Koordinatentupel bleiben unterschiedliche Strukturen. Explizite Übersetzer erzeugen Koordinatenmengen, ausgewählte Punkte, Mittelpunkte und geometrische Realisierungen. Ankertranslation erhält die vollständige strukturierte Menge. Ein Koordinatensystemname ist kein numerischer Basiswechsel. Unbekannte Kreisebene in Dimension größer zwei und nicht ausführbare Transformationen bleiben symbolisch. Polygonflächenzentren werden nur für exakt als einfach, eben und nichtentartet erkannte Polygone berechnet.

Skalarproduktreferenztexte sind keine Nachweise. Aufgelöste Axiomnachweise binden vollständigen Methodensnapshot, Zahlkörper und Linearitätsseite. Der reelle eindimensionale Standardfall wird strukturell erkannt; ein negatives Selbstprodukt liefert ein Gegenbeispiel. Unbewiesene eigene Produkte können unter einer ausdrücklich weitergereichten Begriffsannahme ausgeführt werden; widerlegte Produkte sind gesperrt.

## Alternativen und Folgen
Stille Gleichsetzung von Tupeln, Vektoren, Mengen und Geometrie würde Orientierung und mathematische Bedeutung verlieren. Allgemeine numerische Stichproben dürfen universelle Axiome nicht beweisen. Daher sind Umwandlungen explizit und unentscheidbare Aussagen bleiben sichtbar offen. Neue Konfigurationen nutzen vorhandene Parameter, Eigenschaften und versionsfeste Kartenverweise bei Kartenformat 9.

Definitionskarten zerlegen Potenzen in Produkt, skalare Potenz und Skalierung sowie Unterräume in Linearkombination, Methodenbildung und Bildmenge. Der UI-Aufruf übernimmt aktuelle Eingangswerte für Exponent, Orientierung und affine Variante in die Definitionsmomentaufnahme.
