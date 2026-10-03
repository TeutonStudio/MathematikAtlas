# Mengenvisualisierung und allgemeine Orbitmengen

## Status, Ziel und Nutzerwirkung

Abgeschlossen am 2026-10-03. Produktionsimplementierung, gezielte Regressionen,
vollständiger Prüflauf und unabhängige Knotenprüfung sind abgeschlossen. Die
Releaseintegration folgt getrennt auf Basis des abgenommenen Featurecommits.
Version 2.34.0 ist auf der veröffentlichten Basis 2.33.1 reserviert.

Android soll Mengen in R, R², R³ und C mit räumlich erkennbarer Beweissicherheit
darstellen. Exakte Verfahren haben Vorrang, sofern wirtschaftlich; numerische
Verfahren ergänzen sie. Nicht bestimmbare Bereiche bleiben sichtbar, auch neben
bekannten Treffern. Allgemeine Iterationsmengen müssen eine Mandelbrot-Beispielkarte
ohne speziellen Mandelbrot-Knotentyp ermöglichen.

## Nicht-Ziele

Keine vollständige Entscheidbarkeit beliebiger Mengen, keine implizite Projektion,
keine allgemeinen Refactorings, keine Desktop-UI-Erweiterung, keine Änderung des
Kartenformats und keine stillschweigende Übernahme fremder CAS-PRs. Vorhandene lokale
Gradle-, IDE-, Test- und Dokumentationsänderungen bleiben erhalten.

## Untersuchte Basis und bestätigte Fehler

Ausgangsbasis ist der veröffentlichte Commit
`21ce83fec32438087ae2fa8bd08f17766f0a20ce` (2.33.1). Der SamAI-Arbeitsbranch
`samai/v2.34.0/mengenvisualisierung-orbits` basiert auf dem geprüften
Reservierungscommit `e7d3c47f9a1fc9af45593da72dafb3eb5101faba`. Das ältere
`PROJECT_CONTEXT` nennt noch Kartenformat 5; maßgeblich ist Writerformat 8.

| Codeort | Auslöser / Nutzerwirkung | Korrekturbedingung |
|---|---|---|
| `MathematikRechenSystem/.../kern/Mengen.kt`, `ElementBeziehung` | `1 ∈ {x}` wird bei struktureller Ungleichheit ausgeschlossen | Ohne Bindung unbekannt, mit x=1 bewiesen |
| `MathematikKnoten/.../visualisierung/sampling/ZahlengeradenNormalisierung.kt` | Zeichentoleranz verschmilzt Lücken oder löscht kurze offene Intervalle | Exakte Mengentopologie unabhängig von Anzeigeoptionen |
| `VisualisierungsSampler`, natürliche Zahlen | 0 und toleranznahe Nichtganze werden zugelassen | N beginnt wie im Kern bei 1; Ganzzahligkeit nicht per Zeichentoleranz |
| `VisualisierungsSampler.sampleRegion` | Bekannte Treffer verdrängen unbekannte Teilbereiche | Räumliche Unbekanntheit samt Ursache bleibt im Ergebnis |
| `VisualisierungsSampler`, kombinierte Residuen | x=0 und y=0 können als Linie erscheinen | Konjunktion wird als Schnitt ausgewertet, keine Übernahme nur eines Residuums |
| `VisualisierungsSampler`, Vorzeichenwechsel | 1/x über Null kann als Nullstelle erscheinen | Keine Nullstellenbehauptung ohne Stetigkeits-/Definitionsnachweis |

Alle aufgeführten Fehler sind korrigiert und durch neue oder angepasste
Regressionstests abgedeckt. Android-Laufzeitverifikation auf Gerät oder Emulator
fehlt weiterhin; die JVM- und Buildprüfungen ersetzen sie nicht.

## Mathematische Semantik

Die vorhandenen Entscheidungszustände bleiben maßgeblich. Eine numerische Schätzung
ist zusätzliche Evidenz, kein Beweis. Punktbeweis und Zellbeweis sind getrennt:
vollständig enthalten/ausgeschlossen verlangt einen Nachweis für jeden Punkt der
Zelle. Gemischte und unbekannte Zellen sind unterschiedlich zu kennzeichnen.

Exakte rationale Grenzen und offene/geschlossene Endpunkte bleiben bei
Mengenoperationen erhalten. Strukturelle Ungleichheit beweist keine mathematische
Ungleichheit. Definitionslücken, fehlende Bindungen, nicht unterstützte Operatoren,
numerische Unsicherheit und Budgetende erhalten unterscheidbare Ursachen.

Auswertungsreihenfolge: exakte Normalisierung; zertifizierende Intervallrechnung;
separat gekennzeichnete numerische Vorschau. Zertifizierte Grundoperationen sind
rationale Konstanten, Variablen, Addition, Subtraktion, Multiplikation, Division
ohne Null im Nenner, ganzzahlige Potenzen und Vergleiche. Rundung muss einschließend
sein. Nicht zertifizierte Transzendente bleiben im Beweispfad unbekannt.

Gleichungsmengen werden als Punkte/Kurven/Flächen dargestellt, nicht als bewiesen
gefüllte Volumenzellen. Q und R ohne Q dürfen nicht als vollständige reelle
Intervalle erscheinen. C behält zwei reelle Komponenten, auch bei Imaginärteil 0.

## Daten-, Knoten- und Anschlussvertrag

Neue unveränderliche Kernausdrücke: `OrbitFamilie : MathematischesObjekt` und
`OrbitBeschraenktheitsMenge : MengenAusdruck`. Keine Closures, Cache- oder UI-Objekte
persistieren. Substitution, freie Variablen, Binder, Typableitung, Mitgliedschaft
und LaTeX-Darstellung müssen die neuen Ausdrücke kennen.

| Neuer Typ | Eingänge in Reihenfolge | Ausgang |
|---|---|---|
| `mathematik.orbit` | `schritt`: Methode; `start`: Objekt | `orbit`: Objekt |
| `mathematik.orbitBeschraenktheit` | `orbit`: Objekt; `parameterraum`: Menge | `menge`: Menge |

Alle Eingänge erforderlich, einfach belegbar, links; Ausgang rechts. Vorhandene
Anschlussarten, IDs pro Instanz durch bestehende Vorlagenfabrik; keine dynamischen
Anschlüsse oder Rückkanten. Fachliche Signaturen werden zusätzlich validiert.

Orbit: z_0(p)=s(p), z_(n+1)(p)=F(z_n(p),p), n ab 0. F besitzt zwei Argumente und
eine skalare reelle oder komplexe Ausgabe. Ein Parameter darf zusammengesetzt sein.
Start ist eine konstante Zahl oder mathematische Methode s(p). Inspector wählt
das Zustandsargument (Standard erstes); zweites ist Parameter. Auswahl wird über
bestehende Kartenaktionen persistiert, mit Undo/Redo. Nichtmathematische
Script-/Engine-Methoden werden nicht automatisch ausgeführt.

Beschränktheitsmenge: {p in P | es gibt ein endliches R>=0, sodass für alle n>=0
|z_n(p)|<=R}. Aufnahme verlangt Beweis, etwa exakte Periodizität oder einen
invarianten beschränkten Bereich; Ausschluss verlangt nachgewiesene Divergenz.
Endliches Nichtentkommen sowie Überschreiten eines willkürlichen Radius entscheiden
dieses Prädikat nicht. Undefinierte Orbits sind kein Beweis für Unbeschränktheit.

Mandelbrot-Erkennung nur anhand F(z,c)=z²+c und z_0=0. Sichere Hauptkardioiden- und
Periode-2-Kreistests sowie zertifiziertes |z_n|>2 verwenden; niemals >=2.
Bei x=Re(c), y=Im(c), q=(x-1/4)²+y²: Kardioide q(q+x-1/4)<=y²/4,
Periode-2-Kreis (x+1)²+y²<=1/16. Grenznahe Rechnungen müssen exakt oder
einschließend sein. Nach Budgetende bleibt die Mitgliedschaft unbekannt.
Quelle: https://loiseaujc.github.io/Scientific_Computing_on_a_Laptop/Maths/Mandelbrot/binary_mandelbrot.html

## Architektur und betroffene Komponenten

- Rechenkern: Mengenlogik, exakte Normalisierung, Intervallzertifikate, Orbitobjekte.
- MathematikKnoten: bestehender Katalog und Auswerterregister; keine Parallelregister.
- Visualisierung: strukturierte Zellen mit Grenzen, Status, Grund und Näherung;
  adaptive Intervall-, Quad- und Octree-Unterteilung.
- Android-Inspector: Zuordnung jeder Komponente zu Raumachse, optionaler Farbe
  oder festem Schnittwert. Ignorieren bedeutet ausdrücklich fester Schnitt.
- Renderer: enthalten gefüllt, ausgeschlossen frei, unbekannt schraffiert,
  Näherungskonturen eigener Linienstil; Legende und Bereichsdetails.

Mehrere Farbwerte am selben Ort werden markiert; Details nennen belegte Werte oder
Bereiche mit Vollständigkeitsstatus. Keine willkürliche Auswahl oder Mittelwerte.
Numerisch untersuchte Farbbereiche bleiben als fensterbegrenzt gekennzeichnet.

Auswertung außerhalb des UI-Threads, kooperativ abbrechbar und mit Teilresultaten.
Alte Aufträge dürfen neue Konfigurationen nicht überschreiben. Cache-Schlüssel
berücksichtigen Ausdruck, Annahmen, Bindungen, Fenster und Auswertungsoptionen.
Standard: insgesamt 250.000 Auswertungsschritte, höchstens 256 je Orbit, im
Inspector einstellbar. Beide Grenzen gelten gemeinsam; Budgetende bedeutet unbekannt.

## Meilensteine und Umsetzungsschritte

- [x] Fachlichen Bestand untersuchen und unabhängige mathematische Prüfung einholen.
- [x] Releasebasis klären und zulässige y-Version reservieren.
- [x] Abdeckungsmatrix für alle Zahlen-/Mengenausdrücke und Operatoren erstellen:
  symbolische Semantik, Definitionsbereich, Mitgliedschaft, Numerik, Darstellung.
- [x] Kernregressionen zuerst ergänzen; symbolische Mitgliedschaft und exakte
  Intervalloperationen korrigieren, N/Z-Abweichungen beseitigen.
- [x] Intervallzertifikate und logische Kombination implementieren; adaptive
  Zellen samt Unbekanntheitsgründen in den vorhandenen Sampler integrieren.
- [x] Inspector, komplexe Komponenten, feste Schnitte, Farbe und Legende ergänzen.
- [x] Orbitobjekte, beide Knoten und reguläre Mandelbrot-Standardkarte integrieren.
- [x] Persistenz der Beispielkarte und Abbruchpfade über JVM-Tests prüfen; die
  Android-Laufzeitprüfung bleibt mangels verbundenem Zielgerät als Prüfgrenze offen.
- [x] Unabhängiger node_verifier prüft Diff und Kriterien; Findings korrigieren.
- [x] Fakten in CURRENT_STATE aktualisieren, Architekturentscheidungen als ADR
  festhalten; erst nach Abnahme diesen Plan nach completed verschieben.

## Tests und Validierung

Neue Fälle: 1 in {x}; (0,0.01) bei Zeichentoleranz 0.08; [0,1] vereinigt mit
[1.01,2]; [0,1] ohne {1/2}; (0,1) geschnitten mit (1,2); 0 nicht in N;
1 in N; 1.01 nicht in Z; x=0 und y=0; 1/x=0; bekannte und unbekannte
Teilregionen zugleich; Q und R ohne Q nur als gemischte Zellen; C-Wert 1+0i;
Reihenfolge bei C×R und R×C;
feste Schnitte und mehrwertige Farbe. Für Zertifikate zusätzlich unabhängige
rationale Prüfpunkte verwenden, ohne Stichproben mit Beweisen gleichzusetzen.

Orbitfälle: konstante und exakt periodische Orbits, z_(n+1)=z_n+1 ab 0,
unbekannter generischer Orbit; Start 3 und danach konstant 0 bleibt beschränkt.
Mandelbrot c=0,-1,-2 enthalten, c=1,2 ausgeschlossen; geringe Budgets unbekannt.
Variablenfang, Starttyp, Methodensignatur, fehlende Eingänge und Abbruch prüfen.

Katalog-/Auswertertests, IDs, JSON-Roundtrip, ältere Konfigurationen, Kopieren,
Gruppenkarten, Undo/Redo und unveraltete asynchrone Ergebnisse testen.

Befehle: `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py`,
`pruefe_versionsfolge.py`, `pruefe_standardkarten.py`, `pruefe_methodenmodell.py`,
`pruefe_kern.py`; relevante Gradle-Modultests, dann `./gradlew test :app:assembleDebug`.
Android-Gesten, Inspector, Kartenwechsel und Abbruch auf Gerät oder isoliertem
Emulator prüfen. Keine vorhandenen AVD-Daten löschen. Nicht ausführbare Prüfungen
mit konkretem Grund offenhalten.

## Persistenz, Migration und Rückfallstrategie

Kartenformat 8 bleibt bestehen. Neue optionale Konfigurationsfelder besitzen
kompatible Lese-Defaults; vorhandene IDs/Verbindungen bleiben erhalten. Orbitwerte
werden aus dem Graph erneut berechnet. Alte Apps können neue Knotentypen nicht
vollständig auswerten; dies ist keine Berechtigung, Daten zu verwerfen.
Nicht unterstützte mathematische Verfahren fallen auf unbekannt mit Diagnose
zurück, nicht auf eine falsche Ja/Nein-Entscheidung.

## Versionswirkung und Integrationsrisiko

Zwei neue separat erzeugbare Typen begründen die reservierte y-Version 2.34.0.
2.33.1 ist auf `origin/master` veröffentlicht. Die fremden CAS-PRs #433, #435 und
#436 wurden nicht in diesen Auftrag übernommen. Vor der Integration werden
Versionsachse, Android-Version, Roadmapstatus und vollständiger Diff erneut geprüft.

## Entscheidungsprotokoll und Abweichungen

- Nutzer: R1/R2/R3/C; Sicherheit sichtbar; ausgeblendete Dimensionen als feste
  Schnitte; mehrwertige Farbdimension markieren; neue allgemeine Knoten erlaubt.
- Mathematische Prüfung: Beweis/Schätzung und Punkt/Zelle trennen; generische
  Beschränktheit ist kein Test gegen einen willkürlichen Fluchtradius.
- 2026-10-01: Nach Unterbrechung aktuellen Arbeitsbaum geprüft, unverändert.
  Releaseprüfung erneut beauftragt. Noch kein Produktcode verändert.
- 2026-10-01: Nutzer hat ausdrücklich das Fertigstellen offener Releasearbeit
  vor der Planimplementierung beauftragt. Isolierter Release-Worktree unter
  `/tmp/matlas-release-2331`; PR #439 enthält Dokumentations-/Agentenänderungen.
  SamAI-Commit `44e10827` synchronisiert aktuellen master und die fehlenden
  2.33.1-Metadaten. Commitidentität geprüft, normaler Push auf bestehenden
  PR-Branch erfolgreich. Neue CI und Veröffentlichung sind noch offen.
  Die automatische Freigabeprüfung lehnte zunächst die nur an den Unteragenten
  weitergereichte Autorisierung ab; die erneute Root-Prüfung mit dem direkten
  Nutzerauftrag genehmigte Commit und Push. Kein Force-Push, keine verlorenen
  lokalen Änderungen.
- 2026-10-01: 2.33.1 wurde als Commit `21ce83fe` veröffentlicht; 2.34.0 wurde als
  y-Version mit den Typ-Schlüsseln `mathematik.orbit` und
  `mathematik.orbitBeschraenktheit` reserviert.
- 2026-10-03: Exakte R1-Algebra, rationale Zellzertifikate, sichtbare unbekannte
  Bereiche, R1/R2/R3/C, feste Schnitte, mehrwertige Farbe, kooperativer Abbruch,
  allgemeine Orbitobjekte und beide Knoten sind implementiert. Die Mandelbrotkarte
  wird ausschließlich aus regulären Knoten aufgebaut und als produktive
  Standardkarte installiert.
- 2026-10-03: Die erste unabhängige Verifikation fand offene R1-Filterrouten,
  eindimensionale Tupelbindung, unsichere endliche Double-Algebra, reparierbare
  Inspectorzustände, Ganzzahlbereichsüberlauf und die fehlende Produktinstallation
  der Beispielkarte. Alle Befunde sind korrigiert und durch Regressionen gedeckt;
  die erneute unabhängige Abnahme läuft.
- 2026-10-03: Die zweite Prüfungsrunde fand eine fensterbreitenabhängige
  R1-Gleichheitsschwelle, unklare Produktleere, Long-Überläufe bei diskreten
  Faktoren, fehlendes Sichtfenster-Clipping kontinuierlicher Faktoren und eine
  überlaufende Inspector-Bereichseingabe. Die Produktionspfade sind korrigiert.
  Regressionen decken nun insbesondere `(R ohne Q) × R`, `Q × Q`, schmale offene
  Intervalle, sehr breite Intervalle, `N × R` außerhalb des Sichtfensters,
  Methodenbilder ohne Fenstertreffer sowie Integerbereiche oberhalb `Long` ab.
- 2026-10-03: Die dritte Prüfungsrunde fand einen unbudgetierten Rückfall großer
  endlicher Mengenalgebra auf Rastermitgliedschaft sowie einen zu schmalen
  asynchronen Renderer-Schlüssel. Rein endliche Algebra wird nun vor CAS-Arbeit
  mit `BigInteger` nach Kardinalität und tatsächlichem Operationsaufwand begrenzt.
  Der Renderer schlüsselt auf dem vollständigen bedingten Eingangswert und zeigt
  nichtleere Annahmen an; Änderungen an Annahmen und Laufzeitbindungen brechen
  dadurch den alten Auftrag ab und starten eine neue Auswertung.
- 2026-10-03: Die vierte Prüfungsrunde fand, dass ein komplexer Produktfaktor
  fälschlich nur als eine Koordinate gezählt wurde. Produkte entfalten komplexe
  Faktoren nun geordnet: `C × R` als `(Re, Im, r)` und `R × C` als
  `(r, Re, Im)`. Punktmitgliedschaft respektiert diese Reihenfolge; nicht
  vollständig beweisbare komplexe Faktor-Zellen bleiben unbekannt.
- 2026-10-03: Der unabhängige `node_verifier` hat den finalen Diff ohne
  verbleibenden blockierenden oder nichtblockierenden Befund freigegeben. Die
  C×R/R×C-Regression bestand unabhängig mit erzwungener Neuausführung; die
  fehlende Geräteabnahme und Releaseintegration bleiben ausdrücklich außerhalb
  dieser fachlichen Abnahme offen.
- Bewusste Grenze: Allgemeine Orbitbeschränktheit bleibt außerhalb exakter Zyklen
  offen. Für die erkannte quadratische Nullstartfamilie kommen sichere Innen- und
  Fluchtnachweise hinzu; endliches Nichtentkommen wird nicht als Beweis verwendet.

## Ergebnis und Verifikation

Die gezielten Kern-, Knoten-, Katalog-, Visualisierungs-, Standardkarten- und
JSON-Roundtriptests bestehen. `./gradlew test :app:assembleDebug` bestand zuletzt
am 2026-10-03 mit 149 Tasks; Repository-, Versionsfolge-, Standardkarten-,
Methodenmodell- und Diffprüfung sind erfolgreich.
`pruefe_releaseplan.py` bleibt bis zum Abschluss der reservierten 2.34.0 aktiv;
`pruefe_kern.py` findet in seiner eigenen Umgebung `kotlinc` und `java` nicht,
während die Gradle-Kerntests bestehen. Noch offen sind der finale SamAI-Commit und
die Releaseintegration. Eine Geräteabnahme
ist nicht möglich, weil der vorhandene AVD nicht bootete und ADB in der Sandbox
keine Socket-/Netlink-Rechte erhält.
