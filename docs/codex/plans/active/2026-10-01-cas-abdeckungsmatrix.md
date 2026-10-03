# CAS-Abdeckungsmatrix für Mengenvisualisierung

Stand 2026-10-03 auf dem Arbeitsbranch
`samai/v2.34.0/mengenvisualisierung-orbits`. Die Matrix erfasst alle konkreten
`ZahlAusdruck`- und `MengenAusdruck`-Typen des implementierten Stands. Sie belegt
das Routing, aber keine vollständige mathematische Korrektheit aller CAS-Ausdrücke.

Statuskürzel:

- **A**: exakt oder für die ganze rationale Zelle zertifiziert,
- **N**: numerische, als Approximation gekennzeichnete Vorschau,
- **R**: nach symbolischer Reduktion auf einen unterstützten Typ,
- **U**: unbestimmt mit sichtbarem Grund,
- **X**: kein räumlicher Mengenträger; bewusst nicht visualisiert.

## Vollständiges Typinventar

Der Rechenkern enthält 28 konkrete Zahltypen und 42 konkrete Mengentypen. π und e
sind Instanzen von `MathematischeKonstante`; N, N₀, Z, Q, R, C, Quaternionen und
unbekannte Namen sind Instanzen von `BenannteMenge`.

| Datei | Konkrete `ZahlAusdruck`-Typen |
|---|---|
| `Zahlen.kt` | RationaleZahl, Variable, MathematischeKonstante |
| `Operatoren.kt` | Addition, Multiplikation, Maximum, Minimum, Division, Potenz, Betrag, Sinus, Cosinus, ArcSinus, ArcCosinus, Exponentialfunktion, NatürlicherLogarithmus, Wurzel, KomplexeZahl, Argument, Logarithmus |
| `StrukturierteDivision.kt` | InversesElement, StrukturierteDivision |
| `IterierteOperatoren.kt` | IterierteSumme, IteriertesProdukt |
| `FallAusdruck.kt` | ZahlFallAusdruck |
| `DifferentialModell.kt` | DifferentialVariable, DifferentialTerm |
| `HyperModell.kt` | SymbolischerHyperReellerWert |

| Datei | Konkrete `MengenAusdruck`-Typen |
|---|---|
| `Mengen.kt` | EndlicheMenge, LeereMenge, BenannteMenge, ReellesIntervall, Vereinigung, Schnitt, MengenDifferenz, KartesischesProdukt, Tupelraum, Folgenraum, Vektorraum, Matrizenraum, DefinierteMenge, GefilterteMenge |
| `Mengenraeume.kt` | Primzahlen, GaußscheGanzeZahlen, GaußschePrimzahlen, Potenzmenge, Abbildungsmenge, Tensorraum, ModuloZahlenraum |
| `PraedikatsMengen.kt` | MengenParameter, FehlendeObermenge, PrädikatsMenge |
| `BeschraenkteZahlmenge.kt` | BeschraenkteZahlmenge |
| `SymmetrischeDifferenz.kt` | SymmetrischeDifferenz |
| `FallAusdruck.kt` | MengenFallAusdruck |
| `Methoden.kt` | Abbild |
| `MethodenGraph.kt` | MethodenGraphMenge |
| `IterierteOperatoren.kt` | IterierteVereinigung, IteriertesKartesischesProdukt, IterierterSchnitt |
| `DifferentialModell.kt` | DifferenzierbarkeitsBereich, AbleitungsZielraum |
| `TangentialObjekt.kt` | TangentialMenge |
| `TopologischerRand.kt` | TopologischerAbschluss, TopologischesInneres, TopologischerRand, TopologischerRandImRaum |
| `geometrie/GeometrieModell.kt` | GeometrischeTrägermenge, KoordinatenBild |
| `OrbitMengen.kt` | OrbitBeschraenktheitsMenge |

## Zahlenoperatoren

Die Spalte „Zelle“ bezeichnet den neuen rationalen Intervallnachweis. Ein fehlender
Zellnachweis wird nicht als Nichtmitgliedschaft gedeutet. Der vorhandene
`NumerischerAuswerter` darf dann Punktwerte für eine Vorschau liefern; die Zelle
bleibt trotzdem sichtbar unbestimmt.

| Typ | Symbolik / Definitionsbereich | Zelle | Punktvorschau | Visualisierungsstatus |
|---|---|---|---|---|
| RationaleZahl | exakter Bruch | A | N | A |
| Variable | exakte Bindung; ohne Bindung offen | A/U | N/U | A oder U |
| MathematischeKonstante | π/e symbolisch | U | N | N, Zelle U |
| Addition | rekursiv, rationale Reduktion | A | N | A/N |
| Multiplikation | rekursiv, rationale Reduktion | A | N | A/N |
| Maximum | rekursiv | A | N | A/N |
| Minimum | rekursiv | A | N | A/N |
| Division | Definitionslücke bei Nenner null | A außerhalb null, sonst U | N/U | A/N/U |
| Potenz | exakte ganzzahlige Exponenten bis Betrag 256 | A im zertifizierten Teilbereich | N/U | A/N/U |
| Betrag | stückweise exakte Intervallhülle | A | N | A/N |
| Sinus | symbolisch | U | N | N, Zelle U |
| Cosinus | symbolisch | U | N | N, Zelle U |
| ArcSinus | Definitionsbereich [-1,1] | U | N/U | N, Zelle U |
| ArcCosinus | Definitionsbereich [-1,1] | U | N/U | N, Zelle U |
| Exponentialfunktion | symbolisch | U | N | N, Zelle U |
| NatürlicherLogarithmus | Definitionsbereich >0 | U | N/U | N, Zelle U |
| Wurzel | reell nur für nichtnegative Argumente | U | N/U | N, Zelle U |
| KomplexeZahl | zwei rekursive reelle Komponenten | U | Domänenauswerter | A als Punkt in C, sonst U |
| Argument | für null undefiniert | U | Domänenauswerter | N/U |
| Logarithmus | Basis >0, Basis !=1, Argument >0 | U | N/U | N, Zelle U |
| InversesElement | strukturabhängiger Vertrag | U | U | U |
| StrukturierteDivision | strukturabhängiger Vertrag | U | U | U |
| IterierteSumme | endliche Fälle materialisieren | R/U | R/U | R oder U |
| IteriertesProdukt | endliche Fälle materialisieren | R/U | R/U | R oder U |
| ZahlFallAusdruck | nur bei zellweit entschiedener Bedingung | A/U | N/U | A/N/U |
| DifferentialVariable | Analysis-Fachobjekt | U | U | X |
| DifferentialTerm | Analysis-Fachobjekt | U | U | X |
| SymbolischerHyperReellerWert | Hyperzahl-Fachobjekt | U | U | X |

Die zertifizierten logischen Kombinationen umfassen Wahrheit, Negation,
Konjunktion, Disjunktion, Implikation, Äquivalenz, Gleichheit, Ungleichheit und
Ordnungsvergleiche. Nicht unterstützte Aussagenoperatoren liefern einen Grund und
keine erfundene Boolesche Entscheidung.

## Mengentypen

| Typ | Mitgliedschaft / Normalisierung | Räumliche Materialisierung | Status |
|---|---|---|---|
| LeereMenge | exakt ausgeschlossen | explizit mathematisch leer | A |
| EndlicheMenge | Gleichheitsdisjunktion; symbolische Gleichheit bleibt offen | exakte darstellbare Punkte | A/U |
| BenannteMenge | N ab 1, Z, Q, R und C fachlich getrennt; andere Namen konservativ | N/Z/R in R1; C in C; Q als gemischte R1-Zellen, niemals als volles Intervall | A/N/U/X je Instanz |
| ReellesIntervall | rationale offene/geschlossene Grenzen | exakte R1-Segmente | A |
| Vereinigung | logisches Oder; exakte R1-Algebra | zertifizierte Zellkombination plus Punkte | A/N/U |
| Schnitt | logisches Und; leerer Schnitt erhält Grundmenge | zertifizierte Zellkombination plus Punkte | A/N/U |
| MengenDifferenz | links und nicht rechts | exakte R1-Algebra und Zellkombination | A/N/U |
| SymmetrischeDifferenz | exklusives Oder | exakte R1-Algebra und Zellkombination | A/N/U |
| KartesischesProdukt | faktorweise; C zählt geordnet als Real- und Imaginärteil | endliche/exakte oder rasterbegrenzte Produktdomänen; C×R und R×C entfalten zu R3 | A/N/U |
| Tupelraum | Strukturvertrag | kein endlicher räumlicher Träger ohne Konkretisierung | X |
| Folgenraum | Strukturvertrag | kein endlichdimensionaler Träger | X |
| Vektorraum | Strukturvertrag | keine implizite Basis/Projektion | X |
| Matrizenraum | Strukturvertrag | keine implizite Projektion | X |
| DefinierteMenge | gebundene Variablen und Aussage | Zellzertifikat, Punktvorschau, feste Schnitte, Farbe | A/N/U |
| GefilterteMenge | Grundmenge und Prädikat | nach Reduktion beziehungsweise numerische Region | R/N/U |
| Primzahlen | exakte Fachmitgliedschaft | kein eigener räumlicher Normalisierer | X |
| GaußscheGanzeZahlen | exakte Fachmitgliedschaft | kein eigener C-Rasterpfad | X |
| GaußschePrimzahlen | exakte Fachmitgliedschaft | kein eigener C-Rasterpfad | X |
| Potenzmenge | Mengen-von-Mengen-Vertrag | kein räumlicher Träger | X |
| Abbildungsmenge | Methodenraum | kein räumlicher Träger | X |
| Tensorraum | Strukturvertrag | keine implizite Projektion | X |
| ModuloZahlenraum | endlicher algebraischer Raum | kein eigener Koordinatenpfad | X |
| MengenParameter | benötigt Bindung | ohne Bindung sichtbar unbestimmt | U |
| FehlendeObermenge | explizite Unvollständigkeit | nicht materialisierbar mit Diagnose | U |
| PrädikatsMenge | Prädikat, optional fensterbegrenzt | nur nach ausdrücklicher Fensterfreigabe | N/U |
| BeschraenkteZahlmenge | Träger mit Grenzen | nach vorhandener symbolischer Reduktion | R/U |
| MengenFallAusdruck | entschiedener Zweig oder offen | entschiedener Zweig; sonst unbekannte Zelle | R/U |
| Abbild | Methodenbild einer Domäne | eigener budgetierter Samplingpfad | N/U |
| MethodenGraphMenge | Methoden-Graphvertrag | kein direkter Visualisierungspfad | X |
| IterierteVereinigung | endliche Fälle materialisieren | nach Reduktion | R/U |
| IteriertesKartesischesProdukt | endliche Fälle materialisieren | nach Reduktion | R/U |
| IterierterSchnitt | endliche Fälle materialisieren | nach Reduktion | R/U |
| DifferenzierbarkeitsBereich | Analysis-Vertrag | nur nach exakter Reduktion | R/X |
| AbleitungsZielraum | Analysis-Vertrag | kein direkter räumlicher Träger | X |
| TangentialMenge | Tangentialobjekt-Vertrag | keine implizite Koordinatenwahl | X |
| TopologischerAbschluss | exakte Spezialfälle | nach Reduktion | R/U |
| TopologischesInneres | exakte Spezialfälle | nach Reduktion | R/U |
| TopologischerRand | exakte Spezialfälle | nach Reduktion | R/U |
| TopologischerRandImRaum | relativer Rand | nach Reduktion | R/U |
| GeometrischeTrägermenge | Geometrievertrag | benötigt explizite Koordinatenkonvertierung | X |
| KoordinatenBild | explizite Koordinaten | eigener Adapterpfad | A/N/U |
| OrbitBeschraenktheitsMenge | exakte Zyklen; für z²+c zusätzlich Kardioide, Periode-2-Kreis und Fluchtnachweis | adaptive C-Zellen und Punktvorschau; Budgetende bleibt sichtbar | A/N/U |

## Durchgängige Regeln und belegte Korrekturen

- Exakte R1-Algebra hält rationale Grenzen, Offenheit und Einzelpunkte bis zum
  Fensterbeschnitt getrennt. Die Zeichentoleranz ändert keine Mengentopologie.
- `1 ∈ {x}` ist ohne Bindung unbestimmt; strukturelle Ungleichheit beweist keine
  mathematische Ungleichheit. Das gilt auch für endliche Differenz, Schnitt,
  Teilmenge, Disjunktheit, Mächtigkeit und iterierte Operatoren.
- N beginnt bei 1; Ganzzahligkeit hängt nicht von einer Zeichentoleranz ab.
  Diskrete Produktfaktoren zählen außerhalb des `Long`-Bereichs mit `BigInteger`;
  ein leeres Sichtfenster wird nicht als mathematisch leere Menge ausgegeben.
- Kontinuierliche Produktfaktoren werden vor der Rasterbildung auf das sichtbare
  Fenster geschnitten. Offene Grenzen verwenden keine Zeichentoleranz, sodass
  auch schmale nichtleere Intervalle sichtbar bleiben.
- Ein Vorzeichenwechsel über einer Definitionslücke ist kein Nullstellennachweis.
- Ein Zellstatus behauptet etwas über jeden Punkt der Zelle. Punktstichproben
  füllen keine Zelle. Gemischte und unbekannte Zellen bleiben räumlich sichtbar.
- Nicht dargestellte Komponenten werden ausschließlich über benannte feste
  Schnittwerte gebunden. Es gibt keine stillschweigende Projektion.
- C verwendet Real- und Imaginärteil. Mehrere Farbwerte an derselben Position
  werden zusammengeführt und mit einer Mehrdeutigkeitsmarkierung versehen.
- Orbit-Nichtentkommen innerhalb des Budgets beweist keine Beschränktheit.
  Exakte Periodizität beweist Aufnahme; bei der erkannten quadratischen
  Nullstartfamilie beweist |z_n|>2 Ausschluss. Sonst bleibt das Ergebnis offen.
- Rein endliche Mengenalgebra wird vor Normalisierung nach Ausgabegröße und dem
  Aufwand des vorhandenen CAS-Pfads begrenzt. Insbesondere zählen die paarweisen
  Gleichheitsentscheidungen von Schnitt, Differenz und XOR zum gemeinsamen Budget;
  eine Überschreitung fällt nicht auf unbudgetierte Rastermitgliedschaft zurück.

## Prüfgrenzen

Die Matrix verspricht keine allgemeine Entscheidbarkeit transzendenter Ausdrücke,
unendlicher strukturierter Räume oder generischer dynamischer Systeme. Diese Fälle
werden diagnostiziert und bleiben sichtbar unbestimmt beziehungsweise bewusst
nicht räumlich. Die Android-Touch- und Lebenszyklusprüfung bleibt bis zu einem
verfügbaren Gerät oder erfolgreich bootenden isolierten Emulator offen.
