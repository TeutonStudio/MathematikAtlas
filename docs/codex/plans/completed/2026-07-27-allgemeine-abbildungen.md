# Allgemeine Abbildungen und Funktionsparameter

Status: `[x] abgeschlossen`

## Ziel und Nutzerwirkung

`mathematik.abbild` akzeptiert allgemeine Methoden. Nutzer können mit dem neuen Knoten `mathematik.allgemeinerParameter` eine nicht numerische Methodensignatur erzeugen und endliche Mengen beliebiger mathematischer Objekte abbilden.

## Nicht-Ziele

- Numerische Operatoren, Matrixerzeugung, Komposition, Iteration und Methodenanalysis werden nicht verallgemeinert.
- Grundmengen bleiben im Inspector auf `N`, `Z`, `Q`, `R`, `C` beschränkt.
- Die Zugehörigkeit eines konkreten Arguments zum deklarierten Wertevorrat wird nicht zusätzlich entschieden.

## Untersuchte Ausgangslage

- `Funktion.parameter` enthielt nur `Variable`, und Anwendung sowie Substitution akzeptierten nur `ZahlAusdruck`.
- Der Abbild-Handle verwendete `ZahlFunktion`, obwohl der Auswerter bereits `Funktion` las.
- `TermZuMethode` leitet Argumente über Quellenmetadaten der freien Variablen ab.

## Semantik und Vertrag

- `FunktionsParameter` ist der gemeinsame bindbare Parametertyp; `Variable` bleibt dessen numerischer Spezialfall, `AllgemeinerParameter` der allgemeine Fall.
- `Funktion` bindet Namen auf `MathematischesObjekt`. Numerische Teil-APIs verlangen weiterhin explizit `Variable`.
- Eine Abbildung hat genau einen freien Parameter und genau eine Ausgabe. Für eine endliche Menge wird jedes Element gebunden; sonst bleibt das Ergebnis symbolisch `Abbild`.
- `mathematik.abbild`: `menge : Menge` (Eingang, genau eins), `methode : Funktion` (Eingang, genau eins), `menge : Menge` (Ausgang, genau eins).
- `mathematik.allgemeinerParameter`: stabiler Typ-Schlüssel, keine Eingänge, `wert : Objekt` als Ausgang; persistierte Inspector-Parameter `name` (Standard `a`) und `werteVorrat` (Standard `R`).

## Architektur und Persistenz

- Der neue Knoten wird über `MathematikKnotenVorlagen.alle` und `StandardMathematikAuswerter` registriert; der vorhandene Inspector wird erweitert.
- Die Lade-Migration stellt bei alten Abbild-Knoten nur die Anschlussart von `methode` auf `Funktion` um. Anschluss-ID, Reihenfolge und Kanten bleiben erhalten.
- Der vorhandene JSON-Mechanismus speichert Vorlageninstanzen ohne Schemaänderung.

## Umsetzungsschritte

- [x] Funktionsparameter, Bindung, Substitution und Abbildungsberechnung verallgemeinern.
- [x] Numerische Teiloperationen gegen allgemeine Parameter absichern.
- [x] Vorlagen, Auswerter, Inspector und Auswahlkatalog ergänzen.
- [x] Lade-Migration und Regressionstests ergänzen.
- [x] Gesamtsuite, Build, Repositoryprüfung und Diff-Abnahme dokumentieren.

## Tests und Validierung

- Kern: allgemeine Parameter, allgemeine endliche Bildmengen sowie ein- und mehrwertige Fehlerfälle.
- Knoten: allgemeiner Parameter → Term zu Methode und allgemeiner Abbild-Handle.
- App: Abbild-Migration erhält Anschluss-ID und erweitert die Art.
- Prüfungen: `./gradlew test`, `./gradlew :app:assembleDebug`, `python3 scripts/pruefe_repository.py`.

## Risiken und Rückfallstrategie

Allgemeine Parameter können nicht an spezialisierte Zahlanschlüsse verbunden werden. Ein Rückfall entfernt lediglich den neuen Knoten und stellt den Abbild-Handle auf `ZahlFunktion` zurück; die Migration selbst löscht keine Kanten.

## Entscheidungsprotokoll

- 2026-07-27: Allgemeine Parameter werden als neuer Typ statt einer Enttypisierung von `Variable` modelliert. Das bewahrt die bestehenden Zahlterme und schränkt nur die APIs ein, die mathematisch numerisch bleiben müssen.
- 2026-07-27: Die vorhandene Quellenmetadatenklasse bleibt unverändert und dient auch allgemeinen Parametern; sie transportiert bereits Name und Wertevorrat.

## Abweichungen

Keine.

## Ergebnis und Verifikation

- Die allgemeine Abbildung, der allgemeine Parameterknoten und die Migration sind umgesetzt.
- `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug`: erfolgreich.
- `python3 scripts/pruefe_repository.py`: erfolgreich.
- Der Abschluss-Diff enthält nur die hier beschriebenen Kern-, Knoten-, Inspector-, Migrations-, Test- und Dokumentationsänderungen.
