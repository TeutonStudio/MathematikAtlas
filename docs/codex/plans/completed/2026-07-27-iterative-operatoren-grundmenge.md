# Zielmengen als Grundmenge iterativer Mengenoperatoren

Status: abgeschlossen

## Ziel und Nutzerwirkung

Eine einwertige mengenwertige Methode versteht ihre deklarierte Zielmenge als feste Grundmenge ihrer Mengenausgaben. Der Schnitt über eine leere Indexmenge ergibt diese abgeleitete Grundmenge.

## Nicht-Ziele

- Kein Potenzmengen-Typ und kein zusätzlich gespeichertes Grundmengenfeld.
- Keine Änderung stabiler Knoten- oder Anschluss-IDs.
- Keine erfundene Zielmenge bei der Migration alter Karten.

## Untersuchten Istzustand

- `Funktion` führt Zielmengen, Wertvorräte, allgemeine Ausgaben und Substitution bereits zentral.
- `iterierterSchnitt` verwendet für leere endliche Indexmengen bereits `einzigeZielMenge`.
- Der Karten-Ausgang besitzt `wert` und `zielmenge`; die Migration ergänzt den fehlenden Anschluss ohne Verbindung.
- Der Adapter überträgt bereits `BedingterWert.zielMenge` in die erzeugte Funktion.
- Die fehlende Absicherung ist die valide mengenwertige Grundmenge inklusive Unabhängigkeit vom Iterationsparameter. Die Teilmengenprüfung ist derzeit nur lokal und nur für zwei endliche Mengen implementiert.

## Fachliche Semantik

Für eine mengenwertige Methode `A : I -> G` modelliert der Atlas die Werte als Teilmengen von `G` (mathematisch präziser wäre `A : I -> P(G)`). Die Zielmenge ist die einzige Quelle der Grundmenge, darf nicht vom einzigen Parameter abhängen und der leere Schnitt ist `G`.

## Architekturentscheidungen

- Eine rekursive Variablenanalyse wird im Rechenkern zentral bereitgestellt und nicht je Operator dupliziert.
- Die Teilmengenprüfung wird als gemeinsame CAS-Funktion umgesetzt; nur sicher widerlegte Beziehungen führen zum Fehler.
- Alle Aufrufer für mengenwertige Iteration nutzen die abgesicherte Grundmengen-API.

## Betroffene Dateien

- `MathematikRechenSystem/.../kern/Funktionen.kt`, `Mengen.kt`, `IterierteOperatoren.kt`
- `MathematikKartenAdapter/.../KartenAuswerter.kt`
- `MathematikKnoten/.../MathematikKnotenRenderer.kt` sowie vorhandene Vorlagen/Registry nur bei nachgewiesenem Bedarf
- vorhandene Modul-Tests, `docs/codex/ARCHITECTURE.md`, ADR

## Meilensteine

- [x] Kernvalidierung, Variablen- und Teilmengenanalyse ergänzt.
- [x] Adapter-, Vorlagen- und Rendererpfad gegen den Kernvertrag geprüft und gezielt korrigiert.
- [x] Fokus-Tests für Semantik, Kartenmetadaten und Determinismus ergänzt.
- [x] Gradle- und Repositoryprüfungen ausgeführt, Diff geprüft.

## Persistenz und Migration

Die persistierte Kartenstruktur bleibt unverändert. Die vorhandene UI-Migration ergänzt ausschließlich den unverbundenen Anschluss `zielmenge` an alte Karten-Ausgänge.

## Risiken und Rückfallstrategie

Symbolische Teilmengenbeziehungen bleiben unentschieden und erhalten die symbolische Auswertung. Falls lokale Gradle-Ausführung an der Umgebung scheitert, werden Befehl und konkrete Ursache dokumentiert.

## Fortschritt

2026-07-27: Dokumentation und Ist-Code geprüft; Umsetzung begonnen.
2026-07-27: Kern-, Adapter-, Knoten- und App-Prüfungen erfolgreich. Die zusätzliche Python-Kernprüfung bleibt wegen fehlendem `kotlinc` nicht ausführbar.

## Entscheidungsprotokoll

2026-07-27: Die Zielmenge bleibt das alleinige gespeicherte Metadatum. Ein abgeleitetes, validierendes API verhindert die Fehlinterpretation allgemeiner Zahlenfunktionen als Mengenmethoden.

## Abweichungen

Keine.

## Ergebnis und Verifikation

- `./gradlew test`, die drei angeforderten Modul-Testtasks und `:app:assembleDebug` waren mit JDK 17 erfolgreich.
- `scripts/pruefe_repository.py` und `scripts/pruefe_architektur.py` waren erfolgreich.
- `scripts/pruefe_kern.py` beendet sich erwartbar mit Code 2, weil in der Umgebung kein `kotlinc` installiert ist; der Gradle-Kern-Test kompiliert und testet den Rechenkern dennoch erfolgreich.
