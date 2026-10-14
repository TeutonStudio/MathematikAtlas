# Variable-Grundraum und Tupelreihenfolge

Status: Implementiert und unabhängig geprüft.

## Ziel und Nutzerwirkung
Variable erhält einen optionalen Mengeneingang `grundraum`. Tupel erhält numerische Standardwerte im Inspector und die vorhandene sortierbare Operandenliste.

## Nicht-Ziele
Keine neuen Knotentypen und kein neues JSON-Format. Die Änderung ist Teil des v3.2.1-Änderungsrelease.

## Untersuchter Istzustand
`MathematikAuswerter` erzeugt Variablenquellen aus Inspectorparametern. `KartenAuswerter` liest bereits rationale Tupelstandardwerte. `DynamischeOperandenListe` ordnet Anschlüsse stabil und atomar, war aber im Android-Inspector nur für Rechner aktiviert.

## Fachliche Semantik
Ein verbundener Grundraum ersetzt den Inspector-Grundbereich einschließlich Intervall. Ohne Verbindung bleibt das bestehende Verhalten. Bei strukturierten Variablen gilt die Menge als Komponentenbereich; der besondere Winkelraum behält seinen bisherigen Vertrag. Eingangsannahmen werden übernommen. Tupelstandardwerte sind ganze/rationale Zahlen, verbundene Quellen haben Vorrang.

## Daten- und Anschlussvertrag
Optionaler einzelner Mengeneingang links, Name `grundraum`. Ausgangs-IDs bleiben erhalten. Migration ergänzt fehlende Eingänge deterministisch. Umordnung verändert nur Anschlussreihenfolge; Standardwerte bleiben an Anschlussnamen gebunden.

## Architekturentscheidungen
Auswertung in MathematikKnoten; Migration im gemeinsamen Codec; vorhandene neutrale Sortieraktionen und Adapterstandardwerte wiederverwenden.

## Betroffene Dateien
MathematikKnotenVorlagen.kt, MathematikAuswerter.kt, VariablenStruktur.kt, migration/MathematikKartenCodec.kt, KnotenInspektorFenster.kt und VariablenGrundraumTest.kt.

## Meilensteine und Schritte
- [x] Aufrufpfade und vergleichbare Rechner-/Intervallpfade untersucht.
- [x] Eingang, Auswertung, idempotente Migration ergänzt.
- [x] Android-Tupelstandardwerte und Sortierung aktiviert.
- [x] Knotentests, Android-Build und Abschlussdiff erfolgreich; unabhängige Prüfung ohne weitere Findings.

## Tests und Validierung
Repositoryprüfung erfolgreich. `./gradlew :MathematikKnoten:testDebugUnitTest :app:assembleDebug --offline` erfolgreich. Zusätzliche Regressionen prüfen Vorrang vor Inspectorintervall sowie IDs, Migration und JSON-Roundtrip.

## Persistenz und Migration
Gemeinsame Normalisierung bei Schreiben, Dekodierung und Laden. Bestehende Verbindungen bleiben erhalten. Kein Schemawechsel.

## Risiken und Rückfallstrategie
UI-Gesten benötigen Geräteprüfung. Änderungen können dateiweise zurückgenommen werden; persistierte zusätzliche Ports sind gewöhnliche Graphdaten.

## Fortschritt
Implementation fertig. Reihenfolgeerhalt bei Längenänderung nach unabhängiger Prüfung ergänzt und per Regression nachgewiesen.

## Entscheidungsprotokoll
2026-10-13: Grundraum ersetzt Inspectorbereich, damit eine verbundene Menge tatsächlich maßgeblich ist. Vorhandene Tupelstandardwertsyntax beibehalten statt neuer Parser.

## Abweichungen
Keine.

## Ergebnis und Verifikation
Repository-, Releaseplan- und Versionsfolgeprüfungen erfolgreich. Knotentests und assembleDebug erfolgreich. Unabhängiger node_verifier hat die Korrektur abgenommen. `pruefe_kern.py` nicht ausführbar: kotlinc fehlt im PATH; `./gradlew test --offline` einschließlich Kern-, App- und Desktoptests erfolgreich. Keine Geräteprüfung. Anschließend in Branchcommit `8465f6c9` integriert; v3.2.1 ist bis zum finalen Mastercommit ein Releasekandidat.
