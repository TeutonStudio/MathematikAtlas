# Vektorpotenz, Unterraum und Geometrieübersetzung

Status: abgeschlossen und durch Koordinator unabhängig verifiziert; kein Release.

## Ziel und Nutzerwirkung
Bestehenden Vektorrechner um alternierende Vektorpotenz, Mengenrechner um linearen/affinen Spann und Übersetzer um explizite Geometrie-Koordinatenübergänge erweitern. Skalarproduktnachweise dürfen nicht durch bloße Referenztexte vorgetäuscht werden.

## Nicht-Ziele
Keine neuen Knotentypen, Dependencies, globale Gitkonfiguration oder approximative Formerkennung. Vorhandene Nutzeränderungen bleiben erhalten.

## Untersuchter Istzustand
VektorRechner.kt trägt die Kernoperationen; SkalarproduktKnoten.kt dekoriert den gemeinsamen Auswerter. SkalarproduktPruefung.kt erklärt jeden nichtleeren Referenztext zum Nachweis (zu korrigieren). RechnerOperatorDefinitionen.kt erzeugt Registry-Signaturen. MengenOperatorKnoten.kt und ÜbersetzerKnoten.kt sind die bestehenden Integrationspfade. Definitionen benötigen spezielle ausführbare Graphen statt Regel-Fallback.

## Fachliche und mathematische Semantik
P0(a)=1, P1(a)=a, P2k(a)=<a,a>^k und P2k+1(a)=<a,a>^k a. p ist nichtnegative Ganzzahl, Tupel bedeutet Spalte; R/C, Quaternion-Altpfa­de bleiben erhalten. Eigene unbewiesene Produkte bleiben mit Annahmen ausführbar, widerlegte gesperrt. Unterraum ist {c+Σ b_k a^k | b∈R^n}, ohne c linear. Rationale Gaußelimination exakt; symbolische Pivots unbestimmt. Geometrie bleibt strukturiert, reell und dimensionstreu; kein erfundenes Koordinatensystem, Kreisfläche oder Gruppenmittelpunkt.

## Daten-, Node-, Handle- und Edge-Vertrag
Bestehende Typ-Schlüssel, neue Operatorschlüssel vektor.potenz und menge.unterraum. Konfiguration in bestehenden Parametern/Kartenreferenzen, stabile IDs durch vorhandene Konfiguratoren. p optional (Vorgabe 2), a und dynamische Richtungen typisiert, c optional. Potenzausgang Zahl/Zeile/Spalte. Kartenaktionen garantieren Undo/Redo.

## Architekturentscheidungen
Kernsemantik im Android-freien Rechenkern; Plattformen nutzen gemeinsame Konfiguration. Nachweise werden aufgelöst und an Methode/Version gebunden. Geometrie wird explizit übersetzt, nicht stillschweigend Strukturtypen gleichgesetzt.

## Betroffene Dateien und Symbole
VektorRechner, RechnerOperatorDefinitionen, SkalarproduktPruefung/SkalarproduktKnoten, MengenOperatorKnoten, ÜbersetzerKnoten, gemeinsame Definitionskarten und Android-/Desktopinspektoren sowie fachliche Tests.

## Meilensteine
- [x] Bestand und Versions-/Branchfreigabe: samai/v3.1.2/vektorpotenz-unterraum-geometrie; x reserviert, keine neuen Knoten.
- [x] Skalarproduktprüfung und Vektorpotenz: Kern-/Knotenkompilierung und Kerntests bestanden.
- [x] Linearer/affiner Unterraum: exakter Rang, Mitgliedschaft, Methodensubstitution in Kerntests.
- [x] Geometrieübersetzungen und Anker: rationale R3-Polygonzentren, Kreisgrenze, komplexe Zielpunktabwehr und vollständige Translation in Kern-/Knotentests.
- [x] Android/Desktop Inspector und ausführbare Definitionen: Potenzgraph p0..4, Linearkombinationsbild, c-only Singleton und Codec in Tests.
- [x] Gesamtprüfung: 319 Testsuiten / 1536 Tests ohne Fehler; APK/Plattformkompilierung und Android-Lint grün. Unabhängige Prüfung durch Koordinator ohne verbleibende eigene Blocker.

## Konkrete Umsetzungsschritte
Bestehende Auswahlverträge wiederverwenden, Kernfälle ergänzen, Registry und Auswerter anschließen, beide Plattformen konfigurieren, Definitionen aus ausführbaren vorhandenen Knoten zusammensetzen. Je Meilenstein gezielte Tests, anschließend Gesamtprüfungen.

## Tests und Validierung
p=0..4 für (2,3), komplex/gewichtet, ungültige Exponenten, falsche Nachweistexte, inkonsistente Räume; Unterraumrang/Mitgliedschaft einschließlich abhängigen Richtungen und Symbolik; Geometrieobjekte, Anker, Mittelpunktdiagnosen, Definitionen, JSON/Undo. Repository-/Releaseplan-/Versionsfolge-/Kernskripte, Gradle test, assembleDebug, Desktopkompilierung und verfügbare Laufzeitprüfungen.

## Persistenz und Migration
Format 9 bleibt; neue Parameter sind rückwärtskompatibel. Methodenreferenzen versionsfest. Ergebnisse/Nachweiscaches nicht speichern. Historische unauflösbare Nachweise bleiben unbestimmt.

## Risiken und Rückfallstrategie
Breiter bestehender Arbeitsdiff: nur additive/fokussierte Änderungen, kein Restore. Unbeweisbare Symbolik ausdrücklich unbestimmt. Nicht vorhandene Geräte separat dokumentieren.

## Fortschritt
2026-10-12: verpflichtende Dokumentation und vorhandene Pfade gelesen; Releasefreigabe noch ausstehend.

## Entscheidungsprotokoll
2026-10-12: Genehmigter Plan und mathematische Auflagen übernommen; keine neuen Typ-Schlüssel. Versionsachse x; konkrete Version 3.1.2 durch master_verwalter reserviert, Basis ff020f06; Produktmetadaten bleiben 3.1.1 bis Release.

## Abweichungen vom ursprünglichen Plan
Noch keine; Auflagen präzisieren p0/p1, exakte Rangprüfung und Geometriegrenzen.

## Ergebnis und Verifikation
Gradle `test :app:assembleDebug :desktopApp:compileKotlin` erfolgreich: 319 Testsuiten, 1536 Tests, keine Fehler. Gezielte Kern-/Knoten-/Appkompilierung erfolgreich. Android Debug-APK gebaut. Repository-, Releaseplan- und Versionsfolgeprüfungen durch Koordinator erfolgreich. Android `lintDebug` erfolgreich; gesamter Abschlusslauf BUILD SUCCESSFUL in 1m24s.

`scripts/pruefe_kern.py` ist ohne separates `kotlinc` nicht ausführbar; die Gradle-Kerntests sind erfolgreich. Keine Android-Laufzeitabnahme: ADB meldet kein Gerät, keine AVD verfügbar. Bestehende Desktop-Compose-Suite erfolgreich; kein neuer manueller Produktdropdown-Laufzeitnachweis. Keine Commits/Pushes; Autor-/Committermetadaten daher nicht neu erzeugt.

Grenzen: Allgemeine externe Nachweisreferenzen bleiben unaufgelöst, wenn kein methodengebundener Resolverbeleg vorhanden ist. Integrierte strukturelle Prüfung deckt den reellen eindimensionalen Standardfall und sein negatives Gegenbeispiel ab; universelle Axiome werden nicht durch Stichproben bewiesen. Geometrische Symbolik und unbekannte intrinsische Dimension bleiben diagnostisch unbestimmt. Koordinatensystemnamen sind reine Labels. Definitionsgraphen sind Momentaufnahmen der aktuellen Eingangsorientierung, Potenz und Richtungsbelegung; der UI-Resolver aktualisiert sie bei Ergebniswechsel.
