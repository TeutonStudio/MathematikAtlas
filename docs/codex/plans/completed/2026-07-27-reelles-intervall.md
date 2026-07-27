# ExecPlan: Knoten »Reelles Intervall«

## Status

- Datum: 2026-07-27
- Verantwortlicher Workflow: `node_planner` → `math_reviewer` → `node_implementer` → `node_verifier`
- Zustand: abgeschlossen; mathematisch freigegeben und unabhängig abgenommen

## Ziel und Nutzerwirkung

Der Einfügedialog enthält in der Kategorie »Mengen« den Knoten »Reelles Intervall«. Zwei geordnete Zahl-Eingänge erzeugen die abgeschlossene reelle Menge zwischen unterer und oberer Grenze. Für verbundene Zahlen `a` und `b` erscheint die Formel `[a,b]`; die Ausgabe ist mit allen bestehenden Mengen-Eingängen kompatibel.

## Nicht-Ziele

- Keine offenen, halboffenen oder unbeschränkten Intervallvarianten und keine konfigurierbare Randinklusion.
- Keine Approximation mit `Double`, keine Ordnung komplexer Zahlen und keine neue Anschlussart.
- Keine spezielle Intervall-Visualisierung; der vorhandene Visualisierungsknoten darf weiterhin nur seine bisher unterstützten Mengenformen annehmen.
- Keine Änderung des JSON-Formats oder Migration bereits gespeicherter Knoten.

## Untersuchter Istzustand

### Relevante Dateien und Symbole

- `MathematikRechenSystem/.../kern/Mengen.kt`: bestehende Mengen-Ausdrücke, Mengenoperationen, `ElementBeziehung`, Kardinalität und Mengenprädikate. Es gibt keinen Intervalltyp.
- `MathematikRechenSystem/.../kern/Funktionen.kt`: vollständige Substitution (`ersetze`) und Variablenanalyse (`enthalteneVariablen`) für zusammengesetzte Mengen.
- `MathematikKnoten/.../MathematikKnotenVorlagen.kt`: zentrale Katalogquelle; Hilfen `eingang` und `ausgang`; bestehende `EndlicheMenge` und Zahlbereiche als vergleichbare Mengenknoten.
- `MathematikKnoten/.../MathematikAuswerter.kt`: Registry `StandardMathematikAuswerter.erzeugeRegister`; `BedingterWert.istNachweisbarReell()` erzwingt bereits beim Extremwert die Reellheit.
- `MathematikKnoten/.../MathematikKnotenRenderer.kt`: die Standarddarstellung rendert das LaTeX des Ausgabewerts, sodass kein Spezialrenderer nötig ist.
- `MathematikKnoten/.../MathematikAnschlussArten.kt`: `mathematik.menge` ist die bestehende Ausgangsart und Unterart von `mathematik.objekt`.
- `KnotenKartenVerwalter/.../logik/GraphPrüfung.kt` und `AnschlussArtRegister.kt`: Ein Eingang hat höchstens eine eingehende Kante; Ausgänge der Art `Menge` sind zu Mengeneingängen kompatibel.
- `app/.../KnotenInspektoren.kt` und `MathematikAtlasApp.kt`: der generische Inspector zeigt nur persistierte Parameter. Dieser Knoten hat keine editierbare Konfiguration; ein neuer Inspector wäre Schattenzustand ohne Zweck.
- `app/.../speicher/KartenJson.kt`: serialisiert Node-Art, Anschlüsse und Parameter bereits generisch, einschließlich stabiler Anschluss-IDs.

### Vergleichbare Implementierungen

- `EndlicheMenge` hat einen Mengen-Ausgang, aber Parameter statt Eingängen.
- `Maximum`/`Minimum` in Vorlage und Auswerter akzeptieren ausschließlich nachweisbar reelle `BedingterWert`-Eingaben und sind daher das Vorbild für die Reellheitsprüfung.
- `LösungsmengeUndVisualisierungTest` zeigt die Konvention für Vorlagen- und Registry-Tests; `DefinierteMengeTest` prüft Kern-LaTeX und Variablenbindung.

### Bestätigte Befehle

- Gradle Wrapper (kein `package.json`, keine Paket-Lockdatei): `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test`
- Debug-Build: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug`
- Repository-Prüfung: `python3 scripts/pruefe_repository.py`

## Fachliche und mathematische Semantik

Der Knoten bildet das **abgeschlossene** reelle Intervall

```text
[a,b] = { x ∈ ℝ | a ≤ x ≤ b }.
```

Beide Grenzen müssen als `BedingterWert.istNachweisbarReell()` nachweisbar reell sein. Damit sind rationale Zahlen sowie reelle Variablen und reellheitserhaltende Terme mit vorhandenen Metadaten zulässig; komplexe oder nicht nachweisbar reelle Terme führen zu einem fachlichen Auswertungsfehler.

Die Fabrik im Rechenkern entscheidet genau folgende Fälle:

- Zwei vereinfachbare rationale Grenzen mit `a > b` ergeben `LeereMenge`.
- Zwei gleiche vereinfachbare rationale Grenzen ergeben `EndlicheMenge(setOf(a))`.
- Zwei rationale Grenzen mit `a < b` ergeben `ReellesIntervall(a, b)`.
- Für symbolische, aber nachweisbar reelle Grenzen entsteht `ReellesIntervall(a, b)` ohne zusätzliche Ordnungsannahme. Die Begrenzung ist damit ein symbolischer Ausdruck, nicht die Behauptung, dass `a ≤ b` bereits bewiesen ist.

`ReellesIntervall` ist ein fachlicher `MengenAusdruck` mit den Feldern `untereGrenze: ZahlAusdruck` und `obereGrenze: ZahlAusdruck`. Seine LaTeX-Repräsentation ist `\\left[${untereGrenze.zuLatex()},${obereGrenze.zuLatex()}\\right]`.

Für rationale Elemente und rationale Grenzen entscheidet `ElementBeziehung` die Mitgliedschaft exakt. Der Umfang umfasst darüber hinaus keine vollständige Entscheidungslogik für symbolische Mitgliedschaft, Teilmengen oder Mächtigkeiten eines Intervalls. Diese bestehenden Operationen liefern außerhalb ihrer derzeit beweisbaren Fälle weiterhin »unbekannt« bzw. ihren dokumentierten Fehlerzustand.

## Node-, Handle- und Edge-Vertrag

| Schlüssel | Richtung / Kante | Art | Kardinalität | Ordnung | Bedeutung / Fehlermodus |
|---|---|---|---|---|---|
| `untereGrenze` | Eingang / links | `mathematik.zahl` | genau eins | 0 | untere Intervallgrenze; fehlt oder nicht reell → Auswertungsfehler |
| `obereGrenze` | Eingang / links | `mathematik.zahl` | genau eins | 1 | obere Intervallgrenze; fehlt oder nicht reell → Auswertungsfehler |
| `menge` | Ausgang / rechts | `mathematik.menge` | eine erzeugte Ausgabe | 0 | `LeereMenge`, einelementige `EndlicheMenge` oder `ReellesIntervall` |

- Node-Art/Registry-Schlüssel: `mathematik.reellesIntervall`.
- Katalogname: `Reelles Intervall`; Kategorie: `Mengen`.
- Beide Eingänge sind nicht dynamisch (`kannSichErweitern = false`); der Node hat keinen Parameter und keine eigene Schemaversion.
- Instanz-Anschluss-IDs entstehen wie bei jeder `KnotenVorlage` über `erzeuge` neu, bleiben anschließend beim Speichern, Laden, Kopieren und Undo/Redo stabil.
- Der bestehende `GraphPrüfung`-Pfad akzeptiert nur Ausgang → Eingang, eine hierarchisch kompatible `Zahl`-Quelle und höchstens eine Kante je Eingang; Zyklen bleiben verboten. Keine Änderung daran.

## Architekturentscheidungen

1. Die Menge wird als eigener Kern-Ausdruck modelliert, nicht als LaTeX-Text und nicht als `DefinierteMenge`. Das bewahrt die Intervallstruktur für spätere Mengenoperationen und hält Darstellung von Semantik getrennt.
2. Die Vereinfachung zu leerer/einelementiger Menge liegt in einer reinen Kernfabrik (`reellesIntervall`), nicht im Compose-Renderer oder Registry-Auswerter.
3. Der Registry-Auswerter validiert die Laufzeitmetadaten der verbundenen Werte, weil die graphische Anschlussart `Zahl` allein Reellheit nicht ausdrückt.
4. Es gibt keine persistierte Konfiguration. Daher ist kein Inspector, kein JSON-Formatwechsel und keine Lade-Migration erforderlich.

## Betroffene Dateien

| Datei | geplante Änderung | Begründung |
|---|---|---|
| `MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Mengen.kt` | `ReellesIntervall` und die zentrale Fabrik `reellesIntervall(untereGrenze, obereGrenze, kontext)` ergänzen; rationale Grenzfälle exakt reduzieren. | Fachliche Quelle der Wahrheit. |
| `MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Funktionen.kt` | Substitution, Objektvereinfachung und Variablenanalyse um `ReellesIntervall` ergänzen. | Erhält die Verträge aller zusammengesetzten Mengen. |
| `MathematikRechenSystem/src/test/kotlin/de/TeutonStudio/MathematikRechenSystem/DefinierteMengeTest.kt` oder neue fokussierte `ReellesIntervallTest.kt` | Kern-Tests für LaTeX, rationale Sonderfälle, Substitution und freie Variablen. | Niedrigste sinnvolle Testebene. |
| `MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt` | Vorlage `ReellesIntervall` definieren und in `alle` aufnehmen. | Zentraler Einfügekatalog. |
| `MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt` | Registry-Eintrag `mathematik.reellesIntervall`; beide Eingänge lesen, Reellheit prüfen, Kernfabrik aufrufen und Annahmen/Reellheitsmetadaten weiterreichen. | Graphintegration ohne parallele Semantik. |
| `MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/LoesungsmengeUndVisualisierungTest.kt` oder neue fokussierte `ReellesIntervallKnotenTest.kt` | Vorlage/Handle-Vertrag, Registry-Auswertung, symbolische reelle Eingaben und Ablehnung komplexer Eingaben testen. | Node-Vertrag und Adaptergrenze. |
| `app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/speicher/KartenJsonTest.kt` | Roundtrip des erzeugten Intervallknotens mit beiden Anschluss-IDs ergänzen. | Persistenzvertrag für den neuen Knotentyp. |
| `docs/codex/CURRENT_STATE.md` | Nach Abnahme nur nachweisbare Fakten zum neuen Knotentyp, Semantik und Prüfstatus ergänzen. | Dokumentation bleibt synchron. |

Nicht zu ändern: `MathematikAnschlussArten.kt`, `GraphPrüfung.kt`, `KnotenInspektoren.kt`, `KartenJson.kt`, `MathematikKnotenRenderer.kt`, `AtlasZustand.kt` und der Visualisierungs-Sampler, sofern die Umsetzung keine konkrete Kompilierlücke zeigt.

## Meilensteine

- [x] M1: Kernmodell mit exakter rationaler Fallunterscheidung sowie Substitution/Variablenanalyse implementiert und Kern-Tests grün.
- [x] M2: Vorlage und Registry-Auswerter implementiert; der Knoten lässt sich im vorhandenen Katalog einfügen und auswerten.
- [x] M3: Persistenztest, vollständige passende Prüfungen, Diff-Prüfung und unabhängige Abschlussverifikation abgeschlossen.

## Konkrete Umsetzungsschritte

1. Im Rechenkern `ReellesIntervall` als `MengenAusdruck` ergänzen; keine `Double`-Konvertierung verwenden.
2. Eine zentrale Fabrik schreiben, die Grenzen mit `vereinfache(..., kontext)` auswertet und nur bei zwei `RationaleZahl`-Werten nach Ordnung in `LeereMenge`, Singleton oder Intervall aufteilt. Symbolische Grenzen bleiben in vereinfachter Form Intervallgrenzen.
3. `ersetze`, `vereinfacheObjekt` und `enthalteneVariablen` für den neuen strukturellen Ausdruck erweitern.
4. Die statische Vorlage mit Größe ungefähr anderer zweieingängiger Mengenknoten, Eingängen `untereGrenze`/`obereGrenze` und Ausgang `menge` einfügen; in `alle` registrieren.
5. In der bestehenden Auswerter-Registry beide Eingaben als `ZahlAusdruck` lesen, beide `BedingterWert` auf `istNachweisbarReell()` prüfen, bei Verstoß eine eindeutige Meldung liefern und andernfalls `BedingterWert(reellesIntervall(...), annahmen(k), reelleVariablen = reelleVariablen(k.eingänge.values))` zurückgeben.
6. Prüfen, dass der Standardrenderer die Ausgabe bereits aus `zuLatex()` zeigt. Keinen Sonderfall hinzufügen, wenn `[a,b]` korrekt durch `LatexText` erscheint.
7. Tests ergänzen, danach erst `CURRENT_STATE.md` und die Meilensteine mit tatsächlich ausgeführten Ergebnissen aktualisieren.

## Persistenz und Migration

Der Knoten speichert ausschließlich den generischen Art-Schlüssel und seine zwei generierten Anschlussobjekte; er hat keine Parameter oder Eigenschaften. `KartenJson` serialisiert dies bereits vollständig. Alte Karten können den neuen Typ nicht enthalten, daher ist keine Migration notwendig. Kopieren erzeugt gemäß bestehendem Editorverhalten neue Node- und Anschluss-IDs; Laden bewahrt gespeicherte IDs und Edges. Der Roundtrip-Test muss das für beide Eingänge und den Ausgang nachweisen.

## Tests und Validierung

| Prüfung | Befehl oder Methode | erwartetes Ergebnis |
|---|---|---|
| Kern: LaTeX | `ReellesIntervall(...).zuLatex()` | geschlossenes `[a,b]` als `\\left[...\\right]` |
| Kern: rationale Fälle | Unit-Test der Fabrik | `3,2 → LeereMenge`, `2,2 → {2}`, `2,3 → ReellesIntervall(2,3)` |
| Kern: Struktur | Substitutions- und Variablen-Test | Grenzen werden ersetzt; freie Variablen beider Grenzen sind enthalten |
| Node-Vertrag | Vorlage- und Registry-Test | stabiler Schlüssel, zwei Zahl-Eingänge in Ordnung 0/1, Mengen-Ausgang |
| Reellheit | Registry-Test | reelle Variable mit `ReelleZahlen` zulässig; komplexe/nicht nachweisbar reelle Eingabe abgelehnt |
| Symbolik | Registry-Test | symbolische reelle Grenzen bleiben ein Intervall ohne zusätzliche Annahme |
| Graphintegration | `GraphPrüfung` über bestehende Zahlenquellen oder vorhandene Testkonvention | Zahl-Ausgang → beide Eingänge erlaubt; Mengen-Ausgang → Mengeneingang erlaubt; doppelte Eingangskante abgelehnt |
| Persistenz | `KartenJsonTest` | Art und alle drei Anschluss-IDs überleben den JSON-Roundtrip |
| Vollständige Tests | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test` | alle JVM-Tests erfolgreich |
| Debug-Build | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug` | erfolgreich |
| Strukturprüfung | `python3 scripts/pruefe_repository.py` | erfolgreich |
| Abschluss | `git diff --check` und unabhängige `node_verifier`-Prüfung | kein Whitespacefehler und keine offenen blockierenden Findings |

## Risiken und Rückfallstrategie

- Die mathematische Notation »Intervall« kann sonst offene Ränder implizieren. Der feste Vertrag verwendet daher ausdrücklich die abgeschlossene Schreibweise `[a,b]`; neue Varianten erfordern später einen eigenen Parametervertrag und erneute mathematische Prüfung.
- Die bestehende Mengenlogik kann keine allgemeine Intervallmitgliedschaft oder Kardinalität entscheiden. Diese Funktionalität bleibt bewusst außerhalb des Umfangs; bei Bedarf wird sie im Kern modelliert und getestet, nicht im Node nachgebildet.
- Ein symbolisches Intervall mit unbeweisbarer Reihenfolge bleibt absichtlich symbolisch. Es darf nicht vorschnell als leer oder als zusätzliche Annahme persistiert werden.
- Bei einem unerwarteten Compile-Fund in einer erschöpfenden Verzweigung wird nur der notwendige Kernpfad ergänzt und im Plan dokumentiert; keine unbezogene Mengenrefaktorierung.

## Fortschritt

- [x] Bestand kartiert: Vorlagen, Registry, Kern, Inspector, Graphprüfung, Persistenz, Tests und Gradle-Konfiguration geprüft.
- [x] Mathematische Freigabe eingeholt: abgeschlossenes Intervall und rationaler Sonderfallvertrag bestätigt.
- [x] Implementierung: Kern-Ausdruck, Substitution, Variablenanalyse, Vorlage, Registry und Tests ergänzt.
- [x] Nach zwei Verifikationsfindings korrigiert: Der Registry-Pfad verwendet nun `RechenKontext` und reicht Reellheitsmetadaten weiter; der Graphvertrag besitzt einen Integrationstest.
- [x] Unabhängige Abschlussverifikation: ohne offene Findings abgenommen.

## Entscheidungsprotokoll

| Datum | Entscheidung | Alternativen | Begründung |
|---|---|---|---|
| 2026-07-27 | Ein fester abgeschlossener Intervalltyp `mathematik.reellesIntervall`. | Vier separate offene/geschlossene Varianten; ein Randmodus-Parameter. | Der Auftrag nennt nur zwei Zahl-Eingänge und keine Randkonfiguration; ein fester, klarer Vertrag vermeidet verdeckte Standardannahmen. |
| 2026-07-27 | Rationale entartete/umgedrehte Grenzen werden sofort reduziert. | Immer einen Intervallausdruck speichern; `a>b` als Fehler behandeln. | `a>b` beschreibt die leere Menge, `a=b` eine einpunktige Menge; beide sind exakte, nützliche Mengenwerte. |
| 2026-07-27 | Symbolische nachweisbar reelle Grenzen behalten die Intervallform ohne Annahme. | `a≤b` als Bedingung hinzufügen oder die Auswertung abbrechen. | Die Reihenfolge ist ohne zusätzliche Information nicht entscheidbar; ein symbolischer Mengen-Ausdruck bleibt korrekt und verliert keine Information. |

## Abweichungen vom Plan

Keine. Während der Umsetzung werden Abweichungen hier mit Begründung ergänzt.

## Abnahmekriterien

- [x] Der Katalog enthält »Reelles Intervall« mit dem Schlüssel `mathematik.reellesIntervall` in der Kategorie »Mengen«.
- [x] Jede Instanz besitzt genau die Zahl-Eingänge `untereGrenze` (Ordnung 0) und `obereGrenze` (Ordnung 1) sowie den Mengen-Ausgang `menge`; alle Anschluss-IDs sind instanzstabil.
- [x] `2` und `3` liefern `[2,3]`; `3` und `2` liefern `∅`; `2` und `2` liefern `{2}`.
- [x] Zwei nachweisbar reelle symbolische Grenzen liefern einen `ReellesIntervall`-Ausdruck, ohne `a≤b` als Annahme hinzuzufügen.
- [x] Komplexe oder nicht nachweisbar reelle Eingaben führen zu einem fachlichen Auswertungsfehler und erzeugen keine Mengen-Ausgabe.
- [x] Der Standardrenderer zeigt den aus `zuLatex()` abgeleiteten Intervallwert, ohne eine zweite Intervallsemantik zu besitzen.
- [x] Der JSON-Roundtrip erhält Node-Art, beide Eingangs- und die Ausgangs-ID.
- [x] Alle oben genannten passenden Prüfungen sind ausgeführt und dokumentiert; der Enddiff ist unabhängig abgenommen.

## Ergebnis und Verifikation

- `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :MathematikRechenSystem:test :MathematikKnoten:test :app:testDebugUnitTest`: erfolgreich.
- `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test`: erfolgreich nach den Abschlusskorrekturen.
- `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug`: erfolgreich nach den Abschlusskorrekturen.
- `python3 scripts/pruefe_repository.py` und `git diff --check`: erfolgreich.
- Unabhängige Abschlussverifikation: ohne offene Findings abgenommen. Sie bestätigte zusätzlich erzwungene Neu-Kompilierung der Intervalltests, den vollständigen Test- und Buildlauf, Repository-Prüfung und Diff-Prüfung.
