# ADR-0002: Beweisbewusste Mengenvisualisierung und allgemeine Orbitmengen

- Status: akzeptiert
- Datum: 2026-10-03
- Beteiligte: Nutzer, SamAI, mathematische Prüfung
- Bezug zu ExecPlan: `plans/completed/2026-10-01-mengenvisualisierung-orbits.md`

## Kontext

Die bisherige Visualisierung vermischte exakte Mengenalgebra, numerische
Punktstichproben und Zeichenheuristiken. Dadurch konnten kurze Lücken verschwinden,
Definitionslücken wie Nullstellen aussehen und unentscheidbare Bereiche neben
gefundenen Punkten unsichtbar werden. Für Iterationsmengen kommt hinzu, dass ein
endlicher, nicht entkommener Orbit weder allgemeine Beschränktheit noch
Mitgliedschaft in einer Mandelbrot-artigen Menge beweist.

## Entscheidung

Die Visualisierung verwendet drei getrennte Evidenzebenen:

1. Rationale Mengen in R1 werden vor dem Zeichnen exakt als Segmente mit
   Unendlichkeiten und offenen oder geschlossenen Endpunkten normalisiert.
2. Räumliche Regionen werden in Zellen zerlegt. Eine Zelle gilt nur dann als
   enthalten oder ausgeschlossen, wenn ein Nachweis für alle ihre Punkte vorliegt.
   Gemischte und unbekannte Zellen bleiben mit Status und Grund im Ergebnis und
   werden räumlich markiert.
3. Numerische Punktwerte ergänzen die Darstellung als Vorschau. Sie verändern
   keinen Zellnachweis und keine exakte Mengentopologie.

Nicht dargestellte Variablen werden durch explizite feste Schnittwerte gebunden;
es gibt keine implizite Projektion. Komplexe Mengen verwenden einen eigenen
zweidimensionalen Raum aus Real- und Imaginärteil. Eine optionale Farbdimension
darf mehrere Werte an einem Bildpunkt liefern; diese Mehrdeutigkeit wird markiert.

Für dynamische Systeme werden `OrbitFamilie` und
`OrbitBeschraenktheitsMenge` als Android- und Compose-freie Kernausdrücke
eingeführt. Die Knoten `mathematik.orbit` und
`mathematik.orbitBeschraenktheit` bauen diese Ausdrücke über bestehende Methoden-
und Mengenanschlüsse auf. Die Mandelbrot-Beispielkarte ist eine reguläre
Zusammenschaltung dieser allgemeinen Knoten.

Allgemeine Aufnahme in eine Orbitbeschränktheitsmenge verlangt einen Beweis, etwa
einen exakt wiederholten Zustand. Ein endlicher Lauf ohne Flucht bleibt unbekannt.
Die Form `z_(n+1)=z_n²+c`, `z_0=0` wird strukturell erkannt und darf zusätzlich
exakte Hauptkardioiden-, Periode-2-Kreis- und Fluchtnachweise verwenden. Das
Erreichen von `|z_n|>2` beweist dort Ausschluss; Budgetende beweist nichts.

## Alternativen

### Nur Rasterpunkte

Eine reine Punktwolke ist einfach und schnell, kann aber keine Aussage über die
Fläche zwischen Punkten belegen und macht unbekannte Regionen nicht zuverlässig
sichtbar.

### Unbekannt als ausgeschlossen behandeln

Diese Variante erzeugt ruhige Bilder, liefert jedoch falsche mathematische
Aussagen bei fehlenden Bindungen, Definitionslücken, nicht unterstützten
Operatoren oder erschöpften Budgets.

### Eigener Mandelbrot-Knoten

Ein Sonderknoten wäre einfacher zu optimieren, würde aber die beauftragte
allgemeine Iterationssemantik umgehen und eine zweite Quelle mathematischer
Wahrheit schaffen.

## Begründung

Die Trennung erhält den Unterschied zwischen Beweis und Schätzung bis in die UI.
Sie erlaubt schnelle exakte Pfade für einfache Mengen, begrenzte numerische
Vorschauen für allgemeinere Fälle und ehrliche offene Bereiche, wenn das CAS keine
Entscheidung tragen kann. Allgemeine Orbitobjekte bleiben wiederverwendbar und
lassen spezielle sichere Erkennungen zu, ohne sie zum Knotentyp zu machen.

## Konsequenzen

### Positiv

- Exakte R1-Ergebnisse hängen nicht von Pixel- oder Zeichentoleranzen ab.
- Unbestimmtheit besitzt einen Ort, einen Status und eine Diagnose.
- R1, R2, R3 und C verwenden denselben Ergebnisvertrag.
- Abbruch und gemeinsame Raster-/Orbitbudgets begrenzen aufwendige Auswertungen.
- Persistierte Karten benötigen keine Formatänderung; neue Felder haben Defaults.

### Negativ

- Transzendente Ausdrücke erzeugen derzeit häufig unbekannte Zellen, obwohl eine
  spätere einschließende Intervallbibliothek mehr entscheiden könnte.
- R3 zeigt projizierte Zellrahmen und Punktmarken, jedoch keine triangulierte
  Oberfläche.
- Generische Orbitbeschränktheit bleibt in vielen mathematisch interessanten
  Fällen offen.

### Risiken

- Double-Koordinaten dienen weiterhin der Anzeige und dem numerischen Raster. Sie
  dürfen nicht zurück in exakte Mengenentscheidungen gelangen.
- Zusätzliche Zellzertifikate müssen allquantifiziert bleiben; Eckpunktproben
  reichen dafür nicht.

## Umsetzung und Verifikation

`ExakteReelleMengen.kt`, `ZertifizierteIntervalle.kt` und `OrbitMengen.kt`
enthalten die Kernsemantik. `VisualisierungsSampler` hält Zellen, Punktvorschau und
Diagnosen getrennt; der Compose-Renderer zeichnet enthaltene, gemischte und
unbekannte Bereiche verschieden. Kern-, Knoten-, Katalog-, Persistenz- und
Beispielkartentests prüfen die belegten Fälle. Die Android-Laufzeitprüfung bleibt
bis zu einem verfügbaren Gerät oder isolierten Emulator offen.

## Ersetzt durch

_Falls später ersetzt._
