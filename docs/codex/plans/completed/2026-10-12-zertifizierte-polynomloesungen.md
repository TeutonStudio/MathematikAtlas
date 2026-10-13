# Zertifizierte Polynomlösungen

## Titel und Status
Abgeschlossen mit dokumentierter Android-Laufzeitprüfgrenze; Auftrag: analytischer und numerischer Langdruckdialog für skalare, Vektor- und Matrixgleichungen.

## Ziel und Nutzerwirkung
Zwei Tabs mit getrennten Existenz-, Vollständigkeits-, Dimensions- und Dezimalziffernnachweisen. n bezeichnet Abschneiden gegen null, nicht Rundung.

## Nicht-Ziele
Kein allgemeiner algebraischer Geometrielöser, keine unbelegten globalen Aussagen für nichtlineare Matrixsysteme, keine Veröffentlichung.

## Untersuchter Istzustand
Gemeinsamer PrädikatsRechenwegDialog auf Android/Desktop; Gleichungsauswerter ruft lösePrädikat auf. Rationale Intervalle und Gaußverfahren existieren. Matrixeinträge können normale Zahlterme mit Variablen sein. Umfangreicher vorhandener Arbeitsstand auf samai/v3.1.2/vektorpotenz-unterraum-geometrie bleibt erhalten.

## Fachliche und mathematische Semantik
X²a+Xb+c und X²A+XB+C werden in Originalreihenfolge komponentenweise expandiert. Rang entscheidet nur lineare Systeme. Leere Teilmengen beweisen keine Existenz. Numerische Kandidaten benötigen Einschließungszertifikate; erfolglose Suche bleibt offen. Familien benötigen für numerische Auswertung eine Parameterbelegung.

## Daten-, Node-, Handle- und Edge-Vertrag
Vorhandenen Gleichungslöser erweitern, keine neue Knotenart. Numerikkonfiguration in parameter, Laufzeitergebnisse im Adapterergebnis. Alte Karten verwenden n=6 und Suchradius=10. Stabile Ports bleiben erhalten.

## Architekturentscheidungen
Mathematische Verfahren im Kern; gemeinsamer Hintergrunddienst in MathematikKnoten; Compose rendert Ergebnisse. Zertifikate werden unabhängig überprüft. Keine Produktionsabhängigkeit.

## Betroffene Dateien und Symbole
Neue Kernmodelle/-verfahren für Polynomlösungen und Gleichungssysteme; KnotenAuswertungsErgebnis, StandardMathematikAuswerter, PrädikatsRechenwegDialog und dessen Android-/Desktop-Aufrufer.

## Meilensteine
- [x] Fachmodell und Zertifikate
- [x] Skalare Wurzeln und Ziffernnachweis
- [x] Vektor-/Matrixsysteme und Eigenwerte
- [x] Dialog und persistierte Konfiguration
- [x] Tests und unabhängige Verifikation

## Konkrete Umsetzungsschritte
Exakte rationale Polynomarithmetik und Sturm-Isolation; komplexe rationale Polynome mit lokalen Rouché-Zertifikaten; komponentenweise Polynomsysteme mit exaktem linearem Pfad und Krawczyk-Nachweisen. Gemeinsame UI mit n-Eingabe, Abbruch und Iterationsprotokoll.

## Tests und Validierung
Gezielte Kern- und Knotenregressionen einschließlich falscher Zertifikate, Dezimalgrenzen, Nichtkommutativität, Familien und offenem Status; danach Gradle-Tests, Repositoryprüfungen und Debugbuild. Laufzeitprüfungen separat dokumentieren.

## Persistenz und Migration
Nur neue Stringparameter; kein JSON-Formatwechsel. Abgeleitete Ergebnisse werden nicht gespeichert. Änderungen über vorhandene Kartenaktionen.

## Risiken und Rückfallstrategie
Budgetierte Suche kann offen enden. Keine unbelegten Lösungen ausgeben. Bestehenden exakten Prädikatspfad als Rückfall erhalten. Vorhandene Änderungen nicht zurücksetzen.

## Fortschritt
Implementierung abgeschlossen. 13 neue Kernnachweistests, 6 Integrationsprüfungen und 2 echte Desktop-Compose-Dialogtests bestehen. Insgesamt 1557 Tests ohne Fehler oder Überspringen. Vorhandener Arbeitsstand erhalten.

## Entscheidungsprotokoll
2026-10-12: Nutzer wählt nachweisbare Teilfälle und erste n Dezimalziffern ohne Rundung. Matrixunbekannte über geordnete skalare Eintragsvariablen repräsentieren, statt einen zweiten symbolischen Matrixausdrucksbaum einzuführen.

## Abweichungen vom ursprünglichen Plan
Komplexe Isolationsnachweise verwenden rationale Rouché-Scheiben statt Rechteck-Randzählungen; Koordinatenintervalle bleiben gesicherte Schranken. Analytische Cardano-/Ferrari-Darstellungen werden mit Zweigbedingungen ausgegeben; algebraische Wurzeln bleiben durch Originalpolynom und Nachweis bestimmt. Im quadratischen System wird der Rang des linearen Anteils diagnostiziert, ohne daraus Lösbarkeit abzuleiten. Begrenzungen: C-Startwertsuche bis Grad 32, Matrixvariable bis Dimension 8, lokale nichtlineare Systemsuche bis 16 reelle Unbekannte. Weitere Fälle bleiben ausdrücklich offen. Android-Laufzeitprüfung entfällt, weil adb devices -l kein Gerät meldet; Desktop-Laufzeitprüfung wurde tatsächlich ausgeführt.

## Ergebnis und Verifikation
- Final: ./gradlew test :app:assembleDebug erfolgreich (149 Tasks, davon 28 ausgeführt).
- XML-Ergebnisse: TypSystem 10, Kern 483, Graph 119, Adapter 49, Knoten 618, App 268, Desktop 10; zusammen 1557, keine Fehler/übersprungenen Tests.
- Repository-/Architekturprüfung, Releaseplan, Versionsfolge und git diff --check erfolgreich.
- scripts/pruefe_kern.py nicht ausführbar: kotlinc fehlt im PATH; vollständige Kernprüfungen über Gradle bestehen.
- node_verifier prüfte unabhängig drei Durchgänge; alle Findings zu Vielfachheit, Protokoll und Familienaufträgen geschlossen. Finale mathematische Prüfung ohne weitere Befunde.
- Android-Debug-APK gebaut; kein verbundener Emulator, keine physische Geräteprüfung.
- Keine neue Knotenart, Abhängigkeit, Kartenformatversion oder Versionsreservierung. Kein Commit/Push erzeugt.
- Arbeitsbranch samai/v3.1.2/vektorpotenz-unterraum-geometrie, vorhandener HEAD ff020f06c5f6aad7e17cef0292cce0b16384917b. Autor-/Committerangaben für einen neuen Commit entfallen.
