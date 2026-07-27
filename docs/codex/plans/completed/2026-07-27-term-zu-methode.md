# Term zu Methode – allgemeiner Term

Status: `[x] abgeschlossen`

## Ziel und Nutzerwirkung

`mathematik.termZuMethode` erhält genau einen allgemeinen Termeingang. Freie, im vorgeschalteten Term-Graph verbundenen Variablen werden automatisch zu geordneten Argumenten; ihre Inspector-Wertemengen bilden den Definitionsbereich, die Zielmenge wird im Inspector festgelegt.

## Vertrag

- Handles: `term : mathematik.objekt` (Eingang) und `methode : mathematik.funktion` (Ausgang).
- Parameter bleiben numerische `Variable`-Objekte. Gleichnamige Quellen werden zu einem Parameter vereinigt; unterschiedliche Wertemengen desselben Namens sind ein Fehler.
- Die Reihenfolge folgt topologischer Quellreihenfolge, bei Gleichstand der Node-ID, und kann im Inspector persistiert überschrieben werden.
- Die Variablen-Wertemenge und die Zielmenge sind eine der Grundmengen `N`, `Z`, `Q`, `R`, `C`, Standard `R`.
- Alte Argument-, Zielmengen- und Variablen-Wertevorrat-Handles werden beim Laden entfernt; unvereinbare ausgehende Methodenkanten werden bereinigt.

## Ergebnis und Verifikation

- [x] Auswertung, Vorlagen, Inspector, Renderer und Lade-Migration angepasst.
- [x] Vertrags-, Auswertungs-, Migrations- und LaTeX-Tests ergänzt.
- [x] `python3 scripts/pruefe_repository.py` erfolgreich.
- [x] `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug` erfolgreich.

## Entscheidungen

- 2026-07-27: Der Ausgang ist allgemein `Funktion`, nicht dynamisch spezialisiert. Damit sind beliebige mathematische Termwerte korrekt modelliert; spezialisierte Funktionsanschlüsse werden bewusst nicht automatisch bedient.
- 2026-07-27: Wertemengen werden im Variable-Inspector konfiguriert. Dies vermeidet zwei widersprüchliche Quellen gegenüber dem bisherigen Eingang.
