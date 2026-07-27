# Matrix-Erzeugungsarten – ExecPlan

## Status

`[-]` Implementiert; die lokale Gradle-Umgebung verhindert noch die vollständige Ausführung der JVM-Tests.

## Ziel und Nutzerwirkung

Der bestehende Matrix-Knoten kann Matrizen entweder aus einzelnen skalaren Eingängen oder aus einer zweistelligen Zahlmethode erzeugen. Höhe und Breite sind im Inspector konfigurierbar. Die Indizes sind nullbasiert: `0 <= zeile < höhe`, `0 <= spalte < breite`.

## Istzustand und Entscheidung

- Der bestehende Knoten hat variadische Zeilenvektor-Eingänge; gespeicherte Kanten können nicht verlustfrei in skalare Eingänge zerlegt werden.
- Neue Knoten starten als `2 x 2` mit Einzel-Eingaben.
- Die Methode wird als `f(zeile, spalte)` aufgerufen, muss zwei Parameter und eine Zahl-Ausgabe besitzen.
- Das Verkleinern und der Moduswechsel entfernen betroffene Kanten in derselben Undo/Redo-Aktion.
- Alte Matrix-Knoten werden beim Laden auf den neuen `2 x 2`-Einzel-Eingabe-Modus migriert; alte Zeilenvektor-Kanten werden entfernt.

## Vertrag

- Persistierte Parameter: `erzeugungsArt` (`einzelEingaben` oder `methode`), `höhe`, `breite`.
- Einzelmodus: ein Zahl-Eingang `eintrag_<zeile>_<spalte>` je Matrixelement.
- Methodenmodus: ein Zahlfunktions-Eingang `methode`.
- Ausgang: unverändert `matrix` vom Typ Matrix.
- Eine Größen- oder Modusänderung bewahrt überlappende Einzel-Eingänge samt stabiler IDs und entfernt nicht mehr gültige Anschlüsse samt Kanten.

## Umsetzung und Prüfung

- `[x]` Rechenkern-Helfer und Knoten-Auswertung ergänzt.
- `[x]` Konfigurationsaktion, Inspector und Lade-Migration ergänzt.
- `[!]` Domänen-, Knoten- und Editor-Tests ergänzt, aber nicht gestartet: Gradle scheitert vor der Konfiguration an `libnative-platform.so`; die Zusatzprüfung findet kein `kotlinc`.
- `[x]` Diff und Repository-Architekturprüfung abgeschlossen; `git diff --check` und `python3 scripts/pruefe_repository.py` waren erfolgreich.

## Risiken

Die Migration ist absichtlich destruktiv, weil der Bestand keinen Vektor-zu-Skalar-Aufspaltknoten besitzt. Sie wird beim Laden idempotent angewandt und die entfernten Kanten sind vor dem nächsten Speichern noch nicht in einer gespeicherten Version überschrieben.
