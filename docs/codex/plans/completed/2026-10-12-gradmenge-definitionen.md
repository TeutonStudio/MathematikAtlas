## 1. Titel und Status

Gradmenge und ausführbare Definitionen; implementiert und unabhängig abgenommen (2026-10-12).

## 2. Ziel und Nutzerwirkung

Eigene Winkelmenge, R↔Grad, Sinusdefinition, Tupelgrundmengen und vollständiger Definitionsaudit.

## 3. Nicht-Ziele

Keine neue Knotenart, Abhängigkeit, Version oder Veröffentlichung. Weitere Auditkandidaten werden nicht implementiert.

## 4. Untersuchter Istzustand

Übersetzer bietet Struktur- und Polarwechsel. Zahlenrechner behandelt Grad bisher als reelle Skalierung. Tupelräume behalten Komponentenräume.

## 5. Fachliche und mathematische Semantik

Grad = R/(360 Z) als additive Gruppe. R-Eingaben sind Bogenmaß. Hauptvertreter in [0,2π) oder [-π,π). sin_R = sin_Grad ∘ q. Kein Quotientenring mit üblicher reeller Multiplikation.

## 6. Daten-, Node-, Handle- und Edge-Vertrag

Bestehende Art- und Anschluss-IDs bleiben erhalten. Gespeicherte Modi ergänzen Übersetzer und Sinus. Winkel sind kein Untertyp von Reell.

## 7. Architekturentscheidungen

Semantik im Android-freien Kern; bestehende Register und Codecs verwenden.

## 8. Betroffene Dateien und Symbole

Kern: Winkel, Mengen, Methoden, Typen, Tupel. Knoten: Übersetzer und Zahlenrechner. App/Desktop: Inspector und Definitionsbibliothek.

## 9. Meilensteine

- [x] Domäne: `WinkelTest` prüft Klassen, Hauptvertreter, Symbolik, numerische Kleinstwerte, Methodenkomposition und Tupelräume.
- [x] Knoten/UI: bestehende Mengenquelle, Übersetzer und Registry-Sinusvariante; Winkelparameter, Android-/Desktopinspektoren, stabile IDs und Undo/Redo getestet.
- [x] Definitionsgraph und Audit: ausführbarer Sinusgraph; 465 konfigurierte Einträge und alle 131 Assets in `docs/codex/DEFINITIONSKARTEN_ANALYSE.md`.
- [x] Tests und unabhängige Verifikation: 1525 Tests ohne Fehler oder Überspringen; Produktcode von `node_verifier` freigegeben. Auditkorrektur anschließend eingearbeitet.

## 10. Konkrete Umsetzungsschritte

Zuerst Kernausdrücke und Besucher, dann bestehende Knoten, anschließend Definitionen und Nachweise.

## 11. Tests und Validierung

Gezielte Kern-/Knotentests, kompletter Gradle-Testlauf und APK/Desktopkompilierung; Repositoryprüfungen.

## 12. Persistenz und Migration

Neue Parameter sind optional; Standard positiv. Historische numerische Polarwinkel bleiben Gradrepräsentanten. Keine JSON-Formaterhöhung.

## 13. Risiken und Rückfallstrategie

Symbolische Hauptwerte brauchen eigene AST-Semantik; unbekannte Werte nicht numerisch erfinden. Fremde Arbeitsbaumänderungen bewahren.

## 14. Fortschritt

2026-10-12: Alle Meilensteine umgesetzt. Unabhängige Prüfung fand zwei mathematische Randfälle (kleine Winkel bei Hauptwertnormalisierung und rationale Klassen nach Vereinfachung); beide korrigiert und durch Regressionen abgesichert. Bekannte nichtreelle/undefinierte Vertreter werden bereits bei Konstruktion zurückgewiesen. Registry statt separater Sinus-Anschlusslogik. Präzise Komponentenräume werden auch beim Abflachen und beim leeren Tupel erhalten.

## 15. Entscheidungsprotokoll

2026-10-12: Eigener Winkeltyp statt Reell-Untertyp; bestehende Sinusvariante statt zusätzlicher Knotenart.

## 16. Abweichungen vom ursprünglichen Plan

Keine Umfangsabweichung. Die additive Winkelgruppe wird ausdrücklich nicht als Quotientenring der üblichen reellen Multiplikation bezeichnet. Die geometrische Grad-Sinusgrunddefinition ist eine Regelkarte; die davon abgeleitete reelle Sinusdefinition ist ausführbar. Frei konfigurierte Formeln und Nutzerdaten sind über ihre Verträge statt durch eine endliche Variantenliste erfasst.

## 17. Ergebnis und Verifikation

Erfolgreich:

- `MATLAS_DEFINITIONSAUDIT=/tmp/matlas-definitionen.tsv ./gradlew test :app:assembleDebug :desktopApp:compileKotlin --console=plain`; `/tmp/grad-final5.log`, BUILD SUCCESSFUL. 1525 Tests: TypSystem 10, Kern 466, Graph 119, Adapter 49, Knoten 608, App 265, Desktop 8; keine Fehler/Überspringungen.
- Nach abschließender Auswahlmarkierung im Intervallinspektor: `./gradlew :app:assembleDebug :desktopApp:compileKotlin --console=plain`; `/tmp/grad-ui-final.log`, erfolgreich.
- Repository-/Architekturprüfung, Releaseplan und Versionsfolge erfolgreich; bestehende Metadaten 3.1.1 unverändert.
- `python3 scripts/analysiere_definitionskarten.py /tmp/matlas-definitionen.tsv`; 465 Einträge, 131 statische Definitionen (113 Regelkarten, 18 Graphen ohne Regelknoten). Die Grad-Sinus-Sonderklassifikation erfolgt vor der reellen Sinusheuristik.
- `git diff --check` ohne Befund. Fremde IDE-/Schemaänderungen bleiben erhalten.
- Unabhängiger `node_verifier`: finale Produktcodefreigabe ohne verbleibenden Blocker; angeregte Auditkorrektur umgesetzt.

Prüfgrenzen: `python3 scripts/pruefe_kern.py` endet mit „kotlinc und java werden … benötigt“; die Gradle-Kerntests bestehen. Keine manuelle Android-/Desktop-UI-Abnahme in dieser Aufgabe; vorhandener Sitzungsnachweis meldet kein angeschlossenes Android-Gerät. Kein Release oder Commit erzeugt. Bestehender Branch `master`, unveränderte HEAD-Basis `ff020f06c5f6aad7e17cef0292cce0b16384917b` (Author Alexander, Committer GitHub); daher keine neue SamAI-Commitidentität zu prüfen.

Zentrale Quellen: `Winkel.kt`, `TupelGrundmengen.kt`, `ÜbersetzerKnoten.kt`, `RechnerOperatorDefinitionen.kt`, `ZahlenRechnerKonfiguration.kt`, `GradDefinitionen.kt`, `DefinitionsInventarTest.kt` und ADR 0050.
