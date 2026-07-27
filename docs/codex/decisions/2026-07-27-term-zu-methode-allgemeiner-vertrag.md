# ADR: Allgemeiner Vertrag für „Term zu Methode"

## Entscheidung

Der Knoten nimmt ein `MathematischesObjekt` entgegen und gibt eine allgemeine `Funktion` aus. Freie numerische Variablen werden aus dem Term und ihren Graphquellen abgeleitet; ihre Grundmengen werden nicht als eigene Handles gespeichert.

## Begründung

Der Rechenkern kann beliebige mathematische Objekte als Funktionswert speichern, kennt aber weiterhin nur numerische Variablen und deren Substitution. Ein allgemeiner Ausgang ist daher fachlich korrekt; eine zur Laufzeit wechselnde Anschlussart wäre ein zusätzlicher, bisher nicht vorhandener Graphvertrag.

## Konsequenz

Die erzeugte Funktion kann nicht direkt an spezialisierte Zahl- oder Mengenfunktionsanschlüsse verbunden werden. Dafür bleiben die vorhandenen spezialisierten Methoden-Knoten maßgeblich.
