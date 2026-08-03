# ADR: Aussagenrechner, Prädikate und Quantorbindung

- Status: angenommen
- Datum: 2026-08-03
- Beziehungen: #137, #142

## Kontext

Aussageoperatoren, Prädikate und Quantoren müssen dieselbe Ausdrucksstruktur wie der Zahlen-Formelbauer verwenden. Sichtbare Variablennamen sind dafür keine ausreichende Identität: Zwei verschachtelte Quantoren dürfen beide `x` anzeigen und dennoch verschiedene Quellen binden.

## Entscheidung

### Gemeinsamer Ausdruckskern

`FormelAusdruck` bleibt die gemeinsame, UI-neutrale Struktur für Formelansicht und Graphprojektion. LaTeX ist kontrollierter Import und kanonischer Export, niemals die semantische Quelle.

### Aussagenrechner

Ein späterer gemeinsamer Aussagenrechner darf folgende Operatorfamilien als stabile Operator-IDs bündeln:

- Negation,
- Konjunktion, Disjunktion und Adjunktion,
- Implikation und Äquivalenz,
- Gleichheit und Ordnungsvergleiche,
- weitere reine Aussageoperatoren, sofern Ein- und Ausgangsvertrag vollständig typisiert sind.

Bestehende Knoten werden erst migriert, wenn für ihren Zustand eine verlustfreie Operator-ID, Argumentreihenfolge und Definitionskarte existiert. Quantoren werden nicht als bloße Operatorvariante eines variadischen Rechners behandelt, weil sie einen lexikalischen Gültigkeitsbereich erzeugen.

### Stabile Variablenquellen

Jedes Variablenvorkommen verweist über seine Ausdrucks-ID auf eine `FormelVariablenQuelle` mit stabiler Quell-ID. Der sichtbare Name ist nur Darstellung. Quellen werden als frei, gebunden oder extern klassifiziert.

- **frei:** wird zu einem Formeleingang beziehungsweise Methodenparameter,
- **gebunden:** besitzt genau eine Quantorbindung,
- **extern:** wird aus einem vorhandenen Graph- oder Kartenanschluss bereitgestellt und nicht in der lokalen Formel gebunden.

### Prädikate

Ein Prädikat ist ein Methodenvertrag mit Zieltyp `AUSSAGE`, entsprechend der Zielmenge `{Wahr, Falsch}`. Seine Argumentliste enthält geordnete stabile Quell-IDs. Namen dürfen nicht zur Deduplizierung oder Bindung verwendet werden.

### Quantoren und Gültigkeitsbereiche

Eine Quantorbindung enthält:

- eine stabile Bindungs-ID,
- `∀` oder `∃`,
- genau eine gebundene Variablenquelle,
- die Ausdrucks-ID der Wurzel ihres lexikalischen Gültigkeitsbereichs.

Ein gebundenes Vorkommen außerhalb dieses Teilbaums ist ungültig. Verschachtelung ergibt sich aus enthaltenen Gültigkeitsbereichen; Alpha-Umbenennung verändert die semantische Identität nicht.

### Teilformeln und Fehlerzustände

Unvollständige Formeln bleiben über `FormelAusdruck.Platzhalter` darstellbar. Eine Formel mit offenen Platzhaltern, fehlenden Quellen, mehrfachen Bindungen oder Vorkommen außerhalb ihres Gültigkeitsbereichs darf nicht als ausführbarer Knoten übernommen werden.

### Roundtrip-Grenze

Der Ausdrucks-/Graph-Roundtrip erhält:

- Ausdrucks- und Operator-IDs,
- Argumentrollen und Reihenfolge,
- Quell- und Bindungs-IDs,
- explizite Gruppierung,
- Prädikatsverträge und Quantorgültigkeitsbereiche.

Reine Layoutinformationen, freie Knotenanordnung und nichtsemantische Schreibvarianten gehören nicht zur Garantie. Nicht darstellbare Graphstrukturen werden als Fehler oder explizite Platzhalter zurückgegeben, nicht stillschweigend umgedeutet.

## Folgen

- #137 besitzt einen gemeinsamen, erweiterbaren Ausdruckskern mit kontrolliertem LaTeX-Roundtrip und produktiver CAS-UI.
- #142 ist in getrennt implementierbare Verträge für Aussagenoperatoren, Prädikate und Quantoren zerlegt.
- Ein späterer Aussagenrechner kann dieselbe Formel-UI erweitern, ohne einen zweiten Parser oder eine namensbasierte Variablenlogik einzuführen.

## Nicht entschieden

- automatische Beweise,
- Quantorenelimination,
- Modellprüfung,
- semantische Gleichheit beliebiger Formeln,
- sofortige Migration aller historischen Aussageknoten.
