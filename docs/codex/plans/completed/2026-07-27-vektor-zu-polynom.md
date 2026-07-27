# ExecPlan: Vektor zu Polynom

## Status

- Datum: 2026-07-27
- Verantwortlicher Workflow: `node_planner` → `math_reviewer` → `node_implementer` → `node_verifier`
- Zustand: verifiziert

## Ziel und Nutzerwirkung

Der neue Knoten **„Vektor zu Polynom“** wandelt einen verbundenen Zeilen- oder
Spaltenvektor von Koeffizienten in einen skalaren Zahlterm um. Für
`(c₀, …, cₙ)` und die wählbare Unbestimmte `x` liefert er
`P(x) = c₀ + c₁x + … + cₙxⁿ` (für die Anzeige in konventionell absteigender
Potenzreihenfolge konstruiert). Das Ergebnis kann ohne Sonderpfad an die
vorhandenen Rechnen-, Analysis- und Methoden-Knoten angeschlossen werden.

## Nicht-Ziele

- Kein eigener Polynomtyp, keine Polynomdivision und keine Faktorisierung.
- Keine Festlegung eines Koeffizientenrings oder -körpers, da der Bestand nur
  `ZahlAusdruck` typisiert.
- Keine Polynomfunktion mit Definitions- oder Zielmenge; diese entsteht bei
  Bedarf weiterhin durch `mathematik.termZuMethode`.
- Keine Änderung an bestehenden Vektor-, Term- oder Persistenzformaten.
- Keine separate, persistierte Formel- oder Renderer-Semantik.

## Untersuchter Istzustand

### Relevante Dateien und Symbole

- `MathematikRechenSystem/.../kern/LineareAlgebra.kt`: `OrientierterVektor`
  besitzt die nichtleere Liste `werte: List<ZahlAusdruck>`; konkrete
  `SpaltenVektor` und `ZeilenVektor` sind die zwei zulässigen Orientierungen.
- `MathematikRechenSystem/.../kern/Operatoren.kt`: `addition`,
  `multiplikation`, `Potenz` und `vereinfache` erzeugen die bestehenden
  kanonischen Zahlterme.
- `MathematikRechenSystem/.../kern/Funktionen.kt`: `enthältVariable` prüft
  Variablennamen rekursiv und ist die zentrale Invariante für Koeffizienten.
- `MathematikKnoten/.../MathematikAnschlussArten.kt`: `SpaltenVektor` und
  `ZeilenVektor` sind Unterarten von `Vektor`; `Zahl` ist der bestehende
  skalare Termanschluss.
- `MathematikKnoten/.../MathematikKnotenVorlagen.kt`: zentraler
  Vorlagenkatalog `MathematikKnotenVorlagen.alle`; die Vektorvorlagen zeigen
  Größe, Kategorie und Anschlusskonventionen.
- `MathematikKnoten/.../MathematikAuswerter.kt`:
  `StandardMathematikAuswerter.erzeugeRegister` ist das zentrale
  Auswerterregister; `Vektor`- und `ZeilenVektor`-Auswerter sind vergleichbar.
- `MathematikKnoten/.../MathematikKnotenRenderer.kt` und `LatexText.kt`:
  Knoten zeigen das LaTeX des Ausgabewerts; Multiplikation und Hochstellung
  werden bereits gerendert.
- `KnotenKartenVerwalter/.../logik/GraphPrüfung.kt`: akzeptiert Ausgänge, die
  in der Anschluss-Hierarchie Unterarten des Eingangstyps sind, erlaubt nur
  eine Kante je explizitem Eingang und verhindert Zyklen.
- `app/.../MathematikAtlasApp.kt`, `Inspektor`: Nicht-Spezialknoten zeigen
  ihre Parameterfelder über den bestehenden generischen Pfad. Damit ist
  `variable` editierbar und ein neues Inspector-Register nicht nötig.
- `app/.../speicher/KartenJson.kt`: serialisiert `KnotenDaten.parameter`
  generisch; ein neuer String-Parameter benötigt keine Formatmigration.

### Vergleichbare Implementierungen

`mathematik.vektor` und `mathematik.zeilenVektor` bilden dynamische
Zahleingänge zu orientierten Vektoren. Der neue Knoten ist absichtlich deren
Gegenrichtung für einen einzelnen, fest definierten Vektoreingang. Der
Knoten `mathematik.termZuMethode` belegt, dass ein Zahlterm erst in einem
separaten Schritt zu einer Methode wird.

### Bestätigte Befehle

- Gradle-Wrapper und Module: `settings.gradle.kts`.
- Vollständige JVM-Prüfung laut aktuellem Projektzustand:
  `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test`.
- Debug-Build laut aktuellem Projektzustand:
  `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug`.
- Strukturprüfung: `python3 scripts/pruefe_repository.py`.

## Fachliche und mathematische Semantik

Die Eingabe ist ein **nichtleerer orientierter Koeffizientenvektor**
`c = (c₀, …, cₙ)`. Orientierung und Matrixform haben keine Bedeutung für die
Koeffizientenreihenfolge; nur die Reihenfolge von `werte` ist signifikant.
Die Ausgabe ist der formale Zahlterm

`P(X) = Σᵢ₌₀ⁿ cᵢ · Xⁱ`.

- Der erste Eintrag ist der konstante Koeffizient (aufsteigende Ordnung).
- Die Länge ist eine Gradobergrenze. Insbesondere senkt ein letzter
  Nullkoeffizient den tatsächlichen Grad; das Nullpolynom ist gültig.
- Ein Vektor der Länge eins erzeugt den konstanten Term `c₀`.
- Die Komponente `cᵢ` darf die gewählte Unbestimmte `X` nicht frei enthalten.
  Andere freie Variablen bleiben als parameterabhängige Koeffizienten erlaubt.
- Eine leere Eingabeliste, ein leerer Variablenname oder ein Selbstbezug eines
  Koeffizienten sind ungültige Konfigurationen und erzeugen keinen Ersatzwert.
- Die Kernfunktion verwendet ausschließlich die bestehenden
  Konstruktorfunktionen, damit `0` und `1` wie überall im Rechenkern
  vereinfacht werden. LaTeX wird anschließend nur aus diesem Ergebnis erzeugt.

Die mathematische Prüfung ist **freigegeben mit Auflagen**: aufsteigende
Koeffizientenordnung, beide Vektororientierungen, nichtleere und nicht in
Koeffizienten enthaltene Unbestimmte sowie kein behaupteter Koeffizientenkörper
sind verbindlich.

## Node-, Handle- und Edge-Vertrag

| Feld | Festlegung |
|---|---|
| Node-Art | `mathematik.vektorZuPolynom` |
| Name/Kategorie | `Vektor zu Polynom` / `Vektoren` |
| persistierte Konfiguration | `parameter["variable"]`, Standard `"x"`; beim Auswerten trimmen und nichtleer verlangen |
| Eingang | stabiler Vorlagenname `vektor`; links, Eingang, `mathematik.vektor`, genau eine Kante, nicht dynamisch |
| Ausgang | stabiler Vorlagenname `wert`; rechts, Ausgang, `mathematik.zahl`, beliebig viele Folgekanten |
| Ordnung | Einzige Eingangskante: nicht anwendbar; die Vektorkomponentenordnung ist fachlich signifikant |
| Kompatibilität | Obertyp `Vektor` akzeptiert die bestehenden Ausgänge von Spalten- und Zeilenvektor; Matrix, Tupel und Zahl werden durch `GraphPrüfung` abgelehnt |
| Fehler | fehlende Verbindung, nicht interpretierbarer Vektor, leerer Variablenname oder Selbstbezug werden als `KnotenAuswertungsErgebnis.fehler` sichtbar; keine Kante/kein Ersatzterm |

Instanz-IDs für Knoten und Anschlüsse bleiben durch `KnotenVorlage.erzeuge`
generiert. Die Vorlagennamen `vektor` und `wert` sind die stabilen
Auswertungs- und Persistenzschlüssel; sichtbare Übersetzungen ändern sie nicht.
Der vorhandene Graphvertrag verhindert außerdem eine zweite Eingangskante und
Zyklen.

## Architekturentscheidungen

1. Die Polynomkonstruktion wird als testbare Rechenkernfunktion, beispielsweise
   `polynomAusKoeffizienten(koeffizienten, variable)`, implementiert. Der
   Auswerter konvertiert nur den Eingang und delegiert. Das verhindert eine
   zweite mathematische Wahrheit in Node oder Renderer.
2. Der Ausgang bleibt `Zahl` statt eines neuen `Polynom`-Anschlusses. Ein
   eigener Typ wäre erst gerechtfertigt, wenn Operationen eine Typtrennung
   gegenüber allgemeinen Zahltermen brauchen; der aktuelle Funktionsumfang
   verlangt das nicht.
3. Der Eingang verwendet den gemeinsamen Obertyp `Vektor`, statt zwei
   Duplikatknoten für Zeile und Spalte. Die Orientierung ist für eine
   Koeffizientenfolge nicht semantisch relevant.
4. Es gibt keinen Spezial-Inspector: Der vorhandene generische
   Parameterbearbeitungspfad in `MathematikAtlasApp.Inspektor` zeigt und
   speichert `variable` bereits über `KnotenParameterÄndern`.

## Betroffene Dateien

| Datei | geplante Änderung | Begründung |
|---|---|---|
| `MathematikRechenSystem/.../kern/LineareAlgebra.kt` oder eine fachlich passendere vorhandene Kerndatei | zentrale Funktion zur Polynomkonstruktion und Kern-Tests | Vektor-zu-Term-Semantik bleibt Compose- und Graph-frei |
| `MathematikKnoten/.../MathematikKnotenVorlagen.kt` | Vorlage und Aufnahme in `alle` | zentraler Katalog- und Erzeugungspfad |
| `MathematikKnoten/.../MathematikAuswerter.kt` | Registry-Eintrag und Delegation an den Kern | bestehender Auswertungspfad |
| `MathematikKnoten/src/test/...` | Node-, Registry- und Integrationsfälle | Vertrag für Handles und Auswertung |
| `MathematikRechenSystem/src/test/...` | reine Semantik- und Randfalltests | niedrigste sinnvolle Testebene |
| `app/src/test/.../speicher/KartenJsonTest.kt` | Parameter-Roundtrip ergänzen, sofern neuer Node dort referenziert wird | Persistenz des Parameters nachweisen |
| `app/.../KnotenInspektoren.kt` | keine Änderung geplant | der generische Inspectorpfad deckt den einzelnen String-Parameter ab |
| `docs/codex/CURRENT_STATE.md` | nach Verifikation Fakten zu Node, Prüfungen und Testpfaden aktualisieren | Dokumentation folgt nachweisbarem Istzustand |

## Meilensteine

- [x] M1: Der generische Parameter-Inspector ist bestätigt; die reine
  Kernfunktion `polynomAusKoeffizienten` und ihre Randfalltests sind ergänzt.
- [x] M2: Vorlage und Auswerterregistrierung sind integriert; der
  gemeinsame Vektoreingang akzeptiert beide Orientierungen.
- [x] M3: JSON-Roundtrip, vollständige JVM-Tests, Debug-Build,
  Repositoryprüfung und Diff-Prüfung sind erfolgreich.
- [x] M4: `node_verifier` hat Diff, Architektur, mathematische Auflagen,
  Fehlerpfade und Persistenz unabhängig geprüft und abgenommen.

## Konkrete Umsetzungsschritte

1. Vor jedem Schreiben den aktuellen Arbeitsbaum prüfen und die vorhandene,
   fremde Änderung an `2026-07-27-loesungsmenge-visualisierung.md` unangetastet
   lassen.
2. Eine öffentliche Kernfunktion implementieren: Nichtleere Koeffizienten
   validieren, `variable.name.trim()` prüfen, Selbstbezug über
   `enthältVariable` verbieten und die Terme `c₀`, `c₁·X`,
   `cᵢ·Potenz(X,i)` erzeugen; die Terme für die konventionelle Anzeige in
   absteigender Potenzreihenfolge an `addition` übergeben und anschließend
   normalisieren.
3. Kern-Unit-Tests für Standardfall, Nullpolynom, Konstanten, variablen Namen,
   Zeilen-/Spaltenäquivalenz und verbotenen Selbstbezug ergänzen.
4. In `MathematikKnotenVorlagen` die Vorlage mit genau den zwei beschriebenen
   Anschlüssen erstellen und einmal in `alle` registrieren.
5. Im existierenden `StandardMathematikAuswerter` registrieren: Eingang als
   `OrientierterVektor` lesen, `werte` an den Kern delegieren, Annahmen
   unverändert weitergeben und eine verständliche Fehlernachricht aus
   Validierungsfehlern liefern.
6. Den vorhandenen Renderer und den bestätigten generischen Inspector nutzen;
   keine Formelspeicherung, Sonderlogik oder einen Spezial-Inspector ergänzen.
7. Node- und Graphvertragsfälle sowie JSON-Roundtrip mit `variable` testen;
   Kopieren und Undo/Redo über die bestehenden Aktionen gezielt prüfen, sofern
   die Testhilfen diese Operationen bereits abdecken.
8. `node_verifier` führt die unabhängige Abschlussprüfung durch. Danach
   Dokumentation aktualisieren, Plan nach `plans/completed/` verschieben und
   nur bei einer dauerhaften, über den Knoten hinausgehenden Entscheidung ein
   ADR erstellen.

## Persistenz und Migration

Der Knoten nutzt nur die bestehende String-Parameter-Map und die generisch
serialisierten Anschlüsse. Neue Karten speichern `variable: "x"`; beim Laden
einer hypothetischen unvollständigen Instanz gilt das sichere Default `x`.
Bestehende Karten kennen die neue Art nicht und werden nicht migriert, weil
keine vorhandene Node-Datenstruktur umgedeutet wird. Ein Schema-Bump ist nicht
erforderlich. Der Test muss belegen, dass der Parameter und die konkreten,
instanzstabilen Anschlussreferenzen im JSON-Roundtrip erhalten bleiben.

## Tests und Validierung

| Prüfung | Befehl oder Methode | erwartetes Ergebnis |
|---|---|---|
| Kernsemantik | gezielte JVM-Tests im Modul `MathematikRechenSystem` | aufsteigende Ordnung, `0`, Konstanten, Selbstbezug und Variablenname sind abgedeckt |
| Node-Vertrag | gezielte JVM-Tests im Modul `MathematikKnoten` | Vorlage, Registry, beide Orientierungen, fehlender Eingang und LaTeX aus Ausgabewert funktionieren |
| Graphintegration | Test mit `GraphPrüfung` | Spalten- und Zeilenvektor sind zulässig; Matrix/Tupel sind unzulässig; zweiter Eingang und Zyklus bleiben unzulässig |
| Persistenz | `app`-JVM-Test für `KartenJson` | `variable`, Node-Art und Anschlussreferenzen überstehen Schreiben/Laden |
| Gesamtprüfung | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test` | alle JVM-Tests erfolgreich |
| Build | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug` | Debug-Build erfolgreich |
| Struktur | `python3 scripts/pruefe_repository.py` | Repositoryprüfung erfolgreich |
| Abschluss | `git diff --check` und gezielte Diff-Prüfung | keine Whitespace- oder unbeabsichtigten Änderungen |

## Risiken und Rückfallstrategie

- Der gegenwärtige LaTeX-Renderer kann negative Summanden als `+ -…` darstellen.
  Das ist kein Grund, die fachliche Polynomkonstruktion in die Darstellung zu
  verlagern; eine spätere Anzeigeverbesserung ist ein separates Thema.
- Die vorhandenen Vektortypen erzwingen Nichtleere, aber der Kern prüft sie
  zusätzlich, damit die Fachfunktion unabhängig korrekt bleibt.
- Der generische Inspectorpfad ist geprüft; Änderungen am Parameter werden
  unmittelbar als `KnotenParameterÄndern` in die bestehende Undo/Redo-Historie
  geschrieben.
- Kann die allgemeine Vereinfachung einen erwarteten Strukturtest verändern,
  prüfen die Tests semantisch (oder gegen die aktuelle kanonische LaTeX-Ausgabe)
  statt eine nicht garantierte Baumform festzuschreiben.

## Fortschritt

- [x] 2026-07-27: Pflichtdokumentation, Knotenpfade, Rechenkern, Registrierung,
  Graphprüfung, Renderer und Persistenz gelesen.
- [x] 2026-07-27: Mathematikprüfung freigegeben mit den oben dokumentierten
  Auflagen.
- [x] 2026-07-27: Rechenkern, Vorlage, Auswerter und Tests implementiert.
  `./gradlew test`, `:app:assembleDebug`, `pruefe_repository.py` und
  `git diff --check` waren erfolgreich; der erste Testlauf fand und bestätigte
  eine korrigierte `Int`-zu-`Long`-Konvertierung beim Potenzexponenten.
- [x] 2026-07-27: `node_verifier` hat den zunächst fehlenden Default für den
  nicht persistierten Parameter `variable` beanstandet. Der Auswerter fällt
  bei fehlendem Schlüssel nun auf `x` zurück, behandelt explizit leere Werte
  weiter als Fehler; der Regressionstest und die Nachprüfung sind erfolgreich.

## Entscheidungsprotokoll

| Datum | Entscheidung | Alternativen | Begründung |
|---|---|---|---|
| 2026-07-27 | Aufsteigende Koeffizientenfolge `c₀,…,cₙ` | absteigende Ordnung | Der konstante Eintrag liegt an Index 0; die Abbildung ist eindeutig und im Plan ausdrücklich sichtbar. |
| 2026-07-27 | Gemeinsamer Eingangstyp `Vektor` | nur Spaltenvektor; zwei getrennte Knoten | Zeilen- und Spaltenorientierung enthält keine zusätzliche Information für eine Koeffizientenfolge. |
| 2026-07-27 | Ausgabe als `Zahl` | neuer Polynomtyp; `Funktion` | Der Rechenkern und der Graph führen Zahlterme bereits durch die benötigten Folgeoperationen; eine Funktion benötigt zusätzliche Mengenmetadaten. |
| 2026-07-27 | Kernfunktion statt Auswerterlogik | Konstruktion im Node oder Renderer | Die mathematische Semantik bleibt isoliert testbar und wird nur einmal definiert. |

## Abweichungen vom Plan

Keine. Die fremde, bereits vorhandene Änderung an
`docs/codex/plans/active/2026-07-27-loesungsmenge-visualisierung.md` bleibt
ausdrücklich außerhalb dieses Plans.

## Abnahmekriterien

- [x] Ein Spalten- und ein Zeilenvektor `(2, -3, 5)` liefern bei Variable `x`
  denselben Zahlterm `5x² - 3x + 2` gemäß der bestehenden kanonischen Ausgabe.
- [x] `(0, 0)` liefert `0`; `(7)` liefert `7`; `(0, 1)` liefert `x`.
- [x] Für die Variable `y` enthält `(2, 3)` die Unbestimmte `y`; ein
  Koeffizient, der `y` enthält, führt zu einem verständlichen Fehler.
- [x] Der Eingang akzeptiert beide Vektororientierungen, aber keine Matrix,
  kein Tupel und keine Zahl.
- [x] Ein fehlender Vektoreingang erzeugt keinen Ersatzterm und beschädigt
  keine anderen Knotenergebnisse.
- [x] Der Knoten ist über Vorlage und Auswerterregister erreichbar und zeigt
  ausschließlich den aus dem Ergebnis abgeleiteten Formeltext.
- [x] Nach Speichern/Laden bleiben Node-Art, `variable`, Anschluss-IDs und
  eine gültige Verbindung erhalten.
- [x] Gezielte Tests, vollständige JVM-Tests, Debug-Build und
  Repositoryprüfung sind mit Ergebnis dokumentiert.

## Ergebnis und Verifikation

Implementierung und unabhängige Verifikation sind abgeschlossen. Der Plan wird
nach `docs/codex/plans/completed/` verschoben.
