# ADR: Typisierte Zielmenge für „Term zu Methode"

## Entscheidung

`mathematik.termZuMethode` speichert und bearbeitet keine Zielmenge mehr. Die Zielmenge wird zentral aus dem Term und den Wertebereichen seiner freien Parameter abgeleitet.

Zahlterme verwenden die konservative Zahlenbereichsinferenz. Aussagen verwenden `{⊤, ⊥}`, Mengen ihren abgeleiteten festen Elementträger, Tupel Produktraume sowie Vektoren und Matrizen dimensions- und skalarbereichsbezogene Räume.

## Begründung

Eine Inspector-Auswahl war eine zweite, von Term und Parameterwerten unabhängige Wahrheit. Sie konnte eine fachlich zu kleine oder bei nichtnumerischen Ausdrücken falsche Zielmenge erzeugen.

Allgemeine Parameter erhalten deshalb einen rekursiv persistierten, typisierten Wertebereich. Bei mengenwertigen Ausgaben beschreibt er die feste Grundmenge, wie es der bestehende Iterationsvertrag verlangt.

## Konsequenzen

- Alte `zielmenge`-Parameter werden beim Laden entfernt und beeinflussen die Auswertung nicht mehr.
- Alte allgemeine Parameter mit `werteVorrat` `N` bis `C` werden als numerische Wertebereiche migriert.
- Funktionen, gebundene Funktionen und Mächtigkeiten bleiben bis zu einem eigenen Trägermengenvertrag außerhalb der Inferenz.
