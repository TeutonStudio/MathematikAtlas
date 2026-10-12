# Prädikate auflösen und auswerten

Status: in Umsetzung; beauftragter Plan vom 2026-10-12.

## Ziel und Nutzerwirkung
Auflösen liefert bereichstreue Lösungsmengen; Auswerten liefert vereinfachte Objekte und begründete Aussagenentscheidungen. Antippbare Rechenschritte mit Unterdialog auf Android/Desktop.

## Nicht-Ziele
Keine neuen Knotentypen, Produktionsabhängigkeiten oder Releaseveröffentlichung. Höhere Polynome, transzendente und nichtlineare Systeme bleiben symbolisch.

## Untersuchter Istzustand
MathematikAuswerter enthält private Vereinfachung und einen nur linearen Löser. löseLinear verwechselt Identität/Widerspruch. Quellen von Variable und allgemeinem Parameter tragen dieselben Rollenmetadaten. Gauß besitzt bereits Tabellen und Zeilenoperationen. Umfangreiche fremde uncommittete Änderungen vorhanden; erhalten.

## Semantik und Verträge
Alle Variablenquellen sind Zielvariablen; Parameterquellen bleiben frei. Bereiche und Annahmen gelten für alle Lösungen. Offene Aussagen sind keine dritte Wahrheitskonstante. Nullstelliger Raum ist {()}. Historische Eingaben ohne Metadaten verwenden den gespeicherten Variablennamen. Konflikte gleicher Namen werden gemeldet.

## Architekturentscheidungen
Domänenrechnung im Kern, Herkunft im Adapter, gemeinsame Compose-Schrittansicht in MathematikKnoten. Ergebnisse und Schritte sind nicht persistiert. Bestehende Anschluss-IDs und Registry-Schlüssel bleiben.

## Betroffene Dateien und Symbole
Kern: PrädikatsLösung, UmformungsSchritt, Vereinfachung. Adapter: VariablenQuelle, KnotenAuswertungsErgebnis. Knoten: StandardMathematikAuswerter, gemeinsame Rechenwegansicht. Android-/Desktopdialoganbindung.

## Meilensteine und Umsetzungsschritte
- [-] Quellenrollen und Domänenlöser mit linear/quadratisch, Gauß, endlichen Bereichen und Mengenlogik.
- [ ] Rechenweg und Detaildialog gemeinsam anbinden.
- [ ] Regressionen, Gesamtprüfungen und unabhängige Abnahme.

## Tests und Validierung
Domänenrandfälle, Herkunft und verbundene Karten, historische Namen, Schrittketten, Android/Desktop-Kompilierung; Repositoryprüfungen und Gradletests. Laufzeitprüfung soweit Geräte verfügbar.

## Persistenz und Migration
Keine Formatänderung. Rollen und Ergebnisspuren werden aus dem Graph rekonstruiert. Bestehende Knoten und Verbindungen bleiben.

## Risiken und Rückfallstrategie
Unbekannte Mitgliedschaft/Verfahren behalten exakte Restbedingungen. Kein stilles Verwerfen unbekannter Kandidaten. Keine vollständigen Lösungen aus Teilaufzählung. Bei Budgetende symbolisch zurückfallen.

## Fortschritt
Initiale Bestandsprüfung abgeschlossen; Implementierung läuft.

## Entscheidungsprotokoll
2026-10-12: Nutzer wählt Algebra-Grundpaket und Beibehaltung generischer Termauswertung; Rollen automatisch aus Variable-/Parameterknoten.

## Abweichungen vom ursprünglichen Plan
Noch keine.

## Ergebnis und Verifikation
Ausstehend.
