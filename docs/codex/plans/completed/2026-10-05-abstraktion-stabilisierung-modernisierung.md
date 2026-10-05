# Abstraktion, Stabilisierung und Modernisierung

## Status

Begonnen am 2026-10-05 auf `samai/v3.0.0/alpha` nach dem geprüften
Ausgangscommit `c3cc1edc`.

## Ziel und Nutzerwirkung

Die App erkennt beweisbar gleichwertige, kürzere Teilgraphen und kann sie als
eine atomare, rückgängig machbare Kartenaktion ersetzen. Die erste Regel bildet
explizite Polynom-Rechengraphen auf den vorhandenen Tupel- und Polynomrechner ab.
Parallel werden bekannte Datenverlust- und Importfehler stabilisiert und die
gemeinsamen Module für eine unterstützte Android-/Desktop-Architektur bereinigt.

## Nicht-Ziele

- kein neuer Knotentyp und keine ungeprüfte heuristische Optimierung,
- kein globales „Alle ersetzen“,
- keine Änderung des Kartenformats ohne nachgewiesenen Schemabedarf,
- keine direkte Integration nach `master`.

## Untersuchter Istzustand

- `KartenEditorZustand` speichert vor einer Aktion vollständige immutable
  `KartenDaten` und eignet sich damit für atomare Teilgraphaktionen.
- `AuswahlZuKarte.vorschauFürNeueKarte` enthält bisher app-lokale
  Teilgraph-Grenzlogik.
- `KartenAuswerter` liefert pro Ausgang `BedingterWert` und damit die benötigte
  semantische Schnittstelle.
- Tupel und Polynom werden bereits über `konfiguriereTupel` sowie
  `konfiguriereErweitertenZahlenRechner` konfiguriert.
- Der Basistest `./gradlew test :app:assembleDebug` war vor Beginn erfolgreich.

## Fachliche und mathematische Semantik

`zerlegeAlsPolynom` beweist die Zerlegung rekursiv über Konstanten, Variable,
Addition, Multiplikation und nichtnegative ganzzahlige Potenzen. Andere freie
Variablen sind symbolische Koeffizienten. Numerische Stichproben sind kein
Beweis. Ein Vorschlag ist nur gültig, wenn Ausgangsart, Typ, Term-/Methodenform,
Signatur, Zielmenge, Wertevorräte und Annahmen erhalten bleiben.

## Daten-, Node-, Handle- und Edge-Vertrag

Der fachneutrale `TeilgraphErsetzungsPlan` enthält einen exakten Snapshot der
betroffenen Knoten und inzidenten Verbindungen. Grenzverbindungen werden
ausdrücklich ersetzt und behalten ihre IDs. Vor Anwendung werden Existenz,
Unverändertheit, Anschlusskompatibilität, Kardinalität, externe Nutzung und
Zyklusfreiheit erneut geprüft. Zielknoten entstehen ausschließlich über die
bestehenden Konfiguratoren.

## Architekturentscheidungen

- Teilgraphanalyse und -aktion: `KnotenKartenVerwalter`.
- reine Polynomzerlegung: `MathematikRechenSystem`.
- Regel, Index, Zielknotenkonfiguration und semantischer Nachweis:
  `MathematikKnoten`.
- Dialog und App-Koordination: `app`.
- Der Ersetzungsplan ist die kanonische Quelle für Zählwerte; Vorschläge halten
  keine duplizierten Knoten- oder Verbindungsmengen.

## Betroffene Symbole

- `KartenDaten.analysiereTeilgraph`, `TeilgraphErsetzungsPlan`,
  `KartenAktion.TeilgraphErsetzen`
- `zerlegeAlsPolynom`
- `AbstraktionsRegel`, `AbstraktionsRegister`, `PolynomAbstraktionsRegel`
- `DialogWerkzeugLeiste`, `AbstraktionsWerkzeugDialog`, `AtlasZustand`

## Meilensteine und Fortschritt

- [x] Ausgangsänderungen separat gesichert; Commit `c3cc1edc`, JVM-Tests und APK erfolgreich.
- [x] Fachneutrale Teilgraphanalyse, Vorschau und atomare Aktion.
- [x] Exakte Polynomzerlegung mit Kerntests.
- [x] Abstraktionsregister, Polynomregel und semantische Regressionen.
- [x] Dialog, Auswahl, Anwendung und Neuberechnung.
- [x] Persistenz-/Importstabilisierung und Redundanzprüfung.
- [x] Unterstützte KMP-Migration oder konkret dokumentierte technische Sperre.
- [x] Vollständige Prüfungen, Dokumentationsabgleich und Abschlussdiff.

## Konkrete Umsetzungsschritte

1. Grenzlogik aus `AuswahlZuKarte` extrahieren und dort wiederverwenden.
2. Snapshot-basierten Ersetzungsplan, Vorschaufehler und Kartenaktion ergänzen.
3. Polynomkoeffizienten exakt zerlegen und Operationen budgetieren.
4. Auswertungsindex und rückwärts laufende Polynomregel implementieren.
5. Zielknoten konfigurieren, extern genutzte Zwischenknoten ausschließen und
   Ersatzkarte erneut auswerten.
6. Dialog an `auswertungsRevision` koppeln und stale Vorschläge sichtbar
   ablehnen.
7. Bekannte Speicher-/Importfehler und anschließend redundante Produktpfade
   bereinigen.
8. Gemeinsame Quellen auf die unterstützte KMP-Konfiguration migrieren.

## Tests und Validierung

Gezielte Kern-, Graph-, Knoten- und App-Tests laufen nach jedem Meilenstein.
Abschlussbefehle sind Repository-, Releaseplan-, Versionsfolge- und Kernprüfung,
`./gradlew test`, `:app:lintDebug` und `:app:assembleDebug`. Eine physische
Geräteprüfung bleibt als getrennte Laufzeitprüfung zu protokollieren.

## Persistenz und Migration

Die Abstraktion verwendet ausschließlich bestehende Knotenarten und das aktuelle
Graphschema. Es ist keine Formatmigration vorgesehen. Stabilisierung des
Speicherpfads darf bestehende Karten und Backups nicht überschreiben oder
still löschen.

## Risiken und Rückfallstrategie

- Falsch positive Ersetzungen werden durch exakte Zerlegung, externe
  Nutzungsprüfung, erneute Graphprüfung und semantischen Vergleich verhindert.
- Jeder Meilenstein bleibt ein separater SamAI-Commit und kann unabhängig
  rückgängig gemacht werden.
- Die KMP-Migration beginnt erst auf grünem Feature- und Stabilisierungstand.

## Entscheidungsprotokoll

- 2026-10-05: Bestehende Produktänderungen vor Beginn separat committen, damit
  neue Architekturänderungen überprüfbar bleiben.
- 2026-10-05: Ersetzungsplan statt duplizierter Vorschlagsfelder als einzige
  Quelle für entfernte und neue Graphobjekte verwenden.

## Abweichungen vom ursprünglichen Plan

- Eine KMP-Migration ist in diesem Fachrefactor technisch gesperrt: Das Repository
  verwendet Kotlin 2.3.21 mit AGP 9.4.0, außerhalb des dokumentierten
  unterstützten KMP-AGP-Pfads. Ein Downgrade oder eine nicht unterstützte
  Kombination widerspräche `ARCHITECTURE.md` und ADR 2026-08-09; die physische
  Shadowmodul-Ablösung bleibt deshalb in Issue #395.
- Die ältere JSON-Dialogdatei ist keine vollständig tote Kopie: Sie enthält
  weiterhin gemeinsam verwendete Analysemodelle und getestete Hilfsfunktionen.
  Eine Löschung ohne vorherige Extraktion führte nachweisbar zu
  Kompilierungsfehlern. Diese Entkopplung bleibt ein eigenes Refactoring.

## Ergebnis und Verifikation

Umgesetzt sind die gemeinsame Teilgraphgrundlage, die exakte
Polynomzerlegung, das erweiterbare Abstraktionsregister, die sichere
Polynomregel, der App-Dialog und die atomare Anwendung. Ergänzend wurden
Lebenszyklus-Speichern, `.matlas`-Import, Importfehlergrenze, Fallback auf
lesbare Kartenversionen und Datei-I/O stabilisiert.

Erfolgreich liefen:

- `python3 scripts/pruefe_repository.py`
- `python3 scripts/pruefe_releaseplan.py`
- `python3 scripts/pruefe_versionsfolge.py`
- `./gradlew test :app:lintDebug :app:assembleDebug`

`python3 scripts/pruefe_kern.py` blieb eine Umgebungsgrenze, weil das Skript
kein `kotlinc` und `java` fand. Ein Emulator-/Gerätetest war nicht Bestandteil
dieser lokalen Prüfung.
