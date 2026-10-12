# Mengenoperatorgrundlage

## Status und Ziel
Implementiert und statisch abgenommen, 2026-10-12. Die zehn bestehenden Mengenquellen erhalten nullstellige
Operatordefinitionen und verwenden denselben Auswahldialog wie der Mengenrechner.

## Nicht-Ziele
Keine neuen Knotentypen, keine Releaseintegration, keine frei programmierbaren
Operatoren. Eigene versionsfeste Mengenkarten bleiben unterstützt.

## Untersuchte Ausgangslage
`MengenKnotenAuswahl` enthielt zehn Erzeuger im Knotenmodul. Der Mengenknoten
verwendete ein Dropdown, der Mengenrechner den gemeinsamen Operatordialog.
`prüfeTeilmenge` kannte nur die Kette N/Z/Q/R/C. N0 war bereits durch
`NichtnegativeGanzeZahlenSemantik` definiert. Primzahl- und gaußsche Mitgliedschaft
fehlten. Der bestehende Branch enthält die inzwischen committete Rechnerkonsolidierung.
Persönliche IDE-Dateien werden nicht geändert.

## Fachliche Semantik
N beginnt bei 1, N0 bei 0. Inklusion ist eine partielle Ordnung mit den Zweigen
P < N < N0 < Z < Q < R < C und Z < Z[i] < C sowie PZi < Z[i].
P ist keine Teilmenge von PZi (Gegenbeispiel 2); PZi ist keine Teilmenge von P
(Gegenbeispiel 1+i). Gaußsche Primzahlen auf den Achsen haben Absolutwert einer
Primzahl kongruent 3 modulo 4. Abseits der Achsen muss a²+b² prim sein.
Quelle: https://mathworld.wolfram.com/GaussianPrime.html .
Exakte Probedivision besitzt ein Budget von 10000 ungeraden Kandidaten;
Budgetüberschreitung ergibt unbekannt. Symbolische Zahlen bleiben unbekannt.

## Daten-, Knoten- und Anschlussvertrag
`MengenKonstante` im Kern bündelt stabile bisherige Auswahl-IDs, Definition,
Mengenausdruck und `OperatorSignatur` ohne Eingänge mit Mengenausgang.
Die bestehende Knotenart, `mengeAuswahl`, Anschluss-IDs und Kartenverweise bleiben.
Unbekannte IDs werden erhalten und als Fehler beziehungsweise Dialogeintrag angezeigt.

## Architekturentscheidung
Siehe ADR 0045. Mengenquellen verwenden bestehende Signaturtypen, ohne sie als
numerische Rechnerfamilie zu deklarieren. Mengenwissen gehört in den Kern;
Inspector und Auswerter referenzieren dieselben Definitionen.

## Betroffene Bereiche
Kern: MengenKonstanten, Mengen, ObjektVergleich, SymmetrischeDifferenz.
Knoten: KonsolidierteKnoten. App: MengenKnotenInspektor.
Tests für Kernentscheidungen, Quellenkonfiguration, Auswertung und Persistenz.

## Meilensteine und Umsetzung
- [x] Istzustand und Vergleichspfad untersucht.
- [x] Konstantenkatalog, Mitgliedschaft, Inklusion und sichere Vereinfachung implementieren.
- [x] Gemeinsamen Dialog anbinden; feste eigene Karten und Anschlussidentität erhalten.
- [x] Regressionstests, Repositoryprüfung, JVM-Tests und Android-Build.
- [x] Unabhängige Verifikation und Abschlussdiff.

## Tests und Validierung
Paarweise Inklusion/Gleichheit aller zehn Konstanten, Grenzfälle 0/negative Werte/
Brüche/komplexe Zahlen, gaußsche Primzahlen und Budgetgrenze. Sichere Mengenalgebra,
symbolische Unbekanntheit, Quellenwechsel mit Undo/Redo sowie JSON-Roundtrip.
Laufzeitprüfung des Dialogs auf Android/Desktop getrennt von Kompilierung ausweisen.

## Persistenz und Migration
Keine Formatänderung. N0 verwendet das bereits vorhandene benannte Kernobjekt;
alte Auswahlkennungen bleiben identisch. Historische Migration bleibt unverändert.

## Risiken und Rückfallstrategie
Beschränkte Primzahlentscheidbarkeit ausdrücklich ausweisen. Bei UI-Problemen
kann der Inspector separat zurückgenommen werden; das Datenformat bleibt kompatibel.

## Fortschritt und Entscheidungsprotokoll
2026-10-12: Vorhandene N0-Semantik wiederverwenden. Quellenkatalog separat von
numerischen Familien, aber mit gemeinsamer Signatur und Dialogvorlage.

## Abweichungen
Der bestehende DesktopInspektor enthält weder den Mengen- noch den
Mengenrechner-Auswahldialog. Deshalb keine zweite Desktop-Dialogimplementierung;
Desktop-Kern- und Knotenpfade sind kompiliert und getestet. Die gemeinsame
Dialogvorlage betrifft den vorhandenen App-Inspector.

Keine zusätzliche numerische `OperatorFamilie`: Mengenquellen benötigen weder
Formelmodus noch numerischen Dispatcher. Der gemeinsame Vertrag ist OperatorSignatur.

## Ergebnis und Verifikation
- `python3 scripts/pruefe_repository.py`: erfolgreich einschließlich Architektur.
- `python3 scripts/pruefe_releaseplan.py`: erfolgreich.
- `python3 scripts/pruefe_versionsfolge.py`: erfolgreich; bestehender Arbeitsbranch
  mit veröffentlichter Version 3.1.0 zulässig, keine Releaseänderung vorgenommen.
- `./gradlew test :app:assembleDebug :desktopApp:compileKotlin --offline`: erfolgreich.
- Ergänzende gezielte Topologie-, Verbindungs- und Undo/Redo-Regressionsläufe:
  erfolgreich. Aktuelle XML-Ergebnisse: 1447 Tests, keine Fehler/Überspringungen
  (TypSystem 10, Kern 440, Graph 96, Adapter 49, Knoten 589, App 260, Desktop 3).
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, `kotlinc` fehlt im PATH.
  Kernkompilierung und -tests über Gradle erfolgreich.
- `adb devices -l`: kein verbundenes Gerät; kein lokales AVD unter ~/.android/avd.
  Keine Dialog-Laufzeitprüfung auf Emulator/Hardware behauptet. Nicht blockierende
  Abnahmegrenze: UI kompiliert, Konfiguration/Verbindungen/Undo/Redo/Codec getestet.
- Unabhängiger `node_verifier`: finale statische Abnahme ohne Blocker; Dialog-LaTeX
  und Legacy-Umgebungsraumgrenze nach Findings korrigiert und nachgeprüft.
- `git diff --check`: erfolgreich. Persönliche IDE-Dateien unverändert belassen.

Änderungen bleiben uncommittet auf `samai/v3.1.1/rechneroperatoren-konsolidierung`,
Basis HEAD `7123235cc911c838bfbd98117d597af442f9bde3`, Autor und Committer SamAI.
Keine neue Version, kein Branch, kein Commit oder Push erzeugt.
