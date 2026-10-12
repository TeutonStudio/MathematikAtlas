# Rechner-Auswertungsschemata

## 1. Status
Implementiert und technisch geprüft; keine Veröffentlichung und kein Commit. Unabhängige Diffprüfung ohne verbleibende Blocker. Visuelle Geräteabnahme bleibt offen.

## 2. Ziel und Nutzerwirkung
Gemeinsamer Schema-Zugang für Zahlen-, Vektor-, Matrix-, Tensorrechner und vorhandene Prädikatsnachweise auf Android/Desktop. Interaktive Komponentenaddition und Matrixproduktsummanden.

## 3. Nicht-Ziele
Neue Knotentypen, neue mathematische Verfahren, vollständige Analysisherleitungen und Releaseverwaltung.

## 4. Istzustand
RechnerOperatorRegistry enthält vier Rechnerfamilien. Android besitzt dynamische Definitionskarten und spezialisierte Formeldefinitionen. Desktop hatte nur den Falk-Zugang. FalkMatrix besaß Farblogik, bekam aber ausschließlich null-Auswahl und leere Klickhandler.

## 5. Semantik
Matrixprodukt bleibt streng linksassoziativ; jedes Zwischenprodukt steht einmal unter seinem rechten Faktor und dient als nächster linker Operand. DetailliertesFalkSchema liefert geordnete Summanden. Addition liest tatsächliche Komponenten aus Tensorartig und das tatsächliche Auswertungsergebnis; keine eigene Berechnung im Renderer. Methoden zeigen vorhandene Vorschriften, gemeinsame Parameter, Definitionsraum und Ergebnisannahmen.

## 6. Datenverträge
RechnerSchemaModell ist eine abgeleitete, nicht persistierte Projektion aus Knoten und KnotenAuswertungsErgebnis. Keine Anschluss-, Knoten- oder Formatänderung. Unbekannte Operatoren zeigen vorhandene Daten und Knotennamen.

## 7. Architektur
Gemeinsame Compose-Inhalte im bestehenden gemeinsamen Quellpfad. Vorhandene Kernverträge bleiben mathematische Quelle. Android-Konzeptdarstellung bekommt eine eigene Auswertungsschema-Variante neben FalkSchema. Siehe ADR 0048.

## 8. Betroffene Bereiche
MatrixproduktFalkSchema.kt: Schema-Projektion, Komponentenansicht, Matrixkette und gemeinsamer Dialog. Android-Rechnerinspektoren und Konzeptresolver; Desktop-Inspector. Regressionstests in Knoten und Desktop.

## 9. Meilensteine
- [x] Bestand und gemeinsame Verträge geprüft.
- [x] Matrixauswahl, Summanden und einmalige Zwischenproduktdarstellung implementiert.
- [x] Gemeinsame Schemata und Zugänge implementiert; Android/Desktop kompiliert.
- [x] Tests, unabhängige Prüfung und Dokumentation; visuelle Abnahmegrenzen ausdrücklich dokumentiert.

## 10. Umsetzungsschritte
Registrygestützte Projektion, direkte Zugänge in den Inspektoren, zusätzlicher Konzeptreiter auch bei speziellen Formeldefinitionen. Auswahl bei Ergebnis-/Operatorwechsel zurücksetzen. Komponentenauswahl achsenweise ohne riesige Indexlisten; Matrixachsen mit vorhandener Projektion.

## 11. Tests und Validierung
Repositoryprüfung erfolgreich. Erste Android/Desktop-Kompilierung erfolgreich. Regressionstest unterscheidet Falk-Reiter weiterhin von Addition; allgemeiner Reiter separat ergänzt. Neue Tests prüfen echte Werte, Tensorindizes, Fehler-/Fehlzustände und interaktive Falk-Auswahl mit Ergebniswechsel. Vollständiger Gradle-Testlauf, APK-Build und lintDebug erfolgreich. Anschließend Zahlenformelregression gezielt erfolgreich; nach nativer Summandenformelanzeige Compose-Regressionslauf und APK-Build erneut erfolgreich. Zusatzprüfung pruefe_kern.py nicht ausführbar: kotlinc/java fehlen im PATH; Gradle verwendet seine Toolchain.

## 12. Persistenz und Migration
Keine Migration erforderlich. UI-Auswahl wird ausschließlich mit remember gehalten und nicht in Karten geschrieben.

## 13. Risiken und Rückfallstrategie
Nicht jeder Operator liefert fachliche Teilschritte; diese zeigen Definition, tatsächliche Ein-/Ausgaben und vorhandene Nachweise. Kein neues Herleitungsverfahren. ADB meldet keine Geräte. Android-Geräteprüfung und visuelle Theme-/Schriftabnahme nicht erfolgt. Rückfall durch Rücknahme der ausschließlich aufgabenspezifischen Änderungen.

## 14. Fortschritt
2026-10-12: Erste Kompilierung erfolgreich; anschließend neue Regressionstests und vollständiger Test-/Buildlauf gestartet.

## 15. Entscheidungsprotokoll
2026-10-12: Nutzer bestätigt alle Rechner, Struktur- und Methodenaddition, einmalige Zwischenprodukte und operatorgerechte Detailtiefe. Gemeinsamen Rahmen statt getrennter Plattformrenderer gewählt.

## 16. Abweichungen
Komponenten werden als auswählbare Achsenkoordinate mit zugeordneten farbigen Werten dargestellt; vollständige große Strukturansichten bleiben als LaTeX erhalten. Nicht vorhandene fachliche Herleitungen werden nicht ergänzt.

## 17. Ergebnis und Verifikation
Repositoryprüfung und git diff --check erfolgreich. Vollständiger Testlauf, Android-assembleDebug/lintDebug sowie Desktop-Kompilierung und Compose-Test erfolgreich. Unabhängige Prüfung fand Auswahlreset zwischen P-Schritten, Methodenprodukt-Routing und falsche Symbol-/Formelprojektion; alle korrigiert und mit Regressionen abgesichert. Kein Release, keine Versionsänderung; Arbeit im bestehenden Checkout auf master ohne Commit. Vorbestehende .idea-Änderungen bleiben unberührt.
