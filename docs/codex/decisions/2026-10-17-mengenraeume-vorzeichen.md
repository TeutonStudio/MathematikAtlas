# Mengenräume und vorzeichenrichtige Ausgabe

Datum: 17.10.2026. Status: angenommen für lokale Umsetzung.

Tupelräume sind kartesische Potenzräume A^n mit n>=0. Allgemeine Tensorräume
haben ein endliches, nichtleeres Tupel positiver natürlicher Dimensionen. Bei
Stufe zwei gehören sowohl passende Matrix- als auch Tensorwerte zum Raum;
die Werte bleiben strukturell verschieden. Historische matrixForm-Räume
behalten ihren engeren Matrixvertrag. Es entsteht kein neuer Knotentyp.

Die Alternative eigener Matrizen-Auswahlvarianten würde dieselbe Konfiguration
mehrfach anbieten. Stattdessen konfiguriert die Quelle Menge ihre Dimensionsart;
der Mengenrechner bietet Tupelraum und Tensorraum. Historische n/m-Anschlüsse
werden durch sichtbare Tupelzusammenstellung verbunden, mit erhaltenen IDs,
Standardwerten, Fallbacks und Quellenbindungen.

Subtraktion bleibt intern eine Addition negativer Faktoren. Die zentrale Ausgabe
liest das Vorzeichen aus dem Ausdrucksbaum und klammert den Gegenwert nach
Operatorrang. Kontextbezogene negative Summanden dürfen als Subtraktion ihres
Betrags erscheinen; die Voraussetzungen bleiben am BedingtenWert erhalten.
Eine neue persistierte Differenzklasse und Stringersetzungen im Renderer
würden Modell und Anzeige unnötig auseinanderziehen.

Renderer 1.3.0 versteht brace-Symbole, aber nicht deren Namen im Delimiterpfad.
Deshalb verwendet die Fassade Symbolnamen für einzelne Klammern und escaped
Klammern nach Größenbefehlen. Keine zusätzliche TeX-/Produktionsabhängigkeit.
