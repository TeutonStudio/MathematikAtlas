# Matrizenmengen und sichtbare Anschlusssignaturen

Datum: 2026-10-17. Status: umgesetzt, Prüfung im aktiven ExecPlan.

Matrizenmengen werden über die bestehenden Knoten `mathematik.menge` (Varianten
`R.matrizen` und `C.matrizen`) und `mathematik.mengenrechner` (Operator
`menge.matrizenmenge`) erzeugt. Die Dimensionseingänge `n` und `m` akzeptieren
positive ganze Zahlen und eindeutig gebundene natürliche Variablen; offene
Positivitätsbedingungen und Quellenidentitäten werden im Ergebnis weitergeführt.
Der Operator besitzt zusätzlich den Mengeneingang `grundmenge`. Die Knoten
behaupten keine algebraischen Axiome über einer beliebigen Grundmenge.

Der bestehende `Tensorraum` erhält das optionale Merkmal `matrixForm`, das genau
zwei Achsen verlangt. Es kennzeichnet Matrixelemente ausdrücklich, ohne die
bisherigen allgemeinen Tensoren zweiter Stufe umzudeuten. Typableitung,
Mitgliedschaft, konkrete Variablenkomponenten, Topologie, Eigenschaften und
Visualisierungsdimension verwenden diese Unterscheidung. Der mathematische
Wertcodec schreibt dafür die additive Variante `MatrizenTensorraum`; bisherige
`Tensorraum`- und `Matrizenraum`-Werte bleiben unverändert lesbar. Die bestehende
Wire-Version und das neutrale Kartenformat werden nicht verändert.

Dimensionsausdrücke und ihre Bindungen bleiben im Kernwert erhalten und werden
bei Methodenanwendungen substituiert. Ein symbolisches Dimensionsliteral im
neutralen Typ enthält dagegen keine vollständige Quellenbindung. Die Rückprojektion
`bereichFürTyp` liefert dafür ausdrücklich `FehlendeObermenge`, statt Namen zu
parsen oder eine ungebundene Ersatzvariable zu erfinden.

Historische `mathematik.matrizenraum`-Knoten werden im gemeinsamen Codec zum
Mengenrechner migriert. Bestehende Knoten-, Grundmengenport-, Ausgangs- und
Verbindungs-IDs bleiben erhalten. Zeilen-/Spaltenparameter werden explizite
Standardwerte der neuen Ports. Ungültige Dimensionen oder mehrdeutige Rollen
führen zum vorhandenen Diagnosepfad. Der historische Auswerter bleibt ausschließlich
für Lade-/Definitionskompatibilität registriert, nicht als Erstellen-Angebot.

Editor und Legende benutzen dieselbe Symbolbeschreibung. Methodeneinträge
unterscheiden die geordneten Farbgruppen beider Halbkreise. Ihre Farben bleiben
während eines Verbindungszugs deckend; ein gestrichelter Außenring markiert
inkompatible Methodenanschlüsse. Die Verbindungsprüfung bleibt maßgeblich.
