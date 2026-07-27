# Term zu Methode – abgeleitete maximale Zielmenge

Status: `[x] abgeschlossen`

## Ziel und Nutzerwirkung

`mathematik.termZuMethode` soll keine zu kleine numerische Zielmenge erzeugen. Seine Zielmenge wird aus dem konfigurierten Ziel und dem durch den verbundenen Term nachweisbaren Wertebereich als größte gemeinsame Grundmenge `N < Z < Q < R < C` abgeleitet.

## Nicht-Ziele

- Keine neue Typ- oder Anschlussart.
- Keine exakte Bildmengenanalyse symbolischer Terme.
- Keine Änderung des allgemeinen Funktionsausgangs oder der automatischen Argumentableitung.

## Untersuchte Istsituation

- Der Node wird in `MathematikKnoten/.../MathematikAuswerter.kt` erzeugt und speichert `zielmenge` als Inspector-Parameter.
- `BedingterWert.werteVorrat` enthält zurzeit die Grundmenge einer Variablen, wird aber bei zusammengesetzten Zahlwerten nicht zuverlässig fortgeführt.
- Die unterstützten Grundmengen werden lokal als `N`, `Z`, `Q`, `R`, `C` gelesen. Ihre einschlägige Inklusionsordnung ist total.

## Fachliche Semantik

Für einen numerischen Term ist die deklarierte Zielmenge die größte der explizit gewählten Grundmenge und aller nachweisbaren Grundmengen, die der Termwert benötigt. Die Ableitung ist konservativ: Sie liefert eine Obermenge, keine exakte Bildmenge. Bei nicht numerischen Termen ohne numerischen Wertebereich bleibt die explizite Zielmenge erhalten.

## Daten-, Node-, Handle- und Edge-Vertrag

Der Node-Typ, seine zwei Anschlüsse (`term : objekt`, `methode : funktion`), die persistierten Parameter und Kanten bleiben stabil. `zielmenge` bleibt als Untergrenze und Rückfallwert persistiert; der tatsächlich erzeugte Wert kann größer sein.

## Architekturentscheidung

Die Rangordnung und Zusammenführung der fünf Grundmengen werden bei den auswertbaren Werte-Metadaten implementiert. Dadurch bleibt der Inspector frei von mathematischer Doppelwahrheit und die Funktion enthält die tatsächlich abgeleitete Zielmenge.

## Betroffene Dateien und Symbole

- `MathematikKnoten/.../MathematikAuswerter.kt`: Wertebereich fortführen und Zielmenge des Nodes ableiten.
- `MathematikKnoten/.../AuswertenTest.kt`: Direkt- und Graphfall der Ableitung prüfen.
- `docs/codex/plans/...` und `docs/codex/CURRENT_STATE.md`: Vertrag und Ergebnis dokumentieren.

## Meilensteine

- [x] Semantik mit Math Reviewer abgeglichen: Zahlenbereich nur über die totale Grundmengenkette; nichtnumerische Terme nutzen den persistierten Rückfallwert.
- [x] Wertebereich und Zielmengen-Maximum im Rechenkern und Node-Auswerter implementiert.
- [x] Tests ergänzt und passende Prüfungen ausgeführt: vollständige JVM-Tests und Debug-Build erfolgreich.
- [x] Diff geprüft, Iststand und ADR aktualisiert; unabhängige Abschlussprüfung freigegeben.

## Konkrete Umsetzungsschritte

1. Die vorhandene Grundmengenordnung als totalen, validierten Vergleich kapseln.
2. Numerische Zwischenergebnisse führen ihren konservativen Wertebereich aus den Eingaben weiter; komplexe Zahlwerte markieren `C`.
3. `termZuMethode` vereinigt Inspector-Zielmenge und Wertebereich des Termwerts.
4. Tests decken Hochstufung zu `C`, Bewahrung eines größeren Inspector-Werts und Weitergabe über den Termgraphen ab.

## Tests und Validierung

- Betroffene Knoten-Unit-Tests.
- Projektweite JVM-Tests und Debug-Build mit dem in `CURRENT_STATE.md` dokumentierten JDK.
- `python3 scripts/pruefe_repository.py`.

## Persistenz und Migration

Kein neues Persistenzfeld und keine Handleänderung. Bestehende Karten behalten ihre Konfiguration; beim nächsten Auswerten wird die möglicherweise größere Zielmenge abgeleitet.

## Risiken und Rückfallstrategie

Die Metadaten geben keine exakte Bildmenge an. Falls ein Term keinen nachweisbaren numerischen Wertebereich besitzt, bleibt die bisherige explizite Zielmenge unverändert. Die Änderung kann durch Entfernen der Zielmengen-Zusammenführung zurückgenommen werden, ohne gespeicherte Karten zu migrieren.

## Fortschritt

- 2026-07-27: Istpfad und bestehende Tests geprüft; mathematische Review angefordert.
- 2026-07-27: Math Reviewer bestätigt die Semantik und die klare Grenze für nichtnumerische Terme.
- 2026-07-27: Unabhängige Abschlussprüfung freigegeben; erzwungene Modultests sowie `git diff --check` sauber.

## Entscheidungsprotokoll

- 2026-07-27: Die Zielmenge bleibt persistierbar und wird nicht vollständig durch eine unvollständige Bildmengenanalyse ersetzt. Sie dient als explizite Untergrenze, der nachweisbare Wertebereich kann sie nur vergrößern.

## Abweichungen vom ursprünglichen Plan

Keine.

## Ergebnis und Verifikation

- `MathematikRechenSystem/.../Operatoren.kt` enthält die zentrale, konservative Inferenz und die validierte Grundmengenordnung.
- `mathematik.termZuMethode` bildet für Zahlterme das Maximum aus persistierter Zielmenge und inferiertem Wertebereich; für andere `MathematischesObjekt`-Arten bleibt die persistierte Zielmenge erhalten.
- Neue Kern- und Knotentests prüfen Konstanten, Division, komplexe Variablen, den Graphpfad und nichtnumerische Rückfälle.
- Erfolgreich: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug`.
- Erfolgreich: `python3 scripts/pruefe_repository.py` und `git diff --check`.
- Erfolgreich in der unabhängigen Abschlussprüfung: `./gradlew :MathematikRechenSystem:test :MathematikKnoten:test --rerun-tasks`.
